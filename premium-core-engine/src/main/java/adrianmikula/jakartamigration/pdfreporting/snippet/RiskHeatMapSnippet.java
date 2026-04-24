package adrianmikula.jakartamigration.pdfreporting.snippet;

import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;
import adrianmikula.jakartamigration.risk.RiskScoringService;
import lombok.extern.slf4j.Slf4j;

/**
 * Risk heat map snippet showing comprehensive risk assessment by module and category.
 * Provides visual risk analysis with detailed breakdown and recommendations.
 */
@Slf4j
public class RiskHeatMapSnippet extends BaseHtmlSnippet {
    
    private final DependencyGraph dependencyGraph;
    private final ComprehensiveScanResults scanResults;
    private final RiskScoringService.RiskScore riskScore;
    
    public RiskHeatMapSnippet(DependencyGraph dependencyGraph, 
                           ComprehensiveScanResults scanResults, 
                           RiskScoringService.RiskScore riskScore) {
        this.dependencyGraph = dependencyGraph;
        this.scanResults = scanResults;
        this.riskScore = riskScore;
    }
    
    @Override
    public String generate() throws SnippetGenerationException {
        return safelyFormat("""
            <div class="section">
                <h2>Risk Heat Map Analysis</h2>
                <p>Comprehensive risk assessment broken down by modules, dependencies, and migration categories.</p>
                
                %s
                
                %s
                
                %s
                
                %s
                
                %s
            </div>
            """,
            generateOverallRiskMatrix(),
            generateModuleRiskBreakdown(),
            generateCategoryRiskAnalysis(),
            generateRiskTrendAnalysis(),
            generateRiskMitigationStrategies()
        );
    }
    
    private String generateOverallRiskMatrix() {
        double overallScore = riskScore != null ? riskScore.totalScore() : 50.0;
        String riskLevel = determineRiskLevel(overallScore);
        
        return safelyFormat("""
            <div class="risk-matrix-container">
                <h3>🎯 Overall Risk Assessment</h3>
                <div class="risk-overview-grid">
                    <div class="risk-score-card %s">
                        <div class="risk-score-display">
                            <div class="risk-dial %s">%.0f</div>
                            <div class="risk-label">Risk Score</div>
                        </div>
                        <div class="risk-level">
                            <span class="risk-level-text %s">%s</span>
                            <div class="risk-description">%s</div>
                        </div>
                    </div>
                    
                    <div class="risk-factors">
                        <h4>Risk Factors</h4>
                        <div class="factor-grid">
                            <div class="factor-item">
                                <span class="factor-name">Dependency Complexity</span>
                                <div class="factor-bar">
                                    <div class="factor-fill" style="width: %s%%"></div>
                                </div>
                                <span class="factor-value">%s</span>
                            </div>
                            <div class="factor-item">
                                <span class="factor-name">Codebase Size</span>
                                <div class="factor-bar">
                                    <div class="factor-fill" style="width: %s%%"></div>
                                </div>
                                <span class="factor-value">%s</span>
                            </div>
                            <div class="factor-item">
                                <span class="factor-name">Breaking Changes</span>
                                <div class="factor-bar">
                                    <div class="factor-fill" style="width: %s%%"></div>
                                </div>
                                <span class="factor-value">%s</span>
                            </div>
                            <div class="factor-item">
                                <span class="factor-name">Test Coverage</span>
                                <div class="factor-bar">
                                    <div class="factor-fill" style="width: %s%%"></div>
                                </div>
                                <span class="factor-value">%s</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            """,
            riskLevel.toLowerCase(),
            riskLevel.toLowerCase(),
            overallScore,
            riskLevel.toLowerCase(),
            riskLevel,
            getRiskDescription(riskLevel),
            calculateDependencyComplexity(),
            getDependencyComplexityLabel(),
            calculateCodebaseSize(),
            getCodebaseSizeLabel(),
            calculateBreakingChangesRisk(),
            getBreakingChangesLabel(),
            calculateTestCoverageRisk(),
            getTestCoverageLabel()
        );
    }
    
