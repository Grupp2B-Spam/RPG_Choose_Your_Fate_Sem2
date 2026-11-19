'use strict';

const { test } = require('node:test');
const assert = require('node:assert/strict');

const { run } = require('../lib/checks/geometry.js');

const box = ({ id, parent = '1', x, y, w, h }) => ({
  id,
  parent,
  vertex: true,
  edge: false,
  style: '',
  geometry: { x, y, width: w, height: h },
  points: [],
});

const edge = ({ id, points, parent = '1' }) => ({
  id,
  parent,
  vertex: false,
  edge: true,
  style: '',
  geometry: null,
  points,
});

const ctx = (cells, tabId = 'arch-main') => ({
  scan: { tabs: [{ id: tabId, name: tabId, cells }] },
});

test('check 6 reports two overlapping siblings', () => {
  const findings = run(
    ctx([
      box({ id: 'a', x: 0, y: 0, w: 100, h: 50 }),
      box({ id: 'b', x: 50, y: 10, w: 100, h: 50 }),
    ])
  );
  const found = findings.find((f) => f.check === 6);
  assert.equal(found.severity, 'error');
  assert.match(found.message, /a.*b|b.*a/);
});

test('check 6 ignores a child nested inside its parent', () => {
  const findings = run(
    ctx([
      box({ id: 'outer', x: 0, y: 0, w: 400, h: 200 }),
      box({ id: 'inner', parent: 'outer', x: 10, y: 10, w: 100, h: 50 }),
    ])
  );
  assert.deepEqual(findings, []);
});

test('check 6 ignores boxes that merely touch at an edge', () => {
  const findings = run(
    ctx([
      box({ id: 'a', x: 0, y: 0, w: 100, h: 50 }),
      box({ id: 'b', x: 100, y: 0, w: 100, h: 50 }),
    ])
  );
  assert.deepEqual(findings, []);
});

test('check 6 ignores a cell with no geometry', () => {
  const noGeometry = { id: 'g', parent: '1', vertex: true, edge: false, style: '', geometry: null, points: [] };
  assert.deepEqual(run(ctx([noGeometry, box({ id: 'a', x: 0, y: 0, w: 10, h: 10 })])), []);
});

test('check 7 warns when two edges share a vertical corridor', () => {
  const findings = run(
    ctx([
      edge({ id: 'e1', points: [{ x: 600, y: 100 }] }),
      edge({ id: 'e2', points: [{ x: 600, y: 300 }] }),
    ])
  );
  const found = findings.find((f) => f.check === 7);
  assert.equal(found.severity, 'warning');
});

test('check 7 is quiet when corridors differ', () => {
  const findings = run(
    ctx([
      edge({ id: 'e1', points: [{ x: 600, y: 100 }] }),
      edge({ id: 'e2', points: [{ x: 640, y: 300 }] }),
    ])
  );
  assert.deepEqual(findings, []);
});

test('check 7 ignores edges with no waypoints', () => {
  assert.deepEqual(run(ctx([edge({ id: 'e1', points: [] }), edge({ id: 'e2', points: [] })])), []);
});

test('corridors on different tabs do not collide', () => {
  const findings = run({
    scan: {
      tabs: [
        { id: 'arch-main', name: 'a', cells: [edge({ id: 'e1', points: [{ x: 600, y: 100 }] })] },
        { id: 'contracts', name: 'b', cells: [edge({ id: 'e2', points: [{ x: 600, y: 300 }] })] },
      ],
    },
  });
  assert.deepEqual(findings, []);
});
