<#
.SYNOPSIS
    Take the system down.

.DESCRIPTION
    By default this removes the workloads and leaves the data. The
    PersistentVolumes use the Retain reclaim policy, so deleting the namespace
    does not delete what is in the databases.

    One consequence: the volumes survive, and so do the root passwords baked into
    them on first start. Deleting the secrets while keeping the volumes leaves new
    passwords that do not match the old databases, and every service then fails to
    authenticate.

    -IncludeData removes the volumes too. Use it when a schema has changed, since
    MySQL and MongoDB run their init scripts only against an empty data directory.

.PARAMETER IncludeData
    Also delete the PersistentVolumes, their claims, and the secrets. Everything
    in the databases is lost.

.PARAMETER IncludeIngressController
    Also uninstall ingress-nginx. Off by default: it is shared cluster
    infrastructure and another project may be using it.

.EXAMPLE
    .\kubernetes\scripts\teardown.ps1

.EXAMPLE
    .\kubernetes\scripts\teardown.ps1 -IncludeData
#>
[CmdletBinding()]
param(
    [switch]$IncludeData,
    [switch]$IncludeIngressController
)

$ErrorActionPreference = 'Stop'

$Namespace      = 'choose-your-fate'
$IngressVersion = 'controller-v1.15.1'
$RepoRoot       = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path

function Write-Step { param([string]$Text) Write-Host ''; Write-Host "==> $Text" -ForegroundColor Cyan }
function Write-Ok   { param([string]$Text) Write-Host "    [ok]   $Text" -ForegroundColor Green }

Write-Step 'Removing the workloads'
Push-Location $RepoRoot
try {
    # --ignore-not-found so that tearing down twice is not an error.
    kubectl delete -R -f kubernetes --ignore-not-found
} finally {
    Pop-Location
}
Write-Ok 'workloads removed'

if ($IncludeData) {
    Write-Step 'Removing data and secrets'

    # The claims go with the namespace delete above, but the PersistentVolumes are
    # cluster-scoped and Retain means they outlive their claims, so they have to
    # be named explicitly.
    $volumes = @(
        'rabbitmq-data',
        'story-content-vault-data',
        'recap-vault-data',
        'character-vault-data',
        'scene-read-model-data'
    )
    foreach ($volume in $volumes) {
        kubectl delete persistentvolume $volume --ignore-not-found
    }

    kubectl delete namespace $Namespace --ignore-not-found
    Write-Ok 'volumes and namespace removed'

    Write-Host ''
    Write-Host '    The directories under /var/lib/choose-your-fate inside the Docker Desktop VM' -ForegroundColor DarkGray
    Write-Host '    are left behind. Deleting the PersistentVolume releases the claim; it does not' -ForegroundColor DarkGray
    Write-Host '    erase the files. A fresh setup.ps1 plus deploy.ps1 will reuse those directories,' -ForegroundColor DarkGray
    Write-Host '    which still hold the OLD database passwords. If you want a genuinely empty' -ForegroundColor DarkGray
    Write-Host '    database, remove the directory too:' -ForegroundColor DarkGray
    Write-Host '        docker run --rm -v /var/lib/choose-your-fate:/data busybox:1.36 rm -rf /data/recap-vault' -ForegroundColor DarkGray
} else {
    Write-Host ''
    Write-Host '    Data kept. Add -IncludeData to delete the volumes and secrets as well.' -ForegroundColor DarkGray
}

if ($IncludeIngressController) {
    Write-Step 'Uninstalling ingress-nginx'
    $ingressUrl = "https://raw.githubusercontent.com/kubernetes/ingress-nginx/$IngressVersion/deploy/static/provider/cloud/deploy.yaml"
    kubectl delete -f $ingressUrl --ignore-not-found
    Write-Ok 'ingress-nginx removed'
} else {
    Write-Host ''
    Write-Host '    ingress-nginx kept. It is shared cluster infrastructure; add' -ForegroundColor DarkGray
    Write-Host '    -IncludeIngressController if you are certain nothing else on this cluster uses it.' -ForegroundColor DarkGray
}

Write-Host ''
Write-Host 'Teardown complete.' -ForegroundColor Green
