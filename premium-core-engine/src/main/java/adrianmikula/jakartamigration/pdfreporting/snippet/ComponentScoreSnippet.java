package adrianmikula.jakartamigration.pdfreporting.snippet;

import adrianmikula.jakartamigration.risk.RiskScoringService;

import java.util.Map;

/**
 * Displays component score breakdown from RiskScoringService.RiskScore.componentScores().
 * Shows per-component risk scores that contribute to the total risk score.
 */
public class ComponentScoreSnippet extends BaseHtmlSnippet {

    private final RiskScoringService.RiskScore riskScore;

    public ComponentScoreSnippet(RiskScoringService.RiskScore riskScore) {
        this.riskScore = riskScore;
    }

    @Override
    public String generate() throws SnippetGenerationException {
        if (riskScore == null || riskScore.componentScores() == null || riskScore.componentScores().isEmpty()) {
            return generateNoDataMessage();
        }

        Map<String, Integer> componentScores = riskScore.componentScores();
        int totalScore = componentScores.values().stream().mapToInt(Integer::intValue).sum();

        // Sort by score descending (highest impact first)
        var sortedComponents = componentScores.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .toList();

        return safelyFormat("""
            <div class="section component-scores">
                <h2>Risk Component Breakdown</h2>
                <p>Per-component risk scores that contribute to the total risk assessment.</p>

                <div class="total-score-display">
                    <span class="total-label">Total Risk Score:</span>
                    <span class="total-value %.1f</span>
                </div>

                <div class="component-bars">
                    %s
                </div>

                <div class="component-table-container">
                    <table class="component-table">
                        <thead>
                            <tr>
                                <th>Component</th>
                                <th>Score</th>
                                <th>Contribution</th>
                            </tr>
                        </thead>
                        <tbody>
                            %s
                        </tbody>
                    </table>
                </div>
            </div>
            """,
            riskScore.totalScore(),
            getRiskClass(riskScore.totalScore()),
            generateComponentBars(sortedComponents, totalScore),
            generateComponentRows(sortedComponents, totalScore)
        );
    }

    private String generateComponentBars(java.util.List<Map.Entry<String, Integer>> components, int totalScore) {
        StringBuilder bars = new StringBuilder();
        for (Map.Entry<String, Integer> entry : components) {
            String component = entry.getKey();
            int score = entry.getValue();
            double percentage = totalScore > 0 ? (score * 100.0 / totalScore) : 0;
            String riskClass = getRiskClass(score);

            bars.append(String.format("""
                <div class="component-bar-item">
                    <span class="component-name">%s</span>
                    <div class="component-bar">
                        <div class="component-bar-fill %s" style="width: %.1f%%"></div>
                    </div>
                    <span class="component-score">%d</span>
                </div>
                """,
                escapeHtml(component),
                riskClass,
                percentage,
                score
            ));
        }
        return bars.toString();
    }

    private String generateComponentRows(java.util.List<Map.Entry<String, Integer>> components, int totalScore) {
        StringBuilder rows = new StringBuilder();
        for (Map.Entry<String, Integer> entry : components) {
            String component = entry.getKey();
            int score = entry.getValue();
            double contribution = totalScore > 0 ? (score * 100.0 / totalScore) : 0;
            String riskClass = getRiskClass(score);

            rows.append(String.format("""
                <tr>
                    <td>%s</td>
                    <td class=\"score-cell %s\">%d</td>
                    <td class=\"contribution-cell\">%.1f%%</td>
                </tr>
                """,
                escapeHtml(component),
                riskClass,
                score,
                contribution
            ));
        }
        return rows.toString();
    }

    private String getRiskClass(double score) {
        if (score < 30) return "low";
        if (score < 60) return "medium";
        if (score < 80) return "high";
        return "critical";
    }

    private String generateNoDataMessage() {
        return """
            <div class="section component-scores">
                <h2>Risk Component Breakdown</h2>
                <div class="no-data-message">
                    <p>No component risk scores available. Detailed risk analysis may not have been performed.</p>
                </div>
            </div>
            """;
    }

    @Override
    public boolean isApplicable() {
        return riskScore != null && riskScore.componentScores() != null && !riskScore.componentScores().isEmpty();
    }

    @Override
    public int getOrder() {
        return 49; // After Risk Findings Detail (48), before Risk Heat Map (50)
    }
}