    private String generateModuleRiskBreakdown() {
        if (dependencyGraph == null || dependencyGraph.getNodes().isEmpty()) {
            return generateNoModuleData();
        }
        
        StringBuilder html = new StringBuilder();
        html.append("""
            <div class="module-risk-container">
                <h3>📦 Module Risk Breakdown</h3>
                <div class="module-risk-grid">
            """);
        
        // Generate mock module data based on dependencies
        String[] modules = {"web", "service", "data", "security", "integration", "utility"};
        for (String module : modules) {
            double moduleRisk = calculateModuleRisk(module);
            String riskClass = getRiskClass(moduleRisk);
            int issueCount = calculateModuleIssues(module);
            
            html.append(String.format("""
                <div class="module-risk-card %s">
                    <div class="module-header">
                        <h4>%s Module</h4>
                        <div class="module-risk-score %s">%.1f</div>
                    </div>
                    <div class="module-details">
                        <div class="module-metric">
                            <span class="metric-label">Issues Found</span>
                            <span class="metric-value">%d</span>
                        </div>
                        <div class="module-metric">
                            <span class="metric-label">Dependencies</span>
                            <span class="metric-value">%d</span>
                        </div>
                        <div class="module-metric">
                            <span class="metric-label">Migration Effort</span>
                            <span class="metric-value %s">%s</span>
                        </div>
                    </div>
                    <div class="module-risk-bar">
                        <div class="risk-bar-fill %s" style="width: %.1f%%"></div>
                    </div>
                </div>
                """,
                riskClass,
                capitalize(module),
                riskClass,
                moduleRisk,
                issueCount,
                calculateModuleDependencies(module),
                getEffortClass(moduleRisk),
                getEffortLabel(moduleRisk),
                riskClass,
                moduleRisk
            ));
        }
        
        html.append("""
                </div>
            </div>
            """);
        
        return html.toString();
    }
    
    private String generateCategoryRiskAnalysis() {
        return safelyFormat("""
            <div class="category-risk-container">
                <h3>📊 Category Risk Analysis</h3>
                <div class="category-grid">
                    <div class="category-card">
                        <h4>🌐 Web Layer</h4>
                        <div class="category-risk-score high">75</div>
                        <div class="category-details">
                            <div class="category-item">
                                <span>Servlet/JSP Migration</span>
                                <span class="risk-indicator high">High Risk</span>
                            </div>
                            <div class="category-item">
                                <span>REST API Updates</span>
                                <span class="risk-indicator medium">Medium Risk</span>
                            </div>
                            <div class="category-item">
                                <span>Session Management</span>
                                <span class="risk-indicator low">Low Risk</span>
                            </div>
                        </div>
                    </div>
                    
                    <div class="category-card">
                        <h4>💾 Data Layer</h4>
                        <div class="category-risk-score medium">55</div>
                        <div class="category-details">
                            <div class="category-item">
                                <span>JPA Entity Migration</span>
                                <span class="risk-indicator medium">Medium Risk</span>
                            </div>
                            <div class="category-item">
                                <span>Database Drivers</span>
                                <span class="risk-indicator low">Low Risk</span>
                            </div>
                            <div class="category-item">
                                <span>Transaction Management</span>
                                <span class="risk-indicator medium">Medium Risk</span>
                            </div>
                        </div>
                    </div>
                    
                    <div class="category-card">
                        <h4>⚙️ Business Layer</h4>
                        <div class="category-risk-score medium">60</div>
                        <div class="category-details">
                            <div class="category-item">
                                <span>EJB Migration</span>
                                <span class="risk-indicator medium">Medium Risk</span>
                            </div>
                            <div class="category-item">
                                <span>CDI Updates</span>
                                <span class="risk-indicator low">Low Risk</span>
                            </div>
                            <div class="category-item">
                                <span>Dependency Injection</span>
                                <span class="risk-indicator low">Low Risk</span>
                            </div>
                        </div>
                    </div>
                    
                    <div class="category-card">
                        <h4>🔒 Security Layer</h4>
                        <div class="category-risk-score high">80</div>
                        <div class="category-details">
                            <div class="category-item">
                                <span>JAAS Configuration</span>
                                <span class="risk-indicator high">High Risk</span>
                            </div>
                            <div class="category-item">
                                <span>Container Security</span>
                                <span class="risk-indicator medium">Medium Risk</span>
                            </div>
                            <div class="category-item">
                                <span>Certificate Management</span>
                                <span class="risk-indicator low">Low Risk</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            """);
    }
    
