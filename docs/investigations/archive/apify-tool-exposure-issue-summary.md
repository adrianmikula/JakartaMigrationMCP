# Apify Tool Exposure Issue - Summary

## Current Status

✅ **Docker Configuration**: Fixed and verified  
✅ **Actor Configuration**: `usesStandbyMode: true`, `webServerMcpPath: "/mcp/streamable-http"`  
✅ **MCP Server**: Tools are properly exposed at `/mcp/streamable-http`  
❌ **Apify Gateway**: Only exposes Apify platform tools, not Actor's MCP tools

## The Problem

When connecting to `https://mcp.apify.com/?tools=actors,docs,adrian_m/JakartaMigrationMCP`:
- **Expected**: Jakarta Migration tools (analyzeJakartaReadiness, detectBlockers, etc.)
- **Actual**: Only Apify platform tools (call-actor, search-actors, fetch-actor-details, etc.)

## Root Cause

Apify's MCP gateway (`mcp.apify.com`) is designed to:
1. Expose Apify platform tools (Actors, storage, docs)
2. **NOT** automatically proxy Actor MCP tools

The investigation document mentions tools are "dynamically generated from the Actor's Input Schema," but:
- Our input schema defines Actor configuration (transport, license keys)
- Our MCP tools are defined in Java code with `@McpTool` annotations
- The gateway doesn't query the Actor's MCP endpoint to discover tools

## Solutions

### Option 1: Direct Actor Access (Recommended)

Connect directly to the Actor's container URL when it's running in standby mode:

```json
{
  "mcpServers": {
    "jakarta-migration-apify": {
      "type": "streamable-http",
      "url": "https://api.apify.com/v2/acts/adrian_m~JakartaMigrationMCP/runs/{runId}/container/mcp/streamable-http"
    }
  }
}
```

**Limitation**: Requires Actor to be running and you need the run ID.

### Option 2: Use Apify's call-actor Tool

Use the `call-actor` tool from the gateway to invoke the Jakarta Migration Actor, which would then expose its tools.

**Limitation**: Adds an extra layer of indirection.

### Option 3: Verify Apify Tool Discovery

Apify might need to:
1. Query the Actor's MCP endpoint (`/mcp/streamable-http`) when in standby mode
2. Discover tools via `tools/list` request
3. Cache and expose them through the gateway

**Action Needed**: Verify if Apify's infrastructure queries the Actor's MCP endpoint for tool discovery.

## Configuration Updates Applied

✅ Updated `.actor/actor.json`:
- `webServerMcpPath: "/mcp/streamable-http"` (changed from `/mcp/sse`)

✅ Updated `Dockerfile`:
- Changed to use `streamable-http` transport
- Updated environment variables
- Updated CMD to use streamable-http profile

## Next Steps

1. **Redeploy Actor** with updated configuration
2. **Test Direct Access**: Try connecting directly to Actor container URL
3. **Verify Gateway Discovery**: Check if Apify queries the MCP endpoint
4. **Contact Apify Support**: If gateway discovery isn't working, verify expected behavior

## Alternative: Use Local Docker Server

For testing, use the locally running Docker server which correctly exposes all tools:
- URL: `http://localhost:8080/mcp/streamable-http`
- All 6 Jakarta Migration tools are available
- Verified working ✅

