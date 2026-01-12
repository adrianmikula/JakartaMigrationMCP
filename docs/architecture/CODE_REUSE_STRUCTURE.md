# Code Reuse Structure - Maximum Reuse, Zero Duplication

## Overview

The dual package approach is designed for **maximum code reuse** with **zero duplication**. Free code exists in only one place and is reused by the premium package.

## Architecture

### Free Package (Main Project)
```
src/main/java/adrianmikula/jakartamigration/
├── dependencyanalysis/     ✅ FREE - Single source of truth
├── sourcecodescanning/      ✅ FREE - Single source of truth
└── mcp/
    └── JakartaMigrationTools.java  ✅ FREE - 4 free tools only
```

**Build Output**: 
- `jakarta-migration-mcp-free-{version}.jar` (executable JAR)
- `jakarta-migration-mcp-{version}.jar` (library JAR for premium to depend on)

### Premium Package (Subfolder)
```
jakarta-migration-mcp-premium/
├── src/main/java/adrianmikula/jakartamigration/
│   ├── coderefactoring/        ✅ PREMIUM - Premium only
│   ├── runtimeverification/    ✅ PREMIUM - Premium only
│   ├── config/                 ✅ PREMIUM - License validation
│   ├── api/                    ✅ PREMIUM - License API, Stripe
│   ├── storage/                 ✅ PREMIUM - License storage
│   └── mcp/
│       └── JakartaMigrationTools.java  ✅ PREMIUM - All 7 tools
└── build.gradle.kts
    └── dependencies {
        implementation(project(":"))  ✅ DEPENDS ON FREE PACKAGE
    }
```

**Build Output**: `jakarta-migration-mcp-premium-{version}.jar`

## Code Reuse Mechanism

### Gradle Multi-Project Build

The premium package **depends on the free package** as a Gradle project dependency:

```kotlin
// jakarta-migration-mcp-premium/build.gradle.kts
dependencies {
    // DEPEND ON FREE PACKAGE - No code duplication!
    implementation(project(":"))
    
    // ... other dependencies
}
```

This means:
- ✅ Free packages (`dependencyanalysis`, `sourcecodescanning`) are compiled once in the main project
- ✅ Premium package imports and uses them directly (no duplication)
- ✅ Changes to free code automatically propagate to premium
- ✅ Single source of truth for all free functionality

### Import Structure

**Premium JakartaMigrationTools** imports from free package:
```java
// These come from the free package (project dependency)
import adrianmikula.jakartamigration.dependencyanalysis.domain.*;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyAnalysisModule;
import adrianmikula.jakartamigration.sourcecodescanning.service.SourceCodeScanner;

// These are premium-only
import adrianmikula.jakartamigration.coderefactoring.domain.MigrationPlan;
import adrianmikula.jakartamigration.runtimeverification.service.RuntimeVerificationModule;
```

## What Gets Reused

### ✅ Reused (No Duplication)
- `dependencyanalysis/` package - Used by both free and premium
- `sourcecodescanning/` package - Used by both free and premium
- MCP infrastructure (`McpStreamableHttpController`, `McpSseController`, etc.)
- Resources (YAML files, configs)

### ❌ Not Reused (Different Implementations)
- `JakartaMigrationTools.java` - Different classes:
  - Free version: 4 tools only
  - Premium version: 7 tools (extends free functionality)

## Build Process

### Building Free Package
```bash
./gradlew bootJar
# Creates: build/libs/jakarta-migration-mcp-free-{version}.jar
# Also creates: build/libs/jakarta-migration-mcp-{version}.jar (library JAR)
```

### Building Premium Package
```bash
./gradlew :jakarta-migration-mcp-premium:bootJar
# Creates: jakarta-migration-mcp-premium/build/libs/jakarta-migration-mcp-premium-{version}.jar
# Automatically compiles free package first (project dependency)
```

### Building Both
```bash
./gradlew build
# Builds both packages, with premium depending on free
```

## Migration to Separate Repo

When premium package moves to a separate repository:

### Current (Multi-Project)
```kotlin
// Premium build.gradle.kts
dependencies {
    implementation(project(":"))  // Project dependency
}
```

### Future (Published Library)
```kotlin
// Premium build.gradle.kts (in separate repo)
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/adrianmikula/JakartaMigrationMCP")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_USERNAME")
            password = project.findProperty("gpr.token") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    // Depend on published free package JAR
    implementation("adrianmikula:jakarta-migration-mcp-free:${version}")
}
```

**Migration Steps**:
1. Publish free package JAR to Maven repository (GitHub Packages, Maven Central, etc.)
2. Update premium `build.gradle.kts` to use published dependency instead of `project(":")`
3. Remove `include("jakarta-migration-mcp-premium")` from main `settings.gradle.kts`
4. Move premium folder to separate repository

## Benefits

1. **Zero Code Duplication**: Free code exists in one place only
2. **Automatic Updates**: Changes to free code automatically available to premium
3. **Smaller Premium JAR**: Premium JAR doesn't include duplicated free code
4. **Easier Maintenance**: Fix bugs once in free package, premium gets fix automatically
5. **Clear Separation**: Premium only contains premium-specific code

## Verification

To verify no duplication:
```bash
# Check that premium doesn't have free packages
ls jakarta-migration-mcp-premium/src/main/java/adrianmikula/jakartamigration/
# Should NOT see: dependencyanalysis/, sourcecodescanning/

# Check that premium depends on free
grep "project(\":\")" jakarta-migration-mcp-premium/build.gradle.kts
# Should see: implementation(project(":"))
```

## Summary

- ✅ **Free code**: Single source in main project
- ✅ **Premium code**: Only premium-specific packages
- ✅ **Reuse mechanism**: Gradle project dependency (`implementation(project(":"))`)
- ✅ **Zero duplication**: Free packages not copied to premium
- ✅ **Future-proof**: Easy migration to published library dependency

