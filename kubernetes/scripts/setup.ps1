<#
.SYNOPSIS
    One-time setup for the Choose Your Fate cluster.

.DESCRIPTION
    Everything here is done once per machine. Separate from the deploy step
    because it creates things that must NOT be in version control (passwords,
    keys, certificates) and installs a controller outside this namespace.

    Safe to re-run. Secrets are created only when missing and never overwritten:
    a database bakes its root password into the data directory on first start, so
    regenerating one against an existing volume locks the service out of its own
    database, with an error that looks nothing like the cause.

.PARAMETER WithKeda
    Also install KEDA. Nothing in this tree uses it yet; it is the autoscaler the
    serverless text-to-speech work and the scene indexer will need.

.PARAMETER WithDashboard
    Also install the Kubernetes Dashboard, a read-only web UI. Off by default:
    five more containers for a view Lens already gives.

    It is upstream's Helm chart rendered to YAML inside a container, so Helm is
    not a prerequisite on the machine.

.EXAMPLE
    .\kubernetes\scripts\setup.ps1
#>
[CmdletBinding()]
param(
    [switch]$WithKeda,
    [switch]$WithDashboard
)

$ErrorActionPreference = 'Stop'

$Namespace         = 'choose-your-fate'
$IngressVersion    = 'controller-v1.15.1'
$KedaVersion       = 'v2.17.2'
$DashboardVersion  = '7.14.0'
$HelmImage         = 'alpine/helm:3.16.3'
$RepoRoot          = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$LocalDir          = Join-Path $RepoRoot '.local'

function Write-Step  { param([string]$Text) Write-Host ''; Write-Host "==> $Text" -ForegroundColor Cyan }
function Write-Ok    { param([string]$Text) Write-Host "    [ok]   $Text" -ForegroundColor Green }
function Write-Kept  { param([string]$Text) Write-Host "    [keep] $Text" -ForegroundColor DarkGray }
function Write-Note  { param([string]$Text) Write-Host "    [note] $Text" -ForegroundColor Yellow }

function New-RandomPassword {
    param([int]$Length = 28)
    # Letters and digits only. These travel through JDBC URLs, MongoDB connection
    # strings, shell quoting and YAML, and a slash or an at-sign breaks in exactly
    # one of those places.
    $alphabet = [char[]]((48..57) + (65..90) + (97..122))
    $bytes = New-Object byte[] $Length
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    $chars = foreach ($b in $bytes) { $alphabet[$b % $alphabet.Length] }
    return (-join $chars)
}

function Get-OpenSslPath {
    $onPath = Get-Command openssl -ErrorAction SilentlyContinue
    if ($onPath) { return $onPath.Source }
    # Git for Windows ships one, but it is not on PATH in every shell.
    $candidates = @(
        (Join-Path $env:ProgramFiles 'Git\usr\bin\openssl.exe'),
        (Join-Path $env:ProgramFiles 'Git\ucrt64\bin\openssl.exe'),
        (Join-Path $env:ProgramFiles 'Git\mingw64\bin\openssl.exe')
    )
    foreach ($candidate in $candidates) {
        if (Test-Path -LiteralPath $candidate) { return $candidate }
    }
    return $null
}

function Invoke-Quietly {
    <#
        Runs a native command purely for its exit code.

        $ErrorActionPreference is lowered for the duration: Windows PowerShell
        turns anything a native executable writes to stderr into a
        NativeCommandError, which with Stop aborts the script -- so an ordinary
        "not found" from kubectl would read as a crash.
    #>
    param([scriptblock]$Command)
    $previous = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        & $Command 2>$null | Out-Null
        return ($LASTEXITCODE -eq 0)
    } finally {
        $ErrorActionPreference = $previous
    }
}

function Test-SecretExists {
    param([string]$Name)
    $previous = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $found = & kubectl get secret $Name -n $Namespace --ignore-not-found -o name 2>$null
        return [bool]$found
    } finally {
        $ErrorActionPreference = $previous
    }
}

function Invoke-Kubectl {
    param([string[]]$Arguments, [string]$What)
    & kubectl @Arguments
    if ($LASTEXITCODE -ne 0) { throw "kubectl failed while $What (exit code $LASTEXITCODE)" }
}

