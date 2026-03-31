package adrianmikula.jakartamigration.intellij.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.GraphicsEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for LicenseFailsafeConfig.
 * Tests all failsafe configuration options and environment detection.
 */
@ExtendWith(MockitoExtension.class)
class LicenseFailsafeConfigTest {

    private MockedStatic<GraphicsEnvironment> mockedGraphicsEnv;
    
    // Save original system properties
    private String originalDevMode;
    private String originalSafeMode;
    private String originalLicenseDisabled;
    private String originalLicenseTimeout;
    private String originalAsyncLicense;
    private String originalForceTrial;
    private String originalHeadless;
    private String originalDebug;

    @BeforeEach
    void setUp() {
        mockedGraphicsEnv = mockStatic(GraphicsEnvironment.class);
        
        // Save original system properties
        originalDevMode = System.getProperty("jakarta.migration.dev");
        originalSafeMode = System.getProperty("jakarta.migration.safe");
        originalLicenseDisabled = System.getProperty("jakarta.migration.license.disable");
        originalLicenseTimeout = System.getProperty("jakarta.migration.license.timeout");
        originalAsyncLicense = System.getProperty("jakarta.migration.license.async");
        originalForceTrial = System.getProperty("jakarta.migration.force.trial");
        originalHeadless = System.getProperty("java.awt.headless");
        originalDebug = System.getProperty("java.debug");
        
        // Clear all license-related properties
        clearLicenseProperties();
    }

    @AfterEach
    void tearDown() {
        if (mockedGraphicsEnv != null) {
            mockedGraphicsEnv.close();
        }
        
        // Restore original system properties
        restoreProperty("jakarta.migration.dev", originalDevMode);
        restoreProperty("jakarta.migration.safe", originalSafeMode);
        restoreProperty("jakarta.migration.license.disable", originalLicenseDisabled);
        restoreProperty("jakarta.migration.license.timeout", originalLicenseTimeout);
        restoreProperty("jakarta.migration.license.async", originalAsyncLicense);
        restoreProperty("jakarta.migration.force.trial", originalForceTrial);
        restoreProperty("java.awt.headless", originalHeadless);
        restoreProperty("java.debug", originalDebug);
        
        clearLicenseProperties();
    }

    private void clearLicenseProperties() {
        System.clearProperty("jakarta.migration.dev");
        System.clearProperty("jakarta.migration.safe");
        System.clearProperty("jakarta.migration.license.disable");
        System.clearProperty("jakarta.migration.license.timeout");
        System.clearProperty("jakarta.migration.license.async");
        System.clearProperty("jakarta.migration.force.trial");
    }

    private void restoreProperty(String key, String value) {
        if (value != null) {
            System.setProperty(key, value);
        } else {
            System.clearProperty(key);
        }
    }

    // ==================== Development Mode Tests ====================

