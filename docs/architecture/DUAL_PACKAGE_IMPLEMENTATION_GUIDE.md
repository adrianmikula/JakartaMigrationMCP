# Dual Package Implementation Guide

## Overview

This guide provides step-by-step instructions for splitting the Jakarta Migration MCP into two packages:
1. **Free Package** (`@jakarta-migration/mcp-server`) - Open-source
2. **Premium Package** (`@jakarta-migration/mcp-server-premium`) - Closed-source

## Current Code Structure

### Free Components (Open-Source)
```
src/main/java/adrianmikula/jakartamigration/
├── dependencyanalysis/     # FREE - Dependency analysis
├── sourcecodescanning/     # FREE - Source code scanning
└── mcp/
    └── JakartaMigrationTools.java
        ├── analyzeJakartaReadiness()  # FREE
        ├── detectBlockers()           # FREE
        ├── recommendVersions()        # FREE
        └── analyzeMigrationImpact()   # FREE
```

### Premium Components (Closed-Source)
```
src/main/java/adrianmikula/jakartamigration/
├── coderefactoring/        # PREMIUM - Code refactoring
├── runtimeverification/    # PREMIUM - Runtime verification
├── config/                 # PREMIUM - License validation
└── mcp/
    └── JakartaMigrationTools.java
        ├── createMigrationPlan()  # PREMIUM
        ├── refactorProject()      # PREMIUM
        └── verifyRuntime()        # PREMIUM
```

### Shared Components
```
src/main/java/adrianmikula/jakartamigration/
├── api/                    # Shared - License API, Stripe webhooks
└── storage/                # Shared - License storage
```

## Implementation Plan

### Phase 1: Create Free Package Structure

#### Step 1.1: Create Free Package Directory
```bash
mkdir jakarta-migration-mcp-free
cd jakarta-migration-mcp-free
```

#### Step 1.2: Copy Free Components
```bash
# From root project
cp -r src/main/java/adrianmikula/jakartamigration/dependencyanalysis ./
cp -r src/main/java/adrianmikula/jakartamigration/sourcecodescanning ./
cp -r src/main/java/adrianmikula/jakartamigration/mcp ./
# Edit JakartaMigrationTools.java to remove premium methods
```

#### Step 1.3: Create Free Package build.gradle.kts
```kotlin
plugins {
    java
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
    jacoco
}

group = "adrianmikula"
version = "1.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Spring AI MCP Server
    implementation("org.springframework.ai:spring-ai-starter-mcp-server-webmvc:1.1.2")
    implementation("org.springframework.ai:spring-ai-mcp-annotations:1.1.2")
    implementation("org.springaicommunity:mcp-annotations:0.8.0")

    // Git Operations (for dependency analysis)
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.8.0.202311291450-r")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.bootJar {
    archiveFileName.set("jakarta-migration-mcp-free-${version}.jar")
}
```

#### Step 1.4: Create Free Package JakartaMigrationTools.java
```java
package adrianmikula.jakartamigration.mcp;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Jakarta Migration MCP Tools - FREE VERSION
 * 
 * This class provides free analysis tools only.
 * Premium features (refactoring, verification, planning) are available
 * in @jakarta-migration/mcp-server-premium package.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JakartaMigrationTools {

    private final DependencyAnalysisModule dependencyAnalysisModule;
    private final DependencyGraphBuilder dependencyGraphBuilder;
    private final SourceCodeScanner sourceCodeScanner;

    @McpTool(
        name = "analyzeJakartaReadiness",
        description = "Analyzes project for Jakarta migration readiness. FREE tool - analysis only."
    )
    public String analyzeJakartaReadiness(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath) {
        // Implementation (same as current)
    }

    @McpTool(
        name = "detectBlockers",
        description = "Detects blockers preventing Jakarta migration. FREE tool - analysis only."
    )
    public String detectBlockers(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath) {
        // Implementation (same as current)
    }

    @McpTool(
        name = "recommendVersions",
        description = "Recommends Jakarta-compatible dependency versions. FREE tool - analysis only."
    )
    public String recommendVersions(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath) {
        // Implementation (same as current)
    }

    @McpTool(
        name = "analyzeMigrationImpact",
        description = "Full migration impact analysis. FREE tool - analysis only."
    )
    public String analyzeMigrationImpact(
            @McpToolParam(description = "Path to the project root directory", required = true) String projectPath) {
        // Implementation (same as current)
    }

    // NOTE: Premium tools (createMigrationPlan, refactorProject, verifyRuntime)
    // are NOT included in this free version.
    // Users should install @jakarta-migration/mcp-server-premium for these features.
}
```

