package adrianmikula.jakartamigration.intellij.ui.components;

import adrianmikula.jakartamigration.intellij.ui.UIColors;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Reusable component for displaying feature notifications with clickable action links.
 * Designed to be positioned at the top of plugin window above all tabs.
 */
public class NewFeatureNotification {
    
    private final JPanel mainPanel;
    private final JLabel messageLabel;
    private JLabel yesLink;
    private JLabel noLink;
    
    /**
     * Creates a new feature notification component.
     * 
     * @param message The notification message to display
     * @param onYes Action to execute when Yes link is clicked
     * @param onNo Action to execute when No link is clicked
     */
    public NewFeatureNotification(String message, Runnable onYes, Runnable onNo) {
        this.mainPanel = new JPanel(new BorderLayout());
        
        // Create message label with small font
        this.messageLabel = new JLabel(message);
        this.messageLabel.setFont(this.messageLabel.getFont().deriveFont(Font.PLAIN, 10f));
        this.messageLabel.setForeground(UIColors.TEXT_SECONDARY);
        
        // Create Yes link with web-link styling
        this.yesLink = new JLabel("Yes");
        this.yesLink.setFont(this.yesLink.getFont().deriveFont(Font.PLAIN, 10f));
        this.yesLink.setForeground(UIColors.LINK);
        this.yesLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        this.yesLink.addMouseListener(createLinkMouseListener(onYes));
        
        // Create No link with web-link styling
        this.noLink = new JLabel("No");
        this.noLink.setFont(this.noLink.getFont().deriveFont(Font.PLAIN, 10f));
        this.noLink.setForeground(UIColors.LINK);
        this.noLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        this.noLink.addMouseListener(createLinkMouseListener(onNo));
        
        // Style the main panel
        this.mainPanel.setBackground(UIColors.PANEL_BACKGROUND);
        this.mainPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UIColors.BORDER),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        
        // Layout components
        JPanel contentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        contentPanel.setOpaque(false);
        contentPanel.add(messageLabel);
        contentPanel.add(Box.createHorizontalStrut(8));
        contentPanel.add(yesLink);
        contentPanel.add(Box.createHorizontalStrut(12));
        contentPanel.add(noLink);
        
        this.mainPanel.add(contentPanel, BorderLayout.CENTER);
    }
    
    /**
     * Creates a mouse listener for clickable links with hover effects.
     */
    private MouseAdapter createLinkMouseListener(Runnable onClick) {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (onClick != null) {
                    onClick.run();
                }
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                JLabel label = (JLabel) e.getSource();
                label.setForeground(UIColors.LINK_HOVER);
                label.setText(label.getText());
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                JLabel label = (JLabel) e.getSource();
                label.setForeground(UIColors.LINK);
                label.setText(label.getText());
            }
        };
    }
    
    /**
     * Gets the main panel component.
     * 
     * @return The notification panel
     */
    public JPanel getPanel() {
        return mainPanel;
    }
    
    /**
     * Sets the visibility of the notification.
     * 
     * @param visible true to show the notification, false to hide it
     */
    public void setVisible(boolean visible) {
        mainPanel.setVisible(visible);
    }
    
    /**
     * Checks if the notification is currently visible.
     * 
     * @return true if visible, false otherwise
     */
    public boolean isVisible() {
        return mainPanel.isVisible();
    }
    
    /**
     * Updates the notification message.
     * 
     * @param message The new message to display
     */
    public void setMessage(String message) {
        messageLabel.setText(message);
    }
    
    /**
     * Creates a new feature notification component with a single action link.
     * 
     * @param message The notification message to display
     * @param action Action to execute when the link is clicked
     * @param linkText Text to display for the action link
     */
    public NewFeatureNotification(String message, Runnable action, String linkText) {
        this.mainPanel = new JPanel(new BorderLayout());

        this.messageLabel = new JLabel(message);
        this.messageLabel.setFont(this.messageLabel.getFont().deriveFont(Font.PLAIN, 10f));
        this.messageLabel.setForeground(UIColors.TEXT_SECONDARY);

        JPanel contentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        contentPanel.setOpaque(false);
        contentPanel.add(messageLabel);

        if (action != null && linkText != null) {
            JLabel actionLink = new JLabel(linkText);
            actionLink.setFont(actionLink.getFont().deriveFont(Font.PLAIN, 10f));
            actionLink.setForeground(UIColors.LINK);
            actionLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            actionLink.addMouseListener(createLinkMouseListener(action));
            contentPanel.add(Box.createHorizontalStrut(8));
            contentPanel.add(actionLink);
        }

        this.mainPanel.setBackground(UIColors.PANEL_BACKGROUND);
        this.mainPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UIColors.BORDER),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        this.mainPanel.add(contentPanel, BorderLayout.CENTER);

        this.yesLink = null;
        this.noLink = null;
    }

    /**
     * Convenience method to create a usage permission notification.
     * 
     * @param onYes Action when user opts in
     * @param onNo Action when user opts out
     * @return Configured notification for usage permission
     */
    public static NewFeatureNotification createUsagePermissionNotification(Runnable onYes, Runnable onNo) {
        String message = "Help improve this plugin by sharing anonymous usage data and error reports? ";
        return new NewFeatureNotification(message, onYes, onNo);
    }

    /**
     * Convenience method to create a trial-unavailable notification with a sponsor link.
     * 
     * @param onSponsor Action when user clicks the sponsor link
     * @return Configured notification for trial unavailability
     */
    public static NewFeatureNotification createTrialUnavailableNotification(Runnable onSponsor) {
        String message = "Free trial is no longer available. Please consider sponsoring us.";
        return new NewFeatureNotification(message, onSponsor, "Sponsor");
    }

    /**
     * Convenience method to create an advanced refactor notification with a learn-more link.
     * 
     * @param onLearnMore Action when user clicks the learn more link
     * @return Configured notification for advanced refactor
     */
    public static NewFeatureNotification createAdvancedRefactorNotification(Runnable onLearnMore) {
        String message = "Advanced Refactor is now available! Safely test complex refactor sequences in sandboxed testcontainers. ";
        return new NewFeatureNotification(message, onLearnMore, "Learn More");
    }
}
