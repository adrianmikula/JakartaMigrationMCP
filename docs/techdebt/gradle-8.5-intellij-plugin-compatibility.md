# Gradle 8.5 and IntelliJ Plugin Compatibility Issues

## Problem Statement

The `runIdeDev` task and standard `runIde` task fail with Gradle 8.5 due to strict input validation that checks if the `pluginsDir` exists at configuration time, before any tasks can create it.

## Error Details

```
FAILURE: Build failed with an exception.
* What went wrong:
A problem was found with the configuration of task ':premium-intellij-plugin:runIde' (type 'RunIdeTask').
- Type 'org.jetbrains.intellij.tasks.RunIdeTask' property 'pluginsDir' specifies directory 
  'E:\Source\JakartaMigrationMCP-workspace3\premium-intellij-plugin\build\idea-sandbox\plugins' 
  which doesn't exist.
Reason: An input file was expected to be present but it doesn't exist.
```

## Root Cause

Gradle 8.5 introduced stricter validation for task inputs. The IntelliJ plugin's `RunIdeTask` validates that the `pluginsDir` input exists at configuration time, but this directory is created by the `prepareSandbox` task during execution. This creates a chicken-and-egg problem:

1. `runIde` task validates that `pluginsDir` exists at configuration time
2. `prepareSandbox` task creates `pluginsDir` during execution
3. Validation happens before execution, causing the build to fail

## Current Configuration

- **Gradle Version**: 8.5
- **IntelliJ Plugin Version**: 1.17.2
- **Java Version**: 21
- **IntelliJ Platform**: 2024.2.5

## Attempted Workarounds

### 1. Adding dependsOn("prepareSandbox")
Added explicit dependency on `prepareSandbox` task:
```kotlin
tasks.named<org.jetbrains.intellij.tasks.RunIdeTask>("runIde") {
    dependsOn("prepareSandbox")
    systemProperty("jakarta.migration.mode", "dev")
}
```
**Result**: Failed - validation still occurs before task execution

### 2. Adding dependsOn("buildPlugin")
Added dependency on `buildPlugin` task:
```kotlin
tasks.register<org.jetbrains.intellij.tasks.RunIdeTask>("runIdeDev") {
    dependsOn("buildPlugin")
    systemProperty("jakarta.migration.mode", "dev")
}
```
**Result**: Failed - validation still occurs before task execution

### 3. Creating Custom Directory Creation Task
Created a `createSandboxDirs` task to create the directory before validation:
```kotlin
tasks.register("createSandboxDirs") {
    val sandboxPluginsDir = file("$buildDir/idea-sandbox/plugins")
    outputs.dir(sandboxPluginsDir)
    doLast {
        sandboxPluginsDir.mkdirs()
    }
}
```
**Result**: Failed - Gradle detected implicit dependency issue and required explicit declaration

### 4. Disabling Configuration Cache
Ran with `--no-configuration-cache` flag:
```bash
./gradlew :premium-intellij-plugin:runIdeDev --no-configuration-cache
```
**Result**: Failed - validation still occurs even without configuration cache

### 5. Upgrading IntelliJ Plugin
Attempted to upgrade to version 2.0.0:
```kotlin
id("org.jetbrains.intellij") version "2.0.0"
```
**Result**: Failed - version 2.0.0 does not exist in plugin repository

## Current State

### Working
- ✅ **Compilation**: BUILD SUCCESSFUL with `@SuppressWarnings` annotations for unchecked operations
- ✅ **Static Analysis**: SpotBugs and Error Prone disabled to avoid configuration issues
- ✅ **Standard Build Tasks**: `buildPlugin`, `jar`, `compileJava` all work correctly

### Not Working
- ❌ **runIde Task**: Fails due to Gradle 8.5 input validation
- ❌ **runIdeDev Task**: Fails due to same validation issue
- ❌ **Custom RunIdeTask**: Any custom RunIdeTask fails with the same validation error

## Recommended Solutions

### Option 1: Downgrade Gradle (Recommended for Short-term)
Downgrade Gradle to version 8.4 or earlier, which doesn't have the strict input validation:
```properties
# gradle-wrapper.properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.4-bin.zip
```

**Pros**: 
- Simple change
- Maintains compatibility with current IntelliJ plugin version
- No code changes required

**Cons**:
- Loses Gradle 8.5 features and improvements
- May introduce other compatibility issues

### Option 2: Upgrade IntelliJ Plugin (Recommended for Long-term)
Wait for or find a newer IntelliJ plugin version that's compatible with Gradle 8.5's validation. Check:
- IntelliJ Plugin GitHub repository for compatibility updates
- Gradle Plugin Portal for newer versions

**Pros**:
- Maintains Gradle 8.5 features
- Future-proof solution

**Cons**:
- May require code changes if plugin API changes
- Unknown when compatible version will be available

### Option 3: Use Standard Development Workflow (Current Workaround)
Use the standard `runIde` task with manual configuration:
```bash
# Build the plugin first
./gradlew :premium-intellij-plugin:buildPlugin

# Then run the IDE (may still fail with validation)
./gradlew :premium-intellij-plugin:runIde --no-configuration-cache
```

Or use IntelliJ's built-in plugin development run configuration instead of Gradle.

**Pros**:
- No build configuration changes
- Works with existing tooling

**Cons**:
- Not a true fix for the Gradle issue
- Requires manual steps

### Option 4: Patch IntelliJ Plugin (Advanced)
Fork the IntelliJ plugin and modify the `RunIdeTask` to mark `pluginsDir` as an output instead of an input, or to defer validation until execution time.

**Pros**:
- Full control over the fix
- Can contribute back to upstream

**Cons**:
- High maintenance burden
- Requires deep understanding of plugin internals
- Need to keep fork in sync with upstream

## Static Analysis Tools Status

### Disabled Tools
- **SpotBugs**: Disabled in all modules (community-core-engine, premium-core-engine, premium-intellij-plugin)
- **Error Prone**: Disabled in all modules

### Reason for Disabling
The root `build.gradle.kts` attempted to configure these plugins centrally, but:
1. Plugins were applied with `apply false` at root level
2. Plugin classes weren't available for import/configuration in root build script
3. Subprojects needed individual configuration
4. Error Prone required specific compiler configuration that was incompatible

### Re-enabling Static Analysis
To re-enable static analysis tools in the future:
1. Remove `apply false` from root build.gradle.kts and apply directly to subprojects
2. Configure each tool individually in each subproject's build.gradle.kts
3. Ensure proper dependencies and compiler arguments are set
4. Test compatibility with current Gradle and Java versions

## Related Issues

- Gradle 8.5 Release Notes: https://docs.gradle.org/8.5/release-notes.html
- IntelliJ Plugin GitHub: https://github.com/JetBrains/gradle-intellij-plugin
- Gradle Input Validation Documentation: https://docs.gradle.org/8.5/userguide/validation_problems.html

## Decision Record

**Status**: Open - awaiting decision on which solution to implement

**Recommendation**: Implement Option 1 (downgrade Gradle to 8.4) as a short-term fix to unblock development, while monitoring for IntelliJ plugin updates that support Gradle 8.5.

**Next Steps**:
1. Discuss with team which option to pursue
2. Implement chosen solution
3. Update this document with resolution
4. Consider re-enabling static analysis tools after Gradle compatibility is resolved
