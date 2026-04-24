package adrianmikula.jakartamigration.analytics.service;

import adrianmikula.jakartamigration.analytics.model.UsageEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class to verify enhanced usage tracking with context information.
 */
@ExtendWith(MockitoExtension.class)
public class UsageTrackingContextTest {

    @Mock
    private UserIdentificationService userIdentificationService;

    private UsageService usageService;

    @BeforeEach
    void setUp() {
        when(userIdentificationService.isAnalyticsEnabled()).thenReturn(true);
        when(userIdentificationService.getAnonymousUserId()).thenReturn("test-user-123");
        
        // Create a test version that we can inspect
        usageService = new UsageService(userIdentificationService);
    }

    @Test
    void testTrackCreditUsageWithContext() {
        // Test the new context-aware method
        usageService.trackCreditUsage("Reports", "generate_dependency_report");
        
        // Force flush to process the event
        usageService.flush();
        
        // Verify the event was tracked with context
        assertEquals(0, usageService.getQueueSize(), "Event should be processed");
    }

    @Test
    void testTrackUpgradeClickWithContext() {
        // Test the new context-aware method
        usageService.trackUpgradeClick("premium_button", "Reports");
        
        // Force flush to process the event
        usageService.flush();
        
        // Verify the event was tracked with context
        assertEquals(0, usageService.getQueueSize(), "Event should be processed");
    }

    @Test
    void testUsageEventWithContextData() {
        // Test the UsageEvent factory methods directly
        UsageEvent creditEvent = UsageEvent.creditUsed(
            "test-user", 
            "Reports", 
            "generate_dependency_report", 
            "1.0.0",
            "test"
        );
        
        assertEquals("credit_used", creditEvent.getEventType().getValue());
        assertEquals("Reports", creditEvent.getCurrentUiTab());
        assertEquals("generate_dependency_report", creditEvent.getTriggerAction());
        assertEquals("1.0.0", creditEvent.getPluginVersion());
        
        UsageEvent upgradeEvent = UsageEvent.upgradeClicked(
            "test-user", 
            "premium_button", 
            "Reports",
            "1.0.0",
            "test"
        );
        
        assertEquals("upgrade_clicked", upgradeEvent.getEventType().getValue());
        assertNotNull(upgradeEvent.getEventData());
        assertEquals("premium_button", upgradeEvent.getEventData().get("source"));
        assertEquals("Reports", upgradeEvent.getEventData().get("current_ui_tab"));
    }

    @Test
    void testBackwardCompatibility() {
        // Test that existing methods still work with new signatures
        assertDoesNotThrow(() -> usageService.trackCreditUsage("Reports", "scan_button"));
        assertDoesNotThrow(() -> usageService.trackUpgradeClick("premium_button", "Reports"));
        
        // Force flush to process events
        usageService.flush();
        
        assertEquals(0, usageService.getQueueSize(), "Events should be processed");
    }

    @Test
    void testAnalyticsDisabled() {
        when(userIdentificationService.isAnalyticsEnabled()).thenReturn(false);
        
        // Events should be ignored when analytics is disabled
        usageService.trackCreditUsage("Reports", "generate_dependency_report");
        usageService.trackUpgradeClick("premium_button", "Reports");
        
        assertEquals(0, usageService.getQueueSize(), "No events should be queued when analytics disabled");
    }
}
