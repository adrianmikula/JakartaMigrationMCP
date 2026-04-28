package adrianmikula.jakartamigration.pdfreporting.snippet;

import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;
import adrianmikula.jakartamigration.advancedscanning.domain.JpaAnnotationUsage;
import adrianmikula.jakartamigration.advancedscanning.domain.JpaProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.JpaScanResult;

import java.util.Map;

/**
 * Displays real JPA scan findings from JpaProjectScanResult.
 * All data sourced from actual javax.persistence.* scanner outputs.
 */
public class JpaFindingsSnippet extends BaseHtmlSnippet {

    private final ComprehensiveScanResults scanResults;

    public JpaFindingsSnippet(ComprehensiveScanResults scanResults) {
        this.scanResults = scanResults;
    }

    @Override
    public String generate() throws SnippetGenerationException {
        if (scanResults == null || scanResults.jpaResults() == null) {
            return generateNoDataMessage();
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> jpaResultsMap = (Map<String, Object>) scanResults.jpaResults();
        JpaProjectScanResult jpaResult = extractJpaResult(jpaResultsMap);

        if (jpaResult == null || !jpaResult.hasJavaxUsage()) {
            return generateNoJpaUsageMessage();
        }

        return safelyFormat("""
            <div class="section jpa-findings">
                <h2>JPA Annotation Findings</h2>
                <p>Real scan results from javax.persistence.* usage analysis.</p>

                <div class="findings-summary">
                    <span class="summary-stat">Files with JPA: %d</span>
                    <span class="summary-stat">Total Annotations: %d</span>
                </div>

                <div class="findings-table-container">
                    <table class="findings-table">
                        <thead>
                            <tr>
                                <th>File Path</th>
                                <th>Line</th>
                                <th>Javax Annotation</th>
                                <th>Jakarta Equivalent</th>
                                <th>Element</th>
                            </tr>
                        </thead>
                        <tbody>
                            %s
                        </tbody>
                    </table>
                </div>
            </div>
            """,
            jpaResult.totalFilesWithJavaxUsage(),
            jpaResult.totalAnnotationsFound(),
            generateJpaRows(jpaResult)
        );
    }

    private String generateJpaRows(JpaProjectScanResult jpaResult) {
        StringBuilder rows = new StringBuilder();

        for (JpaScanResult fileResult : jpaResult.fileResults()) {
            if (!fileResult.hasJavaxUsage()) continue;

            String filePath = escapeHtml(fileResult.filePath().toString());

            for (JpaAnnotationUsage usage : fileResult.annotations()) {
                String jakartaEquivalent = usage.hasJakartaEquivalent()
                    ? escapeHtml(usage.jakartaEquivalent())
                    : "<em>No equivalent available</em>";

                rows.append(String.format("""
                    <tr>
                        <td class="file-path">%s</td>
                        <td class="line-num">%d</td>
                        <td class="javax-ref">%s</td>
                        <td class="jakarta-equiv">%s</td>
                        <td class="element">%s</td>
                    </tr>
                    """,
                    filePath,
                    usage.lineNumber(),
                    escapeHtml(usage.annotationName()),
                    jakartaEquivalent,
                    escapeHtml(usage.elementName() != null ? usage.elementName() : "-")
                ));
            }
        }

        return rows.toString();
    }

    @SuppressWarnings("unchecked")
    private JpaProjectScanResult extractJpaResult(Map<String, Object> jpaResultsMap) {
        // Try to get the result object from the map
        Object result = jpaResultsMap.get("result");
        if (result instanceof JpaProjectScanResult) {
            return (JpaProjectScanResult) result;
        }
        // Fallback: try to find any JpaProjectScanResult in the map values
        for (Object value : jpaResultsMap.values()) {
            if (value instanceof JpaProjectScanResult) {
                return (JpaProjectScanResult) value;
            }
        }
        return null;
    }

    private String generateNoDataMessage() {
        return """
            <div class="section jpa-findings">
                <h2>JPA Annotation Findings</h2>
                <div class="no-data-message">
                    <p>No JPA scan data available. Run JPA scanner to detect javax.persistence.* usage.</p>
                </div>
            </div>
            """;
    }

    private String generateNoJpaUsageMessage() {
        return """
            <div class="section jpa-findings">
                <h2>JPA Annotation Findings</h2>
                <div class="success-message">
                    <p>No javax.persistence.* usage detected. Project appears clean for JPA migration.</p>
                </div>
            </div>
            """;
    }

    @Override
    public boolean isApplicable() {
        return scanResults != null && scanResults.jpaResults() != null;
    }

    @Override
    public int getOrder() {
        return 50; // After coverage, before CDI
    }
}
