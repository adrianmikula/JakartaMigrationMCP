# Quick MCP Configuration Guide

This is a quick reference for configuring the installed MCP servers in Cursor.

## How to Configure

1. **Open Cursor Settings**
   - Press `Ctrl+,` (Windows/Linux) or `Cmd+,` (Mac)
   - Or: File → Preferences → Settings

2. **Navigate to MCP Settings**
   - Search for "MCP" in settings
   - Or go to: Features → MCP

3. **Add Server Configuration**
   - Click "+ Add New MCP Server" or "Add MCP Server"
   - Paste the JSON configuration below
   - Toggle each server ON

4. **Restart Cursor**
   - **Important**: Close Cursor completely and reopen
   - MCP servers only load on startup

## Configuration JSON

Copy this into Cursor's MCP settings:

```json
{
  "mcpServers": {
    "code-index": {
      "command": "npx",
      "args": ["-y", "@hayhandsome/code-index-mcp"],
      "env": {
        "CODE_INDEX_PATH": "./src"
      }
    },
    "memory-bank": {
      "command": "npx",
      "args": [
        "-y",
        "@aakarsh-sasi/memory-bank-mcp",
        "--mode",
        "code",
        "--path",
        ".",
        "--folder",
        ".memory-bank"
      ]
    },
    "maven-deps": {
      "command": "npx",
      "args": ["-y", "mcp-maven-deps"]
    },
    "docker": {
      "command": "npx",
      "args": ["-y", "docker-mcp-server"]
    },
    "architect": {
      "command": "npx",
      "args": ["-y", "@agiflowai/architect-mcp"]
    },
    "postgresql": {
      "command": "npx",
      "args": ["-y", "mcp-server-postgresql"],
      "env": {
        "POSTGRES_HOST": "localhost",
        "POSTGRES_PORT": "5432",
        "POSTGRES_DB": "your_database",
        "POSTGRES_USER": "your_user",
        "POSTGRES_PASSWORD": "your_password"
      }
    }
  }
}
```

## Server Status

| Server | Status | Notes |
|--------|--------|-------|
| Code Index | ✅ Installed | Needs Cursor configuration |
| Memory Bank | ✅ Installed | Has path bug (may need workaround) |
| Semgrep | ✅ Working | Already configured |
| Maven Dependencies | ✅ Installed | Alternative to @maven-tools/mcp-server |
| Docker | ✅ Installed | Alternative to @modelcontextprotocol/server-docker |
| Architect | ✅ Installed | Alternative to @squirrelogic/mcp-architect |
| PostgreSQL | ✅ Installed | Alternative to @henkdz/postgresql-mcp-server |

## Testing After Configuration

After restarting Cursor, test the servers:

1. **Code Index**: Ask "Search the codebase for Spring Boot configuration"
2. **Memory Bank**: Ask "Remember that we use TDD approach"
3. **Semgrep**: Already working - can scan files automatically

## Troubleshooting

### Server Not Found
- Ensure npm packages are installed globally: `npm list -g`
- Try using full path instead of `npx`

### Path Issues (Memory Bank)
- The Memory Bank MCP has a known path resolution bug
- Workaround: Try different path formats or report issue to maintainer

### Server Not Loading
- Restart Cursor completely
- Check Cursor's MCP logs (if available)
- Verify command and args are correct

## Next Steps

See `MCP_TEST_RESULTS.md` for detailed testing results and recommendations.

