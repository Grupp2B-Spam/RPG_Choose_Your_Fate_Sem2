'use strict';

/**
 * Turning manifest entries into the HTML fragments draw.io stores in a cell value.
 *
 * Returns raw HTML. Escaping for an XML attribute happens in splice.js, once, so that
 * nothing here has to know where it is going to be put.
 *
 * An unknown id or field returns null rather than a fallback string. A blank box on a
 * diagram looks deliberate; a failed build does not.
 */

const BADGES = {
  implemented: (s) => `● v${s.version}`,
  skeleton: (s) => (s.version ? `◐ skeleton · v${s.version}` : '◐ skeleton'),
  planned: () => '○ planned',
};

const MARK_WRITTEN = '✓';
const MARK_TODO = '⚠';

function badge(s) {
  if (s.kind === 'legacy') return 'monolith — being decomposed';
  const make = BADGES[s.status];
  return make ? make(s) : s.status;
}

function card(s) {
  const lines = [`<b>${s.display}</b>`, s.purpose, badge(s)];
  // A branch name is a fact with a shelf life: it is worth showing while the code is
  // not on main, and it disappears from the manifest when the branch merges.
  if (s.branch && s.branch !== 'main') lines.push(`<i>branch: ${s.branch}</i>`);
  return lines.join('<br>');
}

function apis(s) {
  const exposed = s.exposes || [];
  if (exposed.length === 0) return '<b>APIs</b><br>none yet';
  const lines = exposed.map((e) => {
    const version = e.versioning ? ` <i>(${e.versioning})</i>` : '';
    return `${e.style} · ${e.detail}${version}`;
  });
  for (const c of s.calls || []) {
    lines.push(`${c.style} → ${c.target} · ${c.method}`);
  }
  return [`<b>APIs</b>`, ...lines].join('<br>');
}

function events(s) {
  const out = [];
  for (const p of s.publishes || []) out.push(`▲ ${p.event}`);
  for (const c of s.consumes || []) out.push(`▼ ${c.event}`);
  if (out.length === 0) return '<b>Events</b><br>none';
  return ['<b>Events</b>', ...out].join('<br>');
}

/** Every contract this service is party to, whether it implements it or publishes it. */
function contractsOf(manifest, s) {
  const found = new Map();
  const add = (c) => {
    if (c && c.path && !found.has(c.path)) found.set(c.path, c);
  };
  for (const e of s.exposes || []) add(e.contract);
  for (const c of s.calls || []) add(c.contract);
  for (const p of s.publishes || []) {
    const event = manifest.events.find((e) => e.name === p.event);
    if (event) add(event.contract);
  }
  return [...found.values()];
}

function contracts(manifest, s) {
  const list = contractsOf(manifest, s);
  if (list.length === 0) return '<b>Contracts</b><br>none';
  const lines = list.map((c) => {
    const mark = c.state === 'written' ? MARK_WRITTEN : MARK_TODO;
    // The contract's own version, which is not the service's artifact version and moves
    // independently of it — the thing a consumer actually codes against.
    const version = c.version ? ` ${c.version}` : '';
    return `${mark} ${c.path.split('/').pop()}${version}`;
  });
  return ['<b>Contracts</b>', ...lines].join('<br>');
}

/**
 * A piece of infrastructure, as the diagram prints it.
 *
 * The image tag is the reason this is a token rather than typed into the template:
 * it is a version, and a version typed into a drawing is a version that goes stale.
 */
function infraLabel(i) {
  const lines = [`<b>${i.display}</b>`, i.kind];
  if (i.image) lines.push(i.image);
  if (i.external) lines.push('external to the cluster');
  if (i.container) lines.push(`container: ${i.container}`);
  if (i.ports && i.ports.length > 0) lines.push(`ports: ${i.ports.join(' · ')}`);
  if (i.note) lines.push(`<font style="font-size: 10px">${i.note}</font>`);
  return lines.join('<br>');
}

function eventLabel(e) {
  const n = (e.queues || []).length;
  return [
    `<b>${e.name}</b>`,
    e.exchange,
    `${n} ${n === 1 ? 'queue' : 'queues'}`,
  ].join('<br>');
}

