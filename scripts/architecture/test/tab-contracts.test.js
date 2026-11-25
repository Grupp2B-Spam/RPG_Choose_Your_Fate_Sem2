'use strict';

const { test } = require('node:test');
const assert = require('node:assert/strict');

const h = require('./helpers.js');

test('tab 6 renders a row per node plus a header', () => {
  const rows = h.textOf('contracts').match(/<tr/g) || [];
  assert.equal(rows.length, h.manifest.services.length + 1);
});

test('tab 6 shows the last-updated stamp from the manifest', () => {
  assert.match(h.textOf('contracts'), new RegExp(h.manifest.meta.updated));
});

test('tab 6 names every service in the manifest', () => {
  const text = h.textOf('contracts');
  for (const s of h.manifest.services) {
    assert.match(text, new RegExp(s.display.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `${s.id} is missing`);
  }
});

test('tab 6 names the one AsyncAPI contract that exists', () => {
  assert.match(h.textOf('contracts'), /storycontent-manager\.asyncapi\.yaml/);
});

test('tab 6 explains what the marks mean', () => {
  const text = h.textOf('contracts');
  assert.match(text, /✓/);
  assert.match(text, /⚠/);
});

test('tab 7 no longer mentions the dropped commerce service', () => {
  const text = h.textOf('alt-indexer');
  assert.doesNotMatch(text, /PurchaseManager|SlotsPurchased/);
});

test('no tab anywhere still mentions the dropped commerce service', () => {
  for (const tab of h.doc.tabs) {
    assert.doesNotMatch(h.textOf(tab.id), /PurchaseManager|SlotsPurchased/, `${tab.id} still has commerce`);
  }
});

test('tab 7 explains why the separate indexer was rejected', () => {
  const text = h.textOf('alt-indexer');
  assert.match(text, /share one datastore/i);
  assert.match(text, /one service, two deployments/i);
});

test('tab 7 states the test that decides ownership', () => {
  assert.match(h.textOf('alt-indexer'), /without asking SceneManager/);
});

test('tab 7 says when a separate indexer would be right', () => {
  assert.match(h.textOf('alt-indexer'), /search cluster/i);
});

test('all nine tabs are present', () => {
  assert.equal(h.doc.tabs.length, 9);
});

test('the manifest declares exactly the tabs the diagram has', () => {
  assert.deepEqual([...h.manifest.meta.tabs].sort(), h.doc.tabs.map((t) => t.id).sort());
});
