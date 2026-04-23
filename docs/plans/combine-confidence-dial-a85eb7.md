# Updated Confidence Dial Implementation Plan

This plan combines confidence score and validation score into a single 'confidence' dial, and fixes migration effort dial to properly include project size factor.

## Current State Analysis
- Two separate gauges: ConfidenceGauge (dependency knowledge) and ValidationConfidenceGauge (test coverage)
- Migration effort only considers automation score + organizational dependencies, missing project size
- Project size is calculated correctly (`getTotalFileCount()`) but not used in effort calculation
- 4-gauge layout: Risk, Effort, Confidence Score, Validation Confidence

## Updated Implementation Steps

### 1. Create Combined Confidence Gauge Component
- Create new `CombinedConfidenceGauge` class extending `ScoreGauge`
- Combine two factors with 50/50 weighting:
  - Dependency knowledge (50%): percentage of dependencies with known Jakarta status
  - Test coverage (50%): enhanced test coverage analysis
- Use appropriate color scheme for combined confidence metric

### 2. Fix Migration Effort Calculation
- Add project size factor to effort scoring configuration in `risk-scoring.yaml`
- Update effort weights to: automation (40%), organizational dependencies (30%), project size (30%)
- Add `calculateProjectSizeScore()` method using inverse logic (larger projects = higher effort)
- Modify `calculateEffortScore()` to include all three factors

### 3. Update DashboardComponent Layout
- Remove ValidationConfidenceGauge from 4-gauge layout
- Replace ConfidenceGauge with CombinedConfidenceGauge
- Update layout from 4 gauges to 3 gauges (Risk, Effort, Confidence)

### 4. Update Explanation Panels
- Combine confidence and validation explanation panels into single panel
- Show two bullet points:
  - "Dependencies with known status (%)"
  - "Test coverage for migration risk (%)"
- Update effort explanation to include project size bullet

### 5. Configuration Updates
- Add project size scoring configuration to `risk-scoring.yaml`:
  ```yaml
  effortScoring:
    weights:
      automationScore: 0.4
      organisationalDepsScore: 0.3
      projectSizeScore: 0.3
    thresholds:
      maxProjectFiles: 10000  # Cap for project size calculation
  ```

### 6. Update Calculation Methods
- Modify `calculateConfidenceScore()` to combine dependency knowledge + test coverage
- Remove `calculateEnhancedValidationConfidence()` method
- Update `updateGauges()` to use new combined calculations

## Technical Decisions
- Confidence: 50% dependency knowledge + 50% test coverage (no project size)
- Effort: 40% automation + 30% organizational deps + 30% project size
- Layout: 3-gauge layout for cleaner dashboard
- Project size logic: larger projects increase effort score (more complexity)

## Files to Modify
1. `DashboardComponent.java` - Main integration and calculations
2. New `CombinedConfidenceGauge.java` - Gauge component
3. `risk-scoring.yaml` - Add project size scoring configuration
4. Remove `ValidationConfidenceGauge.java` - No longer needed
