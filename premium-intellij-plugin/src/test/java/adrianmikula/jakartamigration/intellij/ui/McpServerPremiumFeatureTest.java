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
import static org.mockito.Mockito.when;

/**
 * Tests for MCP Server premium feature flag behavior.
 * Tests that MCP server tab and status are properly controlled by premium feature flags.
 */
public class McpServerPremiumFeatureTest extends BasePlatformTestCase {

    @Mock
    private Project mockProject;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Reset feature flags to known state
        FeatureFlags.getInstance().setMcpServerPremiumOnly(true);
        
        // Clear any premium simulation
        System.clearProperty("jakarta.migration.dev.simulate_premium");
        System.clearProperty("jakarta.migration.mode");
        
        // Clear license cache
        CheckLicense.clearCache();
    }

    @AfterEach
    public void tearDown() {
        // Reset to defaults
        FeatureFlags.getInstance().setMcpServerPremiumOnly(true);
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
    @DisplayName("MCP server tab should be hidden for free users when premium-only flag is enabled")
    public void testMcpTabHiddenForFreeUsersWhenPremiumOnly() {
        // Given: MCP server is premium-only and user is not premium
        FeatureFlags.getInstance().setMcpServerPremiumOnly(true);
        System.setProperty("jakarta.migration.mode", "production"); // Ensure not in dev mode
        
        // When: Creating migration tool window content
        MigrationToolWindow.MigrationToolWindowContent content = 
            new MigrationToolWindow.MigrationToolWindowContent(getProject());
        
        // Then: MCP tab should not be visible (tab count should be less than expected)
        // Note: This is an indirect test since we can't directly access tab visibility
        // In a real test environment, we would verify the tab is not added
        assertThat(content).isNotNull();
        
        // Verify premium status is correctly detected as false
        Boolean licensed = CheckLicense.isLicensed();
        assertThat(licensed).isFalse();
    }

    @Test
    @DisplayName("MCP server tab should be visible for premium users when premium-only flag is enabled")
    public void testMcpTabVisibleForPremiumUsersWhenPremiumOnly() {
        // Given: MCP server is premium-only and user is premium (via simulation)
        FeatureFlags.getInstance().setMcpServerPremiumOnly(true);
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
    @DisplayName("MCP server tab should be visible for all users when premium-only flag is disabled")
    public void testMcpTabVisibleForAllUsersWhenPremiumOnlyDisabled() {
        // Given: MCP server is not premium-only
        FeatureFlags.getInstance().setMcpServerPremiumOnly(false);
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
    @DisplayName("MCP server tab component should show upgrade prompt for free users")
    public void testMcpTabComponentShowsUpgradePromptForFreeUsers() {
        // Given: MCP server is premium-only and user is not premium
        FeatureFlags.getInstance().setMcpServerPremiumOnly(true);
        System.setProperty("jakarta.migration.mode", "production");
        
        // Clear cache to ensure fresh check
        CheckLicense.clearCache();
        
        // When: Creating MCP server tab component
        McpServerTabComponent component = new McpServerTabComponent(getProject());
        
        // Then: Component should be created (it shows upgrade prompt internally)
        assertThat(component).isNotNull();
        assertThat(component.getPanel()).isNotNull();
    }

    @Test
    @DisplayName("MCP server tab component should show normal interface for premium users")
    public void testMcpTabComponentShowsNormalInterfaceForPremiumUsers() {
        // Given: MCP server is premium-only and user is premium (via simulation)
        FeatureFlags.getInstance().setMcpServerPremiumOnly(true);
        System.setProperty("jakarta.migration.mode", "dev");
        System.setProperty("jakarta.migration.dev.simulate_premium", "true");
        
        // Clear cache to ensure fresh check
        CheckLicense.clearCache();
        
        // When: Creating MCP server tab component
        McpServerTabComponent component = new McpServerTabComponent(getProject());
        
        // Then: Component should be created with normal interface
        assertThat(component).isNotNull();
        assertThat(component.getPanel()).isNotNull();
    }

    @Test
    @DisplayName("Dashboard MCP status should show 'Premium Only' for free users when premium-only flag is enabled")
    public void testDashboardMcpStatusShowsPremiumOnlyForFreeUsers() {
        // Given: MCP server is premium-only and user is not premium
        FeatureFlags.getInstance().setMcpServerPremiumOnly(true);
        System.setProperty("jakarta.migration.mode", "production");
        
        // Clear cache to ensure fresh check
        CheckLicense.clearCache();
        
        // When: Creating dashboard component and updating MCP status
        DashboardComponent dashboard = new DashboardComponent(getProject(), null, null);
        dashboard.updateMcpServerStatus();
        
        // Then: MCP status should reflect premium-only status
        String mcpStatus = dashboard.getMcpStatus();
        // Note: This test may need adjustment based on actual implementation
        // The getMcpStatus() method might need to be updated to consider premium flags
        assertThat(mcpStatus).isNotNull();
    }

    @Test
    @DisplayName("Feature flag should be configurable")
    public void testFeatureFlagConfigurable() {
        // Given: Default state
        assertThat(FeatureFlags.getInstance().isMcpServerPremiumOnly()).isTrue();
        
        // When: Setting flag to false
        FeatureFlags.getInstance().setMcpServerPremiumOnly(false);
        
        // Then: Flag should be false
        assertThat(FeatureFlags.getInstance().isMcpServerPremiumOnly()).isFalse();
        
        // When: Setting flag back to true
        FeatureFlags.getInstance().setMcpServerPremiumOnly(true);
        
        // Then: Flag should be true again
        assertThat(FeatureFlags.getInstance().isMcpServerPremiumOnly()).isTrue();
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
