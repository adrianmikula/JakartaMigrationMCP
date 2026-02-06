# Apify SSE Endpoint Compliance Check

## Apify MCP Server Reference

Based on Apify's documentation, their MCP server:
- **URL**: `https://mcp.apify.com` (or `https://mcp.apify.com/sse` for SSE)
- **Transport**: SSE (Server-Sent Events)
- **Authentication**: OAuth or Bearer token via `Authorization` header
- **Protocol**: JSON-RPC 2.0 over SSE

## Our Implementation vs Apify Requirements

### ✅ What We Have Correct

1. **SSE Endpoint** (`GET /mcp/sse`)
   - ✅ Returns `text/event-stream` content type
   - ✅ Uses Spring's `SseEmitter`
   - ✅ Handles connection lifecycle

2. **JSON-RPC Endpoint** (`POST /mcp/sse`)
   - ✅ Accepts JSON-RPC 2.0 requests
   - ✅ Returns JSON-RPC 2.0 responses
   - ✅ Handles `initialize`, `tools/list`, `tools/call`

3. **MCP Protocol Methods**
   - ✅ `initialize` - Returns server info and capabilities
   - ✅ `tools/list` - Lists available tools
   - ✅ `tools/call` - Executes tools

4. **Tool Discovery**
   - ✅ Auto-discovers `@McpTool` annotated methods
   - ✅ Builds proper input schemas
   - ✅ Returns tools in MCP format

### ⚠️ Potential Issues / Missing Features

1. **Bidirectional Communication**
   - ⚠️ **Issue**: Apify clients may expect to send requests via SSE events, not just POST
   - **Current**: We accept POST requests, but SSE connection might need to handle incoming messages
   - **Apify Pattern**: Client connects via EventSource, sends POST requests, receives SSE events

2. **Session Management**
   - ⚠️ **Missing**: Apify uses `session_id` for correlating messages
   - **Current**: No session management
   - **Impact**: May work for single-client scenarios, but not for multi-client

3. **Authentication**
   - ⚠️ **Missing**: No Authorization header handling
   - **Apify Requirement**: `Authorization: Bearer <TOKEN>` header
   - **Impact**: Won't work with Apify's authentication

4. **SSE Event Format**
   - ⚠️ **Unclear**: Exact SSE event format Apify expects
   - **Current**: We send events with name "message" and JSON data
   - **Need**: Verify if this matches Apify's expectations

5. **Query Parameters**
   - ⚠️ **Missing**: Apify supports query parameters like `?tools=actor1,actor2`
   - **Current**: We accept `message` query param but don't handle tool filtering
   - **Impact**: Can't filter which tools are available

## Required Fixes for Full Apify Compliance

### 1. Add Authentication Support

```java
@GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter sseEndpoint(
    @RequestHeader(value = "Authorization", required = false) String authHeader,
    @RequestParam(required = false) String message) {
    
    // Validate Authorization header
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        // Handle unauthorized
    }
    
    String token = authHeader.substring(7); // Remove "Bearer "
    // Validate token...
}
```

### 2. Add Session Management

```java
private final Map<String, SseEmitter> sessions = new ConcurrentHashMap<>();

@GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public ResponseEntity<?> sseEndpoint(...) {
    String sessionId = UUID.randomUUID().toString();
    SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
    sessions.put(sessionId, emitter);
    
    // Send session_id as initial event
    emitter.send(SseEmitter.event()
        .name("session")
        .data(Map.of("session_id", sessionId)));
    
    return ResponseEntity.ok(emitter);
}
```

### 3. Add Separate Message Endpoint

```java
@PostMapping(value = "/message", consumes = MediaType.APPLICATION_JSON_VALUE)
public ResponseEntity<Map<String, Object>> handleMessage(
    @RequestHeader(value = "Authorization", required = false) String authHeader,
    @RequestBody Map<String, Object> request) {
    
    String sessionId = (String) request.get("session_id");
    SseEmitter emitter = sessions.get(sessionId);
    
    if (emitter == null) {
        return ResponseEntity.badRequest().build();
    }
    
    // Process request and send response via SSE
    handleMcpRequestOverSse(emitter, request);
    return ResponseEntity.ok(Map.of("status", "sent"));
}
```

### 4. Support Tool Filtering

```java
@GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter sseEndpoint(
    @RequestParam(required = false) String tools) {
    
    Set<String> enabledTools = parseToolsParameter(tools);
    // Filter tools based on enabledTools
}
```

## Current Status

### ✅ Works For:
- Basic MCP protocol communication
- Tool discovery and execution
- JSON-RPC 2.0 compliance
- Simple SSE connections

### ❌ Doesn't Work For:
- Apify's authentication (Bearer token)
- Multi-client scenarios (no session management)
- Tool filtering via query parameters
- Full bidirectional SSE communication

## Recommendation

For **Apify deployment**, we need to:
1. ✅ **Keep current implementation** - It works for basic MCP
2. ⚠️ **Add authentication** - Required for Apify
3. ⚠️ **Add session management** - If multi-client support needed
4. ⚠️ **Add tool filtering** - If dynamic tool selection needed

For **testing with Apify**, the current implementation should work for basic tool calls, but authentication will be required for production use.

## Next Steps

1. Test current endpoint with Apify (may work without auth for testing)
2. Add authentication support if needed
3. Add session management if multi-client support required
4. Add tool filtering if dynamic selection needed

