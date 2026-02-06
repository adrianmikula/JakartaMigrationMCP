# MCP Annotation Research: Working Examples Analysis

## Executive Summary

After researching open-source Java MCP server implementations, we've identified several critical issues in our current setup that explain why `@McpTool` annotations keep getting disabled. The primary problem is **not** with the annotations themselves, but with **application startup failures** that prevent the MCP server from initializing properly.

## Key Findings from Working Examples

### 1. Spring AI Community MCP Annotations Project

**Repository**: `spring-ai-community/mcp-annotations`

**Key Patterns**:
- Uses `@Component` annotation on tool classes
- All methods return simple types (`String`, `Integer`, `Boolean`) or POJOs
- Uses `@McpTool` and `@McpToolParam` from `org.springaicommunity.mcp.annotation` package (SINGULAR, not plural)
- Configuration uses `type: SYNC` for synchronous methods
- No database dependencies - pure MCP server
- Minimal Spring Boot configuration

**Dependencies**:
```gradle
implementation("org.springframework.ai:spring-ai-starter-mcp-server-webmvc")
implementation("org.springaicommunity:mcp-annotations:0.8.0")
```

### 2. MCP Declarative Java SDK (Non-Spring)

**Repository**: `codeboyzhou/mcp-declarative-java-sdk`

**Key Patterns**:
- Standalone SDK (doesn't use Spring)
- Uses similar annotation patterns but different framework
- Not directly applicable to our Spring-based setup

### 3. Common Patterns Across Working Examples

1. **No Database Dependencies**: All working MCP servers are stateless and don't require databases
2. **Minimal Auto-Configuration**: Only essential Spring Boot features enabled
3. **Clean STDOUT**: All logging goes to STDERR, STDOUT is reserved for JSON-RPC
4. **Simple Return Types**: Methods return `String`, primitives, or simple POJOs
5. **Proper Component Scanning**: Tools are `@Component` beans that Spring can discover

## Root Cause Analysis: Our Implementation Issues

### Issue #1: Database Auto-Configuration Not Properly Excluded

**Problem**: The log file (`mcp-server-output.log`) shows:
```
HikariPool-1 - Exception during pool initialization.
org.postgresql.util.PSQLException: FATAL: database "template" does not exist
```

**Root Cause**: Despite excluding `DataSourceAutoConfiguration` in `@SpringBootApplication`, Spring is still trying to:
- Initialize HikariCP connection pool
- Run Liquibase migrations
- Scan for JPA repositories

**Evidence**:
- Log shows "Bootstrapping Spring Data JPA repositories"
- Log shows "HikariPool-1 - Starting..."
- Application fails to start before MCP server can initialize

**Impact**: When the application fails to start, the MCP server never initializes, so tools never get registered. This creates the illusion that annotations are "broken" when the real issue is startup failure.

### Issue #2: Spring Data Dependencies Still Present

**Problem**: The build file includes WebFlux but we may have transitive dependencies pulling in:
- Spring Data JPA
- Spring Data Redis
- Database drivers

**Evidence from Log**:
```
Multiple Spring Data modules found, entering strict repository configuration mode
Bootstrapping Spring Data JPA repositories in DEFAULT mode.
Finished Spring Data repository scanning in 275 ms. Found 1 JPA repository interface.
```

**Impact**: Spring is scanning for repositories even though we don't need them, and this triggers database initialization.

### Issue #3: Annotation Scanner Bean Post Processor Warnings

**Problem**: Log shows warnings about annotation scanner beans:
```
Bean 'org.springframework.ai.mcp.server.common.autoconfigure.annotations.McpServerAnnotationScannerAutoConfiguration' 
of type [...] is not eligible for getting processed by all BeanPostProcessors
```

**Impact**: This suggests the annotation scanner may not be processing beans correctly, though this is likely a warning rather than a fatal error.

## Comparison: Our Setup vs. Working Examples

| Aspect | Working Examples | Our Implementation | Status |
|--------|-----------------|-------------------|--------|
| Database Dependencies | None | Attempting to connect to PostgreSQL | ❌ **FAILING** |
| Spring Data | Not included | JPA/Redis scanning active | ❌ **ISSUE** |
| Auto-Configuration Exclusions | Minimal, focused | Excluded but not working | ❌ **ISSUE** |
| Return Types | Simple (String, primitives) | Simple (String) | ✅ **OK** |
| Component Annotation | `@Component` | `@Component` | ✅ **OK** |
| Annotation Package | `org.springaicommunity.mcp.annotation` (singular) | `org.springaicommunity.mcp.annotation` (singular) | ✅ **OK** |
| Configuration Type | `SYNC` | `SYNC` | ✅ **OK** |
| Logging to STDOUT | None (all to STDERR) | Configured correctly | ✅ **OK** |

## Recommended Fixes

### Fix #1: Properly Exclude All Database-Related Auto-Configuration

**Action**: Update `@SpringBootApplication` to exclude more auto-configurations:

```java
@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class,
    LiquibaseAutoConfiguration.class,
    // Add these:
    JdbcTemplateAutoConfiguration.class,
    JpaRepositoriesAutoConfiguration.class,
    TransactionManagerAutoConfiguration.class
})
```

### Fix #2: Remove Spring Data Dependencies

**Action**: Check `build.gradle.kts` for any Spring Data dependencies and remove them. The MCP server doesn't need:
- Spring Data JPA
- Spring Data Redis
- Any database drivers

### Fix #3: Verify No Repository Interfaces Exist

**Action**: Search for any `@Repository` interfaces or classes extending Spring Data repositories and remove them, or ensure they're not being scanned.

### Fix #4: Add Explicit Component Scan Configuration

**Action**: Ensure component scanning only includes MCP-related packages:

```java
@ComponentScan(basePackages = {
    "adrianmikula.jakartamigration.mcp",
    "adrianmikula.jakartamigration.config",
    // Only include packages that contain MCP tools and required services
})
```

## Testing Strategy

1. **Startup Test**: Verify application starts without database errors
2. **Annotation Scanner Test**: Verify tools are discovered and registered
3. **MCP Connection Test**: Verify Cursor can connect and list tools
4. **Tool Execution Test**: Verify tools can be called successfully

## Next Steps

1. ✅ Research completed
2. ⏳ Fix database auto-configuration exclusions
3. ⏳ Remove unnecessary Spring Data dependencies
4. ⏳ Test application startup
5. ⏳ Verify annotation scanner discovers tools
6. ⏳ Test MCP server connection in Cursor

## References

- [Spring AI Community MCP Annotations](https://github.com/spring-ai-community/mcp-annotations)
- [Spring AI MCP Documentation](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-annotations-overview.html)
- [MCP Declarative Java SDK](https://github.com/codeboyzhou/mcp-declarative-java-sdk)


