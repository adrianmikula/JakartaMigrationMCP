package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.JakartaMcpRegistrationActivity;
import adrianmikula.jakartamigration.intellij.mcp.JakartaMcpServerProvider;
import adrianmikula.jakartamigration.intellij.config.FeatureFlags;
import adrianmikula.jakartamigration.intellij.license.CheckLicense;
import com.intellij.openapi.diagnostic.Logger;

import javax.swing.*;
import java.awt.*;

/**
 * Manages MCP server status display updates for the dashboard.
 * Extracted from DashboardComponent to reduce class size.
 */
public class DashboardMcpStatusUpdater {
    private static final Logger LOG = Logger.getInstance(DashboardMcpStatusUpdater.class);

    private final JLabel mcpStatusIndicator;
    private final JLabel mcpStatusValue;
    private final JLabel mcpToolsValue;
    private final JLabel mcpServerVersionValue;

    public DashboardMcpStatusUpdater(JLabel mcpStatusIndicator, JLabel mcpStatusValue, 
                                      JLabel mcpToolsValue, JLabel mcpServerVersionValue) {
        this.mcpStatusIndicator = mcpStatusIndicator;
        this.mcpStatusValue = mcpStatusValue;
        this.mcpToolsValue = mcpToolsValue;
        this.mcpServerVersionValue = mcpServerVersionValue;
    }

    /**
     * Updates the MCP server status display.
     * Called on initialization and can be called to refresh.
     */
    public void updateMcpServerStatus() {
        SwingUtilities.invokeLater(() -> {
            // Check if MCP server is premium-only and user is not premium
            boolean mcpServerPremiumOnly = FeatureFlags.getInstance().isMcpServerPremiumOnly();
            boolean isPremium = CheckLicense.isLicensed();
            
            if (mcpServerPremiumOnly && !isPremium) {
                // MCP server is premium-only but user is not premium
                if (mcpStatusIndicator != null) {
                    mcpStatusIndicator.setForeground(new Color(255, 140, 0)); // Orange for premium required
                }
                if (mcpStatusValue != null) {
                    mcpStatusValue.setText("Premium Only");
                    mcpStatusValue.setForeground(new Color(255, 140, 0));
                }
                if (mcpToolsValue != null) {
                    mcpToolsValue.setText("🔒");
                    mcpToolsValue.setForeground(new Color(255, 140, 0));
                }
                if (mcpServerVersionValue != null) {
                    mcpServerVersionValue.setText("-");
                }
                LOG.info("MCP Server Status: Premium Only - user is not premium and MCP server is premium-only feature");
                return;
            }

            JakartaMcpServerProvider provider = JakartaMcpRegistrationActivity.getServerProvider();

            if (provider != null && provider.isReady()) {
                // MCP is connected and ready
                if (mcpStatusIndicator != null) {
                    mcpStatusIndicator.setForeground(new Color(0, 180, 0));
                }
                if (mcpStatusValue != null) {
                    mcpStatusValue.setText("Connected");
                    mcpStatusValue.setForeground(new Color(0, 120, 0));
                }

                int toolCount = provider.getToolCount();
                if (mcpToolsValue != null) {
                    mcpToolsValue.setText(String.valueOf(toolCount));
                    mcpToolsValue.setForeground(new Color(0, 100, 200));
                }

                if (mcpServerVersionValue != null) {
                    mcpServerVersionValue.setText(provider.getServerVersion());
                }

                LOG.info("MCP Server Status: Connected with " + toolCount + " tools");
            } else if (provider != null) {
                // MCP provider exists but not ready
                if (mcpStatusIndicator != null) {
                    mcpStatusIndicator.setForeground(Color.ORANGE);
                }
                if (mcpStatusValue != null) {
                    mcpStatusValue.setText("Initializing");
                    mcpStatusValue.setForeground(Color.ORANGE);
                }

                if (mcpToolsValue != null) {
                    mcpToolsValue.setText("-");
                    mcpToolsValue.setForeground(Color.GRAY);
                }

                if (mcpServerVersionValue != null) {
                    mcpServerVersionValue.setText(provider.getServerVersion());
                }

                LOG.info("MCP Server Status: Provider exists but not ready");
            } else {
                // MCP provider not initialized - check if AI Assistant is available
                if (mcpStatusIndicator != null) {
                    mcpStatusIndicator.setForeground(Color.GRAY);
                }
                if (mcpStatusValue != null) {
                    mcpStatusValue.setText("Not Available");
                    mcpStatusValue.setForeground(Color.GRAY);
                }

                if (mcpToolsValue != null) {
                    mcpToolsValue.setText("-");
                }
                if (mcpServerVersionValue != null) {
                    mcpServerVersionValue.setText("1.0.0");
                }

                LOG.info("MCP Server Status: Provider not initialized - AI Assistant may not be active");
            }
        });
    }

    /**
     * Gets the current MCP server status as a string.
     * 
     * @return "Connected", "Not Ready", or "Not Initialized"
     */
    public String getMcpStatus() {
        JakartaMcpServerProvider provider = JakartaMcpRegistrationActivity.getServerProvider();
        if (provider != null && provider.isReady()) {
            return "Connected";
        } else if (provider != null) {
            return "Not Ready";
        } else {
            return "Not Initialized";
        }
    }

    /**
     * Gets the number of loaded MCP tools.
     * 
     * @return Number of tools, or 0 if not ready
     */
    public int getMcpToolCount() {
        JakartaMcpServerProvider provider = JakartaMcpRegistrationActivity.getServerProvider();
        if (provider != null && provider.isReady()) {
            return provider.getToolCount();
        }
        return 0;
    }
}
