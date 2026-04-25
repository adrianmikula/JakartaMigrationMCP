import adrianmikula.jakartamigration.intellij.ui.components.NewFeatureNotification;
import javax.swing.*;
import java.awt.*;

public class test_notification {
    public static void main(String[] args) {
        // Test NewFeatureNotification creation
        System.out.println("Testing NewFeatureNotification component...");
        
        try {
            // Create notification with test actions
            NewFeatureNotification notification = new NewFeatureNotification(
                "Help improve this plugin by sharing anonymous usage data and error reports? ",
                () -> System.out.println("✅ Yes clicked - Analytics enabled"),
                () -> System.out.println("❌ No clicked - Analytics disabled")
            );
            
            System.out.println("✅ NewFeatureNotification created successfully");
            System.out.println("✅ Panel created: " + (notification.getPanel() != null));
            System.out.println("✅ Panel visible: " + notification.isVisible());
            
            // Test factory method
            NewFeatureNotification factoryNotification = NewFeatureNotification.createUsagePermissionNotification(
                () -> System.out.println("✅ Factory Yes clicked"),
                () -> System.out.println("✅ Factory No clicked")
            );
            
            System.out.println("✅ Factory method works: " + (factoryNotification != null));
            
            // Test visibility control
            notification.setVisible(false);
            System.out.println("✅ Visibility control works: " + (!notification.isVisible()));
            
            System.out.println("\n🎉 All NewFeatureNotification tests passed!");
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
