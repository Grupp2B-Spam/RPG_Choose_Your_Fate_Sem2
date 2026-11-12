import http from 'node:http';
import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';

const publicRoot = new URL('./public/', import.meta.url);
const staticFiles = new Map([
  ['/', ['index.html', 'text/html; charset=utf-8']],
  ['/index.html', ['index.html', 'text/html; charset=utf-8']],
  ['/styles.css', ['styles.css', 'text/css; charset=utf-8']],
  ['/app.js', ['app.js', 'text/javascript; charset=utf-8']],
]);

const defaults = {
  host: process.env.DASHBOARD_HOST || '127.0.0.1',
  port: Number(process.env.PORT || 3000),
  managerUrl: process.env.SCENE_MANAGER_URL || 'http://127.0.0.1:8080',
  managerPublicUrl: process.env.SCENE_MANAGER_PUBLIC_URL || 'http://127.0.0.1:8080',
  rabbitUrl: process.env.RABBIT_HTTP_URL || 'http://127.0.0.1:15672',
  rabbitPublicUrl: process.env.RABBIT_PUBLIC_URL || 'http://127.0.0.1:15672',
  rabbitUser: process.env.RABBIT_USER || 'dev',
  rabbitPass: process.env.RABBIT_PASS || 'dev-local-only',
  amqpHostAddress: process.env.AMQP_HOST_ADDRESS || '127.0.0.1:5672',
};

const queueDefinitions = [
  { name: 'scene-indexer.scene-changed', role: 'Scene update queue', event: 'scene.changed' },
  { name: 'scene-indexer.dead', role: 'Failed scene events', event: 'scene.changed.failed' },
  { name: 'demo.storypath.choice-made', role: 'Mock StoryPathManager', event: 'choice.made' },
  { name: 'demo.character.choice-made', role: 'Mock CharacterManager', event: 'choice.made' },
];

const demoScenes = [
  {
    sceneId: 'demo-start', version: 1, operation: 'UPSERT',
    title: 'The Crossroads', text: 'The road splits beneath an ancient oak.',
    choices: [
      { id: 'left', text: 'Take the road to the mill', nextSceneId: 'demo-mill', effects: { courage: 1 } },
      { id: 'right', text: 'Walk toward the village', nextSceneId: 'demo-village', effects: {} },
    ],
  },
  { sceneId: 'demo-mill', version: 1, operation: 'UPSERT', title: 'Old Mill', text: 'A waterwheel turns slowly.', choices: [] },
  { sceneId: 'demo-village', version: 1, operation: 'UPSERT', title: 'Village', text: 'Lights flicker along a cobbled street.', choices: [] },
];

function json(res, status, value) {
  res.writeHead(status, {
    'content-type': 'application/json; charset=utf-8',
    'cache-control': 'no-store',
    'x-content-type-options': 'nosniff',
  });
  res.end(JSON.stringify(value));
}

function headerAuth(config) {
  return `Basic ${Buffer.from(`${config.rabbitUser}:${config.rabbitPass}`).toString('base64')}`;
}

function makeUpstreamError(name, error) {
  return { error: `${name} is unavailable`, detail: error.message };
}

async function requestJson(url, options = {}, fetcher = fetch) {
  const response = await fetcher(url, { signal: AbortSignal.timeout(5000), ...options });
  const text = await response.text();
  let body;
  try { body = text ? JSON.parse(text) : null; }
  catch { body = { message: text.slice(0, 1500) }; }
  return { status: response.status, ok: response.ok, body };
}

async function readJson(req, maxBytes = 65536) {
  let size = 0;
  const chunks = [];
  for await (const chunk of req) {
    size += chunk.length;
    if (size > maxBytes) {
      const error = new Error('JSON body exceeds 64 KB');
      error.status = 413;
      throw error;
    }
    chunks.push(chunk);
  }
  try { return JSON.parse(Buffer.concat(chunks).toString('utf8')); }
  catch {
    const error = new Error('Request body must contain valid JSON');
    error.status = 400;
    throw error;
  }
}

