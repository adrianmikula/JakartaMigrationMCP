package adrianmikula.jakartamigration.intellij.ui.components;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

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
        assertNotNull(notification);
        assertNotNull(notification.getPanel());
        assertTrue(notification.isVisible());
        
        System.out.println("✅ NewFeatureNotification component test passed!");
    }
}
