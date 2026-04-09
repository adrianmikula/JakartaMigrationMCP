package adrianmikula.jakartamigration.intellij.ui.components;

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
    private final JBLabel upgradeLink;

    public TruncationNoticePanel() {
        super(new BorderLayout());
        messageLabel = new JBLabel();
        upgradeLink = createUpgradeLink();
        initializeUI();
    }

    private void initializeUI() {
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(255, 193, 7)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        setBackground(new Color(255, 249, 230)); // Light yellow background

        // Left: Warning icon + message
        JPanel leftPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftPanel.setOpaque(false);

        JBLabel iconLabel = new JBLabel("⚠");
        iconLabel.setFont(iconLabel.getFont().deriveFont(Font.BOLD, 14f));
        iconLabel.setForeground(new Color(255, 193, 7)); // Yellow warning color
        leftPanel.add(iconLabel);

        messageLabel.setFont(messageLabel.getFont().deriveFont(Font.PLAIN, 12f));
        messageLabel.setForeground(new Color(133, 100, 4)); // Dark yellow/brown text
        leftPanel.add(messageLabel);

        add(leftPanel, BorderLayout.CENTER);

        // Right: Upgrade link
        JPanel rightPanel = new JBPanel<>(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightPanel.setOpaque(false);
        rightPanel.add(upgradeLink);
        add(rightPanel, BorderLayout.EAST);
    }

    private JBLabel createUpgradeLink() {
        JBLabel link = new JBLabel("<html><a href='#'>Upgrade to Premium</a></html>");
        link.setForeground(new Color(0, 102, 204)); // Blue link color
        link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        link.setToolTipText("Get unlimited access to all features");

        link.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openMarketplace();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                link.setText("<html><a href='#'><b>Upgrade to Premium</b></a></html>");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                link.setText("<html><a href='#'>Upgrade to Premium</a></html>");
            }
        });

        return link;
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
        upgradeLink.setVisible(visible);
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
