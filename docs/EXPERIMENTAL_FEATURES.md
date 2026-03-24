# Experimental Features Guide

## Overview

Experimental features in Jakarta Migration Plugin provide cutting-edge functionality that is still under development. These features require explicit user consent and are only available to premium users.

## Available Experimental Features

### 1. Runtime Tab ⚡ (Experimental)
- **Purpose**: Diagnose runtime errors with AI-powered analysis
- **Features**: 
  - Error pattern recognition
  - Automated remediation suggestions
  - Runtime error diagnosis and troubleshooting
- **Requirements**: Premium subscription + Experimental features enabled

### 2. Reports Tab 📊 (Experimental)
- **Purpose**: Generate comprehensive PDF reports based on scan data
- **Features**:
  - Dependency analysis reports
  - Advanced scan results
  - Migration recommendations
  - Professional PDF export
- **Requirements**: Premium subscription + Experimental features enabled

## How to Enable Experimental Features

### Method 1: Gradle Properties (Recommended)

Edit `gradle.properties` in your project:

```properties
# Enable experimental features
jakarta.migration.experimental_features=true
```

### Method 2: System Property

Set the system property when starting IntelliJ:

```bash
-Djakarta.migration.experimental_features=true
```

### Method 3: Programmatic (Advanced)

```java
adrianmikula.jakartamigration.intellij.config.FeatureFlags.getInstance()
    .setExperimentalFeaturesEnabled(true);
```

## Tab Visibility Behavior

### Premium Users with Experimental Features Enabled
- ✅ Runtime ⚡ (Experimental) - Full functionality
- ✅ Reports 📊 (Experimental) - Full functionality
- ✅ All other premium tabs (Refactor, History, Advanced Scans)

### Premium Users with Experimental Features Disabled
- ❌ Runtime tab - Hidden
- ❌ Reports tab - Hidden  
- ✅ All other premium tabs (Refactor, History, Advanced Scans)

### Community Users (Free Tier)
- 🔒 Runtime tab - Locked placeholder (if experimental enabled)
- 🔒 Reports tab - Locked placeholder (if experimental enabled)
- 🔒 Premium tabs - Locked placeholders
- ✅ Community tabs (Dashboard, Dependencies, Dependency Graph, Migration Strategy, Support, AI)

## Reports Tab Features

### Available Report Types

1. **Dependency Report**
   - Shows dependency graph analysis
   - Includes Jakarta compatibility information
   - Lists potential blockers

2. **Scan Results Report**
   - Advanced scanning results
   - JPA, Bean Validation, Servlet/JSP findings
   - Issue counts and recommendations

3. **Comprehensive Report**
   - Combines all analysis data
   - Executive summary
   - Complete migration roadmap

### Report Generation Process

1. **Select Report Type**: Choose from the three available report types
2. **Choose Output Location**: Select where to save the PDF file
3. **Generate Report**: The system creates a professional PDF report
4. **View Results**: Option to automatically open the generated PDF

### Report Contents

#### Dependency Analysis Section
- Total dependencies count
- Jakarta compatible dependencies
- Potential blockers
- Dependency tree visualization

#### Advanced Scan Results Section
- JPA entity analysis
- Bean validation findings
- Servlet/JSP component analysis
- Third-party library assessment

#### Recommendations Section
- Migration priority list
- Step-by-step migration plan
- Risk assessment
- Resource requirements

## Runtime Tab Features

### Error Analysis
- **Pattern Recognition**: Identifies common Jakarta migration error patterns
- **Root Cause Analysis**: Determines underlying causes of runtime issues
- **Solution Suggestions**: Provides specific remediation steps

### Diagnostic Tools
- **Stack Trace Analysis**: Parses runtime exceptions for Jakarta-specific issues
- **Dependency Conflicts**: Identifies version conflicts at runtime
- **Configuration Issues**: Detects misconfigured Jakarta components

## Safety and Stability

### Experimental Feature Status
- Features marked as "Experimental" are:
  - Under active development
  - Subject to API changes
  - May have limited testing coverage
  - Not recommended for production use

### Data Safety
- All experimental features:
  - Use read-only operations by default
  - Create backups before making changes
  - Require explicit user confirmation
  - Log all actions for audit trail

### Rollback Capability
- Changes made by experimental features can be:
  - Undone through the History tab
  - Reverted using Git integration
  - Manually reversed with provided steps

## Feedback and Bug Reporting

### Providing Feedback
- Use the Support tab to report issues
- Include detailed error descriptions
- Attach generated reports when applicable
- Specify that the issue relates to experimental features

### Known Limitations
- Runtime analysis may not catch all edge cases
- Report generation uses sample data for some sections
- Performance may vary with project size
- Some features require specific project configurations

## Future Development

### Planned Enhancements
- **Runtime Tab**: Real-time monitoring, predictive error detection
- **Reports Tab**: Custom templates, interactive reports, batch generation
- **Integration**: CI/CD pipeline integration, automated reporting

### Feature Graduation
Experimental features may graduate to stable status when:
- Sufficient testing coverage is achieved
- User feedback is consistently positive
- Performance meets production standards
- Documentation is comprehensive

## Troubleshooting

### Common Issues

1. **Tabs Not Visible**
   - Verify experimental features are enabled
   - Check premium subscription status
   - Restart IntelliJ after changing settings

2. **Report Generation Fails**
   - Ensure write permissions to output directory
   - Check for sufficient disk space
   - Verify project has been analyzed first

3. **Runtime Analysis Errors**
   - Ensure project has been built
   - Check for missing dependencies
   - Verify runtime configuration

### Getting Help
- Check the Support tab for additional resources
- Review generated logs for error details
- Contact support with specific error messages
- Consider disabling experimental features if issues persist

## Disabling Experimental Features

To disable experimental features, set the property to `false`:

```properties
jakarta.migration.experimental_features=false
```

Or remove the property entirely (defaults to `false`).

After disabling:
- Restart IntelliJ
- Experimental tabs will be hidden
- Existing data remains intact
- Can be re-enabled later without data loss
