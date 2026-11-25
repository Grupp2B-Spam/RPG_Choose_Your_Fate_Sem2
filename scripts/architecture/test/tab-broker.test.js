'use strict';

const { test } = require('node:test');
const assert = require('node:assert/strict');

const h = require('./helpers.js');

const TAB = 'broker-topology';

test('the broker topology tab exists', () => {
  assert.ok(h.tabOf(TAB));
  assert.equal(h.tabOf(TAB).name, 'Broker topology');
});

test('every exchange in the manifest has a box', () => {
  const text = h.textOf(TAB);
  for (const exchange of new Set(h.manifest.events.map((e) => e.exchange))) {
    assert.match(text, new RegExp(exchange.replace('.', '\\.')), `${exchange} is missing`);
  }
  assert.match(text, /\bdlx\b/, 'the dead-letter exchange is missing');
});

test('every queue in the manifest has a box', () => {
  const text = h.textOf(TAB);
  for (const e of h.manifest.events) {
    for (const queue of e.queues || []) {
      assert.match(text, new RegExp(queue.replace(/\./g, '\\.')), `${queue} is missing`);
    }
  }
});

test('the queues that exist are solid and the ones that do not are dashed', () => {
  for (const id of ['bt-q-scene-content', 'bt-q-item-rules', 'bt-q-race-definitions']) {
    assert.doesNotMatch(h.cellById(TAB, id).style, /dashed=1/, `${id} is declared and should be solid`);
  }
  for (const id of ['bt-q-storypath-pc', 'bt-q-character-pc', 'bt-q-tts-recap', 'bt-q-account-del']) {
    assert.match(h.cellById(TAB, id).style, /dashed=1/, `${id} does not exist yet`);
  }
});

test('each declared queue has a dead-letter queue of its own', () => {
  const text = h.textOf(TAB);
  for (const e of h.manifest.events.filter((x) => x.deadLetter)) {
    for (const dlq of e.deadLetter.queues) {
      assert.match(text, new RegExp(dlq.replace(/\./g, '\\.')), `${dlq} is missing`);
    }
  }
});

test('every binding edge carries the routing key it binds on', () => {
  const bindings = h.edgesOf(TAB).filter((e) => /^bt-b\d$/.test(e.id));
  assert.equal(bindings.length, 7);
  const keys = new Set(h.manifest.events.map((e) => e.name));
  for (const b of bindings) {
    assert.ok(keys.has(h.unescape(b.value)), `${b.id} is not labelled with a routing key`);
  }
});

test('the tab explains the first-start window the arrangement exists for', () => {
  const text = h.textOf(TAB);
  assert.match(text, /stores nothing/i);
  assert.match(text, /discarded/i);
  assert.match(text, /returns success/i);
});

test('the tab says the arrangement is interim and how it is undone', () => {
  const text = h.textOf(TAB);
  assert.match(text, /interim/i);
  assert.match(text, /applied at deploy time/i);
});

test('the tab explains that a queue is point-to-point', () => {
  assert.match(h.textOf(TAB), /exactly one consumer/i);
});

test('the renamed queue is drawn with the reason it was renamed', () => {
  const text = h.unescape(h.cellById(TAB, 'bt-q-race-definitions').value);
  assert.match(text, /race-cache/);
  assert.match(text, /publisher owns the queue/i);
});

test('the tab says the solid half was read off a running broker', () => {
  assert.match(h.textOf(TAB), /running broker/i);
  assert.match(h.textOf(TAB), /4\.4\.0/);
});

test('the dead-letter note says why the queues matter and not just the exchange', () => {
  const text = h.textUnder(TAB, 'bt-dlq-note');
  assert.match(text, /nothing bound to it discards/i);
  assert.match(text, /Nothing consumes/i);
});
