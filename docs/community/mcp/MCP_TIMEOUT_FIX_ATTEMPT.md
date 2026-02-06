# MCP Timeout Fix Attempt - Disabling Instructions/Offerings

## Issue

Cursor is timing out after 1 minute when loading MCP tools. The timeout is caused by `GetInstructions` and `ListOfferings` actions hanging because Spring AI MCP 1.1.2 can't find server info.

## Fix Attempt

Added configuration to disable instructions and offerings capabilities in `application.yml`:

```yaml
spring:
  ai:
    mcp:
      server:
        capabilities:
          instructions: false
          offerings: false
          tools: true
          resources: true
          prompts: true
```

## Testing

After rebuilding the JAR:
1. Restart Cursor completely
2. Check if tools load without timeout
3. Verify tools are still accessible

## Notes

- **Uncertain if supported**: Spring AI MCP 1.1.2 may not support disabling capabilities via configuration
- **If it doesn't work**: This is a known Spring AI MCP 1.1.2 limitation that may require upgrading to a newer version
- **Tools should still work**: Even if instructions/offerings fail, tools are discoverable via `tools/list`

## Alternative Solutions

If this doesn't work:
1. Upgrade Spring AI MCP to a newer version (if available)
2. Wait for Spring AI MCP fix
3. Use tools directly via `tools/list` (bypasses instructions/offerings)

