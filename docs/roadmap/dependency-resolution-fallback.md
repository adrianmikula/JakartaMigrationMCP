# Dependency Resolution Fallback Strategy

## Problem

The premium IntelliJ plugin and MCP server use build-tool-based dependency resolution
(`mvn dependency:tree`, `gradle dependencies`) to obtain accurate, resolved dependency
graphs with correct versions. When Maven or Gradle are unavailable (e.g. in a Docker
container, CI environment, or fresh machine without build tools installed), this
command-based approach fails.

The plugin currently falls back to naive regex parsing of build files, which does not
handle:
- Gradle version catalogs (`libs.*` references)
- BOM platforms (`platform(...)`)
- Property-based versions (`${springVersion}`)
- Transitive dependencies

## Fallback Architecture

```
┌─────────────────────────────────────────────────────────────┐
│  PremiumDependencyGraphBuilder (premium-core-engine)        │
│  1. Try Maven dependency:tree / Gradle dependencies         │
│  2. If build tools unavailable →                            │
│     MavenDependencyGraphBuilder (community-core-engine)     │
│     - Regex parses build files                                │
│     - Loads version catalogs (libs.versions.toml)            │
│     - Skips platforms/project refs                            │
│  3. If regex parsing also fails →                           │
│     DirectoryCrawlerDependencyGraphBuilder                    │
│     - Scans for JAR files in directory tree                  │
└─────────────────────────────────────────────────────────────┘
```

## Current Status

| Layer | Implementation | Status |
|-------|---------------|--------|
| Command-based resolver | `DependencyTreeCommandExecutorImpl` | ✅ Working |
| Premium graph builder | `PremiumDependencyGraphBuilder` | ✅ Created |
| Premium analysis module | `PremiumDependencyAnalysisModule` | ✅ Created |
| Community regex parser | `MavenDependencyGraphBuilder` | ✅ Enhanced for version catalogs |
| Community classifier | `SimpleNamespaceClassifier` | ✅ Fixed unknown-version handling |
| Directory crawler | `DirectoryCrawlerDependencyGraphBuilder` | ✅ Existing fallback |

## Roadmap Items

### Short Term (v1.1)
- [x] Move command-based resolver to `premium-core-engine`
- [x] Create `PremiumDependencyGraphBuilder` with build-tool → regex → crawler fallback
- [x] Create `PremiumDependencyAnalysisModule` wired into premium MCP server
- [x] Fix `SimpleNamespaceClassifier` to treat unresolvable Spring Boot versions as `UNKNOWN`
- [x] Fix `MavenDependencyGraphBuilder` to parse `gradle/libs.versions.toml`

### Medium Term (v1.2)
- [ ] Add Gradle Kotlin DSL `plugins` block version extraction (for `id("...") version "..."`)
- [ ] Add Maven BOM import resolution from `dependencyManagement` imports
- [ ] Cache version catalog parsing results per project for repeated scans

### Long Term (v2.0)
- [ ] Embed a lightweight Maven/Gradle resolver (e.g. Aether) to resolve dependencies
  without requiring the user to have Maven/Gradle installed
- [ ] Pre-computed dependency metadata database for common open-source libraries
  to avoid runtime resolution entirely

## Testing

Verify fallback chain works by:
1. Running MCP server in an environment WITHOUT `mvn`/`gradle` on PATH
2. Scanning a Gradle project with `libs.versions.toml`
3. Confirming `PremiumDependencyGraphBuilder` falls back to `MavenDependencyGraphBuilder`
4. Confirming `SimpleNamespaceClassifier` returns `UNKNOWN` for unresolvable Spring Boot starters
5. Confirming no `NO_JAKARTA_EQUIVALENT` false-positive blockers are created
