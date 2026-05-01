package adrianmikula.jakartamigration.pdfreporting.snippet;

import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import lombok.extern.slf4j.Slf4j;

/**
 * Dependency matrix snippet showing detailed version compatibility analysis.
 * Provides comprehensive dependency information with compatibility status and recommendations.
 */
@Slf4j
public class DependencyMatrixSnippet extends BaseHtmlSnippet {
    
    private final DependencyGraph dependencyGraph;
    
    public DependencyMatrixSnippet(DependencyGraph dependencyGraph) {
        this.dependencyGraph = dependencyGraph;
    }
    
    @Override
    public String generate() throws SnippetGenerationException {
        if (dependencyGraph == null || dependencyGraph.getNodes().isEmpty()) {
            return generateNoDependenciesMessage();
        }
        
        return safelyFormat("""
            <div class="section">
                <h2>Dependency Compatibility Matrix</h2>
                <p>Detailed analysis of all dependencies and their Jakarta EE compatibility status.</p>
                
                %s
                
                <div class="compatibility-summary">
                    <h3>Compatibility Summary</h3>
                    <div class="metrics-grid">
                        <div class="metric-card compatible">
                            <div class="metric-value">%d</div>
                            <div class="metric-label">Jakarta Compatible</div>
                        </div>
                        <div class="metric-card needs-update">
                            <div class="metric-value">%d</div>
                            <div class="metric-label">Needs Update</div>
                        </div>
                        <div class="metric-card incompatible">
                            <div class="metric-value">%d</div>
                            <div class="metric-label">Incompatible</div>
                        </div>
                        <div class="metric-card unknown">
                            <div class="metric-value">%d</div>
                            <div class="metric-label">Unknown Status</div>
                        </div>
                    </div>
                </div>
                
                %s
            </div>
            """, 
            generateDependencyTable(),
            calculateCompatibleCount(),
            calculateNeedsUpdateCount(),
            calculateIncompatibleCount(),
            calculateUnknownCount(),
            generateCompatibilityNotes()
        );
    }
    
    private String generateDependencyTable() {
        StringBuilder html = new StringBuilder();
        html.append("""
            <div class="dependency-table-container">
                <table class="dependency-matrix-table">
                    <thead>
                        <tr>
                            <th>Dependency</th>
                            <th>Current Version</th>
                            <th>Jakarta Version</th>
                            <th>Compatibility</th>
                            <th>Breaking Changes</th>
                            <th>Migration Effort</th>
                            <th>Recommendation</th>
                        </tr>
                    </thead>
                    <tbody>
            """);
        
        dependencyGraph.getNodes().stream()
            .limit(20) // Limit to first 20 for readability
            .forEach(artifact -> {
                String compatibility = determineCompatibility(artifact);
                String breakingChanges = getBreakingChanges(artifact);
                String effort = getMigrationEffort(artifact);
                String recommendation = getRecommendation(artifact);
                
                html.append(String.format("""
                    <tr class="%s">
                        <td class="dependency-name">%s</td>
                        <td class="current-version">%s</td>
                        <td class="jakarta-version">%s</td>
                        <td class="compatibility %s">%s</td>
                        <td class="breaking-changes">%s</td>
                        <td class="migration-effort">%s</td>
                        <td class="recommendation">%s</td>
                    </tr>
                    """,
                    getRowClass(compatibility),
                    super.escapeHtml(artifact.artifactId()),
                    super.escapeHtml(getCurrentVersion(artifact)),
                    super.escapeHtml(getJakartaVersion(artifact)),
                    compatibility.toLowerCase().replace(" ", "-"),
                    super.escapeHtml(compatibility),
                    super.escapeHtml(breakingChanges),
                    super.escapeHtml(effort),
                    super.escapeHtml(recommendation)
                ));
            });
        
        if (dependencyGraph.getNodes().size() > 20) {
            html.append(String.format("""
                <tr class="more-items">
                    <td colspan="7" class="more-items-text">
                        ... and %d more dependencies (showing first 20)
                    </td>
                </tr>
                """, dependencyGraph.getNodes().size() - 20));
        }
        
        html.append("""
                    </tbody>
                </table>
            </div>
            """);
        
        return html.toString();
    }
    
