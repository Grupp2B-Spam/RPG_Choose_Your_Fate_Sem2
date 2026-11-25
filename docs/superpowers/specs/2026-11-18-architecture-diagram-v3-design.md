# Architecture diagram v3 — design

**Date:** 18 November 2026
**Status:** awaiting approval; nothing built yet
**Revised:** 19 November 2026, after a builder review and the group's answers (§8.1)
**Supersedes:** `Mandetory/Mandetory 1/architecture-diagram-v2.drawio` (+ `-EN`)

---

## 1. Intent

The group needs an architecture diagram that is still true in January.

The v2 diagram was accurate the week it was drawn and has been wrong since: commerce was
dropped (ADR-001), RabbitMQ was chosen (ADR-002), gRPC became the standard for internal
synchronous calls (ADR-014), and `RecapManager` was implemented against a gRPC contract
nobody had drawn. Every one of those changes required someone to reopen an 85 KB XML file
and edit coordinates by hand, so none of them happened.

v3 exists to change the economics of keeping it current. The information that changes
often — a service's version, which API styles it exposes, which events it publishes and
consumes, which contract file describes it — is moved out of the drawing into a manifest
that is one file, one line per fact, and checkable against the repository. The drawing
itself, which changes rarely, stays hand-authored.

The test of success is not that v3 is pretty. It is that the next ADR costs one line.

---

## 2. Goals

1. **One diagram file covering three levels**, because the report requires all three:
   the logical architecture (report 2.1), the docker-compose development environment
   (3.1), and the local Kubernetes deployment (3.2).
2. **All three communication styles shown distinctly** — asynchronous events, internal
   gRPC, external REST/GraphQL — so that ADR-014's rule is visible rather than stated.
   v2 only distinguished REST from events.
3. **Per-service version and contract tracking on the diagram**, driven by a manifest:
   version, lifecycle status, stack, exposed API styles, published and consumed events,
   and the contract file that describes each.
4. **One generated output**, built from the hand-authored template plus the manifest, so
   the file that ships is never the file anyone hand-edited.
5. **A verification step that fails when the diagram disagrees with the repository** —
   a declared contract file that does not exist, an exchange name that does not appear
   in the publisher's source, a port that does not match its compose file.
6. **Honest status marking.** A service that does not exist yet must not look like one
   that does.
7. **The verify step wired into CI**, so drift is caught by the pipeline rather than by
   whoever opens the file last.

## 3. Non-goals

- The CI/CD pipeline diagram (report 3.2.2). Different subject, different shape.
- The cloud deployment diagram (report 3.3).
- ER diagrams and database schemas. `database-diagrams/` already owns those.
- Replacing `beslutningslog.md`. The diagram shows *what*; the log explains *why*. The
  diagram may reference an ADR number; it must not restate the argument.
- Making architectural decisions. Where a decision is open — ADR-013's sequence-number
  assignment, the GraphQL owner, the OAuth provider — the diagram records that it is
  open. It does not resolve it.

## 4. Anti-goals

These would make the work a failure even if it shipped:

- **A diagram that overstates the system.** If `SceneManager` is drawn the same as
  `RecapManager`, the diagram lies about four of seven services, and an examiner who
  notices stops trusting all of it.
- **A generator that becomes a project.** If maintaining the tooling costs more than
  maintaining the drawing did, v3 has made things worse.
- **A file nobody can open in draw.io.** The group has to be able to open the source,
  drag a box, and save. The moment the source is only machine-editable, it belongs to
  whoever wrote the generator.
- **A generated file somebody edits by hand.** The moment the shipped `.drawio` and the
  template disagree, the manifest stops being the source of truth and v3 is v2 again.
  The drift check exists for this and nothing else.

---

## 5. Constraints

