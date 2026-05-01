package adrianmikula.jakartamigration.pdfreporting.snippet;

import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;
import adrianmikula.jakartamigration.advancedscanning.domain.ReflectionUsageProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.ReflectionUsageScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.ReflectionUsage;

import java.util.Map;

/**
 * Displays reflection usage findings from ReflectionUsageProjectScanResult.
 * Shows reflection patterns that may break during namespace migration.
 */
public class ReflectionUsageFindingsSnippet extends BaseHtmlSnippet {

    private final ComprehensiveScanResults scanResults;

    public ReflectionUsageFindingsSnippet(ComprehensiveScanResults scanResults) {
        this.scanResults = scanResults;
    }

    @Override
    public String generate() throws SnippetGenerationException {
        if (scanResults == null) {
            return generateNoDataMessage();
        }

        ReflectionUsageProjectScanResult result = extractResult();
        if (result == null || !result.hasFindings()) {
            return generateNoReflectionUsageMessage();
        }

        return safelyFormat("""
            <div class="section reflection-usage-findings">
                <h2>Reflection Usage</h2>
                <p>Reflection patterns that may require attention during Jakarta EE migration.</p>

                <div class="findings-summary">
                    <div class="summary-stat">
                        <span class="stat-value">%d</span>
                        <span class="stat-label">Files with Reflection</span>
                    </div>
                    <div class="summary-stat">
                        <span class="stat-value">%d</span>
                        <span class="stat-label">Total Reflection Usages</span>
                    </div>
                </div>

                <div class="findings-table-container">
                    <table class="findings-table">
                        <thead>
                            <tr>
                                <th>File Path</th>
                                <th>Usage Type</th>
                                <th>Reflection Target</th>
                                <th>Risk Assessment</th>
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
            result.getFilesWithFindings(),
            result.getTotalUsagesFound(),
            generateFindingRows(result)
        );
    }

    private ReflectionUsageProjectScanResult extractResult() {
        // Reflection usage results may be stored in various maps
        Map<String, Object>[] mapsToCheck = new Map[]{
            scanResults.servletJspResults(),
            scanResults.thirdPartyLibResults()
        };

        for (Map<String, Object> map : mapsToCheck) {
            if (map == null) continue;
            for (Object value : map.values()) {
                if (value instanceof ReflectionUsageProjectScanResult) {
                    return (ReflectionUsageProjectScanResult) value;
                }
            }
        }
        return null;
    }

    private String generateFindingRows(ReflectionUsageProjectScanResult result) {
        StringBuilder rows = new StringBuilder();
        int count = 0;
        final int MAX_ROWS = 50;

        for (ReflectionUsageScanResult fileResult : result.getFileResults()) {
            if (fileResult.getUsages() == null) continue;

            String filePath = fileResult.getFilePath() != null
                ? fileResult.getFilePath()
                : "Unknown";

            for (ReflectionUsage usage : fileResult.getUsages()) {
                if (count >= MAX_ROWS) break;

                rows.append(String.format("""
                    <tr>
                        <td>%s</td>
                        <td>%s</td>
                        <td>%s</td>
                        <td class=\"risk-cell\">%s</td>
                        <td>%d</td>
                    </tr>
                    """,
                    escapeHtml(filePath),
                    escapeHtml(usage.getUsageType()),
                    escapeHtml(usage.getReflectionTarget()),
                    escapeHtml(usage.getRiskAssessment()),
                    usage.getLineNumber()
                ));
                count++;
            }

            if (count >= MAX_ROWS) break;
        }

        if (count >= MAX_ROWS && result.getTotalUsagesFound() > MAX_ROWS) {
            rows.append(String.format("""
                <tr class=\"more-rows\">
                    <td colspan=\"5\">... and %d more usages</td>
                </tr>
                """,
                result.getTotalUsagesFound() - MAX_ROWS
            ));
        }

        return rows.toString();
    }

    private String generateNoDataMessage() {
        return """
            <div class="section reflection-usage-findings">
                <h2>Reflection Usage</h2>
                <div class="no-data-message">
                    <p>No reflection usage scan data available.</p>
                </div>
            </div>
            """;
    }

    private String generateNoReflectionUsageMessage() {
        return """
            <div class="section reflection-usage-findings">
                <h2>Reflection Usage</h2>
                <div class="success-message">
                    <p>No reflection usage detected. No namespace-related reflection risks identified.</p>
                </div>
            </div>
            """;
    }

    @Override
    public boolean isApplicable() {
        if (scanResults == null) {
            return false;
        }
        ReflectionUsageProjectScanResult result = extractResult();
        return result != null && result.hasFindings();
    }

    @Override
    public int getOrder() {
        return 57; // After Docker/CI/CD Findings
    }
}
