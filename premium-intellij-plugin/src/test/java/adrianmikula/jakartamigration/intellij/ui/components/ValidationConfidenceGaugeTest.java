package adrianmikula.jakartamigration.intellij.ui.components;

import org.junit.Test;
import static org.junit.Assert.*;

public class ValidationConfidenceGaugeTest {

    @Test
    public void testGaugeInitialization() {
        ValidationConfidenceGauge gauge = new ValidationConfidenceGauge("Test Gauge");
        assertNotNull(gauge);
        assertEquals(0, gauge.getScore());
    }

    @Test
    public void testScoreSetting() {
        ValidationConfidenceGauge gauge = new ValidationConfidenceGauge("Test Gauge");
        gauge.setScore(75);
        assertEquals(75, gauge.getScore());
    }

    @Test
    public void testScoreClamping() {
        ValidationConfidenceGauge gauge = new ValidationConfidenceGauge("Test Gauge");
        gauge.setScore(150); // Above max
        assertEquals(100, gauge.getScore());

        gauge.setScore(-10); // Below min
        assertEquals(0, gauge.getScore());
    }

    @Test
    public void testGetScoreLabel() {
        ValidationConfidenceGauge gauge = new ValidationConfidenceGauge("Test Gauge");

        gauge.setScore(90);
        assertEquals("High", gauge.getScoreLabel());

        gauge.setScore(60);
        assertEquals("Medium", gauge.getScoreLabel());

        gauge.setScore(30);
        assertEquals("Low", gauge.getScoreLabel());

        gauge.setScore(10);
        assertEquals("Very Low", gauge.getScoreLabel());
    }

    @Test
    public void testGetArcColorForRange() {
        ValidationConfidenceGauge gauge = new ValidationConfidenceGauge("Test Gauge");

        // Test all four ranges
        assertNotNull(gauge.getArcColorForRange(0)); // Red
        assertNotNull(gauge.getArcColorForRange(1)); // Orange
        assertNotNull(gauge.getArcColorForRange(2)); // Yellow
        assertNotNull(gauge.getArcColorForRange(3)); // Green
    }
}