| Constraint | Consequence |
|---|---|
| Node 22 and npm 11 are present; no root `package.json` with dependencies | Scripts are plain Node with zero dependencies. No `js-yaml`, so the manifest is JSON. |
| No Python on PATH (`python3` resolves to the Windows Store shim) | Node only, despite `scripts/build_availability_test_docx.py` existing. |
| PowerShell's XML round-trip corrupts draw.io files by adding namespace declarations | The build script does **text splicing** on the template, never parse-and-reserialise. This is a hard rule carried from v2. |
| draw.io re-serialises any file it saves | Only the *template* is opened in draw.io. Generated outputs are never hand-edited. |
| Services live on unmerged branches — originally three; `storycontent-manager` and `contracts` landed on `main` on 19 November, leaving `Scenes` and `character-manager-service` | The manifest describes the system **as if those branches were merged**. This is the group's decision, taken deliberately: a diagram of `main` alone would show empty boxes for services that are in fact written. Verify's filesystem checks therefore report absences from any single checkout, which is why checks 8–11 are warnings rather than errors (§7.7). The first merge bore this out: four of the five warnings cleared on their own when `main` caught up. |
| `choose-your-fate-backend` is a running monolith that no compose file starts, and the source the services are being lifted from (ADR-008) | It appears on the diagram as a legacy node being decomposed, so that tab 1 shows what exists today next to what is replacing it. See §7.3 and §7.9. |
| `main`'s `docker-compose.yml` ran `rabbitmq:3.13-management` under the container name `bar-scan-rabbitmq` — a leftover from another project — until 19 November | The consolidated compose (RabbitMQ 4, `include:` per service) is now on `main`, and tab 4 draws it as current state. |
| The report is delivered in English | The diagram is English only. v2's Danish/English pair is dropped, and with it the translation table and the second output file. See §7.1. |
| Seven services, several with no code yet | Status is a first-class field, not a footnote. |

---

## 6. Approaches

Three were considered. The group chose C.

**A — Hand-written XML, as v2.** Write the Danish file by hand, generate English with a
translation table, verify with a script. Known workflow, full control of layout.
*Rejected because* it reproduces the exact failure being fixed: version and contract
facts would appear in ten to twelve places across seven tabs and two languages, each
edited by hand. The reason v2 went stale was never that anyone disagreed with updating
it — it was that updating it cost an afternoon.

**B — Everything generated from a manifest.** One manifest; a script renders all tabs in
both languages. One source of truth, and a new service costs one JSON block.
*Rejected because* the three levels have three different shapes — a context map, a
container diagram, a deployment diagram — plus two sequence flows. A layout engine good
enough for all five is a larger piece of work than the diagram, and the quality that made
v2 readable (deliberate edge corridors, grouped bounded contexts) is not what a grid
algorithm produces.

**C — Hybrid: hand-authored layout, manifest-driven content. (Chosen.)** Boxes and edges
are hand-written XML with stable cell ids, in a template that opens in draw.io. The
manifest holds only what changes often. A build script splices manifest content into the
template and emits both language files. A verify script checks the manifest against the
repository and the outputs against a fresh render.
*Trade-off accepted:* adding a service requires someone to draw a box, not just edit
JSON. That is the right side of the trade — services are added rarely and their versions
change constantly.

---

## 7. Design

### 7.1 Language

**English only.** The template, the manifest prose and the single output file are all
English.

v2 shipped a Danish file and an English one generated from it by a translation table. The
pair is dropped: the report is written in English, so the Danish file was the one nobody
read, and keeping it meant a translation table, a second output, a second drift check and
a class of failure — Danish text reaching the English file unnoticed — that only existed
because the pair existed. The group discusses the diagram in Danish regardless; the labels
on it do not have to be.

This removes `i18n.json`, `lib/i18n.js` and `architecture.da.drawio` from §7.2, and the
translation step from §7.6.

### 7.2 Files on disk

```
docs/architecture/
  architecture.template.drawio   hand-authored, opens in draw.io           (SOURCE)
  architecture.json              the manifest                              (SOURCE)
  architecture.drawio            generated - do not edit                   (OUTPUT)
  README.md                      how to change the diagram, 20 lines
scripts/architecture/
  build.js                       template + manifest -> architecture.drawio
  verify.js                      manifest vs repo, output vs fresh render, geometry
  lib/                           shared helpers (splice, escape, geometry, drawio scan)
```

The output is committed. It is the artifact the report embeds, and a reviewer cloning the
repo must be able to open it without running a build.

### 7.3 Manifest schema

`architecture.json` is an object with `meta`, `services`, `infrastructure`, `events` and
`openIssues`. Illustrative extract, comments for explanation only (the real file is
strict JSON):

