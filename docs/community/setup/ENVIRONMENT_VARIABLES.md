# Environment Variables Setup Guide

This guide explains how to configure environment variables for the Jakarta Migration MCP Server.

## Quick Start

1. **Copy the example file (optional):**
   ```bash
   cp .env.example .env
   ```
   Note: The Jakarta Migration MCP server does not require any secrets or API keys. Use `.env` only if you have other tooling that reads it.

2. **For local development:** No environment variables are required. The server uses defaults (stdio transport, ENTERPRISE tier with all features enabled).

## Environment Variables Reference

### MCP Transport Configuration

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `MCP_TRANSPORT` | Transport mode: `stdio` (local) or `sse` (HTTP) | No | `stdio` |
| `MCP_SSE_PORT` | Port for SSE server (only when transport is `sse`) | No | `8080` |
| `MCP_SSE_PATH` | Path for SSE endpoint | No | `/mcp/sse` |

### npm Wrapper (when using npm package)

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `JAKARTA_MCP_JAR_PATH` | Path to JAR file; if set, the wrapper uses this instead of downloading | No | Empty (auto-download from GitHub releases) |

### Feature Flags (application.yml)

Feature tier is configured in `application.yml` under `jakarta.migration.feature-flags.default-tier`. The default is `ENTERPRISE`, so all features are available. No environment variables are needed for licensing or payment.

## Local Development Setup

### Run with Gradle

```bash
./gradlew bootRun
```

No `.env` or environment variables are required.

### Run with npm wrapper

```bash
# Use default (download JAR from GitHub releases)
npx -y @jakarta-migration/mcp-server

# Or use local JAR
export JAKARTA_MCP_JAR_PATH=/path/to/build/libs/jakarta-migration-mcp-1.0.0-SNAPSHOT.jar
npx -y @jakarta-migration/mcp-server
```

## Troubleshooting

### Environment Variables Not Loading

- The application reads `MCP_TRANSPORT`, `MCP_SSE_PORT`, and `MCP_SSE_PATH` from the process environment.
- Ensure variables are set in the same environment that starts the server (e.g. IDE, terminal, or process manager).

### JAR path not used

- When using the npm wrapper, set `JAKARTA_MCP_JAR_PATH` in the environment that runs `npx` (e.g. your IDE’s MCP server configuration).
- Restart the MCP client after changing environment variables.

## Related Documentation

- [Feature Flags Setup](FEATURE_FLAGS_SETUP.md)
- [NPM Installation Config](NPM_INSTALLATION_CONFIG.md)
- [MCP Transport Configuration](MCP_TRANSPORT_CONFIGURATION.md)
