package adrianmikula.jakartamigration.intellij.license;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Simple tests for license verification functionality.
 * Tests basic behavior without complex mocking.
 */
class CheckLicenseSimpleTest {

    @BeforeEach
    void setUp() {
        CheckLicense.clearCache();
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("jakarta.migration.premium");
        System.clearProperty("jakarta.migration.trial.end");
        CheckLicense.clearCache();
    }

    // ==================== Trial System Tests ====================

    @Test
    @DisplayName("Should start trial correctly")
    void shouldStartTrialCorrectly() {
        // Given
        long beforeTime = System.currentTimeMillis();

        // When
        CheckLicense.startTrial();

        // Then
        assertThat(System.getProperty("jakarta.migration.premium")).isEqualTo("true");
        
        String trialEnd = System.getProperty("jakarta.migration.trial.end");
        assertThat(trialEnd).isNotNull();
        
        long trialEndTime = Long.parseLong(trialEnd);
        long expectedEndTime = beforeTime + 7L * 24 * 60 * 60 * 1000;
        
        // Allow for small timing differences (within 1 second)
        assertThat(Math.abs(trialEndTime - expectedEndTime)).isLessThan(1000);
    }

    @Test
    @DisplayName("Should return true when trial is active")
    void shouldReturnTrueWhenTrialActive() {
        // Given
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", 
                String.valueOf(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000));

        // When
        Boolean result = CheckLicense.isLicensed();

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when trial has expired")
    void shouldReturnFalseWhenTrialExpired() {
        // Given
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", 
                String.valueOf(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000));

        // When
        Boolean result = CheckLicense.isLicensed();

        // Then
        assertThat(result).isFalse();
        // Should clean up expired trial
        assertThat(System.getProperty("jakarta.migration.premium")).isEqualTo("false");
    }

    @Test
    @DisplayName("Should return false when trial system property is not set")
    void shouldReturnFalseWhenTrialNotSet() {
        // Given - no trial properties set

        // When
        Boolean result = CheckLicense.isLicensed();

        // Then
        assertThat(result).isFalse();
    }

    // ==================== License Status String Tests ====================

    @Test
    @DisplayName("Should return trial status with days remaining")
    void shouldReturnTrialStatusWithDaysRemaining() {
        // Given
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", 
                String.valueOf(System.currentTimeMillis() + (2 * 24 * 60 * 60 * 1000))); // 2 days

        // When
        String status = CheckLicense.getLicenseStatusString();

        // Then
        assertThat(status).startsWith("Trial - ");
        assertThat(status).contains("days remaining");
    }

    @Test
    @DisplayName("Should show Free status for expired trial")
    void shouldShowFreeStatusForExpiredTrial() {
        // Given
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", 
                String.valueOf(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000));

        // When
        String status = CheckLicense.getLicenseStatusString();

        // Then
        assertThat(status).isEqualTo("Free");
    }

    @Test
    @DisplayName("Should show Free status when no trial is set")
    void shouldShowFreeStatusWhenNoTrialSet() {
        // Given - no trial properties set

        // When
        String status = CheckLicense.getLicenseStatusString();

        // Then
        assertThat(status).isEqualTo("Free");
    }

    // ==================== Cache Tests ====================

    @Test
    @DisplayName("Should clear cache when requested")
    void shouldClearCacheWhenRequested() {
        // Given
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", 
                String.valueOf(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000));

        // When
        Boolean result1 = CheckLicense.isLicensed(); // Cache result
        CheckLicense.clearCache(); // Clear cache
        Boolean result2 = CheckLicense.isLicensed(); // Re-check

        // Then
        assertThat(result1).isTrue();
        assertThat(result2).isTrue();
        // The important thing is that clearing cache doesn't throw
    }

    // ==================== Edge Case Tests ====================

    @Test
    @DisplayName("Should handle invalid trial end time gracefully")
    void shouldHandleInvalidTrialEndTimeGracefully() {
        // Given
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", "invalid_timestamp");

        // When
        Boolean result = CheckLicense.isLicensed();
        String status = CheckLicense.getLicenseStatusString();

        // Then
        assertThat(result).isFalse();
        assertThat(status).isEqualTo("Free");
    }

    @Test
    @DisplayName("Should handle negative trial end time")
    void shouldHandleNegativeTrialEndTime() {
        // Given
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", "-1");

        // When
        Boolean result = CheckLicense.isLicensed();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should handle zero trial end time")
    void shouldHandleZeroTrialEndTime() {
        // Given
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", "0");

        // When
        Boolean result = CheckLicense.isLicensed();

        // Then
        assertThat(result).isFalse();
    }

    // ==================== Registration Dialog Tests ====================

    @Test
    @DisplayName("Should request license without throwing exceptions")
    void shouldRequestLicenseWithoutThrowing() {
        // When/Then - should not throw
        assertThatCode(() -> CheckLicense.requestLicense("Test message"))
                .doesNotThrowAnyException();
    }

    // ==================== Performance Tests ====================

    @Test
    @DisplayName("Should handle rapid trial status checks efficiently")
    void shouldHandleRapidTrialStatusChecksEfficiently() {
        // Given
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", 
                String.valueOf(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000));

        // When
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            CheckLicense.isLicensed();
        }
        long endTime = System.currentTimeMillis();

        // Then - should be fast due to caching
        assertThat(endTime - startTime).isLessThan(50);
    }

    // ==================== Integration Tests ====================

    @Test
    @DisplayName("Should handle trial activation during license check")
    void shouldHandleTrialActivationDuringLicenseCheck() {
        // Given
        Boolean initialResult = CheckLicense.isLicensed();
        assertThat(initialResult).isFalse();

        // When
        CheckLicense.startTrial();
        Boolean afterTrialResult = CheckLicense.isLicensed();

        // Then
        assertThat(afterTrialResult).isTrue();
        assertThat(CheckLicense.getLicenseStatusString()).startsWith("Trial - ");
    }

    @Test
    @DisplayName("Should handle trial expiration during license check")
    void shouldHandleTrialExpirationDuringLicenseCheck() {
        // Given
        CheckLicense.startTrial();
        Boolean activeResult = CheckLicense.isLicensed();
        assertThat(activeResult).isTrue();

        // When
        CheckLicense.clearCache();
        // Manually expire trial
        System.setProperty("jakarta.migration.trial.end", 
                String.valueOf(System.currentTimeMillis() - 60 * 1000)); // 1 minute ago
        Boolean expiredResult = CheckLicense.isLicensed();

        // Then
        assertThat(expiredResult).isFalse();
        assertThat(System.getProperty("jakarta.migration.premium")).isEqualTo("false");
    }

    // ==================== Security Tests ====================

    @Test
    @DisplayName("Should not allow trial manipulation through incomplete system properties")
    void shouldNotAllowTrialManipulation() {
        // Given
        System.setProperty("jakarta.migration.premium", "true");
        // Don't set trial.end - incomplete trial setup

        // When
        Boolean result = CheckLicense.isLicensed();

        // Then
        assertThat(result).isFalse(); // Should not accept incomplete trial setup
    }

    @Test
    @DisplayName("Should clean up expired trial properties")
    void shouldCleanUpExpiredTrialProperties() {
        // Given
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", 
                String.valueOf(System.currentTimeMillis() - 60 * 60 * 1000)); // 1 hour ago

        // When
        CheckLicense.isLicensed();

        // Then
        assertThat(System.getProperty("jakarta.migration.premium")).isEqualTo("false");
        // trial.end should remain for debugging purposes
        assertThat(System.getProperty("jakarta.migration.trial.end")).isNotNull();
    }
}
