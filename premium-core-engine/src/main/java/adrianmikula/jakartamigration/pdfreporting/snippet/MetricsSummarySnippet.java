package adrianmikula.jakartamigration.pdfreporting.snippet;

import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.risk.RiskScoringService;

/**
 * Metrics summary snippet showing key report statistics.
 * Handles both full dependency data and Eclipse project cases.
 */
public class MetricsSummarySnippet extends BaseHtmlSnippet {
    
    private final DependencyGraph dependencyGraph;
    private final ComprehensiveScanResults scanResults;
    private final RiskScoringService.RiskScore riskScore;
    
    public MetricsSummarySnippet(DependencyGraph dependencyGraph, 
                                ComprehensiveScanResults scanResults, 
                                RiskScoringService.RiskScore riskScore) {
        this.dependencyGraph = dependencyGraph;
        this.scanResults = scanResults;
        this.riskScore = riskScore;
    }
    
    @Override
    public String generate() throws SnippetGenerationException {
        if (dependencyGraph != null) {
            return generateFullMetrics();
        } else {
            return generateEclipseMetrics();
        }
    }
    
    private String generateFullMetrics() throws SnippetGenerationException {
        int totalDependencies = dependencyGraph.getNodes().size();
        int jakartaCompatible = calculateJakartaCompatible(dependencyGraph);
        int totalIssues = scanResults != null ? scanResults.totalIssuesFound() : 0;
        double riskScoreValue = riskScore != null ? riskScore.totalScore() : 50.0;
        int readinessScore = 100 - (int) riskScoreValue;

        return safelyFormat("""
            <div class="section">
                <h2>Project Metrics</h2>
                <div class="metrics-grid">
                    <div class="metric-card">
                        <div class="metric-value">%d</div>
                        <div class="metric-label">Total Dependencies</div>
                    </div>
                    <div class="metric-card">
                        <div class="metric-value">%d</div>
                        <div class="metric-label">Jakarta Compatible</div>
                    </div>
                    <div class="metric-card">
                        <div class="metric-value">%d</div>
                        <div class="metric-label">Issues Found</div>
                    </div>
                    <div class="metric-card">
                        <div class="metric-value">%d%%</div>
                        <div class="metric-label">Readiness Score</div>
                    </div>
                </div>
            </div>
            """, totalDependencies, jakartaCompatible, totalIssues, readinessScore);
    }
    
    private String generateEclipseMetrics() throws SnippetGenerationException {
        int totalIssues = scanResults != null ? scanResults.totalIssuesFound() : 0;
        double riskScoreValue = riskScore != null ? riskScore.totalScore() : 50.0;
        int readinessScore = 100 - (int) riskScoreValue;

        return safelyFormat("""
            <div class="section">
                <h2>Available Analysis Results</h2>
                <div class="metrics-grid">
                    <div class="metric-card">
                        <div class="metric-value">%d</div>
                        <div class="metric-label">Issues Found</div>
                    </div>
                    <div class="metric-card">
                        <div class="metric-value">%d%%</div>
                        <div class="metric-label">Readiness Score</div>
                    </div>
                </div>
            </div>
            """, totalIssues, readinessScore);
    }
    
    @Override
    public boolean isApplicable() {
        return true; // Always show metrics
    }
    
    @Override
    public int getOrder() {
        return 30; // Show after header and warning
    }
    
    private int calculateJakartaCompatible(DependencyGraph dependencyGraph) {
        if (dependencyGraph == null || dependencyGraph.getNodes().isEmpty()) {
            return 0;
        }
        
        // Calculate actual Jakarta compatibility from artifact data
        int compatible = 0;
        for (var artifact : dependencyGraph.getNodes()) {
            if (isJakartaCompatible(artifact)) {
                compatible++;
            }
        }
        return compatible;
    }
    
    private boolean isJakartaCompatible(adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact artifact) {
        // Check if artifact is already Jakarta-compatible based on groupId and version
        String groupId = artifact.groupId();
        String artifactId = artifact.artifactId();
        String version = artifact.version();
        
        // Already Jakarta EE artifacts
        if (groupId.startsWith("jakarta.")) {
            return true;
        }
        
        // Spring Boot 3.x+ is Jakarta EE 9+ compatible
        if (groupId.contains("spring") && !version.isEmpty()) {
            try {
                // Extract major version
                String majorVersion = version.split("\\.")[0];
                int major = Integer.parseInt(majorVersion);
                if (groupId.contains("spring-boot") && major >= 3) {
                    return true;
                }
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                // Unable to parse version, assume not compatible
            }
        }
        
        // Jakarta EE compatible artifacts (already migrated)
        if (artifactId.startsWith("jakarta")) {
            return true;
        }
        
        // javax.* artifacts need migration (not compatible)
        if (groupId.startsWith("javax.")) {
            return false;
        }
        
        // Unknown - require manual verification
        return false;
    }
    
}
