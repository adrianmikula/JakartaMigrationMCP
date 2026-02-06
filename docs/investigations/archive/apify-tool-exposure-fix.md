# Apify Tool Exposure Fix

## Problem

Apify MCP gateway (`mcp.apify.com`) only exposes Apify platform tools (call-actor, search-actors, etc.), not the Jakarta Migration MCP server's tools (analyzeJakartaReadiness, detectBlockers, etc.).

## Root Cause

The investigation document mentions: **"MCP tools are dynamically generated from the Actor's Input Schema"**. 

However, our `input_schema.json` defines Actor configuration parameters (transport mode, license keys, etc.), NOT the MCP tools themselves. Apify's gateway might be trying to generate tools from the input schema instead of querying the Actor's MCP endpoint.

## Solution Options

### Option 1: Direct Actor Access (Recommended)

Instead of using the Apify gateway, connect directly to the Actor's container URL when it's running in standby mode:

**Configuration:**
```json
{
  "mcpServers": {
    "jakarta-migration-apify": {
      "type": "sse",
      "url": "https://api.apify.com/v2/acts/adrian_m~JakartaMigrationMCP/runs/{runId}/container/mcp/sse"
    }
  }
}
```

**Problem**: Requires the Actor to be running and you need the run ID.

### Option 2: Update webServerMcpPath to Support Streamable HTTP

The Actor is configured for SSE (`/mcp/sse`), but streamable HTTP is recommended. We should:

1. Update `webServerMcpPath` to `/mcp/streamable-http` or support both
2. Update Dockerfile to use streamable-http by default
3. Verify Apify can discover tools via streamable-http

### Option 3: Verify Apify Tool Discovery Mechanism

Apify might need to:
1. Query the Actor's MCP endpoint (`/mcp/sse`) to discover tools
2. Cache the tool list
3. Expose them through the gateway

We should verify:
- Is the Actor's MCP endpoint accessible from Apify's infrastructure?
- Does Apify query `/mcp/sse` to discover tools?
- Are there any authentication/authorization issues?

## Recommended Fix

### Step 1: Update webServerMcpPath to Support Streamable HTTP

Update `.actor/actor.json`:
```json
{
  "webServerMcpPath": "/mcp/streamable-http"
}
```

### Step 2: Update Dockerfile to Use Streamable HTTP

Change the Dockerfile to use streamable-http transport instead of SSE.

### Step 3: Test Direct Actor Access

Test connecting directly to the Actor's container URL to verify tools are exposed correctly.

### Step 4: Verify Gateway Discovery

If Apify's gateway should discover tools automatically, verify:
- The Actor is in standby mode
- The MCP endpoint is accessible
- Apify can query the endpoint to discover tools

## Next Steps

1. Update configuration to support streamable-http
2. Test direct Actor access
3. Contact Apify support if gateway discovery isn't working as expected

