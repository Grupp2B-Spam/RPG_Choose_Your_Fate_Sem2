/* Choose Your Fate - Scene Control. Browser code has no RabbitMQ credentials. */
const byId = (id) => document.getElementById(id);
const defaultEvent = {
  eventId: '00000000-0000-4000-8000-000000000101',
  eventType: 'scene-content.updated',
  occurredAt: '2026-11-27T12:00:00Z',
  schemaVersion: 1,
  payload: {
    sceneId: 'demo-start', sceneVersion: 1, chapterId: 'demo-chapter',
    status: 'PUBLISHED', name: 'The Crossroads',
    description: 'The road splits beneath an ancient oak.', npcIds: [], questIds: [],
    choices: [
      { choiceId: 'left', label: 'Take the road to the mill', beat: 'You take the road to the mill.',
        destinationSceneId: 'demo-mill', weight: 1, grantedItemIds: [] },
      { choiceId: 'right', label: 'Walk toward the village', beat: 'You walk toward the village.',
        destinationSceneId: 'demo-village', weight: 1, grantedItemIds: [] },
    ],
  },
};
const deleteEvent = {
  eventId: '00000000-0000-4000-8000-000000000102',
  eventType: 'scene-content.updated',
  occurredAt: '2026-11-27T12:01:00Z',
  schemaVersion: 1,
  payload: { sceneId: 'demo-start', sceneVersion: 2, status: 'RETIRED', chapterId: 'demo-chapter' },
};
const contractDefinitions = {
  scene: {
    kind: 'MONGO DOCUMENT', name: 'SceneDocument',
    description: 'The read-optimized scene projection. SceneIndexer writes it to MongoDB; SceneManager retrieves it.',
    note: 'A deleted scene is retained as a tombstone (deleted: true) and is hidden from the REST API.',
    example: {
      id: 'demo-start', sceneId: 'demo-start', version: 1, chapterId: 'demo-chapter',
      status: 'PUBLISHED', name: 'The Crossroads',
      description: 'The road splits beneath an ancient oak.',
      choices: [{ id: 'left', text: 'Take the road to the mill', nextSceneId: 'demo-mill', effects: { courage: 1 } }],
      npcIds: [], questIds: [],
      deleted: false,
    },
  },
  choice: {
    kind: 'NESTED VALUE', title: 'Choice',
    description: 'A selectable option inside a scene. The optional target scene is a reference by ID.',
    note: 'The effects map is a provisional game contract; CharacterManager eventually interprets it.',
    example: { id: 'left', text: 'Take the road to the mill', nextSceneId: 'demo-mill', effects: { courage: 1 } },
  },
  updated: {
    kind: 'INCOMING EVENT', title: 'SceneChangedEvent',
    description: 'Full scene update published by StoryContentManager (or this mock) on storycontent.events with routing key scene-content.updated.',
    note: 'PUBLISHED upserts the scene. RETIRED withdraws it and retains a tombstone. Versions must increase per scene.',
    example: defaultEvent,
  },
  made: {
    kind: 'OUTGOING EVENT', title: 'ChoiceMadeEvent',
    description: 'Emitted by SceneManager after local choice validation, routed to both demo StoryPath and Character subscribers.',
    note: 'Includes a scene/choice text snapshot so subsequent content edits do not change the player\'s recorded history.',
    example: {
      eventId: 'generated-uuid', eventType: 'player-choice.made',
      occurredAt: '2026-11-12T12:00:00Z', schemaVersion: 1,
      payload: {
        characterId: 'demo-character', sceneId: 'demo-start', sceneVersion: 1,
        choiceId: 'left', choiceLabel: 'Take the road to the mill',
        beat: 'You take the road to the mill.', destinationSceneId: 'demo-mill',
      },
    },
  },
};
let currentContract = 'scene';
let lastResponse = '';
let toastTimer;

function displayToast(message) {
  const toast = byId('toast');
  toast.textContent = message;
  toast.hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { toast.hidden = true; }, 3600);
}

async function api(path, options) {
  let response;
  try { response = await fetch(path, { cache: 'no-store', ...options }); }
  catch (error) { throw new Error('Dashboard request failed: ' + error.message); }
  const text = await response.text();
  let data;
  try { data = text ? JSON.parse(text) : null; }
  catch { data = { rawResponse: text }; }
  return { ok: response.ok, status: response.status, data };
}

