package adrianmikula.jakartamigration.pdfreporting.snippet;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify the CSS fix for risk trend analysis phase boxes.
 * Addresses issue where phase boxes had large solid colored boxes with no content.
 */
class RiskTrendCssFixTest {

    @Test
    void generatedHtmlShouldContainPhaseRiskElementsWithProperStructure() throws SnippetGenerationException {
        // Arrange
        RiskHeatMapSnippet snippet = new RiskHeatMapSnippet(null, null, null);

        // Act
        String html = snippet.generate();

        // Assert - Verify the HTML structure is correct
        assertNotNull(html);

        // Verify phase elements exist
        assertTrue(html.contains("Phase 1: Dependency Updates"), "Should contain Phase 1 heading");
        assertTrue(html.contains("Phase 2: Code Migration"), "Should contain Phase 2 heading");
        assertTrue(html.contains("Phase 3: Configuration Updates"), "Should contain Phase 3 heading");
        assertTrue(html.contains("Phase 4: Testing &amp; Validation"), "Should contain Phase 4 heading");

        // Verify risk labels exist inside phase-risk divs (not using inline percentage heights)
        assertTrue(html.contains("<div class=\"phase-risk high-risk\">"), "Should contain high-risk div without inline height");
        assertTrue(html.contains("<div class=\"phase-risk medium-risk\">"), "Should contain medium-risk div without inline height");
        assertTrue(html.contains("<div class=\"phase-risk low-risk\">"), "Should contain low-risk div without inline height");

        // Verify risk labels are present
        assertTrue(html.contains("<div class=\"risk-label\">High Risk</div>"), "Should contain High Risk label");
        assertTrue(html.contains("<div class=\"risk-label\">Medium Risk</div>"), "Should contain Medium Risk label");
        assertTrue(html.contains("<div class=\"risk-label\">Low Risk</div>"), "Should contain Low Risk label");

        // Verify legend elements exist
        assertTrue(html.contains("class=\"legend-color high-risk\""), "Should contain legend color for high risk");
        assertTrue(html.contains("class=\"legend-color medium-risk\""), "Should contain legend color for medium risk");
        assertTrue(html.contains("class=\"legend-color low-risk\""), "Should contain legend color for low risk");

        // Verify NO inline percentage heights (the fix removes these)
        assertFalse(html.contains("style=\"height: 80%;\""), "Should NOT contain inline 80% height");
        assertFalse(html.contains("style=\"height: 60%;\""), "Should NOT contain inline 60% height");
        assertFalse(html.contains("style=\"height: 40%;\""), "Should NOT contain inline 40% height");
        assertFalse(html.contains("style=\"height: 30%;\""), "Should NOT contain inline 30% height");
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
