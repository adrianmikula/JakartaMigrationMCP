# HTML Refactoring Report Requirements

## Overview
Developer action-oriented redesign of the Jakarta Migration HTML refactoring report.

## Report Structure

### Section 1: Header
**Purpose:** Report identification and branding
**Data Sources:**
- `RefactoringActionReportRequest.projectName()` - Project name display
- `RefactoringActionReportRequest.reportTitle()` - Report title
- System timestamp - Generation date/time
- Static: Plugin icon SVG, version info

**Layout:**
- Left: Plugin icon (32x32px)
- Center: Report title, project name, report type
- Right: Timestamp

---

### Section 2: Executive Summary
**Purpose:** Quick overview of migration scope and readiness
**Data Sources:**
- `scanResults.summary().totalFindings()` - Total javax references found
- `recipeRecommendations.size()` - Number of available recipes
- `refactoringReadiness.get("automationReady")` - Percentage ready for automation
- `priorityRanking.get("highPriority")` - High priority action count

**Layout:**
- Intro paragraph (1-2 sentences)
- 4 metric cards in grid layout:
  - Files Requiring Refactoring
  - OpenRewrite Recipes Available
  - Ready for Automation (%)
  - Priority Actions (high priority count)

---

### Section 3: Migration Checklist
**Purpose:** Phased action plan for developers to follow
**Data Sources:**
- `scanResults.summary()` - Determines which phases are needed
- `recipeRecommendations` - Recipe availability drives checklist items

**Layout:**
- Section heading: "Migration Checklist"
- 4 phases as styled blocks:
  1. **Preparation**
     - [ ] Backup project
     - [ ] Review breaking changes documentation
     - [ ] Set up Jakarta EE 9+ compatible runtime
  2. **Recipe Execution**
     - [ ] Run Jakarta EE 9 namespace migration recipe
     - [ ] Run JPA entity annotation recipe (if applicable)
     - [ ] Run Servlet API migration recipe (if applicable)
  3. **Manual Changes**
     - [ ] Update remaining javax imports (list from `javaxReferences` where recipeAvailable=false)
     - [ ] Update configuration files
     - [ ] Update dependency versions in build files
  4. **Validation**
     - [ ] Compile project
     - [ ] Run unit tests
     - [ ] Run integration tests
     - [ ] Deploy to test environment

---

### Section 4: File-by-File Action Breakdown
**Purpose:** Detailed per-file analysis for targeted refactoring
**Data Sources:**
- `javaxReferences` list - Each item contains:
  - `file` - File path
  - `line` - Line number(s)
  - `reference` - Javax class/reference found
  - `priority` - HIGH, MEDIUM, or LOW
  - `recipeAvailable` - Boolean

**Layout:**
- Section heading: "File-by-File Refactoring Actions"
- Intro paragraph explaining the table
- Single table with columns:
  | File | Line | Javax Reference | Priority | Recipe Available |
  |------|------|-----------------|----------|------------------|
