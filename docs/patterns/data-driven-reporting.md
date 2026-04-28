# Data-Driven Reporting Pattern

This document defines the correct approach for creating and updating HTML reports in the Jakarta Migration MCP project. The core principle is **data fidelity**: reports must display only actual data from existing scanners and services, with zero hardcoding, assumptions, or hallucinated values.

## Core Principle

**Reports are responsible for formatting and displaying data only.**

- ✅ **Correct:** Report displays data from `ComprehensiveScanResults.summary().totalFilesScanned()`
- ❌ **Incorrect:** Report displays "Estimated 100 files will be scanned"

All content displayed in reports must originate from:
- `ComprehensiveScanResults` (actual scanner outputs)
- `RecipeRecommendation` (actual recipe service outputs)
- `RiskAssessment` (actual risk analysis outputs)
- Individual scan result domain objects (JpaProjectScanResult, CdiInjectionProjectScanResult, etc.)

## Architecture Overview

### Snippet-Based Report Generation

Reports are composed of `HtmlSnippet` implementations that generate HTML sections:

```java
public interface HtmlSnippet {
    String generate() throws SnippetGenerationException;  // Generate HTML content
    boolean isApplicable();                              // Should this snippet be included?
    default int getOrder() { return 100; }                 // Sort order in report
}
```

### Report Assembly

The `RefactoringSnippetFactory` creates snippets in order:

```java
public List<HtmlSnippet> createSnippets(RefactoringActionReportRequest request) {
    List<HtmlSnippet> snippets = new ArrayList<>();

    // 1. Header - always included
    snippets.add(new HeaderSnippet(request.projectName(), ...));

    // 2. Scan Summary - displays actual scan statistics
    if (request.scanResults() != null) {
        snippets.add(new ScanSummarySnippet(request.scanResults()));
    }

    // 3. Recipe Recommendations - displays actual recipe data
    if (request.recipeRecommendations() != null) {
        snippets.add(new RecipeRecommendationsSnippet(request.recipeRecommendations()));
    }

    // 4. Scan Findings - displays actual findings from each scanner
    snippets.add(new ScanFindingsByCategorySnippet(request.scanResults()));

    return snippets;
}
```

## Implementation Guidelines

### 1. Extend BaseHtmlSnippet

All snippets should extend `BaseHtmlSnippet` for common utilities:

```java
public class ScanSummarySnippet extends BaseHtmlSnippet {
    private final ComprehensiveScanResults scanResults;

    public ScanSummarySnippet(ComprehensiveScanResults scanResults) {
        this.scanResults = scanResults;
    }

    @Override
    public String generate() throws SnippetGenerationException {
        // Generate HTML using actual data
    }
}
```

### 2. Access Data Through Existing Records/Objects

Use the actual data structures from the domain:

```java
// ✅ CORRECT: Use actual data from scan results
var summary = scanResults.summary();
int totalFiles = summary.totalFilesScanned();
int filesWithIssues = summary.filesWithIssues();
double readinessScore = summary.readinessScore();

// ❌ INCORRECT: Do not hardcode or estimate
int estimatedFiles = 100; // Never hardcode
double estimatedScore = 0.75; // Never estimate
```

### 3. Use safelyFormat for HTML Generation

The `BaseHtmlSnippet.safelyFormat()` method handles formatting safely:

```java
@Override
public String generate() throws SnippetGenerationException {
    var summary = scanResults.summary();

    return safelyFormat("""
        <div class="metrics-grid">
            <div class="metric-card">
                <div class="metric-value">%d</div>
                <div class="metric-label">Files Scanned</div>
            </div>
            <div class="metric-card">
                <div class="metric-value">%.0f%%</div>
                <div class="metric-label">Readiness Score</div>
            </div>
        </div>
        """,
        summary.totalFilesScanned(),
        summary.readinessScore() * 100
    );
}
```

### 4. Check Data Availability in isApplicable()

Only include snippets when data is available:

```java
@Override
public boolean isApplicable() {
    return scanResults != null &&
           scanResults.summary() != null &&
           scanResults.summary().totalFilesScanned() > 0;
}
```

### 5. Handle Missing Data Gracefully

When data is null or empty, show a no-data message rather than omitting the section:

```java
@Override
public String generate() throws SnippetGenerationException {
    if (scanResults == null || scanResults.summary() == null) {
        return """
            <div class="section">
                <h2>Scan Summary</h2>
                <div class="no-data-message">
                    <p>No scan data available. Run the Jakarta migration scanner.</p>
                </div>
            </div>
            """;
    }
    // ... generate content with actual data
}
```

