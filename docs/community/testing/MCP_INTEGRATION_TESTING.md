# MCP Integration Testing

## Overview

This document describes the automated integration tests for the Jakarta Migration MCP Server. These tests treat the MCP server as a "black box" and verify that all connections, endpoints, and metadata are valid, just as Claude, Cursor, or Apify would use the server in a real-world scenario.

## Test Strategy: "The Out-of-Process Mock"

The integration tests follow the "Golden Path" for testing MCP servers:

1. **Spin up the MCP server** (as a sub-process for STDIO or as a Spring Boot instance for SSE)
2. **Communicate via JSON-RPC protocol** (the standard MCP protocol)
3. **Verify JSON-RPC response contracts** (ensure responses match MCP specification)

## Test Suites

### 1. STDIO Transport Tests (`McpServerStdioIntegrationTest`)

**Purpose**: Tests the MCP server using STDIO transport, which is used by Cursor, Claude Code, and other local MCP clients.

**How it works**:
- Starts the MCP server as a sub-process (Java process running the JAR)
- Communicates via stdin/stdout using JSON-RPC 2.0 protocol
- Verifies all MCP protocol methods work correctly

**Tests**:
- ✅ Server initialization (`initialize` method)
- ✅ List tools (`tools/list` method)
- ✅ Tool execution (`tools/call` method)
- ✅ Tool input schema validation
- ✅ Error handling for invalid requests
- ✅ All Jakarta migration tools (analyzeJakartaReadiness, detectBlockers, recommendVersions, createMigrationPlan, verifyRuntime)
- ✅ Sentinel tools (check_env)

**Running**:
```bash
# Build the JAR first
gradlew bootJar

# Run STDIO integration tests
gradlew test --tests "integration.mcp.McpServerStdioIntegrationTest"
```

### 2. SSE Transport Tests (`McpServerSseIntegrationTest`)

**Purpose**: Tests the MCP server using SSE transport, which is used by Apify and other HTTP-based MCP clients.

**How it works**:
- Starts the MCP server as a Spring Boot application (in-process)
- Tests the `/mcp/sse` endpoint using MockMvc
- Verifies SSE endpoint and JSON-RPC responses

**Tests**:
- ✅ SSE endpoint connection (`GET /mcp/sse`)
- ✅ Server initialization via POST
- ✅ List tools via POST
- ✅ Tool execution via POST
- ✅ Tool input schema validation
- ✅ Error handling for invalid requests
- ✅ All Jakarta migration tools
- ✅ Sentinel tools

**Running**:
```bash
# Run SSE integration tests
gradlew test --tests "integration.mcp.McpServerSseIntegrationTest"
```

## Test Coverage

### Contract Tests

These tests verify that the MCP server adheres to the MCP protocol specification:

1. **JSON-RPC 2.0 Compliance**
   - All requests must have `jsonrpc: "2.0"`
   - All requests must have an `id` field
   - All responses must match the request `id`
   - Error responses must follow JSON-RPC error format

2. **MCP Protocol Methods**
   - `initialize`: Returns server info and capabilities
   - `tools/list`: Returns list of available tools
   - `tools/call`: Executes a tool and returns results

3. **Tool Metadata**
   - All tools must have a `name` field
   - All tools must have a `description` field (LLMs rely on these!)
   - All tools must have an `inputSchema` field
   - Input schemas must be valid JSON Schema

### Tool Tests

Each MCP tool is tested to ensure:
- ✅ Tool can be called successfully
- ✅ Tool returns valid JSON response
- ✅ Response contains expected fields (e.g., `status`)
- ✅ Tool handles invalid inputs gracefully

### Error Handling Tests

- ✅ Invalid tool names return proper error responses
- ✅ Missing required parameters return proper error responses
- ✅ Server handles malformed JSON gracefully

## Running All Integration Tests

```bash
# Run all MCP integration tests
gradlew test --tests "integration.mcp.*"

# Run with coverage
gradlew test --tests "integration.mcp.*" jacocoTestReport
```

## CI/CD Integration

These tests are designed to run in CI/CD pipelines:

- ✅ **No external dependencies**: Tests don't require LLM API keys
- ✅ **Fast execution**: Tests run in seconds, not minutes
- ✅ **Deterministic**: Tests produce consistent results
- ✅ **Isolated**: Each test is independent and can run in parallel

## Debugging Failed Tests

### Using MCP Inspector

If a test fails and you need to debug the JSON-RPC communication, you can use the MCP Inspector:

```bash
# For STDIO transport
npx @modelcontextprotocol/inspector java -jar build/libs/jakarta-migration-mcp-1.0.0-SNAPSHOT.jar --spring.profiles.active=mcp-stdio

# This will show the JSON-RPC traffic in real-time
```

### Enabling Debug Logging

Add to `application-test.yml`:
```yaml
logging:
  level:
    adrianmikula.jakartamigration: DEBUG
    org.springframework.ai.mcp: DEBUG
```

## Test Data

The tests create temporary test projects with minimal Maven `pom.xml` files to test Jakarta migration tools. These are automatically cleaned up after each test.

## Future Enhancements

Potential improvements to the test suite:

1. **Performance Tests**: Measure tool execution time
2. **Concurrency Tests**: Test multiple simultaneous tool calls
3. **Load Tests**: Test server under high load
4. **Contract Tests**: Use JSON Schema validation for all responses
5. **Snapshot Tests**: Capture and verify tool response formats

## References

- [MCP Protocol Specification](https://modelcontextprotocol.io/docs/specification)
- [JSON-RPC 2.0 Specification](https://www.jsonrpc.org/specification)
- [Spring AI MCP Documentation](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html)

