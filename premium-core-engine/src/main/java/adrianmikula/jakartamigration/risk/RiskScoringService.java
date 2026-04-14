package adrianmikula.jakartamigration.risk;

import adrianmikula.jakartamigration.platforms.config.RiskScoringConfig;
import org.yaml.snakeyaml.Yaml;

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
    private final Map<String, Object> scanCalculationConfig;
    private final Map<String, Object> pdfFormulaConfig;
    private final Map<String, Object> complexityScoringConfig;
    private final RiskScoringConfig riskScoringConfig;

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
        scanCalculationConfig = new HashMap<>();
        pdfFormulaConfig = new HashMap<>();
        complexityScoringConfig = new HashMap<>();
        riskScoringConfig = new RiskScoringConfig();
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
            // Load from classpath
            InputStream inputStream = getClass().getClassLoader()
                    .getResourceAsStream("config/risk-scoring.yaml");

            if (inputStream == null) {
                throw new RuntimeException("CRITICAL: risk-scoring.yaml not found in classpath.");
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

            // Load scan calculation multipliers
            if (config.containsKey("scanCalculation")) {
                scanCalculationConfig.putAll((Map<String, Object>) config.get("scanCalculation"));
            }

            // Load PDF formula weights
            if (config.containsKey("pdfFormula")) {
                pdfFormulaConfig.putAll((Map<String, Object>) config.get("pdfFormula"));
            }

            // Load complexity scoring config
            if (config.containsKey("complexityScoring")) {
                complexityScoringConfig.putAll((Map<String, Object>) config.get("complexityScoring"));
            }

        } catch (Exception e) {
            throw new RuntimeException("CRITICAL: Failed to load risk-scoring.yaml. Risk calculation cannot proceed.",
                    e);
        }
    }

    /**
     * Calculates overall risk score based on scan findings, dependency issues, and project metrics.
     */
    public RiskScore calculateRiskScore(
            Map<String, List<RiskFinding>> scanFindings,
            Map<String, Integer> dependencyIssues) {

        return calculateRiskScore(scanFindings, dependencyIssues, 0, 0.0);
    }

    /**
     * Calculates overall risk score based on scan findings, dependency issues, and project metrics.
     *
     * @param scanFindings Map of scan type to list of risk findings
     * @param dependencyIssues Map of dependency issue types to scores
     * @param totalFileCount Total number of files in the project
     * @param platformRiskScore Platform compatibility risk score (0-10)
     */
    public RiskScore calculateRiskScore(
            Map<String, List<RiskFinding>> scanFindings,
            Map<String, Integer> dependencyIssues,
            int totalFileCount,
            double platformRiskScore) {

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

            // Apply base weight as a multiplier for normalization (configurable divisor)
            Number baseWeightDivisor = (Number) scanCalculationConfig.getOrDefault("baseWeightDivisor", 10.0);
            scanTypeScore = (scanTypeScore * config.baseWeight) / baseWeightDivisor.doubleValue();
            rawScanScore += scanTypeScore;
        }

        // Calculate dependency issues score
        double rawDepScore = 0;
        for (Map.Entry<String, Integer> entry : dependencyIssues.entrySet()) {
            rawDepScore += entry.getValue();
        }

        // Calculate code complexity based on file count
        double rawComplexityScore = calculateComplexityScore(totalFileCount);

        // Platform risk score (already normalized 0-10)
        double rawPlatformScore = platformRiskScore;

        // Get weights from calculation config
        @SuppressWarnings("unchecked")
        Map<String, Double> weights = (Map<String, Double>) calculationConfig.get("componentWeights");
        if (weights == null) {
            throw new IllegalArgumentException("Missing 'componentWeights' in risk-scoring.yaml");
        }

        Number scanWeightNum = (Number) weights.get("scanFindings");
        Number depWeightNum = (Number) weights.get("dependencyIssues");
        Number complexityWeightNum = (Number) weights.get("codeComplexity");
        Number platformWeightNum = (Number) weights.get("platformRisk");

        if (scanWeightNum == null || depWeightNum == null || complexityWeightNum == null || platformWeightNum == null) {
            throw new IllegalArgumentException(
                    "Missing one or more required weights (scanFindings, dependencyIssues, codeComplexity, platformRisk) in risk-scoring.yaml");
        }

        double scanWeight = scanWeightNum.doubleValue();
        double depWeight = depWeightNum.doubleValue();
        double complexityWeight = complexityWeightNum.doubleValue();
        double platformWeight = platformWeightNum.doubleValue();

        // Get max total score for normalization
        Number maxScoreNum = (Number) calculationConfig.get("maxTotalScore");
        double maxTotalScore = maxScoreNum != null ? maxScoreNum.doubleValue() : 100.0;

        componentScores.put("scanFindings", (int) rawScanScore);
        componentScores.put("dependencyIssues", (int) rawDepScore);
        componentScores.put("codeComplexity", (int) rawComplexityScore);
        componentScores.put("platformRisk", (int) rawPlatformScore);

        // Weighted total (normalized to 0-100 scale)
        double totalScore = (rawScanScore * scanWeight) +
                (rawDepScore * depWeight) +
                (rawComplexityScore * complexityWeight) +
                (rawPlatformScore * platformWeight);

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

    /**
     * Calculates risk score from basic metrics (used by PDF reports).
     * Score ranges from 0 (no risk) to 100 (maximum risk).
     *
     * @param blockers Number of blocking dependencies that need migration
     * @param nonCompatibleDeps Number of non-compatible dependencies
     * @param totalIssues Total number of issues found
     * @param criticalIssues Number of critical issues
     * @return Risk score from 0 to 100
     */
    public double calculateRiskScore(int blockers, int nonCompatibleDeps, int totalIssues, int criticalIssues) {
        double score = 0.0;

        // Load PDF formula weights from YAML configuration
        Number pointsPerBlocker = (Number) pdfFormulaConfig.getOrDefault("pointsPerBlocker", 10);
        Number pointsPerNonCompatibleDep = (Number) pdfFormulaConfig.getOrDefault("pointsPerNonCompatibleDep", 2);
        Number pointsPerIssue = (Number) pdfFormulaConfig.getOrDefault("pointsPerIssue", 0.5);
        Number pointsPerCriticalIssue = (Number) pdfFormulaConfig.getOrDefault("pointsPerCriticalIssue", 5);
        Number maxScoreCap = (Number) pdfFormulaConfig.getOrDefault("maxScoreCap", 100.0);

        // Factor 1: Dependencies that need migration (blockers)
        score += blockers * pointsPerBlocker.doubleValue();

        // Add score for non-compatible dependencies
        score += nonCompatibleDeps * pointsPerNonCompatibleDep.doubleValue();

        // Factor 2: Issues
        score += totalIssues * pointsPerIssue.doubleValue();
        score += criticalIssues * pointsPerCriticalIssue.doubleValue();

        // Cap at configured maximum
        return Math.min(score, maxScoreCap.doubleValue());
    }

    /**
     * Determines risk level based on calculated score.
     *
     * @param score Risk score (0-100)
     * @return Risk level string (LOW, MEDIUM, HIGH, CRITICAL)
     */
    public String determineRiskLevel(double score) {
        if (score < 25) {
            return "LOW";
        } else if (score < 50) {
            return "MEDIUM";
        } else if (score < 75) {
            return "HIGH";
        } else {
            return "CRITICAL";
        }
    }

    /**
     * Calculates estimated migration time in weeks based on risk score, team size, and environment count.
     * Uses an exponential formula that doubles every 25 risk points.
     * Time estimate increases logarithmically with environment count and decreases with dev team size.
     *
     * For average team (5 devs) and single environment:
     * - Risk 1-10: ~1 week
     * - Risk 25: 5 weeks
     * - Risk 50: 10 weeks
     * - Risk 75: 20 weeks
     * - Risk 100: 40 weeks
     *
     * @param riskScore The calculated risk score (0-100)
     * @param devTeamSize Number of developers on the team (default 5 if <= 0)
     * @param environmentCount Number of deployment environments (default 1 if <= 0)
     * @return Estimated effort in weeks
     */
    public int calculateMigrationTimeWeeks(double riskScore, int devTeamSize, int environmentCount) {
        // Ensure valid inputs
        int teamSize = devTeamSize > 0 ? devTeamSize : 5;
        int environments = environmentCount > 0 ? environmentCount : 1;

        // Base formula: exponential growth that doubles every 25 risk points
        // At risk 10: 1 week, risk 25: 5 weeks, risk 50: 10 weeks, risk 75: 20 weeks, risk 100: 40 weeks
        double baseWeeks;
        if (riskScore <= 10) {
            baseWeeks = 1.0;
        } else if (riskScore <= 25) {
            // Linear interpolation from 1 to 5 weeks between risk 10 and 25
            baseWeeks = 1.0 + ((riskScore - 10) / 15.0) * 4.0;
        } else {
            // Exponential: 5 weeks at risk 25, doubling every 25 points
            double exponent = (riskScore - 25) / 25.0;
            baseWeeks = 5.0 * Math.pow(2, exponent);
        }

        // Apply team size factor: larger teams = less time (inverse relationship)
        // Team factor: 1.0 at 5 devs, 0.5 at 10 devs, 2.0 at 2.5 devs
        double teamFactor = 5.0 / teamSize;

        // Apply environment factor: more environments = more time (logarithmic relationship)
        // Logarithmic scale: 1 env = 1.0, 10 envs = 1.5, 100 envs = 2.0, 1000 envs = 2.5
        double environmentFactor = 1.0 + (Math.log10(environments) / 2.0);

        double totalWeeks = baseWeeks * teamFactor * environmentFactor;

        return (int) Math.ceil(totalWeeks);
    }

    /**
     * Calculates estimated migration time with default team size (5) and environment count (1).
     *
     * @param riskScore The calculated risk score (0-100)
     * @return Estimated effort in weeks
     */
    public int calculateMigrationTimeWeeks(double riskScore) {
        return calculateMigrationTimeWeeks(riskScore, 5, 1);
    }

    /**
     * Gets formatted migration time string based on risk score with team and environment inputs.
     *
     * @param riskScore The calculated risk score (0-100)
     * @param devTeamSize Number of developers on the team
     * @param environmentCount Number of deployment environments
     * @return Formatted string like "3 weeks" or "1 week"
     */
    public String formatMigrationTime(double riskScore, int devTeamSize, int environmentCount) {
        int effortWeeks = calculateMigrationTimeWeeks(riskScore, devTeamSize, environmentCount);
        return effortWeeks + (effortWeeks == 1 ? " week" : " weeks");
    }

    /**
     * Gets formatted migration time string with default team size and single environment.
     *
     * @param riskScore The calculated risk score (0-100)
     * @return Formatted string like "3 weeks" or "1 week"
     */
    public String formatMigrationTime(double riskScore) {
        return formatMigrationTime(riskScore, 5, 1);
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
     * Calculates complexity score based on the total number of files in the project.
     * Uses a logarithmic scale to prevent very large projects from dominating the score.
     *
     * @param totalFileCount Total number of files in the project
     * @return Normalized complexity score (0-10 scale)
     */
    private double calculateComplexityScore(int totalFileCount) {
        if (totalFileCount <= 0) {
            return 0.0;
        }

        // Use logarithmic scale: log10(fileCount) normalized to 0-10
        // 1-10 files: 0-2 points
        // 11-100 files: 2-4 points
        // 101-1000 files: 4-6 points
        // 1001-10000 files: 6-8 points
        // 10000+ files: 8-10 points

        // Load complexity scoring config from YAML
        Number logScaleDivisor = (Number) complexityScoringConfig.getOrDefault("logScaleDivisor", 5.0);
        Number maxComplexityScore = (Number) complexityScoringConfig.getOrDefault("maxScore", 10.0);

        double logScale = Math.log10(totalFileCount);
        double normalizedScore = (logScale / logScaleDivisor.doubleValue()) * maxComplexityScore.doubleValue();

        // Cap at configured maximum
        return Math.min(normalizedScore, maxComplexityScore.doubleValue());
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

    /**
     * Gets the RiskScoringConfig with effort scoring weights and thresholds.
     * Used by UI components for effort score calculation.
     */
    public RiskScoringConfig getRiskScoringConfig() {
        return riskScoringConfig;
    }
}
