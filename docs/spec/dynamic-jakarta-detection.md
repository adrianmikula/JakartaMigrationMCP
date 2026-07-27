# Dynamic Jakarta Compatibility Detection

**Date:** 2026-07-27

## Goal

Reduce reliance on hardcoded mappings by using the existing bytecode scanner and OpenRewrite package rename patterns to classify and recommend Jakarta equivalents for in-the-wild libraries.

## Requirements

1. **Bytecode-aware fallback classification**
   - `RecipeBasedClassifier` must fall back to `DefaultJarCompatibilityScanner` / `BytecodeSignalExtractor` when coordinate/prefix classification returns `UNKNOWN`.
   - The fallback must detect *any* `javax.*` / `jakarta.*` package usage, not only the six hardcoded packages.
   - `BytecodeNamespaceClassifier` (or equivalent deep classification) must become the default `NamespaceClassifier` for `TransitiveDependencyScannerImpl`.

2. **Package-rename-driven Maven Central lookup**
   - `ImprovedMavenCentralLookupService` must accept a set of detected `javax` package names.
   - When coordinate-based lookup fails, it must map each `javax` package to the corresponding `jakarta` package using OpenRewrite `packageRenameMap` and search Maven Central for artifacts in the `jakarta` package group.
   - `TransitiveDependencyScannerImpl` must pass the packages detected by the bytecode scanner to the lookup.

3. **Correct scan reasons**
   - JAR-based classifications must produce the appropriate `ScanReason` (`BYTECODE_SCAN_JAVAX`, `BYTECODE_SCAN_JAKARTA`, `BYTECODE_SCAN_MIXED`, `BYTECODE_SCAN_UNKNOWN`).
   - `TransitiveDependencyScannerImpl` must use the new reasons and avoid duplicate JAR scans.

## Acceptance criteria

- `RecipeBasedClassifier` with an unavailable coordinate but a JAR containing `javax.xml.rpc` must return `JAVAX`.
- `ImprovedMavenCentralLookupService.findJakartaEquivalents("org.apache.axis", "axis", Set.of("javax.xml.rpc"))` must attempt a `jakarta.xml.rpc` group search.
- `TransitiveDependencyScannerImpl` default constructor uses `BytecodeNamespaceClassifier` (or a `RecipeBasedClassifier` with a real `DefaultJarCompatibilityScanner`).
- Existing fast tests still pass.

## Testing gaps and future improvements

### Immediate test coverage gaps

- **No full `RecipeBasedClassifier` JAR-fallback integration test.** There is no test that gives the classifier an unknown coordinate and a real JAR containing `javax.xml.rpc` and verifies the result is `JAVAX`.
- **`ImprovedMavenCentralLookupService` package-rename test is mocked only.** The mocked `HttpClient` confirms the `jakarta.xml.rpc` group search is attempted, but a real network integration test with `JakartaLookupVerificationTest` for `org.apache.axis` is still needed.
- **`TransitiveDependencyScannerImpl` end-to-end flow is not tested.** There is no integration test that exercises the complete pipeline: unknown dependency → bytecode scan → `BYTECODE_SCAN_JAVAX` reason → Maven Central package-rename lookup → updated `TransitiveDependencyUsage` recommendation.
- **`BytecodeNamespaceClassifier` mixed/unknown scenarios are lightly covered.** Missing tests where a fast classifier returns `UNKNOWN` and the deep scanner returns `MIXED` or `UNKNOWN`.
- **`AdvancedScanningModule` wiring is untested.** No test verifies that the module injects `BytecodeNamespaceClassifier` and the `packageRenameMap` into `ImprovedMavenCentralLookupService`.
- **`JarScanSignal` package arrays are not validated in `DefaultJarCompatibilityScanner` tests.** The unit tests around `JarScanSignal` and `JarCompatibilityReport` should assert `javaxPackages` and `jakartaPackages` are passed through correctly.

### Performance and resilience gaps

- **No performance benchmark for `DefaultJarCompatibilityScanner` fallback.** Should add a slow/perf test measuring scan time for JARs with many classes to catch regressions in `BytecodeSignalExtractor`.
- **Thread-pool cleanup is not verified.** `DefaultJarCompatibilityScanner` uses an executor; there is no test ensuring shutdown occurs without leaked threads.
- **No test for offline/cache fallback.** Maven Central lookups can fail or be rate-limited; the package-rename lookup currently does not cache or fallback to a local index.

### Future improvements

- **Cache the recipe pattern map.** `RecipePatternExtractor` currently reloads `~/.jakarta-migration/recipe-patterns.json` each time the module is created; caching it would reduce I/O and startup time.
- **Batch Maven Central requests.** Coordinate-search and package-rename searches are performed one request at a time; grouping or parallelizing them would lower API pressure.
- **Confidence scoring for inferred matches.** Package-rename hits currently use a fixed heuristic; a scoring model based on group/artifact similarity and version recency would be more reliable.
- **Expand bytecode signal detection.** Detect `javax`/`jakarta` references in method bodies, `invokedynamic`, string constants, and annotation attribute arrays, not just descriptors and super/interfaces.
- **Shade/relocation detection.** Identify when an artifact repackages `javax.*` or `jakarta.*` classes and avoid false classification.
- **OSGi / JPMS metadata support.** Read `Bundle-SymbolicName` and `Automatic-Module-Name` for additional namespace clues.
- **Feature flag for deep scanning.** Some users may want to disable bytecode scanning for speed; a runtime flag would let them fall back to coordinate-only classification.
