package adrianmikula.jakartamigration.intellij.license;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

import java.awt.Desktop;
import java.net.URI;
import java.util.concurrent.TimeUnit;

/**
 * Service that monitors license status and notifies users when their license expires.
 *
 * This service runs on startup and periodically checks if the license has expired.
 * It shows a notification once per expiration event to avoid spamming the user.
 *
 * Features:
 * - Checks license status on startup and periodically (every 6 hours)
 * - Shows notification when license expires
 * - Provides actions to upgrade, renew, or dismiss
 * - Tracks notification state to avoid duplicates
 * - Integrates with existing license checking infrastructure
 */
public class LicenseExpirationNotifier implements StartupActivity {
    private static final Logger LOG = Logger.getInstance(LicenseExpirationNotifier.class);
    
    // Notification group ID - must match plugin.xml
    private static final String NOTIFICATION_GROUP_ID = "JakartaMigration.LicenseExpiration";
    
    // Plugin marketplace URL
    private static final String MARKETPLACE_URL = "https://plugins.jetbrains.com/plugin/30093-jakarta-migration";
    
    // Persistence keys
    private static final String EXPIRATION_NOTIFIED_KEY = "jakarta.migration.expiration.notified";
    private static final String LAST_CHECK_KEY = "jakarta.migration.expiration.lastCheck";
    private static final String WAS_LICENSED_KEY = "jakarta.migration.was.licensed";
    
    // Check interval: 6 hours in milliseconds
    private static final long CHECK_INTERVAL_MS = TimeUnit.HOURS.toMillis(6);
    
    // Singleton instance tracking
    private static volatile LicenseExpirationNotifier instance;
    private static volatile boolean isInitialized = false;
    
    @Override
    public void runActivity(@NotNull Project project) {
        LOG.info("LicenseExpirationNotifier: Starting up");
        
        // Store singleton instance
        instance = this;
        isInitialized = true;
        
        // Perform initial check
        checkAndNotify(project);
        
        // Schedule periodic checks
        schedulePeriodicChecks(project);
    }
    
    /**
     * Schedules periodic license checks using the application executor service.
     */
    private void schedulePeriodicChecks(@NotNull Project project) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            while (!project.isDisposed() && isInitialized) {
                try {
                    Thread.sleep(CHECK_INTERVAL_MS);
                    
                    if (!project.isDisposed()) {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (!project.isDisposed()) {
                                checkAndNotify(project);
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    LOG.debug("LicenseExpirationNotifier: Periodic check interrupted");
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }
    
    /**
     * Checks license status and shows notification if expired.
     * This method is safe to call from any thread.
     */
    public static void checkAndNotify(@NotNull Project project) {
        try {
            // Use SafeLicenseChecker to get current license status
            SafeLicenseChecker.LicenseResult result = SafeLicenseChecker.checkLicenseSafe();
            
            // Get the last known license state
            PropertiesComponent props = PropertiesComponent.getInstance();
            boolean wasLicensed = props.getBoolean(WAS_LICENSED_KEY, false);
            boolean wasNotified = props.getBoolean(EXPIRATION_NOTIFIED_KEY, false);
            
            // Update the "was licensed" state for future checks
            boolean isCurrentlyLicensed = result.isLicensed;
            if (isCurrentlyLicensed) {
                // User is licensed now - update state and reset notification flag
                props.setValue(WAS_LICENSED_KEY, true);
                props.setValue(EXPIRATION_NOTIFIED_KEY, false);
            }
            
            // Check if license just expired (was licensed, now not licensed)
            if (wasLicensed && !isCurrentlyLicensed && !wasNotified) {
                LOG.info("LicenseExpirationNotifier: License expired detected, showing notification");
                showExpirationNotification(project, result);
                props.setValue(EXPIRATION_NOTIFIED_KEY, true);
            }
            
            // Update last check timestamp
            props.setValue(LAST_CHECK_KEY, String.valueOf(System.currentTimeMillis()));
            
        } catch (Exception e) {
            LOG.warn("LicenseExpirationNotifier: Error checking license status", e);
        }
    }
    
    /**
     * Shows the expiration notification with appropriate actions.
     */
    private static void showExpirationNotification(@NotNull Project project, 
                                                    @NotNull SafeLicenseChecker.LicenseResult result) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed()) {
                return;
            }
            
            // Determine notification content based on current state
            String title = "License Expired";
            String content = "Your Jakarta Migration license has expired. Renew to continue using premium features.";
            
            // Create notification
            Notification notification = NotificationGroupManager.getInstance()
                    .getNotificationGroup(NOTIFICATION_GROUP_ID)
                    .createNotification(title, content, NotificationType.WARNING);
            
            // Add upgrade/renew action
            notification.addAction(new NotificationAction("Upgrade to Premium") {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                    openMarketplace();
                    notification.expire();
                }
            });
            
            // Add dismiss action
            notification.addAction(new NotificationAction("Dismiss") {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                    notification.expire();
                }
            });
            
            // Show notification
            notification.notify(project);
            
            LOG.info("LicenseExpirationNotifier: Expiration notification shown");
        });
    }
    
    /**
     * Opens the JetBrains Marketplace plugin page.
     */
    private static void openMarketplace() {
        try {
            Desktop.getDesktop().browse(new URI(MARKETPLACE_URL));
        } catch (Exception e) {
            LOG.warn("LicenseExpirationNotifier: Failed to open marketplace URL", e);
        }
    }
    
    /**
     * Manually trigger a license check (for testing or external use).
     */
    public static void triggerCheck(@NotNull Project project) {
        checkAndNotify(project);
    }
    
    /**
     * Resets the notification state (useful for testing).
     */
    public static void resetNotificationState() {
        PropertiesComponent props = PropertiesComponent.getInstance();
        props.setValue(EXPIRATION_NOTIFIED_KEY, false);
        props.setValue(WAS_LICENSED_KEY, false);
        LOG.info("LicenseExpirationNotifier: Notification state reset");
    }
    
    /**
     * Gets the last check timestamp.
     */
    public static long getLastCheckTime() {
        return PropertiesComponent.getInstance().getLong(LAST_CHECK_KEY, 0);
    }
    
    /**
     * Shutdown hook - stops periodic checks.
     */
    public static void shutdown() {
        isInitialized = false;
        LOG.info("LicenseExpirationNotifier: Shutdown");
    }
}
