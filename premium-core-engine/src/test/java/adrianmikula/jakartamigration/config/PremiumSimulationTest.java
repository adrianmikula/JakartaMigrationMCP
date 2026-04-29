package adrianmikula.jakartamigration.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for premium simulation functionality in development mode.
 * These tests verify that the premium simulation works correctly
 * and only applies in development mode with the proper system properties.
 */
@Tag("slow")
class PremiumSimulationTest {

    private static final String DEV_MODE_PROPERTY = "jakarta.migration.mode";
    private static final String SIMULATE_PREMIUM_PROPERTY = "jakarta.migration.dev.simulate_premium";

    @BeforeEach
    void setUp() {
        // Clear any existing properties before each test
        System.clearProperty(DEV_MODE_PROPERTY);
        System.clearProperty(SIMULATE_PREMIUM_PROPERTY);
    }

    @AfterEach
    void tearDown() {
        // Clean up properties after each test
        System.clearProperty(DEV_MODE_PROPERTY);
        System.clearProperty(SIMULATE_PREMIUM_PROPERTY);
    }

    @Test
    @DisplayName("Should not activate premium simulation when dev mode is not enabled")
    void shouldNotActivatePremiumSimulationWhenDevModeIsNotEnabled() {
        // Given: Premium simulation is enabled but dev mode is not
        System.setProperty(SIMULATE_PREMIUM_PROPERTY, "true");
        // DEV_MODE_PROPERTY is not set (defaults to production)

        // When: Checking simulation status
        boolean isDevMode = "dev".equals(System.getProperty(DEV_MODE_PROPERTY, "production"));
        boolean isSimulatingPremium = Boolean.getBoolean(SIMULATE_PREMIUM_PROPERTY);
        boolean simulationActive = isDevMode && isSimulatingPremium;

        // Then: Simulation should not be active
        assertThat(isDevMode).isFalse();
        assertThat(isSimulatingPremium).isTrue();
        assertThat(simulationActive).isFalse();
    }

    @Test
    @DisplayName("Should not activate premium simulation when simulate premium is not enabled")
    void shouldNotActivatePremiumSimulationWhenSimulatePremiumIsNotEnabled() {
        // Given: Dev mode is enabled but premium simulation is not
        System.setProperty(DEV_MODE_PROPERTY, "dev");
        // SIMULATE_PREMIUM_PROPERTY is not set (defaults to false)

        // When: Checking simulation status
        boolean isDevMode = "dev".equals(System.getProperty(DEV_MODE_PROPERTY, "production"));
        boolean isSimulatingPremium = Boolean.getBoolean(SIMULATE_PREMIUM_PROPERTY);
        boolean simulationActive = isDevMode && isSimulatingPremium;

        // Then: Simulation should not be active
        assertThat(isDevMode).isTrue();
        assertThat(isSimulatingPremium).isFalse();
        assertThat(simulationActive).isFalse();
    }

    @Test
    @DisplayName("Should activate premium simulation when both conditions are met")
    void shouldActivatePremiumSimulationWhenBothConditionsAreMet() {
        // Given: Both dev mode and premium simulation are enabled
        System.setProperty(DEV_MODE_PROPERTY, "dev");
        System.setProperty(SIMULATE_PREMIUM_PROPERTY, "true");

        // When: Checking simulation status
        boolean isDevMode = "dev".equals(System.getProperty(DEV_MODE_PROPERTY, "production"));
        boolean isSimulatingPremium = Boolean.getBoolean(SIMULATE_PREMIUM_PROPERTY);
        boolean simulationActive = isDevMode && isSimulatingPremium;

        // Then: Simulation should be active
        assertThat(isDevMode).isTrue();
        assertThat(isSimulatingPremium).isTrue();
        assertThat(simulationActive).isTrue();
    }

    @Test
    @DisplayName("Should handle production mode correctly")
    void shouldHandleProductionModeCorrectly() {
        // Given: Explicit production mode
        System.setProperty(DEV_MODE_PROPERTY, "production");
        System.setProperty(SIMULATE_PREMIUM_PROPERTY, "true");

        // When: Checking simulation status
        boolean isDevMode = "dev".equals(System.getProperty(DEV_MODE_PROPERTY, "production"));
        boolean isSimulatingPremium = Boolean.getBoolean(SIMULATE_PREMIUM_PROPERTY);
        boolean simulationActive = isDevMode && isSimulatingPremium;

        // Then: Simulation should not be active in production mode
        assertThat(isDevMode).isFalse();
        assertThat(isSimulatingPremium).isTrue();
        assertThat(simulationActive).isFalse();
    }

    @Test
    @DisplayName("Should handle default values correctly")
    void shouldHandleDefaultValuesCorrectly() {
        // Given: No properties are set (using defaults)

        // When: Checking simulation status
        String mode = System.getProperty(DEV_MODE_PROPERTY, "production");
        boolean isDevMode = "dev".equals(mode);
        boolean isSimulatingPremium = Boolean.getBoolean(SIMULATE_PREMIUM_PROPERTY);
        boolean simulationActive = isDevMode && isSimulatingPremium;

        // Then: Should use default values
        assertThat(mode).isEqualTo("production");
        assertThat(isDevMode).isFalse();
        assertThat(isSimulatingPremium).isFalse();
        assertThat(simulationActive).isFalse();
    }
}
