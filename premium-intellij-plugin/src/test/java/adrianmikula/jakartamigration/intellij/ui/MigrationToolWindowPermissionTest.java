package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.analytics.service.UserIdentificationService;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import javax.swing.*;
import java.awt.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Integration tests for usage permission request functionality in MigrationToolWindow.
 */
public class MigrationToolWindowPermissionTest extends BasePlatformTestCase {
    
    private UserIdentificationService mockUserIdentificationService;
    
    private MigrationToolWindow.MigrationToolWindowContent toolWindowContent;
    
    @Override
    public void setUp() throws Exception {
        super.setUp();
        mockUserIdentificationService = mock(UserIdentificationService.class);
    }
    
    public void testShouldShowNotificationWhenPermissionNotRequested() {
        when(mockUserIdentificationService.isUsagePermissionRequested()).thenReturn(false);
        Project project = getProject();
        
        toolWindowContent = new MigrationToolWindow.MigrationToolWindowContent(project) {
            @Override
            protected UserIdentificationService createUserIdentificationService() {
                return mockUserIdentificationService;
            }
        };
        
        JPanel contentPanel = toolWindowContent.getContentPanel();
        assertThat(contentPanel).isNotNull();
        
        JPanel notificationPanel = findNotificationPanel(contentPanel);
        assertThat(notificationPanel).as("Notification panel should be present").isNotNull();
        assertThat(notificationPanel.isVisible()).as("Notification should be visible").isTrue();
        
        JLabel messageLabel = findLabelWithText(notificationPanel, "Help improve this plugin by sharing anonymous usage data and error reports? ");
        assertThat(messageLabel).as("Permission message should be displayed").isNotNull();
        
        JLabel yesLink = findLabelWithText(notificationPanel, "Yes");
        JLabel noLink = findLabelWithText(notificationPanel, "No");
        assertThat(yesLink).as("Yes link should be present").isNotNull();
        assertThat(noLink).as("No link should be present").isNotNull();
    }
    
    public void testShouldNotShowNotificationWhenPermissionAlreadyRequested() {
        when(mockUserIdentificationService.isUsagePermissionRequested()).thenReturn(true);
        Project project = getProject();
        
        toolWindowContent = new MigrationToolWindow.MigrationToolWindowContent(project) {
            @Override
            protected UserIdentificationService createUserIdentificationService() {
                return mockUserIdentificationService;
            }
        };
        
        JPanel contentPanel = toolWindowContent.getContentPanel();
        assertThat(contentPanel).isNotNull();
        
        JPanel notificationPanel = findNotificationPanel(contentPanel);
        assertThat(notificationPanel).as("Notification panel should not be present").isNull();
    }
    
    public void testShouldEnableAnalyticsWhenUserClicksYes() {
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
        
        simulateClick(yesLink);
        
        verify(mockUserIdentificationService).setUsagePermissionRequested();
        verify(mockUserIdentificationService).setUsageMetricsEnabled(true);
        verify(mockUserIdentificationService).setErrorReportingEnabled(true);
        
        assertThat(notificationPanel.isVisible()).as("Notification should be hidden after user response").isFalse();
    }
    
    public void testShouldDisableAnalyticsWhenUserClicksNo() {
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
        
        simulateClick(noLink);
        
        verify(mockUserIdentificationService).setUsagePermissionRequested();
        verify(mockUserIdentificationService).setUsageMetricsEnabled(false);
        verify(mockUserIdentificationService).setErrorReportingEnabled(false);
        
        assertThat(notificationPanel.isVisible()).as("Notification should be hidden after user response").isFalse();
    }
    
    public void testShouldMarkPermissionAsRequestedEvenWhenUserClicksNo() {
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
        
        simulateClick(noLink);
        
        verify(mockUserIdentificationService).setUsagePermissionRequested();
        
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
        assertThat(newNotificationPanel).as("Notification should not appear on subsequent initialization").isNull();
    }
    
    public void testShouldPositionNotificationAtTopOfContentPanel() {
        when(mockUserIdentificationService.isUsagePermissionRequested()).thenReturn(false);
        Project project = getProject();
        
        toolWindowContent = new MigrationToolWindow.MigrationToolWindowContent(project) {
            @Override
            protected UserIdentificationService createUserIdentificationService() {
                return mockUserIdentificationService;
            }
        };
        
        JPanel contentPanel = toolWindowContent.getContentPanel();
        assertThat(contentPanel.getLayout()).isInstanceOf(BorderLayout.class);
        
        Component northComponent = ((BorderLayout) contentPanel.getLayout()).getLayoutComponent(BorderLayout.NORTH);
        assertThat(northComponent).as("Component should be positioned at NORTH").isNotNull();
        
        if (northComponent instanceof JPanel) {
            JPanel northPanel = (JPanel) northComponent;
            Component notificationComponent = ((BorderLayout) northPanel.getLayout()).getLayoutComponent(BorderLayout.NORTH);
            assertThat(notificationComponent).as("Notification should be at top of north panel").isNotNull();
        }
    }
    
