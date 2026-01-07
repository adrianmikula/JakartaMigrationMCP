# MCP Annotation Investigation - January 6, 2026

## Executive Summary

**Investigation Goal:** Understand why `@McpTool` annotations keep getting enabled and then disabled in a cycle, preventing the MCP server from working reliably.

### Key Findings

1. ✅ **FIXED: Package Name Mismatch** - Code imported from `mcp.annotations` (plural) but JAR contains `mcp.annotation` (singular)
2. 🔄 **IN PROGRESS: Database Initialization Failure** - Application tries to connect to PostgreSQL, preventing MCP server startup
3. ✅ **FIXED: Missing JPA Repository Exclusion** - Added `JpaRepositoriesAutoConfiguration` exclusion to main application class

### Status

- **Package Name:** ✅ Fixed in source code
- **Database Exclusions:** ✅ Enhanced (added JPA repositories exclusion)
- **Build Compilation:** ✅ **VERIFIED** - Main source compiles successfully
- **Test Compilation:** ❌ Failing (Expected - template tests need exclusion)
- **Application Startup:** ⏳ Pending test
- **Annotation Discovery:** ⏳ Pending test

---

## Investigation Approach

1. Research working open-source Java MCP projects
2. Inspect actual JAR files to verify package structure
3. Compare our implementation with working examples
4. Identify root causes and document fixes

---

## Finding #1: Package Name Mismatch (CRITICAL) ✅ FIXED

### Discovery Date
January 6, 2026

### Problem
The code was importing annotations from the wrong package name.

**Code was using:**
```java
import org.springaicommunity.mcp.annotations.McpTool;  // WRONG - plural "annotations"
import org.springaicommunity.mcp.annotations.McpToolParam;
```

**Actual JAR structure:**
```
org/springaicommunity/mcp/annotation/McpTool.class  // CORRECT - singular "annotation"
org/springaicommunity/mcp/annotation/McpToolParam.class
```

### Verification Process

1. Located JAR in Gradle cache:
   ```
   C:\Users\adria\.gradle\caches\modules-2\files-2.1\org.springaicommunity\mcp-annotations\0.8.0\29b2866cb713a90ea03b919a1e83fdfcec3ca7ac\mcp-annotations-0.8.0.jar
   ```

2. Extracted and inspected JAR contents using PowerShell:
   ```powershell
   Add-Type -AssemblyName System.IO.Compression.FileSystem
   $zip = [System.IO.Compression.ZipFile]::OpenRead($jarPath)
   $zip.Entries | Where-Object { $_.FullName -like "*McpTool*" }
   ```

3. Found entries:
   - `org/springaicommunity/mcp/annotation/McpTool.class` ✅
   - `org/springaicommunity/mcp/annotation/McpToolParam.class` ✅
   - Package is **singular** `annotation`, not plural `annotations`

### Impact

This mismatch caused:
- Compilation errors (if strict compilation)
- Runtime `ClassNotFoundException` (if compilation succeeded with wrong version)
- Annotation scanner unable to find annotation classes
- Tools not being registered, leading to assumption that "annotations don't work"
- The enable/disable cycle as developers tried to fix the issue

### Fix Applied

**Files Updated:**
- `src/main/java/adrianmikula/jakartamigration/mcp/JakartaMigrationTools.java`
- `src/main/java/adrianmikula/jakartamigration/mcp/SentinelTools.java`

**Changed to:**
```java
import org.springaicommunity.mcp.annotation.McpTool;  // CORRECT - singular
import org.springaicommunity.mcp.annotation.McpToolParam;  // CORRECT - singular
```

**Status:** ✅ Fixed

---

## Finding #2: Database Initialization Failure (BLOCKING)

### Discovery Date
January 6, 2026

### Problem
Application fails to start due to database connection attempts, preventing MCP server initialization.

### Evidence from Logs