```jsonc
{
  "meta": {
    "version": 3,
    "updated": "2026-11-18",
    "tabs": ["arch-main", "flow-choice", "…"]   // the tabs the diagram must have
  },

  "services": [
    {
      "id": "recap-manager",            // Kubernetes name, lowercase-hyphen
      "kind": "service",                // service | legacy | infrastructure
      "display": "RecapManager",        // the name used in prose and on the diagram
      "version": "0.0.1-SNAPSHOT",      // Maven artifact version
      "status": "implemented",          // planned | skeleton | implemented
      "branch": "main",                 // where the code lives today (see §5)
      "stack": "Spring Boot 4.0.3 / Java 21",
      "context": "state",               // identity | content | state | media
      "purpose": "Generates per-character recaps with an LLM.",
      "adr": ["ADR-012", "ADR-013"],

      "datastore": { "name": "recap-vault", "kind": "MySQL 9.5", "hostPort": 3307 },

      // Optional. Omitted entirely for a service with no agreed port, and the
      // renderer then prints "port not decided" rather than inventing one.
      "ports": { "http": 8090 },

      // Lets check 10 find the right file: names differ per service, and the key
      // inside is not always the service id.
      "compose": {
        "file": "microservices/recapManager/docker-compose-recap-manager.yml",
        "service": "recap-manager"
      },

      "exposes": [
        {
          "style": "REST",                        // REST | GraphQL | gRPC
          "detail": "/choose-your-fate/ai",
          "versioning": "X-API-VERSION header",
          "contract": { "path": "...openapi.yaml", "state": "todo" }
        }
      ],

      "calls": [
        {
          "style": "gRPC",
          "target": "storypath-manager",
          "method": "StoryPathService.GetChoicesAfter",
          "contract": {
            "path": "microservices/recapManager/src/main/proto/storypath/v1/story_path.proto",
            "state": "written",
            "note": "Lives in the caller's tree. ADR-012 says it belongs with the implementer."
          }
        }
      ],

      "publishes": [
        { "event": "recap-generated", "exchange": "recap.events", "state": "planned" }
      ],

      // "scalable" is a property of the (consumer, event) pair, not of the event.
      // ADR-011's table makes player-choice.made scalable for CharacterManager and
      // explicitly not for StoryPathManager, so it cannot live on the event.
      "consumes": [
        // { "event": "player-choice.made", "queue": "...", "scalable": false,
        //   "why": "The path is an ordered log." }
      ],

      "gaps": ["Does not publish recap-generated yet."]
    }
  ],

  // An entry carrying an `image` is a version that can go stale, so check 4 requires
  // the diagram to draw it through an {{infra:<id>:label}} token rather than typing the
  // tag into the template. Entries with no version — an external API, say — may stay
  // hand-drawn.
  "infrastructure": [
    { "id": "rabbitmq", "display": "RabbitMQ", "kind": "broker",
      "image": "rabbitmq:4-management-alpine", "ports": [5672, 15672] }
  ],

  "events": [
    {
      "name": "scene-content.updated",
      "exchange": "storycontent.events",
      "publisher": "storycontent-manager",
      "consumers": ["scene-manager"],
      "queues": ["scenemanager.scene-content"],
      "contract": { "path": "microservices/contracts/storycontent-manager.asyncapi.yaml",
                    "state": "written" },
      "ordering": "version-guarded"   // how the consumer is protected from stale redelivery
    }
  ],

  "openIssues": [
    { "id": "ADR-013", "short": "Who assigns the per-character sequence number?",
      "blocks": ["storypath-manager"] }
  ]
}
```

**`kind` distinguishes three sorts of node.** `service` is one of the seven. `infrastructure`
is RabbitMQ, MinIO, Ollama, ElevenLabs. `legacy` is `choose-your-fate-backend`, the
monolith: it has a `version`, a `stack` and a datastore like any other node, but no
`context`, because its whole point is that it spans all four. Its entry carries a
`replacedBy` list naming the services being lifted out of it, which is what tab 1 draws
the decomposition arrows from. It is on the diagram because it is the only thing that
runs today, and a diagram that omits it describes a system nobody can start.

