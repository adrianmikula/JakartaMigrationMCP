# Jakarta Compatibility Detection Improvements Roadmap

**Date:** 2026-07-27
**Status:** Draft / Implemented baseline

## Current state

The baseline implementation now supports dynamic, bytecode-driven Jakarta compatibility detection:

- `RecipeBasedClassifier` falls back to `DefaultJarCompatibilityScanner` / `BytecodeSignalExtractor` instead of a hardcoded six-package regex scan.
- `JarScanSignal` exposes detected `javaxPackages` and `jakartaPackages`.
- `ImprovedMavenCentralLookupService` can use an OpenRewrite `packageRenameMap` to search Maven Central by `jakarta.*` package groups.
- `TransitiveDependencyScannerImpl` uses `BytecodeNamespaceClassifier` by default and passes detected packages into the lookup service.
- `AdvancedScanningModule` wires the `packageRenameMap` lookup into the scanner pipeline.
- Unit tests cover package-prefix extraction and mocked package-rename lookup.

## Goals

1. Reduce false negatives for libraries that use `javax.*` APIs but are not in the hardcoded recipe coordinate map.
2. Reduce false positives and improve confidence of inferred Jakarta equivalents.
3. Keep scan latency acceptable for large classpaths and multi-module builds.
4. Make the system robust to network failures, Maven Central rate limits, and stale recipe data.

---

## 1. Performance and scalability

### 1.1 Cache the OpenRewrite package rename map
**Problem:** `RecipePatternExtractor.getPatterns().toPackageRenameMap()` is rebuilt every time `AdvancedScanningModule` is instantiated, even though `~/.jakarta-migration/recipe-patterns.json` rarely changes.
**Improvement:** Load and parse the file once per JVM, or warm the cache in the plugin `ApplicationComponent` / `ProjectService` lifecycle. Invalidate on file modification or after a configurable TTL.
**Benefit:** Lower I/O and faster construction of `AdvancedScanningModule`.

