package adrianmikula.jakartamigration.analytics.service;

import adrianmikula.jakartamigration.analytics.config.SupabaseConfig;
import adrianmikula.jakartamigration.analytics.util.ConcurrencyTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests for concurrent access safety in analytics services.
 * Tests thread safety, race conditions, and data consistency.
 */
@ExtendWith(MockitoExtension.class)
class ConcurrentAccessTest {

    @TempDir
    Path tempDir;

    @Mock
    private SupabaseConfig supabaseConfig;

    private UserIdentificationService userIdentificationService;
    private UsageService usageService;
    private ErrorReportingService errorReportingService;

    @BeforeEach
    void setUp() {
        when(supabaseConfig.getSupabaseUrl()).thenReturn("https://test-wmngdqhgybiomoxjfxpc.supabase.co");
        when(supabaseConfig.getSupabaseAnonKey()).thenReturn("test-anon-key");
        when(supabaseConfig.isAnalyticsEnabled()).thenReturn(true);
        when(supabaseConfig.isErrorReportingEnabled()).thenReturn(true);
        when(supabaseConfig.getAnalyticsBatchSize()).thenReturn(5);
        when(supabaseConfig.getAnalyticsFlushIntervalSeconds()).thenReturn(1);
        when(supabaseConfig.isConfigured()).thenReturn(true);

        userIdentificationService = new UserIdentificationService(tempDir, supabaseConfig);
        usageService = new UsageService(userIdentificationService);
        errorReportingService = new ErrorReportingService(userIdentificationService);
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
    }

    @Test
    void shouldHandleConcurrentUsageTracking() throws Exception {
        // Given
        int threadCount = 10;
        int eventsPerThread = 20;
        AtomicInteger totalTracked = new AtomicInteger(0);

        // When
        List<ConcurrencyTestHelper.ConcurrentResult<Void>> results = 
            ConcurrencyTestHelper.runConcurrentConsumer(threadCount, (threadIndex) -> {
                for (int i = 0; i < eventsPerThread; i++) {
                    usageService.trackCreditUsage("scan_" + threadIndex + "_" + i);
                    totalTracked.incrementAndGet();
                }
            });

        // Then
        // All threads should complete successfully
        assertThat(results).hasSize(threadCount);
        assertThat(results.stream().filter(ConcurrencyTestHelper.ConcurrentResult::hasError)).isEmpty();
        assertThat(totalTracked.get()).isEqualTo(threadCount * eventsPerThread);
        
        // Wait for processing
        waitForQueuesToEmpty(10);
    }

    @Test
    void shouldHandleConcurrentErrorReporting() throws Exception {
        // Given
        int threadCount = 8;
        int errorsPerThread = 15;
        AtomicInteger totalReported = new AtomicInteger(0);

        // When
        List<ConcurrencyTestHelper.ConcurrentResult<Void>> results = 
            ConcurrencyTestHelper.runConcurrentConsumer(threadCount, (threadIndex) -> {
                for (int i = 0; i < errorsPerThread; i++) {
                    RuntimeException error = new RuntimeException("Error_" + threadIndex + "_" + i);
                    errorReportingService.reportError(error);
                    totalReported.incrementAndGet();
                }
            });

        // Then
        assertThat(results).hasSize(threadCount);
        assertThat(results.stream().filter(ConcurrencyTestHelper.ConcurrentResult::hasError)).isEmpty();
        assertThat(totalReported.get()).isEqualTo(threadCount * errorsPerThread);
        
        // Wait for processing
        waitForQueuesToEmpty(10);
    }

    @Test
    void shouldHandleMixedConcurrentOperations() throws Exception {
        // Given
        int threadCount = 12;
        AtomicInteger usageCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // When
        List<ConcurrencyTestHelper.ConcurrentResult<Void>> results = 
            ConcurrencyTestHelper.runConcurrentConsumer(threadCount, (threadIndex) -> {
                for (int i = 0; i < 10; i++) {
                    if (i % 2 == 0) {
                        usageService.trackCreditUsage("mixed_" + threadIndex + "_" + i);
                        usageCount.incrementAndGet();
                    } else {
                        errorReportingService.reportError(
                            new RuntimeException("Mixed error " + threadIndex + "_" + i));
                        errorCount.incrementAndGet();
                    }
                }
            });

        // Then
        assertThat(results).hasSize(threadCount);
        assertThat(results.stream().filter(ConcurrencyTestHelper.ConcurrentResult::hasError)).isEmpty();
        assertThat(usageCount.get() + errorCount.get()).isEqualTo(threadCount * 10);
        
        // Wait for processing
        waitForQueuesToEmpty(15);
    }

