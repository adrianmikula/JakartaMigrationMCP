# Scan UI Non-Blocking Performance

## Requirement

Scan operations (quick scan, deep scan, and individual scanner batches) must not block or freeze the main IntelliJ plugin/IDE UI. The Event Dispatch Thread (EDT) must remain responsive throughout all scan phases, allowing users to interact with the IDE while scans run in the background.

## Rationale

Scans involve CPU-intensive operations (AST parsing, file system walks, bytecode analysis) that can take several seconds on large projects. If these operations block the EDT, the entire IDE becomes unresponsive — violating IntelliJ platform guidelines and degrading user experience.

## Architecture

Scans execute on background threads via:
- `CompletableFuture.runAsync()` / `.supplyAsync()` in `MigrationToolWindow`
- Bounded `FixedThreadPool(2)` in `AdvancedScanningService`
- Per-scanner `ForkJoinPool(4)` in `BaseScanner`

UI updates flow through:
- `ThrottledProgressListener` (debounce + rate-limit) → `ApplicationManager.getApplication().invokeLater()` → EDT

## Performance Budgets

| Metric | Budget | Rationale |
|--------|--------|-----------|
| EDT responsiveness during scan | < 2s for heartbeat task | IDE should feel responsive |
| Full scan completion | < 30s for small project (5 files with javax imports) | Reasonable wait for user |
| Throttled listener dispatch overhead | < 500ms for 100 rapid callbacks | Listener shouldn't be bottleneck |
| Concurrent EDT + scan interference | 0 deadlocks, 0 missed EDT tasks | No thread contention |

## Test Coverage

File: `premium-intellij-plugin/src/test/java/.../service/ScanUiNonBlockingPerformanceTest.java`

| Test | Verifies |
|------|----------|
| `scanAllDoesNotBlockEdt` | EDT heartbeat completes promptly during scan |
| `scanAllCompletesWithinBudget` | Full scan meets wall-clock budget |
| `throttledListenerDoesNotIntroduceExcessiveOverhead` | Throttled dispatch latency is bounded |
| `concurrentEdtAndScanDoNotInterfere` | No deadlocks or EDT starvation under concurrent load |

## Tag

Tests use `BasePlatformTestCase` (JUnit 3 style) following the same pattern as
`AdvancedScanningServiceProgressTest`. Included in the IntelliJ Platform test matrix tasks:
```bash
./gradlew :premium-intellij-plugin:testIntelliJ_2024_1
```
