# Java 21 Adoption - DECISION: STAY WITH JAVA 17

## Problem Statement

The project cannot fully adopt Java 21 due to IntelliJ Platform Gradle Plugin compatibility constraints.

## Decision

**Date**: 2026-05-14

**Decision**: Stay with Java 17 for now.

**Reasoning**:
- IntelliJ Platform Gradle Plugin 1.x enforces Java 17 compatibility checks for IntelliJ Platform 2024.3
- Migrating to Plugin 2.x requires a complete rewrite of the build configuration (significant effort)
- The plugin compatibility warnings are enforcement checks in the Gradle plugin, not actual platform limitations
- Virtual threads (Java 21 feature) would need to be disabled anyway due to plugin compatibility
- Java 17 is stable and widely supported, with no immediate need for Java 21 features

## Current State

- **Compilation**: All modules compile with Java 17
- **Runtime**: Virtual threads code is commented out (preserved for future use)
- **IntelliJ Platform**: 2024.3 (since-build 243)
- **IntelliJ Gradle Plugin**: 1.17.3
- **All modules**: Java 17 toolchain and compatibility

## Previous Attempts

### Attempt 1: Upgrade to Java 21 with Plugin 1.x
- **Action**: Upgraded all modules to Java 21, kept Plugin 1.17.3
- **Result**: Build succeeded with warnings, but plugin verification reported Java 17 requirement
- **Conclusion**: Plugin enforces Java 17 checks even though platform supports Java 21

### Attempt 2: Migrate to Plugin 2.x
- **Action**: Attempted migration to IntelliJ Platform Gradle Plugin 2.0.0
- **Result**: Plugin 2.x has completely different API requiring full build configuration rewrite
- **Conclusion**: Too complex for immediate adoption; deferred to future

## Workaround

### Virtual Threads Disabled
- **File**: `premium-core-engine/src/main/java/.../DefaultJarCompatibilityScanner.java`
- **Action**: Commented out virtual thread code in `createExecutor()` method
- **Reason**: Avoid potential runtime issues from plugin compatibility checks
- **Impact**: Performance optimization lost, but functionality preserved with traditional thread pools
- **Comment**: Code is preserved with comments explaining the limitation

## Future Considerations

### Option 1: Migrate to IntelliJ Platform Gradle Plugin 2.x
- **Benefit**: Proper Java 21 support with newer plugin API
- **Cost**: Complete rewrite of build configuration (different API, syntax, task structure)
- **Timeline**: Significant effort required
- **Status**: Deferred - revisit when Plugin 2.x migration becomes necessary for other reasons

### Option 2: Wait for Plugin 1.x Java 21 Support
- **Benefit**: No build configuration changes needed
- **Cost**: Unclear if JetBrains will update Plugin 1.x to support Java 21
- **Timeline**: Unknown
- **Status**: Monitor JetBrains releases

### Option 3: Target Newer IntelliJ Platform
- **Benefit**: May have better Java 21 support in 1.x plugin
- **Cost**: Users need newer IntelliJ version
- **Risk**: May still have compatibility checks
- **Status**: Not tested

## Related Files

- `premium-intellij-plugin/build.gradle.kts` - IntelliJ plugin version 1.17.3, platform 2024.3, Java 17
- `premium-core-engine/build.gradle.kts` - Java 17 toolchain
- `community-core-engine/build.gradle.kts` - Java 17 toolchain
- `community-mcp-server/build.gradle.kts` - Java 17 toolchain
- `premium-mcp-server/build.gradle.kts` - Java 17 toolchain
- `gradle.properties` - intellij.sinceBuild=243.0, Java 17 system properties
- `premium-core-engine/src/main/java/.../DefaultJarCompatibilityScanner.java` - Virtual threads commented out

## References

- [IntelliJ Platform Gradle Plugin 2.x Migration](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html)
- [IntelliJ Platform Versions](https://jb.gg/intellij-platform-versions)
- [Java 21 Virtual Threads](https://openjdk.org/jeps/444)

## Date Created

2026-05-12
## Date Updated

2026-05-14
