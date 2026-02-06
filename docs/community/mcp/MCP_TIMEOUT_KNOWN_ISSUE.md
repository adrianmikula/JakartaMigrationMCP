# MCP Timeout Issue - Known Spring AI MCP 1.1.2 Limitation

## Status: ⚠️ BLOCKING ISSUE

Cursor times out after 1 minute when loading MCP tools due to Spring AI MCP 1.1.2 bug with GetInstructions/ListOfferings.

## Root Cause

Spring AI MCP 1.1.2 has a bug where:
1. It enables `instructions` and `offerings` capabilities automatically
2. When Cursor requests `GetInstructions` or `ListOfferings`, Spring AI MCP tries to retrieve server info
3. Server info is not properly stored/retrieved, causing the request to hang
4. Cursor times out after 1 minute, marking the server as errored
5. Tools never get discovered because Cursor gives up before calling `tools/list`

## Attempted Fixes (All Failed)

1. ❌ **Added server description** - Spring AI MCP doesn't use it
2. ❌ **Created McpServerInfoConfiguration bean** - Spring AI MCP doesn't use it
3. ❌ **Disabled capabilities in application.yml** - Spring AI MCP 1.1.2 doesn't support this config

## Impact

- **BLOCKING**: Cursor cannot discover tools because it times out during initialization
- **Tools are registered**: Server logs show "Registered tools: 6"
- **Tools are functional**: But Cursor never gets to use them due to timeout

## Solutions

### Option 1: Upgrade Spring AI MCP (Recommended)

Check for a newer version that fixes this issue:

```kotlin
// In build.gradle.kts
extra["springAiVersion"] = "1.1.3" // or latest available
```

Then rebuild and test.

### Option 2: Wait for Spring AI MCP Fix

Monitor:
- Spring AI GitHub: https://github.com/spring-projects/spring-ai/issues
- Spring AI Releases: https://github.com/spring-projects/spring-ai/releases

### Option 3: Use Alternative MCP Implementation

Consider:
- Using a different MCP server framework
- Implementing a custom MCP server without Spring AI MCP
- Using SSE transport instead of stdio (may have different behavior)

### Option 4: Workaround - Manual Tool Discovery

If Cursor supports it, try:
1. Skip the initial discovery phase
2. Call `tools/list` directly
3. Use tools by name

**Note**: This requires Cursor to support bypassing the initialization phase.

## Current Configuration

```yaml
spring:
  ai:
    mcp:
      server:
        name: jakarta-migration-mcp
        version: 1.0.0-SNAPSHOT
        transport: stdio
        type: SYNC
        annotation-scanner:
          enabled: true
```

## Verification

To verify the issue:
1. Check Cursor MCP logs - should show timeout after ~1 minute
2. Check server logs - should show "No server info found" errors
3. Server logs should show "Registered tools: 6" before timeout

## Next Steps

1. **Check for Spring AI MCP updates**: Look for version 1.1.3 or later
2. **File a bug report**: If no fix exists, report to Spring AI team
3. **Consider alternatives**: Evaluate other MCP server frameworks
4. **Monitor Spring AI releases**: Watch for fixes

## References

- Spring AI MCP Docs: https://docs.spring.io/spring-ai/reference/api/mcp/
- Spring AI Issues: https://github.com/spring-projects/spring-ai/issues
- MCP Protocol: https://modelcontextprotocol.io/

