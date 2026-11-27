<#
.SYNOPSIS
    Build the images and deploy the whole system.

.DESCRIPTION
    This is the command to run day to day. It does two things:

        1. builds a container image for each service
        2. kubectl apply -R -f kubernetes

    Step 2 is the whole deploy and works on its own once the images exist.
    Nothing has to be applied by hand first or in a particular order: where
    ordering matters it is handled by readiness probes and initContainers.

    IMAGES AND THE REGISTRY
    Docker Desktop's Kubernetes can use a locally built image, which is not true
    of kind or minikube. Two things make it work: an explicit version tag rather
    than :latest, and imagePullPolicy: IfNotPresent. The kubelet then reports the
    image as "already present on machine" and never contacts a registry.

    So the cluster runs without a registry. Pushing to Docker Hub is still what
    CI will do, and -Push is here for when the account exists. Until then the
    image names carry registry.invalid, a reserved domain that can never resolve,
    so a missing image fails loudly instead of pulling a stranger's image with
    the same name.

.PARAMETER SkipBuild
    Apply the manifests without rebuilding. Useful when only a manifest changed.

.PARAMETER Push
    Also push each image to the registry named in the manifests. Fails early if
    that is still the placeholder.

.PARAMETER Only
    Build only the named services. Accepts several.

.EXAMPLE
    .\kubernetes\scripts\deploy.ps1

.EXAMPLE
    .\kubernetes\scripts\deploy.ps1 -Only scene-manager, scene-indexer

.EXAMPLE
    .\kubernetes\scripts\deploy.ps1 -Push
#>
[CmdletBinding()]
param(
    [switch]$SkipBuild,
    [switch]$Push,
    [string[]]$Only
)

$ErrorActionPreference = 'Stop'

$Namespace     = 'choose-your-fate'
$RepoRoot      = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$KubernetesDir = Join-Path $RepoRoot 'kubernetes'
$MavenImage    = 'maven:3.9-eclipse-temurin-21'
$MavenCache    = 'choose-your-fate-m2'

function Write-Step { param([string]$Text) Write-Host ''; Write-Host "==> $Text" -ForegroundColor Cyan }
function Write-Ok   { param([string]$Text) Write-Host "    [ok]   $Text" -ForegroundColor Green }
function Write-Note { param([string]$Text) Write-Host "    [note] $Text" -ForegroundColor Yellow }

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

function Test-KubeObject {
    param([string]$Kind, [string]$Name)
    $previous = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $found = & kubectl get $Kind $Name -n $Namespace --ignore-not-found -o name 2>$null
        return [bool]$found
    } finally {
        $ErrorActionPreference = $previous
    }
}

# Each service, where its Dockerfile lives, and whether a jar has to exist
# before the image can be built.
#
# Three of these Dockerfiles start with "COPY target/*.jar" and expect the jar
# to be there; the other three run Maven in a build stage. Unifying them on the
# self-building pattern would be one fewer step for CI to get right.
$Services = @(
    @{ Name = 'storycontent-manager'; Path = 'microservices\storycontent-manager';      NeedsJar = $true  },
    @{ Name = 'recap-manager';        Path = 'microservices\recapManager';              NeedsJar = $true  },
    @{ Name = 'account-manager';      Path = 'microservices\accountManager';            NeedsJar = $true  },
    @{ Name = 'character-manager';    Path = 'microservices\character-manager-service'; NeedsJar = $false },
    @{ Name = 'scene-manager';        Path = 'microservices\Scenes\scene-manager';      NeedsJar = $false },
    @{ Name = 'scene-indexer';        Path = 'microservices\Scenes\scene-indexer';      NeedsJar = $false }
)

function Get-ImageReference {
    <#
        Reads the image reference straight out of the service's deployment.yaml,
        so that what gets built is by construction what the cluster will run.
        Keeping the tag in a variable here instead would be a second place to
        edit, and the two would eventually disagree.
    #>
    param([string]$ServiceName)

    $manifest = Join-Path $KubernetesDir "$ServiceName\deployment.yaml"
    if (-not (Test-Path -LiteralPath $manifest)) {
        throw "No deployment.yaml for $ServiceName at $manifest"
    }
    $content = Get-Content -LiteralPath $manifest -Raw
    # Anchored on the service name, so the busybox initContainer image is not
    # mistaken for the application image.
    $pattern = 'image:\s*(\S+/' + [regex]::Escape($ServiceName) + ':\S+)'
    $match = [regex]::Match($content, $pattern)
    if (-not $match.Success) {
        throw "Could not find an image reference for $ServiceName in $manifest"
    }
    return $match.Groups[1].Value
}

# ---------------------------------------------------------------------------
# Preflight
# ---------------------------------------------------------------------------
Write-Step 'Checking prerequisites'

if (-not (Invoke-Quietly { docker version --format '{{.Server.Version}}' })) {
    throw 'Docker Desktop is not running. Start it, wait for the whale to settle, then run this again.'
}

if (-not (Invoke-Quietly { kubectl get namespace $Namespace })) {
    throw "The namespace '$Namespace' does not exist. Run .\kubernetes\scripts\setup.ps1 first."
}

# The manifests reference secrets by name. Without them the pods stop at
# CreateContainerConfigError.
$requiredSecrets = @(
    'rabbitmq-credentials',
    'story-content-vault-credentials',
    'recap-vault-credentials',
    'character-vault-credentials',
    'scene-read-model-credentials',
    'jwt-public-key',
    'choose-your-fate-tls'
)
$missingSecrets = @()
foreach ($secret in $requiredSecrets) {
    if (-not (Test-KubeObject 'secret' $secret)) { $missingSecrets += $secret }
}
if ($missingSecrets.Count -gt 0) {
    throw "Missing secrets: $($missingSecrets -join ', '). Run .\kubernetes\scripts\setup.ps1 first."
}
Write-Ok 'docker, namespace and secrets are in place'

