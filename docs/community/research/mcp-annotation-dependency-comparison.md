# MCP Annotation Dependency Comparison

## Issue: Compilation Error

The build is failing with:
```
error: package org.springaicommunity.mcp.annotations does not exist
import org.springaicommunity.mcp.annotations.McpTool;  // WRONG - should be 'annotation' (singular)
```

However, the dependency tree shows the dependency is resolved:
```
+--- org.springaicommunity:mcp-annotations:0.8.0 (*)
```

## Dependency Analysis

### Current Configuration

**build.gradle.kts**:
```kotlin
implementation("org.springframework.ai:spring-ai-starter-mcp-server-webmvc:${property("springAiVersion")}")
implementation("org.springframework.ai:spring-ai-mcp-annotations:${property("springAiVersion")}")
implementation("org.springaicommunity:mcp-annotations:0.8.0")
```

**Repositories**:
- Maven Central
- Spring Milestone
- Spring Snapshot
- JitPack (for spring-ai-community mcp-annotations)

### Dependency Tree Verification

The dependency is present in the compile classpath:
```
+--- org.springframework.ai:spring-ai-mcp-annotations:1.1.2
|    +--- org.springaicommunity:mcp-annotations:0.8.0
+--- org.springaicommunity:mcp-annotations:0.8.0 (*)
```

## Possible Causes

### 1. Repository Issue
The `org.springaicommunity:mcp-annotations:0.8.0` might not be available in Maven Central and may need to come from:
- JitPack (already configured)
- A different repository
- A different group ID or artifact ID

### 2. Version Mismatch
The version `0.8.0` might not exist or might be incompatible with Spring AI 1.1.2.

### 3. Compile vs Runtime
The dependency might be marked as `runtimeOnly` somewhere in the transitive dependencies, making it unavailable at compile time.

## Research: Working Examples

### Spring AI Community MCP Annotations

**Repository**: `spring-ai-community/mcp-annotations`

**Expected Configuration**:
- The annotations should come from `org.springaicommunity.mcp.annotation` package (SINGULAR, not plural)
- The dependency should be `org.springaicommunity:mcp-annotations`
- Version should match what Spring AI 1.1.2 expects
- **CRITICAL**: Package name is `annotation` (singular), not `annotations` (plural)

### Alternative: Use Spring AI's Wrapper

Spring AI 1.1.2's `spring-ai-mcp-annotations` module wraps `org.springaicommunity:mcp-annotations`, but the annotations might be re-exported in a different package.

**Check**: Does Spring AI re-export the annotations in `org.springframework.ai.mcp.annotations`?

## Recommended Solutions

### Solution 1: Verify Repository Access
Check if the dependency can be downloaded:
```bash
gradle dependencies --configuration compileClasspath | grep springaicommunity
```

### Solution 2: Try Different Version
Try using the version that Spring AI 1.1.2 expects:
```kotlin
implementation("org.springaicommunity:mcp-annotations:0.7.0") // or other version
```

### Solution 3: Use Spring AI's Re-exported Package
If Spring AI re-exports the annotations, use:
```java
import org.springframework.ai.mcp.annotations.McpTool;
import org.springframework.ai.mcp.annotations.McpToolParam;
```

### Solution 4: Check JitPack Configuration
Ensure JitPack is properly configured and the dependency is available:
```kotlin
repositories {
    maven {
        url = uri("https://jitpack.io")
        content {
            includeGroup("org.springaicommunity")
        }
    }
}
```

## Next Steps

1. Check if `org.springaicommunity:mcp-annotations:0.8.0` exists in Maven Central or JitPack
2. Verify the correct version for Spring AI 1.1.2
3. Check if Spring AI re-exports the annotations in a different package
4. Try downloading the dependency manually to verify it exists

