package adrianmikula.jakartamigration.pdfreporting.snippet;

import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;
import adrianmikula.jakartamigration.advancedscanning.domain.DockerCicdUsage;

import java.util.Map;

/**
 * Displays Docker and CI/CD configuration findings.
 * Shows Java references found in Dockerfiles and CI/CD configuration files.
 */
public class DockerCicdFindingsSnippet extends BaseHtmlSnippet {

    private final ComprehensiveScanResults scanResults;

    public DockerCicdFindingsSnippet(ComprehensiveScanResults scanResults) {
        this.scanResults = scanResults;
    }

    @Override
    public String generate() throws SnippetGenerationException {
        if (scanResults == null) {
            return generateNoDataMessage();
        }

        java.util.List<DockerCicdUsage> usages = extractUsages();
        if (usages == null || usages.isEmpty()) {
            return generateNoDockerCicdMessage();
        }

        int dockerfileCount = countByFileType(usages, DockerCicdUsage.DockerCicdFileType.DOCKERFILE);
        int cicdCount = usages.size() - dockerfileCount;

        return safelyFormat("""
            <div class="section docker-cicd-findings">
                <h2>Docker & CI/CD Configuration</h2>
                <p>Java references found in Docker and CI/CD configuration files.</p>

                <div class="summary-stats">
                    <div class="stat">
                        <span class="stat-value">%d</span>
                        <span class="stat-label">Docker References</span>
                    </div>
                    <div class="stat">
                        <span class="stat-value">%d</span>
                        <span class="stat-label">CI/CD References</span>
                    </div>
                    <div class="stat">
                        <span class="stat-value">%d</span>
                        <span class="stat-label">Total References</span>
                    </div>
                </div>

                <div class="findings-table-container">
                    <table class="findings-table">
                        <thead>
                            <tr>
                                <th>File Type</th>
                                <th>Reference Type</th>
                                <th>Java Version</th>
                                <th>Command</th>
                                <th>Migration Note</th>
                            </tr>
                        </thead>
                        <tbody>
                            %s
                        </tbody>
                    </table>
                </div>
            </div>
            """,
            dockerfileCount,
            cicdCount,
            usages.size(),
            generateFindingRows(usages)
        );
    }

    @SuppressWarnings("unchecked")
    private java.util.List<DockerCicdUsage> extractUsages() {
        // Docker/CI-CD results may be stored in various maps as lists
        Map<String, Object>[] mapsToCheck = new Map[]{
            scanResults.buildConfigResults(),
            scanResults.thirdPartyLibResults()
        };

        java.util.List<DockerCicdUsage> allUsages = new java.util.ArrayList<>();

        for (Map<String, Object> map : mapsToCheck) {
            if (map == null) continue;
            for (Object value : map.values()) {
                if (value instanceof DockerCicdUsage) {
                    allUsages.add((DockerCicdUsage) value);
                } else if (value instanceof java.util.List) {
                    java.util.List<?> list = (java.util.List<?>) value;
                    for (Object item : list) {
                        if (item instanceof DockerCicdUsage) {
                            allUsages.add((DockerCicdUsage) item);
                        }
                    }
                }
            }
        }

        return allUsages.isEmpty() ? null : allUsages;
    }

    private int countByFileType(java.util.List<DockerCicdUsage> usages, DockerCicdUsage.DockerCicdFileType type) {
        return (int) usages.stream().filter(u -> u.fileType() == type).count();
    }

    private String generateFindingRows(java.util.List<DockerCicdUsage> usages) {
        StringBuilder rows = new StringBuilder();
        int count = 0;
        final int MAX_ROWS = 50;

        for (DockerCicdUsage usage : usages) {
            if (count >= MAX_ROWS) break;

            String javaVersion = usage.hasJavaVersion() ? usage.javaVersion() : "N/A";
            String migrationNote = usage.hasJakartaMigrationImplications()
                ? usage.jakartaMigrationNote()
                : "None";

            rows.append(String.format("""
                <tr>
                    <td>%s</td>
                    <td>%s</td>
                    <td>%s</td>
                    <td>%s</td>
                    <td>%s</td>
                </tr>
                """,
                escapeHtml(usage.fileType().getDescription()),
                escapeHtml(usage.referenceType().getDescription()),
                escapeHtml(javaVersion),
                escapeHtml(truncate(usage.command(), 50)),
                escapeHtml(migrationNote)
            ));
            count++;
        }

        if (count >= MAX_ROWS && usages.size() > MAX_ROWS) {
            rows.append(String.format("""
                <tr class=\"more-rows\">
                    <td colspan=\"5\">... and %d more references</td>
                </tr>
                """,
                usages.size() - MAX_ROWS
            ));
        }

        return rows.toString();
    }

    private String truncate(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }

    private String generateNoDataMessage() {
        return """
            <div class="section docker-cicd-findings">
                <h2>Docker & CI/CD Configuration</h2>
                <div class="no-data-message">
                    <p>No Docker or CI/CD scan data available.</p>
                </div>
            </div>
            """;
    }

    private String generateNoDockerCicdMessage() {
        return """
            <div class="section docker-cicd-findings">
                <h2>Docker & CI/CD Configuration</h2>
                <div class="success-message">
                    <p>No Docker or CI/CD configuration files found. This project does not use containerization or automated CI/CD.</p>
                </div>
            </div>
            """;
    }

    @Override
    public boolean isApplicable() {
        if (scanResults == null) {
            return false;
        }
        java.util.List<DockerCicdUsage> usages = extractUsages();
        return usages != null && !usages.isEmpty();
    }

    @Override
    public int getOrder() {
        return 56; // After Deprecated API Findings
    }
}
