package adrianmikula.jakartamigration.intellij.ui.components;

import java.awt.Color;

/**
 * ConfidenceGauge - A speedometer-style gauge component for displaying confidence scores.
 * Higher scores indicate higher confidence (standard color scheme).
 *
 * Color ranges:
 * - 0-25: Red (Low confidence)
 * - 26-50: Orange (Medium-low confidence)
 * - 51-75: Yellow (Medium-high confidence)
 * - 76-100: Green (High confidence)
 */
public class ConfidenceGauge extends ScoreGauge {

    public ConfidenceGauge(String title) {
        super(title);
    }

    @Override
    protected Color getArcColorForRange(int rangeIndex) {
        // Confidence gauge: higher score = better (red on left, green on right)
        switch (rangeIndex) {
            case 0: return new Color(220, 53, 69);   // Red (0-25)
            case 1: return new Color(255, 165, 0);   // Orange (26-50)
            case 2: return new Color(255, 193, 7);   // Yellow (51-75)
            case 3: return new Color(40, 167, 69);   // Green (76-100)
            default: return Color.GRAY;
        }
    }

    @Override
    protected String getScoreLabel() {
        if (score >= 80) {
            return "High";
        } else if (score >= 50) {
            return "Medium";
        } else if (score >= 25) {
            return "Low";
        } else {
            return "Very Low";
        }
    }

    @Override
    protected Color getScoreColor() {
        // Color the score based on the confidence level
        if (score >= 80) {
            return new Color(40, 167, 69);   // Green
        } else if (score >= 50) {
            return new Color(255, 193, 7);   // Yellow
        } else if (score >= 25) {
            return new Color(255, 165, 0);   // Orange
        } else {
            return new Color(220, 53, 69);   // Red
        }
    }
}
