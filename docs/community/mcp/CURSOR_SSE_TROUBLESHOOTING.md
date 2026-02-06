# Cursor SSE Endpoint Troubleshooting

## Issue: "No tools, prompts or resources"

When configuring Cursor to use the SSE endpoint, it shows "no tools, prompts or resources" even though the endpoint is working.

## Current Implementation

The custom SSE controller (`McpSseController.java`) implements:
- ✅ GET `/mcp/sse` - SSE connection endpoint
- ✅ POST `/mcp/sse` - JSON-RPC request endpoint
- ✅ MCP protocol methods: `initialize`, `tools/list`, `tools/call`

## Possible Causes

### 1. Cursor SSE Transport Expectations

Cursor might expect SSE transport to work differently than our implementation:
- Different message format
- Different endpoint structure
- Different initialization flow

### 2. Response Format Issues

The response format might not match what Cursor expects:
- Missing fields in capabilities
- Incorrect tool schema format
- Missing protocol version handling

### 3. Connection Flow Issues

Cursor might be:
- Connecting but not receiving responses properly
- Expecting responses via SSE events instead of POST responses
- Not calling `initialize` or `tools/list` correctly

## Testing Steps

### 1. Verify Endpoint Works

```bash
# Test tools/list
curl -X POST http://localhost:8080/mcp/sse \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}'
```

Should return tools list.

### 2. Test Initialize

```bash
curl -X POST http://localhost:8080/mcp/sse \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test","version":"1.0.0"}}}'
```

Should return server info and capabilities.

### 3. Check Cursor Configuration

Cursor configuration should be:
```json
{
  "mcpServers": {
    "jakarta-migration": {
      "type": "sse",
      "url": "http://localhost:8080/mcp/sse"
    }
  }
}
```

### 4. Check Server Logs

When Cursor connects, check server logs for:
- SSE connection established
- Incoming requests
- Response sent

## Potential Solutions

### Solution 1: Use stdio Instead

If SSE doesn't work with Cursor, use stdio transport:
```json
{
  "mcpServers": {
    "jakarta-migration": {
      "command": "java",
      "args": ["-jar", "path/to/jakarta-migration-mcp-1.0.0-SNAPSHOT.jar", "--spring.profiles.active=mcp-stdio"]
    }
  }
}
```

### Solution 2: Enhance SSE Implementation

If SSE is required, we may need to:
- Implement proper bidirectional SSE communication
- Handle messages via SSE events instead of POST
- Add proper connection management

### Solution 3: Check Cursor Version

Ensure Cursor is up to date - SSE support might be version-dependent.

## Next Steps

1. Test with curl to verify endpoint works
2. Check Cursor logs for connection errors
3. Consider using stdio transport for Cursor (SSE is mainly for Apify)
4. If SSE is required, may need to implement full bidirectional SSE protocol

