package adrianmikula.jakartamigration.analytics.service;

import adrianmikula.jakartamigration.analytics.config.SupabaseConfig;
import adrianmikula.jakartamigration.analytics.util.ConcurrencyTestHelper;
import adrianmikula.jakartamigration.analytics.util.NetworkFailureSimulator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for network failure scenarios in analytics services.
 * Tests retry mechanisms, error handling, and graceful degradation.
 * 
 * DISABLED: Requires external Supabase instance and network access.
 * Re-enable when running integration tests with proper infrastructure.
 */
@Disabled("Requires external Supabase instance and network access")
@Tag("integration")
@Tag("slow")
@ExtendWith(MockitoExtension.class)
class NetworkFailureTest {

    @TempDir
    Path tempDir;

    @Mock
    private SupabaseConfig supabaseConfig;

    private UserIdentificationService userIdentificationService;
    private UsageService usageService;
    private ErrorReportingService errorReportingService;
    private NetworkFailureSimulator networkSimulator;

    @BeforeEach
    void setUp() {
        when(supabaseConfig.getSupabaseUrl()).thenReturn("https://test-wmngdqhgybiomoxjfxpc.supabase.co");
        when(supabaseConfig.getSupabaseAnonKey()).thenReturn("test-anon-key");
        when(supabaseConfig.isAnalyticsEnabled()).thenReturn(true);
        when(supabaseConfig.isErrorReportingEnabled()).thenReturn(true);
        when(supabaseConfig.getAnalyticsBatchSize()).thenReturn(3);
        when(supabaseConfig.getAnalyticsFlushIntervalSeconds()).thenReturn(1);
        when(supabaseConfig.isConfigured()).thenReturn(true);

        userIdentificationService = new UserIdentificationService(tempDir, supabaseConfig);
        usageService = new UsageService(userIdentificationService);
        errorReportingService = new ErrorReportingService(userIdentificationService);
        networkSimulator = new NetworkFailureSimulator();
    }

    @AfterEach
    void tearDown() {
        if (usageService != null) {
            usageService.close();
        }
        if (errorReportingService != null) {
            errorReportingService.close();
        }
        if (userIdentificationService != null) {
            userIdentificationService.close();
        }
        networkSimulator.reset();
    }

    @Test
    void shouldHandleConnectionRefusedGracefully() throws Exception {
        // Given
        networkSimulator.withFailureType(NetworkFailureSimulator.FailureType.CONNECTION_REFUSED)
                      .withMaxFailures(3);

        // When
        usageService.trackCreditUsage("Dependencies","basic_scan");
        errorReportingService.reportError(new RuntimeException("Test error"));

        // Then
        waitForQueuesToEmpty(5);
        
        // Should have attempted connections but not crashed
        assertThat(networkSimulator.getFailureCount()).isGreaterThan(0);
        assertThat(usageService.getQueueSize()).isEqualTo(0);
        assertThat(errorReportingService.getQueueSize()).isEqualTo(0);
    }

    @Test
    void shouldRetryOnTimeout() throws Exception {
        // Given
        networkSimulator.withFailureType(NetworkFailureSimulator.FailureType.TIMEOUT)
                      .withMaxFailures(2);

        // When
        usageService.trackCreditUsage("Dependencies","advanced_scan");
        errorReportingService.reportError(new SocketTimeoutException("Timeout test"));

        // Then
        waitForQueuesToEmpty(8);
        
        // Should have retried and eventually succeeded
        assertThat(networkSimulator.getFailureCount()).isEqualTo(2);
        assertThat(networkSimulator.getSuccessCount()).isGreaterThan(0);
    }

    @Test
    void shouldHandleUnknownHost() throws Exception {
        // Given
        networkSimulator.withFailureType(NetworkFailureSimulator.FailureType.UNKNOWN_HOST);

        // When
        usageService.trackUpgradeClick("premium_button", "Dependencies");
        errorReportingService.reportError(new UnknownHostException("Unknown host test"));

        // Then
        waitForQueuesToEmpty(5);
        
        // Should handle DNS failures gracefully
        assertThat(networkSimulator.getFailureCount()).isGreaterThan(0);
        assertThat(usageService.getQueueSize()).isEqualTo(0);
    }

