'use strict';

const { test } = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');

const { loadManifest } = require('../lib/manifest.js');
const { createResolver } = require('../lib/render.js');

const MANIFEST = path.join(__dirname, '..', '..', '..', 'docs', 'architecture', 'architecture.json');
const manifest = loadManifest(MANIFEST);
const resolve = createResolver(manifest);

test('card carries display name, purpose and version badge', () => {
  const html = resolve('service:recap-manager:card');
  assert.match(html, /RecapManager/);
  assert.match(html, /recap of a character/);
  assert.match(html, /0\.0\.1-SNAPSHOT/);
});

test('card of a planned service says planned and shows no version', () => {
  const html = resolve('service:account-manager:card');
  assert.match(html, /planned/);
  assert.doesNotMatch(html, /SNAPSHOT/);
});

test('card of a branch service names the branch', () => {
  assert.match(
    resolve('service:storycontent-manager:card'),
    /patrick\/create-storycontentmanager-microservice/
  );
});

test('card of a service on main does not clutter itself with the branch name', () => {
  assert.doesNotMatch(resolve('service:recap-manager:card'), /branch/i);
});

test('card of the legacy node says it is being decomposed', () => {
  assert.match(resolve('service:choose-your-fate-backend:card'), /decomposed/);
});

test('apis lists one line per exposed style', () => {
  assert.match(resolve('service:recap-manager:apis'), /REST.*\/choose-your-fate\/ai/s);
});

test('apis of a service exposing nothing yet says so', () => {
  assert.match(resolve('service:texttospeech-manager:apis'), /none yet/);
});

test('events separates published from consumed', () => {
  const html = resolve('service:scene-manager:events');
  assert.match(html, /player-choice\.made/);
  assert.match(html, /scene-content\.updated/);
});

test('events of a service with neither says so', () => {
  assert.match(resolve('service:choose-your-fate-backend:events'), /none/i);
});

test('contracts marks a written contract differently from a todo', () => {
  const written = resolve('service:storycontent-manager:contracts');
  const todo = resolve('service:character-manager:contracts');
  assert.notEqual(written, todo);
  assert.match(written, /storycontent-manager\.asyncapi\.yaml/);
});

test('contracts shows the proto for the service that implements it', () => {
  assert.match(resolve('service:storypath-manager:contracts'), /story_path\.proto/);
});

test('event label carries exchange and queue count', () => {
  const html = resolve('event:player-choice.made:label');
  assert.match(html, /scenemanager\.events/);
  assert.match(html, /2 queues/);
});

test('contract table has one row per service plus a header', () => {
  const rows = resolve('table:contracts').match(/<tr/g);
  assert.equal(rows.length, 9);
});

test('meta:updated renders the manifest date', () => {
  assert.equal(resolve('meta:updated'), '2026-11-18');
});

test('an unknown id returns null rather than an empty string', () => {
  assert.equal(resolve('service:scene-mangager:card'), null);
});

test('an unknown field returns null', () => {
  assert.equal(resolve('service:recap-manager:colour'), null);
});

test('an unknown token kind returns null', () => {
  assert.equal(resolve('sevice:recap-manager:card'), null);
});

test('infra label carries the display name and the image tag', () => {
  const html = resolve('infra:rabbitmq:label');
  assert.match(html, /RabbitMQ/);
  assert.match(html, /rabbitmq:4-management-alpine/);
});

test('infra label of an external service says so instead of inventing an image', () => {
  const html = resolve('infra:elevenlabs:label');
  assert.match(html, /external/i);
  assert.doesNotMatch(html, /image/i);
});

test('an unknown infra id returns null', () => {
  assert.equal(resolve('infra:rabbitmq-2:label'), null);
});

test('an unknown infra field returns null', () => {
  assert.equal(resolve('infra:rabbitmq:colour'), null);
});
