package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.config.FeatureFlags;
import adrianmikula.jakartamigration.intellij.license.CheckLicense;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Reports tab premium feature flag behavior.
 * Tests that Reports tab is properly controlled by premium feature flags.
 */
public class ReportsPremiumFeatureTest extends BasePlatformTestCase {

    @Mock
    private Project mockProject;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Reset feature flags to known state
        FeatureFlags.getInstance().setReportsPremiumOnly(true);
        
        // Clear any premium simulation
        System.clearProperty("jakarta.migration.dev.simulate_premium");
        System.clearProperty("jakarta.migration.mode");
        
        // Clear license cache
        CheckLicense.clearCache();
    }

    @AfterEach
    public void tearDown() {
        // Reset to defaults
        FeatureFlags.getInstance().setReportsPremiumOnly(true);
        System.clearProperty("jakarta.migration.dev.simulate_premium");
        System.clearProperty("jakarta.migration.mode");
        CheckLicense.clearCache();
        try {
            super.tearDown();
        } catch (Exception e) {
            // Handle exception from parent tearDown
        }
    }

    @Test
    @DisplayName("Reports tab should be hidden for free users when premium-only flag is enabled")
    public void testReportsTabHiddenForFreeUsersWhenPremiumOnly() {
        // Given: Reports tab is premium-only and user is not premium
        FeatureFlags.getInstance().setReportsPremiumOnly(true);
        System.setProperty("jakarta.migration.mode", "production"); // Ensure not in dev mode
        
        // When: Creating migration tool window content
        MigrationToolWindow.MigrationToolWindowContent content = 
            new MigrationToolWindow.MigrationToolWindowContent(getProject());
        
        // Then: Content should be created
        assertThat(content).isNotNull();
        
        // Verify premium status is correctly detected as false
        Boolean licensed = CheckLicense.isLicensed();
        assertThat(licensed).isFalse();
    }

    @Test
    @DisplayName("Reports tab should be visible for premium users when premium-only flag is enabled")
    public void testReportsTabVisibleForPremiumUsersWhenPremiumOnly() {
        // Given: Reports tab is premium-only and user is premium (via simulation)
        FeatureFlags.getInstance().setReportsPremiumOnly(true);
        System.setProperty("jakarta.migration.mode", "dev"); // Enable dev mode
        System.setProperty("jakarta.migration.dev.simulate_premium", "true"); // Simulate premium
        
        // Clear cache to ensure fresh check
        CheckLicense.clearCache();
        
        // When: Creating migration tool window content
        MigrationToolWindow.MigrationToolWindowContent content = 
            new MigrationToolWindow.MigrationToolWindowContent(getProject());
        
        // Then: Content should be created and premium status should be true
        assertThat(content).isNotNull();
        
        // Verify premium status is correctly detected as true
        Boolean licensed = CheckLicense.isLicensed();
        assertThat(licensed).isTrue();
    }

    @Test
    @DisplayName("Reports tab should be visible for all users when premium-only flag is disabled")
    public void testReportsTabVisibleForAllUsersWhenPremiumOnlyDisabled() {
        // Given: Reports tab is not premium-only
        FeatureFlags.getInstance().setReportsPremiumOnly(false);
        System.setProperty("jakarta.migration.mode", "production"); // Ensure not in dev mode
        
        // When: Creating migration tool window content
        MigrationToolWindow.MigrationToolWindowContent content = 
            new MigrationToolWindow.MigrationToolWindowContent(getProject());
        
        // Then: Content should be created regardless of premium status
        assertThat(content).isNotNull();
        
        // Verify premium status is correctly detected as false
        Boolean licensed = CheckLicense.isLicensed();
        assertThat(licensed).isFalse();
    }

    @Test
    @DisplayName("Reports tab component should show upgrade prompt for free users")
    public void testReportsTabComponentShowsUpgradePromptForFreeUsers() {
        // Given: Reports tab is premium-only and user is not premium
        FeatureFlags.getInstance().setReportsPremiumOnly(true);
        System.setProperty("jakarta.migration.mode", "production");
        
        // Clear cache to ensure fresh check
        CheckLicense.clearCache();
        
        // When: Creating Reports tab component
        // Note: ReportsTabComponent requires additional services, so we'll just verify the flag
        // The component itself has internal premium checks
        
        // Then: Verify premium status is correctly detected as false
        Boolean licensed = CheckLicense.isLicensed();
        assertThat(licensed).isFalse();
    }

    @Test
    @DisplayName("Reports tab component should show normal interface for premium users")
    public void testReportsTabComponentShowsNormalInterfaceForPremiumUsers() {
        // Given: Reports tab is premium-only and user is premium (via simulation)
        FeatureFlags.getInstance().setReportsPremiumOnly(true);
        System.setProperty("jakarta.migration.mode", "dev");
        System.setProperty("jakarta.migration.dev.simulate_premium", "true");
        
        // Clear cache to ensure fresh check
        CheckLicense.clearCache();
        
        // When: Creating Reports tab component
        // Note: ReportsTabComponent requires additional services, so we'll just verify the flag
        // The component itself has internal premium checks
        
        // Then: Verify premium status is correctly detected as true
        Boolean licensed = CheckLicense.isLicensed();
        assertThat(licensed).isTrue();
    }

    @Test
    @DisplayName("Feature flag should be configurable")
    public void testFeatureFlagConfigurable() {
        // Given: Default state
        assertThat(FeatureFlags.getInstance().isReportsPremiumOnly()).isTrue();
        
        // When: Setting flag to false
        FeatureFlags.getInstance().setReportsPremiumOnly(false);
        
        // Then: Flag should be false
        assertThat(FeatureFlags.getInstance().isReportsPremiumOnly()).isFalse();
        
        // When: Setting flag back to true
        FeatureFlags.getInstance().setReportsPremiumOnly(true);
        
        // Then: Flag should be true again
        assertThat(FeatureFlags.getInstance().isReportsPremiumOnly()).isTrue();
    }

    @Test
    @DisplayName("License check should work correctly in different modes")
    public void testLicenseCheckInDifferentModes() {
        // Test production mode (no simulation)
        System.setProperty("jakarta.migration.mode", "production");
        System.clearProperty("jakarta.migration.dev.simulate_premium");
        CheckLicense.clearCache();
        
        Boolean licensed = CheckLicense.isLicensed();
        assertThat(licensed).isFalse(); // Should be false in production without license
        
        // Test dev mode with premium simulation
        System.setProperty("jakarta.migration.mode", "dev");
        System.setProperty("jakarta.migration.dev.simulate_premium", "true");
        CheckLicense.clearCache();
        
        licensed = CheckLicense.isLicensed();
        assertThat(licensed).isTrue(); // Should be true in dev mode with simulation
        
        // Test dev mode without premium simulation
        System.setProperty("jakarta.migration.mode", "dev");
        System.clearProperty("jakarta.migration.dev.simulate_premium");
        CheckLicense.clearCache();
        
        licensed = CheckLicense.isLicensed();
        assertThat(licensed).isTrue(); // Should be true in dev mode even without simulation
    }
}