    private String generateCompatibilityNotes() {
        return """
            <div class="compatibility-notes">
                <h3>Migration Guidelines</h3>
                <div class="guidelines-grid">
                    <div class="guideline-card">
                        <h4>🟢 Jakarta Compatible</h4>
                        <p>Dependencies that already support Jakarta EE namespaces. No migration required but verify version compatibility.</p>
                    </div>
                    <div class="guideline-card">
                        <h4>🟡 Needs Update</h4>
                        <p>Dependencies have Jakarta EE versions available. Update to newer version and update imports.</p>
                    </div>
                    <div class="guideline-card">
                        <h4>🔴 Incompatible</h4>
                        <p>Dependencies without Jakarta EE support. Consider alternatives or create compatibility layer.</p>
                    </div>
                    <div class="guideline-card">
                        <h4>⚪ Unknown Status</h4>
                        <p>Compatibility status not determined. Manual verification required before migration.</p>
                    </div>
                </div>
            </div>
            """;
    }
    
    private String generateNoDependenciesMessage() {
        return """
            <div class="section">
                <h2>Dependency Compatibility Matrix</h2>
                <div class="warning-box">
                    <h3>⚠️ No Dependencies Found</h3>
                    <p>Unable to analyze dependencies. This might indicate:</p>
                    <ul>
                        <li>Eclipse project without Maven/Gradle build files</li>
                        <li>Missing or corrupted dependency configuration</li>
                        <li>Build system not supported by analysis</li>
                    </ul>
                    <p>Consider migrating to Maven or Gradle for comprehensive dependency analysis.</p>
                </div>
            </div>
            """;
    }
    
    @Override
    public boolean isApplicable() {
        return dependencyGraph != null;
    }
    
    @Override
    public int getOrder() {
        return 40; // Show after metrics
    }
    
    // Helper methods for dependency analysis
    private String determineCompatibility(adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact artifact) {
        if (artifact.isJakartaCompatible()) {
            return "Jakarta Compatible";
        } else if (artifact.groupId().startsWith("javax.") || 
                   artifact.groupId().contains("spring") ||
                   artifact.artifactId().contains("javax")) {
            return "Needs Update";
        } else if (artifact.groupId().contains("legacy") || 
                   artifact.artifactId().contains("old") ||
                   artifact.version().matches("^[0-4]\\.")) {
            return "Incompatible";
        }
        return "Unknown Status";
    }
    
    private String getCurrentVersion(adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact artifact) {
        return artifact.version();
    }
    
    private String getJakartaVersion(adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact artifact) {
        String compatibility = determineCompatibility(artifact);
        String groupId = artifact.groupId();
        String artifactId = artifact.artifactId();
        String version = artifact.version();
        
        return switch (compatibility) {
            case "Jakarta Compatible" -> getCurrentVersion(artifact);
            case "Needs Update" -> getRecommendedVersion(groupId, artifactId, version);
            case "Incompatible" -> "N/A";
            default -> "Unknown - manual check required";
        };
    }
    
    private String getRecommendedVersion(String groupId, String artifactId, String currentVersion) {
        // Provide specific version recommendations based on artifact type
        if (groupId.contains("spring-boot")) {
            return "3.2.x (Jakarta EE 10)";
        } else if (groupId.contains("spring-framework")) {
            return "6.1.x (Jakarta EE 10)";
        } else if (groupId.contains("spring-security")) {
            return "6.2.x (Jakarta EE 10)";
        } else if (groupId.contains("spring-cloud")) {
            return "2023.x (Jakarta EE 10)";
        } else if (groupId.contains("hibernate")) {
            return "6.4.x (Jakarta EE 10)";
        } else if (artifactId.contains("jakarta") || groupId.startsWith("jakarta")) {
            return "Already Jakarta - verify version";
        } else if (groupId.startsWith("javax.servlet") || artifactId.contains("servlet")) {
            return "6.0 (Jakarta EE 10)";
        } else if (groupId.startsWith("javax.persistence") || artifactId.contains("persistence")) {
            return "3.1 (Jakarta EE 10)";
        } else if (groupId.startsWith("javax.validation") || artifactId.contains("validation")) {
            return "3.0 (Jakarta EE 10)";
        } else if (groupId.startsWith("javax.ejb")) {
            return "4.0 (Jakarta EE 10)";
        } else {
            return "Check Jakarta EE compatibility matrix";
        }
    }
    
    private String getBreakingChanges(adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact artifact) {
        String compatibility = determineCompatibility(artifact);
        String groupId = artifact.groupId();
        String artifactId = artifact.artifactId();
        
        return switch (compatibility) {
            case "Jakarta Compatible" -> "None";
            case "Needs Update" -> getSpecificBreakingChanges(groupId, artifactId);
            case "Incompatible" -> "Major API changes - research alternatives";
            default -> "Unknown - manual verification required";
        };
    }
    
