package adrianmikula.jakartamigration.intellij.ui.components;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify the correct risk dial angle configuration
 */
public class RiskGaugeCorrectAnglesTest {

    private RiskGauge riskGauge;

    @BeforeEach
    void setUp() {
        riskGauge = new RiskGauge("Test Gauge");
    }

    @Test
    @DisplayName("Test correct needle angle mapping")
    void testCorrectNeedleAngles() {
        // Test the exact angle mappings as specified
        assertEquals(180, riskGauge.calculateNeedleAngle(0), "Score 0 should be 180°");
        assertEquals(225, riskGauge.calculateNeedleAngle(25), "Score 25 should be 225°");
        assertEquals(270, riskGauge.calculateNeedleAngle(50), "Score 50 should be 270°");
        assertEquals(315, riskGauge.calculateNeedleAngle(75), "Score 75 should be 315°");
        assertEquals(360, riskGauge.calculateNeedleAngle(100), "Score 100 should be 360°");
    }

    @Test
    @DisplayName("Test needle movement is linear and clockwise")
    void testNeedleMovementLinear() {
        int previousAngle = 179; // Start lower than first expected
        
        // Test that needle angle increases linearly as score increases
        for (int score = 0; score <= 100; score += 5) {
            int currentAngle = riskGauge.calculateNeedleAngle(score);
            assertTrue(currentAngle >= previousAngle, 
                "Needle angle should increase (clockwise) as score increases. Score " + score + " gave angle " + currentAngle + " vs previous " + previousAngle);
            assertTrue(currentAngle >= 180 && currentAngle <= 360, 
                "Angle should be within 180-360° range. Score " + score + " gave angle " + currentAngle);
            previousAngle = currentAngle;
        }
    }

    @Test
    @DisplayName("Test key boundary angles for visual verification")
    void testKeyBoundaryAngles() {
        // Test the exact boundaries between color zones
        assertEquals(180, riskGauge.calculateNeedleAngle(0), "Green zone start");
        assertEquals(225, riskGauge.calculateNeedleAngle(25), "Green-Yellow boundary");
        assertEquals(270, riskGauge.calculateNeedleAngle(50), "Yellow-Orange boundary");
        assertEquals(315, riskGauge.calculateNeedleAngle(75), "Orange-Red boundary");
        assertEquals(360, riskGauge.calculateNeedleAngle(100), "Red zone end");
        
        System.out.println("=== RISK GAUGE ANGLE VERIFICATION ===");
        System.out.println("Score 0 (Green): " + riskGauge.calculateNeedleAngle(0) + "°");
        System.out.println("Score 25 (Green-Yellow): " + riskGauge.calculateNeedleAngle(25) + "°");
        System.out.println("Score 50 (Yellow-Orange): " + riskGauge.calculateNeedleAngle(50) + "°");
        System.out.println("Score 75 (Orange-Red): " + riskGauge.calculateNeedleAngle(75) + "°");
        System.out.println("Score 100 (Red): " + riskGauge.calculateNeedleAngle(100) + "°");
        System.out.println("=====================================");
    }
}
