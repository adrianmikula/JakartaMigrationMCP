package adrianmikula.jakartamigration.intellij.ui.components;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EffortGauge component angle calculations.
 * Tests needle rotation logic and label display for different score ranges.
 */
public class EffortGaugeTest {

    private EffortGauge effortGauge;

    @BeforeEach
    void setUp() {
        effortGauge = new EffortGauge("Test Gauge");
    }

    @Test
    @DisplayName("Score 0 should point needle to 180 degrees (left)")
    void testScore0PointsLeft() {
        effortGauge.setScore(0);
        assertEquals(180, getNeedleAngle(), "Score 0 should point left (180°)");
    }

    @Test
    @DisplayName("Score 25 should point needle to 225 degrees (bottom-left)")
    void testScore25PointsBottomLeft() {
        effortGauge.setScore(25);
        assertEquals(225, getNeedleAngle(), "Score 25 should point bottom-left (225°)");
    }

    @Test
    @DisplayName("Score 50 should point needle to 270 degrees (bottom)")
    void testScore50PointsBottom() {
        effortGauge.setScore(50);
        assertEquals(270, getNeedleAngle(), "Score 50 should point bottom (270°)");
    }

    @Test
    @DisplayName("Score 75 should point needle to 315 degrees (bottom-right)")
    void testScore75PointsBottomRight() {
        effortGauge.setScore(75);
        assertEquals(315, getNeedleAngle(), "Score 75 should point bottom-right (315°)");
    }

    @Test
    @DisplayName("Score 100 should point needle to 360 degrees (right)")
    void testScore100PointsRight() {
        effortGauge.setScore(100);
        assertEquals(360, getNeedleAngle(), "Score 100 should point right (360°)");
    }

    @ParameterizedTest
    @DisplayName("All boundary scores should be within expected ranges")
    @ValueSource(ints = {0, 1, 25, 49, 50, 51, 75, 76, 99, 100})
    void testBoundaryScores(int score) {
        effortGauge.setScore(score);
        int angle = getNeedleAngle();

        // Verify angle is within expected range (180-360 degrees)
        assertTrue(angle >= 180 && angle <= 360,
            "Score " + score + " should be between 180° and 360°, got " + angle);

        // Verify angle increases as score increases (clockwise movement)
        if (score <= 25) {
            assertTrue(angle >= 180 && angle <= 225,
                "Score " + score + " should be in green zone (180-225°), got " + angle);
        } else if (score <= 50) {
            assertTrue(angle >= 225 && angle <= 270,
                "Score " + score + " should be in yellow zone (225-270°), got " + angle);
        } else if (score <= 75) {
            assertTrue(angle >= 270 && angle <= 315,
                "Score " + score + " should be in orange zone (270-315°), got " + angle);
        } else {
            assertTrue(angle >= 315 && angle <= 360,
                "Score " + score + " should be in red zone (315-360°), got " + angle);
        }
    }

    @Test
    @DisplayName("Score should be clamped to 0-100 range")
    void testScoreClamping() {
        // Test negative score
        effortGauge.setScore(-10);
        assertEquals(180, getNeedleAngle(), "Negative score should be clamped to 0 (180°)");

        // Test score above 100
        effortGauge.setScore(150);
        assertEquals(360, getNeedleAngle(), "Score above 100 should be clamped to 100 (360°)");
    }

    @Test
    @DisplayName("Gauge arc ranges should match needle logic")
    void testArcRangeConsistency() {
        // Test that each color zone matches the expected needle angles
        effortGauge.setScore(0); // Should be in green zone pointing left
        assertEquals(180, getNeedleAngle(), "Score 0 should be in green zone (180°)");

        effortGauge.setScore(25); // Should be at green-yellow boundary
        assertEquals(225, getNeedleAngle(), "Score 25 should be at green-yellow boundary (225°)");

        effortGauge.setScore(50); // Should be at yellow-orange boundary
        assertEquals(270, getNeedleAngle(), "Score 50 should be at yellow-orange boundary (270°)");

        effortGauge.setScore(75); // Should be at orange-red boundary
        assertEquals(315, getNeedleAngle(), "Score 75 should be at orange-red boundary (315°)");

        effortGauge.setScore(100); // Should be in red zone pointing right
        assertEquals(360, getNeedleAngle(), "Score 100 should be in red zone (360°)");
    }

    /**
     * Helper method to get the current needle angle from the gauge.
     * Uses the package-private calculateNeedleAngle method.
     */
    private int getNeedleAngle() {
        return effortGauge.calculateNeedleAngle(effortGauge.getScore());
    }
}
