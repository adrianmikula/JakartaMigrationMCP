package adrianmikula.jakartamigration.pdfreporting.snippet;

import lombok.extern.slf4j.Slf4j;

/**
 * Implementation roadmap snippet showing detailed migration plan with resource allocation.
 * Provides phase-by-phase breakdown with timelines, resources, and deliverables.
 */
@Slf4j
public class ImplementationRoadmapSnippet extends BaseHtmlSnippet {
    
    @Override
    public String generate() throws SnippetGenerationException {
        return safelyFormat("""
            <div class="section">
                <h2>Implementation Roadmap</h2>
                <p>Comprehensive migration plan with detailed phases, resource allocation, and timeline estimates.</p>
                
                %s
                
                %s
                
                %s
                
                %s
                
                %s
            </div>
            """,
            generateExecutiveSummary(),
            generatePhaseBreakdown(),
            generateResourceAllocation(),
            generateTimelineEstimates(),
            generateRiskMitigation()
        );
    }
    
    private final adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph dependencyGraph;
    private final adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults scanResults;
    private final adrianmikula.jakartamigration.risk.RiskScoringService.RiskScore riskScore;
    
    public ImplementationRoadmapSnippet(
            adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph dependencyGraph,
            adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults scanResults,
            adrianmikula.jakartamigration.risk.RiskScoringService.RiskScore riskScore) {
        this.dependencyGraph = dependencyGraph;
        this.scanResults = scanResults;
        this.riskScore = riskScore;
    }
    
    public ImplementationRoadmapSnippet() {
        this(null, null, null);
    }
    
    private String generateExecutiveSummary() {
        // Calculate dynamic estimates based on actual scan data
        TimelineEstimate estimate = calculateTimelineEstimate();
        
        return String.format("""
            <div class="executive-summary-container">
                <h3>📋 Executive Summary</h3>
                <div class="summary-grid">
                    <div class="summary-card">
                        <h4>Migration Duration</h4>
                        <div class="summary-value">%d-%d weeks</div>
                        <p>Based on %d dependencies and %d issues found</p>
                    </div>
                    <div class="summary-card">
                        <h4>Team Size</h4>
                        <div class="summary-value">%d-%d developers</div>
                        <p>Recommended based on project complexity</p>
                    </div>
                    <div class="summary-card">
                        <h4>Total Effort</h4>
                        <div class="summary-value">%d-%d person-hours</div>
                        <p>Includes development, testing, and deployment</p>
                    </div>
                    <div class="summary-card">
                        <h4>Success Rate</h4>
                        <div class="summary-value">%d%%</div>
                        <p>Estimated based on risk analysis</p>
                    </div>
                </div>
            </div>
            """,
            estimate.minWeeks,
            estimate.maxWeeks,
            estimate.dependencyCount,
            estimate.issueCount,
            estimate.minTeamSize,
            estimate.maxTeamSize,
            estimate.minHours,
            estimate.maxHours,
            estimate.successRate
        );
    }
    
    private TimelineEstimate calculateTimelineEstimate() {
        int dependencyCount = (dependencyGraph != null) ? dependencyGraph.getNodes().size() : 5;
        int issueCount = (scanResults != null && scanResults.summary() != null) 
            ? scanResults.summary().filesWithIssues() : 10;
        double riskScoreValue = (riskScore != null) ? riskScore.totalScore() : 50.0;
        
        // Base weeks for setup
        int baseWeeks = 1;
        
        // Weeks per dependency (version resolution, testing)
        double weeksPerDep = 0.3;
        int depWeeks = (int) Math.ceil(dependencyCount * weeksPerDep);
        
        // Weeks per batch of issues
        double weeksPerIssueBatch = 0.5;
        int issueWeeks = (int) Math.ceil(issueCount / 5.0 * weeksPerIssueBatch);
        
        // Risk adjustment
        double riskMultiplier = 1.0 + (riskScoreValue / 100.0); // 1.0 to 2.0
        
        int calculatedWeeks = (int) Math.ceil((baseWeeks + depWeeks + issueWeeks) * riskMultiplier);
        int minWeeks = Math.max(2, calculatedWeeks - 2);
        int maxWeeks = calculatedWeeks + 2;
        
        // Team size calculation
        int minTeamSize = Math.max(2, Math.min(3, calculatedWeeks / 3));
        int maxTeamSize = Math.max(4, Math.min(8, calculatedWeeks / 2));
        
        // Effort calculation (40 hours per week per developer)
        int avgTeamSize = (minTeamSize + maxTeamSize) / 2;
        int minHours = minWeeks * avgTeamSize * 30; // 30 hrs/week (partial allocation)
        int maxHours = maxWeeks * maxTeamSize * 40;  // 40 hrs/week (full time)
        
        // Success rate based on risk (inverse relationship)
        int successRate = (int) Math.max(60, 95 - (riskScoreValue / 2));
        
        return new TimelineEstimate(minWeeks, maxWeeks, dependencyCount, issueCount,
            minTeamSize, maxTeamSize, minHours, maxHours, successRate);
    }
    
