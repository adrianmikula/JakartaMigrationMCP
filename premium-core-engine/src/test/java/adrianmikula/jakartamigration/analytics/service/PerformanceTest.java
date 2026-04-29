package adrianmikula.jakartamigration.analytics.service;

import adrianmikula.jakartamigration.analytics.config.SupabaseConfig;
import adrianmikula.jakartamigration.analytics.util.ConcurrencyTestHelper;
import adrianmikula.jakartamigration.analytics.util.TestDataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Performance and load testing for analytics services.
 * Tests throughput, memory usage, and scalability under load.
 */
@Tag("slow")
@ExtendWith(MockitoExtension.class)
class PerformanceTest {

    @TempDir
    Path tempDir;

    @Mock
    private SupabaseConfig supabaseConfig;

    private UserIdentificationService userIdentificationService;
    private UsageService usageService;
    private ErrorReportingService errorReportingService;
    private MemoryMXBean memoryBean;

    @BeforeEach
    void setUp() {
        when(supabaseConfig.getSupabaseUrl()).thenReturn("https://test-wmngdqhgybiomoxjfxpc.supabase.co");
        when(supabaseConfig.getSupabaseAnonKey()).thenReturn("test-anon-key");
        when(supabaseConfig.isAnalyticsEnabled()).thenReturn(true);
        when(supabaseConfig.isErrorReportingEnabled()).thenReturn(true);
        when(supabaseConfig.getAnalyticsBatchSize()).thenReturn(10);
        when(supabaseConfig.getAnalyticsFlushIntervalSeconds()).thenReturn(1);
        when(supabaseConfig.isConfigured()).thenReturn(true);

        userIdentificationService = new UserIdentificationService(tempDir, supabaseConfig);
        usageService = new UsageService(userIdentificationService);
        errorReportingService = new ErrorReportingService(userIdentificationService);
        memoryBean = ManagementFactory.getMemoryMXBean();
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
    void shouldProcessSingleEventQuickly() {
        // Given
        long startTime = System.currentTimeMillis();

        // When
        usageService.trackCreditUsage("basic_scan", "performance_test");

        // Then
        long processingTime = System.currentTimeMillis() - startTime;
        assertThat(processingTime).isLessThan(100); // Should process in <100ms
        
        waitForQueuesToEmpty(5);
    }

    @Test
    void shouldHandleModerateLoad() throws Exception {
        // Given
        int eventCount = 100;

        // When
        ConcurrencyTestHelper.PerformanceMetrics metrics = 
            ConcurrencyTestHelper.runWithMetrics(5, eventCount, () -> {
                usageService.trackCreditUsage("moderate_load_test", "performance_test");
                return "completed";
            });

        // Then
        metrics.logMetrics();
        assertThat(metrics.getTotalOperations()).isEqualTo(eventCount);
        assertThat(metrics.getSuccessRate()).isGreaterThan(0.95); // 95%+ success
        assertThat(metrics.getOperationsPerSecond()).isGreaterThan(20); // 20+ ops/sec
        assertThat(metrics.getAverageExecutionTime()).isLessThan(50.0); // <50ms avg
    }

    @Test
    void shouldHandleHighLoad() throws Exception {
        // Given
        int eventCount = 1000;

        // When
        ConcurrencyTestHelper.PerformanceMetrics metrics = 
            ConcurrencyTestHelper.runWithMetrics(10, eventCount, () -> {
                usageService.trackCreditUsage("high_load_test", "performance_test");
                return "completed";
            });

        // Then
        metrics.logMetrics();
        assertThat(metrics.getTotalOperations()).isEqualTo(eventCount);
        assertThat(metrics.getSuccessRate()).isGreaterThan(0.90); // 90%+ success
        assertThat(metrics.getOperationsPerSecond()).isGreaterThan(50); // 50+ ops/sec
        assertThat(metrics.getAverageExecutionTime()).isLessThan(100.0); // <100ms avg
        
        waitForQueuesToEmpty(15);
    }

    @Test
    void shouldHandleExtremeLoad() throws Exception {
        // Given
        int eventCount = 10000;

        // When
        ConcurrencyTestHelper.PerformanceMetrics metrics = 
            ConcurrencyTestHelper.runWithMetrics(20, eventCount, () -> {
                usageService.trackCreditUsage("extreme_load_test", "performance_test");
                return "completed";
            });

        // Then
        metrics.logMetrics();
        assertThat(metrics.getTotalOperations()).isEqualTo(eventCount);
        assertThat(metrics.getSuccessRate()).isGreaterThan(0.85); // 85%+ success under extreme load
        assertThat(metrics.getOperationsPerSecond()).isGreaterThan(100); // 100+ ops/sec
        
        waitForQueuesToEmpty(30);
    }

    @Test
    void shouldMaintainMemoryEfficiencyUnderLoad() throws Exception {
        // Given
        long initialMemory = memoryBean.getHeapMemoryUsage().getUsed();
        int eventCount = 5000;

        // When
        ConcurrencyTestHelper.runWithMetrics(15, eventCount, () -> {
            usageService.trackCreditUsage("memory_efficiency_test", "performance_test");
            errorReportingService.reportError(new RuntimeException("Memory test error"));
            return "completed";
        });

        long finalMemory = memoryBean.getHeapMemoryUsage().getUsed();
        long memoryIncrease = finalMemory - initialMemory;

        // Then
        // Memory increase should be reasonable (<50MB for 5000 events)
        assertThat(memoryIncrease).isLessThan(50 * 1024 * 1024L);
        
        waitForQueuesToEmpty(20);
    }

    @Test
    void shouldHandleBurstLoad() throws Exception {
        // Given
        int burstSize = 1000;
        int burstCount = 5;

        // When
        ConcurrencyTestHelper.PerformanceMetrics metrics = 
            ConcurrencyTestHelper.runWithMetrics(1, burstSize, () -> {
                for (int i = 0; i < burstSize; i++) {
                    usageService.trackCreditUsage("burst_test_" + i, "performance_test");
                }
                return "completed";
            });

        // Then
        metrics.logMetrics();
        assertThat(metrics.getTotalOperations()).isEqualTo(burstSize);
        assertThat(metrics.getSuccessRate()).isEqualTo(1.0); // 100% success for single thread
        assertThat(metrics.getOperationsPerSecond()).isGreaterThan(500); // 500+ ops/sec for burst
        
        waitForQueuesToEmpty(10);
    }

    @Test
    void shouldHandleSustainedLoad() throws Exception {
        // Given
        int durationSeconds = 10;
        int targetOpsPerSecond = 100;

        // When
        long startTime = System.currentTimeMillis();
        ConcurrencyTestHelper.PerformanceMetrics metrics = 
            ConcurrencyTestHelper.runWithMetrics(5, durationSeconds * targetOpsPerSecond, () -> {
                usageService.trackCreditUsage("sustained_load_test", "performance_test");
                return "completed";
            });
        long endTime = System.currentTimeMillis();

        // Then
        metrics.logMetrics();
        long actualDuration = (endTime - startTime) / 1000;
        assertThat(actualDuration).isGreaterThanOrEqualTo(durationSeconds - 1); // Allow 1s variance
        assertThat(metrics.getOperationsPerSecond()).isBetween(80.0, 120.0); // Target range
        
        waitForQueuesToEmpty(15);
    }

    @Test
    void shouldHandleMixedLoadTypes() throws Exception {
        // Given
        int usageEvents = 2000;
        int errorReports = 500;
        int upgradeClicks = 300;

        // When
        ConcurrencyTestHelper.PerformanceMetrics usageMetrics = 
            ConcurrencyTestHelper.runWithMetrics(8, usageEvents, () -> {
                usageService.trackCreditUsage("mixed_load_test", "performance_test");
                return "completed";
            });

        ConcurrencyTestHelper.PerformanceMetrics errorMetrics = 
            ConcurrencyTestHelper.runWithMetrics(4, errorReports, () -> {
                errorReportingService.reportError(new RuntimeException("Mixed load error"));
                return "completed";
            });

        ConcurrencyTestHelper.PerformanceMetrics upgradeMetrics = 
            ConcurrencyTestHelper.runWithMetrics(2, upgradeClicks, () -> {
                usageService.trackUpgradeClick("mixed_load_test", "performance_test");
                return "completed";
            });

        // Then
        usageMetrics.logMetrics();
        errorMetrics.logMetrics();
        upgradeMetrics.logMetrics();
        
        // All should meet performance targets
        assertThat(usageMetrics.getOperationsPerSecond()).isGreaterThan(100);
        assertThat(errorMetrics.getOperationsPerSecond()).isGreaterThan(50);
        assertThat(upgradeMetrics.getOperationsPerSecond()).isGreaterThan(25);
        
        waitForQueuesToEmpty(15);
    }

    @Test
    void shouldScaleWithIncreasingLoad() throws Exception {
        // Given
        int[] loadLevels = {100, 500, 1000, 2000, 5000};
        double[] minThroughput = new double[loadLevels.length];

        // When
        for (int i = 0; i < loadLevels.length; i++) {
            int loadLevel = loadLevels[i];
            final int index = i; // Make effectively final for lambda
            
            ConcurrencyTestHelper.PerformanceMetrics metrics = 
                ConcurrencyTestHelper.runWithMetrics(5, loadLevel, () -> {
                    usageService.trackCreditUsage("scaling_test_" + index, "performance_test");
                    return "completed";
                });
            
            minThroughput[i] = metrics.getOperationsPerSecond();
            
            // Allow processing between load levels
            waitForQueuesToEmpty(5);
        }

        // Then
        // Throughput should generally increase with load (up to saturation point)
        for (int i = 1; i < minThroughput.length; i++) {
            assertThat(minThroughput[i]).isGreaterThanOrEqualTo(minThroughput[i-1] * 0.8);
        }
    }

    @Test
    void shouldHandleMemoryPressure() throws Exception {
        // Given
        long initialMemory = memoryBean.getHeapMemoryUsage().getUsed();
        int memoryPressureEvents = 10000;

        // When
        // Create memory pressure with large events
        ConcurrencyTestHelper.runWithMetrics(10, memoryPressureEvents, () -> {
            // Create large error messages to increase memory usage
            String largeMessage = "x".repeat(1000);
            errorReportingService.reportError(new RuntimeException(largeMessage));
            usageService.trackCreditUsage("memory_pressure_test", "performance_test");
            return "completed";
        });

        long peakMemory = memoryBean.getHeapMemoryUsage().getUsed();
        long memoryIncrease = peakMemory - initialMemory;

        // Then
        // Should handle memory pressure without crashing
        assertThat(memoryIncrease).isLessThan(200 * 1024 * 1024L); // <200MB increase
        
        waitForQueuesToEmpty(20);
    }

    @Test
    void shouldMaintainConsistentPerformance() throws Exception {
        // Given
        int rounds = 5;
        int eventsPerRound = 500;
        double[] throughputValues = new double[rounds];

        // When
        for (int round = 0; round < rounds; round++) {
            final int roundIndex = round; // Make effectively final for lambda
            ConcurrencyTestHelper.PerformanceMetrics metrics = 
                ConcurrencyTestHelper.runWithMetrics(5, eventsPerRound, () -> {
                    usageService.trackCreditUsage("consistency_test_round_" + roundIndex, "performance_test");
                    return "completed";
                });
            
            throughputValues[round] = metrics.getOperationsPerSecond();
            
            // Allow processing between rounds
            waitForQueuesToEmpty(5);
        }

        // Then
        // Performance should be consistent (within 20% variance)
        double averageThroughput = java.util.Arrays.stream(throughputValues)
            .average()
            .orElse(0.0);
        
        for (double throughput : throughputValues) {
            double variance = Math.abs(throughput - averageThroughput) / averageThroughput;
            assertThat(variance).isLessThan(0.2); // Within 20% of average
        }
    }

    @Test
    void shouldHandleQueueSaturation() throws Exception {
        // Given
        int saturationEvents = 50000; // Very high load to test queue limits

        // When
        long startTime = System.currentTimeMillis();
        ConcurrencyTestHelper.PerformanceMetrics metrics = 
            ConcurrencyTestHelper.runWithMetrics(20, saturationEvents, () -> {
                usageService.trackCreditUsage("saturation_test", "performance_test");
                return "completed";
            });
        long endTime = System.currentTimeMillis();

        // Then
        metrics.logMetrics();
        
        // Should complete without infinite queue growth
        long processingTime = endTime - startTime;
        assertThat(processingTime).isLessThan(60000); // <60 seconds
        
        // Success rate should remain reasonable even under saturation
        assertThat(metrics.getSuccessRate()).isGreaterThan(0.80); // 80%+ success
        
        waitForQueuesToEmpty(30);
    }

    @Test
    void shouldHandleConcurrentPerformance() throws Exception {
        // Given
        int threadCount = 15;
        int eventsPerThread = 200;

        // When
        ConcurrencyTestHelper.PerformanceMetrics metrics = 
            ConcurrencyTestHelper.runWithMetrics(threadCount, eventsPerThread, () -> {
                usageService.trackCreditUsage("concurrent_performance_test", "performance_test");
                return "completed";
            });

        // Then
        metrics.logMetrics();
        
        // Should maintain performance under concurrency
        assertThat(metrics.getTotalOperations()).isEqualTo(threadCount * eventsPerThread);
        assertThat(metrics.getSuccessRate()).isGreaterThan(0.90); // 90%+ success
        assertThat(metrics.getOperationsPerSecond()).isGreaterThan(200); // 200+ ops/sec total
        assertThat(metrics.getAverageExecutionTime()).isLessThan(50.0); // <50ms avg
        
        waitForQueuesToEmpty(20);
    }

    @Test
    void shouldMeasureResourceCleanup() throws Exception {
        // Given
        long initialMemory = memoryBean.getHeapMemoryUsage().getUsed();
        int cleanupEvents = 1000;

        // When
        // Create and close multiple service instances
        for (int i = 0; i < 10; i++) {
            UserIdentificationService tempUserIdService = 
                new UserIdentificationService(tempDir, supabaseConfig);
            UsageService tempUsageService = new UsageService(tempUserIdService);
            ErrorReportingService tempErrorService = new ErrorReportingService(tempUserIdService);
            
            // Add some events
            for (int j = 0; j < cleanupEvents / 10; j++) {
                tempUsageService.trackCreditUsage("cleanup_test_" + i + "_" + j, "performance_test");
                tempErrorService.reportError(new RuntimeException("Cleanup error " + i + "_" + j));
            }
            
            // Close services
            tempUsageService.close();
            tempErrorService.close();
            tempUserIdService.close();
        }

        // Force garbage collection
        System.gc();
        Thread.sleep(1000);
        
        long finalMemory = memoryBean.getHeapMemoryUsage().getUsed();

        // Then
        // Memory should be cleaned up properly
        long memoryIncrease = finalMemory - initialMemory;
        assertThat(memoryIncrease).isLessThan(100 * 1024 * 1024L); // <100MB for all instances
    }

    @Test
    void shouldHandlePerformanceDegradationGracefully() throws Exception {
        // Given
        int normalEvents = 1000;
        int degradedEvents = 1000;

        // Measure normal performance
        ConcurrencyTestHelper.PerformanceMetrics normalMetrics = 
            ConcurrencyTestHelper.runWithMetrics(5, normalEvents, () -> {
                usageService.trackCreditUsage("normal_performance_test", "performance_test");
                return "completed";
            });
        
        waitForQueuesToEmpty(5);

        // Simulate degraded conditions (e.g., by adding delay)
        when(supabaseConfig.getAnalyticsFlushIntervalSeconds()).thenReturn(10); // Slower processing
        
        // Measure degraded performance
        ConcurrencyTestHelper.PerformanceMetrics degradedMetrics = 
            ConcurrencyTestHelper.runWithMetrics(5, degradedEvents, () -> {
                usageService.trackCreditUsage("degraded_performance_test", "performance_test");
                return "completed";
            });

        // Then
        normalMetrics.logMetrics();
        degradedMetrics.logMetrics();
        
        // Should still complete successfully, though slower
        assertThat(normalMetrics.getSuccessRate()).isEqualTo(1.0);
        assertThat(degradedMetrics.getSuccessRate()).isEqualTo(1.0);
        
        // Degraded performance should be slower but still reasonable
        assertThat(degradedMetrics.getOperationsPerSecond())
            .isLessThan(normalMetrics.getOperationsPerSecond());
        assertThat(degradedMetrics.getOperationsPerSecond())
            .isGreaterThan(10); // Still >10 ops/sec
        
        waitForQueuesToEmpty(20);
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
