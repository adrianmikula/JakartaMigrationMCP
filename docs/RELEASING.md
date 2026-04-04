# Release Management and Version Requirements

This document outlines the release management process, version number format requirements, and platform requirements for the Jakarta Migration plugin.

## Version Number Format Requirements

### Plugin Version Format
The plugin follows semantic versioning: `MAJOR.MINOR.PATCH`

#### Current Version: `1.0.8`

#### Version Components:
- **MAJOR**: Breaking changes or major new features
- **MINOR**: New features or significant enhancements  
- **PATCH**: Bug fixes and minor improvements

#### Version Validation:
- Plugin version in `gradle.properties`: `1.0.8`
- Plugin version in `plugin.xml`: `<version>1.0.8</version>`
- Both must match exactly for successful builds

### Marketplace Release Version Requirements

#### Release Version Format:
- Format: `YYYYMM` (Year + Month)
- Example: `20218` for plugin version `1.0.8`
- Must match the beginning of the plugin version number

#### Current Configuration:
```xml
<!-- plugin.xml -->
<product-descriptor 
    code="PJAKARTAMIGRATI" 
    release-date="20250325" 
    release-version="20218"/>
```

#### Validation Rules:
- `release-version` must start with the same digits as plugin version
- For `1.0.8`, the release version must be `20218` (both start with "2021")
- `release-date` follows `YYYYMMDD` format (2025-03-25 → 20250325)

## Platform Detection Requirements

### Overview
The Platforms tab provides application server detection and compatibility analysis for Jakarta EE migration planning.

### Configuration File: `platforms.yaml`
Location: `premium-intellij-plugin/src/main/resources/config/platforms.yaml`

### Supported Application Servers:

#### Apache Tomcat
- **Detection Files**: `bin/catalina.bat`, `lib/catalina.jar`, `conf/server.xml`
- **Jakarta Compatibility**: Minimum `10.0+`
- **Java Requirement**: `11+`
- **Spring Requirement**: `5+`
- **Supported Versions**: `10.0`, `10.1`, `11.0`, `11.1`

#### Red Hat WildFly
- **Detection Files**: `bin/standalone.sh`, `modules/system/layers/base/org/jboss/as/server/main/module.xml`
- **Jakarta Compatibility**: Minimum `27.0+`
- **Java Requirement**: `17+`
- **Spring Requirement**: `6+`
- **Supported Versions**: `27.0`, `27.1`, `28.0`, `29.0`

#### Eclipse Jetty
- **Detection Files**: `start.ini`, `lib/jetty-server-*.jar`
- **Jakarta Compatibility**: Minimum `12.0+`
- **Java Requirement**: `11+`
- **Spring Requirement**: `5+`
- **Supported Versions**: `12.0`, `11.0`, `10.0`

#### IBM WebSphere Liberty
- **Detection Files**: `lib/com.ibm.websphere.appserver.api.jar`, `etc/server.env`
- **Jakarta Compatibility**: Minimum `23.0+`
- **Java Requirement**: `17+`
- **Spring Requirement**: `6+`
- **Supported Versions**: `23.0`, `23.1`, `24.0`

#### Apache TomEE
- **Detection Files**: `bin/tomee.sh`, `lib/tomee-catalina-*.jar`
- **Jakarta Compatibility**: Minimum `9.0+`
- **Java Requirement**: `11+`
- **Spring Requirement**: `5+`
- **Supported Versions**: `9.0`, `9.1`, `10.0`

#### Payara Server
- **Detection Files**: `bin/asadmin`, `lib/payara-*.jar`
- **Jakarta Compatibility**: Minimum `7.0+`
- **Java Requirement**: `11+`
- **Spring Requirement**: `5+`
- **Supported Versions**: `7.0`, `7.1`, `8.0`

#### JBoss EAP
- **Detection Files**: `bin/standalone.sh`, `modules/system/layers/base/org/jboss/as/product/module.xml`
- **Jakarta Compatibility**: Minimum `8.0+`
- **Java Requirement**: `17+`
- **Spring Requirement**: `6+`
- **Supported Versions**: `8.0`, `8.1`, `8.2`

## Risk Scoring Integration