    private record TimelineEstimate(
        int minWeeks,
        int maxWeeks,
        int dependencyCount,
        int issueCount,
        int minTeamSize,
        int maxTeamSize,
        int minHours,
        int maxHours,
        int successRate
    ) {}
    
    private String generatePhaseBreakdown() {
        return """
            <div class="phase-breakdown-container">
                <h3>🔄 Phase-by-Phase Breakdown</h3>
                <div class="phases-grid">
                    <div class="phase-card">
                        <div class="phase-header">
                            <h4>Phase 1: Preparation</h4>
                            <div class="phase-duration">2 weeks</div>
                        </div>
                        <div class="phase-content">
                            <div class="phase-objectives">
                                <h5>Objectives</h5>
                                <ul>
                                    <li>Environment setup and backup</li>
                                    <li>Dependency analysis and planning</li>
                                    <li>Team training and documentation</li>
                                    <li>Tool configuration</li>
                                </ul>
                            </div>
                            <div class="phase-deliverables">
                                <h5>Deliverables</h5>
                                <ul>
                                    <li>Migration plan document</li>
                                    <li>Environment backup</li>
                                    <li>Dependency inventory</li>
                                    <li>Training materials</li>
                                </ul>
                            </div>
                            <div class="phase-resources">
                                <h5>Resources</h5>
                                <div class="resource-item">
                                    <span class="resource-role">Tech Lead</span>
                                    <span class="resource-allocation">50%</span>
                                </div>
                                <div class="resource-item">
                                    <span class="resource-role">Senior Developer</span>
                                    <span class="resource-allocation">100%</span>
                                </div>
                                <div class="resource-item">
                                    <span class="resource-role">DevOps Engineer</span>
                                    <span class="resource-allocation">25%</span>
                                </div>
                            </div>
                        </div>
                    </div>
                    
                    <div class="phase-card">
                        <div class="phase-header">
                            <h4>Phase 2: Dependency Migration</h4>
                            <div class="phase-duration">3 weeks</div>
                        </div>
                        <div class="phase-content">
                            <div class="phase-objectives">
                                <h5>Objectives</h5>
                                <ul>
                                    <li>Update build configuration</li>
                                    <li>Migrate dependencies to Jakarta EE</li>
                                    <li>Resolve version conflicts</li>
                                    <li>Initial compilation testing</li>
                                </ul>
                            </div>
                            <div class="phase-deliverables">
                                <h5>Deliverables</h5>
                                <ul>
                                    <li>Updated build files</li>
                                    <li>Dependency compatibility matrix</li>
                                    <li>Compilation baseline</li>
                                    <li>Issue tracking report</li>
                                </ul>
                            </div>
                            <div class="phase-resources">
                                <h5>Resources</h5>
                                <div class="resource-item">
                                    <span class="resource-role">Senior Developer</span>
                                    <span class="resource-allocation">100%</span>
                                </div>
                                <div class="resource-item">
                                    <span class="resource-role">Developer</span>
                                    <span class="resource-allocation">100%</span>
                                </div>
                                <div class="resource-item">
                                    <span class="resource-role">QA Engineer</span>
                                    <span class="resource-allocation">50%</span>
                                </div>
                            </div>
                        </div>
                    </div>
                    
                    <div class="phase-card">
                        <div class="phase-header">
                            <h4>Phase 3: Code Migration</h4>
                            <div class="phase-duration">4 weeks</div>
                        </div>
                        <div class="phase-content">
                            <div class="phase-objectives">
                                <h5>Objectives</h5>
                                <ul>
                                    <li>Update package imports</li>
                                    <li>Migrate configuration files</li>
                                    <li>Update annotations and descriptors</li>
                                    <li>Implement compatibility layers</li>
                                </ul>
                            </div>
                            <div class="phase-deliverables">
                                <h5>Deliverables</h5>
                                <ul>
                                    <li>Migrated source code</li>
                                    <li>Configuration updates</li>
                                    <li>Code review reports</li>
                                    <li>Migration documentation</li>
                                </ul>
                            </div>
                            <div class="phase-resources">
                                <h5>Resources</h5>
                                <div class="resource-item">
                                    <span class="resource-role">Tech Lead</span>
                                    <span class="resource-allocation">75%</span>
                                </div>
                                <div class="resource-item">
                                    <span class="resource-role">Senior Developer</span>
                                    <span class="resource-allocation">100%</span>
                                </div>
                                <div class="resource-item">
                                    <span class="resource-role">Developer</span>
                                    <span class="resource-allocation">100%</span>
                                </div>
                                <div class="resource-item">
                                    <span class="resource-role">Developer</span>
                                    <span class="resource-allocation">100%</span>
                                </div>
                            </div>
                        </div>
                    </div>
                    
                    <div class="phase-card">
                        <div class="phase-header">
                            <h4>Phase 4: Testing &amp; Validation</h4>
                            <div class="phase-duration">2 weeks</div>
                        </div>
                        <div class="phase-content">
                            <div class="phase-objectives">
                                <h5>Objectives</h5>
                                <ul>
                                    <li>Comprehensive testing</li>
                                    <li>Performance validation</li>
                                    <li>Security assessment</li>
                                    <li>User acceptance testing</li>
                                </ul>
                            </div>
                            <div class="phase-deliverables">
                                <h5>Deliverables</h5>
                                <ul>
                                    <li>Test execution reports</li>
                                    <li>Performance benchmarks</li>
                                    <li>Security scan results</li>
                                    <li>UAT sign-off</li>
                                </ul>
                            </div>
                            <div class="phase-resources">
                                <h5>Resources</h5>
                                <div class="resource-item">
                                    <span class="resource-role">QA Engineer</span>
                                    <span class="resource-allocation">100%</span>
                                </div>
                                <div class="resource-item">
                                    <span class="resource-role">Senior Developer</span>
                                    <span class="resource-allocation">50%</span>
                                </div>
                                <div class="resource-item">
                                    <span class="resource-role">DevOps Engineer</span>
                                    <span class="resource-allocation">75%</span>
                                </div>
                            </div>
                        </div>
                    </div>
                    
                    <div class="phase-card">
                        <div class="phase-header">
                            <h4>Phase 5: Deployment &amp; Rollout</h4>
                            <div class="phase-duration">1 week</div>
                        </div>
                        <div class="phase-content">
                            <div class="phase-objectives">
                                <h5>Objectives</h5>
                                <ul>
                                    <li>Production deployment</li>
                                    <li>Monitoring setup</li>
                                    <li>Performance tuning</li>
                                    <li>Documentation finalization</li>
                                </ul>
                            </div>
                            <div class="phase-deliverables">
                                <h5>Deliverables</h5>
                                <ul>
                                    <li>Production deployment</li>
                                    <li>Monitoring dashboards</li>
                                    <li>Final documentation</li>
                                    <li>Lessons learned report</li>
                                </ul>
                            </div>
                            <div class="phase-resources">
                                <h5>Resources</h5>
                                <div class="resource-item">
                                    <span class="resource-role">Tech Lead</span>
                                    <span class="resource-allocation">100%</span>
                                </div>
                                <div class="resource-item">
                                    <span class="resource-role">DevOps Engineer</span>
                                    <span class="resource-allocation">100%</span>
                                </div>
                                <div class="resource-item">
                                    <span class="resource-role">Senior Developer</span>
                                    <span class="resource-allocation">25%</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            """;
    }
    