function formatJSON(data) { return JSON.stringify(data, null, 2); }
function showResponse(method, path, status, data, elapsed) {
  byId('response-title').textContent = `${method} ${path}`;
  const badge = byId('response-status');
  badge.textContent = status ? `${status} ${status >= 200 && status < 300 ? 'OK' : 'ERROR'}` : 'OFFLINE';
  badge.className = 'response-status ' + (status >= 200 && status < 300 ? 'success' : 'failure');
  byId('response-meta').textContent = `${method} ${path} · ${Math.round(elapsed)} ms`;
  lastResponse = formatJSON(data);
  byId('response-body').textContent = lastResponse;
  byId('response-timestamp').textContent = new Date().toLocaleTimeString();
}

function setServiceState(kind, state) {
  const running = Boolean(state?.online);
  byId(`${kind}-state`).textContent = running ? 'Online' : 'Offline';
  byId(`${kind}-state`).className = 'metric-value ' + (running ? 'connected' : 'disconnected');
  byId(`side-${kind}-dot`).className = 'status-dot ' + (running ? 'online' : 'offline');
  byId(`side-${kind}-label`).textContent = running ? 'Online' : 'Offline';
  if (state?.message) byId(`${kind}-detail`).textContent = 'Connection unavailable';
}

async function loadStatus() {
  try {
    const response = await api('/api/status');
    if (!response.ok) throw new Error('Cannot check status');
    setServiceState('manager', response.data.manager);
    setServiceState('broker', response.data.broker);
    if (response.data.broker?.version) byId('broker-detail').textContent = `${lastConnectionConfig?.rabbitPublicUrl || 'RabbitMQ management'} · RabbitMQ ${response.data.broker.version}`;
  } catch {
    setServiceState('manager', { online: false });
    setServiceState('broker', { online: false });
  }
}

function renderQueue(queue) {
  const tr = document.createElement('tr');
  function td(text, className) {
    const node = document.createElement('td');
    if (className) node.className = className;
    node.textContent = text;
    tr.appendChild(node);
    return node;
  }
  td(queue.name);
  const purpose = td(queue.role);
  const event = document.createElement('small');
  event.textContent = queue.event;
  purpose.appendChild(event);
  td(queue.error ? '-' : String(queue.ready), 'number-col');
  td(queue.error ? '-' : String(queue.unacked), 'number-col');
  td(queue.error ? '-' : String(queue.consumers), 'number-col');
  const cell = td('');
  const label = document.createElement('span');
  label.className = 'queue-state ' + (queue.error ? 'down' : queue.exists ? '' : 'waiting');
  label.textContent = queue.error ? 'Unavailable' : queue.exists ? 'Declared' : 'Not created';
  cell.appendChild(label);
  return tr;
}

async function loadQueues() {
  const table = byId('queue-table');
  try {
    const response = await api('/api/queues');
    if (!response.ok) throw new Error('Queue API returned an error');
    table.replaceChildren(...response.data.queues.map(renderQueue));
    byId('queues-updated').textContent = `UPDATED ${new Date().toLocaleTimeString()}`;
  } catch {
    const row = document.createElement('tr');
    const cell = document.createElement('td');
    cell.colSpan = 6;
    cell.className = 'loading-row';
    cell.textContent = 'Queue metrics unavailable. Start RabbitMQ and refresh.';
    row.appendChild(cell);
    table.replaceChildren(row);
    byId('queues-updated').textContent = 'OFFLINE';
  }
}

