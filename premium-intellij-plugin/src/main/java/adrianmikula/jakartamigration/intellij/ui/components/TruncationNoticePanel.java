package adrianmikula.jakartamigration.intellij.ui.components;

import adrianmikula.jakartamigration.analytics.service.UsageService;
import adrianmikula.jakartamigration.analytics.service.UserIdentificationService;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;

/**
 * Reusable panel for displaying truncation notices with upgrade prompt.
 * Shows a yellow warning panel with "Showing X of Y items" message
 * and an "Upgrade to Premium" link.
 */
public class TruncationNoticePanel extends JBPanel<TruncationNoticePanel> {

    private static final String MARKETPLACE_URL = "https://plugins.jetbrains.com/plugin/30093-jakarta-migration";

    private final JBLabel messageLabel;
    private final JButton upgradeButton;
    private final UsageService usageService;

    public TruncationNoticePanel() {
        super(new BorderLayout());
        messageLabel = new JBLabel();
        
        // Initialize analytics service for upgrade tracking
        UserIdentificationService userIdentificationService = new UserIdentificationService();
        this.usageService = new UsageService(userIdentificationService);
        
        upgradeButton = createUpgradeButton();
        initializeUI();
    }

    private void initializeUI() {
        setOpaque(true);
        setBackground(new Color(255, 243, 205)); // Prominent amber warning background
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(2, 0, 2, 0, new Color(255, 140, 0)),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));

        // Left: warning icon + bold message
        JPanel leftPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setOpaque(false);

        JBLabel iconLabel = new JBLabel("⚠");
        iconLabel.setFont(iconLabel.getFont().deriveFont(Font.BOLD, 22f));
        iconLabel.setForeground(new Color(255, 130, 0));
        leftPanel.add(iconLabel);

        messageLabel.setFont(messageLabel.getFont().deriveFont(Font.BOLD, 13f));
        messageLabel.setForeground(new Color(120, 70, 0));
        leftPanel.add(messageLabel);

        add(leftPanel, BorderLayout.CENTER);

        // Right: prominent upgrade button
        JPanel rightPanel = new JBPanel<>(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightPanel.setOpaque(false);
        rightPanel.add(upgradeButton);
        add(rightPanel, BorderLayout.EAST);
    }

    private JButton createUpgradeButton() {
        JButton button = new JButton("⬆ Upgrade to Premium");
        button.setToolTipText("Unlock unlimited rows and all premium features");
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBackground(new Color(255, 215, 0));
        button.setForeground(new Color(80, 60, 0));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 152, 0), 1),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        button.setFont(button.getFont().deriveFont(Font.BOLD));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        button.addActionListener(e -> {
            if (usageService != null) {
                usageService.trackUpgradeClick("truncation_notice", "TruncationNotice");
            }
            openMarketplace();
        });

        return button;
    }

    /**
     * Updates the panel with the current truncation information.
     *
     * @param shown number of items currently shown
     * @param total total number of items available
     * @param itemName the name of the items being displayed (e.g., "dependencies", "recipes")
     */
    public void updateMessage(int shown, int total, String itemName) {
        messageLabel.setText(String.format("Showing %d of %d %s.", shown, total, itemName));
        setVisible(shown < total);
    }

    /**
     * Updates the panel with the current truncation information using default "items" terminology.
     *
     * @param shown number of items currently shown
     * @param total total number of items available
     */
    public void updateMessage(int shown, int total) {
        updateMessage(shown, total, "items");
    }

    /**
     * Sets the visibility of the upgrade link.
     *
     * @param visible true to show the upgrade link, false to hide it
     */
    public void setUpgradeLinkVisible(boolean visible) {
        upgradeButton.setVisible(visible);
    }

    /**
     * Opens the JetBrains Marketplace to upgrade.
     */
    private void openMarketplace() {
        try {
            Desktop.getDesktop().browse(new URI(MARKETPLACE_URL));
        } catch (Exception ex) {
            // Log error or show dialog - fallback handled by caller if needed
            JOptionPane.showMessageDialog(
                    this,
                    "Please visit: " + MARKETPLACE_URL,
                    "Upgrade to Premium",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }
    }
}
