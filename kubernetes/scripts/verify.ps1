<#
.SYNOPSIS
    Check that the system works, not just that the pods are green.

.DESCRIPTION
    Green pods prove that processes started. They do not prove that a request
    reaches a service through the gateway, that the broker has the topology the
    design assumes, or that readiness means anything. Each check here establishes
    one of those.

        1  Gateway routing      a request through the ingress reaches a service
        2  Route groups         the admin group is a separate policy, not a
                                separate gateway
        3  Queue topology       what the broker actually has, against what the
                                contract says it should have
        4  Readiness is real    take RabbitMQ away and watch readiness fall
                                while liveness holds

    Check 4 deliberately stops the broker for about a minute and then puts it
    back. It is the only one that proves the probes are wired to anything, which
    is why it is on by default. Use -SkipDisruptive to leave the broker alone.

.PARAMETER SkipDisruptive
    Skip check 4, which scales RabbitMQ to zero and back.

.EXAMPLE
    .\kubernetes\scripts\verify.ps1
#>
[CmdletBinding()]
param(
    [switch]$SkipDisruptive
)

$ErrorActionPreference = 'Continue'

$Namespace = 'choose-your-fate'
$Gateway   = 'http://localhost'

$script:Passed = 0
$script:Failed = 0

function Write-Check { param([string]$Text) Write-Host ''; Write-Host "--- $Text" -ForegroundColor Cyan }
function Write-Pass  { param([string]$Text) Write-Host "    PASS  $Text" -ForegroundColor Green; $script:Passed++ }
function Write-Fail  { param([string]$Text) Write-Host "    FAIL  $Text" -ForegroundColor Red;   $script:Failed++ }
function Write-Info  { param([string]$Text) Write-Host "          $Text" -ForegroundColor DarkGray }
function Write-Flag  { param([string]$Text) Write-Host "    NOTE  $Text" -ForegroundColor Yellow }

function Invoke-Http {
    <#
        Invoke-WebRequest throws on any non-2xx status, which makes a 404 and a
        connection refusal look the same to the caller. They are not the same
        thing at all: one is a service answering, the other is nothing
        answering. This flattens both into an object so the checks can tell.
    #>
    param(
        [string]$Uri,
        [string]$Method = 'GET',
        [hashtable]$Headers = @{}
    )
    try {
        $response = Invoke-WebRequest -Uri $Uri -Method $Method -Headers $Headers -UseBasicParsing -TimeoutSec 20
        return [pscustomobject]@{
            Reached    = $true
            StatusCode = [int]$response.StatusCode
            Body       = $response.Content
            Server     = $response.Headers['Server']
        }
    } catch [System.Net.WebException] {
        $webResponse = $_.Exception.Response
        if ($webResponse) {
            $reader = New-Object System.IO.StreamReader($webResponse.GetResponseStream())
            $body = $reader.ReadToEnd()
            $reader.Close()
            return [pscustomobject]@{
                Reached    = $true
                StatusCode = [int]$webResponse.StatusCode
                Body       = $body
                Server     = $webResponse.Headers['Server']
            }
        }
        return [pscustomobject]@{ Reached = $false; StatusCode = 0; Body = $_.Exception.Message; Server = $null }
    } catch {
        return [pscustomobject]@{ Reached = $false; StatusCode = 0; Body = $_.Exception.Message; Server = $null }
    }
}

function Invoke-InBroker {
    param([string[]]$Arguments)
    $pod = kubectl get pod -n $Namespace -l app.kubernetes.io/name=rabbitmq -o jsonpath='{.items[0].metadata.name}'
    if (-not $pod) { return $null }
    return (& kubectl exec -n $Namespace $pod -- @Arguments 2>$null)
}

Write-Host 'Verifying the Choose Your Fate cluster' -ForegroundColor White
Write-Host "Namespace: $Namespace   Gateway: $Gateway"

# ===========================================================================
# 0. What is actually running
# ===========================================================================
Write-Check '0. Pods'
$notReady = kubectl get pods -n $Namespace --no-headers 2>$null |
    Where-Object { $_ -notmatch '\s+([0-9]+)/\1\s+Running' }
if ($notReady) {
    Write-Flag 'Not every pod is ready. The checks below may fail for that reason rather than their own.'
    $notReady | ForEach-Object { Write-Info $_.Trim() }
} else {
    Write-Pass 'every pod is Running and ready'
}

# ===========================================================================
# 1. A request through the gateway reaches a service
# ===========================================================================
# The skeleton endpoint is used on purpose: it returns 202 with a body naming
# the routing key it published, so a success is unambiguous AND it puts a real
# message on the broker, which check 3 then looks for. One action, two pieces
# of evidence.
Write-Check '1. Gateway routing (ingress-nginx -> storycontent-manager)'
$publish = Invoke-Http -Uri "$Gateway/admin/v1/test-events/scene-content" -Method POST

