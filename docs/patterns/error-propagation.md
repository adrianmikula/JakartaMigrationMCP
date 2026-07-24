# Error Propagation Pattern

This document defines the standard pattern for propagating errors from core
logic through service layers to the UI. Every error must reach the user in a
meaningful way — never silently swallowed, never shown as raw exception text.

## Overview

Operations fail for many reasons: network timeouts, file I/O errors, parse
failures, unavailable tools, permission issues. The user must always know
*what* failed, *why* it failed, and *what partial results* they're looking at.

The error propagation stack has four layers:

```
┌──────────────────────────────────────────┐
│  Core Logic (service / scanner / module) │
│  - catch block → log + set error on      │
│    result object                         │
├──────────────────────────────────────────┤
│  Bridge / Adapter (service facade)       │
│  - reads error fields from result        │
│  - maps internal status → UI status      │
│  - propagates per-item and batch errors  │
├──────────────────────────────────────────┤
│  UI (table / tree / panel)              │
│  - detail column shows explanation       │
│  - status column shows state             │
│  - banner shows batch-level summary      │
├──────────────────────────────────────────┤
│  Notification (balloon / toast / alert)  │
│  - shows critical failures               │
│  - deduplicated per operation            │
└──────────────────────────────────────────┘
```

These layers apply regardless of whether the operation is a dependency scan,
a file analysis, a report generation, or any other long-running task.

## Layer 1: Core Logic — Setting Error Information

### Rule 1: Never return empty on error without explanation

When an operation fails, the result object must carry an error message. If the
operation produced partial results before failing, return those partial results
alongside the error — never discard them.

**Anti-pattern:**
```java
try {
    return performOperation(input);
} catch (Exception e) {
    return Result.empty();  // error lost, partial data lost
}
```

**Correct — partial results + error message:**
```java
try {
    return performOperation(input);
} catch (Exception e) {
    List<Item> partial = collectedSoFar;
    log.warn("Failed to process {}: {} — {}", input,
        e.getClass().getSimpleName(), e.getMessage());
    return Result.withError(partial, humanFriendlyMessage(e));
}
```

**Correct — no partial results:**
```java
try {
    return performOperation(input);
} catch (Exception e) {
    log.warn("Failed to process {}: {} — {}", input,
        e.getClass().getSimpleName(), e.getMessage());
    return Result.error(humanFriendlyMessage(e));
}
```

### Rule 2: Annotate child items with parent-level errors

When a batch operation (file scan, directory walk, multi-module build) fails
partially, every child item produced by the failed parent should carry the
error. This ensures that when the UI maps items to rows, each row knows why
its data may be incomplete.

```java
BatchResult batch = processItems(items);
if (batch.hasError()) {
    for (Item item : batch.getItems()) {
        if (item.getDetailMessage() == null || item.getDetailMessage().isEmpty()) {
            item.setDetailMessage(batch.getErrorMessage());
        }
    }
}
```

Don't overwrite a more specific per-item error with the batch error:

```java
if (item.getDetailMessage() == null || item.getDetailMessage().isEmpty()) {
    item.setDetailMessage(batch.getErrorMessage());
}
```

### Rule 3: Use human-friendly error messages, not exception messages

Exception messages are implementation details (`java.net.SocketTimeoutException:
connect timed out`). Users need actionable explanations.

Define error messages as constants in a shared location:

```java
public final class ErrorMessages {
    public static final String TOOL_NOT_FOUND =
        "Required tool not found — install it or add a wrapper to your project";
    public static final String COMMAND_FAILED =
        "Command failed — results are based on fallback analysis (partial)";
    public static final String TIMEOUT =
        "Operation timed out — input may be too large or the system too slow";
    public static final String PARSE_ERROR =
        "Could not parse output — using fallback parser";
    public static final String NETWORK_ERROR =
        "Network request failed — check connectivity or proxy settings";
    public static final String PERMISSION_DENIED =
        "Access denied — check file permissions";

    private ErrorMessages() {}
}
```

Map exception types to messages:

```java
static String humanFriendlyMessage(Exception e) {
    return switch (e) {
        case TimeoutException _        -> ErrorMessages.TIMEOUT;
        case ConnectException _        -> ErrorMessages.NETWORK_ERROR;
        case SecurityException _       -> ErrorMessages.PERMISSION_DENIED;
        case IOException _             -> ErrorMessages.TOOL_NOT_FOUND;
        case JsonParseException _      -> ErrorMessages.PARSE_ERROR;
        default                        -> ErrorMessages.COMMAND_FAILED;
    };
}
```

### Rule 4: Catch blocks must log at the appropriate level

| Impact | Log Level | When |
|--------|-----------|------|
| User loses data | `log.warn` | File/operation returns empty or partial results |
| Enrichment skipped | `log.debug` | Optional step failed (lookup, scan, enrichment) |
| Expected failure | `log.debug` | File not found, permission denied in optional path |
| Cleanup failure | `log.debug` | ThreadLocal cleanup, cache eviction, temp file delete |

**Always include the exception class name** so intermittent failures
(`OutOfMemoryError` vs `IOException`) are distinguishable in logs:

```java
} catch (Exception e) {
    log.warn("Failed to process {}: {} — {}", filePath,
        e.getClass().getSimpleName(), e.getMessage());
}
```

**Never use empty catch blocks:**

```java
// WRONG — error completely silent
} catch (Exception e) {
    // skip this entry
}

// WRONG — comment is not a substitute for logging
} catch (Exception e) {
    // continue walking
}

// CORRECT
} catch (Exception e) {
    log.debug("Skipping entry {} in {}: {} — {}", entry, source,
        e.getClass().getSimpleName(), e.getMessage());
}
```

### Rule 5: Error state enums must have distinct values for each failure mode

Don't collapse multiple error types into a single `UNKNOWN` value. Each
failure mode should have its own enum constant so the UI can display distinct
status text.

**Anti-pattern:**
```java
enum Status { SUCCESS, FAILED, UNKNOWN }
// UI can only show "Failed" — no way to distinguish build error from parse error
```

**Correct:**
```java
enum Status {
    SUCCESS,
    BUILD_TOOL_ERROR,   // "Build command failed — using fallback"
    PARSE_ERROR,        // "Could not parse output — partial results"
    NETWORK_ERROR,      // "Network unavailable — cached data shown"
    TIMEOUT,            // "Operation timed out — partial results"
    UNKNOWN             // "Classification pending — review manually"
}
```

The enum carries the *reason*. The `detailMessage` string carries the
*explanation*. Always set both.

## Layer 2: Bridge — Mapping Errors to UI Model

### Rule 6: Read error fields from result objects

The bridge/facade layer is responsible for translating core logic results into
UI model objects. It must read error fields from *both* batch-level and
item-level results:

```java
for (ItemResult itemResult : batchResult.getItemResults()) {
    UIModel uiItem = convertToUIModel(itemResult);

    // Propagate batch-level error if item doesn't have its own
    if (uiItem.getDetailMessage() == null || uiItem.getDetailMessage().isEmpty()) {
        if (batchResult.hasError()) {
            uiItem.setDetailMessage(batchResult.getErrorMessage());
        }
    }
    results.add(uiItem);
}
```

### Rule 7: Don't collapse error states into generic "unknown"

**Anti-pattern:**
```java
case BUILD_TOOL_ERROR -> Status.UNKNOWN;
// UI shows: "? Unknown" — ambiguous, unhelpful
```

**Correct:**
```java
case BUILD_TOOL_ERROR -> Status.BUILD_TOOL_ERROR;
// UI shows: "⚠ Build Tool Error" with detailMessage tooltip
```

If adding a new enum value isn't feasible, keep the existing value but ensure
the `detailMessage` is always set and displayed in the detail column.

### Rule 8: Batch-level errors become a summary banner

When a batch operation has errors, display a summary at the top of the result
list so the user immediately understands the scope of the problem:

```java
if (batchResult.getErrorCount() > 0) {
    String banner = String.format(
        "⚠ %d of %d operations failed — results for those items are partial",
        batchResult.getErrorCount(), batchResult.getTotalCount());
    uiTable.setBannerText(banner);
}
```

## Layer 3: UI — Displaying Errors

### Rule 9: Every row must have a detail column