let lastConnectionConfig = null;
const settingsFields = [
  'rabbitUrl', 'rabbitPublicUrl', 'amqpDockerAddress', 'amqpHostAddress',
  'updateQueue', 'updateRoutingKey', 'deadQueue',
  'exchange', 'choiceRoutingKey', 'choiceStoryPathQueue', 'choiceCharacterQueue',
  'managerUrl', 'managerPublicUrl', 'mongoDockerAddress', 'mongoHostAddress',
];
function renderSettings(config) {
  lastConnectionConfig = config;
  const form = byId('connection-form');
  for (const key of settingsFields) form.elements.namedItem(key).value = config[key] || '';
  byId('choice-broker-address').textContent = config.amqpDockerAddress;
  byId('rabbit-ui-link').href = config.rabbitPublicUrl;
  byId('manager-detail').textContent = config.managerPublicUrl;
  byId('broker-detail').textContent = config.rabbitPublicUrl;
  byId('arch-update-routing').textContent = config.updateRoutingKey;
  byId('arch-update-queue').textContent = config.updateQueue;
  byId('arch-indexer-amqp').textContent = `RabbitMQ ${config.amqpDockerAddress}`;
  byId('arch-choice-routing').textContent = config.choiceRoutingKey;
  byId('arch-choice-exchange').textContent = config.exchange;
  byId('arch-choice-broker').textContent = `RabbitMQ ${config.amqpDockerAddress}`;
  byId('arch-choice-subscribers').textContent = `${config.choiceStoryPathQueue} · ${config.choiceCharacterQueue}`;
  byId('mock-event-route').textContent = `${config.exchange} → ${config.updateRoutingKey}`;
}
function settingsFeedback(message, error = false) {
  const messageBox = byId('connection-feedback');
  messageBox.textContent = message;
  messageBox.classList.toggle('error', error);
}
async function loadConfig() {
  try {
    const response = await api('/api/config');
    if (!response.ok) throw new Error(response.data.error || 'Could not load connection settings');
    renderSettings(response.data);
    settingsFeedback('Loaded local connection settings. Addresses are editable below.');
  } catch (error) { settingsFeedback(error.message, true); }
}
async function saveConnections(event) {
  event.preventDefault();
  const button = byId('save-connections');
  await whileBusy(button, async () => {
    const form = byId('connection-form');
    const data = Object.fromEntries(settingsFields.map(key => [key, form.elements.namedItem(key).value.trim()]));
    try {
      const response = await api('/api/config', { method: 'PUT', headers: { 'content-type': 'application/json' }, body: JSON.stringify(data) });
      if (!response.ok) throw new Error(response.data.error || 'Could not save settings');
      renderSettings(response.data.config);
      settingsFeedback('Saved. Dashboard API and management connections are active now; Java/Docker addresses are reference values until you update/restart those services.');
      displayToast('Connection settings saved');
      await Promise.all([loadStatus(), loadQueues()]);
    } catch (error) { settingsFeedback(error.message, true); displayToast(error.message); }
  });
}
async function resetConnections() {
  if (!confirm('Reset all dashboard connection settings to the original defaults?')) return;
  const button = byId('reset-connections');
  await whileBusy(button, async () => {
    try {
      const response = await api('/api/config/reset', { method: 'POST' });
      if (!response.ok) throw new Error(response.data.error || 'Reset failed');
      renderSettings(response.data.config);
      settingsFeedback('Restored dashboard defaults. Java service configuration is unchanged.');
      displayToast('Dashboard defaults restored');
      await Promise.all([loadStatus(), loadQueues()]);
    } catch (error) { settingsFeedback(error.message, true); }
  });
}

function createPreview(scene) {
  const box = byId('scene-preview');
  box.replaceChildren();
  const name = document.createElement('h3'); name.className = 'preview-name'; name.textContent = scene.name || '(unnamed scene)';
  const version = document.createElement('div'); version.className = 'preview-version'; version.textContent = `ID: ${scene.id} / VERSION: ${scene.version}`;
  const description = document.createElement('p'); description.className = 'preview-story'; description.textContent = scene.description || scene.text || '';
  box.append(name, version, description);
  for (const choice of scene.choices || []) {
    const row = document.createElement('div'); row.className = 'preview-choice';
    const indicator = document.createElement('span'); indicator.textContent = '→';
    const choiceText = document.createElement('b'); choiceText.textContent = choice.text;
    const next = document.createElement('small'); next.textContent = choice.nextSceneId || 'End scene';
    row.append(indicator, choiceText, next);
    box.appendChild(row);
  }
}

async function runEndpoint(method, path, payload) {
  const start = performance.now();
  const options = { method };
  if (payload !== undefined) {
    options.headers = { 'content-type': 'application/json' };
    options.body = JSON.stringify(payload);
  }
  try {
    const response = await api('/api' + path, options);
    showResponse(method, path, response.status, response.data, performance.now() - start);
    if (method === 'GET' && /^\/scenes\/[^/]+$/.test(path) && response.ok && response.data) createPreview(response.data);
    if (!response.ok) displayToast(`Request returned HTTP ${response.status}`);
  } catch (error) {
    showResponse(method, path, 0, { error: error.message }, performance.now() - start);
    displayToast(error.message);
  }
}

function modeChanged(which) {
  const isDelete = which === 'DELETE';
  byId('event-json').value = formatJSON(isDelete ? deleteEvent : defaultEvent);
  byId('load-upsert').classList.toggle('selected', !isDelete);
  byId('load-delete').classList.toggle('selected', isDelete);
}

function setPublishStatus(message, success) {
  const node = byId('publish-status');
  node.textContent = message;
  node.className = success ? 'ok' : 'error';
}

async function whileBusy(button, work) {
  button.disabled = true;
  try { await work(); } finally { button.disabled = false; }
}