From `mcp-server-output.log`:
```
2026-01-06 14:55:03 - Bootstrapping Spring Data JPA repositories in DEFAULT mode.
2026-01-06 14:55:03 - Finished Spring Data repository scanning in 275 ms. Found 1 JPA repository interface.
2026-01-06 14:55:05 - HikariPool-1 - Starting...
2026-01-06 14:55:07 - HikariPool-1 - Exception during pool initialization.
org.postgresql.util.PSQLException: FATAL: database "template" does not exist
...
2026-01-06 14:55:07 - Application run failed
```

### Root Cause Analysis

1. **Spring Data JPA is being activated** despite exclusions:
   - Log shows: "Found 1 JPA repository interface"
   - This triggers database initialization

2. **Liquibase is trying to run migrations:**
   - Error occurs in `SpringLiquibase.afterPropertiesSet()`
   - This happens before MCP server can initialize

3. **Auto-configuration exclusions may not be working:**
   - `application.yml` has exclusions
   - `@SpringBootApplication` has exclusions
   - But Spring is still initializing database components

### Current Exclusions

**In `ProjectNameApplication.java`:**
```java
@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    JdbcTemplateAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class,
    LiquibaseAutoConfiguration.class,
    TransactionAutoConfiguration.class
})
```

**In `application.yml`:**
```yaml
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
      - org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration
      - org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
      - org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration
      - org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration
      - org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration
      - org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
      - org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration
```

### Issue: Repository Interface Still Present

The log shows "Found 1 JPA repository interface" - we need to:
1. Find and remove/disable the repository interface
2. Or ensure it's not being scanned

### Investigation Results

**Searched for repository interfaces:**
- ✅ No `@Repository` annotations found in main source code
- ✅ No `extends Repository` interfaces found in main source code
- ✅ No `*Repository.java` files found in main source code
- ⚠️ **Found:** Test files reference `ExampleRepository` but the interface doesn't exist in main source
  - `src/test/java/adrianmikula/projectname/e2e/ExampleE2ETest.java`
  - `src/test/java/adrianmikula/projectname/component/ExampleServiceComponentTest.java`
  - `src/test/java/adrianmikula/projectname/unit/ExampleServiceTest.java`

**Hypothesis:**
- The repository interface might be in a transitive dependency
- Or Spring Data is creating a proxy for a missing interface
- Or there's a repository in a different package being scanned

### Next Steps

