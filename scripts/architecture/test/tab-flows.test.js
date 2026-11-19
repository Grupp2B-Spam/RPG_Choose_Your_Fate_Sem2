'use strict';

const { test } = require('node:test');
const assert = require('node:assert/strict');

const h = require('./helpers.js');

test('both flow tabs exist with the expected ids', () => {
  const ids = h.doc.tabs.map((t) => t.id);
  assert.ok(ids.includes('flow-choice'));
  assert.ok(ids.includes('flow-recap'));
});

test('the choice flow names the per-character sequence number', () => {
  assert.match(h.textOf('flow-choice'), /sequence/i);
});

test('the choice flow marks ADR-013 as open', () => {
  assert.match(h.textOf('flow-choice'), /ADR-013/);
});

test('the choice flow shows one event reaching two independent consumers', () => {
  const text = h.textOf('flow-choice');
  assert.match(text, /charactermanager\.player-choice/);
  assert.match(text, /storypathmanager\.player-choice/);
  assert.match(text, /no saga/i);
});

test('the choice flow returns the next scene without waiting for the consumers', () => {
  assert.match(h.textOf('flow-choice'), /without waiting/i);
});

test('the recap flow calls StoryPathManager over gRPC, not REST', () => {
  const text = h.textOf('flow-recap');
  assert.match(text, /GetChoicesAfter/);
  assert.doesNotMatch(text, /GET \/storypaths/);
});

test('the recap flow shows the checkpoint and the snapshot sequence', () => {
  const text = h.textOf('flow-recap');
  assert.match(text, /checkpoint/i);
  assert.match(text, /snapshotSequence/);
});

test('the recap flow still explains content-hash idempotence', () => {
  assert.match(h.textOf('flow-recap'), /sha256/);
});

test('the recap flow says plainly which steps are not built yet', () => {
  assert.match(h.textOf('flow-recap'), /not built yet/i);
});

test('the gRPC messages are drawn in the gRPC colour', () => {
  const grpc = h
    .edgesOf('flow-recap')
    .filter((e) => h.strokeColour(e) === '#6c5ce7');
  assert.ok(grpc.length >= 3, 'the path fetch should be drawn as gRPC');
});

test('no lifeline on either flow overlaps another', () => {
  for (const tab of ['flow-choice', 'flow-recap']) {
    const lifelines = h.cellsOf(tab).filter((c) => /^f[cr]-l\d$/.test(c.id));
    assert.equal(lifelines.length, 6);
    const sorted = [...lifelines].sort((a, b) => a.geometry.x - b.geometry.x);
    for (let i = 1; i < sorted.length; i += 1) {
      const previous = sorted[i - 1].geometry;
      assert.ok(sorted[i].geometry.x >= previous.x + previous.width, `${tab} lifelines collide`);
    }
  }
});

test('annotations sit inside the lifeline they describe', () => {
  for (const tab of ['flow-choice', 'flow-recap']) {
    for (const cell of h.cellsOf(tab).filter((c) => /^f[cr]-a\d$/.test(c.id))) {
      assert.match(cell.parent, /^f[cr]-l\d$/, `${cell.id} is not attached to a lifeline`);
    }
  }
});
