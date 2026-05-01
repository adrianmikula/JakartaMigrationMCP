package adrianmikula.jakartamigration.pdfreporting.snippet;

import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;
import adrianmikula.jakartamigration.advancedscanning.domain.ThirdPartyLibProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.ThirdPartyLibUsage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Shows build file (pom.xml/build.gradle) references for library dependencies.
 * Helps developers locate and update dependency declarations.
 */
public class BuildFileReferenceSnippet extends BaseHtmlSnippet {

    private final ComprehensiveScanResults scanResults;

    public BuildFileReferenceSnippet(ComprehensiveScanResults scanResults) {
        this.scanResults = scanResults;
    }

    @Override
    public String generate() throws SnippetGenerationException {
        if (scanResults == null || scanResults.thirdPartyLibResults() == null) {
            return generateNoDataMessage();
        }

        ThirdPartyLibProjectScanResult libResult = extractLibResult();
        if (libResult == null || !libResult.hasFindings()) {
            return generateNoLibrariesMessage();
        }

        // Group libraries by build file
        Map<String, List<LibDependency>> buildFileGroups = groupByBuildFile(libResult);

        if (buildFileGroups.isEmpty()) {
            return generateGenericLibraryList(libResult);
        }

        return safelyFormat("""
            <div class="section build-file-references">
                <h2>Build File Dependency Updates</h2>
                <p>Exact dependency declarations to update in your build files.</p>

                <div class="build-file-summary">
                    <span class="summary-stat">%d Libraries to Update</span>
                    <span class="summary-stat">%d Build Files Affected</span>
                </div>

                %s

                <div class="build-file-instructions">
                    <h4>Update Instructions</h4>
                    <ol>
                        <li>Locate the dependency declaration in your build file</li>
                        <li>Update the version or replace with the suggested artifact</li>
                        <li>Save and refresh the project in your IDE</li>
                        <li>Run <code>mvn clean compile</code> or <code>gradle build</code> to verify</li>
                    </ol>
                </div>
            </div>
            """,
            libResult.getTotalLibraries(),
            buildFileGroups.size(),
            generateBuildFileSections(buildFileGroups)
        );
    }

    private Map<String, List<LibDependency>> groupByBuildFile(ThirdPartyLibProjectScanResult libResult) {
        List<LibDependency> deps = new ArrayList<>();

        for (ThirdPartyLibUsage lib : libResult.getLibraries()) {
            // Try to determine build file from coordinates or project structure
            String buildFile = determineBuildFile(lib);

            deps.add(new LibDependency(
                buildFile,
                lib.getGroupId(),
                lib.getArtifactId(),
                lib.getCurrentVersion(),
                lib.getSuggestedReplacement(),
                lib.getIssueType(),
                lib.getComplexity().toString(),
                generateMavenDependency(lib),
                generateGradleDependency(lib),
                generateSuggestedMavenDependency(lib),
                generateSuggestedGradleDependency(lib)
            ));
        }

        return deps.stream().collect(Collectors.groupingBy(LibDependency::buildFile));
    }

    private String determineBuildFile(ThirdPartyLibUsage lib) {
        // Look for common build file locations
        String projectPath = lib.getCoordinates().split(":")[0];
        if (projectPath.contains("pom.xml")) {
            return "pom.xml";
        }
        if (projectPath.contains("build.gradle")) {
            return "build.gradle";
        }
        // Default assumption based on typical structure
        return "pom.xml"; // Default to Maven
    }

    private String generateMavenDependency(ThirdPartyLibUsage lib) {
        return String.format("""
            <dependency>
                <groupId>%s</groupId>
                <artifactId>%s</artifactId>
                <version>%s</version>
            </dependency>
            """,
            lib.getGroupId(),
            lib.getArtifactId(),
            lib.getCurrentVersion()
        ).trim();
    }

    private String generateGradleDependency(ThirdPartyLibUsage lib) {
        return String.format("implementation '%s:%s:%s'",
            lib.getGroupId(),
            lib.getArtifactId(),
            lib.getCurrentVersion()
        );
    }

    private String generateSuggestedMavenDependency(ThirdPartyLibUsage lib) {
        String replacement = lib.getSuggestedReplacement();
        if (replacement == null || replacement.isBlank()) {
            return "<!-- Manual review required - no automatic replacement available -->";
        }

        // Parse suggested replacement coordinates
        String[] parts = replacement.split(":");
        if (parts.length >= 3) {
            return String.format("""
                <dependency>
                    <groupId>%s</groupId>
                    <artifactId>%s</artifactId>
                    <version>%s</version>
                </dependency>
                """,
                parts[0], parts[1], parts[2]
            ).trim();
        }

        return "<!-- Replace with: " + replacement + " -->";
    }

    private String generateSuggestedGradleDependency(ThirdPartyLibUsage lib) {
        String replacement = lib.getSuggestedReplacement();
        if (replacement == null || replacement.isBlank()) {
            return "// Manual review required - no automatic replacement available";
        }
        return String.format("implementation '%s'", replacement);
    }

