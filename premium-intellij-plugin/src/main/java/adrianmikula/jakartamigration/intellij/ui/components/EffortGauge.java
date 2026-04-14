package adrianmikula.jakartamigration.intellij.ui.components;

import java.awt.Color;

/**
 * EffortGauge - A speedometer-style gauge component for displaying migration effort scores.
 * Higher scores indicate higher effort (green on left for easy, red on right for hard).
 *
 * Color ranges:
 * - 0-25: Green (Easy effort)
 * - 26-50: Yellow (Moderate effort)
 * - 51-75: Orange (Significant effort)
 * - 76-100: Red (High effort)
 */
public class EffortGauge extends ScoreGauge {

    public EffortGauge(String title) {
        super(title);
    }

    @Override
    protected Color getArcColorForRange(int rangeIndex) {
        // Effort gauge: higher score = harder (green on left, red on right)
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
        if (score <= 25) {
            return "Easy";
        } else if (score <= 50) {
            return "Moderate";
        } else if (score <= 75) {
            return "Significant";
        } else {
            return "High";
        }
    }

    @Override
    protected Color getScoreColor() {
        // Color the score based on the effort level
        if (score <= 25) {
            return new Color(40, 167, 69);   // Green
        } else if (score <= 50) {
            return new Color(255, 193, 7);   // Yellow
        } else if (score <= 75) {
            return new Color(255, 165, 0);   // Orange
        } else {
            return new Color(220, 53, 69);   // Red
        }
    }
}
