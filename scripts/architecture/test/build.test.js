'use strict';

const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

const { build, TEMPLATE_RELATIVE, OUTPUT_RELATIVE } = require('../build.js');

const FIXTURES = path.join(__dirname, 'fixtures');
const FIXTURE = path.join(FIXTURES, 'ok');
const FIXTURE_BAD_TOKEN = path.join(FIXTURES, 'bad-token');

/**
 * A copy of the good fixture whose template has Windows line endings. Built here rather
 * than committed, because Git's own checkout conversion would decide what a committed
 * CRLF file looks like on disk, and then the test would prove nothing.
 */
function crlfFixture() {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'arch-crlf-'));
  const dir = path.join(root, 'docs', 'architecture');
  fs.mkdirSync(dir, { recursive: true });
  fs.copyFileSync(
    path.join(FIXTURE, 'docs', 'architecture', 'architecture.json'),
    path.join(dir, 'architecture.json')
  );
  const template = fs.readFileSync(path.join(FIXTURE, TEMPLATE_RELATIVE), 'utf8');
  fs.writeFileSync(path.join(root, TEMPLATE_RELATIVE), template.replace(/\n/g, '\r\n'));
  return root;
}

test('renders the fixture template', () => {
  assert.match(build({ repoRoot: FIXTURE }).xml, /value="Player client"/);
});

test('replaces the token with escaped card content', () => {
  const { xml } = build({ repoRoot: FIXTURE });
  assert.match(xml, /&lt;b&gt;RecapManager&lt;\/b&gt;/);
  assert.doesNotMatch(xml, /\{\{/);
});

test('the output carries the generated banner', () => {
  assert.match(build({ repoRoot: FIXTURE }).xml, /^<\?xml[^>]*\?>\n<!-- GENERATED/);
});

test('output uses LF only', () => {
  assert.doesNotMatch(build({ repoRoot: FIXTURE }).xml, /\r/);
});

test('output ends with a newline', () => {
  assert.match(build({ repoRoot: FIXTURE }).xml, /\n$/);
});

test('a CRLF template produces the same bytes as an LF one', () => {
  assert.equal(build({ repoRoot: crlfFixture() }).xml, build({ repoRoot: FIXTURE }).xml);
});

test('an unresolved token throws and names it', () => {
  assert.throws(() => build({ repoRoot: FIXTURE_BAD_TOKEN }), /scene-mangager/);
});

test('stats report what was resolved', () => {
  const { stats } = build({ repoRoot: FIXTURE });
  assert.equal(stats.resolved, 1);
  assert.equal(stats.services, 1);
});

test('the output path sits next to the template', () => {
  assert.equal(path.dirname(OUTPUT_RELATIVE), path.dirname(TEMPLATE_RELATIVE));
});
