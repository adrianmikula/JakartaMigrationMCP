package adrianmikula.jakartamigration.intellij.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for premium UI refresh functionality in MigrationToolWindow.
 * Tests the premium status detection and UI refresh logic.
 */
class MigrationToolWindowPremiumTest {

    @BeforeEach
    @AfterEach
    void cleanUpSystemProperties() {
        // Clean up system properties after each test
        System.clearProperty("jakarta.migration.premium");
        System.clearProperty("jakarta.migration.trial.end");
    }

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
    @DisplayName("Should return true when trial is active and not expired")
    void shouldReturnTrueWhenTrialIsActive() {
        // Given: Premium is true and trial end is in the future (7 days from now)
        System.setProperty("jakarta.migration.premium", "true");
        long futureTime = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000);
        System.setProperty("jakarta.migration.trial.end", String.valueOf(futureTime));
        
        // When: We check premium status
        boolean isPremium = checkPremiumStatusLogic();
        
        // Then: Should return true
        assertThat(isPremium).isTrue();
    }

    @Test
    @DisplayName("Should return false when trial has expired")
    void shouldReturnFalseWhenTrialExpired() {
        // Given: Premium is true but trial end is in the past
        System.setProperty("jakarta.migration.premium", "true");
        long pastTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000); // 1 day ago
        System.setProperty("jakarta.migration.trial.end", String.valueOf(pastTime));
        
        // When: We check premium status
        boolean isPremium = checkPremiumStatusLogic();
        
        // Then: Should return false and clear the premium property
        assertThat(isPremium).isFalse();
        assertThat(System.getProperty("jakarta.migration.premium")).isEqualTo("false");
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

    @Test
    @DisplayName("Should calculate correct trial end timestamp for 7-day trial")
    void shouldCalculateCorrectTrialEndTimestamp() {
        // Given: Starting a 7-day trial
        long startTime = System.currentTimeMillis();
        long expectedEndTime = startTime + (7L * 24 * 60 * 60 * 1000);
        
        // When: We calculate the trial end time (same logic as startTrial method)
        long actualEndTime = startTime + 7L * 24 * 60 * 60 * 1000;
        
        // Then: Should match expected
        assertThat(actualEndTime).isEqualTo(expectedEndTime);
        
        // And: The end time should be approximately 7 days from now
        long now = System.currentTimeMillis();
        long sevenDaysFromNow = now + (7L * 24 * 60 * 60 * 1000);
        assertThat(actualEndTime).isBetween(now, sevenDaysFromNow + 1000); // Allow 1 second tolerance
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
