package adrianmikula.jakartamigration.pdfreporting.snippet;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Displays a side-by-side comparison table of migration strategies.
 * Compares strategies across different risk categories based on their
 * benefits and risks text from property files.
 */
public class StrategyComparisonSnippet extends BaseHtmlSnippet {

    private static final String PROPERTIES_FILE = "migration-strategies.properties";
    private static final String[] STRATEGY_KEYS = {
        "big_bang",
        "incremental",
        "transform",
        "microservices",
        "adapter",
        "strangler"
    };

    private static final String[] RISK_CATEGORIES = {
        "Development Effort",
        "Dependency Risks",
        "Tech Debt Accumulation",
        "Test Effort",
        "Backwards Compatibility",
        "Production Rollout Risk"
    };

    private final Properties properties;

    public StrategyComparisonSnippet() {
        this.properties = loadProperties();
    }

    @Override
    public String generate() throws SnippetGenerationException {
        if (properties == null || properties.isEmpty()) {
            return generateNoDataMessage();
        }

        Map<String, StrategyData> strategies = loadStrategies();
        if (strategies.isEmpty()) {
            return generateNoDataMessage();
        }

        return safelyFormat("""
            <div class="section strategy-comparison">
                <h2>Strategy Comparison</h2>
                <p>Side-by-side comparison of migration strategies across risk categories.</p>

                <div class="comparison-table-container">
                    <table class="comparison-table">
                        <thead>
                            <tr>
                                <th>Risk Category</th>
                                %s
                            </tr>
                        </thead>
                        <tbody>
                            %s
                        </tbody>
                    </table>
                </div>

                <div class="comparison-legend">
                    <div class="legend-item">
                        <div class="legend-color high-risk"></div>
                        <span>High Risk</span>
                    </div>
                    <div class="legend-item">
                        <div class="legend-color medium-risk"></div>
                        <span>Medium Risk</span>
                    </div>
                    <div class="legend-item">
                        <div class="legend-color low-risk"></div>
                        <span>Low Risk</span>
                    </div>
                </div>
            </div>
            """,
            generateStrategyHeaders(strategies),
            generateComparisonRows(strategies)
        );
    }

    private String generateStrategyHeaders(Map<String, StrategyData> strategies) {
        StringBuilder headers = new StringBuilder();
        for (String key : STRATEGY_KEYS) {
            StrategyData strategy = strategies.get(key);
            if (strategy != null) {
                headers.append(String.format("<th>%s</th>", escapeHtml(strategy.displayName)));
            }
        }
        return headers.toString();
    }

    private String generateComparisonRows(Map<String, StrategyData> strategies) {
        StringBuilder rows = new StringBuilder();
        for (String category : RISK_CATEGORIES) {
            rows.append("<tr>");
            rows.append(String.format("<td class=\"category-cell\">%s</td>", escapeHtml(category)));
            
            for (String key : STRATEGY_KEYS) {
                StrategyData strategy = strategies.get(key);
                if (strategy != null) {
                    String riskLevel = assessRiskForCategory(category, strategy);
                    String riskClass = getRiskClass(riskLevel);
                    rows.append(String.format("<td class=\"risk-cell %s\">%s</td>", riskClass, riskLevel));
                }
            }
            
            rows.append("</tr>");
        }
        return rows.toString();
    }

    /**
     * Assesses risk level for a specific category based on strategy text.
     * Uses keyword matching to derive High/Medium/Low levels.
     */
    private String assessRiskForCategory(String category, StrategyData strategy) {
        String combinedText = (strategy.benefits + " " + strategy.risks).toLowerCase();
        
        return switch (category) {
            case "Development Effort" -> assessDevelopmentEffort(combinedText);
            case "Dependency Risks" -> assessDependencyRisks(combinedText);
            case "Tech Debt Accumulation" -> assessTechDebt(combinedText);
            case "Test Effort" -> assessTestEffort(combinedText);
            case "Backwards Compatibility" -> assessBackwardsCompatibility(combinedText);
            case "Production Rollout Risk" -> assessProductionRolloutRisk(combinedText);
            default -> "Medium";
        };
    }

    private String assessDevelopmentEffort(String text) {
        if (text.contains("complex") || text.contains("specialized") || text.contains("most complex")) {
            return "High";
        } else if (text.contains("incremental") || text.contains("one at a time") || text.contains("gradual")) {
            return "Low";
        } else if (text.contains("all at once") || text.contains("single comprehensive")) {
            return "Medium";
        }
        return "Medium";
    }

    private String assessDependencyRisks(String text) {
        if (text.contains("dual dependencies") || text.contains("compatibility") || text.contains("classpath")) {
            return "High";
        } else if (text.contains("independent") || text.contains("one dependency")) {
            return "Low";
        }
        return "Medium";
    }

    private String assessTechDebt(String text) {
        if (text.contains("duplicate logic") || text.contains("adapter") || text.contains("temporary")) {
            return "High";
        } else if (text.contains("gradual") || text.contains("replacement")) {
            return "Medium";
        } else if (text.contains("single comprehensive") || text.contains("all at once")) {
            return "Low";
        }
        return "Medium";
    }

    private String assessTestEffort(String text) {
        if (text.contains("comprehensive") || text.contains("thorough") || text.contains("continuous")) {
            return "High";
        } else if (text.contains("independent") || text.contains("one at a time")) {
            return "Low";
        }
        return "Medium";
    }

    private String assessBackwardsCompatibility(String text) {
        if (text.contains("maintain backward") || text.contains("adapter") || text.contains("compatibility layer")) {
            return "Low";
        } else if (text.contains("all at once") || text.contains("single comprehensive")) {
            return "High";
        }
        return "Medium";
    }

    private String assessProductionRolloutRisk(String text) {
        if (text.contains("downtime") || text.contains("extended") || text.contains("entire codebase")) {
            return "High";
        } else if (text.contains("independent") || text.contains("incremental") || text.contains("one at a time")) {
            return "Low";
        }
        return "Medium";
    }

    private String getRiskClass(String riskLevel) {
        return switch (riskLevel.toLowerCase()) {
            case "high" -> "high-risk";
            case "medium" -> "medium-risk";
            case "low" -> "low-risk";
            default -> "medium-risk";
        };
    }

    private Map<String, StrategyData> loadStrategies() {
        Map<String, StrategyData> strategies = new HashMap<>();
        
        for (String key : STRATEGY_KEYS) {
            String displayName = properties.getProperty("strategy." + key + ".displayName");
            String benefits = properties.getProperty("strategy." + key + ".benefits");
            String risks = properties.getProperty("strategy." + key + ".risks");
            
            if (displayName != null && benefits != null && risks != null) {
                strategies.put(key, new StrategyData(displayName, benefits, risks));
            }
        }
        
        return strategies;
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (input == null) {
                return null;
            }
            props.load(input);
        } catch (IOException e) {
            return null;
        }
        return props;
    }

    private String generateNoDataMessage() {
        return """
            <div class="section strategy-comparison">
                <h2>Strategy Comparison</h2>
                <div class="no-data-message">
                    <p>Strategy comparison data not available. Could not load strategy properties.</p>
                </div>
            </div>
            """;
    }

    @Override
    public boolean isApplicable() {
        return properties != null && !properties.isEmpty();
    }

    @Override
    public int getOrder() {
        return 51; // Show after Migration Strategies
    }

    private record StrategyData(String displayName, String benefits, String risks) {}
}