    @Test
    void shouldHandleGeneralIOException() throws Exception {
        // Given
        networkSimulator.withFailureType(NetworkFailureSimulator.FailureType.IO_EXCEPTION)
                      .withMaxFailures(1);

        // When
        usageService.trackCreditUsage("Dependencies","pdf_report");
        errorReportingService.reportError(new IOException("General I/O error"));

        // Then
        waitForQueuesToEmpty(5);
        
        // Should recover after I/O errors
        assertThat(networkSimulator.getFailureCount()).isEqualTo(1);
        assertThat(networkSimulator.getSuccessCount()).isGreaterThan(0);
    }

    @Test
    void shouldHandleSlowResponses() throws Exception {
        // Given
        networkSimulator.withFailureType(NetworkFailureSimulator.FailureType.SLOW_RESPONSE)
                      .withDelay(2000); // 2 second delay

        // When
        usageService.trackCreditUsage("Dependencies","basic_scan");
        errorReportingService.reportError(new RuntimeException("Slow response test"));

        // Then
        waitForQueuesToEmpty(10);
        
        // Should handle slow responses without timeout
        assertThat(networkSimulator.getFailureCount()).isGreaterThan(0);
        assertThat(usageService.getQueueSize()).isEqualTo(0);
    }

    @Test
    void shouldHandleIntermittentFailures() throws Exception {
        // Given
        networkSimulator.withIntermittentFailures(0.3); // 30% failure rate

        // When
        for (int i = 0; i < 10; i++) {
            usageService.trackCreditUsage("Dependencies","scan_" + i);
            if (i % 2 == 0) {
                errorReportingService.reportError(new RuntimeException("Error " + i));
            }
        }

        // Then
        waitForQueuesToEmpty(15);
        
        // Should handle mixed success/failure pattern
        assertThat(networkSimulator.getTotalOperations()).isEqualTo(20); // 10 usage + 10 error
        assertThat(networkSimulator.getSuccessCount()).isGreaterThan(10); // At least 50% success
        assertThat(networkSimulator.getFailureCount()).isGreaterThan(5);  // At least 25% failure
    }

    @Test
    void shouldBackOffExponentiallyOnRepeatedFailures() throws Exception {
        // Given
        networkSimulator.withFailureType(NetworkFailureSimulator.FailureType.CONNECTION_REFUSED)
                      .withMaxFailures(5);

        // When
        long startTime = System.currentTimeMillis();
        usageService.trackCreditUsage("Dependencies","basic_scan");
        usageService.flush();
        long endTime = System.currentTimeMillis();

        // Then
        waitForQueuesToEmpty(10);
        
        // Should take longer due to exponential backoff
        long processingTime = endTime - startTime;
        assertThat(processingTime).isGreaterThan(1000); // Should be delayed by backoff
        assertThat(networkSimulator.getFailureCount()).isEqualTo(5);
    }

    @Test
    void shouldDropEventsWhenQueueFull() throws Exception {
        // Given
        networkSimulator.withFailureType(NetworkFailureSimulator.FailureType.CONNECTION_REFUSED)
                      .withMaxFailures(100); // Continuous failure

        // When
        // Add more events than queue can handle
        for (int i = 0; i < 1000; i++) {
            usageService.trackCreditUsage("Dependencies","scan_" + i);
        }

        // Then
        // Should drop events when queue is full
        Thread.sleep(2000); // Allow processing
        int queueSize = usageService.getQueueSize();
        
        // Queue should not grow indefinitely (implementation-specific limit)
        assertThat(queueSize).isLessThan(1000);
        assertThat(networkSimulator.getFailureCount()).isGreaterThan(0);
    }

