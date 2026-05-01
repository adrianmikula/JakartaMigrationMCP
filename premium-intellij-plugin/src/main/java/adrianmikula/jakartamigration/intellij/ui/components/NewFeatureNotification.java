package adrianmikula.jakartamigration.intellij.ui.components;

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
    private final JLabel yesLink;
    private final JLabel noLink;
    
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
        this.messageLabel.setForeground(new Color(60, 60, 60));
        
        // Create Yes link with web-link styling
        this.yesLink = new JLabel("Yes");
        this.yesLink.setFont(this.yesLink.getFont().deriveFont(Font.PLAIN, 10f));
        this.yesLink.setForeground(new Color(0, 100, 180));
        this.yesLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        this.yesLink.addMouseListener(createLinkMouseListener(onYes));
        
        // Create No link with web-link styling
        this.noLink = new JLabel("No");
        this.noLink.setFont(this.noLink.getFont().deriveFont(Font.PLAIN, 10f));
        this.noLink.setForeground(new Color(0, 100, 180));
        this.noLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        this.noLink.addMouseListener(createLinkMouseListener(onNo));
        
        // Style the main panel
        this.mainPanel.setBackground(new Color(248, 249, 250));
        this.mainPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 200, 200)),
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
                label.setForeground(new Color(0, 80, 160));
                label.setText(label.getText());
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                JLabel label = (JLabel) e.getSource();
                label.setForeground(new Color(0, 100, 180));
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
}
