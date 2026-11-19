'use strict';

const { test } = require('node:test');
const assert = require('node:assert/strict');

const { spliceTokens, escapeForAttribute } = require('../lib/splice.js');

test('replaces a whole-value token with the escaped fragment', () => {
  const out = spliceTokens('<mxCell value="{{a}}" />', () => '<b>Hi</b>');
  assert.equal(out.text, '<mxCell value="&lt;b&gt;Hi&lt;/b&gt;" />');
  assert.deepEqual(out.resolved, ['a']);
  assert.deepEqual(out.unresolved, []);
});

test('escapes ampersand, angle brackets and quotes exactly once', () => {
  const out = spliceTokens('<mxCell value="{{a}}" />', () => 'Tom & "Jerry" <x>');
  assert.equal(out.text, '<mxCell value="Tom &amp; &quot;Jerry&quot; &lt;x&gt;" />');
  assert.doesNotMatch(out.text, /&amp;amp;/);
});

test('escapeForAttribute leaves plain text alone', () => {
  assert.equal(escapeForAttribute('RecapManager 0.0.1'), 'RecapManager 0.0.1');
});

test('reports an unresolved token and leaves it in place', () => {
  const out = spliceTokens('<mxCell value="{{ghost}}" />', () => null);
  assert.deepEqual(out.unresolved, ['ghost']);
  assert.match(out.text, /\{\{ghost\}\}/);
});

test('a token outside a whole value is reported as unresolved', () => {
  const out = spliceTokens('<mxCell value="see {{a}}" />', () => 'X');
  assert.deepEqual(out.resolved, []);
  assert.equal(out.unresolved.length, 1);
});

test('leaves cells without tokens untouched', () => {
  const src = '<mxCell value="Player client" style="x" />';
  assert.equal(spliceTokens(src, () => 'X').text, src);
});

test('replaces every occurrence of the same token', () => {
  const out = spliceTokens('<a value="{{a}}"/><b value="{{a}}"/>', () => 'X');
  assert.equal(out.resolved.length, 2);
  assert.equal(out.text, '<a value="X"/><b value="X"/>');
});

test('reports each distinct unresolved token once per occurrence', () => {
  const out = spliceTokens('<a value="{{x}}"/><b value="{{y}}"/>', () => null);
  assert.deepEqual(out.unresolved.sort(), ['x', 'y']);
});