**`SceneIndexer` is not a service entry.** Per ADR-010 it is the projection worker of
`scene-manager` — same codebase, same schema, same release — so it is modelled as a second
deployment under that service (`deployments: [{ "name": "scene-indexer", "role":
"projection" }]`), not as an eighth box with its own version. Drawing it as a peer would
restate on the diagram the exact confusion ADR-010 was written to clear up.

**`state` on a contract is declared, not detected.** `"written"` means the group believes
the file exists; `"todo"` means it is a known gap. Verify compares the declaration against
the filesystem and reports a mismatch. The *rendering* uses the declaration, so the output
is deterministic regardless of which checkout it was built in — which matters because the
contracts directory is currently uncommitted and absent from some branches.

### 7.4 Template and tokens

The template is ordinary draw.io XML. Where generated content belongs, a cell's `value`
contains a token:

```xml
<mxCell id="svc-recap" value="{{service:recap-manager:card}}" style="..." vertex="1">
```

Token grammar, deliberately small:

| Token | Renders |
|---|---|
| `{{service:<id>:card}}` | Name, purpose, version + status badge — the standard box content |
| `{{service:<id>:apis}}` | The exposed API styles, one line each |
| `{{service:<id>:events}}` | Published and consumed event names |
| `{{service:<id>:contracts}}` | Contract file basenames, marked by declared state |
| `{{event:<name>:label}}` | Event name + exchange + queue count |
| `{{infra:<id>:label}}` | A broker or store: name, image tag, ports |
| `{{event:<name>:payload}}` | The message body as a field list, or a statement that none is designed |
| `{{meta:envelope}}` | The fields every message carries, whatever the topic |
| `{{table:contracts}}` | The whole contract matrix (tab 6) as HTML |
| `{{meta:updated}}` | The manifest date, for the "last updated" stamp |

Rules:

- Tokens are plain ASCII with no XML-significant characters, so draw.io never re-encodes
  them and the template stays valid XML whether or not it has been rendered.
- A token occupies the **whole** value of a cell. No mixing literal text and tokens — it
  makes splicing fragile and the rendered result hard to predict.
- Replacement is text splicing on the raw template string. The replacement is
  HTML-escaped for an XML attribute (`<br>` becomes `&lt;br&gt;`), matching how draw.io
  stores rich values.
- An unresolved token is a build error, not a warning. A diagram that ships with
  `{{service:scene-manager:card}}` printed on it is worse than a failed build.

### 7.5 No translation layer

Deliberately absent. §7.1 explains why: the diagram is English only, so there is no
translation table, no second output, and no class of failure where one language silently
diverges from the other.

Section number retained so that the cross-references in §7.6 to §7.10 keep pointing at
what they were written to point at.

### 7.6 Build

`node scripts/architecture/build.js`

1. Read the template and the manifest, normalising the template to `\n` line endings.
2. Resolve every token against the manifest. An unresolved token exits non-zero.
3. Write `architecture.drawio` with a generated banner as the first XML comment after the
   declaration:
   `<!-- GENERATED by scripts/architecture/build.js from architecture.template.drawio - do not edit -->`
4. Print a summary: services rendered, tokens resolved.

### 7.7 Verify

`node scripts/architecture/verify.js`, exit non-zero on any **error**; warnings are
printed and do not fail.

| # | Check | Severity |
|---|---|---|
| 1 | The committed output is byte-identical to a fresh render (drift) | error |
| 2 | The output is well-formed XML with the expected tab count and ids | error |
| 3 | Every token in the template resolves | error |
| 4 | Every manifest service appears in at least one template token (no orphans) | error |
| 5 | Every service `id` is lowercase-hyphen and matches the Kubernetes naming convention | error |
| 6 | No two vertices overlap **among siblings of the same parent** | error |
| 7 | No two edges share a vertical corridor (carried from v2) | warning |
| 8 | A contract declared `written` exists on disk; one declared `todo` does not | warning |
| 9 | Each exchange name in the manifest appears literally in the publishing service's source | warning |
| 10 | Each service's `ports.http` matches `compose.file` / `compose.service`, skipped when `ports` is absent | warning |
| 11 | Every event has at least one consumer, and every consumer names a queue | warning |
| 12 | A service's box is dashed exactly when its status is `planned` | error |
| 13 | Every `parent`, `source` and `target` names a cell that exists | error |
| 14 | No child is drawn outside its parent's bounds | error |
| 15 | Every payload field the manifest declares still appears in the contract that describes it | warning |

