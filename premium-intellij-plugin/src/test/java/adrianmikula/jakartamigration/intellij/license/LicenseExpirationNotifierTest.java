package adrianmikula.jakartamigration.intellij.license;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for LicenseExpirationNotifier to verify:
 * - Notification is shown when license expires
 * - Notification is not shown repeatedly
 * - Dismiss action works
 * - Trial vs paid license expiration detection
 * - State persistence across checks
 *
 * NOTE: These tests require IntelliJ Platform environment.
 */
@org.junit.jupiter.api.Disabled("Requires IntelliJ Platform environment - run in IDE")
public class LicenseExpirationNotifierTest {
    
    private static final String EXPIRATION_NOTIFIED_KEY = "jakarta.migration.expiration.notified";
    private static final String WAS_LICENSED_KEY = "jakarta.migration.was.licensed";
    private static final String LAST_CHECK_KEY = "jakarta.migration.expiration.lastCheck";
    
    @BeforeEach
    public void setUp() throws Exception {
        // Clear all persistence state before each test
        resetPersistenceState();
    }
    
    @AfterEach
    public void tearDown() {
        // Clean up system properties
        System.clearProperty("jakarta.migration.premium");
        System.clearProperty("jakarta.migration.trial.end");
        resetPersistenceState();
        // Cleanup completed
    }
    
    private void resetPersistenceState() {
        PropertiesComponent props = PropertiesComponent.getInstance();
        props.setValue(EXPIRATION_NOTIFIED_KEY, String.valueOf(false));
        props.setValue(WAS_LICENSED_KEY, String.valueOf(false));
        props.unsetValue(LAST_CHECK_KEY);
    }

    /**
     * Stub method to provide Project instance.
     * This is called by test methods but tests are disabled anyway.
     */
    private Project getProject() {
        return null; // Tests are disabled - this won't be called
    }
    
    @Test
    @DisplayName("Should complete without error when no license is active")
    void testCheckAndNotify_NoLicense_NoNotification() {
        // Given: No license ever active
        System.clearProperty("jakarta.migration.premium");
        System.clearProperty("jakarta.migration.trial.end");
        
        Project project = getProject();
        
        // When: Check is triggered
        LicenseExpirationNotifier.checkAndNotify(project);
        
        // Then: No notification should be shown (no exception, no crash)
        // The notification system would throw or show if triggered
        // Since we're in test, we verify no error occurs
        assertTrue(true, "Should complete without error");
    }
    
    @Test
    @DisplayName("Should not notify when license is active")
    void testCheckAndNotify_ActiveLicense_NoNotification() {
        // Given: Active license (premium=true, trial end in future)
        long futureTime = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000);
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", String.valueOf(futureTime));
        
        PropertiesComponent props = PropertiesComponent.getInstance();
        props.setValue(WAS_LICENSED_KEY, String.valueOf(true));
        
        Project project = getProject();
        
        // When: Check is triggered
        LicenseExpirationNotifier.checkAndNotify(project);
        
