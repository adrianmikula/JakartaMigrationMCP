#!/usr/bin/env pwsh
# Test script to verify MCP server can start correctly
# This helps diagnose issues before testing in Cursor

param(
    [string]$JarPath = "build/libs/jakarta-migration-mcp-1.0.0-SNAPSHOT.jar",
    [int]$TimeoutSeconds = 10
)

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "MCP Server Startup Test" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# Check if JAR exists
if (-not (Test-Path $JarPath)) {
    Write-Host "ERROR: JAR file not found at: $JarPath" -ForegroundColor Red
    Write-Host "Please build the project first" -ForegroundColor Yellow
    exit 1
}

Write-Host "JAR file found: $JarPath" -ForegroundColor Green
$jarSize = (Get-Item $JarPath).Length / 1MB
Write-Host "Size: $([math]::Round($jarSize, 2)) MB" -ForegroundColor Gray
Write-Host ""

# Check Java
$javaCheck = Get-Command java -ErrorAction SilentlyContinue
if ($null -eq $javaCheck) {
    Write-Host "ERROR: Java not found in PATH" -ForegroundColor Red
    Write-Host "Please install Java 21 or add it to PATH" -ForegroundColor Yellow
    exit 1
}

$javaVersion = java -version 2>&1 | Select-Object -First 1
Write-Host "Java found: $javaVersion" -ForegroundColor Green
Write-Host ""

Write-Host "Starting MCP server..." -ForegroundColor Yellow
Write-Host ""

# Start server
$jarFullPath = (Resolve-Path $JarPath).Path
$process = Start-Process -FilePath "java" `
    -ArgumentList "-jar", $jarFullPath, "--spring.main.web-application-type=none", "--spring.profiles.active=mcp-stdio", "--spring.ai.mcp.server.transport=stdio" `
    -NoNewWindow `
    -PassThru `
    -RedirectStandardOutput "mcp-server-output.log" `
    -RedirectStandardError "mcp-server-error.log"

Write-Host "Server process started (PID: $($process.Id))" -ForegroundColor Green
Write-Host "Waiting for startup..." -ForegroundColor Yellow
Start-Sleep -Seconds 5

# Check if process is still running
if ($process.HasExited) {
    Write-Host "ERROR: Server process exited immediately!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Standard Output:" -ForegroundColor Yellow
    if (Test-Path "mcp-server-output.log") {
        Get-Content "mcp-server-output.log" | Write-Host
    }
    Write-Host ""
    Write-Host "Standard Error:" -ForegroundColor Yellow
    if (Test-Path "mcp-server-error.log") {
        Get-Content "mcp-server-error.log" | Write-Host
    }
    exit 1
}

Write-Host "Server process is running" -ForegroundColor Green
Write-Host ""

# Check logs
if (Test-Path "mcp-server-output.log") {
    $output = Get-Content "mcp-server-output.log"
    $springStarted = $false
    $mcpFound = $false
    
    foreach ($line in $output) {
        if ($line -match "Started.*Application") {
            Write-Host "Spring Boot started successfully" -ForegroundColor Green
            $springStarted = $true
        }
        if ($line -match "MCP|mcp") {
            Write-Host "MCP-related messages found" -ForegroundColor Green
            $mcpFound = $true
        }
    }
    
    if (-not $springStarted) {
        Write-Host "Spring Boot startup message not found" -ForegroundColor Yellow
    }
    if (-not $mcpFound) {
        Write-Host "No MCP messages found in logs" -ForegroundColor Yellow
    }
}

if (Test-Path "mcp-server-error.log") {
    $errors = Get-Content "mcp-server-error.log"
    if ($errors.Count -gt 0) {
        Write-Host ""
        Write-Host "Errors found in logs:" -ForegroundColor Yellow
        $errors | Select-Object -First 10 | Write-Host
    } else {
        Write-Host "No errors in logs" -ForegroundColor Green
    }
}

# Stop the server
Write-Host ""
Write-Host "Stopping server..." -ForegroundColor Yellow
Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 1

# Cleanup
Remove-Item "mcp-server-output.log" -ErrorAction SilentlyContinue
Remove-Item "mcp-server-error.log" -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Test Complete" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Green
Write-Host "1. Configure Cursor MCP settings (see docs/testing/CURSOR_MCP_TESTING.md)" -ForegroundColor White
Write-Host "2. Restart Cursor completely" -ForegroundColor White
Write-Host "3. Test tools in Cursor" -ForegroundColor White
Write-Host ""
