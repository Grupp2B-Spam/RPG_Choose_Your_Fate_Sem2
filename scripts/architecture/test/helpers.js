'use strict';

/**
 * Shared helpers for the per-tab tests.
 *
 * Everything is read from a fresh in-memory render, not from the committed
 * architecture.drawio: a tab test should fail when the template or the manifest is
 * wrong, and whether the committed copy is stale is check 1's job, not theirs.
 */

const path = require('node:path');

const { build } = require('../build.js');
const { loadManifest } = require('../lib/manifest.js');
const { scan } = require('../lib/drawio.js');

const ROOT = path.resolve(__dirname, '..', '..', '..');

const XML = build({ repoRoot: ROOT }).xml;
const manifest = loadManifest(path.join(ROOT, 'docs', 'architecture', 'architecture.json'));
const doc = scan(XML);

const unescape = (s) =>
  String(s || '')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/&amp;/g, '&');

const tabOf = (tabId) => doc.tabs.find((t) => t.id === tabId);
const cellsOf = (tabId) => (tabOf(tabId) || { cells: [] }).cells;
const cellById = (tabId, id) => cellsOf(tabId).find((c) => c.id === id);

/** All visible text on a tab, with entities decoded, for plain substring assertions. */
const textOf = (tabId) =>
  cellsOf(tabId)
    .map((c) => unescape(c.value))
    .join('\n');

const cellOf = (serviceId, tabId = 'arch-main') => cellById(tabId, `panel-${serviceId}`);
const styleOf = (serviceId, tabId = 'arch-main') => (cellOf(serviceId, tabId) || {}).style || '';

const edgesOf = (tabId) => cellsOf(tabId).filter((c) => c.edge);
const strokeColour = (cell) => {
  const m = /strokeColor=(#[0-9a-fA-F]{6})/.exec(cell.style || '');
  return m ? m[1].toLowerCase() : null;
};
const isNode = (cell) => cell.vertex && cell.geometry !== null;

const servicesWhere = (predicate) => manifest.services.filter(predicate);

module.exports = {
  ROOT,
  XML,
  manifest,
  scan,
  doc,
  unescape,
  tabOf,
  cellsOf,
  cellById,
  textOf,
  cellOf,
  styleOf,
  edgesOf,
  strokeColour,
  isNode,
  servicesWhere,
};