    @Test
    void shouldPreventRaceConditionsInUserIdGeneration() throws Exception {
        // Given
        int threadCount = 20;
        ConcurrentHashMap<String, String> userIds = new ConcurrentHashMap<>();

        // When
        List<ConcurrencyTestHelper.ConcurrentResult<String>> results = 
            ConcurrencyTestHelper.runConcurrent(threadCount, () -> {
                UserIdentificationService service = 
                    new UserIdentificationService(tempDir, supabaseConfig);
                String userId = service.getAnonymousUserId();
                userIds.put(Thread.currentThread().getName(), userId);
                service.close();
                return userId;
            });

        // Then
        assertThat(results).hasSize(threadCount);
        assertThat(results.stream().filter(ConcurrencyTestHelper.ConcurrentResult::hasError)).isEmpty();
        
        // All user IDs should be the same (no race condition)
        List<String> uniqueUserIds = userIds.values().stream().distinct().toList();
        assertThat(uniqueUserIds).hasSize(1);
    }

    @Test
    void shouldHandleConcurrentServiceCreation() throws Exception {
        // Given
        int threadCount = 15;

        // When
        List<ConcurrencyTestHelper.ConcurrentResult<UsageService>> results = 
            ConcurrencyTestHelper.runConcurrent(threadCount, () -> {
                UserIdentificationService service = 
                    new UserIdentificationService(tempDir, supabaseConfig);
                UsageService usageService = new UsageService(service);
                usageService.trackCreditUsage("concurrent_creation_test");
                usageService.close();
                service.close();
                return usageService;
            });

        // Then
        assertThat(results).hasSize(threadCount);
        assertThat(results.stream().filter(ConcurrencyTestHelper.ConcurrentResult::hasError)).isEmpty();
    }

