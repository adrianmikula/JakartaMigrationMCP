# Apify SSE Workaround - Custom Controller

## Quickest Solution for Apify Deployment

Since Spring AI MCP webmvc starter doesn't register SSE endpoints, we've created a **custom SSE controller** that implements the MCP protocol over HTTP/SSE.

## What Was Done

Created `McpSseController.java` that:
1. ✅ Exposes `/mcp/sse` endpoint (GET for SSE, POST for JSON-RPC)
2. ✅ Implements MCP protocol (`initialize`, `tools/list`, `tools/call`)
3. ✅ Automatically discovers tools from `@McpTool` annotated methods
4. ✅ Executes tools via reflection

## How It Works

### Endpoints

- **GET `/mcp/sse`**: SSE connection endpoint (for streaming)
- **POST `/mcp/sse`**: JSON-RPC endpoint (for tool calls)

### MCP Protocol Methods Supported

- `initialize`: Returns server info and capabilities
- `tools/list`: Lists all available tools
- `tools/call`: Executes a tool with parameters
- `ping`: Health check

## Usage for Apify

### 1. Build the JAR

```bash
gradlew bootJar
```

### 2. Start Server with SSE Profile

```bash
java -jar build/libs/jakarta-migration-mcp-1.0.0-SNAPSHOT.jar --spring.profiles.active=mcp-sse
```

### 3. Verify Endpoint

```bash
# Test tools/list
curl -X POST http://localhost:8080/mcp/sse \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}'
```

### 4. Apify Configuration

In your Apify Actor, configure:
- **MCP Endpoint**: `http://localhost:8080/mcp/sse`
- **Health Check**: `http://localhost:8080/actuator/health`

## Advantages

✅ **Works immediately** - No waiting for Spring AI MCP fix  
✅ **Full MCP protocol** - Implements standard MCP methods  
✅ **Auto-discovery** - Automatically finds all `@McpTool` methods  
✅ **Type-safe** - Handles parameter type conversion  

## Limitations

⚠️ **Simplified SSE** - Basic SSE implementation (may need enhancement for full streaming)  
⚠️ **Reflection-based** - Uses reflection to discover/execute tools  
⚠️ **No stdio fallback** - This controller only works in SSE mode  

## Next Steps

1. Test with Apify Actor deployment
2. Enhance SSE streaming if needed
3. Add error handling improvements
4. Consider adding WebSocket support for bidirectional communication

