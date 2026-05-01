# Platforms Tab Implementation

## Overview
This document describes the implementation of the Platforms tab for Jakarta Migration IntelliJ plugin, providing comprehensive details of the architecture, components, and functionality.

## Architecture

### Premium Core Module Components

#### Platform Detection Service
- **Location**: `premium-core-engine/src/main/java/adrianmikula/jakartamigration/platforms/service/PlatformDetectionService.java`
- **Purpose**: Scans projects for application servers using configurable patterns
- **Key Features**:
  - Pattern-based detection using regex matching
  - Jakarta EE compatibility checking
  - Risk score calculation based on platform changes
  - Support for multiple application server types

#### Platform Configuration Models
- **Location**: `premium-core-engine/src/main/java/adrianmikula/jakartamigration/platforms/model/`
- **Components**:
  - `PlatformConfig.java`: Configuration for a single platform type
  - `DetectionPattern.java`: Pattern for detecting specific platform files
  - `JakartaCompatibility.java`: Jakarta EE version compatibility info
  - `PlatformDetection.java`: Result of platform detection
  - `PlatformScanResult.java`: Complete scan result with risk scoring

#### Configuration Loading
- **Location**: `premium-core-engine/src/main/java/adrianmikula/jakartamigration/platforms/config/PlatformConfigLoader.java`
- **Purpose**: Loads platform configurations from `platforms.yaml`
- **Features**:
  - Jackson YAML parsing
  - Resource loading from classpath
  - Error handling for missing configuration files

### Premium IntelliJ Module Components

#### Platforms Tab UI Component
- **Location**: `premium-intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/PlatformsTabComponent.java`
- **Purpose**: Swing UI component for platform detection and management
- **Key Features**:
  - Scan button with background processing
  - Results display with platform panels
  - Premium feature gating with lock icon
  - Upgrade/trial button integration
  - Responsive layout with proper error handling

#### Feature Flag Integration
- **Location**: `premium-intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/config/FeatureFlags.java`
- **Integration**: Added `platformsTab` feature flag
- **Behavior**:
  - Non-premium users: Lock icon and disabled scan functionality
  - Premium users: Full platform scanning capabilities

## Data Flow

### Platform Detection Workflow
```
User clicks "Analyse Project" 
    ↓
PlatformDetectionService.scanProject()
    ↓
PlatformsTabComponent.displayResults()
    ↓
DashboardComponent.updateGauges()
    ↓
DashboardComponent.updateSummary()
    ↓
DashboardComponent.updateScanResultsTable()
```

### Configuration Loading
```
Plugin Startup
    ↓
PlatformConfigLoader.loadPlatformConfigs()
    ↓
Parse platforms.yaml
    ↓
Create PlatformConfig objects
```

## Risk Scoring Implementation

### Risk Calculation Rules
Based on `platforms.yaml` configuration:
- **Major version change** (javax → jakarta): +100 points
- **Framework change** (java/spring/runtime): +50 points
- **Runtime change**: +25 points

### Integration with Dashboard
Platform scan results automatically trigger dashboard updates:
- **Gauges**: Updated with new risk scores
- **Summary**: Updated with platform compatibility information
- **Scan Results Table**: Updated with platform detection details

## Supported Platforms

### Application Servers Supported
From `platforms.yaml` configuration:

1. **Apache Tomcat** - Detection via `catalina.bat`, `catalina.jar`, `server.xml`
2. **Red Hat WildFly** - Detection via `standalone.sh`, module XML files
3. **Eclipse Jetty** - Detection via `start.ini`, `jetty-server-*.jar`
4. **IBM WebSphere Liberty** - Detection via JAR files, `server.env`
5. **Apache TomEE** - Detection via `tomee.sh`, `tomee-catalina-*.jar`
6. **Payara Server** - Detection via `asadmin`, `payara-*.jar`
7. **JBoss EAP** - Detection via `standalone.sh`, product module XML

### Detection Patterns
Each platform uses file-based detection with regex patterns:
- **File Pattern**: Specific files to search for
- **Regex Pattern**: Version extraction using regular expressions
- **Version Group**: Regex group for version number extraction

## Testing Strategy

### Unit Tests
Following TDD principles with comprehensive test coverage:

#### Core Service Tests
- **Location**: `premium-core-engine/src/test/java/adrianmikula/jakartamigration/platforms/service/PlatformDetectionServiceTest.java`
- **Coverage Areas**:
  - Platform detection logic
  - Version comparison algorithms
  - Risk scoring calculations
  - Configuration loading
  - Error handling scenarios

#### UI Component Tests
- **Location**: `premium-intellij-plugin/src/test/java/adrianmikula/jakartamigration/intellij/ui/PlatformsTabComponentTest.java`
- **Coverage Areas**:
  - Premium feature gating
  - Scan button functionality
  - Results display behavior
  - Error handling and user feedback
  - Component interaction testing

### Test Data Strategy
- **Mock Projects**: Create temporary project structures for each platform type
- **Mock Configurations**: Use real platform configurations from YAML
- **Edge Cases**: Test missing files, corrupted configurations, permission issues

## Quality Assurance

### Code Quality Standards
Following AGENTS.md rules:

#### SOLID Principles
- **Single Responsibility**: Each class has one clear purpose
- **Open/Closed**: Open for extension, closed for modification
- **Liskov Substitution**: Strategy pattern for risk scoring
- **Interface Segregation**: Separate UI, service, and configuration layers

#### KISS Principle
- **File Size**: All source files under 500 lines
- **DRY Principle**: Reuse existing patterns and utilities
- **2026 Best Practices**: Modern Java features and industry standards

#### Licensing Compliance
- **Community/Premium Separation**: Core engine in community module, UI in premium module
- **Feature Flag Gating**: Proper premium feature access control

## Integration Points

### Dashboard Integration
Platform detection results integrate with existing dashboard:
- **Risk Score Updates**: Platform changes reflected in migration effort gauge
- **Summary Updates**: Platform compatibility shown in migration summary
- **Scan Results**: Platform details added to scan results table

### Tool Window Integration
Platforms tab integrates with existing tool window structure:
- **Tab Management**: Added to existing tab system
- **Event Handling**: Proper integration with project lifecycle events
- **Resource Management**: Efficient resource loading and cleanup

## Performance Considerations

### Background Processing
- **SwingWorker**: All scanning operations run in background threads
- **Progress Feedback**: Real-time progress updates during scanning
- **Cancellation Support**: Proper handling of user-initiated cancellation

### Memory Management
- **Lazy Loading**: Configuration loaded only when needed
- **Efficient Caching**: Platform configurations cached after first load
- **Resource Cleanup**: Proper disposal of UI resources

## Future Enhancements

### Potential Extensions
- **Additional Platforms**: Easy addition of new application server types
- **Advanced Detection**: Heuristic detection for complex project structures
- **Integration Scanning**: Cross-platform compatibility analysis
- **Performance Metrics**: Detailed scanning performance tracking

## Security Considerations

### File System Access
- **Permission Handling**: Graceful handling of file access restrictions
- **Path Traversal Protection**: Safe file path construction and validation
- **Input Validation**: Proper validation of user inputs and configuration data

### Error Handling
- **Comprehensive Logging**: Detailed error reporting for troubleshooting
- **User Feedback**: Clear error messages with actionable guidance
- **Graceful Degradation**: Fallback behavior when scanning fails

## Conclusion

The Platforms tab implementation provides a comprehensive solution for application server detection and Jakarta EE compatibility analysis, following all established architectural patterns and quality standards. The modular design allows for easy extension and maintenance while providing a robust foundation for migration planning and risk assessment.
