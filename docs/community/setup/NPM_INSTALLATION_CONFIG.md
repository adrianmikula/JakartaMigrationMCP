# NPM Installation Configuration Guide

This guide explains how to configure the Jakarta Migration MCP Server when installed via npm.

## Configuration (Optional)

The server runs with sensible defaults. You only need configuration if you want to:

- Use a custom JAR path (e.g. local build)
- Override MCP transport for HTTP-based runs

### Environment Variables

Set these in your shell or in your MCP client configuration:

| Variable | Description | Default |
|----------|-------------|---------|
| `JAKARTA_MCP_JAR_PATH` | Path to JAR file (npm wrapper uses this instead of downloading) | Auto-download from GitHub releases |
| `MCP_TRANSPORT` | Transport mode: `stdio` (local) or `sse` (HTTP) | `stdio` |
| `MCP_SSE_PORT` | Port for SSE server (when using SSE) | `8080` |
| `MCP_SSE_PATH` | Path for SSE endpoint | `/mcp/sse` |

### Using a Local JAR

If you build from source and want the npm wrapper to use your JAR:

**Windows (PowerShell):**
```powershell
$env:JAKARTA_MCP_JAR_PATH = "E:\path\to\build\libs\jakarta-migration-mcp-1.0.0-SNAPSHOT.jar"
npx -y @jakarta-migration/mcp-server
```

**Linux/Mac:**
```bash
export JAKARTA_MCP_JAR_PATH=/path/to/build/libs/jakarta-migration-mcp-1.0.0-SNAPSHOT.jar
npx -y @jakarta-migration/mcp-server
```

## JAR Cache Location

When using the npm wrapper without `JAKARTA_MCP_JAR_PATH`, the JAR is downloaded and cached:

- **Windows**: `%USERPROFILE%\AppData\.cache\jakarta-migration-mcp\`
- **Linux/Mac**: `~/.cache/jakarta-migration-mcp/`

## Troubleshooting

### JAR Not Found

1. Run with `--download-only` to force download: `npx -y @jakarta-migration/mcp-server --download-only`
2. If using `JAKARTA_MCP_JAR_PATH`, ensure the path is correct and the JAR exists.
3. Check network access to GitHub releases if auto-download fails.

### Configuration Not Applied

1. Environment variables must be set in the environment that starts the MCP server (e.g. your IDE’s MCP config or shell).
2. Restart the MCP client after changing environment variables.

## Related Documentation

- [Environment Variables](ENVIRONMENT_VARIABLES.md)
- [Feature Flags Setup](FEATURE_FLAGS_SETUP.md)
- [MCP Transport Configuration](MCP_TRANSPORT_CONFIGURATION.md)
