package adrianmikula.jakartamigration.intellij.service;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for calculating risk scores based on scan findings and dependency
 * analysis.
 * Uses YAML configuration for risk levels and weights.
 */
public class RiskScoringService {

    private static RiskScoringService instance;
    private final Map<String, RiskConfig> riskConfigs;
    private final Map<String, CategoryConfig> categoryConfigs;
    private final Map<String, Object> calculationConfig;

    public static class RiskConfig {
        public String displayName;
        public double baseWeight;
        public Map<String, FindingConfig> findings;
    }

    public static class FindingConfig {
        public String riskLevel;
        public String description;
    }

    public static class CategoryConfig {
        public int minScore;
        public int maxScore;
        public String label;
        public String color;

        public CategoryConfig() {
        }

        public CategoryConfig(int minScore, int maxScore, String label, String color) {
            this.minScore = minScore;
            this.maxScore = maxScore;
            this.label = label;
            this.color = color;
        }
    }

    public record RiskScore(
            double totalScore,
            String category,
            String categoryLabel,
            String categoryColor,
            Map<String, Integer> componentScores,
            List<RiskFinding> findings) {
    }

    public record RiskFinding(
            String scanType,
            String findingType,
            String description,
            String riskLevel,
            int score) {
    }

    private RiskScoringService() {
        riskConfigs = new ConcurrentHashMap<>();
        categoryConfigs = new ConcurrentHashMap<>();
        calculationConfig = new HashMap<>();
        loadConfiguration();
    }

    public static synchronized RiskScoringService getInstance() {
        if (instance == null) {
            instance = new RiskScoringService();
        }
        return instance;
    }

    private void loadConfiguration() {
        try {
            // Try to load from classpath
            InputStream inputStream = getClass().getClassLoader()
                    .getResourceAsStream("config/risk-scoring.yaml");

            if (inputStream == null) {
                // Try file system
                File configFile = new File("premium-intellij-plugin/src/main/resources/config/risk-scoring.yaml");
                if (configFile.exists()) {
                    inputStream = new FileInputStream(configFile);
                } else {
                    throw new RuntimeException("CRITICAL: risk-scoring.yaml not found in classpath or filesystem.");
                }
            }

            Yaml yaml = new Yaml();
            Map<String, Object> config = yaml.load(inputStream);

            // Load scan risks
            if (config.containsKey("scanRisks")) {
                Map<String, Object> scanRisks = (Map<String, Object>) config.get("scanRisks");
                for (Map.Entry<String, Object> entry : scanRisks.entrySet()) {
                    Map<String, Object> scanConfig = (Map<String, Object>) entry.getValue();
                    RiskConfig rc = new RiskConfig();
                    rc.displayName = (String) scanConfig.get("displayName");
                    rc.baseWeight = ((Number) scanConfig.getOrDefault("baseWeight", 10)).doubleValue();
                    rc.findings = new HashMap<>();

                    if (scanConfig.containsKey("findings")) {
                        Map<String, Object> findings = (Map<String, Object>) scanConfig.get("findings");
                        for (Map.Entry<String, Object> finding : findings.entrySet()) {
                            Map<String, Object> findingConfig = (Map<String, Object>) finding.getValue();
                            FindingConfig fc = new FindingConfig();
                            fc.riskLevel = (String) findingConfig.get("riskLevel");
                            fc.description = (String) findingConfig.get("description");
                            rc.findings.put(finding.getKey(), fc);
                        }
                    }
                    riskConfigs.put(entry.getKey(), rc);
                }
            }

            // Load category configs
            if (config.containsKey("riskCategories")) {
                Map<String, Object> categories = (Map<String, Object>) config.get("riskCategories");
                for (Map.Entry<String, Object> entry : categories.entrySet()) {
                    Map<String, Object> catConfig = (Map<String, Object>) entry.getValue();
                    CategoryConfig cc = new CategoryConfig();
                    cc.minScore = (Integer) catConfig.get("minScore");
                    cc.maxScore = (Integer) catConfig.get("maxScore");
                    cc.label = (String) catConfig.get("label");
                    cc.color = (String) catConfig.get("color");
                    categoryConfigs.put(entry.getKey(), cc);
                }
            }

            // Load calculation config
            if (config.containsKey("riskCalculation")) {
                calculationConfig.putAll((Map<String, Object>) config.get("riskCalculation"));
            }

        } catch (Exception e) {
            throw new RuntimeException("CRITICAL: Failed to load risk-scoring.yaml. Risk calculation cannot proceed.",
                    e);
        }
    }

