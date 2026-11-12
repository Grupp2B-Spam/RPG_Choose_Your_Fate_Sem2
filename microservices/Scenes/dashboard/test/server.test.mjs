import { test, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { createDashboardServer } from '../server.mjs';

const calls = [];
function asJson(body, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { 'content-type': 'application/json' } });
}

async function fakeFetch(input, init = {}) {
  const url = new URL(input);
  calls.push({ url: url.href, method: init.method || 'GET', body: init.body, headers: init.headers });
  if (url.hostname === 'manager.test') {
    if (url.pathname === '/scenes/demo-start/nextscenes') return asJson([{ id: 'demo-mill', title: 'Old Mill' }]);
    if (url.pathname === '/scenes/demo-start/choices/left' && init.method === 'POST') {
      return asJson({ eventId: 'abc-123', nextSceneId: 'demo-mill' }, 202);
    }
    if (url.pathname === '/scenes/demo-start') return asJson({ id: 'demo-start', version: 1, title: 'The Crossroads', text: 'Old road', choices: [] });
    if (url.pathname === '/scenes/not-found') return asJson({ title: 'Not Found', status: 404 }, 404);
    return asJson({ error: 'Unknown manager route' }, 404);
  }
  if (url.hostname === 'rabbit.test') {
    if (url.pathname === '/api/overview') return asJson({ rabbitmq_version: '4.0.0' });
    if (url.pathname === '/api/exchanges/%2F/game.events/publish' && init.method === 'POST') {
      return asJson({ routed: true });
    }
    if (url.pathname.startsWith('/api/queues/%2F/')) {
      if (url.pathname.endsWith('/scene-indexer.dead')) return asJson({ error: 'Not found' }, 404);
      return asJson({ messages_ready: 3, messages_unacknowledged: 1, consumers: 2 });
    }
    return asJson({ error: 'Unknown RabbitMQ route' }, 404);
  }
  throw new Error(`Unexpected upstream: ${url.href}`);
}

let server, base;
before(async () => {
  server = createDashboardServer({
    fetcher: fakeFetch,
    managerUrl: 'http://manager.test:8080', rabbitUrl: 'http://rabbit.test:15672',
    rabbitUser: 'server-only-user', rabbitPass: 'server-only-password',
  });
  await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve));
  base = `http://127.0.0.1:${server.address().port}`;
});
after(async () => { await new Promise((resolve) => server.close(resolve)); });

async function call(path, options = {}) {
  const response = await fetch(base + path, options);
  const text = await response.text();
  return { status: response.status, body: JSON.parse(text), headers: response.headers };
}

for (const file of ['/', '/app.js', '/styles.css']) {
  test(`Serves frontend asset ${file}`, async () => {
    const response = await fetch(base + file);
    assert.equal(response.status, 200);
    assert.ok((await response.text()).length > 100);
    assert.equal(response.headers.get('x-content-type-options'), 'nosniff');
  });
}

test('Shows safe configuration without leaking RabbitMQ secrets', async () => {
  const { status, body } = await call('/api/config');
  assert.equal(status, 200);
  assert.equal(body.updateQueue, 'scene-indexer.scene-changed');
  assert.equal(body.brokerAmqpDocker, 'rabbitmq:5672');
  assert.ok(!JSON.stringify(body).includes('server-only-password'));
  assert.ok(!JSON.stringify(body).includes('server-only-user'));
});

test('Indicates healthy SceneManager and RabbitMQ services', async () => {
  const { body } = await call('/api/status');
  assert.equal(body.manager.online, true);
  assert.equal(body.broker.online, true);
  assert.equal(body.broker.version, '4.0.0');
});

test('Reports queue metrics and handles uncreated queues', async () => {
  const { status, body } = await call('/api/queues');
  assert.equal(status, 200);
  assert.equal(body.queues.length, 4);
  assert.equal(body.queues[0].name, 'scene-indexer.scene-changed');
  assert.equal(body.queues[0].ready, 3);
  assert.equal(body.queues[0].consumers, 2);
  assert.equal(body.queues[1].exists, false);
});

