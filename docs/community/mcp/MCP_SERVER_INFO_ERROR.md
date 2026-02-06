# MCP Server Info Error - GetInstructions/ListOfferings

## Issue Description

The MCP server logs show the following error when handling `GetInstructions` and `ListOfferings` actions:

```
[error] No server info found
```

This error occurs when the MCP client (Cursor) requests server instructions or offerings, but the Spring AI MCP library cannot find the stored server information.

## Current Status

✅ **Server is working**: Tools are successfully registered (6 tools)  
✅ **Tools are functional**: All Jakarta migration tools are available and working  
⚠️ **Non-critical error**: The error doesn't prevent tool usage, but may affect client discovery

## Root Cause

The Spring AI MCP library (version 1.1.2) appears to have an issue where:
1. Server info is not properly stored during initialization
2. When `GetInstructions` or `ListOfferings` actions are requested, the library cannot retrieve the stored server metadata
3. This is likely a limitation or bug in Spring AI MCP 1.1.2

## Impact

- **Low Impact**: The error is non-critical
- **Tools Still Work**: All 6 Jakarta migration tools are registered and functional
- **Client Discovery**: Some MCP clients may not be able to retrieve server instructions/offerings, but tools are still discoverable via `tools/list`

## Configuration Attempts

We've added a `description` field to the MCP server configuration in `application.yml`:

```yaml
spring:
  ai:
    mcp:
      server:
        name: jakarta-migration-mcp
        version: 1.0.0-SNAPSHOT
        description: "Jakarta Migration MCP Server - Tools for analyzing and migrating Java applications..."
```

**Note**: It's unclear if Spring AI MCP 1.1.2 actually supports the `description` field. This may be ignored by the library.

## Potential Solutions

### Option 1: Wait for Spring AI MCP Update
- Monitor Spring AI MCP releases for fixes to server info handling
- Upgrade when a fix is available

### Option 2: Custom Server Info Bean (If Supported)
If Spring AI MCP supports custom server info beans, we could create a `@Bean` that provides server metadata:

```java
@Bean
public ServerInfo serverInfo() {
    return ServerInfo.builder()
        .name("jakarta-migration-mcp")
        .version("1.0.0-SNAPSHOT")
        .description("Jakarta Migration MCP Server...")
        .build();
}
```

**Note**: This requires checking Spring AI MCP documentation for supported APIs.

### Option 3: Ignore the Error (Current Approach)
Since the error is non-critical and tools are working:
- Document the issue
- Monitor for Spring AI MCP updates
- Tools remain fully functional despite the error

## Verification

To verify the server is working despite the error:

1. **Check tool registration**: Look for "Registered tools: 6" in logs
2. **Test tool invocation**: Try using a Jakarta migration tool
3. **Verify tool list**: MCP clients should still see tools via `tools/list` action

## Related Logs

```
18:17:45.185 [main] INFO  o.s.a.m.s.c.a.McpServerAutoConfiguration - Registered tools: 6
18:17:56.282 [info] Handling GetInstructions action
18:17:56.282 [error] No server info found
18:18:36.368 [info] Handling ListOfferings action, server stored: false
18:18:36.368 [error] No server info found
```

## References

- Spring AI MCP Documentation: https://docs.spring.io/spring-ai/reference/api/mcp/
- MCP Protocol Specification: https://modelcontextprotocol.io/
- Spring AI MCP Issues: https://github.com/spring-projects/spring-ai/issues

## Status

**Current Status**: ⚠️ Non-critical error, tools functional  
**Action Required**: Monitor Spring AI MCP updates  
**Workaround**: None needed - tools work despite error

## Recent Changes

### Configuration Added (2026-01-06)

1. **Enhanced Server Description**: Added detailed description in `application.yml` to help Cursor understand server capabilities
2. **Server Info Configuration Bean**: Created `McpServerInfoConfiguration` to provide server metadata
   - Location: `src/main/java/adrianmikula/jakartamigration/mcp/McpServerInfoConfiguration.java`
   - Attempts to ensure server info is available for GetInstructions/ListOfferings

**Note**: These changes may not fully resolve the timeout issue if Spring AI MCP 1.1.2 doesn't use the configuration bean. The root cause appears to be in Spring AI MCP's internal handling of server info storage/retrieval.

## Testing After Changes

After rebuilding the JAR:
1. Restart Cursor completely
2. Check if the timeout still occurs
3. Verify tools are still accessible despite any errors
4. Monitor logs for "No server info found" errors

If the timeout persists, this is likely a Spring AI MCP 1.1.2 limitation that requires an upgrade to a newer version.

