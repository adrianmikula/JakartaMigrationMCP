# Module Dependency Boundaries

**Date:** 2026-02-04  
**Status:** Enforced ✓

This document defines the strict dependency direction rules for the Jakarta Migration MCP project. These boundaries ensure clean open-core extraction and prevent architectural decay.

---

## Dependency Rules (Non-Negotiable)

```
Allowed Dependencies:
├── migration-core → (nothing - no internal dependencies)
├── mcp-server → migration-core
├── intellij-plugin → migration-core
├── premium-engine → migration-core + mcp-server
└── premium-intellij → migration-core + mcp-server + intellij-plugin

Forbidden Dependencies:
├── migration-core ↛ mcp-server
├── migration-core ↛ intellij-plugin
├── migration-core ↛ premium-engine
├── migration-core ↛ premium-intellij
├── mcp-server ↛ intellij-plugin
└── mcp-server ↛ premium-intellij
```

---

## Current Dependency Analysis

### ✅ migration-core
**Dependencies:** None (external libraries only)
- `org.slf4j:slf4j-api`
- `org.yaml:snakeyaml`
- `com.fasterxml.jackson.core:*`
- `org.xerial:sqlite-jdbc`
- `org.ow2.asm:*`
- `org.openrewrite:*`

### ✅ mcp-server
**Dependencies:** `migration-core`
- `implementation(project(":migration-core"))`
- Spring Boot web dependencies
- Spring AI MCP server

### ✅ intellij-plugin
**Dependencies:** `migration-core`
- `implementation(project(":migration-core"))`
- Jackson for JSON
- IntelliJ Platform SDK

### premium-engine (planned)
**Dependencies:** `migration-core` + `mcp-server`
- `implementation(project(":migration-core"))`
- `implementation(project(":mcp-server"))`

### premium-intellij (planned)
**Dependencies:** `migration-core` + `mcp-server` + `intellij-plugin`
- `implementation(project(":migration-core"))`
- `implementation(project(":mcp-server"))`
- `implementation(project(":intellij-plugin"))`

---

## Technical Enforcement

### 1. Gradle Build Configuration

Each module's `build.gradle.kts` explicitly declares dependencies:

```kotlin
// migration-core/build.gradle.kts
dependencies {
    // Only external dependencies - NO project() dependencies
    implementation("org.slf4j:slf4j-api:2.0.9")
    // ...
}
```

### 2. Package Visibility (Java 9+)

Use package-private visibility to restrict access:

```java
// In migration-core
package adrianmikula.jakartamigration.config;

// Only accessible within migration-core package
class InternalConfig {
    // Not public - cannot be accessed from other modules
}
```

### 3. Compile Dependency Check

Add compile-time verification using Gradle's configuration:

```kotlin
// In settings.gradle.kts or root build.gradle.kts
subprojects {
    configurations.all {
        resolutionStrategy {
            // Fail if any forbidden dependency is detected
            eachDependency { details ->
                if (details.group == "adrianmikula.jakartamigration") {
                    val allowedModules = when (project.name) {
                        "migration-core" -> setOf()  // No internal deps
                        "mcp-server" -> setOf("migration-core")
                        "intellij-plugin" -> setOf("migration-core")
                        else -> setOf()
                    }
                    require(allowedModules.contains(details.name)) {
                        "Forbidden dependency: ${project.name} cannot depend on ${details.name}"
                    }
                }
            }
        }
    }
}
```

---

## Verification Commands

Check for dependency violations:

```bash
# View dependency tree
./gradlew :migration-core:dependencies --configuration compileClasspath
./gradlew :mcp-server:dependencies --configuration compileClasspath
./gradlew :intellij-plugin:dependencies --configuration compileClasspath

# Check for internal dependencies
grep -r "project(" --include="*.gradle.kts" .

# Find imports between modules
grep -r "import adrianmikula.jakartamigration.mcp" --include="*.java" .
grep -r "import adrianmikula.jakartamigration.intellij" --include="*.java" .
```

---

## Why These Boundaries Matter

### Open-Core Extraction

When extracting the community edition:

1. **migration-core** becomes standalone (Apache 2.0)
2. **mcp-server** bundles with core
3. **intellij-plugin** bundles with core
4. **premium-*** modules stay proprietary

Without these boundaries, migration-core would accidentally depend on proprietary code, making open-core extraction impossible.

### Dependency Inversion

Higher-level modules (plugins, premium features) depend on lower-level abstractions (core). This allows:

- Testing core without UI dependencies
- Reusing core in other contexts (CLI, CI/CD)
- Clean licensing separation

---

## Related Documentation

- [`docs/research/licensing-research.md`](../research/licensing-research.md)
- [`docs/improvements/OPENCORE_LICENSING_PLAN_2026-02-04.md`](../improvements/OPENCORE_LICENSING_PLAN_2026-02-04.md)
