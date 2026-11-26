<#
.SYNOPSIS
    Point every image reference at a real registry.

.DESCRIPTION
    The manifests ship with registry.invalid as the registry. That is a
    placeholder, chosen on purpose: .invalid is reserved by RFC 2606 and can
    never resolve, so an image that is genuinely missing fails loudly instead of
    being quietly pulled from whichever stranger happens to own that name on
    Docker Hub. A friendlier-looking placeholder would be squattable.

    This rewrites the registry in every deployment.yaml. The image reference in
    the manifest is the single source of truth -- deploy.ps1 reads its build and
    push tags from there -- so changing it here changes it everywhere.

    Run it once, when the group has decided whose Docker Hub account or
    organisation the images belong in, and commit the result.

.PARAMETER Registry
    The Docker Hub namespace, for example a username or an organisation. Pass
    registry.invalid to go back to the placeholder.

.EXAMPLE
    .\kubernetes\scripts\set-registry.ps1 some-org

.EXAMPLE
    .\kubernetes\scripts\set-registry.ps1 -Registry ghcr.io/some-org
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$Registry
)

$ErrorActionPreference = 'Stop'

$KubernetesDir = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$Services = @(
    'storycontent-manager',
    'recap-manager',
    'account-manager',
    'character-manager',
    'scene-manager',
    'scene-indexer'
)

$Registry = $Registry.TrimEnd('/')
if ($Registry -notmatch '^[a-z0-9][a-z0-9._/-]*$') {
    throw "'$Registry' does not look like a registry namespace. Docker requires lowercase letters, digits, dots, dashes, underscores and slashes."
}

$changed = 0
foreach ($service in $Services) {
    $manifest = Join-Path $KubernetesDir "$service\deployment.yaml"
    if (-not (Test-Path -LiteralPath $manifest)) {
        Write-Host "    [skip] no deployment.yaml for $service" -ForegroundColor DarkGray
        continue
    }

    $content = Get-Content -LiteralPath $manifest -Raw
    # Anchored on the service name, so the busybox initContainer image is left
    # alone. It comes from Docker Hub and has nothing to do with our registry.
    $pattern = '(image:\s*)\S+/(' + [regex]::Escape($service) + ':\S+)'
    $updated = [regex]::Replace($content, $pattern, ('${1}' + $Registry + '/${2}'))

    if ($updated -eq $content) {
        Write-Host "    [same] $service" -ForegroundColor DarkGray
        continue
    }

    # Written without a BOM. kubectl copes either way, but a BOM in a YAML file
    # shows up as a mystery character in every diff from then on.
    [System.IO.File]::WriteAllText($manifest, $updated, (New-Object System.Text.UTF8Encoding $false))
    $match = [regex]::Match($updated, 'image:\s*(\S+/' + [regex]::Escape($service) + ':\S+)')
    Write-Host "    [set]  $($match.Groups[1].Value)" -ForegroundColor Green
    $changed++
}

Write-Host ''
Write-Host "$changed manifest(s) updated." -ForegroundColor Green
if ($Registry -ne 'registry.invalid') {
    Write-Host 'Next:  docker login, then  .\kubernetes\scripts\deploy.ps1 -Push' -ForegroundColor Cyan
}
Write-Host 'Remember to commit the change, so everyone builds and pulls the same images.' -ForegroundColor Cyan
