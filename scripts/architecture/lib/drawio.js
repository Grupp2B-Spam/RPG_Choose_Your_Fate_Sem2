'use strict';

/**
 * A read-only scanner for draw.io XML.
 *
 * It exists so the checks can reason about tabs, cells and geometry without a parser.
 * Nothing here reconstructs XML: the output is for inspection only, and the build still
 * works by text splicing. Treating this as a round-trip parser is what corrupted these
 * files last time.
 */

// Attribute order is not guaranteed. draw.io writes name before id today, but a file
// that puts id first is equally valid, and a regex demanding one order would report
// every tab as missing rather than reading the file.
const DIAGRAM = /<diagram(\s[^>]*)?>([\s\S]*?)<\/diagram>/g;
const DIAGRAM_PRESENT = /<diagram\b/;
const CELL_OPEN = /<mxCell\b[^>]*>/g;
const CELL_CLOSE = '</mxCell>';
const ATTR = /(\w+)="([^"]*)"/g;
const GEOMETRY = /<mxGeometry\b([^>]*)/;
const POINTS_ARRAY = /<Array as="points">([\s\S]*?)<\/Array>/;
const POINT = /<mxPoint\b([^>]*)/g;
const TAG = /<[^>]*>/g;
const BARE_AMPERSAND = /&(?!(?:[a-zA-Z][a-zA-Z0-9]*|#[0-9]+|#x[0-9a-fA-F]+);)/g;

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
    const a = attributes(d[1] || '');
    const cells = cellBlocks(d[2]).map(parseCell);
    tabs.push({ name: a.name, id: a.id, cells });
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

  // The two ways a .drawio file ends up unopenable without any of the counts above
  // noticing. Both come from hand-editing the template in a text editor, which the
  // README invites people to do.
  const bare = xmlText.match(BARE_AMPERSAND);
  if (bare) {
    problems.push(
      `${bare.length} bare ampersand(s): write &amp; — an unescaped & makes draw.io refuse the file`
    );
  }

  for (const tag of xmlText.matchAll(TAG)) {
    const quotes = (tag[0].match(/"/g) || []).length;
    if (quotes % 2 !== 0) {
      problems.push(`unbalanced quote in ${tag[0].slice(0, 60).replace(/\s+/g, ' ')}…`);
      break;
    }
  }

  return problems;
}

/** True when the file contains something calling itself a diagram that `scan` could not read. */
function looksUnreadable(xmlText, scanned) {
  return scanned.tabs.length === 0 && DIAGRAM_PRESENT.test(xmlText);
}

module.exports = { scan, wellFormedProblems, looksUnreadable };
