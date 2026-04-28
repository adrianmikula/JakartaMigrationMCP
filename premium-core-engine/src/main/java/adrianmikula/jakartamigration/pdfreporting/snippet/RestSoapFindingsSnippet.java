package adrianmikula.jakartamigration.pdfreporting.snippet;

import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;
import adrianmikula.jakartamigration.advancedscanning.domain.RestSoapProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.RestSoapScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.RestSoapUsage;

import java.util.Map;

/**
 * Displays REST/SOAP service findings from RestSoapProjectScanResult.
 * Shows JAX-RS (REST) and JAX-WS (SOAP) service usage.
 */
public class RestSoapFindingsSnippet extends BaseHtmlSnippet {

    private final ComprehensiveScanResults scanResults;

    public RestSoapFindingsSnippet(ComprehensiveScanResults scanResults) {
        this.scanResults = scanResults;
    }

    @Override
    public String generate() throws SnippetGenerationException {
        if (scanResults == null) {
            return generateNoDataMessage();
        }

        RestSoapProjectScanResult result = extractResult();
        if (result == null || !result.hasJavaxUsage()) {
            return generateNoUsageMessage();
        }

        int restCount = countByType(result, "REST");
        int soapCount = countByType(result, "SOAP");

        return safelyFormat("""
            <div class="section rest-soap-findings">
                <h2>REST/SOAP Services</h2>
                <p>JAX-RS (REST) and JAX-WS (SOAP) service analysis.</p>

                <div class="service-summary">
                    <div class="service-stat">
                        <span class="stat-value">%d</span>
                        <span class="stat-label">REST Endpoints</span>
                    </div>
                    <div class="service-stat">
                        <span class="stat-value">%d</span>
                        <span class="stat-label">SOAP Services</span>
                    </div>
                    <div class="service-stat">
                        <span class="stat-value">%d</span>
                        <span class="stat-label">Files with javax Usage</span>
                    </div>
                </div>

                <div class="findings-table-container">
                    <table class="findings-table">
                        <thead>
                            <tr>
                                <th>File Path</th>
                                <th>Service Type</th>
                                <th>javax Class</th>
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
            restCount,
            soapCount,
            result.totalFilesWithJavaxUsage(),
            generateFindingRows(result)
        );
    }

    private RestSoapProjectScanResult extractResult() {
        // REST/SOAP results may be stored in various maps
        Map<String, Object>[] mapsToCheck = new Map[]{
            scanResults.servletJspResults(),
            scanResults.thirdPartyLibResults()
        };

        for (Map<String, Object> map : mapsToCheck) {
            if (map == null) continue;
            for (Object value : map.values()) {
                if (value instanceof RestSoapProjectScanResult) {
                    return (RestSoapProjectScanResult) value;
                }
            }
        }
        return null;
    }

    private int countByType(RestSoapProjectScanResult result, String type) {
        int count = 0;
        for (RestSoapScanResult fileResult : result.fileResults()) {
            if (fileResult.usages() == null) continue;
            for (RestSoapUsage usage : fileResult.usages()) {
                if (type.equalsIgnoreCase(usage.usageType())) {
                    count++;
                }
            }
        }
        return count;
    }

    private String generateFindingRows(RestSoapProjectScanResult result) {
        StringBuilder rows = new StringBuilder();
        int count = 0;
        final int MAX_ROWS = 50;

        for (RestSoapScanResult fileResult : result.fileResults()) {
            if (fileResult.usages() == null) continue;

            String filePath = fileResult.filePath() != null
                ? fileResult.filePath().toString()
                : "Unknown";

            for (RestSoapUsage usage : fileResult.usages()) {
                if (count >= MAX_ROWS) break;

                String jakartaEquiv = usage.jakartaEquivalent() != null && !usage.jakartaEquivalent().isBlank()
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
                    escapeHtml(usage.usageType()),
                    escapeHtml(usage.className()),
                    escapeHtml(jakartaEquiv),
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
            <div class="section rest-soap-findings">
                <h2>REST/SOAP Services</h2>
                <div class="no-data-message">
                    <p>No REST/SOAP scan data available.</p>
                </div>
            </div>
            """;
    }

    private String generateNoUsageMessage() {
        return """
            <div class="section rest-soap-findings">
                <h2>REST/SOAP Services</h2>
                <div class="success-message">
                    <p>No JAX-RS or JAX-WS usage detected. Project appears clean for REST/SOAP migration.</p>
                </div>
            </div>
            """;
    }

    @Override
    public boolean isApplicable() {
        if (scanResults == null) {
            return false;
        }
        RestSoapProjectScanResult result = extractResult();
        return result != null && result.hasJavaxUsage();
    }

    @Override
    public int getOrder() {
        return 54; // After Security API Findings
    }
}
