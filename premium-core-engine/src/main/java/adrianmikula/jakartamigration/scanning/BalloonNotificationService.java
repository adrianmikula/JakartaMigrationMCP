package adrianmikula.jakartamigration.scanning;

import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Deduplication service for IDE balloon notifications.
 * Ensures each notification type is shown only once per project scan.
 * When a notification handler is registered (e.g., from the IntelliJ plugin),
 * notifications are dispatched to the IDE. Otherwise, they are logged only.
 */
@Slf4j
public class BalloonNotificationService {

    private final Set<String> shownNotifications = ConcurrentHashMap.newKeySet();
    private BiConsumer<String, String> notificationHandler;

    public BalloonNotificationService() {
    }

    /**
     * Creates a service with a notification handler for IDE integration.
     *
     * @param notificationHandler receives (title, message) and should display an IDE balloon
     */
    public BalloonNotificationService(BiConsumer<String, String> notificationHandler) {
        this.notificationHandler = notificationHandler;
    }

    /**
     * Sets or replaces the notification handler (e.g., for IDE plugin integration).
     */
    public void setNotificationHandler(BiConsumer<String, String> handler) {
        this.notificationHandler = handler;
    }

    /**
     * Shows a notification if it hasn't been shown before in this session.
     *
     * @param key     unique key for deduplication (e.g., "build-tool-failure:/path/to/project")
     * @param title   notification title
     * @param message notification message
     * @return true if notification was shown (not a duplicate)
     */
    public boolean showOnce(String key, String title, String message) {
        if (!shownNotifications.add(key)) {
            log.debug("Skipping duplicate notification: {}", key);
            return false;
        }
        if (notificationHandler != null) {
            try {
                notificationHandler.accept(title, message);
            } catch (Exception e) {
                log.warn("Failed to show notification '{}': {}", title, e.getMessage());
            }
        } else {
            log.info("[NOTIFICATION] {}: {}", title, message);
        }
        return true;
    }

    /**
     * Checks if a notification with the given key has already been shown.
     */
    public boolean hasShown(String key) {
        return shownNotifications.contains(key);
    }

    /**
     * Resets all shown notifications (e.g., for a new project scan).
     */
    public void reset() {
        shownNotifications.clear();
    }
}
