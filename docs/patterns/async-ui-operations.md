# Async UI Operations Pattern

This document defines the standard pattern for performing long-running operations in IntelliJ plugins without blocking the Event Dispatch Thread (EDT), which causes UI freezing.

## Overview

In Swing/IntelliJ plugin development, all UI updates must occur on the Event Dispatch Thread (EDT). However, long-running operations (file I/O, network calls, scanning) must **never** run on the EDT, as this will freeze the UI and make the application unresponsive.

## The Problem

When a button action listener performs a long-running operation synchronously on the EDT, the entire UI freezes until the operation completes:

```java
// ❌ BAD: Blocks EDT, UI freezes
private void handleDeepScan(ActionEvent e) {
    setScanButtonsEnabled(false);
    
    // This blocks the EDT for the entire duration of the scan
    performDeepScan(projectPath);  // SYNCHRONOUS CALL ON EDT
    
    setScanButtonsEnabled(true);
}
```

## The Solution

Wrap long-running operations in `CompletableFuture.runAsync()` or `CompletableFuture.supplyAsync()` to run them on a background thread, then marshal UI updates back to EDT using `ApplicationManager.getApplication().invokeLater()`.

### Standard Pattern

```java
// ✅ GOOD: Runs on background thread, UI stays responsive
private void handleDeepScan(ActionEvent e) {
    // UI updates can happen synchronously on EDT
    setScanButtonsEnabled(false);
    dashboardComponent.setAnalysisRunning(true);
    
    // Run long-running operation on background thread
    CompletableFuture.runAsync(() -> {
        try {
            performDeepScan(projectPath);
        } catch (Exception ex) {
            LOG.error("Deep scan failed", ex);
            
            // Marshal UI updates back to EDT
            ApplicationManager.getApplication().invokeLater(() -> {
                dashboardComponent.setAnalysisRunning(false);
                setScanButtonsEnabled(true);
                NotificationHelper.showWarning(project,
                        "Scan Failed",
                        "Deep scan failed: " + ex.getMessage());
            });
        }
    });
}
```

### Pattern for Operations with Return Values

When you need the result of the operation:

```java
// ✅ GOOD: Async operation with return value
private void handleQuickScan(ActionEvent e) {
    setScanButtonsEnabled(false);
    
    CompletableFuture<DependencyAnalysisReport> future = CompletableFuture.supplyAsync(() -> {
        // This runs on background thread
        return analysisService.analyzeProject(projectPath);
    });
    
    future.thenAccept(report -> {
        // Marshal UI updates back to EDT
        ApplicationManager.getApplication().invokeLater(() -> {
            if (report != null) {
                updateDashboardFromReport(report);
                setScanButtonsEnabled(true);
            }
        });
    });
}
```

### Chaining Multiple Async Operations

When you have multiple phases that must run sequentially:

```java
// ✅ GOOD: Chained async operations
private void performDeepScan(Path projectPath) {
    // Phase 1: Deep dependency analysis
    CompletableFuture<TransitiveDependencyProjectScanResult> deepFuture = 
        CompletableFuture.supplyAsync(() -> {
            return advancedScanningService.scanDependenciesDeep(projectPath, dashboardComponent);
        });
    
    // Phase 2: Advanced scans (runs after Phase 1 completes)
    CompletableFuture<AdvancedScanSummary> advFuture = deepFuture.thenCompose(deepResult -> {
        dashboardComponent.onScanPhase("Advanced Scans", 1, 3);
        return CompletableFuture.supplyAsync(() -> {
            return advancedScanningService.scanAll(projectPath, dashboardComponent);
        });
    });
    
    // Phase 3: Platform detection (runs after Phase 2 completes)
    CompletableFuture<Void> platformFuture = advFuture.thenRun(() -> {
        dashboardComponent.onScanPhase("Platform Detection", 2, 3);
        if (platformsTabComponent != null) {
            platformsTabComponent.scanProject();
        }
    });
    
    // Final completion handling
    platformFuture.whenComplete((v, throwable) -> {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (throwable != null) {
                LOG.error("Scan failed", throwable);
                dashboardComponent.setAnalysisRunning(false);
                setScanButtonsEnabled(true);
            } else {
                dashboardComponent.onScanComplete();
                setScanButtonsEnabled(true);
            }
        });
    });
}
```

## Key Rules

1. **Never block the EDT**: Any operation that takes more than ~100ms must run off the EDT
2. **Always marshal UI updates to EDT**: Use `ApplicationManager.getApplication().invokeLater()` for any Swing/IntelliJ UI component updates from background threads
3. **Disable UI controls before async work**: Disable buttons/controls before starting async work, re-enable in completion handler
4. **Handle errors properly**: Catch exceptions in background thread and marshal error UI updates to EDT
5. **Use CompletableFuture chains**: For multi-phase operations, use `thenCompose`/`thenRun`/`thenAccept` to chain operations

## Common Pitfalls

### ❌ Calling async methods synchronously

```java
// BAD: This still blocks EDT even though the method uses CompletableFuture internally
private void handleScan(ActionEvent e) {
    performDeepScan(projectPath);  // Method returns immediately, but EDT still blocked
}
```

### ❌ UI updates from background thread

```java
// BAD: UI update from background thread - causes Swing threading violations
CompletableFuture.runAsync(() -> {
    String result = longRunningOperation();
    statusLabel.setText(result);  // ❌ UI update from background thread!
});
```

### ❌ Forgetting to re-enable controls on error

```java
// BAD: If exception thrown, buttons stay disabled forever
CompletableFuture.runAsync(() -> {
    try {
        performDeepScan(projectPath);
    } catch (Exception e) {
        LOG.error("Failed", e);
        // ❌ Forgot to re-enable buttons!
    }
});
```

## Testing Considerations

When testing async operations:
- Use `CompletableFuture.get()` with timeout in tests to wait for completion
- Verify UI updates happen on EDT (use `ApplicationManager.getApplication().invokeAndWait()` in tests)
- Test error scenarios to ensure UI controls are properly re-enabled
- Test cancellation/interruption handling if applicable

## References

- [Swing Threading Rules](https://docs.oracle.com/javase/tutorial/uiswing/concurrency/)
- [IntelliJ Platform Threading](https://plugins.jetbrains.com/docs/intellij/general-threading-rules.html)
- [CompletableFuture Documentation](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html)

## Example from Jakarta Migration Plugin

See `MigrationToolWindow.java` for a complete implementation:
- `handleDeepScan()` - Entry point with async wrapper
- `performDeepScan()` - Chained async operations for multi-phase scanning
- `handleQuickScan()` - Async operation with return value handling