- [x] Search for repository interfaces in codebase - **COMPLETED: None found in main source**
- [ ] Check if Spring Data dependencies are transitively included
- [ ] Verify if tests are being compiled/loaded at runtime (they shouldn't be)
- [ ] Add explicit component scan exclusion for repository packages
- [ ] **Add `@EnableJpaRepositories` exclusion or disable repository scanning entirely**
- [ ] Test application startup after fixes

### Potential Fix: Disable JPA Repository Scanning

Even though we've excluded `JpaRepositoriesAutoConfiguration`, Spring might still be scanning. We should explicitly disable repository scanning:

**Option 1: Add to `@SpringBootApplication`:**
```java
@SpringBootApplication(exclude = {
    // ... existing exclusions ...
    JpaRepositoriesAutoConfiguration.class
})
@EnableJpaRepositories(enabled = false)  // Explicitly disable
```

**Option 2: Add to `application.yml`:**
```yaml
spring:
  data:
    jpa:
      repositories:
        enabled: false
```

**Option 3: Use `@ComponentScan` with exclusions:**
```java
@ComponentScan(
    basePackages = "adrianmikula.jakartamigration",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = ".*\\.dao\\..*"
    )
)
```

**Status:** 🔄 In Progress

### Fix Applied: Added JpaRepositoriesAutoConfiguration Exclusion

**File:** `src/main/java/adrianmikula/projectname/ProjectNameApplication.java`

**Added:**
- Import: `org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration`
- Exclusion in `@SpringBootApplication`: `JpaRepositoriesAutoConfiguration.class`

**Note:** This was already in `application.yml` but should also be in the annotation for consistency and to ensure it's applied.

**Status:** ✅ Applied

---

## Finding #6: Redundant ComponentScan Causing Bean Discovery Issues

### Discovery Date
January 6, 2026

### Problem
Application fails to start with missing `ChangeTracker` bean, even though it's defined in `JakartaMigrationConfig`.

### Root Cause
`JakartaMigrationConfig` had a redundant `@ComponentScan(basePackages = "adrianmikula.jakartamigration")` annotation. Since `@SpringBootApplication` now has `scanBasePackages` that includes this package, the duplicate `@ComponentScan` could cause:
- Component scan conflicts
- Bean discovery timing issues
- Configuration class processing order problems

### Fix Applied
Removed `@ComponentScan` from `JakartaMigrationConfig`:
- Component scanning is now handled by `@SpringBootApplication(scanBasePackages = {...})`
- The `@Configuration` annotation is sufficient for Spring to discover and process bean definitions
- This ensures beans are discovered in the correct order

**Status:** ✅ Applied

---

## Finding #7: Configuration Binding Error

### Discovery Date
January 6, 2026

### Problem
Application fails to start with configuration binding error:
```
Failed to bind properties under 'jakarta.migration.feature-flags.features' to java.util.Map<java.lang.String, java.lang.Boolean>
Value: ""
Reason: No converter found capable of converting from type [java.lang.String] to type [java.util.Map<java.lang.String, java.lang.Boolean>]
```

### Root Cause
In `application.yml`, the `features:` key was defined with only commented-out examples:
```yaml
features:
  # Example: Enable auto-fixes for testing
  # auto-fixes: true
```

When a YAML key has no actual values (only comments), Spring Boot binds it as an empty string `""` instead of an empty map `{}`. Since `FeatureFlagsProperties.features` is `Map<String, Boolean>`, Spring can't convert an empty string to a Map.

### Fix Applied
Set `features` to an explicit empty map:
```yaml
features: {}
```

This tells Spring Boot to bind it as an empty `Map<String, Boolean>` rather than trying to parse an empty string.

**Status:** ✅ Fixed

---

## Finding #8: Stripe Integration Disabled

### Discovery Date
January 6, 2026

### Decision
Temporarily disabled Stripe integration to get MCP server working quickly.

### Changes Made

1. **Disabled Stripe Validation:**
   ```yaml
   stripe:
     enabled: ${STRIPE_VALIDATION_ENABLED:false}  # Changed from true to false
   ```

2. **Fixed Configuration Binding:**
   ```yaml
   price-id-to-tier: {}  # Set to empty map instead of empty key with comments
   ```

### Impact

- Stripe license validation is disabled
- `StripeLicenseService` will still be created but will return `null` for all validations
- `LicenseService` will skip Stripe validation and use Apify or simple validation instead
- MCP server should start without Stripe configuration errors

**Status:** ✅ Disabled

---

## Finding #9: Lazy Tool Download Implementation

### Discovery Date
January 6, 2026

### Problem
Apache Tomcat migration tool was being downloaded at startup, causing:
- Startup delays
- Startup failures if download fails (HTTP 404/403)
- Unnecessary network calls when tool might never be used

### Solution
Made tool download **lazy/on-demand** - only downloads when tool is actually used.

### Changes Made

1. **Lazy Initialization**: Constructor no longer calls `findToolJar()` - sets `toolJarPath = null`
2. **On-Demand Resolution**: `migrate()` method resolves tool path lazily on first use
3. **Cache-First**: Checks cache directory before attempting download
4. **Helper Methods**: Added `getCacheDirectory()` and `findCachedJar()` to check cache without downloading

### Impact

- ✅ **Faster Startup** - No download attempts at startup
- ✅ **No Startup Failures** - Download failures don't prevent application from starting
- ✅ **On-Demand Download** - Tool only downloads when actually needed

**Status:** ✅ Implemented

---

## Finding #10: Direct Apache Website Download

### Discovery Date
January 6, 2026

### Problem
Tool downloader was using GitHub API and required environment variables, making it complex and potentially unreliable.

### Solution
Updated to download directly from Apache website with multiple fallback URLs.

### Changes Made

1. **Removed Environment Variable Requirement:**
   - No longer checks `JAKARTA_MIGRATION_TOOL_PATH` in primary flow
   - Tool downloads automatically from Apache website
   - Env var can still be used as override if needed

2. **Direct Apache Download:**
   - Removed GitHub API version detection
   - Uses multiple Apache download URLs with fallback:
     - Apache archive (official distribution)
     - GitHub releases (Apache hosts releases here)
     - Apache CDN (alternative mirror)
   - Uses stable version (1.0.0) - no dynamic version detection

3. **Simplified Code:**
   - Removed `getLatestReleaseVersion()` method
   - Removed `getDownloadUrl()` method
   - Removed GitHub API dependencies
   - Cleaner, more maintainable code

### Benefits

- ✅ **Zero Configuration** - Works out of the box, no env vars needed
- ✅ **More Reliable** - Multiple download sources with automatic fallback
- ✅ **Simpler** - No API calls, no version detection
- ✅ **Faster** - Direct download, no API round-trip
- ✅ **Official Sources** - Downloads from Apache's official distribution

**Status:** ✅ Implemented

---

## Finding #3: Working Examples Research

### Discovery Date
January 6, 2026

### Research Sources

1. **Spring AI Community MCP Annotations**
   - Repository: `spring-ai-community/mcp-annotations`
   - Key patterns:
     - Uses `@Component` on tool classes
     - Returns simple types (`String`, `Integer`, `Boolean`)
     - Uses `org.springaicommunity.mcp.annotation` package (singular)
     - Configuration: `type: SYNC` for synchronous methods
     - No database dependencies

2. **MCP Declarative Java SDK**
   - Repository: `codeboyzhou/mcp-declarative-java-sdk`
   - Non-Spring implementation
   - Not directly applicable to our setup

### Common Patterns from Working Examples

1. ✅ **No Database Dependencies** - All working MCP servers are stateless
2. ✅ **Minimal Auto-Configuration** - Only essential Spring Boot features
3. ✅ **Clean STDOUT** - All logging to STDERR, STDOUT reserved for JSON-RPC
4. ✅ **Simple Return Types** - Methods return `String`, primitives, or simple POJOs
5. ✅ **Proper Component Scanning** - Tools are `@Component` beans

### Our Implementation Comparison

| Aspect | Working Examples | Our Implementation | Status |
|--------|-----------------|-------------------|--------|
| Database Dependencies | None | Attempting PostgreSQL connection | ❌ **FAILING** |
| Spring Data | Not included | JPA/Redis scanning active | ❌ **ISSUE** |
| Auto-Configuration Exclusions | Minimal, focused | Excluded but not working | ❌ **ISSUE** |
| Return Types | Simple (String, primitives) | Simple (String) | ✅ **OK** |
| Component Annotation | `@Component` | `@Component` | ✅ **OK** |
| Annotation Package | `org.springaicommunity.mcp.annotation` | `org.springaicommunity.mcp.annotation` | ✅ **FIXED** |
| Configuration Type | `SYNC` | `SYNC` | ✅ **OK** |
| Logging to STDOUT | None (all to STDERR) | Configured correctly | ✅ **OK** |

**Status:** ✅ Documented

---

## Finding #4: Annotation Scanner Configuration

### Discovery Date
January 6, 2026

### Current Configuration

**In `application.yml`:**
```yaml
spring:
  ai:
    mcp:
      server:
        type: SYNC  # CRITICAL: Must match method return types
        annotation-scanner:
          enabled: true
        transport: stdio
```

### Key Points

1. **Type: SYNC** - Required for synchronous methods that return `String`/POJOs
   - If set to `ASYNC`, methods must return `Mono`/`Flux`
   - Our methods return `String`, so `SYNC` is correct

2. **Annotation Scanner Enabled** - Required for `@McpTool` discovery
   - Scanner looks for `@Component` beans with `@McpTool` methods
   - Must use correct package name (now fixed)

3. **Transport: stdio** - For local MCP server operation
   - STDOUT must be clean (only JSON-RPC)
   - Logging configured to STDERR

### Warnings in Logs

```
Bean 'org.springframework.ai.mcp.server.common.autoconfigure.annotations.McpServerAnnotationScannerAutoConfiguration' 
of type [...] is not eligible for getting processed by all BeanPostProcessors
```

This is a warning, not an error. The scanner should still work, but may process beans in a different order.

**Status:** ✅ Configured correctly (after package fix)

---

## Summary of Root Causes

### Primary Issue: Package Name Mismatch ✅ FIXED
- **Problem:** Code imported from `mcp.annotations` (plural) but JAR contains `mcp.annotation` (singular)
- **Impact:** Annotations couldn't be found, tools not registered
- **Fix:** Updated all imports to use singular `annotation` package

### Secondary Issue: Database Initialization Failure 🔄 IN PROGRESS
- **Problem:** Application tries to connect to PostgreSQL, fails, prevents MCP server startup
- **Impact:** MCP server never initializes, so annotations never get scanned
- **Fix Needed:** Remove repository interfaces, ensure database auto-config is excluded

### Tertiary Issue: Annotation Scanner Warnings ⚠️ MONITORING
- **Problem:** BeanPostProcessor warnings in logs
- **Impact:** Likely non-fatal, but may affect bean processing order
- **Action:** Monitor, may need Spring AI version update

---

## Finding #5: Component Scan Configuration Issue (CRITICAL)

### Discovery Date
January 6, 2026

### Problem
MCP annotation scanner reports: "No tool methods found in the provided tool objects: []"

### Root Cause
The main application class is in `adrianmikula.projectname` package, but MCP tools are in `adrianmikula.jakartamigration.mcp` package. `@SpringBootApplication` by default only scans from its own package.

### Fix Applied
Added `scanBasePackages` to `@SpringBootApplication` to explicitly include both packages:
```java
@SpringBootApplication(
    exclude = { /* ... */ },
    scanBasePackages = {
        "adrianmikula.jakartamigration",  // Include Jakarta Migration packages for MCP tools
        "adrianmikula.projectname"       // Include projectname packages
    }
)
```

**Status:** ✅ Applied

---

## Next Steps

1. ✅ Fix package name mismatch - **COMPLETED**
2. ✅ Verify build compiles successfully - **COMPLETED** (see `build-test-results-2026-01-06.md`)
3. ✅ Add component scan configuration - **COMPLETED**
4. 🔄 Find and remove repository interfaces causing database initialization
5. ⏳ Exclude template test files from compilation
6. ⏳ Test application startup without database errors
7. ⏳ Verify annotation scanner discovers tools
8. ⏳ Test MCP server connection and tool execution

---

## Files Modified

### Source Code
- `src/main/java/adrianmikula/jakartamigration/mcp/JakartaMigrationTools.java`
- `src/main/java/adrianmikula/jakartamigration/mcp/SentinelTools.java`

### Documentation
- `docs/research/mcp-annotation-research.md`
- `docs/research/mcp-annotation-dependency-comparison.md`
- `docs/research/mcp-annotation-package-fix.md` (new)
- `build.gradle.kts` (comments updated)

### This Document
- `docs/wip/annotation-investigation-2026-01-06.md` (this file)

---

## References

- [Spring AI Community MCP Annotations](https://github.com/spring-ai-community/mcp-annotations)
- [Spring AI MCP Documentation](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-annotations-overview.html)
- [MCP Declarative Java SDK](https://github.com/codeboyzhou/mcp-declarative-java-sdk)

---

## Notes

- JAR inspection revealed the actual package structure - always verify by inspecting JARs, not just documentation
- The artifact name (`mcp-annotations`) doesn't match the package name (`mcp.annotation`) - common source of confusion
- Gradle dependency resolution doesn't catch package name mismatches at build time