# ---------------------------------------------------------------------------
# 1. The cluster
# ---------------------------------------------------------------------------
Write-Step 'Checking the cluster'

if (-not (Get-Command kubectl -ErrorAction SilentlyContinue)) {
    throw 'kubectl is not on PATH. Install it, then run this again.'
}

if (-not (Invoke-Quietly { docker version --format '{{.Server.Version}}' })) {
    throw 'Docker Desktop is not running. Start it, wait for the whale to settle, then run this again.'
}

$context = kubectl config current-context 2>$null
if ($LASTEXITCODE -ne 0) { throw 'kubectl has no current context.' }
if ($context -ne 'docker-desktop') {
    Write-Note "The current context is '$context', not 'docker-desktop'."
    Write-Note 'Switch with:  kubectl config use-context docker-desktop'
    throw 'Refusing to install into a context this project does not own.'
}

if (-not (Invoke-Quietly { kubectl get nodes })) {
    Write-Note 'The cluster did not answer. In Docker Desktop: Settings > Kubernetes > Enable Kubernetes (Kubeadm).'
    Write-Note 'If it will not come up, the usual cause on this course is WSL being out of date:'
    Write-Note '    wsl -v'
    Write-Note '    wsl --update'
    throw 'No reachable Kubernetes cluster.'
}
Write-Ok "context $context, node reachable"

# ---------------------------------------------------------------------------
# 2. The namespace
# ---------------------------------------------------------------------------
Write-Step 'Creating the namespace'
Invoke-Kubectl @('apply', '-f', (Join-Path $PSScriptRoot '..\00-namespace\namespace.yaml')) 'creating the namespace'

# ---------------------------------------------------------------------------
# 3. The ingress controller
# ---------------------------------------------------------------------------
# Pinned to a version so everyone gets the same controller. It goes in its own
# namespace because it is cluster infrastructure and outlives this project.
#
# The "cloud" provider manifest asks for a LoadBalancer Service, which Docker
# Desktop binds to localhost -- that is why the gateway is at http://localhost/
# with no port and no tunnel.
Write-Step "Installing ingress-nginx ($IngressVersion)"
$ingressUrl = "https://raw.githubusercontent.com/kubernetes/ingress-nginx/$IngressVersion/deploy/static/provider/cloud/deploy.yaml"
Invoke-Kubectl @('apply', '-f', $ingressUrl) 'installing ingress-nginx'

