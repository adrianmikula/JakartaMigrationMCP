# MCP Server Testing Guide

## Setup Complete ✅

The Jakarta Migration MCP Server has been configured with Spring AI MCP support:

1. ✅ **Dependency Added**: `spring-ai-starter-mcp-server` in `build.gradle.kts`
2. ✅ **@McpTool Annotations**: All 5 tools are annotated
3. ✅ **Configuration Added**: MCP server config in `application.yml`

## Tools Exposed

The following MCP tools are now available:

1. **analyzeJakartaReadiness** - Analyzes project for Jakarta migration readiness
2. **detectBlockers** - Detects blockers preventing Jakarta migration
3. **recommendVersions** - Recommends Jakarta-compatible dependency versions
4. **createMigrationPlan** - Creates comprehensive migration plan
5. **verifyRuntime** - Verifies runtime execution of migrated applications

## Building and Running

### Step 1: Build the Project

```bash
# Using Gradle
gradle build

# Or if you have gradlew
./gradlew build
```

### Step 2: Run the MCP Server

```bash
# Run as Spring Boot application
java -jar build/libs/jakarta-migration-mcp-1.0.0-SNAPSHOT.jar --spring.main.web-application-type=none

# Or run directly
gradle bootRun --args="--spring.main.web-application-type=none"
```

**Note**: `--spring.main.web-application-type=none` is important for MCP stdio mode.

### Step 3: Verify MCP Server is Running

Check the logs for:
- MCP server startup messages
- Tool registration messages
- Any errors

## Testing with Cursor

### Configure Cursor MCP

1. Open Cursor Settings (`Ctrl+,` or `Cmd+,`)
2. Navigate to **Features** → **MCP**
3. Add configuration:

```json
{
  "mcpServers": {
    "jakarta-migration": {
      "command": "java",
      "args": [
        "-jar",
        "E:/Source/JakartaMigrationMCP/build/libs/jakarta-migration-mcp-1.0.0-SNAPSHOT.jar",
        "--spring.main.web-application-type=none",
        "--spring.profiles.active=mcp"
      ]
    }
  }
}
```

4. **Restart Cursor completely**

### Test Tools in Cursor

Once configured, you can use agentic tool calls:

```
Analyze Jakarta readiness for examples/javax-servlet-examples-master/javax-servlet-examples-master
```

```
Detect blockers for examples/demo-spring-javax-validation-example-master/demo-spring-javax-validation-example-master
```

```
Create a migration plan for examples/JavaxEmail-main/JavaxEmail-main
```

## Troubleshooting

### Issue: Spring AI MCP Server dependency not found

**Solution**: 
- Check if Spring AI 0.8.0 supports MCP Server
- If not, upgrade Spring AI version in `build.gradle.kts`:
  ```kotlin
  extra["springAiVersion"] = "1.0.0"  // or latest version
  ```

### Issue: @McpTool annotation not found

**Solution**:
- Verify the dependency is correctly added
- Check import: `import org.springframework.ai.mcp.server.McpTool;`
- Rebuild the project

### Issue: MCP tools not appearing in Cursor

**Solutions**:
1. Verify JAR path in Cursor config is correct
2. Check that `--spring.main.web-application-type=none` is in args
3. Restart Cursor completely
4. Check Cursor logs for MCP errors
5. Verify MCP server is running and tools are registered

### Issue: Compilation errors

**Solution**:
- Ensure Spring AI version supports MCP Server
- Check that all dependencies are resolved
- Try: `gradle clean build`

## Expected Behavior

When the MCP server starts successfully, you should see:

1. **Spring Boot startup logs**
2. **MCP Server initialization logs**
3. **Tool registration logs** (5 tools registered)
4. **Server ready message**

## Next Steps

1. ✅ Build the project
2. ✅ Test MCP server starts correctly
3. ✅ Configure in Cursor
4. ✅ Test tools on example projects
5. ✅ Document any issues found

---

*Last Updated: 2026-01-27*

