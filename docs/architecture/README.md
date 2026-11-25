# Architecture diagram

Seven tabs: the logical architecture, two flows, the docker-compose environment, the
local Kubernetes deployment, a contract matrix, and one rejected alternative.

## Which files you edit

| File | |
|---|---|
| `architecture.json` | **Source.** Versions, status, APIs, events, contracts, open issues. |
| `architecture.template.drawio` | **Source.** The layout. Open this one in draw.io. |
| `architecture.drawio` | **Generated. Never edit it** — your changes will be overwritten and CI will fail. |

## Changing it

```
node scripts/architecture/build.js     # template + manifest -> architecture.drawio
node scripts/architecture/verify.js    # checks; exits non-zero on an error
```

- **A version, an event, a contract, a status:** edit `architecture.json`, run the build.
  Every tab that mentions it is correct again.
- **A new service:** add it to `architecture.json` *and* draw a box in the template with a
  `{{service:<id>:card}}` token. Verify fails if you add one without the other.
- **A new tab:** add its `<diagram>` to the template and its id to `meta.tabs`.
- **A message payload:** edit the event's `payload.fields` in `architecture.json`. Verify
  warns if a field you declare is no longer in the contract file that describes the event,
  so the picture and the AsyncAPI document cannot drift apart quietly. An event with no
  designed payload keeps `state: "undesigned"` and an empty field list — the page then
  says so, rather than printing a guess that would be read as a contract.
- **A broker or store version:** edit the `infrastructure` entry. Anything with an
  `image` must be drawn through `{{infra:<id>:label}}` — verify refuses a version that
  lives in the manifest but is typed into the drawing, because then bumping it changes
  nothing and nobody is told.

Commit both sources and the generated file together.

## How it stays honest

`verify.js` fails the build when the committed `.drawio` differs from a fresh render, when
a token does not resolve, when a service in the manifest is drawn nowhere, when two boxes
overlap, when a service's dashed border disagrees with its status, when an edge points at
a cell that no longer exists, when a box is drawn outside its group, or when the file
contains a bare `&` or an unbalanced quote that would stop draw.io opening it. It warns —
without failing — when a contract file the manifest calls
written is not in the checkout, when an exchange name does not appear in its publisher's
source, or when a port disagrees with the compose file. Those are warnings because most
of the system is still on unmerged branches, and a check that is always red is a check
people switch off.

The same two commands run in CI, in the `architecture` job.

A token must be the **whole** value of a cell: `value="{{service:recap-manager:card}}"`.
Mixing prose and a token in one label is rejected by the build.

## Writing the text

State the reason, do not cite a decision number. A reader holding this diagram cannot
open the decision log, so `(ADR-011)` tells them nothing they can act on, while "an
ordered log, so competing consumers would produce a false history" tells them everything.
The log still exists and is still the record; the diagram just has to stand on its own.