    private String generateBuildFileSections(Map<String, List<LibDependency>> buildFileGroups) {
        StringBuilder sections = new StringBuilder();

        buildFileGroups.forEach((buildFile, deps) -> {
            sections.append(generateBuildFileSection(buildFile, deps));
        });

        return sections.toString();
    }

    private String generateBuildFileSection(String buildFile, List<LibDependency> deps) {
        boolean isMaven = buildFile.endsWith(".xml") || buildFile.endsWith("pom.xml");

        StringBuilder section = new StringBuilder();
        section.append(String.format("""
            <div class="build-file-section">
                <h3>📁 %s</h3>
                <div class="dependency-count">%d dependencies to update</div>
                """,
            escapeHtml(buildFile),
            deps.size()
        ));

        for (LibDependency dep : deps) {
            section.append(generateDependencyCard(dep, isMaven));
        }

        section.append("</div>");
        return section.toString();
    }

    private String generateDependencyCard(LibDependency dep, boolean isMaven) {
        String currentCode = isMaven ? dep.mavenDeclaration() : dep.gradleDeclaration();
        String suggestedCode = isMaven ? dep.suggestedMavenDeclaration() : dep.suggestedGradleDeclaration();

        String complexityClass = dep.complexity().toLowerCase();

        return String.format("""
            <div class="dependency-card complexity-%s">
                <div class="dep-header">
                    <code class="dep-coordinates">%s:%s</code>
                    <span class="complexity-badge %s">%s</span>
                    <span class="issue-badge %s">%s</span>
                </div>
                <div class="version-change">
                    <span class="version-current">%s</span>
                    <span class="version-arrow">→</span>
                    <span class="version-suggested">%s</span>
                </div>
                <div class="code-comparison build-file-comparison">
                    <div class="code-before">
                        <div class="code-label">Current</div>
                        <pre><code>%s</code></pre>
                    </div>
                    <div class="code-after">
                        <div class="code-label">Suggested</div>
                        <pre><code>%s</code></pre>
                    </div>
                </div>
            </div>
            """,
            complexityClass,
            escapeHtml(dep.groupId()),
            escapeHtml(dep.artifactId()),
            complexityClass,
            dep.complexity(),
            dep.issueType().replace("-", ""),
            dep.issueType(),
            escapeHtml(dep.currentVersion()),
            escapeHtml(dep.suggestedReplacement() != null ? dep.suggestedReplacement() : "Manual review"),
            escapeHtml(currentCode),
            escapeHtml(suggestedCode)
        );
    }

    private String generateGenericLibraryList(ThirdPartyLibProjectScanResult libResult) {
        StringBuilder list = new StringBuilder();
        list.append("""
            <div class="section build-file-references">
                <h2>Build File Dependency Updates</h2>
                <p>Libraries requiring updates (build file locations unknown):</p>
                <div class="library-list">
            """);

        for (ThirdPartyLibUsage lib : libResult.getLibraries()) {
            list.append(String.format("""
                <div class="library-item">
                    <code>%s</code>
                    <span class="version">%s</span>
                    <span class="complexity-badge %s">%s</span>
                </div>
                """,
                escapeHtml(lib.getCoordinates()),
                escapeHtml(lib.getCurrentVersion()),
                lib.getComplexity().toString().toLowerCase(),
                lib.getComplexity()
            ));
        }

        list.append("""
                </div>
            </div>
            """);

        return list.toString();
    }

    private ThirdPartyLibProjectScanResult extractLibResult() {
        Map<String, Object> libMap = scanResults.thirdPartyLibResults();
        if (libMap != null) {
            for (Object value : libMap.values()) {
                if (value instanceof ThirdPartyLibProjectScanResult result) {
                    return result;
                }
            }
        }
        return null;
    }

    private String generateNoDataMessage() {
        return """
            <div class="section build-file-references">
                <h2>Build File Dependency Updates</h2>
                <div class="no-data-message">
                    <p>No library scan data available. Run third-party library scanner to analyze dependencies.</p>
                </div>
            </div>
            """;
    }

    private String generateNoLibrariesMessage() {
        return """
            <div class="section build-file-references">
                <h2>Build File Dependency Updates</h2>
                <div class="success-message">
                    <p>No third-party library updates required. All dependencies appear compatible with Jakarta EE.</p>
                </div>
            </div>
            """;
    }

    @Override
    public boolean isApplicable() {
        return scanResults != null && scanResults.thirdPartyLibResults() != null;
    }

    @Override
    public int getOrder() {
        return 62; // After library migration, before recipe recommendations
    }

    /**
     * Record for library dependency data
     */
    private record LibDependency(
        String buildFile,
        String groupId,
        String artifactId,
        String currentVersion,
        String suggestedReplacement,
        String issueType,
        String complexity,
        String mavenDeclaration,
        String gradleDeclaration,
        String suggestedMavenDeclaration,
        String suggestedGradleDeclaration
    ) {}
}
