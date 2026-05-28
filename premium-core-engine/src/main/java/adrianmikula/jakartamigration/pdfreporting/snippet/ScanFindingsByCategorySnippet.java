package adrianmikula.jakartamigration.pdfreporting.snippet;

import adrianmikula.jakartamigration.advancedscanning.domain.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * HTML snippet displaying scan findings grouped by package (directory) then by file.
 * Displays actual data from ComprehensiveScanResults only.
 * Each finding retains its scanner category badge.
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
        return "Scan Findings by File and Package";
    }

    @Override
    public String generate() throws SnippetGenerationException {
        if (scanResults == null) {
            return generateNoDataMessage();
        }

        List<Finding> allFindings = extractAllFindings();
        if (allFindings.isEmpty()) {
            return generateNoDataMessage();
        }

        StringBuilder html = new StringBuilder();
        html.append("""
            <div class="section">
                <h2>Scan Findings by File and Package</h2>
                <p>Detailed findings from code analysis, grouped by source file and package directory.</p>

                <div class="findings-by-file">
            """);

        // Group by package path (sorted), then by file name (sorted)
        Map<String, Map<String, List<Finding>>> grouped = allFindings.stream()
            .collect(Collectors.groupingBy(
                Finding::packagePath,
                TreeMap::new,
                Collectors.groupingBy(
                    Finding::fileName,
                    TreeMap::new,
                    Collectors.toList()
                )
            ));

        for (Map.Entry<String, Map<String, List<Finding>>> packageEntry : grouped.entrySet()) {
            String packagePath = packageEntry.getKey();
            Map<String, List<Finding>> filesInPackage = packageEntry.getValue();

            html.append("<div class=\"package-section\">\n");
            html.append("    <h3 class=\"package-heading\">").append(escapeHtml(packagePath)).append("</h3>\n");

            for (Map.Entry<String, List<Finding>> fileEntry : filesInPackage.entrySet()) {
                String fileName = fileEntry.getKey();
                List<Finding> fileFindings = fileEntry.getValue();

                html.append("    <div class=\"file-section\">\n");
                html.append("        <h4 class=\"file-heading\">").append(escapeHtml(fileName)).append("</h4>\n");
                html.append("        <table class=\"findings-table\">\n");
                html.append("            <thead>\n");
                html.append("                <tr>\n");
                html.append("                    <th>Line</th>\n");
                html.append("                    <th>Category</th>\n");
                html.append("                    <th>Javax Reference</th>\n");
                html.append("                    <th>Jakarta Equivalent</th>\n");
                html.append("                </tr>\n");
                html.append("            </thead>\n");
                html.append("            <tbody>\n");

                for (Finding finding : fileFindings) {
                    html.append("                <tr>\n");
                    html.append("                    <td>").append(finding.lineNumber()).append("</td>\n");
                    html.append("                    <td>").append(generateCategoryBadge(finding.category())).append("</td>\n");
                    html.append("                    <td>").append(escapeHtml(finding.javaxReference())).append("</td>\n");
                    html.append("                    <td>").append(escapeHtml(finding.jakartaEquivalent() != null ? finding.jakartaEquivalent() : "-")).append("</td>\n");
                    html.append("                </tr>\n");
                }

                html.append("            </tbody>\n");
                html.append("        </table>\n");
                html.append("    </div>\n");
            }

            html.append("</div>\n");
        }

        html.append("""
                </div>
            </div>
            """);

        return html.toString();
    }

    private String generateCategoryBadge(String category) {
        String cssClass = switch (category.toLowerCase()) {
            case "jpa", "bean validation" -> "category-jpa";
            case "cdi" -> "category-cdi";
            case "servlet" -> "category-servlet";
            case "build config" -> "category-build";
            case "third party" -> "category-database";
            default -> "category-web";
        };
        return String.format("<span class=\"category-badge %s\">%s</span>", cssClass, escapeHtml(category));
    }

    private List<Finding> extractAllFindings() {
        List<Finding> findings = new ArrayList<>();

        if (scanResults.jpaResults() != null && !scanResults.jpaResults().isEmpty()) {
            findings.addAll(extractJpaFindings(scanResults.jpaResults()));
        }

        if (scanResults.beanValidationResults() != null && !scanResults.beanValidationResults().isEmpty()) {
            findings.addAll(extractBeanValidationFindings(scanResults.beanValidationResults()));
        }

        if (scanResults.cdiResults() != null && !scanResults.cdiResults().isEmpty()) {
            findings.addAll(extractCdiFindings(scanResults.cdiResults()));
        }

        if (scanResults.servletJspResults() != null && !scanResults.servletJspResults().isEmpty()) {
            findings.addAll(extractServletFindings(scanResults.servletJspResults()));
        }

        if (scanResults.buildConfigResults() != null && !scanResults.buildConfigResults().isEmpty()) {
            findings.addAll(extractBuildConfigFindings(scanResults.buildConfigResults()));
        }

        if (scanResults.thirdPartyLibResults() != null && !scanResults.thirdPartyLibResults().isEmpty()) {
            findings.addAll(extractThirdPartyFindings(scanResults.thirdPartyLibResults()));
        }

        return findings;
    }

    private List<Finding> extractJpaFindings(Map<String, Object> jpaResults) {
        List<Finding> findings = new ArrayList<>();
        JpaProjectScanResult result = extractResult(jpaResults, JpaProjectScanResult.class);
        if (result == null || !result.hasJavaxUsage()) {
            return findings;
        }
        for (JpaScanResult fileResult : result.fileResults()) {
            if (!fileResult.hasJavaxUsage()) continue;
            for (JpaAnnotationUsage usage : fileResult.annotations()) {
                findings.add(new Finding(
                    fileResult.filePath(),
                    fileResult.filePath().getFileName().toString(),
                    packagePathOf(fileResult.filePath()),
                    usage.lineNumber(),
                    "JPA",
                    usage.annotationName(),
                    usage.hasJakartaEquivalent() ? usage.jakartaEquivalent() : null
                ));
            }
        }
        return findings;
    }

    private List<Finding> extractBeanValidationFindings(Map<String, Object> beanValidationResults) {
        List<Finding> findings = new ArrayList<>();
        BeanValidationProjectScanResult result = extractResult(beanValidationResults, BeanValidationProjectScanResult.class);
        if (result == null || !result.hasJavaxUsage()) {
            return findings;
        }
        for (BeanValidationScanResult fileResult : result.fileResults()) {
            if (!fileResult.hasJavaxUsage()) continue;
            for (BeanValidationUsage usage : fileResult.annotations()) {
                findings.add(new Finding(
                    fileResult.filePath(),
                    fileResult.filePath().getFileName().toString(),
                    packagePathOf(fileResult.filePath()),
                    usage.lineNumber(),
                    "Bean Validation",
                    usage.annotationName(),
                    usage.hasJakartaEquivalent() ? usage.jakartaEquivalent() : null
                ));
            }
        }
        return findings;
    }

    private List<Finding> extractCdiFindings(Map<String, Object> cdiResults) {
        List<Finding> findings = new ArrayList<>();
        CdiInjectionProjectScanResult result = extractResult(cdiResults, CdiInjectionProjectScanResult.class);
        if (result == null || !result.hasJavaxUsage()) {
            return findings;
        }
        for (CdiInjectionScanResult fileResult : result.fileResults()) {
            if (!fileResult.hasJavaxUsage()) continue;
            for (CdiInjectionUsage usage : fileResult.usages()) {
                findings.add(new Finding(
                    fileResult.filePath(),
                    fileResult.filePath().getFileName().toString(),
                    packagePathOf(fileResult.filePath()),
                    usage.lineNumber(),
                    "CDI",
                    usage.className(),
                    usage.hasJakartaEquivalent() ? usage.jakartaEquivalent() : null
                ));
            }
        }
        return findings;
    }

    private List<Finding> extractServletFindings(Map<String, Object> servletResults) {
        List<Finding> findings = new ArrayList<>();
        ServletJspProjectScanResult result = extractResult(servletResults, ServletJspProjectScanResult.class);
        if (result == null || !result.hasJavaxUsage()) {
            return findings;
        }
        for (ServletJspScanResult fileResult : result.fileResults()) {
            if (!fileResult.hasJavaxUsage()) continue;
            for (ServletJspUsage usage : fileResult.usages()) {
                findings.add(new Finding(
                    fileResult.filePath(),
                    fileResult.filePath().getFileName().toString(),
                    packagePathOf(fileResult.filePath()),
                    usage.lineNumber(),
                    "Servlet",
                    usage.className(),
                    usage.hasJakartaEquivalent() ? usage.jakartaEquivalent() : null
                ));
            }
        }
        return findings;
    }

    private List<Finding> extractBuildConfigFindings(Map<String, Object> buildResults) {
        List<Finding> findings = new ArrayList<>();
        BuildConfigProjectScanResult result = extractResult(buildResults, BuildConfigProjectScanResult.class);
        if (result == null || !result.hasJavaxDependencies()) {
            return findings;
        }
        for (BuildConfigScanResult fileResult : result.fileResults()) {
            if (!fileResult.hasJavaxDependencies()) continue;
            for (BuildConfigUsage usage : fileResult.usages()) {
                String javaxRef = usage.groupId() + ":" + usage.artifactId();
                String jakartaEq = usage.hasJakartaEquivalent()
                    ? usage.jakartaGroupId() + ":" + usage.jakartaArtifactId()
                    : null;
                findings.add(new Finding(
                    fileResult.filePath(),
                    fileResult.filePath().getFileName().toString(),
                    packagePathOf(fileResult.filePath()),
                    usage.lineNumber(),
                    "Build Config",
                    javaxRef,
                    jakartaEq
                ));
            }
        }
        return findings;
    }

    private List<Finding> extractThirdPartyFindings(Map<String, Object> thirdPartyResults) {
        List<Finding> findings = new ArrayList<>();
        if (thirdPartyResults.isEmpty()) {
            return findings;
        }
        ThirdPartyLibProjectScanResult result = extractResult(thirdPartyResults, ThirdPartyLibProjectScanResult.class);
        if (result != null && result.hasFindings()) {
            String pkg = result.getProjectPath() != null ? result.getProjectPath() : "";
            String file = result.getBuildFile() != null && !result.getBuildFile().isEmpty()
                ? result.getBuildFile()
                : "third-party-dependencies";
            for (ThirdPartyLibUsage usage : result.getLibraries()) {
                String javaxRef = usage.getLibraryName() != null ? usage.getLibraryName()
                    : usage.getGroupId() + ":" + usage.getArtifactId();
                String jakartaEq = usage.getSuggestedReplacement() != null && !usage.getSuggestedReplacement().isEmpty()
                    ? usage.getSuggestedReplacement()
                    : null;
                findings.add(new Finding(
                    Path.of(pkg, file),
                    file,
                    pkg,
                    0,
                    "Third Party",
                    javaxRef,
                    jakartaEq
                ));
            }
        } else {
            // Fallback for generic map-based third party results
            for (Map.Entry<String, Object> entry : thirdPartyResults.entrySet()) {
                findings.add(new Finding(
                    Path.of("unknown"),
                    "unknown",
                    "",
                    0,
                    "Third Party",
                    entry.getKey(),
                    entry.getValue() != null ? entry.getValue().toString() : null
                ));
            }
        }
        return findings;
    }

    private String packagePathOf(Path filePath) {
        Path parent = filePath.getParent();
        return parent != null ? parent.toString() : "";
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
                <h2>Scan Findings by File and Package</h2>
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
               (scanResults.cdiResults() != null && !scanResults.cdiResults().isEmpty()) ||
               (scanResults.servletJspResults() != null && !scanResults.servletJspResults().isEmpty()) ||
               (scanResults.buildConfigResults() != null && !scanResults.buildConfigResults().isEmpty()) ||
               (scanResults.thirdPartyLibResults() != null && !scanResults.thirdPartyLibResults().isEmpty());
    }

    @Override
    public int getOrder() {
        return 40; // After recipe recommendations
    }

    /**
     * Normalized finding record for cross-category grouping.
     */
    private record Finding(
        Path filePath,
        String fileName,
        String packagePath,
        int lineNumber,
        String category,
        String javaxReference,
        String jakartaEquivalent
    ) {}
}
