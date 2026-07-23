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
- Add groupId prefix heuristics from `CompatibilityConfigLoader`

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
| ASM visitor (class/field/method analysis) | `BytecodeSignalExtractor.SignalCollectingVisitor` | `AsmBytecodeAnalyzer.NamespaceDetector` | Keep `BytecodeSignalExtractor` (richer signals), deprecate `AsmBytecodeAnalyzer` |
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

### Phase 8: Cleanup & Delete Old/Legacy Code (Reuse — 100%)

**Purpose:** Remove technical debt, consolidate duplicate code, and delete old/legacy scanning code to avoid duplication and codebase complexity.

**Prerequisites:** All previous phases must be complete and all tests passing.

**Changes:**
- Remove 3 duplicate `scanProject()` overloads from `TransitiveDependencyScannerImpl` (consolidate to single entry point)
- Fix `redirectErrorStream(true)` bug in `DependencyTreeCommandExecutorImpl.java`
- Expand `compatibility.yaml` with comprehensive coordinate maps
- Remove dead code identified in tech debt docs

**Code to delete:**
| File/Class | Reason |
|------------|--------|
| `AsmBytecodeAnalyzer.java` (runtimeverification package) | Duplicates `BytecodeSignalExtractor` logic; keep only `BytecodeSignalExtractor` |
| `BytecodeAnalyzer.java` interface (runtimeverification package) | Interface for deleted `AsmBytecodeAnalyzer` |
| Duplicate `scanProject()` overloads in `TransitiveDependencyScannerImpl` | Consolidated to single entry point |
| Dead code identified in tech debt docs | See `docs/techdebt/dependency-scanner-duplication.md` |

**Code to simplify (not delete):**
| File/Class | Change |
|------------|--------|
| `BytecodeNamespaceClassifier.java` | Simplify to delegate to `RecipeBasedClassifier` + `DefaultJarCompatibilityScanner` without redundant logic |
| `CompatibilityConfigLoader.java` | Keep but consolidate coordinate maps with `RecipePatternExtractor` data |

**Verification before deletion:**
- Run full test suite: `mise run test`
- Run fast test loop: `mise run fast-test`
- Verify quick scan shows <10% UNKNOWN
- Verify deep scan shows <20% BUILD_TOOL_ERROR
- Verify no OOM with 100+ dependency trees
- Manual testing: run quick scan and deep scan on a real project

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
| 8. Cleanup & Delete Old/Legacy Code | 0% new | 100% existing | Low |

**Total:** ~50% new code, ~50% reuse/refactoring/deletion

## Key Files to Modify

| File | Changes |
|------|---------|
| `community-core-engine/.../SimpleNamespaceClassifier.java` | Expand coordinate maps |
| `premium-core-engine/.../TransitiveDependencyScannerImpl.java` | Add ASM integration, fix fallback, remove duplicates |
| `premium-core-engine/.../DependencyTreeCommandExecutorImpl.java` | Fix stderr bug |
| `premium-core-engine/src/main/resources/compatibility.yaml` | Expand mappings |
| `premium-core-engine/.../jaranalysis/classifier/BytecodeNamespaceClassifier.java` | Simplify to delegate to new scanning infrastructure |

## Key Files to Create

| File | Purpose |
|------|---------|
| `premium-core-engine/.../scanning/RecipePatternExtractor.java` | Scrape OpenRewrite recipes |
| `premium-core-engine/.../scanning/RecipeBasedClassifier.java` | Coordinate + regex matching |
| `premium-core-engine/.../scanning/BalloonNotificationService.java` | Deduplicated IDE notifications |
| `premium-core-engine/src/test/.../integration/RecipePatternExtractorIntegrationTest.java` | Integration test for recipe extraction |
| `premium-core-engine/src/test/.../integration/RecipeBasedClassifierIntegrationTest.java` | Integration test for classification |
| `premium-core-engine/src/test/.../integration/SimpleNamespaceClassifierExpansionIntegrationTest.java` | Integration test for expanded classifier |
| `premium-core-engine/src/test/.../integration/TransitiveDependencyScannerImplIntegrationTest.java` | Integration test for scanner |
| `premium-core-engine/src/test/.../integration/BytecodeScannerIntegrationTest.java` | Integration test for ASM scanning |
| `premium-core-engine/src/test/.../unit/.../RecipeBasedClassifierMemoryTest.java` | Memory test for classifier |
| `premium-core-engine/src/test/.../unit/.../TransitiveDependencyScannerImplMemoryTest.java` | Memory test for scanner |
| `premium-core-engine/src/test/.../unit/.../BytecodeSignalExtractorPerformanceTest.java` | Performance test for ASM |
| `premium-core-engine/src/test/.../unit/.../DefaultJarCompatibilityScannerPerformanceTest.java` | Performance test for scanner |
| `premium-core-engine/src/test/.../unit/.../DependencyAnalysisPipelinePerformanceTest.java` | Performance test for pipeline |

## Key Files to Delete

| File | Reason |
|------|--------|
| `premium-core-engine/.../runtimeverification/service/impl/AsmBytecodeAnalyzer.java` | Duplicates `BytecodeSignalExtractor` logic |
| `premium-core-engine/.../runtimeverification/service/BytecodeAnalyzer.java` | Interface for deleted `AsmBytecodeAnalyzer` |

## Validation Criteria

After implementation:
- **Quick scan:** Should show <10% UNKNOWN (down from ~90%)
- **Deep scan:** Should show <20% BUILD_TOOL_ERROR (down from ~90%)
- **No OOM:** Should handle 100+ dependency trees without memory issues
- **Balloon notification:** Should show once per project on build tool failure
- **No regression:** All existing tests must pass after deletion of old code
- **Codebase complexity:** Reduced number of scanning-related classes and interfaces
