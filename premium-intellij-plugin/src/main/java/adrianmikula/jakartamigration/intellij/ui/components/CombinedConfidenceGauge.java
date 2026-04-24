package adrianmikula.jakartamigration.intellij.ui.components;

import java.awt.Color;

/**
 * CombinedConfidenceGauge - A speedometer-style gauge component for displaying combined confidence scores.
 * Combines dependency knowledge and test coverage into a single confidence metric.
 * Higher scores indicate higher confidence in migration success.
 *
 * Color ranges:
 * - 0-25: Red (Very Low confidence)
 * - 26-50: Orange (Low confidence)
 * - 51-75: Yellow (Medium confidence)
 * - 76-100: Green (High confidence)
 */
public class CombinedConfidenceGauge extends ScoreGauge {

    public CombinedConfidenceGauge(String title) {
        super(title);
    }

    @Override
    protected Color getArcColorForRange(int rangeIndex) {
        // Combined confidence gauge: higher score = better (red on left, green on right)
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
        } else if (score >= 60) {
            return "Good";
        } else if (score >= 40) {
            return "Medium";
        } else if (score >= 20) {
            return "Low";
        } else {
            return "Very Low";
        }
    }

    @Override
    protected Color getScoreColor() {
        // Color the score based on the combined confidence level
        if (score >= 80) {
            return new Color(40, 167, 69);   // Green
        } else if (score >= 60) {
            return new Color(32, 156, 238);  // Blue
        } else if (score >= 40) {
            return new Color(255, 193, 7);   // Yellow
        } else if (score >= 20) {
            return new Color(255, 165, 0);   // Orange
        } else {
            return new Color(220, 53, 69);   // Red
        }
    }
}
