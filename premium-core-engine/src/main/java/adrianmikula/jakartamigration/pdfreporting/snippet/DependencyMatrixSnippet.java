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
        return switch (compatibility) {
            case "Jakarta Compatible" -> getCurrentVersion(artifact);
            case "Needs Update" -> "Available";
            case "Incompatible" -> "N/A";
            default -> "Unknown";
        };
    }
    
    private String getBreakingChanges(adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact artifact) {
        String compatibility = determineCompatibility(artifact);
        return switch (compatibility) {
            case "Jakarta Compatible" -> "None";
            case "Needs Update" -> "Package rename";
            case "Incompatible" -> "Major API changes";
            default -> "Unknown";
        };
    }
    
    private String getMigrationEffort(adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact artifact) {
        String compatibility = determineCompatibility(artifact);
        return switch (compatibility) {
            case "Jakarta Compatible" -> "None";
            case "Needs Update" -> "Low";
            case "Incompatible" -> "High";
            default -> "Unknown";
        };
    }
    
    private String getRecommendation(adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact artifact) {
        String compatibility = determineCompatibility(artifact);
        return switch (compatibility) {
            case "Jakarta Compatible" -> "Keep current version";
            case "Needs Update" -> "Update to Jakarta version";
            case "Incompatible" -> "Find alternative";
            default -> "Manual verification";
        };
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
