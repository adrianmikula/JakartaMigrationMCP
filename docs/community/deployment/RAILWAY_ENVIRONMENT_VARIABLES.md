# Railway Environment Variables

## Overview

This document lists environment variables for deploying the Jakarta Migration MCP Server on Railway. The project is distributed primarily as an npm package; Railway is optional for self-hosted HTTP (streamable-http) deployment.

**Note**: Environment variables are set in the Railway dashboard, not in `railway.json`. The `railway.json` file is for build and deploy configuration only.

## Required Environment Variables

### Core Configuration

| Variable | Value | Required | Description |
|----------|-------|----------|-------------|
| `SPRING_PROFILES_ACTIVE` | `mcp-streamable-http` | ✅ **Critical** | Activates the streamable-http profile for web server mode |
| `PORT` | (Auto-set by Railway) | ✅ Auto-provided | Railway automatically provides this — don't set manually |

## Optional Environment Variables

| Variable | Value | Description |
|----------|-------|-------------|
| `MCP_STREAMABLE_HTTP_PORT` | number | Override port (default: uses Railway's `PORT`) |

## Setting Variables in Railway

1. Go to your Railway project dashboard → your service → **Variables** tab
2. Click **+ New Variable**, enter name and value
3. Railway redeploys automatically when variables change

## Verification

After deployment:

1. **Health**: `curl https://your-app.railway.app/actuator/health`
2. **MCP endpoint**: `curl -X POST https://your-app.railway.app/mcp/streamable-http -H "Content-Type: application/json" -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}'`

## References

- [Railway Environment Variables](https://docs.railway.com/develop/variables)
- [Railway Deployment Guide](RAILWAY_DEPLOYMENT.md)
