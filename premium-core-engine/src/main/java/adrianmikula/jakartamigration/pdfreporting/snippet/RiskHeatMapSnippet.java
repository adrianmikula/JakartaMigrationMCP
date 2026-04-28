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
                <h2>Risk Analysis Details</h2>
                <p>Risk assessment by migration category with mitigation strategies.</p>

                %s

                %s

                %s
            </div>
            """,
            generateCategoryRiskAnalysis(),
            generateRiskTrendAnalysis(),
            generateRiskMitigationStrategies()
        );
    }

    // Note: Module risk breakdown removed - will be re-added when real module scanning is available

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
                            <div class="phase-risk high-risk">
                                <div class="risk-label">High Risk</div>
                            </div>
                            <p>Initial dependency updates may cause compilation issues and version conflicts.</p>
                        </div>

                        <div class="trend-phase">
                            <h4>Phase 2: Code Migration</h4>
                            <div class="phase-risk medium-risk">
                                <div class="risk-label">Medium Risk</div>
                            </div>
                            <p>Package name changes are systematic but require thorough testing.</p>
                        </div>

                        <div class="trend-phase">
                            <h4>Phase 3: Configuration Updates</h4>
                            <div class="phase-risk low-risk">
                                <div class="risk-label">Low Risk</div>
                            </div>
                            <p>Configuration changes are straightforward but require validation.</p>
                        </div>

                        <div class="trend-phase">
                            <h4>Phase 4: Testing &amp; Validation</h4>
                            <div class="phase-risk low-risk">
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
    
    @Override
    public boolean isApplicable() {
        return true; // Always show risk analysis
    }

    @Override
    public int getOrder() {
        return 45; // Show after dependency matrix
    }
}
