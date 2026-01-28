#!/usr/bin/env pwsh
# Test PREMIUM MCP API: start the premium JAR and send JSON-RPC requests.
# Expects pro tools (createMigrationPlan, analyzeMigrationImpact, verifyRuntime) to return real results,
# not upgrade_required, when default tier is PREMIUM (e.g. for local testing).

param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$PremiumJarPath = "jakarta-migration-mcp-premium/build/libs/jakarta-migration-mcp-premium-1.0.0-SNAPSHOT.jar",
    [switch]$StartServer,
    [switch]$NoStop
)

$ErrorActionPreference = "Stop"
$script:RequestId = 1

# Run from repo root
$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
if (Test-Path (Join-Path $repoRoot "gradlew.bat")) {
    Set-Location $repoRoot
}
$PremiumJarPath = $PremiumJarPath -replace '/', [System.IO.Path]::DirectorySeparatorChar

function New-JsonRpcRequest {
    param([string]$Method, [object]$Params = @{})
    $body = @{
        jsonrpc = "2.0"
        id      = $script:RequestId++
        method  = $Method
        params  = $Params
    } | ConvertTo-Json -Depth 10 -Compress
    return $body
}

function Invoke-McpRequest {
    param([string]$Method, [object]$Params = @{})
    $body = New-JsonRpcRequest -Method $Method -Params $Params
    $response = Invoke-RestMethod -Uri "$BaseUrl/mcp/streamable-http" -Method Post -Body $body -ContentType "application/json"
    return $response
}

function Get-ContentText {
    param($Response)
    if (-not $Response.result -or -not $Response.result.content) { return $null }
    return $Response.result.content[0].text
}

function Write-Result {
    param([string]$Tool, [string]$Summary, [bool]$Ok)
    $color = if ($Ok) { "Green" } else { "Red" }
    Write-Host "  $Tool : " -NoNewline
    Write-Host $Summary -ForegroundColor $color
}

if ($StartServer) {
    $jar = Resolve-Path $PremiumJarPath -ErrorAction SilentlyContinue
    if (-not $jar) {
        Write-Host "Building premium JAR..." -ForegroundColor Yellow
        & .\gradlew.bat :jakarta-migration-mcp-premium:bootJar -q
        $jar = Resolve-Path $PremiumJarPath
    }
    Write-Host "Starting PREMIUM server (Streamable HTTP, default-tier=PREMIUM for testing)..." -ForegroundColor Cyan
    $javaExe = if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
        Join-Path $env:JAVA_HOME "bin\java.exe"
    } else {
        $cmd = Get-Command java -ErrorAction SilentlyContinue
        if ($cmd) { $cmd.Source } else { "java" }
    }
    # Use default-tier=PREMIUM so pro tools run without a real license
    $script:ServerProcess = Start-Process -FilePath $javaExe -ArgumentList "-jar", $jar.Path, "--spring.profiles.active=mcp-streamable-http", "--jakarta.migration.feature-flags.default-tier=PREMIUM" -PassThru -WindowStyle Hidden
    $max = 90
    for ($i = 0; $i -lt $max; $i++) {
        Start-Sleep -Seconds 2
        try {
            $health = Invoke-RestMethod -Uri "$BaseUrl/actuator/health" -Method Get -ErrorAction SilentlyContinue
            if ($health.status -eq "UP") { break }
        } catch { }
        if ($i -eq $max - 1) {
            if (-not $NoStop -and $script:ServerProcess) { $script:ServerProcess | Stop-Process -Force -ErrorAction SilentlyContinue }
            throw "Server did not become healthy in time."
        }
    }
    Write-Host "Premium server is up." -ForegroundColor Green
}

Write-Host ""
Write-Host "=== MCP API (Premium): tools/list ===" -ForegroundColor Cyan
try {
    $list = Invoke-McpRequest -Method "tools/list" -Params @{}
    $tools = $list.result.tools
    Write-Host "Tools count: $($tools.Count)"
    foreach ($t in $tools) { Write-Host "  - $($t.name)" }
    Write-Result "tools/list" "OK" $true
} catch {
    Write-Result "tools/list" $_.Exception.Message $false
    if ($StartServer -and -not $NoStop -and $script:ServerProcess) { $script:ServerProcess | Stop-Process -Force -ErrorAction SilentlyContinue }
    exit 1
}

