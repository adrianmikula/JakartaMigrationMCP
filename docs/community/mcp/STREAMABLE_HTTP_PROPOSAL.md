# Streamable HTTP Transport Proposal

## Why Streamable HTTP?

### Current Situation
- **SSE is deprecated** in MCP spec (as of 2025-03-26)
- **SSE implementation is complex**: Keepalive, session management, bidirectional communication
- **Streamable HTTP is simpler**: Single endpoint, standard HTTP POST, JSON responses

### Benefits of Streamable HTTP

1. **Simpler Implementation**
   - Single POST endpoint (no SSE complexity)
   - Standard HTTP requests/responses
   - No keepalive messages needed
   - No session management complexity

2. **Better Performance**
   - More stable connections
   - Better proxy compatibility
   - Lower overhead than SSE

3. **Future-Proof**
   - Recommended by MCP spec
   - Growing adoption in MCP community
   - Can optionally support SSE for long-running tasks

## Streamable HTTP Specification

Based on MCP spec and community implementations:

### Endpoint Structure
```
POST /mcp/streamable-http
```

### Request Format
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "initialize",
  "params": {
    "protocolVersion": "2024-11-05",
    "capabilities": {},
    "clientInfo": {
      "name": "client-name",
      "version": "1.0.0"
    }
  }
}
```

### Response Format
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "protocolVersion": "2024-11-05",
    "capabilities": {...},
    "serverInfo": {
      "name": "jakarta-migration-mcp",
      "version": "1.0.0"
    }
  }
}
```

### Session Management (Optional)
- Session ID via query parameter: `?session=<session-id>`
- Or via header: `X-Session-Id: <session-id>`
- Simpler than SSE's bidirectional connection management

### Authentication
- Same as SSE: `Authorization: Bearer <token>`
- Tool filtering: `?tools=tool1,tool2`

## Implementation Plan

### Phase 1: Basic Streamable HTTP Controller
1. Create `McpStreamableHttpController.java`
2. Single POST endpoint: `/mcp/streamable-http`
3. Reuse existing MCP protocol handlers from `McpSseController`
4. Support all MCP methods: `initialize`, `tools/list`, `tools/call`

### Phase 2: Configuration
1. Add `streamable-http` transport option
2. Update `application.yml` with Streamable HTTP config
3. Create `application-mcp-streamable-http.yml` profile

### Phase 3: Testing
1. Add integration tests for Streamable HTTP
2. Test session management (if needed)
3. Test authentication and tool filtering

### Phase 4: Documentation
1. Update README with Streamable HTTP option
2. Add examples for Apify deployment
3. Document migration from SSE to Streamable HTTP

## Comparison: SSE vs Streamable HTTP

| Feature | SSE | Streamable HTTP |
|---------|-----|-----------------|
| **Complexity** | High (keepalive, sessions, bidirectional) | Low (standard HTTP) |
| **Endpoints** | GET (SSE) + POST (JSON-RPC) | Single POST |
| **Session Management** | Complex (connection-based) | Simple (query param/header) |
| **Keepalive** | Required (every 15s) | Not needed |
| **Proxy Compatibility** | Can be problematic | Excellent |
| **MCP Spec Status** | Deprecated | Recommended |
| **Implementation Size** | ~500 lines | ~200 lines (estimated) |

## Recommendation

**Yes, we should add Streamable HTTP support** because:

1. ✅ **Simpler to implement and maintain**
2. ✅ **Better aligned with MCP spec** (SSE is deprecated)
3. ✅ **More reliable** (no keepalive/timeout issues)
4. ✅ **Better for Apify** (standard HTTP is more compatible)
5. ✅ **Can coexist with SSE** (backward compatibility)

## Next Steps

1. Implement `McpStreamableHttpController` (reuse logic from SSE controller)
2. Add configuration for Streamable HTTP transport
3. Add integration tests
4. Update documentation
5. Test with Apify (if they support it) or prepare for when they do

## Backward Compatibility

- Keep SSE controller for existing deployments
- Add Streamable HTTP as new option
- Allow both to coexist (different endpoints)
- Eventually deprecate SSE in favor of Streamable HTTP

