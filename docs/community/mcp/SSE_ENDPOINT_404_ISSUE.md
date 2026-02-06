# SSE Endpoint 404 Issue

## Problem

Server starts successfully with SSE transport, but the MCP SSE endpoint returns 404:
- ✅ Health endpoint works: `http://localhost:8080/actuator/health` (200 OK)
- ❌ MCP SSE endpoint: `http://localhost:8080/mcp/sse` (404 Not Found)

## Possible Causes

### 1. Spring AI MCP webmvc Starter May Not Support SSE

The `spring-ai-starter-mcp-server-webmvc` starter might only support stdio transport, not SSE. SSE might require:
- A different starter (e.g., `spring-ai-starter-mcp-server-webflux` for reactive/SSE)
- Or SSE might not be supported by Spring AI MCP 1.1.2

### 2. SSE Endpoint Not Registered

Spring AI MCP might not be registering the SSE endpoint even when transport is set to SSE. This could be:
- A bug in Spring AI MCP 1.1.2
- Missing configuration
- SSE not actually supported by webmvc starter

### 3. Different Endpoint Path

The actual SSE endpoint path might be different from `/mcp/sse`. Possible alternatives:
- `/mcp` (without /sse)
- `/api/mcp/sse`
- Different path configured by Spring AI MCP

## Investigation Needed

1. **Check Spring AI MCP Documentation**: Verify if webmvc starter supports SSE
2. **Check Server Logs**: Look for MCP endpoint registration messages
3. **Check Spring AI MCP Source**: See how SSE endpoints are registered
4. **Try Different Paths**: Test various endpoint paths

## Workaround

If SSE is not supported by webmvc starter:
- Use stdio transport (current working setup)
- Wait for Spring AI MCP fix/update
- Consider using a different MCP server framework

## Next Steps

1. Check Spring AI MCP documentation for SSE support in webmvc starter
2. Look for endpoint registration in server logs
3. Test if Cursor can connect even with 404 (might work differently)
4. Consider that SSE might not be supported by Spring AI MCP webmvc