    private JPanel findNotificationPanel(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JPanel) {
                JPanel panel = (JPanel) component;
                if (panel.getLayout() instanceof BorderLayout) {
                    BorderLayout layout = (BorderLayout) panel.getLayout();
                    Component northComponent = layout.getLayoutComponent(BorderLayout.NORTH);
                    if (northComponent instanceof JPanel) {
                        JPanel northPanel = (JPanel) northComponent;
                        if (containsNotificationMessage(northPanel)) {
                            return northPanel;
                        }
                    }
                }
            }
        }
        return null;
    }
    
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

    // ==================== Trial Unavailable Notification Tests ====================

    public void testShouldShowTrialNotificationWhenNotYetShown() {
        when(mockUserIdentificationService.isTrialUnavailableNotificationShown()).thenReturn(false);
        when(mockUserIdentificationService.isUsagePermissionRequested()).thenReturn(true);
        Project project = getProject();

        toolWindowContent = new MigrationToolWindow.MigrationToolWindowContent(project) {
            @Override
            protected UserIdentificationService createUserIdentificationService() {
                return mockUserIdentificationService;
            }
        };

        JPanel contentPanel = toolWindowContent.getContentPanel();
        assertThat(contentPanel).isNotNull();

        JPanel trialPanel = findTrialNotificationPanel(contentPanel);
        assertThat(trialPanel).as("Trial notification panel should be present").isNotNull();
        assertThat(trialPanel.isVisible()).as("Trial notification should be visible").isTrue();

        JLabel messageLabel = findLabelWithText(trialPanel, "Free trial is regrettably no longer available");
        assertThat(messageLabel).as("Trial message should be displayed").isNotNull();

        JLabel sponsorLink = findLabelWithText(trialPanel, "Sponsor");
        assertThat(sponsorLink).as("Sponsor link should be present").isNotNull();
    }

    public void testShouldNotShowTrialNotificationWhenAlreadyShown() {
        when(mockUserIdentificationService.isTrialUnavailableNotificationShown()).thenReturn(true);
        when(mockUserIdentificationService.isUsagePermissionRequested()).thenReturn(true);
        Project project = getProject();

        toolWindowContent = new MigrationToolWindow.MigrationToolWindowContent(project) {
            @Override
            protected UserIdentificationService createUserIdentificationService() {
                return mockUserIdentificationService;
            }
        };

        JPanel contentPanel = toolWindowContent.getContentPanel();
        assertThat(contentPanel).isNotNull();

        JPanel trialPanel = findTrialNotificationPanel(contentPanel);
        assertThat(trialPanel).as("Trial notification panel should not be present").isNull();
    }

    public void testShouldHideTrialNotificationAndMarkAsShownWhenSponsorClicked() {
        when(mockUserIdentificationService.isTrialUnavailableNotificationShown()).thenReturn(false);
        when(mockUserIdentificationService.isUsagePermissionRequested()).thenReturn(true);
        Project project = getProject();

        toolWindowContent = new MigrationToolWindow.MigrationToolWindowContent(project) {
            @Override
            protected UserIdentificationService createUserIdentificationService() {
                return mockUserIdentificationService;
            }
        };

        JPanel contentPanel = toolWindowContent.getContentPanel();
        JPanel trialPanel = findTrialNotificationPanel(contentPanel);
        JLabel sponsorLink = findLabelWithText(trialPanel, "Sponsor");

        simulateClick(sponsorLink);

        verify(mockUserIdentificationService).setTrialUnavailableNotificationShown();
        assertThat(trialPanel.isVisible()).as("Trial notification should be hidden after click").isFalse();
    }

    public void testShouldNotShowTrialNotificationAgainAfterSponsorClicked() {
        when(mockUserIdentificationService.isTrialUnavailableNotificationShown()).thenReturn(false);
        when(mockUserIdentificationService.isUsagePermissionRequested()).thenReturn(true);
        Project project = getProject();

        toolWindowContent = new MigrationToolWindow.MigrationToolWindowContent(project) {
            @Override
            protected UserIdentificationService createUserIdentificationService() {
                return mockUserIdentificationService;
            }
        };

        JPanel contentPanel = toolWindowContent.getContentPanel();
        JPanel trialPanel = findTrialNotificationPanel(contentPanel);
        JLabel sponsorLink = findLabelWithText(trialPanel, "Sponsor");
        simulateClick(sponsorLink);

        when(mockUserIdentificationService.isTrialUnavailableNotificationShown()).thenReturn(true);

        MigrationToolWindow.MigrationToolWindowContent newToolWindowContent =
            new MigrationToolWindow.MigrationToolWindowContent(project) {
                @Override
                protected UserIdentificationService createUserIdentificationService() {
                    return mockUserIdentificationService;
                }
            };

        JPanel newContentPanel = newToolWindowContent.getContentPanel();
        JPanel newTrialPanel = findTrialNotificationPanel(newContentPanel);
        assertThat(newTrialPanel).as("Trial notification should not reappear after being dismissed").isNull();
    }

    private JPanel findTrialNotificationPanel(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JPanel) {
                JPanel panel = (JPanel) component;
                if (panel.getLayout() instanceof BorderLayout) {
                    BorderLayout layout = (BorderLayout) panel.getLayout();
                    Component northComponent = layout.getLayoutComponent(BorderLayout.NORTH);
                    if (northComponent instanceof JPanel) {
                        JPanel northPanel = (JPanel) northComponent;
                        if (containsTrialNotificationMessage(northPanel)) {
                            return northPanel;
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean containsTrialNotificationMessage(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel) {
                JLabel label = (JLabel) component;
                String text = label.getText();
                if (text != null && text.contains("Free trial is regrettably no longer available")) {
                    return true;
                }
            }
            if (component instanceof Container) {
                if (containsTrialNotificationMessage((Container) component)) {
                    return true;
                }
            }
        }
        return false;
    }
}
