# Testing MCP Tools in Cursor

## Prerequisites

✅ **Project Built**: JAR file exists at `build/libs/jakarta-migration-mcp-1.0.0-SNAPSHOT.jar`  
✅ **Configuration**: `CURSOR_MCP_CONFIG.json` is configured  
✅ **Spring AI**: Version 1.1.2 with MCP server support  

## Step 1: Verify JAR File

```powershell
# Check JAR exists
Test-Path "build\libs\jakarta-migration-mcp-1.0.0-SNAPSHOT.jar"

# Check size (should be ~246 MB)
(Get-Item "build\libs\jakarta-migration-mcp-1.0.0-SNAPSHOT.jar").Length / 1MB
```

## Step 2: Verify Cursor Configuration

The `CURSOR_MCP_CONFIG.json` should contain:

```json
{
  "mcpServers": {
    "jakarta-migration": {
      "command": "java",
      "args": [
        "-jar",
        "E:/Source/JakartaMigrationMCP/build/libs/jakarta-migration-mcp-1.0.0-SNAPSHOT.jar",
        "--spring.main.web-application-type=none",
        "--spring.profiles.active=mcp-stdio",
        "--spring.ai.mcp.server.transport=stdio"
      ],
      "env": {}
    }
  }
}
```

**Important**: 
- Use absolute path to JAR file
- Include `--spring.main.web-application-type=none` for stdio mode
- Set profile to `mcp-stdio`

## Step 3: Test MCP Server Startup (Manual)

Before testing in Cursor, verify the server can start:

```powershell
# Test server startup
java -jar build\libs\jakarta-migration-mcp-1.0.0-SNAPSHOT.jar `
  --spring.main.web-application-type=none `
  --spring.profiles.active=mcp-stdio `
  --spring.ai.mcp.server.transport=stdio
```

**Expected Output**:
- Spring Boot startup logs
- MCP server initialization
- No errors about missing tools

**Note**: The server will wait for stdio input. Press `Ctrl+C` to stop.

## Step 4: Configure Cursor

1. **Locate Cursor MCP Configuration**:
   - Windows: `%APPDATA%\Cursor\User\globalStorage\saoudrizwan.claude-dev\settings\cline_mcp_settings.json`
   - Or check Cursor Settings → Features → MCP

2. **Add Configuration**:
   - Copy contents from `CURSOR_MCP_CONFIG.json`
   - Ensure JAR path is absolute and correct
   - Save the configuration

3. **Restart Cursor Completely**:
   - Close all Cursor windows
   - Restart Cursor
   - This is critical for MCP server initialization

## Step 5: Verify Tools in Cursor

### Check MCP Server Status

1. Open Cursor
2. Check for MCP server connection:
   - Look for MCP status indicator (if available)
   - Check Cursor logs for MCP connection messages
   - Verify no errors in Cursor's developer console

### Test Tool Discovery

Try asking Cursor:

```
@jakarta-migration What tools are available?
```

Or:

```
Can you list the available Jakarta migration tools?
```

### Test Individual Tools

1. **Analyze Jakarta Readiness**:
   ```
   @jakarta-migration analyzeJakartaReadiness examples/demo-spring-javax-validation-example-master/demo-spring-javax-validation-example-master
   ```

2. **Detect Blockers**:
   ```
   @jakarta-migration detectBlockers examples/javax-servlet-examples-master/javax-servlet-examples-master
   ```

3. **Recommend Versions**:
   ```
   @jakarta-migration recommendVersions examples/JavaxEmail-main/JavaxEmail-main
   ```

4. **Create Migration Plan**:
   ```
   @jakarta-migration createMigrationPlan examples/tomcat10-jakartaee9-main/tomcat10-jakartaee9-main
   ```

5. **Verify Runtime**:
   ```
   @jakarta-migration verifyRuntime path/to/migrated-app.jar
   ```

## Troubleshooting

### Issue: MCP Server Not Connecting

**Symptoms**:
- No tools available in Cursor
- Error messages in Cursor logs

**Solutions**:
1. Verify JAR path is absolute and correct
2. Check Java is in PATH: `java -version`
3. Verify JAR file exists and is accessible
4. Check Cursor logs for detailed error messages
5. Try running the JAR manually to verify it starts

### Issue: Tools Not Visible

**Symptoms**:
- MCP server connects but no tools listed

**Possible Causes**:
1. Tools not registered (annotation issue)
2. Spring context not loading tools
3. MCP server not discovering tools

**Solutions**:
1. Check application logs for tool registration messages
2. Verify `spring.ai.mcp.server.annotation-scanner.enabled=true` in `application.yml`
3. Check if tools need manual registration via `ToolCallbackProvider`
4. Review Spring AI MCP documentation for tool registration

### Issue: Tool Execution Fails

**Symptoms**:
- Tools are visible but fail when called

**Solutions**:
1. Check tool method signatures match expected format
2. Verify all dependencies are available
3. Check application logs for detailed error messages
4. Test tools directly via unit tests

### Issue: Java Not Found

**Symptoms**:
- Cursor can't start MCP server
- "java: command not found" errors

**Solutions**:
1. Verify Java is installed: `java -version`
2. Add Java to system PATH
3. Use full path to Java in Cursor config:
   ```json
   "command": "C:\\Program Files\\Java\\jdk-21\\bin\\java.exe"
   ```

## Expected Behavior

When working correctly:

1. ✅ Cursor connects to MCP server on startup
2. ✅ Tools are listed and available
3. ✅ Tools can be invoked via natural language
4. ✅ Tools return JSON responses
5. ✅ No errors in Cursor logs

## Verification Checklist

- [ ] JAR file exists and is built
- [ ] Cursor MCP configuration is correct
- [ ] Cursor has been restarted after configuration
- [ ] MCP server connects (check logs)
- [ ] Tools are visible/listed
- [ ] At least one tool executes successfully
- [ ] Tool responses are in expected JSON format

## Next Steps

1. ✅ Test all 5 tools with example projects
2. ✅ Verify tool responses are correct
3. ✅ Test error handling (invalid paths, etc.)
4. ✅ Document any issues found
5. ✅ Create integration tests for tool registration

---

*Last Updated: 2026-01-27*