Checks 12 to 14 were added after a review of the first build, which demonstrated two ways
the diagram could ship something false with CI green. Check 12 closes the gap between the
badge, which comes from the manifest, and the border, which is hand-drawn: without it a
one-line manifest edit could put `● v1.0.0` inside a dashed "does not exist yet" box.
Checks 13 and 14 catch content draw.io discards silently — an edge whose endpoint was
deleted, or a box dragged outside its group — which otherwise disappears from the diagram
with nothing said.

Check 2 also rejects a bare `&` and an unbalanced quote. Those are the two ways a
hand-edited template produces a file draw.io refuses to open, and the substring counts
alone did not see them.

Check 6 is scoped to siblings of the same parent deliberately: a status badge sits inside
its service box and a service box sits inside its bounded-context group, so an unscoped
overlap rule would flag every correctly nested element in the diagram.

Checks 8–11 are warnings rather than errors because the services they describe are spread
across unmerged branches and several do not exist at all, so no single checkout can
satisfy them (§5). They become meaningful as the branches merge, without blocking anyone
today.

Check 1 only works if line endings are stable: `build.js` normalises the template to `\n`
on read and writes `\n` explicitly, and `.gitattributes` gains `*.drawio text eol=lf`
alongside the existing `*.sh` rule. Otherwise Git's checkout conversion rewrites the file
on Windows and the byte comparison fails on a machine where nothing is wrong.

**CI:** its own job in `.github/workflows/backend.yml` — not a step inside an existing one.
Every job in that file sets `working-directory: ./choose-your-fate-backend` and sets up
Java and Maven, none of which this needs. A separate `architecture` job with
`actions/setup-node` and one `run: node scripts/architecture/verify.js` keeps it
independent of whether the Maven build passes. Errors fail the job; warnings appear in
the log.

### 7.8 Tabs

Seven, not six. The contract matrix was going to be crammed into the architecture tab;
it is clearer as its own tab, and it is the tab the report's section 2.2 is written from.

| # | Tab | Report section | Content |
|---|---|---|---|
| 1 | Architecture | 2.1 | Seven services grouped by bounded context (IDENTITY / CONTENT / STATE / GENERATED MEDIA), each box carrying name, purpose, version and a status badge, with its datastore attached. `SceneIndexer` is drawn inside `SceneManager` as its projection deployment, not as a peer (ADR-010). The monolith `choose-your-fate-backend` sits apart as a legacy node, with pale arrows to the services being lifted out of it — the decomposition is the story this tab tells. RabbitMQ shown with one exchange per publisher. Three edge styles (§7.9). |
| 2 | Flow — player makes a choice | 2.3, 2.4.4 | Updated from v2: the per-character sequence number (ADR-013), two independent consumers, and ADR-013's open question marked on the diagram as open. |
| 3 | Flow — recap with narration | 2.3, 2.4.2 | Rewritten: `RecapManager` to `StoryPathManager` over **gRPC** `GetChoicesAfter(characterId, afterSequence, pageSize, snapshotSequence)`, the stored checkpoint, pagination via `snapshotSequence`, then `recap-generated` to TTS. Content-hash idempotence retained from v2. |
| 4 | Development environment — docker-compose | 3.1 | Containers, images, host ports, volumes, healthchecks, the `include:` structure and the `prod` profile. Drawn from the **consolidated** compose — RabbitMQ 4 and one included file per service — which is the one the group keeps, not the stale `rabbitmq:3.13` / `bar-scan-rabbitmq` version still on `main` (§5). Current state, not target state: what `docker compose up` starts. |
| 5 | Local Kubernetes | 3.2 | Drawn as a **flow**, not a catalogue: clients outside the cluster boundary, traffic in through the Ingress, out through the broker to its consumers, with KEDA, the autoscaler and a proposed observability stack arranged around it. Every arrow is labelled with the relationship it carries. Kubernetes objects use draw.io's `mxgraph.kubernetes` stencils, in a child cell beside the text so a missing stencil leaves the card readable. Datastores are one StatefulSets box rather than eight, because tab 1 already says who owns what. Target state: no Kubernetes manifest exists, so every node is `planned` under a banner saying so, and the two undecided questions — how images reach the cluster, and how KEDA gets its metric — are drawn as open. |
| 6 | Broker topology | 2.3, 2.4.3 | Exchanges, the routing key on every binding, the queue it feeds and who drains it, and the dead-letter path hanging off each declared queue. Solid is what was read off a running broker; dashed is what the contracts describe and nobody has built. Three things it makes visible that were previously only prose: that one publisher feeds two different services from one exchange, that publish-subscribe here is several queues bound to the same routing key rather than a shared queue, and that a rejected message has somewhere to go. It also carries why the publisher currently declares its consumers' queues, and how that is undone. |
| 7 | Published events | 2.2, 2.3, 5.4 | Who publishes what, and what is in it. A column per exchange, the publisher above its events, and under each event the payload as a field list rendered from the manifest. Documented shapes come from the AsyncAPI contract and are checked against it (check 15). `player-choice.made` is marked proposed; the two events nobody has designed are drawn dashed and say so, because a plausible guess printed in that shape would be read as a contract. An envelope box carries the five fields every message shares. |
| 8 | Contracts and versions | 2.2, 5.3 | A matrix: service by version, status, stack, exposed APIs, publishes, consumes, contract file, ADRs. Generated wholly from the manifest via `{{table:contracts}}`. This is the living reference. |
| 9 | Rejected alternative — separate indexer | 2.4.1 | Carried over from v2, with `PurchaseManager` and `SlotsPurchased` removed (ADR-001). |

