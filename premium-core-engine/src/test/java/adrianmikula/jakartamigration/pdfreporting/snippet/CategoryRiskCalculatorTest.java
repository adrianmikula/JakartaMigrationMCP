package adrianmikula.jakartamigration.pdfreporting.snippet;

import adrianmikula.jakartamigration.risk.RiskScoringService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for CategoryRiskCalculator.
 */
class CategoryRiskCalculatorTest {

    @Test
    @DisplayName("Should calculate category risks from findings")
    void shouldCalculateCategoryRisksFromFindings() {
        // Arrange
        CategoryRiskCalculator calculator = new CategoryRiskCalculator();
        List<RiskScoringService.RiskFinding> findings = List.of(
            new RiskScoringService.RiskFinding("servletJsp", "javaxServletImport", "Servlet import needs migration", "HIGH", 15),
            new RiskScoringService.RiskFinding("jpa", "entityWithJakartaId", "Jakarta ID annotation", "LOW", 3),
            new RiskScoringService.RiskFinding("cdi", "cdiBean", "CDI managed bean", "LOW", 3)
        );
        RiskScoringService.RiskScore riskScore = new RiskScoringService.RiskScore(
            50.0, "medium", "Medium", "#ffc107", Map.of(), findings
        );

        // Act
        Map<CategoryRiskCalculator.Category, CategoryRiskCalculator.CategoryRisk> results = 
            calculator.calculateCategoryRisks(riskScore);

        // Assert
        assertNotNull(results);
        assertTrue(results.containsKey(CategoryRiskCalculator.Category.WEB_LAYER));
        assertTrue(results.containsKey(CategoryRiskCalculator.Category.DATA_LAYER));
        assertTrue(results.containsKey(CategoryRiskCalculator.Category.BUSINESS_LAYER));
    }

    @Test
    @DisplayName("Should return empty map when risk score is null")
    void shouldReturnEmptyMapWhenRiskScoreIsNull() {
        // Arrange
        CategoryRiskCalculator calculator = new CategoryRiskCalculator();

        // Act
        Map<CategoryRiskCalculator.Category, CategoryRiskCalculator.CategoryRisk> results = 
            calculator.calculateCategoryRisks(null);

        // Assert
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Should return empty map when findings are null")
    void shouldReturnEmptyMapWhenFindingsAreNull() {
        // Arrange
        CategoryRiskCalculator calculator = new CategoryRiskCalculator();
        RiskScoringService.RiskScore riskScore = new RiskScoringService.RiskScore(
            50.0, "medium", "Medium", "#ffc107", Map.of(), null
        );

        // Act
        Map<CategoryRiskCalculator.Category, CategoryRiskCalculator.CategoryRisk> results = 
            calculator.calculateCategoryRisks(riskScore);

        // Assert
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Should map servlet findings to web layer")
    void shouldMapServletFindingsToWebLayer() {
        // Arrange
        CategoryRiskCalculator calculator = new CategoryRiskCalculator();
        List<RiskScoringService.RiskFinding> findings = List.of(
            new RiskScoringService.RiskFinding("servletJsp", "javaxServletImport", "Servlet import", "HIGH", 15)
        );
        RiskScoringService.RiskScore riskScore = new RiskScoringService.RiskScore(
            50.0, "medium", "Medium", "#ffc107", Map.of(), findings
        );

        // Act
        Map<CategoryRiskCalculator.Category, CategoryRiskCalculator.CategoryRisk> results = 
            calculator.calculateCategoryRisks(riskScore);

        // Assert
        assertTrue(results.containsKey(CategoryRiskCalculator.Category.WEB_LAYER));
        assertFalse(results.containsKey(CategoryRiskCalculator.Category.DATA_LAYER));
        assertEquals(1, results.get(CategoryRiskCalculator.Category.WEB_LAYER).findingCount());
    }

    @Test
    @DisplayName("Should map JPA findings to data layer")
    void shouldMapJpaFindingsToDataLayer() {
        // Arrange
        CategoryRiskCalculator calculator = new CategoryRiskCalculator();
        List<RiskScoringService.RiskFinding> findings = List.of(
            new RiskScoringService.RiskFinding("jpa", "entityWithJakartaId", "Jakarta ID", "LOW", 3)
        );
        RiskScoringService.RiskScore riskScore = new RiskScoringService.RiskScore(
            50.0, "medium", "Medium", "#ffc107", Map.of(), findings
        );

        // Act
        Map<CategoryRiskCalculator.Category, CategoryRiskCalculator.CategoryRisk> results = 
            calculator.calculateCategoryRisks(riskScore);

        // Assert
        assertTrue(results.containsKey(CategoryRiskCalculator.Category.DATA_LAYER));
        assertEquals(1, results.get(CategoryRiskCalculator.Category.DATA_LAYER).findingCount());
    }

    @Test
    @DisplayName("Should calculate higher score for critical findings")
    void shouldCalculateHigherScoreForCriticalFindings() {
        // Arrange
        CategoryRiskCalculator calculator = new CategoryRiskCalculator();
        List<RiskScoringService.RiskFinding> findings = List.of(
            new RiskScoringService.RiskFinding("servletJsp", "criticalIssue1", "Critical issue 1", "CRITICAL", 25),
            new RiskScoringService.RiskFinding("servletJsp", "criticalIssue2", "Critical issue 2", "CRITICAL", 25),
            new RiskScoringService.RiskFinding("servletJsp", "criticalIssue3", "Critical issue 3", "CRITICAL", 25)
        );
        RiskScoringService.RiskScore riskScore = new RiskScoringService.RiskScore(
            50.0, "medium", "Medium", "#ffc107", Map.of(), findings
        );

        // Act
        Map<CategoryRiskCalculator.Category, CategoryRiskCalculator.CategoryRisk> results = 
            calculator.calculateCategoryRisks(riskScore);

        // Assert
        assertFalse(results.isEmpty(), "Should have category risks");
        CategoryRiskCalculator.CategoryRisk webRisk = results.get(CategoryRiskCalculator.Category.WEB_LAYER);
        assertNotNull(webRisk, "Web layer risk should be calculated");
        assertEquals(75.0, webRisk.score(), 0.01, "3 critical findings should score 75");
        assertEquals("HIGH", webRisk.riskLevel(), "Score of 75 is HIGH risk level");
    }
}