- Rows grouped and color-coded by priority:
  - HIGH: Red background (#fdf2f2)
  - MEDIUM: Yellow background (#fef9e7)
  - LOW: Green background (#e8f8f5)
- Recipe Available column shows "Available" (green) or "Manual Required" (red)

---

### Section 5: OpenRewrite Recipe Execution Guide
**Purpose:** Step-by-step commands for running available recipes
**Data Sources:**
- `recipeRecommendations` list - Each contains:
  - `recipeName` - Name of the recipe
  - `command` - Maven/Gradle command to execute
  - `expectedFiles` - Estimated files to be modified
  - `prerequisites` - Required setup before running

**Layout:**
- Section heading: "OpenRewrite Recipe Execution Guide"
- Intro paragraph
- Recipe cards (one per recipe) with:
  - Recipe name as card title
  - Command in code block (copy-paste ready)
  - Expected files affected count
  - Prerequisites list
- Recipes ordered by recommended execution sequence

**Sample Content:**
```
Recipe: Jakarta EE 9 to 10 Migration
Command: mvn org.openrewrite.maven:rewrite-maven-plugin:run -Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-migrate-java:LATEST -Drewrite.activeRecipes=org.openrewrite.java.migrate.jakarta.JavaxMigrationToJakarta
Expected Files: 15
Prerequisites:
- Maven 3.6+ installed
- Project compiles successfully before migration
```

---

### Section 6: Manual Migration Tasks
**Purpose:** Identify changes requiring developer intervention
**Data Sources:**
- `javaxReferences` filtered where `recipeAvailable=false`
- Each item contains:
  - `file` - File path
  - `reference` - Javax class requiring manual update
  - `reason` - Why it's not automated (if available)

**Layout:**
- Section heading: "Manual Migration Tasks"
- Intro paragraph explaining these require manual work
- Table or list showing:
  | File | Javax Reference | Reason |
  |------|-----------------|--------|
- Or styled cards for each manual task with:
  - File path
  - Javax reference found
  - Jakarta equivalent to use
  - Brief explanation of required changes

---

### Section 8: Validation Checklist
**Purpose:** Post-migration verification steps
**Data Sources:**
- Static best practices checklist
- Context from `scanResults` to customize validation steps

**Layout:**
- Section heading: "Validation Checklist"
- Numbered list in styled container:
  1. Apply OpenRewrite recipes where available
  2. Manually update remaining javax imports to jakarta
  3. Run comprehensive tests to verify functionality
  4. Update configuration files and properties
  5. Validate application startup with Jakarta EE runtime
  6. Perform integration testing with external systems
  7. Update documentation and deployment scripts

---

### Section 9: Footer
**Purpose:** Report metadata and page information
**Data Sources:**
- Static: Plugin name and version ("Jakarta Migration Tool v3.0")
- Page number calculation
- Static: Report type label

**Layout:**
- Left: Plugin info
- Center: Page X of Y
- Right: Report type label

---

## CSS Requirements

### Color Scheme
- Primary accent: `#e74c3c` (red) - for refactoring action theme
- Secondary: `#c0392b` (dark red)
- High priority: `#e74c3c` (red)
- Medium priority: `#f39c12` (orange)
- Low priority: `#27ae60` (green)
- Recipe available: `#27ae60` (green)
- Manual required: `#e74c3c` (red)

### Typography
- Font family: `'Segoe UI', Tahoma, Geneva, Verdana, sans-serif`
- Body: 16px / 1.6 line-height
- Headings: 1.8em, color `#2c3e50`
- Code: `'Courier New', monospace`

### Layout
- Container max-width: 1200px
- Section padding: 30px
- Metric cards: Grid with `auto-fit, minmax(250px, 1fr)`
- Tables: Full width, collapsed borders, hover effects

### Print Media
- Remove shadows
- Ensure all content fits within page margins
- Table font sizes reduced for readability

---

## Data Source Reference

### RefactoringActionReportRequest Fields
| Field | Type | Description |
|-------|------|-------------|
| `projectName` | String | Project being analyzed |
| `reportTitle` | String | Title of the report |
| `dependencyGraph` | DependencyGraph | Dependency analysis results |
| `scanResults` | ComprehensiveScanResults | Code scanning results |
| `recipeRecommendations` | List<RecipeRecommendation> | OpenRewrite recipes |
| `javaxReferences` | List<Map<String,Object>> | Found javax references |
| `openRewriteRecipes` | List<Map<String,Object>> | Recipe details |
| `refactoringReadiness` | Map<String,Integer> | Automation readiness metrics |
| `priorityRanking` | Map<String,Integer> | Priority counts |
| `customData` | Map<String,Object> | Additional context |

### Scan Results Structure
| Field | Type | Description |
|-------|------|-------------|
| `summary().totalFindings()` | int | Total javax references |
| `summary().categoriesFound()` | List<String> | API categories detected |
| `findings()` | List<Finding> | Detailed findings per file |
| `codeExamples()` | List<CodeExample> | Before/after examples |

---

## Verification Checklist

Completed:
- [x] No hardcoded strings in snippets (except structural HTML)
- [x] All displayed data originates from actual method calls on scan/risk/recipe objects
- [x] No estimated times or completion guidance
- [x] No assumed jakarta equivalents (only use what scanners provide)
- [x] No placeholder content or "example" data
- [x] When data is null/missing, section displays "No data available" or is hidden
- [x] All tests verify data-driven output only
- [x] Deleted hardcoded snippets: MigrationChecklistSnippet, RecipeExecutionGuideSnippet, ManualTasksSnippet, FileByFileActionSnippet
- [x] Created data-driven snippets: ScanSummarySnippet, ScanFindingsByCategorySnippet, ScannerRecommendationsSnippet
- [x] Updated RefactoringSnippetFactory to use only data-driven snippets

## Implementation Complete

**Files Created:**
- `ScanSummarySnippet.java` - Displays actual scan statistics from ComprehensiveScanResults.summary()
- `ScanFindingsByCategorySnippet.java` - Displays actual findings from scan result maps
- `ScannerRecommendationsSnippet.java` - Displays verbatim scanner recommendations

**Files Deleted (Hardcoded):**
- `MigrationChecklistSnippet.java`
- `RecipeExecutionGuideSnippet.java`
- `ManualTasksSnippet.java`
- `FileByFileActionSnippet.java`

**Files Updated:**
- `RefactoringSnippetFactory.java` - Now creates only data-driven snippets
- `ScanSummarySnippetTest.java` - Tests for actual data display
- `ScannerRecommendationsSnippetTest.java` - Tests for verbatim recommendation display