/**
 * A message body, written out the way you would write it down: field, type, and the one
 * sentence that says why the field is there.
 *
 * An event nobody has designed renders as exactly that. Inventing a plausible shape
 * would be the worst thing this file could do, because it would read like a contract.
 */
const PAYLOAD_STATE = {
  documented: 'from the AsyncAPI contract',
  proposed: 'proposed — not agreed, and no contract file exists',
  undesigned: 'not designed yet',
};

function fieldLines(fields) {
  return fields.map((f) => {
    const note = f.note
      ? `<br><font style="font-size: 9px" color="#78909c">&nbsp;&nbsp;&nbsp;&nbsp;${f.note}</font>`
      : '';
    return `&nbsp;&nbsp;"${f.name}": <font color="#b07d00">${f.type}</font>${note}`;
  });
}

function payload(e) {
  const p = e.payload || { state: 'undesigned', fields: [] };
  const heading =
    `<b>${e.name}</b><br><font style="font-size: 9px">${PAYLOAD_STATE[p.state] || p.state}</font>`;

  if (!p.fields || p.fields.length === 0) {
    return `${heading}<br><br><i>No payload has been designed. Nothing is written here, because a plausible guess would read like a contract.</i>`;
  }

  return `${heading}<br><br>{<br>${fieldLines(p.fields).join(',<br>')}<br>}`;
}

function envelope(meta) {
  const fields = meta.envelope || [];
  if (fields.length === 0) return null;
  return `<b>Envelope — the same on every topic</b><br><br>{<br>${fieldLines(fields).join(',<br>')}<br>}`;
}

function contractTable(manifest) {
  const head =
    '<tr><th>Service</th><th>Version</th><th>Status</th><th>Stack</th>' +
    '<th>Exposes</th><th>Publishes</th><th>Consumes</th><th>Contracts</th></tr>';

  const rows = manifest.services.map((s) => {
    const exposes = (s.exposes || []).map((e) => e.style).concat(
      (s.calls || []).map((c) => `${c.style}→${c.target}`)
    );
    const cells = [
      s.display,
      s.version || '—',
      s.kind === 'legacy' ? 'legacy' : s.status,
      s.stack || '—',
      exposes.join(', ') || '—',
      (s.publishes || []).map((p) => p.event).join('<br>') || '—',
      (s.consumes || []).map((c) => c.event).join('<br>') || '—',
      contractsOf(manifest, s)
        .map((c) => `${c.state === 'written' ? MARK_WRITTEN : MARK_TODO} ${c.path.split('/').pop()}`)
        .join('<br>') || '—',
    ];
    return `<tr>${cells.map((c) => `<td>${c}</td>`).join('')}</tr>`;
  });

  return `<table>${head}${rows.join('')}</table>`;
}

/**
 * @param {object} manifest
 * @returns {(token: string) => string|null}
 */
function createResolver(manifest) {
  const byId = new Map(manifest.services.map((s) => [s.id, s]));
  const eventsByName = new Map((manifest.events || []).map((e) => [e.name, e]));
  const infraById = new Map((manifest.infrastructure || []).map((i) => [i.id, i]));

  return function resolve(token) {
    const parts = String(token).split(':');

    if (parts[0] === 'meta' && parts.length === 2) {
      if (parts[1] === 'updated') return manifest.meta.updated;
      if (parts[1] === 'envelope') return envelope(manifest.meta);
      return null;
    }

    if (parts[0] === 'table' && parts.length === 2) {
      return parts[1] === 'contracts' ? contractTable(manifest) : null;
    }

    if (parts[0] === 'service' && parts.length === 3) {
      const s = byId.get(parts[1]);
      if (!s) return null;
      switch (parts[2]) {
        case 'card': return card(s);
        case 'apis': return apis(s);
        case 'events': return events(s);
        case 'contracts': return contracts(manifest, s);
        default: return null;
      }
    }

    if (parts[0] === 'infra' && parts.length === 3) {
      const i = infraById.get(parts[1]);
      if (!i) return null;
      return parts[2] === 'label' ? infraLabel(i) : null;
    }

    if (parts[0] === 'event' && parts.length === 3) {
      const e = eventsByName.get(parts[1]);
      if (!e) return null;
      if (parts[2] === 'label') return eventLabel(e);
      if (parts[2] === 'payload') return payload(e);
      return null;
    }

    return null;
  };
}

module.exports = { createResolver };
