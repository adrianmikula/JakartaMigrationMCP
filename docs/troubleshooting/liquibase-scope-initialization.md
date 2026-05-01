# Liquibase "Cannot find default log service" Error

## Problem

When running the Jakarta Migration plugin in IntelliJ IDEA, the following error occurs during database initialization:

```
java.lang.RuntimeException: Failed to initialize Liquibase scope
    at adrianmikula.jakartamigration.analysis.persistence.CentralMigrationAnalysisStore.initializeDatabase(...)
Caused by: liquibase.exception.UnexpectedLiquibaseException: Cannot find default log service
    at liquibase.Scope.getCurrentScope(Scope.java:98)
    at liquibase.Scope.enter(Scope.java:215)
```

## Root Cause

Liquibase 4.x uses a `Scope` system for dependency injection and configuration management. When initializing, it tries to discover services (like `LogService`) through Java's Service Provider Interface (SPI) mechanism.

In the IntelliJ plugin environment:
- The plugin runs in an **isolated ClassLoader** separate from the IDE's main ClassLoader
- SPI-based service discovery fails because the ClassLoader cannot find `META-INF/services/liquibase.logging.LogService` files
- Without a `LogService`, Liquibase cannot initialize its internal scope, causing the error

## Solution

The fix is to manually provide a `LogService` when creating the Liquibase scope, bypassing the automatic service discovery:

```java
// Instead of relying on SPI discovery, explicitly provide the LogService
Scope.child(Scope.Attr.logService, new JavaLogService(), () -> {
    // Liquibase operations here
    liquibase.update();
});
```

### Implementation Details

The key changes in `CentralMigrationAnalysisStore.java`:

1. **Use `Scope.child()` with explicit `LogService`**: This creates a child scope with the `JavaLogService` already configured
2. **Remove manual scope management**: Let `Scope.child()` handle the scope lifecycle automatically
3. **Wrap all Liquibase operations** inside the `Scope.child()` lambda

### Why This Works

- `Scope.child()` properly initializes the scope hierarchy, even when no root scope exists
- Providing `Scope.Attr.logService` directly injects the service, bypassing SPI discovery
- The lambda pattern ensures proper scope cleanup via try-with-resources internally

## References

- [Liquibase GitHub Issue #3819](https://github.com/liquibase/liquibase/issues/3819) - Multiple ClassLoader issue
- [Liquibase PR #1768](https://github.com/liquibase/liquibase/pull/1768) - Root scope initialization fix
- [Liquibase Embedding Guide](https://contribute.liquibase.com/extensions-integrations/integration-guides/embedding-liquibase/) - Official embedding documentation

## Affected Versions

- **Liquibase**: 4.x (tested with 4.24.0)
- **IntelliJ Platform**: 2023.x, 2024.x
- **Plugin Environment**: Any isolated ClassLoader environment (OSGi, PF4J, etc.)

## Verification

After applying the fix:
1. Build the plugin: `./gradlew :premium-intellij-plugin:buildPlugin`
2. Run the development IDE: `mise run runIdeDev`
3. Open the Migration tool window
4. Database should initialize without the error

## Related Issues

This same pattern applies to any plugin system using isolated ClassLoaders:
- Eclipse plugins
- NetBeans modules
- OSGi bundles
- PF4J plugins
