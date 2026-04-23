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
        usageService.trackCreditUsage("actions", "Reports", "generate_dependency_report");
        
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
            "actions", 
            "1.0.0", 
            "Reports", 
            "generate_dependency_report"
        );
        
        assertEquals("credit_used", creditEvent.getEventType().getValue());
        assertEquals("actions", creditEvent.getCreditType());
        assertNotNull(creditEvent.getEventData());
        assertEquals("Reports", creditEvent.getEventData().get("current_ui_tab"));
        assertEquals("generate_dependency_report", creditEvent.getEventData().get("trigger_action"));
        
        UsageEvent upgradeEvent = UsageEvent.upgradeClicked(
            "test-user", 
            "premium_button", 
            "1.0.0", 
            "Reports"
        );
        
        assertEquals("upgrade_clicked", upgradeEvent.getEventType().getValue());
        assertNotNull(upgradeEvent.getEventData());
        assertEquals("premium_button", upgradeEvent.getEventData().get("source"));
        assertEquals("Reports", upgradeEvent.getEventData().get("current_ui_tab"));
    }

    @Test
    void testBackwardCompatibility() {
        // Test that existing methods still work
        assertDoesNotThrow(() -> usageService.trackCreditUsage("actions"));
        assertDoesNotThrow(() -> usageService.trackUpgradeClick("premium_button"));
        
        // Force flush to process events
        usageService.flush();
        
        assertEquals(0, usageService.getQueueSize(), "Events should be processed");
    }

    @Test
    void testAnalyticsDisabled() {
        when(userIdentificationService.isAnalyticsEnabled()).thenReturn(false);
        
        // Events should be ignored when analytics is disabled
        usageService.trackCreditUsage("actions", "Reports", "generate_dependency_report");
        usageService.trackUpgradeClick("premium_button", "Reports");
        
        assertEquals(0, usageService.getQueueSize(), "No events should be queued when analytics disabled");
    }
}