## What to Avoid

### ❌ Hardcoded Checklists

```java
// WRONG: Never hardcode checklist items
private List<String> getChecklistItems() {
    return List.of(
        "Backup your project",
        "Update dependencies",
        "Run OpenRewrite recipes"
    );
}
```

### ❌ Assumed Jakarta Equivalents

```java
// WRONG: Never assume jakarta equivalents
private String getJakartaEquivalent(String javaxName) {
    return javaxName.replace("javax.", "jakarta."); // Don't assume!
}
```

### ❌ Estimated Times or Effort

```java
// WRONG: Never estimate completion times
private int estimateCompletionTime(int fileCount) {
    return fileCount * 5; // Never estimate!
}
```

### ❌ Placeholder/Example Data

```java
// WRONG: Never use example/sample data
private String getExampleRecipe() {
    return "org.openrewrite.java.migrate.jakarta.JavaxMigrationToJakarta";
}
```

### ❌ Generic Descriptions

```java
// WRONG: Never add generic guidance not from scanners
private String getGenericGuidance() {
    return "Consider reviewing the official Jakarta EE migration guide.";
}
```

## Correct Pattern Examples

### Displaying Scan Summary Statistics

```java
public class ScanSummarySnippet extends BaseHtmlSnippet {
    private final ComprehensiveScanResults scanResults;

    @Override
    public String generate() throws SnippetGenerationException {
        if (scanResults == null || scanResults.summary() == null) {
            return generateNoDataMessage();
        }

        var summary = scanResults.summary();

        return safelyFormat("""
            <div class="section">
                <h2>Scan Summary</h2>
                <div class="metrics-grid">
                    <div class="metric-card">
                        <div class="metric-value">%d</div>
                        <div class="metric-label">Files Scanned</div>
                    </div>
                    <div class="metric-card">
                        <div class="metric-value">%d</div>
                        <div class="metric-label">Files with javax References</div>
                    </div>
                    <div class="metric-card">
                        <div class="metric-value">%.0f%%</div>
                        <div class="metric-label">Readiness Score</div>
                    </div>
                </div>
            </div>
            """,
            summary.totalFilesScanned(),
            summary.filesWithIssues(),
            summary.readinessScore() * 100
        );
    }

    @Override
    public boolean isApplicable() {
        return scanResults != null && scanResults.summary() != null;
    }
}
```

### Displaying Recipe Recommendations

```java
public class RecipeRecommendationsSnippet extends BaseHtmlSnippet {
    private final List<RecipeRecommendation> recommendations;

    @Override
    public String generate() throws SnippetGenerationException {
        if (recommendations == null || recommendations.isEmpty()) {
            return generateNoDataMessage();
        }

        StringBuilder cards = new StringBuilder();
        for (RecipeRecommendation rec : recommendations) {
            // Use actual recipe data only
            String recipeName = rec.recipe().name();
            String description = rec.recipe().description();
            double confidence = rec.confidenceScore();
            String reason = rec.reason();
            int affectedFilesCount = rec.affectedFiles().size();
            String openRewriteCommand = rec.recipe().openRewriteRecipeName();

            cards.append(generateRecipeCard(
                recipeName, description, confidence, reason,
                affectedFilesCount, openRewriteCommand
            ));
        }

        return wrapInSection(cards.toString());
    }

    private String generateRecipeCard(String name, String description,
                                     double confidence, String reason,
                                     int affectedFiles, String command) {
        StringBuilder card = new StringBuilder();
        card.append(String.format("""
            <div class="recipe-card">
                <h3>%s</h3>
                <p>%s</p>
                <div class="confidence-score">%.0f%%</div>
                <p class="reason">%s</p>
                <p>Affected files: %d</p>
            """, escapeHtml(name), escapeHtml(description),
                confidence * 100, escapeHtml(reason), affectedFiles));

        // Only include command if it's available in the data
        if (command != null && !command.isBlank()) {
            card.append(String.format("""
                <code>%s</code>
            """, escapeHtml(command)));
        }

        card.append("</div>");
        return card.toString();
    }
}
```

### Displaying Scan Findings by Category

