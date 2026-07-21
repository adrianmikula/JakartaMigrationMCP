# Advanced Refactor Sub-Tabs Specification

## Overview
This specification describes the redesign of the Refactoring tab to include sub-tabs, separating basic single-recipe refactoring (Basic Refactor) from advanced multi-recipe sequence testing (Advanced Refactor).

## Goals
1. **Separate concerns**: Distinguish between simple single-recipe application and complex sequence testing
2. **Premium feature gating**: Advanced Refactor is a premium-only feature
3. **Experiment engine integration**: Leverage premium-experiment-engine for safe testing using testcontainers
4. **History tracking**: Allow users to review past refactor experiments and re-use them

## UI Structure

### Refactoring Tab Layout
The Refactoring tab will contain two sub-tabs:

1. **Basic Refactor** (current functionality, available to all users)
   - Single recipe selection and application
   - Recipe cards organized by category
   - Details panel with Apply/Undo buttons
   - File modification results display

2. **Advanced Refactor** (new premium feature)
   - Recipe sequence builder
   - Experiment runner using testcontainers
   - Experiment history viewer
   - Load past experiments for re-editing

### Sub-Tab Navigation
- Use `JTabbedPane` at the top of the Refactoring tab
- Tab labels: "Basic Refactor" and "Advanced Refactor ⭐"
- Premium badge on Advanced Refactor tab
- Free users see Advanced Refactor tab but it shows upgrade prompt when selected

## Advanced Refactor Features

### 1. Recipe Sequence Builder

#### UI Components
- **Available Recipes Panel**: List of all available recipes (from RecipeService)
- **Selected Sequence Panel**: Shows current sequence being built
- **Add/Remove buttons**: Move recipes between panels
- **Sequence metadata**: Name, description fields
- **Save Sequence button**: Save sequence for future use

#### Data Model
```java
// Use existing MigrationSequence from premium-experiment-engine
MigrationSequence {
    String name
    String description
    List<SequenceStep> steps
    Instant createdAt
    List<String> tags
}
```

#### User Flow
1. User sees list of available recipes (grouped by category)
2. User clicks recipes to add them to the sequence
3. User can reorder steps using drag-and-drop or up/down buttons
4. User enters sequence name and description
5. User clicks "Save Sequence" to persist

### 2. Experiment Runner

#### UI Components
- **Sequence Selector**: Dropdown to select saved sequence
- **Run Experiment button**: Triggers testcontainers execution
- **Progress indicator**: Shows experiment progress
- **Results panel**: Displays experiment results
  - Success/failure status
  - Files modified count
  - Test results (pass/fail/skip)
  - Step-by-step execution details

#### Integration with premium-experiment-engine
```java
// Use ExperimentTools MCP interface
ExperimentTools experimentTools = new ExperimentTools(
    projectRoot,
    containerFactory,
    dockerImage,
    testTimeoutSeconds
);

// Run experiment
ExperimentResult result = experimentTools.runMigrationExperiment(
    sequenceName,
    projectPath,
    Optional.of(gitRef)  // Optional: run on specific git ref
);
```

#### Execution Flow
1. User selects a saved sequence
2. User optionally selects a git ref (defaults to current HEAD)
3. User clicks "Run Experiment"
4. System:
   - Creates temporary directory
   - Copies project snapshot (or git archive if ref specified)
   - Starts testcontainer with specified Docker image
   - Executes each step in sequence
   - Runs project tests
   - Records results
5. Results displayed in UI

### 3. Experiment History

#### UI Components
- **History Table**: Shows past experiments
  - Columns: Date, Sequence Name, Status, Files Modified, Test Results
  - Color-coded status (green=success, red=failure, yellow=running)
- **Filter controls**: Filter by sequence name, status, date range
- **Load button**: Click to load sequence from history into builder
- **View Details button**: Show full experiment results

#### Data Model
```java
// Use existing ExperimentResult from premium-experiment-engine
ExperimentResult {
    String runId
    String sequenceName
    Instant startedAt
    Instant finishedAt
    ExperimentStatus status
    List<StepResult> stepResults
    TestOutcome testOutcome
    String diffSummary
    String errorMessage
}
```

#### User Flow
1. User sees list of past experiments (most recent first)
2. User can filter by sequence name or status
3. User clicks on an experiment row to view details
4. User clicks "Load Sequence" to copy that sequence into the builder for editing
5. User can modify and run as a new experiment

### 4. Premium Feature Gating

#### License Check
```java
boolean isPremium = CheckLicense.isLicensed();
boolean experimentEngineEnabled = FeatureFlags.getInstance()
    .isFeatureEnabled(FeatureFlag.EXPERIMENT_ENGINE);
```

