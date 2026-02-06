# Java MCP Server Building Blocks

This document catalogs Java libraries and frameworks that provide building blocks for writing MCP (Model Context Protocol) servers, helping you avoid reinventing the wheel.

## Table of Contents

1. [Official MCP Java SDK](#official-mcp-java-sdk)
2. [Spring AI MCP Server](#spring-ai-mcp-server)
3. [Quarkus MCP Server SDK](#quarkus-mcp-server-sdk)
4. [Tinystruct MCP](#tinystruct-mcp)
5. [Comparison & Recommendations](#comparison--recommendations)
6. [Quick Start Examples](#quick-start-examples)

---

## Official MCP Java SDK ⭐⭐⭐

**Repository**: https://github.com/modelcontextprotocol/java-sdk

**Purpose**: Official Java SDK for building MCP servers and clients

**Key Features**:
- **JSON Serialization**: Built-in support for MCP protocol JSON-RPC messages
- **Asynchronous Processing**: Support for both sync and async processing models
- **Observability**: Integration with observability frameworks
- **Protocol Handling**: Complete MCP protocol implementation
- **Tool Specification**: Easy tool definition and registration
- **Resource Management**: Built-in resource handling
- **Prompt Templates**: Support for prompt templates

**Dependencies** (Maven):

```xml
<dependency>
    <groupId>com.modelcontextprotocol</groupId>
    <artifactId>mcp-java-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Dependencies** (Gradle):

```kotlin
implementation("com.modelcontextprotocol:mcp-java-sdk:1.0.0")
```

**When to Use**:
- Building standalone MCP servers (not Spring/Quarkus)
- Need full control over protocol implementation
- Want official, well-maintained SDK
- Building MCP clients as well as servers

**Pros**:
- ✅ Official SDK from MCP organization
- ✅ Complete protocol implementation
- ✅ Framework-agnostic
- ✅ Well-documented
- ✅ Active development

**Cons**:
- ❌ More boilerplate than framework-integrated solutions
- ❌ Need to handle JSON-RPC manually

**Example Usage**:

```java
import com.modelcontextprotocol.sdk.*;
import com.modelcontextprotocol.sdk.server.*;

public class JakartaMigrationMcpServer {
    public static void main(String[] args) {
        McpServer server = McpServer.builder()
            .name("jakarta-migration")
            .version("1.0.0")
            .tool("analyze_jakarta_readiness", analyzeJakartaReadiness())
            .tool("refactor_namespace", refactorNamespace())
            .build();
        
        server.start();
    }
    
    private static Tool analyzeJakartaReadiness() {
        return Tool.builder()
            .name("analyze_jakarta_readiness")
            .description("Scans pom.xml and build.gradle for Jakarta blockers")
            .inputSchema(/* JSON schema */)
            .handler((params) -> {
                // Your implementation
            })
            .build();
    }
}
```

**Resources**:
- GitHub: https://github.com/modelcontextprotocol/java-sdk
- Documentation: Check GitHub README and examples

---

## Spring AI MCP Server ⭐⭐⭐

**Repository**: https://github.com/spring-projects/spring-ai

**Purpose**: Spring Boot integration for MCP servers

**Key Features**:
- **Annotation-Based**: Use `@Tool` annotation to expose methods as MCP tools
- **Auto-Configuration**: Zero-config setup with Spring Boot
- **Spring Integration**: Leverage Spring's dependency injection and ecosystem
- **Dual Transport**: Supports both stdio and HTTP-based SSE endpoints
- **Type Conversion**: Automatic parameter conversion to MCP format
- **Spring Boot Actuator**: Built-in health checks and metrics

**Dependencies** (Maven):

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Dependencies** (Gradle):

```kotlin
implementation("org.springframework.ai:spring-ai-starter-mcp-server:1.0.0")
```

**When to Use**:
- ✅ **Already using Spring Boot** (like this project!)
- ✅ Want minimal boilerplate
- ✅ Need Spring ecosystem integration (JPA, WebFlux, etc.)
- ✅ Building Jakarta migration tools that need Spring features

**Pros**:
- ✅ Minimal code - just add `@Tool` annotations
- ✅ Leverages existing Spring Boot knowledge
- ✅ Automatic configuration
- ✅ Integrates with Spring AI ecosystem
- ✅ Production-ready with Actuator support

**Cons**:
- ❌ Requires Spring Boot (adds framework overhead if not needed)
- ❌ Less control over protocol details

**Example Usage**:

```java
import org.springframework.ai.mcp.server.Tool;
import org.springframework.stereotype.Component;

@Component
public class JakartaMigrationTools {
    
    @Tool(
        name = "analyze_jakarta_readiness",
        description = "Scans pom.xml and build.gradle for Jakarta blockers"
    )
    public JakartaReadinessReport analyzeJakartaReadiness(
        @ToolParam("repoPath") String repoPath
    ) {
        // Your implementation using Spring services
        return new JakartaReadinessReport(/* ... */);
    }
    
    @Tool(
        name = "refactor_namespace",
        description = "Uses OpenRewrite to flip javax.* to jakarta.* imports"
    )
    public RefactorResult refactorNamespace(
        @ToolParam("filePath") String filePath
    ) {
        // Your implementation
        return new RefactorResult(/* ... */);
    }
}
```

**Configuration** (`application.yml`):

```yaml
spring:
  ai:
    mcp:
      server:
        name: jakarta-migration-mcp
        version: 1.0.0
        transport: stdio  # or 'sse' for HTTP
        # SSE configuration (if using HTTP)
        sse:
          port: 8080
          path: /mcp/sse
```

**Resources**:
- Spring AI Docs: https://docs.spring.io/spring-ai/reference/
- Spring Blog: https://spring.io/blog/2025/05/20/spring-ai-1-0-GA-released
- Examples: https://github.com/spring-projects/spring-ai/tree/main/spring-ai-samples/mcp-server

---

## Quarkus MCP Server SDK ⭐⭐

**Repository**: https://github.com/modelcontextprotocol/servers (Quarkus examples)

**Purpose**: Quarkus framework integration for MCP servers

**Key Features**:
- **Fast Startup**: Quarkus's native compilation and fast startup
- **Low Memory**: Optimized for cloud-native, resource-constrained environments
- **CDI Integration**: Uses Quarkus's dependency injection
- **Native Image**: Can compile to native executables
- **Reactive**: Built on reactive principles

**Dependencies** (Maven):

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-mcp-server</artifactId>
    <version>1.0.0</version>
</dependency>
```

**When to Use**:
- Building microservices or serverless functions
- Need ultra-fast startup times
- Want native compilation
- Prefer Quarkus over Spring Boot

**Pros**:
- ✅ Fast startup (milliseconds)
- ✅ Low memory footprint
- ✅ Native compilation support
- ✅ Cloud-native optimized

**Cons**:
- ❌ Smaller ecosystem than Spring
- ❌ Less documentation/examples
- ❌ Requires Quarkus framework

**Example Usage**:

```java
import io.quarkus.mcp.server.Tool;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class JakartaMigrationTools {
    
    @Tool(
        name = "analyze_jakarta_readiness",
        description = "Scans for Jakarta blockers"
    )
    public JakartaReadinessReport analyzeJakartaReadiness(String repoPath) {
        // Implementation
    }
}
```

**Resources**:
- Quarkus MCP Examples: https://github.com/modelcontextprotocol/servers
- Quarkus Docs: https://quarkus.io

---

## Tinystruct MCP ⭐

**Repository**: https://github.com/punkpeye/awesome-mcp-devtools (mentioned)

**Purpose**: Lightweight framework for extensible MCP servers

**Key Features**:
- **Minimal Dependencies**: Lightweight framework
- **Extensible**: Easy to extend and customize
- **Simple API**: Straightforward programming model

**When to Use**:
- Need minimal dependencies
- Want lightweight solution
- Building simple MCP servers

**Pros**:
- ✅ Very lightweight
- ✅ Minimal dependencies
- ✅ Simple API

**Cons**:
- ❌ Less documentation
- ❌ Smaller community
- ❌ May need more manual work

**Note**: This appears to be less documented. Consider the official SDK or Spring AI for production use.

---

## Comparison & Recommendations

### For This Jakarta Migration Project

**Recommended: Spring AI MCP Server** ⭐⭐⭐

**Why**:
1. ✅ **Already using Spring Boot** - This project is Spring Boot 3.2+
2. ✅ **Minimal Integration** - Just add dependency and `@Tool` annotations
3. ✅ **OpenRewrite Integration** - Can easily use OpenRewrite (already added) in tool implementations
4. ✅ **Production Ready** - Spring Boot ecosystem provides monitoring, health checks, etc.
5. ✅ **Familiar** - Team already knows Spring Boot

**Alternative: Official MCP Java SDK**

Use if:
- You want framework-agnostic solution
- Need more control over protocol
- Building standalone server (not Spring Boot app)

### Comparison Table

| Feature | Official SDK | Spring AI | Quarkus | Tinystruct |
|---------|-------------|-----------|---------|------------|
| **Framework Required** | None | Spring Boot | Quarkus | Tinystruct |
| **Boilerplate** | Medium | Low | Low | Low |
| **Documentation** | Good | Excellent | Good | Limited |
| **Ecosystem** | Medium | Large | Medium | Small |
| **Startup Time** | Fast | Medium | Very Fast | Fast |
| **Memory** | Medium | Medium | Low | Low |
| **Production Ready** | Yes | Yes | Yes | Unknown |
| **Best For** | Standalone | Spring Apps | Microservices | Simple servers |

---

## Quick Start Examples

### Using Spring AI MCP Server (Recommended for This Project)

1. **Add Dependency**:

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server</artifactId>
    <version>1.0.0</version>
</dependency>
```

```kotlin
// build.gradle.kts
implementation("org.springframework.ai:spring-ai-starter-mcp-server:1.0.0")
```

2. **Create Tool Component**:

```java
package com.jakartamigration.mcp;

import org.springframework.ai.mcp.server.Tool;
import org.springframework.stereotype.Component;

@Component
public class JakartaMigrationTools {
    
    @Tool(
        name = "analyze_jakarta_readiness",
        description = "Analyzes a Java project for Jakarta migration readiness"
    )
    public JakartaReadinessReport analyzeReadiness(
        @ToolParam("projectPath") String projectPath
    ) {
        // Use OpenRewrite to analyze
        // Return report
    }
    
    @Tool(
        name = "migrate_to_jakarta",
        description = "Migrates javax.* to jakarta.* using OpenRewrite"
    )
    public MigrationResult migrateToJakarta(
        @ToolParam("projectPath") String projectPath
    ) {
        // Use OpenRewrite recipes
        // Apply migrations
        // Return result
    }
}
```

3. **Configure** (`application.yml`):

```yaml
spring:
  ai:
    mcp:
      server:
        name: jakarta-migration-mcp
        version: 1.0.0
        transport: stdio
```

4. **Run**:

```bash
# The MCP server will start automatically with Spring Boot
./gradlew bootRun

# Or build JAR
./gradlew bootJar
java -jar build/libs/jakarta-migration-mcp.jar
```

### Using Official MCP Java SDK

1. **Add Dependency**:

```xml
<dependency>
    <groupId>com.modelcontextprotocol</groupId>
    <artifactId>mcp-java-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

2. **Create Server**:

```java
import com.modelcontextprotocol.sdk.*;

public class JakartaMigrationServer {
    public static void main(String[] args) {
        McpServer server = McpServer.builder()
            .name("jakarta-migration")
            .version("1.0.0")
            .tool(createAnalyzeTool())
            .tool(createMigrateTool())
            .build();
        
        server.start();
    }
    
    private static Tool createAnalyzeTool() {
        // Tool definition
    }
}
```

---

## Integration with OpenRewrite

All these libraries can integrate with OpenRewrite (already added to this project):

```java
// Example: Using OpenRewrite in a Spring AI MCP Tool
@Component
public class JakartaMigrationTools {
    
    private final JavaParser javaParser;
    
    public JakartaMigrationTools() {
        this.javaParser = JavaParser.fromJavaVersion()
            .classpath("classpath")
            .build();
    }
    
    @Tool(name = "refactor_javax_to_jakarta")
    public RefactorResult refactor(String filePath) {
        // Parse Java file
        List<J.CompilationUnit> cu = javaParser.parse(filePath);
        
        // Apply OpenRewrite recipe
        AddJakartaNamespace recipe = new AddJakartaNamespace();
        List<J.CompilationUnit> refactored = recipe.visit(cu);
        
        // Write back
        // ...
        
        return new RefactorResult(/* ... */);
    }
}
```

---

## Next Steps

1. **Choose Library**: For this project, use **Spring AI MCP Server**
2. **Add Dependency**: Update `pom.xml` and `build.gradle.kts`
3. **Create Tools**: Implement Jakarta migration tools with `@Tool` annotations
4. **Integrate OpenRewrite**: Use OpenRewrite in tool implementations
5. **Test**: Build and test the MCP server

---

## Resources

- **Official MCP Java SDK**: https://github.com/modelcontextprotocol/java-sdk
- **Spring AI MCP**: https://docs.spring.io/spring-ai/reference/
- **Spring AI Examples**: https://github.com/spring-projects/spring-ai/tree/main/spring-ai-samples/mcp-server
- **Quarkus MCP**: https://github.com/modelcontextprotocol/servers
- **MCP Protocol Spec**: https://modelcontextprotocol.io

---

*Last Updated: January 2026*

