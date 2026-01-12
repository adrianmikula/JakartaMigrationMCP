# MCP Servers Installation Script for Windows
# Installs all requested MCP servers for Cursor

Write-Host "Installing MCP Servers for Cursor..." -ForegroundColor Green

# Check Node.js
Write-Host "`nChecking Node.js installation..." -ForegroundColor Yellow
$nodeVersion = node --version 2>$null
if (-not $nodeVersion) {
    Write-Host "ERROR: Node.js is not installed. Please install Node.js 18+ from https://nodejs.org" -ForegroundColor Red
    exit 1
}
Write-Host "Node.js version: $nodeVersion" -ForegroundColor Green

# Check Python (for Logic-LM)
Write-Host "`nChecking Python installation..." -ForegroundColor Yellow
$pythonVersion = python --version 2>$null
if (-not $pythonVersion) {
    Write-Host "WARNING: Python is not installed. Logic-LM MCP Server will not be installed." -ForegroundColor Yellow
    Write-Host "Install Python 3.8+ from https://www.python.org if needed." -ForegroundColor Yellow
} else {
    Write-Host "Python version: $pythonVersion" -ForegroundColor Green
}

# Core servers
Write-Host "`nInstalling core MCP servers..." -ForegroundColor Yellow
npm install -g @code-index/mcp-server
npm install -g @aakarsh-sasi/memory-bank-mcp

# Build and dependency management
Write-Host "`nInstalling build and dependency management servers..." -ForegroundColor Yellow
npm install -g @gradle/develocity-mcp-server
npm install -g @antigravity/npm-plus-mcp
npm install -g @antigravity/spring-initializr-mcp

# Design and architecture
Write-Host "`nInstalling design and architecture servers..." -ForegroundColor Yellow
npm install -g @antoinebou12/uml-mcp
npm install -g @playbooks/ai-diagram-prototype-generator
npm install -g @squirrelogic/mcp-architect

# Docker
Write-Host "`nInstalling Docker server..." -ForegroundColor Yellow
npm install -g @modelcontextprotocol/server-docker

# Database
Write-Host "`nInstalling database server..." -ForegroundColor Yellow
npm install -g @henkdz/postgresql-mcp-server

# Python server
if ($pythonVersion) {
    Write-Host "`nInstalling Python-based server..." -ForegroundColor Yellow
    pip install logic-lm-mcp-server
}

# Storybook addon (project dependency - note for user)
Write-Host "`nNote: Storybook MCP Addon should be installed as a project dependency:" -ForegroundColor Cyan
Write-Host "  npm install --save-dev @storybook/addon-mcp" -ForegroundColor Cyan

Write-Host "`nInstallation complete!" -ForegroundColor Green
Write-Host "`nNext steps:" -ForegroundColor Yellow
Write-Host "1. Configure MCP servers in Cursor: Settings → Features → MCP" -ForegroundColor White
Write-Host "2. See docs/setup/MCP_SETUP.md for complete configuration" -ForegroundColor White
Write-Host "3. Restart Cursor after configuration" -ForegroundColor White

