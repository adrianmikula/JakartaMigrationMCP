package adrianmikula.jakartamigration.pdfreporting.snippet;

import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;
import adrianmikula.jakartamigration.advancedscanning.domain.ThirdPartyLibProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.ThirdPartyLibUsage;

import java.util.Map;

/**
 * Displays real third-party library migration findings.
 * All data sourced from actual ThirdPartyLibProjectScanResult.
 */
public class LibraryMigrationSnippet extends BaseHtmlSnippet {

    private final ComprehensiveScanResults scanResults;

    public LibraryMigrationSnippet(ComprehensiveScanResults scanResults) {
        this.scanResults = scanResults;
    }

    @Override
    public String generate() throws SnippetGenerationException {
        if (scanResults == null || scanResults.thirdPartyLibResults() == null) {
            return generateNoDataMessage();
        }

        ThirdPartyLibProjectScanResult libResult = extractLibResult();

        if (libResult == null || !libResult.hasFindings()) {
            return generateNoLibrariesMessage();
        }

        return safelyFormat("""
            <div class="section library-findings">
                <h2>Third-Party Library Migration Guide</h2>
                <p>Real analysis of libraries requiring Jakarta EE migration.</p>

                <div class="findings-summary">
                    <span class="summary-stat">Libraries Requiring Updates: %d</span>
                    <span class="summary-stat risk-level risk-%s">Risk Level: %s</span>
                </div>

                <div class="findings-table-container">
                    <table class="findings-table library-table">
                        <thead>
                            <tr>
                                <th>Library</th>
                                <th>Current Version</th>
                                <th>Issue Type</th>
                                <th>Complexity</th>
                                <th>Suggested Replacement</th>
                            </tr>
                        </thead>
                        <tbody>
                            %s
                        </tbody>
                    </table>
                </div>

                <div class="complexity-legend">
                    <h4>Complexity Levels</h4>
                    <span class="complexity-badge high">HIGH</span>: javax-only libraries requiring major changes
                    <span class="complexity-badge medium">MEDIUM</span>: Partial Jakarta support, needs verification
                    <span class="complexity-badge low">LOW</span>: Outdated but Jakarta-compatible versions available
                </div>
            </div>
            """,
            libResult.getTotalLibraries(),
            libResult.getRiskLevel().toString().toLowerCase(),
            libResult.getRiskLevel(),
            generateLibraryRows(libResult)
        );
    }

    private String generateLibraryRows(ThirdPartyLibProjectScanResult libResult) {
        StringBuilder rows = new StringBuilder();

        for (ThirdPartyLibUsage lib : libResult.getLibraries()) {
            String complexityClass = lib.getComplexity().toString().toLowerCase();
            String issueType = escapeHtml(lib.getIssueType());

            String suggestedReplacement = lib.getSuggestedReplacement() != null
                ? escapeHtml(lib.getSuggestedReplacement())
                : "<em>No replacement suggested</em>";

            rows.append(String.format("""
                <tr class="complexity-%s">
                    <td class="library-coordinates"><code>%s</code></td>
                    <td class="version">%s</td>
                    <td class="issue-type"><span class="badge issue-%s">%s</span></td>
                    <td class="complexity"><span class="complexity-badge %s">%s</span></td>
                    <td class="replacement">%s</td>
                </tr>
                """,
                complexityClass,
                escapeHtml(lib.getCoordinates()),
                escapeHtml(lib.getCurrentVersion()),
                issueType.replace("-", ""),
                issueType,
                complexityClass,
                lib.getComplexity(),
                suggestedReplacement
            ));
        }

        return rows.toString();
    }

    @SuppressWarnings("unchecked")
    private ThirdPartyLibProjectScanResult extractLibResult() {
        Map<String, Object> libMap = scanResults.thirdPartyLibResults();
        if (libMap != null) {
            for (Object value : libMap.values()) {
                if (value instanceof ThirdPartyLibProjectScanResult) {
                    return (ThirdPartyLibProjectScanResult) value;
                }
            }
        }
        return null;
    }

    private String generateNoDataMessage() {
        return """
            <div class="section library-findings">
                <h2>Third-Party Library Migration Guide</h2>
                <div class="no-data-message">
                    <p>No library scan data available. Run third-party library scanner to analyze dependencies.</p>
                </div>
            </div>
            """;
    }

    private String generateNoLibrariesMessage() {
        return """
            <div class="section library-findings">
                <h2>Third-Party Library Migration Guide</h2>
                <div class="success-message">
                    <p>No third-party libraries requiring Jakarta migration detected. All dependencies appear compatible.</p>
                </div>
            </div>
            """;
    }

    @Override
    public boolean isApplicable() {
        return scanResults != null && scanResults.thirdPartyLibResults() != null;
    }

    @Override
    public int getOrder() {
        return 60; // After CDI findings
    }
}
