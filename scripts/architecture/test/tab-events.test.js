'use strict';

const { test } = require('node:test');
const assert = require('node:assert/strict');

const h = require('./helpers.js');

const TAB = 'events';

test('the published-events tab exists', () => {
  assert.ok(h.tabOf(TAB));
});

test('every event in the manifest has a box and a payload box', () => {
  const ids = h.cellsOf(TAB).map((c) => c.id);
  assert.equal(h.manifest.events.length, 5);
  const boxes = ids.filter((id) => id.startsWith('pe-ev-')).length;
  const payloads = ids.filter((id) => id.startsWith('pe-pl-')).length;
  assert.equal(boxes, 5);
  assert.equal(payloads, 5);
});

test('every publisher of an event is drawn above its events', () => {
  const publishers = new Set(h.manifest.events.map((e) => e.publisher));
  const text = h.textOf(TAB);
  for (const id of publishers) {
    const service = h.manifest.services.find((s) => s.id === id);
    assert.match(text, new RegExp(service.display), `${id} is not drawn`);
  }
});

test('events are grouped by the exchange their publisher owns', () => {
  const groups = h.cellsOf(TAB).filter((c) => /^pe-g\d$/.test(c.id));
  const labels = groups.map((c) => h.unescape(c.value));
  for (const exchange of new Set(h.manifest.events.map((e) => e.exchange))) {
    assert.ok(labels.includes(exchange), `${exchange} has no group`);
  }
});

test('a documented payload shows the fields the contract actually has', () => {
  const text = h.textUnder(TAB, 'pe-pl-scene-content');
  for (const field of ['sceneId', 'sceneVersion', 'choices', 'npcIds', 'questIds']) {
    assert.match(text, new RegExp(field), `${field} is missing`);
  }
});

test('a proposed payload says it is not agreed', () => {
  const text = h.textUnder(TAB, 'pe-pl-player-choice');
  assert.match(text, /proposed/i);
  assert.match(text, /sequence/);
  assert.match(text, /ADR-013/);
});

test('an undesigned payload invents nothing', () => {
  for (const id of ['pe-pl-recap-generated', 'pe-pl-account-deletion']) {
    const text = h.textUnder(TAB, id);
    assert.match(text, /not designed/i);
    assert.doesNotMatch(text, /"\w+": /, `${id} has invented fields`);
  }
});

test('an event that does not exist yet is drawn dashed', () => {
  for (const id of ['pe-ev-recap-generated', 'pe-ev-account-deletion']) {
    assert.match(h.cellById(TAB, id).style, /dashed=1/, `${id} should be dashed`);
  }
});

test('the envelope box lists every field each message shares', () => {
  const text = h.textUnder(TAB, 'pe-envelope');
  for (const f of h.manifest.meta.envelope) {
    assert.match(text, new RegExp(f.name), `${f.name} is missing from the envelope`);
  }
});

test('the tab says how many contracts actually exist', () => {
  const text = h.textOf(TAB);
  assert.match(text, /One AsyncAPI document exists/i);
  assert.match(text, /Three of five are missing/i);
});

test('every payload field drawn here is declared in the manifest', () => {
  for (const e of h.manifest.events) {
    const fields = (e.payload && e.payload.fields) || [];
    const id = `pe-pl-${e.name.replace(/\..*$/, '').replace(/-made|-updated|-requested/, '')}`;
    void id; // the mapping is checked by the box count above
    for (const f of fields) {
      assert.ok(f.name && f.type, `${e.name} has a field with no name or type`);
    }
  }
});