# --enable-metrics is off by default in this controller version, and port 10254
# answers 200 either way -- without the flag it serves only the Go runtime's own
# numbers, nothing about the gateway's traffic.
#
# Patched rather than edited into the upstream manifest, which is applied from a
# pinned URL. Done before the readiness wait, so the controller starts once with
# the flag instead of being restarted into it.
$controllerArgs = kubectl get deployment ingress-nginx-controller --namespace ingress-nginx `
    -o jsonpath='{.spec.template.spec.containers[0].args}' 2>$null
if ($controllerArgs -and $controllerArgs -notmatch '--enable-metrics') {
    $patch = '[{"op":"add","path":"/spec/template/spec/containers/0/args/-","value":"--enable-metrics=true"}]'
    Invoke-Kubectl @('patch', 'deployment', 'ingress-nginx-controller', '--namespace', 'ingress-nginx',
        '--type=json', '-p', $patch) 'enabling controller metrics'
    Write-Ok 'metrics enabled on the controller'
} else {
    Write-Kept 'controller metrics already enabled'
}

# Our own Service rather than a port added to the upstream one: that Service is
# replaced wholesale every time the pinned manifest is re-applied, so an added
# port would disappear with it.
$ingressMetricsService = @'
apiVersion: v1
kind: Service
metadata:
  name: ingress-nginx-controller-metrics
  namespace: ingress-nginx
  labels:
    app.kubernetes.io/name: ingress-nginx
    app.kubernetes.io/component: controller-metrics
  annotations:
    prometheus.io/scrape: "true"
    prometheus.io/port: "10254"
    prometheus.io/path: /metrics
spec:
  type: ClusterIP
  selector:
    app.kubernetes.io/name: ingress-nginx
    app.kubernetes.io/component: controller
  ports:
    - name: metrics
      port: 10254
      targetPort: 10254
      protocol: TCP
'@
$ingressMetricsService | kubectl apply -f -
if ($LASTEXITCODE -ne 0) { throw 'could not create the ingress metrics Service' }
Write-Ok 'ingress-nginx-controller-metrics (nothing scrapes it yet)'

Write-Host '    waiting for the controller to become ready (up to 5 minutes)...'
# rollout status, not "wait --for=condition=Ready pod". The apply returns before
# the controller has created any pod, and a pod selector that matches nothing
# fails immediately instead of waiting for one to appear.
kubectl rollout status deployment/ingress-nginx-controller --namespace ingress-nginx --timeout=300s
if ($LASTEXITCODE -ne 0) {
    Write-Note 'The controller did not become ready in time. Look at:  kubectl -n ingress-nginx get pods'
} else {
    Write-Ok 'ingress-nginx is ready'
}

# ---------------------------------------------------------------------------
# 4. Secrets
# ---------------------------------------------------------------------------
# None of this is committed. The passwords exist only inside the cluster; the JWT
# keypair goes to .local/ at the repository root, which .gitignore excludes.
Write-Step 'Creating secrets (only the ones that are missing)'

if (-not (Test-Path -LiteralPath $LocalDir)) {
    New-Item -ItemType Directory -Path $LocalDir | Out-Null
}

# -- broker ------------------------------------------------------------------
if (Test-SecretExists 'rabbitmq-credentials') {
    Write-Kept 'rabbitmq-credentials already exists'
} else {
    $rabbitPassword = New-RandomPassword
    Invoke-Kubectl @('create', 'secret', 'generic', 'rabbitmq-credentials',
        '-n', $Namespace,
        '--from-literal=username=choose-your-fate',
        "--from-literal=password=$rabbitPassword") 'creating rabbitmq-credentials'
    Write-Ok 'rabbitmq-credentials'
}

# -- one password per database, because there is one database per service ----
foreach ($vault in @('story-content-vault', 'recap-vault', 'character-vault')) {
    $secretName = "$vault-credentials"
    if (Test-SecretExists $secretName) {
        Write-Kept "$secretName already exists"
        continue
    }
    $vaultPassword = New-RandomPassword
    Invoke-Kubectl @('create', 'secret', 'generic', $secretName,
        '-n', $Namespace,
        "--from-literal=root-password=$vaultPassword") "creating $secretName"
    Write-Ok $secretName
}

# -- mongo, which needs whole connection strings -----------------------------
# The whole URI lives in the secret: assembling it in the Deployment would put
# the password there in plain text.
if (Test-SecretExists 'scene-read-model-credentials') {
    Write-Kept 'scene-read-model-credentials already exists'
} else {
    $mongoRoot    = New-RandomPassword
    $mongoIndexer = New-RandomPassword
    $mongoManager = New-RandomPassword
    Invoke-Kubectl @('create', 'secret', 'generic', 'scene-read-model-credentials',
        '-n', $Namespace,
        '--from-literal=root-username=root',
        "--from-literal=root-password=$mongoRoot",
        "--from-literal=indexer-password=$mongoIndexer",
        "--from-literal=manager-password=$mongoManager",
        "--from-literal=indexer-uri=mongodb://indexer:$mongoIndexer@scene-read-model:27017/scenes?authSource=scenes",
        "--from-literal=manager-uri=mongodb://manager:$mongoManager@scene-read-model:27017/scenes?authSource=scenes") 'creating scene-read-model-credentials'
    Write-Ok 'scene-read-model-credentials'
}

# -- the JWT verification key ------------------------------------------------
# One keypair for the system: AccountManager hands out the public half over SOAP
# and RecapManager verifies tokens against it. The private half goes to .local/
# so tokens can be minted by hand until AccountManager can sign them itself.
$openssl    = Get-OpenSslPath
$privatePem = Join-Path $LocalDir 'jwt-private.pem'
$publicPem  = Join-Path $LocalDir 'jwt-public.pem'

if (-not (Test-Path -LiteralPath $publicPem)) {
    if (-not $openssl) {
        throw "openssl was not found, so the JWT keypair cannot be generated. Install Git for Windows or OpenSSL, or put an RSA public key at $publicPem yourself."
    }
    Write-Host '    generating a 2048-bit RSA keypair...'
    # Through Invoke-Quietly: openssl writes its progress dots to stderr, which
    # PowerShell would otherwise treat as a failure.
    if (-not (Invoke-Quietly { & $openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out $privatePem })) {
        throw 'openssl could not generate the private key.'
    }
    if (-not (Invoke-Quietly { & $openssl rsa -pubout -in $privatePem -out $publicPem })) {
        throw 'openssl could not derive the public key.'
    }
    Write-Ok "keypair written to $LocalDir (the private half never leaves that folder)"
} else {
    Write-Kept 'a JWT keypair already exists in .local'
}

if (Test-SecretExists 'jwt-public-key') {
    Write-Kept 'jwt-public-key already exists'
} else {
    Invoke-Kubectl @('create', 'secret', 'generic', 'jwt-public-key',
        '-n', $Namespace,
        "--from-file=public.pem=$publicPem") 'creating jwt-public-key'
    Write-Ok 'jwt-public-key'
}

# The same public key, where the two compose files expect to bind-mount it. Both
# point at a path that need not exist, and a missing file there fails confusingly.
$keyTargets = @(
    @{ Dir = (Join-Path $RepoRoot 'microservices\recapManager\.local');   File = 'recap-jwt-public.pem' },
    @{ Dir = (Join-Path $RepoRoot 'microservices\accountManager\.local'); File = 'account-jwt-public.pem' }
)
foreach ($target in $keyTargets) {
    if (-not (Test-Path -LiteralPath $target.Dir)) {
        New-Item -ItemType Directory -Path $target.Dir | Out-Null
    }
    $destination = Join-Path $target.Dir $target.File
    if (-not (Test-Path -LiteralPath $destination)) {
        Copy-Item -LiteralPath $publicPem -Destination $destination
        Write-Ok "public key copied to $destination"
    } else {
        Write-Kept "$destination already exists"
    }
}

# -- TLS for the gateway -----------------------------------------------------
# Self-signed, for localhost. What matters is that TLS terminates at the gateway
# and not in the services; a certificate browsers trust is a different problem.
$tlsCrt = Join-Path $LocalDir 'gateway-tls.crt'
$tlsKey = Join-Path $LocalDir 'gateway-tls.key'

if (Test-SecretExists 'choose-your-fate-tls') {
    Write-Kept 'choose-your-fate-tls already exists'
} else {
    if (-not (Test-Path -LiteralPath $tlsCrt)) {
        if (-not $openssl) { throw 'openssl was not found, so the gateway certificate cannot be generated.' }
        Write-Host '    generating a self-signed certificate for localhost...'
        $opensslArgs = @(
            'req', '-x509', '-nodes', '-newkey', 'rsa:2048', '-days', '365',
            '-keyout', $tlsKey, '-out', $tlsCrt,
            '-subj', '/CN=localhost/O=choose-your-fate',
            '-addext', 'subjectAltName=DNS:localhost,IP:127.0.0.1'
        )
        if (-not (Invoke-Quietly { & $openssl @opensslArgs })) {
            throw 'openssl could not generate the certificate.'
        }
    }
    Invoke-Kubectl @('create', 'secret', 'tls', 'choose-your-fate-tls',
        '-n', $Namespace,
        "--cert=$tlsCrt", "--key=$tlsKey") 'creating choose-your-fate-tls'
    Write-Ok 'choose-your-fate-tls'
}

# ---------------------------------------------------------------------------
# 5. Database schemas, as ConfigMaps built from the service folders
# ---------------------------------------------------------------------------
# The SQL is not copied into kubernetes/. There is one schema file per service,
# in that service's own folder, and this reads it from there.
#
# Refreshed on every run, unlike the secrets: they hold no credentials. MySQL
# runs init scripts only against an empty data directory, so refreshing the
# ConfigMap does not apply a changed schema to an existing database. The README
# says how to reset one.
Write-Step 'Building database schema ConfigMaps from the service folders'

function Set-SqlConfigMap {
    param(
        [string]$Name,
        [string]$SourcePath,
        [string]$KeyName
    )
    if (-not (Test-Path -LiteralPath $SourcePath)) {
        Write-Note "$SourcePath does not exist, so $Name was skipped. That database will start empty."
        return
    }

    # MySQL feeds init scripts straight into its client, which reads a BOM as the
    # first token of the first statement and fails with a syntax error pointing at
    # a line that looks fine. recapManager's schema file has one.
    #
    # ReadAllText drops the BOM and WriteAllText with UTF8Encoding($false) does not
    # put one back, so the rewrite below removes it. This check only reports it.
    $firstBytes = [System.IO.File]::ReadAllBytes($SourcePath) | Select-Object -First 3
    if ($firstBytes.Count -eq 3 -and $firstBytes[0] -eq 0xEF -and $firstBytes[1] -eq 0xBB -and $firstBytes[2] -eq 0xBF) {
        $fileName = [System.IO.Path]::GetFileName($SourcePath)
        Write-Note "$fileName starts with a UTF-8 BOM, which MySQL chokes on. It is left out of the ConfigMap -- worth fixing at the source."
    }

    $text = [System.IO.File]::ReadAllText($SourcePath)
    $temp = Join-Path ([System.IO.Path]::GetTempPath()) $KeyName
    [System.IO.File]::WriteAllText($temp, $text, (New-Object System.Text.UTF8Encoding $false))

    $rendered = kubectl create configmap $Name -n $Namespace "--from-file=$KeyName=$temp" --dry-run=client -o yaml
    if ($LASTEXITCODE -ne 0) { throw "could not render ConfigMap $Name" }
    $rendered | kubectl apply -f -
    if ($LASTEXITCODE -ne 0) { throw "could not apply ConfigMap $Name" }
    Remove-Item -LiteralPath $temp -Force
    Write-Ok $Name
}

Set-SqlConfigMap -Name 'recap-vault-init' -KeyName '01_schema_recap_vault.sql' `
    -SourcePath (Join-Path $RepoRoot 'microservices\recapManager\src\main\resources\01_schema_recap_vault.sql')

