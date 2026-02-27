# SDD: Refactor and Runtime Tabs

## 1. Overview

Add two new tabs to the IntelliJ Migration Tool Window:

1. **Refactor Tab** - Display available OpenRewrite recipes in a table with "Apply" buttons
2. **Runtime Tab** - Text area for pasting runtime errors with diagnosis and recommendations

## 2. Refactor Tab

### 2.1 Purpose
Display available refactoring recipes (OpenRewrite) that can be applied to migrate from javax to jakarta.

### 2.2 UI Components

| Component | Description |
|-----------|-------------|
| JTable | Table showing recipes with columns: Name, Description, Safety, Actions |
| Apply Button | Button in each row to apply that specific recipe |
| Refresh Button | Button to reload recipes from RecipeLibrary |

### 2.3 Data Model

```java
// Recipe data from RecipeLibrary
record RecipeInfo(
    String name,
    String description,
    String pattern,
    SafetyLevel safety,
    boolean reversible
)
```

### 2.4 Recipes to Display

| Recipe Name | Description | Safety |
|-------------|-------------|--------|
| AddJakartaNamespace | Converts javax.* imports to jakarta.* | HIGH |
| UpdatePersistenceXml | Updates persistence.xml namespace | HIGH |
| UpdateWebXml | Updates web.xml namespace | MEDIUM |

### 2.5 User Flow

1. User navigates to Refactor tab
2. Table displays all available recipes
3. User clicks "Apply" on a recipe
4. Dialog prompts for files/directory to refactor
5. Progress shown during refactoring
6. Results displayed (files modified, any errors)

## 3. Runtime Tab

### 3.1 Purpose
Allow users to paste runtime errors from their migrated application and get AI-powered diagnosis with remediation recommendations.

### 3.2 UI Components

| Component | Description |
|-----------|-------------|
| JTextArea | Large text area for pasting stack traces/errors |
| Diagnose Button | Button to analyze the errors |
| Results Panel | Panel showing diagnosis results |

### 3.3 Diagnosis Results Display

| Section | Description |
|---------|-------------|
| Error Category | NAMESPACE_MIGRATION, CLASSPATH_ISSUE, BINARY_INCOMPATIBILITY, CONFIGURATION_ERROR |
| Root Cause | Human-readable explanation of the error |
| Contributing Factors | List of factors that may be contributing |
| Confidence Score | How confident the system is in the diagnosis |
| Remediation Steps | Ordered list of steps to fix the issue |

### 3.4 User Flow

1. User navigates to Runtime tab
2. User pastes error output/stack trace into text area
3. User clicks "Diagnose"
4. System parses errors and analyzes them
5. Results displayed with root cause and remediation steps

## 4. Technical Implementation

### 4.1 New UI Components

```
premium-intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/
├── RefactorTabComponent.java     # New - Refactor tab UI
├── RuntimeTabComponent.java     # New - Runtime tab UI
└── MigrationToolWindow.java    # Modified - Add new tabs
```

### 4.2 Dependencies

- RecipeLibrary from community-core-engine
- ErrorAnalyzer from community-core-engine
- MigrationAnalysisService (existing)

### 4.3 Services Needed

1. **RecipeService** - Wrapper around RecipeLibrary for UI consumption
2. **RuntimeDiagnosisService** - Wrapper around ErrorAnalyzer for UI consumption

## 5. Acceptance Criteria

### Refactor Tab
- [ ] Table displays all available recipes
- [ ] Each recipe has an "Apply" button
- [ ] Clicking Apply shows file selection dialog
- [ ] Refactoring results are displayed after completion

### Runtime Tab
- [ ] Text area accepts multi-line error input
- [ ] "Diagnose" button triggers analysis
- [ ] Error category is displayed
- [ ] Root cause explanation is shown
- [ ] Remediation steps are displayed with ordering
- [ ] Confidence score is shown

## 6. Test Scenarios (TDD)

### Refactor Tab Tests
1. `testRecipeTablePopulated` - Verify recipes load in table
2. `testApplyButtonTriggersRefactor` - Verify button click starts refactor
3. `testRefactorResultsDisplayed` - Verify results show after refactor

### Runtime Tab Tests
1. `testErrorParsing` - Verify errors are parsed from input
2. `testDiagnosisResultsDisplayed` - Verify diagnosis shows results
3. `testRemediationStepsShown` - Verify remediation steps display
