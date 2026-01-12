# Start Jakarta Migration MCP Server in Docker for Cursor Testing
# This script starts the MCP server and provides the Cursor configuration

param(
    [string]$ProjectPath = "E:\Source\JakartaMigrationMCP",
    [int]$Port = 8080,
    [switch]$Build = $false
)

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Jakarta Migration MCP Server" -ForegroundColor Cyan
Write-Host "Docker Streamable HTTP - Cursor Setup" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check Docker
Write-Host "[1/5] Checking Docker..." -ForegroundColor Yellow
$dockerCheck = docker version 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "  [ERROR] Docker is not running or not installed" -ForegroundColor Red
    Write-Host "  Error: $dockerCheck" -ForegroundColor Red
    Write-Host ""
    Write-Host "  Please:" -ForegroundColor Yellow
    Write-Host "  1. Start Docker Desktop" -ForegroundColor White
    Write-Host "  2. Wait for Docker to fully start" -ForegroundColor White
    Write-Host "  3. Try again" -ForegroundColor White
    exit 1
}
Write-Host "  [OK] Docker is running" -ForegroundColor Green

# Build if needed
if ($Build) {
    Write-Host ""
    Write-Host "[2/5] Building Docker image..." -ForegroundColor Yellow
    docker build -t jakarta-migration-mcp . 2>&1 | ForEach-Object { Write-Host "  $_" -ForegroundColor Gray }
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  [ERROR] Build failed" -ForegroundColor Red
        exit 1
    }
    Write-Host "  [OK] Image built" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "[2/5] Checking Docker image..." -ForegroundColor Yellow
    $imageCheck = docker images jakarta-migration-mcp --format "{{.Repository}}" 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  [ERROR] Failed to check Docker images" -ForegroundColor Red
        Write-Host "  Error: $imageCheck" -ForegroundColor Red
        Write-Host "  Make sure Docker Desktop is running" -ForegroundColor Yellow
        exit 1
    }
    $exists = $imageCheck | Select-String "jakarta-migration-mcp"
    if (-not $exists) {
        Write-Host "  Building image (first time)..." -ForegroundColor Yellow
        docker build -t jakarta-migration-mcp . 2>&1 | ForEach-Object { Write-Host "  $_" -ForegroundColor Gray }
        if ($LASTEXITCODE -ne 0) {
            Write-Host "  [ERROR] Build failed" -ForegroundColor Red
            exit 1
        }
    }
    Write-Host "  [OK] Image ready" -ForegroundColor Green
}

# Validate project path
Write-Host ""
Write-Host "[3/5] Validating project path..." -ForegroundColor Yellow
if (-not (Test-Path $ProjectPath)) {
    Write-Host "  [ERROR] Path not found: $ProjectPath" -ForegroundColor Red
    exit 1
}
Write-Host "  [OK] Project path: $ProjectPath" -ForegroundColor Green

# Stop existing container
Write-Host ""
Write-Host "[4/5] Starting container..." -ForegroundColor Yellow
docker stop jakarta-mcp-cursor 2>&1 | Out-Null
docker rm jakarta-mcp-cursor 2>&1 | Out-Null

# Start container
$volumePath = $ProjectPath.Replace('\', '/')
Write-Host "  Starting container..." -ForegroundColor Yellow
$containerOutput = docker run -d `
    --name jakarta-mcp-cursor `
    -p "${Port}:8080" `
    -v "${ProjectPath}:C:\workspace\test-project:ro" `
    -e MCP_TRANSPORT=streamable-http `
    -e SPRING_PROFILES_ACTIVE=mcp-streamable-http `
    -e MCP_STREAMABLE_HTTP_PORT=8080 `
    jakarta-migration-mcp:latest 2>&1

if ($LASTEXITCODE -ne 0) {
    Write-Host "  [ERROR] Failed to start container" -ForegroundColor Red
    Write-Host "  Error: $containerOutput" -ForegroundColor Red
    exit 1
}
Write-Host "  [OK] Container started" -ForegroundColor Green

# Wait for server to start
Write-Host "  Waiting for server to start..." -ForegroundColor Yellow
Start-Sleep -Seconds 5

# Test health endpoint
$healthUrl = "http://localhost:${Port}/actuator/health"
try {
    $response = Invoke-WebRequest -Uri $healthUrl -TimeoutSec 5 -UseBasicParsing
    if ($response.StatusCode -eq 200) {
        Write-Host "  [OK] Server is healthy" -ForegroundColor Green
    }
} catch {
    Write-Host "  [WARNING] Health check failed, but container is running" -ForegroundColor Yellow
    Write-Host "  Server may still be starting up..." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "[5/5] Cursor Configuration" -ForegroundColor Yellow
Write-Host ""
Write-Host "Copy this configuration to Cursor MCP settings:" -ForegroundColor Cyan
Write-Host ""
Write-Host "{" -ForegroundColor Gray
Write-Host '  "mcpServers": {' -ForegroundColor Gray
Write-Host '    "jakarta-migration-local": {' -ForegroundColor Gray
Write-Host '      "type": "streamable-http",' -ForegroundColor Gray
Write-Host "      `"url`": `"http://localhost:${Port}/mcp/streamable-http`"" -ForegroundColor Green
Write-Host '    }' -ForegroundColor Gray
Write-Host '  }' -ForegroundColor Gray
Write-Host '}' -ForegroundColor Gray
Write-Host ""

# Save config to file
$configPath = "CURSOR_MCP_CONFIG_STREAMABLE_HTTP.json"
$config = @{
    mcpServers = @{
        "jakarta-migration-local" = @{
            type = "streamable-http"
            url = "http://localhost:${Port}/mcp/streamable-http"
        }
    }
} | ConvertTo-Json -Depth 10

$config | Out-File -FilePath $configPath -Encoding utf8
Write-Host "Configuration saved to: $configPath" -ForegroundColor Green
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Setup Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "1. Copy the configuration above to Cursor MCP settings" -ForegroundColor White
Write-Host "2. Restart Cursor or reload MCP servers" -ForegroundColor White
Write-Host "3. Test with: analyzeJakartaReadiness using path: C:\workspace\test-project" -ForegroundColor White
Write-Host ""
Write-Host "Container commands:" -ForegroundColor Yellow
Write-Host "  View logs: docker logs -f jakarta-mcp-cursor" -ForegroundColor White
Write-Host "  Stop: docker stop jakarta-mcp-cursor" -ForegroundColor White
Write-Host "  Remove: docker rm jakarta-mcp-cursor" -ForegroundColor White
Write-Host ""