$tempProj = Join-Path $env:TEMP "mcp-premium-api-test-$([Guid]::NewGuid().ToString('N').Substring(0,8))"
New-Item -ItemType Directory -Path $tempProj -Force | Out-Null
$pom = @"
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.test</groupId>
  <artifactId>test</artifactId>
  <version>1.0</version>
  <dependencies>
    <dependency>
      <groupId>javax.servlet</groupId>
      <artifactId>javax.servlet-api</artifactId>
      <version>4.0.1</version>
    </dependency>
  </dependencies>
</project>
"@
Set-Content -Path (Join-Path $tempProj "pom.xml") -Value $pom

try {
    Write-Host ""
    Write-Host "=== Free tools (expect real analysis) ===" -ForegroundColor Cyan

    $r = Invoke-McpRequest -Method "tools/call" -Params @{ name = "analyzeJakartaReadiness"; arguments = @{ projectPath = $tempProj } }
    $text = Get-ContentText $r
    $ok = $text -and $text -match '"status"' -and $text -notmatch '"status":\s*"upgrade_required"'
    Write-Result "analyzeJakartaReadiness" $(if ($ok) { "real analysis" } else { "unexpected" }) $ok

    $r = Invoke-McpRequest -Method "tools/call" -Params @{ name = "detectBlockers"; arguments = @{ projectPath = $tempProj } }
    $text = Get-ContentText $r
    $ok = $text -and $text -match '"status"' -and $text -notmatch '"status":\s*"upgrade_required"'
    Write-Result "detectBlockers" $(if ($ok) { "real analysis" } else { "unexpected" }) $ok

    $r = Invoke-McpRequest -Method "tools/call" -Params @{ name = "recommendVersions"; arguments = @{ projectPath = $tempProj } }
    $text = Get-ContentText $r
    $ok = $text -and $text -match '"status"' -and $text -notmatch '"status":\s*"upgrade_required"'
    Write-Result "recommendVersions" $(if ($ok) { "real analysis" } else { "unexpected" }) $ok

    Write-Host ""
    Write-Host "=== Pro tools (expect real results on premium build with PREMIUM tier) ===" -ForegroundColor Cyan

    $r = Invoke-McpRequest -Method "tools/call" -Params @{ name = "createMigrationPlan"; arguments = @{ projectPath = $tempProj } }
    $text = Get-ContentText $r
    # Premium with PREMIUM tier: expect plan content (phases, etc.), not upgrade_required
    $ok = $text -and $text -match '"status"' -and $text -notmatch '"status":\s*"upgrade_required"'
    Write-Result "createMigrationPlan" $(if ($ok) { "real plan (not upgrade_required)" } else { "got: $($text.Substring(0, [Math]::Min(80, $text.Length)))..." }) $ok

    $r = Invoke-McpRequest -Method "tools/call" -Params @{ name = "analyzeMigrationImpact"; arguments = @{ projectPath = $tempProj } }
    $text = Get-ContentText $r
    $ok = $text -and $text -match '"status"' -and $text -notmatch '"status":\s*"upgrade_required"'
    Write-Result "analyzeMigrationImpact" $(if ($ok) { "real impact (not upgrade_required)" } else { "unexpected" }) $ok

    # verifyRuntime: without a real JAR may return error or verification result; should not be upgrade_required on premium
    $r = Invoke-McpRequest -Method "tools/call" -Params @{ name = "verifyRuntime"; arguments = @{ jarPath = "$env:TEMP\dummy.jar" } }
    $text = Get-ContentText $r
    $ok = $text -and ($text -notmatch '"status":\s*"upgrade_required"' -or $text -match '"status":\s*"error"')
    Write-Result "verifyRuntime" $(if ($ok) { "not upgrade_required (premium)" } else { "unexpected" }) $ok
} finally {
    Remove-Item -Recurse -Force $tempProj -ErrorAction SilentlyContinue
}

Write-Host ""
Write-Host "=== check_env ===" -ForegroundColor Cyan
$r = Invoke-McpRequest -Method "tools/call" -Params @{ name = "check_env"; arguments = @{ name = "PATH" } }
$text = Get-ContentText $r
$ok = $text -and ($text -match "Defined:" -or $text -match "Missing:")
Write-Result "check_env" $(if ($ok) { "OK" } else { "unexpected" }) $ok

if ($StartServer -and -not $NoStop -and $script:ServerProcess) {
    Write-Host ""
    Write-Host "Stopping server..." -ForegroundColor Cyan
    $script:ServerProcess | Stop-Process -Force -ErrorAction SilentlyContinue
}

Write-Host ""
Write-Host "Done. Use -StartServer to build and start the premium JAR (e.g. .\scripts\test-mcp-api-calls-premium.ps1 -StartServer)." -ForegroundColor Gray