### Platform Change Impact
When platform changes are detected, the risk scoring system adjusts:

#### Risk Scoring Rules (from `platforms.yaml`):
```yaml
riskScoring:
  majorVersionChange: 100    # Major appserver version change
  frameworkChange: 50       # Related runtime/framework changes
  runtimeChange: 25        # Java/JDK version changes
```

#### Implementation:
- **Major Version Change**: +100 to risk score (significant migration impact)
- **Framework Change**: +50 to risk score (moderate migration impact)
- **Runtime Change**: +25 to risk score (minor migration impact)

### UI Components

#### Platforms Tab Features:
- **Scan Button**: Detects current application server and Jakarta compatibility
- **Compatibility Display**: Shows whether detected version supports Jakarta EE
- **Upgrade Recommendations**: Displays minimum Jakarta-compatible version required
- **Requirements Display**: Shows additional upgrade requirements (Java, Spring, etc.)
- **Visual Indicators**: Color-coded status for compatible/incompatible versions

#### Integration Points:
- **Dashboard Integration**: Platform detection results affect risk scores
- **Migration Strategy**: Platform compatibility influences migration approach recommendations
- **Advanced Scans**: Platform-specific patterns used in advanced scanning modules

## Build and Distribution

### IDE Package Exclusions
To prevent bundling issues, the plugin excludes certain IDE packages:

```kotlin
// build.gradle.kts
intellij {
    prepareSandbox {
        exclude {
            // Exclude org.jetbrains.concurrency package to prevent bundling
            // This package should be provided by IntelliJ platform
        }
    }
}
```

### Plugin Descriptor Validation
The plugin.xml must pass all JetBrains Marketplace validation checks:

- ✅ **Version Consistency**: plugin.xml version matches gradle.properties
- ✅ **Release Version Format**: release-version matches plugin version prefix
- ✅ **No IDE Package Bundling**: Explicit exclusions for problematic packages
- ✅ **Proper Dependencies**: All dependencies correctly declared and resolved

## Release Process

### Pre-Release Checklist:
1. **Version Update**: Update `gradle.properties`, `plugin.xml`, and `release-version`
2. **Platform Config**: Verify `platforms.yaml` is up-to-date
3. **Risk Scoring**: Test platform detection and risk calculation
4. **Build Validation**: Ensure no IDE package bundling warnings
5. **Compatibility Testing**: Test with target IntelliJ versions
6. **Documentation**: Update changelog and release notes

### Post-Release:
1. **Tag Release**: Create Git tag with version number
2. **Marketplace Upload**: Submit to JetBrains Marketplace
3. **Documentation Update**: Update all relevant documentation
4. **Version Bump**: Prepare next development version

## Quality Assurance

### Testing Requirements:
- **Unit Tests**: Minimum 50% code coverage for platform detection
- **Integration Tests**: Platform scanning with various application servers
- **Compatibility Tests**: Verify detection patterns work across versions
- **Performance Tests**: Ensure platform scanning doesn't impact IDE performance

### Code Quality Standards:
- **SDD Compliance**: All implementations follow specifications in `docs/spec/`
- **TDD Approach**: Tests written before implementation
- **KISS Principle**: Source files kept under 500 lines
- **DRY Principle**: Reuse existing detection patterns and utilities
- **SOLID Principles**: Proper separation of concerns and maintainable code

## Version History

### v1.0.8 (Current)
- **Dashboard Refactoring**: Implemented new three-section layout with speedometer gauges
- **Platform Detection**: Added comprehensive application server detection
- **Risk Scoring Integration**: Platform changes affect migration risk assessment
- **Plugin Distribution**: Fixed IDE package bundling issues
- **Marketplace Compliance**: Corrected release-version parameter format

### Previous Versions:
*See `plugin.xml` changelog for detailed version history*

## Future Requirements

### Planned Enhancements:
- **Extended Platform Support**: Additional application servers (Tomcat, GlassFish, etc.)
- **Dynamic Detection**: Runtime pattern matching without hardcoded version lists
- **Enhanced Risk Scoring**: More sophisticated platform impact analysis
- **Automated Recommendations**: Version upgrade suggestions based on detected platform

---

*This document should be updated with each release to reflect current requirements and implementation status.*
