'use strict';

/**
 * Checks 2, 3, 4, 5 and 11 — the ones that can be answered from the manifest and the
 * rendered file alone, without looking at the rest of the repository.
 *
 * All of them are errors except 11: a file that will not open, a token that never
 * resolved, or a service the diagram forgot to draw are defects in the artifact itself.
 */

const { wellFormedProblems, looksUnreadable } = require('../drawio.js');

const ID_PATTERN = /^[a-z][a-z0-9-]*$/;
const TOKEN = /\{\{([^"{}]+)\}\}/g;

const error = (check, message) => ({ check, severity: 'error', message });
const warning = (check, message) => ({ check, severity: 'warning', message });

function run(ctx) {
  const findings = [];
  const { manifest, template, xml, scan, resolve } = ctx;

  // --- 2: the file opens, and its tabs are the ones the manifest declares -----------
  for (const problem of wellFormedProblems(xml)) {
    findings.push(error(2, `architecture.drawio is not well formed: ${problem}`));
  }

  const declared = manifest.meta.tabs || [];
  const present = scan.tabs.map((t) => t.id);

  // Blame the file, not the manifest. A diagram the scanner cannot parse yields zero
  // tabs, and comparing that against the manifest would print one "missing tab" per
  // declared tab and send the reader looking in architecture.json, where nothing is
  // wrong.
  if (looksUnreadable(xml, scan)) {
    return [
      ...findings,
      error(2, 'the scanner could not read any tab from this file — its <diagram> elements are malformed'),
    ];
  }

  for (const id of declared) {
    if (!present.includes(id)) {
      findings.push(error(2, `manifest declares tab '${id}' but the diagram has no such tab`));
    }
  }
  for (const id of present) {
    if (!declared.includes(id)) {
      findings.push(error(2, `the diagram has tab '${id}' which the manifest does not declare`));
    }
  }
  const seen = new Set();
  for (const id of present) {
    if (seen.has(id)) findings.push(error(2, `duplicate tab id '${id}'`));
    seen.add(id);
  }

  // --- 3: every token in the template resolves --------------------------------------
  for (const m of template.matchAll(TOKEN)) {
    if (resolve(m[1]) == null) {
      findings.push(error(3, `template token {{${m[1]}}} does not resolve against the manifest`));
    }
  }

  // --- 4: no service is in the manifest but absent from the drawing -----------------
  for (const s of manifest.services) {
    if (!template.includes(`{{service:${s.id}:`)) {
      findings.push(error(4, `service '${s.id}' is in the manifest but no template token draws it`));
    }
  }

  // --- 4 (continued): a version in the manifest that the drawing does not print ------
  // Infrastructure without a version has nothing to drift, so it may stay hand-drawn.
  // One that carries an image tag must be rendered from the manifest, or bumping it
  // there changes nothing on the diagram and nobody is told.
  for (const i of manifest.infrastructure || []) {
    if (i.image && !template.includes(`{{infra:${i.id}:`)) {
      findings.push(
        error(4, `infrastructure '${i.id}' declares image '${i.image}' but no token draws it`)
      );
    }
  }

  // --- 5: ids are usable as Kubernetes object names ---------------------------------
  for (const s of manifest.services) {
    if (!ID_PATTERN.test(s.id)) {
      findings.push(error(5, `service id '${s.id}' is not a valid Kubernetes object name`));
    }
  }

  // --- 12: the border agrees with the status ----------------------------------------
  // The badge comes from the manifest; the border is hand-drawn in the template. The
  // legend promises that a dashed box is a service that does not exist, so the two
  // halves disagreeing is the diagram stating something false about the system.
  const panels = new Map();
  for (const tab of scan.tabs) {
    for (const c of tab.cells) {
      if (c.id && c.id.startsWith('panel-')) panels.set(c.id.slice('panel-'.length), c);
    }
  }
  for (const s of manifest.services) {
    const panel = panels.get(s.id);
    if (!panel) continue; // check 4 already reports a service nothing draws
    const dashed = /dashed=1/.test(panel.style);
    if (s.status === 'planned' && !dashed) {
      findings.push(error(12, `${s.id} is planned but its box is not drawn dashed`));
    }
    if (s.status !== 'planned' && dashed) {
      findings.push(error(12, `${s.id} is ${s.status} but its box is drawn as planned`));
    }
  }

  // --- 13: nothing references a cell that is not there ------------------------------
  // draw.io drops a cell whose parent is missing, and an edge whose endpoint is gone,
  // without complaining. Content disappears from the diagram quietly.
  for (const tab of scan.tabs) {
    const ids = new Set(tab.cells.map((c) => c.id));
    for (const c of tab.cells) {
      for (const [field, value] of [['parent', c.parent], ['source', c.source], ['target', c.target]]) {
        if (value && !ids.has(value)) {
          findings.push(error(13, `${tab.id}: '${c.id}' has ${field}='${value}', which is not a cell`));
        }
      }
    }
  }

  // --- 14: no child is drawn outside its parent -------------------------------------
  for (const tab of scan.tabs) {
    const byId = new Map(tab.cells.map((c) => [c.id, c]));
    for (const c of tab.cells) {
      if (!c.vertex || !c.geometry) continue;
      const parent = byId.get(c.parent);
      if (!parent || !parent.geometry) continue;
      const g = c.geometry;
      const outside =
        g.x < 0 || g.y < 0 || g.x + g.width > parent.geometry.width || g.y + g.height > parent.geometry.height;
      if (outside) {
        findings.push(
          error(14, `${tab.id}: '${c.id}' is drawn outside its parent '${c.parent}' and would be clipped`)
        );
      }
    }
  }

  // --- 11: every event reaches someone, through a named queue -----------------------
  for (const e of manifest.events || []) {
    const consumers = e.consumers || [];
    const queues = e.queues || [];
    if (consumers.length === 0) {
      findings.push(warning(11, `event '${e.name}' has no consumers; the exchange would discard it`));
    }
    if (queues.length < consumers.length) {
      findings.push(
        warning(11, `event '${e.name}' has ${consumers.length} consumer(s) but ${queues.length} queue(s)`)
      );
    }
  }

  return findings;
}

module.exports = { run };
