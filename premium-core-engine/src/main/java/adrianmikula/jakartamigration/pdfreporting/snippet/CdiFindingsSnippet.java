package adrianmikula.jakartamigration.pdfreporting.snippet;

import adrianmikula.jakartamigration.advancedscanning.domain.CdiInjectionProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.CdiInjectionScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.CdiInjectionUsage;
import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;

import java.util.Map;

/**
 * Displays real CDI injection findings from CdiInjectionProjectScanResult.
 * All data sourced from actual javax.inject and javax.enterprise scanner outputs.
 */
public class CdiFindingsSnippet extends BaseHtmlSnippet {

    private final ComprehensiveScanResults scanResults;

    public CdiFindingsSnippet(ComprehensiveScanResults scanResults) {
        this.scanResults = scanResults;
    }

    @Override
    public String generate() throws SnippetGenerationException {
        if (scanResults == null) {
            return generateNoDataMessage();
        }

        CdiInjectionProjectScanResult cdiResult = extractCdiResult();

        if (cdiResult == null || !cdiResult.hasJavaxUsage()) {
            return generateNoCdiUsageMessage();
        }

        return safelyFormat("""
            <div class="section cdi-findings">
                <h2>CDI Injection Findings</h2>
                <p>Real scan results from javax.inject and javax.enterprise usage analysis.</p>

                <div class="findings-summary">
                    <span class="summary-stat">Files with CDI: %d</span>
                    <span class="summary-stat">Total Annotations: %d</span>
                </div>

                <div class="findings-table-container">
                    <table class="findings-table">
                        <thead>
                            <tr>
                                <th>File Path</th>
                                <th>Line</th>
                                <th>Javax Class</th>
                                <th>Jakarta Equivalent</th>
                                <th>Usage Type</th>
                            </tr>
                        </thead>
                        <tbody>
                            %s
                        </tbody>
                    </table>
                </div>
            </div>
            """,
            cdiResult.totalFilesWithJavaxUsage(),
            cdiResult.totalAnnotationsFound(),
            generateCdiRows(cdiResult)
        );
    }

    private String generateCdiRows(CdiInjectionProjectScanResult cdiResult) {
        StringBuilder rows = new StringBuilder();

        for (CdiInjectionScanResult fileResult : cdiResult.fileResults()) {
            if (!fileResult.hasJavaxUsage()) continue;

            String filePath = escapeHtml(fileResult.filePath().toString());

            for (CdiInjectionUsage usage : fileResult.usages()) {
                String jakartaEquivalent = usage.hasJakartaEquivalent()
                    ? escapeHtml(usage.jakartaEquivalent())
                    : "<em>No equivalent available</em>";

                rows.append(String.format("""
                    <tr>
                        <td class="file-path">%s</td>
                        <td class="line-num">%d</td>
                        <td class="javax-ref">%s</td>
                        <td class="jakarta-equiv">%s</td>
                        <td class="usage-type"><span class="badge %s">%s</span></td>
                    </tr>
                    """,
                    filePath,
                    usage.lineNumber(),
                    escapeHtml(usage.className()),
                    jakartaEquivalent,
                    usage.usageType().toLowerCase().replace(" ", "-"),
                    escapeHtml(usage.usageType())
                ));
            }
        }

        return rows.toString();
    }

    @SuppressWarnings("unchecked")
    private CdiInjectionProjectScanResult extractCdiResult() {
        // CDI results are stored in the dedicated cdiResults map
        Map<String, Object> resultsMap = scanResults.cdiResults();
        if (resultsMap != null) {
            for (Object value : resultsMap.values()) {
                if (value instanceof CdiInjectionProjectScanResult) {
                    return (CdiInjectionProjectScanResult) value;
                }
            }
        }
        return null;
    }

    private String generateNoDataMessage() {
        return """
            <div class="section cdi-findings">
                <h2>CDI Injection Findings</h2>
                <div class="no-data-message">
                    <p>No CDI scan data available. Run CDI scanner to detect javax.inject usage.</p>
                </div>
            </div>
            """;
    }

    private String generateNoCdiUsageMessage() {
        return """
            <div class="section cdi-findings">
                <h2>CDI Injection Findings</h2>
                <div class="success-message">
                    <p>No javax.inject or javax.enterprise usage detected. Project appears clean for CDI migration.</p>
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
        return 55; // After JPA findings
    }
}
