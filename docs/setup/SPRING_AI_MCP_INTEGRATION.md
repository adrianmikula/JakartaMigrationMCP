# Spring AI MCP Server Integration

## Current Status

❌ **NOT CONFIGURED** - The project is not currently exposing MCP tools via Spring AI MCP Server.

## Issues Found

1. **Dependency Missing**: Spring AI MCP Server dependency is commented out in `build.gradle.kts`
2. **No @Tool Annotations**: `JakartaMigrationTools` methods are not annotated with `@Tool`
3. **No MCP Configuration**: Missing MCP server configuration in `application.yml`

## Required Changes

### 1. Add Spring AI MCP Server Dependency

**Current** (build.gradle.kts line 64):
```kotlin
// TODO: Re-enable when dependency is available
// implementation("org.springframework.ai:spring-ai-starter-mcp-server:${property("springAiVersion")}")
```

**Action Needed**: 
- Check if `spring-ai-starter-mcp-server` is available for Spring AI 0.8.0
- If not available, consider upgrading Spring AI version or using alternative MCP SDK
- Uncomment and add the dependency

### 2. Add @Tool Annotations

**Current**: `JakartaMigrationTools` has methods but no `@Tool` annotations

**Required**: Add `@Tool` annotations to each method:

```java
import org.springframework.ai.mcp.server.Tool;

@Component
public class JakartaMigrationTools {
    
    @Tool(
        name = "analyzeJakartaReadiness",
        description = "Analyzes a Java project for Jakarta migration readiness"
    )
    public String analyzeJakartaReadiness(String projectPath) {
        // existing implementation
    }
    
    @Tool(
        name = "detectBlockers",
        description = "Detects blockers that prevent Jakarta migration"
    )
    public String detectBlockers(String projectPath) {
        // existing implementation
    }
    
    // ... etc for all methods
}
```

### 3. Configure MCP Server

**Add to `application.yml`**:

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

## Alternative: Official MCP Java SDK

If Spring AI MCP Server is not available for Spring AI 0.8.0, consider using the official MCP Java SDK:

```kotlin
// build.gradle.kts
implementation("com.modelcontextprotocol:mcp-java-sdk:1.0.0")
```

This would require implementing the MCP protocol manually but provides more control.

## Next Steps

1. ✅ Check Spring AI MCP Server availability for version 0.8.0
2. ✅ If available, uncomment dependency and add @Tool annotations
3. ✅ If not available, either:
   - Upgrade Spring AI to a version with MCP Server support
   - Use official MCP Java SDK
   - Implement custom MCP protocol handler
4. ✅ Add MCP server configuration to application.yml
5. ✅ Test MCP tools are exposed correctly

---

*Last Updated: 2026-01-27*

