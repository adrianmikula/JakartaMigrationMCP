package adrianmikula.jakartamigration.pdfreporting.snippet;

import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.risk.RiskScoringService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Executive summary snippet for project managers and product owners.
 * Provides business-focused migration overview with risk assessment,
 * timeline estimates, and resource requirements.
 */
public class ExecutiveSummarySnippet extends BaseHtmlSnippet {
    
    private final String projectName;
    private final DependencyGraph dependencyGraph;
    private final ComprehensiveScanResults scanResults;
    private final RiskScoringService.RiskScore riskScore;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    public ExecutiveSummarySnippet(String projectName,
                                   DependencyGraph dependencyGraph,
                                   ComprehensiveScanResults scanResults,
                                   RiskScoringService.RiskScore riskScore) {
        this.projectName = projectName;
        this.dependencyGraph = dependencyGraph;
        this.scanResults = scanResults;
        this.riskScore = riskScore;
    }
    
    @Override
    public String generate() throws SnippetGenerationException {
        return safelyFormat("""
            <div class="section executive-section">
                <h2>Executive Summary</h2>
                <p class="executive-subtitle">Business overview and strategic recommendations for Jakarta EE migration</p>
                
                %s
                %s
                %s
            </div>
            """,
            generateBusinessOverview(),
            generateRiskSummary(),
            generateKeyRecommendations()
        );
    }
    
    private String generateBusinessOverview() {
        String generatedAt = LocalDateTime.now().format(DATE_FORMATTER);
        int totalFiles = getTotalFilesScanned();
        int issuesFound = getTotalIssuesFound();
        int depCount = getDependencyCount();
        
        return String.format("""
            <div class="executive-overview">
                <div class="overview-header">
                    <h3>📊 Project Overview</h3>
                    <span class="report-date">Generated: %s</span>
                </div>
                <div class="overview-grid">
                    <div class="overview-card">
                        <span class="overview-label">Project</span>
                        <span class="overview-value">%s</span>
                    </div>
                    <div class="overview-card">
                        <span class="overview-label">Files Analyzed</span>
                        <span class="overview-value">%d</span>
                    </div>
                    <div class="overview-card">
                        <span class="overview-label">Issues Identified</span>
                        <span class="overview-value">%d</span>
                    </div>
                    <div class="overview-card">
                        <span class="overview-label">Dependencies</span>
                        <span class="overview-value">%d</span>
                    </div>
                </div>
            </div>
            """,
            generatedAt,
            escapeHtml(projectName != null ? projectName : "Unknown Project"),
            totalFiles,
            issuesFound,
            depCount
        );
    }
    
    private String generateRiskSummary() {
        double riskScoreValue = getRiskScore();
        String riskLevel = determineRiskLevel(riskLevelScore(riskScoreValue));
        int readinessScore = Math.max(0, 100 - (int) riskScoreValue);
        
        String riskDescription = switch (riskLevel) {
            case "LOW" -> "Straightforward migration with minimal business risk.";
            case "MEDIUM" -> "Moderate complexity requiring planned approach and testing.";
            case "HIGH" -> "Significant effort required. Recommend phased migration approach.";
            case "CRITICAL" -> "Complex migration with high risk. Expert consultation advised.";
            default -> "Risk assessment requires further analysis.";
        };
        
        String businessImpact = switch (riskLevel) {
            case "LOW" -> "Minimal downtime expected. Standard deployment windows sufficient.";
            case "MEDIUM" -> "Plan for extended testing period. Consider staging environment validation.";
            case "HIGH" -> "Business continuity planning recommended. Parallel run strategy advised.";
            case "CRITICAL" -> "Executive stakeholder engagement required. Detailed rollback plan essential.";
            default -> "Impact assessment pending.";
        };
        
        return String.format("""
            <div class="executive-risk-summary">
                <h3>🎯 Risk Assessment</h3>
                <div class="risk-summary-grid">
                    <div class="risk-gauge-card %s">
                        <div class="risk-gauge-value">%.0f</div>
                        <div class="risk-gauge-label">Risk Score</div>
                        <div class="risk-gauge-level">%s</div>
                    </div>
                    <div class="readiness-card">
                        <div class="readiness-value">%d%%</div>
                        <div class="readiness-label">Migration Readiness</div>
                        <div class="readiness-bar">
                            <div class="readiness-fill" style="width: %d%%"></div>
                        </div>
                    </div>
                </div>
                <div class="risk-descriptions">
                    <div class="risk-description-item">
                        <strong>Migration Complexity:</strong> %s
                    </div>
                    <div class="risk-description-item">
                        <strong>Business Impact:</strong> %s
                    </div>
                </div>
            </div>
            """,
            riskLevel.toLowerCase(),
            riskScoreValue,
            riskLevel,
            readinessScore,
            readinessScore,
            riskDescription,
            businessImpact
        );
    }
    