async function publishEvent() {
  const button = byId('publish-event');
  await whileBusy(button, async () => {
    let event;
    try { event = JSON.parse(byId('event-json').value); }
    catch { setPublishStatus('Invalid JSON. Correct the event before publishing.', false); return; }
    if (event.payload?.status === 'RETIRED'
        && !confirm(`Publish a RETIRED tombstone for ${event.payload.sceneId}?`)) return;
    try {
      const response = await api('/api/mock/publish', {
        method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify(event),
      });
      if (!response.ok) {
        setPublishStatus(response.data.detail || response.data.error || 'Event not published', false);
        return;
      }
      setPublishStatus(`Published ${response.data.sceneId} v${response.data.version} to scene-content.updated.`, true);
      displayToast('Scene changed event routed to the indexer queue');
      await loadQueues();
    } catch (error) { setPublishStatus(error.message, false); }
  });
}

async function seedScenes(button) {
  await whileBusy(button, async () => {
    setPublishStatus('Publishing three demo events...', true);
    try {
      const response = await api('/api/mock/seed', { method: 'POST' });
      if (!response.ok) {
        setPublishStatus(response.data?.detail || response.data?.error || 'Seed failed', false);
        displayToast('Could not seed scenes - check broker connection');
        return;
      }
      setPublishStatus('Three demo events published: demo-start, demo-mill, demo-village. Indexing is asynchronous.', true);
      displayToast('Demo scene events published');
      await loadQueues();
    } catch (error) { setPublishStatus(error.message, false); }
  });
}

function loadContract(key) {
  if (!contractDefinitions[key]) return;
  currentContract = key;
  const contract = contractDefinitions[key];
  document.querySelectorAll('.contract-tab').forEach((button) => {
    const selected = button.dataset.contract === key;
    button.classList.toggle('selected', selected);
    button.setAttribute('aria-selected', String(selected));
  });
  byId('contract-kind').textContent = contract.kind;
  byId('contract-title').textContent = contract.name || contract.title;
  byId('contract-description').textContent = contract.description;
  byId('contract-note').textContent = contract.note;
  byId('contract-code').textContent = formatJSON(contract.example);
}

async function copyText(text) {
  try { await navigator.clipboard.writeText(text); displayToast('Copied to clipboard'); }
  catch { displayToast('Clipboard unavailable. Select the text to copy.'); }
}

function setupHandlers() {
  byId('refresh-button').addEventListener('click', () => { loadStatus(); loadQueues(); });
  byId('seed-scenes').addEventListener('click', (event) => seedScenes(event.currentTarget));
  byId('get-scene-form').addEventListener('submit', (event) => {
    event.preventDefault();
    runEndpoint('GET', `/scenes/${encodeURIComponent(byId('get-scene-id').value.trim())}`);
  });
  byId('get-next-form').addEventListener('submit', (event) => {
    event.preventDefault();
    runEndpoint('GET', `/scenes/${encodeURIComponent(byId('get-next-id').value.trim())}/nextscenes`);
  });
  byId('choose-form').addEventListener('submit', (event) => {
    event.preventDefault();
    const sceneId = encodeURIComponent(byId('choice-scene-id').value.trim());
    const choiceId = encodeURIComponent(byId('choice-id').value.trim());
    runEndpoint('POST', `/scenes/${sceneId}/choices/${choiceId}`, { characterId: byId('character-id').value });
  });
  byId('load-upsert').addEventListener('click', () => modeChanged('UPSERT'));
  byId('load-delete').addEventListener('click', () => modeChanged('DELETE'));
  byId('publish-event').addEventListener('click', publishEvent);
  byId('copy-response').addEventListener('click', () => copyText(lastResponse || byId('response-body').textContent));
  byId('copy-contract').addEventListener('click', () => copyText(formatJSON(contractDefinitions[currentContract].example)));
  byId('connection-form').addEventListener('submit', saveConnections);
  byId('reset-connections').addEventListener('click', resetConnections);
  byId('copy-connections').addEventListener('click', () => copyText(JSON.stringify(Object.fromEntries(settingsFields.map(key => [key, byId('connection-form').elements.namedItem(key).value.trim()])), null, 2)));
  document.querySelectorAll('.contract-tab').forEach((button) => button.addEventListener('click', () => loadContract(button.dataset.contract)));
  document.querySelectorAll('.nav-link').forEach((link) => link.addEventListener('click', () => {
    document.querySelectorAll('.nav-link').forEach((item) => item.classList.remove('active'));
    link.classList.add('active');
  }));
}


