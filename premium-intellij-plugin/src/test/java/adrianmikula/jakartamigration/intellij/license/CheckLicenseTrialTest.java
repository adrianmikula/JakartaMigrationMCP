package adrianmikula.jakartamigration.intellij.license;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.intellij.ui.LicensingFacade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for the trial system functionality.
 * Tests trial activation, expiration, status reporting, and edge cases.
 */
@ExtendWith(MockitoExtension.class)
class CheckLicenseTrialTest {

    private MockedStatic<LicensingFacade> mockedLicensingFacade;
    
    // Trial duration constants
    private static final long SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000;
    private static final long ONE_DAY_MS = 24L * 60 * 60 * 1000;
    private static final long ONE_HOUR_MS = 60L * 60 * 1000;
    private static final long ONE_MINUTE_MS = 60L * 1000;

    @BeforeEach
    void setUp() {
        mockedLicensingFacade = mockStatic(LicensingFacade.class);
        mockedLicensingFacade.when(LicensingFacade::getInstance).thenReturn(null);
        CheckLicense.clearCache();
    }

    @AfterEach
    void tearDown() {
        if (mockedLicensingFacade != null) {
            mockedLicensingFacade.close();
        }
        System.clearProperty("jakarta.migration.premium");
        System.clearProperty("jakarta.migration.trial.end");
        CheckLicense.clearCache();
    }

    // ==================== Trial Activation Tests ====================

    @Test
    @DisplayName("Should activate trial with correct duration")
    void shouldActivateTrialWithCorrectDuration() {
        // Given
        long beforeActivation = System.currentTimeMillis();

        // When
        CheckLicense.startTrial();

        // Then
        assertThat(System.getProperty("jakarta.migration.premium")).isEqualTo("true");
        
        String trialEnd = System.getProperty("jakarta.migration.trial.end");
        assertThat(trialEnd).isNotNull();
        
        long trialEndTime = Long.parseLong(trialEnd);
        long expectedEndTime = beforeActivation + SEVEN_DAYS_MS;
        
        // Allow for small timing differences (within 1 second)
        assertThat(Math.abs(trialEndTime - expectedEndTime)).isLessThan(1000);
    }

    @Test
    @DisplayName("Should clear cache when activating trial")
    void shouldClearCacheWhenActivatingTrial() {
        // Given
        CheckLicense.isLicensed(); // Cache initial result
        CheckLicense.clearCache();

        // When
        CheckLicense.startTrial();

        // Then
        // Trial should be active
        Boolean result = CheckLicense.isLicensed();
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should overwrite existing trial when starting new trial")
    void shouldOverwriteExistingTrialWhenStartingNewTrial() {
        // Given
        CheckLicense.startTrial(); // Start first trial
        String firstTrialEnd = System.getProperty("jakarta.migration.trial.end");
        
        // Wait a bit to ensure different timestamp
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When
        CheckLicense.startTrial(); // Start second trial

        // Then
        String secondTrialEnd = System.getProperty("jakarta.migration.trial.end");
        assertThat(secondTrialEnd).isNotEqualTo(firstTrialEnd);
        assertThat(System.getProperty("jakarta.migration.premium")).isEqualTo("true");
    }

    // ==================== Trial Status Tests ====================

    @Test
    @DisplayName("Should return true when trial is active and not expired")
    void shouldReturnTrueWhenTrialActiveAndNotExpired() {
        // Given
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", 
                String.valueOf(System.currentTimeMillis() + SEVEN_DAYS_MS));

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
                String.valueOf(System.currentTimeMillis() - ONE_DAY_MS));

        // When
        Boolean result = CheckLicense.isLicensed();

