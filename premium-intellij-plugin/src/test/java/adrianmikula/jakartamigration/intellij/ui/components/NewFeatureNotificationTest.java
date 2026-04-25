package adrianmikula.jakartamigration.intellij.ui.components;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for NewFeatureNotification component.
 */
public class NewFeatureNotificationTest {
    
    private NewFeatureNotification notification;
    private AtomicBoolean yesClicked;
    private AtomicBoolean noClicked;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        yesClicked = new AtomicBoolean(false);
        noClicked = new AtomicBoolean(false);
    }
    
    @Test
    @DisplayName("Should create notification with correct message")
    void shouldCreateNotificationWithCorrectMessage() {
        // Given
        String message = "Test notification message";
        
        // When
        notification = new NewFeatureNotification(message, 
            () -> yesClicked.set(true), 
            () -> noClicked.set(true));
        
        // Then
        JPanel panel = notification.getPanel();
        assertNotNull(panel);
        assertTrue(panel.isVisible());
        
        // Find message label in the panel
        JLabel messageLabel = findLabelByText(panel, message);
        assertNotNull(messageLabel);
        assertEquals(message, messageLabel.getText());
        assertEquals(10f, messageLabel.getFont().getSize(), 0.1f);
    }
    
    @Test
    @DisplayName("Should create Yes and No links with correct styling")
    void shouldCreateYesNoLinksWithCorrectStyling() {
        // Given
        String message = "Test message";
        
        // When
        notification = new NewFeatureNotification(message, 
            () -> yesClicked.set(true), 
            () -> noClicked.set(true));
        
        // Then
        JPanel panel = notification.getPanel();
        
        // Find Yes link
        JLabel yesLink = findLabelByText(panel, "Yes");
        assertNotNull(yesLink);
        assertEquals("Yes", yesLink.getText());
        assertEquals(10f, yesLink.getFont().getSize(), 0.1f);
        assertEquals(Cursor.HAND_CURSOR, yesLink.getCursor());
        
        // Find No link
        JLabel noLink = findLabelByText(panel, "No");
        assertNotNull(noLink);
        assertEquals("No", noLink.getText());
        assertEquals(10f, noLink.getFont().getSize(), 0.1f);
        assertEquals(Cursor.HAND_CURSOR, noLink.getCursor());
    }
    
    @Test
    @DisplayName("Should execute Yes action when Yes link is clicked")
    void shouldExecuteYesActionWhenYesLinkClicked() {
        // Given
        String message = "Test message";
        notification = new NewFeatureNotification(message, 
            () -> yesClicked.set(true), 
            () -> noClicked.set(true));
        
        JPanel panel = notification.getPanel();
        JLabel yesLink = findLabelByText(panel, "Yes");
        
        // When
        simulateClick(yesLink);
        
        // Then
        assertTrue(yesClicked.get());
        assertFalse(noClicked.get());
    }
    
    @Test
    @DisplayName("Should execute No action when No link is clicked")
    void shouldExecuteNoActionWhenNoLinkClicked() {
        // Given
        String message = "Test message";
        notification = new NewFeatureNotification(message, 
            () -> yesClicked.set(true), 
            () -> noClicked.set(true));
        
        JPanel panel = notification.getPanel();
        JLabel noLink = findLabelByText(panel, "No");
        
        // When
        simulateClick(noLink);
        
        // Then
        assertTrue(noClicked.get());
        assertFalse(yesClicked.get());
    }
    
    @Test
    @DisplayName("Should change link color on hover")
    void shouldChangeLinkColorOnHover() {
        // Given
        String message = "Test message";
        notification = new NewFeatureNotification(message, 
            () -> yesClicked.set(true), 
            () -> noClicked.set(true));
        
        JPanel panel = notification.getPanel();
        JLabel yesLink = findLabelByText(panel, "Yes");
        Color originalColor = yesLink.getForeground();
        
        // When
        simulateMouseEntered(yesLink);
        
        // Then
        assertNotEquals(originalColor, yesLink.getForeground());
        assertEquals(new Color(0, 80, 160), yesLink.getForeground());
        
        // When mouse exits
        simulateMouseExited(yesLink);
        
        // Then color should return to original
        assertEquals(originalColor, yesLink.getForeground());
    }
    
    @Test
    @DisplayName("Should set visibility correctly")
    void shouldSetVisibilityCorrectly() {
        // Given
        String message = "Test message";
        notification = new NewFeatureNotification(message, 
            () -> yesClicked.set(true), 
            () -> noClicked.set(true));
        
        // Initially visible
        assertTrue(notification.isVisible());
        
        // When hidden
        notification.setVisible(false);
        
        // Then
        assertFalse(notification.isVisible());
        assertFalse(notification.getPanel().isVisible());
        
        // When shown again
        notification.setVisible(true);
        
        // Then
        assertTrue(notification.isVisible());
        assertTrue(notification.getPanel().isVisible());
    }
    
    @Test
    @DisplayName("Should update message correctly")
    void shouldUpdateMessageCorrectly() {
        // Given
        String originalMessage = "Original message";
        String newMessage = "Updated message";
        notification = new NewFeatureNotification(originalMessage, 
            () -> yesClicked.set(true), 
            () -> noClicked.set(true));
        
        // When
        notification.setMessage(newMessage);
        
        // Then
        JPanel panel = notification.getPanel();
        JLabel messageLabel = findLabelByText(panel, newMessage);
        assertNotNull(messageLabel);
        assertEquals(newMessage, messageLabel.getText());
    }
    
    @Test
    @DisplayName("Should create usage permission notification with factory method")
    void shouldCreateUsagePermissionNotificationWithFactory() {
        // When
        AtomicBoolean factoryYesClicked = new AtomicBoolean(false);
        AtomicBoolean factoryNoClicked = new AtomicBoolean(false);
        
        notification = NewFeatureNotification.createUsagePermissionNotification(
            () -> factoryYesClicked.set(true),
            () -> factoryNoClicked.set(true)
        );
        
        // Then
        assertNotNull(notification);
        assertTrue(notification.isVisible());
        
        JPanel panel = notification.getPanel();
        JLabel messageLabel = findLabelByText(panel, "Help improve this plugin by sharing anonymous usage data and error reports? ");
        assertNotNull(messageLabel);
        
        // Verify Yes and No links are present
        JLabel yesLink = findLabelByText(panel, "Yes");
        JLabel noLink = findLabelByText(panel, "No");
        assertNotNull(yesLink);
        assertNotNull(noLink);
    }
    
    @Test
    @DisplayName("Should handle null actions gracefully")
    void shouldHandleNullActionsGracefully() {
        // Given
        String message = "Test message";
        
        // When - should not throw exception
        assertDoesNotThrow(() -> {
            notification = new NewFeatureNotification(message, null, null);
        });
        
        // Then
        assertNotNull(notification);
        JPanel panel = notification.getPanel();
        assertNotNull(panel);
    }
    
    /**
     * Helper method to find a JLabel with specific text in a component hierarchy.
     */
    private JLabel findLabelByText(Container container, String text) {
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel) {
                JLabel label = (JLabel) component;
                if (text.equals(label.getText())) {
                    return label;
                }
            }
            if (component instanceof Container) {
                JLabel found = findLabelByText((Container) component, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
    
    /**
     * Helper method to simulate a mouse click on a component.
     */
    private void simulateClick(JLabel label) {
        MouseEvent clickEvent = new MouseEvent(
            label,
            MouseEvent.MOUSE_CLICKED,
            System.currentTimeMillis(),
            0,
            5, 5,
            1,
            false
        );
        label.dispatchEvent(clickEvent);
    }
    
    /**
     * Helper method to simulate mouse entered event.
     */
    private void simulateMouseEntered(JLabel label) {
        MouseEvent enterEvent = new MouseEvent(
            label,
            MouseEvent.MOUSE_ENTERED,
            System.currentTimeMillis(),
            0,
            5, 5,
            0,
            false
        );
        label.dispatchEvent(enterEvent);
    }
    
    /**
     * Helper method to simulate mouse exited event.
     */
    private void simulateMouseExited(JLabel label) {
        MouseEvent exitEvent = new MouseEvent(
            label,
            MouseEvent.MOUSE_EXITED,
            System.currentTimeMillis(),
            0,
            5, 5,
            0,
            false
        );
        label.dispatchEvent(exitEvent);
    }
}
