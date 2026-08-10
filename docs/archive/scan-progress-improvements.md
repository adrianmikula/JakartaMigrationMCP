# Scan Progress Bar Improvements

*Last reviewed: 2026-08-09*

## Implementation Status

The following phases have been implemented:

- **Phase 1** - `PremiumScanOrchestrator` now reports `1/3`, `2/3`, and `3/3` when source, dependency, and advanced scans complete.
- **Phase 2** - `AdvancedScanningService.scanAllInternal` increments progress after each individual scan instead of only after whole batches.
- **Phase 4** - Quick scan now physically skips the transitive-dependency scan by passing `includeTransitive=false` to `runScansSequentially`. The UI total is 16 steps and `AdvancedScanSummary` receives an empty `TransitiveDependencyProjectScanResult`.
- **Phase 5** - `ThrottledProgressListener` tracks its pending `ScheduledFuture` objects, cancels them on flush, and synchronizes scheduling to avoid duplicate or out-of-order updates.

## Problem Statement

The scan progress bar in the IntelliJ plugin is frequently observed stuck near **0%** (often around **10%**) for long periods while a scan is running in the background. The UI is not actually frozen, but the percentage barely moves, giving the impression that the tool has stalled.

## Root Cause Analysis

### 1. Coarse-grained, fixed-step progress reporting

`AdvancedScanningService` in the premium-intellij-plugin uses a hard-coded step count:

- `TOTAL_ADVANCED_SCAN_STEPS = 17` (`AdvancedScanningService.java:114`)
- 1 step for file discovery
- 4 steps after the first batch of scanners completes
- 4 steps after the second batch
- 8 steps after the third batch

The counter is only updated **after** a whole batch finishes, not while any individual scanner is running.

**Quick scan path (`scanAllExcludingTransitive` → `runScansSequentially`):**

```
discovery (1/17 = 5.9%)
JPA       (2/17 = 11.8%)
Bean Validation (3/17 = 17.6%)
...
```

After file discovery and JPA, the bar is already at ~11.8%. JPA scanning can be slow on large code bases, so the progress bar sits at ~10% for the entire JPA phase.

**Deep scan path (`scanAllInternal`):**

```
discovery (1/17 = 5.9%)
batch 1   (5/17 = 29.4%)
batch 2   (9/17 = 52.9%)
batch 3   (17/17 = 100%)
```

The bar stays at ~6% while the first four scanners run, because all four are counted only when the whole batch completes.

### 2. Source and dependency phases never advance

`PremiumScanOrchestrator.orchestrate` reports three high-level phases with `completed = 0` and `total = 3`:

- `Source Code Scanning (0/3)`
- `Dependency Analysis (0/3)`
- `Advanced Scans (0/3)`

Those are reported at start, but the first two are **never** updated to `1/3` or `2/3`. The user therefore sees 0% progress while the source scan and dependency analysis are actually running.

### 3. Percentage calculation is naive

`DashboardComponent.onScanPhase` computes the bar value with integer division:

```java
int percentage = (completed * 100) / total;
```

This produces big jumps and gives no feedback for long sub-tasks.

### 4. Misleading quick-scan implementation

`scanAllExcludingTransitive` calls `runScansSequentially`, but `runScansSequentially` still calls `scanForTransitiveDependencies`. The total of 17 is also used for quick scans, even though quick scans are intended to skip transitive analysis. This can leave the bar at 16/17 (~94%) because the transitive step is reported and counted the same way.

## Concurrency and UI Threading

- **Concurrency:** `PremiumScanOrchestrator` runs source scanning, dependency analysis, and advanced scans in parallel via `CompletableFuture.supplyAsync` on the default ForkJoin pool. `AdvancedScanningService` submits scan tasks to a fixed `scanExecutor` of at most 2 threads, then blocks the orchestration thread with `.join()` until each batch completes.
- **UI thread safety:** All UI updates are dispatched with `ApplicationManager.getApplication().invokeLater` (in the throttled listener and `MigrationToolWindow`) and `SwingUtilities.invokeLater` inside `DashboardComponent`.
- **Is the UI frozen?** No. The EDT is not blocked by the scan. The bar appears frozen because progress events are emitted too infrequently, not because the UI thread is stuck.

## Known Issues in `ThrottledProgressListener`

- `flushPendingUpdates` clears in-flight scheduling flags and directly calls the delegate, but it does not prevent a concurrently scheduled `dispatchPhaseUpdate` from also firing, which can produce duplicate or out-of-order updates.
- The throttle (100ms debounce) can collapse rapid per-dependency progress from `TransitiveDependencyScannerImpl`, further hiding real movement.

## Recommended Implementation Plan

### Phase 1: Fix the missing high-level phase updates

In `PremiumScanOrchestrator.orchestrate`:

- Mark `Source Code Scanning` and `Dependency Analysis` as `1/3` when those futures complete.
- Keep `Advanced Scans` at `3/3` only after the advanced scan future finishes.

### Phase 2: Report intra-scanner progress

For each scanner in `AdvancedScanningService`:

- Emit `onScanPhase` at the start of the scanner with `completed`/`total` based on the number of files being scanned.
- Emit incremental updates as files are processed, if the underlying scanner supports it.

For `TransitiveDependencyScannerImpl`:

- The existing `onPhaseProgress` calls for `Classifying dependencies`, `Scanning JARs`, and `Looking up Maven Central` are already present. Ensure they are not throttled down to invisibility.

### Phase 3: Make the progress bar indeterminate when no fine-grained total is available

If a scanner cannot provide a meaningful per-item count, set `total = 0` so `DashboardComponent` switches the `JProgressBar` to indeterminate mode instead of showing a misleading low percentage.

### Phase 4: Correct the quick-scan total

- Rename or refactor `scanAllExcludingTransitive` so it genuinely excludes `scanForTransitiveDependencies`.
- Compute the step total from the actual list of scanners that will run, instead of a hard-coded `17`.

### Phase 5: Fix `ThrottledProgressListener` race

- Ensure `flushPendingUpdates` cancels any pending scheduled dispatch and waits for it to complete before calling the delegate, or move the flush onto the same single-threaded executor used for throttling.

## Related Files

- `premium-intellij-plugin/.../ui/DashboardComponent.java`
- `premium-intellij-plugin/.../ui/MigrationToolWindow.java`
- `premium-intellij-plugin/.../ui/ThrottledProgressListener.java`
- `premium-intellij-plugin/.../service/AdvancedScanningService.java`
- `premium-core-engine/.../scanning/orchestration/PremiumScanOrchestrator.java`
- `premium-core-engine/.../advancedscanning/service/impl/TransitiveDependencyScannerImpl.java`
