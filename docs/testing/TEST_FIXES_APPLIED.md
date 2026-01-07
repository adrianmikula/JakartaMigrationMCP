# Test Fixes Applied - QA Check

## Issue Identified

**Test Failure**: `McpServerSseIntegrationTest > testToolInputSchemaValidation() FAILED`
- **Error**: `IllegalStateException: ApplicationContext failure threshold (1) exceeded`
- **Root Cause**: Spring ApplicationContext failing to load, likely due to missing or conflicting configuration

## Fixes Applied

### 1. Added Test Properties to McpServerSseIntegrationTest

**File**: `src/test/java/integration/mcp/McpServerSseIntegrationTest.java`

Added test properties to disable optional services:
```java
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    classes = adrianmikula.projectname.ProjectNameApplication.class,
    properties = {
        "jakarta.migration.stripe.enabled=false",
        "jakarta.migration.apify.enabled=false",
        "jakarta.migration.storage.file.enabled=false",
        "spring.ai.mcp.server.transport=sse"
    }
)
```

**Why**: These services may require external dependencies or configuration that aren't available in test environment.

### 2. Created Test-Specific Configuration

**File**: `src/test/resources/application-mcp-sse.yml` (NEW)

Created test-specific configuration for `mcp-sse` profile:
```yaml
jakarta:
  migration:
    stripe:
      enabled: false
    apify:
      enabled: false
    storage:
      file:
        enabled: false

spring:
  ai:
    mcp:
      server:
        transport: sse
        sse:
          port: 8080
          path: /mcp/sse
```

**Why**: Provides test-specific defaults that don't require external services.

## Expected Result

After these fixes:
- ✅ Spring ApplicationContext should load successfully
- ✅ Test should be able to run
- ✅ MockMvc should be available for testing

## Verification

To verify the fix works:

```bash
# Run the specific failing test
mise run test --tests "integration.mcp.McpServerSseIntegrationTest.testToolInputSchemaValidation"

# Or run all SSE integration tests
mise run test --tests "integration.mcp.McpServerSseIntegrationTest"
```

## Additional Notes

If the test still fails, check:
1. **Actual error message** - Look for the root cause in the full stack trace
2. **Missing beans** - Check if any required beans are missing
3. **Configuration conflicts** - Verify no conflicting properties
4. **Dependencies** - Ensure all test dependencies are available

## Related Files

- `src/test/java/integration/mcp/McpServerSseIntegrationTest.java` - Test file
- `src/test/resources/application-mcp-sse.yml` - Test configuration
- `src/main/resources/application-mcp-sse.yml` - Main configuration (for reference)

