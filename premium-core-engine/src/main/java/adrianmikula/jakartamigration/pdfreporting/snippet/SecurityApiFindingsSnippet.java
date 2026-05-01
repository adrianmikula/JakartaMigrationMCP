package adrianmikula.jakartamigration.pdfreporting.snippet;

import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;
import adrianmikula.jakartamigration.advancedscanning.domain.SecurityApiProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.SecurityApiScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.SecurityApiUsage;

import java.util.Map;

/**
 * Displays Security API findings from SecurityApiProjectScanResult.
 * Shows javax.security.* usages with Jakarta equivalents.
 */
public class SecurityApiFindingsSnippet extends BaseHtmlSnippet {

    private final ComprehensiveScanResults scanResults;

    public SecurityApiFindingsSnippet(ComprehensiveScanResults scanResults) {
        this.scanResults = scanResults;
    }

    @Override
    public String generate() throws SnippetGenerationException {
        if (scanResults == null) {
            return generateNoDataMessage();
        }

        // Security API results may be stored in servletJspResults or a dedicated map
        SecurityApiProjectScanResult result = extractResult();
        if (result == null || !result.hasJavaxUsage()) {
            return generateNoSecurityUsageMessage();
        }

        return safelyFormat("""
            <div class="section security-api-findings">
                <h2>Security API Findings</h2>
                <p>javax.security.* usage analysis with Jakarta Security equivalents.</p>

                <div class="findings-summary">
                    <div class="summary-stat">
                        <span class="stat-value">%d</span>
                        <span class="stat-label">Files Scanned</span>
                    </div>
                    <div class="summary-stat">
                        <span class="stat-value">%d</span>
                        <span class="stat-label">Files with javax Usage</span>
                    </div>
                    <div class="summary-stat">
                        <span class="stat-value">%d</span>
                        <span class="stat-label">Total javax Usages</span>
                    </div>
                </div>

                <div class="findings-table-container">
                    <table class="findings-table">
                        <thead>
                            <tr>
                                <th>File Path</th>
                                <th>javax Class</th>
                                <th>Method</th>
                                <th>Jakarta Equivalent</th>
                                <th>Line</th>
                            </tr>
                        </thead>
                        <tbody>
                            %s
                        </tbody>
                    </table>
                </div>
            </div>
            """,
            result.getTotalFilesScanned(),
            result.getFilesWithJavaxUsage(),
            result.getTotalJavaxUsages(),
            generateFindingRows(result)
        );
    }

    @SuppressWarnings("unchecked")
    private SecurityApiProjectScanResult extractResult() {
        // Try to find security API results in available maps
        // They may be stored under various keys
        Map<String, Object>[] mapsToCheck = new Map[]{
            scanResults.servletJspResults(),
            scanResults.thirdPartyLibResults()
        };

        for (Map<String, Object> map : mapsToCheck) {
            if (map == null) continue;
            for (Object value : map.values()) {
                if (value instanceof SecurityApiProjectScanResult) {
                    return (SecurityApiProjectScanResult) value;
                }
            }
        }
        return null;
    }

    private String generateFindingRows(SecurityApiProjectScanResult result) {
        StringBuilder rows = new StringBuilder();
        int count = 0;
        final int MAX_ROWS = 50;

        for (SecurityApiScanResult fileResult : result.getFileResults()) {
            if (fileResult.getUsages() == null) continue;

            String filePath = fileResult.getFilePath() != null
                ? fileResult.getFilePath().toString()
                : "Unknown";

            for (SecurityApiUsage usage : fileResult.getUsages()) {
                if (count >= MAX_ROWS) break;

                String jakartaEquiv = usage.getJakartaEquivalent() != null && !usage.getJakartaEquivalent().isBlank()
                    ? usage.getJakartaEquivalent()
                    : "N/A";

                rows.append(String.format("""
                    <tr>
                        <td>%s</td>
                        <td>%s</td>
                        <td>%s</td>
                        <td>%s</td>
                        <td>%d</td>
                    </tr>
                    """,
                    escapeHtml(filePath),
                    escapeHtml(usage.getJavaxClass()),
                    escapeHtml(usage.getMethod()),
                    escapeHtml(jakartaEquiv),
                    usage.getLineNumber()
                ));
                count++;
            }

            if (count >= MAX_ROWS) break;
        }

        if (count >= MAX_ROWS && result.getTotalJavaxUsages() > MAX_ROWS) {
            rows.append(String.format("""
                <tr class=\"more-rows\">
                    <td colspan=\"5\">... and %d more usages</td>
                </tr>
                """,
                result.getTotalJavaxUsages() - MAX_ROWS
            ));
        }

        return rows.toString();
    }

    private String generateNoDataMessage() {
        return """
            <div class="section security-api-findings">
                <h2>Security API Findings</h2>
                <div class="no-data-message">
                    <p>No Security API scan data available.</p>
                </div>
            </div>
            """;
    }

    private String generateNoSecurityUsageMessage() {
        return """
            <div class="section security-api-findings">
                <h2>Security API Findings</h2>
                <div class="success-message">
                    <p>No javax.security.* usage detected. Project appears clean for Security API migration.</p>
                </div>
            </div>
            """;
    }

    @Override
    public boolean isApplicable() {
        if (scanResults == null) {
            return false;
        }
        SecurityApiProjectScanResult result = extractResult();
        return result != null && result.hasJavaxUsage();
    }

    @Override
    public int getOrder() {
        return 53; // After Servlet/JSP Findings
    }
}
