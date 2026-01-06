# Fixing "No Tools" Issue with SSE Endpoint

## Problem

When connecting to the SSE endpoint (`/mcp/sse`), Cursor shows "no tools, prompts, or resources" even though:
- ✅ Tests pass (endpoint works programmatically)
- ✅ Tools are registered (6 tools available)
- ✅ POST endpoint returns tools correctly

## Root Cause

Cursor with SSE transport expects:
1. **SSE connection** via GET `/mcp/sse` (receives SSE stream)
2. **Requests** sent via POST (but responses should come via SSE events)
3. **Session correlation** between POST requests and SSE connection

Our current implementation:
- ✅ GET `/mcp/sse` returns SseEmitter
- ✅ POST `/mcp/sse` accepts requests
- ❌ POST responses are JSON (not sent via SSE)
- ❌ No session management to correlate requests with SSE connections

## Solution Implemented

### 1. Session Management

Added session-based connection tracking:
- Each SSE connection gets a unique `sessionId`
- Session ID sent as initial SSE event
- Active connections stored in `ConcurrentHashMap`

### 2. Bidirectional Communication

Updated POST endpoint to:
- Accept `session_id` parameter
- If session_id provided, send response via SSE
- If no session_id, return JSON directly (backward compatible)

### 3. Alternative Message Endpoint

Added `/mcp/message` endpoint for SSE-based communication:
- Client sends POST requests here
- Server responds via SSE stream
- Requires `session_id` in request

## How It Works Now

### For SSE Clients (Cursor/Apify)

1. **Connect to SSE**:
   ```
   GET /mcp/sse
   → Receives: {"type": "session", "sessionId": "uuid-here"}
   ```

2. **Send Requests**:
   ```
   POST /mcp/sse?session_id=uuid-here
   Content-Type: application/json
   {"jsonrpc": "2.0", "id": 1, "method": "initialize", "params": {...}}
   → Response sent via SSE event
   ```

3. **Receive Responses**:
   - Responses come via SSE events on the original connection
   - Event name: "message"
   - Event data: JSON-RPC response

### For Non-SSE Clients

Still works with direct JSON responses:
```
POST /mcp/sse
Content-Type: application/json
{"jsonrpc": "2.0", "id": 1, "method": "tools/list", "params": {}}
→ Returns: {"jsonrpc": "2.0", "id": 1, "result": {"tools": [...]}}
```

## Testing

### Manual Test

1. **Start server**:
   ```bash
   java -jar build/libs/jakarta-migration-mcp-1.0.0-SNAPSHOT.jar --spring.profiles.active=mcp-sse
   ```

2. **Connect to SSE** (in browser console or curl):
   ```javascript
   const eventSource = new EventSource('http://localhost:8080/mcp/sse');
   eventSource.onmessage = (e) => {
       console.log('Received:', JSON.parse(e.data));
   };
   ```

3. **Send initialize request**:
   ```bash
   curl -X POST "http://localhost:8080/mcp/sse?session_id=YOUR_SESSION_ID" \
     -H "Content-Type: application/json" \
     -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test","version":"1.0.0"}}}'
   ```

4. **Check SSE stream** for response

### For Cursor

Cursor should now:
1. Connect to GET `/mcp/sse`
2. Receive session ID
3. Send POST requests with session_id
4. Receive responses via SSE
5. See tools properly

## If Still Not Working

### Check Cursor Logs

Look for:
- SSE connection established
- Session ID received
- POST requests being sent
- Responses received

### Alternative: Use stdio for Cursor

If SSE still doesn't work with Cursor, use stdio transport:
```json
{
  "mcpServers": {
    "jakarta-migration": {
      "command": "java",
      "args": [
        "-jar",
        "path/to/jakarta-migration-mcp-1.0.0-SNAPSHOT.jar"
      ]
    }
  }
}
```

**Note**: SSE is primarily for Apify deployment. Cursor works best with stdio transport.

## Next Steps

1. Test with Cursor to verify tools are visible
2. If still not working, check Cursor's SSE implementation requirements
3. Consider that Cursor might not fully support SSE transport (use stdio instead)

