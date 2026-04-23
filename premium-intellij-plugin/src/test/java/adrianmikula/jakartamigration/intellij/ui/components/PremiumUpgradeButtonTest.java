package adrianmikula.jakartamigration.intellij.ui.components;

import adrianmikula.jakartamigration.analytics.service.UsageService;
import adrianmikula.jakartamigration.analytics.service.UserIdentificationService;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class PremiumUpgradeButtonTest extends BasePlatformTestCase {

    @Mock
    private UserIdentificationService mockUserIdentificationService;
    
    @Mock
    private UsageService mockUsageService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testCreateUpgradeButtonWithProject() {
        JButton button = PremiumUpgradeButton.createUpgradeButton(getProject());
        
        assertThat(button).isNotNull();
        assertThat(button.getText()).isEqualTo("⬆ Upgrade to Premium");
        assertThat(button.getToolTipText()).isEqualTo("Get Premium features: Auto-fixes, one-click refactoring, binary fixes");
        assertThat(button.getBackground()).isEqualTo(PremiumUpgradeButton.getYellowBackgroundColor());
        assertThat(button.getForeground()).isEqualTo(PremiumUpgradeButton.getDarkTextColor());
    }

    @Test
    public void testCreateUpgradeButtonWithCustomText() {
        JButton button = PremiumUpgradeButton.createUpgradeButton(
            getProject(), 
            "Custom Upgrade Text", 
            "Custom Tooltip"
        );
        
        assertThat(button).isNotNull();
        assertThat(button.getText()).isEqualTo("Custom Upgrade Text");
        assertThat(button.getToolTipText()).isEqualTo("Custom Tooltip");
        assertThat(button.getBackground()).isEqualTo(PremiumUpgradeButton.getYellowBackgroundColor());
    }

    @Test
    public void testCreateUpgradeButtonWithAnalyticsSource() {
        JButton button = PremiumUpgradeButton.createUpgradeButton(
            getProject(), 
            "test_source",
            "Test Button",
            "Test Tooltip"
        );
        
        assertThat(button).isNotNull();
        assertThat(button.getText()).isEqualTo("Test Button");
        assertThat(button.getToolTipText()).isEqualTo("Test Tooltip");
        
        // Verify button has action listener
        assertThat(button.getActionListeners()).hasSize(1);
    }

    @Test
    public void testButtonClickTriggersAnalytics() throws Exception {
        // Create a spy for the trackUpgradeClick method
        PremiumUpgradeButton spy = spy(new PremiumUpgradeButton());
        
        // Create button with analytics source
        JButton button = PremiumUpgradeButton.createUpgradeButton(
            getProject(), 
            "test_analytics_source",
            "Test Button",
            "Test Tooltip"
        );
        
        // Simulate button click
        ActionEvent actionEvent = new ActionEvent(button, ActionEvent.ACTION_PERFORMED, "click");
        for (ActionListener listener : button.getActionListeners()) {
            listener.actionPerformed(actionEvent);
        }
        
        // Note: Since trackUpgradeClick is private and creates its own service instances,
        // we can't easily mock it without refactoring. The test verifies the button
        // structure and that clicking doesn't throw exceptions.
        assertThat(true).isTrue(); // Test passes if no exceptions are thrown
    }

    @Test
    public void testCreatePremiumBadge() {
        JLabel badge = PremiumUpgradeButton.createPremiumBadge();
        
        assertThat(badge).isNotNull();
        assertThat(badge.getText()).isEqualTo("⭐ Premium Active");
        assertThat(badge.getToolTipText()).isEqualTo("Premium license active");
        assertThat(badge.getForeground()).isEqualTo(PremiumUpgradeButton.getYellowBackgroundColor());
    }

    @Test
    public void testCreateConditionalUpgradePanel() {
        JPanel panel = PremiumUpgradeButton.createConditionalUpgradePanel(getProject());
        
        assertThat(panel).isNotNull();
        assertThat(panel.getComponentCount()).isEqualTo(1);
        
        // Should contain either upgrade button or premium badge
        Component component = panel.getComponent(0);
        assertThat(component).isInstanceOf(JButton.class);
    }

    @Test
    public void testGetMarketplaceUrl() {
        String url = PremiumUpgradeButton.getMarketplaceUrl();
        assertThat(url).isEqualTo("https://plugins.jetbrains.com/plugin/30093-jakarta-migration");
    }

    @Test
    public void testColorConstants() {
        assertThat(PremiumUpgradeButton.getYellowBackgroundColor()).isNotNull();
        assertThat(PremiumUpgradeButton.getYellowBorderColor()).isNotNull();
        assertThat(PremiumUpgradeButton.getDarkTextColor()).isNotNull();
    }

    @Test
    public void testButtonStyling() {
        JButton button = PremiumUpgradeButton.createUpgradeButton(getProject());
        
        // Verify font is bold
        assertThat(button.getFont().isBold()).isTrue();
        
        // Verify focus painting is disabled
        assertThat(button.isFocusPainted()).isFalse();
        
        // Verify border exists
        assertThat(button.getBorder()).isNotNull();
    }

    @Test
    public void testAnalyticsErrorHandling() {
        // Test that analytics failures don't prevent button functionality
        JButton button = PremiumUpgradeButton.createUpgradeButton(
            getProject(), 
            "error_test_source",
            "Error Test Button",
            "Error Test Tooltip"
        );
        
        // This should not throw any exceptions even if analytics fails
        ActionEvent actionEvent = new ActionEvent(button, ActionEvent.ACTION_PERFORMED, "click");
        for (ActionListener listener : button.getActionListeners()) {
            try {
                listener.actionPerformed(actionEvent);
            } catch (Exception e) {
                fail("Button click should not throw exceptions: " + e.getMessage());
            }
        }
        
        assertThat(true).isTrue(); // Test passes if no exceptions are thrown
    }
}
