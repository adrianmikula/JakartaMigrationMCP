# MCP Transport Configuration

The Jakarta Migration MCP Server supports two transport mechanisms:

1. **stdio** (Standard Input/Output) - For local use with MCP clients like Cursor, Claude Code
2. **SSE** (Server-Sent Events) - For HTTP-based deployments like Apify

## Transport Modes

### stdio Transport (Default - Local Use)

**Use Case**: Running locally with MCP clients (Cursor, Claude Code, Antigravity)

**How it works**:
- MCP client spawns the server process
- Communication happens via stdin/stdout
- No web server required
- Fastest and most direct communication

**Configuration**:
```yaml
spring:
  ai:
    mcp:
      server:
        transport: stdio
```

**Running**:
```bash
# Via npm wrapper (defaults to stdio)
npx -y @jakarta-migration/mcp-server

# Or explicitly set transport
MCP_TRANSPORT=stdio npx -y @jakarta-migration/mcp-server
```

**MCP Client Configuration** (Cursor/Claude Code):
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

### SSE Transport (HTTP - Apify/Cloud)

**Use Case**: Deploying to Apify, cloud hosting, or HTTP-based MCP clients

**How it works**:
- Server runs as HTTP server
- MCP client connects via HTTP SSE endpoint
- Supports remote access and cloud deployments
- Required for Apify Actor deployments

**Configuration**:
```yaml
spring:
  ai:
    mcp:
      server:
        transport: sse
        sse:
          port: 8080
          path: /mcp/sse
```

**Running**:
```bash
# Set transport to SSE
MCP_TRANSPORT=sse java -jar jakarta-migration-mcp-server.jar

# Or with custom port/path
MCP_TRANSPORT=sse \
MCP_SSE_PORT=8080 \
MCP_SSE_PATH=/mcp/sse \
java -jar jakarta-migration-mcp-server.jar
```

**MCP Client Configuration** (for SSE):
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

## Configuration Methods

### Method 1: Environment Variable (Recommended)

Set `MCP_TRANSPORT` environment variable:

```bash
# stdio (local)
export MCP_TRANSPORT=stdio

# SSE (HTTP)
export MCP_TRANSPORT=sse
export MCP_SSE_PORT=8080
export MCP_SSE_PATH=/mcp/sse
```

### Method 2: Spring Profile

Use Spring profiles to switch modes:

```bash
# stdio mode
java -jar app.jar --spring.profiles.active=mcp-stdio

# SSE mode
java -jar app.jar --spring.profiles.active=mcp-sse
```

### Method 3: Application Properties

Override in `application.yml` or via command line:

```bash
# stdio
java -jar app.jar --spring.ai.mcp.server.transport=stdio

# SSE
java -jar app.jar \
  --spring.ai.mcp.server.transport=sse \
  --spring.ai.mcp.server.sse.port=8080 \
  --spring.ai.mcp.server.sse.path=/mcp/sse
```

## Apify Deployment

When deploying to Apify as an Actor, use SSE transport:

**Dockerfile** (for Apify):
```dockerfile
FROM openjdk:21-jre-slim

COPY jakarta-migration-mcp-server.jar /app/app.jar

ENV MCP_TRANSPORT=sse
ENV MCP_SSE_PORT=8080
ENV MCP_SSE_PATH=/mcp/sse

EXPOSE 8080

CMD ["java", "-jar", "/app/app.jar", "--spring.profiles.active=mcp-sse"]
```

**Apify Actor Configuration**:
- Set environment variable: `MCP_TRANSPORT=sse`
- Expose port: `8080`
- Health check endpoint: `http://localhost:8080/actuator/health`
- MCP endpoint: `http://localhost:8080/mcp/sse`

## Verification

### Verify stdio Mode

1. Start server:
   ```bash
   MCP_TRANSPORT=stdio npx -y @jakarta-migration/mcp-server
   ```

2. Check logs - should see:
   ```
   MCP Server started with transport: stdio
   ```

3. No web server should be running (no HTTP port listening)

### Verify SSE Mode

1. Start server:
   ```bash
   MCP_TRANSPORT=sse java -jar app.jar
   ```

2. Check logs - should see:
   ```
   MCP Server started with transport: sse
   Tomcat started on port(s): 8080
   ```

3. Verify endpoints:
   ```bash
   # Health check
   curl http://localhost:8080/actuator/health
   
   # MCP SSE endpoint (should return SSE stream)
   curl -N http://localhost:8080/mcp/sse
   ```

## Troubleshooting

### Issue: Server not responding in stdio mode

**Solution**: Ensure `--spring.main.web-application-type=none` is set (handled automatically by npm wrapper)

### Issue: SSE endpoint returns 404

**Solution**: 
- Verify `spring.ai.mcp.server.transport=sse` is set
- Check that web server is enabled (not `web-application-type=none`)
- Verify path matches: `/mcp/sse` (or your configured path)

### Issue: Port already in use (SSE mode)

**Solution**: Set custom port:
```bash
MCP_SSE_PORT=9090 java -jar app.jar
```

## Default Behavior

- **npm wrapper (`index.js`)**: Defaults to `stdio` transport
- **Direct JAR execution**: Defaults to `stdio` transport
- **Apify deployment**: Should use `sse` transport

The transport mode is automatically detected based on:
1. `MCP_TRANSPORT` environment variable
2. Spring profile (`mcp-stdio` or `mcp-sse`)
3. Default: `stdio` (for local use)

