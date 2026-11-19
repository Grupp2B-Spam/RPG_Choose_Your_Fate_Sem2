'use strict';

/**
 * A read-only scanner for draw.io XML.
 *
 * It exists so the checks can reason about tabs, cells and geometry without a parser.
 * Nothing here reconstructs XML: the output is for inspection only, and the build still
 * works by text splicing. Treating this as a round-trip parser is what corrupted these
 * files last time.
 */

const DIAGRAM = /<diagram name="([^"]*)" id="([^"]*)"[^>]*>([\s\S]*?)<\/diagram>/g;
const CELL_OPEN = /<mxCell\b[^>]*>/g;
const CELL_CLOSE = '</mxCell>';
const ATTR = /(\w+)="([^"]*)"/g;
const GEOMETRY = /<mxGeometry\b([^>]*)/;
const POINTS_ARRAY = /<Array as="points">([\s\S]*?)<\/Array>/;
const POINT = /<mxPoint\b([^>]*)/g;

function attributes(text) {
  const found = {};
  for (const m of text.matchAll(ATTR)) found[m[1]] = m[2];
  return found;
}

function num(value) {
  const n = Number(value);
  return Number.isFinite(n) ? n : 0;
}

/**
 * The text of every <mxCell> in a fragment, children included.
 *
 * A non-greedy regex cannot do this: a cell that holds waypoints contains a
 * self-closing <mxPoint ... /> and the match would stop there, hiding everything after
 * it. So the opening tag is matched, and a cell that is not self-closing is closed by
 * searching for its </mxCell>.
 */
function cellBlocks(fragment) {
  const blocks = [];
  CELL_OPEN.lastIndex = 0;
  let m;
  while ((m = CELL_OPEN.exec(fragment)) !== null) {
    if (m[0].endsWith('/>')) {
      blocks.push(m[0]);
      continue;
    }
    const close = fragment.indexOf(CELL_CLOSE, CELL_OPEN.lastIndex);
    if (close === -1) break; // unterminated; wellFormedProblems reports it
    blocks.push(fragment.slice(m.index, close + CELL_CLOSE.length));
    CELL_OPEN.lastIndex = close + CELL_CLOSE.length;
  }
  return blocks;
}

function parseCell(block) {
  const cut = block.indexOf('>');
  const head = cut === -1 ? block : block.slice(0, cut);
  const body = cut === -1 ? '' : block.slice(cut + 1);
  const a = attributes(head);

  const cell = {
    id: a.id,
    parent: a.parent,
    value: a.value,
    style: a.style || '',
    vertex: a.vertex === '1',
    edge: a.edge === '1',
    source: a.source,
    target: a.target,
    geometry: null,
    points: [],
  };

  const geometry = GEOMETRY.exec(body) || GEOMETRY.exec(head);
  if (geometry) {
    const g = attributes(geometry[1]);
    if (g.x !== undefined || g.width !== undefined) {
      cell.geometry = { x: num(g.x), y: num(g.y), width: num(g.width), height: num(g.height) };
    }
  }

  const array = POINTS_ARRAY.exec(body);
  if (array) {
    for (const m of array[1].matchAll(POINT)) {
      const p = attributes(m[1]);
      cell.points.push({ x: num(p.x), y: num(p.y) });
    }
  }

  return cell;
}

/**
 * @param {string} xmlText
 * @returns {{ tabs: Array<{ name: string, id: string, cells: object[] }> }}
 */
function scan(xmlText) {
  const tabs = [];
  for (const d of xmlText.matchAll(DIAGRAM)) {
    const cells = cellBlocks(d[3]).map(parseCell);
    tabs.push({ name: d[1], id: d[2], cells });
  }
  return { tabs };
}

/**
 * Structural soundness, short of a real parser: the things that would make draw.io
 * refuse to open the file, or make `scan` quietly see less than is there.
 *
 * @returns {string[]} one line per problem
 */
function wellFormedProblems(xmlText) {
  const problems = [];
  const count = (needle) => xmlText.split(needle).length - 1;

  if (!/^<\?xml\b/.test(xmlText)) problems.push('missing XML declaration');
  if (count('<mxfile') !== 1 || count('</mxfile>') !== 1) {
    problems.push('expected exactly one <mxfile> element');
  }
  if (count('<diagram ') !== count('</diagram>')) {
    problems.push('unbalanced <diagram> elements');
  }
  if (count('<root>') !== count('</root>')) {
    problems.push('unbalanced <root> elements');
  }

  // Every <mxCell the file contains must have been consumed by the cell regex. If it
  // was not, the file is truncated or a cell is unterminated, and the scan is blind to
  // whatever follows.
  const matched = cellBlocks(xmlText).length;
  if (matched !== count('<mxCell')) {
    problems.push(`${count('<mxCell') - matched} unterminated <mxCell> element(s)`);
  }

  return problems;
}

module.exports = { scan, wellFormedProblems };
