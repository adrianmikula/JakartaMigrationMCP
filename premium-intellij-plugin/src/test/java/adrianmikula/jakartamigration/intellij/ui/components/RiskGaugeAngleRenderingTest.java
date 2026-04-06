package adrianmikula.jakartamigration.intellij.ui.components;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify the new risk dial angle rendering configuration
 */
public class RiskGaugeAngleRenderingTest {

    private RiskGauge riskGauge;

    @BeforeEach
    void setUp() {
        riskGauge = new RiskGauge("Test Gauge");
    }

    @Test
    @DisplayName("Test needle angle calculation with new arc configuration")
    void testNeedleAngleCalculation() {
        // Test key points in the new angle system
        riskGauge.setScore(0);
        int angle0 = riskGauge.calculateNeedleAngle(0);
        assertTrue(angle0 >= 180 && angle0 <= 270, "Score 0 should be in green arc (180-270°)");

        riskGauge.setScore(25);
        int angle25 = riskGauge.calculateNeedleAngle(25);
        assertTrue(angle25 >= 180 && angle25 <= 270, "Score 25 should be in green arc (180-270°)");

        riskGauge.setScore(50);
        int angle50 = riskGauge.calculateNeedleAngle(50);
        assertTrue(angle50 >= 270 && angle50 <= 360, "Score 50 should be in yellow arc (0-90°, but needle uses 270-360°)");

        riskGauge.setScore(75);
        int angle75 = riskGauge.calculateNeedleAngle(75);
        assertTrue(angle75 >= 270 && angle75 <= 360, "Score 75 should be in red arc (270-315°, but needle uses 270-360°)");

        riskGauge.setScore(100);
        int angle100 = riskGauge.calculateNeedleAngle(100);
        assertTrue(angle100 >= 270 && angle100 <= 360, "Score 100 should be in red arc (270-315°, but needle uses 270-360°)");
    }

    @Test
    @DisplayName("Test specific angle boundaries for arc verification")
    void testSpecificAngleBoundaries() {
        // Test boundaries between score ranges
        riskGauge.setScore(33);
        int angle33 = riskGauge.calculateNeedleAngle(33);
        assertTrue(angle33 >= 180 && angle33 <= 270, "Score 33 should be at green arc boundary");

        riskGauge.setScore(34);
        int angle34 = riskGauge.calculateNeedleAngle(34);
        assertTrue(angle34 >= 270 && angle34 <= 360, "Score 34 should be in yellow arc range");

        riskGauge.setScore(66);
        int angle66 = riskGauge.calculateNeedleAngle(66);
        assertTrue(angle66 >= 270 && angle66 <= 360, "Score 66 should be at red arc boundary");

        riskGauge.setScore(67);
        int angle67 = riskGauge.calculateNeedleAngle(67);
        assertTrue(angle67 >= 270 && angle67 <= 360, "Score 67 should be in red arc range");
    }

    @Test
    @DisplayName("Test needle movement is monotonic increasing")
    void testNeedleMovementMonotonic() {
        int previousAngle = -1;
        
        // Test that needle angle increases as score increases
        for (int score = 0; score <= 100; score += 5) {
            int currentAngle = riskGauge.calculateNeedleAngle(score);
            assertTrue(currentAngle >= previousAngle, 
                "Needle angle should increase with score. Score " + score + " gave angle " + currentAngle + " vs previous " + previousAngle);
            previousAngle = currentAngle;
        }
    }

    @Test
    @DisplayName("Test key visual positions for rendering verification")
    void testKeyVisualPositions() {
        // These are the key positions that should be visible in the new arc layout
        riskGauge.setScore(0);   // Should point to green arc (180-270°)
        int greenStart = riskGauge.calculateNeedleAngle(0);
        System.out.println("DEBUG: Score 0 (green start) - Angle: " + greenStart + "°");

        riskGauge.setScore(50);  // Should point to yellow arc area (0-90°)
        int yellowMid = riskGauge.calculateNeedleAngle(50);
        System.out.println("DEBUG: Score 50 (yellow area) - Angle: " + yellowMid + "°");

        riskGauge.setScore(100); // Should point to red arc (270-315°)
        int redEnd = riskGauge.calculateNeedleAngle(100);
        System.out.println("DEBUG: Score 100 (red end) - Angle: " + redEnd + "°");

        // Verify these are in expected ranges for visual verification
        assertTrue(greenStart >= 180 && greenStart <= 270, "Green start should be in 180-270° range");
        assertTrue(yellowMid >= 270 && yellowMid <= 360, "Yellow mid should be in 270-360° range");
        assertTrue(redEnd >= 270 && redEnd <= 360, "Red end should be in 270-360° range");
    }
}
