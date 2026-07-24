# Dependency Scanning Architecture Overhaul

## Problem Statement

Two core issues with the current dependency scanning architecture:

1. **Quick scan shows ~90% of dependencies as Jakarta-incompatible** — even when many actually support Jakarta
2. **Deep scan shows ~90% of dependencies with build tool errors** and fails to scan transitive dependencies

### Root Cause Analysis

**Quick Scan 90% Incompatible:**
- `SimpleNamespaceClassifier` (community-core-engine) only has **5 hardcoded Jakarta artifacts** and **4 hardcoded javax artifacts**
- Libraries like Hibernate 6.x, Jersey 3.x, RESTEasy 6.x are all classified UNKNOWN because their groupIds don't start with `jakarta.*`
- The `CompatibilityConfigLoader` exists but is never used by the quick scan path

**Deep Scan 90% Build Tool Errors:**
- When `mvn dependency:tree` or `gradle dependencies` fails for ANY reason, the catch block falls back to regex parsing and marks **every dependency** as `BUILD_TOOL_ERROR`
- Bug in `DependencyTreeCommandExecutorImpl.java:120-143`: `redirectErrorStream(true)` merges stderr into stdout, but the error handler reads from `getErrorStream()` which is always empty after `waitFor()`
- Failure triggers: no system mvn, wrapper not found, 120s timeout, build fails for any reason, non-zero exit code

## Architecture Overview

### Deep Scan Flow (revised)
```
1. Run mvn/gradle dependency:tree
2. For each dependency:
   a. Coordinate-based matching (O(1) lookup) → if result, done
   b. If UNKNOWN → ASM bytecode scanning (DefaultJarCompatibilityScanner)
   c. If build tool fails → regex scanning + balloon notification
3. Propagate transitive status to direct dependencies
```

### Quick Scan Flow
```
1. Run mvn/gradle dependency:tree
2. For each dependency: coordinate-based matching only (no JAR scanning)
```

## Implementation Plan

### Phase 1: `RecipePatternExtractor` + Integration Tests (New Code — 100%)

**Purpose:** Scrape OpenRewrite recipes from GitHub to extract coordinate mappings and package rename patterns.

**Implementation:**
- Scrape `jakarta-ee-9.yml` from GitHub raw URL (`rewrite-migrate-java`)
- Parse `ChangeDependency` blocks → coordinate mappings (groupId:artifactId → Jakarta equivalents)
- Parse `ChangePackage` blocks → package rename patterns (javax.* → jakarta.*)
- Cache to `~/.jakarta-migration/recipe-patterns.json`
- On-demand refresh: check if cache is stale (>7 days), re-scrape if needed

**Dependencies:** None (standalone service)

**Integration Test:** `RecipePatternExtractorIntegrationTest`
- `shouldExtractChangeDependencyBlocks()` — Verify coordinate mappings are extracted
- `shouldExtractChangePackageBlocks()` — Verify package rename patterns are extracted
- `shouldCacheAndReloadPatterns()` — Verify cache persistence and reload
- `shouldHandleStaleCache()` — Verify on-demand refresh when cache is >7 days old

---

### Phase 2: `RecipeBasedClassifier` + Integration Tests (New Code — 100%)

**Purpose:** Fast hybrid classification using recipe-derived patterns.

**Implementation:**
- Two-tier matching:
  1. **Fast coordinate lookup** (`Map<String, String>`) — O(1), handles 80% of cases
  2. **Full regex scan** (for UNKNOWN only) — reads JAR entries, matches package patterns
- Returns: `COMPATIBLE`, `NEEDS_REFACTORING`, `POTENTIALLY_COMPATIBLE`, `INCOMPATIBLE`, `UNKNOWN`
- Thread pool for concurrent regex scanning (configurable, default 4 threads)

**Dependencies:** `RecipePatternExtractor`

**Integration Test:** `RecipeBasedClassifierIntegrationTest`
- `shouldClassifyKnownJakartaArtifact()` — Verify `jakarta.servlet:jakarta.servlet-api:6.0.0` → COMPATIBLE
- `shouldClassifyKnownJavaxArtifact()` — Verify `javax.servlet:javax.servlet-api:4.0.1` → INCOMPATIBLE
- `shouldClassifyUnknownArtifactWithRegex()` — Verify regex matching for UNKNOWN artifacts
- `shouldClassifyHibernate6AsCompatible()` — Verify `org.hibernate.orm:hibernate-core:6.0.0` → COMPATIBLE

---

### Phase 3: Expand `SimpleNamespaceClassifier` + Integration Tests (Reuse — 80%)

