package adrianmikula.jakartamigration.intellij.ui.components;

import adrianmikula.jakartamigration.risk.RiskScoringService;

import java.awt.Color;

/**
 * RiskGauge - A speedometer-style gauge component for displaying risk scores.
 * Higher scores indicate higher risk (reversed color scheme).
 *
 * Color ranges:
 * - 0-25: Green (Low risk)
 * - 26-50: Yellow (Medium risk)
 * - 51-75: Orange (High risk)
 * - 76-100: Red (Critical risk)
 */
public class RiskGauge extends ScoreGauge {
    private RiskScoringService.CategoryConfig category;
    private RiskScoringService riskScoringService;

    public RiskGauge(String title) {
        super(title);
        this.riskScoringService = RiskScoringService.getInstance();
    }

    @Override
    public void setScore(int score) {
        super.setScore(score);
        this.category = riskScoringService.getCategoryConfigForScore(this.score);
    }

    @Override
    protected Color getArcColorForRange(int rangeIndex) {
        // Risk gauge: higher score = worse (green on left, red on right)
        switch (rangeIndex) {
            case 0: return new Color(40, 167, 69);   // Green (0-25)
            case 1: return new Color(255, 193, 7);   // Yellow (26-50)
            case 2: return new Color(255, 165, 0);   // Orange (51-75)
            case 3: return new Color(220, 53, 69);   // Red (76-100)
            default: return Color.GRAY;
        }
    }

    @Override
    protected String getScoreLabel() {
        return category != null ? category.label : "";
    }

    @Override
    protected Color getScoreColor() {
        return Color.BLACK;
    }
}
