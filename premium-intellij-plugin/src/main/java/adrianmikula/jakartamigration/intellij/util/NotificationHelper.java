package adrianmikula.jakartamigration.intellij.util;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Helper utility for displaying notification balloons.
 * Provides a centralized way to show warnings and errors as non-intrusive notifications.
 */
public class NotificationHelper {

    private static final String NOTIFICATION_GROUP_ID = "JakartaMigration.Notifications";

    /**
     * Displays a warning notification balloon.
     *
     * @param project The project context
     * @param title The notification title
     * @param message The notification message
     */
    public static void showWarning(@NotNull Project project, @NotNull String title, @NotNull String message) {
        Notification notification = NotificationGroupManager.getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP_ID)
                .createNotification(title, message, NotificationType.WARNING);
        Notifications.Bus.notify(notification, project);
    }

    /**
     * Displays an error notification balloon.
     *
     * @param project The project context
     * @param title The notification title
     * @param message The notification message
     */
    public static void showError(@NotNull Project project, @NotNull String title, @NotNull String message) {
        Notification notification = NotificationGroupManager.getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP_ID)
                .createNotification(title, message, NotificationType.ERROR);
        Notifications.Bus.notify(notification, project);
    }
}
