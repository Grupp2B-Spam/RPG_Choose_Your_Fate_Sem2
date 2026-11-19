'use strict';

const { test } = require('node:test');
const assert = require('node:assert/strict');

const { validateManifest } = require('../lib/manifest.js');

// A manifest with exactly the fields the validator requires and nothing more.
// Each test mutates one thing, so a failure names the rule that broke.
function minimal() {
  return {
    meta: { version: 3, updated: '2026-11-18', broker: 'RabbitMQ 4 (management-alpine)' },
    services: [
      {
        id: 'recap-manager',
        kind: 'service',
        display: 'RecapManager',
        status: 'implemented',
        context: 'state',
        purpose: 'Generates per-character recaps with an LLM.',
        ports: { http: 8090 },
        publishes: [{ event: 'recap-generated', exchange: 'recap.events', state: 'planned' }],
        consumes: [],
      },
      {
        id: 'texttospeech-manager',
        kind: 'service',
        display: 'TextToSpeechManager',
        status: 'planned',
        context: 'media',
        purpose: 'Turns a recap into audio.',
        publishes: [],
        consumes: [{ event: 'recap-generated', queue: 'texttospeechmanager.recap', scalable: true }],
      },
    ],
    events: [
      {
        name: 'recap-generated',
        exchange: 'recap.events',
        publisher: 'recap-manager',
        consumers: ['texttospeech-manager'],
        queues: ['texttospeechmanager.recap'],
      },
    ],
  };
}

test('accepts a minimal valid manifest', () => {
  assert.deepEqual(validateManifest(minimal()), []);
});

test('rejects a service id that is not lowercase-hyphen', () => {
  const m = minimal();
  m.services[0].id = 'RecapManager';
  assert.match(validateManifest(m).join('\n'), /RecapManager.*lowercase-hyphen/);
});

test('rejects a duplicate service id', () => {
  const m = minimal();
  m.services.push({ ...m.services[0] });
  assert.match(validateManifest(m).join('\n'), /duplicate service id 'recap-manager'/);
});

test('rejects an unknown status', () => {
  const m = minimal();
  m.services[0].status = 'nearly';
  assert.match(validateManifest(m).join('\n'), /status 'nearly'/);
});

test('rejects an unknown kind', () => {
  const m = minimal();
  m.services[0].kind = 'thing';
  assert.match(validateManifest(m).join('\n'), /kind 'thing'/);
});

test('rejects an event naming a consumer that is not a service', () => {
  const m = minimal();
  m.events[0].consumers = ['ghost-manager'];
  assert.match(validateManifest(m).join('\n'), /'ghost-manager'/);
});

test('rejects an event whose publisher is not a service', () => {
  const m = minimal();
  m.events[0].publisher = 'ghost-manager';
  assert.match(validateManifest(m).join('\n'), /publisher 'ghost-manager'/);
});

test('rejects a consumes entry naming an event that does not exist', () => {
  const m = minimal();
  m.services[1].consumes[0].event = 'nothing.happened';
  assert.match(validateManifest(m).join('\n'), /nothing\.happened/);
});

test('accepts a service with no ports', () => {
  const m = minimal();
  delete m.services[0].ports;
  assert.deepEqual(validateManifest(m), []);
});

test('rejects a legacy node that declares a context', () => {
  const m = minimal();
  m.services.push({
    id: 'choose-your-fate-backend',
    kind: 'legacy',
    display: 'choose-your-fate-backend',
    status: 'implemented',
    context: 'state',
    purpose: 'The monolith.',
  });
  assert.match(validateManifest(m).join('\n'), /legacy.*context/);
});

test('reports a missing meta block', () => {
  const m = minimal();
  delete m.meta;
  assert.match(validateManifest(m).join('\n'), /meta/);
});
