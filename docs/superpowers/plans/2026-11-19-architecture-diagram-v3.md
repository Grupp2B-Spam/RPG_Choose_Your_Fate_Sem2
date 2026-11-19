# Architecture Diagram v3 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a seven-tab draw.io architecture diagram, generated from a hand-authored template plus a machine-checkable manifest, so that service versions, API contracts and communication styles stay current as the system evolves.

**Architecture:** One hand-authored template (`architecture.template.drawio`) holds the layout and opens in draw.io. One JSON manifest holds everything that changes often. `build.js` splices manifest content into template tokens by text substitution — never by parsing and re-serialising XML — and writes `architecture.drawio`. `verify.js` re-renders, compares byte-for-byte, and checks the manifest against the repository.

**Tech Stack:** Node 22 (`node:*` builtins only), `node:test` + `node:assert/strict`, draw.io mxGraph XML, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-11-18-architecture-diagram-v3-design.md`

## Global Constraints

- **Zero npm dependencies.** Only Node builtins. No `package.json` is added.
- **Node 22**, already installed. Tests run with `node --test scripts/architecture/test/`.
- **Never parse and re-serialise draw.io XML.** All transformation is text splicing on the raw string. Reading for inspection uses the read-only scanner in `lib/drawio.js`.
- **The generated file is written with `\n` line endings**, and the template is normalised to `\n` on read.
- **English only.** One template, one output file, no translation layer. (Changed after the plan was first written; Task 5 is gone and the numbering keeps its gap so cross-references still hold.)
- **Tokens occupy a whole attribute value**: the splicer matches `value="{{...}}"` and nothing else. A `{{` surviving anywhere else is an error.
- **Service ids are lowercase-hyphen**, matching `kubernetes-konventioner.md` §5.
- **Vocabulary:** `kind` is `service | legacy | infrastructure`; `status` is `planned | skeleton | implemented`.
- **Severity:** checks 1–6 are errors, 7–11 warnings, 12 info. Errors exit non-zero; warnings do not.
- Every commit message ends with `Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>`.
- Paths in this plan are relative to the repository root.

## Review Focus

Four failure modes the spec implies but no task's own tests would otherwise exercise. Each has a test attached to the task that owns the code. (A fifth, about translation-table collisions, went with the Danish file.)

1. **A token naming an id that is not in the manifest** (`{{service:scene-mangager:card}}`) must fail the build, naming the token. Rendering it empty would ship a blank box that looks deliberate. → Task 4.
2. **Manifest prose containing `&`, `<` or `"`** must be escaped exactly once for an XML attribute. Double-escaping yields a visible `&amp;amp;`; no escaping yields a file draw.io refuses to open. → Task 4.
3. **A service with no `ports`**, which the spec explicitly permits, must make check 10 skip rather than crash or report a false mismatch. Most services will be in this state for weeks. → Task 9.
4. **A template saved from draw.io on Windows, arriving with CRLF**, must not make the drift check fail. Every group member is on Windows; this would otherwise fire on the first save. → Task 6.

---

## File Structure

```
docs/architecture/
  architecture.json              manifest (source)
  architecture.template.drawio   layout (source)
  architecture.drawio            generated
  README.md                      how to change the diagram
scripts/architecture/
  build.js                       CLI: render the output
  verify.js                      CLI: all checks, exit 1 on error
  lib/
    manifest.js                  load + validate
    render.js                    token -> HTML string
    splice.js                    token splicing + attribute escaping
    drawio.js                    read-only scanner
    checks/structure.js          checks 2, 3, 4, 5, 11
    checks/geometry.js           checks 6, 7
    checks/repo.js               checks 8, 9, 10
  test/
    *.test.js
```

---

### Task 1: Manifest loader and validator

**Files:**
- Create: `scripts/architecture/lib/manifest.js`
- Test: `scripts/architecture/test/manifest.test.js`

**Interfaces:**
- Consumes: nothing.
- Produces:
  - `validateManifest(obj) -> string[]` — list of problem descriptions, empty when valid.
  - `loadManifest(absPath) -> object` — reads JSON, throws `Error` whose `.problems` is the array above when non-empty.

- [ ] **Step 1: Write the failing tests**

```js
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { validateManifest } from '../lib/manifest.js';

test('accepts a minimal valid manifest', () => {
  assert.deepEqual(validateManifest(minimal()), []);
});

test('rejects a service id that is not lowercase-hyphen', () => {
  const m = minimal(); m.services[0].id = 'RecapManager';
  assert.match(validateManifest(m).join('\n'), /RecapManager.*lowercase-hyphen/);
});

test('rejects a duplicate service id', () => {
  const m = minimal(); m.services.push({ ...m.services[0] });
  assert.match(validateManifest(m).join('\n'), /duplicate service id 'recap-manager'/);
});

test('rejects an unknown status', () => {
  const m = minimal(); m.services[0].status = 'nearly';
  assert.match(validateManifest(m).join('\n'), /status 'nearly'/);
});

test('rejects an unknown kind', () => {
  const m = minimal(); m.services[0].kind = 'thing';
  assert.match(validateManifest(m).join('\n'), /kind 'thing'/);
});

test('rejects an event naming a consumer that is not a service', () => {
  const m = minimal(); m.events[0].consumers = ['ghost-manager'];
  assert.match(validateManifest(m).join('\n'), /'ghost-manager'/);
});

test('rejects an event whose publisher is not a service', () => {
  const m = minimal(); m.events[0].publisher = 'ghost-manager';
  assert.match(validateManifest(m).join('\n'), /publisher 'ghost-manager'/);
});

test('accepts a service with no ports', () => {
  const m = minimal(); delete m.services[0].ports;
  assert.deepEqual(validateManifest(m), []);
});

test('rejects a legacy node that declares a context', () => {
  const m = minimal();
  m.services.push({ id: 'choose-your-fate-backend', kind: 'legacy', display: 'Monolith',
                    status: 'implemented', context: 'state', purpose: 'x' });
  assert.match(validateManifest(m).join('\n'), /legacy.*context/);
});
```

`minimal()` is a local helper returning one `service`, one `event` referencing it, and a `meta` block.

- [ ] **Step 2: Run tests to verify they fail**

Run: `node --test scripts/architecture/test/manifest.test.js`
Expected: FAIL, cannot find module `../lib/manifest.js`

- [ ] **Step 3: Implement `validateManifest` and `loadManifest` in `scripts/architecture/lib/manifest.js`**

Validation rules, in this order: `meta.version`, `meta.updated` and `meta.broker` present; each service has `id`, `kind`, `display`, `status`, `purpose`; `id` matches `/^[a-z][a-z0-9-]*$/`; ids unique; `kind` and `status` from the vocabularies in Global Constraints; `context` is one of `identity | content | state | media` and is **forbidden** when `kind` is `legacy`; every `events[].publisher` and every entry of `events[].consumers` is a known service id; every `consumes[].event` names a known event. `ports`, `datastore`, `compose`, `branch`, `adr`, `gaps`, `deployments` and `replacedBy` are optional.

- [ ] **Step 4: Run tests to verify they pass**

Run: `node --test scripts/architecture/test/manifest.test.js`
Expected: PASS, 9 tests

- [ ] **Step 5: Commit**

```bash
git add scripts/architecture/lib/manifest.js scripts/architecture/test/manifest.test.js
git commit -m "feat(architecture): manifest loader and validator"
```

---

### Task 2: The manifest itself

**Files:**
- Create: `docs/architecture/architecture.json`
- Test: `scripts/architecture/test/architecture-json.test.js`

**Interfaces:**
- Consumes: `loadManifest` from Task 1.
- Produces: the manifest every later task renders from.

Facts to record, from the spec §5 and the repository. The branches are treated as merged (spec §8.1).

| id | kind | status | branch | stack | datastore | ports |
|---|---|---|---|---|---|---|
| `account-manager` | service | planned | — | — | account vault, MySQL | omit |
| `storycontent-manager` | service | skeleton | `patrick/create-storycontentmanager-microservice` | Spring Boot 4.0.3 / Java 21 | `story-content-vault`, MySQL 9.5, host 3309 | http 8081 |
| `scene-manager` | service | skeleton | `SceneManager` | — | scene read model, MongoDB | omit |
| `character-manager` | service | skeleton | `mikolsen/create-charactermanager-microservice` | — | character vault, MySQL | omit |
| `storypath-manager` | service | planned | — | — | storypath vault, MySQL | grpc 9091 |
| `recap-manager` | service | implemented | `main` | Spring Boot 4.0.3 / Java 21 | `recap-vault`, MySQL 9.5, host 3307 | http 8090 |
| `texttospeech-manager` | service | planned | — | Quarkus, native (ADR-009) | audio store, MinIO | omit |
| `choose-your-fate-backend` | legacy | implemented | `main` | Spring Boot 4.0.3 / Java 21 | monolith database, MySQL | omit |

`scene-manager` carries `deployments: [{ "name": "scene-indexer", "role": "projection" }]` (ADR-010). `choose-your-fate-backend` carries `replacedBy` listing all seven service ids, and no `context`.

Events, from `microservices/contracts/README.md`:

| event | exchange | publisher | consumers (queue, scalable) |
|---|---|---|---|
| `scene-content.updated` | `storycontent.events` | `storycontent-manager` | `scene-manager` (`scenemanager.scene-content`, true) |
| `item-definition.updated` | `storycontent.events` | `storycontent-manager` | `character-manager` (`charactermanager.item-rules`, true) |
| `player-choice.made` | `scenemanager.events` | `scene-manager` | `storypath-manager` (`storypathmanager.player-choice`, **false**), `character-manager` (`charactermanager.player-choice`, true) |
| `recap-generated` | `recap.events` | `recap-manager` | `texttospeech-manager` (`texttospeechmanager.recap`, true) |
| `account-deletion-requested` | `account.events` | `account-manager` | every data-owning service |

`storypath-manager`'s `scalable: false` carries `"why": "The path is an ordered log (ADR-011)."`

Infrastructure: `rabbitmq` (`rabbitmq:4-management-alpine`, 5672/15672), `minio`, `ollama` (external, 11434), `elevenlabs` (external, paid per call).

Open issues, from spec §10: ADR-013 sequence assignment (blocks `storypath-manager`), GraphQL owner (blocks `scene-manager`, `character-manager`), OAuth provider (blocks `account-manager`), KEDA ScaledJob (blocks `texttospeech-manager`), port allocation (blocks none, `meta`-level).

- [ ] **Step 1: Write the failing tests**

```js
test('the real manifest is valid', () => {
  assert.deepEqual(validateManifest(loadManifest(MANIFEST)), []);
});

test('records seven services and one legacy node', () => {
  const m = loadManifest(MANIFEST);
  assert.equal(m.services.filter(s => s.kind === 'service').length, 7);
  assert.equal(m.services.filter(s => s.kind === 'legacy').length, 1);
});

test('recap-manager is the only implemented service', () => {
  const m = loadManifest(MANIFEST);
  const done = m.services.filter(s => s.kind === 'service' && s.status === 'implemented');
  assert.deepEqual(done.map(s => s.id), ['recap-manager']);
});

test('no service invents a port it has not been given', () => {
  const m = loadManifest(MANIFEST);
  const withPorts = m.services.filter(s => s.ports).map(s => s.id).sort();
  assert.deepEqual(withPorts, ['recap-manager', 'storycontent-manager', 'storypath-manager']);
});

test('scene-indexer is a deployment of scene-manager, not a service', () => {
  const m = loadManifest(MANIFEST);
  assert.equal(m.services.some(s => s.id === 'scene-indexer'), false);
  const sm = m.services.find(s => s.id === 'scene-manager');
  assert.deepEqual(sm.deployments.map(d => d.name), ['scene-indexer']);
});

test('player-choice.made is scalable for character-manager but not storypath-manager', () => {
  const m = loadManifest(MANIFEST);
  const byId = id => m.services.find(s => s.id === id)
                      .consumes.find(c => c.event === 'player-choice.made');
  assert.equal(byId('character-manager').scalable, true);
  assert.equal(byId('storypath-manager').scalable, false);
});

test('the monolith declares no bounded context and names what replaces it', () => {
  const m = loadManifest(MANIFEST);
  const mono = m.services.find(s => s.kind === 'legacy');
  assert.equal(mono.context, undefined);
  assert.equal(mono.replacedBy.length, 7);
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `node --test scripts/architecture/test/architecture-json.test.js`
Expected: FAIL, `docs/architecture/architecture.json` does not exist

- [ ] **Step 3: Write `docs/architecture/architecture.json`**

Use the tables above. Contract entries: `storycontent-manager.asyncapi.yaml` is `state: "written"`; the `.proto` is `state: "written"` with the note from spec §7.3 about living in the caller's tree; every other contract is `state: "todo"`. `recap-manager` exposes REST at `/choose-your-fate/ai` with `"versioning": "X-API-VERSION header"`. `compose` blocks for `recap-manager` and `storycontent-manager` only.

- [ ] **Step 4: Run tests to verify they pass**

Run: `node --test scripts/architecture/test/architecture-json.test.js`
Expected: PASS, 7 tests

- [ ] **Step 5: Commit**

```bash
git add docs/architecture/architecture.json scripts/architecture/test/architecture-json.test.js
git commit -m "feat(architecture): record the system in architecture.json"
```

---

### Task 3: Token rendering

**Files:**
- Create: `scripts/architecture/lib/render.js`
- Test: `scripts/architecture/test/render.test.js`

**Interfaces:**
- Consumes: a manifest object (Task 1).
- Produces: `createResolver(manifest) -> (token: string) => string | null` — returns an **unescaped HTML fragment**, or `null` when the token names something the manifest does not contain.

Tokens, per spec §7.4: `service:<id>:card`, `service:<id>:apis`, `service:<id>:events`, `service:<id>:contracts`, `event:<name>:label`, `table:contracts`, `meta:updated`.

- [ ] **Step 1: Write the failing tests**

```js
test('card carries display name, purpose and version badge', () => {
  const html = resolve('service:recap-manager:card');
  assert.match(html, /RecapManager/);
  assert.match(html, /0\.0\.1-SNAPSHOT/);
});

test('card of a planned service says planned and shows no version', () => {
  const html = resolve('service:account-manager:card');
  assert.match(html, /planned/);
  assert.doesNotMatch(html, /SNAPSHOT/);
});

test('card of a branch service names the branch', () => {
  assert.match(resolve('service:storycontent-manager:card'), /patrick\/create-storycontentmanager-microservice/);
});

test('card of the legacy node says it is being decomposed', () => {
  assert.match(resolve('service:choose-your-fate-backend:card'), /decomposed/);
});

test('apis lists one line per exposed style', () => {
  assert.match(resolve('service:recap-manager:apis'), /REST.*\/choose-your-fate\/ai/s);
});

test('apis of a service exposing nothing yet says so', () => {
  assert.match(resolve('service:account-manager:apis'), /none yet/);
});

test('events separates published from consumed', () => {
  const html = resolve('service:scene-manager:events');
  assert.match(html, /player-choice\.made/);
  assert.match(html, /scene-content\.updated/);
});

test('contracts marks a written contract differently from a todo', () => {
  const written = resolve('service:storycontent-manager:contracts');
  const todo = resolve('service:character-manager:contracts');
  assert.notEqual(written, todo);
  assert.match(written, /storycontent-manager\.asyncapi\.yaml/);
});

test('event label carries exchange and queue count', () => {
  const html = resolve('event:player-choice.made:label');
  assert.match(html, /scenemanager\.events/);
  assert.match(html, /2/);
});

test('contract table has one row per service plus a header', () => {
  const rows = resolve('table:contracts').match(/<tr/g);
  assert.equal(rows.length, 9);
});

test('meta:updated renders the manifest date', () => {
  assert.equal(resolve('meta:updated'), '2026-11-18');
});

test('an unknown id returns null rather than an empty string', () => {
  assert.equal(resolve('service:scene-mangager:card'), null);
});

test('an unknown token kind returns null', () => {
  assert.equal(resolve('service:recap-manager:colour'), null);
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `node --test scripts/architecture/test/render.test.js`
Expected: FAIL, cannot find module `../lib/render.js`

- [ ] **Step 3: Implement `createResolver` in `scripts/architecture/lib/render.js`**

Split the token on `:` into kind, id and field. Return `null` for an unknown kind, an unknown id, or an unknown field — never a fallback string. Use `<br>` between lines and `<b>` for the service name, matching how v2 stores rich values. The contract table is a `<table>` with a header row and one row per entry of `services`, columns: service, version, status, stack, exposed APIs, publishes, consumes, contract, ADRs.

- [ ] **Step 4: Run tests to verify they pass**

Run: `node --test scripts/architecture/test/render.test.js`
Expected: PASS, 13 tests

- [ ] **Step 5: Commit**

```bash
git add scripts/architecture/lib/render.js scripts/architecture/test/render.test.js
git commit -m "feat(architecture): render manifest content as diagram tokens"
```

---

### Task 4: Token splicing and attribute escaping

**Files:**
- Create: `scripts/architecture/lib/splice.js`
- Test: `scripts/architecture/test/splice.test.js`

**Interfaces:**
- Consumes: a resolver (Task 3).
- Produces:
  - `escapeForAttribute(html: string) -> string`
  - `spliceTokens(text: string, resolve: (token: string) => string|null) -> { text: string, resolved: string[], unresolved: string[] }`

- [ ] **Step 1: Write the failing tests**

```js
test('replaces a whole-value token with the escaped fragment', () => {
  const out = spliceTokens('<mxCell value="{{a}}" />', () => '<b>Hi</b>');
  assert.equal(out.text, '<mxCell value="&lt;b&gt;Hi&lt;/b&gt;" />');
  assert.deepEqual(out.resolved, ['a']);
});

test('escapes ampersand, angle brackets and quotes exactly once', () => {
  const out = spliceTokens('<mxCell value="{{a}}" />', () => 'Tom & "Jerry" <x>');
  assert.equal(out.text, '<mxCell value="Tom &amp; &quot;Jerry&quot; &lt;x&gt;" />');
  assert.doesNotMatch(out.text, /&amp;amp;/);
});

test('reports an unresolved token and leaves it in place', () => {
  const out = spliceTokens('<mxCell value="{{ghost}}" />', () => null);
  assert.deepEqual(out.unresolved, ['ghost']);
  assert.match(out.text, /\{\{ghost\}\}/);
});

test('a token outside a whole value is reported as unresolved', () => {
  const out = spliceTokens('<mxCell value="see {{a}}" />', () => 'X');
  assert.deepEqual(out.resolved, []);
  assert.equal(out.unresolved.length, 1);
});

test('leaves cells without tokens untouched', () => {
  const src = '<mxCell value="Player client" style="x" />';
  assert.equal(spliceTokens(src, () => 'X').text, src);
});

test('replaces every occurrence of the same token', () => {
  const out = spliceTokens('<a value="{{a}}"/><b value="{{a}}"/>', () => 'X');
  assert.equal(out.resolved.length, 2);
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `node --test scripts/architecture/test/splice.test.js`
Expected: FAIL, cannot find module `../lib/splice.js`

- [ ] **Step 3: Implement `escapeForAttribute` and `spliceTokens` in `scripts/architecture/lib/splice.js`**

`escapeForAttribute` replaces `&` first, then `<`, `>`, `"`. `spliceTokens` matches `/value="\{\{([^"]+)\}\}"/g` for replacement, then scans the result for any remaining `{{...}}` and reports each as unresolved — that is what makes Review Focus 1 and the whole-value rule both fail loudly.

- [ ] **Step 4: Run tests to verify they pass**

Run: `node --test scripts/architecture/test/splice.test.js`
Expected: PASS, 6 tests

- [ ] **Step 5: Commit**

```bash
git add scripts/architecture/lib/splice.js scripts/architecture/test/splice.test.js
git commit -m "feat(architecture): splice tokens into the template"
```

---

### Task 5: *(removed)*

The translation pass and `i18n.json` were dropped when the diagram became English only.
Task numbers after this point are unchanged so that the cross-references in tasks 6 to 16
keep pointing at the right tasks.

---

### Task 6: The build command

**Files:**
- Create: `scripts/architecture/build.js`
- Modify: `.gitattributes`
- Test: `scripts/architecture/test/build.test.js`

**Interfaces:**
- Consumes: `loadManifest`, `createResolver`, `spliceTokens`.
- Produces: `build({ repoRoot }) -> { xml: string, stats: { services, resolved } }`, plus a CLI entry point that writes `docs/architecture/architecture.drawio`.

- [ ] **Step 1: Write the failing tests**

```js
test('renders the fixture template', () => {
  assert.match(build({ repoRoot: FIXTURE }).xml, /value="Player client"/);
});

test('the output carries the generated banner', () => {
  assert.match(build({ repoRoot: FIXTURE }).xml, /^<\?xml[^>]*\?>\n<!-- GENERATED/);
});

test('output uses LF only', () => {
  assert.doesNotMatch(build({ repoRoot: FIXTURE }).xml, /\r/);
});

test('a CRLF template produces the same bytes as an LF one', () => {
  assert.equal(build({ repoRoot: FIXTURE_CRLF }).xml, build({ repoRoot: FIXTURE }).xml);
});

test('an unresolved token throws and names it', () => {
  assert.throws(() => build({ repoRoot: FIXTURE_BAD_TOKEN }), /scene-mangager/);
});

test('stats report what was resolved', () => {
  const { stats } = build({ repoRoot: FIXTURE });
  assert.ok(stats.resolved > 0);
  assert.ok(stats.services > 0);
});
```

Fixtures are small template and manifest files under `scripts/architecture/test/fixtures/`.

- [ ] **Step 2: Run tests to verify they fail**

Run: `node --test scripts/architecture/test/build.test.js`
Expected: FAIL, cannot find module `../build.js`

- [ ] **Step 3: Implement `build` and the CLI in `scripts/architecture/build.js`, and add `*.drawio text eol=lf` to `.gitattributes`**

Read the template, replace `\r\n` with `\n` immediately — that is Review Focus 4 and it must happen before anything else. Splice, then throw an `Error` listing `unresolved` when non-empty. Insert the banner after the XML declaration. Write with `fs.writeFileSync(path, text)` where `text` already ends in `\n`.

- [ ] **Step 4: Run tests to verify they pass**

Run: `node --test scripts/architecture/test/build.test.js`
Expected: PASS, 6 tests

- [ ] **Step 5: Commit**

```bash
git add scripts/architecture/build.js scripts/architecture/test/ .gitattributes
git commit -m "feat(architecture): build command"
```

---

### Task 7: Read-only draw.io scanner and structural checks

**Files:**
- Create: `scripts/architecture/lib/drawio.js`, `scripts/architecture/lib/checks/structure.js`
- Test: `scripts/architecture/test/structure.test.js`

**Interfaces:**
- Consumes: output XML text, the manifest, the template text.
- Produces:
  - `scan(xmlText) -> { tabs: [{ name, id, cells: [{ id, parent, value, style, vertex, edge, source, target, geometry, points }] }] }`
  - `run(ctx) -> Finding[]` where `Finding = { check: number, severity, message }` and `ctx = { repoRoot, manifest, template, outputs, scans }`

Checks 2 (well-formed, expected tabs), 3 (every token resolves), 4 (no orphan services), 5 (id convention), 11 (every event has a consumer and every consumer names a queue).

- [ ] **Step 1: Write the failing tests**

```js
test('scan finds tabs, cells and geometry', () => {
  const s = scan(SAMPLE);
  assert.equal(s.tabs.length, 2);
  assert.equal(s.tabs[0].cells.find(c => c.id === 'a').geometry.width, 180);
});

test('scan distinguishes edges from vertices', () => {
  const s = scan(SAMPLE);
  assert.equal(s.tabs[0].cells.find(c => c.id === 'e1').edge, true);
});

test('check 2 fails on unbalanced XML', () => {
  assert.ok(findings(ctxWith({ en: '<mxfile><diagram>' })).some(f => f.check === 2));
});

test('check 2 fails when a tab is missing', () => {
  assert.ok(findings(ctxWithSixTabs()).some(f => f.check === 2 && /seven/.test(f.message)));
});

test('check 3 fails on a surviving token', () => {
  assert.ok(findings(ctxWith({ en: '<mxCell value="{{x}}"/>' })).some(f => f.check === 3));
});

test('check 4 names a service no token mentions', () => {
  const f = findings(ctxWithOrphan('texttospeech-manager'));
  assert.match(f.find(x => x.check === 4).message, /texttospeech-manager/);
});

test('check 5 rejects an id that is not lowercase-hyphen', () => {
  assert.ok(findings(ctxWithBadId()).some(f => f.check === 5));
});

test('check 11 warns about an event with no consumers', () => {
  const f = findings(ctxWithLonelyEvent());
  assert.equal(f.find(x => x.check === 11).severity, 'warning');
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `node --test scripts/architecture/test/structure.test.js`
Expected: FAIL, cannot find module `../lib/drawio.js`

- [ ] **Step 3: Implement `scan` and the structural checks**

`scan` is regex-based and read-only: never reconstruct XML from it. Check 2's well-formedness test is a tag-balance count plus the presence of seven `<diagram>` elements with the expected ids.

- [ ] **Step 4: Run tests to verify they pass**

Run: `node --test scripts/architecture/test/structure.test.js`
Expected: PASS, 8 tests

- [ ] **Step 5: Commit**

```bash
git add scripts/architecture/lib/drawio.js scripts/architecture/lib/checks/structure.js scripts/architecture/test/structure.test.js
git commit -m "feat(architecture): draw.io scanner and structural checks"
```

---

### Task 8: Geometry checks

**Files:**
- Create: `scripts/architecture/lib/checks/geometry.js`
- Test: `scripts/architecture/test/geometry.test.js`

**Interfaces:**
- Consumes: `scan` output (Task 7).
- Produces: `run(ctx) -> Finding[]` for check 6 (sibling overlap, error) and check 7 (shared edge corridor, warning).

- [ ] **Step 1: Write the failing tests**

```js
test('check 6 reports two overlapping siblings', () => {
  assert.ok(run(ctxBoxes([{id:'a',parent:'1',x:0,y:0,w:100,h:50},
                          {id:'b',parent:'1',x:50,y:10,w:100,h:50}]))
            .some(f => f.check === 6 && f.severity === 'error'));
});

test('check 6 ignores a child nested inside its parent', () => {
  assert.deepEqual(run(ctxBoxes([{id:'outer',parent:'1',x:0,y:0,w:400,h:200},
                                 {id:'inner',parent:'outer',x:10,y:10,w:100,h:50}])), []);
});

test('check 6 ignores boxes that merely touch at an edge', () => {
  assert.deepEqual(run(ctxBoxes([{id:'a',parent:'1',x:0,y:0,w:100,h:50},
                                 {id:'b',parent:'1',x:100,y:0,w:100,h:50}])), []);
});

test('check 7 warns when two edges share a vertical corridor', () => {
  const f = run(ctxEdges([{ id:'e1', points:[{x:600,y:100}] },
                          { id:'e2', points:[{x:600,y:300}] }]));
  assert.equal(f.find(x => x.check === 7).severity, 'warning');
});

test('check 7 is quiet when corridors differ', () => {
  assert.deepEqual(run(ctxEdges([{ id:'e1', points:[{x:600,y:100}] },
                                 { id:'e2', points:[{x:640,y:300}] }])), []);
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `node --test scripts/architecture/test/geometry.test.js`
Expected: FAIL, cannot find module `../lib/checks/geometry.js`

- [ ] **Step 3: Implement the geometry checks**

Overlap compares only cells sharing a `parent`, and treats shared boundaries as non-overlapping (strict inequality on both axes). A corridor is the `x` of an edge waypoint; two edges share one when their waypoint `x` values are within 20 units.

- [ ] **Step 4: Run tests to verify they pass**

Run: `node --test scripts/architecture/test/geometry.test.js`
Expected: PASS, 5 tests

- [ ] **Step 5: Commit**

```bash
git add scripts/architecture/lib/checks/geometry.js scripts/architecture/test/geometry.test.js
git commit -m "feat(architecture): overlap and edge-corridor checks"
```

---

### Task 9: Repository checks

**Files:**
- Create: `scripts/architecture/lib/checks/repo.js`
- Test: `scripts/architecture/test/repo.test.js`

**Interfaces:**
- Consumes: the manifest and `repoRoot`.
- Produces: `run(ctx) -> Finding[]` for checks 8 (declared contract state versus disk), 9 (exchange name present in the publisher's source), 10 (`ports.http` versus the compose file).

All three are warnings: the services they describe sit on unmerged branches (spec §5).

- [ ] **Step 1: Write the failing tests**

```js
test('check 8 warns when a contract declared written is absent', () => {
  const f = run(ctxContract({ path: 'nope.yaml', state: 'written' }));
  assert.equal(f.find(x => x.check === 8).severity, 'warning');
});

test('check 8 warns when a contract declared todo already exists', () => {
  assert.ok(run(ctxContract({ path: 'exists.yaml', state: 'todo' })).some(f => f.check === 8));
});

test('check 8 is quiet when declaration and disk agree', () => {
  assert.deepEqual(run(ctxContract({ path: 'exists.yaml', state: 'written' })), []);
});

test('check 9 warns when the exchange name is not in the publisher source', () => {
  assert.ok(run(ctxExchange('ghost.events')).some(f => f.check === 9));
});

test('check 10 warns on a port that disagrees with compose', () => {
  const f = run(ctxCompose({ http: 9999, composePublishes: '8090:8090' }));
  assert.match(f.find(x => x.check === 10).message, /9999/);
});

test('check 10 matches on the host side of a published pair', () => {
  assert.deepEqual(run(ctxCompose({ http: 8090, composePublishes: '8090:8090' })), []);
});

test('check 10 skips a service with no ports', () => {
  assert.deepEqual(run(ctxNoPorts()), []);
});

test('check 10 skips a service with no compose block', () => {
  assert.deepEqual(run(ctxNoCompose()), []);
});

test('a missing compose file is a warning, not a crash', () => {
  const f = run(ctxCompose({ http: 8090, file: 'missing.yml' }));
  assert.equal(f.length, 1);
  assert.equal(f[0].severity, 'warning');
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `node --test scripts/architecture/test/repo.test.js`
Expected: FAIL, cannot find module `../lib/checks/repo.js`

- [ ] **Step 3: Implement the repository checks**

Check 9 greps the publishing service's source tree for the literal exchange string. Check 10 reads `compose.file`, finds the `compose.service` key, and compares `ports.http` against the **host** side of each published `host:container` pair. Absent `ports`, absent `compose`, and an unreadable file each produce a skip or a single warning — never a throw. That is Review Focus 4.

- [ ] **Step 4: Run tests to verify they pass**

Run: `node --test scripts/architecture/test/repo.test.js`
Expected: PASS, 9 tests

- [ ] **Step 5: Commit**

```bash
git add scripts/architecture/lib/checks/repo.js scripts/architecture/test/repo.test.js
git commit -m "feat(architecture): checks against the repository"
```

---

### Task 10: The verify command

**Files:**
- Create: `scripts/architecture/verify.js`
- Test: `scripts/architecture/test/verify.test.js`

**Interfaces:**
- Consumes: every check module, plus `build` (Task 6).
- Produces: `verify({ repoRoot }) -> { findings: Finding[], ok: boolean }`, plus a CLI exiting 1 when any finding has severity `error`.

Adds check 1 (drift: the committed output is byte-identical to a fresh render).

- [ ] **Step 1: Write the failing tests**

```js
test('check 1 passes when the committed output matches a fresh render', () => {
  assert.equal(verify({ repoRoot: FIXTURE_BUILT }).findings.some(f => f.check === 1), false);
});

test('check 1 fails when a committed output was hand-edited', () => {
  const f = verify({ repoRoot: FIXTURE_EDITED }).findings.find(x => x.check === 1);
  assert.equal(f.severity, 'error');
});

test('check 1 fails when an output is missing entirely', () => {
  assert.ok(verify({ repoRoot: FIXTURE_NO_OUTPUT }).findings.some(f => f.check === 1));
});

test('ok is false when any finding is an error', () => {
  assert.equal(verify({ repoRoot: FIXTURE_EDITED }).ok, false);
});

test('ok is true when only warnings are present', () => {
  const r = verify({ repoRoot: FIXTURE_WARNINGS_ONLY });
  assert.ok(r.findings.some(f => f.severity === 'warning'));
  assert.equal(r.ok, true);
});

test('findings are sorted errors first', () => {
  const sev = verify({ repoRoot: FIXTURE_EDITED }).findings.map(f => f.severity);
  assert.deepEqual(sev, [...sev].sort((a, b) => (a === 'error' ? -1 : 1)));
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `node --test scripts/architecture/test/verify.test.js`
Expected: FAIL, cannot find module `../verify.js`

- [ ] **Step 3: Implement `verify` and the CLI in `scripts/architecture/verify.js`**

Run `build` in memory, compare against the files on disk, then run the three check modules. Print findings grouped by severity, errors first. Exit 1 when `ok` is false.

- [ ] **Step 4: Run tests to verify they pass**

Run: `node --test scripts/architecture/test/verify.test.js`
Expected: PASS, 6 tests

- [ ] **Step 5: Commit**

```bash
git add scripts/architecture/verify.js scripts/architecture/test/verify.test.js
git commit -m "feat(architecture): verify command with drift detection"
```

---

### Task 11: Template skeleton and tab 1 — Architecture

**Files:**
- Create: `docs/architecture/architecture.template.drawio`
- Create: `docs/architecture/architecture.drawio` (generated)
- Create: `scripts/architecture/test/helpers.js`
- Test: `scripts/architecture/test/tab-architecture.test.js`

**Interfaces:**
- Consumes: every token from Task 3.
- Produces:
  - the template that tasks 12–15 add tabs to. Tab ids: `arch-main`, `flow-choice`, `flow-recap`, `env-compose`, `env-k8s`, `contracts`, `alt-indexer`.
  - `scripts/architecture/test/helpers.js`, used by tasks 12–15 as well:
    `XML` (built output text), `manifest`, `textOf(tabId)`, `cellsOf(tabId)`,
    `cellOf(serviceId)`, `styleOf(serviceId)`, `edgesOf(tabId)`,
    `strokeColour(cell)`, `isNode(cell)`.

Content per spec §7.8 row 1 and §7.9: four bounded-context groups; eight service boxes, each a `card` cell with two stacked detail cells beneath it carrying `{{service:<id>:apis}}` and `{{service:<id>:events}}`, and a third carrying `{{service:<id>:contracts}}` — the three detail tokens exist for this tab and nothing else renders them. Datastores use the v2 database symbol, RabbitMQ shows one exchange per publisher, the monolith is a grey legacy node with pale unlabelled arrows to its `replacedBy` services, plus the three edge styles, a legend, and open-issue notes anchored to the services they block.

- [ ] **Step 1: Write the failing tests**

```js
test('tab 1 exists with the expected id', () => {
  assert.ok(scan(XML).tabs.some(t => t.id === 'arch-main'));
});

test('every service in the manifest has a box on tab 1', () => {
  const values = cellsOf('arch-main').map(c => c.value).join(' ');
  for (const s of manifest.services) assert.match(values, new RegExp(s.display));
});

test('planned services are drawn with a dashed border', () => {
  for (const s of planned()) assert.match(styleOf(s.id), /dashed=1/);
});

test('implemented services are drawn with a solid border', () => {
  assert.doesNotMatch(styleOf('recap-manager'), /dashed=1/);
});

test('the three communication styles use three distinct stroke colours', () => {
  const colours = new Set(edgesOf('arch-main').map(strokeColour).filter(Boolean));
  assert.ok(colours.has('#d79b00') && colours.has('#6c5ce7') && colours.has('#82b366'));
});

test('the monolith is present and outside every bounded-context group', () => {
  const mono = cellOf('choose-your-fate-backend');
  assert.equal(mono.parent, '1');
});

test('ADR-013 is noted on storypath-manager', () => {
  assert.match(cellsOf('arch-main').map(c => c.value).join(' '), /ADR-013/);
});

test('every service box carries its API, event and contract detail cells', () => {
  const t = textOf('arch-main');
  assert.match(t, /\/choose-your-fate\/ai/);        // apis
  assert.match(t, /player-choice\.made/);            // events
  assert.match(t, /storycontent-manager\.asyncapi\.yaml/); // contracts
});

test('tab 1 is titled in English', () => {
  assert.equal(scan(XML).tabs.find(t => t.id === 'arch-main').name, 'Architecture');
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `node --test scripts/architecture/test/tab-architecture.test.js`
Expected: FAIL, template does not exist

- [ ] **Step 3: Author tab 1 in the template and build**

Lay the four context groups out on a 1169×827 landscape page, following v2's grouping. Give every edge its own vertical corridor as explicit waypoints, nested so the topmost target gets the outermost corridor (spec §7.10). Then run `node scripts/architecture/build.js`.

- [ ] **Step 4: Run the tests and the verifier**

Run: `node --test scripts/architecture/test/tab-architecture.test.js && node scripts/architecture/verify.js`
Expected: tests PASS; verify reports no errors

- [ ] **Step 5: Commit**

```bash
git add docs/architecture/ scripts/architecture/test/helpers.js scripts/architecture/test/tab-architecture.test.js
git commit -m "feat(architecture): template skeleton and architecture tab"
```

---

### Task 12: Tabs 2 and 3 — the two flows

**Files:**
- Modify: `docs/architecture/architecture.template.drawio`, `docs/architecture/architecture.drawio`
- Test: `scripts/architecture/test/tab-flows.test.js`

**Interfaces:**
- Consumes: the template from Task 11.
- Produces: tabs `flow-choice` and `flow-recap`.

Port the lifeline layout from `Mandetory/Mandetory 1/architecture-diagram-v2.drawio` tabs 2 and 3, translating the Danish into English and applying the changes in spec §7.8: the per-character sequence number and ADR-013's open question on tab 2; gRPC replacing REST, the stored checkpoint and `snapshotSequence` pagination on tab 3.

- [ ] **Step 1: Write the failing tests**

```js
test('the choice flow names the per-character sequence number', () => {
  assert.match(textOf('flow-choice'), /sequence/i);
});

test('the choice flow marks ADR-013 as open', () => {
  assert.match(textOf('flow-choice'), /ADR-013/);
});

test('the recap flow calls StoryPathManager over gRPC, not REST', () => {
  const t = textOf('flow-recap');
  assert.match(t, /GetChoicesAfter/);
  assert.doesNotMatch(t, /GET \/storypaths/);
});

test('the recap flow shows the checkpoint and snapshot sequence', () => {
  const t = textOf('flow-recap');
  assert.match(t, /checkpoint/i);
  assert.match(t, /snapshotSequence/);
});

test('the recap flow still explains content-hash idempotence', () => {
  assert.match(textOf('flow-recap'), /sha256|hash/i);
});

test('both flow tabs exist with the expected ids', () => {
  const ids = scan(XML).tabs.map(t => t.id);
  assert.ok(ids.includes('flow-choice') && ids.includes('flow-recap'));
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `node --test scripts/architecture/test/tab-flows.test.js`
Expected: FAIL, tabs do not exist

- [ ] **Step 3: Author tabs 2 and 3 and build**

- [ ] **Step 4: Run the tests and the verifier**

Run: `node --test scripts/architecture/test/tab-flows.test.js && node scripts/architecture/verify.js`
Expected: tests PASS; verify reports no errors

- [ ] **Step 5: Commit**

```bash
git add docs/architecture/ scripts/architecture/test/tab-flows.test.js
git commit -m "feat(architecture): choice and recap flow tabs"
```

---

### Task 13: Tab 4 — docker-compose environment

**Files:**
- Modify: `docs/architecture/architecture.template.drawio`, `docs/architecture/architecture.drawio`
- Test: `scripts/architecture/test/tab-compose.test.js`

**Interfaces:**
- Consumes: the template.
- Produces: tab `env-compose`.

Current state, drawn from the consolidated compose (spec §8.1.4): the root file with its `include:` list, RabbitMQ 4 as `choose-your-fate-rabbitmq` on 5672 and 15672, `recap-manager` on 8090 with `recap-vault` on host 3307, `storycontent-manager` on 8081 with `story-content-vault` on host 3309, the `frontend` on 5173, named volumes, healthchecks and `depends_on` conditions, and the `prod` profile marked where it applies.

- [ ] **Step 1: Write the failing tests**

```js
test('tab 4 shows RabbitMQ 4 under its project container name', () => {
  const t = textOf('env-compose');
  assert.match(t, /4-management-alpine/);
  assert.match(t, /choose-your-fate-rabbitmq/);
});

test('tab 4 does not show the stale broker still on main', () => {
  assert.doesNotMatch(textOf('env-compose'), /bar-scan-rabbitmq|3\.13/);
});

test('tab 4 shows the include structure', () => {
  assert.match(textOf('env-compose'), /include/);
});

test('tab 4 shows host ports for both vaults', () => {
  const t = textOf('env-compose');
  assert.match(t, /3307/);
  assert.match(t, /3309/);
});

test('tab 4 is labelled as current state', () => {
  assert.match(textOf('env-compose'), /current state/i);
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `node --test scripts/architecture/test/tab-compose.test.js`
Expected: FAIL, tab does not exist

- [ ] **Step 3: Author tab 4 and build**

- [ ] **Step 4: Run the tests and the verifier**

Run: `node --test scripts/architecture/test/tab-compose.test.js && node scripts/architecture/verify.js`
Expected: tests PASS; verify reports no errors

- [ ] **Step 5: Commit**

```bash
git add docs/architecture/ scripts/architecture/test/tab-compose.test.js
git commit -m "feat(architecture): docker-compose environment tab"
```

---

### Task 14: Tab 5 — local Kubernetes

**Files:**
- Modify: `docs/architecture/architecture.template.drawio`, `docs/architecture/architecture.drawio`
- Test: `scripts/architecture/test/tab-k8s.test.js`

**Interfaces:**
- Consumes: the template.
- Produces: tab `env-k8s`.

Target state, from `kubernetes-konventioner.md`: a Deployment and Service per microservice using the agreed DNS names, StatefulSets for the datastores, the liveness/readiness split with readiness reflecting broker connectivity, configuration through environment variables, a local registry, and a KEDA ScaledJob for `texttospeech-manager` driven by queue depth. A banner states that nothing here is built. The undecided image-delivery question is drawn as an open issue.

- [ ] **Step 1: Write the failing tests**

```js
test('tab 5 carries a banner saying it is not built', () => {
  assert.match(textOf('env-k8s'), /planned|not yet built/i);
});

test('tab 5 uses the agreed Kubernetes service names', () => {
  const t = textOf('env-k8s');
  for (const id of ['storycontent-manager','scene-manager','character-manager',
                    'storypath-manager','recap-manager','texttospeech-manager',
                    'account-manager']) assert.match(t, new RegExp(id));
});

test('tab 5 distinguishes liveness from readiness', () => {
  const t = textOf('env-k8s');
  assert.match(t, /liveness/i);
  assert.match(t, /readiness/i);
});

test('tab 5 shows the KEDA ScaledJob on the TTS service', () => {
  assert.match(textOf('env-k8s'), /KEDA|ScaledJob/);
});

test('image delivery is drawn as an open question', () => {
  const t = textOf('env-k8s');
  assert.match(t, /kind load/);
  assert.match(t, /registry/i);
});

test('every node on tab 5 is drawn as planned', () => {
  assert.ok(cellsOf('env-k8s').filter(isNode).every(c => /dashed=1/.test(c.style)));
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `node --test scripts/architecture/test/tab-k8s.test.js`
Expected: FAIL, tab does not exist

- [ ] **Step 3: Author tab 5 and build**

- [ ] **Step 4: Run the tests and the verifier**

Run: `node --test scripts/architecture/test/tab-k8s.test.js && node scripts/architecture/verify.js`
Expected: tests PASS; verify reports no errors

- [ ] **Step 5: Commit**

```bash
git add docs/architecture/ scripts/architecture/test/tab-k8s.test.js
git commit -m "feat(architecture): local Kubernetes tab"
```

---

### Task 15: Tabs 6 and 7 — contract matrix and rejected alternative

**Files:**
- Modify: `docs/architecture/architecture.template.drawio`, `docs/architecture/architecture.drawio`
- Test: `scripts/architecture/test/tab-contracts.test.js`

**Interfaces:**
- Consumes: `{{table:contracts}}` from Task 3.
- Produces: tabs `contracts` and `alt-indexer`.

Tab 6 is one cell holding `{{table:contracts}}` plus a title and the `{{meta:updated}}` stamp. Tab 7 is ported from v2's fourth tab with `PurchaseManager`, `SlotsPurchased` and their notes removed (ADR-001).

- [ ] **Step 1: Write the failing tests**

```js
test('tab 6 renders a row per service', () => {
  assert.equal((textOf('contracts').match(/<tr/g) || []).length, 9);
});

test('tab 6 shows the last-updated stamp', () => {
  assert.match(textOf('contracts'), /2026-11-18/);
});

test('tab 6 names the AsyncAPI contract that exists', () => {
  assert.match(textOf('contracts'), /storycontent-manager\.asyncapi\.yaml/);
});

test('tab 7 no longer mentions the dropped commerce service', () => {
  const t = textOf('alt-indexer');
  assert.doesNotMatch(t, /PurchaseManager|SlotsPurchased/);
});

test('tab 7 still explains why the separate indexer was rejected', () => {
  assert.match(textOf('alt-indexer'), /shared|share/i);
});

test('all seven tabs are present', () => {
  assert.equal(scan(XML).tabs.length, 7);
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `node --test scripts/architecture/test/tab-contracts.test.js`
Expected: FAIL, tabs do not exist

- [ ] **Step 3: Author tabs 6 and 7 and build**

- [ ] **Step 4: Run the tests and the verifier**

Run: `node --test scripts/architecture/test/tab-contracts.test.js && node scripts/architecture/verify.js`
Expected: tests PASS; verify reports no errors

- [ ] **Step 5: Commit**

```bash
git add docs/architecture/ scripts/architecture/test/tab-contracts.test.js
git commit -m "feat(architecture): contract matrix and rejected alternative tabs"
```

---

### Task 16: README and CI job

**Files:**
- Create: `docs/architecture/README.md`
- Modify: `.github/workflows/backend.yml`

**Interfaces:**
- Consumes: the finished build and verify commands.
- Produces: nothing other tasks depend on.

`README.md` says in about twenty lines: which files are sources and which are generated, that the outputs must never be hand-edited, the two commands, where the manifest records version and contract state, and that a new service means drawing a box in the template as well as adding a manifest entry.

The CI job is new, not a step in an existing one: every job in `backend.yml` sets `working-directory: ./choose-your-fate-backend` and installs Java (spec §7.7).

- [ ] **Step 1: Write the README and add the `architecture` job**

```yaml
  architecture:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '22'
      - run: node --test scripts/architecture/test/
      - run: node scripts/architecture/verify.js
```

- [ ] **Step 2: Verify the workflow parses and the job is independent**

Run: `node -e "const s=require('fs').readFileSync('.github/workflows/backend.yml','utf8'); if(!/^  architecture:$/m.test(s)) process.exit(1)"`
Expected: exit 0, and the new job declares no `needs` and no `defaults`

- [ ] **Step 3: Run the whole suite and the verifier from a clean tree**

Run: `node --test scripts/architecture/test/ && node scripts/architecture/build.js && node scripts/architecture/verify.js && git diff --exit-code docs/architecture/`
Expected: all PASS, and `git diff --exit-code` is silent — a rebuild changes nothing

- [ ] **Step 4: Commit**

```bash
git add docs/architecture/README.md .github/workflows/backend.yml
git commit -m "docs(architecture): README and CI verification job"
```