### Phase 2: Create Premium Package Structure

#### Step 2.1: Create Premium Package Directory (Private Repository)
```bash
# Create in private GitHub repository
mkdir jakarta-migration-mcp-premium
cd jakarta-migration-mcp-premium
```

#### Step 2.2: Copy All Components
```bash
# Copy everything from original project
cp -r ../JakartaMigrationMCP/src/main/java/adrianmikula/jakartamigration/* ./
```

#### Step 2.3: Create Premium Package build.gradle.kts
```kotlin
plugins {
    java
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
    id("org.openrewrite.rewrite") version "6.8.0"
    jacoco
    // Add obfuscation plugin for IP protection
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "adrianmikula"
version = "1.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    // All dependencies from original build.gradle.kts
    // Including:
    // - Spring Boot starters
    // - Spring AI MCP Server
    // - OpenRewrite (for refactoring)
    // - ASM (for bytecode analysis)
    // - All other dependencies
}

tasks.bootJar {
    archiveFileName.set("jakarta-migration-mcp-premium-${version}.jar")
    
    // Optional: Add obfuscation
    // Note: ProGuard/R8 configuration would go here
}
```

#### Step 2.4: Premium Package JakartaMigrationTools.java
```java
package adrianmikula.jakartamigration.mcp;

/**
 * Jakarta Migration MCP Tools - PREMIUM VERSION
 * 
 * This class provides ALL tools including premium features:
 * - Free analysis tools (from free package)
 * - Premium refactoring tools
 * - Premium verification tools
 * - Premium planning tools
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JakartaMigrationTools {

    // All dependencies (free + premium)
    private final DependencyAnalysisModule dependencyAnalysisModule;
    private final DependencyGraphBuilder dependencyGraphBuilder;
    private final SourceCodeScanner sourceCodeScanner;
    private final RefactoringEngine refactoringEngine;  // PREMIUM
    private final RuntimeVerificationModule runtimeVerificationModule;  // PREMIUM
    private final MigrationPlanner migrationPlanner;  // PREMIUM
    private final FeatureFlagsService featureFlagsService;  // PREMIUM

    // Free tools (same as free package)
    @McpTool(name = "analyzeJakartaReadiness", ...)
    public String analyzeJakartaReadiness(...) { ... }

    @McpTool(name = "detectBlockers", ...)
    public String detectBlockers(...) { ... }

    @McpTool(name = "recommendVersions", ...)
    public String recommendVersions(...) { ... }

    @McpTool(name = "analyzeMigrationImpact", ...)
    public String analyzeMigrationImpact(...) { ... }

    // Premium tools (only in premium package)
    @McpTool(
        name = "createMigrationPlan",
        description = "Creates comprehensive migration plan. Requires PREMIUM license."
    )
    public String createMigrationPlan(...) {
        // License check
        if (!featureFlagsService.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)) {
            return createUpgradeRequiredResponse(...);
        }
        // Implementation
    }

    @McpTool(
        name = "refactorProject",
        description = "Automatically refactors source code. Requires PREMIUM license."
    )
    public String refactorProject(...) {
        // License check
        if (!featureFlagsService.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)) {
            return createUpgradeRequiredResponse(...);
        }
        // Implementation
    }

    @McpTool(
        name = "verifyRuntime",
        description = "Verifies runtime execution. Requires PREMIUM license."
    )
    public String verifyRuntime(...) {
        // License check
        if (!featureFlagsService.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)) {
            return createUpgradeRequiredResponse(...);
        }
        // Implementation
    }
}
```

