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
                <p>Risk assessment by migration category based on actual scan findings.</p>

                %s
            </div>
            """,
            generateCategoryRiskAnalysis()
        );
    }

    private String generateCategoryRiskAnalysis() {
        if (riskScore == null || riskScore.findings() == null || riskScore.findings().isEmpty()) {
            return safelyFormat("""
                <div class="category-risk-container">
                    <h3>📊 Category Risk Analysis</h3>
                    <div class="no-data-message">
                        <p>Category risk analysis requires scan results. Run the Jakarta migration scanner to see category-level risk assessment.</p>
                    </div>
                </div>
                """);
        }

        CategoryRiskCalculator calculator = new CategoryRiskCalculator();
        var categoryRisks = calculator.calculateCategoryRisks(riskScore);

        if (categoryRisks.isEmpty()) {
            return safelyFormat("""
                <div class="category-risk-container">
                    <h3>📊 Category Risk Analysis</h3>
                    <div class="no-data-message">
                        <p>No category-specific findings detected in scan results.</p>
                    </div>
                </div>
                """);
        }

        StringBuilder categoryCards = new StringBuilder();
        for (CategoryRiskCalculator.CategoryRisk categoryRisk : categoryRisks.values()) {
            categoryCards.append(generateCategoryCard(categoryRisk));
        }

        return safelyFormat("""
            <div class="category-risk-container">
                <h3>📊 Category Risk Analysis</h3>
                <div class="category-grid">
                    %s
                </div>
            </div>
            """,
            categoryCards.toString()
        );
    }

    private String generateCategoryCard(CategoryRiskCalculator.CategoryRisk categoryRisk) {
        var category = categoryRisk.category();
        return safelyFormat("""
            <div class="category-card">
                <h4>%s %s</h4>
                <div class="category-risk-score %s">%.0f</div>
                <div class="category-details">
                    <div class="category-item">
                        <span>Findings Count</span>
                        <span class="risk-indicator %s">%d</span>
                    </div>
                    <div class="category-item">
                        <span>Risk Level</span>
                        <span class="risk-indicator %s">%s</span>
                    </div>
                </div>
            </div>
            """,
            category.getEmoji(),
            category.getDisplayName(),
            categoryRisk.getRiskLevelClass(),
            categoryRisk.score(),
            categoryRisk.getRiskLevelClass(),
            categoryRisk.findingCount(),
            categoryRisk.getRiskLevelClass(),
            categoryRisk.riskLevel()
        );
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
