# JUnit 3 Test Visibility Under JUnit Platform

Tests that extend `BasePlatformTestCase` (or any JUnit 3 `TestCase` subclass) have
limited discoverability when run through Gradle's JUnit Platform integration. This
document explains the problem, its root cause, and the workarounds used in this project.

## The Problem

`BasePlatformTestCase` extends `junit.framework.TestCase` (JUnit 3). When Gradle's
`useJUnitPlatform()` discovers tests, it uses JUnit Platform's engine-based discovery.
JUnit 3 tests are handled by the **JUnit Vintage engine**, which discovers them
differently from JUnit 5 tests:

1. **`--tests` filter does not match them.** Running
   `./gradlew test --tests "*MyTest"` will fail with "No tests found" even though the
   class exists on the test classpath. The JUnit Platform filter mechanism only matches
   JUnit 5 test descriptors.

2. **`include()` / `exclude()` patterns in `useJUnitPlatform {}` do not match them.**
   Patterns like `includeTags("slow")` or `excludeTags("integration")` only affect
   JUnit 5 tests. JUnit 3 tests have no tag concept, so these filters silently ignore
   them.

3. **Gradle's `Test` task reports 0 tests when filtered.** Even though the Vintage
   engine discovers the tests internally, Gradle's test reporting layer (which reads
   JUnit Platform events) doesn't surface them when a `--tests` filter is active.

## Affected Tests in This Project

The following test classes extend `BasePlatformTestCase` and are affected:

| Class | Module | Purpose |
|-------|--------|---------|
| `AdvancedScanningServiceProgressTest` | premium-intellij-plugin | Progress listener callbacks during scanning |
| `ScanUiNonBlockingPerformanceTest` | premium-intellij-plugin | Scan non-blocking EDT performance |
| `ScanProgressListenerTest` | premium-intellij-plugin | DashboardComponent as ScanProgressListener |
| `SourceScansComponentTest` | premium-intellij-plugin | Source scans tab component |
| `SourceScansComponentRefreshTest` | premium-intellij-plugin | Source scans refresh behavior |

## Root Cause

Gradle's `test` task delegates to JUnit Platform, which loads both the **Jupiter**
(JUnit 5) and **Vintage** (JUnit 3/4) engines. However, Gradle's `--tests` filter
constructs JUnit 5 test descriptors (based on `@Test`, `@DisplayName`, class/method
names in Jupiter format). When the Vintage engine reports discovered tests, their
descriptors don't match the filter, so they're excluded from execution.

Similarly, `useJUnitPlatform { includeTags("slow") }` only filters JUnit 5 tags.
Vintage engine tests are unaffected by tag filters — they're either all included or
all excluded based on class-pattern `include()`/`exclude()` rules.

## Workarounds

### 1. Class-Pattern Include in Dedicated Test Tasks

For tests that must be isolated (performance, integration), add them to a dedicated
test task using class-pattern includes:

```kotlin
register<Test>("testIntelliJ_2024_1") {
    useJUnitPlatform()
    // ...
    include("**/*ServiceProgressTest.class")
    include("**/*PerformanceTest.class")  // Catches BasePlatformTestCase perf tests
    include("**/*ListenerTest.class")
    // ...
}
```

Run with:
```bash
./gradlew :premium-intellij-plugin:testIntelliJ_2024_1
```

### 2. Avoid `@Tag` on BasePlatformTestCase Tests

`@Tag("slow")` has no effect on JUnit 3 tests. Instead:

- Use class-name patterns for include/exclude (e.g., `exclude("**/*PerformanceTest.class")`)
- Or accept that the test runs in the default `test` task alongside other JUnit 3 tests

### 3. Prefer JUnit 5 for New Tests When Possible

If the test doesn't need IntelliJ Platform fixtures (`getProject()`, `myFixture`, etc.),
write it as a pure JUnit 5 test. This gives full access to `@Tag`, `@DisplayName`,
`--tests` filtering, and `includeTags()`/`excludeTags()`.

If you need the IntelliJ Platform but not `BasePlatformTestCase` directly, consider
using `BasePlatformTestCase` only for setup and writing test methods as JUnit 5-style
methods (though this doesn't fully solve the discovery issue).

### 4. Use `@TempDir` Instead of `getProject().getBasePath()`

For tests that only need a file system location (not a live IntelliJ project), use
JUnit 5's `@TempDir` and avoid `BasePlatformTestCase` entirely:

```java
class MyScannerTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldScanFiles() throws IOException {
        Files.writeString(tempDir.resolve("Test.java"),
            "import javax.servlet.http.HttpServletRequest;");
        // ...
    }
}
```

## Impact on This Project

The `test` task in `premium-intellij-plugin/build.gradle.kts` excludes tests by class
pattern:

```kotlin
test {
    useJUnitPlatform()
    exclude("**/*IntegrationTest.class")
    excludeTags("slow", "integration")  // Only affects JUnit 5 tests
}
```

The `excludeTags` line is ineffective for JUnit 3 tests. Any `BasePlatformTestCase`
test that isn't excluded by a class-pattern rule **will run** in the default `test`
task — which may be undesirable for slow tests.

The matrix tasks (`testIntelliJ_2023_1`, etc.) use `include()` patterns to select
specific test classes. JUnit 3 tests must be explicitly listed in these patterns to
be discovered.

## Verification

To check whether a JUnit 3 test is being discovered, look for its XML result file:

```bash
find premium-intellij-plugin/build/test-results/test -name "*MyTest*.xml"
```

If the file exists, the test ran. If not, it wasn't discovered by the active task.