```java
public class ScanFindingsByCategorySnippet extends BaseHtmlSnippet {
    private final ComprehensiveScanResults scanResults;

    private String generateJpaSection(Map<String, Object> jpaResults) {
        // Extract actual JpaProjectScanResult from the map
        JpaProjectScanResult jpaResult = extractResult(jpaResults, JpaProjectScanResult.class);

        // Only proceed if actual data exists and has findings
        if (jpaResult == null || !jpaResult.hasJavaxUsage()) {
            return ""; // Skip section if no data
        }

        StringBuilder rows = new StringBuilder();
        for (JpaScanResult fileResult : jpaResult.fileResults()) {
            if (!fileResult.hasJavaxUsage()) continue;

            String filePath = escapeHtml(fileResult.filePath().toString());

            for (JpaAnnotationUsage usage : fileResult.annotations()) {
                // Use actual data from scan result - never assume
                String jakartaEquivalent = usage.hasJakartaEquivalent()
                    ? escapeHtml(usage.jakartaEquivalent())
                    : "-"; // Show dash if no equivalent provided by scanner

                rows.append(String.format("""
                    <tr>
                        <td>%s</td>
                        <td>%d</td>
                        <td>%s</td>
                        <td>%s</td>
                    </tr>
                """,
                    filePath,
                    usage.lineNumber(),           // Actual line from scanner
                    escapeHtml(usage.annotationName()), // Actual annotation from scanner
                    jakartaEquivalent             // Only if scanner provides it
                ));
            }
        }

        return rows.isEmpty() ? "" : generateTable("JPA Findings", rows.toString());
    }
}
```

## Testing Data-Driven Snippets

### Verify Actual Data Display

```java
@Test
@DisplayName("Should generate summary with actual scan statistics")
void shouldGenerateSummaryWithActualData() throws SnippetGenerationException {
    // Arrange with known values
    var summary = new ComprehensiveScanResults.ScanSummary(
        100, 25, 5, 10, 15, 0.75
    );
    var scanResults = new ComprehensiveScanResults(
        "/test", LocalDateTime.now(),
        Collections.emptyMap(), ..., Collections.emptyList(),
        30, summary
    );
    var snippet = new ScanSummarySnippet(scanResults);

    // Act
    String html = snippet.generate();

    // Assert - verify actual data appears in output
    assertTrue(html.contains("100"), "Should display actual files scanned");
    assertTrue(html.contains("25"), "Should display actual files with issues");
    assertTrue(html.contains("75%"), "Should display actual readiness score");
}
```

### Verify No Hardcoded Values

```java
@Test
@DisplayName("Should not contain hardcoded values")
void shouldNotContainHardcodedValues() throws SnippetGenerationException {
    // Arrange
    var snippet = new ScanSummarySnippet(createMockScanResults());

    // Act
    String html = snippet.generate();

    // Assert - verify no placeholder text
    assertFalse(html.contains("Estimated"), "Should not contain estimated time");
    assertFalse(html.contains("approximately"), "Should not contain approximations");
    assertFalse(html.contains("sample"), "Should not contain sample data");
}
```

## Common Pitfalls and Fixes

### 1. Data Type Mismatches in Tests

When creating mock data for tests, ensure you use the correct domain object types for each scan result type:

```java
// ❌ WRONG: Using JavaxUsage for BeanValidationScanResult
List<JavaxUsage> usages = List.of(
    new JavaxUsage("javax.validation.constraints.NotNull", "jakarta.validation.constraints.NotNull", 25, "field: email")
);
List<BeanValidationScanResult> fileResults = List.of(
    new BeanValidationScanResult(Paths.get("/test/project/User.java"), usages, 50)
);

// ✅ CORRECT: Using BeanValidationUsage for BeanValidationScanResult
List<BeanValidationUsage> usages = List.of(
    new BeanValidationUsage("javax.validation.constraints.NotNull", "jakarta.validation.constraints.NotNull", 25, "email", "field")
);
List<BeanValidationScanResult> fileResults = List.of(
    new BeanValidationScanResult(Paths.get("/test/project/User.java"), usages, 50)
);
```

**Domain object type mapping:**
- `JpaProjectScanResult` → uses `JpaAnnotationUsage`
- `BeanValidationProjectScanResult` → uses `BeanValidationUsage`
- `CdiInjectionProjectScanResult` → uses `CdiInjectionUsage`
- `ServletJspProjectScanResult` → uses `ServletJspUsage`
- `BuildConfigProjectScanResult` → uses `BuildConfigUsage`

### 2. Constructor Parameter Mismatches

Always verify the exact constructor signature of record types:

```java
// ❌ WRONG: BuildConfigUsage requires 7 parameters (groupId, artifactId, currentVersion, jakartaGroupId, jakartaArtifactId, recommendedVersion, lineNumber)
new BuildConfigUsage("javax.servlet", "javax.servlet-api", "jakarta.servlet", "jakarta.servlet-api", "4.0.1", true)

// ✅ CORRECT: All 7 parameters in correct order
new BuildConfigUsage("javax.servlet", "javax.servlet-api", "4.0.0", "jakarta.servlet", "jakarta.servlet-api", "6.0.0", 15)
```

