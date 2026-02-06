# SSE Integration Tests

## Overview

Comprehensive integration tests for the MCP SSE endpoint controller, ensuring Apify compliance and proper MCP protocol implementation.

## Test File

**Location**: `src/test/java/component/jakartamigration/mcp/McpSseControllerIntegrationTest.java`

## Test Coverage

### ✅ SSE Connection Tests

1. **Should establish SSE connection**
   - Verifies GET `/mcp/sse` returns `text/event-stream`
   - Tests basic SSE endpoint functionality

2. **Should accept Authorization header**
   - Verifies `Authorization: Bearer <TOKEN>` header is accepted
   - Tests Apify authentication requirement

### ✅ MCP Protocol Tests

3. **Should handle initialize request**
   - Tests `initialize` method
   - Verifies JSON-RPC 2.0 response format
   - Checks protocol version, capabilities, and server info

4. **Should handle tools/list request**
   - Tests `tools/list` method
   - Verifies tools are returned with proper schema
   - Checks tool structure (name, description, inputSchema)

5. **Should handle tools/call request**
   - Tests `tools/call` method
   - Verifies tool execution
   - Checks response format with content array

6. **Should handle ping request**
   - Tests `ping` method
   - Verifies pong response

7. **Should return error for unknown method**
   - Tests error handling
   - Verifies proper JSON-RPC error format
   - Checks error code (-32601 for method not found)

### ✅ Apify Compliance Tests

8. **Should handle initialize with Authorization header**
   - Tests authentication header acceptance
   - Verifies no errors with Bearer token

9. **Should filter tools via query parameter**
   - Tests `?tools=tool1,tool2` query parameter
   - Verifies endpoint accepts filter parameter

10. **Should return all tools when no filter specified**
    - Tests default behavior (all tools returned)
    - Verifies at least 6 tools are available

## Running the Tests

### Run All SSE Integration Tests

```bash
./gradlew test --tests "component.jakartamigration.mcp.McpSseControllerIntegrationTest"
```

### Run Specific Test

```bash
./gradlew test --tests "component.jakartamigration.mcp.McpSseControllerIntegrationTest.shouldHandleInitializeRequest"
```

### Run with Profile

The tests use `@ActiveProfiles("mcp-sse")` to ensure SSE configuration is loaded.

## Test Configuration

- **Spring Boot Test**: Full web environment with random port
- **MockMvc**: For HTTP endpoint testing
- **Active Profile**: `mcp-sse` (SSE transport configuration)
- **ObjectMapper**: For JSON request/response handling

## What's Tested

### ✅ Covered

- SSE endpoint establishment
- JSON-RPC 2.0 protocol compliance
- MCP standard methods (initialize, tools/list, tools/call, ping)
- Authentication header support
- Tool filtering query parameter
- Error handling
- Response format validation

### ⚠️ Not Covered (Future Enhancements)

- Actual SSE event streaming (requires EventSource client)
- Session management (if implemented)
- Token validation logic (if added)
- Bidirectional SSE communication
- Tool filtering implementation (query param parsing works, but filtering logic not fully tested)

## Test Results

All tests should pass when:
- ✅ Server starts with `mcp-sse` profile
- ✅ All MCP tools are registered
- ✅ SSE endpoint is accessible
- ✅ JSON-RPC protocol is properly implemented

## Integration with CI/CD

These tests can be integrated into CI/CD pipelines to ensure:
- SSE endpoint remains functional
- Apify compliance is maintained
- MCP protocol changes don't break compatibility

## Next Steps

1. **Add SSE Event Streaming Tests**: Test actual SSE event reception
2. **Add Tool Filtering Tests**: Verify tools are actually filtered
3. **Add Session Management Tests**: If session management is implemented
4. **Add Token Validation Tests**: If token validation is added