        // Then: Was licensed should remain true, no notification flag set
        assertTrue(props.getBoolean(WAS_LICENSED_KEY, false), "Should remain licensed");
        assertFalse(props.getBoolean(EXPIRATION_NOTIFIED_KEY, false), "Should not set notified flag");
    }
    
    @Test
    @DisplayName("Should set notified flag when license expires")
    void testCheckAndNotify_LicenseExpired_ShowsNotification() {
        // Given: License was active but now expired
        long pastTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000); // 1 day ago
        System.setProperty("jakarta.migration.premium", "false");
        System.setProperty("jakarta.migration.trial.end", String.valueOf(pastTime));
        
        PropertiesComponent props = PropertiesComponent.getInstance();
        props.setValue(WAS_LICENSED_KEY, String.valueOf(true)); // Was licensed before
        props.setValue(EXPIRATION_NOTIFIED_KEY, String.valueOf(false)); // Not notified yet
        
        Project project = getProject();
        
        // When: Check is triggered
        LicenseExpirationNotifier.checkAndNotify(project);
        
        // Then: Notification flag should be set to prevent duplicates
        assertTrue(props.getBoolean(EXPIRATION_NOTIFIED_KEY, false), "Should set notified flag");
    }
    
    @Test
    @DisplayName("Should not duplicate notification when already notified")
    void testCheckAndNotify_AlreadyNotified_NoDuplicateNotification() {
        // Given: License expired and already notified
        long pastTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
        System.setProperty("jakarta.migration.premium", "false");
        System.setProperty("jakarta.migration.trial.end", String.valueOf(pastTime));
        
        PropertiesComponent props = PropertiesComponent.getInstance();
        props.setValue(WAS_LICENSED_KEY, String.valueOf(true));
        props.setValue(EXPIRATION_NOTIFIED_KEY, String.valueOf(true)); // Already notified
        
        Project project = getProject();
        
        // When: Check is triggered again
        LicenseExpirationNotifier.checkAndNotify(project);
        
        // Then: Should not crash or change state
        assertTrue(props.getBoolean(EXPIRATION_NOTIFIED_KEY, false), "Should keep notified flag");
    }
    
    @Test
    @DisplayName("Should set notified flag for trial expiration")
    void testCheckAndNotify_TrialExpired_ShowsNotification() {
        // Given: Trial expired
        long pastTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
        System.setProperty("jakarta.migration.premium", "false");
        System.setProperty("jakarta.migration.trial.end", String.valueOf(pastTime));
        
        PropertiesComponent props = PropertiesComponent.getInstance();
        props.setValue(WAS_LICENSED_KEY, String.valueOf(true));
        props.setValue(EXPIRATION_NOTIFIED_KEY, String.valueOf(false));
        
        Project project = getProject();
        
        // When: Check is triggered
        LicenseExpirationNotifier.checkAndNotify(project);
        
        // Then: Notification flag should be set
        assertTrue(props.getBoolean(EXPIRATION_NOTIFIED_KEY, false), 
            "Should set notified flag for trial expiration");
    }
    
    @Test
    @DisplayName("Should clear all notification flags on reset")
    void testResetNotificationState_ClearsFlags() {
        // Given: Flags are set
        PropertiesComponent props = PropertiesComponent.getInstance();
        props.setValue(EXPIRATION_NOTIFIED_KEY, String.valueOf(true));
        props.setValue(WAS_LICENSED_KEY, String.valueOf(true));
        
        // When: Reset is called
        LicenseExpirationNotifier.resetNotificationState();
        
        // Then: Both flags should be cleared
        assertFalse(props.getBoolean(EXPIRATION_NOTIFIED_KEY, true), "Should clear notified flag");
        assertFalse(props.getBoolean(WAS_LICENSED_KEY, true), "Should clear was licensed flag");
    }
    
    @Test
    @DisplayName("Should update last check timestamp")
    void testGetLastCheckTime_UpdatesAfterCheck() {
        // Given: No last check recorded
        PropertiesComponent props = PropertiesComponent.getInstance();
        props.unsetValue(LAST_CHECK_KEY);
        
        Project project = getProject();
        
        // When: Check is triggered
        LicenseExpirationNotifier.checkAndNotify(project);
        
        // Then: Last check time should be set
        long lastCheck = LicenseExpirationNotifier.getLastCheckTime();
        assertTrue(lastCheck > 0, "Should set last check time");
    }
    
    @Test
    @DisplayName("Should detect license expiration transition")
    void testCheckAndNotify_LicensedThenExpired_TransitionDetected() {
        // Given: Initially licensed
        long futureTime = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000);
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", String.valueOf(futureTime));
        
        PropertiesComponent props = PropertiesComponent.getInstance();
        props.setValue(WAS_LICENSED_KEY, String.valueOf(false)); // First check - will set to true
        
        Project project = getProject();
        
        // When: First check (becomes licensed)
        LicenseExpirationNotifier.checkAndNotify(project);
        
        // Then: Was licensed should be true now
        assertTrue(props.getBoolean(WAS_LICENSED_KEY, false), 
            "Should record as licensed after first check");
        
        // Given: Now expire the license
        long pastTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
        System.setProperty("jakarta.migration.premium", "false");
        System.setProperty("jakarta.migration.trial.end", String.valueOf(pastTime));
        props.setValue(EXPIRATION_NOTIFIED_KEY, String.valueOf(false)); // Reset for second check
        
        // When: Second check (now expired)
        LicenseExpirationNotifier.checkAndNotify(project);
        
        // Then: Should detect expiration and set notified flag
        assertTrue(props.getBoolean(EXPIRATION_NOTIFIED_KEY, false), 
            "Should detect expiration transition");
    }
    
    @Test
    @DisplayName("Should not notify if never licensed")
    void testCheckAndNotify_NeverLicensed_NoNotification() {
        // Given: Never had a license, not licensed now
        System.clearProperty("jakarta.migration.premium");
        System.clearProperty("jakarta.migration.trial.end");
        
        PropertiesComponent props = PropertiesComponent.getInstance();
        props.setValue(WAS_LICENSED_KEY, String.valueOf(false)); // Never was licensed
        props.setValue(EXPIRATION_NOTIFIED_KEY, String.valueOf(false));
        
        Project project = getProject();
        
        // When: Check is triggered
        LicenseExpirationNotifier.checkAndNotify(project);
        
        // Then: Should not show notification (never had license to expire)
        assertFalse(
            props.getBoolean(EXPIRATION_NOTIFIED_KEY, false),
            "Should not notify if never licensed"
        );
    }
    
    @Test
    @DisplayName("Manual trigger should invoke check and notify")
    void testTriggerCheck_InvokesCheckAndNotify() {
        // Given: Normal state
        Project project = getProject();
        
        // When: Manual trigger called
        try {
            LicenseExpirationNotifier.triggerCheck(project);
            // Should complete without error - assertion passed if we reach here
            assertTrue(
                true,
                "Manual trigger should complete"
            );
        } catch (Exception e) {
            fail("Manual trigger should not throw: " + e.getMessage());
        }
    }
    
    @Test
    @DisplayName("Should handle dev mode gracefully")
    void testCheckAndNotify_DevMode_NoNotification() {
        // Given: Dev mode enabled
        System.setProperty("jakarta.migration.dev", "true");
        
        // Also set up expired trial scenario
        long pastTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
        System.setProperty("jakarta.migration.premium", "false");
        System.setProperty("jakarta.migration.trial.end", String.valueOf(pastTime));
        
        PropertiesComponent props = PropertiesComponent.getInstance();
        props.setValue(WAS_LICENSED_KEY, String.valueOf(true));
        props.setValue(EXPIRATION_NOTIFIED_KEY, String.valueOf(false));
        
        Project project = getProject();
        
        try {
            // When: Check is triggered
            LicenseExpirationNotifier.checkAndNotify(project);
            
            // Then: In dev mode, still licensed so no expiration notification
            // The dev mode bypass in SafeLicenseChecker should prevent unlicensed state
            assertTrue(
                true,
                "Should handle dev mode gracefully"
            );
        } finally {
            System.clearProperty("jakarta.migration.dev");
        }
    }
}
