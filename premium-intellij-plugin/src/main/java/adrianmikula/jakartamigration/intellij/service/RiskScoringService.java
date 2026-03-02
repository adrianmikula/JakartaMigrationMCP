package adrianmikula.jakartamigration.intellij.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for calculating risk scores based on scan findings and dependency analysis.
 * Uses YAML configuration for risk levels and weights.
 */
public class RiskScoringService {
    
    private static RiskScoringService instance;
    private final Map<String, RiskConfig> riskConfigs;
    private final Map<String, CategoryConfig> categoryConfigs;
    private final Map<String, Object> calculationConfig;
    
    public static class RiskConfig {
        public String displayName;
        public int baseWeight;
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
        
        public CategoryConfig() {}
        
        public CategoryConfig(int minScore, int maxScore, String label, String color) {
            this.minScore = minScore;
            this.maxScore = maxScore;
            this.label = label;
            this.color = color;
        }
    }
    
    public record RiskScore(
        int totalScore,
        String category,
        String categoryLabel,
        String categoryColor,
        Map<String, Integer> componentScores,
        List<RiskFinding> findings
    ) {}
    
    public record RiskFinding(
        String scanType,
        String findingType,
        String description,
        String riskLevel,
        int score
    ) {}
    
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
                    loadDefaults();
                    return;
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
                    rc.baseWeight = (Integer) scanConfig.getOrDefault("baseWeight", 10);
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
            loadDefaults();
        }
    }
    
    private void loadDefaults() {
        // Default category configs
        categoryConfigs.put("trivial", new CategoryConfig(0, 10, "Trivial", "#28a745"));
        categoryConfigs.put("low", new CategoryConfig(11, 30, "Low", "#17a2b8"));
        categoryConfigs.put("medium", new CategoryConfig(31, 50, "Medium", "#ffc107"));
        categoryConfigs.put("high", new CategoryConfig(51, 75, "High", "#fd7e14"));
        categoryConfigs.put("extreme", new CategoryConfig(76, 100, "Extreme", "#dc3545"));
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
        int scanScore = 0;
        for (Map.Entry<String, List<RiskFinding>> entry : scanFindings.entrySet()) {
            int scanTypeScore = 0;
            RiskConfig config = riskConfigs.get(entry.getKey());
            
            for (RiskFinding finding : entry.getValue()) {
                scanTypeScore += finding.score();
                allFindings.add(finding);
            }
            
            if (config != null && config.baseWeight > 0) {
                scanTypeScore = Math.min(scanTypeScore, config.baseWeight * 10);
            }
            scanScore += scanTypeScore;
        }
        componentScores.put("scanFindings", Math.min(scanScore, 40));
        
        // Calculate dependency issues score
        int depScore = 0;
        for (Map.Entry<String, Integer> entry : dependencyIssues.entrySet()) {
            depScore += entry.getValue();
        }
        componentScores.put("dependencyIssues", Math.min(depScore, 40));
        
        // Code complexity (placeholder - could be enhanced)
        componentScores.put("codeComplexity", 5);
        
        // Calculate total
        int totalScore = componentScores.values().stream()
                .mapToInt(Integer::intValue)
                .sum();
        totalScore = Math.min(totalScore, 100);
        
        // Get category
        String category = getCategoryForScore(totalScore);
        CategoryConfig catConfig = categoryConfigs.get(category);
        
        return new RiskScore(
            totalScore,
            category,
            catConfig != null ? catConfig.label : "Unknown",
            catConfig != null ? catConfig.color : "#888888",
            componentScores,
            allFindings
        );
    }
    
    private String getCategoryForScore(int score) {
        for (Map.Entry<String, CategoryConfig> entry : categoryConfigs.entrySet()) {
            if (score >= entry.getValue().minScore && score <= entry.getValue().maxScore) {
                return entry.getKey();
            }
        }
        return "high";
    }
    
    /**
     * Gets the risk level for a specific finding type.
     */
    public String getRiskLevelForFinding(String scanType, String findingType) {
        RiskConfig config = riskConfigs.get(scanType);
        if (config != null && config.findings != null) {
            FindingConfig fc = config.findings.get(findingType);
            if (fc != null) {
                return fc.riskLevel;
            }
        }
        return "medium";
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
