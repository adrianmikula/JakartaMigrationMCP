#!/usr/bin/env pwsh
# Direct test of MCP tools by calling the Java methods
# This helps verify tools work before testing in Cursor

param(
    [string]$ProjectPath = "examples/demo-spring-javax-validation-example-master/demo-spring-javax-validation-example-master"
)

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "MCP Tools Direct Test" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# Check if project path exists
if (-not (Test-Path $ProjectPath)) {
    Write-Host "ERROR: Project path not found: $ProjectPath" -ForegroundColor Red
    Write-Host "Available example projects:" -ForegroundColor Yellow
    Get-ChildItem "examples" -Directory | ForEach-Object { Write-Host "  - examples/$($_.Name)" }
    exit 1
}

Write-Host "Testing project: $ProjectPath" -ForegroundColor Green
Write-Host ""

# Note: This is a conceptual test
# In practice, MCP tools are called via the MCP protocol through Cursor
# This script verifies the project structure and prepares for MCP testing

Write-Host "Project structure check:" -ForegroundColor Yellow
if (Test-Path "$ProjectPath/pom.xml") {
    Write-Host "  ✓ Maven project (pom.xml found)" -ForegroundColor Green
}
if (Test-Path "$ProjectPath/build.gradle") {
    Write-Host "  ✓ Gradle project (build.gradle found)" -ForegroundColor Green
}
if (Test-Path "$ProjectPath/build.gradle.kts") {
    Write-Host "  ✓ Gradle Kotlin project (build.gradle.kts found)" -ForegroundColor Green
}

Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Ready for MCP Testing" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "To test MCP tools in Cursor:" -ForegroundColor Green
Write-Host "1. Ensure Cursor MCP config is set up (see CURSOR_MCP_CONFIG.json)" -ForegroundColor White
Write-Host "2. Restart Cursor completely" -ForegroundColor White
Write-Host "3. Try asking Cursor:" -ForegroundColor White
Write-Host "   'Analyze Jakarta readiness for $ProjectPath'" -ForegroundColor Cyan
Write-Host "   'Detect blockers for $ProjectPath'" -ForegroundColor Cyan
Write-Host "   Create a migration plan for $ProjectPath" -ForegroundColor Cyan
Write-Host ""