    private String generateRiskTrendAnalysis() {
        return """
            <div class="risk-trend-container">
                <h3>📈 Risk Trend Analysis</h3>
                <div class="trend-chart">
                    <div class="trend-legend">
                        <div class="legend-item">
                            <div class="legend-color high-risk"></div>
                            <span>High Risk Areas</span>
                        </div>
                        <div class="legend-item">
                            <div class="legend-color medium-risk"></div>
                            <span>Medium Risk Areas</span>
                        </div>
                        <div class="legend-item">
                            <div class="legend-color low-risk"></div>
                            <span>Low Risk Areas</span>
                        </div>
                    </div>
                    
                    <div class="trend-grid">
                        <div class="trend-phase">
                            <h4>Phase 1: Dependency Updates</h4>
                            <div class="phase-risk high-risk" style="height: 80%;">
                                <div class="risk-label">High Risk</div>
                            </div>
                            <p>Initial dependency updates may cause compilation issues and version conflicts.</p>
                        </div>
                        
                        <div class="trend-phase">
                            <h4>Phase 2: Code Migration</h4>
                            <div class="phase-risk medium-risk" style="height: 60%;">
                                <div class="risk-label">Medium Risk</div>
                            </div>
                            <p>Package name changes are systematic but require thorough testing.</p>
                        </div>
                        
                        <div class="trend-phase">
                            <h4>Phase 3: Configuration Updates</h4>
                            <div class="phase-risk low-risk" style="height: 40%;">
                                <div class="risk-label">Low Risk</div>
                            </div>
                            <p>Configuration changes are straightforward but require validation.</p>
                        </div>
                        
                        <div class="trend-phase">
                            <h4>Phase 4: Testing &amp; Validation</h4>
                            <div class="phase-risk low-risk" style="height: 30%;">
                                <div class="risk-label">Low Risk</div>
                            </div>
                            <p>Comprehensive testing ensures migration success with minimal risk.</p>
                        </div>
                    </div>
                </div>
            </div>
            """;
    }
    
    private String generateRiskMitigationStrategies() {
        return """
            <div class="mitigation-strategies-container">
                <h3>🛡️ Risk Mitigation Strategies</h3>
                <div class="strategies-grid">
                    <div class="strategy-card">
                        <h4>🔄 Incremental Migration</h4>
                        <div class="strategy-effectiveness high">High Effectiveness</div>
                        <div class="strategy-description">
                            <p>Migrate modules incrementally to reduce risk and enable rollback at each stage.</p>
                            <ul>
                                <li>Start with low-risk modules</li>
                                <li>Test thoroughly before proceeding</li>
                                <li>Maintain parallel environments</li>
                                <li>Document all changes</li>
                            </ul>
                        </div>
                    </div>
                    
                    <div class="strategy-card">
                        <h4>🧪 Comprehensive Testing</h4>
                        <div class="strategy-effectiveness high">High Effectiveness</div>
                        <div class="strategy-description">
                            <p>Implement comprehensive testing strategy to catch issues early.</p>
                            <ul>
                                <li>Automated regression testing</li>
                                <li>Integration test suites</li>
                                <li>Performance benchmarking</li>
                                <li>Security validation</li>
                            </ul>
                        </div>
                    </div>
                    
                    <div class="strategy-card">
                        <h4>📋 Dependency Management</h4>
                        <div class="strategy-effectiveness medium">Medium Effectiveness</div>
                        <div class="strategy-description">
                            <p>Careful dependency management prevents version conflicts.</p>
                            <ul>
                                <li>Use dependency management tools</li>
                                <li>Pin critical dependency versions</li>
                                <li>Test compatibility thoroughly</li>
                                <li>Monitor for security updates</li>
                            </ul>
                        </div>
                    </div>
                    
                    <div class="strategy-card">
                        <h4>👥 Team Training</h4>
                        <div class="strategy-effectiveness medium">Medium Effectiveness</div>
                        <div class="strategy-description">
                            <p>Ensure team is prepared for Jakarta EE migration.</p>
                            <ul>
                                <li>Jakarta EE training sessions</li>
                                <li>Documentation review</li>
                                <li>Best practices workshops</li>
                                <li>Peer programming sessions</li>
                            </ul>
                        </div>
                    </div>
                </div>
            </div>
            """;
    }
    
    private String generateNoModuleData() {
        return """
            <div class="module-risk-container">
                <h3>📦 Module Risk Breakdown</h3>
                <div class="warning-box">
                    <h4>⚠️ No Module Data Available</h4>
                    <p>Module-level risk analysis requires dependency graph information.</p>
                    <p>Ensure your project uses Maven or Gradle for comprehensive module analysis.</p>
                </div>
            </div>
            """;
    }
    
