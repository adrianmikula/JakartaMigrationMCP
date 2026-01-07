# Apify Deployment Fixes Applied

## Date: 2026-01-07

## Summary

Applied all recommended fixes from the investigation document to resolve the "invisible tool" problem on Apify deployment.

## Fixes Applied

### 1. ✅ Updated Dockerfile to Use APIFY_CONTAINER_PORT

**Problem**: Dockerfile was using hardcoded port 8080, but Apify requires using `APIFY_CONTAINER_PORT` environment variable.

**Fix Applied**:
- Updated `ENV MCP_SSE_PORT` to use `${APIFY_CONTAINER_PORT:-8080}` (fallback to 8080 if not set)
- Updated `HEALTHCHECK` to use `${APIFY_CONTAINER_PORT:-8080}`
- Updated `CMD` to pass `APIFY_CONTAINER_PORT` to Spring Boot via system property and `--server.port`

**Files Changed**:
- `Dockerfile`

**Key Changes**:
```dockerfile
# Before
ENV MCP_SSE_PORT=8080
CMD ["java", "-jar", "app.jar", ...]

# After
ENV MCP_SSE_PORT=${APIFY_CONTAINER_PORT:-8080}
CMD ["sh", "-c", "java ... -DMCP_SSE_PORT=${APIFY_CONTAINER_PORT:-8080} ... --server.port=${APIFY_CONTAINER_PORT:-8080}"]
```

### 2. ✅ Added Standby Mode and webServerMcpPath to actor.json

**Problem**: Apify won't "see" the MCP tools unless the Actor configuration specifies where the MCP endpoint lives.

**Fix Applied**:
- Added `usesStandbyMode: true` - Enables Apify's standby mode for MCP servers
- Added `webServerMcpPath: "/mcp/sse"` - Tells Apify where the MCP endpoint is located

**Files Changed**:
- `.actor/actor.json`

**Key Changes**:
```json
{
  "actorSpecification": 1,
  ...
  "usesStandbyMode": true,
  "webServerMcpPath": "/mcp/sse"
}
```

### 3. ✅ Updated Spring Boot Configuration to Read APIFY_CONTAINER_PORT

**Problem**: Spring Boot was only reading from `MCP_SSE_PORT`, not `APIFY_CONTAINER_PORT`.

**Fix Applied**:
- Updated `application-mcp-sse.yml` to check `APIFY_CONTAINER_PORT` first, then fall back to `MCP_SSE_PORT`, then default to 8080
- Applied to both `server.port` and `spring.ai.mcp.server.sse.port`

**Files Changed**:
- `src/main/resources/application-mcp-sse.yml`

**Key Changes**:
```yaml
# Before
server:
  port: ${MCP_SSE_PORT:8080}

# After
server:
  port: ${APIFY_CONTAINER_PORT:${MCP_SSE_PORT:8080}}
```

### 4. ✅ Verified Input Schema is Valid

**Problem**: MCP tools are dynamically generated from the Actor's Input Schema. If the schema is missing or invalid, tools won't appear.

**Status**: ✅ **Input schema is valid**
- Schema exists at `.actor/input_schema.json`
- Valid JSON format
- Not too complex (no deeply nested objects that would cause issues)
- Follows Apify's input schema specification

## Additional Considerations

### Protocol Mismatch
✅ **Already Correct**: The server uses HTTP transport (SSE) via `McpSseController`, not stdio. This is correct for Apify deployment.

### Memory Limits
⚠️ **Note**: The Actor may need memory configuration in `actor.json` if migration tasks are memory-intensive. Default is usually 256MB or 512MB, which might be insufficient for large Java projects.

**Recommendation**: Consider adding memory configuration to `.actor/actor.json`:
```json
{
  "defaultRunOptions": {
    "memory": 2048
  }
}
```

### Authentication
✅ **Already Handled**: The server supports authentication via `Authorization: Bearer <TOKEN>` header, as implemented in `McpSseController`.

## Testing Checklist

After deploying to Apify, verify:

1. ✅ **Actor starts successfully** - Check Apify Console logs
2. ✅ **Health check passes** - `/actuator/health` endpoint responds
3. ✅ **MCP endpoint accessible** - `GET /mcp/sse` returns SSE stream
4. ✅ **Tools are visible** - MCP client can see Jakarta migration tools
5. ✅ **Port configuration** - Server listens on `APIFY_CONTAINER_PORT`

## Next Steps

1. **Deploy to Apify** with these fixes
2. **Test MCP connection** using `https://mcp.apify.com?tools=actors,adrian_m/JakartaMigrationMCP`
3. **Verify tools appear** in MCP client
4. **Monitor logs** for any port or transport issues

## References

- [Apify Actor Documentation](https://docs.apify.com/platform/actors)
- [Apify Standby Mode](https://docs.apify.com/platform/actors/development/standby-mode)
- [Investigation Document](./apify-invisible-deployment-01-07-2026.md)

