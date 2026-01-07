# Component Scan Issue - January 6, 2026

## Problem Identified

The MCP annotation scanner reports:
```
WARN  o.s.m.p.tool.SyncMcpToolProvider - No tool methods found in the provided tool objects: []
```

This indicates that the annotation scanner is running but not finding any `@McpTool` annotated methods.

## Root Cause Analysis

### Package Structure Issue

**Main Application Class:**
- Package: `adrianmikula.projectname`
- Location: `src/main/java/adrianmikula/projectname/ProjectNameApplication.java`
- Annotation: `@SpringBootApplication` (scans from its own package by default)

**MCP Tools:**
- Package: `adrianmikula.jakartamigration.mcp`
- Location: `src/main/java/adrianmikula/jakartamigration/mcp/`
- Classes: `JakartaMigrationTools.java`, `SentinelTools.java`
- Annotations: `@Component` with `@McpTool` methods

### The Issue

`@SpringBootApplication` by default scans from the package of the class it's on. Since `ProjectNameApplication` is in `adrianmikula.projectname`, it only scans:
- `adrianmikula.projectname` and subpackages
- **NOT** `adrianmikula.jakartamigration` and subpackages

### Current Configuration

**JakartaMigrationConfig.java:**
```java
@Configuration
@ComponentScan(basePackages = "adrianmikula.jakartamigration")
```

This should help, but there might be a timing issue where the MCP annotation scanner runs before this configuration is processed, or the scanner doesn't see beans from this scan.

## Research: Working Examples

### Pattern from Spring AI Community Examples

Working Spring AI MCP servers typically:
1. Put the main application class in the same package hierarchy as the MCP tools
2. OR explicitly configure `@ComponentScan` on the `@SpringBootApplication` class
3. Ensure MCP tool classes are in packages that are scanned

### Recommended Fix

**Option 1: Add ComponentScan to Main Application (RECOMMENDED)**

Update `ProjectNameApplication.java`:
```java
@SpringBootApplication(
    exclude = {
        DataSourceAutoConfiguration.class,
        // ... other exclusions
    },
    scanBasePackages = {
        "adrianmikula.jakartamigration",  // Include Jakarta Migration packages
        "adrianmikula.projectname"         // Include projectname packages
    }
)
```

**Option 2: Move Application Class**

Move `ProjectNameApplication.java` to `adrianmikula.jakartamigration` package so it naturally scans the correct packages.

**Option 3: Ensure JakartaMigrationConfig is Processed First**

Verify that `JakartaMigrationConfig` is being processed and its `@ComponentScan` is effective.

## Verification Steps

1. Check if `JakartaMigrationTools` and `SentinelTools` beans are being created
2. Verify component scanning includes `adrianmikula.jakartamigration.mcp` package
3. Check annotation scanner logs for which packages it's scanning
4. Test with explicit `scanBasePackages` on `@SpringBootApplication`

## Next Steps

- [ ] Add `scanBasePackages` to `@SpringBootApplication`
- [ ] Verify beans are created (add logging)
- [ ] Test annotation scanner discovery
- [ ] Document working configuration