if (-not $publish.Reached) {
    Write-Fail "nothing answered on $Gateway"
    Write-Info $publish.Body
    Write-Info 'Is the ingress controller up?  kubectl -n ingress-nginx get svc'
} elseif ($publish.StatusCode -eq 202) {
    Write-Pass "POST /admin/v1/test-events/scene-content -> 202 Accepted"
    Write-Info "body: $($publish.Body)"
    Write-Info 'This travelled localhost:80 -> ingress-nginx -> Service -> pod, so routing, the'
    Write-Info 'Service selector and the container port are all confirmed in one request.'
} elseif ($publish.StatusCode -eq 503) {
    Write-Fail '503 from the gateway: it routed, but no ready endpoint was behind the route'
    Write-Info 'kubectl -n choose-your-fate get endpoints storycontent-manager'
} else {
    Write-Fail "unexpected status $($publish.StatusCode)"
    Write-Info "body: $($publish.Body)"
}

# The query side, through the same gateway on a different prefix. A 404 here is
# a pass: it is scene-manager saying the scene does not exist, which it cannot
# say unless the request got to it. What would be a failure is nginx answering
# instead -- so the body is what distinguishes them, not the status code.
Write-Check '1b. Gateway routing (ingress-nginx -> scene-manager)'
$scene = Invoke-Http -Uri "$Gateway/scenes/verification-probe"
if (-not $scene.Reached) {
    Write-Fail "nothing answered on $Gateway/scenes/..."
} elseif ($scene.Body -match 'nginx') {
    Write-Fail "nginx answered, not scene-manager -- the route is not matching (status $($scene.StatusCode))"
} else {
    Write-Pass "GET /scenes/verification-probe -> $($scene.StatusCode) from scene-manager, not from nginx"
    Write-Info 'A 404 is the expected answer: no such scene. The point is which process said it.'
}

# ===========================================================================
# 2. One gateway, two route groups
# ===========================================================================
Write-Check '2. Route groups'
# -o json and ConvertFrom-Json, not -o jsonpath.
#
# A jsonpath expression needs embedded double quotes for its separators, and
# Windows PowerShell 5.1 eats those when it builds the command line for a native
# executable. The expression arrives at kubectl malformed, matches nothing, and
# returns empty -- which reads exactly like "there are no Ingress objects". The
# other jsonpath queries in this script have no embedded quotes and are fine.
$ingressJson = kubectl get ingress -n $Namespace -o json 2>$null | ConvertFrom-Json
$items = @()
if ($ingressJson -and $ingressJson.items) { $items = @($ingressJson.items) }

if ($items.Count -eq 0) {
    Write-Fail 'no Ingress objects found'
} else {
    $rows = foreach ($item in $items) {
        [pscustomobject]@{
            Name    = $item.metadata.name
            Limit   = $item.metadata.annotations.'nginx.ingress.kubernetes.io/limit-rps'
            Address = $item.status.loadBalancer.ingress[0].hostname
        }
    }
    $rows = @($rows)
    foreach ($row in $rows) {
        Write-Info "$($row.Name): limit-rps=$($row.Limit) address=$($row.Address)"
    }
    $addresses = @($rows | ForEach-Object { $_.Address } | Where-Object { $_ } | Sort-Object -Unique)
    $limits    = @($rows | ForEach-Object { $_.Limit } | Sort-Object -Unique)
    if ($rows.Count -ge 2 -and $addresses.Count -eq 1 -and $limits.Count -ge 2) {
        Write-Pass "two route groups, both admitted at the same address ($($addresses[0])), with different limits ($($limits -join ' and ') rps)"
        Write-Info 'Same controller, same address, different rate limits. That is the claim the'
        Write-Info 'design makes -- one gateway with route groups, not two gateways -- and this'
        Write-Info 'is what it looks like from outside.'
    } else {
        Write-Fail "expected two Ingress objects sharing one address with different limits; found $($rows.Count) across $($addresses.Count) address(es) and $($limits.Count) distinct limit(s)"
    }
}

# ===========================================================================
# 3. Queue topology in the broker
# ===========================================================================
Write-Check '3. Queue topology in RabbitMQ'
$exchanges = Invoke-InBroker @('rabbitmqctl', '-q', 'list_exchanges', 'name')
$queues    = Invoke-InBroker @('rabbitmqctl', '-q', 'list_queues', 'name', 'messages')
$bindings  = Invoke-InBroker @('rabbitmqctl', '-q', 'list_bindings', 'source_name', 'routing_key', 'destination_name')