/** Draw right-angle arrows between the actual card edges, including responsive layouts.
 * SVG paths are presentation only; nodes and queue labels remain real HTML.
 */
function setupArchitectureConnectors() {
  const board = document.querySelector('.architecture-board');
  const mongo = board?.querySelector('.arch-database');
  const manager = board?.querySelector('.arch-manager-node');
  const choice = board?.querySelector('.arch-choice');
  const canvas = byId('arch-connectors');
  if (!board || !mongo || !manager || !choice || !canvas) return;

  const readPath = byId('arch-read-path');
  const publishPath = byId('arch-publish-path');
  const readLabel = byId('arch-read-label');
  const publishLabel = byId('arch-publish-label');
  let frame = 0;

  function draw() {
    frame = 0;
    const root = board.getBoundingClientRect();
    if (!root.width || !root.height) return;
    canvas.setAttribute('viewBox', `0 0 ${root.width} ${root.height}`);
    const position = (el) => {
      const r = el.getBoundingClientRect();
      return { left: r.left - root.left, right: r.right - root.left,
        top: r.top - root.top, bottom: r.bottom - root.top,
        midX: (r.left + r.right) / 2 - root.left,
        midY: (r.top + r.bottom) / 2 - root.top };
    };
    const db = position(mongo);
    const mgr = position(manager);
    const queue = position(choice);
    const mobile = window.matchMedia('(max-width:700px)').matches;
    const stacked = window.matchMedia('(max-width:1120px)').matches;
    // Labels are positioned independently of their paths and never intercept clicks.
    const place = (element, x, y, align = 'left') => {
      element.style.left = `${x}px`;
      element.style.top = `${y}px`;
      element.style.transform = align === 'center' ? 'translate(-50%,-50%)' : 'translateY(-50%)';
    };

    if (mobile) {
      const readMid = (db.bottom + mgr.top) / 2;
      readPath.setAttribute('d', `M ${db.midX} ${db.bottom} V ${readMid} H ${mgr.midX} V ${mgr.top}`);
      place(readLabel, Math.min(db.midX + 15, root.width - readLabel.offsetWidth - 6), readMid);
      const choiceMid = (mgr.bottom + queue.top) / 2;
      publishPath.setAttribute('d', `M ${mgr.midX} ${mgr.bottom} V ${choiceMid} H ${queue.midX} V ${queue.top}`);
      place(publishLabel, Math.max(6, mgr.midX - publishLabel.offsetWidth - 17), choiceMid);
    } else {
      const readX = mgr.midX;
      readPath.setAttribute('d', `M ${db.right} ${db.midY} H ${readX} V ${mgr.bottom}`);
      place(readLabel, Math.min(readX + 12, root.width - readLabel.offsetWidth - 6),
        (db.midY + mgr.bottom) / 2);

      if (stacked) {
        // The right side is an open lane, avoiding the scene cards above.
        const laneX = root.width - 20;
        const turnY = queue.top - 19;
        publishPath.setAttribute('d', `M ${mgr.right} ${mgr.midY} H ${laneX} V ${turnY} H ${queue.midX} V ${queue.top}`);
        place(publishLabel, queue.midX, turnY - 15, 'center');
      } else {
        const bendX = (mgr.right + queue.left) / 2;
        publishPath.setAttribute('d', `M ${mgr.right} ${mgr.midY} H ${bendX} V ${queue.midY} H ${queue.left}`);
        // Prefer the free space just left of the vertical leg, between the cards.
        if (mgr.top - queue.bottom > publishLabel.offsetHeight + 10) {
          place(publishLabel, Math.max(6, bendX - publishLabel.offsetWidth - 9),
            (queue.bottom + mgr.top) / 2);
        } else {
          place(publishLabel, bendX, Math.min(mgr.top, queue.top) - 17, 'center');
        }
      }
    }
  }
  const schedule = () => {
    if (!frame) frame = requestAnimationFrame(draw);
  };
  // Avoid stale fixed coordinates when labels wrap or the viewport changes.
  if (typeof ResizeObserver !== 'undefined') {
    const observer = new ResizeObserver(schedule);
    [board, mongo, manager, choice].forEach((element) => observer.observe(element));
  }
  window.addEventListener('resize', schedule);
  if (document.fonts?.ready) document.fonts.ready.then(schedule);
  schedule();
}

setupArchitectureConnectors();
setupHandlers();
modeChanged('UPSERT');
loadContract('scene');
loadConfig();
loadStatus();
loadQueues();
setInterval(() => { loadStatus(); loadQueues(); }, 15000);
