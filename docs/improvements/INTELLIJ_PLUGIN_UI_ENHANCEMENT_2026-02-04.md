# IntelliJ Plugin UI Enhancement - New Tabs Implementation

**Date:** 2026-02-04
**Status:** Planned

---

## Overview

Add new UI tabs to the Jakarta Migration IntelliJ plugin to improve user experience and expose additional functionality.

---

## Changes Required

### 1. Add Refactor Tab

**Location:** MigrationToolWindow.java

**Components:**
- Table displaying refactoring tasks found during analysis
- Columns:
  - Task Type (import, package, XML namespace, reflection)
  - File Path
  - Description
  - Auto-fix Support (boolean)
  - Status (pending, fixed)
- Actions:
  - Manual fix link/button (opens file at location)
  - Auto-fix button (paid/premium only)

**Data Source:**
- Read from SQLite persistence store
- Filter by analysis ID/project

**Mockup:**
```
┌─────────────────────────────────────────────────────────────────┐
│ Refactor Tasks (12 found)                                      │
├─────────────────────────────────────────────────────────────────┤
│ Type           │ File              │ Description     │ Action   │
│───────────────┼───────────────────┼────────────────┼──────────┤
│ Import        │ UserService.java   │ javax.servlet  │ [Fix]   │
│ XML Namespace │ persistence.xml    │ Update xmlns   │ [Fix]   │
│ Reflection    │ ConfigLoader.java  │ Class.forName  │ [Manual]│
│ Package       │ User.java         │ javax package  │ [Fix]   │
└─────────────────────────────────────────────────────────────────┘
```

---

### 2. Add Runtime Tab

**Location:** MigrationToolWindow.java

**Components:**
- Stack Trace Analyzer section:
  - Text area for pasting stack traces
  - "Analyze" button
  - Results display area
- Future additions (placeholder):
  - Runtime error log upload
  - Application health checks
  - Memory analysis

**Mockup:**
```
┌─────────────────────────────────────────────────────────────────┐
│ Runtime Analysis                                                │
├─────────────────────────────────────────────────────────────────┤
│ Paste stack trace here:                                         │
│ ┌─────────────────────────────────────────────────────────┐   │
│ │                                                         │   │
│ │ java.lang.NoClassDefFoundError: javax/servlet/...       │   │
│ │     at com.example.MyServlet.doGet(MyServlet.java:25)    │   │
│ │     ...                                                 │   │
│ └─────────────────────────────────────────────────────────┘   │
│                                                              │
│ [Analyze]                                                    │
│                                                              │
│ Results:                                                     │
│ ┌─────────────────────────────────────────────────────────┐   │
│ │ Category: NAMESPACE_MIGRATION                           │   │
│ │ Root Cause: javax class not migrated to jakarta          │   │
│ │ Confidence: 0.9                                         │   │
│ │                                                          │   │
│ │ Suggested Fixes:                                        │   │
│ │ 1. Update import statements                              │   │
│ │ 2. Add Jakarta dependency                                │   │
│ └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

### 3. Hide Migration Phases UI Section

**Location:** MigrationToolWindow.java or existing UI components

**Reason:**
- Migration phases feature is immature
- Not ready for production use
- Can be re-enabled when feature is complete

**Implementation:**
- Comment out or remove migration phases panel
- Add TODO comment for future re-enablement

---

### 4. Enhance Dependencies Tab

**Add to existing Dependencies tab:**

#### 4.1 XML Namespace Occurrences Section

**Components:**
- Table showing XML files with javax namespace references
- Columns:
  - File Path
  - Namespace Type (persistence.xml, web.xml, faces-config.xml)
  - Issue Count
  - Severity (warning, error)

**Mockup:**
```
XML Namespace Issues (5 files)
┌────────────────────────────────────────────────┐
│ File                │ Type         │ Count    │
│────────────────────┼──────────────┼──────────┤
│ persistence.xml     │ persistence  │ 3        │
│ web.xml            │ javaee      │ 2        │
└────────────────────────────────────────────────┘
```

#### 4.2 Reflection Usage Occurrences Section

**Components:**
- Table showing reflection-based javax usage
- Columns:
  - File Path
  - Pattern Type (Class.forName, loadClass, etc.)
  - Affected Class
  - Criticality (high, medium, low)

**Mockup:**
```
Reflection Usage (8 occurrences)
┌────────────────────────────────────────────────────┐
│ File              │ Pattern     │ Class          │ Crit │
│──────────────────┼─────────────┼────────────────┼──────┤
│ ConfigLoader.java │ forName     │ javax.servlet  │ HIGH │
│ Factory.java     │ loadClass   │ javax.mail     │ MED  │
└────────────────────────────────────────────────────┘
```

---

## Files to Modify

| File | Change |
|------|--------|
| `MigrationToolWindow.java` | Add tabs, refactor UI structure |
| `MigrationToolWindow.form` | Add UI components (if using GUI designer) |
| `MigrationAnalysisPersistenceService.java` | Add query methods for refactoring tasks |
| `SqliteMigrationAnalysisStore.java` | Add table/index for refactoring tasks |
| `plugin.xml` | Register new tabs if needed |

---

## Database Schema Updates

### New Table: refactoring_tasks

```sql
CREATE TABLE IF NOT EXISTS refactoring_tasks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    analysis_id INTEGER NOT NULL,
    task_type TEXT NOT NULL,  -- 'import', 'package', 'xml_namespace', 'reflection'
    file_path TEXT NOT NULL,
    description TEXT,
    original_value TEXT,
    target_value TEXT,
    auto_fix_supported BOOLEAN DEFAULT FALSE,
    status TEXT DEFAULT 'pending',  -- 'pending', 'fixed', 'skipped'
    premium_required BOOLEAN DEFAULT FALSE,
    line_number INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (analysis_id) REFERENCES analysis_reports(id)
);

