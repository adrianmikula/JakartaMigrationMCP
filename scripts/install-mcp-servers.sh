#!/bin/bash
# MCP Servers Installation Script for Linux/macOS
# Installs all requested MCP servers for Cursor

echo "Installing MCP Servers for Cursor..."

# Check Node.js
echo -e "\nChecking Node.js installation..."
if ! command -v node &> /dev/null; then
    echo "ERROR: Node.js is not installed. Please install Node.js 18+ from https://nodejs.org"
    exit 1
fi
echo "Node.js version: $(node --version)"

# Check Python (for Logic-LM)
echo -e "\nChecking Python installation..."
if ! command -v python3 &> /dev/null; then
    echo "WARNING: Python is not installed. Logic-LM MCP Server will not be installed."
    echo "Install Python 3.8+ from https://www.python.org if needed."
    PYTHON_AVAILABLE=false
else
    echo "Python version: $(python3 --version)"
    PYTHON_AVAILABLE=true
fi

# Core servers
echo -e "\nInstalling core MCP servers..."
npm install -g @code-index/mcp-server
npm install -g @aakarsh-sasi/memory-bank-mcp

# Build and dependency management
echo -e "\nInstalling build and dependency management servers..."
npm install -g @gradle/develocity-mcp-server
npm install -g @antigravity/npm-plus-mcp
npm install -g @antigravity/spring-initializr-mcp

# Design and architecture
echo -e "\nInstalling design and architecture servers..."
npm install -g @antoinebou12/uml-mcp
npm install -g @playbooks/ai-diagram-prototype-generator
npm install -g @squirrelogic/mcp-architect

# Docker
echo -e "\nInstalling Docker server..."
npm install -g @modelcontextprotocol/server-docker

# Database
echo -e "\nInstalling database server..."
npm install -g @henkdz/postgresql-mcp-server

# Python server
if [ "$PYTHON_AVAILABLE" = true ]; then
    echo -e "\nInstalling Python-based server..."
    pip3 install logic-lm-mcp-server
fi

# Storybook addon (project dependency - note for user)
echo -e "\nNote: Storybook MCP Addon should be installed as a project dependency:"
echo "  npm install --save-dev @storybook/addon-mcp"

echo -e "\nInstallation complete!"
echo -e "\nNext steps:"
echo "1. Configure MCP servers in Cursor: Settings → Features → MCP"
echo "2. See docs/setup/MCP_SETUP.md for complete configuration"
echo "3. Restart Cursor after configuration"

