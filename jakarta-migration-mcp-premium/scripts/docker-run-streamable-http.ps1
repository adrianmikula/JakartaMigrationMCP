# Docker Run Script for Jakarta Migration MCP Server (Streamable HTTP)
# Starts the MCP server in Docker for local testing

param(
    [string]$ProjectPath = "E:\Source\JakartaMigrationMCP",
    [int]$Port = 8080,
    [switch]$Background = $false,
    [switch]$Build = $false
)

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Jakarta Migration MCP Server" -ForegroundColor Cyan
Write-Host "Docker Streamable HTTP Mode" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if Docker is running
Write-Host "[1/4] Checking Docker..." -ForegroundColor Yellow
try {
    docker version | Out-Null
    Write-Host "  ✓ Docker is running" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Docker is not running or not installed" -ForegroundColor Red
    Write-Host "  Please start Docker Desktop and try again" -ForegroundColor Yellow
    exit 1
}

# Build image if requested
if ($Build) {
    Write-Host ""
    Write-Host "[2/4] Building Docker image..." -ForegroundColor Yellow
    docker build -t jakarta-migration-mcp .
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  ✗ Build failed" -ForegroundColor Red
        exit 1
    }
    Write-Host "  ✓ Image built successfully" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "[2/4] Checking if image exists..." -ForegroundColor Yellow
    $imageExists = docker images jakarta-migration-mcp --format "{{.Repository}}" | Select-String "jakarta-migration-mcp"
    if (-not $imageExists) {
        Write-Host "  ⚠ Image not found. Building..." -ForegroundColor Yellow
        docker build -t jakarta-migration-mcp .
        if ($LASTEXITCODE -ne 0) {
            Write-Host "  ✗ Build failed" -ForegroundColor Red
            exit 1
        }
        Write-Host "  ✓ Image built successfully" -ForegroundColor Green
    } else {
        Write-Host "  ✓ Image exists" -ForegroundColor Green
    }
}

# Check if project path exists
Write-Host ""
Write-Host "[3/4] Validating project path..." -ForegroundColor Yellow
if (-not (Test-Path $ProjectPath)) {
    Write-Host "  ✗ Project path does not exist: $ProjectPath" -ForegroundColor Red
    Write-Host "  Please provide a valid project path" -ForegroundColor Yellow
    exit 1
}
Write-Host "  ✓ Project path exists: $ProjectPath" -ForegroundColor Green

# Stop existing container if running
Write-Host ""
Write-Host "[4/4] Starting container..." -ForegroundColor Yellow
$existingContainer = docker ps -a --filter "name=jakarta-mcp-local" --format "{{.Names}}"
if ($existingContainer) {
    Write-Host "  Stopping existing container..." -ForegroundColor Yellow
    docker stop jakarta-mcp-local 2>&1 | Out-Null
    docker rm jakarta-mcp-local 2>&1 | Out-Null
}

# Build docker run command
$dockerArgs = @(
    "run"
    "--name", "jakarta-mcp-local"
    "-p", "${Port}:8080"
    "-v", "`"${ProjectPath}:C:\workspace\test-project:ro`""
    "-e", "MCP_TRANSPORT=streamable-http"
    "-e", "SPRING_PROFILES_ACTIVE=mcp-streamable-http"
    "-e", "MCP_STREAMABLE_HTTP_PORT=8080"
)

if ($Background) {
    $dockerArgs += "-d"
} else {
    $dockerArgs += "-it"
    $dockerArgs += "--rm"
}

$dockerArgs += "jakarta-migration-mcp:latest"

Write-Host "  Starting container..." -ForegroundColor Yellow
Write-Host ""
Write-Host "  MCP Endpoint: http://localhost:${Port}/mcp/streamable-http" -ForegroundColor Cyan
Write-Host "  Health Check: http://localhost:${Port}/actuator/health" -ForegroundColor Cyan
Write-Host "  Project Path: $ProjectPath" -ForegroundColor Cyan
Write-Host "  Container Path: C:\workspace\test-project" -ForegroundColor Cyan
Write-Host ""

if ($Background) {
    docker $dockerArgs
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Container started in background" -ForegroundColor Green
        Write-Host ""
        Write-Host "To view logs: docker logs -f jakarta-mcp-local" -ForegroundColor Yellow
        Write-Host "To stop: docker stop jakarta-mcp-local" -ForegroundColor Yellow
    } else {
        Write-Host "✗ Failed to start container" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "Container running. Press Ctrl+C to stop." -ForegroundColor Yellow
    Write-Host ""
    docker $dockerArgs
}