#### Free User Experience
- Advanced Refactor tab is visible
- When selected, shows upgrade prompt:
  - "Advanced Refactor requires Premium"
  - "Test refactor sequences safely using containers"
  - Upgrade button to JetBrains Marketplace
  - Link to pricing information

#### Premium User Experience
- Full access to all Advanced Refactor features
- No upgrade prompts
- Premium badge visible on tab

## Technical Implementation

### Component Structure
```
RefactorTabComponent (main container)
├── JTabbedPane (sub-tabs)
│   ├── BasicRefactorComponent (existing RefactorTabComponent logic)
│   └── AdvancedRefactorComponent (new)
│       ├── SequenceBuilderPanel
│       │   ├── AvailableRecipesList
│       │   ├── SelectedSequenceList
│       │   └── SequenceMetadataForm
│       ├── ExperimentRunnerPanel
│       │   ├── SequenceSelector
│       │   ├── RunButton
│       │   └── ResultsDisplay
│       └── ExperimentHistoryPanel
│           ├── HistoryTable
│           ├── FilterControls
│           └── DetailView
```

### Key Classes to Create
1. `AdvancedRefactorComponent` - Main container for Advanced Refactor sub-tab
2. `SequenceBuilderPanel` - UI for building recipe sequences
3. `ExperimentRunnerPanel` - UI for running experiments
4. `ExperimentHistoryPanel` - UI for viewing experiment history
5. `ExperimentService` - Service layer integrating with premium-experiment-engine

### Service Integration
```java
public class ExperimentService {
    private final ExperimentTools experimentTools;
    private final RecipeService recipeService;
    
    public List<RecipeDefinition> getAvailableRecipes(Path projectPath);
    public MigrationSequence saveSequence(MigrationSequence sequence);
    public List<MigrationSequence> listSequences();
    public ExperimentResult runExperiment(String sequenceName, Path projectPath, Optional<String> gitRef);
    public List<ExperimentResult> getHistory(Optional<String> sequenceName, Optional<ExperimentStatus> status, int limit);
}
```

## Sequence Step Types

The Advanced Refactor supports the following step types (from premium-experiment-engine):

1. **OPENREWRITE** - Apply OpenRewrite recipe
2. **DEPENDENCY_UPGRADE** - Upgrade dependency versions
3. **ECLIPSE_TRANSFORMER** - Apply Eclipse transformer
4. **GRADLE_JAKARTA_PLUGIN** - Apply Gradle Jakarta plugin
5. **REGEX_REPLACEMENT** - Apply regex-based replacements

Each step type has specific configuration parameters.

## Error Handling

### Container Execution Errors
- Docker not available: Show error message with Docker installation instructions
- Container start failure: Show error with container logs
- Step execution failure: Show which step failed and why
- Test execution failure: Show test results with failure details

### License Errors
- License expired: Show renewal prompt
- License validation failed: Show contact support message

## Testing Strategy

### Unit Tests
- `AdvancedRefactorComponentTest` - UI component tests
- `SequenceBuilderPanelTest` - Sequence builder logic
- `ExperimentServiceTest` - Service layer tests
- `ExperimentHistoryPanelTest` - History display tests

### Integration Tests
- Test experiment runner with mock testcontainers
- Test history persistence
- Test sequence save/load

### UI Tests
- Test sub-tab navigation
- Test premium gating behavior
- Test sequence builder interactions
- Test history loading and filtering

## Migration Path

### Phase 1: UI Structure
1. Create sub-tab layout in RefactorTabComponent
2. Move existing logic to BasicRefactorComponent
3. Create placeholder AdvancedRefactorComponent with premium gating

### Phase 2: Sequence Builder
1. Implement SequenceBuilderPanel
2. Integrate with RecipeService
3. Implement sequence save/load

### Phase 3: Experiment Runner
1. Implement ExperimentRunnerPanel
2. Integrate with premium-experiment-engine
3. Add progress tracking and result display

### Phase 4: History
1. Implement ExperimentHistoryPanel
2. Integrate with HistoryService
3. Add load-from-history functionality

### Phase 5: Polish
1. Add drag-and-drop reordering
2. Add keyboard shortcuts
3. Improve error messages
4. Add tooltips and help text

## Performance Considerations

- Experiment execution runs asynchronously to avoid blocking UI
- History loading uses pagination (limit 50 results by default)
- Recipe list is cached and refreshed only when needed
- Testcontainers execution uses configurable timeout (default 300s)

## Security Considerations

- Git ref execution is optional and requires explicit user selection
- Container execution runs in isolated environment
- No direct file system modifications on user's project during experiment
- All experiment artifacts stored in `.experiments` directory (gitignored)

## Future Enhancements

- Sequence templates (pre-defined common sequences)
- Sequence sharing between projects
- Comparison of multiple experiment runs
- Export experiment results as reports
- Integration with CI/CD pipelines
