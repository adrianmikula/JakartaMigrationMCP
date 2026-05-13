# Java 21 Adoption Limitations

## Problem Statement

The project cannot fully adopt Java 21 across all modules due to IntelliJ Platform constraints.

## Root Cause

The IntelliJ Platform has strict Java version requirements based on the target platform version. The `premium-intellij-plugin` module targets IntelliJ 2023.3.4, which requires Java 17.

## Attempts to Upgrade

### Attempt 1: Upgrade to IntelliJ 2024.1.4
- **Action**: Upgraded IntelliJ version from 2023.3.4 to 2024.1.4
- **Result**: Plugin verification failed with error:
  ```
  The 'since-build' property is lower than the target IntelliJ Platform major version: 233.0 < 241.
  The Java configuration specifies targetCompatibility=21 but since-build='233.0' property requires targetCompatibility=17.
  The Java configuration specifies targetCompatibility=21 but IntelliJ Platform 2024.1.4 requires targetCompatibility=17.
  ```
- **Conclusion**: IntelliJ 2024.1.4 still requires Java 17

### Attempt 2: Upgrade to IntelliJ 2024.2.5
- **Action**: Upgraded IntelliJ version to 2024.2.5 (expected to support Java 21)
- **Result**: Gradle cache/download issues:
  ```
  Failed to query the value of task ':premium-intellij-plugin:runIde' property 'ideDir'.
  java.nio.file.NoSuchFileException: C:\Users\adria\.gradle\caches\modules-2\files-2.1\com.jetbrains.intellij.idea\ideaIC\2024.2.5\...
  ```
- **Conclusion**: Cache corruption prevented download; even if successful, documentation indicates 2024.2.5 may still require Java 17

## Current State

All modules use Java 17:
- `community-core-engine`: Java 17
- `community-mcp-server`: Java 17
- `premium-core-engine`: Java 17
- `premium-intellij-plugin`: Java 17 (IntelliJ 2023.3.4)
- `premium-mcp-server`: Java 17

## Workarounds Implemented

### Virtual Threads
- **Problem**: Virtual threads (Java 21 feature) were used in `DefaultJarCompatibilityScanner`
- **Solution**: Removed virtual thread code and reverted to traditional thread pools
- **Impact**: Performance optimization lost, but functionality preserved

### Early Exit Logic
- **Problem**: Early exit in `BytecodeSignalExtractor` was interfering with maxClasses tests
- **Solution**: Disabled early exit completely
- **Impact**: Performance optimization lost, but tests now pass
- **Note**: This is a temporary workaround; should be re-enabled with proper maxClasses handling

### Test Compatibility
- **Problem**: `virtualThreadsEnabledWithParallelScan` test required Java 21
- **Solution**: Added conditional skip for Java 17 using `isJava21Plus()` helper method
- **Impact**: Test skipped on Java 17, but passes on Java 21 if future upgrade succeeds

## Future Considerations

### Option 1: Wait for IntelliJ Platform Java 21 Support
- Monitor IntelliJ Platform releases for full Java 21 support
- Target IntelliJ 2025.x or later when Java 21 becomes the baseline
- Update `since-build` and `until-build` properties accordingly

### Option 2: Multi-Release JAR
- Consider using multi-release JAR to support both Java 17 and Java 21
- Use reflection for Java 21 features (virtual threads)
- Keep IntelliJ plugin at Java 17 while allowing core modules to use Java 21
- **Challenge**: IntelliJ plugin depends on core modules, creating dependency chain issues

### Option 3: Separate Plugin Module
- Split core functionality into Java 21-compatible library
- Create a thin Java 17 adapter layer for IntelliJ plugin
- **Challenge**: Significant refactoring effort, may not be worth the complexity

## Dependencies

- **Lombok**: Version 1.18.34 (compatible with Java 17)
- **IntelliJ Platform**: Version 2023.3.4 (requires Java 17)
- **Gradle**: Version 8.5 (supports Java 17 and 21)

## Related Files

- `premium-intellij-plugin/build.gradle.kts` - IntelliJ version configuration
- `premium-core-engine/build.gradle.kts` - Core module Java version
- `community-core-engine/build.gradle.kts` - Community module Java version
- `premium-core-engine/src/main/java/.../DefaultJarCompatibilityScanner.java` - Virtual thread removal
- `premium-core-engine/src/main/java/.../BytecodeSignalExtractor.java` - Early exit disabled
- `premium-core-engine/src/test/java/.../DefaultJarCompatibilityScannerTest.java` - Test skip logic

## References

- [IntelliJ Platform Versions](https://jb.gg/intellij-platform-versions)
- [IntelliJ Platform 2024.1 Compatibility](https://plugins.jetbrains.com/docs/intellij/2024-1/compatibility-guide.html)
- [Java 21 Virtual Threads](https://openjdk.org/jeps/444)

## Date Created

2026-05-12
