# MCP Dual Package Exposure to AI Agents

## Overview

With the dual package approach, AI agents will see **2 separate MCP servers**, not one unified server.

## Server Configuration

### Free Package (`@jakarta-migration/mcp-server`)

**MCP Server Name**: `jakarta-migration-mcp-free` (or `jakarta-migration-mcp`)

**Tools Exposed** (4 tools):
1. `analyzeJakartaReadiness` - Analyzes project for Jakarta migration readiness
2. `detectBlockers` - Detects blockers preventing Jakarta migration
3. `recommendVersions` - Recommends Jakarta-compatible dependency versions
4. `analyzeMigrationImpact` - Full migration impact analysis

**Configuration Example**:
```json
{
  "mcpServers": {
    "jakarta-migration-free": {
      "command": "npx",
      "args": ["-y", "@jakarta-migration/mcp-server"]
    }
  }
}
```

### Premium Package (`@jakarta-migration/mcp-server-premium`)

**MCP Server Name**: `jakarta-migration-mcp-premium`

**Tools Exposed** (7 tools):
1. `analyzeJakartaReadiness` - (Free tool, included)
2. `detectBlockers` - (Free tool, included)
3. `recommendVersions` - (Free tool, included)
4. `analyzeMigrationImpact` - (Free tool, included)
5. `createMigrationPlan` - Creates comprehensive migration plan (PREMIUM)
6. `refactorProject` - Automatically refactors source code (PREMIUM)
7. `verifyRuntime` - Verifies runtime execution (PREMIUM)

**Configuration Example**:
```json
{
  "mcpServers": {
    "jakarta-migration-premium": {
      "command": "npx",
      "args": ["-y", "@jakarta-migration/mcp-server-premium"]
    }
  }
}
```

## How AI Agents See It

### Scenario 1: Free User
- **Installs**: `@jakarta-migration/mcp-server`
- **Sees**: 1 MCP server with 4 tools
- **Can Use**: All 4 free analysis tools

### Scenario 2: Premium User (Recommended)
- **Installs**: `@jakarta-migration/mcp-server-premium` only
- **Sees**: 1 MCP server with 7 tools
- **Can Use**: All tools (free + premium)
- **Note**: Premium package includes all free tools, so no need to install free package

### Scenario 3: Premium User (Both Installed - Not Recommended)
- **Installs**: Both `@jakarta-migration/mcp-server` and `@jakarta-migration/mcp-server-premium`
- **Sees**: 2 separate MCP servers
  - Server 1: 4 free tools
  - Server 2: 7 tools (4 free + 3 premium)
- **Issue**: Duplicate tools (4 free tools appear in both servers)
- **Recommendation**: Premium users should only install the premium package

## Tool Discovery

Each MCP server exposes tools via the `tools/list` endpoint. The Spring AI MCP Server framework:

1. Scans for `@McpTool` annotations on methods in `JakartaMigrationTools` class
2. Builds tool schemas from method parameters
3. Returns tool list in MCP protocol format

**Free Server** (`JakartaMigrationTools.java` in main project):
- Scans only the free version class
- Finds 4 `@McpTool` annotations
- Returns 4 tools

**Premium Server** (`JakartaMigrationTools.java` in premium package):
- Scans the premium version class
- Finds 7 `@McpTool` annotations (4 free + 3 premium)
- Returns 7 tools

## Best Practices

### For Free Users
```json
{
  "mcpServers": {
    "jakarta-migration": {
      "command": "npx",
      "args": ["-y", "@jakarta-migration/mcp-server"]
    }
  }
}
```

### For Premium Users
```json
{
  "mcpServers": {
    "jakarta-migration": {
      "command": "npx",
      "args": ["-y", "@jakarta-migration/mcp-server-premium"]
    }
  }
}
```

**Do NOT install both packages** - Premium package includes all free tools.

## Tool Naming

Both servers use the same tool names for free tools:
- `analyzeJakartaReadiness`
- `detectBlockers`
- `recommendVersions`
- `analyzeMigrationImpact`

This means:
- ✅ Premium users get the same free tools with the same names
- ⚠️ If both servers are installed, AI agents will see duplicate tools
- ✅ AI agents can use either server's free tools (they're functionally identical)

## Server Identification

Each server identifies itself with a different name:

**Free Server**:
```json
{
  "serverInfo": {
    "name": "jakarta-migration-mcp-free",
    "version": "1.0.0"
  }
}
```

**Premium Server**:
```json
{
  "serverInfo": {
    "name": "jakarta-migration-mcp-premium",
    "version": "1.0.0"
  }
}
```

AI agents can distinguish between servers using the `serverInfo.name` field.

## Summary

- **2 separate MCP servers** (not one unified server)
- **Free users**: 1 server, 4 tools
- **Premium users**: 1 server, 7 tools (recommended - don't install free package)
- **Tool names are consistent** across both servers for free tools
- **Premium package is self-contained** - includes all free tools

