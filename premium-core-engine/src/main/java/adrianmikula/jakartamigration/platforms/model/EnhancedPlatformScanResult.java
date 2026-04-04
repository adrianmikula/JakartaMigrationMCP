package adrianmikula.jakartamigration.platforms.model;

import java.util.List;
import java.util.Map;

/**
 * Enhanced platform scan result that includes deployment artifact counts
 */
public class EnhancedPlatformScanResult {
    private final List<String> detectedPlatforms;
    private final Map<String, Integer> deploymentArtifacts;
    private final Map<String, Integer> platformSpecificArtifacts;
    
    public EnhancedPlatformScanResult(List<String> detectedPlatforms, 
                                    Map<String, Integer> deploymentArtifacts,
                                    Map<String, Integer> platformSpecificArtifacts) {
        this.detectedPlatforms = detectedPlatforms;
        this.deploymentArtifacts = deploymentArtifacts;
        this.platformSpecificArtifacts = platformSpecificArtifacts;
    }
    
    public List<String> getDetectedPlatforms() {
        return detectedPlatforms;
    }
    
    public Map<String, Integer> getDeploymentArtifacts() {
        return deploymentArtifacts;
    }
    
    public Map<String, Integer> getPlatformSpecificArtifacts() {
        return platformSpecificArtifacts;
    }
    
    public int getTotalDeploymentCount() {
        return deploymentArtifacts.values().stream().mapToInt(Integer::intValue).sum();
    }
    
    public int getWarCount() {
        return deploymentArtifacts.getOrDefault("war", 0);
    }
    
    public int getEarCount() {
        return deploymentArtifacts.getOrDefault("ear", 0);
    }
    
    public int getJarCount() {
        return deploymentArtifacts.getOrDefault("jar", 0);
    }
    
    @Override
    public String toString() {
        return String.format("EnhancedPlatformScanResult{platforms=%s, artifacts=%s, platformSpecific=%s}", 
                           detectedPlatforms, deploymentArtifacts, platformSpecificArtifacts);
    }
}
