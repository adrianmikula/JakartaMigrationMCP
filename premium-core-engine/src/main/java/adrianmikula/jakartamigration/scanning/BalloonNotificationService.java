package adrianmikula.jakartamigration.scanning;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Deduplication service for IDE balloon notifications.
 * Uses a per-key cooldown so the same notification is suppressed for
 * {@link #COOLDOWN_MS} after it was last shown, then allowed again.
 * When a notification handler is registered (e.g., from the IntelliJ plugin),
 * notifications are dispatched to the IDE. Otherwise, they are logged only.
 */
@Slf4j
public class BalloonNotificationService {

    /** Cooldown period in milliseconds – a notification with the same key won't be re-shown within this window. */
    static final long COOLDOWN_MS = 5 * 60 * 1000L; // 5 minutes

    /** Maps notification key → epoch millis when it was last shown. */
    private final ConcurrentHashMap<String, Long> lastShownTimestamps = new ConcurrentHashMap<>();

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
     * Shows a notification unless it was already shown within the cooldown window.
     *
     * @param key     unique key for deduplication (e.g., "build-tool-failure:/path/to/project")
     * @param title   notification title
     * @param message notification message
     * @return true if notification was shown (not suppressed by cooldown)
     */
    public boolean showOnce(String key, String title, String message) {
        long now = System.currentTimeMillis();
        Long existing = lastShownTimestamps.get(key);

        if (existing != null && (now - existing) < COOLDOWN_MS) {
            log.debug("Skipping notification within cooldown window: {}", key);
            return false;
        }

        // Atomic update: only show if this thread actually installed the new timestamp.
        // putIfAbsent covers the first-show race; replace covers the re-show race.
        boolean updated = (existing == null)
                ? lastShownTimestamps.putIfAbsent(key, now) == null
                : lastShownTimestamps.replace(key, existing, now);

        if (!updated) {
            log.debug("Notification for {} already handled by another thread", key);
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
     * Checks if a notification with the given key is currently within its cooldown window.
     */
    public boolean hasShown(String key) {
        Long lastShown = lastShownTimestamps.get(key);
        return lastShown != null && (System.currentTimeMillis() - lastShown) < COOLDOWN_MS;
    }

    /**
     * Resets all shown notifications (e.g., for a new project scan).
     */
    public void reset() {
        lastShownTimestamps.clear();
    }
}
