package adrianmikula.jakartamigration.pdfreporting.snippet;

import adrianmikula.jakartamigration.risk.RiskScoringService;

import java.util.*;

/**
 * Calculates category-level risk scores from scan findings.
 * Maps scan types to migration categories (Web, Data, Business, Security)
 * and calculates risk scores based on actual findings.
 */
public class CategoryRiskCalculator {

    /**
     * Represents a migration category with its associated scan types.
     */
    public enum Category {
        WEB_LAYER("Web Layer", "🌐", List.of("servletJsp", "restSoap")),
        DATA_LAYER("Data Layer", "💾", List.of("jpa", "beanValidation")),
        BUSINESS_LAYER("Business Layer", "⚙️", List.of("cdi", "cdiInjection")),
        SECURITY_LAYER("Security Layer", "🔒", List.of("securityApi", "jmsMessaging"));

        private final String displayName;
        private final String emoji;
        private final List<String> scanTypes;

        Category(String displayName, String emoji, List<String> scanTypes) {
            this.displayName = displayName;
            this.emoji = emoji;
            this.scanTypes = scanTypes;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getEmoji() {
            return emoji;
        }

        public List<String> getScanTypes() {
            return scanTypes;
        }
    }

    /**
     * Represents a calculated category risk score.
     */
    public record CategoryRisk(
        Category category,
        double score,
        String riskLevel,
        int findingCount,
        List<String> findingTypes
    ) {
        public String getRiskLevelClass() {
            return switch (riskLevel.toLowerCase()) {
                case "critical", "high" -> "high";
                case "medium" -> "medium";
                case "low" -> "low";
                default -> "unknown";
            };
        }
    }

    /**
     * Calculates category risk scores from RiskScore findings.
     *
     * @param riskScore The risk score with findings
     * @return Map of category to calculated risk
     */
    public Map<Category, CategoryRisk> calculateCategoryRisks(RiskScoringService.RiskScore riskScore) {
        if (riskScore == null || riskScore.findings() == null) {
            return Collections.emptyMap();
        }

        Map<Category, CategoryRisk> results = new HashMap<>();

        for (Category category : Category.values()) {
            List<RiskScoringService.RiskFinding> categoryFindings = new ArrayList<>();
            
            // Collect findings for this category's scan types
            for (RiskScoringService.RiskFinding finding : riskScore.findings()) {
                if (category.getScanTypes().contains(finding.scanType())) {
                    categoryFindings.add(finding);
                }
            }

            if (!categoryFindings.isEmpty()) {
                double score = calculateCategoryScore(categoryFindings);
                String riskLevel = determineRiskLevel(score);
                Set<String> uniqueFindingTypes = new HashSet<>();
                for (RiskScoringService.RiskFinding f : categoryFindings) {
                    uniqueFindingTypes.add(f.findingType());
                }

                results.put(category, new CategoryRisk(
                    category,
                    score,
                    riskLevel,
                    categoryFindings.size(),
                    new ArrayList<>(uniqueFindingTypes)
                ));
            }
        }

        return results;
    }

    /**
     * Calculates a category score from its findings.
     * Uses a weighted sum based on risk level.
     */
    private double calculateCategoryScore(List<RiskScoringService.RiskFinding> findings) {
        double totalScore = 0.0;
        
        for (RiskScoringService.RiskFinding finding : findings) {
            double weight = switch (finding.riskLevel().toUpperCase()) {
                case "CRITICAL" -> 25.0;
                case "HIGH" -> 15.0;
                case "MEDIUM" -> 8.0;
                case "LOW" -> 3.0;
                default -> 1.0;
            };
            totalScore += weight;
        }

        // Normalize to 0-100 scale
        return Math.min(totalScore, 100.0);
    }

    /**
     * Determines risk level from calculated score.
     */
    private String determineRiskLevel(double score) {
        if (score >= 75) {
            return "HIGH";
        } else if (score >= 50) {
            return "MEDIUM";
        } else if (score >= 25) {
            return "LOW";
        } else {
            return "LOW";
        }
    }
}
