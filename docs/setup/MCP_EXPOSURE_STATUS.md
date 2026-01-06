# MCP Tools Exposure Status

## Current Status: ❌ NOT CONFIGURED

The Jakarta Migration MCP tools are **NOT** currently exposed via Spring AI MCP Server.

## Issues Found

### 1. Spring AI MCP Server Dependency Missing

**Location**: `build.gradle.kts` line 64

**Current State**:
```kotlin
// TODO: Re-enable when dependency is available
// implementation("org.springframework.ai:spring-ai-starter-mcp-server:${property("springAiVersion")}")
```

**Issue**: The dependency is commented out with a TODO note.

**Spring AI Version**: 0.8.0 (line 38)

**Action Required**: 
- Check if `spring-ai-starter-mcp-server` is available for Spring AI 0.8.0
- If not available, consider:
  - Upgrading Spring AI to a version with MCP Server support (e.g., 1.0.0+)
  - Using the official MCP Java SDK as an alternative

### 2. Missing @Tool Annotations

**Location**: `src/main/java/adrianmikula/jakartamigration/mcp/JakartaMigrationTools.java`

**Current State**: Methods exist but are not annotated with `@Tool`

**Methods that need @Tool annotations**:
- `analyzeJakartaReadiness(String projectPath)`
- `detectBlockers(String projectPath)`
- `recommendVersions(String projectPath)`
- `createMigrationPlan(String projectPath)`
- `verifyRuntime(String jarPath, Integer timeoutSeconds)`

**Action Required**: Add `@Tool` annotations once Spring AI MCP Server dependency is added.

### 3. Missing MCP Server Configuration

**Location**: `src/main/resources/application.yml`

**Current State**: No Spring AI MCP server configuration exists.

**Action Required**: Add configuration:

```yaml
spring:
  ai:
    mcp:
      server:
        name: jakarta-migration-mcp
        version: 1.0.0-SNAPSHOT
        transport: stdio  # or 'sse' for HTTP-based
        enabled: true
```

## Recommended Solution

### Option 1: Use Spring AI MCP Server (Recommended)

**Steps**:

1. **Check Spring AI MCP Server Availability**:
   - Verify if `spring-ai-starter-mcp-server` exists for Spring AI 0.8.0
   - If not, upgrade Spring AI to latest version (1.0.0+)

2. **Add Dependency**:
   ```kotlin
   implementation("org.springframework.ai:spring-ai-starter-mcp-server:${property("springAiVersion")}")
   ```

3. **Add @Tool Annotations**:
   ```java
   import org.springframework.ai.mcp.server.Tool;
   
   @Tool(
       name = "analyzeJakartaReadiness",
       description = "Analyzes a Java project for Jakarta migration readiness"
   )
   public String analyzeJakartaReadiness(String projectPath) {
       // existing implementation
   }
   ```

4. **Add Configuration** to `application.yml`

### Option 2: Use Official MCP Java SDK

**Steps**:

1. **Add Dependency**:
   ```kotlin
   implementation("com.modelcontextprotocol:mcp-java-sdk:1.0.0")
   ```

2. **Implement MCP Server**:
   - Create a main class that implements MCP protocol
   - Register tools manually
   - Handle JSON-RPC communication

3. **More boilerplate** but provides full control

## Verification Steps

Once configured, verify MCP tools are exposed:

1. **Build the project**: `./gradlew bootJar`
2. **Run the JAR**: `java -jar build/libs/jakarta-migration-mcp-1.0.0-SNAPSHOT.jar`
3. **Check logs** for MCP server startup messages
4. **Test with MCP client** or Cursor to see if tools are available

## Next Actions

1. ✅ Document current status (this file)
2. ⏳ Check Spring AI MCP Server availability for version 0.8.0
3. ⏳ If available, uncomment dependency and add @Tool annotations
4. ⏳ If not available, decide on upgrade path or alternative SDK
5. ⏳ Add MCP server configuration to application.yml
6. ⏳ Test MCP tools are exposed correctly

---

*Last Updated: 2026-01-27*

