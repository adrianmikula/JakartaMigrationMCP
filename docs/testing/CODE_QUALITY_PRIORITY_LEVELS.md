# Code Quality Priority Levels

## Overview

The code quality tools are configured to fail the build only on **high-priority** issues, while allowing **medium/low-priority** issues to pass with warnings. This ensures critical bugs are caught while not blocking development on minor style issues.

## Priority Classification

### SpotBugs

**High Priority (Fails Build):**
- Rank 1-9: Critical bugs
  - Null pointer dereferences
  - Resource leaks
  - Infinite loops
  - Dead code
  - Incorrect equals()/hashCode() implementations

**Medium/Low Priority (Warnings Only):**
- Rank 10-20: Less critical issues
  - Style suggestions
  - Performance optimizations
  - Code smell warnings

### PMD

**High Priority (Fails Build):**
- Priority 1: Critical code quality issues
  - Empty catch blocks
  - Unused variables
  - Broken null checks
  - Missing break in switch
  - Return from finally block

**Medium/Low Priority (Warnings Only):**
- Priority 2-5: Code quality suggestions
  - Complexity warnings
  - Style recommendations
  - Best practice suggestions

### Checkstyle

**All Issues (Warnings Only):**
- All Checkstyle issues are treated as warnings
- Build will not fail on style violations
- Issues are reported for team awareness

### OWASP Dependency Check

**High Priority (Fails Build):**
- CVSS Score >= 7.0: Critical/High vulnerabilities
  - Remote code execution
  - SQL injection
  - Cross-site scripting (XSS)
  - Authentication bypass

**Medium/Low Priority (Warnings Only):**
- CVSS Score < 7.0: Medium/Low vulnerabilities
  - Information disclosure
  - Denial of service
  - Low-severity issues

## Build Behavior

### Local Development
```bash
# Run analysis (generates reports)
./gradlew codeQualityCheck

# Verify and fail on high-priority issues
./gradlew codeQualityVerify
```

### CI Pipeline
1. Runs `codeQualityCheck` to generate reports
2. Runs `codeQualityVerify` which:
   - ✅ **Fails build** if high-priority bugs found
   - ⚠️ **Warns but passes** if only medium/low-priority issues found
   - 📊 **Uploads reports** as artifacts for review

## Example Output

### High-Priority Bug Found (Build Fails)
```
❌ SpotBugs found 2 HIGH PRIORITY bugs (build will fail):
  - NP_NULL_ON_SOME_PATH in MyService.java:42 (rank=1, priority=HIGH)
  - RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE in MyService.java:58 (rank=2, priority=HIGH)

⚠️ SpotBugs found 5 medium/low priority issues (warnings only):
  - DM_STRING_CTOR in MyService.java:12 (rank=12, priority=MEDIUM)
  ...
```

### Only Medium/Low-Priority Issues (Build Passes)
```
✅ SpotBugs: No high-priority bugs found
   (5 medium/low priority issues - see report for details)

⚠️ PMD found 3 medium/low priority issues (warnings only):
  - UnusedLocalVariable in MyService.java:15
  ...
```

## Adjusting Priority Thresholds

To change what fails the build, edit `build.gradle.kts`:

### SpotBugs
```kotlin
// Change rank threshold (currently 9)
if (rank <= 9) {  // Change to 7 for stricter, or 12 for more lenient
    highPriorityBugs.add(bugInfo)
}
```

### PMD
```kotlin
// Change priority threshold (currently 1)
if (priority == 1) {  // Change to <= 2 for stricter
    highPriorityIssues.add(issueInfo)
}
```

### OWASP
```kotlin
// Change CVSS threshold (currently 7.0)
failBuildOnCVSS = 7.0f  // Change to 8.0f for stricter, 6.0f for more lenient
```

## Best Practices

1. **Fix High-Priority Issues Immediately**: These are real bugs that should be addressed
2. **Review Medium/Low-Priority Issues**: Address during code reviews or refactoring
3. **Use Suppressions Sparingly**: Only suppress false positives, document why
4. **Monitor Trends**: Track quality metrics over time
5. **Team Alignment**: Ensure team understands priority levels

## Suppressing False Positives

### SpotBugs
Add to `config/spotbugs/exclude.xml`:
```xml
<Match>
    <Bug pattern="NP_NULL_ON_SOME_PATH"/>
    <Class name="com.example.MyService"/>
    <Method name="specificMethod"/>
</Match>
```

### PMD
Add annotation to code:
```java
@SuppressWarnings("PMD.EmptyCatchBlock")
public void method() {
    try {
        // code
    } catch (Exception e) {
        // intentionally empty
    }
}
```

### OWASP
Add to `config/owasp/suppressions.xml`:
```xml
<suppress>
    <notes>False positive - vulnerability doesn't affect our use case</notes>
    <packageUrl regex="true">^pkg:maven/.*/.*@.*$</packageUrl>
    <cve>CVE-2023-XXXXX</cve>
</suppress>
```