### 7.9 Visual language

**Communication styles** — the point of goal 2, so they must be unmistakable at a glance:

| Style | Edge | Rationale |
|---|---|---|
| Asynchronous event | dashed, amber `#d79b00`, open arrowhead, labelled `publishes` / `consumes` | Dashed reads as "not waiting for an answer" |
| Internal gRPC | solid, indigo `#6c5ce7`, filled arrowhead, label is the method name | Solid reads as "blocking"; the method name is the contract |
| External REST / GraphQL | solid, green `#82b366`, filled arrowhead, label is the route or operation | Matches v2's client colour, so the boundary to the browser stays where readers expect |
| Datastore access | thin, grey, no label | Ownership, not communication — deliberately quiet |

**Status badges** on every service box:

| Status | Badge | Box treatment |
|---|---|---|
| `implemented` | filled dot + version | solid border |
| `skeleton` | half dot + "skeleton" | solid border, lighter fill |
| `planned` | hollow dot + "planned" | dashed border |
| `kind: legacy` | "monolith — being decomposed" | grey fill, solid border, no bounded-context group |

A service whose code sits on an unmerged branch carries the branch name under the badge.
That is a fact with a shelf life — it disappears from the manifest when the branch merges,
and the diagram stops mentioning it.

The monolith's decomposition arrows are deliberately pale and unlabelled: they are a
migration relationship, not communication, and they must not compete with the three edge
styles above.

The dashed border on `planned` is what prevents the anti-goal in §4: four of seven
services are outlines, and that is visible from across the room.

**Databases** keep v2's symbol:
`image;html=1;image=img/lib/clip_art/computers/Database_128x128.png`

**Open issues** appear as a small amber note anchored to the service they block, carrying
the ADR number: ADR-013 on `StoryPathManager`, GraphQL on `SceneManager` and
`CharacterManager`, OAuth on `AccountManager`, KEDA on `TextToSpeechManager`.

### 7.10 Edge corridors

Carried from v2, because it is the single thing that most affects whether the result is
readable: **each edge gets its own vertical corridor, nested so the topmost target gets
the outermost corridor and no two cross.** Corridors are explicit `Array`/`mxPoint`
waypoints in the template, not left to draw.io's router. Verify check 7 reports
violations.

---

## 8. Decisions

### 8.1 Taken by the group

1. **The manifest describes `main` plus the unmerged branches, as if they were merged.**
   A diagram of `main` alone would show empty boxes for three services that are written.
2. **The monolith is on the diagram**, so the difference between what runs today and what
   replaces it is visible rather than asserted.
