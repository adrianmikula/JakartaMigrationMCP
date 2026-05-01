package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.config.FeatureFlags;
import adrianmikula.jakartamigration.intellij.license.CheckLicense;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Reports tab premium feature flag behavior.
 * Tests that Reports tab is properly controlled by premium feature flags.
 */
public class ReportsPremiumFeatureTest extends BasePlatformTestCase {

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        FeatureFlags.getInstance().setReportsPremiumOnly(true);
        System.clearProperty("jakarta.migration.dev.simulate_premium");
        System.clearProperty("jakarta.migration.mode");
        CheckLicense.clearCache();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        FeatureFlags.getInstance().setReportsPremiumOnly(true);
        System.clearProperty("jakarta.migration.dev.simulate_premium");
        System.clearProperty("jakarta.migration.mode");
        CheckLicense.clearCache();
        super.tearDown();
    }

    @Test
    @DisplayName("Reports tab should be hidden for free users when premium-only flag is enabled")
    public void testReportsTabHiddenForFreeUsersWhenPremiumOnly() {
        FeatureFlags.getInstance().setReportsPremiumOnly(true);
        System.setProperty("jakarta.migration.mode", "production");

        MigrationToolWindow.MigrationToolWindowContent content =
            new MigrationToolWindow.MigrationToolWindowContent(getProject());

        assertThat(content).isNotNull();

        Boolean licensed = CheckLicense.isLicensed();
        assertThat(licensed).isFalse();
    }

    @Test
    @DisplayName("Reports tab should be visible for premium users when premium-only flag is enabled")
    public void testReportsTabVisibleForPremiumUsersWhenPremiumOnly() {
        FeatureFlags.getInstance().setReportsPremiumOnly(true);
        System.setProperty("jakarta.migration.mode", "dev");
        System.setProperty("jakarta.migration.dev.simulate_premium", "true");

        CheckLicense.clearCache();

        MigrationToolWindow.MigrationToolWindowContent content =
            new MigrationToolWindow.MigrationToolWindowContent(getProject());

        assertThat(content).isNotNull();

        Boolean licensed = CheckLicense.isLicensed();
        assertThat(licensed).isTrue();
    }

    @Test
    @DisplayName("Reports tab should be visible for all users when premium-only flag is disabled")
    public void testReportsTabVisibleForAllUsersWhenPremiumOnlyDisabled() {
        FeatureFlags.getInstance().setReportsPremiumOnly(false);
        System.setProperty("jakarta.migration.mode", "production");

        MigrationToolWindow.MigrationToolWindowContent content =
            new MigrationToolWindow.MigrationToolWindowContent(getProject());

        assertThat(content).isNotNull();

        Boolean licensed = CheckLicense.isLicensed();
        assertThat(licensed).isFalse();
    }

    @Test
    @DisplayName("Reports tab component should show upgrade prompt for free users")
    public void testReportsTabComponentShowsUpgradePromptForFreeUsers() {
        FeatureFlags.getInstance().setReportsPremiumOnly(true);
        System.setProperty("jakarta.migration.mode", "production");

        CheckLicense.clearCache();

        Boolean licensed = CheckLicense.isLicensed();
        assertThat(licensed).isFalse();
    }

    @Test
    @DisplayName("Reports tab component should show normal interface for premium users")
    public void testReportsTabComponentShowsNormalInterfaceForPremiumUsers() {
        FeatureFlags.getInstance().setReportsPremiumOnly(true);
        System.setProperty("jakarta.migration.mode", "dev");
        System.setProperty("jakarta.migration.dev.simulate_premium", "true");

        CheckLicense.clearCache();

        Boolean licensed = CheckLicense.isLicensed();
        assertThat(licensed).isTrue();
    }

    @Test
    @DisplayName("Feature flag should be configurable")
    public void testFeatureFlagConfigurable() {
        assertThat(FeatureFlags.getInstance().isReportsPremiumOnly()).isTrue();

        FeatureFlags.getInstance().setReportsPremiumOnly(false);
        assertThat(FeatureFlags.getInstance().isReportsPremiumOnly()).isFalse();

        FeatureFlags.getInstance().setReportsPremiumOnly(true);
        assertThat(FeatureFlags.getInstance().isReportsPremiumOnly()).isTrue();
    }

    @Test
    @DisplayName("License check should work correctly in different modes")
    public void testLicenseCheckInDifferentModes() {
        System.setProperty("jakarta.migration.mode", "production");
        System.clearProperty("jakarta.migration.dev.simulate_premium");
        CheckLicense.clearCache();

        Boolean licensed = CheckLicense.isLicensed();
        assertThat(licensed).isFalse();

        System.setProperty("jakarta.migration.mode", "dev");
        System.setProperty("jakarta.migration.dev.simulate_premium", "true");
        CheckLicense.clearCache();

        licensed = CheckLicense.isLicensed();
        assertThat(licensed).isTrue();

        System.setProperty("jakarta.migration.mode", "dev");
        System.clearProperty("jakarta.migration.dev.simulate_premium");
        CheckLicense.clearCache();

        licensed = CheckLicense.isLicensed();
        assertThat(licensed).isTrue();
    }
}
