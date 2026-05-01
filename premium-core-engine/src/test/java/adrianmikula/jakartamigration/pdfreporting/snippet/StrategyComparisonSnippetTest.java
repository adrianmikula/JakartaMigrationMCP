package adrianmikula.jakartamigration.pdfreporting.snippet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for StrategyComparisonSnippet.
 */
class StrategyComparisonSnippetTest {

    @Test
    @DisplayName("Should generate comparison table when properties are available")
    void shouldGenerateComparisonTableWhenPropertiesAvailable() throws SnippetGenerationException {
        // Arrange
        StrategyComparisonSnippet snippet = new StrategyComparisonSnippet();

        // Act
        String html = snippet.generate();

        // Assert
        assertNotNull(html);
        assertTrue(snippet.isApplicable(), "Snippet should be applicable when properties are loaded");
        assertTrue(html.contains("Strategy Comparison"), "Should contain section title");
        assertTrue(html.contains("comparison-table"), "Should contain comparison table class");
    }

    @Test
    @DisplayName("Should display all 6 strategies in table headers")
    void shouldDisplayAllSixStrategiesInTableHeaders() throws SnippetGenerationException {
        // Arrange
        StrategyComparisonSnippet snippet = new StrategyComparisonSnippet();

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("Big Bang"), "Should contain Big Bang header");
        assertTrue(html.contains("Incremental"), "Should contain Incremental header");
        assertTrue(html.contains("Transform"), "Should contain Transform header");
        assertTrue(html.contains("Microservices"), "Should contain Microservices header");
        assertTrue(html.contains("Adapter Pattern"), "Should contain Adapter Pattern header");
        assertTrue(html.contains("Strangler"), "Should contain Strangler header");
    }

    @Test
    @DisplayName("Should display all risk categories as rows")
    void shouldDisplayAllRiskCategoriesAsRows() throws SnippetGenerationException {
        // Arrange
        StrategyComparisonSnippet snippet = new StrategyComparisonSnippet();

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("Development Effort"), "Should contain Development Effort category");
        assertTrue(html.contains("Dependency Risks"), "Should contain Dependency Risks category");
        assertTrue(html.contains("Tech Debt Accumulation"), "Should contain Tech Debt Accumulation category");
        assertTrue(html.contains("Test Effort"), "Should contain Test Effort category");
        assertTrue(html.contains("Backwards Compatibility"), "Should contain Backwards Compatibility category");
        assertTrue(html.contains("Production Rollout Risk"), "Should contain Production Rollout Risk category");
    }

    @Test
    @DisplayName("Should display risk levels in table cells")
    void shouldDisplayRiskLevelsInTableCells() throws SnippetGenerationException {
        // Arrange
        StrategyComparisonSnippet snippet = new StrategyComparisonSnippet();

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("High") || html.contains("Medium") || html.contains("Low"), 
            "Should contain risk level indicators");
        assertTrue(html.contains("risk-cell"), "Should contain risk-cell class");
    }

    @Test
    @DisplayName("Should display comparison legend")
    void shouldDisplayComparisonLegend() throws SnippetGenerationException {
        // Arrange
        StrategyComparisonSnippet snippet = new StrategyComparisonSnippet();

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("comparison-legend"), "Should contain legend");
        assertTrue(html.contains("High Risk"), "Should contain High Risk legend item");
        assertTrue(html.contains("Medium Risk"), "Should contain Medium Risk legend item");
        assertTrue(html.contains("Low Risk"), "Should contain Low Risk legend item");
    }

    @Test
    @DisplayName("Should not contain numerical scores or costs")
    void shouldNotContainNumericalScoresOrCosts() throws SnippetGenerationException {
        // Arrange
        StrategyComparisonSnippet snippet = new StrategyComparisonSnippet();

        // Act
        String html = snippet.generate();

        // Assert - verify no numerical estimates
        assertFalse(html.contains("weeks") || html.contains("days") || html.contains("hours"), 
            "Should not contain time estimates");
        assertFalse(html.contains("$") || html.contains("cost") || html.contains("budget"), 
            "Should not contain cost estimates");
    }

    @Test
    @DisplayName("Should have correct order in snippet sequence")
    void shouldHaveCorrectOrder() {
        // Arrange
        StrategyComparisonSnippet snippet = new StrategyComparisonSnippet();

        // Act
        int order = snippet.getOrder();

        // Assert
        assertEquals(51, order, "Should have order 51 to show after Migration Strategies");
    }
}
