# Apify Tool Exposure Issue - 2026-01-07

## Problem

When connecting to `https://mcp.apify.com/?tools=actors,docs,adrian_m/JakartaMigrationMCP`, only Apify platform tools are exposed (call-actor, search-actors, fetch-actor-details, etc.), not the Jakarta Migration MCP server's tools (analyzeJakartaReadiness, detectBlockers, etc.).

## Root Cause Analysis

### Current Configuration

✅ **Actor Configuration** (`.actor/actor.json`):
- `usesStandbyMode: true` - ✅ Correct
- `webServerMcpPath: "/mcp/sse"` - ✅ Set correctly

✅ **Dockerfile**:
- Uses `apify/actor-node:20` base image
- Exposes port 8080
- Uses `APIFY_CONTAINER_PORT` environment variable
- Configured for SSE transport (`/mcp/sse`)

### The Issue

Apify's MCP gateway (`mcp.apify.com`) is designed to:
1. **Expose Apify platform tools** - search-actors, call-actor, fetch-actor-details, etc.
2. **NOT directly proxy Actor MCP tools** - The Actor's MCP tools are not automatically exposed through the gateway

### How Apify MCP Gateway Works

When you connect to `https://mcp.apify.com/?tools=actors,docs,adrian_m/JakartaMigrationMCP`:
- The `?tools=actors,docs,adrian_m/JakartaMigrationMCP` parameter tells the gateway to:
  - Include Apify platform tools (`actors`, `docs`)
  - Include tools from the specified Actor (`adrian_m/JakartaMigrationMCP`)
- However, the gateway exposes the Actor as a **callable tool** (via `call-actor`), not the Actor's MCP tools directly

### Expected vs Actual Behavior

**Expected**: Jakarta Migration tools (analyzeJakartaReadiness, detectBlockers, etc.) should be directly available

**Actual**: Only Apify platform tools are available, and you need to use `call-actor` to invoke the Jakarta Migration Actor

## Possible Solutions

### Option 1: Direct Actor URL (Not Through Gateway)

Instead of using `mcp.apify.com`, connect directly to the Actor's container URL:

```
https://api.apify.com/v2/acts/adrian_m~JakartaMigrationMCP/runs/{runId}/...
```

**Problem**: This requires the Actor to be running, and you need the run ID.

### Option 2: Update webServerMcpPath to Support Streamable HTTP

The Actor is currently configured for SSE (`/mcp/sse`), but streamable HTTP (`/mcp/streamable-http`) is the recommended transport. We should:

1. Update `webServerMcpPath` to support both or use streamable-http
2. Update Dockerfile to support streamable-http by default
3. Verify Apify can discover tools via streamable-http

### Option 3: Apify Gateway Tool Discovery

The gateway might need additional configuration or the Actor might need to register its tools differently. This could involve:

1. **Input Schema**: Apify might generate tools from the Actor's input schema, not from the MCP tools
2. **Tool Registration**: The Actor might need to register its MCP tools with Apify's platform
3. **Standby Mode Discovery**: Apify might need to query the Actor's MCP endpoint to discover tools

## Investigation Needed

1. **Check Apify Documentation**: Verify how standby mode Actors expose MCP tools through the gateway
2. **Test Direct Actor Access**: Try connecting directly to the Actor's container URL
3. **Verify Tool Discovery**: Check if Apify queries the `/mcp/sse` endpoint to discover tools
4. **Check Input Schema**: Verify if Apify generates tools from `input_schema.json` instead of MCP tools

## Next Steps

1. ✅ Document the issue
2. ⏭️ Research Apify's standby mode MCP tool discovery mechanism
3. ⏭️ Test direct Actor URL access
4. ⏭️ Update configuration if needed (webServerMcpPath, transport type)
5. ⏭️ Verify tool exposure after fixes

