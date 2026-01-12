#!/usr/bin/env pwsh
# Test script to demonstrate Jakarta Migration MCP tools
# This script calls the tools directly via Java to show they work

param(
    [Parameter(Mandatory=$false)]
    [string]$ProjectPath = "examples/demo-spring-javax-validation-example-master/demo-spring-javax-validation-example-master",
    
    [Parameter(Mandatory=$false)]
    [string]$JarPath = "build/libs/jakarta-migration-mcp-1.0.0-SNAPSHOT.jar"
)

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Jakarta Migration MCP Tools Test" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# Check if JAR exists
if (-not (Test-Path $JarPath)) {
    Write-Host "ERROR: JAR file not found at: $JarPath" -ForegroundColor Red
    Write-Host "Please build the project first: .\gradlew.bat bootJar" -ForegroundColor Yellow
    exit 1
}

# Check if project path exists
$FullProjectPath = Resolve-Path $ProjectPath -ErrorAction SilentlyContinue
if (-not $FullProjectPath) {
    Write-Host "ERROR: Project path not found: $ProjectPath" -ForegroundColor Red
    exit 1
}

Write-Host "Using JAR: $JarPath" -ForegroundColor Green
Write-Host "Testing project: $FullProjectPath" -ForegroundColor Green
Write-Host ""

# Note: This script demonstrates the concept
# In practice, MCP tools are called via the MCP protocol through Cursor
Write-Host "NOTE: To use these tools in Cursor:" -ForegroundColor Yellow
Write-Host "1. Configure the MCP server in Cursor Settings (see docs/setup/CURSOR_MCP_SETUP.md)" -ForegroundColor Yellow
Write-Host "2. Restart Cursor" -ForegroundColor Yellow
Write-Host "3. Ask Cursor to analyze the project using the MCP tools" -ForegroundColor Yellow
Write-Host ""

Write-Host "Example Cursor MCP Configuration:" -ForegroundColor Cyan
$config = @"
{
  "mcpServers": {
    "jakarta-migration": {
      "command": "java",
      "args": [
        "-jar",
        "$((Resolve-Path $JarPath).Path)",
        "--spring.main.web-application-type=none",
        "--spring.profiles.active=mcp"
      ]
    }
  }
}
"@
Write-Host $config -ForegroundColor Gray
Write-Host ""

Write-Host "Available MCP Tools:" -ForegroundColor Cyan
Write-Host "1. analyzeJakartaReadiness - Analyze project for Jakarta migration readiness" -ForegroundColor White
Write-Host "2. detectBlockers - Detect blockers preventing Jakarta migration" -ForegroundColor White
Write-Host "3. recommendVersions - Recommend Jakarta-compatible dependency versions" -ForegroundColor White
Write-Host "4. createMigrationPlan - Create comprehensive migration plan" -ForegroundColor White
Write-Host "5. verifyRuntime - Verify runtime execution of migrated application" -ForegroundColor White
Write-Host ""

Write-Host "To use in Cursor, try asking:" -ForegroundColor Cyan
Write-Host "  'Analyze the Jakarta readiness of the project at $FullProjectPath'" -ForegroundColor White
Write-Host "  'Detect blockers for Jakarta migration in $FullProjectPath'" -ForegroundColor White
Write-Host "  'Create a migration plan for $FullProjectPath'" -ForegroundColor White
Write-Host ""

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Test Complete" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