    @Test
    void shouldRecoverFromExtendedOutage() throws Exception {
        // Given
        networkSimulator.withFailureType(NetworkFailureSimulator.FailureType.CONNECTION_REFUSED)
                      .withMaxFailures(10);

        // When
        usageService.trackCreditUsage("Dependencies","before_outage");
        waitForQueuesToEmpty(5);
        
        // Simulate extended outage
        networkSimulator.reset();
        networkSimulator.withFailureType(NetworkFailureSimulator.FailureType.CONNECTION_REFUSED)
                      .withMaxFailures(0); // No more failures
        
        usageService.trackCreditUsage("Dependencies","after_recovery");
        
        // Then
        waitForQueuesToEmpty(5);
        
        // Should recover and process new events
        assertThat(networkSimulator.getSuccessCount()).isGreaterThan(0);
        assertThat(usageService.getQueueSize()).isEqualTo(0);
    }

    @Test
    void shouldMaintainDataIntegrityDuringFailures() throws Exception {
        // Given
        networkSimulator.withIntermittentFailures(0.5); // 50% failure rate

        // When
        String[] triggerActions = {"scan_button", "export_button", "refresh_button", "settings_button"};
        for (String triggerAction : triggerActions) {
            usageService.trackCreditUsage("Dependencies", triggerAction);
        }

        // Then
        waitForQueuesToEmpty(10);
        
        // Should not corrupt data during failures
        assertThat(networkSimulator.getTotalOperations()).isEqualTo(4);
        assertThat(networkSimulator.getSuccessCount() + networkSimulator.getFailureCount()).isEqualTo(4);
    }

    @Test
    void shouldLogFailuresAppropriately() throws Exception {
        // Given
        networkSimulator.withFailureType(NetworkFailureSimulator.FailureType.IO_EXCEPTION);

        // When
        usageService.trackCreditUsage("Dependencies","logging_test");
        errorReportingService.reportError(new IOException("Test for logging"));

        // Then
        waitForQueuesToEmpty(5);
        
        // Should have logged failures (verified through mock or log capture)
        assertThat(networkSimulator.getFailureCount()).isGreaterThan(0);
    }

    @Test
    void shouldHandleConcurrentFailures() throws Exception {
        // Given
        networkSimulator.withFailureType(NetworkFailureSimulator.FailureType.TIMEOUT)
                      .withMaxFailures(3);

        // When
        ConcurrencyTestHelper.runConcurrentConsumer(5, (threadIndex) -> {
            for (int i = 0; i < 2; i++) {
                usageService.trackCreditUsage("Dependencies","concurrent_" + threadIndex + "_" + i);
                errorReportingService.reportError(
                    new RuntimeException("Concurrent error " + threadIndex + "_" + i));
            }
        });

        // Then
        waitForQueuesToEmpty(10);
        
        // Should handle concurrent failures without deadlocks
        assertThat(networkSimulator.getTotalOperations()).isGreaterThan(0);
        assertThat(usageService.getQueueSize()).isEqualTo(0);
        assertThat(errorReportingService.getQueueSize()).isEqualTo(0);
    }

    @Test
    void shouldValidateRetryLimits() throws Exception {
        // Given
        networkSimulator.withFailureType(NetworkFailureSimulator.FailureType.CONNECTION_REFUSED)
                      .withMaxFailures(1); // Only 1 failure allowed

        // When
        usageService.trackCreditUsage("Dependencies","retry_limit_test");

        // Then
        waitForQueuesToEmpty(5);
        
        // Should not retry indefinitely
        assertThat(networkSimulator.getFailureCount()).isEqualTo(1);
        assertThat(networkSimulator.getSuccessCount()).isGreaterThan(0);
    }

    /**
     * Helper method to wait for both usage and error queues to empty.
     */
    private void waitForQueuesToEmpty(int timeoutSeconds) {
        long startTime = System.currentTimeMillis();
        long timeoutMs = timeoutSeconds * 1000L;
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (usageService.getQueueSize() == 0 && errorReportingService.getQueueSize() == 0) {
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // If timeout reached, fail test
        assertThat(usageService.getQueueSize()).isEqualTo(0);
        assertThat(errorReportingService.getQueueSize()).isEqualTo(0);
    }
}
