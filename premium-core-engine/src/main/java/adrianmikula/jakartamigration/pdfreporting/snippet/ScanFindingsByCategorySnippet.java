package adrianmikula.jakartamigration.pdfreporting.snippet;

import adrianmikula.jakartamigration.advancedscanning.domain.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * HTML snippet displaying scan findings organized by category.
 * Displays actual data from ComprehensiveScanResults only.
 *
 * References: docs/spec/html-refactoring-report-requirements.md Section 4
 */
public class ScanFindingsByCategorySnippet extends BaseHtmlSnippet {

    private final ComprehensiveScanResults scanResults;

    public ScanFindingsByCategorySnippet(ComprehensiveScanResults scanResults) {
        this.scanResults = scanResults;
    }

    @Override
    public String getSnippetName() {
        return "Scan Findings by Category";
    }

    @Override
    public String generate() throws SnippetGenerationException {
        if (scanResults == null) {
            return generateNoDataMessage();
        }

        StringBuilder html = new StringBuilder();
        html.append("""
            <div class="section">
                <h2>Scan Findings by Category</h2>
                <p>Detailed findings from code analysis, grouped by API category.</p>
                
                <div class="findings-by-category">
            """);

        // JPA Findings
        if (scanResults.jpaResults() != null && !scanResults.jpaResults().isEmpty()) {
            html.append(generateJpaSection(scanResults.jpaResults()));
        }

        // CDI/Bean Validation Findings
        if (scanResults.beanValidationResults() != null && !scanResults.beanValidationResults().isEmpty()) {
            html.append(generateCdiSection(scanResults.beanValidationResults()));
        }

        // Servlet/JSP Findings
        if (scanResults.servletJspResults() != null && !scanResults.servletJspResults().isEmpty()) {
            html.append(generateServletSection(scanResults.servletJspResults()));
        }

        // Build Config Findings
        if (scanResults.buildConfigResults() != null && !scanResults.buildConfigResults().isEmpty()) {
            html.append(generateBuildConfigSection(scanResults.buildConfigResults()));
        }

        // Third Party Library Findings
        if (scanResults.thirdPartyLibResults() != null && !scanResults.thirdPartyLibResults().isEmpty()) {
            html.append(generateThirdPartySection(scanResults.thirdPartyLibResults()));
        }

        html.append("""
                </div>
            </div>
            """);

        return html.toString();
    }

    private String generateJpaSection(Map<String, Object> jpaResults) {
        JpaProjectScanResult jpaResult = extractResult(jpaResults, JpaProjectScanResult.class);
        if (jpaResult == null || !jpaResult.hasJavaxUsage()) {
            return "";
        }

        StringBuilder rows = new StringBuilder();
        for (JpaScanResult fileResult : jpaResult.fileResults()) {
            if (!fileResult.hasJavaxUsage()) continue;

            String filePath = escapeHtml(fileResult.filePath().toString());

            for (JpaAnnotationUsage usage : fileResult.annotations()) {
                String jakartaEquivalent = usage.hasJakartaEquivalent()
                    ? escapeHtml(usage.jakartaEquivalent())
                    : "-";

                rows.append(String.format("""
                        <tr>
                            <td>%s</td>
                            <td>%d</td>
                            <td>%s</td>
                            <td>%s</td>
                        </tr>
                    """,
                    filePath,
                    usage.lineNumber(),
                    escapeHtml(usage.annotationName()),
                    jakartaEquivalent
                ));
            }
        }

        if (rows.isEmpty()) {
            return "";
        }

        return String.format("""
                <div class="category-section">
                    <h3>JPA Findings</h3>
                    <table class="findings-table">
                        <thead>
                            <tr>
                                <th>File</th>
                                <th>Line</th>
                                <th>Javax Annotation</th>
                                <th>Jakarta Equivalent</th>
                            </tr>
                        </thead>
                        <tbody>
                            %s
                        </tbody>
                    </table>
                </div>
            """, rows);
    }

    private String generateCdiSection(Map<String, Object> cdiResults) {
        CdiInjectionProjectScanResult cdiResult = extractResult(cdiResults, CdiInjectionProjectScanResult.class);
        if (cdiResult == null || !cdiResult.hasJavaxUsage()) {
            return "";
        }

        StringBuilder rows = new StringBuilder();
        for (CdiInjectionScanResult fileResult : cdiResult.fileResults()) {
            if (!fileResult.hasJavaxUsage()) continue;

            String filePath = escapeHtml(fileResult.filePath().toString());

            for (CdiInjectionUsage usage : fileResult.usages()) {
                String jakartaEquivalent = usage.hasJakartaEquivalent()
                    ? escapeHtml(usage.jakartaEquivalent())
                    : "-";

                rows.append(String.format("""
                        <tr>
                            <td>%s</td>
                            <td>%d</td>
                            <td>%s</td>
                            <td>%s</td>
                            <td>%s</td>
                        </tr>
                    """,
                    filePath,
                    usage.lineNumber(),
                    escapeHtml(usage.className()),
                    jakartaEquivalent,
                    escapeHtml(usage.usageType())
                ));
            }
        }

        if (rows.isEmpty()) {
            return "";
        }

        return String.format("""
                <div class="category-section">
                    <h3>CDI/Injection Findings</h3>
                    <table class="findings-table">
                        <thead>
                            <tr>
                                <th>File</th>
                                <th>Line</th>
                                <th>Javax Class</th>
                                <th>Jakarta Equivalent</th>
                                <th>Usage Type</th>
                            </tr>
                        </thead>
                        <tbody>
                            %s
                        </tbody>
                    </table>
                </div>
            """, rows);
    }