### 3. Scan Type Key Mismatches

When passing scan results to `ScanRecipeRecommendationService`, use the full scan type keys that match `SCAN_TO_RECIPE_MAPPING`:

```java
// ❌ WRONG: Using abbreviated keys
scanResultsMap.put("jpa", scanResults.jpaResults());
scanResultsMap.put("servlet", scanResults.servletJspResults());

// ✅ CORRECT: Using full scan type names
scanResultsMap.put("JPA_ANNOTATION_SCANNER", scanResults.jpaResults());
scanResultsMap.put("SERVLET_JSP_SCANNER", scanResults.servletJspResults());
scanResultsMap.put("BEAN_VALIDATION_SCANNER", scanResults.beanValidationResults());
scanResultsMap.put("CDI_INJECTION_SCANNER", scanResults.cdiResults());
scanResultsMap.put("BUILD_CONFIG_SCANNER", scanResults.buildConfigResults());
scanResultsMap.put("THIRD_PARTY_LIB_SCANNER", scanResults.thirdPartyLibResults());
```

### 4. Path vs String in Scan Results

Some scan result constructors require `Path` objects, not strings:

```java
// ❌ WRONG: Passing String where Path is expected
new BuildConfigScanResult("/test/project/pom.xml", usages, "maven")

// ✅ CORRECT: Using Paths.get() to create Path object
new BuildConfigScanResult(Paths.get("/test/project/pom.xml"), usages, "maven")
```

### 5. Test Structure Best Practices

Follow standard JUnit 5 patterns for test setup:

```java
// ❌ WRONG: Manually creating temp directory
class RefactoringActionReportTest {
    private Path tempDir;

    @BeforeEach
    void setUp() {
        tempDir = Path.of(System.getProperty("java.io.tmpdir"));
    }

    @Test
    void shouldGenerateReport() {
        // Test without exception declaration
    }
}

// ✅ CORRECT: Using @TempDir annotation and declaring exceptions
class RefactoringActionReportTest {
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Setup without tempDir
    }

    @Test
    void shouldGenerateReport() throws Exception {
        // Test with exception declaration for file I/O
    }
}
```

### 6. Data Conversion Between Generic and Specific Types

When converting from generic `ProjectScanResult<T>` to specific types (e.g., `JpaProjectScanResult`), ensure proper conversion logic:

```java
// Example conversion in AdvancedScanningService.getLastScanResults()
private JpaProjectScanResult convertToJpaProjectScanResult(ProjectScanResult<JpaAnnotationUsage> result) {
    if (result == null) {
        return JpaProjectScanResult.empty();
    }
    List<JpaScanResult> fileResults = result.fileResults().stream()
        .map(fr -> new JpaScanResult(fr.filePath(), (List<JpaAnnotationUsage>) fr.usages(), fr.lineCount()))
        .toList();
    return new JpaProjectScanResult(fileResults, result.totalFilesScanned(), result.filesWithIssues(), result.totalIssuesFound());
}
```

## Verification Checklist

When creating or updating report snippets:

- [ ] All displayed data comes from actual method calls on domain objects
- [ ] No hardcoded strings, checklists, or guidance
- [ ] No estimated times, effort, or completion dates
- [ ] No assumed jakarta equivalents or API mappings
- [ ] No placeholder, example, or sample data
- [ ] `isApplicable()` checks for data availability
- [ ] Graceful handling of null/empty data
- [ ] Tests verify actual data appears in output
- [ ] Tests verify no hardcoded values appear in output
- [ ] CSS uses existing classes (no new styles needed)
- [ ] Test mock data uses correct domain object types for each scan result
- [ ] Constructor parameters match record definitions exactly
- [ ] Scan type keys match those in `SCAN_TO_RECIPE_MAPPING`
- [ ] Path objects are used where required (not Strings)
- [ ] Tests use `@TempDir` annotation properly
- [ ] Test methods declare `throws Exception` for file I/O operations

## References

- `docs/spec/html-refactoring-report-requirements.md` - Detailed requirements for HTML refactoring report
- `premium-core-engine/src/main/java/adrianmikula/jakartamigration/pdfreporting/snippet/` - Snippet implementations
- `BaseHtmlSnippet.java` - Abstract base class for snippets
- `HtmlSnippet.java` - Interface definition
- `RefactoringSnippetFactory.java` - Snippet assembly

## Related Patterns

- `test-organization.md` - Testing patterns for snippet validation
- `error-reporting.md` - Error handling in reports
