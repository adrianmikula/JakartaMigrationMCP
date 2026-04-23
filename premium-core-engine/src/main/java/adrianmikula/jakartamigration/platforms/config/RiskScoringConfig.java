package adrianmikula.jakartamigration.platforms.config;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;
import java.util.HashMap;

/**
 * Configuration class for risk scoring weights loaded from YAML.
 * All values are loaded from risk-scoring.yaml, with sensible defaults if not present.
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

    // Effort scoring weights and thresholds
    private final Map<String, Double> effortScoringWeights;
    private final Map<String, Integer> effortScoringThresholds;

    public RiskScoringConfig() {
        this.platformBaseRisks = new HashMap<>();
        this.deploymentArtifacts = new HashMap<>();
        this.configurationFiles = new HashMap<>();
        this.complexityMultipliers = new HashMap<>();
        this.baseCalculations = new HashMap<>();
        this.effortScoringWeights = new HashMap<>();
        this.effortScoringThresholds = new HashMap<>();

        // Load from YAML, falling back to defaults if not present
        loadFromYaml();
    }

    private void loadFromYaml() {
        try {
            InputStream inputStream = getClass().getClassLoader()
                    .getResourceAsStream("config/risk-scoring.yaml");

            if (inputStream == null) {
                // Fall back to defaults if YAML not found
                setDefaults();
                return;
            }

            Yaml yaml = new Yaml();
            Map<String, Object> config = yaml.load(inputStream);

            // Load platform risks
            if (config.containsKey("platformRisks")) {
                Map<String, Object> platformRisks = (Map<String, Object>) config.get("platformRisks");
                if (platformRisks.containsKey("baseScores")) {
                    Map<String, Object> baseScores = (Map<String, Object>) platformRisks.get("baseScores");
                    for (Map.Entry<String, Object> entry : baseScores.entrySet()) {
                        platformBaseRisks.put(entry.getKey(), ((Number) entry.getValue()).intValue());
                    }
                }
            }

            // Load deployment artifacts
            if (config.containsKey("deploymentArtifacts")) {
                Map<String, Object> artifacts = (Map<String, Object>) config.get("deploymentArtifacts");
                for (Map.Entry<String, Object> entry : artifacts.entrySet()) {
                    deploymentArtifacts.put(entry.getKey(), ((Number) entry.getValue()).intValue());
                }
            }

            // Load legacy calculations
            if (config.containsKey("legacyCalculations")) {
                Map<String, Object> legacy = (Map<String, Object>) config.get("legacyCalculations");
                for (Map.Entry<String, Object> entry : legacy.entrySet()) {
                    baseCalculations.put(entry.getKey(), ((Number) entry.getValue()).intValue());
                }
            }

            // Load effort scoring configuration
            if (config.containsKey("effortScoring")) {
                Map<String, Object> effortScoring = (Map<String, Object>) config.get("effortScoring");

                // Load weights
                if (effortScoring.containsKey("weights")) {
                    Map<String, Object> weights = (Map<String, Object>) effortScoring.get("weights");
                    for (Map.Entry<String, Object> entry : weights.entrySet()) {
                        effortScoringWeights.put(entry.getKey(), ((Number) entry.getValue()).doubleValue());
                    }
                }

                // Load thresholds
                if (effortScoring.containsKey("thresholds")) {
                    Map<String, Object> thresholds = (Map<String, Object>) effortScoring.get("thresholds");
                    for (Map.Entry<String, Object> entry : thresholds.entrySet()) {
                        effortScoringThresholds.put(entry.getKey(), ((Number) entry.getValue()).intValue());
                    }
                }
            }

            // Fill in any missing values with defaults
            setDefaultsForMissing();

        } catch (Exception e) {
            // Fall back to defaults on any error
            setDefaults();
        }
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
        deploymentArtifacts.put("ear", 4);
        deploymentArtifacts.put("war", 2);
        deploymentArtifacts.put("jar", 1);

        // Fixed calculation values (simplified approach)
        baseCalculations.put("platformMultiplier", 15);
        baseCalculations.put("maxPlatformRisk", 60);
        baseCalculations.put("maxTotalRisk", 100);
    }

    private void setDefaultsForMissing() {
        // Only set defaults for keys that are missing
        if (!platformBaseRisks.containsKey("websphere")) platformBaseRisks.put("websphere", 25);
        if (!platformBaseRisks.containsKey("liberty")) platformBaseRisks.put("liberty", 25);
        if (!platformBaseRisks.containsKey("wildfly")) platformBaseRisks.put("wildfly", 20);
        if (!platformBaseRisks.containsKey("jboss")) platformBaseRisks.put("jboss", 20);
        if (!platformBaseRisks.containsKey("tomcat")) platformBaseRisks.put("tomcat", 15);
        if (!platformBaseRisks.containsKey("jetty")) platformBaseRisks.put("jetty", 18);
        if (!platformBaseRisks.containsKey("default")) platformBaseRisks.put("default", 12);

        if (!deploymentArtifacts.containsKey("ear")) deploymentArtifacts.put("ear", 4);
        if (!deploymentArtifacts.containsKey("war")) deploymentArtifacts.put("war", 2);
        if (!deploymentArtifacts.containsKey("jar")) deploymentArtifacts.put("jar", 1);

        if (!baseCalculations.containsKey("platformMultiplier")) baseCalculations.put("platformMultiplier", 15);
        if (!baseCalculations.containsKey("maxPlatformRisk")) baseCalculations.put("maxPlatformRisk", 60);
        if (!baseCalculations.containsKey("maxTotalRisk")) baseCalculations.put("maxTotalRisk", 100);

        // Effort scoring defaults (updated for new weights)
        if (!effortScoringWeights.containsKey("automationScore")) effortScoringWeights.put("automationScore", 0.4);
        if (!effortScoringWeights.containsKey("testCoverageScore")) effortScoringWeights.put("testCoverageScore", 0.0);
        if (!effortScoringWeights.containsKey("organisationalDepsScore")) effortScoringWeights.put("organisationalDepsScore", 0.3);
        if (!effortScoringWeights.containsKey("projectSizeScore")) effortScoringWeights.put("projectSizeScore", 0.3);

        // Effort scoring thresholds
        if (!effortScoringThresholds.containsKey("maxOrganisationalDeps")) effortScoringThresholds.put("maxOrganisationalDeps", 100);
        if (!effortScoringThresholds.containsKey("maxTestCoverage")) effortScoringThresholds.put("maxTestCoverage", 100);
        if (!effortScoringThresholds.containsKey("maxProjectFiles")) effortScoringThresholds.put("maxProjectFiles", 10000);
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

    // Effort scoring getters
    public double getAutomationScoreWeight() {
        return effortScoringWeights.getOrDefault("automationScore", 0.33);
    }

    public double getTestCoverageScoreWeight() {
        return effortScoringWeights.getOrDefault("testCoverageScore", 0.33);
    }

    public double getOrganisationalDepsScoreWeight() {
        return effortScoringWeights.getOrDefault("organisationalDepsScore", 0.3);
    }

    public double getProjectSizeScoreWeight() {
        return effortScoringWeights.getOrDefault("projectSizeScore", 0.3);
    }

    public int getMaxOrganisationalDepsThreshold() {
        return effortScoringThresholds.getOrDefault("maxOrganisationalDeps", 100);
    }

    public int getMaxTestCoverage() {
        return effortScoringThresholds.getOrDefault("maxTestCoverage", 100);
    }

    public int getMaxProjectFilesThreshold() {
        return effortScoringThresholds.getOrDefault("maxProjectFiles", 10000);
    }
}
