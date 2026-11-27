import {
  existsSync,
  mkdirSync,
  readFileSync,
  renameSync,
  unlinkSync,
  writeFileSync,
} from 'node:fs';
import { readFile } from 'node:fs/promises';
import http from 'node:http';
import { dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const publicRoot = new URL('./public/', import.meta.url);
const staticFiles = new Map([
  ['/', ['index.html', 'text/html; charset=utf-8']],
  ['/index.html', ['index.html', 'text/html; charset=utf-8']],
  ['/styles.css', ['styles.css', 'text/css; charset=utf-8']],
  ['/app.js', ['app.js', 'text/javascript; charset=utf-8']],
  ['/favicon.ico', ['favicon.ico', 'image/x-icon']],
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
  amqpDockerAddress: process.env.AMQP_DOCKER_ADDRESS || 'rabbitmq:5672',
  mongoHostAddress: process.env.MONGO_HOST_ADDRESS || '127.0.0.1:27017',
  mongoDockerAddress: process.env.MONGO_DOCKER_ADDRESS || 'mongo:27017',
  exchange: process.env.GAME_EXCHANGE || 'game.events',
  updateQueue: process.env.UPDATE_QUEUE || 'scene-indexer.scene-changed',
  updateRoutingKey: process.env.UPDATE_ROUTING_KEY || 'scene.changed',
  deadQueue: process.env.DEAD_QUEUE || 'scene-indexer.dead',
  choiceRoutingKey: process.env.CHOICE_ROUTING_KEY || 'choice.made',
  choiceStoryPathQueue: process.env.CHOICE_STORYPATH_QUEUE || 'demo.storypath.choice-made',
  choiceCharacterQueue: process.env.CHOICE_CHARACTER_QUEUE || 'demo.character.choice-made',
};

// Queue names are logical RabbitMQ destinations, not separate IP addresses.
function queueDefinitions(config) {
  return [
    { name: config.updateQueue, role: 'SceneUpdateQueue / SceneIndexer', event: config.updateRoutingKey },
    { name: config.deadQueue, role: 'Indexer dead-letter queue', event: 'scene.changed.failed' },
    { name: config.choiceStoryPathQueue, role: 'ChoiceQueue / demo StoryPathManager', event: config.choiceRoutingKey },
    { name: config.choiceCharacterQueue, role: 'ChoiceQueue / demo CharacterManager', event: config.choiceRoutingKey },
  ];
}

// Only local, Docker DNS and private-network targets can be entered, because
// dashboard connections are made by the server using its broker credentials.
const editableKeys = [
  'managerUrl', 'managerPublicUrl', 'rabbitUrl', 'rabbitPublicUrl',
  'amqpHostAddress', 'amqpDockerAddress', 'mongoHostAddress', 'mongoDockerAddress',
  'exchange', 'updateQueue', 'updateRoutingKey', 'deadQueue',
  'choiceRoutingKey', 'choiceStoryPathQueue', 'choiceCharacterQueue',
];
const urlKeys = new Set(['managerUrl', 'managerPublicUrl', 'rabbitUrl', 'rabbitPublicUrl']);
const addressKeys = new Set(['amqpHostAddress', 'amqpDockerAddress', 'mongoHostAddress', 'mongoDockerAddress']);
const nameKeys = new Set(editableKeys.filter(key => !urlKeys.has(key) && !addressKeys.has(key)));

function localHost(hostname) {
  const host = hostname.toLowerCase();
  if (['localhost', 'host.docker.internal', 'rabbitmq', 'scene-manager', 'mongo'].includes(host)) return true;
  if (host === '::1' || host === '[::1]') return true;
  const p = host.split('.');
  if (p.length !== 4 || p.some(n => !/^\d{1,3}$/.test(n) || Number(n) > 255)) return false;
  const [a, b] = p.map(Number);
  return a === 127 || a === 10 || (a === 192 && b === 168) || (a === 172 && b >= 16 && b <= 31);
}

function validateSettings(input) {
  if (!input || typeof input !== 'object' || Array.isArray(input)) throw Object.assign(new Error('Settings must be a JSON object'), { status: 400 });
  const keys = Object.keys(input);
  if (!keys.length || keys.some(key => !editableKeys.includes(key))) throw Object.assign(new Error('Unknown or empty settings'), { status: 400 });
  const values = {};
  for (const key of keys) {
    const raw = input[key];
    if (typeof raw !== 'string' || raw.length > 250 || !raw.trim()) throw Object.assign(new Error(`${key} must be a non-empty string`), { status: 400 });
    const value = raw.trim();
    if (urlKeys.has(key)) {
      let parsed;
      try { parsed = new URL(value); } catch { /* handled below */ }
      if (!parsed || !['http:', 'https:'].includes(parsed.protocol) || !localHost(parsed.hostname)
          || parsed.username || parsed.password || parsed.pathname !== '/' || parsed.search || parsed.hash) {
        throw Object.assign(new Error(`${key} must be a local/private HTTP(S) origin with no credentials or path`), { status: 400 });
      }
      values[key] = parsed.origin;
    } else if (addressKeys.has(key)) {
      const m = /^([a-zA-Z0-9.\-]+|\[::1\]):(\d{1,5})$/.exec(value);
      if (!m || !localHost(m[1]) || Number(m[2]) < 1 || Number(m[2]) > 65535) {
        throw Object.assign(new Error(`${key} must be a local/private host:port`), { status: 400 });
      }
      values[key] = value;
    } else if (nameKeys.has(key)) {
      if (!/^[a-zA-Z0-9_.-]{1,120}$/.test(value)) throw Object.assign(new Error(`${key} must be a valid RabbitMQ name`), { status: 400 });
      values[key] = value;
    }
  }
  return values;
}

function publicSettings(config) {
  return {
    // Keep the previous public keys for older dashboard clients.
    sceneManager: new URL(config.managerPublicUrl).origin,
    brokerManagement: new URL(config.rabbitPublicUrl).origin,
    brokerAmqpHost: config.amqpHostAddress,
    brokerAmqpDocker: config.amqpDockerAddress,
    mockPublisher: 'Dashboard server -> RabbitMQ management API',
    demoId: 'demo-start',
    ...Object.fromEntries(editableKeys.map(key => [key, config[key]])),
  };
}

const demoScenes = [
  {
    sceneId: 'demo-start', version: 1, operation: 'UPSERT', chapterId: 'demo-chapter',
    status: 'PUBLISHED', name: 'The Crossroads',
    description: 'The road splits beneath an ancient oak.', npcIds: [], questIds: [],
    choices: [
      { id: 'left', text: 'Take the road to the mill', nextSceneId: 'demo-mill', effects: { courage: 1 } },
      { id: 'right', text: 'Walk toward the village', nextSceneId: 'demo-village', effects: {} },
    ],
  },
  {
    sceneId: 'demo-mill', version: 1, operation: 'UPSERT', chapterId: 'demo-chapter',
    status: 'PUBLISHED', name: 'Old Mill', description: 'A waterwheel turns slowly.',
    npcIds: [], questIds: [], choices: [],
  },
  {
    sceneId: 'demo-village', version: 1, operation: 'UPSERT', chapterId: 'demo-chapter',
    status: 'PUBLISHED', name: 'Village', description: 'Lights flicker along a cobbled street.',
    npcIds: [], questIds: [], choices: [],
  },
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
    const name = event.name ?? event.title;
    const description = event.description ?? event.text;
    if (typeof name !== 'string' || typeof description !== 'string' || !Array.isArray(event.choices)) {
      return 'UPSERT requires name, description and choices[]';
    }
    for (const choice of event.choices) {
      if (!choice || typeof choice.id !== 'string' || !choice.id.trim() || typeof choice.text !== 'string') {
        return 'Every choice requires id and text';
      }
    }
    if (event.chapterId !== undefined && typeof event.chapterId !== 'string') return 'chapterId must be a string';
    if (event.status !== undefined && typeof event.status !== 'string') return 'status must be a string';
    for (const key of ['npcIds', 'questIds']) {
      if (event[key] !== undefined && (!Array.isArray(event[key]) || event[key].some(id => typeof id !== 'string'))) {
        return `${key} must be an array of strings`;
      }
    }
  }
  return null;
}

async function publishScene(event, config, fetcher) {
  const url = new URL(`/api/exchanges/%2F/${encodeURIComponent(config.exchange)}/publish`, config.rabbitUrl);
  const published = await requestJson(url, {
    method: 'POST',
    headers: {
      authorization: headerAuth(config),
      'content-type': 'application/json',
    },
    body: JSON.stringify({
      properties: { delivery_mode: 2, content_type: 'application/json' },
      routing_key: config.updateRoutingKey,
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
  const configFile = overrides.configFile === undefined
    ? (process.env.DASHBOARD_CONFIG_FILE || fileURLToPath(new URL('./config/connections.json', import.meta.url)))
    : overrides.configFile;
  const baseConfig = { ...defaults, ...overrides };
  const fetcher = overrides.fetcher || fetch;
  let saved = {};
  if (configFile && existsSync(configFile)) {
    try { saved = validateSettings(JSON.parse(readFileSync(configFile, 'utf8'))); }
    catch (error) { throw new Error(`Invalid dashboard config file: ${error.message}`); }
  }
  let config = { ...baseConfig, ...saved };
  function persist(next) {
    if (!configFile) return;
    mkdirSync(dirname(configFile), { recursive: true });
    const temporary = `${configFile}.tmp`;
    writeFileSync(temporary, JSON.stringify(next, null, 2) + '\n', { mode: 0o600 });
    renameSync(temporary, configFile);
  }

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
        return json(res, 200, publicSettings(config));
      }

      if (req.method === 'PUT' && path === '/api/config') {
        if (req.headers['content-type']?.split(';')[0].trim() !== 'application/json') {
          return json(res, 415, { error: 'Use application/json to save settings' });
        }
        // Avoid cross-origin pages changing the loopback dashboard's configuration.
        if (req.headers.origin && new URL(req.headers.origin).host !== req.headers.host) {
          return json(res, 403, { error: 'Cross-origin settings changes are not allowed' });
        }
        const changes = validateSettings(await readJson(req));
        const updated = { ...saved, ...changes };
        persist(updated);
        saved = updated;
        config = { ...baseConfig, ...saved };
        return json(res, 200, { message: 'Dashboard settings saved', config: publicSettings(config) });
      }

      if (req.method === 'POST' && path === '/api/config/reset') {
        if (req.headers.origin && new URL(req.headers.origin).host !== req.headers.host) {
          return json(res, 403, { error: 'Cross-origin reset is not allowed' });
        }
        if (configFile && existsSync(configFile)) unlinkSync(configFile);
        saved = {};
        config = { ...baseConfig };
        return json(res, 200, { message: 'Dashboard settings reset', config: publicSettings(config) });
      }

      if (req.method === 'GET' && path === '/api/status') {
        const [manager, broker] = await Promise.allSettled([
          requestJson(new URL('/scenes/demo-start', config.managerUrl), {}, fetcher),
          requestJson(new URL('/api/overview', config.rabbitUrl), { headers: { authorization: headerAuth(config) } }, fetcher),
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
        const results = await Promise.all(queueDefinitions(config).map(async (queue) => {
          const endpoint = `/api/queues/%2F/${encodeURIComponent(queue.name)}`;
          try {
            const result = await requestJson(new URL(endpoint, config.rabbitUrl), {
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
        const upstream = new URL(`/scenes${rest}`, config.managerUrl);
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
    console.log('Editable connection settings: /api/config (saved locally in dashboard/config/)');
  });
}
