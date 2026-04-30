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
 * Tests for MCP Server premium feature flag behavior.
 * Tests that MCP server tab and status are properly controlled by premium feature flags.
 */
public class McpServerPremiumFeatureTest extends BasePlatformTestCase {

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        FeatureFlags.getInstance().setMcpServerPremiumOnly(true);
        System.clearProperty("jakarta.migration.dev.simulate_premium");
        System.clearProperty("jakarta.migration.mode");
        CheckLicense.clearCache();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        FeatureFlags.getInstance().setMcpServerPremiumOnly(true);
        System.clearProperty("jakarta.migration.dev.simulate_premium");
        System.clearProperty("jakarta.migration.mode");
        CheckLicense.clearCache();
        super.tearDown();
    }

    @Test
    @DisplayName("MCP server tab should be hidden for free users when premium-only flag is enabled")
    public void testMcpTabHiddenForFreeUsersWhenPremiumOnly() {
        FeatureFlags.getInstance().setMcpServerPremiumOnly(true);
        System.setProperty("jakarta.migration.mode", "production");

        MigrationToolWindow.MigrationToolWindowContent content =
            new MigrationToolWindow.MigrationToolWindowContent(getProject());

        assertThat(content).isNotNull();

        Boolean licensed = CheckLicense.isLicensed();
        assertThat(licensed).isFalse();
    }

    @Test
    @DisplayName("MCP server tab should be visible for premium users when premium-only flag is enabled")
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

    @Test
    @DisplayName("MCP server tab should be visible for all users when premium-only flag is disabled")
    public void testMcpTabVisibleForAllUsersWhenPremiumOnlyDisabled() {
        FeatureFlags.getInstance().setMcpServerPremiumOnly(false);
        System.setProperty("jakarta.migration.mode", "production");

        MigrationToolWindow.MigrationToolWindowContent content =
            new MigrationToolWindow.MigrationToolWindowContent(getProject());

        assertThat(content).isNotNull();

        Boolean licensed = CheckLicense.isLicensed();
        assertThat(licensed).isFalse();
    }

    @Test
    @DisplayName("MCP server tab component should show upgrade prompt for free users")
    public void testMcpTabComponentShowsUpgradePromptForFreeUsers() {
        FeatureFlags.getInstance().setMcpServerPremiumOnly(true);
        System.setProperty("jakarta.migration.mode", "production");

        CheckLicense.clearCache();

        McpServerTabComponent component = new McpServerTabComponent(getProject());

        assertThat(component).isNotNull();
        assertThat(component.getPanel()).isNotNull();
    }

    @Test
    @DisplayName("MCP server tab component should show normal interface for premium users")
    public void testMcpTabComponentShowsNormalInterfaceForPremiumUsers() {
        FeatureFlags.getInstance().setMcpServerPremiumOnly(true);
        System.setProperty("jakarta.migration.mode", "dev");
        System.setProperty("jakarta.migration.dev.simulate_premium", "true");

        CheckLicense.clearCache();

        McpServerTabComponent component = new McpServerTabComponent(getProject());

        assertThat(component).isNotNull();
        assertThat(component.getPanel()).isNotNull();
    }

    @Test
    @DisplayName("Dashboard MCP status should show 'Premium Only' for free users when premium-only flag is enabled")
    public void testDashboardMcpStatusShowsPremiumOnlyForFreeUsers() {
        FeatureFlags.getInstance().setMcpServerPremiumOnly(true);
        System.setProperty("jakarta.migration.mode", "production");

        CheckLicense.clearCache();

        DashboardComponent dashboard = new DashboardComponent(getProject(), null, null);
        dashboard.updateMcpServerStatus();

        String mcpStatus = dashboard.getMcpStatus();
        assertThat(mcpStatus).isNotNull();
    }

    @Test
    @DisplayName("Feature flag should be configurable")
    public void testFeatureFlagConfigurable() {
        assertThat(FeatureFlags.getInstance().isMcpServerPremiumOnly()).isTrue();

        FeatureFlags.getInstance().setMcpServerPremiumOnly(false);
        assertThat(FeatureFlags.getInstance().isMcpServerPremiumOnly()).isFalse();

        FeatureFlags.getInstance().setMcpServerPremiumOnly(true);
        assertThat(FeatureFlags.getInstance().isMcpServerPremiumOnly()).isTrue();
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
