'use strict';

/**
 * Loading and validating architecture.json.
 *
 * The manifest is what the diagram asserts as fact, so a typo here ships as a
 * statement about the system. Validation is therefore strict about the things that
 * would go unnoticed — an id that does not match the Kubernetes naming convention, an
 * event naming a consumer that does not exist — and silent about the things a service
 * is legitimately allowed not to know yet, such as its port.
 */

const fs = require('node:fs');

const KINDS = ['service', 'legacy', 'infrastructure'];
const STATUSES = ['planned', 'skeleton', 'implemented'];
const CONTEXTS = ['identity', 'content', 'state', 'media'];

const ID_PATTERN = /^[a-z][a-z0-9-]*$/;

/**
 * @param {object} obj
 * @returns {string[]} one line per problem; empty when the manifest is valid.
 */
function validateManifest(obj) {
  const problems = [];

  if (!obj || typeof obj !== 'object') {
    return ['manifest is not an object'];
  }

  for (const field of ['version', 'updated', 'broker']) {
    if (obj.meta == null || obj.meta[field] == null) {
      problems.push(`meta.${field} is missing`);
    }
  }

  const services = Array.isArray(obj.services) ? obj.services : [];
  if (!Array.isArray(obj.services)) problems.push('services is missing or not an array');

  const seen = new Set();
  for (const s of services) {
    const where = `service '${s && s.id}'`;

    for (const field of ['id', 'kind', 'display', 'status', 'purpose']) {
      if (s[field] == null) problems.push(`${where} is missing ${field}`);
    }

    if (typeof s.id === 'string' && !ID_PATTERN.test(s.id)) {
      problems.push(`service id '${s.id}' is not lowercase-hyphen`);
    }
    if (seen.has(s.id)) problems.push(`duplicate service id '${s.id}'`);
    seen.add(s.id);

    if (s.kind != null && !KINDS.includes(s.kind)) {
      problems.push(`${where} has unknown kind '${s.kind}'`);
    }
    if (s.status != null && !STATUSES.includes(s.status)) {
      problems.push(`${where} has unknown status '${s.status}'`);
    }
    if (s.context != null && !CONTEXTS.includes(s.context)) {
      problems.push(`${where} has unknown context '${s.context}'`);
    }
    // The monolith spans every bounded context, which is the whole reason it is being
    // decomposed. Letting it claim one would misreport that on the diagram.
    if (s.kind === 'legacy' && s.context != null) {
      problems.push(`${where} is legacy and must not declare a context`);
    }
  }

  const events = Array.isArray(obj.events) ? obj.events : [];
  const eventNames = new Set(events.map((e) => e && e.name));

  for (const e of events) {
    const where = `event '${e && e.name}'`;
    for (const field of ['name', 'exchange', 'publisher']) {
      if (e[field] == null) problems.push(`${where} is missing ${field}`);
    }
    if (e.publisher != null && !seen.has(e.publisher)) {
      problems.push(`${where} names publisher '${e.publisher}' which is not a service`);
    }
    for (const c of e.consumers || []) {
      if (!seen.has(c)) {
        problems.push(`${where} names consumer '${c}' which is not a service`);
      }
    }
  }

  for (const s of services) {
    for (const c of s.consumes || []) {
      if (c.event != null && !eventNames.has(c.event)) {
        problems.push(`service '${s.id}' consumes '${c.event}' which is not a declared event`);
      }
    }
  }

  return problems;
}

/**
 * @param {string} absPath
 * @returns {object} the parsed manifest
 * @throws {Error} with `.problems` set when the manifest is invalid
 */
function loadManifest(absPath) {
  const obj = JSON.parse(fs.readFileSync(absPath, 'utf8'));
  const problems = validateManifest(obj);
  if (problems.length > 0) {
    const error = new Error(`${absPath} is not a valid manifest:\n  ${problems.join('\n  ')}`);
    error.problems = problems;
    throw error;
  }
  return obj;
}

module.exports = { validateManifest, loadManifest, KINDS, STATUSES, CONTEXTS };
