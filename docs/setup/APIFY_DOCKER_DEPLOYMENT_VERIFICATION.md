# Apify Docker Deployment Verification Guide

**Purpose:** Ensure your Docker deployment correctly exposes MCP tools on Apify platform.

---

## Overview

Your Jakarta Migration MCP server is deployed as an Apify Actor. This guide helps you verify that:
1. ✅ Docker image builds correctly
2. ✅ MCP server starts and exposes tools
3. ✅ Tools are accessible via HTTP endpoints
4. ✅ Apify platform can discover and use your tools

---

## Understanding the Architecture

### How MCP Tools Are Exposed

```
┌─────────────────────────────────────────────────────────┐
│  Apify Actor Container (Docker)                         │
│                                                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │  Spring Boot Application                          │  │
│  │                                                   │  │
│  │  ┌────────────────────────────────────────────┐  │  │
│  │  │  McpStreamableHttpController              │  │  │
│  │  │  Endpoint: /mcp/streamable-http           │  │  │
│  │  │  Methods: initialize, tools/list, tools/call│  │  │
│  │  └────────────────────────────────────────────┘  │  │
│  │                                                   │  │
│  │  ┌────────────────────────────────────────────┐  │  │
│  │  │  JakartaMigrationTools                      │  │  │
│  │  │  @McpTool annotated methods:                │  │  │
│  │  │  - analyzeJakartaReadiness                  │  │  │
│  │  │  - detectBlockers                           │  │  │
│  │  │  - recommendVersions                       │  │  │
│  │  │  - createMigrationPlan                      │  │  │
│  │  │  - analyzeMigrationImpact                   │  │  │
│  │  │  - verifyRuntime                            │  │  │
│  │  └────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────┘  │
│                                                          │
│  Port: ${APIFY_CONTAINER_PORT:-8080}                     │
└─────────────────────────────────────────────────────────┘
           │
           │ HTTP POST /mcp/streamable-http
           │
           ▼
┌─────────────────────────────────────────────────────────┐
│  Apify Platform                                          │
│                                                          │
│  ⚠️  Gateway (mcp.apify.com) only exposes:              │
│     - Apify platform tools (call-actor, etc.)            │
│     - NOT your Actor's MCP tools                        │
│                                                          │
│  ✅  Direct Actor Access:                               │
│     - Container URL when Actor is running                │
│     - Standby mode endpoint                             │
└─────────────────────────────────────────────────────────┘
```

### Key Configuration Files

1. **Dockerfile** - Containerizes your Java application
2. **.actor/actor.json** - Apify Actor configuration
3. **application-mcp-streamable-http.yml** - Spring Boot profile for HTTP transport
4. **McpStreamableHttpController** - Custom controller exposing MCP protocol

---

## Step 1: Local Docker Build & Test

### 1.1 Build Docker Image Locally

```bash
# Build the Docker image
docker build -t jakarta-migration-mcp:test .

# Verify image was created
docker images | grep jakarta-migration-mcp
```

**Expected Output:**
```
jakarta-migration-mcp   test   <image-id>   <size>   <time>
```

### 1.2 Run Container Locally

```bash
# Run container with port mapping
docker run -d \
  --name jakarta-mcp-test \
  -p 8080:8080 \
  -e MCP_TRANSPORT=streamable-http \
  -e SPRING_PROFILES_ACTIVE=mcp-streamable-http \
  jakarta-migration-mcp:test

# Check container logs
docker logs jakarta-mcp-test

# Follow logs in real-time
docker logs -f jakarta-mcp-test
```

**Expected Log Output:**
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.0)

... (Spring Boot startup logs)

Started ProjectNameApplication in X.XXX seconds
MCP Server started on port 8080
MCP Streamable HTTP endpoint: /mcp/streamable-http
```

### 1.3 Verify Health Endpoint

```bash
# Test health endpoint
curl http://localhost:8080/actuator/health

