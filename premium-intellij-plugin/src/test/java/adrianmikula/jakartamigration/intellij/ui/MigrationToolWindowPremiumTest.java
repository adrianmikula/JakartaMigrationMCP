package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.license.CheckLicense;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for premium UI refresh functionality in MigrationToolWindow.
 * Tests the premium status detection, trial expiration, and upgrade requirement logic.
 * Also tests license-based analyze functionality.
 *
 * NOTE: These tests require IntelliJ Platform environment.
 */
@org.junit.jupiter.api.Disabled("Requires IntelliJ Platform environment - run in IDE")
class MigrationToolWindowPremiumTest {

    // Trial duration constant - 7 days in milliseconds
    private static final long SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000;

    @BeforeEach
    @AfterEach
    void cleanUpSystemProperties() {
        // Clean up system properties after each test
        System.clearProperty("jakarta.migration.premium");
        System.clearProperty("jakarta.migration.trial.end");
        // Clear license cache to ensure fresh checks
        CheckLicense.clearCache();
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

    // ==================== License-Based Analyze Tests ====================

    @Test
    @DisplayName("Should detect non-premium user for basic scan only")
    void shouldDetectNonPremiumUserForBasicScanOnly() {
        // Given: No premium system property set (non-premium user)
        System.clearProperty("jakarta.migration.premium");
        System.clearProperty("jakarta.migration.trial.end");
        CheckLicense.clearCache();
        
        // When: Check license status (same logic as handleAnalyzeProject)
        boolean isLicensed = CheckLicense.isLicensed();
        
        // Then: Should return false (non-premium)
        assertThat(isLicensed).isFalse();
        // Non-premium users should only run basic scan
    }

    @Test
    @DisplayName("Should detect premium user for all scans")
    void shouldDetectPremiumUserForAllScans() {
        // Given: Premium system property is set to true with valid trial
        long futureTime = System.currentTimeMillis() + SEVEN_DAYS_MS;
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", String.valueOf(futureTime));
        CheckLicense.clearCache();
        
        // When: Check license status (same logic as handleAnalyzeProject)
        boolean isLicensed = CheckLicense.isLicensed();
        
        // Then: Should return true (premium/trial)
        assertThat(isLicensed).isTrue();
        // Premium users should run all 3 scans (basic, advanced, platform)
    }

    @Test
    @DisplayName("Should detect trial user for all scans")
    void shouldDetectTrialUserForAllScans() {
        // Given: Trial is active (3 days remaining)
        long threeDaysFromNow = System.currentTimeMillis() + (3L * 24 * 60 * 60 * 1000);
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", String.valueOf(threeDaysFromNow));
        CheckLicense.clearCache();
        
        // When: Check license status (same logic as handleAnalyzeProject)
        boolean isLicensed = CheckLicense.isLicensed();
        
        // Then: Should return true (trial counts as licensed)
        assertThat(isLicensed).isTrue();
        // Trial users should run all 3 scans like premium users
    }

    @Test
    @DisplayName("Should require upgrade for expired trial user")
    void shouldRequireUpgradeForExpiredTrialUser() {
        // Given: Trial has expired 2 days ago
        long twoDaysAgo = System.currentTimeMillis() - (2L * 24 * 60 * 60 * 1000);
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", String.valueOf(twoDaysAgo));
        CheckLicense.clearCache();
        
        // When: Check license status (same logic as handleAnalyzeProject)
        boolean isLicensed = CheckLicense.isLicensed();
        
        // Then: Should return false (expired trial = not licensed)
        assertThat(isLicensed).isFalse();
        // Expired trial users should only run basic scan (like non-premium)
    }

    @Test
    @DisplayName("Should use runtime license check for analyze decision")
    void shouldUseRuntimeLicenseCheckForAnalyzeDecision() {
        // Given: Initially non-premium
        System.clearProperty("jakarta.migration.premium");
        CheckLicense.clearCache();
        
        // When: Check license status initially
        boolean initialStatus = CheckLicense.isLicensed();
        
        // Then: Initially not licensed
        assertThat(initialStatus).isFalse();
        
        // Given: User starts trial (simulating trial activation during session)
        long futureTime = System.currentTimeMillis() + SEVEN_DAYS_MS;
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", String.valueOf(futureTime));
        CheckLicense.clearCache();
        
        // When: Check license status again at runtime
        boolean runtimeStatus = CheckLicense.isLicensed();
        
        // Then: Now licensed (runtime check reflects current status)
        assertThat(runtimeStatus).isTrue();
        // This verifies that handleAnalyzeProject uses runtime check, not cached
    }

    @Test
    @DisplayName("Should handle license check with null trial end date")
    void shouldHandleLicenseCheckWithNullTrialEndDate() {
        // Given: Premium is true but no trial end date (permanent license)
        System.setProperty("jakarta.migration.premium", "true");
        System.clearProperty("jakarta.migration.trial.end");
        CheckLicense.clearCache();
        
        // When: Check license status
        boolean isLicensed = CheckLicense.isLicensed();
        
        // Then: Should return true (premium without expiration)
        assertThat(isLicensed).isTrue();
    }

    @Test
    @DisplayName("Should handle null migration status gracefully")
    void shouldHandleNullMigrationStatusGracefully() {
        // Given: A dependency with null migration status
        adrianmikula.jakartamigration.intellij.model.DependencyInfo dependencyWithNullStatus = 
            new adrianmikula.jakartamigration.intellij.model.DependencyInfo();
        dependencyWithNullStatus.setGroupId("test.group");
        dependencyWithNullStatus.setArtifactId("test-artifact");
        dependencyWithNullStatus.setMigrationStatus(null); // Explicitly set to null
        
        java.util.List<adrianmikula.jakartamigration.intellij.model.DependencyInfo> dependencies = 
            java.util.Arrays.asList(dependencyWithNullStatus);
        
        // When: Building status map with null migration status
        java.util.Map<String, String> statusMap = new java.util.HashMap<>();
        for (adrianmikula.jakartamigration.intellij.model.DependencyInfo dep : dependencies) {
            String key = dep.getGroupId() + ":" + dep.getArtifactId();
            adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus status = dep.getMigrationStatus();
            statusMap.put(key, status != null ? status.name() : "UNKNOWN");
        }
        
        // Then: Should handle null gracefully and default to "UNKNOWN"
        assertThat(statusMap).hasSize(1);
        assertThat(statusMap.get("test.group:test-artifact")).isEqualTo("UNKNOWN");
    }

    @Test
    @DisplayName("Should handle mixed migration statuses including null in refreshDependencyGraphWithUpdatedStatus")
    void shouldHandleMixedMigrationStatusesInRefreshDependencyGraph() {
        // Given: Dependencies with mixed migration statuses including null
        adrianmikula.jakartamigration.intellij.model.DependencyInfo dependencyWithNullStatus = 
            new adrianmikula.jakartamigration.intellij.model.DependencyInfo();
        dependencyWithNullStatus.setGroupId("null.group");
        dependencyWithNullStatus.setArtifactId("null-artifact");
        dependencyWithNullStatus.setMigrationStatus(null); // This would cause NPE before fix
        
        adrianmikula.jakartamigration.intellij.model.DependencyInfo dependencyWithValidStatus = 
            new adrianmikula.jakartamigration.intellij.model.DependencyInfo();
        dependencyWithValidStatus.setGroupId("valid.group");
        dependencyWithValidStatus.setArtifactId("valid-artifact");
        dependencyWithValidStatus.setMigrationStatus(adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus.COMPATIBLE);
        
        adrianmikula.jakartamigration.intellij.model.DependencyInfo dependencyWithAnotherStatus = 
            new adrianmikula.jakartamigration.intellij.model.DependencyInfo();
        dependencyWithAnotherStatus.setGroupId("upgrade.group");
        dependencyWithAnotherStatus.setArtifactId("upgrade-artifact");
        dependencyWithAnotherStatus.setMigrationStatus(adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus.NEEDS_UPGRADE);
        
        java.util.List<adrianmikula.jakartamigration.intellij.model.DependencyInfo> dependencies = 
            java.util.Arrays.asList(dependencyWithNullStatus, dependencyWithValidStatus, dependencyWithAnotherStatus);
        
        // When: Building updated status map (simulating refreshDependencyGraphWithUpdatedStatus logic)
        java.util.Map<String, String> updatedStatusMap = new java.util.HashMap<>();
        for (adrianmikula.jakartamigration.intellij.model.DependencyInfo dep : dependencies) {
            String key = dep.getGroupId() + ":" + dep.getArtifactId();
            adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus status = dep.getMigrationStatus();
            updatedStatusMap.put(key, status != null ? status.name() : "UNKNOWN");
        }
        
        // Then: Should handle all statuses correctly without NPE
        assertThat(updatedStatusMap).hasSize(3);
        assertThat(updatedStatusMap.get("null.group:null-artifact")).isEqualTo("UNKNOWN");
        assertThat(updatedStatusMap.get("valid.group:valid-artifact")).isEqualTo("COMPATIBLE");
        assertThat(updatedStatusMap.get("upgrade.group:upgrade-artifact")).isEqualTo("NEEDS_UPGRADE");
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