### Phase 3: Create npm Packages

#### Step 3.1: Free Package npm Structure
```json
// jakarta-migration-mcp-free/package.json
{
  "name": "@jakarta-migration/mcp-server",
  "version": "1.0.0",
  "description": "Jakarta Migration MCP Server - Free Analysis Tools",
  "main": "index.js",
  "bin": {
    "jakarta-migration-mcp": "index.js"
  },
  "scripts": {
    "postinstall": "node scripts/postinstall.js"
  },
  "keywords": [
    "mcp",
    "jakarta",
    "migration",
    "analysis"
  ],
  "author": "Adrian Mikula",
  "license": "BSD-3-Clause",
  "repository": {
    "type": "git",
    "url": "https://github.com/adrianmikula/JakartaMigrationMCP"
  },
  "engines": {
    "node": ">=18.0.0"
  },
  "files": [
    "index.js",
    "scripts/",
    "README.md",
    "LICENSE"
  ]
}
```

#### Step 3.2: Free Package index.js
```javascript
#!/usr/bin/env node

const PACKAGE_NAME = '@jakarta-migration/mcp-server';
const JAR_NAME = `jakarta-migration-mcp-free-${VERSION}.jar`;
const GITHUB_RELEASES_URL = `https://github.com/${GITHUB_REPO}/releases/download/v${VERSION}`;

// Rest of the code same as current index.js
// But downloads free JAR instead of full JAR
```

#### Step 3.3: Premium Package npm Structure
```json
// jakarta-migration-mcp-premium/package.json
{
  "name": "@jakarta-migration/mcp-server-premium",
  "version": "1.0.0",
  "description": "Jakarta Migration MCP Server - Premium Features (Closed Source)",
  "main": "index.js",
  "bin": {
    "jakarta-migration-mcp-premium": "index.js"
  },
  "scripts": {
    "postinstall": "node scripts/postinstall.js"
  },
  "keywords": [
    "mcp",
    "jakarta",
    "migration",
    "premium",
    "refactoring"
  ],
  "author": "Adrian Mikula",
  "license": "UNLICENSED",
  "repository": {
    "type": "git",
    "url": "https://github.com/adrianmikula/JakartaMigrationMCP-Premium"
  },
  "engines": {
    "node": ">=18.0.0"
  },
  "files": [
    "index.js",
    "scripts/",
    "README.md",
    "LICENSE"
  ]
}
```

#### Step 3.4: Premium Package index.js
```javascript
#!/usr/bin/env node

const PACKAGE_NAME = '@jakarta-migration/mcp-server-premium';
const JAR_NAME = `jakarta-migration-mcp-premium-${VERSION}.jar`;
// Use private GitHub releases or private npm registry
const GITHUB_RELEASES_URL = `https://github.com/${GITHUB_REPO}/releases/download/v${VERSION}`;

// Rest of the code same as current index.js
// But downloads premium JAR
```

### Phase 4: Build & Distribution

#### Step 4.1: Free Package Build (Public CI)
```yaml
# .github/workflows/release-free.yml
name: Release Free Package

on:
  push:
    tags:
      - 'v*'

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
      - name: Build Free JAR
        run: ./gradlew bootJar
      - name: Create Release
        uses: softprops/action-gh-release@v1
        with:
          files: build/libs/jakarta-migration-mcp-free-*.jar
      - name: Publish to npm
        run: npm publish
        env:
          NODE_AUTH_TOKEN: ${{ secrets.NPM_TOKEN }}
```

#### Step 4.2: Premium Package Build (Private CI)
```yaml
# .github/workflows/release-premium.yml (in private repo)
name: Release Premium Package

