# MCP Annotation Package Name Fix

## Root Cause Identified

After inspecting the actual JAR file from the Gradle cache, we discovered a **package name mismatch** that explains why the `@McpTool` annotations keep getting disabled.

### The Problem

**Code was importing from:**
```java
import org.springaicommunity.mcp.annotations.McpTool;  // WRONG - plural "annotations"
import org.springaicommunity.mcp.annotations.McpToolParam;
```

**But the JAR actually contains:**
```
org/springaicommunity/mcp/annotation/McpTool.class  // CORRECT - singular "annotation"
org/springaicommunity/mcp/annotation/McpToolParam.class
```

### Verification

Inspected the JAR file from Gradle cache:
```
C:\Users\adria\.gradle\caches\modules-2\files-2.1\org.springaicommunity\mcp-annotations\0.8.0\29b2866cb713a90ea03b919a1e83fdfcec3ca7ac\mcp-annotations-0.8.0.jar
```

**Found entries:**
- `org/springaicommunity/mcp/annotation/McpTool.class` ✅
- `org/springaicommunity/mcp/annotation/McpToolParam.class` ✅
- `org/springaicommunity/mcp/annotation/McpResource.class`
- `org/springaicommunity/mcp/annotation/McpPrompt.class`
- And other annotation classes...

### Why This Caused the Enable/Disable Cycle

1. **Compilation Issues**: When the package name was wrong, the code wouldn't compile, leading to disabling annotations
2. **Runtime ClassNotFoundException**: Even if it compiled (perhaps with a different version), at runtime the classes wouldn't be found
3. **Annotation Scanner Failure**: Spring's annotation scanner couldn't find the annotation classes, so tools weren't registered
4. **Silent Failures**: The application might start, but tools wouldn't appear, leading to the assumption that annotations "don't work"

### The Fix

**Updated imports in:**
- `src/main/java/adrianmikula/jakartamigration/mcp/JakartaMigrationTools.java`
- `src/main/java/adrianmikula/jakartamigration/mcp/SentinelTools.java`

**Changed from:**
```java
import org.springaicommunity.mcp.annotations.McpTool;  // WRONG
import org.springaicommunity.mcp.annotations.McpToolParam;  // WRONG
```

**Changed to:**
```java
import org.springaicommunity.mcp.annotation.McpTool;  // CORRECT - singular
import org.springaicommunity.mcp.annotation.McpToolParam;  // CORRECT - singular
```

### Correct Package Structure

The `org.springaicommunity:mcp-annotations:0.8.0` JAR contains:
- Package: `org.springaicommunity.mcp.annotation` (singular)
- Classes:
  - `McpTool`
  - `McpToolParam`
  - `McpResource`
  - `McpPrompt`
  - `McpProgress`
  - And others...

### Next Steps

1. ✅ Fixed package imports in source code
2. ⏳ Verify build compiles successfully
3. ⏳ Test that MCP server starts without errors
4. ⏳ Verify annotation scanner discovers tools
5. ⏳ Test tool execution via MCP client

### Lessons Learned

1. **Always verify package names by inspecting the actual JAR** - don't rely on documentation alone
2. **The artifact name (`mcp-annotations`) doesn't match the package name (`mcp.annotation`)** - this is a common source of confusion
3. **Gradle dependency resolution doesn't catch package name mismatches** - the JAR downloads successfully even if you use the wrong package name in code

### References

- JAR Location: `~/.gradle/caches/modules-2/files-2.1/org.springaicommunity/mcp-annotations/0.8.0/`
- Source Repository: https://github.com/spring-ai-community/mcp-annotations
- Spring AI Documentation: https://docs.spring.io/spring-ai/reference/api/mcp/mcp-annotations-overview.html

