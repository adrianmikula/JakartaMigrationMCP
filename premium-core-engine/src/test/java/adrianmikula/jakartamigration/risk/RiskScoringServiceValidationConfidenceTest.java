package adrianmikula.jakartamigration.risk;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Validation Confidence functionality in RiskScoringService.
 */
public class RiskScoringServiceValidationConfidenceTest {

    private final RiskScoringService riskService = RiskScoringService.getInstance();

    @Test
    void testValidationConfidenceCalculationWithHighTestCoverage() {
        // High test coverage should result in high validation confidence
        Map<String, List<RiskScoringService.RiskFinding>> scanFindings = new HashMap<>();
        Map<String, Integer> depIssues = new HashMap<>();

        RiskScoringService.RiskScore score = riskService.calculateRiskScore(
            scanFindings, depIssues, 1000, 5.0, 100, 20, 50);

        assertThat(score.componentScores()).containsKey("validationConfidence");
        int validationConfidence = score.componentScores().get("validationConfidence");
        assertThat(validationConfidence).isGreaterThan(60); // Should be reasonably high
    }

    @Test
    void testValidationConfidenceCalculationWithLowTestCoverage() {
        // Low test coverage should result in low validation confidence
        Map<String, List<RiskScoringService.RiskFinding>> scanFindings = new HashMap<>();
        Map<String, Integer> depIssues = new HashMap<>();

        RiskScoringService.RiskScore score = riskService.calculateRiskScore(
            scanFindings, depIssues, 1000, 5.0, 10, 2, 5);

        assertThat(score.componentScores()).containsKey("validationConfidence");
        int validationConfidence = score.componentScores().get("validationConfidence");
        assertThat(validationConfidence).isLessThan(50); // Should be low
    }

    @Test
    void testValidationConfidenceWithZeroFiles() {
        // Zero files should result in zero validation confidence
        Map<String, List<RiskScoringService.RiskFinding>> scanFindings = new HashMap<>();
        Map<String, Integer> depIssues = new HashMap<>();

        RiskScoringService.RiskScore score = riskService.calculateRiskScore(
            scanFindings, depIssues, 0, 5.0, 0, 0, 0);

        assertThat(score.componentScores()).containsKey("validationConfidence");
        int validationConfidence = score.componentScores().get("validationConfidence");
        assertThat(validationConfidence).isEqualTo(0);
    }

    @Test
    void testValidationConfidenceWeights() {
        // Test that different components contribute according to weights
        // High unit test coverage but low integration tests
        Map<String, List<RiskScoringService.RiskFinding>> scanFindings = new HashMap<>();
        Map<String, Integer> depIssues = new HashMap<>();

        RiskScoringService.RiskScore scoreHighUnit = riskService.calculateRiskScore(
            scanFindings, depIssues, 1000, 5.0, 100, 0, 50); // High unit, no integration

        RiskScoringService.RiskScore scoreHighIntegration = riskService.calculateRiskScore(
            scanFindings, depIssues, 1000, 5.0, 0, 100, 50); // No unit, high integration

        int confidenceHighUnit = scoreHighUnit.componentScores().get("validationConfidence");
        int confidenceHighIntegration = scoreHighIntegration.componentScores().get("validationConfidence");

        // Both should be reasonable but different due to weighting
        assertThat(confidenceHighUnit).isGreaterThan(0);
        assertThat(confidenceHighIntegration).isGreaterThan(0);
        // Unit test weighted higher (0.4 vs 0.3), so high unit should be higher
        assertThat(confidenceHighUnit).isGreaterThan(confidenceHighIntegration);
    }

    @Test
    void testValidationConfidenceScoreClamping() {
        // Ensure validation confidence is clamped between 0-100
        Map<String, List<RiskScoringService.RiskFinding>> scanFindings = new HashMap<>();
        Map<String, Integer> depIssues = new HashMap<>();

        // Extreme values should be clamped
        RiskScoringService.RiskScore score = riskService.calculateRiskScore(
            scanFindings, depIssues, 10000, 5.0, 10000, 1000, 1000);

        int validationConfidence = score.componentScores().get("validationConfidence");
        assertThat(validationConfidence).isBetween(0, 100);
    }
}