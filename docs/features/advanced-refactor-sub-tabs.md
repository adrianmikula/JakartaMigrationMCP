# Advanced Refactor Sub-Tabs Feature

## Overview
The Refactoring tab has been enhanced with sub-tabs to separate basic single-recipe refactoring from advanced multi-recipe sequence testing.

## Architecture

### Component Structure
```
RefactorTabComponent (main container)
├── JTabbedPane (sub-tabs)
│   ├── Basic Refactor (BasicRefactorComponent)
│   └── Advanced Refactor (AdvancedRefactorComponent)
│       ├── Sequence Builder (SequenceBuilderPanel)
│       ├── Run Experiment (ExperimentRunnerPanel)
│       └── History (ExperimentHistoryPanel)
```

### Key Components

#### BasicRefactorComponent
- Extracted from the original RefactorTabComponent
- Provides single-recipe refactoring functionality
- Available to all users (free users limited by credits)
- Features:
  - Recipe cards organized by category
  - Details panel with Apply/Undo buttons
  - File modification results display
  - Credit tracking for free tier

#### AdvancedRefactorComponent
- Premium-gated feature
- Contains three sub-panels for sequence management
- Features:
  - Premium upgrade prompt for free users
  - Feature flag check for EXPERIMENT_ENGINE
  - Cross-panel communication

#### SequenceBuilderPanel
- UI for building recipe sequences
- Features:
  - Available recipes list with category filtering
  - Selected sequence list with reorder controls
  - Sequence metadata (name, description)
  - Save sequence functionality

#### ExperimentRunnerPanel
- UI for running experiments
- Features:
  - Sequence selector dropdown
  - Optional git ref specification
  - Progress tracking
  - Results display with step-by-step details

#### ExperimentHistoryPanel
- UI for viewing experiment history
- Features:
  - History table with status filtering
  - Color-coded status indicators
  - Load sequence from history for re-editing
  - Detailed experiment results view

#### ExperimentService
- Service layer integrating with premium-experiment-engine
- Provides simplified API for UI components
- Features:
  - Recipe management
  - Sequence CRUD operations
  - Experiment execution
  - History retrieval
  - Experiment comparison

## Premium Feature Gating

### License Check
```java
boolean isPremium = CheckLicense.isLicensed() != null && CheckLicense.isLicensed();
```

### Feature Flag
The Advanced Refactor uses the `EXPERIMENT_ENGINE` feature flag from `FeatureFlag.EXPERIMENT_ENGINE`.

### Free User Experience
- Advanced Refactor tab is visible but shows upgrade prompt
- Premium badge displayed as lock icon (🔒)
- Upgrade button links to JetBrains Marketplace

### Premium User Experience
- Full access to all Advanced Refactor features
- Premium badge displayed as star (⭐)
- No upgrade prompts

## Integration with Premium Experiment Engine

The Advanced Refactor integrates with the `premium-experiment-engine` module through:

1. **ExperimentTools**: Main facade for experiment operations
2. **SequenceService**: Manages migration sequences
3. **HistoryService**: Tracks experiment results
4. **ExperimentRunner**: Executes experiments in testcontainers

### Testcontainers Integration
The experiment engine uses testcontainers to:
- Create isolated Docker environments
- Apply refactor sequences safely
- Run tests to verify changes
- Compare results before/after

## Data Models

### MigrationSequence
```java
record MigrationSequence(
    String name,
    String description,
    List<SequenceStep> steps,
    Instant createdAt,
    List<String> tags
)
```

### SequenceStep
```java
record SequenceStep(
    SequenceStepType type,
    String recipe,
    String recipeVersion,
    // ... additional fields for different step types
    Map<String, Object> extra
)
```

### ExperimentResult
```java
record ExperimentResult(
    String runId,
    String sequenceName,
    Instant startedAt,
    Instant finishedAt,
    ExperimentStatus status,
    List<StepResult> stepResults,
    TestOutcome testOutcome,
    String diffSummary,
    String errorMessage
)
```

## User Flow

### Building a Sequence
1. User opens Advanced Refactor tab
2. Navigates to Sequence Builder
3. Selects recipes from available list
4. Orders steps using up/down buttons
5. Enters sequence name and description
6. Clicks "Save Sequence"

### Running an Experiment
1. User navigates to Run Experiment tab
2. Selects a saved sequence from dropdown
3. Optionally specifies a git ref
4. Clicks "Run Experiment"
5. System executes in testcontainers
6. Results displayed with status and details

### Viewing History
1. User navigates to History tab
2. Sees list of past experiments
3. Can filter by status
4. Clicks "Load Sequence" to edit and re-run
5. Clicks "View Details" for full results

## Testing

### Unit Tests
- `BasicRefactorComponentTest`: Tests basic refactor component
- `AdvancedRefactorComponentTest`: Tests advanced refactor component
- `ExperimentServiceTest`: Tests service layer integration

### Test Coverage
- Component creation and initialization
- Callback registration
- Service method delegation
- Premium gating logic

## Future Enhancements

1. **Drag-and-drop reordering**: Improve sequence builder UX
2. **Keyboard shortcuts**: Add quick actions for common operations
3. **Sequence templates**: Pre-defined common sequences
4. **Sequence sharing**: Export/import sequences between projects
5. **Comparison reports**: Side-by-side comparison of experiment runs
6. **CI/CD integration**: Automate experiment runs in pipelines

## Files Modified

### New Files
- `premium-intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/refactor/BasicRefactorComponent.java`
- `premium-intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/refactor/AdvancedRefactorComponent.java`
- `premium-intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/refactor/SequenceBuilderPanel.java`
- `premium-intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/refactor/ExperimentRunnerPanel.java`
- `premium-intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/refactor/ExperimentHistoryPanel.java`
- `premium-intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/service/ExperimentService.java`
- `premium-intellij-plugin/src/test/java/adrianmikula/jakartamigration/intellij/ui/refactor/BasicRefactorComponentTest.java`
- `premium-intellij-plugin/src/test/java/adrianmikula/jakartamigration/intellij/ui/refactor/AdvancedRefactorComponentTest.java`
- `premium-intellij-plugin/src/test/java/adrianmikula/jakartamigration/intellij/service/ExperimentServiceTest.java`

### Modified Files
- `premium-intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/RefactorTabComponent.java`: Refactored to use sub-tabs
- `premium-intellij-plugin/build.gradle.kts`: Added premium-experiment-engine dependency
- `premium-intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/MigrationToolWindow.java`: Fixed logging issues

## Dependencies

### Added Dependencies
- `premium-experiment-engine`: For experiment execution and testcontainers integration

### Existing Dependencies Used
- `premium-core-engine`: For RecipeService and refactor operations
- `community-core-engine`: For shared domain models
