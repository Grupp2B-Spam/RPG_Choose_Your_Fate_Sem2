'use strict';

const { test } = require('node:test');
const assert = require('node:assert/strict');

const h = require('./helpers.js');

const TAB = 'arch-main';

test('tab 1 exists with the expected id', () => {
  assert.ok(h.tabOf(TAB));
});

test('tab 1 is titled in English', () => {
  assert.equal(h.tabOf(TAB).name, 'Architecture');
});

test('every service in the manifest has a box on tab 1', () => {
  const text = h.textOf(TAB);
  for (const s of h.manifest.services) {
    assert.match(text, new RegExp(s.display.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `${s.id} is missing`);
  }
});

test('planned services are drawn with a dashed border', () => {
  for (const s of h.servicesWhere((x) => x.status === 'planned' && x.kind === 'service')) {
    assert.match(h.styleOf(s.id), /dashed=1/, `${s.id} should be dashed`);
  }
});

test('services that exist are not drawn as planned', () => {
  for (const s of h.servicesWhere((x) => x.status !== 'planned')) {
    assert.doesNotMatch(h.styleOf(s.id), /dashed=1/, `${s.id} should not be dashed`);
  }
});

test('the three communication styles use three distinct stroke colours', () => {
  const colours = new Set(h.edgesOf(TAB).map(h.strokeColour).filter(Boolean));
  assert.ok(colours.has('#d79b00'), 'no asynchronous edge');
  assert.ok(colours.has('#6c5ce7'), 'no gRPC edge');
  assert.ok(colours.has('#82b366'), 'no REST edge');
});

test('the legend explains all three styles', () => {
  const text = h.textOf(TAB);
  assert.match(text, /asynchronous event/i);
  assert.match(text, /gRPC/);
  assert.match(text, /REST \/ GraphQL/);
});

test('the monolith is present and outside every bounded-context group', () => {
  const mono = h.cellOf('choose-your-fate-backend');
  assert.ok(mono, 'the monolith has no box');
  assert.equal(mono.parent, '1');
});

test('the monolith points at the contexts that replace it', () => {
  const targets = h
    .edgesOf(TAB)
    .filter((e) => e.source === 'panel-choose-your-fate-backend')
    .map((e) => e.target)
    .sort();
  assert.deepEqual(targets, ['grp-content', 'grp-identity', 'grp-media', 'grp-state']);
});

test('SceneIndexer is drawn inside SceneManager, not as a peer', () => {
  const indexer = h.cellById(TAB, 'deploy-scene-indexer');
  assert.equal(indexer.parent, 'panel-scene-manager');
});

test('ADR-013 is noted as open', () => {
  assert.match(h.textOf(TAB), /ADR-013/);
});

test('every open issue in the manifest is written somewhere on the tab', () => {
  const text = h.textOf(TAB).toLowerCase();
  const marker = { 'ADR-013': 'adr-013', 'ADR-012': 'adr-012', GRAPHQL: 'graphql', OAUTH: 'oauth', KEDA: 'keda', PORTS: 'port allocation', IMAGES: null };
  for (const issue of h.manifest.openIssues) {
    const needle = marker[issue.id];
    if (needle) assert.ok(text.includes(needle), `${issue.id} is not mentioned`);
  }
});

test('every service box carries its API, event and contract detail cells', () => {
  const text = h.textOf(TAB);
  assert.match(text, /\/choose-your-fate\/ai/); // apis
  assert.match(text, /player-choice\.made/); // events
  assert.match(text, /storycontent-manager\.asyncapi\.yaml/); // contracts
});

test('each event in the manifest has a box inside the broker', () => {
  for (const e of h.manifest.events) {
    const id = `ev-${e.name.replace(/\./g, '-')}`;
    const cell = h.cellById(TAB, id);
    assert.ok(cell, `${e.name} has no box`);
    assert.equal(cell.parent, 'broker');
  }
});

test('every service that owns a datastore has one drawn', () => {
  for (const s of h.servicesWhere((x) => x.datastore && x.kind === 'service')) {
    assert.ok(h.cellById(TAB, `db-${s.id}`), `${s.id} has no datastore symbol`);
  }
});

test('long edges are routed through explicit waypoints, each in its own corridor', () => {
  const corridors = h
    .edgesOf(TAB)
    .filter((e) => e.points.length > 0)
    .map((e) => e.points[0].x);
  assert.ok(corridors.length >= 12, 'most long edges should be routed deliberately');
  assert.equal(new Set(corridors).size, corridors.length, 'two edges share a corridor');
});
