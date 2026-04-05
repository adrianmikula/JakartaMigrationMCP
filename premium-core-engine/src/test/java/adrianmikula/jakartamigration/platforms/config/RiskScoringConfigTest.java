package adrianmikula.jakartamigration.platforms.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for risk scoring configuration loading from YAML
 */
public class RiskScoringConfigTest {
    
    private PlatformConfigLoader configLoader;
    
    @BeforeEach
    void setUp() {
        configLoader = new PlatformConfigLoader();
    }
    
    @Test
    void testRiskScoringConfigLoadedFromYAML() {
        RiskScoringConfig riskConfig = configLoader.getRiskScoringConfig();
        
        assertNotNull(riskConfig);
        
        // Test platform base risks from YAML
        assertEquals(25, riskConfig.getPlatformBaseRisk("websphere"));
        assertEquals(25, riskConfig.getPlatformBaseRisk("liberty"));
        assertEquals(20, riskConfig.getPlatformBaseRisk("wildfly"));
        assertEquals(20, riskConfig.getPlatformBaseRisk("jboss"));
        assertEquals(15, riskConfig.getPlatformBaseRisk("tomcat"));
        assertEquals(18, riskConfig.getPlatformBaseRisk("jetty"));
        assertEquals(12, riskConfig.getPlatformBaseRisk("unknown")); // default
        
        // Test deployment artifact risks from YAML
        assertEquals(4, riskConfig.getDeploymentArtifactRisk("ear"));
        assertEquals(2, riskConfig.getDeploymentArtifactRisk("war"));
        assertEquals(1, riskConfig.getDeploymentArtifactRisk("jar"));
        
        // Test base calculations (hardcoded defaults)
        assertEquals(15, riskConfig.getPlatformMultiplier());
        assertEquals(60, riskConfig.getMaxPlatformRisk());
        assertEquals(100, riskConfig.getMaxTotalRisk());
        
        // Test simplified artifact complexity calculation
        assertEquals(0, riskConfig.calculateArtifactComplexityRisk(5));  // No risk for 5 or fewer
        assertEquals(1, riskConfig.calculateArtifactComplexityRisk(6));  // 1 point for 6 artifacts
        assertEquals(5, riskConfig.calculateArtifactComplexityRisk(10)); // 5 points for 10 artifacts
        assertEquals(20, riskConfig.calculateArtifactComplexityRisk(30)); // Capped at 20
    }
    
    @Test
    void testRiskScoringConfigDefaults() {
        // Create a new config without YAML loading to test defaults
        RiskScoringConfig defaultConfig = new RiskScoringConfig();
        
        // Should have default values
        assertEquals(25, defaultConfig.getPlatformBaseRisk("websphere"));
        assertEquals(4, defaultConfig.getDeploymentArtifactRisk("ear"));
        assertEquals(15, defaultConfig.getPlatformMultiplier());
        assertEquals(100, defaultConfig.getMaxTotalRisk());
        assertEquals(0, defaultConfig.calculateArtifactComplexityRisk(3)); // Default calculation
    }
    
    @Test
    void testRiskScoringConfigCaseInsensitive() {
        RiskScoringConfig riskConfig = configLoader.getRiskScoringConfig();
        
        // Test case insensitive lookups
        assertEquals(25, riskConfig.getPlatformBaseRisk("WEBSPHERE"));
        assertEquals(25, riskConfig.getPlatformBaseRisk("WebSphere"));
        assertEquals(4, riskConfig.getDeploymentArtifactRisk("EAR"));
        assertEquals(2, riskConfig.getDeploymentArtifactRisk("WAR"));
        assertEquals(1, riskConfig.getDeploymentArtifactRisk("JAR"));
    }
}
