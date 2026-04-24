package adrianmikula.jakartamigration.intellij.ui.components;

import adrianmikula.jakartamigration.analytics.service.UsageService;
import adrianmikula.jakartamigration.analytics.service.UserIdentificationService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Utility class for creating consistent premium upgrade buttons across UI components.
 * Provides factory methods for creating upgrade buttons with standard styling and behavior.
 */
public class PremiumUpgradeButton {

    // Standard colors for premium upgrade buttons
    private static final Color YELLOW_BACKGROUND = new Color(255, 215, 0);
    private static final Color YELLOW_BORDER = new Color(255, 152, 0);
    private static final Color DARK_TEXT = new Color(80, 60, 0);
    private static final Color PREMIUM_GOLD = new Color(255, 215, 0);
    
    // Standard text and URLs
    private static final String UPGRADE_BUTTON_TEXT = "⬆ Upgrade to Premium";
    private static final String UPGRADE_TOOLTIP = "Get Premium features: Auto-fixes, one-click refactoring, binary fixes";
    private static final String MARKETPLACE_URL = "https://plugins.jetbrains.com/plugin/30093-jakarta-migration";
    private static final String PREMIUM_BADGE_TEXT = "⭐ Premium Active";
    private static final String PREMIUM_BADGE_TOOLTIP = "Premium license active";

    /**
     * Creates a standard premium upgrade button with yellow background and border.
     * 
     * @param project the IntelliJ project for error handling
     * @param currentUiTab the current UI tab where the button is displayed
     * @return a configured JButton with upgrade functionality
     */
    public static JButton createUpgradeButton(Project project, String currentUiTab) {
        return createUpgradeButton(project, "premium_button_factory", UPGRADE_BUTTON_TEXT, UPGRADE_TOOLTIP, currentUiTab);
    }

    /**
     * Creates a premium upgrade button with custom text.
     * 
     * @param project the IntelliJ project for error handling
     * @param buttonText custom text for the button
     * @param tooltip custom tooltip text
     * @param currentUiTab the current UI tab where the button is displayed
     * @return a configured JButton with upgrade functionality
     */
    public static JButton createUpgradeButton(Project project, String buttonText, String tooltip, String currentUiTab) {
        return createUpgradeButton(project, "premium_button_factory_custom", buttonText, tooltip, currentUiTab);
    }
    
    /**
     * Creates a premium upgrade button with analytics tracking.
     * 
     * @param project the IntelliJ project for error handling
     * @param analyticsSource the source identifier for analytics tracking
     * @param buttonText custom text for the button
     * @param tooltip custom tooltip text
     * @param currentUiTab the current UI tab where the button is displayed
     * @return a configured JButton with upgrade functionality
     */
    public static JButton createUpgradeButton(Project project, String analyticsSource, String buttonText, String tooltip, String currentUiTab) {
        JButton upgradeButton = new JButton(buttonText);
        upgradeButton.setToolTipText(tooltip);
        upgradeButton.setBackground(YELLOW_BACKGROUND);
        upgradeButton.setForeground(DARK_TEXT);
        upgradeButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(YELLOW_BORDER, 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        upgradeButton.setFont(upgradeButton.getFont().deriveFont(Font.BOLD));
        upgradeButton.setFocusPainted(false);
        
        // Add click handler with analytics tracking
        upgradeButton.addActionListener(e -> {
            // Track upgrade click analytics
            trackUpgradeClick(analyticsSource, currentUiTab);
            
            // Open marketplace
            try {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(MARKETPLACE_URL));
            } catch (Exception ex) {
                Messages.showErrorDialog(project, "Could not open upgrade URL", "Error");
            }
        });
        
        return upgradeButton;
    }

    /**
     * Creates a premium badge label for users who already have premium.
     * 
     * @return a configured JLabel showing premium status
     */
    public static JLabel createPremiumBadge() {
        JLabel premiumBadge = new JLabel(PREMIUM_BADGE_TEXT);
        premiumBadge.setForeground(PREMIUM_GOLD);
        premiumBadge.setToolTipText(PREMIUM_BADGE_TOOLTIP);
        premiumBadge.setFont(premiumBadge.getFont().deriveFont(Font.BOLD));
        return premiumBadge;
    }

    /**
     * Creates a panel containing either an upgrade button or premium badge based on license status.
     * This is a convenience method for conditional display.
     * 
     * @param project the IntelliJ project
     * @return a JPanel containing the appropriate component
     */
    public static JPanel createConditionalUpgradePanel(Project project) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        
        boolean isPremium = adrianmikula.jakartamigration.intellij.license.CheckLicense.isLicensed();
        
        if (!isPremium) {
            panel.add(createUpgradeButton(project, "PremiumUpgradePanel"));
        } else {
            panel.add(createPremiumBadge());
        }
        
        return panel;
    }

    /**
     * Creates a panel with an upgrade button and optional additional components.
     * 
     * @param project the IntelliJ project
     * @param additionalComponents additional components to add to the panel
     * @return a JPanel containing the upgrade button and additional components
     */
    public static JPanel createUpgradePanelWithComponents(Project project, JComponent... additionalComponents) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        
        // Add additional components first (if any)
        for (JComponent component : additionalComponents) {
            panel.add(component);
        }
        
        // Add upgrade button or premium badge
        boolean isPremium = adrianmikula.jakartamigration.intellij.license.CheckLicense.isLicensed();
        
        if (!isPremium) {
            panel.add(createUpgradeButton(project, "UpgradePanelWithComponents"));
        } else {
            panel.add(createPremiumBadge());
        }
        
        return panel;
    }

    /**
     * Gets the standard yellow background color used for upgrade buttons.
     * 
     * @return the yellow background color
     */
    public static Color getYellowBackgroundColor() {
        return YELLOW_BACKGROUND;
    }

    /**
     * Gets the standard yellow border color used for upgrade buttons.
     * 
     * @return the yellow border color
     */
    public static Color getYellowBorderColor() {
        return YELLOW_BORDER;
    }

    /**
     * Gets the standard dark text color used for upgrade buttons.
     * 
     * @return the dark text color
     */
    public static Color getDarkTextColor() {
        return DARK_TEXT;
    }

    /**
     * Gets the marketplace URL for premium upgrade.
     * 
     * @return the marketplace URL
     */
    public static String getMarketplaceUrl() {
        return MARKETPLACE_URL;
    }
    
    /**
     * Tracks upgrade click analytics for the premium upgrade button with context.
     * This method is called when the user clicks on any premium upgrade button.
     * 
     * @param source the source identifier for analytics tracking
     * @param currentUiTab the current UI tab where the upgrade button was clicked
     */
    private static void trackUpgradeClick(String source, String currentUiTab) {
        try {
            UserIdentificationService userIdentificationService = new UserIdentificationService();
            UsageService usageService = new UsageService(userIdentificationService);
            usageService.trackUpgradeClick(source, currentUiTab);
        } catch (Exception e) {
            // Log error but don't prevent the upgrade action
            System.err.println("Failed to track upgrade click analytics: " + e.getMessage());
        }
    }
}