# Expected response:
# {"status":"UP"}
```

### 1.4 Test MCP Initialize

```bash
curl -X POST http://localhost:8080/mcp/streamable-http \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "initialize",
    "params": {
      "protocolVersion": "2024-11-05",
      "capabilities": {},
      "clientInfo": {
        "name": "test-client",
        "version": "1.0.0"
      }
    }
  }'
```

**Expected Response:**
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "protocolVersion": "2024-11-05",
    "capabilities": {
      "tools": {}
    },
    "serverInfo": {
      "name": "jakarta-migration-mcp",
      "version": "1.0.0-SNAPSHOT"
    }
  }
}
```

### 1.5 Test Tools List (Critical Test)

```bash
curl -X POST http://localhost:8080/mcp/streamable-http \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "tools/list",
    "params": {}
  }'
```

**Expected Response (should include all 6 tools):**
```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "result": {
    "tools": [
      {
        "name": "analyzeJakartaReadiness",
        "description": "Analyzes a Java project for Jakarta EE migration readiness...",
        "inputSchema": {
          "type": "object",
          "properties": {
            "projectPath": {
              "type": "string",
              "description": "Path to the Java project root directory"
            }
          },
          "required": ["projectPath"]
        }
      },
      {
        "name": "detectBlockers",
        "description": "Detects dependencies and patterns that prevent Jakarta migration...",
        ...
      },
      {
        "name": "recommendVersions",
        ...
      },
      {
        "name": "createMigrationPlan",
        ...
      },
      {
        "name": "analyzeMigrationImpact",
        ...
      },
      {
        "name": "verifyRuntime",
        ...
      }
    ]
  }
}
```

**✅ Success Criteria:**
- Response contains exactly **6 tools**
- Each tool has `name`, `description`, and `inputSchema`
- No errors in response

### 1.6 Test Tool Execution

```bash
# Test a simple tool call (this will fail without actual project, but should return proper error)
curl -X POST http://localhost:8080/mcp/streamable-http \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 3,
    "method": "tools/call",
    "params": {
      "name": "analyzeJakartaReadiness",
      "arguments": {
        "projectPath": "/tmp/test-project"
      }
    }
  }'
```

**Expected Response (error is OK - means tool is registered):**
```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "result": {
    "content": [
      {
        "type": "text",
        "text": "Error: Project path does not exist..."
      }
    ],
    "isError": true
  }
}
```

### 1.7 Cleanup

```bash
# Stop and remove test container
docker stop jakarta-mcp-test
docker rm jakarta-mcp-test
```

---

## Step 2: Verify Dockerfile Configuration

### 2.1 Check Critical Dockerfile Settings

Review your `Dockerfile` and verify:

```dockerfile
# ✅ Uses APIFY_CONTAINER_PORT (not hardcoded)
ENV MCP_STREAMABLE_HTTP_PORT=${APIFY_CONTAINER_PORT:-8080}
EXPOSE 8080

# ✅ Uses streamable-http transport
ENV MCP_TRANSPORT=streamable-http
ENV SPRING_PROFILES_ACTIVE=mcp-streamable-http

# ✅ CMD passes port to Spring Boot
CMD sh -c "java ... --server.port=${APIFY_CONTAINER_PORT:-8080} ..."
```

### 2.2 Verify Actor Configuration

Check `.actor/actor.json`:

```json
{
  "usesStandbyMode": true,                    // ✅ Required for MCP
  "webServerMcpPath": "/mcp/streamable-http", // ✅ Must match your endpoint
  "defaultRunOptions": {
    "memory": 2048                             // ✅ Sufficient for Java app
  }
}
```

**Critical Settings:**
- ✅ `usesStandbyMode: true` - Enables Apify to keep Actor running
- ✅ `webServerMcpPath` - Must match your MCP endpoint path
- ✅ Memory allocation - Java needs at least 1GB, 2GB recommended

---

## Step 3: Deploy to Apify & Verify

### 3.1 Deploy Actor

```bash
# Push to Apify (if using Apify CLI)
apify push

# Or use Apify Console:
# 1. Go to https://console.apify.com/actors
# 2. Create/Update Actor
# 3. Upload Dockerfile and source code
# 4. Build and deploy
```

