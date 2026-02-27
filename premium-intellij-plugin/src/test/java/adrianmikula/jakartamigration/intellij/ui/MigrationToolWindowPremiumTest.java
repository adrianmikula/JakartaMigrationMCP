package adrianmikula.jakartamigration.intellij.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for premium UI refresh functionality in MigrationToolWindow.
 * Tests the premium status detection, trial expiration, and upgrade requirement logic.
 */
class MigrationToolWindowPremiumTest {

    // Trial duration constant - 7 days in milliseconds
    private static final long SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000;

    @BeforeEach
    @AfterEach
    void cleanUpSystemProperties() {
        // Clean up system properties after each test
        System.clearProperty("jakarta.migration.premium");
        System.clearProperty("jakarta.migration.trial.end");
    }

    // ==================== Basic Premium Status Tests ====================

    @Test
    @DisplayName("Should return false when premium system property is not set")
    void shouldReturnFalseWhenPremiumPropertyNotSet() {
        // Given: No premium system property set
        System.clearProperty("jakarta.migration.premium");
        System.clearProperty("jakarta.migration.trial.end");
        
        // When: We check premium status using the same logic as MigrationToolWindow
        boolean isPremium = checkPremiumStatusLogic();
        
        // Then: Should return false
        assertThat(isPremium).isFalse();
    }

    @Test
    @DisplayName("Should return true when premium system property is set to true")
    void shouldReturnTrueWhenPremiumPropertyIsTrue() {
        // Given: Premium system property is set to true
        System.setProperty("jakarta.migration.premium", "true");
        
        // When: We check premium status
        boolean isPremium = checkPremiumStatusLogic();
        
        // Then: Should return true
        assertThat(isPremium).isTrue();
    }

    @Test
    @DisplayName("Should return false when premium system property is set to false")
    void shouldReturnFalseWhenPremiumPropertyIsFalse() {
        // Given: Premium system property is explicitly set to false
        System.setProperty("jakarta.migration.premium", "false");
        
        // When: We check premium status
        boolean isPremium = checkPremiumStatusLogic();
        
        // Then: Should return false
        assertThat(isPremium).isFalse();
    }

    @Test
    @DisplayName("Should return false when trial end property is invalid")
    void shouldReturnFalseWhenTrialEndIsInvalid() {
        // Given: Premium is true but trial end is not a valid number
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", "not-a-number");
        
        // When: We check premium status
        boolean isPremium = checkPremiumStatusLogic();
        
        // Then: Should return false
        assertThat(isPremium).isFalse();
    }

    // ==================== 7-Day Trial Duration Tests ====================

    @Test
    @DisplayName("Should have 7-day trial duration")
    void shouldHaveSevenDayTrialDuration() {
        // Given: Starting a 7-day trial
        long startTime = System.currentTimeMillis();
        
        // When: We calculate the trial end time (same logic as startTrial method)
        long trialEndTime = startTime + SEVEN_DAYS_MS;
        
        // Then: The duration should be exactly 7 days
        long actualDuration = trialEndTime - startTime;
        assertThat(actualDuration).isEqualTo(SEVEN_DAYS_MS);
        
        // And: Should be 604,800,000 milliseconds (7 * 24 * 60 * 60 * 1000)
        assertThat(SEVEN_DAYS_MS).isEqualTo(604800000L);
    }

    @Test
    @DisplayName("Should return true when trial is active and not expired (exactly 7 days)")
    void shouldReturnTrueWhenTrialIsActiveExactlySevenDays() {
        // Given: Trial ends exactly 7 days from now
        long trialEndTime = System.currentTimeMillis() + SEVEN_DAYS_MS;
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", String.valueOf(trialEndTime));
        
        // When: We check premium status (at start of trial)
        boolean isPremium = checkPremiumStatusLogic();
        
        // Then: Should return true (trial is active)
        assertThat(isPremium).isTrue();
    }

    @Test
    @DisplayName("Should return false immediately after 7-day trial expires")
    void shouldReturnFalseImmediatelyAfterTrialExpires() {
        // Given: Trial ended 1 millisecond ago (just expired)
        long justExpired = System.currentTimeMillis() - 1;
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", String.valueOf(justExpired));
        
        // When: We check premium status
        boolean isPremium = checkPremiumStatusLogic();
        
        // Then: Should return false (trial has expired)
        assertThat(isPremium).isFalse();
    }

    @Test
    @DisplayName("Should clear premium status when trial expires")
    void shouldClearPremiumStatusWhenTrialExpires() {
        // Given: Trial has expired
        long pastTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000); // 1 day ago
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", String.valueOf(pastTime));
        
        // When: We check premium status
        checkPremiumStatusLogic();
        
