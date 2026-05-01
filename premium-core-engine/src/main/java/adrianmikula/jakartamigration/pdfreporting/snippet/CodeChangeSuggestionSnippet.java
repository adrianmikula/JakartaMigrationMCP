package adrianmikula.jakartamigration.pdfreporting.snippet;

import adrianmikula.jakartamigration.advancedscanning.domain.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Shows before/after code change suggestions for each finding.
 * Helps developers understand exactly what code changes are needed.
 */
public class CodeChangeSuggestionSnippet extends BaseHtmlSnippet {

    private final ComprehensiveScanResults scanResults;

    public CodeChangeSuggestionSnippet(ComprehensiveScanResults scanResults) {
        this.scanResults = scanResults;
    }

    @Override
    public String generate() throws SnippetGenerationException {
        if (scanResults == null) {
            return generateNoDataMessage();
        }

        List<CodeChange> changes = collectCodeChanges();

        if (changes.isEmpty()) {
            return generateNoChangesMessage();
        }

        return safelyFormat("""
            <div class="section code-change-suggestions">
                <h2>Code Change Suggestions</h2>
                <p>Exact code changes needed for Jakarta EE migration. Copy-paste friendly examples.</p>

                <div class="change-summary">
                    <span class="summary-stat">%d Changes Required</span>
                    <span class="summary-stat highlight-jpa">%d JPA</span>
                    <span class="summary-stat highlight-cdi">%d CDI</span>
                    <span class="summary-stat highlight-servlet">%d Servlet</span>
                </div>

                <div class="changes-container">
                    %s
                </div>
            </div>
            """,
            changes.size(),
            countByCategory(changes, "JPA"),
            countByCategory(changes, "CDI"),
            countByCategory(changes, "Servlet"),
            generateChangeSections(changes)
        );
    }

    private List<CodeChange> collectCodeChanges() {
        List<CodeChange> changes = new ArrayList<>();
        collectJpaChanges(changes);
        collectCdiChanges(changes);
        collectServletChanges(changes);
        return changes;
    }

    private void collectJpaChanges(List<CodeChange> changes) {
        if (scanResults == null || scanResults.jpaResults() == null) return;

        Map<String, Object> jpaMap = scanResults.jpaResults();
        for (Object value : jpaMap.values()) {
            if (value instanceof JpaProjectScanResult jpaResult) {
                for (JpaScanResult fileResult : jpaResult.fileResults()) {
                    for (JpaAnnotationUsage usage : fileResult.annotations()) {
                        changes.add(new CodeChange(
                            "JPA",
                            fileResult.filePath().toString(),
                            usage.lineNumber(),
                            usage.elementName(),
                            usage.annotationName(),
                            usage.jakartaEquivalent(),
                            generateBeforeAfter(usage.annotationName(), usage.jakartaEquivalent(), "annotation"),
                            usage.hasJakartaEquivalent() ? "Replace annotation" : "Manual migration required"
                        ));
                    }
                }
            }
        }
    }

    private void collectCdiChanges(List<CodeChange> changes) {
        if (scanResults == null) return;
        Map<String, Object> cdiMap = scanResults.beanValidationResults();
        if (cdiMap == null) return;

        for (Object value : cdiMap.values()) {
            if (value instanceof CdiInjectionProjectScanResult cdiResult) {
                for (CdiInjectionScanResult fileResult : cdiResult.fileResults()) {
                    for (CdiInjectionUsage usage : fileResult.usages()) {
                        String beforeCode = generateBeforeCode(usage.className(), usage.usageType());
                        String afterCode = generateAfterCode(usage.jakartaEquivalent(), usage.usageType());

                        changes.add(new CodeChange(
                            "CDI",
                            fileResult.filePath().toString(),
                            usage.lineNumber(),
                            usage.context(),
                            beforeCode,
                            afterCode,
                            generateBeforeAfter(beforeCode, afterCode, "code"),
                            usage.hasJakartaEquivalent() ? "Replace import/annotation" : "Manual migration required"
                        ));
                    }
                }
            }
        }
    }

    private void collectServletChanges(List<CodeChange> changes) {
        if (scanResults == null || scanResults.servletJspResults() == null) return;

        Map<String, Object> servletMap = scanResults.servletJspResults();
        for (Object value : servletMap.values()) {
            if (value instanceof ServletJspProjectScanResult servletResult) {
                for (ServletJspScanResult fileResult : servletResult.fileResults()) {
                    for (ServletJspUsage usage : fileResult.usages()) {
                        changes.add(new CodeChange(
                            "Servlet",
                            fileResult.filePath().toString(),
                            usage.lineNumber(),
                            usage.context(),
                            usage.className(),
                            usage.jakartaEquivalent(),
                            generateBeforeAfter(usage.className(), usage.jakartaEquivalent(), "import"),
                            usage.hasJakartaEquivalent() ? "Replace import" : "Manual migration required"
                        ));
                    }
                }
            }
        }
    }