    private String generateResourceAllocation() {
        return """
            <div class="resource-allocation-container">
                <h3>👥 Resource Allocation Summary</h3>
                <div class="resource-overview">
                    <div class="resource-category">
                        <h4>Development Team</h4>
                        <div class="resource-breakdown">
                            <div class="resource-role-item">
                                <span class="role-name">Technical Lead</span>
                                <div class="role-details">
                                    <span class="role-level">Senior</span>
                                    <span class="role-effort">320 hours</span>
                                    <span class="role-cost">$48,000</span>
                                </div>
                            </div>
                            <div class="resource-role-item">
                                <span class="role-name">Senior Developer</span>
                                <div class="role-details">
                                    <span class="role-level">Senior</span>
                                    <span class="role-effort">640 hours</span>
                                    <span class="role-cost">$96,000</span>
                                </div>
                            </div>
                            <div class="resource-role-item">
                                <span class="role-name">Developer</span>
                                <div class="role-details">
                                    <span class="role-level">Mid</span>
                                    <span class="role-effort">640 hours</span>
                                    <span class="role-cost">$80,000</span>
                                </div>
                            </div>
                            <div class="resource-role-item">
                                <span class="role-name">Junior Developer</span>
                                <div class="role-details">
                                    <span class="role-level">Junior</span>
                                    <span class="role-effort">320 hours</span>
                                    <span class="role-cost">$32,000</span>
                                </div>
                            </div>
                        </div>
                    </div>
                    
                    <div class="resource-category">
                        <h4>Support Team</h4>
                        <div class="resource-breakdown">
                            <div class="resource-role-item">
                                <span class="role-name">QA Engineer</span>
                                <div class="role-details">
                                    <span class="role-level">Senior</span>
                                    <span class="role-effort">400 hours</span>
                                    <span class="role-cost">$60,000</span>
                                </div>
                            </div>
                            <div class="resource-role-item">
                                <span class="role-name">DevOps Engineer</span>
                                <div class="role-details">
                                    <span class="role-level">Senior</span>
                                    <span class="role-effort">320 hours</span>
                                    <span class="role-cost">$48,000</span>
                                </div>
                            </div>
                            <div class="resource-role-item">
                                <span class="role-name">Technical Writer</span>
                                <div class="role-details">
                                    <span class="role-level">Mid</span>
                                    <span class="role-effort">160 hours</span>
                                    <span class="role-cost">$24,000</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                
                <div class="resource-summary">
                    <div class="summary-metrics">
                        <div class="metric-item">
                            <span class="metric-label">Total Team Size</span>
                            <span class="metric-value">8 people</span>
                        </div>
                        <div class="metric-item">
                            <span class="metric-label">Total Effort</span>
                            <span class="metric-value">2,800 hours</span>
                        </div>
                        <div class="metric-item">
                            <span class="metric-label">Estimated Cost</span>
                            <span class="metric-value">$388,000</span>
                        </div>
                        <div class="metric-item">
                            <span class="metric-label">Duration</span>
                            <span class="metric-value">12 weeks</span>
                        </div>
                    </div>
                </div>
            </div>
            """;
    }
    