function validateSceneEvent(event) {
  if (!event || typeof event !== 'object' || Array.isArray(event)) return 'Event must be a JSON object';
  if (typeof event.sceneId !== 'string' || !event.sceneId.trim()) return 'sceneId is required';
  if (!Number.isSafeInteger(event.version) || event.version < 1) return 'version must be a positive integer';
  if (!['UPSERT', 'DELETE'].includes(event.operation)) return 'operation must be UPSERT or DELETE';
  if (event.operation === 'UPSERT') {
    if (typeof event.title !== 'string' || typeof event.text !== 'string' || !Array.isArray(event.choices)) {
      return 'UPSERT requires title, text and choices[]';
    }
    for (const choice of event.choices) {
      if (!choice || typeof choice.id !== 'string' || !choice.id.trim() || typeof choice.text !== 'string') {
        return 'Every choice requires id and text';
      }
    }
  }
  return null;
}

async function publishScene(event, config, fetcher) {
  const url = new URL('/api/exchanges/%2F/game.events/publish', config.rabbitUrl);
  const published = await requestJson(url, {
    method: 'POST',
    headers: {
      authorization: headerAuth(config),
      'content-type': 'application/json',
    },
    body: JSON.stringify({
      properties: { delivery_mode: 2, content_type: 'application/json' },
      routing_key: 'scene.changed',
      payload: JSON.stringify(event),
      payload_encoding: 'string',
    }),
  }, fetcher);
  if (!published.ok) {
    const error = new Error(`RabbitMQ management API returned HTTP ${published.status}`);
    error.status = 502;
    throw error;
  }
  if (published.body?.routed !== true) {
    const error = new Error('Event was not routed to a queue. Start SceneIndexer first.');
    error.status = 409;
    throw error;
  }
  return { sceneId: event.sceneId, version: event.version, routed: true };
}