### 3.2 Start Actor in Standby Mode

In Apify Console:
1. Go to your Actor
2. Click **"Run"**
3. Select **"Standby Mode"** (if available)
4. Start the run

**Alternative:** Use Apify API:
```bash
curl -X POST "https://api.apify.com/v2/acts/adrian_m~JakartaMigrationMCP/runs" \
  -H "Authorization: Bearer YOUR_APIFY_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "standby": true
  }'
```

### 3.3 Get Container URL

Once Actor is running, get the container URL:

```bash
# Get run details
curl "https://api.apify.com/v2/acts/adrian_m~JakartaMigrationMCP/runs/{RUN_ID}" \
  -H "Authorization: Bearer YOUR_APIFY_TOKEN"

# Look for: "containerUrl": "https://api.apify.com/v2/acts/.../runs/.../container"
```

### 3.4 Test Direct Container Access

```bash
# Test tools/list on container
curl -X POST "https://api.apify.com/v2/acts/adrian_m~JakartaMigrationMCP/runs/{RUN_ID}/container/mcp/streamable-http" \
  -H "Authorization: Bearer YOUR_APIFY_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tools/list",
    "params": {}
  }'
```

**✅ Success:** Should return all 6 tools (same as local test)

---

## Step 4: Understanding Apify Gateway Limitation

### The Problem

When you connect to `https://mcp.apify.com/?tools=actors,docs,adrian_m/JakartaMigrationMCP`:
- ❌ **You get:** Only Apify platform tools (call-actor, search-actors, etc.)
- ✅ **You want:** Your Jakarta Migration MCP tools

### Why This Happens

Apify's MCP gateway (`mcp.apify.com`) is designed to:
1. Expose Apify platform tools (Actors, storage, docs)
2. **NOT** automatically proxy Actor MCP tools

The gateway doesn't query your Actor's MCP endpoint to discover tools.

### Solutions

#### Option 1: Direct Container Access (Recommended for Testing)

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

**Limitations:**
- Requires Actor to be running
- Need to update URL with new run ID each time
- Not suitable for production use

#### Option 2: Use Apify's call-actor Tool

Use the gateway's `call-actor` tool to invoke your Actor, which then exposes its tools.

**Limitations:**
- Adds extra layer of indirection
- More complex integration

#### Option 3: Contact Apify Support

Verify if Apify plans to support Actor MCP tool discovery through the gateway. This may be a feature request.

---

## Step 5: Verification Checklist

Use this checklist to ensure your deployment is correct:

### Pre-Deployment

- [ ] Dockerfile uses `APIFY_CONTAINER_PORT` (not hardcoded port)
- [ ] Dockerfile sets `MCP_TRANSPORT=streamable-http`
- [ ] Dockerfile sets `SPRING_PROFILES_ACTIVE=mcp-streamable-http`
- [ ] `.actor/actor.json` has `usesStandbyMode: true`
- [ ] `.actor/actor.json` has `webServerMcpPath: "/mcp/streamable-http"`
- [ ] `.actor/actor.json` has sufficient memory (≥2048MB)

### Local Testing

- [ ] Docker image builds successfully
- [ ] Container starts without errors
- [ ] Health endpoint responds: `curl http://localhost:8080/actuator/health`
- [ ] MCP initialize returns server info
- [ ] **Tools list returns exactly 6 tools** ⭐ **CRITICAL**
- [ ] Each tool has proper `name`, `description`, `inputSchema`
- [ ] Tool execution endpoint responds (even if with error)

### Apify Deployment

- [ ] Actor builds successfully on Apify
- [ ] Actor starts without errors (check logs)
- [ ] Health endpoint accessible: `{containerUrl}/actuator/health`
- [ ] MCP endpoint accessible: `{containerUrl}/mcp/streamable-http`
- [ ] Tools list returns 6 tools via direct container access
- [ ] Container URL is accessible with Apify API token

### Production Readiness