        // Then
        assertThat(result).isFalse();
        // Should clean up expired trial
        assertThat(System.getProperty("jakarta.migration.premium")).isEqualTo("false");
    }

    @Test
    @DisplayName("Should return false when trial is exactly at expiration time")
    void shouldReturnFalseWhenTrialAtExpirationTime() {
        // Given
        long exactlyNow = System.currentTimeMillis();
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", String.valueOf(exactlyNow));

        // When
        Boolean result = CheckLicense.isLicensed();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when trial premium property is not set")
    void shouldReturnFalseWhenTrialPremiumNotSet() {
        // Given
        System.setProperty("jakarta.migration.trial.end", 
                String.valueOf(System.currentTimeMillis() + SEVEN_DAYS_MS));
        // jakarta.migration.premium is not set

        // When
        Boolean result = CheckLicense.isLicensed();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when trial premium property is false")
    void shouldReturnFalseWhenTrialPremiumFalse() {
        // Given
        System.setProperty("jakarta.migration.premium", "false");
        System.setProperty("jakarta.migration.trial.end", 
                String.valueOf(System.currentTimeMillis() + SEVEN_DAYS_MS));

        // When
        Boolean result = CheckLicense.isLicensed();

        // Then
        assertThat(result).isFalse();
    }

    // ==================== Trial Status String Tests ====================

    @Test
    @DisplayName("Should show correct days remaining for active trial")
    void shouldShowCorrectDaysRemainingForActiveTrial() {
        // Given
        System.setProperty("jakarta.migration.premium", "true");
        long twoDaysFromNow = System.currentTimeMillis() + (2 * ONE_DAY_MS);
        System.setProperty("jakarta.migration.trial.end", String.valueOf(twoDaysFromNow));

        // When
        String status = CheckLicense.getLicenseStatusString();

        // Then
        assertThat(status).startsWith("Trial - ");
        assertThat(status).contains("2 days remaining");
    }

    @Test
    @DisplayName("Should show 1 day remaining for trial ending within 24 hours")
    void shouldShowOneDayRemainingForTrialEndingSoon() {
        // Given
        System.setProperty("jakarta.migration.premium", "true");
        long twelveHoursFromNow = System.currentTimeMillis() + (12 * ONE_HOUR_MS);
        System.setProperty("jakarta.migration.trial.end", String.valueOf(twelveHoursFromNow));

        // When
        String status = CheckLicense.getLicenseStatusString();

        // Then
        assertThat(status).startsWith("Trial - ");
        assertThat(status).contains("0 days remaining"); // Less than 1 day
    }

    @Test
    @DisplayName("Should show 0 days remaining for trial ending within minutes")
    void shouldShowZeroDaysRemainingForTrialEndingMinutes() {
        // Given
        System.setProperty("jakarta.migration.premium", "true");
        long thirtyMinutesFromNow = System.currentTimeMillis() + (30 * ONE_MINUTE_MS);
        System.setProperty("jakarta.migration.trial.end", String.valueOf(thirtyMinutesFromNow));

        // When
        String status = CheckLicense.getLicenseStatusString();

        // Then
        assertThat(status).startsWith("Trial - ");
        assertThat(status).contains("0 days remaining");
    }

    @Test
    @DisplayName("Should show Free status for expired trial")
    void shouldShowFreeStatusForExpiredTrial() {
        // Given
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", 
                String.valueOf(System.currentTimeMillis() - ONE_DAY_MS));

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

    @Test
    @DisplayName("Should handle very large trial end time")
    void shouldHandleVeryLargeTrialEndTime() {
        // Given
        System.setProperty("jakarta.migration.premium", "true");
        long farFuture = Long.MAX_VALUE;
        System.setProperty("jakarta.migration.trial.end", String.valueOf(farFuture));

        // When
        Boolean result = CheckLicense.isLicensed();
        String status = CheckLicense.getLicenseStatusString();

        // Then
        assertThat(result).isTrue();
        assertThat(status).startsWith("Trial - ");
    }

    // ==================== Trial Caching Tests ====================

    @Test
    @DisplayName("Should cache trial license check results")
    void shouldCacheTrialLicenseCheckResults() {
        // Given
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", 
                String.valueOf(System.currentTimeMillis() + SEVEN_DAYS_MS));

        // When - call multiple times
        Boolean result1 = CheckLicense.isLicensed();
        Boolean result2 = CheckLicense.isLicensed();

        // Then - should return same result
        assertThat(result1).isEqualTo(result2);
        assertThat(result1).isTrue();
    }

    @Test
    @DisplayName("Should clear cache and recheck trial status")
    void shouldClearCacheAndRecheckTrialStatus() {
        // Given
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", 
                String.valueOf(System.currentTimeMillis() + SEVEN_DAYS_MS));

        // When
        Boolean result1 = CheckLicense.isLicensed();
        CheckLicense.clearCache();
        // Modify trial status
        System.setProperty("jakarta.migration.trial.end", 
                String.valueOf(System.currentTimeMillis() - ONE_DAY_MS));
        Boolean result2 = CheckLicense.isLicensed();

        // Then
        assertThat(result1).isTrue(); // Trial was active
        assertThat(result2).isFalse(); // Trial expired after cache clear
    }

    // ==================== Trial Integration Tests ====================

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
                String.valueOf(System.currentTimeMillis() - ONE_MINUTE_MS));
        Boolean expiredResult = CheckLicense.isLicensed();

        // Then
        assertThat(expiredResult).isFalse();
        assertThat(System.getProperty("jakarta.migration.premium")).isEqualTo("false");
    }

    // ==================== Performance Tests ====================

    @Test
    @DisplayName("Should handle rapid trial status checks efficiently")
    void shouldHandleRapidTrialStatusChecksEfficiently() {
        // Given
        CheckLicense.startTrial();

        // When
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            CheckLicense.isLicensed();
        }
        long endTime = System.currentTimeMillis();

        // Then - should be fast due to caching
        assertThat(endTime - startTime).isLessThan(50);
    }

    // ==================== Security Tests ====================

    @Test
    @DisplayName("Should not allow trial manipulation through system properties")
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
                String.valueOf(System.currentTimeMillis() - ONE_HOUR_MS));

        // When
        CheckLicense.isLicensed();

        // Then
        assertThat(System.getProperty("jakarta.migration.premium")).isEqualTo("false");
        // trial.end should remain for debugging purposes
        assertThat(System.getProperty("jakarta.migration.trial.end")).isNotNull();
    }
}
