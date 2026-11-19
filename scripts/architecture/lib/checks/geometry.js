'use strict';

/**
 * Checks 6 and 7 — the two things that most decide whether the result is readable.
 *
 * Overlap is an error: two boxes on top of each other is a broken drawing, not a matter
 * of taste. A shared edge corridor is a warning: it is usually wrong, but a short edge
 * that happens to pass near another is sometimes fine, and an error would make the
 * check something people route around.
 */

const CORRIDOR_TOLERANCE = 20;

const error = (check, message) => ({ check, severity: 'error', message });
const warning = (check, message) => ({ check, severity: 'warning', message });

/**
 * Strict overlap: boxes that share only a boundary do not overlap. Butting two boxes up
 * against each other is a normal thing to do in a diagram.
 */
function overlaps(a, b) {
  return (
    a.x < b.x + b.width &&
    b.x < a.x + a.width &&
    a.y < b.y + b.height &&
    b.y < a.y + a.height
  );
}

function run(ctx) {
  const findings = [];

  for (const tab of ctx.scan.tabs) {
    // --- 6: no two siblings overlap -------------------------------------------------
    // Scoped to a shared parent on purpose. draw.io geometry is relative to the parent,
    // so cells with different parents are not in the same coordinate space — and a
    // status badge inside its service box, or a service box inside its bounded-context
    // group, is correct nesting that an unscoped rule would flag on every element.
    const boxes = tab.cells.filter((c) => c.vertex && c.geometry);
    const byParent = new Map();
    for (const c of boxes) {
      if (!byParent.has(c.parent)) byParent.set(c.parent, []);
      byParent.get(c.parent).push(c);
    }

    for (const siblings of byParent.values()) {
      for (let i = 0; i < siblings.length; i += 1) {
        for (let j = i + 1; j < siblings.length; j += 1) {
          if (overlaps(siblings[i].geometry, siblings[j].geometry)) {
            findings.push(
              error(6, `${tab.id}: '${siblings[i].id}' and '${siblings[j].id}' overlap`)
            );
          }
        }
      }
    }

    // --- 7: each edge gets its own vertical corridor ---------------------------------
    const corridors = [];
    for (const e of tab.cells.filter((c) => c.edge && c.points.length > 0)) {
      for (const p of e.points) corridors.push({ id: e.id, x: p.x });
    }

    for (let i = 0; i < corridors.length; i += 1) {
      for (let j = i + 1; j < corridors.length; j += 1) {
        if (corridors[i].id === corridors[j].id) continue;
        if (Math.abs(corridors[i].x - corridors[j].x) <= CORRIDOR_TOLERANCE) {
          findings.push(
            warning(
              7,
              `${tab.id}: edges '${corridors[i].id}' and '${corridors[j].id}' share the corridor at x≈${corridors[i].x}`
            )
          );
        }
      }
    }
  }

  return findings;
}

module.exports = { run, CORRIDOR_TOLERANCE };
