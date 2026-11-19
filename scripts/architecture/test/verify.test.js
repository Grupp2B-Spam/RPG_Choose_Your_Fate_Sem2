'use strict';

const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

const { build, TEMPLATE_RELATIVE, MANIFEST_RELATIVE, OUTPUT_RELATIVE } = require('../build.js');
const { verify } = require('../verify.js');

const FIXTURE = path.join(__dirname, 'fixtures', 'ok');

/**
 * A temporary copy of the good fixture, optionally damaged.
 *
 * The built output is produced here rather than committed, so a test can never pass
 * because someone remembered to regenerate a fixture.
 */
function makeRepo({ mutateManifest, mutateOutput, omitOutput } = {}) {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'arch-verify-'));
  fs.mkdirSync(path.join(root, 'docs', 'architecture'), { recursive: true });

  const manifest = JSON.parse(fs.readFileSync(path.join(FIXTURE, MANIFEST_RELATIVE), 'utf8'));
  if (mutateManifest) mutateManifest(manifest);
  fs.writeFileSync(path.join(root, MANIFEST_RELATIVE), `${JSON.stringify(manifest, null, 2)}\n`);

  fs.copyFileSync(path.join(FIXTURE, TEMPLATE_RELATIVE), path.join(root, TEMPLATE_RELATIVE));

  if (!omitOutput) {
    let xml = build({ repoRoot: root }).xml;
    if (mutateOutput) xml = mutateOutput(xml);
    fs.writeFileSync(path.join(root, OUTPUT_RELATIVE), xml);
  }
  return root;
}

test('a freshly built repository has no findings at all', () => {
  const result = verify({ repoRoot: makeRepo() });
  assert.deepEqual(result.findings, []);
  assert.equal(result.ok, true);
});

test('check 1 fails when a committed output was hand-edited', () => {
  const root = makeRepo({ mutateOutput: (xml) => xml.replace('Player client', 'Player kliennt') });
  const found = verify({ repoRoot: root }).findings.find((f) => f.check === 1);
  assert.equal(found.severity, 'error');
  assert.match(found.message, /build/);
});

test('check 1 fails when the output is missing entirely', () => {
  const findings = verify({ repoRoot: makeRepo({ omitOutput: true }) }).findings;
  assert.ok(findings.some((f) => f.check === 1));
});

test('ok is false when any finding is an error', () => {
  const root = makeRepo({ mutateOutput: (xml) => xml.replace('Player client', 'x') });
  assert.equal(verify({ repoRoot: root }).ok, false);
});

test('ok is true when only warnings are present', () => {
  const root = makeRepo({
    mutateManifest: (m) => {
      m.services[0].exposes.push({
        style: 'REST',
        detail: 'd',
        contract: { path: 'microservices/contracts/nope.yaml', state: 'written' },
      });
    },
  });
  const result = verify({ repoRoot: root });
  assert.ok(result.findings.some((f) => f.severity === 'warning'));
  assert.ok(result.findings.every((f) => f.severity !== 'error'));
  assert.equal(result.ok, true);
});

test('findings are sorted errors first', () => {
  const root = makeRepo({
    mutateOutput: (xml) => xml.replace('Player client', 'x'),
    mutateManifest: (m) => {
      m.services[0].exposes.push({
        style: 'REST',
        detail: 'd',
        contract: { path: 'microservices/contracts/nope.yaml', state: 'written' },
      });
    },
  });
  const severities = verify({ repoRoot: root }).findings.map((f) => f.severity);
  const errorsLast = severities.lastIndexOf('error');
  const warningsFirst = severities.indexOf('warning');
  assert.ok(warningsFirst === -1 || errorsLast < warningsFirst);
});

test('a template token that does not resolve is reported as an error', () => {
  // The output cannot be built at all from a manifest that no longer answers the
  // token, so verify has to report it rather than let the throw escape.
  const root = makeRepo({
    omitOutput: true,
    mutateManifest: (m) => {
      m.services[0].id = 'renamed-manager';
    },
  });
  const result = verify({ repoRoot: root });
  assert.ok(result.findings.some((f) => f.check === 3));
  assert.equal(result.ok, false);
});
