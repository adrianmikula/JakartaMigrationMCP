package adrianmikula.jakartamigration.pdfreporting.snippet;

import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;
import adrianmikula.jakartamigration.advancedscanning.domain.DeprecatedApiProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.DeprecatedApiScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.DeprecatedApiUsage;

import java.util.Map;

/**
 * Displays deprecated API findings from DeprecatedApiProjectScanResult.
 * Shows deprecated javax.* APIs that should be migrated.
 */
public class DeprecatedApiFindingsSnippet extends BaseHtmlSnippet {

    private final ComprehensiveScanResults scanResults;

    public DeprecatedApiFindingsSnippet(ComprehensiveScanResults scanResults) {
        this.scanResults = scanResults;
    }

    @Override
    public String generate() throws SnippetGenerationException {
        if (scanResults == null) {
            return generateNoDataMessage();
        }

        DeprecatedApiProjectScanResult result = extractResult();
        if (result == null || !result.hasDeprecatedApiUsage()) {
            return generateNoDeprecatedApiMessage();
        }

        return safelyFormat("""
            <div class="section deprecated-api-findings">
                <h2>Deprecated API Usage</h2>
                <p>Deprecated javax.* APIs that should be migrated to Jakarta EE.</p>

                <div class="findings-summary">
                    <div class="summary-stat">
                        <span class="stat-value">%d</span>
                        <span class="stat-label">Files Scanned</span>
                    </div>
                    <div class="summary-stat">
                        <span class="stat-value">%d</span>
                        <span class="stat-label">Files with Deprecated APIs</span>
                    </div>
                    <div class="summary-stat">
                        <span class="stat-value">%d</span>
                        <span class="stat-label">Total Deprecated Usages</span>
                    </div>
                </div>

                <div class="findings-table-container">
                    <table class="findings-table">
                        <thead>
                            <tr>
                                <th>File Path</th>
                                <th>Class</th>
                                <th>Method</th>
                                <th>Deprecation Type</th>
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
            result.totalFilesScanned(),
            result.totalFilesWithDeprecatedApi(),
            result.totalUsagesFound(),
            generateFindingRows(result)
        );
    }

    private DeprecatedApiProjectScanResult extractResult() {
        // Deprecated API results may be stored in various maps
        Map<String, Object>[] mapsToCheck = new Map[]{
            scanResults.servletJspResults(),
            scanResults.thirdPartyLibResults()
        };

        for (Map<String, Object> map : mapsToCheck) {
            if (map == null) continue;
            for (Object value : map.values()) {
                if (value instanceof DeprecatedApiProjectScanResult) {
                    return (DeprecatedApiProjectScanResult) value;
                }
            }
        }
        return null;
    }

    private String generateFindingRows(DeprecatedApiProjectScanResult result) {
        StringBuilder rows = new StringBuilder();
        int count = 0;
        final int MAX_ROWS = 50;

        for (DeprecatedApiScanResult fileResult : result.fileResults()) {
            if (fileResult.usages() == null) continue;

            String filePath = fileResult.filePath() != null
                ? fileResult.filePath().toString()
                : "Unknown";

            for (DeprecatedApiUsage usage : fileResult.usages()) {
                if (count >= MAX_ROWS) break;

                String methodName = usage.methodName() != null && !usage.methodName().isBlank()
                    ? usage.methodName()
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
                    escapeHtml(usage.className()),
                    escapeHtml(methodName),
                    escapeHtml(usage.deprecationType()),
                    usage.lineNumber()
                ));
                count++;
            }

            if (count >= MAX_ROWS) break;
        }

        if (count >= MAX_ROWS && result.totalUsagesFound() > MAX_ROWS) {
            rows.append(String.format("""
                <tr class=\"more-rows\">
                    <td colspan=\"5\">... and %d more usages</td>
                </tr>
                """,
                result.totalUsagesFound() - MAX_ROWS
            ));
        }

        return rows.toString();
    }

    private String generateNoDataMessage() {
        return """
            <div class="section deprecated-api-findings">
                <h2>Deprecated API Usage</h2>
                <div class="no-data-message">
                    <p>No deprecated API scan data available.</p>
                </div>
            </div>
            """;
    }

    private String generateNoDeprecatedApiMessage() {
        return """
            <div class="section deprecated-api-findings">
                <h2>Deprecated API Usage</h2>
                <div class="success-message">
                    <p>No deprecated javax.* APIs detected. All APIs are current.</p>
                </div>
            </div>
            """;
    }

    @Override
    public boolean isApplicable() {
        if (scanResults == null) {
            return false;
        }
        DeprecatedApiProjectScanResult result = extractResult();
        return result != null && result.hasDeprecatedApiUsage();
    }

    @Override
    public int getOrder() {
        return 55; // After REST/SOAP Findings
    }
}