    private String generateTimelineEstimates() {
        return """
            <div class="timeline-container">
                <h3>📅 Timeline Estimates</h3>
                <div class="timeline-grid">
                    <div class="timeline-week">
                        <div class="week-header">Week 1-2</div>
                        <div class="week-content">
                            <div class="week-tasks">
                                <h5>Preparation Phase</h5>
                                <ul>
                                    <li>Environment setup and backup</li>
                                    <li>Dependency analysis</li>
                                    <li>Team training</li>
                                    <li>Migration planning</li>
                                </ul>
                            </div>
                            <div class="week-milestone">
                                <span class="milestone-complete">✓</span>
                                <span>Migration Plan Approved</span>
                            </div>
                        </div>
                    </div>
                    
                    <div class="timeline-week">
                        <div class="week-header">Week 3-5</div>
                        <div class="week-content">
                            <div class="week-tasks">
                                <h5>Dependency Migration</h5>
                                <ul>
                                    <li>Build configuration updates</li>
                                    <li>Dependency migration</li>
                                    <li>Version conflict resolution</li>
                                    <li>Initial testing</li>
                                </ul>
                            </div>
                            <div class="week-milestone">
                                <span class="milestone-pending">○</span>
                                <span>Dependencies Updated</span>
                            </div>
                        </div>
                    </div>
                    
                    <div class="timeline-week">
                        <div class="week-header">Week 6-9</div>
                        <div class="week-content">
                            <div class="week-tasks">
                                <h5>Code Migration</h5>
                                <ul>
                                    <li>Package import updates</li>
                                    <li>Configuration migration</li>
                                    <li>Annotation updates</li>
                                    <li>Compatibility layer implementation</li>
                                </ul>
                            </div>
                            <div class="week-milestone">
                                <span class="milestone-pending">○</span>
                                <span>Code Migration Complete</span>
                            </div>
                        </div>
                    </div>
                    
                    <div class="timeline-week">
                        <div class="week-header">Week 10-11</div>
                        <div class="week-content">
                            <div class="week-tasks">
                                <h5>Testing &amp; Validation</h5>
                                <ul>
                                    <li>Comprehensive testing</li>
                                    <li>Performance validation</li>
                                    <li>Security assessment</li>
                                    <li>UAT execution</li>
                                </ul>
                            </div>
                            <div class="week-milestone">
                                <span class="milestone-pending">○</span>
                                <span>Testing Complete</span>
                            </div>
                        </div>
                    </div>
                    
                    <div class="timeline-week">
                        <div class="week-header">Week 12</div>
                        <div class="week-content">
                            <div class="week-tasks">
                                <h5>Deployment &amp; Rollout</h5>
                                <ul>
                                    <li>Production deployment</li>
                                    <li>Monitoring setup</li>
                                    <li>Performance tuning</li>
                                    <li>Documentation finalization</li>
                                </ul>
                            </div>
                            <div class="week-milestone">
                                <span class="milestone-pending">○</span>
                                <span>Go Live</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            """;
    }
    