test('Proxies GET /scenes/{id} JSON unchanged', async () => {
  const { status, body } = await call('/api/scenes/demo-start');
  assert.equal(status, 200);
  assert.equal(body.id, 'demo-start');
});

test('Proxies GET /scenes/{id}/nextscenes JSON unchanged', async () => {
  const { status, body } = await call('/api/scenes/demo-start/nextscenes');
  assert.equal(status, 200);
  assert.deepEqual(body, [{ id: 'demo-mill', title: 'Old Mill' }]);
});

test('Proxies POST /scenes/{id}/choices/{choiceId} and JSON body', async () => {
  const { status, body } = await call('/api/scenes/demo-start/choices/left', {
    method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify({ characterId: 'demo-character' }),
  });
  assert.equal(status, 202);
  assert.equal(body.nextSceneId, 'demo-mill');
  const sent = calls.find((c) => c.url.includes('/scenes/demo-start/choices/left'));
  assert.equal(JSON.parse(sent.body).characterId, 'demo-character');
});

test('Preserves SceneManager 404 responses', async () => {
  const { status, body } = await call('/api/scenes/not-found');
  assert.equal(status, 404);
  assert.equal(body.status, 404);
});

test('Rejects malformed request bodies and unsupported routes', async () => {
  assert.equal((await call('/api/scenes/demo-start/choices/left', { method: 'POST', body: '{bad' })).status, 400);
  assert.equal((await call('/api/scenes/demo-start', { method: 'DELETE' })).status, 405);
});

test('Publishes a valid scene.changed event with server-side RabbitMQ auth', async () => {
  const scene = { sceneId: 'demo-new', version: 3, operation: 'UPSERT', title: 'New', text: 'Hello', choices: [] };
  const { status, body } = await call('/api/mock/publish', { method: 'POST', body: JSON.stringify(scene) });
  assert.equal(status, 202);
  assert.equal(body.sceneId, 'demo-new');
  const publish = calls.filter((c) => c.url.includes('/api/exchanges/%2F/game.events/publish')).at(-1);
  const payload = JSON.parse(publish.body);
  assert.equal(payload.routing_key, 'scene.changed');
  assert.equal(payload.properties.delivery_mode, 2);
  assert.equal(JSON.parse(payload.payload).sceneId, 'demo-new');
  assert.equal(publish.headers.authorization, 'Basic ' + Buffer.from('server-only-user:server-only-password').toString('base64'));
});

test('Rejects invalid events without sending them to RabbitMQ', async () => {
  const before = calls.length;
  const { status, body } = await call('/api/mock/publish', { method: 'POST', body: JSON.stringify({ sceneId: 'bad', version: 0, operation: 'UPSERT' }) });
  assert.equal(status, 400);
  assert.match(body.error, /version/);
  assert.equal(calls.length, before);
});

test('Seeds three demo-prefixed scenes with version 1', async () => {
  const before = calls.length;
  const { status, body } = await call('/api/mock/seed', { method: 'POST' });
  assert.equal(status, 202);
  assert.equal(body.published.length, 3);
  assert.deepEqual(body.published.map((scene) => scene.sceneId), ['demo-start', 'demo-mill', 'demo-village']);
  const events = calls.slice(before).map((c) => JSON.parse(JSON.parse(c.body).payload));
  assert.equal(events.every((event) => event.version === 1), true);
  assert.equal(events[0].choices.length, 2);
});

test('Returns 502 when a dependency is unreachable', async () => {
  const broken = createDashboardServer({
    managerUrl: 'http://manager.test', rabbitUrl: 'http://rabbit.test',
    fetcher: () => { throw new Error('connection refused'); },
  });
  await new Promise((resolve) => broken.listen(0, '127.0.0.1', resolve));
  const url = `http://127.0.0.1:${broken.address().port}`;
  try {
    const r = await fetch(url + '/api/scenes/demo-start');
    assert.equal(r.status, 502);
    assert.match((await r.json()).error, /SceneManager/);
    const status = await (await fetch(url + '/api/status')).json();
    assert.equal(status.manager.online, false);
    assert.equal(status.broker.online, false);
  } finally { await new Promise((resolve) => broken.close(resolve)); }
});