**Purpose:** Fix quick scan false negatives by adding comprehensive coordinate maps.

**Current state:** Only 5 Jakarta artifacts, 4 javax artifacts hardcoded

**Changes:**
- Expand coordinate maps with data from `RecipePatternExtractor` (~40 mappings)
- Add Spring Boot/Framework version checks (already exists, needs expansion)
- Add groupId prefix heuristics from `RecipeBasedClassifier`

**File:** `community-core-engine/src/main/java/adrianmikula/jakartamigration/dependencyanalysis/service/impl/SimpleNamespaceClassifier.java`

**Integration Test:** `SimpleNamespaceClassifierExpansionIntegrationTest`
- `shouldClassifySpringBoot3Project()` — Verify Spring Boot 3.x dependencies → JAKARTA
- `shouldClassifySpringBoot2Project()` — Verify Spring Boot 2.x dependencies → JAVAX
- `shouldClassifyHibernate6Project()` — Verify Hibernate 6.x dependencies → JAKARTA
- `shouldClassifyJersey3Project()` — Verify Jersey 3.x dependencies → JAKARTA

**Repo:** `tgupta018/J2EE7Samples` (contains javax.servlet, javax.persistence, javax.ejb, javax.ws.rs imports)

---

### Phase 4: Fix Build Tool Error Fallback + Integration Tests (Reuse — 70%)

**Purpose:** Prevent BUILD_TOOL_ERROR cascade when Maven/Gradle fails.

