package adrianmikula.jakartamigration.pdfreporting.snippet;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify the CSS fix for risk trend analysis phase boxes.
 * Addresses issue where phase boxes had large solid colored boxes with no content.
 */
class RiskTrendCssFixTest {

    @Test
    void shouldDisplayNoDataWhenRiskScoreNull() throws SnippetGenerationException {
        // Arrange - RiskHeatMapSnippet should gracefully handle null riskScore
        RiskHeatMapSnippet snippet = new RiskHeatMapSnippet(null, null, null);

        // Act
        String html = snippet.generate();

        // Assert - Verify the HTML contains the no-data message
        assertNotNull(html);
        assertTrue(html.contains("Category risk analysis requires scan results"), 
            "Should display no-data message when riskScore is null");
        assertTrue(html.contains("category-risk-container"), "Should contain container div");
        assertTrue(html.contains("no-data-message"), "Should contain no-data-message class");
    }

    @Test
    void reportAssemblerShouldContainRequiredCssStyles() {
        // Arrange
        ReportAssembler assembler = new ReportAssembler();

        // Act - We can't directly test the private CSS generation,
        // but we can verify the report assembles without error
        String html = assembler.assembleReport(java.util.List.of(
            new HtmlSnippet() {
                @Override
                public String generate() { return "<div class='test'>Test Content</div>"; }
                @Override
                public String getSnippetName() { return "TestSnippet"; }
                @Override
                public boolean isApplicable() { return true; }
                @Override
                public int getOrder() { return 1; }
            }
        ), "Test Report");

        // Assert
        assertNotNull(html);
        assertTrue(html.contains("<style>"), "Should contain style section");
        assertTrue(html.contains("</style>"), "Should close style section");
        assertTrue(html.contains(".trend-phase"), "Should contain trend-phase CSS class");
        assertTrue(html.contains(".phase-risk"), "Should contain phase-risk CSS class");
        assertTrue(html.contains(".risk-label"), "Should contain risk-label CSS class");
        assertTrue(html.contains(".legend-color"), "Should contain legend-color CSS class");
        assertTrue(html.contains(".high-risk"), "Should contain high-risk CSS class");
        assertTrue(html.contains(".medium-risk"), "Should contain medium-risk CSS class");
        assertTrue(html.contains(".low-risk"), "Should contain low-risk CSS class");

        // Verify the CSS has proper height declarations (not percentage-based)
        assertTrue(html.contains("min-height: 200px"), "Should set min-height on trend-phase");
        assertTrue(html.contains("height: 120px"), "Should set fixed height on phase-risk");
        assertTrue(html.contains("display: flex"), "Should use flexbox layout");
    }
}
