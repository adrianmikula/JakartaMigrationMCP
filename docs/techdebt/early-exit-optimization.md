# Early Exit Optimization Issues

## Problem Statement

The early exit optimization in `BytecodeSignalExtractor` is currently disabled because it interferes with the `maxClasses` parameter functionality.

## Current State

- **Location**: `premium-core-engine/src/main/java/adrianmikula/jakartamigration/jaranalysis/service/BytecodeSignalExtractor.java`
- **Status**: Disabled (`earlyExitEnabled = false`)
- **Config**: `jar-scanning-weights.yaml` has `earlyExit: true` and `earlyExitThreshold: 10`

## How Early Exit Works

The early exit optimization is designed to stop scanning JAR files early when the confidence score reaches a threshold:

```java
// Score calculation
runningScore = (jakartaClasses.size() * jakartaClassRefWeight) + 
               (javaxClasses.size() * javaxClassRefWeight);

// Early exit condition
if (Math.abs(runningScore) >= earlyExitThreshold) {
    break; // Stop scanning
}
```

With default weights:
- `jakartaClassRefWeight = 5`
- `javaxClassRefWeight = -5`
- `earlyExitThreshold = 10`

This means early exit triggers after just 2 classes of the same namespace are detected.

## Issue with maxClasses Parameter

The `maxClasses` parameter controls how many classes to scan:
- `maxClasses = 0`: Unlimited scan (scan all classes)
- `maxClasses > 0`: Scan exactly N classes

**The conflict**: When `maxClasses > 0`, tests expect exactly N classes to be scanned. However, early exit may stop scanning before reaching N classes if the confidence threshold is reached early.

### Example Test Failure

```java
@Test
void analyzeJarWithZeroMaxClasses() throws IOException {
    // Creates 50 classes with javax references
    for (int i = 0; i < 50; i++) {
        builder.withClass(TestJarBuilder.ClassSpec.builder("test/Cls" + i)
            .withSuper("javax/servlet/http/HttpServlet"));
    }
    var signal = extractor.extractFromJar(jar, 0);
    assertThat(signal.javaxClassRefs()).isEqualTo(50); // Fails with early exit
}
```

With early exit enabled:
- After 2 classes: score = -10 (abs(-10) >= 10, early exit triggers)
- Only 2 classes scanned instead of 50

## Attempted Solutions

### Solution 1: Enable Early Exit Only When maxClasses == 0

**Implementation**:
```java
boolean earlyExitEnabled = JarScanningConfig.get().isEarlyExitEnabled() && maxClasses == 0;
```

**Result**: Tests still failed because tests using `maxClasses=0` expect all classes to be scanned, not early exit.

**Root cause**: The tests that use `maxClasses=0` are verifying "unlimited scanning" behavior, not early exit optimization. They expect all classes to be scanned regardless.

### Solution 2: Increase earlyExitThreshold

**Implementation**: Changed `earlyExitThreshold` from 10 to 1000

**Result**: Tests passed, but this defeats the purpose of early exit optimization. A threshold of 1000 is too high to be useful in practice.

## Required Solution

The proper solution requires a more nuanced approach:

### Option A: Separate Test Mode from Production Mode

Add a configuration flag to disable early exit in tests:
```yaml
features:
  earlyExit: true
  earlyExitThreshold: 10
  earlyExitInTests: false  # New flag
```

Update code to check for test mode:
```java
boolean earlyExitEnabled = JarScanningConfig.get().isEarlyExitEnabled() 
    && maxClasses == 0
    && !isTestMode();
```

**Pros**: Keeps early exit optimization for production, tests pass
**Cons**: Requires detecting test mode, adds complexity

### Option B: Rewrite Tests to Accommodate Early Exit

Update failing tests to not rely on exact class counts when `maxClasses=0`:
```java
@Test
void analyzeJarWithZeroMaxClasses() throws IOException {
    // Create 50 classes
    var signal = extractor.extractFromJar(jar, 0);
    // Instead of exact count, verify signal is detected
    assertThat(signal.javaxClassRefs()).isGreaterThan(0);
    assertThat(signal.hasJavaxSignal()).isTrue();
}
```

**Pros**: Tests are more realistic, early exit optimization works
**Cons**: Loses ability to verify exact scanning behavior

### Option C: Early Exit Only After Minimum Sample Size

Require a minimum number of classes before early exit can trigger:
```java
boolean earlyExitEnabled = JarScanningConfig.get().isEarlyExitEnabled() 
    && maxClasses == 0
    && classesScanned >= MIN_CLASSES_BEFORE_EXIT;

if (earlyExitEnabled && classesScanned >= MIN_CLASSES_BEFORE_EXIT) {
    if (Math.abs(runningScore) >= earlyExitThreshold) {
        break;
    }
}
```

**Pros**: Ensures minimum sample size, allows early exit for large JARs
**Cons**: Adds another configuration parameter

## Impact

**Current**: Early exit optimization is completely disabled, resulting in:
- Performance degradation for large JAR files
- All classes scanned even when confidence is high early
- No performance benefit from the optimization

**Future**: When properly implemented:
- Significant performance improvement for large JARs
- Early termination when confidence threshold is reached
- Better user experience for projects with many dependencies

## Related Files

- `premium-core-engine/src/main/java/adrianmikula/jakartamigration/jaranalysis/service/BytecodeSignalExtractor.java` - Implementation
- `premium-core-engine/src/main/resources/jar-scanning-weights.yaml` - Configuration
- `premium-core-engine/src/test/java/adrianmikula/jakartamigration/jaranalysis/service/BytecodeSignalExtractorTest.java` - Tests
- `docs/techdebt/java-21-adoption-limitations.md` - Related Java 21 adoption issues

## Related Tests

Failing tests when early exit is enabled:
- `BytecodeSignalExtractorTest.analyzeJarWithZeroMaxClasses()` - Expects 50 classes scanned
- `BytecodeSignalExtractorTest.processJarWithMultipleEntryPoints()` - Expects 5 classes scanned

Passing tests (maxClasses > 0):
- `BytecodeSignalExtractorTest.respectMaxClassesLimit()` - Uses maxClasses=5
- `BytecodeSignalExtractorTest.detectMultipleClassesWithMaxLimit()` - Uses maxClasses=10

## Date Created

2026-05-13
