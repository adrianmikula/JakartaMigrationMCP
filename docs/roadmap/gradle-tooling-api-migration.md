# Gradle Tooling API Migration

## Problem Statement

Our Gradle dependency resolution relies on spawning `gradle dependencies` as an external process and parsing stdout with regex. This is fragile across multiple dimensions:

1. **Multi-module projects** require manual `settings.gradle(.kts)` detection and `:moduleName:dependencies` task path notation — a workaround we recently added
2. **Stdout format coupling** — regex parsing depends on tree-drawing characters (`│`, `├──`, `\`) that are not a stable API
3. **No daemon reuse** — each scan spawns a new Gradle process (5–10s startup per scan)
4. **Wrapper detection** — manual 10-directory walk to find `gradlew`, duplicated from what the Tooling API does automatically
5. **Configuration filtering** — removed `--configuration` flag (only accepts single value), now parse ALL configurations from output

The **Gradle Tooling API** (`org.gradle:gradle-tooling-api`) solves all of these problems. It is Gradle's official programmatic API for IDE and tool authors, providing structured model access, daemon reuse, wrapper awareness, and version-independent compatibility.

**ADR**: [docs/adr/0005-adopt-gradle-tooling-api.md](../adr/0005-adopt-gradle-tooling-api.md)

---

## Current State

### Code to Replace

| File | Lines | Purpose | Replacement |
|------|-------|---------|-------------|
| `DependencyTreeCommandExecutorImpl.executeGradleDependenciesAsync()` | ~70 | Spawns `gradle dependencies` process | `GradleToolingApiExecutor` |
| `DependencyTreeCommandExecutorImpl.buildGradleCommand()` | ~20 | Constructs command with wrapper detection | Tooling API handles automatically |
| `DependencyTreeCommandExecutorImpl.findGradleProjectRoot()` | ~20 | Walks up for `settings.gradle(.kts)` | `GradleConnector.forProjectDirectory()` |
| `DependencyTreeCommandExecutorImpl.computeGradleModuleName()` | ~6 | Computes `:module:path` for task notation | `GradleProject.getChildren()` |
| `DependencyTreeCommandExecutorImpl.buildGradleModuleCommand()` | ~15 | Builds `:module:dependencies` command | Tooling API task execution |
| `DependencyTreeCommandExecutorImpl.parseGradleOutput()` | ~25 | Regex-parses tree output | Structured model objects |
| `DependencyTreeCommandExecutorImpl.parseGradleLine()` | ~20 | Parses individual dependency lines | N/A (model objects) |
| `DependencyTreeCommandExecutorImpl.findGradleWrapper()` | ~25 | Walks up for `gradlew`/`gradlew.bat` | Tooling API (wrapper-aware) |
| `DependencyTreeCommandExecutorImpl.isGradleAvailableForProject()` | ~5 | Checks gradle availability | `GradleConnector` throws on failure |
| `GradleBuildParser.java` | ~120 | Regex-parses build.gradle for dependencies | Tooling API resolves the actual build |

**Total**: ~326 lines of Gradle-specific code to remove

### Code to Keep

| File | Reason |
|------|--------|
| `BuildFileDiscovery.java` | Simple directory walking, correct exclusions, needed for initial file discovery |
| `ScopeConstants.java` | Configuration-to-scope mapping still needed for classification |
| `DependencyTreeResult.java` | Shared domain model, Tooling API results will be converted to this |
| `TransitiveDependencyScannerImpl.java` | Orchestrator — will call new executor instead of old one |

---

## Implementation Plan

### Phase 1: Add Tooling API Dependency + Basic Executor

**Goal**: Add the library and create a `GradleToolingApiExecutor` that can resolve dependencies for a single-module project.

**Effort**: Low-Medium

**Changes**:
1. Add `org.gradle:gradle-tooling-api:8.12` to `premium-core-engine/build.gradle.kts`
2. Create `GradleToolingApiExecutor.java` in `premium-core-engine/.../advancedscanning/service/impl/`:
   - Implements `DependencyTreeCommandExecutor` (same interface as existing executor)
   - `executeGradleDependenciesAsync()` uses `GradleConnector` → `ProjectConnection` → `newBuild().forTasks("dependencies").run()`
   - Parses output by running the task and capturing stdout (same as today but via Tooling API)
   - Alternatively: use `EclipseClasspath` or `IntelliJIdeaClasspath` model for structured dependency data
3. Add `GradleToolingApiExecutorTest.java`:
   - `shouldResolveDependenciesForSingleModule()` — verify against a test project with known dependencies
   - `shouldReturnErrorWhenGradleUnavailable()` — verify graceful degradation
   - `shouldReuseDaemonAcrossCalls()` — verify second call is faster than first

**Verification**:
- Compile: `./gradlew :premium-core-engine:compileJava`
- Test: `./gradlew :premium-core-engine:fastTest`
- Manual: run against the project itself (`gradle dependencies` from root)

---

### Phase 2: Multi-Module Support via Tooling API

**Goal**: Handle multi-module Gradle projects natively through `GradleProject.getChildren()`.

**Effort**: Medium

**Changes**:
1. Enhance `GradleToolingApiExecutor`:
   - Call `connection.model(GradleProject.class).get()` to get project hierarchy
   - Iterate `project.getChildren()` to enumerate subprojects
   - For each subproject, run `connection.newBuild().forTasks(":" + child.getName() + ":dependencies").run()`
   - Map results back to `DependencyTreeResult` per build file
2. Update `TransitiveDependencyScannerImpl`:
   - When using Tooling API executor, skip multi-module detection logic (`detectMultiModuleProject`, `findCommonProjectRoot`)
   - The executor handles multi-module internally
3. Delete multi-module workaround code from `DependencyTreeCommandExecutorImpl`:
   - `findGradleProjectRoot()` — no longer needed
   - `computeGradleModuleName()` — no longer needed
   - `buildGradleModuleCommand()` — no longer needed
4. Tests:
   - `shouldResolveAllSubprojectsInMultiModule()` — verify against a multi-module test project
   - `shouldReturnCorrectDependenciesPerSubproject()` — verify each subproject gets its own deps
   - `shouldHandleNestedSubprojects()` — verify `:parent:child:dependencies` task paths

**Test fixture**: Create `test-fixtures/multimodule-gradle/` with:
```
settings.gradle.kts (includes "app", "lib")
build.gradle.kts (root — no dependencies)
app/build.gradle.kts (depends on lib + guava)
lib/build.gradle.kts (depends on commons-lang3)
```

**Verification**:
- Compile: `./gradlew :premium-core-engine:compileJava`
- Test: `./gradlew :premium-core-engine:fastTest`
- Integration: scan a real multi-module Gradle project

---

### Phase 3: Remove Old Gradle Code

**Goal**: Delete all replaced code after confirming Tooling API path is stable.

**Effort**: Low

**Prerequisites**: Phase 2 stable in production for at least 2 weeks (or equivalent test coverage confidence)

**Changes**:
1. Delete from `DependencyTreeCommandExecutorImpl`:
    - `findGradleWrapper()` — Tooling API handles wrapper detection
    - `isGradleAvailableForProject()` — replaced by Tooling API connection check
    - `isGradleAvailable()` — replaced by Tooling API
    - `buildGradleCommand()` — replaced by Tooling API task execution
    - `buildGradleModuleCommand()` — deleted in Phase 2
    - `parseGradleOutput()` — replaced by structured model
    - `parseGradleLine()` — replaced by structured model
    - `findGradleProjectRoot()` — deleted in Phase 2
    - `computeGradleModuleName()` — deleted in Phase 2
2. Remove `@Deprecated` annotations (methods no longer exist)
3. Remove `GRADLE_RESOLVABLE_CONFIGS` from `ScopeConstants` (no longer needed for `--configuration` flag, but keep `GRADLE_COMPILE_CONFIGS`/`GRADLE_TEST_CONFIGS` for scope mapping)
4. Remove feature flag (Tooling API is now the only path)
5. Update `DependencyTreeCommandExecutorImpl` — Gradle methods become thin wrappers that throw `UnsupportedOperationException` (or delete Gradle methods entirely and rename interface)
6. Delete related tests:
   - `buildGradleCommand_shouldNotContainConfigurationFlag`
   - `buildGradleCommand_shouldUseWrapperWhenAvailable`
   - `buildGradleCommand_shouldFallBackToSystemGradle`
   - `findGradleProjectRoot_*` tests
   - `computeGradleModuleName_*` tests

**Verification**:
- Compile: `./gradlew :premium-core-engine:compileJava :community-core-engine:compileJava :premium-intellij-plugin:compileJava`
- Full test suite: `mise run fast-test`
- Grep for deleted method names: `rg "findGradleProjectRoot|computeGradleModuleName|parseGradleOutput|parseGradleLine" --include "*.java"` — no production hits

---

### Phase 4: Performance Benchmarks

**Goal**: Quantify improvement from daemon reuse and structured resolution.

**Effort**: Low-Medium

**Changes**:
1. Create `GradleToolingApiPerformanceTest.java` (tagged `@Tag("slow")`):
   - `shouldResolveSingleModuleFasterThanProcess()` — compare first-call latency
   - `shouldReuseDaemonAcrossMultipleCalls()` — verify 2nd+ calls are fast
   - `shouldResolveMultiModuleWithinBudget()` — <30s for 10 submodules
   - `shouldHandleLargeDependencyTree()` — 500+ dependencies
2. Create `GradleToolingApiMemoryTest.java` (tagged `@Tag("slow")`):
   - `shouldNotLeakMemoryAcrossMultipleConnections()` — verify daemon cleanup
   - `shouldHandleConcurrentConnections()` — 4 parallel scans
3. Benchmark results documented in this file (append to Results section)

**Performance Budgets**:

| Metric | Current (Process) | Target (Tooling API) |
|--------|-------------------|---------------------|
| First scan (single module) | ~8s (daemon startup + resolution) | ~6s (daemon startup + resolution) |
| Subsequent scans | ~8s (new process each time) | ~2s (warm daemon reuse) |
| Multi-module (10 submodules) | ~40s (10 sequential processes) | ~15s (1 connection, 10 task runs) |
| Memory per scan | ~50MB (process) | ~20MB (daemon shared) |

---

## Dependency Impact

### New Dependencies

```kotlin
// premium-core-engine/build.gradle.kts
dependencies {
    implementation("org.gradle:gradle-tooling-api:8.12")
}
```

**Size**: ~2MB (API jar only; runtime Gradle distribution downloaded by Tooling API on first use)

**Transitive dependencies**: None (Tooling API jar is self-contained; Gradle classes are loaded from the target build's distribution)

### Existing Dependencies Affected

None. The Tooling API is additive — no existing libraries are removed or version-changed.

---

## IntelliJ Plugin Considerations

### Daemon Coordination

IntelliJ already manages Gradle daemons via its built-in Gradle integration. The Tooling API will also try to start a daemon. To avoid duplicate daemons:

1. **Option A**: Use `GradleConnector.useGradleUserDir()` to point to the same Gradle user home as IntelliJ (`~/.gradle`). This ensures the Tooling API reuses IntelliJ's existing daemon.
2. **Option B**: Use `GradleConnector.useInstallation()` to point to IntelliJ's bundled Gradle distribution.
3. **Option C**: Accept that two daemons may run ( IntelliJ's and Tooling API's). This uses more memory but avoids tight coupling.

**Recommendation**: Option A (share Gradle user home). This is the simplest and most reliable approach.

### Thread Safety

The Tooling API's `ProjectConnection` is not thread-safe. Each scan should create its own connection (or use a connection pool). The existing `ExecutorService` in `DependencyTreeCommandExecutorImpl` (2 threads) can be reused.

---

## Rollback Plan

If the Tooling API causes issues in production (e.g., daemon startup failures, version incompatibility):

1. The Tooling API executor fails with a clear error message to the user
2. `DependencyTreeResult` domain model is shared — no data format changes
3. If needed, revert to the previous commit — all old code is removed in Phase 3 (no dual-path maintenance)

---

## Results

_Benchmarks will be added after Phase 4 implementation._

| Metric | Before | After | Improvement |
|--------|--------|-------|-------------|
| First scan (single module) | — | — | — |
| Subsequent scans | — | — | — |
| Multi-module (10 submodules) | — | — | — |

---

## Related Documentation

- **ADR**: [docs/adr/0005-adopt-gradle-tooling-api.md](../adr/0005-adopt-gradle-tooling-api.md)
- **Build Tool Patterns**: [docs/patterns/build-tool-invocation.md](../patterns/build-tool-invocation.md) — Rule 9 (task path notation) becomes obsolete after migration
- **Dependency Scanning Overhaul**: [docs/roadmap/dependency-scanning-overhaul.md](dependency-scanning-overhaul.md) — Phase 4 (build tool error fallback) is partially addressed by this migration
- **Virtual Threads**: [docs/roadmap/virtual_threads_performance.md](virtual_threads_performance.md) — Tooling API daemon reuse complements virtual thread improvements
