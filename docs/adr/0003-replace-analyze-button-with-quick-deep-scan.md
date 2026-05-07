# ADR 0003: Replace Analyze Button with Quick Scan & Deep Scan

## Status
Accepted

## Context
The original UI featured a single "▶ Analyze Project" button in the main toolbar. This triggered a full deep scan every time, which:
- Could be slow for large projects due to transitive dependency analysis
- Did not give users flexibility to perform quick checks
- Spent time on expensive transitive analysis even when not needed

Additionally, the "Advanced Scans" tab contained its own "Run Advanced Scans" button, duplicating functionality that should be centrally controlled.

## Decision
Replace the single analyze button with two vertically stacked buttons:

1. **Quick Scan** (fast):
   - Direct dependency analysis (declared dependencies only)
   - Source code scanning (all AdvancedScanners *except* transitive dependencies)
   - Platform detection
   - No credit consumption for free users (or less impact)

2. **Deep Scan** (full, slower):
   - Full transitive dependency analysis (deep Maven/Gradle tree)
   - All advanced scans (including transitive)
   - Platform detection
   - Premium feature or consumes 1 action credit for free users

Both scan types update the UI consistently:
- Progress bar indicates current phase (3 phases total)
- Results populate Dashboard, Dependencies tab, Source Scans tab, and Platforms tab

The "Advanced Scans" tab is renamed to **"Source Scans"** and its self-scan button removed; it becomes a passive display for scan results.

## Technical Changes

### MigrationToolWindow
- Replaced `analyzeButton` field with `quickScanButton` and `deepScanButton`
- Implemented `handleQuickScan()` and `handleDeepScan()` action handlers
- Extracted `performDeepScan()` method from the old `runFullAnalysis()` logic
- Added `setScanButtonsEnabled()` helper for UI state management
- `DashboardComponent` external analyze button set to `null` (no longer needed)
- Removed obsolete methods: `handleAnalyzeProject()`, `runBasicAnalysis()`, `runBasicAnalysisWithTruncation()`, `truncateReport()`, `runFullAnalysis()`
- Updated `refreshFromLibrary()` to call `handleDeepScan()`

### AdvancedScanningService
- `scanAllExcludingTransitive()` method already exists (lines 706-721) and is used by Quick Scan

### SourceScansComponent (renamed from AdvancedScansComponent)
- Removed toolbar with its "Run Advanced Scans" button
- Removed `scanButton` and `progressBar` fields
- Deleted `createToolbar()` method completely
- Component is now a pure display tab; all scanning controlled from main toolbar
- Kept `refreshFromCachedResults()` and listener infrastructure for external update notifications

### UI / UX
- Buttons are vertically stacked, width 150px, height half original double-height button minus 5px spacing
- Toolbar progress bar remains centered; three phases shown: "Basic Dependency Analysis" / "Source Code Scanning" / "Platform Detection" for Quick Scan; "Deep Dependency Analysis" / "Advanced Scans" / "Platform Detection" for Deep Scan
- Tab label changed from "Advanced Scans" to "Source Scans"
- Premium badge added to tab title for premium users

### Credits
- Deep Scan: premium only or consumes 1 action credit (free users)
- Quick Scan: free for all (no credit check)

## Consequences

### Positive
- **Faster feedback** for quick checks (no transitive analysis)
- **Better UX**: users choose depth of analysis
- **Clear separation** of concerns; scanning is centralized
- **Cleaner UI**: no redundant buttons
- **Alignment with freemium model**: Deep Scan as a premium/credit feature

### Negative
- **Breaking change**: Users accustomed to the single analyze button must adapt
- **Two clicks** instead of one for full analysis (but immediate Quick Scan)
- **Legacy menu action** (`Tools → Jakarta Migration → Analyze Readiness`) still shows basic analysis only (may need future update to offer choice)

## Implementation Notes
- All affected methods reference this specification: `.kilo/plans/1778130109717-neon-tiger.md`
- Full backward compatibility not guaranteed; deprecated `handleAnalyzeProject` kept as stub showing info message
- Tests updated to use `SourceScansComponent` class name

## References
- Plan file: `.kilo/plans/1778130109717-neon-tiger.md`
- MigrationToolWindow.java
- SourceScansComponent.java
- AdvancedScanningService.java (existing method `scanAllExcludingTransitive`)
