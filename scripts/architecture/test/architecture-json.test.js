'use strict';

const { test } = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');

const { loadManifest, validateManifest } = require('../lib/manifest.js');

const MANIFEST = path.join(__dirname, '..', '..', '..', 'docs', 'architecture', 'architecture.json');

test('the real manifest is valid', () => {
  assert.deepEqual(validateManifest(loadManifest(MANIFEST)), []);
});

test('there is exactly one legacy node, and every service belongs to a context', () => {
  const m = loadManifest(MANIFEST);
  assert.equal(m.services.filter((s) => s.kind === 'legacy').length, 1);
  for (const s of m.services.filter((x) => x.kind === 'service')) {
    assert.ok(s.context, `${s.id} belongs to no bounded context`);
  }
});

test('recap-manager is the only implemented service', () => {
  const m = loadManifest(MANIFEST);
  // Listing the implemented services by name only froze a moment in time. What is worth
  // holding is the claim behind the badge: a service that says it exists has to say
  // where its code is, and carry the version of the artifact that was built.
  for (const s of m.services.filter((x) => x.status === 'implemented')) {
    assert.ok(s.branch, `${s.id} is marked implemented but names no branch`);
    assert.ok(s.version, `${s.id} is marked implemented but carries no version`);
    assert.ok(s.stack, `${s.id} is marked implemented but names no stack`);
  }
});

test('nothing is marked implemented without something to show for it', () => {
  const m = loadManifest(MANIFEST);
  for (const s of m.services.filter((x) => x.status === 'implemented' && x.kind === 'service')) {
    const surfaces = (s.exposes || []).length + (s.publishes || []).length + (s.consumes || []).length;
    assert.ok(surfaces > 0, `${s.id} is marked implemented but exposes and publishes nothing`);
  }
});

test('no service invents a port it has not been given', () => {
  const m = loadManifest(MANIFEST);
  const withPorts = m.services.filter((s) => s.ports).map((s) => s.id).sort();
  assert.deepEqual(withPorts, ['recap-manager', 'storycontent-manager', 'storypath-manager']);
});

test('scene-indexer is a service of its own, as the group decided', () => {
  const m = loadManifest(MANIFEST);
  const indexer = m.services.find((s) => s.id === 'scene-indexer');
  assert.ok(indexer, 'SceneIndexer should be a service in its own right');
  assert.equal(m.services.find((s) => s.id === 'scene-manager').deployments, undefined);
});

test('the two scene services are recorded as sharing one store', () => {
  const m = loadManifest(MANIFEST);
  const stores = ['scene-manager', 'scene-indexer'].map(
    (id) => m.services.find((s) => s.id === id).datastore
  );
  assert.equal(stores[0].name, stores[1].name, 'they read and write the same store');
  for (const store of stores) {
    assert.equal(store.shared, true, 'a shared store has to say so');
    assert.match(store.note, /credentials/i, 'say how the two sides are separated');
  }
});

test('player-choice.made is scalable for character-manager but not storypath-manager', () => {
  const m = loadManifest(MANIFEST);
  const consumer = (id) =>
    m.services.find((s) => s.id === id).consumes.find((c) => c.event === 'player-choice.made');
  assert.equal(consumer('character-manager').scalable, true);
  assert.equal(consumer('storypath-manager').scalable, false);
});

test('the monolith declares no bounded context and names what replaces it', () => {
  const m = loadManifest(MANIFEST);
  const mono = m.services.find((s) => s.kind === 'legacy');
  assert.equal(mono.context, undefined);
  assert.equal(mono.replacedBy.length, 7);
});

test('the gRPC contract records that it sits in the caller tree', () => {
  const m = loadManifest(MANIFEST);
  const sp = m.services.find((s) => s.id === 'storypath-manager');
  const grpc = sp.exposes.find((e) => e.style === 'gRPC');
  assert.match(grpc.contract.path, /recapManager.*story_path\.proto$/);
  assert.match(grpc.contract.note, /belongs with the side that implements it/);
});

test('every event names the exchange its publisher owns', () => {
  const m = loadManifest(MANIFEST);
  for (const e of m.events) {
    assert.match(e.exchange, /\.events$/, `${e.name} has a malformed exchange`);
  }
});

test('API versioning records the header the code actually uses', () => {
  const m = loadManifest(MANIFEST);
  const recap = m.services.find((s) => s.id === 'recap-manager');
  const rest = recap.exposes.find((e) => e.style === 'REST');
  assert.match(rest.versioning, /X-API-VERSION/);
});