    private String generateServletSection(Map<String, Object> servletResults) {
        ServletJspProjectScanResult servletResult = extractResult(servletResults, ServletJspProjectScanResult.class);
        if (servletResult == null || !servletResult.hasJavaxUsage()) {
            return "";
        }

        StringBuilder rows = new StringBuilder();
        for (ServletJspScanResult fileResult : servletResult.fileResults()) {
            if (!fileResult.hasJavaxUsage()) continue;

            String filePath = escapeHtml(fileResult.filePath().toString());

            for (ServletJspUsage usage : fileResult.usages()) {
                String jakartaEquivalent = usage.hasJakartaEquivalent()
                    ? escapeHtml(usage.jakartaEquivalent())
                    : "-";

                rows.append(String.format("""
                        <tr>
                            <td>%s</td>
                            <td>%d</td>
                            <td>%s</td>
                            <td>%s</td>
                            <td>%s</td>
                        </tr>
                    """,
                    filePath,
                    usage.lineNumber(),
                    escapeHtml(usage.className()),
                    jakartaEquivalent,
                    escapeHtml(usage.usageType())
                ));
            }
        }

        if (rows.isEmpty()) {
            return "";
        }

        return String.format("""
                <div class="category-section">
                    <h3>Servlet/JSP Findings</h3>
                    <table class="findings-table">
                        <thead>
                            <tr>
                                <th>File</th>
                                <th>Line</th>
                                <th>Javax Class</th>
                                <th>Jakarta Equivalent</th>
                                <th>Usage Type</th>
                            </tr>
                        </thead>
                        <tbody>
                            %s
                        </tbody>
                    </table>
                </div>
            """, rows);
    }

    private String generateBuildConfigSection(Map<String, Object> buildResults) {
        BuildConfigProjectScanResult buildResult = extractResult(buildResults, BuildConfigProjectScanResult.class);
        if (buildResult == null || !buildResult.hasJavaxDependencies()) {
            return "";
        }

        StringBuilder rows = new StringBuilder();
        for (BuildConfigScanResult fileResult : buildResult.fileResults()) {
            if (!fileResult.hasJavaxDependencies()) continue;

            String filePath = escapeHtml(fileResult.filePath().toString());

            for (BuildConfigUsage usage : fileResult.usages()) {
                String dependencyName = escapeHtml(usage.groupId() + ":" + usage.artifactId());
                String jakartaEquivalent = usage.hasJakartaEquivalent()
                    ? escapeHtml(usage.jakartaGroupId() + ":" + usage.jakartaArtifactId())
                    : "-";

                rows.append(String.format("""
                        <tr>
                            <td>%s</td>
                            <td>%s</td>
                            <td>%s</td>
                        </tr>
                    """,
                    filePath,
                    dependencyName,
                    jakartaEquivalent
                ));
            }
        }

        if (rows.isEmpty()) {
            return "";
        }

        return String.format("""
                <div class="category-section">
                    <h3>Build Configuration Findings</h3>
                    <table class="findings-table">
                        <thead>
                            <tr>
                                <th>File</th>
                                <th>Dependency</th>
                                <th>Jakarta Equivalent</th>
                            </tr>
                        </thead>
                        <tbody>
                            %s
                        </tbody>
                    </table>
                </div>
            """, rows);
    }

    private String generateThirdPartySection(Map<String, Object> thirdPartyResults) {
        // Third party results structure may vary, display generic table
        if (thirdPartyResults.isEmpty()) {
            return "";
        }

        StringBuilder content = new StringBuilder();
        content.append("""
                <div class="category-section">
                    <h3>Third-Party Library Findings</h3>
                    <table class="findings-table">
                        <thead>
                            <tr>
                                <th>Library</th>
                                <th>Details</th>
                            </tr>
                        </thead>
                        <tbody>
            """);

        for (Map.Entry<String, Object> entry : thirdPartyResults.entrySet()) {
            String key = escapeHtml(entry.getKey());
            String value = entry.getValue() != null ? escapeHtml(entry.getValue().toString()) : "-";
            content.append(String.format("""
                            <tr>
                                <td>%s</td>
                                <td>%s</td>
                            </tr>
            """, key, value));
        }

        content.append("""
                        </tbody>
                    </table>
                </div>
            """);

        return content.toString();
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
                <h2>Scan Findings by Category</h2>
                <div class="no-data-message">
                    <p>No scan findings available. Run the code scanners to detect javax usage patterns.</p>
                </div>
            </div>
            """;
    }

    @Override
    public boolean isApplicable() {
        return scanResults != null && hasAnyResults();
    }

    private boolean hasAnyResults() {
        return (scanResults.jpaResults() != null && !scanResults.jpaResults().isEmpty()) ||
               (scanResults.beanValidationResults() != null && !scanResults.beanValidationResults().isEmpty()) ||
               (scanResults.servletJspResults() != null && !scanResults.servletJspResults().isEmpty()) ||
               (scanResults.buildConfigResults() != null && !scanResults.buildConfigResults().isEmpty()) ||
               (scanResults.thirdPartyLibResults() != null && !scanResults.thirdPartyLibResults().isEmpty());
    }

    @Override
    public int getOrder() {
        return 40; // After recipe recommendations
    }
}