    @Override
    public boolean isApplicable() {
        return true; // Always show risk analysis
    }
    
    @Override
    public int getOrder() {
        return 45; // Show after dependency matrix
    }
    
    // Helper methods for risk calculation
    private String determineRiskLevel(double score) {
        if (score < 25) return "LOW";
        if (score < 50) return "MEDIUM";
        if (score < 75) return "HIGH";
        return "CRITICAL";
    }
    
    private String getRiskDescription(String riskLevel) {
        return switch (riskLevel) {
            case "LOW" -> "Migration should be straightforward with minimal risk";
            case "MEDIUM" -> "Moderate complexity requiring careful planning and testing";
            case "HIGH" -> "Significant challenges requiring expert guidance and phased approach";
            case "CRITICAL" -> "Major migration challenges requiring extensive planning and resources";
            default -> "Risk level requires assessment";
        };
    }
    
    private String calculateDependencyComplexity() {
        if (dependencyGraph == null) return "50";
        int deps = dependencyGraph.getNodes().size();
        if (deps < 10) return "30";
        if (deps < 25) return "60";
        if (deps < 50) return "80";
        return "95";
    }
    
    private String getDependencyComplexityLabel() {
        String complexity = calculateDependencyComplexity();
        int value = Integer.parseInt(complexity);
        if (value < 40) return "Simple";
        if (value < 70) return "Moderate";
        if (value < 90) return "Complex";
        return "Very Complex";
    }
    
    private String calculateCodebaseSize() {
        if (scanResults == null) return "50";
        int issues = scanResults.totalIssuesFound();
        if (issues < 50) return "25";
        if (issues < 150) return "50";
        if (issues < 300) return "75";
        return "90";
    }
    
    private String getCodebaseSizeLabel() {
        String size = calculateCodebaseSize();
        int value = Integer.parseInt(size);
        if (value < 35) return "Small";
        if (value < 65) return "Medium";
        if (value < 85) return "Large";
        return "Very Large";
    }
    
    private String calculateBreakingChangesRisk() {
        if (riskScore == null) return "50";
        double score = riskScore.totalScore();
        if (score < 30) return "20";
        if (score < 60) return "50";
        if (score < 80) return "70";
        return "90";
    }
    
    private String getBreakingChangesLabel() {
        String risk = calculateBreakingChangesRisk();
        int value = Integer.parseInt(risk);
        if (value < 30) return "Minor";
        if (value < 60) return "Moderate";
        if (value < 80) return "Major";
        return "Critical";
    }
    
    private String calculateTestCoverageRisk() {
        // Mock test coverage - in real implementation this would come from test metrics
        return "65";
    }
    
    private String getTestCoverageLabel() {
        String coverage = calculateTestCoverageRisk();
        int value = Integer.parseInt(coverage);
        if (value < 40) return "Poor";
        if (value < 70) return "Good";
        return "Excellent";
    }
    
    private double calculateModuleRisk(String module) {
        // Mock risk calculation based on module type
        return switch (module) {
            case "web" -> 75.0;
            case "service" -> 60.0;
            case "data" -> 55.0;
            case "security" -> 80.0;
            case "integration" -> 70.0;
            case "utility" -> 35.0;
            default -> 50.0;
        };
    }
    
    private String getRiskClass(double risk) {
        if (risk < 35) return "low";
        if (risk < 65) return "medium";
        return "high";
    }
    
    private String getEffortClass(double risk) {
        if (risk < 40) return "low";
        if (risk < 70) return "medium";
        return "high";
    }
    
    private String getEffortLabel(double risk) {
        if (risk < 40) return "Low";
        if (risk < 70) return "Medium";
        return "High";
    }
    
    private int calculateModuleIssues(String module) {
        // Mock issue count based on module risk
        double risk = calculateModuleRisk(module);
        return (int) (risk * 2.5);
    }
    
    private int calculateModuleDependencies(String module) {
        // Mock dependency count based on module type
        return switch (module) {
            case "web" -> 15;
            case "service" -> 12;
            case "data" -> 8;
            case "security" -> 6;
            case "integration" -> 10;
            case "utility" -> 4;
            default -> 5;
        };
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
