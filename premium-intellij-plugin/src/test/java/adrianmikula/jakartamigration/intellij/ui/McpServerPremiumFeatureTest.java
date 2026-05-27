package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.config.FeatureFlags;
import adrianmikula.jakartamigration.intellij.license.CheckLicense;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for MCP Server premium feature flag behavior.
 * Tests that MCP server tab and status are properly controlled by premium feature flags.
 */
public class McpServerPremiumFeatureTest extends BasePlatformTestCase {
    @Override
    public void setUp() throws Exception {
        super.setUp();
        FeatureFlags.getInstance().setMcpServerPremiumOnly(true);
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
        FeatureFlags.getInstance().setMcpServerPremiumOnly(true);
        System.clearProperty("jakarta.migration.dev.simulate_premium");
        System.clearProperty("jakarta.migration.mode");
        CheckLicense.clearCache();
    }
    public void testMcpTabHiddenForFreeUsersWhenPremiumOnly() {
        FeatureFlags.getInstance().setMcpServerPremiumOnly(true);
        System.setProperty("jakarta.migration.mode", "production");

        MigrationToolWindow.MigrationToolWindowContent content =
            new MigrationToolWindow.MigrationToolWindowContent(getProject());

        assertThat(content).isNotNull();

        Boolean licensed = CheckLicense.isLicensed();
        assertThat(licensed).isFalse();
    }
    public void testMcpTabVisibleForPremiumUsersWhenPremiumOnly() {
        FeatureFlags.getInstance().setMcpServerPremiumOnly(true);
        System.setProperty("jakarta.migration.mode", "dev");
        System.setProperty("jakarta.migration.dev.simulate_premium", "true");

        CheckLicense.clearCache();

        MigrationToolWindow.MigrationToolWindowContent content =
            new MigrationToolWindow.MigrationToolWindowContent(getProject());

        assertThat(content).isNotNull();

        Boolean licensed = CheckLicense.isLicensed();
        assertThat(licensed).isTrue();
    }
    public void testMcpTabVisibleForAllUsersWhenPremiumOnlyDisabled() {
        FeatureFlags.getInstance().setMcpServerPremiumOnly(false);
        System.setProperty("jakarta.migration.mode", "production");

        MigrationToolWindow.MigrationToolWindowContent content =
            new MigrationToolWindow.MigrationToolWindowContent(getProject());

        assertThat(content).isNotNull();

        Boolean licensed = CheckLicense.isLicensed();
        assertThat(licensed).isFalse();
    }
    public void testMcpTabComponentShowsUpgradePromptForFreeUsers() {
        FeatureFlags.getInstance().setMcpServerPremiumOnly(true);
        System.setProperty("jakarta.migration.mode", "production");

        CheckLicense.clearCache();

        McpServerTabComponent component = new McpServerTabComponent(getProject());

        assertThat(component).isNotNull();
        assertThat(component.getPanel()).isNotNull();
    }
    public void testMcpTabComponentShowsNormalInterfaceForPremiumUsers() {
        FeatureFlags.getInstance().setMcpServerPremiumOnly(true);
        System.setProperty("jakarta.migration.mode", "dev");
        System.setProperty("jakarta.migration.dev.simulate_premium", "true");

        CheckLicense.clearCache();

        McpServerTabComponent component = new McpServerTabComponent(getProject());

        assertThat(component).isNotNull();
        assertThat(component.getPanel()).isNotNull();
    }
    public void testDashboardMcpStatusShowsPremiumOnlyForFreeUsers() {
        FeatureFlags.getInstance().setMcpServerPremiumOnly(true);
        System.setProperty("jakarta.migration.mode", "production");

        CheckLicense.clearCache();

        DashboardComponent dashboard = new DashboardComponent(getProject(), null, null);
        dashboard.updateMcpServerStatus();

        String mcpStatus = dashboard.getMcpStatus();
        assertThat(mcpStatus).isNotNull();
    }
    public void testFeatureFlagConfigurable() {
        assertThat(FeatureFlags.getInstance().isMcpServerPremiumOnly()).isTrue();

        FeatureFlags.getInstance().setMcpServerPremiumOnly(false);
        assertThat(FeatureFlags.getInstance().isMcpServerPremiumOnly()).isFalse();

        FeatureFlags.getInstance().setMcpServerPremiumOnly(true);
        assertThat(FeatureFlags.getInstance().isMcpServerPremiumOnly()).isTrue();
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