    private String generateBeforeCode(String javaxClass, String usageType) {
        return switch (usageType.toLowerCase()) {
            case "annotation" -> "@" + simpleName(javaxClass);
            case "import" -> "import " + javaxClass + ";";
            case "interface" -> "implements " + simpleName(javaxClass);
            default -> javaxClass;
        };
    }

    private String generateAfterCode(String jakartaClass, String usageType) {
        if (jakartaClass == null || jakartaClass.isBlank()) {
            return "/* No direct equivalent - manual review required */";
        }
        return switch (usageType.toLowerCase()) {
            case "annotation" -> "@" + simpleName(jakartaClass);
            case "import" -> "import " + jakartaClass + ";";
            case "interface" -> "implements " + simpleName(jakartaClass);
            default -> jakartaClass;
        };
    }

    private String simpleName(String fullName) {
        int lastDot = fullName.lastIndexOf('.');
        return lastDot > 0 ? fullName.substring(lastDot + 1) : fullName;
    }

    private String generateBeforeAfter(String before, String after, String type) {
        return safelyFormat("""
            <div class="code-comparison">
                <div class="code-before">
                    <div class="code-label">Current (javax)</div>
                    <pre><code>%s</code></pre>
                </div>
                <div class="code-arrow">→</div>
                <div class="code-after">
                    <div class="code-label">New (jakarta)</div>
                    <pre><code>%s</code></pre>
                </div>
            </div>
            """,
            escapeHtml(before != null ? before : "N/A"),
            escapeHtml(after != null ? after : "No equivalent available")
        );
    }

    private String generateChangeSections(List<CodeChange> changes) {
        StringBuilder sections = new StringBuilder();

        // Group by category
        String[] categories = {"JPA", "CDI", "Servlet"};
        for (String category : categories) {
            List<CodeChange> categoryChanges = changes.stream()
                .filter(c -> c.category().equals(category))
                .limit(5) // Show first 5 per category to keep report manageable
                .toList();

            if (!categoryChanges.isEmpty()) {
                sections.append(generateCategorySection(category, categoryChanges));
            }
        }

        int remaining = changes.size() - Math.min(changes.size(), 15);
        if (remaining > 0) {
            sections.append(String.format("""
                <div class="more-changes-notice">
                    <p>... and %d more changes. See detailed findings sections for complete list.</p>
                </div>
                """, remaining));
        }

        return sections.toString();
    }

    private String generateCategorySection(String category, List<CodeChange> changes) {
        StringBuilder section = new StringBuilder();
        section.append(String.format("""
            <div class="category-section category-%s">
                <h3>%s Changes</h3>
                """,
            category.toLowerCase(),
            category
        ));

        for (CodeChange change : changes) {
            section.append(String.format("""
                <div class="change-item">
                    <div class="change-header">
                        <span class="file-path">%s</span>
                        <span class="line-num">Line %d</span>
                        <span class="action-type">%s</span>
                    </div>
                    %s
                </div>
                """,
                escapeHtml(change.filePath()),
                change.lineNumber(),
                escapeHtml(change.action()),
                change.beforeAfter()
            ));
        }

        section.append("</div>");
        return section.toString();
    }

    private long countByCategory(List<CodeChange> changes, String category) {
        return changes.stream().filter(c -> c.category().equals(category)).count();
    }

    private String generateNoDataMessage() {
        return """
            <div class="section code-change-suggestions">
                <h2>Code Change Suggestions</h2>
                <div class="no-data-message">
                    <p>No scan data available. Run advanced scans to generate code change suggestions.</p>
                </div>
            </div>
            """;
    }

    private String generateNoChangesMessage() {
        return """
            <div class="section code-change-suggestions">
                <h2>Code Change Suggestions</h2>
                <div class="success-message">
                    <p>No Jakarta EE migration code changes detected. Project appears to already use Jakarta EE APIs.</p>
                </div>
            </div>
            """;
    }

    @Override
    public boolean isApplicable() {
        return scanResults != null;
    }

    @Override
    public int getOrder() {
        return 32; // After quick wins, before coverage
    }

    /**
     * Record for code change data
     */
    private record CodeChange(
        String category,
        String filePath,
        int lineNumber,
        String element,
        String before,
        String after,
        String beforeAfter,
        String action
    ) {}
}