    /**
     * Calculates overall risk score based on scan findings and dependency issues.
     */
    public RiskScore calculateRiskScore(
            Map<String, List<RiskFinding>> scanFindings,
            Map<String, Integer> dependencyIssues) {

        Map<String, Integer> componentScores = new HashMap<>();
        List<RiskFinding> allFindings = new ArrayList<>();

        // Calculate scan findings score
        double rawScanScore = 0;
        for (Map.Entry<String, List<RiskFinding>> entry : scanFindings.entrySet()) {
            double scanTypeScore = 0;
            RiskConfig config = riskConfigs.get(entry.getKey());

            if (config == null) {
                throw new IllegalArgumentException("Missing risk configuration for scan type: " + entry.getKey());
            }

            for (RiskFinding finding : entry.getValue()) {
                scanTypeScore += finding.score();
                allFindings.add(finding);
            }

            if (config.baseWeight <= 0) {
                throw new IllegalArgumentException("Invalid baseWeight (<= 0) for scan type: " + entry.getKey());
            }

            // Apply base weight as a multiplier for normalization
            scanTypeScore = (scanTypeScore * config.baseWeight) / 10.0;
            rawScanScore += scanTypeScore;
        }

        // Calculate dependency issues score
        double rawDepScore = 0;
        for (Map.Entry<String, Integer> entry : dependencyIssues.entrySet()) {
            rawDepScore += entry.getValue();
        }

        // Code complexity (placeholder - could be enhanced with actual complexity
        // metrics)
        double rawComplexityScore = 2.0; // Reduced base complexity for 0-100 scale

        // Get weights from calculation config
        @SuppressWarnings("unchecked")
        Map<String, Double> weights = (Map<String, Double>) calculationConfig.get("componentWeights");
        if (weights == null) {
            throw new IllegalArgumentException("Missing 'componentWeights' in risk-scoring.yaml");
        }

        Number scanWeightNum = (Number) weights.get("scanFindings");
        Number depWeightNum = (Number) weights.get("dependencyIssues");
        Number complexityWeightNum = (Number) weights.get("codeComplexity");

        if (scanWeightNum == null || depWeightNum == null || complexityWeightNum == null) {
            throw new IllegalArgumentException(
                    "Missing one or more required weights (scanFindings, dependencyIssues, codeComplexity) in risk-scoring.yaml");
        }

        double scanWeight = scanWeightNum.doubleValue();
        double depWeight = depWeightNum.doubleValue();
        double complexityWeight = complexityWeightNum.doubleValue();

        // Get max total score for normalization
        Number maxScoreNum = (Number) calculationConfig.get("maxTotalScore");
        double maxTotalScore = maxScoreNum != null ? maxScoreNum.doubleValue() : 100.0;

        // Weighted total (normalized to 0-100 scale)
        double totalScore = (rawScanScore * scanWeight) +
                (rawDepScore * depWeight) +
                (rawComplexityScore * complexityWeight);
        
        // Normalize to 0-100 scale
        totalScore = Math.min(totalScore, maxTotalScore);
        totalScore = Math.max(totalScore, 0);

        // Get category
        String category = getCategoryForScore(totalScore);
        CategoryConfig catConfig = categoryConfigs.get(category);

        return new RiskScore(
                totalScore,
                category,
                catConfig != null ? catConfig.label : "Unknown",
                catConfig != null ? catConfig.color : "#888888",
                componentScores,
                allFindings);
    }

    private String getCategoryForScore(double score) {
        for (Map.Entry<String, CategoryConfig> entry : categoryConfigs.entrySet()) {
            if (score >= entry.getValue().minScore && score <= entry.getValue().maxScore) {
                return entry.getKey();
            }
        }
        return "trivial"; // Default category
    }

    /**
     * Gets CategoryConfig for a specific score.
     * Used by UI components to display risk categories.
     */
    public CategoryConfig getCategoryConfigForScore(double score) {
        String categoryKey = getCategoryForScore(score);
        return categoryConfigs.get(categoryKey);
    }

    /**
     * Gets the risk level for a specific finding type.
     */
    public String getRiskLevelForFinding(String scanType, String findingType) {
        RiskConfig config = riskConfigs.get(scanType);
        if (config == null) {
            throw new IllegalArgumentException("No risk configuration found for scan type: " + scanType);
        }
        if (config.findings == null) {
            throw new IllegalArgumentException("No findings configuration for scan type: " + scanType);
        }

        FindingConfig fc = config.findings.get(findingType);
        if (fc == null) {
            throw new IllegalArgumentException(
                    "No risk level defined for finding: " + findingType + " in scan type: " + scanType);
        }
        return fc.riskLevel;
    }

    /**
     * Gets all risk configurations.
     */
    public Map<String, RiskConfig> getRiskConfigs() {
        return new HashMap<>(riskConfigs);
    }

    /**
     * Gets category configuration for a specific category.
     */
    public CategoryConfig getCategoryConfig(String category) {
        return categoryConfigs.get(category);
    }
}
