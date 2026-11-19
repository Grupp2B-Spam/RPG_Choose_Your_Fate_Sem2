'use strict';

const { test } = require('node:test');
const assert = require('node:assert/strict');

const h = require('./helpers.js');

const TAB = 'env-compose';

test('tab 4 shows RabbitMQ 4 under its project container name', () => {
  const text = h.textOf(TAB);
  assert.match(text, /4-management-alpine/);
  assert.match(text, /choose-your-fate-rabbitmq/);
});

test('tab 4 draws the consolidated broker, not the stale one, as the live service', () => {
  const rabbit = h.cellById(TAB, 'dc-rabbit');
  assert.doesNotMatch(h.unescape(rabbit.value), /bar-scan-rabbitmq|3\.13/);
});

test('tab 4 says plainly that main still has the stale broker', () => {
  assert.match(h.textOf(TAB), /bar-scan-rabbitmq/);
});

test('tab 4 shows the include structure', () => {
  const text = h.textOf(TAB);
  assert.match(text, /include:/);
  assert.match(text, /microservices\/recapManager\/docker-compose-recap-manager\.yml/);
  assert.match(text, /microservices\/storycontent-manager\/compose\.yaml/);
});

test('tab 4 shows the host ports for both vaults', () => {
  const text = h.textOf(TAB);
  assert.match(text, /3307/);
  assert.match(text, /3309/);
});

test('tab 4 is labelled as current state', () => {
  assert.match(h.textOf(TAB), /Current state/i);
});

test('tab 4 explains the prod profile', () => {
  assert.match(h.textOf(TAB), /profiles: \[prod\]/);
});

test('tab 4 names the services that have no compose file yet', () => {
  const text = h.textOf(TAB);
  for (const name of ['AccountManager', 'SceneManager', 'CharacterManager', 'StoryPathManager', 'TextToSpeechManager', 'MinIO']) {
    assert.match(text, new RegExp(name), `${name} is not listed as missing`);
  }
});

test('every container on tab 4 sits inside the compose project', () => {
  const containers = h.cellsOf(TAB).filter((c) => /^dc-(root|rabbit|frontend|inc-)/.test(c.id));
  for (const c of containers) {
    assert.equal(c.parent, 'dc-project', `${c.id} is outside the project box`);
  }
});

test('each included file is drawn as its own group with its service and database', () => {
  for (const group of ['dc-inc-recap', 'dc-inc-storycontent']) {
    const children = h.cellsOf(TAB).filter((c) => c.parent === group && c.vertex);
    assert.equal(children.length, 2, `${group} should hold a service and its database`);
  }
});
