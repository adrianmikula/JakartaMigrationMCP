# Test Direct Actor Access to Jakarta Migration MCP Server
# Tests the Actor's container URL directly (bypassing Apify gateway)

param(
    [string]$ApifyApiToken = $env:APIFY_API_TOKEN,
    [string]$ActorId = "adrian_m~JakartaMigrationMCP",
    [string]$RunId = $null
)

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Testing Direct Actor Access" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if (-not $ApifyApiToken) {
    Write-Host "ERROR: APIFY_API_TOKEN environment variable not set" -ForegroundColor Red
    Write-Host "Get your token from: https://console.apify.com/account#/integrations" -ForegroundColor Yellow
    exit 1
}

$headers = @{
    "Authorization" = "Bearer $ApifyApiToken"
    "Content-Type" = "application/json"
}

# If RunId not provided, try to get the latest run
if (-not $RunId) {
    Write-Host "[1/4] Getting latest Actor run..." -ForegroundColor Yellow
    try {
        $runsUrl = "https://api.apify.com/v2/acts/$ActorId/runs?limit=1&status=READY"
        $runsResponse = Invoke-RestMethod -Uri $runsUrl -Method Get -Headers $headers -ErrorAction Stop
        
        if ($runsResponse.data.items.Count -eq 0) {
            Write-Host "  [ERROR] No ready runs found. Please start the Actor in standby mode first." -ForegroundColor Red
            $consoleActorId = $ActorId -replace '~', '/'
            Write-Host "  Go to: https://console.apify.com/actors/$consoleActorId" -ForegroundColor Yellow
            exit 1
        }
        
        $RunId = $runsResponse.data.items[0].id
        Write-Host "  [OK] Found run: $RunId" -ForegroundColor Green
    } catch {
        Write-Host "  [ERROR] Failed to get Actor runs: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "[1/4] Using provided RunId: $RunId" -ForegroundColor Yellow
}

# Get run details to find container URL
Write-Host ""
Write-Host "[2/4] Getting run details..." -ForegroundColor Yellow
try {
    $runUrl = "https://api.apify.com/v2/actor-runs/$RunId"
    $runResponse = Invoke-RestMethod -Uri $runUrl -Method Get -Headers $headers -ErrorAction Stop
    
    if ($runResponse.data.status -ne "READY") {
        Write-Host "  [WARNING] Run status is: $($runResponse.data.status)" -ForegroundColor Yellow
        Write-Host "  Run must be in READY (standby) status for MCP access" -ForegroundColor Yellow
    }
    
    $containerUrl = $runResponse.data.containerUrl
    if (-not $containerUrl) {
        Write-Host "  [ERROR] Container URL not found in run data" -ForegroundColor Red
        exit 1
    }
    
    Write-Host "  [OK] Container URL: $containerUrl" -ForegroundColor Green
} catch {
    Write-Host "  [ERROR] Failed to get run details: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Test MCP endpoint
Write-Host ""
Write-Host "[3/4] Testing MCP endpoint..." -ForegroundColor Yellow
$mcpUrl = "$containerUrl/mcp/streamable-http"

Write-Host "  MCP URL: $mcpUrl" -ForegroundColor Gray

# Test 1: Initialize
Write-Host "  Testing initialize..." -ForegroundColor Gray
$initRequest = @{
    jsonrpc = "2.0"
    id = 1
    method = "initialize"
    params = @{
        protocolVersion = "2024-11-05"
        capabilities = @{}
        clientInfo = @{
            name = "test-client"
            version = "1.0.0"
        }
    }
} | ConvertTo-Json -Depth 10

try {
    $initResponse = Invoke-RestMethod -Uri $mcpUrl -Method Post -Headers $headers -Body $initRequest -ErrorAction Stop
    Write-Host "    [OK] Initialize successful" -ForegroundColor Green
    Write-Host "    Server: $($initResponse.result.serverInfo.name) v$($initResponse.result.serverInfo.version)" -ForegroundColor Gray
} catch {
    Write-Host "    [ERROR] Initialize failed: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "    Response: $responseBody" -ForegroundColor Red
    }
    exit 1
}

# Test 2: List Tools
Write-Host ""
Write-Host "[4/4] Testing tools/list..." -ForegroundColor Yellow
$toolsListRequest = @{
    jsonrpc = "2.0"
    id = 2
    method = "tools/list"
    params = @{}
} | ConvertTo-Json -Depth 10

try {
    $toolsListResponse = Invoke-RestMethod -Uri $mcpUrl -Method Post -Headers $headers -Body $toolsListRequest -ErrorAction Stop
    
    if ($toolsListResponse.result -and $toolsListResponse.result.tools) {
        $tools = $toolsListResponse.result.tools
        Write-Host "  [OK] Found $($tools.Count) tools" -ForegroundColor Green
        Write-Host ""
        
        # Expected Jakarta Migration tools
        $expectedTools = @(
            "analyzeJakartaReadiness",
            "detectBlockers",
            "recommendVersions",
            "createMigrationPlan",
            "analyzeMigrationImpact",
            "verifyRuntime"
        )
        
        Write-Host "  Available Tools:" -ForegroundColor Cyan
        $foundTools = @()
        foreach ($tool in $tools) {
            $foundTools += $tool.name
            $isExpected = $expectedTools -contains $tool.name
            $status = if ($isExpected) { "[OK]" } else { "[?]" }
            $color = if ($isExpected) { "Green" } else { "Yellow" }
            Write-Host "    $status $($tool.name)" -ForegroundColor $color
            if ($tool.description) {
                $desc = $tool.description
                if ($desc.Length -gt 60) { $desc = $desc.Substring(0, 60) + "..." }
                Write-Host "      $desc" -ForegroundColor Gray
            }
        }
        
        Write-Host ""
        Write-Host "  Tool Coverage:" -ForegroundColor Cyan
        $missingTools = $expectedTools | Where-Object { $foundTools -notcontains $_ }
        if ($missingTools.Count -eq 0) {
            Write-Host "    [OK] All expected tools are present!" -ForegroundColor Green
        } else {
            Write-Host "    [ERROR] Missing tools:" -ForegroundColor Red
            foreach ($missing in $missingTools) {
                Write-Host "      - $missing" -ForegroundColor Red
            }
        }
        
        $unexpectedTools = $foundTools | Where-Object { $expectedTools -notcontains $_ }
        if ($unexpectedTools.Count -gt 0) {
            Write-Host ""
            Write-Host "    [?] Additional tools found:" -ForegroundColor Yellow
            foreach ($unexpected in $unexpectedTools) {
                Write-Host "      - $unexpected" -ForegroundColor Yellow
            }
        }
        
    } else {
        Write-Host "  [ERROR] No tools found in response" -ForegroundColor Red
        Write-Host "  Response: $($toolsListResponse | ConvertTo-Json -Depth 10)" -ForegroundColor Red
    }
} catch {
    Write-Host "  [ERROR] Tools list failed: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "  Response: $responseBody" -ForegroundColor Red
    }
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test Complete" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Direct Actor Access URL:" -ForegroundColor Cyan
Write-Host "  $mcpUrl" -ForegroundColor Green
Write-Host ""
Write-Host "Use this URL in your MCP client configuration:" -ForegroundColor Yellow
Write-Host '  { "type": "streamable-http", "url": "' + $mcpUrl + '" }' -ForegroundColor White

