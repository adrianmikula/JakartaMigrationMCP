# Investigation: Jackson Module Conflict (ClassCastException)

## Problem Statement
When running OpenRewrite recipes within the IntelliJ IDEA environment, a `ClassCastException` occurred:
`java.lang.ClassCastException: class com.fasterxml.jackson.module.kotlin.KotlinModule cannot be cast to class com.fasterxml.jackson.databind.Module`

This happened because:
1. The IntelliJ IDE provides its own version of Jackson and its modules (specifically `KotlinModule`) in the `PathClassLoader`.
2. Our OpenRewrite implementation uses its own Jackson `ObjectMapper`.
3. Jackson's SPI mechanism (handling `META-INF/services/com.fasterxml.jackson.databind.Module`) was discovering the IDE's `KotlinModule` via the Thread Context ClassLoader (TCCL).
4. Because the classes were loaded by different ClassLoaders (`PathClassLoader` vs `PluginClassLoader`), they were incompatible, leading to the cast exception.

## Investigation Steps
1. **Initial Conflict**: Identified that `KotlinModule` was being discovered even though we didn't explicitly include it in our classpath.
2. **SPI Discovery**: Realized that Jackson's `findAndRegisterModules()` uses the TCCL to look for services.
3. **Classloader Analysis**: Confirmed that the `PathClassLoader` (bundled with IDEA) was leaking into the OpenRewrite environment.

## Solution: FilteringClassLoader
We implemented a custom `FilteringClassLoader` that:
- Delegates to the parent (PluginClassLoader).
- **Blocks** specific packages: `com.fasterxml.jackson.module.kotlin.*`.
- **Filters** resources: Intercepts `getResources("META-INF/services/com.fasterxml.jackson.databind.Module")` to strip out references to the blocked modules.

## Evolution: Hardened Hybrid Strategy (V2)
The previous attempts were bypassed because `ClassGraph` and `ServiceLoader` were discovering Jackson modules from the parent ClassLoader through URLs and resources that were not being effectively filtered.

We've implemented a "Path-Based Resource Filtering" strategy:
1. **Plugin-Aware URL Filtering**: `FilteringClassLoader` now identifies the plugin's own JAR location (`getPluginPathPrefix()`). 
2. **Selective URL Exposure**: `getURLs()` only returns URLs that are either from our own plugin OR are from the parent but DO NOT match an aggressive blacklist of Jackson-related terms (e.g., `jackson-module-kotlin`, `jackson-datatype-jdk8`).
3. **Resource Masking**: `getResource()` and `getResources()` now use `isSafeUrl()` to filter out any resource coming from a blacklisted path, even if it's found in the parent hierarchy.
4. **Expanded Blacklist**: The package and resource blacklists in `RecipeServiceImpl` have been expanded to include all common Jackson modules Bundled with IntelliJ (Afterburner, Kotlin, JDK8, JSR310, etc.).

## Outcome
By identifying "Safe" vs "Unsafe" URLs based on the plugin's own path, we've created a bulletproof isolation layer that prevents any Jackson-related SPI discovery from leaking into the OpenRewrite environment.