### 1.2 Batch Maven Central requests
**Problem:** `ImprovedMavenCentralLookupService` currently performs one `HttpClient.send` per search strategy / package. With several fallbacks, an unknown dependency can generate 6-10 sequential requests.
**Improvement:** Combine independent searches into a small set of batched or parallel requests (respecting Maven Central's single-query semantics) and deduplicate query candidates before issuing them. Introduce a bounded `Executor` instead of relying on `ForkJoinPool.commonPool()`.
**Benefit:** Fewer network round trips and more predictable latency.

### 1.3 Set class limits and early termination in the deep scanner
**Problem:** `BytecodeNamespaceClassifier` and `DefaultJarCompatibilityScanner` may scan large JARs fully even after they already have enough signal.
**Improvement:** Add a target like `maxClassesToScan` and a `stopAfterSufficientSignal` mode. If `MIXED` or `JAVAX` has been proven, further class reading can be skipped.
**Benefit:** Faster fallback classification of big transitive dependencies.

---

## 2. Accuracy and confidence

### 2.1 Confidence scoring for package-rename matches
**Problem:** Matches found through package-rename search currently receive a fixed 0.7 confidence in `TransitiveDependencyScannerImpl`.
**Improvement:** Compute a score from:
- Exactness of `jakarta` package prefix match,
- Similarity between original and candidate artifact names,
- Version recency and release cadence from Maven Central metadata,
- Whether the candidate's POM declares the expected `jakarta.*` package as `Export-Package` or similar.
**Benefit:** Users get ranked recommendations and can choose a minimum-confidence threshold.

### 2.2 Expand bytecode signal detection
**Problem:** `BytecodeSignalExtractor` only inspects class internal names in superclasses, interfaces, field/method descriptors, and generic signatures.
**Improvement:** Also scan:
- Method body instructions (LDC strings, `invokedynamic`, `MethodHandle` constants),
- Annotation attribute arrays and default values,
- `ConstantValue` attributes that may contain `javax.` class names as strings.
**Benefit:** Detect reflection-based usage and string-based `Class.forName` calls.

### 2.3 Shaded/repackaged package detection
**Problem:** An artifact can contain `javax.servlet` classes under `com.example.shaded.javax.servlet`, which is not necessarily a migration concern.
**Improvement:** Distinguish relocated packages from actual top-level `javax.*`/`jakarta.*` API references. Track whether a `javax.` package appears as a class's own package or only as an internal path component.
**Benefit:** Fewer false positives from fat JARs and relocated dependencies.

### 2.4 OSGi and JPMS metadata
**Problem:** Some artifacts declare namespace information in `MANIFEST.MF` (`Bundle-SymbolicName`, `Import-Package`, `Export-Package`) or `module-info.class`.
**Improvement:** Read these headers during JAR scanning and feed them into the classifier as additional evidence for `JAVAX` vs `JAKARTA`.
**Benefit:** Faster classification for well-modularized libraries without reading all classes.

---

## 3. Resilience and offline support

### 3.1 Local fallback index for Maven Central
**Problem:** When Maven Central is unavailable or rate-limited, package-rename lookup cannot make recommendations.
**Improvement:** Ship a compact local index of known `javax -> jakarta` group/artifact mappings (built from the OpenRewrite recipes and a curated dataset). Look up locally before hitting the network.
**Benefit:** More reliable results in air-gapped or high-latency environments.

### 3.2 Cache and TTL for lookup results
**Problem:** The same unknown coordinate may be looked up repeatedly across scans.
**Improvement:** Add an in-memory cache keyed by `groupId:artifactId:version` to `ImprovedMavenCentralLookupService`, with a configurable TTL and respect for `MavenCentralLookupConfiguration`.
**Benefit:** Avoid duplicate network calls and improve scan times for repeated runs.

### 3.3 Graceful degradation on lookup failure
**Problem:** `findJakartaEquivalents(...).get()` can block or throw if the network is down; current code logs a warning and returns `Optional.empty()`.
**Improvement:** Define a dedicated `LookupResult` type with states `FOUND`, `NOT_FOUND`, `TIMEOUT`, `OFFLINE`, and `ERROR`. Surface the state in `TransitiveDependencyUsage` so the UI can show appropriate messaging.
**Benefit:** Better UX and clearer debugging.

---

## 4. Testing and quality gaps

### 4.1 Full `RecipeBasedClassifier` JAR-fallback integration test
**Need:** A test that starts with an unknown coordinate and a real JAR containing `javax.xml.rpc` references, then asserts the classifier returns `JAVAX`.
**Approach:** Use `TestJarBuilder` to produce a JAR and either point `DefaultJarCompatibilityScanner.resolveJar` to it or test the `analyzeJar` integration directly.

### 4.2 Real Maven Central integration test for `org.apache.axis`
**Need:** Extend `JakartaLookupVerificationTest` to call `findJakartaEquivalents("org.apache.axis", "axis", Set.of("javax.xml.rpc"))` and verify that a `jakarta.xml.rpc` candidate is found.
**Consideration:** This requires network access, so it belongs in `slowTest` and should be guarded by a network-enabled test profile.

### 4.3 End-to-end `TransitiveDependencyScannerImpl` flow test
**Need:** Build a minimal Gradle project with a `javax.*` dependency that is not in the recipe map, run `scanProject`, and assert:
- The dependency is classified as `BYTECODE_SCAN_JAVAX`,
- The `ScanReason` is updated to `MAVEN_LOOKUP_FOUND` with a `jakarta.*` recommendation.
**Approach:** Use the existing temporary-project test harness and mock Maven Central responses where possible.

### 4.4 `BytecodeNamespaceClassifier` mixed and unknown scenarios
**Need:** Tests for the fast classifier returning `UNKNOWN` and the deep scanner producing `MIXED` or `UNKNOWN` results.
**Approach:** Provide a stub `NamespaceClassifier` in `BytecodeNamespaceClassifier` tests and verify the fallback branch.

### 4.5 `AdvancedScanningModule` wiring test
**Need:** Verify that the module constructs `TransitiveDependencyScannerImpl` with `BytecodeNamespaceClassifier` and an `ImprovedMavenCentralLookupService` that contains the package rename map.
**Approach:** Light constructor assertion test in `premium-core-engine`.

### 4.6 Executor and resource cleanup tests
**Need:** Ensure `DefaultJarCompatibilityScanner` and `BytecodeNamespaceClassifier` shut down their thread pools and close resources deterministically.
**Approach:** Add `@AfterEach` checks that the executor is terminated and no leaked threads are reported.

---

## 5. Product and UI

### 5.1 Fast scan and deep scan action buttons
**Idea:** Provide two explicit scan actions in the UI (`DashboardComponent` / `MigrationToolWindow`) instead of a single scan or feature flag.
- **Fast scan:** Skips bytecode scanning and uses the coordinate/prefix maps only. Results are shown with a low confidence level (e.g., 0.4) in the dependencies results table.
- **Deep scan:** Runs `BytecodeNamespaceClassifier` / `DefaultJarCompatibilityScanner`, performs package-rename Maven Central lookup, and produces high-confidence results (e.g., 0.9+).

### 5.2 Confidence column in the dependencies results table
**Idea:** Add a `Confidence` column to `DependenciesTableComponent` and `DashboardComponent` results. The column reflects whether the recommendation came from a fast lookup or a deep bytecode-backed scan. This makes the trade-off visible to the user and supports sorting/filtering later.

### 5.3 Show package-rename rationale in the UI
**Idea:** When a recommendation comes from package-rename lookup (typically during a deep scan), display the detected `javax.*` packages and the mapped `jakarta.*` group so users understand *why* a particular Jakarta equivalent was suggested.

---

## Milestones (proposed)

| Milestone | Scope | Priority |
|-----------|-------|----------|
| M1 | Caching package-rename map and TTL cache for Maven Central lookups | High |
| M2 | Real integration tests for `axis` and end-to-end `TransitiveDependencyScannerImpl` | High |
| M3 | Confidence scoring for package-rename hits | Medium |
| M4 | Shaded/repackaged package detection and expanded bytecode signal | Medium |
| M5 | OSGi / JPMS metadata support and local fallback index | Low |
| M6 | UI fast/deep scan buttons, confidence column, and rationale display | Low |
