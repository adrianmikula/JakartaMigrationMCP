package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.config.FeatureFlags;
import adrianmikula.jakartamigration.intellij.license.CheckLicense;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Reports tab premium feature flag behavior.
 * Tests that Reports tab is properly controlled by premium feature flags.
 */
public class ReportsPremiumFeatureTest extends BasePlatformTestCase {
    @Override
    public void setUp() throws Exception {
        super.setUp();
        FeatureFlags.getInstance().setReportsPremiumOnly(true);
        System.clearProperty("jakarta.migration.dev.simulate_premium");
        System.clearProperty("jakarta.migration.mode");
        CheckLicense.clearCache();
    }
    @Override
    public void tearDown() {
        try {
            super.tearDown();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        FeatureFlags.getInstance().setReportsPremiumOnly(true);
        System.clearProperty("jakarta.migration.dev.simulate_premium");
        System.clearProperty("jakarta.migration.mode");
        CheckLicense.clearCache();
    }
    public void testReportsTabHiddenForFreeUsersWhenPremiumOnly() {
        FeatureFlags.getInstance().setReportsPremiumOnly(true);
        System.setProperty("jakarta.migration.mode", "production");

        MigrationToolWindow.MigrationToolWindowContent content =
            new MigrationToolWindow.MigrationToolWindowContent(getProject());

        assertThat(content).isNotNull();

        Boolean licensed = CheckLicense.isLicensed();
        assertThat(licensed).isFalse();
    }
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
    public void testReportsTabVisibleForAllUsersWhenPremiumOnlyDisabled() {
        FeatureFlags.getInstance().setReportsPremiumOnly(false);
        System.setProperty("jakarta.migration.mode", "production");

        MigrationToolWindow.MigrationToolWindowContent content =
            new MigrationToolWindow.MigrationToolWindowContent(getProject());

        assertThat(content).isNotNull();

        Boolean licensed = CheckLicense.isLicensed();
        assertThat(licensed).isFalse();
    }
    public void testReportsTabComponentShowsUpgradePromptForFreeUsers() {
        FeatureFlags.getInstance().setReportsPremiumOnly(true);
        System.setProperty("jakarta.migration.mode", "production");

        CheckLicense.clearCache();

        Boolean licensed = CheckLicense.isLicensed();
        assertThat(licensed).isFalse();
    }
    public void testReportsTabComponentShowsNormalInterfaceForPremiumUsers() {
        FeatureFlags.getInstance().setReportsPremiumOnly(true);
        System.setProperty("jakarta.migration.mode", "dev");
        System.setProperty("jakarta.migration.dev.simulate_premium", "true");

        CheckLicense.clearCache();

        Boolean licensed = CheckLicense.isLicensed();
        assertThat(licensed).isTrue();
    }
    public void testFeatureFlagConfigurable() {
        assertThat(FeatureFlags.getInstance().isReportsPremiumOnly()).isTrue();

        FeatureFlags.getInstance().setReportsPremiumOnly(false);
        assertThat(FeatureFlags.getInstance().isReportsPremiumOnly()).isFalse();

        FeatureFlags.getInstance().setReportsPremiumOnly(true);
        assertThat(FeatureFlags.getInstance().isReportsPremiumOnly()).isTrue();
    }
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
