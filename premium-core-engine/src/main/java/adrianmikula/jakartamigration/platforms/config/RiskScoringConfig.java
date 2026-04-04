package adrianmikula.jakartamigration.platforms.config;

import java.util.Map;
import java.util.HashMap;

/**
 * Configuration class for risk scoring weights loaded from YAML
 */
public class RiskScoringConfig {
    
    // Platform-specific base risks
    private final Map<String, Integer> platformBaseRisks;
    
    // Deployment artifact risk weights
    private final Map<String, Integer> deploymentArtifacts;
    
    // Configuration file risk weights
    private final Map<String, Integer> configurationFiles;
    
    // Complexity multipliers
    private final Map<String, Object> complexityMultipliers;
    
    // Base calculation weights
    private final Map<String, Integer> baseCalculations;
    
    public RiskScoringConfig() {
        this.platformBaseRisks = new HashMap<>();
        this.deploymentArtifacts = new HashMap<>();
        this.configurationFiles = new HashMap<>();
        this.complexityMultipliers = new HashMap<>();
        this.baseCalculations = new HashMap<>();
        
        // Set default values (will be overridden by YAML loading)
        setDefaults();
    }
    
    private void setDefaults() {
        // Platform base risks
        platformBaseRisks.put("websphere", 25);
        platformBaseRisks.put("liberty", 25);
        platformBaseRisks.put("wildfly", 20);
        platformBaseRisks.put("jboss", 20);
        platformBaseRisks.put("tomcat", 15);
        platformBaseRisks.put("jetty", 18);
        platformBaseRisks.put("default", 12);
        
        // Deployment artifacts
        deploymentArtifacts.put("ear", 8);
        deploymentArtifacts.put("war", 4);
        deploymentArtifacts.put("jar", 1);
        
        // Fixed calculation values (simplified approach)
        baseCalculations.put("platformMultiplier", 15);
        baseCalculations.put("maxPlatformRisk", 60);
        baseCalculations.put("maxTotalRisk", 100);
    }
    
    // Getters for all configuration values
    public int getPlatformBaseRisk(String platformName) {
        String platformKey = platformName.toLowerCase();
        return platformBaseRisks.getOrDefault(platformKey, platformBaseRisks.get("default"));
    }
    
    public int getDeploymentArtifactRisk(String artifactType) {
        return deploymentArtifacts.getOrDefault(artifactType.toLowerCase(), 0);
    }
    
    public int getPlatformMultiplier() {
        return baseCalculations.getOrDefault("platformMultiplier", 15);
    }
    
    public int getMaxPlatformRisk() {
        return baseCalculations.getOrDefault("maxPlatformRisk", 60);
    }
    
    public int getMaxTotalRisk() {
        return baseCalculations.getOrDefault("maxTotalRisk", 100);
    }
    
    // Simplified artifact complexity calculation
    public int calculateArtifactComplexityRisk(int totalArtifacts) {
        // Simple scaling: 1 point per artifact after the first 5, capped at 20
        if (totalArtifacts <= 5) return 0;
        return Math.min(totalArtifacts - 5, 20);
    }
    
    // Setters for YAML loading
    public void setPlatformBaseRisks(Map<String, Integer> platformBaseRisks) {
        this.platformBaseRisks.clear();
        this.platformBaseRisks.putAll(platformBaseRisks);
    }
    
    public void setDeploymentArtifacts(Map<String, Integer> deploymentArtifacts) {
        this.deploymentArtifacts.clear();
        this.deploymentArtifacts.putAll(deploymentArtifacts);
    }
}