Set-SqlConfigMap -Name 'character-vault-init' -KeyName '01_create_schema.sql' `
    -SourcePath (Join-Path $RepoRoot 'microservices\character-manager-service\character-vault\01_create_schema.sql')

# ---------------------------------------------------------------------------
# 6. KEDA, on request
# ---------------------------------------------------------------------------
if ($WithKeda) {
    Write-Step "Installing KEDA ($KedaVersion)"
    $kedaPlain = $KedaVersion.TrimStart('v')
    $kedaUrl = "https://github.com/kedacore/keda/releases/download/$KedaVersion/keda-$kedaPlain.yaml"
    # Server-side apply: the KEDA CRDs have annotations too large for client-side
    # apply.
    Invoke-Kubectl @('apply', '--server-side', '-f', $kedaUrl) 'installing KEDA'
    kubectl wait --namespace keda --for=condition=Available deployment --all --timeout=300s
    if ($LASTEXITCODE -ne 0) {
        Write-Note 'KEDA did not become available in time. Look at:  kubectl -n keda get pods'
    } else {
        Write-Ok 'KEDA is ready. Nothing in this tree uses it yet.'
    }
} else {
    Write-Host ''
    Write-Host '    KEDA was not installed. Add -WithKeda when the first ScaledObject needs it.' -ForegroundColor DarkGray
}

# ---------------------------------------------------------------------------
# 7. The Kubernetes Dashboard, on request
# ---------------------------------------------------------------------------
# Upstream stopped shipping a plain manifest after 2.7.0; everything since is a
# Helm chart. Rather than make Helm a prerequisite for the whole project over one
# optional dev tool, the chart is rendered to YAML in a container and applied with
# kubectl.
#
# "helm template" is a local text transformation and needs no cluster access, so
# the container never talks to the API server. The rendered file is ordinary YAML
# that can be read and diffed before it is applied.
if ($WithDashboard) {
    Write-Step "Installing the Kubernetes Dashboard ($DashboardVersion)"

    if (-not (Invoke-Quietly { docker version --format '{{.Server.Version}}' })) {
        throw 'Docker is needed to render the dashboard chart, and it is not running.'
    }

    $chartDir = Join-Path ([System.IO.Path]::GetTempPath()) "cyf-dashboard-$DashboardVersion"
    New-Item -ItemType Directory -Force -Path $chartDir | Out-Null
    $chartFile    = Join-Path $chartDir 'chart.tgz'
    $renderedFile = Join-Path $chartDir 'rendered.yaml'
    $helmErrFile  = Join-Path $chartDir 'helm-stderr.txt'

    if (-not (Test-Path -LiteralPath $chartFile)) {
        $chartUrl = "https://github.com/kubernetes/dashboard/releases/download/kubernetes-dashboard-$DashboardVersion/kubernetes-dashboard-$DashboardVersion.tgz"
        Write-Host '    downloading the chart...'
        Invoke-WebRequest -Uri $chartUrl -OutFile $chartFile
    }

    Write-Host '    rendering it to YAML...'
    # docker writes its pull progress to stderr, which with Stop ends the script
    # even when the command succeeds. Same trap as openssl and kubectl above.
    $previous = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        & docker run --rm -v "${chartDir}:/chart" $HelmImage `
            template kubernetes-dashboard /chart/chart.tgz `
            --namespace kubernetes-dashboard 1>$renderedFile 2>$helmErrFile
    } finally {
        $ErrorActionPreference = $previous
    }
    if ($LASTEXITCODE -ne 0) {
        Get-Content -LiteralPath $helmErrFile -ErrorAction SilentlyContinue | Select-Object -First 10 | ForEach-Object { Write-Note $_ }
        throw "rendering the dashboard chart failed (exit code $LASTEXITCODE)"
    }

    kubectl create namespace kubernetes-dashboard --dry-run=client -o yaml | kubectl apply -f -
    if ($LASTEXITCODE -ne 0) { throw 'could not create the kubernetes-dashboard namespace' }

    # --namespace on the apply is not optional. "helm template --namespace X"
    # does not write metadata.namespace into the objects it renders unless the
    # chart templates it, and kubectl then puts them wherever the current context
    # points.
    #
    # This chart templates the namespace on its Kong subchart and nowhere else,
    # so without the flag Kong lands in kubernetes-dashboard and the other four
    # deployments in default, looking healthy. The only symptom is Kong stuck on
    # a ConfigMap "not found" that exists -- in the other namespace.
    Invoke-Kubectl @('apply', '--namespace', 'kubernetes-dashboard', '-f', $renderedFile) 'installing the dashboard'

    # The dashboard has no users of its own: you log in with a ServiceAccount
    # token and see what that account may see, so the role is the choice.
    #
    # This binds the built-in "view" role: read-only across the cluster, and it
    # cannot read Secrets. Most guides use cluster-admin instead. Changing the
    # roleRef below to cluster-admin would hand anyone holding a token full
    # control of the cluster, Secrets included.
    $dashboardAccount = @'
apiVersion: v1
kind: ServiceAccount
metadata:
  name: dashboard-viewer
  namespace: kubernetes-dashboard
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: dashboard-viewer
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: view
subjects:
  - kind: ServiceAccount
    name: dashboard-viewer
    namespace: kubernetes-dashboard
'@
    $dashboardAccount | kubectl apply -f -
    if ($LASTEXITCODE -ne 0) { throw 'could not create the dashboard-viewer account' }

    kubectl wait --namespace kubernetes-dashboard --for=condition=Available deployment --all --timeout=300s
    if ($LASTEXITCODE -ne 0) {
        Write-Note 'The dashboard did not become available in time. Look at:  kubectl -n kubernetes-dashboard get pods'
    } else {
        Write-Ok 'the dashboard is ready (read-only, as the dashboard-viewer account)'
        Write-Host ''
        Write-Host '    Open it with these two commands, in two terminals:' -ForegroundColor DarkGray
        Write-Host '        kubectl -n kubernetes-dashboard port-forward svc/kubernetes-dashboard-kong-proxy 8443:443' -ForegroundColor DarkGray
        Write-Host '        kubectl -n kubernetes-dashboard create token dashboard-viewer' -ForegroundColor DarkGray
        Write-Host '    then https://localhost:8443/ and paste the token. See kubernetes/README.md.' -ForegroundColor DarkGray
    }
} else {
    Write-Host ''
    Write-Host '    The dashboard was not installed. Add -WithDashboard if you want a web UI.' -ForegroundColor DarkGray
}

Write-Host ''
Write-Host 'Setup complete.' -ForegroundColor Green
Write-Host 'Next:  .\kubernetes\scripts\deploy.ps1' -ForegroundColor Green
