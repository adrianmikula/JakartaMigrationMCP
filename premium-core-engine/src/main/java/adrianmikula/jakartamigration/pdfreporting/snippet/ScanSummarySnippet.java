package adrianmikula.jakartamigration.pdfreporting.snippet;

import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;

/**
 * HTML snippet displaying scan summary statistics.
 * Displays actual data from ComprehensiveScanResults.summary() only.
 *
 * References: docs/spec/html-refactoring-report-requirements.md Section 2
 */
public class ScanSummarySnippet extends BaseHtmlSnippet {

    private final ComprehensiveScanResults scanResults;

    public ScanSummarySnippet(ComprehensiveScanResults scanResults) {
        this.scanResults = scanResults;
    }

    @Override
    public String getSnippetName() {
        return "Scan Summary";
    }

    @Override
    public String generate() throws SnippetGenerationException {
        if (scanResults == null || scanResults.summary() == null) {
            return generateNoDataMessage();
        }

        var summary = scanResults.summary();
        int totalIssues = scanResults.totalIssuesFound();
        double readinessScore = summary.readinessScore();

        return safelyFormat("""
            <div class="section">
                <h2>Scan Summary</h2>
                <p>Results from Jakarta EE migration readiness analysis.</p>

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
                        <div class="metric-value">%d</div>
                        <div class="metric-label">Total javax References Found</div>
                    </div>
                    <div class="metric-card">
                        <div class="metric-value">%.0f%%</div>
                        <div class="metric-label">Readiness Score</div>
                    </div>
                </div>

                <div class="issue-breakdown">
                    <h4>Issues by Severity</h4>
                    <div class="breakdown-grid">
                        <div class="breakdown-item critical">
                            <span class="breakdown-count">%d</span>
                            <span class="breakdown-label">Critical</span>
                        </div>
                        <div class="breakdown-item warning">
                            <span class="breakdown-count">%d</span>
                            <span class="breakdown-label">Warning</span>
                        </div>
                        <div class="breakdown-item info">
                            <span class="breakdown-count">%d</span>
                            <span class="breakdown-label">Info</span>
                        </div>
                    </div>
                </div>
            </div>
            """,
            summary.totalFilesScanned(),
            summary.filesWithIssues(),
            totalIssues,
            readinessScore * 100,
            summary.criticalIssues(),
            summary.warningIssues(),
            summary.infoIssues()
        );
    }

    private String generateNoDataMessage() {
        return """
            <div class="section">
                <h2>Scan Summary</h2>
                <div class="no-data-message">
                    <p>No scan data available. Run the Jakarta migration scanner to generate analysis results.</p>
                </div>
            </div>
            """;
    }

    @Override
    public boolean isApplicable() {
        return scanResults != null && scanResults.summary() != null;
    }

    @Override
    public int getOrder() {
        return 20; // After header
    }
}