3. **Ports are left blank** for services with no agreed allocation. No invented numbers.
4. **The consolidated compose is authoritative** for tab 4. It was on a branch when this
   was decided and reached `main` on 19 November, so tab 4 now simply draws `main`.
5. **English only** — one output file, no translation layer (§7.1).

### 8.2 Calls made here, to be overruled rather than discovered

1. **The diagram is English only** (§7.1) — the group's call, taken after the first draft.
   v2's Danish/English pair, its translation table and the second output file are dropped.
2. **JSON, not YAML**, for the manifest — zero dependencies, so the build runs on a clean
   clone with nothing but Node.
3. **Seven tabs, not six** — the contract matrix earns its own (§7.8).
4. **Contract `state` is declared in the manifest and merely checked against disk**
   (§7.3), so output does not depend on which branch it was built from.
5. **Checks 8–11 are warnings, not errors** (§7.7), because most services do not exist yet.
6. **Files live in `docs/architecture/`**, not next to the existing root-level `.drawio`
   files. Those three stay where they are as historical artifacts; v3 is not one more
   file in a pile of four.
7. **The API versioning line in the manifest records what the code does** —
   the `X-API-VERSION` header, per `ApiVersionConfig` — which is not what
   `OPEN-DESIGN-ISSUES.md` §2.7 proposes (`/v1` in the path). The diagram shows the code;
   closing the gap is the group's decision, not the diagram's.
8. **`scalable` belongs to the (consumer, event) pair, not the event** (§7.3). ADR-011
   makes `player-choice.made` scalable for `CharacterManager` and not for
   `StoryPathManager`; a flag on the event could not express that.
9. **`SceneIndexer` is a deployment of `scene-manager`, not an eighth service** (§7.3),
   per ADR-010.
10. **Event names follow the routing keys in `contracts/README.md`** — `player-choice.made`,
    kebab-case — not the `PlayerChoice.made` spelling still used in
    `OPEN-DESIGN-ISSUES.md` §1.2. One spelling, and it is the one the broker uses.
11. **Image export is out of scope.** The report embeds exports made by hand from draw.io.
    Automating it needs drawio-cli and Electron, which breaks the zero-dependency
    constraint for a gain CI cannot verify anyway. The residual risk is real and named:
    CI guarantees the XML, not the picture pasted into the .docx.
12. **Tab 4 draws current state, tab 5 draws target state**, each labelled as such (§7.8).
13. **The CI check is its own job**, not a step in the Maven jobs (§7.7).
14. **Outputs are written with LF and `.gitattributes` is extended** (§7.7), without which
    the drift check fails on Windows for no real reason.

---

## 9. Left to the builder

- Exact coordinates, box sizes, and page dimensions per tab.
- Precise wording of explanatory notes, within the guidance in §7.8.
- The internal structure of `scripts/architecture/lib/`.
- How the contract matrix is laid out as HTML inside a single draw.io cell.
- Whether tabs 4 and 5 share a visual vocabulary with tab 1 or deliberately differ.

## 10. What this diagram records as open, and must not resolve

- **ADR-013** — who assigns the per-character sequence number. Until it is answered,
  `StoryPathManager` cannot move into ADR-011's scalable column.
- **ADR-012's misplaced `.proto`** — it describes `StoryPathManager`'s API and lives in
  `RecapManager`'s source tree. The manifest records the path and the note; moving it is
  a code change.
- **GraphQL owner** — unassigned. `SceneManager` and `CharacterManager` are the
  candidates (`OPEN-DESIGN-ISSUES.md` §2.1).
- **OAuth/OIDC provider** — unassigned, belongs in `AccountManager` (§2.2).
- **Dead-lettering retry counts and backoff** (`OPEN-DESIGN-ISSUES.md` §3.2).
- **Port allocation.** 8081 and 8090 are taken, and `recapManager`'s compose defaults
  `JWT_ISSUER` to `http://localhost:8092`, which implies someone has 8092 in mind for
  `AccountManager` without having written it down. Until the group agrees a scheme, the
  manifest leaves `ports` out and the diagram says so.
- **How images reach the local cluster** — `kind load` versus a local registry
  (`kubernetes-konventioner.md` §4). Drawn as open on tab 5.
- **When the monolith is retired.** The diagram shows it being decomposed; it does not
  claim a date, and no ADR has set one.
