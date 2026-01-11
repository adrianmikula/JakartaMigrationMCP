# Dual Package Architecture Analysis

## Overview

This document analyzes the viability of splitting the Jakarta Migration MCP project into two npm packages:
1. **Open-source package** (`@jakarta-migration/mcp-server`) - Free features
2. **Closed-source package** (`@jakarta-migration/mcp-server-premium`) - Premium features

## Current Architecture

### Current Structure
```
@jakarta-migration/mcp-server (npm package)
├── index.js (Node.js wrapper)
├── package.json
└── Downloads JAR from GitHub releases
    └── jakarta-migration-mcp-{version}.jar (single JAR with all features)
        ├── Free tools (analysis)
        ├── Premium tools (refactoring, verification, planning)
        └── License validation (runtime checks)
```

### Current Distribution
- **Single JAR**: Contains all Java code (free + premium)
- **Runtime License Checks**: Premium features check license at runtime
- **Open Source**: Entire Java codebase is visible in GitHub
- **IP Risk**: Premium algorithms and business logic are exposed

## Proposed Architecture Options

### Option 1: Separate JARs with Separate npm Packages

```
@jakarta-migration/mcp-server (open-source)
├── index.js
├── package.json (MIT/BSD license)
└── Downloads: jakarta-migration-mcp-free-{version}.jar
    └── Only free tools (analysis)

@jakarta-migration/mcp-server-premium (closed-source)
├── index.js
├── package.json (Proprietary license)
└── Downloads: jakarta-migration-mcp-premium-{version}.jar
    └── Premium tools (refactoring, verification, planning)
    └── License validation
```

**Integration Strategy:**
- Premium package can depend on free package
- Premium JAR includes all free functionality (no duplication)
- Users install either:
  - `npm install @jakarta-migration/mcp-server` (free only)
  - `npm install @jakarta-migration/mcp-server-premium` (includes free + premium)

### Option 2: Plugin Architecture

```
@jakarta-migration/mcp-server (open-source)
├── index.js
├── Core MCP server
└── Downloads: jakarta-migration-mcp-core-{version}.jar
    └── Free tools + plugin interface

@jakarta-migration/mcp-server-premium (closed-source)
├── Premium plugin JAR
└── Downloads: jakarta-migration-mcp-premium-plugin-{version}.jar
    └── Premium tools as plugin
```

**Integration Strategy:**
- Free package provides plugin interface
- Premium package loads as plugin at runtime
- More complex but allows dynamic loading

### Option 3: Monorepo with Conditional Builds

```
jakarta-migration-mcp (monorepo)
├── packages/
│   ├── mcp-server-free/ (open-source)
│   │   └── Builds: free JAR
│   └── mcp-server-premium/ (private repo)
│       └── Builds: premium JAR (includes free)
```

**Integration Strategy:**
- Shared code in common module
- Free package builds only free features
- Premium package builds all features (free + premium)
- Premium package in private repository

## Detailed Analysis

### Option 1: Separate JARs (Recommended)

#### ✅ **Advantages**

1. **Clear Separation**
   - Free code remains open-source
   - Premium code is completely closed-source
   - Clear boundaries for IP protection

2. **Simple Distribution**
   - Two independent npm packages
   - Users choose which to install
   - Premium package can depend on free package

3. **IP Protection**
   - Premium JAR is compiled bytecode only
   - No source code exposure
   - Obfuscation possible (ProGuard, etc.)

4. **Build Simplicity**
   - Two separate Gradle builds
   - Free build: public GitHub Actions
   - Premium build: private CI/CD

5. **User Experience**
   ```bash
   # Free users
   npm install @jakarta-migration/mcp-server
   
   # Premium users
   npm install @jakarta-migration/mcp-server-premium
   ```

#### ❌ **Challenges**

1. **Code Duplication**
   - Premium JAR must include free functionality
   - Or premium depends on free JAR (complexity)
   - Shared dependencies need management

2. **Build Complexity**
   - Two separate Gradle projects
   - Shared dependencies management
   - Version synchronization

