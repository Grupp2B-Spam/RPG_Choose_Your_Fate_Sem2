/* Choose Your Fate - Scene Control. Browser code has no RabbitMQ credentials. */
const byId = (id) => document.getElementById(id);
const defaultEvent = {
  sceneId: 'demo-start', version: 1, operation: 'UPSERT',
  title: 'The Crossroads', text: 'The road splits beneath an ancient oak.',
  choices: [
    { id: 'left', text: 'Take the road to the mill', nextSceneId: 'demo-mill', effects: { courage: 1 } },
    { id: 'right', text: 'Walk toward the village', nextSceneId: 'demo-village', effects: {} },
  ],
};
const deleteEvent = { sceneId: 'demo-start', version: 2, operation: 'DELETE' };
const contractDefinitions = {
  scene: {
    kind: 'MONGO DOCUMENT', title: 'SceneDocument',
    description: 'The read-optimized scene projection. SceneIndexer writes it to MongoDB; SceneManager retrieves it.',
    note: 'A deleted scene is retained as a tombstone (deleted: true) and is hidden from the REST API.',
    example: {
      id: 'demo-start', version: 1, title: 'The Crossroads',
      text: 'The road splits beneath an ancient oak.',
      choices: [{ id: 'left', text: 'Take the road to the mill', nextSceneId: 'demo-mill', effects: { courage: 1 } }],
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
    description: 'Full scene update published by StoryContentManager (or this mock) on game.events with routing key scene.changed.',
    note: 'Supported operations are UPSERT and DELETE. Versions must increase per scene. DELETE keeps a tombstone.',
    example: defaultEvent,
  },
  made: {
    kind: 'OUTGOING EVENT', title: 'ChoiceMadeEvent',
    description: 'Emitted by SceneManager after local choice validation, routed to both demo StoryPath and Character subscribers.',
    note: 'Includes a scene/choice text snapshot so subsequent content edits do not change the player\'s recorded history.',
    example: {
      eventId: 'generated-uuid', characterId: 'demo-character', sceneId: 'demo-start', sceneVersion: 1,
      sceneTitle: 'The Crossroads', sceneText: 'The road splits beneath an ancient oak.',
      choiceId: 'left', choiceText: 'Take the road to the mill', nextSceneId: 'demo-mill',
      effects: { courage: 1 }, occurredAt: '2026-11-12T12:00:00Z',
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
    if (response.data.broker?.version) byId('broker-detail').textContent = `RabbitMQ ${response.data.broker.version} · management API`;
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

async function loadConfig() {
  try {
    const response = await api('/api/config');
    if (!response.ok) return;
    const c = response.data;
    byId('amqp-host').textContent = c.brokerAmqpHost;
    byId('amqp-docker').textContent = c.brokerAmqpDocker;
    byId('rabbit-ui').textContent = new URL(c.brokerManagement).host;
    byId('rabbit-ui-link').href = c.brokerManagement;
    byId('manager-url').textContent = new URL(c.sceneManager).host;
    byId('manager-detail').textContent = c.sceneManager;
    byId('broker-detail').textContent = c.brokerManagement;
  } catch { /* Static reference values remain visible while offline. */ }
}

function createPreview(scene) {
  const box = byId('scene-preview');
  box.replaceChildren();
  const title = document.createElement('h3'); title.className = 'preview-title'; title.textContent = scene.title || '(untitled scene)';
  const version = document.createElement('div'); version.className = 'preview-version'; version.textContent = `ID: ${scene.id} / VERSION: ${scene.version}`;
  const description = document.createElement('p'); description.className = 'preview-story'; description.textContent = scene.text || '';
  box.append(title, version, description);
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
    if (event.operation === 'DELETE' && !confirm(`Publish a DELETE tombstone for ${event.sceneId}?`)) return;
    try {
      const response = await api('/api/mock/publish', {
        method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify(event),
      });
      if (!response.ok) {
        setPublishStatus(response.data.detail || response.data.error || 'Event not published', false);
        return;
      }
      setPublishStatus(`Published ${response.data.sceneId} v${response.data.version} to scene.changed.`, true);
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
  byId('contract-title').textContent = contract.title;
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
  document.querySelectorAll('.copy-address').forEach((button) => button.addEventListener('click', () => {
    copyText(byId(button.dataset.copyTarget).textContent);
  }));
  document.querySelectorAll('.contract-tab').forEach((button) => button.addEventListener('click', () => loadContract(button.dataset.contract)));
  document.querySelectorAll('.nav-link').forEach((link) => link.addEventListener('click', () => {
    document.querySelectorAll('.nav-link').forEach((item) => item.classList.remove('active'));
    link.classList.add('active');
  }));
}

setupHandlers();
modeChanged('UPSERT');
loadContract('scene');
loadConfig();
loadStatus();
loadQueues();
setInterval(() => { loadStatus(); loadQueues(); }, 15000);
