# ADR 0002: Consolidate Platform Result Objects

## Status
Accepted

## Context
The codebase contained two separate domain objects for platform scan results:
- `PlatformScanResult`: A record with rich `PlatformDetection` objects, risk score, and recommendations
- `EnhancedPlatformScanResult`: A class with simple platform name strings, inferred platforms, and deployment artifact counts

This duplication created several issues:
1. **Dead Code**: `PlatformScanResult` was defined but never instantiated in production code
2. **Data Model Inconsistency**: Two different representations of platform data (rich objects vs simple strings)
3. **Missing Integration**: PDF report service expected `PlatformScanResult` but detection service only provided `EnhancedPlatformScanResult`
4. **Redundant Abstraction**: Two objects serving similar purposes with overlapping concerns

## Decision
Consolidate both objects into a single `EnhancedPlatformScanResult` that:
- Contains raw data only (platforms, artifacts, platform-specific dependencies)
- Includes rich `PlatformDetection` objects with version, compatibility, and requirements from config
- Does NOT contain risk scores or recommendations (these must be calculated by `RiskScoringService`)
- Maintains backward compatibility during transition

## Risk Scoring Architecture
To ensure all risk scoring reuses existing logic from `risk-score.yaml` via `RiskScoringService`:

1. **Single Source of Truth**: All risk weights and scores come from `risk-scoring.yaml`
2. **No Hardcoded Scores**: Never hardcode risk scores in Java code
3. **Centralized Calculation**: All risk scoring must go through `RiskScoringService`
4. **No Duplication**: Never recalculate the same score multiple times - cache and reuse
5. **Separation of Concerns**: Detection service collects data, `RiskScoringService` calculates risk
6. **Data vs Calculation**: Result objects contain raw data only, risk scores calculated on demand

## Consequences
### Positive
- **Single Source of Truth**: One domain object for all platform scan results
- **Richer Data**: Combines detailed platform info with artifact counts
- **Better Integration**: PDF reports can access both platform details and artifact data
- **Reduced Maintenance**: One object to maintain instead of two
- **Eliminates Dead Code**: Removes unused `PlatformScanResult`
- **Consistent Risk Scoring**: All risk calculations go through `RiskScoringService` using YAML configuration

### Negative
- **Breaking Change**: Requires updates to all consumers of the objects
- **Backward Compatibility**: Needed to ensure smooth transition for existing code
- **Test Coverage**: Must update all tests to use new object structure

### Implementation
1. Enhanced `EnhancedPlatformScanResult` with `List<PlatformDetection> detectedPlatformDetails` field
2. Updated `SimplifiedPlatformDetectionService` to create `PlatformDetection` objects from config (data collection only)
3. Updated `PlatformDetectionSnippet` to use enhanced result and display rich data (no risk calculation)
4. Updated `PdfReportService` to use `EnhancedPlatformScanResult` in all request records
5. Verified UI components use `RiskScoringConfig` from YAML for risk calculations
6. Updated test imports to use `EnhancedPlatformScanResult`
7. Removed dead code: `PlatformScanResult.java` and empty `PlatformDetectionResult.java`
8. Updated spec file `spec/platforms-tab.tsp` to reference consolidated object

## References
- [ADR 0001: Jackson Isolation Filtering ClassLoader](./0001-jackson-isolation-filtering-classloader.md)
- [risk-scoring.yaml](../../premium-core-engine/src/main/resources/config/risk-scoring.yaml)
- [RiskScoringService](../../premium-core-engine/src/main/java/adrianmikula/jakartamigration/risk/RiskScoringService.java)