**Changes:**
- When build tool fails, fall back to regex scanning (don't mark everything as `BUILD_TOOL_ERROR`)
- Add `BalloonNotificationService` (new class, ~50 lines) for deduplication
- Fix `redirectErrorStream(true)` bug in `DependencyTreeCommandExecutorImpl.java:120-143`

**Files:**
- `premium-core-engine/.../advancedscanning/service/impl/TransitiveDependencyScannerImpl.java`
- `premium-core-engine/.../advancedscanning/service/impl/DependencyTreeCommandExecutorImpl.java`

**Integration Test:** `TransitiveDependencyScannerImplIntegrationTest`
- `shouldFallbackToRegexOnMavenFailure()` — Verify regex fallback when Maven fails
- `shouldFallbackToRegexOnGradleFailure()` — Verify regex fallback when Gradle fails
- `shouldShowBalloonNotificationOnFailure()` — Verify balloon notification is shown (deduplicated)
- `shouldClassifyDependenciesAfterFallback()` — Verify dependencies are classified after fallback

**Repo:** `tgupta018/J2EE7Samples` (Maven project with javax imports)

---

### Phase 5: ASM/Bytecode Scanning Integration + Integration Tests (Reuse — 90%)

**Purpose:** Use existing ASM infrastructure for UNKNOWN dependencies after coordinate matching.

**Existing code to reuse:**
| Component | File | Status |
|-----------|------|--------|
| `DefaultJarCompatibilityScanner` | `premium-core-engine/.../jaranalysis/service/` | Reuse as-is |
| `BytecodeSignalExtractor` | `premium-core-engine/.../jaranalysis/service/` | Reuse as-is |
| `ScoringEngine` | `premium-core-engine/.../jaranalysis/service/` | Reuse as-is |
| `MetadataSignalExtractor` | `premium-core-engine/.../jaranalysis/service/` | Reuse as-is |
| `JarCompatibilityScanner` interface | `premium-core-engine/.../jaranalysis/service/` | Reuse as-is |
| `JarResolver` | `community-core-engine/.../dependencyanalysis/service/` | Reuse as-is |

**Duplication to consolidate:**
| Duplicated Logic | Location 1 | Location 2 | Resolution |
|------------------|------------|------------|------------|
| ASM visitor (class/field/method analysis) | `BytecodeSignalExtractor.SignalCollectingVisitor` | `AsmBytecodeAnalyzer.NamespaceDetector` | Keep `BytecodeSignalExtractor` (richer signals), delete `AsmBytecodeAnalyzer` |
| JAR scanning orchestration | `DefaultJarCompatibilityScanner` | `BytecodeNamespaceClassifier` | Keep `DefaultJarCompatibilityScanner`, simplify `BytecodeNamespaceClassifier` |

**Changes to `TransitiveDependencyScannerImpl`:**
- For each `UNKNOWN` dependency after coordinate matching, call `jarCompatibilityScanner.analyzeJar(jarPath)`
- Use existing `enrichWithJarScan()` method (already exists at line ~700)
- Add streaming JAR analysis with bounded thread pool

**Integration Test:** `BytecodeScannerIntegrationTest`
- `shouldClassifyJakartaServletJAR()` — Verify `jakarta.servlet-api-6.0.0.jar` → JAKARTA
- `shouldClassifyJavaxServletJAR()` — Verify `javax.servlet-api-4.0.1.jar` → JAVAX
- `shouldClassifyMixedNamespaceJAR()` — Verify JAR with both javax and jakarta → MIXED
- `shouldHandleLargeJAR()` — Verify scanning of large JAR (e.g., `hibernate-core-6.0.0.Final.jar`)

**Repo:** Download JARs from Maven Central (no full repo needed)

---

### Phase 6: Streaming JAR Analysis + Memory Management (New — 60%)

**Purpose:** Prevent OOM while maximizing concurrency for regex JAR scanning.

**Reuse:**
- `DefaultJarCompatibilityScanner` already has `FixedThreadPool` for parallelism
- `TransitiveDependencyScannerImpl.enrichWithJarScansBatch()` already uses `parallelStream()`

**Changes:**
- Stream JAR entries in `BytecodeSignalExtractor.extractFromJar()` (currently sequential `while` loop)
- Add bounded queue to thread pool to prevent OOM
- Add timeout per JAR scan (default 30s)
- Use try-with-resources for all stream operations

---

### Phase 7: Performance Tests (Memory/Profiling) (New Code — 100%)

**Purpose:** Create/update performance and memory tests using the `skills/performance-memory-profiling/SKILL.md` skill.

**Test Location:** `premium-core-engine/src/test/java/.../unit/...` (same package as code under test)

**Test Convention:** All tests must be tagged `@Tag("slow")` to exclude from `fastTest`.

#### Test Case 1: `RecipeBasedClassifierMemoryTest`

**Purpose:** Verify that `RecipeBasedClassifier` does not allocate excessive memory during classification.

**Test Methods:**
- `shouldClassify1000DependenciesWithinBudget()` — Verify <50MB heap for 1000 dependencies
- `shouldNotLeakMemoryOnRepeatedClassification()` — Verify no memory leak on 1000 repeated calls
- `shouldHandleLargeCoordinateMap()` — Verify <10MB heap for 10,000 coordinate mappings

**Memory Budget:** <50MB heap for 1000 dependencies

---

#### Test Case 2: `TransitiveDependencyScannerImplMemoryTest`

**Purpose:** Verify that `TransitiveDependencyScannerImpl` does not allocate excessive memory during deep scan.

**Test Methods:**
- `shouldScan500DependenciesWithinBudget()` — Verify <100MB heap for 500 dependencies
- `shouldNotLeakMemoryOnRepeatedScans()` — Verify no memory leak on 100 repeated scans
- `shouldHandleLargeDependencyTree()` — Verify <200MB heap for 1000 dependencies

**Memory Budget:** <100MB heap for 500 dependencies

---

#### Test Case 3: `BytecodeSignalExtractorPerformanceTest`

**Purpose:** Verify that `BytecodeSignalExtractor` scanning performance is acceptable.

**Test Methods:**
- `shouldScanJARWithinTimeBudget()` — Verify <5s for 10MB JAR
- `shouldScanLargeJARWithinTimeBudget()` — Verify <30s for 50MB JAR
- `shouldHandleConcurrentScanning()` — Verify 4 threads scanning 4 JARs completes in <30s

**Performance Budget:** <5s for 10MB JAR, <30s for 50MB JAR

---

#### Test Case 4: `DefaultJarCompatibilityScannerPerformanceTest`

**Purpose:** Verify that `DefaultJarCompatibilityScanner` caching and parallelism work correctly.

**Test Methods:**
- `shouldCacheResultsAndImprovePerformance()` — Verify second scan is 2x faster than first
- `shouldHandleParallelScanning()` — Verify 4 threads scanning 10 JARs completes in <60s
- `shouldNotExceedMemoryBudget()` — Verify <150MB heap for 10 JARs

**Performance Budget:** <60s for 10 JARs with 4 threads

---

#### Test Case 5: `DependencyAnalysisPipelinePerformanceTest`

**Purpose:** Verify that the full dependency analysis pipeline performs acceptably.

**Test Methods:**
- `shouldAnalyzeSmallProjectWithinBudget()` — Verify <10s for 50 dependencies
- `shouldAnalyzeMediumProjectWithinBudget()` — Verify <60s for 200 dependencies
- `shouldAnalyzeLargeProjectWithinBudget()` — Verify <300s for 500 dependencies

**Performance Budget:** <10s for 50 deps, <60s for 200 deps, <300s for 500 deps

---

### Phase 8: UI Hookup & Integration Fixes (IntelliJ Plugin — 100%)

**Purpose:** Fix the IntelliJ plugin UI so scan progress bars show real progress and dependency scan results (dependency graph + dependencies table) propagate correctly from the new core engine implementation.

**Prerequisites:** Phases 1–7 complete and all tests passing.

**Discovered Issues:**

#### Issue 1: Dependency Graph Edge Key Format Mismatch (CRITICAL)

`TransitiveDependencyEdge` uses 2-part keys (`groupId:artifactId`) via `buildParentMap()` which maps `childArtifactKey → parentArtifactKey`. However, `AdvancedScanningService.buildDependencyGraphFromDeepResult()` (lines 1237–1252) splits on `:` and requires `parts.length >= 3` (expects `groupId:artifactId:version`). **All inter-artifact edges are silently dropped** — the graph shows a flat star topology instead of the actual dependency tree.

**Fix:** Change edge parsing to handle both 2-part (`groupId:artifactId`) and 3-part (`groupId:artifactId:version`) keys. When only 2 parts are available, look up version from the usages map. Add a `usageByKey` map built from `fileResult.getUsages()` keyed by `groupId:artifactId` for version fallback.

**File:** `premium-intellij-plugin/.../service/AdvancedScanningService.java` lines 1237–1252

---

#### Issue 2: Missing Field Mappings to DependencyInfo (Data Loss)

`TransitiveDependencyUsage` exposes `severity`, `recommendation`, and `javaxPackage` fields, but `convertToDependencyInfo()` (lines 1147–1203) does not map them to `DependencyInfo`. This data is completely lost.

Additionally, only the first element of `alternativeVersions` is used; the rest are discarded.

**Fix:**
1. Add `severity`, `recommendation`, and `javaxPackage` fields to `DependencyInfo` (with getters/setters and `@JsonProperty` annotations).
2. Map them in `convertToDependencyInfo()`.
3. Pass all alternative versions (not just the first) to `DependencyInfo`.

**Files:**
- `premium-intellij-plugin/.../model/DependencyInfo.java` — add fields
- `premium-intellij-plugin/.../service/AdvancedScanningService.java` lines 1147–1203 — add mappings

---

#### Issue 3: Progress Bar Shows No/Invisible Movement

Multiple progress reporting gaps cause the scan progress bar to appear stuck:

| Gap | Root Cause | Impact |
|-----|-----------|--------|
| **Command execution** | `mvn dependency:tree` / `gradle dependencies` blocks with zero progress callbacks | 90%+ of scan time with no progress |
| **JAR scanning** | `enrichWithJarScansBatch()` runs `parallelStream()` with no listener | Seconds of invisible work |
| **Maven Central lookup** | `enrichWithMavenLookupsBatch()` runs `parallelStream()` with no listener | Network calls with no progress |
| **Empty phase names** | `""` strings passed to callbacks cause UI to show ` in progress...` | No descriptive text |
| **Double throttling** | `dispatchProgressUpdateAsync()` (100ms) + `ThrottledProgressListener` (100ms) collapse per-file callbacks to ~1/sec | Overly aggressive throttling |

**Fix:**
1. Add coarse-grained progress callbacks during command execution phases (e.g., "Executing Maven...", "Executing Gradle...").
2. Pass `ScanProgressCallback` through to `enrichWithJarScansBatch()` and `enrichWithMavenLookupsBatch()` to report sub-phase progress.
3. Replace empty phase name strings with descriptive labels (e.g., `"Scanning JARs"`, `"Looking up Maven Central"`).
4. Remove double throttling — keep `ThrottledProgressListener` as the single debounce layer, remove `dispatchProgressUpdateAsync()`.

**Files:**
- `premium-core-engine/.../advancedscanning/service/impl/TransitiveDependencyScannerImpl.java`
- `premium-intellij-plugin/.../service/AdvancedScanningService.java`

---

#### Issue 4: scanFileFallback() Drops Listener

When the regex fallback path is used (`scanFileFallback()`), the `ScanProgressListener` is not passed, so no progress is reported for the fallback scan.

**Fix:** Pass the listener through `scanFileFallback()` and emit progress callbacks during the fallback scan.

**File:** `premium-core-engine/.../advancedscanning/service/impl/TransitiveDependencyScannerImpl.java`

---

#### Issue 5: Missing "Transitive Dependencies" Case in onSubScanComplete()

`DashboardComponent.onSubScanComplete()` has a switch statement for scan types (JPA, Bean Validation, Servlet/JSP, etc.) but no case for `"Transitive Dependencies"`. It falls to the `default` log-only branch, so the transitive dependency count is never updated in the UI.

**Fix:** Add a `case "Transitive Dependencies"` branch to update the corresponding scan count label.

**File:** `premium-intellij-plugin/.../ui/DashboardComponent.java` lines 748–788

---

#### Issue 6: updateGauges() EDT Bottleneck

`DashboardComponent.updateGauges()` performs recursive filesystem walks (to count files, test files) directly on the EDT, causing 13+ second freezes. While the 44-second freeze observed in logs was partly from IntelliJ's TextMate lexer, the filesystem walks are still a real bottleneck.

**Fix:** Offload the `getTotalFileCount()` / `getTestFileCount()` filesystem walks to a background thread. Cache results and update gauges on the EDT only when ready.

**File:** `premium-intellij-plugin/.../ui/DashboardComponent.java` lines 1425–1501

---

**Verification:**
- Scan a real project and confirm the dependency graph shows parent → child edges (not a flat star)
- Confirm `severity` / `recommendation` / `javaxPackage` are populated in `DependencyInfo`
- Confirm the progress bar moves visibly during scan phases
- Confirm "Transitive Dependencies" count appears in the dashboard
- Confirm no EDT freezes during `updateGauges()`

---

### Phase 9: Cleanup & Delete Old/Legacy Code (Reuse — 100%)

**Status:** ✅ COMPLETE

**Purpose:** Remove technical debt, consolidate duplicate code, and delete old/legacy scanning code to avoid duplication and codebase complexity.

**Completed Items:**
- ✅ Removed duplicate `scanProject()` overloads from `TransitiveDependencyScannerImpl` (consolidated to single entry point)
- ✅ Removed `AsmBytecodeAnalyzer` and `BytecodeAnalyzer` references (deleted deprecated classes)
- ✅ Created shared `MavenPomParser` utility (DOM-based and regex-based POM parsing with property resolution) — `community-core-engine/.../util/MavenPomParser.java`
- ✅ Created shared `GradleBuildParser` utility (comprehensive Gradle dependency pattern + scope mapping) — `community-core-engine/.../util/GradleBuildParser.java`
- ✅ Created shared `BuildFileDiscovery` utility (consolidates build file discovery and multi-module detection) — `community-core-engine/.../util/BuildFileDiscovery.java`
- ✅ Created shared `ScopeConstants` utility (Maven scope constants + Gradle configuration-to-scope mapping) — `community-core-engine/.../util/ScopeConstants.java`
- ✅ Added `jakarta.*` groupId prefix classification to `RecipeBasedClassifier`
- ✅ Updated `TransitiveDependencyScannerImpl` to use shared utilities and `RecipeBasedClassifier` (replacing `CompatibilityConfigLoader`)
- ✅ Updated `ThirdPartyLibScannerImpl` to use shared utilities and `RecipeBasedClassifier`
- ✅ Updated `BuildConfigScannerImpl` to use shared `MavenPomParser`/`GradleBuildParser`
- ✅ Updated `MavenDependencyGraphBuilder` to use shared `MavenPomParser` and `ScopeConstants`
- ✅ Removed deprecated `CompatibilityConfigLoader` and `CompatibilityConfig`
- ✅ Updated all scanner tests to use `RecipeBasedClassifier` (deleted `CompatibilityConfigLoaderTest`)

**Duplications Resolved:**

| Duplication | Files Changed | Resolution |
|-------------|---------------|------------|
| Maven POM parsing (4 implementations) | `TransitiveDependencyScannerImpl`, `BuildConfigScannerImpl`, `ThirdPartyLibScannerImpl` | Use `MavenPomParser.parseDependencies()`/`parseDependenciesFromContent()` |
| Gradle parsing (5 implementations) | `TransitiveDependencyScannerImpl`, `BuildConfigScannerImpl`, `ThirdPartyLibScannerImpl`, `MavenDependencyGraphBuilder` | Use `GradleBuildParser.parseDependencies()` |
| Property resolution (3 implementations) | `MavenDependencyGraphBuilder` | Delegate to `MavenPomParser.buildPropertiesMap()`, `resolveProperty()` |
| Scope constants (3 locations) | `TransitiveDependencyScannerImpl`, `MavenDependencyGraphBuilder` | Use `ScopeConstants.DEFAULT_MAVEN_SCOPES`, `mapConfigurationToScope()` |
| Build file discovery (5 implementations) | `TransitiveDependencyScannerImpl`, `MavenDependencyGraphBuilder` | Use `BuildFileDiscovery.discoverBuildFiles()` |
| Multi-module detection (2 implementations) | `TransitiveDependencyScannerImpl` | Use `BuildFileDiscovery.detectMultiModuleProject()`, `findCommonProjectRoot()` |
| Dependency classification (4 systems) | `TransitiveDependencyScannerImpl`, `ThirdPartyLibScannerImpl`, `AdvancedScanningModule`, `DependenciesTableComponent` | Replace `CompatibilityConfigLoader` with `RecipeBasedClassifier` implementing `NamespaceClassifier` |

**Deleted Files:**
- `premium-core-engine/.../dependencyanalysis/config/CompatibilityConfigLoader.java` — consolidated into `RecipeBasedClassifier`
- `premium-core-engine/.../dependencyanalysis/config/CompatibilityConfig.java` — data model for deleted config loader
- `premium-core-engine/src/test/.../dependencyanalysis/config/CompatibilityConfigLoaderTest.java` — redundant after deletion

**Verification:**
- Compile check: `./gradlew :community-core-engine:compileJava :premium-core-engine:compileJava` — passes
- Fast test loop: `./gradlew :community-core-engine:fastTest :premium-core-engine:fastTest` — passes

---

### Remaining Tech Debt (Deferred)

These items were identified during Phase 9 cleanup but intentionally deferred due to high regression risk or because they operate at different abstraction levels that make forced consolidation counterproductive.

#### TD-1: javax-to-Jakarta Mapping Table Consolidation (10+ Locations)

**Status:** Deferred — requires design decision before implementation

**Problem:** At least 10 independent javax-to-jakarta mapping locations exist in production code, each maintained separately:

| # | File | Mapping Type | Scope | Entries |
|---|------|-------------|-------|---------|
| 1 | `SimpleNamespaceClassifier.java` | `groupId:artifactId` coordinate maps | Artifact classification | ~46 Jakarta + ~16 javax |
| 2 | `BuildConfigScannerImpl.java` | `groupId:artifactId` → jakarta coords | Build config migration suggestions | ~13 |
| 3 | `RecipeBasedClassifier.java` | Dynamic from OpenRewrite recipe | Coordinate + package rename | Dynamic |
| 4 | `compatibility.yaml` | Package prefix → category | Artifact categorization | ~114 lines |
| 5 | `BeanValidationScannerImpl.java` | Class-level imports | Source code scanning | ~40+ |
| 6 | `ServletJspScannerImpl.java` | Class-level imports | Source code scanning | ~60+ |
| 7 | `CdiInjectionScannerImpl.java` | Class-level imports | Source code scanning | ~25 |
| 8 | `RestSoapScannerImpl.java` | Class-level imports | Source code scanning | ~50 |
| 9 | `JpaAnnotationScannerImpl.java` | Class-level imports | Source code scanning | ~80+ |
| 10 | `ManualTasksSnippet.java` | if/else chain | PDF report generation | ~16 |
| 11 | `IntegrationPointUsage.java` | switch statement | Domain suggestions | ~5 |
| 12 | `ConfigFileScannerImpl.java` | Pattern map | Config file scanning | ~5 |
| 13 | `DeprecatedApiScannerImpl.java` | Deprecated API map | Source code scanning | ~10 |
| 14 | `JmsMessagingScannerImpl.java` | JMS API map | Source code scanning | ~10 |
| 15 | `SecurityApiScannerImpl.java` | Security API map | Source code scanning | ~10 |

**Why Deferred:**
1. **Different abstraction levels:** Items 1–4 operate on artifact coordinates/versions, items 5–9 operate on Java class-level imports, items 10–15 are miscellaneous. These are semantically different mappings that happen to cover the same domain.
2. **RecipeBasedClassifier is authoritative:** The dynamic recipe scraping (`RecipePatternExtractor`) fetches the canonical mappings from OpenRewrite. Hardcoded maps serve as fast-path caches that fall back to the recipe-based classifier. Consolidating them into a single data structure would lose the ability to have scanner-specific logic.
3. **High regression risk:** The scanner-specific maps (BeanValidation, Servlet, CDI, etc.) are fine-grained Java import mappings (e.g., `javax.servlet.http.HttpServlet` → `jakarta.servlet.http.HttpServlet`). Replacing them with a generic lookup table would require a new abstraction layer and thorough testing of every scanner.
4. **Version drift is intentional:** `BuildConfigScannerImpl.DEPENDENCY_MAPPINGS` uses older target versions (e.g., Jakarta Persistence 2.2.3) while `SimpleNamespaceClassifier` uses newer versions (e.g., 3.1.0). This reflects different migration strategies (conservative vs aggressive).

**Recommended Future Approach:**
- Create a `JakartaMappingRegistry` service that loads mappings from `compatibility.yaml` + `RecipePatternExtractor` cache
- Each scanner registers its mapping category (artifact-level, class-level, package-level)
- The registry provides a unified API for lookups with category-specific behavior
- Effort: Medium (new abstraction layer + migration of 10+ files)

---

#### TD-2: `findLineNumber` Duplication in 5 Non-BaseScanner Scanners

**Status:** Deferred — structural issue requiring interface redesign

**Problem:** Five scanner implementations duplicate the `findLineNumber()` method from `BaseScanner` because they implement their own interfaces instead of extending `BaseScanner<T>`:

| Scanner | Interface | Duplicate Methods |
|---------|-----------|-------------------|
| `ClassloaderModuleScannerImpl` | `ClassloaderModuleScanner` | `findLineNumber`, `scanFileWithTracking`, `cleanupThreadLocal`, `ThreadLocal<JavaParser>` |
| `ConfigFileScannerImpl` | `ConfigFileScanner` | `findLineNumber`, `scanFileWithTracking` |
| `DeprecatedApiScannerImpl` | `DeprecatedApiScanner` | `findLineNumber`, `scanFileWithTracking`, `discoverJavaFiles` |
| `JmsMessagingScannerImpl` | `JmsMessagingScanner` | `findLineNumber`, `scanFileWithTracking` |
| `SecurityApiScannerImpl` | `SecurityApiScanner` | `findLineNumber`, `scanFileWithTracking`, `cleanupThreadLocal` |

Additionally, `SourceCodeScannerImpl` (community-core-engine) has a variant `findLineNumberInContent()` with identical logic.

**Why Deferred:**
- Refactoring these to extend `BaseScanner<T>` requires changing their interface hierarchy and ensuring backward compatibility
- Each scanner has slightly different lifecycle requirements (some don't need `ThreadLocal<JavaParser>`)
- Risk of breaking existing scanner behavior during interface restructuring

**Recommended Future Approach:**
- Extract `findLineNumber()` and `countLines()` into a `LineNumberUtils` static utility in `community-core-engine/.../util/`
- Have both `BaseScanner` and the 5 non-BaseScanner scanners delegate to the shared utility
- Low risk, incremental migration

---

#### TD-3: `GradleMultiModuleParser` Duplicate Patterns

**Status:** Deferred — legitimate differences prevent simple replacement

**Problem:** `GradleMultiModuleParser` has its own `DEPENDENCY_PATTERN`, `parseDependencies()`, and `mapConfigurationToScope()` that partially overlap with `GradleBuildParser` and `ScopeConstants`.

**Why Deferred:**
- `GradleMultiModuleParser.parseDependencies()` handles **versionless dependencies** (BOM-managed `group:artifact` without version), **project() dependencies** (excluded), and **BOM/platform imports** (excluded) — features absent from `GradleBuildParser.parseDependencies()`
- `GradleMultiModuleParser.mapConfigurationToScope()` maps `annotationProcessor` and `kapt` to `provided`, while `ScopeConstants.mapConfigurationToScope()` maps them to `compile` — a deliberate semantic difference
- Replacing these would require extending `GradleBuildParser` with optional features, which is a larger refactoring

**Recommended Future Approach:**
- Add optional parameters to `GradleBuildParser.parseDependencies()` (e.g., `includeVersionless`, `excludeProjectDeps`)
- Add a `mapConfigurationToScope(config, strictMode)` variant to `ScopeConstants`
- Effort: Low-Medium

---

#### TD-4: `TestContainersScannerImpl` Inline Patterns

**Status:** Deferred — minimal duplication, scanner-specific patterns

**Problem:** `TestContainersScannerImpl` defines its own `MAVEN_DEP` and `GRADLE_DEP` regex patterns for extracting coordinates from build files.

**Why Deferred:**
- These patterns are intentionally simpler than `MavenPomParser`/`GradleBuildParser` — they only extract `groupId:artifactId:version` without scope information
- The scanner uses them to match against `CONTAINER_PATTERNS` (a map of container-related groupIds), not to build full dependency trees
- The duplication is ~10 lines of pattern definitions with no behavioral overlap

**Recommended Future Approach:**
- Use `GradleBuildParser.extractCoordinates()` for Gradle files (already supports simple coordinate extraction)
- Use `MavenPomParser.parseDependenciesFromContent()` for Maven files (returns full dependency maps)
- Low effort, low risk

## Summary: New vs Reuse

| Phase | New Code | Reuse Existing | Effort |
|-------|----------|----------------|--------|
| 1. RecipePatternExtractor + Tests | 100% new | None | Medium |
| 2. RecipeBasedClassifier + Tests | 100% new | None | Medium |
| 3. Expand SimpleNamespaceClassifier + Tests | 20% new | 80% existing | Low |
| 4. Fix Build Tool Error Fallback + Tests | 30% new | 70% existing | Low |
| 5. ASM/Bytecode Integration + Tests | 10% new | 90% existing | Low |
| 6. Streaming + Memory Mgmt | 60% new | 40% existing | Medium |
| 7. Performance Tests | 100% new | None | Medium |
| 8. UI Hookup & Integration Fixes | 40% new | 60% existing | Medium |
| 9. Cleanup & Delete Old/Legacy Code | 0% new | 100% existing | Low |

**Total:** ~50% new code, ~50% reuse/refactoring/deletion

## Key Files to Modify

| File | Changes | Phase |
|------|---------|-------|
| `community-core-engine/.../SimpleNamespaceClassifier.java` | Expand coordinate maps | 3 |
| `premium-core-engine/.../TransitiveDependencyScannerImpl.java` | Add ASM integration, fix fallback, remove duplicates | 4, 9 |
| `premium-core-engine/.../DependencyTreeCommandExecutorImpl.java` | Fix stderr bug | 4 |
| `premium-core-engine/src/main/resources/compatibility.yaml` | Expand mappings | 3 |
| `premium-intellij-plugin/.../service/AdvancedScanningService.java` | Fix edge key mismatch, add missing field mappings, fix progress reporting | 8 |
| `premium-intellij-plugin/.../model/DependencyInfo.java` | Add severity, recommendation, javaxPackage fields | 8 |
| `premium-intellij-plugin/.../ui/DashboardComponent.java` | Add Transitive Dependencies case, fix EDT bottleneck | 8 |

## Key Files to Create

| File | Purpose | Phase |
|------|---------|-------|
| `premium-core-engine/.../scanning/RecipePatternExtractor.java` | Scrape OpenRewrite recipes | 1 |
| `premium-core-engine/.../scanning/RecipeBasedClassifier.java` | Coordinate + regex matching | 2 |
| `premium-core-engine/.../scanning/BalloonNotificationService.java` | Deduplicated IDE notifications | 4 |
| `premium-core-engine/src/test/.../integration/RecipePatternExtractorIntegrationTest.java` | Integration test for recipe extraction | 1 |
| `premium-core-engine/src/test/.../integration/RecipeBasedClassifierIntegrationTest.java` | Integration test for classification | 2 |
| `premium-core-engine/src/test/.../integration/SimpleNamespaceClassifierExpansionIntegrationTest.java` | Integration test for expanded classifier | 3 |
| `premium-core-engine/src/test/.../integration/TransitiveDependencyScannerImplIntegrationTest.java` | Integration test for scanner | 4 |
| `premium-core-engine/src/test/.../integration/BytecodeScannerIntegrationTest.java` | Integration test for ASM scanning | 5 |
| `premium-core-engine/src/test/.../unit/.../RecipeBasedClassifierMemoryTest.java` | Memory test for classifier | 7 |
| `premium-core-engine/src/test/.../unit/.../TransitiveDependencyScannerImplMemoryTest.java` | Memory test for scanner | 7 |
| `premium-core-engine/src/test/.../unit/.../BytecodeSignalExtractorPerformanceTest.java` | Performance test for ASM | 7 |
| `premium-core-engine/src/test/.../unit/.../DefaultJarCompatibilityScannerPerformanceTest.java` | Performance test for scanner | 7 |
| `premium-core-engine/src/test/.../unit/.../DependencyAnalysisPipelinePerformanceTest.java` | Performance test for pipeline | 7 |

## Validation Criteria

After implementation:
- **Quick scan:** Should show <10% UNKNOWN (down from ~90%)
- **Deep scan:** Should show <20% BUILD_TOOL_ERROR (down from ~90%)
- **No OOM:** Should handle 100+ dependency trees without memory issues
- **Balloon notification:** Should show once per project on build tool failure
- **No regression:** All existing tests must pass after deletion of old code
- **Codebase complexity:** Reduced number of scanning-related classes and interfaces
- **Dependency graph:** Shows parent → child edges (not flat star topology)
- **Dependency info:** Severity, recommendation, and javaxPackage fields populated
- **Progress bar:** Moves visibly during all scan phases
- **Dashboard:** Transitive dependency count displayed
- **EDT responsiveness:** No freezes during gauge updates