3. **Distribution**
   - Two GitHub releases to manage
   - Two npm packages to publish
   - Version coordination

4. **Testing**
   - Need to test both packages
   - Integration testing complexity
   - Premium package testing in private CI

#### 📋 **Implementation Steps**

1. **Split Java Codebase**
   ```
   jakarta-migration-mcp-free/
   ├── build.gradle.kts
   └── src/main/java/
       ├── dependencyanalysis/ (free)
       ├── sourcecodescanning/ (free)
       └── mcp/
           └── JakartaMigrationTools.java (free tools only)

   jakarta-migration-mcp-premium/ (private repo)
   ├── build.gradle.kts
   └── src/main/java/
       ├── coderefactoring/ (premium)
       ├── runtimeverification/ (premium)
       ├── config/ (license validation)
       └── mcp/
           └── JakartaMigrationTools.java (all tools)
   ```

2. **Create Separate npm Packages**
   ```json
   // package.json (free)
   {
     "name": "@jakarta-migration/mcp-server",
     "version": "1.0.0",
     "main": "index.js"
   }

   // package.json (premium)
   {
     "name": "@jakarta-migration/mcp-server-premium",
     "version": "1.0.0",
     "main": "index.js",
     "dependencies": {
       "@jakarta-migration/mcp-server": "^1.0.0"
     }
   }
   ```

3. **Build Process**
   - Free: Public GitHub Actions → Public JAR → Public npm
   - Premium: Private CI/CD → Private JAR → Private npm (or public npm with proprietary license)

### Option 2: Plugin Architecture

#### ✅ **Advantages**

1. **Modular Design**
   - Clean separation of concerns
   - Premium features as optional plugin
   - Easier to add more plugins later

2. **Dynamic Loading**
   - Load premium features only if available
   - Graceful degradation if premium not installed

#### ❌ **Challenges**

1. **Complexity**
   - Plugin interface design
   - Class loading complexity
   - Spring Boot plugin system

2. **IP Protection**
   - Plugin JAR still needs protection
   - Plugin interface exposes some internals

3. **Development Overhead**
   - More complex architecture
   - Plugin versioning
   - Compatibility management

### Option 3: Monorepo with Conditional Builds

#### ✅ **Advantages**

1. **Shared Code**
   - Common dependencies in one place
   - Easier to maintain consistency
   - Single source of truth for shared code

2. **Version Management**
   - Coordinated versioning
   - Shared dependencies

#### ❌ **Challenges**

1. **Repository Management**
   - Premium code in private repo
   - Free code in public repo
   - Complex monorepo setup

2. **Build Complexity**
   - Conditional compilation
   - Feature flags at build time
   - More complex build scripts

## IP Protection Effectiveness

### Current Risk (Single Package)
- ❌ **High Risk**: All source code visible in GitHub
- ❌ **Premium algorithms exposed**: Refactoring logic, verification strategies
- ❌ **Business logic visible**: License validation, payment integration

### With Separate Packages (Option 1)
- ✅ **Low Risk**: Premium JAR is compiled bytecode only
- ✅ **Algorithms protected**: No source code in premium package
- ✅ **Obfuscation possible**: Can use ProGuard/R8 to obfuscate bytecode
- ⚠️ **Decompilation risk**: Java bytecode can be decompiled, but:
  - Decompiled code is harder to understand
  - Obfuscation makes it much harder
  - Legal protection via license terms

### Additional Protection Strategies

1. **Bytecode Obfuscation**
   ```gradle
   // build.gradle.kts (premium)
   plugins {
       id("com.github.johnrengelman.shadow") version "8.1.1"
   }
   
   // Use ProGuard or R8 for obfuscation
   ```

2. **License Enforcement**
   - Runtime license validation
   - Online license checks
   - Hardware fingerprinting (optional)

3. **Legal Protection**
   - Proprietary license terms
   - EULA with restrictions
   - Copyright notices

## Distribution Strategy

