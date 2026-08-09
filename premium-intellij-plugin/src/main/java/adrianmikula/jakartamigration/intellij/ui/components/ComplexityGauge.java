package adrianmikula.jakartamigration.intellij.ui.components;

import java.awt.Color;

/**
 * ComplexityGauge - A speedometer-style gauge component for displaying migration complexity scores.
 * Higher scores indicate higher complexity (green on left for low, red on right for high).
 *
 * Color ranges:
 * - 0-25: Green (Low complexity)
 * - 26-50: Yellow (Moderate complexity)
 * - 51-75: Orange (Significant complexity)
 * - 76-100: Red (High complexity)
 */
public class ComplexityGauge extends ScoreGauge {

    public ComplexityGauge(String title) {
        super(title);
    }

    @Override
    protected Color getArcColorForRange(int rangeIndex) {
        // Complexity gauge: higher score = more complex (green on left, red on right)
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
            return "Low";
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
        // Color the score based on the complexity level
        if (score <= 25) {
            return new Color(40, 167, 69);   // Green (Low complexity)
        } else if (score <= 50) {
            return new Color(255, 193, 7);   // Yellow (Moderate complexity)
        } else if (score <= 75) {
            return new Color(255, 165, 0);   // Orange (Significant complexity)
        } else {
            return new Color(220, 53, 69);   // Red (High complexity)
        }
    }
}
