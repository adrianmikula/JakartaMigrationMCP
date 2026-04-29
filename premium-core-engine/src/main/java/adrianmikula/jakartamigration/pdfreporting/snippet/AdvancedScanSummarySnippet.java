package adrianmikula.jakartamigration.pdfreporting.snippet;

import adrianmikula.jakartamigration.advancedscanning.domain.*;

import java.util.Map;

/**
 * HTML snippet displaying advanced scan results summary by category.
 * Shows total issues per category without detailed file/line information.
 *
 * References: docs/spec/html-to-pdf-reporting.tsp AdvancedScanSummary
 */
public class AdvancedScanSummarySnippet extends BaseHtmlSnippet {

    private final ComprehensiveScanResults scanResults;

    public AdvancedScanSummarySnippet(ComprehensiveScanResults scanResults) {
        this.scanResults = scanResults;
    }

    @Override
    public String getSnippetName() {
        return "Advanced Scan Summary";
    }

    @Override
    public String generate() throws SnippetGenerationException {
        if (scanResults == null) {
            return generateNoDataMessage();
        }

        // Extract counts from each category
        int jpaCount = extractJpaCount();
        int cdiCount = extractCdiCount();
        int servletCount = extractServletCount();
        int buildConfigCount = extractBuildConfigCount();
        int thirdPartyCount = extractThirdPartyCount();
        int transitiveDepCount = extractTransitiveDepCount();
        int totalIssues = scanResults.totalIssuesFound();

        // If all counts are zero, show no data message
        if (jpaCount == 0 && cdiCount == 0 && servletCount == 0 && 
            buildConfigCount == 0 && thirdPartyCount == 0 && transitiveDepCount == 0) {
            return generateNoIssuesMessage();
        }

        return safelyFormat("""
            <div class="section">
                <h2>Advanced Scan Results Summary</h2>
                <p>Summary of javax usage detected by category. Total issues found: %d</p>

                <div class="metrics-grid">
                    %s
                    %s
                    %s
                    %s
                    %s
                    %s
                </div>
            </div>
            """,
            totalIssues,
            generateMetricCard("JPA Annotations", jpaCount, jpaCount > 0),
            generateMetricCard("CDI/Injection", cdiCount, cdiCount > 0),
            generateMetricCard("Servlet/JSP", servletCount, servletCount > 0),
            generateMetricCard("Build Config", buildConfigCount, buildConfigCount > 0),
            generateMetricCard("Third-Party Libraries", thirdPartyCount, thirdPartyCount > 0),
            generateMetricCard("Transitive Dependencies", transitiveDepCount, transitiveDepCount > 0)
        );
    }

    private String generateMetricCard(String label, int count, boolean hasIssues) {
        String cardClass = hasIssues ? "metric-card" : "metric-card metric-card-empty";
        String valueColor = hasIssues ? "#2c3e50" : "#95a5a6";
        
        return safelyFormat("""
            <div class="%s">
                <div class="metric-value" style="color: %s;">%d</div>
                <div class="metric-label">%s</div>
            </div>
            """,
            cardClass,
            valueColor,
            count,
            label
        );
    }

    private int extractJpaCount() {
        if (scanResults.jpaResults() == null || scanResults.jpaResults().isEmpty()) {
            return 0;
        }
        JpaProjectScanResult result = extractResult(scanResults.jpaResults(), JpaProjectScanResult.class);
        return result != null ? result.totalAnnotationsFound() : 0;
    }

    private int extractCdiCount() {
        if (scanResults.cdiResults() == null || scanResults.cdiResults().isEmpty()) {
            return 0;
        }
        CdiInjectionProjectScanResult result = extractResult(scanResults.cdiResults(), CdiInjectionProjectScanResult.class);
        return result != null ? result.totalAnnotationsFound() : 0;
    }

    private int extractServletCount() {
        if (scanResults.servletJspResults() == null || scanResults.servletJspResults().isEmpty()) {
            return 0;
        }
        ServletJspProjectScanResult result = extractResult(scanResults.servletJspResults(), ServletJspProjectScanResult.class);
        return result != null ? result.totalUsagesFound() : 0;
    }

    private int extractBuildConfigCount() {
        if (scanResults.buildConfigResults() == null || scanResults.buildConfigResults().isEmpty()) {
            return 0;
        }
        BuildConfigProjectScanResult result = extractResult(scanResults.buildConfigResults(), BuildConfigProjectScanResult.class);
        return result != null ? result.totalDependenciesFound() : 0;
    }

    private int extractThirdPartyCount() {
        if (scanResults.thirdPartyLibResults() == null || scanResults.thirdPartyLibResults().isEmpty()) {
            return 0;
        }
        // Third party results may be a map, count entries
        return scanResults.thirdPartyLibResults().size();
    }

    private int extractTransitiveDepCount() {
        if (scanResults.transitiveDependencyResults() == null || scanResults.transitiveDependencyResults().isEmpty()) {
            return 0;
        }
        // Transitive dependency results may be a map, count entries
        return scanResults.transitiveDependencyResults().size();
    }

    private <T> T extractResult(Map<String, Object> resultsMap, Class<T> type) {
        for (Object value : resultsMap.values()) {
            if (type.isInstance(value)) {
                return type.cast(value);
            }
        }
        return null;
    }

    private String generateNoDataMessage() {
        return """
            <div class="section">
                <h2>Advanced Scan Results Summary</h2>
                <div class="no-data-message">
                    <p>No scan data available. Run the advanced scanners to detect javax usage patterns.</p>
                </div>
            </div>
            """;
    }

    private String generateNoIssuesMessage() {
        return """
            <div class="section">
                <h2>Advanced Scan Results Summary</h2>
                <div class="no-data-message">
                    <p>No javax usage detected. The project appears to be Jakarta EE compatible.</p>
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
        return 35; // After MetricsSummarySnippet (30), before RiskDialsSnippet (40)
    }
}
