'use strict';

const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

const { run } = require('../lib/checks/repo.js');

/** A throwaway repository on disk, because these checks are about what is really there. */
function repo(files) {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'arch-repo-'));
  for (const [relative, content] of Object.entries(files)) {
    const full = path.join(root, relative);
    fs.mkdirSync(path.dirname(full), { recursive: true });
    fs.writeFileSync(full, content);
  }
  return root;
}

const COMPOSE = `services:
  recap-manager:
    build: .
    ports:
      - "8090:8090"
  recap-vault:
    image: mysql:9.5.0
    ports:
      - "3307:3306"
`;

const service = (over = {}) => ({
  id: 'recap-manager',
  kind: 'service',
  display: 'RecapManager',
  status: 'implemented',
  purpose: 'x',
  exposes: [],
  calls: [],
  publishes: [],
  consumes: [],
  ...over,
});

const ctx = (repoRoot, services, events = []) => ({
  repoRoot,
  manifest: { meta: { version: 3, updated: 'x', broker: 'y' }, services, events },
});

const codes = (c) => run(c).map((f) => f.check);

test('check 8 warns when a contract declared written is absent', () => {
  const root = repo({ 'readme.md': 'x' });
  const s = service({ exposes: [{ style: 'REST', detail: 'd', contract: { path: 'nope.yaml', state: 'written' } }] });
  const found = run(ctx(root, [s])).find((f) => f.check === 8);
  assert.equal(found.severity, 'warning');
  assert.match(found.message, /nope\.yaml/);
});

test('check 8 warns when a contract declared todo already exists', () => {
  const root = repo({ 'exists.yaml': 'x' });
  const s = service({ exposes: [{ style: 'REST', detail: 'd', contract: { path: 'exists.yaml', state: 'todo' } }] });
  assert.ok(codes(ctx(root, [s])).includes(8));
});

test('check 8 is quiet when declaration and disk agree', () => {
  const root = repo({ 'exists.yaml': 'x' });
  const s = service({ exposes: [{ style: 'REST', detail: 'd', contract: { path: 'exists.yaml', state: 'written' } }] });
  assert.deepEqual(run(ctx(root, [s])), []);
});

test('check 9 warns when the exchange name is not in the publisher source', () => {
  const root = repo({ 'svc/Config.java': 'public static final String EXCHANGE = "other.events";' });
  const s = service({
    compose: { file: 'svc/compose.yaml', service: 'recap-manager' },
    publishes: [{ event: 'recap-generated', exchange: 'recap.events', state: 'planned' }],
  });
  const found = run(ctx(root, [s])).filter((f) => f.check === 9);
  assert.equal(found.length, 1);
  assert.match(found[0].message, /recap\.events/);
});

test('check 9 is quiet when the exchange name appears in the source', () => {
  const root = repo({ 'svc/Config.java': 'EXCHANGE = "recap.events";' });
  const s = service({
    compose: { file: 'svc/compose.yaml', service: 'recap-manager' },
    publishes: [{ event: 'recap-generated', exchange: 'recap.events', state: 'planned' }],
  });
  assert.equal(codes(ctx(root, [s])).filter((c) => c === 9).length, 0);
});

test('check 9 skips a service with nowhere to look', () => {
  const root = repo({ 'readme.md': 'x' });
  const s = service({ publishes: [{ event: 'e', exchange: 'recap.events', state: 'planned' }] });
  assert.equal(codes(ctx(root, [s])).filter((c) => c === 9).length, 0);
});

test('check 10 warns on a port that disagrees with compose', () => {
  const root = repo({ 'svc/compose.yaml': COMPOSE });
  const s = service({ ports: { http: 9999 }, compose: { file: 'svc/compose.yaml', service: 'recap-manager' } });
  const found = run(ctx(root, [s])).find((f) => f.check === 10);
  assert.match(found.message, /9999/);
});

test('check 10 matches on the host side of a published pair', () => {
  const root = repo({ 'svc/compose.yaml': COMPOSE });
  const s = service({ ports: { http: 8090 }, compose: { file: 'svc/compose.yaml', service: 'recap-manager' } });
  assert.deepEqual(run(ctx(root, [s])), []);
});

test('check 10 skips a service with no ports', () => {
  const root = repo({ 'svc/compose.yaml': COMPOSE });
  const s = service({ compose: { file: 'svc/compose.yaml', service: 'recap-manager' } });
  assert.deepEqual(run(ctx(root, [s])), []);
});

test('check 10 skips a service with no compose block', () => {
  const root = repo({ 'svc/compose.yaml': COMPOSE });
  assert.deepEqual(run(ctx(root, [service({ ports: { http: 8090 } })])), []);
});

test('a missing compose file is one warning, not a crash', () => {
  const root = repo({ 'readme.md': 'x' });
  const s = service({ ports: { http: 8090 }, compose: { file: 'gone.yaml', service: 'recap-manager' } });
  const findings = run(ctx(root, [s]));
  assert.equal(findings.length, 1);
  assert.equal(findings[0].severity, 'warning');
});

test('a service absent from its own compose file is a warning', () => {
  const root = repo({ 'svc/compose.yaml': COMPOSE });
  const s = service({ ports: { http: 8090 }, compose: { file: 'svc/compose.yaml', service: 'ghost' } });
  assert.match(run(ctx(root, [s])).find((f) => f.check === 10).message, /ghost/);
});

test('every repository finding is a warning, never an error', () => {
  const root = repo({ 'readme.md': 'x' });
  const s = service({
    ports: { http: 8090 },
    compose: { file: 'gone.yaml', service: 'recap-manager' },
    exposes: [{ style: 'REST', detail: 'd', contract: { path: 'nope.yaml', state: 'written' } }],
  });
  assert.ok(run(ctx(root, [s])).every((f) => f.severity === 'warning'));
});

test('a contract attached to two published events is reported once', () => {
  const root = repo({ 'readme.md': 'x' });
  const s = service({
    publishes: [
      { event: 'a.updated', exchange: 'x.events', state: 'planned' },
      { event: 'b.updated', exchange: 'x.events', state: 'planned' },
    ],
  });
  const events = [
    { name: 'a.updated', exchange: 'x.events', publisher: 'recap-manager', consumers: [], contract: { path: 'c.yaml', state: 'written' } },
    { name: 'b.updated', exchange: 'x.events', publisher: 'recap-manager', consumers: [], contract: { path: 'c.yaml', state: 'written' } },
  ];
  const found = run(ctx(root, [s], events)).filter((f) => f.check === 8);
  assert.equal(found.length, 1);
});