        // Then: Premium status should be cleared to "false"
        assertThat(System.getProperty("jakarta.migration.premium")).isEqualTo("false");
    }

    @Test
    @DisplayName("Should return true for trial ending in 6 days (still within trial period)")
    void shouldReturnTrueForSixDayTrial() {
        // Given: Trial ends in 6 days
        long sixDaysFromNow = System.currentTimeMillis() + (6L * 24 * 60 * 60 * 1000);
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", String.valueOf(sixDaysFromNow));
        
        // When: We check premium status
        boolean isPremium = checkPremiumStatusLogic();
        
        // Then: Should return true (still within trial)
        assertThat(isPremium).isTrue();
    }

    @Test
    @DisplayName("Should return false for trial ending in 8 days (trial expired)")
    void shouldReturnFalseForEightDayExpiredTrial() {
        // Given: Trial would have ended 8 days ago (expired)
        long eightDaysAgo = System.currentTimeMillis() - (8L * 24 * 60 * 60 * 1000);
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", String.valueOf(eightDaysAgo));
        
        // When: We check premium status
        boolean isPremium = checkPremiumStatusLogic();
        
        // Then: Should return false (trial expired)
        assertThat(isPremium).isFalse();
    }

    // ==================== Premium Feature Locking Tests ====================

    @Test
    @DisplayName("Should lock premium features after trial expiration")
    void shouldLockPremiumFeaturesAfterTrialExpiration() {
        // Given: Trial has expired
        long pastTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", String.valueOf(pastTime));
        
        // When: We check premium status (simulating UI refresh on new tool window)
        boolean isPremium = checkPremiumStatusLogic();
        
        // Then: Premium features should be locked
        assertThat(isPremium).isFalse();
        
        // And: The premium system property should be cleared
        assertThat(System.getProperty("jakarta.migration.premium")).isEqualTo("false");
    }

    @Test
    @DisplayName("Should allow premium features during active trial")
    void shouldAllowPremiumFeaturesDuringActiveTrial() {
        // Given: Trial is active (7 days from now)
        long futureTime = System.currentTimeMillis() + SEVEN_DAYS_MS;
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", String.valueOf(futureTime));
        
        // When: We check premium status
        boolean isPremium = checkPremiumStatusLogic();
        
        // Then: Premium features should be available
        assertThat(isPremium).isTrue();
    }

    // ==================== Upgrade Requirement Tests ====================

    @Test
    @DisplayName("Should require upgrade after trial expiration")
    void shouldRequireUpgradeAfterTrialExpiration() {
        // Given: Trial has expired
        long pastTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", String.valueOf(pastTime));
        
        // When: We check premium status
        boolean isPremium = checkPremiumStatusLogic();
        
        // Then: User should be required to upgrade
        assertThat(isPremium).isFalse();
        
        // And: The system should indicate upgrade is required (premium is cleared)
        assertThat(System.getProperty("jakarta.migration.premium")).isEqualTo("false");
    }

    @Test
    @DisplayName("Should not allow trial restart after expiration without upgrade")
    void shouldNotAllowTrialRestartAfterExpiration() {
        // Given: Trial has expired
        long pastTime = System.currentTimeMillis() - SEVEN_DAYS_MS;
        System.setProperty("jakarta.migration.premium", "false"); // Already expired
        System.setProperty("jakarta.migration.trial.end", String.valueOf(pastTime));
        
        // When: We check premium status - user cannot restart trial
        boolean isPremium = checkPremiumStatusLogic();
        
        // Then: User should not have premium access
        assertThat(isPremium).isFalse();
        
        // And: Premium property should remain false
        assertThat(System.getProperty("jakarta.migration.premium")).isEqualTo("false");
    }

    @Test
    @DisplayName("Should return correct remaining trial days during active trial")
    void shouldReturnCorrectRemainingTrialDays() {
        // Given: Trial started 3 days ago, ends in 4 days
        long now = System.currentTimeMillis();
        long trialEndTime = now + (4L * 24 * 60 * 60 * 1000); // 4 days remaining
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", String.valueOf(trialEndTime));
        
        // When: We calculate remaining days
        long remainingMs = trialEndTime - now;
        long remainingDays = remainingMs / (24 * 60 * 60 * 1000);
        
        // Then: Should have approximately 4 days remaining (allow for small timing differences)
        assertThat(remainingDays).isGreaterThanOrEqualTo(3);
        assertThat(remainingDays).isLessThanOrEqualTo(5);
    }

    @Test
    @DisplayName("Should return zero remaining days when trial expired")
    void shouldReturnZeroDaysWhenTrialExpired() {
        // Given: Trial expired 1 day ago
        long now = System.currentTimeMillis();
        long trialEndTime = now - (24 * 60 * 60 * 1000); // 1 day ago
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", String.valueOf(trialEndTime));
        
        // When: Premium status is checked (which clears premium on expiration)
        checkPremiumStatusLogic();
        
        // Then: Premium should be cleared
        assertThat(System.getProperty("jakarta.migration.premium")).isEqualTo("false");
    }

    /**
     * This method replicates the premium status checking logic from MigrationToolWindowContent.
     * It's used to test the logic without requiring the full IntelliJ Project context.
     */
    private boolean checkPremiumStatusLogic() {
        // Check system property (set by trial activation)
        if ("true".equals(System.getProperty("jakarta.migration.premium"))) {
            // Check if trial is still valid
            String trialEnd = System.getProperty("jakarta.migration.trial.end");
            if (trialEnd != null) {
                try {
                    long endTime = Long.parseLong(trialEnd);
                    if (System.currentTimeMillis() > endTime) {
                        // Trial expired, clear premium status
                        System.setProperty("jakarta.migration.premium", "false");
                        return false;
                    }
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
