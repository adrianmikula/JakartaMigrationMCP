# Platforms Tab Implementation Specification

## Overview
This specification defines the implementation of the Platforms tab for the Jakarta Migration IntelliJ plugin, following the requirements in `docs/requirements/PLATFORMS.md`.

## Requirements Summary
- **Premium Feature**: Platforms tab is a premium feature with licensing controls
- **Platform Detection**: Scans project for application servers and determines Jakarta compatibility
- **Risk Scoring**: Updates dashboard risk scores based on detected platforms
- **Configuration**: Loads platform definitions from `platforms.yaml`

## Architecture

### Components
1. **Premium Core Module**: PlatformDetectionService
2. **Premium IntelliJ Module**: PlatformsTabComponent
3. **Configuration**: `platforms.yaml` with platform definitions
4. **Feature Flags**: Premium feature gating

### Data Flow
```
User clicks "Analyse Project" → PlatformDetectionService scans project → 
Updates PlatformsTabComponent UI → Recalculates dashboard risk scores
```

## Implementation Details

### 1. Platform Detection Service (Premium Core)

#### Class: `PlatformDetectionService`
- **Location**: `premium-core-engine/src/main/java/adrianmikula/jakartamigration/platforms/service/PlatformDetectionService.java`
- **Purpose**: Scans project for application servers using patterns from platforms.yaml

#### Key Methods:
```java
public class PlatformDetectionService {
    public PlatformScanResult scanProject(Project project)
    public PlatformDetection detectPlatform(Path projectPath, PlatformConfig config)
    public List<PlatformDetection> scanForPlatforms(Path projectPath)
    public int calculateRiskScore(List<PlatformDetection> detections)
}
```

#### Data Models:
```java
public record PlatformDetection(
    String platformType,
    String platformName,
    String detectedVersion,
    boolean isJakartaCompatible,
    String minJakartaVersion,
    Map<String, String> requirements
) {}

public record PlatformScanResult(
    List<PlatformDetection> detectedPlatforms,
    int totalRiskScore,
    List<String> recommendations
) {}
```

### 2. Platform Configuration Models

#### Class: `PlatformConfig`
- **Location**: `premium-core-engine/src/main/java/adrianmikula/jakartamigration/platforms/model/PlatformConfig.java`
- **Purpose**: Represents platform configuration from YAML

```java
public record PlatformConfig(
    String name,
    String description,
    List<DetectionPattern> patterns,
    JakartaCompatibility jakartaCompatibility,
    List<String> javaxVersions,
    Map<String, String> requirements
) {}

public record DetectionPattern(
    String file,
    String regex,
    int versionGroup
) {}

public record JakartaCompatibility(
    String minVersion,
    List<String> supportedVersions
) {}
```

### 3. YAML Configuration Loading

#### Class: `PlatformConfigLoader`
- **Location**: `premium-core-engine/src/main/java/adrianmikula/jakartamigration/platforms/config/PlatformConfigLoader.java`
- **Purpose**: Loads and parses platforms.yaml

```java
public class PlatformConfigLoader {
    public Map<String, PlatformConfig> loadPlatformConfigs()
    public PlatformConfig getPlatformConfig(String platformType)
}
```

### 4. Platforms Tab Component (Premium IntelliJ)

#### Class: `PlatformsTabComponent`
- **Location**: `premium-intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/PlatformsTabComponent.java`
- **Purpose**: UI component for Platforms tab

#### UI Layout:
```java
public class PlatformsTabComponent {
    // Main panel with scan button
    private JPanel mainPanel;
    private JButton scanButton;
    
    // Results display
    private JPanel resultsPanel;
    private List<PlatformDetectionPanel> platformPanels;
    
    // Premium controls
    private JPanel premiumPanel;
    private JButton upgradeButton;
    private JButton trialButton;
}
```

#### Key Methods:
```java
public class PlatformsTabComponent {
    public JPanel getPanel()
    public void scanProject()
    public void displayResults(List<PlatformDetection> detections)
    public void updatePremiumControls(boolean isPremium)
}
```

### 5. Risk Scoring Integration

#### Risk Calculation Rules:
- **Major version change**: +100 points
- **Framework change**: +50 points  
- **Runtime change**: +25 points

#### Integration with Dashboard:
```java
// Update dashboard with platform-based risk scores
dashboardComponent.updateGauges();
dashboardComponent.updateSummary();
dashboardComponent.updateScanResultsTable();
```

## Implementation Steps

### Phase 1: Core Platform Detection
1. Create data models in `premium-core-engine`
2. Implement `PlatformConfigLoader` for YAML parsing
3. Implement `PlatformDetectionService` with scanning logic
4. Add unit tests for core detection logic

### Phase 2: UI Component
1. Create `PlatformsTabComponent` in `premium-intellij-plugin`
2. Implement scan button and results display
3. Add premium feature gating with lock icon
4. Integrate with existing dashboard component

### Phase 3: Integration
1. Add Platforms tab to main tool window
2. Integrate with risk scoring system
3. Add feature flag controls
4. Update dashboard to recalculate scores on platform changes

### Phase 4: Testing & Documentation
1. Write comprehensive unit tests
2. Add integration tests
3. Update documentation
4. Verify premium feature gating

## Success Criteria

### Functional Requirements:
- ✅ Platform detection works for all configured platforms
- ✅ Scan button triggers project analysis
- ✅ Results show platform compatibility status
- ✅ Risk scores updated based on detected platforms
- ✅ Premium feature properly gated

### Technical Requirements:
- ✅ Configuration loaded from platforms.yaml
- ✅ Unit tests achieve 80%+ coverage
- ✅ Integration with existing dashboard
- ✅ Feature flag integration
- ✅ Proper error handling and logging

### Quality Requirements:
- ✅ Code follows SOLID principles
- ✅ Files under 500 lines (KISS principle)
- ✅ No duplicate code (DRY principle)
- ✅ Proper separation of concerns

## Dependencies

### Premium Core Engine:
- YAML parsing library (Jackson/YAML)
- File system utilities
- Regex pattern matching

### Premium IntelliJ Plugin:
- Swing UI components
- Project access APIs
- Dashboard component integration
- Feature flag service

## Testing Strategy

### Unit Tests:
- `PlatformConfigLoaderTest`: YAML parsing
- `PlatformDetectionServiceTest`: Detection logic
- `PlatformsTabComponentTest`: UI component behavior

### Integration Tests:
- End-to-end scanning workflow
- Dashboard integration
- Premium feature gating

### Test Data:
- Sample project files for each platform
- Mock YAML configurations
- Edge cases (missing files, corrupted configs)
