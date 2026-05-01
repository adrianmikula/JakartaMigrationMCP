package adrianmikula.jakartamigration.pdfreporting.snippet;

import adrianmikula.jakartamigration.risk.RiskScoringService;

import java.util.Comparator;
import java.util.List;

/**
 * Displays detailed risk findings from RiskScoringService.RiskScore.findings().
 * Lists all individual risk findings with descriptions, grouped by risk level.
 */
public class RiskFindingsDetailSnippet extends BaseHtmlSnippet {

    private final RiskScoringService.RiskScore riskScore;

    public RiskFindingsDetailSnippet(RiskScoringService.RiskScore riskScore) {
        this.riskScore = riskScore;
    }

    @Override
    public String generate() throws SnippetGenerationException {
        if (riskScore == null || riskScore.findings() == null || riskScore.findings().isEmpty()) {
            return generateNoDataMessage();
        }

        List<RiskScoringService.RiskFinding> findings = riskScore.findings();

        // Sort by risk level (CRITICAL first, then HIGH, MEDIUM, LOW)
        List<RiskScoringService.RiskFinding> sortedFindings = findings.stream()
            .sorted(Comparator.comparingInt((RiskScoringService.RiskFinding f) -> getRiskLevelPriority(f.riskLevel()))
                    .thenComparing(f -> -f.score()))
            .toList();

        long criticalCount = findings.stream().filter(f -> "CRITICAL".equals(f.riskLevel())).count();
        long highCount = findings.stream().filter(f -> "HIGH".equals(f.riskLevel())).count();
        long mediumCount = findings.stream().filter(f -> "MEDIUM".equals(f.riskLevel())).count();
        long lowCount = findings.stream().filter(f -> "LOW".equals(f.riskLevel())).count();

        return safelyFormat("""
            <div class="section risk-findings-detail">
                <h2>Detailed Risk Findings</h2>
                <p>Individual risk findings identified during project analysis.</p>

                <div class="findings-summary">
                    <div class="summary-stat critical">
                        <span class="stat-count">%d</span>
                        <span class="stat-label">Critical</span>
                    </div>
                    <div class="summary-stat high">
                        <span class="stat-count">%d</span>
                        <span class="stat-label">High</span>
                    </div>
                    <div class="summary-stat medium">
                        <span class="stat-count">%d</span>
                        <span class="stat-label">Medium</span>
                    </div>
                    <div class="summary-stat low">
                        <span class="stat-count">%d</span>
                        <span class="stat-label">Low</span>
                    </div>
                </div>

                <div class="findings-table-container">
                    <table class="findings-table">
                        <thead>
                            <tr>
                                <th>Scan Type</th>
                                <th>Finding Type</th>
                                <th>Description</th>
                                <th>Risk Level</th>
                                <th>Score</th>
                            </tr>
                        </thead>
                        <tbody>
                            %s
                        </tbody>
                    </table>
                </div>
            </div>
            """,
            criticalCount,
            highCount,
            mediumCount,
            lowCount,
            generateFindingsRows(sortedFindings)
        );
    }

    private String generateFindingsRows(List<RiskScoringService.RiskFinding> findings) {
        StringBuilder rows = new StringBuilder();
        for (RiskScoringService.RiskFinding finding : findings) {
            String riskLevelClass = getRiskLevelClass(finding.riskLevel());
            rows.append(String.format("""
                <tr class=\"%s\">
                    <td>%s</td>
                    <td>%s</td>
                    <td>%s</td>
                    <td class=\"risk-level-cell %s\">%s</td>
                    <td class=\"score-cell\">%d</td>
                </tr>
                """,
                riskLevelClass,
                escapeHtml(finding.scanType()),
                escapeHtml(finding.findingType()),
                escapeHtml(finding.description()),
                riskLevelClass,
                escapeHtml(finding.riskLevel()),
                finding.score()
            ));
        }
        return rows.toString();
    }

    private int getRiskLevelPriority(String riskLevel) {
        return switch (riskLevel != null ? riskLevel.toUpperCase() : "UNKNOWN") {
            case "CRITICAL" -> 0;
            case "HIGH" -> 1;
            case "MEDIUM" -> 2;
            case "LOW" -> 3;
            default -> 4;
        };
    }

    private String getRiskLevelClass(String riskLevel) {
        return switch (riskLevel != null ? riskLevel.toUpperCase() : "UNKNOWN") {
            case "CRITICAL" -> "critical";
            case "HIGH" -> "high";
            case "MEDIUM" -> "medium";
            case "LOW" -> "low";
            default -> "unknown";
        };
    }

    private String generateNoDataMessage() {
        return """
            <div class="section risk-findings-detail">
                <h2>Detailed Risk Findings</h2>
                <div class="no-data-message">
                    <p>No detailed risk findings available. Risk analysis may not have been performed.</p>
                </div>
            </div>
            """;
    }

    @Override
    public boolean isApplicable() {
        return riskScore != null && riskScore.findings() != null && !riskScore.findings().isEmpty();
    }

    @Override
    public int getOrder() {
        return 48; // After Code Examples (44), before Risk Heat Map (49)
    }
}
