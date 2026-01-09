# Apify Deployment Quick Reference

**Quick guide to ensure your Docker deployment correctly exposes MCP tools on Apify.**

---

## ✅ Pre-Deployment Checklist

### Dockerfile Must Have:

```dockerfile
# ✅ Use APIFY_CONTAINER_PORT (not hardcoded)
ENV MCP_STREAMABLE_HTTP_PORT=${APIFY_CONTAINER_PORT:-8080}
EXPOSE 8080

# ✅ Use streamable-http transport
ENV MCP_TRANSPORT=streamable-http
ENV SPRING_PROFILES_ACTIVE=mcp-streamable-http

# ✅ Pass port to Spring Boot
CMD sh -c "java ... --server.port=${APIFY_CONTAINER_PORT:-8080} ..."
```

### `.actor/actor.json` Must Have:

```json
{
  "usesStandbyMode": true,                    // ✅ Required
  "webServerMcpPath": "/mcp/streamable-http", // ✅ Must match endpoint
  "defaultRunOptions": {
    "memory": 2048                             // ✅ At least 2GB for Java
  }
}
```

---

## 🧪 Quick Verification (Local)

### 1. Build & Run Locally

```bash
# Build
docker build -t jakarta-mcp:test .

# Run
docker run -d -p 8080:8080 \
  -e MCP_TRANSPORT=streamable-http \
  -e SPRING_PROFILES_ACTIVE=mcp-streamable-http \
  jakarta-mcp:test

# Wait for startup (10-15 seconds)
sleep 10
```

### 2. Test Tools List (CRITICAL)

```bash
curl -X POST http://localhost:8080/mcp/streamable-http \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tools/list",
    "params": {}
  }'
```

**✅ Success:** Should return exactly **6 tools**:
- `analyzeJakartaReadiness`
- `detectBlockers`
- `recommendVersions`
- `createMigrationPlan`
- `analyzeMigrationImpact`
- `verifyRuntime`

### 3. Use Verification Script

```powershell
# Windows
.\scripts\verify-apify-mcp-deployment.ps1

# Or with custom URL
.\scripts\verify-apify-mcp-deployment.ps1 -ContainerUrl "http://localhost:8080" -Verbose
```

---

## 🚀 Apify Deployment

### Deploy to Apify

1. Push code to Apify (via CLI or Console)
2. Build Actor
3. Start Actor in **Standby Mode**

### Get Container URL

Once Actor is running, get container URL from:
- Apify Console → Actor → Run → Container URL
- Or API: `GET /v2/acts/{actorId}/runs/{runId}`

### Test Direct Container Access

```bash
curl -X POST "{CONTAINER_URL}/mcp/streamable-http" \
  -H "Authorization: Bearer YOUR_APIFY_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tools/list",
    "params": {}
  }'
```

**✅ Success:** Should return 6 tools (same as local test)

---

## ⚠️ Important: Apify Gateway Limitation

### The Issue

When connecting to `https://mcp.apify.com/?tools=actors,docs,adrian_m/JakartaMigrationMCP`:
- ❌ You get: Only Apify platform tools (call-actor, search-actors, etc.)
- ✅ You want: Your Jakarta Migration MCP tools

### Why

Apify's MCP gateway (`mcp.apify.com`) **does not** automatically proxy Actor MCP tools. It only exposes Apify platform tools.

### Solution: Direct Container Access

Connect directly to the Actor container when it's running:

```json
{
  "mcpServers": {
    "jakarta-migration": {
      "type": "streamable-http",
      "url": "https://api.apify.com/v2/acts/adrian_m~JakartaMigrationMCP/runs/{RUN_ID}/container/mcp/streamable-http",
      "headers": {
        "Authorization": "Bearer YOUR_APIFY_TOKEN"
      }
    }
  }
}
```

**Note:** Requires Actor to be running in standby mode.

---

## 🔍 Troubleshooting

### Tools List Returns Empty Array

**Check:**
1. Spring profile is active: `mcp-streamable-http`
2. Endpoint path matches: `/mcp/streamable-http`
3. `@McpTool` annotations are being scanned
4. Check logs for: "Discovered X MCP tools"

### Container Won't Start

**Check:**
1. Port: `APIFY_CONTAINER_PORT` is set correctly
2. Memory: At least 2048MB in `actor.json`
3. Java: Installed in Dockerfile
4. Logs: Check Apify console for errors

### Gateway Doesn't Show Tools

**This is expected!** Gateway doesn't proxy Actor MCP tools.

**Solution:** Use direct container access (see above).

---

## 📋 Complete Verification Checklist

- [ ] Dockerfile uses `APIFY_CONTAINER_PORT`
- [ ] Dockerfile sets `MCP_TRANSPORT=streamable-http`
- [ ] `actor.json` has `usesStandbyMode: true`
- [ ] `actor.json` has `webServerMcpPath: "/mcp/streamable-http"`
- [ ] Local Docker test: Tools list returns 6 tools
- [ ] Apify deployment: Actor builds successfully
- [ ] Apify deployment: Direct container access returns 6 tools
- [ ] Documentation updated with direct container access method

---

## 📚 Related Documentation

- **[Complete Verification Guide](APIFY_DOCKER_DEPLOYMENT_VERIFICATION.md)** - Detailed step-by-step guide
- **[Tool Exposure Issue Summary](../investigations/apify-tool-exposure-issue-summary.md)** - Understanding the gateway limitation
- **[MCP Transport Configuration](MCP_TRANSPORT_CONFIGURATION.md)** - Transport details

---

**Quick Test Command:**

```bash
# One-liner to test tools list
curl -X POST http://localhost:8080/mcp/streamable-http \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}' \
  | jq '.result.tools | length'

# Should output: 6
```