on:
  push:
    tags:
      - 'v*'

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
      - name: Build Premium JAR
        run: ./gradlew bootJar
      - name: Obfuscate JAR (optional)
        run: |
          # Add ProGuard/R8 obfuscation step
      - name: Create Release (Private)
        uses: softprops/action-gh-release@v1
        with:
          files: build/libs/jakarta-migration-mcp-premium-*.jar
      - name: Publish to npm (Private)
        run: npm publish --access restricted
        env:
          NODE_AUTH_TOKEN: ${{ secrets.NPM_TOKEN }}
```

### Phase 5: IP Protection Strategies

#### Step 5.1: Bytecode Obfuscation
```kotlin
// build.gradle.kts (premium)
plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

// ProGuard configuration
buildscript {
    dependencies {
        classpath("com.guardsquare:proguard-gradle:7.4.2")
    }
}

tasks.register<proguard.gradle.ProGuardTask>("obfuscate") {
    configuration("proguard-rules.pro")
    injars("build/libs/jakarta-migration-mcp-premium-${version}.jar")
    outjars("build/libs/jakarta-migration-mcp-premium-${version}-obfuscated.jar")
}
```

#### Step 5.2: ProGuard Rules
```proguard
# proguard-rules.pro
-keep class adrianmikula.jakartamigration.mcp.JakartaMigrationTools { *; }
-keep class adrianmikula.jakartamigration.mcp.McpToolsConfiguration { *; }
-keep class org.springframework.** { *; }
-keep class org.springframework.ai.** { *; }

# Obfuscate premium packages
-keep,allowobfuscation class adrianmikula.jakartamigration.coderefactoring.** { *; }
-keep,allowobfuscation class adrianmikula.jakartamigration.runtimeverification.** { *; }

# Keep MCP annotations
-keep @interface org.springaicommunity.mcp.annotation.McpTool
-keep @interface org.springaicommunity.mcp.annotation.McpToolParam
```

## Migration Path for Existing Users

### Option A: Keep Both Packages Compatible
```javascript
// Free package can detect premium package
const hasPremium = require('@jakarta-migration/mcp-server-premium');

if (hasPremium) {
    // Use premium JAR
} else {
    // Use free JAR
}
```

### Option B: Clear Separation
- Free users: `npm install @jakarta-migration/mcp-server`
- Premium users: `npm install @jakarta-migration/mcp-server-premium`
- No automatic detection, clear choice

## Testing Strategy

### Free Package Tests
```bash
# Test free package
cd jakarta-migration-mcp-free
./gradlew test
npm test
```

### Premium Package Tests
```bash
# Test premium package (in private repo)
cd jakarta-migration-mcp-premium
./gradlew test
npm test
```

### Integration Tests
- Test free package independently
- Test premium package independently
- Test upgrade path (free → premium)

## Documentation Updates

### Free Package README
```markdown
# Jakarta Migration MCP Server (Free)

Free analysis tools for Jakarta migration.

## Features
- Analyze Jakarta readiness
- Detect blockers
- Recommend versions
- Analyze migration impact

## Premium Features
For automated refactoring, planning, and verification, see:
[@jakarta-migration/mcp-server-premium](https://www.npmjs.com/package/@jakarta-migration/mcp-server-premium)
```

### Premium Package README
```markdown
# Jakarta Migration MCP Server (Premium)

Premium tools for Jakarta migration including automated refactoring.

## Features
- All free features
- Automated code refactoring
- Migration planning
- Runtime verification

## License
Proprietary - See LICENSE file
```

## Timeline Estimate

- **Week 1**: Code separation, create free package
- **Week 2**: Create premium package, set up builds
- **Week 3**: Testing, documentation, npm publishing
- **Week 4**: Migration guide, user communication

## Risk Mitigation

1. **Version Coordination**: Use semantic versioning, coordinate releases
2. **Backward Compatibility**: Maintain API compatibility
3. **User Communication**: Clear migration guide, support channels
4. **Testing**: Comprehensive test coverage for both packages

