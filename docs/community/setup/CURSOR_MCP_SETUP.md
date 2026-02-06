# Jakarta Migration MCP - Cursor Setup Guide

## Quick Setup for Local Development

This guide shows how to configure and test the Jakarta Migration MCP server in Cursor for local development and testing.

## Setup Status ✅

The Jakarta Migration MCP Server has been configured with Spring AI MCP support:

1. ✅ **Dependency Added**: `spring-ai-starter-mcp-server` in `build.gradle.kts`
2. ✅ **@McpTool Annotations**: All 5 tools are annotated
3. ✅ **Configuration Added**: MCP server config in `application.yml`

For detailed status, see [MCP_EXPOSURE_STATUS.md](MCP_EXPOSURE_STATUS.md).

## Prerequisites

1. **Java 21+** installed and in PATH
   - Verify: `java -version`
2. **Node.js 18+** installed
   - Verify: `node --version`
3. **Project built** (JAR file available)
   - Build: `./gradlew bootJar` or `gradle bootJar`
   - JAR location: `build/libs/jakarta-migration-mcp-1.0.0-SNAPSHOT.jar`

## Step 1: Build the Project

```bash
# From project root
./gradlew bootJar

# Or on Windows
gradlew.bat bootJar
```

The JAR will be created at: `build/libs/jakarta-migration-mcp-1.0.0-SNAPSHOT.jar`

## Step 2: Configure Cursor MCP Settings

1. **Open Cursor Settings**
   - Press `Ctrl+,` (Windows/Linux) or `Cmd+,` (Mac)
   - Or: File → Preferences → Settings

2. **Navigate to MCP Settings**
   - Search for "MCP" in settings
   - Or go to: Features → MCP

3. **Add Jakarta Migration MCP Server**

   For **local development** (using local JAR):
   
   ```json
   {
     "mcpServers": {
       "jakarta-migration": {
         "command": "java",
         "args": [
           "-jar",
           "E:/Source/JakartaMigrationMCP/build/libs/jakarta-migration-mcp-1.0.0-SNAPSHOT.jar",
           "--spring.main.web-application-type=none"
         ],
         "env": {
           "JAVA_HOME": "C:/path/to/java"
         }
       }
     }
   }
   ```

   **Important**: Replace `E:/Source/JakartaMigrationMCP` with your actual project path.

   **Alternative**: Use absolute path with environment variable:
   
   ```json
   {
     "mcpServers": {
       "jakarta-migration": {
         "command": "java",
         "args": [
           "-jar",
           "${workspaceFolder}/build/libs/jakarta-migration-mcp-1.0.0-SNAPSHOT.jar",
           "--spring.main.web-application-type=none"
         ]
       }
     }
   }
   ```

4. **Toggle the server ON**

5. **Restart Cursor**
   - Close Cursor completely
   - Reopen Cursor
   - The MCP server should start automatically

## Step 3: Verify MCP Server is Running

### Check Logs

After starting the server, check the logs for:
- MCP server startup messages
- Tool registration messages (should show 5 tools registered)
- Any errors

### Expected Behavior

When the MCP server starts successfully, you should see:
1. **Spring Boot startup logs**
2. **MCP Server initialization logs**
3. **Tool registration logs** (5 tools registered)
4. **Server ready message**

### Test in Cursor

After restarting Cursor, you can verify the MCP server is working by:

1. **Check MCP Status** (if available in Cursor UI)
2. **Try using a tool**: Ask Cursor to analyze a project:
   ```
   Analyze the Jakarta readiness of the project at examples/javax-servlet-examples-master
   ```

## Available MCP Tools

Once configured, the following tools are available:

1. **analyzeJakartaReadiness** - Analyze project for Jakarta migration readiness
2. **detectBlockers** - Detect blockers preventing Jakarta migration
3. **recommendVersions** - Recommend Jakarta-compatible dependency versions
4. **createMigrationPlan** - Create a comprehensive migration plan
5. **verifyRuntime** - Verify runtime execution of migrated applications

## Troubleshooting

### MCP Server Not Starting

- **Check Java**: Ensure Java 21+ is installed and in PATH
- **Check JAR Path**: Verify the JAR file exists at the specified path
- **Check Logs**: Look for MCP server errors in Cursor's output/logs
- **Try Absolute Path**: Use full absolute path instead of relative

### Tools Not Available

- **Restart Cursor**: MCP servers only load on startup
- **Check Configuration**: Verify JSON syntax is correct
- **Check Server Status**: Ensure the server is toggled ON in settings
- **Verify JAR Path**: Check that `--spring.main.web-application-type=none` is in args
- **Check Cursor Logs**: Look for MCP errors in Cursor's logs

### Spring AI MCP Server Dependency Issues

**Issue**: Spring AI MCP Server dependency not found

**Solution**: 
- Check if Spring AI version supports MCP Server (should be 1.1.2+)
- Verify dependency in `build.gradle.kts`: `spring-ai-starter-mcp-server`
- Rebuild: `gradle clean build`

**Issue**: @McpTool annotation not found

**Solution**:
- Verify the dependency is correctly added
- Check import: `import org.springframework.ai.mcp.server.McpTool;`
- Rebuild the project

### Compilation Errors

**Solution**:
- Ensure Spring AI version supports MCP Server
- Check that all dependencies are resolved
- Try: `gradle clean build`

### Path Issues on Windows

- Use forward slashes or escaped backslashes: `E:/Source/...` or `E:\\Source\\...`
- Or use environment variables: `${workspaceFolder}`

## Testing on Example Projects

Once the MCP is configured, you can test it on the example projects:

1. **Analyze Readiness**:
   ```
   Analyze Jakarta readiness for examples/javax-servlet-examples-master
   ```

2. **Detect Blockers**:
   ```
   Detect blockers for Jakarta migration in examples/demo-spring-javax-validation-example-master
   ```

3. **Create Migration Plan**:
   ```
   Create a migration plan for examples/javax-validations-and-api-versioning-spring-master
   ```

4. **Recommend Versions**:
   ```
   Recommend Jakarta-compatible versions for examples/JavaxEmail-main
   ```

---

*Last Updated: 2026-01-27*

