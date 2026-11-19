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
        <mxCell id="b" value="Another box" style="rounded=1;" vertex="1" parent="1">
          <mxGeometry x="400" y="60" width="180" height="75" as="geometry" />
        </mxCell>
        <mxCell id="panel-recap-manager" value="" style="rounded=1;strokeColor=#78909c;" vertex="1" parent="1">
          <mxGeometry x="40" y="200" width="280" height="290" as="geometry" />
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
    meta: { version: 3, updated: '2026-11-18', tabs: ['arch-main', 'contracts'] },
    services: [
      { id: 'recap-manager', kind: 'service', display: 'RecapManager', status: 'implemented', purpose: 'x' },
    ],
    infrastructure: [],
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

// --- the scanner ------------------------------------------------------------------

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

test('scan reads a diagram whose attributes are in the other order', () => {
  const xml = SAMPLE.replace(
    '<diagram name="Architecture" id="arch-main">',
    '<diagram id="arch-main" visible="1" name="Architecture">'
  );
  const tab = scan(xml).tabs.find((t) => t.id === 'arch-main');
  assert.ok(tab, 'the scanner should not depend on attribute order');
  assert.equal(tab.name, 'Architecture');
});

// --- check 2: the file opens ------------------------------------------------------

test('check 2 fails on truncated XML', () => {
  assert.ok(codes({ xml: '<?xml version="1.0"?>\n<mxfile><diagram name="x" id="arch-main">' }).includes(2));
});

test('check 2 fails on a bare ampersand that draw.io would refuse', () => {
  const xml = SAMPLE.replace('value="A box"', 'value="Tom & Jerry"');
  const found = checks({ xml }).find((f) => f.check === 2 && /ampersand/i.test(f.message));
  assert.equal(found.severity, 'error');
});

test('check 2 accepts the entities that are supposed to be there', () => {
  const xml = SAMPLE.replace('value="A box"', 'value="&lt;b&gt;A&lt;/b&gt; &amp; &quot;B&quot; &#39;C&#39;"');
  assert.equal(checks({ xml }).filter((f) => f.check === 2).length, 0);
});

test('check 2 fails on an unbalanced quote in a tag', () => {
  const xml = SAMPLE.replace('value="A box"', 'value="A box');
  assert.ok(codes({ xml }).includes(2));
});

test('check 2 says the scanner could not read the file rather than blaming the manifest', () => {
  // A <diagram> the scanner cannot parse must not look like seven missing tabs.
  const xml = '<?xml version="1.0"?>\n<mxfile><diagram-broken></diagram-broken><diagram</mxfile>';
  const messages = checks({ xml }).map((f) => f.message).join('\n');
  assert.match(messages, /could not read/i);
  assert.doesNotMatch(messages, /manifest declares tab/);
});

test('check 2 fails when a declared tab is missing from the output', () => {
  const m = manifest();
  m.meta.tabs = ['arch-main', 'contracts', 'env-k8s'];
  assert.match(checks({ manifest: m }).find((f) => f.check === 2).message, /env-k8s/);
});

test('check 2 fails on a tab the manifest does not declare', () => {
  const m = manifest();
  m.meta.tabs = ['arch-main'];
  assert.match(checks({ manifest: m }).find((f) => f.check === 2).message, /contracts/);
});

// --- checks 3, 4, 5, 11 ------------------------------------------------------------

test('check 3 fails on a token the resolver cannot satisfy', () => {
  assert.equal(checks({ resolve: () => null }).find((f) => f.check === 3).severity, 'error');
});

test('check 4 names a service no token mentions', () => {
  const m = manifest();
  m.services.push({ id: 'texttospeech-manager', kind: 'service', display: 'T', status: 'planned', purpose: 'x' });
  assert.match(checks({ manifest: m }).find((f) => f.check === 4).message, /texttospeech-manager/);
});

test('check 4 names an infrastructure version that no token draws', () => {
  const m = manifest();
  m.infrastructure = [{ id: 'rabbitmq', display: 'RabbitMQ', kind: 'broker', image: 'rabbitmq:4-management-alpine' }];
  const found = checks({ manifest: m }).find((f) => f.check === 4 && /rabbitmq/.test(f.message));
  assert.equal(found.severity, 'error');
});

test('check 4 ignores infrastructure that declares no version to drift', () => {
  const m = manifest();
  m.infrastructure = [{ id: 'ollama', display: 'Ollama', kind: 'LLM runtime', external: true }];
  assert.equal(checks({ manifest: m }).filter((f) => f.check === 4).length, 0);
});

test('check 5 rejects an id that is not lowercase-hyphen', () => {
  const m = manifest();
  m.services[0].id = 'Recap_Manager';
  assert.ok(codes({ manifest: m }).includes(5));
});

test('check 11 warns about an event with no consumers', () => {
  const m = manifest();
  m.events[0].consumers = [];
  assert.equal(checks({ manifest: m }).find((f) => f.check === 11).severity, 'warning');
});

test('check 11 warns about an event with fewer queues than consumers', () => {
  const m = manifest();
  m.events[0].queues = [];
  assert.ok(codes({ manifest: m }).includes(11));
});

// --- check 12: the border agrees with the status -----------------------------------

test('check 12 fails when a planned service is not drawn dashed', () => {
  const m = manifest();
  m.services[0].status = 'planned';
  const found = checks({ manifest: m }).find((f) => f.check === 12);
  assert.equal(found.severity, 'error');
  assert.match(found.message, /recap-manager/);
});

test('check 12 fails when a service that exists is drawn as planned', () => {
  const xml = SAMPLE.replace(
    'id="panel-recap-manager" value="" style="rounded=1;strokeColor=#78909c;"',
    'id="panel-recap-manager" value="" style="rounded=1;dashed=1;strokeColor=#78909c;"'
  );
  assert.ok(codes({ xml }).includes(12));
});

test('check 12 is quiet when the badge and the border agree', () => {
  assert.equal(checks().filter((f) => f.check === 12).length, 0);
});

// --- checks 13 and 14: nothing is silently dropped ---------------------------------

test('check 13 fails on an edge pointing at a cell that does not exist', () => {
  const xml = SAMPLE.replace('target="b"', 'target="ghost"');
  const found = checks({ xml }).find((f) => f.check === 13);
  assert.equal(found.severity, 'error');
  assert.match(found.message, /ghost/);
});

test('check 13 fails on a cell whose parent does not exist', () => {
  const xml = SAMPLE.replace('id="b" value="Another box" style="rounded=1;" vertex="1" parent="1"', 'id="b" value="Another box" style="rounded=1;" vertex="1" parent="nowhere"');
  assert.ok(codes({ xml }).includes(13));
});

test('check 14 fails when a child escapes its parent bounds', () => {
  const xml = SAMPLE.replace(
    '<mxCell id="b" value="Another box" style="rounded=1;" vertex="1" parent="1">\n          <mxGeometry x="400" y="60" width="180" height="75" as="geometry" />',
    '<mxCell id="b" value="Another box" style="rounded=1;" vertex="1" parent="a">\n          <mxGeometry x="400" y="60" width="180" height="75" as="geometry" />'
  );
  const found = checks({ xml }).find((f) => f.check === 14);
  assert.equal(found.severity, 'error');
  assert.match(found.message, /\bb\b/);
});

test('a well-formed, complete diagram produces no structural findings', () => {
  assert.deepEqual(checks(), []);
});