    private String generateRiskMitigation() {
        return """
            <div class="risk-mitigation-container">
                <h3>🛡️ Risk Mitigation Strategies</h3>
                <div class="mitigation-grid">
                    <div class="mitigation-card">
                        <h4>🔄 Technical Risks</h4>
                        <div class="mitigation-strategies">
                            <div class="risk-item">
                                <span class="risk-name">Version Conflicts</span>
                                <span class="risk-level high">High</span>
                            </div>
                            <div class="strategy-item">
                                <span class="strategy-name">Dependency Testing</span>
                                <span class="strategy-effectiveness">High</span>
                            </div>
                            <div class="strategy-item">
                                <span class="strategy-name">Staging Environment</span>
                                <span class="strategy-effectiveness">High</span>
                            </div>
                            <div class="strategy-item">
                                <span class="strategy-name">Rollback Plan</span>
                                <span class="strategy-effectiveness">Medium</span>
                            </div>
                        </div>
                    </div>
                    
                    <div class="mitigation-card">
                        <h4>⏰ Timeline Risks</h4>
                        <div class="mitigation-strategies">
                            <div class="risk-item">
                                <span class="risk-name">Schedule Delays</span>
                                <span class="risk-level medium">Medium</span>
                            </div>
                            <div class="strategy-item">
                                <span class="strategy-name">Buffer Time</span>
                                <span class="strategy-effectiveness">High</span>
                            </div>
                            <div class="strategy-item">
                                <span class="strategy-name">Parallel Work</span>
                                <span class="strategy-effectiveness">Medium</span>
                            </div>
                            <div class="strategy-item">
                                <span class="strategy-name">Regular Checkpoints</span>
                                <span class="strategy-effectiveness">High</span>
                            </div>
                        </div>
                    </div>
                    
                    <div class="mitigation-card">
                        <h4>👥 Resource Risks</h4>
                        <div class="mitigation-strategies">
                            <div class="risk-item">
                                <span class="risk-name">Skill Gaps</span>
                                <span class="risk-level medium">Medium</span>
                            </div>
                            <div class="strategy-item">
                                <span class="strategy-name">Training Program</span>
                                <span class="strategy-effectiveness">High</span>
                            </div>
                            <div class="strategy-item">
                                <span class="strategy-name">Expert Consultation</span>
                                <span class="strategy-effectiveness">Medium</span>
                            </div>
                            <div class="strategy-item">
                                <span class="strategy-name">Documentation</span>
                                <span class="strategy-effectiveness">Low</span>
                            </div>
                        </div>
                    </div>
                    
                    <div class="mitigation-card">
                        <h4>🎯 Quality Risks</h4>
                        <div class="mitigation-strategies">
                            <div class="risk-item">
                                <span class="risk-name">Regressions</span>
                                <span class="risk-level high">High</span>
                            </div>
                            <div class="strategy-item">
                                <span class="strategy-name">Comprehensive Testing</span>
                                <span class="strategy-effectiveness">High</span>
                            </div>
                            <div class="strategy-item">
                                <span class="strategy-name">Code Reviews</span>
                                <span class="strategy-effectiveness">Medium</span>
                            </div>
                            <div class="strategy-item">
                                <span class="strategy-name">Automated Checks</span>
                                <span class="strategy-effectiveness">Medium</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            """;
    }
    
    @Override
    public boolean isApplicable() {
        return true; // Always show implementation roadmap
    }
    
    @Override
    public int getOrder() {
        return 60; // Show after risk analysis
    }
}
