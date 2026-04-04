# ADR 0001: Jackson Isolation via FilteringClassLoader

## Status
Accepted

## Context
The Jakarta Migration IntelliJ plugin uses OpenRewrite for code refactoring. OpenRewrite, in turn, uses Jackson for configuration and recipe metadata parsing. When running within the IntelliJ IDEA environment, the plugin's classpath is merged with the IDE's classpath.

The IDE (IntelliJ IDEA) bundles its own version of Jackson, which often includes the `com.fasterxml.jackson.module.kotlin.KotlinModule`. This module sometimes has linkage issues or version mismatches when discovered by the Jackson instance running inside the plugin (using the plugin's version of `jackson-databind`). 

Specifically, we encountered a `ClassCastException` where `KotlinModule` (from the IDE's `PathClassLoader`) could not be cast to `com.fasterxml.jackson.databind.Module` (from the plugin's `PluginClassLoader`). This happened during OpenRewrite's environment initialization, which performs a classpath scan to discover recipes and Jackson modules.

## Decision
We decided to implement a hybrid isolation strategy using a custom `FilteringClassLoader` to separate the OpenRewrite environment from conflicting IDE-provided Jackson modules.

The `FilteringClassLoader` acts as a wrapper around the plugin's ClassLoader and:
1.  **Filters Class Loading**: Intercepts `loadClass` calls for `com.fasterxml.jackson.module.kotlin.*` and throws `ClassNotFoundException`.
2.  **Filters Service Discovery**: Intercepts `getResources` calls for `META-INF/services/com.fasterxml.jackson.databind.Module` and removes entries pointing to the IDE's Kotlin modules.
3.  **Exposes Classpath URLs**: Implements `getURLs()` (delegating to the parent via reflection) to support classpath scanning tools like ClassGraph, which is used by OpenRewrite to find recipes.

**Discovery vs. Runtime Strategy:**
- We continue to use the **PluginClassLoader** for the initial environment scan (`Environment.builder().scanClassLoader(pluginClassLoader)`). This ensures all recipes bundled with the plugin are discovered.
- We set the **FilteringClassLoader** as the **Thread Context ClassLoader (TCCL)** during the entire OpenRewrite session. Since Jackson's `ObjectMapper` uses the TCCL for module discovery, this effectively blocks the IDE's conflicting modules during the configuration and execution phases, even if they were technically "visible" during the initial scan.

## Consequences
- **Positive**: Resolves the `ClassCastException` while maintaining full recipe discovery.
- **Positive**: The UI is now resilient to initialization failures, recording them as `RUN_FAILED`.
- **Positive**: Fixed a `NullPointerException` in `RefactorTabComponent` where UI card updates were incorrectly processing spacer components.
