package adrianmikula.jakartamigration.pdfreporting.snippet;

import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;

/**
 * Displays real scan coverage statistics from ScanSummary.
 * All data sourced from actual scanner outputs - no estimates.
 */
public class ScanCoverageSnippet extends BaseHtmlSnippet {

    private final ComprehensiveScanResults scanResults;

    public ScanCoverageSnippet(ComprehensiveScanResults scanResults) {
        this.scanResults = scanResults;
    }

    @Override
    public String generate() throws SnippetGenerationException {
        if (scanResults == null || scanResults.summary() == null) {
            return generateNoDataMessage();
        }

        var summary = scanResults.summary();
        int totalFiles = summary.totalFilesScanned();
        int filesWithIssues = summary.filesWithIssues();
        int cleanFiles = totalFiles - filesWithIssues;
        int critical = summary.criticalIssues();
        int warnings = summary.warningIssues();
        int info = summary.infoIssues();
        double readiness = summary.readinessScore();

        return safelyFormat("""
            <div class="section scan-coverage">
                <h2>Scan Coverage Analysis</h2>
                <p>Based on actual file system scan of the project.</p>

                <div class="metrics-grid coverage-stats">
                    <div class="metric-card scanned">
                        <div class="metric-value">%d</div>
                        <div class="metric-label">Files Scanned</div>
                    </div>
                    <div class="metric-card issues">
                        <div class="metric-value">%d</div>
                        <div class="metric-label">Files with Issues</div>
                    </div>
                    <div class="metric-card clean">
                        <div class="metric-value">%d</div>
                        <div class="metric-label">Clean Files</div>
                    </div>
                    <div class="metric-card readiness">
                        <div class="metric-value">%.0f%%</div>
                        <div class="metric-label">Readiness Score</div>
                    </div>
                </div>

                <h3>Issue Severity Distribution</h3>
                <div class="severity-distribution">
                    <div class="severity-bar">
                        <div class="severity-segment critical" style="width: %d%%;">
                            <span class="severity-count">%d Critical</span>
                        </div>
                        <div class="severity-segment warning" style="width: %d%%;">
                            <span class="severity-count">%d Warning</span>
                        </div>
                        <div class="severity-segment info" style="width: %d%%;">
                            <span class="severity-count">%d Info</span>
                        </div>
                    </div>
                </div>
            </div>
            """,
            totalFiles,
            filesWithIssues,
            cleanFiles,
            readiness,
            calculatePercentage(critical, critical + warnings + info),
            critical,
            calculatePercentage(warnings, critical + warnings + info),
            warnings,
            calculatePercentage(info, critical + warnings + info),
            info
        );
    }

    private String generateNoDataMessage() {
        return """
            <div class="section scan-coverage">
                <h2>Scan Coverage Analysis</h2>
                <div class="no-data-message">
                    <p>No scan data available. Run advanced scans to populate coverage statistics.</p>
                </div>
            </div>
            """;
    }

    private int calculatePercentage(int part, int total) {
        if (total == 0) return 0;
        return Math.max(1, (part * 100) / total); // Minimum 1% for visibility
    }

    @Override
    public boolean isApplicable() {
        return scanResults != null;
    }

    @Override
    public int getOrder() {
        return 35; // After metrics, before detailed findings
    }
}
