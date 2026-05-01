package adrianmikula.jakartamigration.pdfreporting.snippet;

import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;
import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyUsage;

import java.util.Map;

/**
 * Displays transitive dependency conflicts from TransitiveDependencyProjectScanResult.
 * Shows javax dependencies and their transitive impact.
 */
public class TransitiveDependencySnippet extends BaseHtmlSnippet {

    private final ComprehensiveScanResults scanResults;

    public TransitiveDependencySnippet(ComprehensiveScanResults scanResults) {
        this.scanResults = scanResults;
    }

    @Override
    public String generate() throws SnippetGenerationException {
        if (scanResults == null || scanResults.transitiveDependencyResults() == null) {
            return generateNoDataMessage();
        }

        TransitiveDependencyProjectScanResult result = extractResult();
        if (result == null || !result.hasJavaxDependencies()) {
            return generateNoDependenciesMessage();
        }

        return safelyFormat("""
            <div class="section transitive-dependencies">
                <h2>Transitive Dependency Analysis</h2>
                <p>javax.* dependencies identified in transitive dependency tree.</p>

                <div class="summary-stats">
                    <div class="stat">
                        <span class="stat-value">%d</span>
                        <span class="stat-label">Build Files Scanned</span>
                    </div>
                    <div class="stat">
                        <span class="stat-value">%d</span>
                        <span class="stat-label">Files with javax Deps</span>
                    </div>
                    <div class="stat">
                        <span class="stat-value">%d</span>
                        <span class="stat-label">Total javax Dependencies</span>
                    </div>
                </div>

                <div class="dependencies-table-container">
                    <table class="dependencies-table">
                        <thead>
                            <tr>
                                <th>Build File</th>
                                <th>Artifact</th>
                                <th>Version</th>
                                <th>javax Package</th>
                                <th>Severity</th>
                                <th>Scope</th>
                            </tr>
                        </thead>
                        <tbody>
                            %s
                        </tbody>
                    </table>
                </div>
            </div>
            """,
            result.getTotalBuildFilesScanned(),
            result.getFilesWithJavaxDependencies(),
            result.getTotalJavaxDependencies(),
            generateDependencyRows(result)
        );
    }

    @SuppressWarnings("unchecked")
    private TransitiveDependencyProjectScanResult extractResult() {
        Map<String, Object> resultsMap = scanResults.transitiveDependencyResults();
        if (resultsMap == null) return null;

        for (Object value : resultsMap.values()) {
            if (value instanceof TransitiveDependencyProjectScanResult) {
                return (TransitiveDependencyProjectScanResult) value;
            }
        }
        return null;
    }

    private String generateDependencyRows(TransitiveDependencyProjectScanResult result) {
        StringBuilder rows = new StringBuilder();
        int count = 0;
        final int MAX_ROWS = 50; // Limit to prevent huge reports

        for (TransitiveDependencyScanResult fileResult : result.getFileResults()) {
            if (fileResult.getUsages() == null) continue;

            String filePath = fileResult.getFilePath() != null
                ? fileResult.getFilePath().getFileName().toString()
                : "Unknown";

            for (TransitiveDependencyUsage usage : fileResult.getUsages()) {
                if (count >= MAX_ROWS) break;

                String artifact = usage.getGroupId() + ":" + usage.getArtifactId();
                String severityClass = getSeverityClass(usage.getSeverity());

                rows.append(String.format("""
                    <tr class="%s">
                        <td>%s</td>
                        <td>%s</td>
                        <td>%s</td>
                        <td>%s</td>
                        <td class=\"severity-cell %s\">%s</td>
                        <td>%s</td>
                    </tr>
                    """,
                    severityClass,
                    escapeHtml(filePath),
                    escapeHtml(artifact),
                    escapeHtml(usage.getVersion()),
                    escapeHtml(usage.getJavaxPackage()),
                    severityClass,
                    escapeHtml(usage.getSeverity()),
                    escapeHtml(usage.getScope() != null ? usage.getScope() : "compile")
                ));
                count++;
            }

            if (count >= MAX_ROWS) break;
        }

        if (count >= MAX_ROWS && result.getTotalJavaxDependencies() > MAX_ROWS) {
            rows.append(String.format("""
                <tr class=\"more-rows\">
                    <td colspan=\"6\">... and %d more dependencies</td>
                </tr>
                """,
                result.getTotalJavaxDependencies() - MAX_ROWS
            ));
        }

        return rows.toString();
    }

    private String getSeverityClass(String severity) {
        return switch (severity != null ? severity.toUpperCase() : "UNKNOWN") {
            case "CRITICAL" -> "critical";
            case "HIGH" -> "high";
            case "MEDIUM" -> "medium";
            case "LOW" -> "low";
            default -> "unknown";
        };
    }

    private String generateNoDataMessage() {
        return """
            <div class="section transitive-dependencies">
                <h2>Transitive Dependency Analysis</h2>
                <div class="no-data-message">
                    <p>No transitive dependency scan data available.</p>
                </div>
            </div>
            """;
    }

    private String generateNoDependenciesMessage() {
        return """
            <div class="section transitive-dependencies">
                <h2>Transitive Dependency Analysis</h2>
                <div class="success-message">
                    <p>No javax.* transitive dependencies found. All dependencies are Jakarta-compatible.</p>
                </div>
            </div>
            """;
    }

    @Override
    public boolean isApplicable() {
        if (scanResults == null || scanResults.transitiveDependencyResults() == null) {
            return false;
        }
        TransitiveDependencyProjectScanResult result = extractResult();
        return result != null && result.hasJavaxDependencies();
    }

    @Override
    public int getOrder() {
        return 41; // After Dependency Matrix (40)
    }
}
