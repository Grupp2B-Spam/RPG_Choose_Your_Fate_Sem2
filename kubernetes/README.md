# Running Choose Your Fate on Kubernetes

Everything needed to bring the system up on a local cluster: the services, a
database for each of them, the message broker, and one gateway in front of it
all.

You should be able to use this without asking anyone. If you get stuck on
something that is not written down here, that is a gap in this file and worth
reporting.

---

## Before you start

| What | Why |
|---|---|
| **Docker Desktop, running** | It hosts both the container runtime and the cluster. |
| **Kubernetes enabled in Docker Desktop** | Settings → Kubernetes → *Enable Kubernetes*, provisioner **Kubeadm**. |
| **kubectl** | Docker Desktop installs one; any recent version works. |
| **PowerShell** | The scripts are `.ps1`. Windows PowerShell 5.1 is enough; nothing needs PowerShell 7. |

Nothing else. No Maven, no JDK, no Helm — the scripts run Maven inside a
container so the only tool you need on your machine is Docker.

**If Kubernetes will not start in Docker Desktop**, the cause on this course has
consistently been an out-of-date WSL:

```powershell
wsl -v
wsl --update
```

Then restart Docker Desktop and let Kubernetes come up again.

[Lens](https://lenshq.io/) is a free GUI for browsing the cluster and is worth
having, but nothing here depends on it — every step is a `kubectl` command.

---

## Running it

Two commands. The first is once per machine; the second is the one you use from
then on.

```powershell
.\kubernetes\scripts\setup.ps1
```

```powershell
.\kubernetes\scripts\deploy.ps1
```

### What `setup.ps1` does

Once per machine, and safe to re-run:

- checks that Docker is up, that the context is `docker-desktop`, and that the
  cluster answers
- creates the `choose-your-fate` namespace
- installs **ingress-nginx** (pinned to `controller-v1.15.1`), turns on its
  metrics and waits for it
- generates **secrets** — one database password per database, a broker password,
  an RSA keypair for JWT verification, and a self-signed TLS certificate for the
  gateway
- builds the database schema **ConfigMaps** by reading the SQL out of each
  service's own folder

Secrets are created only when missing, never replaced. This matters: a database
bakes its root password into its data directory the first time it starts, so
regenerating a password against an existing volume locks the service out of its
own database, with an authentication error that looks nothing like the cause.

Two optional extras:

| Switch | What it adds |
|---|---|
| `-WithKeda` | KEDA. Nothing uses it yet; it is the autoscaler the serverless text-to-speech work will need. |
| `-WithDashboard` | The Kubernetes Dashboard, a read-only web UI. Five containers in their own namespace. See **Looking at the cluster** below. |

### What `deploy.ps1` does

1. builds a container image per service
2. runs `kubectl apply -R -f kubernetes`
3. waits for every rollout and tells you what did not come up

Step 2 is the whole deploy, and it works on its own once the images exist:

```powershell
kubectl apply -R -f kubernetes
```

Nothing has to be applied by hand first and nothing has to be applied in a
particular order. Where ordering genuinely matters — a service must not start
before its database — it is handled by an `initContainer` that waits on the
port, and by readiness probes. Not by a note somebody has to remember.

Useful flags:

```powershell
.\kubernetes\scripts\deploy.ps1 -SkipBuild                       # manifest-only change
.\kubernetes\scripts\deploy.ps1 -Only scene-manager, scene-indexer
.\kubernetes\scripts\deploy.ps1 -Push                            # also push to the registry
```

### Checking that it works

```powershell
.\kubernetes\scripts\verify.ps1
```

Green pods only prove that processes started. This sends a real request through
the gateway, inspects what the broker actually has, and then takes RabbitMQ away
to confirm that readiness drops while liveness holds. See
[What verification actually proves](#what-verification-actually-proves).

It stops the broker for about a minute and puts it back. Pass `-SkipDisruptive`
to leave it alone.

### Taking it down

```powershell
.\kubernetes\scripts\teardown.ps1                 # workloads go, data stays
.\kubernetes\scripts\teardown.ps1 -IncludeData    # volumes and secrets go too
```

The volumes use the `Retain` reclaim policy, so an ordinary teardown does not
destroy authored content. Use `-IncludeData` when a schema has changed, because
MySQL and MongoDB run their init scripts **only** against an empty data
directory and will otherwise ignore a new schema completely.

Neither form touches the Kubernetes Dashboard, which lives in its own namespace
and belongs to the machine rather than to this project. Remove it with:

```powershell
kubectl delete namespace kubernetes-dashboard; kubectl delete clusterrolebinding dashboard-viewer kubernetes-dashboard-metrics-scraper; kubectl delete clusterrole kubernetes-dashboard-metrics-scraper
```

---

## What you get

| | Address |
|---|---|
| **Gateway** | <http://localhost/> — and <https://localhost/> with a self-signed certificate |
| RabbitMQ management UI | <http://localhost:31672> |
| Service NodePorts | 30081 storycontent, 30090 recap, 30080 scene, 30092 account, 30010 character |

The NodePorts are **debugging doors, not the way in**. They exist so that when
a request through the gateway fails you can hit the service directly and find
out whether the fault is in the service or in the routing — which is otherwise
hard to tell apart. They also reach `/actuator`, which the gateway deliberately
does not expose. Each one is a single file you can delete.

The broker credentials for the management UI are in the cluster, not in this
repository:

```powershell
kubectl -n choose-your-fate get secret rabbitmq-credentials -o jsonpath='{.data.password}' | % { [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($_)) }
```

### Routes through the gateway

| Path | Service | Route group |
|---|---|---|
| `/admin/...` | storycontent-manager | admin, 5 rps |
| `/scenes/...` | scene-manager | public, 50 rps |
| `/choose-your-fate/ai/...` | recap-manager | public |
| `/choose-your-fate/{characters,inventories,items,loadout,quests,race-details}` | character-manager | public |
| `/api/{character-details,equipment}` | character-manager | public |
| `/ws/...` | account-manager | public |

`/choose-your-fate` is listed prefix by prefix rather than as one catch-all,
because two services share that root and a catch-all would hand one service the
other's traffic.

To see the table as the cluster has it, with the pod addresses each route
actually resolves to:

```powershell
kubectl describe ingress -n choose-your-fate
```

An empty backend there — `<error: endpoints "x" not found>`, or nothing in the
parentheses — means the Service selector or the readiness probe is the problem,
not the route. It is the first thing to look at when a path returns 503.

---

## Looking at the cluster

There is no web UI for the gateway itself, and that is deliberate on upstream's
part: the controller is a reconcile loop over your Ingress objects, so a UI with
buttons would be a second source of truth that the next `kubectl apply` would
overwrite. Three things are worth knowing about instead.

### Lens

A desktop app, not a website. It reads the kubeconfig you already have, so
nothing is installed into the cluster. It shows the Ingress objects, live pod
logs and a terminal that exec's into a pod without you typing `kubectl exec`.
If you have it, it covers most of what follows.

One warning: Lens offers to install its own bundled Prometheus ("Lens Metrics")
into a `lens-metrics` namespace. Do not accept it on a cluster where someone
else is setting up monitoring — you end up with two Prometheus installs
scraping the same targets and no obvious sign of which one a graph came from.

### The Kubernetes Dashboard

Installed only by `setup.ps1 -WithDashboard`. Two commands, in two terminals:

```powershell
kubectl -n kubernetes-dashboard port-forward svc/kubernetes-dashboard-kong-proxy 8443:443
```

```powershell
kubectl -n kubernetes-dashboard create token dashboard-viewer
```

Then open <https://localhost:8443/>, accept the self-signed certificate, and
paste the token. It expires after an hour; mint another with the same command.

The dashboard is **read-only**. It logs in as a ServiceAccount bound to the
built-in `view` role, which cannot change anything and cannot read Secrets.
Most guides bind `cluster-admin` instead; this does not, because nothing here
depends on the dashboard and a token that reaches the whole cluster is a poor
trade for a browser tab. The `roleRef` in `setup.ps1` is a one-line change if
the group wants more.

CPU and memory graphs will be empty. That needs `metrics-server`, which is not
installed — see **Metrics** below for why.

### The gateway's own config

When you want to see what nginx is actually running rather than what you asked
for:

```powershell
kubectl exec -it -n ingress-nginx deploy/ingress-nginx-controller -- /bin/bash
```

`/etc/nginx/nginx.conf` inside that pod is generated from `/etc/nginx/template/
nginx.tmpl` every time an Ingress object changes. Never edit it — it is
overwritten on the next reload. Two things in it are worth reading once:

- Both Ingress objects are merged into a **single `server` block** for
  `localhost`. The route groups are not separate servers; they are per-location
  `limit_req` zones inside one. That is the design from the decision log, made
  visible.
- Each `Prefix` path becomes **two** locations, `location = "/scenes"` and
  `location "/scenes/"`. Kubernetes defines `Prefix` as matching whole path
  segments, so `/scenesfoo` must not match — and a bare `location /scenes` in
  nginx would match it, because nginx matches characters.

You will not find pod addresses in that file. Upstreams are resolved at request
time by a Lua balancer, which is why replacing a pod does not reload nginx. The
live list is:

```powershell
kubectl exec -n ingress-nginx deploy/ingress-nginx-controller -- curl -s http://127.0.0.1:10246/configuration/backends
```

And the access log, which names the upstream that served each request:

```powershell
kubectl logs -n ingress-nginx deploy/ingress-nginx-controller -f
```

---

## How it is laid out

One folder per service, each holding its `deployment.yaml` and `service.yaml`,
with a `persistentvolume.yaml` where there is a database. That is the structure
the course asks for, and it is what makes `kubectl apply -R -f kubernetes` a
single command.

```
kubernetes/
  00-namespace/          the namespace, named 00- so apply -R creates it first
  rabbitmq/              broker + volume + management NodePort
  storycontent-manager/  }
  recap-manager/         }  one folder per service:
  account-manager/       }  deployment.yaml, service.yaml, nodeport.yaml
  character-manager/     }
  scene-manager/         }
  scene-indexer/         deployment only -- nothing calls a projector
  story-content-vault/   }
  recap-vault/           }  one folder per database:
  character-vault/       }  deployment, service, persistentvolume
  scene-read-model/      }
  ingress/               the two route groups
  scripts/               setup, deploy, verify, teardown, set-registry
```

### Adding a service

Copy the folder of the service that resembles yours and change the names. In
practice:

1. `mkdir kubernetes/<your-service>`, then copy `deployment.yaml`,
   `service.yaml` and `nodeport.yaml` from `character-manager/` — or from
   `storycontent-manager/` if your service has Actuator.
2. Replace every occurrence of the old name. One name per service: the
   Deployment, the Service, the image and ideally the Maven artifact all use the
   same string.
3. Set `containerPort` and the NodePort to yours. NodePorts have to be in
   30000–32767 and unique; the ones in use are listed above.
4. If it needs a database, copy a `*-vault/` folder too and add its secret to
   `setup.ps1`.
5. Add a path to `ingress/ingress-public.yaml`.
6. Add it to the `$Services` list in `deploy.ps1`.

Nothing else. No change to any existing service, and no change to the deploy
command.

---

## Names

Kubernetes names become DNS names, so they have to be agreed rather than chosen
independently. Lowercase with hyphens, and the same string for the Deployment,
the Service and the image.

Several folders in `microservices/` do not match, which is why the table is
here rather than left implicit:

| Folder in `microservices/` | Name here | Note |
|---|---|---|
| `storycontent-manager` | `storycontent-manager` | matches |
| `recapManager` | `recap-manager` | folder is camelCase; the Maven artifact is already `recap-manager` |
| `accountManager` | `account-manager` | folder is camelCase; the Maven artifact is already `account-manager` |
| `character-manager-service` | `character-manager` | the `-service` suffix says nothing the rest of the name does not; its Maven artifact is still `choose-your-fate-backend`, left over from the monolith |
| `Scenes/scene-manager` | `scene-manager` | matches |
| `Scenes/scene-indexer` | `scene-indexer` | matches |

Renaming a folder or a Maven artifact is the owner's call, so the Kubernetes
names were chosen to be right and the mismatches are recorded instead of being
fixed unilaterally.

Databases are named after what they hold, following the names already in the
compose files: `story-content-vault`, `recap-vault`, `character-vault`, and
`scene-read-model` for the Mongo instance behind the scene services.

---

## Images and the registry

**The image references say `registry.invalid`.** That is a placeholder, and it
is deliberate. `.invalid` is reserved by RFC 2606 and can never resolve, so an
image that is genuinely missing fails loudly rather than quietly pulling
whatever stranger happens to own that name on Docker Hub. A friendlier-looking
placeholder would be squattable.

When the group has decided whose Docker Hub account or organisation to use:

```powershell
.\kubernetes\scripts\set-registry.ps1 <namespace>
docker login
.\kubernetes\scripts\deploy.ps1 -Push
```

and commit the change, so everyone builds and pulls the same images.

**Until then everything still runs.** The course notes say Kubernetes cannot use
local images. That is true of kind and minikube; it is **not** true of Docker
Desktop, which is the runtime this group chose. A locally built image works,
confirmed by the kubelet itself:

```
Normal  Pulled  kubelet  Container image "registry.invalid/storycontent-manager:0.1.0"
                         already present on machine and can be accessed by the pod
```

Two things make that work, and both are in the manifests: an explicit version
tag rather than `:latest`, and `imagePullPolicy: IfNotPresent`. With `:latest`
the default policy is `Always`, which forces a registry lookup and fails.

Pushing to a registry is still what CI has to do, so `-Push` is there for when
the account exists. It is just not a precondition for the cluster to run.

### Two kinds of Dockerfile

Three services (`storycontent-manager`, `recap-manager`, `account-manager`)
have a Dockerfile starting with `COPY target/*.jar` and expect the jar to exist
already. The other three run Maven themselves in a build stage. `deploy.ps1`
handles both — it runs Maven in a container for the first kind — but unifying
them on the self-building pattern would be one fewer thing for CI to get right.

---

## Probes: what is wired up, and what is not

Each service should have **both** probes, and they answer different questions.
Liveness asks whether the process is alive; a failure restarts the pod.
Readiness asks whether it can serve; a failure takes it out of the Service
without restarting it. A service that cannot reach RabbitMQ is not broken and
should not be restarted — restarting will not bring the broker back — but it
must not receive traffic either.

Here is the honest state of it:

| Service | Liveness / readiness | Readiness reflects |
|---|---|---|
| `storycontent-manager` | `/actuator/health/liveness` and `/actuator/health/readiness` | process state **and the broker** |
| `recap-manager` | TCP on 8090 | the port being open |
| `scene-manager` | TCP on 8080 | the port being open |
| `account-manager` | TCP on 8092 | the port being open |
| `character-manager` | TCP on 8010 | the port being open |
| `scene-indexer` | **none** | — |

**Why four services are on TCP probes.** `spring-boot-starter-actuator` appears
in exactly one pom in this repository. Without it there is no
`/actuator/health` to ask, so the probe falls back to asking whether the port is
open. That is strictly weaker: a pod whose database connection pool is exhausted
still accepts TCP connections, and still gets traffic.

**Why `scene-indexer` has none at all.** It sets
`spring.main.web-application-type: none`, so there is no HTTP server and no
listening port — there is nothing for a probe to ask, over HTTP or over TCP.
Kubernetes will restart it if the process exits and will do nothing at all if it
hangs with its broker connection gone.

None of this is fixed here, because four of those five services belong to other
people. The fix is small and is written out below so the group can decide on it.

### The proposed fix, for the group

Per service, two changes:

**1. Add the dependency** (`pom.xml`):

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**2. Expose the health endpoint** (`application.properties` / `.yml`):

```properties
management.endpoints.web.exposure.include=health,info
management.endpoint.health.probes.enabled=true
```

Spring Boot detects that it is running on Kubernetes and splits
`/actuator/health/liveness` from `/actuator/health/readiness` by itself. Then
in this repository, swap `tcpSocket` for `httpGet` in that service's
`deployment.yaml`.

**Two services need something extra.**

`recap-manager`: its `SecurityConfig` ends in `anyRequest().authenticated()`, so
an unauthenticated probe gets `401` and Kubernetes counts that as a failed
probe. Adding the dependency alone would make things *worse* — the pod would
start failing probes it currently passes. `/actuator/health/**` has to be
permitted:

```java
authorize.requestMatchers("/actuator/health/**").permitAll();
```

`scene-indexer`: drop `spring.main.web-application-type: none`. It then starts a
web server that nothing routes to, which is fine — what makes the indexer
unreachable is that no Service object points at it, not that no port is open.
That is the cleaner mechanism anyway.

**What readiness should include** differs per service, and is worth deciding
rather than copying:

| Service | Readiness group | Why |
|---|---|---|
| `storycontent-manager` | `readinessState, rabbit, db` | every write both persists and publishes |
| `scene-manager` | `readinessState, mongo` | serves reads; does not publish on the read path |
| `scene-indexer` | `readinessState, rabbit, mongo` | consumes and projects, so it needs both |
| `recap-manager` | `readinessState, db` | serves from its own store |
| `character-manager` | `readinessState, db` | same |

Including a dependency the service does not need makes a pod drop out of the
Service for an outage it could have survived. Leaving out one it does need means
Kubernetes keeps routing requests to a pod that will fail every one of them.
Both are silent, and neither shows up until the cluster is under load.

**You do not have to change code to set this.** Every Spring property can come
from the environment, so a readiness group is one entry in `deployment.yaml`:

```yaml
- name: MANAGEMENT_ENDPOINT_HEALTH_GROUP_READINESS_INCLUDE
  value: readinessState,rabbit
```

That is how `storycontent-manager` gets its broker-aware readiness here without
a line of source being touched, and it is how graceful shutdown is turned on for
every service in this tree:

```yaml
- name: SERVER_SHUTDOWN
  value: graceful
- name: SPRING_LIFECYCLE_TIMEOUT_PER_SHUTDOWN_PHASE
  value: 30s
```

with `terminationGracePeriodSeconds: 45` on the pod, which must be the larger of
the two or Kubernetes kills the process in the middle of the work the setting
gave it time to finish.

---

## What verification actually proves

`verify.ps1` runs four checks. What each one establishes:

**1 — Gateway routing.** `POST /admin/v1/test-events/scene-content` through the
ingress returns `202`. That single request confirms the ingress is listening on
`localhost:80`, the route matches, the Service selector finds the pod and the
container port is right. It also publishes a real event, which check 3 then
looks for — one action, two pieces of evidence. A second request to `/scenes/...`
confirms a different route group reaches a different service; a `404` there is a
pass, because the point is *which process* said it, and the check looks at the
body to tell `scene-manager` apart from nginx.

**2 — Route groups.** Both Ingress objects are admitted at the same address with
different `limit-rps` values. That is what "one gateway with route groups"
looks like from outside: same controller, same address, different policy.

**3 — Queue topology.** Lists the exchanges, queues and bindings the broker
actually has. Expect to see `storycontent.events` — declared by
StoryContentManager at startup — and also `game.events`, declared by the Scenes
services, **with nothing bridging them**. That is a real, known problem and the
cluster is what makes it visible: StoryContentManager publishes
`scene-content.updated` to `storycontent.events`, while `scene-indexer` consumes
`scene.changed` from `game.events`. Neither half is broken on its own, neither
logs an error, and the queue simply stays empty. In separate compose files both
sides look healthy.

**4 — Readiness is real.** Scales RabbitMQ to zero and watches
`storycontent-manager`. Readiness should go false, the pod should leave the
Service endpoints, and the restart count should **not** change. That last part
is the point: the broker belongs in readiness and not in liveness, and this is
the check that proves the manifest actually achieves it rather than merely
claiming to. RabbitMQ is then scaled back and readiness should recover on its
own.

---

## Metrics: what is exposed, and what collects it

**Nothing collects them.** There is no Prometheus, no Grafana, no Loki and no
Promtail in this cluster, whatever the architecture diagram says. What the
cluster does is advertise where its numbers are, so that whoever installs a
collector does not also have to go looking for them.

Two sources, both annotated with `prometheus.io/scrape`, `prometheus.io/port`
and `prometheus.io/path` — the convention a plain Prometheus discovers targets
by:

| Source | Address | What it gives you |
|---|---|---|
| Gateway | `ingress-nginx-controller-metrics.ingress-nginx:10254` | requests, latencies, status codes, per ingress and per path |
| Broker | `rabbitmq.choose-your-fate:15692` | queue depths, publish and deliver rates, connections |

A Prometheus Operator install would want `ServiceMonitor` objects instead,
which need a CRD this cluster does not have. The annotations work either way,
so they are the cheaper bet until that choice is made.

### One thing to know about the gateway's metrics

`--enable-metrics` is **off by default** in this controller version, and the
way it fails is nasty. Port 10254 answers `200` either way. Without the flag the
body is 128 lines of the Go runtime describing itself and nothing else — a
collector pointed at it scrapes successfully, graphs cleanly, and shows nothing
about HTTP traffic, with no error to explain why. `setup.ps1` sets the flag. If
you ever rebuild the controller by hand, set it too:

```powershell
kubectl -n ingress-nginx exec deploy/ingress-nginx-controller -- curl -s http://127.0.0.1:10254/metrics | Select-String nginx_ingress_controller_requests
```

An empty result means the flag is missing. A result looks like this, and the
`ingress` label is the useful part — it makes the two route groups separable in
a query:

```
nginx_ingress_controller_requests{ingress="choose-your-fate-public",
  path="/scenes", service="scene-manager", status="404"} 30
```

### What is not exposed, and cannot be from here

**No service produces application metrics.** `spring-boot-starter-actuator` is
in one of six `pom.xml` files, and `micrometer-registry-prometheus` — the thing
that turns Actuator into a Prometheus endpoint — is in none of them. So there
are no per-service request rates, no JVM heap numbers, no consumer lag. No
cluster configuration can produce them; it is two lines per service, and it
belongs with each service's owner:

```xml
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

plus `management.endpoints.web.exposure.include=health,prometheus`. The same
`spring-boot-starter-actuator` that the probes section above asks for covers the
first half of this, so the two requests are really one.

### `metrics-server` is not installed, on purpose

`kubectl top`, the CPU and memory graphs in Lens and the Dashboard, and any
future HorizontalPodAutoscaler all need `metrics-server`. It is not here,
because on Docker Desktop it cannot work without turning off a certificate
check.

The kubelet serves its metrics over TLS with a certificate issued by its own
self-signed CA (`CN=docker-desktop-ca@…`), not by the cluster CA, and with no IP
SANs. `metrics-server` trusts the cluster CA, so the scrape fails with:

```
x509: cannot validate certificate for 192.168.65.3 because it doesn't contain any IP SANs
```

There is no configuration that makes verification succeed — the certificate is
simply not one the cluster can vouch for. The only working options are to skip
verification or to do without. On a single-node cluster on one laptop the risk
is close to nothing, but it is still a deliberate decision to disable a
certificate check, so it is the group's to take rather than the setup script's.

If you decide to take it, it is two commands:

```powershell
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/download/v0.9.1/components.yaml
```

```powershell
kubectl patch deployment metrics-server -n kube-system --type=json -p '[{\"op\":\"add\",\"path\":\"/spec/template/spec/containers/0/args/-\",\"value\":\"--kubelet-insecure-tls\"}]'
```

Without the second one the pod runs but never becomes ready, and the logs fill
with the x509 error above. To undo both, `kubectl delete -f` the same URL.

---

## Known gaps

Written down rather than left to be discovered.

- **Nothing scrapes the metrics.** The gateway and the broker expose them; no
  collector is installed. See above.
- **No service exposes application metrics.** Missing Micrometer. See above.
- **`metrics-server` is not installed**, so there are no CPU or memory numbers
  anywhere, including in Lens and the Dashboard. See above for why and how.
- **`scene-indexer` has no probes.** No listening port to probe. See above.
- **Four of five services have TCP probes only.** No Actuator. See above.
- **The two halves of the messaging system do not talk to each other.**
  Different exchanges, different routing keys. Check 3 shows it. Fixing it is a
  contract decision, not a deployment one.
- **`storycontent-manager` is still the walking skeleton on `main`.** It
  publishes events and has no persistence. `story-content-vault` is deployed and
  idle so that merging the branch which adds persistence is a change to the
  service and not also a change to this tree.
- **StoryPathManager does not exist**, so `recap-manager` runs with
  `STORYPATH_MODE=mock`. The recaps it produces are placeholder.
- **No Ollama in the cluster**, so `recap-manager` runs with `AI_ENABLED=false`
  rather than failing at call time with a connection error.
- **StoryContentManager declares its exchange but no consumer queues on
  `main`.** The branch that declares the full topology is not merged.
- **No monitoring.** Promtail, Loki, Grafana and Prometheus are on the
  architecture diagram and in no part of this repository. Nothing here pretends
  otherwise.
- **No autoscaling.** No HPA and no KEDA `ScaledObject`. KEDA can be installed
  with `setup.ps1 -WithKeda`, and nothing uses it yet.
- **The TLS certificate is self-signed**, and HTTP→HTTPS redirect is turned off
  so that `curl` and browsers do not stop to complain about it. TLS terminating
  at the gateway is the part that matters; a certificate anyone would trust is a
  different problem and not one a laptop solves.
- **`recapManager`'s schema file starts with a UTF-8 BOM**, which MySQL reads as
  the first token of the first statement. `setup.ps1` drops it when building the
  ConfigMap and says so. Worth fixing at the source.
- **One replica of RabbitMQ and one of each database.** A second replica of
  either would be a second independent instance, not a cluster. Real clustering
  needs stable network identities, which means a StatefulSet.

---

## When something is wrong

```powershell
kubectl -n choose-your-fate get pods                    # what is running
kubectl -n choose-your-fate describe pod <name>         # why it is not
kubectl -n choose-your-fate logs <name>                 # what it said
kubectl -n choose-your-fate logs <name> -c wait-for-rabbitmq   # stuck in an initContainer
kubectl -n choose-your-fate get endpoints               # is anything behind the Services
kubectl -n ingress-nginx logs deploy/ingress-nginx-controller  # gateway
```

| Symptom | Usually |
|---|---|
| `CreateContainerConfigError` | a secret is missing — run `setup.ps1` |
| `ErrImagePull` with `registry.invalid` | the image was not built — run `deploy.ps1` without `-SkipBuild` |
| Stuck in `Init:0/1` | the dependency it is waiting for is not up; look at the database or broker pod |
| `503` from the gateway | the route matched but no ready endpoint was behind it — check the pod's readiness |
| `404` with an nginx body | no route matched that path — check `ingress/ingress-public.yaml` |
| MySQL authentication failures after a teardown | the volume survived with the old password while the secret was regenerated — `teardown.ps1 -IncludeData` and start over |
| `Probe failed: command timed out` | see below |
| `NoClassDefFoundError` for a class you cannot find in the source | a stale `target/` — see below |

### Two that cost real time while this was built

Both are written down because neither is obvious and both look like something
else.

**A healthy container in a restart loop caused by its own probe.** Watch for
`Liveness probe failed: command timed out` while the container's own log says it
started fine. An `exec` probe that starts a runtime — `rabbitmq-diagnostics`
boots a second Erlang VM, `mongosh` is a Node process — can easily take longer
than the probe's timeout, and **`timeoutSeconds` defaults to 1 second**. A probe
that times out counts as failed, and the kubelet cannot tell a slow answer from
no answer, so Kubernetes kills a perfectly healthy process.

It bites hardest under load, which is exactly when restarting the broker is the
worst thing to do. RabbitMQ here logged `Time to start RabbitMQ: 129435 ms` on a
laptop that was also building six images.

The fix in this tree: RabbitMQ is probed over HTTP against the Prometheus
endpoint on 15692 and over TCP on 5672 — a socket instead of a runtime — and
every remaining `exec` probe has an explicit, generous `timeoutSeconds`. If you
add a service, prefer an HTTP or TCP probe, and if you must exec something,
measure what it costs before choosing a timeout.

**A jar containing classes from a branch you are not on.** `target/` is in
`.gitignore`, so it survives `git checkout` and git shows nothing. Running
`mvn package` without `clean` keeps whatever is already in `target/classes` and
packages it next to the current sources, producing a jar that holds two
branches at once. It surfaces as a `NoClassDefFoundError` at startup, for a
dependency the current `pom.xml` never declared, naming a class whose source
file is not in your checkout.

`deploy.ps1` always runs `clean package` for this reason. If you build by hand,
do the same.
