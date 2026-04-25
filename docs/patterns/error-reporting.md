# Error Reporting Pattern

This document defines the standard pattern for integrating error reporting with Supabase analytics throughout the Jakarta Migration plugin.

## Overview

All user-facing operations should integrate with the `ErrorReportingService` to ensure errors are logged in Supabase for analytics and debugging purposes. This provides visibility into issues that users encounter during plugin usage.

## Standard Pattern

### 1. Component Integration

Add ErrorReportingService to your component constructor:

```java
// Imports
import adrianmikula.jakartamigration.analytics.service.ErrorReportingService;
import adrianmikula.jakartamigration.analytics.service.UserIdentificationService;

// Field declaration
private final ErrorReportingService errorReportingService;

// Constructor with dependency injection
public YourComponent(Project project, OtherService otherService) {
    this(project, otherService, new ErrorReportingService(new UserIdentificationService()));
}

public YourComponent(Project project, OtherService otherService, ErrorReportingService errorReportingService) {
    this.project = project;
    this.otherService = otherService;
    this.errorReportingService = errorReportingService;
    // ... rest of initialization
}
```

### 2. Error Reporting in Catch Blocks

Always report errors in exception handlers:

```java
try {
    // Your operation logic here
    performOperation();
} catch (Exception e) {
    // Existing error handling (UI updates, logging, etc.)
    LOG.error("Operation failed", e);
    statusLabel.setText("Operation failed: " + e.getMessage());
    Messages.showErrorDialog(project, "Failed to perform operation: " + e.getMessage(), "Error");
    
    // Report error to Supabase for analytics
    errorReportingService.reportError(e, "Operation Context Description");
}
```

## Context Messages

Use descriptive context messages that clearly identify the operation:

### Good Examples:
- "PDF Risk Analysis Report Generation"
- "Advanced Scans Execution"
- "Runtime Bytecode Analysis"
- "Migration Analysis Execution"
- "Dependency Graph Loading"

### Bad Examples:
- "Error" (too generic)
- "Failed" (no context)
- "Operation" (vague)

## Integration Guidelines

### When to Add Error Reporting

**Always add error reporting for:**
- User-initiated operations (button clicks, menu actions)
- File operations (opening, saving, reading)
- Network operations (API calls, downloads)
- Analysis operations (scanning, parsing, processing)
- Background tasks that affect the user

**Optional for:**
- Internal validation (may be too verbose)
- Expected failure modes (user input validation)
- Debug logging (use regular logging instead)

### When NOT to Add Error Reporting

**Don't add error reporting for:**
- Expected user errors (invalid input, permission denied)
- Intentional failures (feature flags, license checks)
- Debug/development logging
- Performance monitoring (use analytics instead)

## Component Integration Checklist

When adding error reporting to a new component:

- [ ] Add ErrorReportingService import
- [ ] Add ErrorReportingService field
- [ ] Create overloaded constructor for dependency injection
- [ ] Identify all catch blocks for user-facing operations
- [ ] Add errorReportingService.reportError() calls with descriptive context
- [ ] Update component instantiation in MigrationToolWindow
- [ ] Test that errors appear in Supabase
- [ ] Verify error reporting doesn't impact user experience

## MigrationToolWindow Integration

Update MigrationToolWindow to pass ErrorReportingService to your component:

```java
// In MigrationToolWindow constructor
this.errorReportingService = new ErrorReportingService(new UserIdentificationService());

// When creating your component
yourComponent = new YourComponent(project, otherService, errorReportingService);
```

## Best Practices

### 1. Asynchronous Processing
ErrorReportingService processes errors asynchronously, so it won't impact UI performance.

### 2. Context Consistency
Use consistent naming patterns for context messages:
- Start with operation type: "PDF", "Advanced Scans", "Runtime", etc.
- Include specific action: "Generation", "Execution", "Analysis", etc.
- Keep messages under 50 characters for readability

### 3. Error Hierarchy
Report errors at the appropriate level:
- Report user-facing errors, not internal implementation details
- Report the root cause, not wrapped exceptions
- Report meaningful context, not generic failures

### 4. Testing
Verify error reporting works:
1. Intentionally trigger an error
2. Check Supabase `error_reports` table
3. Verify context message appears correctly
4. Confirm stack trace is captured

## Examples in Codebase

### ReportsTabComponent
```java
} catch (Exception e) {
    outputArea.append("Error generating Risk Analysis report: " + e.getMessage() + "\n");
    statusLabel.setText("Error generating report");
    Messages.showErrorDialog(project, "Failed to generate Risk Analysis report: " + e.getMessage(), "Error");
    
    // Report error to Supabase for analytics
    errorReportingService.reportError(e, "PDF Risk Analysis Report Generation");
}
```

### AdvancedScansComponent
```java
} catch (Exception e) {
    LOG.error("Error running scans", e);
    JOptionPane.showMessageDialog(mainPanel,
            "Error running scans: " + e.getMessage(),
            "Scan Error",
            JOptionPane.ERROR_MESSAGE);
    
    // Report error to Supabase for analytics
    errorReportingService.reportError(e, "Advanced Scans Execution");
}
```

### RuntimeComponent
```java
} catch (Exception ex) {
    // Report error to Supabase for analytics
    errorReportingService.reportError(ex, "Runtime Bytecode Analysis");
    
    SwingUtilities.invokeLater(() -> {
        statusLabel.setText("Analysis failed: " + ex.getMessage());
        statusLabel.setForeground(Color.RED);
        progressBar.setIndeterminate(false);
        runHealthCheckButton.setEnabled(true);
        Messages.showErrorDialog(project, "Analysis failed: " + ex.getMessage(), "Error");
    });
}
```

## Related Documentation

- [Error Reporting Issues](../troubleshooting/COMMON_ISSUES.md#error-reporting-issues)
- [Testing Guidelines](../TESTiNG.md)
- [Architecture Guidelines](../AgentRules/ARCHITECTURE.md)
