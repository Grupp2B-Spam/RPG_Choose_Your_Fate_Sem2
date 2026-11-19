'use strict';

/**
 * Putting rendered content into the template.
 *
 * This is text substitution on the raw template string, never parse-and-reserialise.
 * Round-tripping a .drawio file through an XML library rewrites namespaces and attribute
 * order, which has corrupted these files before.
 *
 * A token must occupy a whole attribute value: `value="{{service:x:card}}"`. Anything
 * else — a token inside a sentence, a token in some other attribute — is reported as
 * unresolved, so the rule fails loudly instead of producing a half-substituted label.
 */

const WHOLE_VALUE = /value="\{\{([^"{}]+)\}\}"/g;
const ANY_LEFTOVER = /\{\{([^{}]*)\}\}/g;

/** Escape for use inside an XML attribute delimited by double quotes. */
function escapeForAttribute(html) {
  return String(html)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

/**
 * @param {string} text the template
 * @param {(token: string) => string|null} resolve
 * @returns {{ text: string, resolved: string[], unresolved: string[] }}
 */
function spliceTokens(text, resolve) {
  const resolved = [];

  const out = text.replace(WHOLE_VALUE, (match, token) => {
    const html = resolve(token);
    if (html == null) return match; // left in place; the leftover scan below reports it
    resolved.push(token);
    return `value="${escapeForAttribute(html)}"`;
  });

  const unresolved = [];
  for (const m of out.matchAll(ANY_LEFTOVER)) unresolved.push(m[1]);

  return { text: out, resolved, unresolved };
}

module.exports = { spliceTokens, escapeForAttribute };
