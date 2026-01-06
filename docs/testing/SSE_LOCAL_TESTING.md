# Testing MCP Server with SSE Transport Locally

This guide shows how to test the Jakarta Migration MCP server using SSE (Server-Sent Events) transport instead of stdio, which may bypass the GetInstructions/ListOfferings timeout issue.

## Why SSE?

Some Spring AI MCP projects report better behavior with SSE transport:
- Different initialization flow
- May bypass GetInstructions/ListOfferings issues
- HTTP-based communication (easier to debug)
- Can test with curl/browser

## Step 1: Start the Server with SSE

### Option A: Using Spring Profile

```powershell
cd E:\Source\JakartaMigrationMCP
java -jar build\libs\jakarta-migration-mcp-1.0.0-SNAPSHOT.jar --spring.profiles.active=mcp-sse
```

### Option B: Using Environment Variables

```powershell
$env:MCP_TRANSPORT="sse"
$env:MCP_SSE_PORT="8080"
$env:MCP_SSE_PATH="/mcp/sse"
java -jar build\libs\jakarta-migration-mcp-1.0.0-SNAPSHOT.jar
```

### Option C: Using Command Line Args

```powershell
java -jar build\libs\jakarta-migration-mcp-1.0.0-SNAPSHOT.jar `
  --spring.ai.mcp.server.transport=sse `
  --spring.ai.mcp.server.sse.port=8080 `
  --spring.ai.mcp.server.sse.path=/mcp/sse
```

## Step 2: Verify Server is Running

### Check Server Logs

You should see:
```
Tomcat started on port(s): 8080
MCP Server started with transport: sse
```

### Test Health Endpoint

```powershell
curl http://localhost:8080/actuator/health
```

Should return:
```json
{"status":"UP"}
```

### Test MCP SSE Endpoint

```powershell
curl -N http://localhost:8080/mcp/sse
```

This should start an SSE stream (may look like JSON-RPC messages).

## Step 3: Configure Cursor for SSE

### Update Cursor MCP Configuration

1. Open Cursor Settings (`Ctrl+,`)
2. Navigate to **Features** → **MCP**
3. Replace the jakarta-migration configuration with:

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

**Or** use the provided config file:
- Copy contents from `CURSOR_MCP_CONFIG_SSE.json`
- Paste into Cursor MCP settings

### Important Notes

- **Server must be running first**: Start the server before configuring Cursor
- **Port must match**: Ensure port 8080 matches your server configuration
- **Restart Cursor**: After changing MCP config, restart Cursor completely

## Step 4: Test in Cursor

1. **Restart Cursor completely**
2. **Check MCP status**: Should show jakarta-migration as connected
3. **Test tool discovery**: Ask "What Jakarta migration tools are available?"
4. **Test a tool**: Try "Analyze Jakarta readiness for examples/..."

## Troubleshooting

### Issue: Server won't start

**Check**:
- Port 8080 is not already in use
- Java is installed and in PATH
- JAR file exists at the path

**Solution**:
```powershell
# Check if port is in use
netstat -ano | findstr :8080

# Use different port if needed
$env:MCP_SSE_PORT="9090"
# Then update Cursor config to use port 9090
```

### Issue: Cursor can't connect

**Check**:
- Server is running (check logs)
- Health endpoint works: `curl http://localhost:8080/actuator/health`
- URL in Cursor config matches server path

**Solution**:
- Verify server logs show "MCP Server started with transport: sse"
- Test SSE endpoint: `curl -N http://localhost:8080/mcp/sse`
- Check Cursor MCP logs for connection errors

### Issue: Still getting timeout

**If SSE also times out**:
- This confirms it's a Spring AI MCP framework bug
- Both stdio and SSE are affected
- Need to wait for Spring AI fix or file bug report

## Comparing stdio vs SSE

| Aspect | stdio | SSE |
|--------|-------|-----|
| **Setup** | Simple (just run JAR) | Need to start server first |
| **Debugging** | Harder (stdin/stdout) | Easier (HTTP endpoints) |
| **Port Required** | No | Yes (8080) |
| **Initialization** | Process-based | HTTP-based |
| **Timeout Issue** | Yes (1 minute) | May be different |

## Expected Behavior

### If SSE Works Better

- Tools load without timeout
- GetInstructions/ListOfferings work or are bypassed
- Tools are accessible in Cursor

### If SSE Has Same Issue

- Confirms it's a Spring AI MCP framework bug
- Both transports affected
- Need framework fix

## Next Steps

After testing SSE:

1. **If it works**: Document SSE as the recommended transport
2. **If it doesn't**: File bug report with Spring AI team
3. **Either way**: Update documentation with findings

## Reverting to stdio

To go back to stdio:

1. Update Cursor config to use stdio (see `CURSOR_MCP_CONFIG.json`)
2. Restart Cursor
3. No need to start server separately (stdio runs as process)