Add a column (or repurpose an existing hidden column) that displays the
`detailMessage` when non-empty. This is the primary mechanism for
communicating per-item errors.

| Column | Rendering |
|--------|-----------|
| Details | Empty for healthy items; muted grey italic for errors; normal text for informational messages |

### Rule 10: Status column must distinguish error states

Each error type must display a distinct label — never use a generic
"? Unknown" for known failure modes:

| Label | Meaning |
|-------|---------|
| "⚠ Error" | Operation failed, partial results shown |
| "? Pending" | Not yet processed |
| "⏳ In Progress" | Currently being processed |
| "✓ Complete" | Fully processed, no issues |
| "⚠ Warning" | Processed with issues |

### Rule 11: Summary banner for batch errors

Show a persistent, dismissible banner when any items in the batch had errors:

```
⚠ 3 of 12 operations failed — results for those items are partial
```

## Layer 4: Notifications — Critical Failures

### Rule 12: Notification services must call the platform API

**Anti-pattern:**
```java
public boolean showOnce(String key, String title, String message) {
    log.info("[NOTIFICATION] {}: {}", title, message);  // no-op!
    return true;
}
```

**Correct:**
```java
public boolean showOnce(String key, String title, String message, Project project) {
    if (!shownNotifications.add(key)) {
        return false;  // deduplicated
    }
    if (project != null) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Your Plugin")
            .createNotification(title, message, NotificationType.WARNING)
            .notify(project);
    }
    log.info("[NOTIFICATION] {}: {}", title, message);
    return true;
}
```

### Rule 13: Notifications are deduplicated per operation

Use a composite key that includes the operation identifier:

```java
String key = "operation-failure:" + operationId;
notificationService.showOnce(key, "Operation Failed", errorMessage, project);
```

This ensures one notification per failed operation, not one per item.

## Anti-Patterns Summary

| Anti-Pattern | Problem | Fix |
|-------------|---------|-----|
| Empty catch block | Error completely silent | Add `log.warn` or `log.debug` + set error on result |
| `return empty()` on error | User loses data + explanation | Return partial results + error message |
| `e.getMessage()` in user text | Technical jargon | Use human-friendly message from constant map |
| `log.debug` for data-affecting errors | Invisible in production | Use `log.warn` when user loses data |
| Collapsing errors to `UNKNOWN` | Ambiguous status display | Distinct enum value per failure mode |
| Error data exists but not displayed | Populated field, no UI column | Add detail column to table |
| No batch-level error summary | User unaware of partial failure | Add banner with error count |
| Notification service logs only | No platform notification | Call platform notification API |
| No exception class in log message | Intermittent errors unidentifiable | Include `e.getClass().getSimpleName()` |

## Checklist for New Features

When adding a new operation, scanner, analyzer, or long-running task:

### Core Logic
- [ ] Every catch block logs at the appropriate level
- [ ] Every catch block includes `e.getClass().getSimpleName()` in the message
- [ ] Result objects carry an `errorMessage` field for errors
- [ ] Child items inherit parent-level errors when the parent fails
- [ ] Error messages are human-friendly, not raw exception text
- [ ] Error state enum has distinct values per failure mode (not just `UNKNOWN`)

### Bridge / Adapter
- [ ] Bridge reads error fields from both batch and item results
- [ ] Error states map to distinct UI statuses (not collapsed to generic `UNKNOWN`)
- [ ] Batch-level errors trigger a summary banner
- [ ] Item-level errors propagate into UI model `detailMessage`

### UI
- [ ] Detail column exists and shows `detailMessage` when non-empty
- [ ] Status column uses distinct labels per error type
- [ ] Summary banner appears when batch has errors
- [ ] Error rendering is visually distinct from healthy data

### Notifications
- [ ] Critical failures trigger platform notifications
- [ ] Notifications are deduplicated per operation
- [ ] Notification service calls the platform API (not just logging)

## Related Documentation

- [Error Reporting Pattern](error-reporting.md) — Supabase analytics error reporting
- [Simplicity and Consistency](simplicity_and_consistency.md) — KISS principles
- [Memory Efficiency](memory_efficiency.md) — Memory-safe patterns
- [Async UI Operations](async-ui-operations.md) — Threading patterns
