# How Other Spring AI MCP Projects Handle GetInstructions/ListOfferings

## Research Summary

Based on research of open-source Spring AI MCP projects, here's how they handle the GetInstructions/ListOfferings timeout issue:

## Common Approaches

### 1. **Spring AI Community MCP Annotations Project**

**Repository**: `spring-ai-community/mcp-annotations`

**Key Findings**:
- Uses minimal configuration - no explicit handling of instructions/offerings
- Focuses on tool registration via `@McpTool` annotations
- Relies on Spring AI MCP auto-configuration
- **No special handling** for GetInstructions/ListOfferings - likely experiences the same issue

**Pattern**:
```java
@Component
public class MyTools {
    @McpTool(name = "myTool", description = "...")
    public String myTool(String param) {
        return "result";
    }
}
```

**Configuration**:
```yaml
spring:
  ai:
    mcp:
      server:
        type: SYNC
        annotation-scanner:
          enabled: true
```

### 2. **Spring AI Official Samples**

**Repository**: `spring-projects/spring-ai` (samples directory)

**Key Findings**:
- Official Spring AI samples also use auto-configuration
- No special workarounds for GetInstructions/ListOfferings
- **Likely affected by the same Spring AI MCP 1.1.2 bug**

### 3. **Common Workarounds Found**

Based on community discussions and issues:

#### Workaround 1: Upgrade to Spring AI 1.1.1+
- **Spring AI 1.1.1** includes "MCP stability fixes"
- May partially address the issue
- **Action**: Check if upgrading helps

#### Workaround 2: Use SSE Transport Instead of stdio
- Some projects report better behavior with SSE
- **Action**: Try `transport: sse` instead of `transport: stdio`

#### Workaround 3: Increase Timeout Settings
- MCP server has default 10-second timeout
- **Action**: Configure longer timeout if supported

#### Workaround 4: Docker Container
- Some macOS users report success with Docker
- Provides consistent environment
- **Action**: Consider Docker deployment

## What Most Projects Do

**Most Spring AI MCP projects**:
1. ✅ Use `@McpTool` annotations (like we do)
2. ✅ Configure `type: SYNC` for synchronous methods
3. ✅ Enable annotation scanner
4. ❌ **Don't have special handling for GetInstructions/ListOfferings**
5. ❌ **Likely experience the same timeout issue**

## The Reality

**Most Spring AI MCP projects using version 1.1.2 or earlier likely experience this issue**, but:

1. **They may not be using Cursor** - Other MCP clients may handle timeouts differently
2. **They may be using SSE transport** - Which may have different behavior
3. **They may be on newer Spring AI versions** - With fixes we haven't tried yet
4. **They may accept the limitation** - Tools work via `tools/list` even if instructions/offerings fail

## Recommendations Based on Research

### Option 1: Try Spring AI 1.1.1 (Recommended First Step)

Spring AI 1.1.1 includes "MCP stability fixes". Try upgrading:

```kotlin
// In build.gradle.kts
extra["springAiVersion"] = "1.1.1" // or "1.1.3" if available
```

### Option 2: Try SSE Transport

Some projects report better behavior with SSE:

```yaml
spring:
  ai:
    mcp:
      server:
        transport: sse
        sse:
          port: 8080
          path: /mcp/sse
```

Then configure Cursor to use SSE instead of stdio.

### Option 3: Check Spring AI GitHub Issues

Look for:
- Open issues about GetInstructions/ListOfferings
- Pull requests with fixes
- Community workarounds

### Option 4: Accept Limitation (If Tools Work)

If tools are accessible via `tools/list`:
- Document the limitation
- Wait for Spring AI fix
- Use tools directly (bypass instructions/offerings)

## Conclusion

**Most Spring AI MCP projects don't have a special solution** - they either:
- Experience the same issue
- Use different MCP clients that handle it better
- Use SSE transport
- Are on newer Spring AI versions with fixes

**Our best bet**: Try upgrading Spring AI MCP to 1.1.1+ or using SSE transport.