- [ ] Understand Apify gateway limitation
- [ ] Have plan for exposing tools (direct access or call-actor)
- [ ] Documentation updated with correct connection method
- [ ] Users know how to connect to Actor container

---

## Step 6: Troubleshooting

### Issue: Tools List Returns Empty Array

**Possible Causes:**
1. `@McpTool` annotations not being scanned
2. Controller not discovering tools
3. Wrong endpoint path

**Solutions:**
```bash
# Check if tools are being discovered
# Look in logs for: "Discovered X MCP tools"

# Verify endpoint path matches actor.json
# Should be: /mcp/streamable-http

# Check Spring profile is active
# Should see: "The following profiles are active: mcp-streamable-http"
```

### Issue: Container Won't Start

**Possible Causes:**
1. Port conflict
2. Memory limit too low
3. Java not found

**Solutions:**
```bash
# Check Apify logs for errors
# Common issues:
# - "Port already in use" → Check APIFY_CONTAINER_PORT
# - "OutOfMemoryError" → Increase memory in actor.json
# - "java: command not found" → Check Dockerfile Java installation
```

### Issue: Gateway Doesn't Show Tools

**This is Expected Behavior** - Apify gateway doesn't proxy Actor MCP tools.

**Solution:** Use direct container access (see Step 4).

---

## Step 7: Automated Verification Script

Create a script to automate verification:

```bash
#!/bin/bash
# verify-deployment.sh

set -e

echo "🔍 Verifying Jakarta Migration MCP Deployment..."

# Test health
echo "1. Testing health endpoint..."
HEALTH=$(curl -s http://localhost:8080/actuator/health)
if [[ $HEALTH == *"UP"* ]]; then
  echo "   ✅ Health check passed"
else
  echo "   ❌ Health check failed: $HEALTH"
  exit 1
fi

# Test tools list
echo "2. Testing tools/list..."
TOOLS_RESPONSE=$(curl -s -X POST http://localhost:8080/mcp/streamable-http \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}')

TOOL_COUNT=$(echo $TOOLS_RESPONSE | jq '.result.tools | length')

if [ "$TOOL_COUNT" -eq 6 ]; then
  echo "   ✅ Tools list returned 6 tools"
else
  echo "   ❌ Expected 6 tools, got $TOOL_COUNT"
  echo "   Response: $TOOLS_RESPONSE"
  exit 1
fi

# List tool names
echo "3. Tool names:"
echo $TOOLS_RESPONSE | jq -r '.result.tools[].name' | while read tool; do
  echo "   - $tool"
done

echo ""
echo "✅ All verification checks passed!"
```

**Usage:**
```bash
# Run locally
docker run -d -p 8080:8080 jakarta-migration-mcp:test
sleep 10  # Wait for startup
./verify-deployment.sh
```

---

## Summary

### What You've Verified

1. ✅ Docker image builds correctly
2. ✅ Container starts and exposes MCP endpoint
3. ✅ All 6 MCP tools are discoverable via `tools/list`
4. ✅ Tools have proper schemas and descriptions
5. ✅ Direct container access works on Apify

### Key Takeaways

1. **Your Docker deployment is correct** - Tools are properly exposed
2. **Apify gateway limitation** - Gateway doesn't proxy Actor MCP tools
3. **Use direct container access** - Connect to Actor container URL when running
4. **Standby mode required** - Actor must be running for tools to be accessible

### Next Steps

1. Document the direct container access method for users
2. Consider creating a wrapper/proxy if gateway access is required
3. Monitor Apify platform updates for gateway MCP tool discovery support

---

**Document Status:** Complete  
**Last Updated:** 2026-01-XX  
**Related Docs:**
- [Apify Tool Exposure Issue Summary](docs/investigations/apify-tool-exposure-issue-summary.md)
- [MCP Transport Configuration](docs/setup/MCP_TRANSPORT_CONFIGURATION.md)
- [Apify SSE Compliance](docs/mcp/APIFY_SSE_COMPLIANCE_SUMMARY.md)

