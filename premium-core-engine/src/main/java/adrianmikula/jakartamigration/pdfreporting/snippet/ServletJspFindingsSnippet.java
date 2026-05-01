package adrianmikula.jakartamigration.pdfreporting.snippet;

import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;
import adrianmikula.jakartamigration.advancedscanning.domain.ServletJspProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.ServletJspScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.ServletJspUsage;

import java.util.Map;

/**
 * Displays Servlet/JSP findings from ServletJspProjectScanResult.
 * Shows javax.servlet.* and javax.servlet.jsp.* usages.
 */
public class ServletJspFindingsSnippet extends BaseHtmlSnippet {

    private final ComprehensiveScanResults scanResults;

    public ServletJspFindingsSnippet(ComprehensiveScanResults scanResults) {
        this.scanResults = scanResults;
    }

    @Override
    public String generate() throws SnippetGenerationException {
        if (scanResults == null || scanResults.servletJspResults() == null) {
            return generateNoDataMessage();
        }

        ServletJspProjectScanResult result = extractResult();
        if (result == null || !result.hasJavaxUsage()) {
            return generateNoServletUsageMessage();
        }

        return safelyFormat("""
            <div class="section servlet-jsp-findings">
                <h2>Servlet/JSP Findings</h2>
                <p>javax.servlet.* and javax.servlet.jsp.* usage analysis.</p>

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
                                <th>Jakarta Equivalent</th>
                                <th>Type</th>
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
            result.totalFilesWithJavaxUsage(),
            result.totalUsagesFound(),
            generateFindingRows(result)
        );
    }

    @SuppressWarnings("unchecked")
    private ServletJspProjectScanResult extractResult() {
        Map<String, Object> resultsMap = scanResults.servletJspResults();
        if (resultsMap == null) return null;

        for (Object value : resultsMap.values()) {
            if (value instanceof ServletJspProjectScanResult) {
                return (ServletJspProjectScanResult) value;
            }
        }
        return null;
    }

    private String generateFindingRows(ServletJspProjectScanResult result) {
        StringBuilder rows = new StringBuilder();
        int count = 0;
        final int MAX_ROWS = 50;

        for (ServletJspScanResult fileResult : result.fileResults()) {
            if (fileResult.usages() == null) continue;

            String filePath = fileResult.filePath() != null
                ? fileResult.filePath().toString()
                : "Unknown";

            for (ServletJspUsage usage : fileResult.usages()) {
                if (count >= MAX_ROWS) break;

                String jakartaEquiv = usage.hasJakartaEquivalent()
                    ? usage.jakartaEquivalent()
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
                    escapeHtml(jakartaEquiv),
                    escapeHtml(usage.usageType()),
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
            <div class="section servlet-jsp-findings">
                <h2>Servlet/JSP Findings</h2>
                <div class="no-data-message">
                    <p>No Servlet/JSP scan data available.</p>
                </div>
            </div>
            """;
    }

    private String generateNoServletUsageMessage() {
        return """
            <div class="section servlet-jsp-findings">
                <h2>Servlet/JSP Findings</h2>
                <div class="success-message">
                    <p>No javax.servlet.* or javax.servlet.jsp.* usage detected. Project appears clean for Servlet/JSP migration.</p>
                </div>
            </div>
            """;
    }

    @Override
    public boolean isApplicable() {
        if (scanResults == null || scanResults.servletJspResults() == null) {
            return false;
        }
        ServletJspProjectScanResult result = extractResult();
        return result != null && result.hasJavaxUsage();
    }

    @Override
    public int getOrder() {
        return 52; // After CDI Findings
    }
}