    private String getSpecificBreakingChanges(String groupId, String artifactId) {
        // Provide specific breaking change information
        if (groupId.contains("spring-boot")) {
            return "javax→jakarta package rename; Spring Boot 3.x changes";
        } else if (groupId.contains("spring")) {
            return "javax→jakarta package rename";
        } else if (groupId.contains("hibernate")) {
            return "javax.persistence→jakarta.persistence; API changes";
        } else if (artifactId.contains("servlet") || groupId.contains("servlet")) {
            return "javax.servlet→jakarta.servlet; namespace change";
        } else if (artifactId.contains("persistence") || groupId.contains("persistence")) {
            return "javax.persistence→jakarta.persistence; annotation changes";
        } else if (artifactId.contains("validation") || groupId.contains("validation")) {
            return "javax.validation→jakarta.validation; constraint changes";
        } else if (groupId.startsWith("javax.ejb")) {
            return "javax.ejb→jakarta.ejb; API changes";
        } else if (groupId.startsWith("javax.ws.rs") || artifactId.contains("jaxrs")) {
            return "javax.ws.rs→jakarta.ws.rs; REST API changes";
        } else {
            return "Package rename: javax.*→jakarta.*";
        }
    }
    
    private String getMigrationEffort(adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact artifact) {
        String compatibility = determineCompatibility(artifact);
        String groupId = artifact.groupId();
        
        return switch (compatibility) {
            case "Jakarta Compatible" -> "None";
            case "Needs Update" -> getSpecificEffortEstimate(groupId);
            case "Incompatible" -> "High - alternative needed";
            default -> "Unknown - assessment required";
        };
    }
    
    private String getSpecificEffortEstimate(String groupId) {
        // Provide effort estimates based on typical migration complexity
        if (groupId.contains("spring-boot-starter-parent") || groupId.contains("spring-boot-dependencies")) {
            return "Medium (coordination required)";
        } else if (groupId.contains("spring-boot")) {
            return "Low (version bump)";
        } else if (groupId.contains("spring")) {
            return "Low-Medium (package updates)";
        } else if (groupId.contains("hibernate")) {
            return "Medium (JPA changes)";
        } else if (groupId.startsWith("javax.servlet")) {
            return "Low (package rename)";
        } else if (groupId.startsWith("javax.persistence")) {
            return "Medium (annotation updates)";
        } else if (groupId.startsWith("javax.ejb")) {
            return "Medium-High (EJB changes)";
        } else {
            return "Low-Medium (package updates)";
        }
    }
    
    private String getRecommendation(adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact artifact) {
        String compatibility = determineCompatibility(artifact);
        String groupId = artifact.groupId();
        String artifactId = artifact.artifactId();
        
        return switch (compatibility) {
            case "Jakarta Compatible" -> "✓ No action required";
            case "Needs Update" -> getSpecificRecommendation(groupId, artifactId);
            case "Incompatible" -> "⚠ Find Jakarta alternative or remove";
            default -> "❓ Verify compatibility manually";
        };
    }
    
    private String getSpecificRecommendation(String groupId, String artifactId) {
        // Provide specific action recommendations
        if (groupId.contains("spring-boot-starter-parent")) {
            return "Update to Spring Boot 3.2.x";
        } else if (groupId.contains("spring-boot")) {
            return "Upgrade with starter parent";
        } else if (groupId.contains("spring")) {
            return "Update to Spring 6.x";
        } else if (groupId.contains("hibernate")) {
            return "Upgrade to Hibernate 6.4.x";
        } else if (artifactId.contains("servlet") || groupId.contains("servlet")) {
            return "Use jakarta.servlet 6.0";
        } else if (artifactId.contains("persistence") || groupId.contains("persistence")) {
            return "Use jakarta.persistence 3.1";
        } else if (artifactId.contains("validation") || groupId.contains("validation")) {
            return "Use jakarta.validation 3.0";
        } else {
            return "Search for Jakarta EE equivalent";
        }
    }
    
    private String getRowClass(String compatibility) {
        return compatibility.toLowerCase().replace(" ", "-");
    }
    
    private int calculateCompatibleCount() {
        return (int) dependencyGraph.getNodes().stream()
            .filter(artifact -> determineCompatibility(artifact).equals("Jakarta Compatible"))
            .count();
    }
    
    private int calculateNeedsUpdateCount() {
        return (int) dependencyGraph.getNodes().stream()
            .filter(artifact -> determineCompatibility(artifact).equals("Needs Update"))
            .count();
    }
    
    private int calculateIncompatibleCount() {
        return (int) dependencyGraph.getNodes().stream()
            .filter(artifact -> determineCompatibility(artifact).equals("Incompatible"))
            .count();
    }
    
    private int calculateUnknownCount() {
        return (int) dependencyGraph.getNodes().stream()
            .filter(artifact -> determineCompatibility(artifact).equals("Unknown Status"))
            .count();
    }
}
