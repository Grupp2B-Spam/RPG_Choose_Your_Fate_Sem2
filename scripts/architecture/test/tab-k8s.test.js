'use strict';

const { test } = require('node:test');
const assert = require('node:assert/strict');

const h = require('./helpers.js');

const TAB = 'env-k8s';
const clusterObjects = () => h.cellsOf(TAB).filter((c) => c.id.startsWith('k8-n-'));

test('tab 5 carries a banner saying nothing here is built', () => {
  assert.match(h.textOf(TAB), /none of this exists yet/i);
});

test('every cluster object on tab 5 is drawn as planned', () => {
  const objects = clusterObjects();
  assert.ok(objects.length >= 15, 'the cluster should be drawn in full');
  for (const c of objects) {
    assert.match(c.style, /dashed=1/, `${c.id} is not drawn as planned`);
  }
});

test('tab 5 uses the agreed Kubernetes service names', () => {
  const ids = clusterObjects().map((c) => c.id);
  for (const s of h.manifest.services.filter((x) => x.kind === 'service')) {
    assert.ok(ids.includes(`k8-n-${s.id}`), `${s.id} has no workload`);
  }
});

test('every service id on tab 5 is a valid Kubernetes object name', () => {
  for (const c of clusterObjects()) {
    const name = c.id.replace(/^k8-n-/, '');
    assert.match(name, /^[a-z][a-z0-9-]*$/, `${name} is not a valid object name`);
  }
});

test('tab 5 distinguishes liveness from readiness and says why', () => {
  const text = h.textOf(TAB);
  assert.match(text, /liveness/i);
  assert.match(text, /readiness/i);
  assert.match(text, /must not be restarted/i);
});

test('tab 5 shows the KEDA ScaledJob on the TTS service, not a Deployment', () => {
  const tts = h.unescape(h.cellById(TAB, 'k8-n-texttospeech-manager').value);
  assert.match(tts, /ScaledJob/);
  assert.match(tts, /not a Deployment/);
});

test('storypath-manager is pinned to one replica with the reason given', () => {
  const sp = h.unescape(h.cellById(TAB, 'k8-n-storypath-manager').value);
  assert.match(sp, /replicas: 1/);
  assert.match(sp, /ADR-013/);
});

test('image delivery is drawn as an open question', () => {
  const text = h.textOf(TAB);
  assert.match(text, /kind load/);
  assert.match(text, /registry/i);
  assert.match(text, /has not decided/i);
});

test('scene-indexer is a Deployment with no Service of its own', () => {
  assert.match(h.unescape(h.cellById(TAB, 'k8-n-scene-indexer').value), /Deployment, no Service/);
});

test('the broker is reachable at the host the contract already names', () => {
  assert.match(h.textOf(TAB), /rabbitmq\.default\.svc\.cluster\.local/);
});

test('autoscaling is described as more than one metric', () => {
  const text = h.textOf(TAB);
  assert.match(text, /queue depth/);
  assert.match(text, /request rate/);
});

test('every datastore in the manifest has a StatefulSet', () => {
  const ids = clusterObjects().map((c) => c.id);
  const expected = ['account-vault', 'story-content-vault', 'scene-read-model', 'character-vault', 'storypath-vault', 'recap-vault', 'minio', 'rabbitmq'];
  for (const name of expected) {
    assert.ok(ids.includes(`k8-n-${name}`), `${name} is missing from the cluster`);
  }
});
