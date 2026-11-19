'use strict';

/**
 * Checks 8, 9 and 10 — the manifest against the repository it describes.
 *
 * All warnings. Three of the services sit on unmerged branches and three do not exist
 * at all, so no single checkout can satisfy these; an error here would mean a red build
 * for weeks, which is how a check gets switched off. They sharpen by themselves as the
 * branches land.
 */

const fs = require('node:fs');
const path = require('node:path');

const SKIP_DIRS = new Set(['node_modules', 'target', '.git', '.idea', 'dist', 'build']);
const MAX_FILES = 2000;

const warning = (check, message) => ({ check, severity: 'warning', message });

/** Where to grep for a service's own strings. Explicit wins; compose location is the fallback. */
function sourceRootOf(s) {
  if (s.sourceRoot) return s.sourceRoot;
  if (s.compose && s.compose.file) return path.dirname(s.compose.file);
  return null;
}

function* walk(dir, budget) {
  let entries;
  try {
    entries = fs.readdirSync(dir, { withFileTypes: true });
  } catch {
    return;
  }
  for (const entry of entries) {
    if (budget.count >= MAX_FILES) return;
    if (entry.isDirectory()) {
      if (SKIP_DIRS.has(entry.name)) continue;
      yield* walk(path.join(dir, entry.name), budget);
    } else {
      budget.count += 1;
      yield path.join(dir, entry.name);
    }
  }
}

function treeContains(absDir, needle) {
  for (const file of walk(absDir, { count: 0 })) {
    let text;
    try {
      text = fs.readFileSync(file, 'utf8');
    } catch {
      continue;
    }
    if (text.includes(needle)) return true;
  }
  return false;
}

/**
 * Every contract the manifest attributes to this service, once each.
 *
 * One AsyncAPI document describes every event a service publishes, so without the
 * deduplication a publisher of two events is reported twice for the same file.
 */
function contractsOf(manifest, s) {
  const byPath = new Map();
  const add = (c) => {
    if (c && c.path && !byPath.has(c.path)) byPath.set(c.path, c);
  };
  for (const e of s.exposes || []) add(e.contract);
  for (const c of s.calls || []) add(c.contract);
  for (const p of s.publishes || []) {
    const event = (manifest.events || []).find((e) => e.name === p.event);
    if (event) add(event.contract);
  }
  return [...byPath.values()];
}

/**
 * The host-side ports a compose file publishes for one service.
 *
 * Indentation-based rather than a YAML parse, deliberately: the alternative is a
 * dependency, and the shape being read here is two levels deep and entirely regular.
 */
function publishedHostPorts(composeText, serviceName) {
  const lines = composeText.split(/\r?\n/);
  const start = lines.findIndex((l) => new RegExp(`^\\s{1,4}${serviceName}:\\s*$`).test(l));
  if (start === -1) return null;

  const indent = lines[start].length - lines[start].trimStart().length;
  const ports = [];
  let inPorts = false;

  for (let i = start + 1; i < lines.length; i += 1) {
    const line = lines[i];
    if (line.trim() === '' || line.trimStart().startsWith('#')) continue;
    const here = line.length - line.trimStart().length;
    if (here <= indent) break; // next service

    if (/^\s*ports:\s*$/.test(line)) {
      inPorts = true;
      continue;
    }
    if (inPorts) {
      const item = /^\s*-\s*"?([0-9]+):([0-9]+)"?/.exec(line);
      if (item) {
        ports.push(Number(item[1]));
        continue;
      }
      if (/^\s*\w[\w.-]*:/.test(line)) inPorts = false;
    }
  }

  return ports;
}

function run(ctx) {
  const findings = [];
  const { repoRoot, manifest } = ctx;

  for (const s of manifest.services) {
    // --- 8: declared contract state against the filesystem --------------------------
    for (const c of contractsOf(manifest, s)) {
      const exists = fs.existsSync(path.join(repoRoot, c.path));
      if (c.state === 'written' && !exists) {
        findings.push(
          warning(8, `${s.id}: contract '${c.path}' is declared written but is not in this checkout`)
        );
      }
      if (c.state === 'todo' && exists) {
        findings.push(
          warning(8, `${s.id}: contract '${c.path}' is declared todo but already exists`)
        );
      }
    }

    // --- 9: the exchange the manifest claims appears in the publisher's own source ---
    const root = sourceRootOf(s);
    if (root) {
      const absolute = path.join(repoRoot, root);
      const exchanges = new Set((s.publishes || []).map((p) => p.exchange).filter(Boolean));
      for (const exchange of exchanges) {
        if (!treeContains(absolute, exchange)) {
          findings.push(
            warning(9, `${s.id}: exchange '${exchange}' does not appear anywhere in ${root}`)
          );
        }
      }
    }

    // --- 10: declared port against the compose file ----------------------------------
    // A service is allowed not to have a port yet, which is the normal state here.
    if (!s.ports || !s.ports.http || !s.compose) continue;

    const composePath = path.join(repoRoot, s.compose.file);
    let composeText;
    try {
      composeText = fs.readFileSync(composePath, 'utf8');
    } catch {
      findings.push(warning(10, `${s.id}: compose file '${s.compose.file}' cannot be read`));
      continue;
    }

    const hostPorts = publishedHostPorts(composeText, s.compose.service);
    if (hostPorts === null) {
      findings.push(
        warning(10, `${s.id}: '${s.compose.service}' is not a service in ${s.compose.file}`)
      );
      continue;
    }
    if (!hostPorts.includes(s.ports.http)) {
      findings.push(
        warning(
          10,
          `${s.id}: manifest says http ${s.ports.http}, ${s.compose.file} publishes ${
            hostPorts.join(', ') || 'nothing'
          }`
        )
      );
    }
  }

  return findings;
}

module.exports = { run, publishedHostPorts };
