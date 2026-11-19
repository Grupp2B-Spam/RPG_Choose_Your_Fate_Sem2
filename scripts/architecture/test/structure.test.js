'use strict';

const { test } = require('node:test');
const assert = require('node:assert/strict');

const { scan } = require('../lib/drawio.js');
const { run } = require('../lib/checks/structure.js');

const SAMPLE = `<?xml version="1.0" encoding="utf-8"?>
<mxfile host="app.diagrams.net" pages="2">
  <diagram name="Architecture" id="arch-main">
    <mxGraphModel grid="1">
      <root>
        <mxCell id="0" />
        <mxCell id="1" parent="0" />
        <mxCell id="a" value="A box" style="rounded=1;dashed=1;" vertex="1" parent="1">
          <mxGeometry x="40" y="60" width="180" height="75" as="geometry" />
        </mxCell>
        <mxCell id="e1" style="strokeColor=#6c5ce7;" edge="1" parent="1" source="a" target="b">
          <mxGeometry relative="1" as="geometry">
            <Array as="points">
              <mxPoint x="600" y="100" />
            </Array>
          </mxGeometry>
        </mxCell>
      </root>
    </mxGraphModel>
  </diagram>
  <diagram name="Contracts" id="contracts">
    <mxGraphModel grid="1">
      <root>
        <mxCell id="0" />
        <mxCell id="1" parent="0" />
      </root>
    </mxGraphModel>
  </diagram>
</mxfile>
`;

function manifest(extra = {}) {
  return {
    meta: { version: 3, updated: '2026-11-18', broker: 'RabbitMQ 4', tabs: ['arch-main', 'contracts'] },
    services: [{ id: 'recap-manager', kind: 'service', display: 'RecapManager', status: 'implemented', purpose: 'x' }],
    events: [
      {
        name: 'recap-generated',
        exchange: 'recap.events',
        publisher: 'recap-manager',
        consumers: ['recap-manager'],
        queues: ['recapmanager.self'],
      },
    ],
    ...extra,
  };
}

function ctx(over = {}) {
  const xml = over.xml || SAMPLE;
  return {
    repoRoot: '/nowhere',
    manifest: over.manifest || manifest(),
    template: over.template || '<mxCell value="{{service:recap-manager:card}}" />',
    xml,
    scan: scan(xml),
    resolve: over.resolve || (() => 'content'),
    ...over,
  };
}

const checks = (over) => run(ctx(over));
const codes = (over) => checks(over).map((f) => f.check);

test('scan finds tabs, cells and geometry', () => {
  const s = scan(SAMPLE);
  assert.equal(s.tabs.length, 2);
  assert.equal(s.tabs[0].name, 'Architecture');
  assert.equal(s.tabs[0].cells.find((c) => c.id === 'a').geometry.width, 180);
});

test('scan distinguishes edges from vertices', () => {
  const s = scan(SAMPLE);
  assert.equal(s.tabs[0].cells.find((c) => c.id === 'e1').edge, true);
  assert.equal(s.tabs[0].cells.find((c) => c.id === 'a').vertex, true);
});

test('scan reads edge waypoints', () => {
  const e = scan(SAMPLE).tabs[0].cells.find((c) => c.id === 'e1');
  assert.deepEqual(e.points, [{ x: 600, y: 100 }]);
});

test('scan keeps the parent of every cell', () => {
  assert.equal(scan(SAMPLE).tabs[0].cells.find((c) => c.id === 'a').parent, '1');
});

test('check 2 fails on truncated XML', () => {
  assert.ok(codes({ xml: '<?xml version="1.0"?>\n<mxfile><diagram name="x" id="arch-main">' }).includes(2));
});

test('check 2 fails when a declared tab is missing from the output', () => {
  const m = manifest();
  m.meta.tabs = ['arch-main', 'contracts', 'env-k8s'];
  const found = checks({ manifest: m }).find((f) => f.check === 2);
  assert.match(found.message, /env-k8s/);
});

test('check 2 fails on a tab the manifest does not declare', () => {
  const m = manifest();
  m.meta.tabs = ['arch-main'];
  assert.match(checks({ manifest: m }).find((f) => f.check === 2).message, /contracts/);
});

test('check 3 fails on a token the resolver cannot satisfy', () => {
  const found = checks({ resolve: () => null }).find((f) => f.check === 3);
  assert.equal(found.severity, 'error');
});

test('check 4 names a service no token mentions', () => {
  const m = manifest();
  m.services.push({ id: 'texttospeech-manager', kind: 'service', display: 'T', status: 'planned', purpose: 'x' });
  assert.match(checks({ manifest: m }).find((f) => f.check === 4).message, /texttospeech-manager/);
});

test('check 5 rejects an id that is not lowercase-hyphen', () => {
  const m = manifest();
  m.services[0].id = 'Recap_Manager';
  assert.ok(codes({ manifest: m }).includes(5));
});

test('check 11 warns about an event with no consumers', () => {
  const m = manifest();
  m.events[0].consumers = [];
  const found = checks({ manifest: m }).find((f) => f.check === 11);
  assert.equal(found.severity, 'warning');
});

test('check 11 warns about an event with fewer queues than consumers', () => {
  const m = manifest();
  m.events[0].queues = [];
  assert.ok(codes({ manifest: m }).includes(11));
});

test('a well-formed, complete diagram produces no structural findings', () => {
  assert.deepEqual(checks(), []);
});
