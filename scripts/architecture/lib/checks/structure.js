'use strict';

/**
 * Checks 2, 3, 4, 5 and 11 — the ones that can be answered from the manifest and the
 * rendered file alone, without looking at the rest of the repository.
 *
 * All of them are errors except 11: a file that will not open, a token that never
 * resolved, or a service the diagram forgot to draw are defects in the artifact itself.
 */

const { wellFormedProblems } = require('../drawio.js');

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

  // --- 5: ids are usable as Kubernetes object names ---------------------------------
  for (const s of manifest.services) {
    if (!ID_PATTERN.test(s.id)) {
      findings.push(error(5, `service id '${s.id}' is not a valid Kubernetes object name`));
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