    @Test
    void shouldMaintainQueueThreadSafety() throws Exception {
        // Given
        int threadCount = 10;
        AtomicInteger producers = new AtomicInteger(0);
        AtomicInteger consumers = new AtomicInteger(0);

        // When
        ConcurrencyTestHelper.runConcurrentConsumer(threadCount, (threadIndex) -> {
            if (threadIndex % 2 == 0) {
                // Producer threads
                for (int i = 0; i < 50; i++) {
                    usageService.trackCreditUsage("queue_test_" + threadIndex + "_" + i);
                    producers.incrementAndGet();
                }
            } else {
                // Consumer threads (just check queue size)
                for (int i = 0; i < 100; i++) {
                    usageService.getQueueSize();
                    consumers.incrementAndGet();
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        });

        // Then
        // Should not cause deadlocks or corruption
        assertThat(producers.get()).isGreaterThan(0);
        assertThat(consumers.get()).isGreaterThan(0);
        
        // Wait for processing
        waitForQueuesToEmpty(10);
    }

    @Test
    void shouldHandleConcurrentFlushOperations() throws Exception {
        // Given
        int threadCount = 8;
        
        // First, add some events
        for (int i = 0; i < 20; i++) {
            usageService.trackCreditUsage("flush_test_" + i);
        }

        // When
        List<ConcurrencyTestHelper.ConcurrentResult<Void>> results = 
            ConcurrencyTestHelper.runConcurrentConsumer(threadCount, (threadIndex) -> {
                for (int i = 0; i < 5; i++) {
                    usageService.flush();
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });

        // Then
        assertThat(results).hasSize(threadCount);
        assertThat(results.stream().filter(ConcurrencyTestHelper.ConcurrentResult::hasError)).isEmpty();
        
        // Queue should be empty after all flushes
        waitForQueuesToEmpty(5);
    }

    @Test
    void shouldHandleConcurrentServiceShutdown() throws Exception {
        // Given
        int threadCount = 6;
        AtomicInteger shutdownCount = new AtomicInteger(0);

        // When
        List<ConcurrencyTestHelper.ConcurrentResult<Void>> results = 
            ConcurrencyTestHelper.runConcurrentConsumer(threadCount, (threadIndex) -> {
                UsageService service = new UsageService(userIdentificationService);
                service.trackCreditUsage("shutdown_test_" + threadIndex);
                service.close();
                shutdownCount.incrementAndGet();
            });

        // Then
        assertThat(results).hasSize(threadCount);
        assertThat(results.stream().filter(ConcurrencyTestHelper.ConcurrentResult::hasError)).isEmpty();
        assertThat(shutdownCount.get()).isEqualTo(threadCount);
    }

    @Test
    void shouldHandleHighConcurrencyLoad() throws Exception {
        // Given
        int threadCount = 25;
        int operationsPerThread = 100;
        ConcurrencyTestHelper.PerformanceMetrics metrics = 
            ConcurrencyTestHelper.runWithMetrics(threadCount, operationsPerThread, () -> {
                usageService.trackCreditUsage("load_test");
                return "completed";
            });

        // Then
        metrics.logMetrics();
        assertThat(metrics.getTotalThreads()).isEqualTo(threadCount);
        assertThat(metrics.getTotalOperations()).isEqualTo(threadCount * operationsPerThread);
        assertThat(metrics.getSuccessRate()).isGreaterThan(0.95); // 95%+ success rate
        assertThat(metrics.getOperationsPerSecond()).isGreaterThan(50); // Performance target
        
        // Wait for processing
        waitForQueuesToEmpty(20);
    }

    @Test
    void shouldPreventDataCorruptionUnderLoad() throws Exception {
        // Given
        int threadCount = 16;
        ConcurrentHashMap<String, Integer> operationCounts = new ConcurrentHashMap<>();

        // When
        List<ConcurrencyTestHelper.ConcurrentResult<Void>> results = 
            ConcurrencyTestHelper.runConcurrentConsumer(threadCount, (threadIndex) -> {
                int count = 0;
                for (int i = 0; i < 25; i++) {
                    usageService.trackCreditUsage("corruption_test_" + threadIndex + "_" + i);
                    count++;
                }
                operationCounts.put(Thread.currentThread().getName(), count);
            });

        // Then
        assertThat(results).hasSize(threadCount);
        assertThat(results.stream().filter(ConcurrencyTestHelper.ConcurrentResult::hasError)).isEmpty();
        
        // Verify total operations
        int totalExpected = threadCount * 25;
        int totalActual = operationCounts.values().stream().mapToInt(Integer::intValue).sum();
        assertThat(totalActual).isEqualTo(totalExpected);
        
        // Wait for processing
        waitForQueuesToEmpty(15);
    }

    @Test
    void shouldHandleConcurrentTabUpdates() throws Exception {
        // Given
        int threadCount = 12;
        String[] tabs = {"Dependencies", "Advanced Scans", "Migration Strategy", "Platforms", "Support", "AI"};
        ConcurrentHashMap<String, String> tabUpdates = new ConcurrentHashMap<>();

        // When
        List<ConcurrencyTestHelper.ConcurrentResult<Void>> results = 
            ConcurrencyTestHelper.runConcurrentConsumer(threadCount, (threadIndex) -> {
                for (int i = 0; i < 10; i++) {
                    String tab = tabs[threadIndex % tabs.length];
                    errorReportingService.setCurrentTab(tab);
                    tabUpdates.put(Thread.currentThread().getName() + "_" + i, tab);
                }
            });

        // Then
        assertThat(results).hasSize(threadCount);
        assertThat(results.stream().filter(ConcurrencyTestHelper.ConcurrentResult::hasError)).isEmpty();
        assertThat(tabUpdates).hasSize(threadCount * 10);
    }

    @Test
    void shouldMaintainConsistentUserAcrossServices() throws Exception {
        // Given
        int threadCount = 8;
        ConcurrentHashMap<String, String> userIds = new ConcurrentHashMap<>();

        // When
        List<ConcurrencyTestHelper.ConcurrentResult<Void>> results = 
            ConcurrencyTestHelper.runConcurrentConsumer(threadCount, (threadIndex) -> {
                // Create both services and verify they use same user ID
                UserIdentificationService service = 
                    new UserIdentificationService(tempDir, supabaseConfig);
                UsageService usageService = new UsageService(service);
                ErrorReportingService errorService = new ErrorReportingService(service);
                
                String usageUserId = service.getAnonymousUserId();
                String errorUserId = service.getAnonymousUserId();
                
                // Both should return same user ID
                assertThat(usageUserId).isEqualTo(errorUserId);
                userIds.put(Thread.currentThread().getName(), usageUserId);
                
                usageService.close();
                errorService.close();
                service.close();
            });

        // Then
        assertThat(results).hasSize(threadCount);
        assertThat(results.stream().filter(ConcurrencyTestHelper.ConcurrentResult::hasError)).isEmpty();
        
        // All threads should get same user ID
        List<String> uniqueUserIds = userIds.values().stream().distinct().toList();
        assertThat(uniqueUserIds).hasSize(1);
    }

    @Test
    void shouldHandleMemoryPressureUnderConcurrency() throws Exception {
        // Given
        int threadCount = 20;
        int largeEventCount = 1000;

        // When
        long startTime = System.currentTimeMillis();
        List<ConcurrencyTestHelper.ConcurrentResult<Void>> results = 
            ConcurrencyTestHelper.runConcurrentConsumer(threadCount, (threadIndex) -> {
                for (int i = 0; i < largeEventCount; i++) {
                    usageService.trackCreditUsage("memory_test_" + threadIndex + "_" + i);
                    errorReportingService.reportError(
                        new RuntimeException("Memory test " + threadIndex + "_" + i));
                }
            });
        long endTime = System.currentTimeMillis();

        // Then
        assertThat(results).hasSize(threadCount);
        assertThat(results.stream().filter(ConcurrencyTestHelper.ConcurrentResult::hasError)).isEmpty();
        
        // Should complete within reasonable time
        long executionTime = endTime - startTime;
        assertThat(executionTime).isLessThan(30000); // 30 seconds max
        
        // Wait for processing (may take longer due to volume)
        waitForQueuesToEmpty(30);
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
