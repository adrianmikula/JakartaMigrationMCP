package adrianmikula.jakartamigration.platforms.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for effort scoring weights in risk configuration.
 */
public class RiskScoringEffortTest {
    
    private RiskScoringConfig riskConfig;
    private PlatformConfigLoader configLoader;
    
    @BeforeEach
    void setUp() {
        configLoader = new PlatformConfigLoader();
        riskConfig = configLoader.getRiskScoringConfig();
    }
    
    @Test
    void testEffortScoringWeightsAreLoaded() {
        // Test that effort scoring weights are properly loaded from YAML
        
        // When
        double automationScoreWeight = riskConfig.getAutomationScoreWeight();
        double testCoverageScoreWeight = riskConfig.getTestCoverageScoreWeight();
        double organisationalDepsScoreWeight = riskConfig.getOrganisationalDepsScoreWeight();
        double projectSizeScoreWeight = riskConfig.getProjectSizeScoreWeight();
        
        // Then: All weights should be positive values
        assertTrue(automationScoreWeight >= 0, "Automation score weight should be non-negative");
        assertTrue(testCoverageScoreWeight >= 0, "Test coverage score weight should be non-negative");
        assertTrue(organisationalDepsScoreWeight >= 0, "Organisational deps score weight should be non-negative");
        assertTrue(projectSizeScoreWeight >= 0, "Project size score weight should be non-negative");
    }
    
    @Test
    void testEffortScoringThresholdsAreLoaded() {
        // Test that effort scoring thresholds are properly loaded from YAML
        
        // When
        int maxOrganisationalDeps = riskConfig.getMaxOrganisationalDepsThreshold();
        int maxTestCoverage = riskConfig.getMaxTestCoverage();
        int maxProjectFiles = riskConfig.getMaxProjectFilesThreshold();
        
        // Then: All thresholds should be positive values
        assertTrue(maxOrganisationalDeps > 0, "Max organisational deps threshold should be positive");
        assertTrue(maxTestCoverage > 0, "Max test coverage threshold should be positive");
        assertTrue(maxProjectFiles > 0, "Max project files threshold should be positive");
    }
    
    @Test
    void testDefaultEffortScoringValues() {
        // Test that default values are applied when YAML doesn't specify them
        
        // When: Get the configuration values
        double automationScoreWeight = riskConfig.getAutomationScoreWeight();
        
        // Then: Should match YAML value (which overrides default)
        assertEquals(0.20, automationScoreWeight, 0.001, "Automation score weight should be 0.20 from YAML");
    }
}
