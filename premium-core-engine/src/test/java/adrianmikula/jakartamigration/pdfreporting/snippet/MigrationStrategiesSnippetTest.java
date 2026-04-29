package adrianmikula.jakartamigration.pdfreporting.snippet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for MigrationStrategiesSnippet.
 */
class MigrationStrategiesSnippetTest {

    @Test
    @DisplayName("Should generate strategy cards when properties are available")
    void shouldGenerateStrategyCardsWhenPropertiesAvailable() throws SnippetGenerationException {
        // Arrange
        MigrationStrategiesSnippet snippet = new MigrationStrategiesSnippet();

        // Act
        String html = snippet.generate();

        // Assert
        assertNotNull(html);
        assertTrue(snippet.isApplicable(), "Snippet should be applicable when properties are loaded");
        assertTrue(html.contains("Migration Strategies"), "Should contain section title");
        assertTrue(html.contains("Big Bang"), "Should contain Big Bang strategy");
        assertTrue(html.contains("Incremental"), "Should contain Incremental strategy");
        assertTrue(html.contains("Transform"), "Should contain Transform strategy");
        assertTrue(html.contains("Microservices"), "Should contain Microservices strategy");
        assertTrue(html.contains("Adapter Pattern"), "Should contain Adapter Pattern strategy");
        assertTrue(html.contains("Strangler"), "Should contain Strangler strategy");
    }

    @Test
    @DisplayName("Should display strategy benefits and risks")
    void shouldDisplayStrategyBenefitsAndRisks() throws SnippetGenerationException {
        // Arrange
        MigrationStrategiesSnippet snippet = new MigrationStrategiesSnippet();

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("Benefits"), "Should contain Benefits section");
        assertTrue(html.contains("Risks"), "Should contain Risks section");
        assertTrue(html.contains("✓"), "Should contain benefits icon");
        assertTrue(html.contains("⚠"), "Should contain risks icon");
    }

    @Test
    @DisplayName("Should not contain hardcoded strategy data")
    void shouldNotContainHardcodedStrategyData() throws SnippetGenerationException {
        // Arrange
        MigrationStrategiesSnippet snippet = new MigrationStrategiesSnippet();

        // Act
        String html = snippet.generate();

        // Assert - verify no hardcoded descriptions
        assertFalse(html.contains("Migrate modules incrementally to reduce risk"), 
            "Should not contain hardcoded strategy descriptions from old implementation");
    }

    @Test
    @DisplayName("Should have correct order in snippet sequence")
    void shouldHaveCorrectOrder() {
        // Arrange
        MigrationStrategiesSnippet snippet = new MigrationStrategiesSnippet();

        // Act
        int order = snippet.getOrder();

        // Assert
        assertEquals(50, order, "Should have order 50 to show after Risk Heat Map");
    }
}
