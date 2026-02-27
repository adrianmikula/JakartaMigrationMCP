package adrianmikula.jakartamigration.intellij.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for UiTextLoader - verifies property file loading.
 */
class UiTextLoaderTest {

    @Test
    void testGetStrategyBenefits() {
        // Test loading benefits for different strategies
        String bigBangBenefits = UiTextLoader.getStrategyBenefits("big_bang");
        assertNotNull(bigBangBenefits);
        assertTrue(bigBangBenefits.contains("Migrate all javax dependencies"));
        
        String incrementalBenefits = UiTextLoader.getStrategyBenefits("incremental");
        assertNotNull(incrementalBenefits);
        assertTrue(incrementalBenefits.contains("Migrate dependencies incrementally"));
        
        String adapterBenefits = UiTextLoader.getStrategyBenefits("adapter");
        assertNotNull(adapterBenefits);
        assertTrue(adapterBenefits.contains("Maintain backward compatibility"));
    }
    
    @Test
    void testGetStrategyRisks() {
        // Test loading risks for different strategies
        String bigBangRisks = UiTextLoader.getStrategyRisks("big_bang");
        assertNotNull(bigBangRisks);
        assertTrue(bigBangRisks.contains("Higher risk"));
        
        String incrementalRisks = UiTextLoader.getStrategyRisks("incremental");
        assertNotNull(incrementalRisks);
        assertTrue(incrementalRisks.contains("Longer overall migration timeline"));
        
        String adapterRisks = UiTextLoader.getStrategyRisks("adapter");
        assertNotNull(adapterRisks);
        assertTrue(adapterRisks.contains("Additional code maintenance"));
    }
    
    @Test
    void testGetPhaseTitle() {
        // Test loading phase titles
        String title1 = UiTextLoader.getPhaseTitle("incremental", 1);
        assertEquals("Dependency Updates", title1);
        
        String title2 = UiTextLoader.getPhaseTitle("incremental", 2);
        assertEquals("Import Replacement", title2);
        
        String title3 = UiTextLoader.getPhaseTitle("big_bang", 1);
        assertEquals("Complete Migration", title3);
    }
    
    @Test
    void testGetPhaseDescription() {
        // Test loading phase descriptions
        String description = UiTextLoader.getPhaseDescription("incremental", 1);
        assertNotNull(description);
        assertTrue(description.contains("Incremental migration"));
        
        String bigBangDesc = UiTextLoader.getPhaseDescription("big_bang", 1);
        assertNotNull(bigBangDesc);
        assertTrue(bigBangDesc.contains("Big Bang migration"));
    }
    
    @Test
    void testGetPhaseStepsArray() {
        // Test loading phase steps as array
        String[] steps = UiTextLoader.getPhaseStepsArray("incremental", 1);
        assertNotNull(steps);
        assertTrue(steps.length > 0);
        assertTrue(steps[0].contains("Analyze dependency tree"));
        
        String[] bigBangSteps = UiTextLoader.getPhaseStepsArray("big_bang", 1);
        assertNotNull(bigBangSteps);
        assertTrue(bigBangSteps[0].contains("Update build files"));
    }
    
    @Test
    void testGetPhaseCount() {
        // Test getting the number of phases for each strategy
        int incrementalCount = UiTextLoader.getPhaseCount("incremental");
        assertEquals(4, incrementalCount);
        
        int transformCount = UiTextLoader.getPhaseCount("transform");
        assertEquals(4, transformCount);
        
        int microservicesCount = UiTextLoader.getPhaseCount("microservices");
        assertEquals(4, microservicesCount);
        
        int adapterCount = UiTextLoader.getPhaseCount("adapter");
        assertEquals(4, adapterCount);
        
        int bigBangCount = UiTextLoader.getPhaseCount("big_bang");
        assertEquals(1, bigBangCount);
    }
    
    @Test
    void testGetWithNewlines() {
        // Test that \n is converted to actual newlines
        String value = UiTextLoader.getWithNewlines("strategy.big_bang.benefits");
        assertNotNull(value);
        assertTrue(value.contains("\n")); // Should have actual newlines, not \n
    }
    
    @Test
    void testGetWithDefaultValue() {
        // Test default value when key doesn't exist
        String defaultValue = "default value";
        String result = UiTextLoader.get("nonexistent.key", defaultValue);
        assertEquals(defaultValue, result);
    }
    
    @Test
    void testHasKey() {
        // Test key existence check
        assertTrue(UiTextLoader.hasKey("strategy.big_bang.benefits"));
        assertTrue(UiTextLoader.hasKey("phase.incremental.1.title"));
        assertFalse(UiTextLoader.hasKey("nonexistent.key"));
    }
    
    @Test
    void testGetNonExistentKey() {
        // Test that null is returned for non-existent keys
        String result = UiTextLoader.get("nonexistent.key.12345");
        assertNull(result);
    }
    
    @Test
    void testAllStrategiesHaveBenefits() {
        // Verify all strategies have benefits defined
        String[] strategies = {"big_bang", "incremental", "transform", "microservices", "adapter"};
        for (String strategy : strategies) {
            String benefits = UiTextLoader.getStrategyBenefits(strategy);
            assertNotNull(benefits, "Benefits should not be null for " + strategy);
            assertFalse(benefits.isEmpty(), "Benefits should not be empty for " + strategy);
        }
    }
    
    @Test
    void testAllStrategiesHaveRisks() {
        // Verify all strategies have risks defined
        String[] strategies = {"big_bang", "incremental", "transform", "microservices", "adapter"};
        for (String strategy : strategies) {
            String risks = UiTextLoader.getStrategyRisks(strategy);
            assertNotNull(risks, "Risks should not be null for " + strategy);
            assertFalse(risks.isEmpty(), "Risks should not be empty for " + strategy);
        }
    }
    
    @Test
    void testAdapterStrategyPhases() {
        // Verify adapter strategy has correct phases
        assertEquals(4, UiTextLoader.getPhaseCount("adapter"));
        
        String phase1 = UiTextLoader.getPhaseTitle("adapter", 1);
        assertEquals("Adapter Interface Design", phase1);
        
        String phase2 = UiTextLoader.getPhaseTitle("adapter", 2);
        assertEquals("Adapter Implementation", phase2);
        
        String phase3 = UiTextLoader.getPhaseTitle("adapter", 3);
        assertEquals("Gradual Replacement", phase3);
        
        String phase4 = UiTextLoader.getPhaseTitle("adapter", 4);
        assertEquals("Remove Adapters", phase4);
    }
    
    @Test
    void testMicroservicesStrategyPhases() {
        // Verify microservices strategy has correct phases (not adapter pattern)
        assertEquals(4, UiTextLoader.getPhaseCount("microservices"));
        
        String phase1 = UiTextLoader.getPhaseTitle("microservices", 1);
        assertEquals("Service Inventory", phase1);
        
        String phase2 = UiTextLoader.getPhaseTitle("microservices", 2);
        assertEquals("Shared Libraries Migration", phase2);
        
        String phase3 = UiTextLoader.getPhaseTitle("microservices", 3);
        assertEquals("Service-by-Service Migration", phase3);
        
        String phase4 = UiTextLoader.getPhaseTitle("microservices", 4);
        assertEquals("Integration Testing", phase4);
    }
}
