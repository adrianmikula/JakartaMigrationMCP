package adrianmikula.jakartamigration.intellij.license;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for premium simulation functionality in development mode.
 * These tests verify that the dev tab premium simulation works correctly
 * and only applies in development mode.
 */
class CheckLicensePremiumSimulationTest {

    private static final String DEV_MODE_PROPERTY = "jakarta.migration.mode";
    private static final String SIMULATE_PREMIUM_PROPERTY = "jakarta.migration.dev.simulate_premium";

    @BeforeEach
    void setUp() {
        // Clear any existing properties
        System.clearProperty(DEV_MODE_PROPERTY);
        System.clearProperty(SIMULATE_PREMIUM_PROPERTY);
        // Clear license cache
        CheckLicense.clearCache();
    }

    @AfterEach
    void tearDown() {
        // Clean up system properties
        System.clearProperty(DEV_MODE_PROPERTY);
        System.clearProperty(SIMULATE_PREMIUM_PROPERTY);
        // Clear license cache
        CheckLicense.clearCache();
    }

    @Test
    @DisplayName("Should return false for premium simulation when not in dev mode")
    void shouldReturnFalseForPremiumSimulationWhenNotInDevMode() {
        // Given - not in dev mode
        System.setProperty(DEV_MODE_PROPERTY, "production");
        System.setProperty(SIMULATE_PREMIUM_PROPERTY, "true");

        // When
        boolean isSimulating = CheckLicense.isSimulatingPremium();

        // Then
        assertThat(isSimulating).isFalse();
    }

    @Test
    @DisplayName("Should return false for premium simulation when property is false")
    void shouldReturnFalseForPremiumSimulationWhenPropertyIsFalse() {
        // Given - in dev mode but simulation disabled
        System.setProperty(DEV_MODE_PROPERTY, "dev");
        System.setProperty(SIMULATE_PREMIUM_PROPERTY, "false");

        // When
        boolean isSimulating = CheckLicense.isSimulatingPremium();

        // Then
        assertThat(isSimulating).isFalse();
    }

    @Test
    @DisplayName("Should return true for premium simulation when enabled in dev mode")
    void shouldReturnTrueForPremiumSimulationWhenEnabledInDevMode() {
        // Given - in dev mode with simulation enabled
        System.setProperty(DEV_MODE_PROPERTY, "dev");
        System.setProperty(SIMULATE_PREMIUM_PROPERTY, "true");

        // When
        boolean isSimulating = CheckLicense.isSimulatingPremium();

        // Then
        assertThat(isSimulating).isTrue();
    }

    @Test
    @DisplayName("Should return false for premium simulation when property not set")
    void shouldReturnFalseForPremiumSimulationWhenPropertyNotSet() {
        // Given - in dev mode but simulation property not set
        System.setProperty(DEV_MODE_PROPERTY, "dev");

        // When
        boolean isSimulating = CheckLicense.isSimulatingPremium();

        // Then
        assertThat(isSimulating).isFalse();
    }

    @Test
    @DisplayName("Should return licensed when premium simulation is enabled in dev mode")
    void shouldReturnLicensedWhenPremiumSimulationEnabledInDevMode() {
        // Given - premium simulation enabled in dev mode
        System.setProperty(DEV_MODE_PROPERTY, "dev");
        System.setProperty(SIMULATE_PREMIUM_PROPERTY, "true");

        // When
        Boolean isLicensed = CheckLicense.isLicensed();

        // Then
        assertThat(isLicensed).isTrue();
    }

    @Test
    @DisplayName("Should return 'Premium (Simulated)' status string when simulation enabled")
    void shouldReturnPremiumSimulatedStatusStringWhenSimulationEnabled() {
        // Given - premium simulation enabled in dev mode
        System.setProperty(DEV_MODE_PROPERTY, "dev");
        System.setProperty(SIMULATE_PREMIUM_PROPERTY, "true");

        // When
        String status = CheckLicense.getLicenseStatusString();

        // Then
        assertThat(status).isEqualTo("Premium (Simulated)");
    }

    @Test
    @DisplayName("Should return 'Development Mode' status when in dev mode but simulation disabled")
    void shouldReturnDevelopmentModeStatusWhenSimulationDisabled() {
        // Given - in dev mode but simulation disabled
        System.setProperty(DEV_MODE_PROPERTY, "dev");
        System.setProperty(SIMULATE_PREMIUM_PROPERTY, "false");

        // When
        String status = CheckLicense.getLicenseStatusString();

        // Then
        assertThat(status).isEqualTo("Development Mode");
    }

    @Test
    @DisplayName("Should clear cache when premium simulation changes")
    void shouldClearCacheWhenPremiumSimulationChanges() {
        // Given - initial state with simulation disabled
        System.setProperty(DEV_MODE_PROPERTY, "dev");
        System.setProperty(SIMULATE_PREMIUM_PROPERTY, "false");
        
        // First license check to populate cache
        CheckLicense.isLicensed();
        
        // When - simulation state changes
        System.setProperty(SIMULATE_PREMIUM_PROPERTY, "true");
        CheckLicense.onPremiumSimulationChanged();
        
        // Then - should get new result reflecting the change
        Boolean isLicensed = CheckLicense.isLicensed();
        assertThat(isLicensed).isTrue();
    }

    @Test
    @DisplayName("Should handle premium simulation with boolean property parsing")
    void shouldHandlePremiumSimulationWithBooleanPropertyParsing() {
        // Given - in dev mode
        System.setProperty(DEV_MODE_PROPERTY, "dev");

        // When/Then - test various boolean values
        System.setProperty(SIMULATE_PREMIUM_PROPERTY, "TRUE");
        assertThat(CheckLicense.isSimulatingPremium()).isTrue();

        System.setProperty(SIMULATE_PREMIUM_PROPERTY, "True");
        assertThat(CheckLicense.isSimulatingPremium()).isTrue();

        System.setProperty(SIMULATE_PREMIUM_PROPERTY, "FALSE");
        assertThat(CheckLicense.isSimulatingPremium()).isFalse();

        System.setProperty(SIMULATE_PREMIUM_PROPERTY, "False");
        assertThat(CheckLicense.isSimulatingPremium()).isFalse();
    }

    @Test
    @DisplayName("Should prioritize premium simulation over dev mode fallback")
    void shouldPrioritizePremiumSimulationOverDevModeFallback() {
        // Given - premium simulation enabled in dev mode
        System.setProperty(DEV_MODE_PROPERTY, "dev");
        System.setProperty(SIMULATE_PREMIUM_PROPERTY, "true");

        // When
        boolean isSimulating = CheckLicense.isSimulatingPremium();
        Boolean isLicensed = CheckLicense.isLicensed();
        String status = CheckLicense.getLicenseStatusString();

        // Then - should show simulation status, not just dev mode
        assertThat(isSimulating).isTrue();
        assertThat(isLicensed).isTrue();
        assertThat(status).isEqualTo("Premium (Simulated)");
    }
}