CREATE INDEX idx_refactoring_tasks_analysis ON refactoring_tasks(analysis_id);
CREATE INDEX idx_refactoring_tasks_type ON refactoring_tasks(task_type);
```

---

## Implementation Steps

### Phase 1: Foundation
1. Add database schema for refactoring_tasks
2. Create PersistenceService methods
3. Add Refactor tab UI skeleton

### Phase 2: Refactor Tab
4. Implement refactoring tasks table
5. Add manual fix navigation
6. Add auto-fix button (premium check)

### Phase 3: Runtime Tab
7. Implement stack trace analyzer UI
8. Connect to explainRuntimeFailure MCP tool
9. Display results

### Phase 4: Dependencies Tab Enhancement
10. Add XML occurrences section
11. Add reflection occurrences section
12. Load data from persistence

### Phase 5: Cleanup
13. Hide/remove migration phases UI
14. Test all new functionality
15. Update documentation

---

## Premium/Licensing Integration

### Auto-fix Button Logic
```java
private void onAutoFixClicked(RefactoringTask task) {
    if (!featureFlags.isPremium()) {
        showUpgradeDialog("Auto-fix is a premium feature");
        return;
    }
    
    // Perform auto-fix
    performAutoFix(task);
}
```

### UI State
- Free users: Auto-fix button disabled, show upgrade icon/tooltip
- Premium users: Full functionality enabled

---

## Testing Checklist

- [ ] Refactor tab displays tasks correctly
- [ ] Manual fix navigation works
- [ ] Premium check blocks auto-fix for free users
- [ ] Runtime tab accepts stack trace input
- [ ] Runtime analysis displays results
- [ ] Dependencies tab shows XML occurrences
- [ ] Dependencies tab shows reflection occurrences
- [ ] Migration phases section is hidden
- [ ] All UI transitions are smooth
- [ ] Loading states are shown

---

## Related Documentation

- [`docs/requirements/intellij-plugin-ui.md`](docs/requirements/intellij-plugin-ui.md) - Original UI requirements
- [`docs/improvements/INTELLIJ_MARKETPLACE_PUBLISHING_2026-02-04.md`](INTELLIJ_MARKETPLACE_PUBLISHING_2026-02-04.md) - Marketplace publishing
- [MigrationToolWindow.java] - Main tool window implementation
