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
        String riskLevel = determineRiskLevel(riskScoreValue);
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
                        <div class="metric-value">%s</div>
                        <div class="metric-label">Risk Level</div>
                    </div>
                    <div class="metric-card">
                        <div class="metric-value">%d%%</div>
                        <div class="metric-label">Readiness Score</div>
                    </div>
                </div>
            </div>
            """, totalDependencies, jakartaCompatible, totalIssues, riskLevel, readinessScore);
    }
    
    private String generateEclipseMetrics() throws SnippetGenerationException {
        int totalIssues = scanResults != null ? scanResults.totalIssuesFound() : 0;
        double riskScoreValue = riskScore != null ? riskScore.totalScore() : 50.0;
        String riskLevel = determineRiskLevel(riskScoreValue);
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
                        <div class="metric-value">%s</div>
                        <div class="metric-label">Risk Level</div>
                    </div>
                    <div class="metric-card">
                        <div class="metric-value">%d%%</div>
                        <div class="metric-label">Readiness Score</div>
                    </div>
                </div>
            </div>
            """, totalIssues, riskLevel, readinessScore);
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
        
        // Simplified calculation - in real implementation this would check actual compatibility
        return (int) (dependencyGraph.getNodes().size() * 0.7); // Assume 70% compatible
    }
    
    private String determineRiskLevel(double riskScore) {
        if (riskScore < 30) return "LOW";
        if (riskScore < 60) return "MEDIUM";
        if (riskScore < 80) return "HIGH";
        return "CRITICAL";
    }
}