/** Create the standalone dashboard server. Does not start it, so tests can use mock upstreams. */
export function createDashboardServer(overrides = {}) {
  const config = { ...defaults, ...overrides };
  const fetcher = overrides.fetcher || fetch;
  const managerBase = new URL(config.managerUrl);
  const rabbitBase = new URL(config.rabbitUrl);

  return http.createServer(async (req, res) => {
    const url = new URL(req.url || '/', 'http://dashboard.local');
    const path = url.pathname;
    try {
      if (req.method === 'GET' && staticFiles.has(path)) {
        const [file, contentType] = staticFiles.get(path);
        const content = await readFile(new URL(file, publicRoot));
        res.writeHead(200, {
          'content-type': contentType,
          'cache-control': 'no-store',
          'content-security-policy': "default-src 'self'; connect-src 'self'; img-src 'self' data:; style-src 'self'; script-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'",
          'x-content-type-options': 'nosniff',
          'x-frame-options': 'DENY',
        });
        res.end(content);
        return;
      }

      if (req.method === 'GET' && path === '/api/config') {
        return json(res, 200, {
          sceneManager: new URL(config.managerPublicUrl).origin,
          brokerManagement: new URL(config.rabbitPublicUrl).origin,
          brokerAmqpHost: config.amqpHostAddress,
          brokerAmqpDocker: 'rabbitmq:5672',
          exchange: 'game.events',
          updateRoutingKey: 'scene.changed',
          updateQueue: 'scene-indexer.scene-changed',
          mockPublisher: 'This dashboard (server-side via RabbitMQ Management API)',
          demoId: 'demo-start',
        });
      }

      if (req.method === 'GET' && path === '/api/status') {
        const [manager, broker] = await Promise.allSettled([
          requestJson(new URL('/scenes/demo-start', managerBase), {}, fetcher),
          requestJson(new URL('/api/overview', rabbitBase), { headers: { authorization: headerAuth(config) } }, fetcher),
        ]);
        return json(res, 200, {
          manager: manager.status === 'fulfilled'
            ? { online: true, status: manager.value.status }
            : { online: false, message: manager.reason.message },
          broker: broker.status === 'fulfilled'
            ? { online: broker.value.ok, status: broker.value.status, version: broker.value.body?.rabbitmq_version || null }
            : { online: false, message: broker.reason.message },
          checkedAt: new Date().toISOString(),
        });
      }

      if (req.method === 'GET' && path === '/api/queues') {
        const results = await Promise.all(queueDefinitions.map(async (queue) => {
          const endpoint = `/api/queues/%2F/${encodeURIComponent(queue.name)}`;
          try {
            const result = await requestJson(new URL(endpoint, rabbitBase), {
              headers: { authorization: headerAuth(config) },
            }, fetcher);
            if (result.status === 404) return { ...queue, exists: false, ready: 0, unacked: 0, consumers: 0 };
            if (!result.ok) return { ...queue, exists: false, error: `HTTP ${result.status}` };
            return {
              ...queue, exists: true, ready: result.body.messages_ready ?? 0,
              unacked: result.body.messages_unacknowledged ?? 0, consumers: result.body.consumers ?? 0,
            };
          } catch (error) {
            return { ...queue, exists: false, error: error.message };
          }
        }));
        return json(res, 200, { queues: results });
      }

      if (path.startsWith('/api/scenes/')) {
        const rest = path.slice('/api/scenes'.length);
        const isReadScene = /^\/[^/]+$/.test(rest);
        const isNext = /^\/[^/]+\/nextscenes$/.test(rest);
        const isChoice = /^\/[^/]+\/choices\/[^/]+$/.test(rest);
        if (!((req.method === 'GET' && (isReadScene || isNext)) || (req.method === 'POST' && isChoice))) {
          return json(res, 405, { error: 'Unsupported SceneManager method or path' });
        }
        const upstream = new URL(`/scenes${rest}`, managerBase);
        const options = { method: req.method };
        if (req.method === 'POST') {
          const input = await readJson(req);
          options.headers = { 'content-type': 'application/json' };
          options.body = JSON.stringify(input);
        }
        try {
          const response = await requestJson(upstream, options, fetcher);
          return json(res, response.status, response.body);
        } catch (error) {
          return json(res, 502, makeUpstreamError('SceneManager', error));
        }
      }

      if (req.method === 'POST' && path === '/api/mock/publish') {
        const event = await readJson(req);
        const validationError = validateSceneEvent(event);
        if (validationError) return json(res, 400, { error: validationError });
        try { return json(res, 202, await publishScene(event, config, fetcher)); }
        catch (error) { return json(res, error.status || 502, makeUpstreamError('SceneUpdateQueue', error)); }
      }

      if (req.method === 'POST' && path === '/api/mock/seed') {
        const results = [];
        for (const scene of demoScenes) {
          try { results.push(await publishScene(scene, config, fetcher)); }
          catch (error) {
            return json(res, error.status || 502, { error: 'Seeding incomplete', detail: error.message, published: results });
          }
        }
        return json(res, 202, { message: 'Three demo scene events published', published: results });
      }

      return json(res, 404, { error: 'Not found' });
    } catch (error) {
      return json(res, error.status || 500, { error: error.message });
    }
  });
}

if (process.argv[1] && fileURLToPath(import.meta.url) === fileURLToPath(new URL(`file://${process.argv[1]}`))) {
  const server = createDashboardServer();
  server.listen(defaults.port, defaults.host, () => {
    console.log(`Scene dashboard: http://${defaults.host}:${defaults.port}`);
    console.log(`SceneManager upstream: ${defaults.managerUrl}`);
    console.log(`RabbitMQ management upstream: ${defaults.rabbitUrl}`);
  });
}