### Free Package Distribution
```
GitHub (Public)
├── Source code (open-source)
├── Releases/
│   └── jakarta-migration-mcp-free-{version}.jar
└── npm: @jakarta-migration/mcp-server
```

### Premium Package Distribution
```
Option A: Public npm with Proprietary License
├── npm: @jakarta-migration/mcp-server-premium
├── License: Proprietary (closed-source)
└── JAR: Compiled bytecode only

Option B: Private npm Registry
├── Private npm registry
├── Requires authentication
└── JAR: Compiled bytecode only

Option C: Direct Download
├── Website/store
├── License key required
└── JAR: Compiled bytecode only
```

## User Experience Considerations

### Installation Scenarios

**Scenario 1: Free User**
```bash
npm install @jakarta-migration/mcp-server
# Gets free JAR with analysis tools only
```

**Scenario 2: Premium User**
```bash
npm install @jakarta-migration/mcp-server-premium
# Gets premium JAR with all tools (free + premium)
# Or depends on free package and adds premium
```

**Scenario 3: Upgrade Path**
```bash
# User starts with free
npm install @jakarta-migration/mcp-server

# Later upgrades to premium
npm uninstall @jakarta-migration/mcp-server
npm install @jakarta-migration/mcp-server-premium
```

### Configuration Compatibility
- Both packages use same config file format
- License key works for both
- MCP configuration compatible

## Implementation Complexity

### Low Complexity (Option 1 - Separate JARs)
- **Effort**: Medium (2-3 weeks)
- **Risk**: Low
- **Maintenance**: Medium (two builds to maintain)

### Medium Complexity (Option 2 - Plugin)
- **Effort**: High (4-6 weeks)
- **Risk**: Medium
- **Maintenance**: High (plugin system complexity)

### High Complexity (Option 3 - Monorepo)
- **Effort**: High (4-6 weeks)
- **Risk**: High (repo management)
- **Maintenance**: Medium

## Recommendations

### ✅ **Recommended: Option 1 (Separate JARs)**

**Rationale:**
1. **Best IP Protection**: Premium code completely closed-source
2. **Simple Architecture**: Clear separation, easy to understand
3. **Good User Experience**: Simple installation, clear upgrade path
4. **Manageable Complexity**: Two builds, but straightforward
5. **Proven Pattern**: Common in enterprise software

### Implementation Plan

**Phase 1: Code Separation (Week 1-2)**
1. Create `jakarta-migration-mcp-free` project
2. Move free tools to free project
3. Create `jakarta-migration-mcp-premium` project (private repo)
4. Move premium tools to premium project
5. Premium project includes free functionality

**Phase 2: Build & Distribution (Week 2-3)**
1. Set up free package build (public CI)
2. Set up premium package build (private CI)
3. Create free npm package
4. Create premium npm package
5. Set up release automation

**Phase 3: Testing & Documentation (Week 3)**
1. Test both packages independently
2. Test upgrade path
3. Update documentation
4. Migration guide for existing users

### Alternative: Hybrid Approach

If full separation is too complex initially:

1. **Keep current structure** but:
   - Obfuscate premium JAR
   - Use proprietary license for premium JAR
   - Keep free code open-source

2. **Gradually migrate** to separate packages:
   - Start with free package
   - Add premium package later
   - Maintain backward compatibility

## Risk Assessment

### Technical Risks
- **Low**: Code separation is straightforward
- **Medium**: Build complexity (manageable)
- **Low**: Distribution complexity (standard npm)

### Business Risks
- **Low**: User confusion (clear documentation)
- **Medium**: Support complexity (two packages)
- **Low**: IP protection (significantly improved)

### Legal Risks
- **Low**: License compliance (clear licenses)
- **Low**: Copyright protection (proprietary license)

## Conclusion

**Splitting into two packages is VIABLE and RECOMMENDED** for IP protection.

**Best Approach**: Option 1 (Separate JARs)
- Clear separation of free and premium
- Good IP protection
- Manageable complexity
- Good user experience

**Next Steps**:
1. Review this analysis
2. Decide on approach
3. Create implementation plan
4. Begin Phase 1 (code separation)

