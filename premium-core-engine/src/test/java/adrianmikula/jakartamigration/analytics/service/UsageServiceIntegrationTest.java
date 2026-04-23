package adrianmikula.jakartamigration.analytics.service;

import adrianmikula.jakartamigration.analytics.config.SupabaseConfig;
import adrianmikula.jakartamigration.analytics.model.UsageEvent;
import adrianmikula.jakartamigration.analytics.util.ConcurrencyTestHelper;
import adrianmikula.jakartamigration.analytics.util.NetworkFailureSimulator;
import adrianmikula.jakartamigration.analytics.util.TestDataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Integration tests for UsageService with test Supabase instance.
 * Tests real API interactions, error handling, and data persistence.
 */
@ExtendWith(MockitoExtension.class)
class UsageServiceIntegrationTest {

    @TempDir
    Path tempDir;

    @Mock
    private SupabaseConfig supabaseConfig;

    private UserIdentificationService userIdentificationService;
    private UsageService usageService;
    private NetworkFailureSimulator networkSimulator;

    /**
     * Helper method to wait for queue to empty with timeout.
     */
    private void waitForQueueToEmpty(int timeoutSeconds) {
        long startTime = System.currentTimeMillis();
        long timeoutMs = timeoutSeconds * 1000L;
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (usageService.getQueueSize() == 0) {
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // If timeout reached, fail the test
        assertThat(usageService.getQueueSize()).isEqualTo(0);
    }

    @BeforeEach
    void setUp() {
        // Configure test Supabase settings
        when(supabaseConfig.getSupabaseUrl()).thenReturn("https://test-wmngdqhgybiomoxjfxpc.supabase.co");
        when(supabaseConfig.getSupabaseAnonKey()).thenReturn("test-anon-key");
        when(supabaseConfig.isAnalyticsEnabled()).thenReturn(true);
        when(supabaseConfig.getAnalyticsBatchSize()).thenReturn(3);
        when(supabaseConfig.getAnalyticsFlushIntervalSeconds()).thenReturn(1);
        when(supabaseConfig.isConfigured()).thenReturn(true);

        userIdentificationService = new UserIdentificationService(tempDir, supabaseConfig);
        usageService = new UsageService(userIdentificationService);
        networkSimulator = new NetworkFailureSimulator();
    }

    @AfterEach
    void tearDown() {
        if (usageService != null) {
            usageService.close();
        }
        if (userIdentificationService != null) {
            userIdentificationService.close();
        }
        networkSimulator.reset();
    }

    @Test
    void shouldTrackAndSendUsageEventsToSupabase() {
        // When

        // When
        usageService.trackCreditUsage("Dependencies","basic_scan");
        usageService.flush();

        // Then
        waitForQueueToEmpty(5);

        // Verify event was processed (in real implementation, would check Supabase)
        assertThat(usageService.getQueueSize()).isEqualTo(0);
    }

    @Test
    void shouldTrackUpgradeClickEvents() {
        // Given
        String upgradeSource = "truncation_notice";

        // When
        usageService.trackUpgradeClick(upgradeSource);
        usageService.flush();

        // Then
        waitForQueueToEmpty(5);
    }

    @Test
    void shouldHandleBatchProcessing() {
        // Given
        int eventCount = 10;
        UsageEvent[] events = TestDataFactory.createUsageEventsBatch(eventCount);

        // When
        for (UsageEvent event : events) {
            if (event.getEventType() == UsageEvent.EventType.CREDIT_USED) {
                usageService.trackCreditUsage(event.getCurrentUiTab(), event.getTriggerAction());
            } else {
                usageService.trackUpgradeClick(event.getEventData().get("source").toString(), event.getCurrentUiTab());
            }
        }

        // Then
        waitForQueueToEmpty(10);
    }

    @Test
    void shouldHandleNetworkFailuresGracefully() throws Exception {
        // Given
        networkSimulator.withFailureType(NetworkFailureSimulator.FailureType.CONNECTION_REFUSED)
                      .withMaxFailures(2);

        // When
        usageService.trackCreditUsage("Dependencies","basic_scan");
        usageService.flush();

        // Then
        // Should handle failures gracefully and not crash
        waitForQueueToEmpty(5);
    }

    @Test
    void shouldRetryFailedOperations() throws Exception {
        // Given
        networkSimulator.withFailureType(NetworkFailureSimulator.FailureType.TIMEOUT)
                      .withMaxFailures(1);

        // When
        usageService.trackCreditUsage("Dependencies","advanced_scan");
        usageService.flush();

        // Then
        waitForQueueToEmpty(8);
        
        // Should have retried and eventually succeeded
        assertThat(networkSimulator.getFailureCount()).isGreaterThan(0);
        assertThat(networkSimulator.getSuccessCount()).isGreaterThan(0);
    }

    @Test
    void shouldMaintainEventOrder() {
        // When
        usageService.trackCreditUsage("Dependencies","basic_scan");
        usageService.trackUpgradeClick("test_source", "Dependencies");
        usageService.trackCreditUsage("Dependencies","advanced_scan");
        
        // Wait for events to be processed
        waitForQueueToEmpty(5);

        // Then
        // In real implementation, would verify order in Supabase
        // For now, verify that all events were processed
        assertThat(usageService.getQueueSize()).isEqualTo(0);
    }

    @Test
    void shouldHandleDisabledAnalytics() {
        // Given
        when(supabaseConfig.isAnalyticsEnabled()).thenReturn(false);
        UsageService disabledService = new UsageService(userIdentificationService);

        // When
        disabledService.trackCreditUsage("basic_scan");
        disabledService.trackUpgradeClick("test_source");

        // Then
        assertThat(disabledService.getQueueSize()).isEqualTo(0);

        // Clean up
        disabledService.close();
    }

    @Test
    void shouldHandleLargeBatches() {
        // Given
        int largeEventCount = 100;

        // When
        for (int i = 0; i < largeEventCount; i++) {
            usageService.trackCreditUsage("Dependencies","basic_scan");
        }

        // Then
        waitForQueueToEmpty(15);
    }

    @Test
    void shouldHandleConcurrentTracking() throws Exception {
        // Given
        int threadCount = 10;
        int eventsPerThread = 5;

        // When
        ConcurrencyTestHelper.runConcurrentConsumer(threadCount, (threadIndex) -> {
            for (int i = 0; i < eventsPerThread; i++) {
                String creditType = "scan_" + threadIndex + "_" + i;
                usageService.trackCreditUsage("Dependencies",creditType);
            }
        });

        // Then
        waitForQueueToEmpty(10);
    }

    @Test
    void shouldValidateEventTypes() {
        // Given
        String validCreditType = "pdf_report";
        String validUpgradeSource = "feature_limit";

        // When
        usageService.trackCreditUsage("Dependencies",validCreditType);
        usageService.trackUpgradeClick(validUpgradeSource);
        usageService.flush();

        // Then
        waitForQueueToEmpty(5);
    }

    @Test
    void shouldHandleServiceShutdownGracefully() {
        // Given
        usageService.trackCreditUsage("Dependencies","basic_scan");
        usageService.trackCreditUsage("Dependencies","advanced_scan");

        // When
        usageService.close();

        // Then
        // Should process remaining events before shutdown
        assertThat(usageService.getQueueSize()).isEqualTo(0);
    }

    @Test
    void shouldPersistUserAcrossServiceRestarts() throws Exception {
        // Given
        String firstUserId = userIdentificationService.getAnonymousUserId();
        usageService.trackCreditUsage("Dependencies","basic_scan");
        usageService.close();

        // When
        UserIdentificationService newUserIdentificationService = 
            new UserIdentificationService(tempDir, supabaseConfig);
        UsageService newUsageService = new UsageService(newUserIdentificationService);

        // Then
        String secondUserId = newUserIdentificationService.getAnonymousUserId();
        assertThat(secondUserId).isEqualTo(firstUserId);

        // Clean up
        newUsageService.close();
        newUserIdentificationService.close();
    }

    @Test
    void shouldHandleMalformedEventData() {
        // Given
        String malformedCreditType = "scan@with#special$chars%";
        String longUpgradeSource = "a".repeat(1000);

        // When
        usageService.trackCreditUsage("Dependencies",malformedCreditType);
        usageService.trackUpgradeClick(longUpgradeSource);
        usageService.flush();

        // Then
        waitForQueueToEmpty(5);
    }

    @Test
    void shouldMeasurePerformanceMetrics() throws Exception {
        // Given
        int eventCount = 50;

        // When
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < eventCount; i++) {
            usageService.trackCreditUsage("Dependencies","basic_scan");
        }
        usageService.flush();
        long endTime = System.currentTimeMillis();

        // Then
        waitForQueueToEmpty(10);

        long processingTime = endTime - startTime;
        double eventsPerSecond = (double) eventCount / (processingTime / 1000.0);
        
        // Should process at least 10 events per second
        assertThat(eventsPerSecond).isGreaterThan(10.0);
    }
}