if (-not $exchanges) {
    Write-Fail 'could not reach the broker'
} else {
    $declared = @($exchanges | Where-Object { $_ -and $_ -notmatch '^amq\.' -and $_.Trim() -ne '' })
    Write-Info 'exchanges (excluding the amq.* defaults):'
    $declared | ForEach-Object { Write-Info "  $_" }
    Write-Info 'queues:'
    if ($queues) { $queues | ForEach-Object { Write-Info "  $_" } } else { Write-Info '  (none)' }
    Write-Info 'bindings:'
    if ($bindings) { $bindings | ForEach-Object { Write-Info "  $_" } } else { Write-Info '  (none)' }

    $hasStoryContent = [bool]($declared | Where-Object { $_ -match 'storycontent\.events' })
    $hasGameEvents   = [bool]($declared | Where-Object { $_ -match 'game\.events' })

    if ($hasStoryContent) {
        Write-Pass 'storycontent.events exists -- StoryContentManager declared its exchange on startup'
    } else {
        Write-Fail 'storycontent.events is missing'
    }

    if ($hasStoryContent -and $hasGameEvents) {
        Write-Flag 'Both storycontent.events and game.events exist, and nothing bridges them.'
        Write-Info 'This is the known split between the contract and the Scenes services:'
        Write-Info 'StoryContentManager publishes scene-content.updated to storycontent.events,'
        Write-Info 'and scene-indexer consumes scene.changed from game.events. Neither side is'
        Write-Info 'broken on its own and neither reports an error -- the queue simply stays'
        Write-Info 'empty. Running them in one cluster is what makes it visible; in separate'
        Write-Info 'compose files both halves look healthy.'
    }

    $sceneQueue = $queues | Where-Object { $_ -match 'scene' }
    if ($sceneQueue) {
        Write-Info "scene queues and their depth: $($sceneQueue -join '; ')"
        Write-Info 'A queue sitting at 0 after check 1 published an event is the symptom above.'
    }
}

# ===========================================================================
# 4. Readiness means something
# ===========================================================================
if ($SkipDisruptive) {
    Write-Check '4. Readiness (skipped)'
    Write-Info 'Run without -SkipDisruptive to actually test it.'
} else {
    Write-Check '4. Readiness falls when the broker goes away, and liveness does not'
    Write-Info 'Scaling RabbitMQ to zero. It goes back up at the end of this check.'

    $pod = kubectl get pod -n $Namespace -l app.kubernetes.io/name=storycontent-manager -o jsonpath='{.items[0].metadata.name}' 2>$null
    if (-not $pod) {
        Write-Fail 'no storycontent-manager pod to observe'
    } else {
        $restartsBefore = [int](kubectl get pod $pod -n $Namespace -o jsonpath='{.status.containerStatuses[0].restartCount}' 2>$null)
        Write-Info "watching pod $pod (restarts so far: $restartsBefore)"

        kubectl scale deployment/rabbitmq -n $Namespace --replicas=0 | Out-Null

        $wentNotReady = $false
        for ($i = 1; $i -le 30; $i++) {
            Start-Sleep -Seconds 2
            $ready = kubectl get pod $pod -n $Namespace -o jsonpath='{.status.containerStatuses[0].ready}' 2>$null
            if ($ready -eq 'false') { $wentNotReady = $true; break }
        }

        if ($wentNotReady) {
            Write-Pass "readiness went false after about $($i * 2)s without the broker"
            $endpoints = kubectl get endpoints storycontent-manager -n $Namespace -o jsonpath='{.subsets[*].addresses[*].ip}' 2>$null
            if (-not $endpoints) {
                Write-Pass 'the pod was removed from the Service endpoints, so it receives no traffic'
            } else {
                Write-Flag "the pod is still in the endpoints list ($endpoints) -- give it another moment"
            }
        } else {
            Write-Fail 'readiness never went false, so it is not reflecting the broker at all'
            Write-Info 'Check that MANAGEMENT_ENDPOINT_HEALTH_GROUP_READINESS_INCLUDE reached the app:'
            Write-Info "  kubectl -n $Namespace exec $pod -- wget -qO- localhost:8081/actuator/health/readiness"
        }

        $restartsDuring = [int](kubectl get pod $pod -n $Namespace -o jsonpath='{.status.containerStatuses[0].restartCount}' 2>$null)
        if ($restartsDuring -eq $restartsBefore) {
            Write-Pass "liveness held: still $restartsDuring restart(s), so the pod was not killed"
            Write-Info 'This is the distinction the conventions are built on. A dependency being'
            Write-Info 'down is a reason to stop sending the pod traffic, and not a reason to'
            Write-Info 'restart it -- restarting would not bring the broker back, and a restart'
            Write-Info 'loop would turn one outage into two.'
        } else {
            Write-Fail "the pod restarted ($restartsBefore -> $restartsDuring): the broker is in liveness, and it should not be"
        }

        Write-Info 'Putting RabbitMQ back...'
        kubectl scale deployment/rabbitmq -n $Namespace --replicas=1 | Out-Null
        kubectl rollout status deployment/rabbitmq -n $Namespace --timeout=180s | Out-Null

        $recovered = $false
        for ($i = 1; $i -le 45; $i++) {
            Start-Sleep -Seconds 2
            $ready = kubectl get pod $pod -n $Namespace -o jsonpath='{.status.containerStatuses[0].ready}' 2>$null
            if ($ready -eq 'true') { $recovered = $true; break }
        }
        if ($recovered) {
            Write-Pass 'readiness recovered on its own once the broker came back'
        } else {
            Write-Fail 'readiness did not recover. Look at the pod logs.'
        }
    }
}

# ===========================================================================
Write-Host ''
Write-Host "$script:Passed passed, $script:Failed failed." -ForegroundColor White
if ($script:Failed -gt 0) { exit 1 }
