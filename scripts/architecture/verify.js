'use strict';

/**
 * Checks the diagram against itself and against the repository.
 *
 *   node scripts/architecture/verify.js
 *
 * Errors exit non-zero; warnings are printed and do not fail. The split is deliberate:
 * most of this system is still on unmerged branches or unwritten, so the checks that
 * depend on files being present would otherwise keep the build red for weeks, and a
 * check that is always red is a check people switch off.
 */

const fs = require('node:fs');
const path = require('node:path');

const { loadManifest } = require('./lib/manifest.js');
const { createResolver } = require('./lib/render.js');
const { scan } = require('./lib/drawio.js');
const { build, TEMPLATE_RELATIVE, MANIFEST_RELATIVE, OUTPUT_RELATIVE } = require('./build.js');

const structure = require('./lib/checks/structure.js');
const geometry = require('./lib/checks/geometry.js');
const repo = require('./lib/checks/repo.js');

const SEVERITY_ORDER = { error: 0, warning: 1, info: 2 };

/**
 * @param {{ repoRoot: string }} options
 * @returns {{ findings: Array<{check:number,severity:string,message:string}>, ok: boolean }}
 */
function verify({ repoRoot }) {
  const findings = [];

  const manifest = loadManifest(path.join(repoRoot, MANIFEST_RELATIVE));
  const template = fs
    .readFileSync(path.join(repoRoot, TEMPLATE_RELATIVE), 'utf8')
    .replace(/\r\n/g, '\n');
  const resolve = createResolver(manifest);

  // --- 1: the committed file is what the template and manifest produce ---------------
  let fresh = null;
  try {
    fresh = build({ repoRoot }).xml;
  } catch (e) {
    // An unresolved token stops the build entirely. Report it as check 3 and carry on
    // with whatever can still be checked, rather than letting the throw escape.
    findings.push({ check: 3, severity: 'error', message: e.message });
  }

  const outputPath = path.join(repoRoot, OUTPUT_RELATIVE);
  let committed = null;
  try {
    committed = fs.readFileSync(outputPath, 'utf8');
  } catch {
    findings.push({
      check: 1,
      severity: 'error',
      message: `${OUTPUT_RELATIVE} is missing; run node scripts/architecture/build.js`,
    });
  }

  if (fresh !== null && committed !== null && fresh !== committed) {
    findings.push({
      check: 1,
      severity: 'error',
      message:
        `${OUTPUT_RELATIVE} differs from a fresh render. It is generated: edit the ` +
        'template or the manifest and run node scripts/architecture/build.js.',
    });
  }

  // Everything below describes the file as it should be, so it runs against the fresh
  // render when there is one — a hand-edit is already reported above, and checking the
  // edited copy would bury that under a second wave of findings.
  const xml = fresh !== null ? fresh : committed;
  if (xml !== null) {
    const ctx = { repoRoot, manifest, template, xml, scan: scan(xml), resolve };
    findings.push(...structure.run(ctx));
    findings.push(...geometry.run(ctx));
    findings.push(...repo.run(ctx));
  }

  findings.sort((a, b) => SEVERITY_ORDER[a.severity] - SEVERITY_ORDER[b.severity]);
  return { findings, ok: !findings.some((f) => f.severity === 'error') };
}

function main() {
  const repoRoot = path.resolve(__dirname, '..', '..');
  const { findings, ok } = verify({ repoRoot });

  for (const f of findings) {
    process.stdout.write(`${f.severity.toUpperCase().padEnd(7)} check ${f.check}: ${f.message}\n`);
  }

  const errors = findings.filter((f) => f.severity === 'error').length;
  const warnings = findings.filter((f) => f.severity === 'warning').length;
  process.stdout.write(`\n${errors} error(s), ${warnings} warning(s)\n`);

  process.exit(ok ? 0 : 1);
}

if (require.main === module) main();

module.exports = { verify };