    private String generateKeyRecommendations() {
        double riskScoreValue = getRiskScore();
        String riskLevel = determineRiskLevel(riskLevelScore(riskScoreValue));
        
        String[] recommendations = switch (riskLevel) {
            case "LOW" -> new String[] {
                "Plan migration during standard maintenance windows",
                "Allocate 2-3 developers for 2-4 weeks",
                "Standard regression testing should be sufficient"
            };
            case "MEDIUM" -> new String[] {
                "Schedule dedicated migration sprint(s)",
                "Include comprehensive integration testing",
                "Plan for staging environment validation",
                "Consider incremental migration by module"
            };
            case "HIGH" -> new String[] {
                "Establish dedicated migration team (4-6 people)",
                "Implement parallel development strategy",
                "Plan extended testing and validation period",
                "Prepare detailed rollback procedures",
                "Engage stakeholders for extended timeline"
            };
            case "CRITICAL" -> new String[] {
                "Executive sponsorship and stakeholder alignment required",
                "Engage external Jakarta EE migration specialists",
                "Consider phased migration over multiple quarters",
                "Invest in comprehensive automated testing",
                "Plan for potential architecture changes",
                "Establish dedicated project governance"
            };
            default -> new String[] {
                "Complete detailed technical assessment",
                "Review findings with technical team",
                "Develop customized migration approach"
            };
        };
        
        StringBuilder recs = new StringBuilder();
        for (String rec : recommendations) {
            recs.append(String.format("<li>%s</li>%n", rec));
        }
        
        return String.format("""
            <div class="executive-recommendations">
                <h3>💡 Strategic Recommendations</h3>
                <div class="recommendations-list">
                    <ul>
                        %s
                    </ul>
                </div>
                <div class="next-steps">
                    <h4>Immediate Next Steps</h4>
                    <ol>
                        <li>Review this report with technical leadership</li>
                        <li>Approve migration timeline and resource allocation</li>
                        <li>Schedule kick-off meeting with development team</li>
                        <li>Set up migration tracking and reporting cadence</li>
                    </ol>
                </div>
            </div>
            """,
            recs.toString()
        );
    }
    
    private double getRiskScore() {
        return (riskScore != null) ? riskScore.totalScore() : 50.0;
    }
    
    private int riskLevelScore(double score) {
        if (score < 30) return 1; // LOW
        if (score < 60) return 2; // MEDIUM
        if (score < 80) return 3; // HIGH
        return 4; // CRITICAL
    }
    
    private String determineRiskLevel(int level) {
        return switch (level) {
            case 1 -> "LOW";
            case 2 -> "MEDIUM";
            case 3 -> "HIGH";
            case 4 -> "CRITICAL";
            default -> "UNKNOWN";
        };
    }
    
    private int getTotalFilesScanned() {
        return (scanResults != null && scanResults.summary() != null)
            ? scanResults.summary().totalFilesScanned() : 0;
    }
    
    private int getTotalIssuesFound() {
        return (scanResults != null)
            ? scanResults.totalIssuesFound() : 0;
    }
    
    private int getDependencyCount() {
        return (dependencyGraph != null) ? dependencyGraph.getNodes().size() : 0;
    }
    
    @Override
    public boolean isApplicable() {
        return true; // Always show executive summary
    }
    
    @Override
    public int getOrder() {
        return 25; // Show early in report, after header/warning but before detailed metrics
    }
    
}