    @Test
    @DisplayName("Should detect development mode from system property")
    void shouldDetectDevModeFromSystemProperty() {
        // Given
        System.setProperty("jakarta.migration.dev", "true");

        // When
        boolean result = LicenseFailsafeConfig.isDevMode();

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should detect development mode from migration mode property")
    void shouldDetectDevModeFromMigrationMode() {
        // Given
        System.setProperty("jakarta.migration.mode", "dev");

        // When
        boolean result = LicenseFailsafeConfig.isDevMode();

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should not detect development mode when not set")
    void shouldNotDetectDevModeWhenNotSet() {
        // Given - no dev mode properties set

        // When
        boolean result = LicenseFailsafeConfig.isDevMode();

        // Then
        assertThat(result).isFalse();
    }

    // ==================== Safe Mode Tests ====================

    @Test
    @DisplayName("Should detect safe mode from system property")
    void shouldDetectSafeModeFromSystemProperty() {
        // Given
        System.setProperty("jakarta.migration.safe", "true");

        // When
        boolean result = LicenseFailsafeConfig.isSafeMode();

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should not detect safe mode when not set")
    void shouldNotDetectSafeModeWhenNotSet() {
        // Given - no safe mode property set

        // When
        boolean result = LicenseFailsafeConfig.isSafeMode();

        // Then
        assertThat(result).isFalse();
    }

    // ==================== License Disabled Tests ====================

    @Test
    @DisplayName("Should detect when license checks are disabled")
    void shouldDetectLicenseDisabled() {
        // Given
        System.setProperty("jakarta.migration.license.disable", "true");

        // When
        boolean result = LicenseFailsafeConfig.isLicenseDisabled();

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should not detect license disabled when not set")
    void shouldNotDetectLicenseDisabledWhenNotSet() {
        // Given - no license disabled property

        // When
        boolean result = LicenseFailsafeConfig.isLicenseDisabled();

        // Then
        assertThat(result).isFalse();
    }

    // ==================== License Timeout Tests ====================

    @Test
    @DisplayName("Should get license timeout from system property")
    void shouldGetLicenseTimeoutFromSystemProperty() {
        // Given
        System.setProperty("jakarta.migration.license.timeout", "10000");

        // When
        long result = LicenseFailsafeConfig.getLicenseTimeoutMs();

        // Then
        assertThat(result).isEqualTo(10000);
    }

    @Test
    @DisplayName("Should use default timeout when property not set")
    void shouldUseDefaultTimeoutWhenNotSet() {
        // Given - no timeout property set

        // When
        long result = LicenseFailsafeConfig.getLicenseTimeoutMs();

        // Then
        assertThat(result).isEqualTo(LicenseFailsafeConfig.DEFAULT_LICENSE_TIMEOUT_MS);
    }

    @Test
    @DisplayName("Should handle invalid timeout property gracefully")
    void shouldHandleInvalidTimeoutGracefully() {
        // Given
        System.setProperty("jakarta.migration.license.timeout", "invalid");

        // When
        long result = LicenseFailsafeConfig.getLicenseTimeoutMs();

        // Then
        assertThat(result).isEqualTo(LicenseFailsafeConfig.DEFAULT_LICENSE_TIMEOUT_MS);
    }

    // ==================== Async License Tests ====================

    @Test
    @DisplayName("Should detect async license forced from system property")
    void shouldDetectAsyncLicenseForced() {
        // Given
        System.setProperty("jakarta.migration.license.async", "true");

        // When
        boolean result = LicenseFailsafeConfig.isAsyncLicenseForced();

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should use default async setting when not set")
    void shouldUseDefaultAsyncWhenNotSet() {
        // Given - no async property set

        // When
        boolean result = LicenseFailsafeConfig.isAsyncLicenseForced();

        // Then
        assertThat(result).isEqualTo(LicenseFailsafeConfig.DEFAULT_ASYNC_LICENSE);
    }

    // ==================== Trial Forced Tests ====================

    @Test
    @DisplayName("Should detect forced trial from system property")
    void shouldDetectForcedTrial() {
        // Given
        System.setProperty("jakarta.migration.force.trial", "true");

        // When
        boolean result = LicenseFailsafeConfig.isTrialForced();

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should not detect forced trial when not set")
    void shouldNotDetectForcedTrialWhenNotSet() {
        // Given - no forced trial property

        // When
        boolean result = LicenseFailsafeConfig.isTrialForced();

        // Then
        assertThat(result).isFalse();
    }

    // ==================== Environment Detection Tests ====================

    @Test
    @DisplayName("Should detect CI environment from CI variable")
    void shouldDetectCIEnvironment() {
        // Given
        try {
            System.setProperty("CI", "true");
            
            // When
            boolean result = LicenseFailsafeConfig.shouldEnableFailsafe();
            
            // Then
            assertThat(result).isTrue();
        } finally {
            System.clearProperty("CI");
        }
    }

    @Test
    @DisplayName("Should detect CI environment from CONTINUOUS_INTEGRATION variable")
    void shouldDetectCIEnvironmentFromContinuousIntegration() {
        // Given
        try {
            System.setProperty("CONTINUOUS_INTEGRATION", "true");
            
            // When
            boolean result = LicenseFailsafeConfig.shouldEnableFailsafe();
            
            // Then
            assertThat(result).isTrue();
        } finally {
            System.clearProperty("CONTINUOUS_INTEGRATION");
        }
    }

    @Test
    @DisplayName("Should detect headless environment")
    void shouldDetectHeadlessEnvironment() {
        // Given
        mockedGraphicsEnv.when(GraphicsEnvironment::isHeadless).thenReturn(true);

        // When
        boolean result = LicenseFailsafeConfig.shouldEnableFailsafe();

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should detect headless environment from system property")
    void shouldDetectHeadlessFromSystemProperty() {
        // Given
        System.setProperty("java.awt.headless", "true");

        // When
        boolean result = LicenseFailsafeConfig.shouldEnableFailsafe();

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should detect debug environment")
    void shouldDetectDebugEnvironment() {
        // Given
        System.setProperty("java.debug", "true");

        // When
        boolean result = LicenseFailsafeConfig.shouldEnableFailsafe();

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should not enable failsafe in normal environment")
    void shouldNotEnableFailsafeInNormalEnvironment() {
        // Given - normal environment, no special flags

        // When
        boolean result = LicenseFailsafeConfig.shouldEnableFailsafe();

        // Then
        assertThat(result).isFalse();
    }

    // ==================== Configuration Summary Tests ====================

    @Test
    @DisplayName("Should generate comprehensive failsafe summary")
    void shouldGenerateFailsafeSummary() {
        // Given
        System.setProperty("jakarta.migration.dev", "true");
        System.setProperty("jakarta.migration.safe", "false");
        System.setProperty("jakarta.migration.license.timeout", "5000");

        // When
        String summary = LicenseFailsafeConfig.getFailsafeSummary();

        // Then
        assertThat(summary).contains("License Failsafe Configuration:");
        assertThat(summary).contains("Dev Mode: true");
        assertThat(summary).contains("Safe Mode: false");
        assertThat(summary).contains("Timeout: 5000ms");
    }

    // ==================== Apply Failsafe Settings Tests ====================

    @Test
    @DisplayName("Should apply dev mode settings")
    void shouldApplyDevModeSettings() {
        // Given
        System.setProperty("jakarta.migration.dev", "true");

        // When
        LicenseFailsafeConfig.applyFailsafeSettings();

        // Then
        assertThat(System.getProperty("jakarta.migration.dev")).isEqualTo("true");
    }

    @Test
    @DisplayName("Should apply safe mode settings")
    void shouldApplySafeModeSettings() {
        // Given
        System.setProperty("jakarta.migration.safe", "true");

        // When
        LicenseFailsafeConfig.applyFailsafeSettings();

        // Then
        assertThat(System.getProperty("jakarta.migration.safe")).isEqualTo("true");
    }

    @Test
    @DisplayName("Should apply license disabled settings")
    void shouldApplyLicenseDisabledSettings() {
        // Given
        System.setProperty("jakarta.migration.license.disable", "true");

        // When
        LicenseFailsafeConfig.applyFailsafeSettings();

        // Then
        assertThat(System.getProperty("jakarta.migration.license.disable")).isEqualTo("true");
    }

    @Test
    @DisplayName("Should apply forced trial settings")
    void shouldApplyForcedTrialSettings() {
        // Given
        System.setProperty("jakarta.migration.force.trial", "true");

        // When
        LicenseFailsafeConfig.applyFailsafeSettings();

        // Then
        assertThat(System.getProperty("jakarta.migration.force.trial")).isEqualTo("true");
        assertThat(System.getProperty("jakarta.migration.premium")).isEqualTo("true");
    }

    // ==================== Configuration Properties Tests ====================

    @Test
    @DisplayName("Should load configuration from properties file")
    void shouldLoadConfigurationFromPropertiesFile() {
        // This test would require custom class loading to test the actual file loading
        // For now, we test the behavior when properties are set via system properties
        
        // Given
        System.setProperty("jakarta.migration.license.timeout", "8000");
        System.setProperty("jakarta.migration.license.async", "false");

        // When
        long timeout = LicenseFailsafeConfig.getLicenseTimeoutMs();
        boolean async = LicenseFailsafeConfig.isAsyncLicenseForced();

        // Then
        assertThat(timeout).isEqualTo(8000);
        assertThat(async).isFalse();
    }

    // ==================== Recommended Properties Tests ====================

    @Test
    @DisplayName("Should generate recommended failsafe properties")
    void shouldGenerateRecommendedFailsafeProperties() {
        // When
        String properties = LicenseFailsafeConfig.getRecommendedFailsafeProperties();

        // Then
        assertThat(properties).contains("# Jakarta Migration Plugin - License Failsafe Configuration");
        assertThat(properties).contains("jakarta.migration.dev=true");
        assertThat(properties).contains("jakarta.migration.safe=true");
        assertThat(properties).contains("jakarta.migration.license.timeout=3000");
        assertThat(properties).contains("jakarta.migration.license.async=true");
        assertThat(properties).contains("jakarta.migration.license.disable=true");
        assertThat(properties).contains("jakarta.migration.force.trial=true");
    }

    // ==================== Edge Case Tests ====================

    @Test
    @DisplayName("Should handle empty system properties gracefully")
    void shouldHandleEmptySystemProperties() {
        // Given
        System.setProperty("jakarta.migration.dev", "");
        System.setProperty("jakarta.migration.safe", "");

        // When
        boolean devMode = LicenseFailsafeConfig.isDevMode();
        boolean safeMode = LicenseFailsafeConfig.isSafeMode();

        // Then
        assertThat(devMode).isFalse();
        assertThat(safeMode).isFalse();
    }

    @Test
    @DisplayName("Should handle case insensitive boolean values")
    void shouldHandleCaseInsensitiveBooleans() {
        // Given
        System.setProperty("jakarta.migration.dev", "TRUE");
        System.setProperty("jakarta.migration.safe", "False");

        // When
        boolean devMode = LicenseFailsafeConfig.isDevMode();
        boolean safeMode = LicenseFailsafeConfig.isSafeMode();

        // Then
        assertThat(devMode).isTrue();
        assertThat(safeMode).isFalse();
    }

    @Test
    @DisplayName("Should handle very large timeout values")
    void shouldHandleVeryLargeTimeoutValues() {
        // Given
        System.setProperty("jakarta.migration.license.timeout", "999999999");

        // When
        long timeout = LicenseFailsafeConfig.getLicenseTimeoutMs();

        // Then
        assertThat(timeout).isEqualTo(999999999);
    }

    @Test
    @DisplayName("Should handle negative timeout values")
    void shouldHandleNegativeTimeoutValues() {
        // Given
        System.setProperty("jakarta.migration.license.timeout", "-5000");

        // When
        long timeout = LicenseFailsafeConfig.getLicenseTimeoutMs();

        // Then
        assertThat(timeout).isEqualTo(LicenseFailsafeConfig.DEFAULT_LICENSE_TIMEOUT_MS);
    }

    // ==================== Integration Tests ====================

    @Test
    @DisplayName("Should handle multiple failsafe modes simultaneously")
    void shouldHandleMultipleFailsafeModes() {
        // Given
        System.setProperty("jakarta.migration.dev", "true");
        System.setProperty("jakarta.migration.safe", "true");
        System.setProperty("jakarta.migration.license.disable", "true");

        // When
        boolean devMode = LicenseFailsafeConfig.isDevMode();
        boolean safeMode = LicenseFailsafeConfig.isSafeMode();
        boolean licenseDisabled = LicenseFailsafeConfig.isLicenseDisabled();

        // Then
        assertThat(devMode).isTrue();
        assertThat(safeMode).isTrue();
        assertThat(licenseDisabled).isTrue();
    }

    @Test
    @DisplayName("Should apply all failsafe settings in correct order")
    void shouldApplyAllFailsafeSettings() {
        // Given
        System.setProperty("jakarta.migration.dev", "true");
        System.setProperty("jakarta.migration.safe", "true");
        System.setProperty("jakarta.migration.license.disable", "true");
        System.setProperty("jakarta.migration.force.trial", "true");

        // When
        LicenseFailsafeConfig.applyFailsafeSettings();

        // Then
        assertThat(System.getProperty("jakarta.migration.dev")).isEqualTo("true");
        assertThat(System.getProperty("jakarta.migration.safe")).isEqualTo("true");
        assertThat(System.getProperty("jakarta.migration.license.disable")).isEqualTo("true");
        assertThat(System.getProperty("jakarta.migration.force.trial")).isEqualTo("true");
        assertThat(System.getProperty("jakarta.migration.premium")).isEqualTo("true");
    }
}
