# Cursor vs Apify Transport - Important Distinction

## Key Finding: Cursor Doesn't Support SSE Transport

**Important**: Cursor IDE primarily supports **stdio transport** for MCP servers, not SSE. The SSE endpoint we created is specifically for **Apify deployment**, not for Cursor.

## Transport Usage

### stdio Transport - For Cursor IDE
- ✅ **Supported by Cursor**
- ✅ **Recommended for local development**
- ✅ **Works with Cursor's MCP integration**

**Configuration for Cursor:**
```json
{
  "mcpServers": {
    "jakarta-migration": {
      "command": "java",
      "args": [
        "-jar",
        "path/to/jakarta-migration-mcp-1.0.0-SNAPSHOT.jar",
        "--spring.profiles.active=mcp-stdio"
      ]
    }
  }
}
```

### SSE Transport - For Apify Only
- ✅ **For Apify Actor deployment**
- ✅ **For HTTP-based MCP clients**
- ❌ **NOT supported by Cursor IDE**

**Configuration for Apify:**
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

## Why "No Tools" Error in Cursor with SSE?

When you configure Cursor to use the SSE endpoint (`type: "sse"`), Cursor:
1. Connects to the SSE endpoint
2. But doesn't properly handle the SSE protocol as implemented
3. Shows "no tools, prompts or resources" because it can't discover tools via SSE

**This is expected** - Cursor doesn't fully support SSE transport for MCP servers.

## Solution

### For Cursor: Use stdio Transport

1. **Update Cursor configuration** to use stdio:
```json
{
  "mcpServers": {
    "jakarta-migration": {
      "command": "java",
      "args": [
        "-jar",
        "E:\\Source\\JakartaMigrationMCP\\build\\libs\\jakarta-migration-mcp-1.0.0-SNAPSHOT.jar"
      ]
    }
  }
}
```

2. **Restart Cursor completely**

3. **Tools should now be discoverable**

### For Apify: Use SSE Transport

The SSE endpoint (`/mcp/sse`) is specifically designed for Apify:
- ✅ Works with Apify MCP server
- ✅ HTTP-based communication
- ✅ Cloud deployment ready

## Summary

- **Cursor**: Use **stdio** transport (command-based)
- **Apify**: Use **SSE** transport (HTTP-based)
- **SSE endpoint**: Created for Apify, not for Cursor
- **"No tools" error**: Expected when using SSE with Cursor (not supported)

## Next Steps

1. **For Cursor**: Switch to stdio transport configuration
2. **For Apify**: Use the SSE endpoint we created
3. **Test**: Verify tools are discoverable with stdio in Cursor