# ---------------------------------------------------------------------------
# Build
# ---------------------------------------------------------------------------
$toBuild = $Services
if ($Only) {
    $toBuild = $Services | Where-Object { $Only -contains $_.Name }
    $unknown = $Only | Where-Object { ($Services.Name) -notcontains $_ }
    if ($unknown) { throw "Unknown service(s): $($unknown -join ', ')" }
}

if (-not $SkipBuild) {
    Write-Step 'Building images'

    $needsMaven = @($toBuild | Where-Object { $_.NeedsJar })
    if ($needsMaven.Count -gt 0) {
        # Maven runs in a container, so the only tool this script needs is
        # Docker. The named volume keeps the dependency cache between runs.
        Invoke-Quietly { docker volume create $MavenCache } | Out-Null
        Write-Note 'The first run fills the Maven cache and can take ten minutes or more.'
        Write-Note 'Maven and docker build are quiet here, so a long pause is normal, not a hang.'
    }

    foreach ($service in $toBuild) {
        $serviceDir = Join-Path $RepoRoot $service.Path
        if (-not (Test-Path -LiteralPath $serviceDir)) {
            throw "$($service.Name): $serviceDir does not exist"
        }
        $image = Get-ImageReference $service.Name

        if ($service.NeedsJar) {
            Write-Host "    [maven] $($service.Name)"
            # "clean package", never bare "package". target/ is in .gitignore, so
            # it survives a branch switch and git shows nothing. Without clean,
            # Maven packages whatever is already in target/classes alongside the
            # current sources, and the jar holds two branches at once.
            #
            # It surfaces at runtime as a NoClassDefFoundError for a class whose
            # source file is not in the checkout. That happened here.
            $mavenArgs = @(
                'run', '--rm',
                '-v', "$($serviceDir):/workspace",
                '-v', "$($MavenCache):/root/.m2",
                '-w', '/workspace',
                $MavenImage,
                'mvn', '-B', '-q', 'clean', 'package', '-DskipTests'
            )
            & docker @mavenArgs
            if ($LASTEXITCODE -ne 0) { throw "$($service.Name): maven package failed" }
        }

        Write-Host "    [image] $image"
        & docker build -q -t $image $serviceDir | Out-Null
        if ($LASTEXITCODE -ne 0) { throw "$($service.Name): docker build failed" }
        Write-Ok $image

        if ($Push) {
            if ($image -like 'registry.invalid/*') {
                throw 'The manifests still use the registry.invalid placeholder. Run .\kubernetes\scripts\set-registry.ps1 <your-docker-hub-namespace> first.'
            }
            & docker push $image
            if ($LASTEXITCODE -ne 0) { throw "$($service.Name): docker push failed. Are you logged in? (docker login)" }
            Write-Ok "pushed $image"
        }
    }
} else {
    Write-Step 'Skipping the build (-SkipBuild)'
}

# ---------------------------------------------------------------------------
# Deploy
# ---------------------------------------------------------------------------
Write-Step 'Applying the manifests'
Write-Host "    kubectl apply -R -f kubernetes" -ForegroundColor DarkGray

Push-Location $RepoRoot
try {
    kubectl apply -R -f kubernetes
    if ($LASTEXITCODE -ne 0) { throw 'kubectl apply failed' }
} finally {
    Pop-Location
}

# Rebuilt images keep the same tag, so apply sees an unchanged Deployment and
# creates no new pod: the old one keeps running the old code and the deploy
# silently does nothing. A rollout restart is what makes the new image take
# effect.
#
# A unique tag per build is what a real pipeline does, and what this should move
# to once CI pushes images.
if (-not $SkipBuild) {
    Write-Step 'Restarting the deployments whose images were rebuilt'
    foreach ($service in $toBuild) {
        kubectl rollout restart "deployment/$($service.Name)" -n $Namespace | Out-Null
        if ($LASTEXITCODE -ne 0) { Write-Note "could not restart $($service.Name)" }
    }
    Write-Ok "$($toBuild.Count) deployment(s) restarted"
}

# ---------------------------------------------------------------------------
# Wait
# ---------------------------------------------------------------------------
# Databases and the broker first, then the services. Not because the order is
# required -- the initContainers handle that -- but so the output reads as
# progress rather than a list of things that are not ready.
Write-Step 'Waiting for rollouts'

$infrastructure = @('rabbitmq', 'story-content-vault', 'recap-vault', 'character-vault', 'scene-read-model')
$applications   = @('storycontent-manager', 'recap-manager', 'account-manager', 'character-manager', 'scene-manager', 'scene-indexer')
$failed = @()

foreach ($deployment in ($infrastructure + $applications)) {
    Write-Host "    waiting for $deployment..."
    kubectl rollout status "deployment/$deployment" -n $Namespace --timeout=300s
    if ($LASTEXITCODE -ne 0) {
        $failed += $deployment
        Write-Note "$deployment did not finish rolling out"
    }
}

Write-Host ''
if ($failed.Count -eq 0) {
    Write-Host 'Everything is up.' -ForegroundColor Green
} else {
    Write-Host "These did not come up: $($failed -join ', ')" -ForegroundColor Yellow
    Write-Host "Look at:  kubectl -n $Namespace describe pod -l app.kubernetes.io/name=$($failed[0])" -ForegroundColor Yellow
}

Write-Host ''
Write-Host 'The gateway is on http://localhost/ (and https://localhost/ with a self-signed certificate).' -ForegroundColor Cyan
Write-Host 'Check it with:  .\kubernetes\scripts\verify.ps1' -ForegroundColor Cyan
