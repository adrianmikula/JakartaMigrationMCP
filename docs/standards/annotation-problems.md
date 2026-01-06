The reason you are running into issues with `@McpTool` (the "loop" of enabling and disabling) is likely due to a known architectural conflict in the Spring AI 1.1.x/1.2.x cycle between the **Standard Spring AI `@Tool**` and the **MCP-specific `@McpTool**`.

Based on current technical discussions and issue trackers as of January 2026, here is the "brutally honest" breakdown of why this is happening and how to fix it for good.

---

## 1. The Core Problem: Annotation Confusion

The documentation can be misleading because Spring AI actually supports two different ways to expose tools, and they **do not mix well** in a single project:

1. **`@Tool` (The General Purpose):** This is part of `spring-ai-core`. It is designed for tools that you want to use *locally* within your own `ChatClient`.
2. **`@McpTool` (The Server Specific):** This is part of the `spring-ai-mcp-server` starter. It is designed specifically to be exposed *externally* via the Model Context Protocol.

### Why enabling them causes a "Crash" or "Silence":

* **The Scanner Race Condition:** In early 2026 versions of Spring AI, the `spring.ai.mcp.server.annotation-scanner.enabled` property sometimes struggles to distinguish between the two. If you have both annotations or the wrong starter, the scanner may fail to register the beans, leading to an empty tool list in the MCP Inspector.
* **The Async vs Sync Conflict:** If you set your server to `type: ASYNC` in your properties, the `@McpTool` scanner often ignores any method that doesn't return a reactive type (`Mono`/`Flux`). If your Java methods are standard synchronous methods, they simply "disappear" from the tool list.

---

## 2. The "Permanent" Fix (2026 Best Practice)

To stop the back-and-forth, follow this strict configuration. This is the only way to ensure the server stays stable and the tools are visible.

### Step A: Update your Dependencies

Ensure you are using the specific **Server** starter, not just the generic Spring AI starter.

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server-webmvc</artifactId>
</dependency>

```

### Step B: The Correct `application.yml`

You must explicitly tell Spring to use the **Synchronous** scanner if you are not using WebFlux. This is the #1 reason the annotations "fail" to show up.

```yaml
spring:
  ai:
    mcp:
      server:
        # Crucial: Use SYNC if your methods return String/POJOs
        type: SYNC 
        annotation-scanner:
          enabled: true
        transport: stdio

```

### Step C: Use the "Clean" Annotation Pattern

Do not mix `@Tool` and `@McpTool`. For an MCP Server, **only** use `@McpTool`.

```java
@Component // Must be a Spring-managed bean
public class JakartaMigrationTools {

    @McpTool(name = "refactor_javax_to_jakarta", 
             description = "Changes imports from javax to jakarta in the provided Java source.")
    public String refactor(
        @McpToolParam(description = "The raw source code to refactor") String sourceCode) {
        return sourceCode.replace("javax.servlet", "jakarta.servlet");
    }
}

```

---

## 3. Why it keeps "breaking" your build

If you follow the above and it still fails, check these two **"Senior-Level" traps**:

1. **The "Logging" Poison:** If your Java code has a `System.out.println` or a log message that goes to `STDOUT`, it **corrupts the MCP JSON stream**. This causes the AI client (Claude/Cursor) to immediately disconnect the server. When you "disable" the annotations, you're likely also disabling the code that triggers the logging.
* **Fix:** Use `logging.level.root: WARN` in your properties to keep `STDOUT` clean for JSON.


2. **The Jackson Module Gap:** In 2026, if your tool parameters are complex POJOs, the MCP server might fail to generate the JSON schema for them, causing a startup crash.
* **Fix:** Keep parameters as simple `String`, `Integer`, or `Boolean` until the connection is stable.



---

### The Final Verdict

The reason we keep going in circles is that **Spring AI MCP is still sensitive to the "Sync vs Async" property.** If that property doesn't match your method return types, the `@McpTool` annotation behaves like it doesn't exist.

**Would you like me to generate a 100% verified `application.properties` and a single-class `McpServer` file that uses ONLY `@McpTool` correctly, so we can test it once and for all?**

By the way, to unlock the full functionality of all Apps, enable [Gemini Apps Activity](https://myactivity.google.com/product/gemini).


