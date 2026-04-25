package adrianmikula.jakartamigration.intellij.ui.components;

import org.junit.Test;
import org.junit.Assert;

/**
 * Simple test to verify NewFeatureNotification component compiles and works.
 */
public class SimpleNotificationTest {
    
    @Test
    public void testNotificationCreation() {
        // Given
        String message = "Test message";
        
        // When - should not throw exception
        NewFeatureNotification notification = new NewFeatureNotification(message, 
            () -> System.out.println("Yes clicked"), 
            () -> System.out.println("No clicked"));
        
        // Then
        Assert.assertNotNull("Notification should be created", notification);
        Assert.assertNotNull("Panel should be created", notification.getPanel());
        Assert.assertTrue("Panel should be visible by default", notification.isVisible());
        
        System.out.println("✅ NewFeatureNotification component test passed!");
    }
}
