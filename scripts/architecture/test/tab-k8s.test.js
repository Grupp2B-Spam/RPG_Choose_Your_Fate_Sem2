'use strict';

const { test } = require('node:test');
const assert = require('node:assert/strict');

const h = require('./helpers.js');

const TAB = 'env-k8s';
const clusterObjects = () => h.cellsOf(TAB).filter((c) => c.id.startsWith('k8-n-'));

test('tab 5 carries a banner saying nothing here is built', () => {
  assert.match(h.textOf(TAB), /nothing below exists yet/i);
});

test('tab 5 is labelled as the destination rather than the present', () => {
  assert.match(h.textOf(TAB), /the destination, not today/i);
});

test('the clients sit outside the cluster boundary', () => {
  for (const id of ['k8-client-player', 'k8-client-author']) {
    assert.equal(h.cellById(TAB, id).parent, '1', `${id} should be outside the cluster`);
  }
});

test('every workload sits inside the cluster boundary', () => {
  const inside = ['k8-n-ingress', 'k8-n-recap-manager', 'k8-n-rabbitmq', 'k8-n-keda'];
  for (const id of inside) {
    assert.equal(h.cellById(TAB, id).parent, 'k8-cluster', `${id} should be in the cluster`);
  }
});

test('the queues are drawn inside the broker', () => {
  const queues = h.cellsOf(TAB).filter((c) => /^k8-q\d$/.test(c.id));
  assert.equal(queues.length, 5);
  for (const q of queues) assert.equal(q.parent, 'k8-n-rabbitmq');
});

test('the proposed observability stack is drawn and marked as undecided', () => {
  for (const id of ['k8-n-promtail', 'k8-n-loki', 'k8-n-prometheus', 'k8-n-grafana', 'k8-n-rabbitmq-exporter']) {
    assert.ok(h.cellById(TAB, id), `${id} is missing`);
  }
  assert.match(h.textUnder(TAB, 'k8-obs'), /not chosen|not decided/i);
  assert.match(h.textUnder(TAB, 'k8-obs'), /OPEN-DESIGN-ISSUES 2\.5/);
});

test('every arrow on tab 5 says what the relationship is', () => {
  for (const e of h.edgesOf(TAB)) {
    assert.ok(h.unescape(e.value).trim().length > 0, `${e.id} has no label`);
  }
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
  const tts = h.textUnder(TAB, 'k8-n-texttospeech-manager');
  assert.match(tts, /ScaledJob/);
  assert.match(tts, /not a Deployment/);
});

test('storypath-manager is pinned to one replica with the reason given', () => {
  const sp = h.textUnder(TAB, 'k8-n-storypath-manager');
  assert.match(sp, /replicas: 1/);
  assert.match(sp, /sequence number has an owner/);
});

test('image delivery is drawn as an open question', () => {
  const text = h.textOf(TAB);
  assert.match(text, /kind load/);
  assert.match(text, /registry/i);
  assert.match(text, /has not decided/i);
});

test('scene-indexer is a Deployment with no Service of its own', () => {
  assert.match(h.textUnder(TAB, 'k8-n-scene-indexer'), /Deployment, no Service/);
});

test('the broker is reachable at the host the contract already names', () => {
  assert.match(h.textOf(TAB), /rabbitmq\.default\.svc\.cluster\.local/);
});

test('autoscaling is described as more than one metric', () => {
  const text = h.textOf(TAB);
  assert.match(text, /queue depth/);
  assert.match(text, /request rate/);
});

test('every datastore in the manifest is named as a StatefulSet', () => {
  const text = h.textUnder(TAB, 'k8-n-statefulsets');
  for (const name of ['account-vault', 'story-content-vault', 'scene-read-model', 'character-vault', 'storypath-vault', 'recap-vault', 'minio']) {
    assert.match(text, new RegExp(name), name + ' is missing from the StatefulSets box');
  }
});
