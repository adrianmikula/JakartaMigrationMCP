package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.analytics.service.UserIdentificationService;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for usage permission request functionality in MigrationToolWindow.
 */
public class MigrationToolWindowPermissionTest extends BasePlatformTestCase {
    
    @Mock
    private UserIdentificationService mockUserIdentificationService;
    
    private MigrationToolWindow.MigrationToolWindowContent toolWindowContent;
    
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.openMocks(this);
    }
    
    @Test
    @DisplayName("Should show notification when permission not requested")
    void shouldShowNotificationWhenPermissionNotRequested() {
        // Given
        when(mockUserIdentificationService.isUsagePermissionRequested()).thenReturn(false);
        Project project = getProject();
        
        // When
        toolWindowContent = new MigrationToolWindow.MigrationToolWindowContent(project) {
            @Override
            protected UserIdentificationService createUserIdentificationService() {
                return mockUserIdentificationService;
            }
        };
        
        // Then
        JPanel contentPanel = toolWindowContent.getContentPanel();
        assertNotNull(contentPanel);
        
        // Find notification panel
        JPanel notificationPanel = findNotificationPanel(contentPanel);
        assertNotNull(notificationPanel, "Notification panel should be present");
        assertTrue(notificationPanel.isVisible(), "Notification should be visible");
        
        // Verify notification contains expected message
        JLabel messageLabel = findLabelWithText(notificationPanel, "Help improve this plugin by sharing anonymous usage data and error reports? ");
        assertNotNull(messageLabel, "Permission message should be displayed");
        
        // Verify Yes and No links are present
        JLabel yesLink = findLabelWithText(notificationPanel, "Yes");
        JLabel noLink = findLabelWithText(notificationPanel, "No");
        assertNotNull(yesLink, "Yes link should be present");
        assertNotNull(noLink, "No link should be present");
    }
    
    @Test
    @DisplayName("Should not show notification when permission already requested")
    void shouldNotShowNotificationWhenPermissionAlreadyRequested() {
        // Given
        when(mockUserIdentificationService.isUsagePermissionRequested()).thenReturn(true);
        Project project = getProject();
        
        // When
        toolWindowContent = new MigrationToolWindow.MigrationToolWindowContent(project) {
            @Override
            protected UserIdentificationService createUserIdentificationService() {
                return mockUserIdentificationService;
            }
        };
        
        // Then
        JPanel contentPanel = toolWindowContent.getContentPanel();
        assertNotNull(contentPanel);
        
        // Notification should not be present
        JPanel notificationPanel = findNotificationPanel(contentPanel);
        assertNull(notificationPanel, "Notification panel should not be present");
    }
    
    @Test
    @DisplayName("Should enable analytics when user clicks Yes")
    void shouldEnableAnalyticsWhenUserClicksYes() {
        // Given
        when(mockUserIdentificationService.isUsagePermissionRequested()).thenReturn(false);
        Project project = getProject();
        
        toolWindowContent = new MigrationToolWindow.MigrationToolWindowContent(project) {
            @Override
            protected UserIdentificationService createUserIdentificationService() {
                return mockUserIdentificationService;
            }
        };
        
        JPanel contentPanel = toolWindowContent.getContentPanel();
        JPanel notificationPanel = findNotificationPanel(contentPanel);
        JLabel yesLink = findLabelWithText(notificationPanel, "Yes");
        
        // When
        simulateClick(yesLink);
        
        // Then
        verify(mockUserIdentificationService).setUsagePermissionRequested();
        verify(mockUserIdentificationService).setUsageMetricsEnabled(true);
        verify(mockUserIdentificationService).setErrorReportingEnabled(true);
        
        // Notification should be hidden
        assertFalse(notificationPanel.isVisible(), "Notification should be hidden after user response");
    }
    
    @Test
    @DisplayName("Should disable analytics when user clicks No")
    void shouldDisableAnalyticsWhenUserClicksNo() {
        // Given
        when(mockUserIdentificationService.isUsagePermissionRequested()).thenReturn(false);
        Project project = getProject();
        
        toolWindowContent = new MigrationToolWindow.MigrationToolWindowContent(project) {
            @Override
            protected UserIdentificationService createUserIdentificationService() {
                return mockUserIdentificationService;
            }
        };
        
        JPanel contentPanel = toolWindowContent.getContentPanel();
        JPanel notificationPanel = findNotificationPanel(contentPanel);
        JLabel noLink = findLabelWithText(notificationPanel, "No");
        
        // When
        simulateClick(noLink);
        
        // Then
        verify(mockUserIdentificationService).setUsagePermissionRequested();
        verify(mockUserIdentificationService).setUsageMetricsEnabled(false);
        verify(mockUserIdentificationService).setErrorReportingEnabled(false);
        
        // Notification should be hidden
        assertFalse(notificationPanel.isVisible(), "Notification should be hidden after user response");
    }
    
    @Test
    @DisplayName("Should mark permission as requested even if user clicks No")
    void shouldMarkPermissionAsRequestedEvenWhenUserClicksNo() {
        // Given
        when(mockUserIdentificationService.isUsagePermissionRequested()).thenReturn(false);
        Project project = getProject();
        
        toolWindowContent = new MigrationToolWindow.MigrationToolWindowContent(project) {
            @Override
            protected UserIdentificationService createUserIdentificationService() {
                return mockUserIdentificationService;
            }
        };
        
        JPanel contentPanel = toolWindowContent.getContentPanel();
        JPanel notificationPanel = findNotificationPanel(contentPanel);
        JLabel noLink = findLabelWithText(notificationPanel, "No");
        
        // When
        simulateClick(noLink);
        
        // Then - permission should be marked as requested
        verify(mockUserIdentificationService).setUsagePermissionRequested();
        
        // Subsequent initialization should not show notification
        when(mockUserIdentificationService.isUsagePermissionRequested()).thenReturn(true);
        
        MigrationToolWindow.MigrationToolWindowContent newToolWindowContent = 
            new MigrationToolWindow.MigrationToolWindowContent(project) {
                @Override
                protected UserIdentificationService createUserIdentificationService() {
                    return mockUserIdentificationService;
                }
            };
        
        JPanel newContentPanel = newToolWindowContent.getContentPanel();
        JPanel newNotificationPanel = findNotificationPanel(newContentPanel);
        assertNull(newNotificationPanel, "Notification should not appear on subsequent initialization");
    }
    
    @Test
    @DisplayName("Should position notification at top of content panel")
    void shouldPositionNotificationAtTopOfContentPanel() {
        // Given
        when(mockUserIdentificationService.isUsagePermissionRequested()).thenReturn(false);
        Project project = getProject();
        
        // When
        toolWindowContent = new MigrationToolWindow.MigrationToolWindowContent(project) {
            @Override
            protected UserIdentificationService createUserIdentificationService() {
                return mockUserIdentificationService;
            }
        };
        
        // Then
        JPanel contentPanel = toolWindowContent.getContentPanel();
        assertEquals(BorderLayout.class, contentPanel.getLayout());
        
        // Notification should be in NORTH position
        Component northComponent = ((BorderLayout) contentPanel.getLayout()).getLayoutComponent(BorderLayout.NORTH);
        assertNotNull(northComponent, "Component should be positioned at NORTH");
        
        // Find notification container within north component
        if (northComponent instanceof JPanel) {
            JPanel northPanel = (JPanel) northComponent;
            Component notificationComponent = ((BorderLayout) northPanel.getLayout()).getLayoutComponent(BorderLayout.NORTH);
            assertNotNull(notificationComponent, "Notification should be at top of north panel");
        }
    }
    
    /**
     * Helper method to find the notification panel in the component hierarchy.
     */
    private JPanel findNotificationPanel(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JPanel) {
                JPanel panel = (JPanel) component;
                // Look for notification container (panel with BorderLayout)
                if (panel.getLayout() instanceof BorderLayout) {
                    BorderLayout layout = (BorderLayout) panel.getLayout();
                    Component northComponent = layout.getLayoutComponent(BorderLayout.NORTH);
                    if (northComponent instanceof JPanel) {
                        JPanel northPanel = (JPanel) northComponent;
                        // Check if this panel contains the notification message
                        if (containsNotificationMessage(northPanel)) {
                            return northPanel;
                        }
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Helper method to check if a panel contains the notification message.
     */
    private boolean containsNotificationMessage(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel) {
                JLabel label = (JLabel) component;
                String text = label.getText();
                if (text != null && text.contains("Help improve this plugin")) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Helper method to find a JLabel with specific text.
     */
    private JLabel findLabelWithText(Container container, String text) {
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel) {
                JLabel label = (JLabel) component;
                if (text.equals(label.getText())) {
                    return label;
                }
            }
            if (component instanceof Container) {
                JLabel found = findLabelWithText((Container) component, text);
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
        java.awt.event.MouseEvent clickEvent = new java.awt.event.MouseEvent(
            label,
            java.awt.event.MouseEvent.MOUSE_CLICKED,
            System.currentTimeMillis(),
            0,
            5, 5,
            1,
            false
        );
        label.dispatchEvent(clickEvent);
    }
}
