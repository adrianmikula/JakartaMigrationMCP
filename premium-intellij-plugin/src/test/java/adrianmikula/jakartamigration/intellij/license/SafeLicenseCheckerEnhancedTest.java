package adrianmikula.jakartamigration.intellij.license;

import adrianmikula.jakartamigration.intellij.config.LicenseFailsafeConfig;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

/**
 * Enhanced tests for SafeLicenseChecker to ensure comprehensive coverage.
 * 
 * These tests cover all edge cases, error scenarios, and performance characteristics
 * of the SafeLicenseChecker implementation.
 */
public class SafeLicenseCheckerEnhancedTest extends BasePlatformTestCase {

    private MockedStatic<LicenseFailsafeConfig> mockedFailsafeConfig;
    private MockedStatic<CheckLicense> mockedCheckLicense;
    
    @Before
    public void setUp() throws Exception {
        super.setUp();
        mockedFailsafeConfig = mockStatic(LicenseFailsafeConfig.class);
        mockedCheckLicense = mockStatic(CheckLicense.class);
        
        // Clear license cache
        SafeLicenseChecker.clearCache();
    }
    
    @After
    public void tearDown() throws Exception {
        if (mockedFailsafeConfig != null) {
            mockedFailsafeConfig.close();
        }
        if (mockedCheckLicense != null) {
            mockedCheckLicense.close();
        }
        SafeLicenseChecker.clearCache();
        super.tearDown();
    }
    
    // ==================== Comprehensive Mode Tests ====================
    
    @Test
    public void testCheckLicenseSafe_AllFailsafeModes() {
        // Test all failsafe modes work correctly
        
        // Dev mode
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isDevMode).thenReturn(true);
        SafeLicenseChecker.LicenseResult devResult = SafeLicenseChecker.checkLicenseSafe();
        assertThat(devResult.isLicensed).isTrue();
        assertThat(devResult.status).isEqualTo("Development Mode");
        
        // Reset
        mockedFailsafeConfig.reset();
        SafeLicenseChecker.clearCache();
        
        // Safe mode
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isDevMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isSafeMode).thenReturn(true);
        SafeLicenseChecker.LicenseResult safeResult = SafeLicenseChecker.checkLicenseSafe();
        assertThat(safeResult.isFallback).isTrue();
        
        // Reset
        mockedFailsafeConfig.reset();
        SafeLicenseChecker.clearCache();
        
        // License disabled
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isDevMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isSafeMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isLicenseDisabled).thenReturn(true);
        SafeLicenseChecker.LicenseResult disabledResult = SafeLicenseChecker.checkLicenseSafe();
        assertThat(disabledResult.isFallback).isTrue();
    }
    
    @Test
    public void testCheckLicenseAsync_AllFailsafeModes() {
        // Test async with all failsafe modes
        
        // Dev mode
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isDevMode).thenReturn(true);
        CompletableFuture<SafeLicenseChecker.LicenseResult> devFuture = SafeLicenseChecker.checkLicenseAsync();
        SafeLicenseChecker.LicenseResult devResult = devFuture.join();
        assertThat(devResult.isLicensed).isTrue();
        assertThat(devResult.status).isEqualTo("Development Mode");
        
        // Reset
        mockedFailsafeConfig.reset();
        SafeLicenseChecker.clearCache();
        
        // Safe mode
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isDevMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isSafeMode).thenReturn(true);
        CompletableFuture<SafeLicenseChecker.LicenseResult> safeFuture = SafeLicenseChecker.checkLicenseAsync();
        SafeLicenseChecker.LicenseResult safeResult = safeFuture.join();
        assertThat(safeResult.isFallback).isTrue();
        
        // Reset
        mockedFailsafeConfig.reset();
        SafeLicenseChecker.clearCache();
        
        // License disabled
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isDevMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isSafeMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isLicenseDisabled).thenReturn(true);
        CompletableFuture<SafeLicenseChecker.LicenseResult> disabledFuture = SafeLicenseChecker.checkLicenseAsync();
        SafeLicenseChecker.LicenseResult disabledResult = disabledFuture.join();
        assertThat(disabledResult.isFallback).isTrue();
    }
    
    // ==================== Timeout and Performance Tests ====================
    
    @Test
    public void testCheckLicenseWithTimeout_CustomTimeout() {
        // Given
        mockedFailsafeConfig.when(LicenseFailsafeConfig::getLicenseTimeoutMs).thenReturn(1000L); // 1 second
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isDevMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isSafeMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isLicenseDisabled).thenReturn(false);
        
        // When
        long startTime = System.currentTimeMillis();
        SafeLicenseChecker.LicenseResult result = SafeLicenseChecker.checkLicenseWithTimeout();
        long endTime = System.currentTimeMillis();
        
        // Then
        assertThat(endTime - startTime).isLessThan(2000); // Should complete within 2 seconds
        assertThat(result).isNotNull();
    }
    
    @Test
    public void testCheckLicenseWithTimeout_VeryShortTimeout() {
        // Given
        mockedFailsafeConfig.when(LicenseFailsafeConfig::getLicenseTimeoutMs).thenReturn(1L); // 1ms
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isDevMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isSafeMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isLicenseDisabled).thenReturn(false);
        
        // When
        SafeLicenseChecker.LicenseResult result = SafeLicenseChecker.checkLicenseWithTimeout();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isFallback).isTrue(); // Should fallback due to timeout
    }
    
    @Test
    public void testCheckLicenseWithTimeout_LongTimeout() {
        // Given
        mockedFailsafeConfig.when(LicenseFailsafeConfig::getLicenseTimeoutMs).thenReturn(10000L); // 10 seconds
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isDevMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isSafeMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isLicenseDisabled).thenReturn(false);
        
        // When
        long startTime = System.currentTimeMillis();
        SafeLicenseChecker.LicenseResult result = SafeLicenseChecker.checkLicenseWithTimeout();
        long endTime = System.currentTimeMillis();
        
        // Then
        assertThat(endTime - startTime).isLessThan(11000); // Should complete within timeout + buffer
        assertThat(result).isNotNull();
    }
    
    // ==================== Cache Behavior Tests ====================
    
    @Test
    public void testCacheBehavior_AfterModeChange() {
        // Given - start with normal mode
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isDevMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isSafeMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isLicenseDisabled).thenReturn(false);
        
        // When - first call
        SafeLicenseChecker.LicenseResult result1 = SafeLicenseChecker.checkLicenseSafe();
        
        // Then - should cache result
        SafeLicenseChecker.LicenseResult result2 = SafeLicenseChecker.checkLicenseSafe();
        assertThat(result1.status).isEqualTo(result2.status);
        
        // When - change to dev mode
        mockedFailsafeConfig.reset();
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isDevMode).thenReturn(true);
        SafeLicenseChecker.clearCache();
        
        // Then - should get new result
        SafeLicenseChecker.LicenseResult result3 = SafeLicenseChecker.checkLicenseSafe();
        assertThat(result3.status).isEqualTo("Development Mode");
        assertThat(result3.status).isNotEqualTo(result1.status);
    }
    
    @Test
    public void testCacheBehavior_Expiration() {
        // This test would require manipulating time or cache duration
        // For now, we test cache clearing behavior
        
        // Given
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isDevMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isSafeMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isLicenseDisabled).thenReturn(false);
        
        // When
        SafeLicenseChecker.checkLicenseSafe(); // Cache result
        SafeLicenseChecker.clearCache(); // Clear cache
        SafeLicenseChecker.checkLicenseSafe(); // Fresh check
        
        // Then - should not throw and should work correctly
        // The important thing is that cache clearing doesn't break anything
    }
    
    // ==================== Error Handling Tests ====================
    
    @Test
    public void testCheckLicenseSafe_WithCheckLicenseException() {
        // Given
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isDevMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isSafeMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isLicenseDisabled).thenReturn(false);
        mockedCheckLicense.when(CheckLicense::isLicensed).thenThrow(new RuntimeException("Test exception"));
        
        // When
        SafeLicenseChecker.LicenseResult result = SafeLicenseChecker.checkLicenseSafe();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isFallback).isTrue();
        assertThat(result.status).isEqualTo("Free");
    }
    
    @Test
    public void testCheckLicenseAsync_WithCheckLicenseException() {
        // Given
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isDevMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isSafeMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isLicenseDisabled).thenReturn(false);
        mockedCheckLicense.when(CheckLicense::isLicensed).thenThrow(new RuntimeException("Test exception"));
        
        // When
        CompletableFuture<SafeLicenseChecker.LicenseResult> future = SafeLicenseChecker.checkLicenseAsync();
        SafeLicenseChecker.LicenseResult result = future.join();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isFallback).isTrue();
        assertThat(result.status).isEqualTo("Free");
    }
    
    @Test
    public void testCheckLicenseWithTimeout_WithCheckLicenseException() {
        // Given
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isDevMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isSafeMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isLicenseDisabled).thenReturn(false);
        mockedCheckLicense.when(CheckLicense::isLicensed).thenThrow(new RuntimeException("Test exception"));
        
        // When
        SafeLicenseChecker.LicenseResult result = SafeLicenseChecker.checkLicenseWithTimeout();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isFallback).isTrue();
        assertThat(result.status).isEqualTo("Free");
    }
    
    // ==================== Trial System Integration Tests ====================
    
    @Test
    public void trialStatus_WithForcedTrial() {
        // Given
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isDevMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isSafeMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isLicenseDisabled).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isTrialForced).thenReturn(true);
        
        // When
        SafeLicenseChecker.LicenseResult result = SafeLicenseChecker.checkLicenseSafe();
        
        // Then
        assertThat(result.isLicensed).isTrue();
        assertThat(result.isFallback).isTrue();
        assertThat(result.status).isEqualTo("Trial (Forced)");
    }
    
    @Test
    public void trialStatus_WithActiveTrial() {
        // Given
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isDevMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isSafeMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isLicenseDisabled).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isTrialForced).thenReturn(false);
        
        // Set up trial
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", 
            String.valueOf(System.currentTimeMillis() + 24 * 60 * 60 * 1000));
        
        try {
            // When
            SafeLicenseChecker.LicenseResult result = SafeLicenseChecker.checkLicenseSafe();
            
            // Then
            assertThat(result.isLicensed).isTrue();
            assertThat(result.isFallback).isTrue();
            assertThat(result.status).isEqualTo("Trial Active");
        } finally {
            System.clearProperty("jakarta.migration.premium");
            System.clearProperty("jakarta.migration.trial.end");
        }
    }
    
    @Test
    public void trialStatus_WithExpiredTrial() {
        // Given
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isDevMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isSafeMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isLicenseDisabled).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isTrialForced).thenReturn(false);
        
        // Set up expired trial
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", 
            String.valueOf(System.currentTimeMillis() - 24 * 60 * 60 * 1000));
        
        try {
            // When
            SafeLicenseChecker.LicenseResult result = SafeLicenseChecker.checkLicenseSafe();
            
            // Then
            assertThat(result.isLicensed).isFalse();
            assertThat(result.isFallback).isTrue();
            assertThat(result.status).isEqualTo("Trial Expired");
        } finally {
            System.clearProperty("jakarta.migration.premium");
            System.clearProperty("jakarta.migration.trial.end");
        }
    }
    
    // ==================== Concurrent Access Tests ====================
    
    @Test
    public void testConcurrentLicenseChecks() throws InterruptedException {
        // Given
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isDevMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isSafeMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isLicenseDisabled).thenReturn(false);
        
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        SafeLicenseChecker.LicenseResult[] results = new SafeLicenseChecker.LicenseResult[threadCount];
        
        // When
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                results[index] = SafeLicenseChecker.checkLicenseSafe();
            });
        }
        
        long startTime = System.currentTimeMillis();
        
        for (Thread thread : threads) {
            thread.start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        long endTime = System.currentTimeMillis();
        
        // Then
        assertThat(endTime - startTime).isLessThan(2000); // Should complete quickly
        
        for (SafeLicenseChecker.LicenseResult result : results) {
            assertThat(result).isNotNull();
            assertThat(result.status).isNotNull();
        }
    }
    
    @Test
    public void testConcurrentAsyncLicenseChecks() throws InterruptedException {
        // Given
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isDevMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isSafeMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isLicenseDisabled).thenReturn(false);
        
        int threadCount = 10;
        CompletableFuture<SafeLicenseChecker.LicenseResult>[] futures = new CompletableFuture[threadCount];
        
        // When
        for (int i = 0; i < threadCount; i++) {
            futures[i] = SafeLicenseChecker.checkLicenseAsync();
        }
        
        long startTime = System.currentTimeMillis();
        
        // Wait for all to complete
        for (CompletableFuture<SafeLicenseChecker.LicenseResult> future : futures) {
            future.get(5, TimeUnit.SECONDS);
        }
        
        long endTime = System.currentTimeMillis();
        
        // Then
        assertThat(endTime - startTime).isLessThan(5000); // Should complete quickly
        
        for (CompletableFuture<SafeLicenseChecker.LicenseResult> future : futures) {
            SafeLicenseChecker.LicenseResult result = future.join();
            assertThat(result).isNotNull();
            assertThat(result.status).isNotNull();
        }
    }
    
    // ==================== Memory and Resource Tests ====================
    
    @Test
    public void testMemoryUsage_WithManyCalls() {
        // Given
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isDevMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isSafeMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isLicenseDisabled).thenReturn(false);
        
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // When - make many calls
        for (int i = 0; i < 1000; i++) {
            SafeLicenseChecker.checkLicenseSafe();
        }
        
        // Force garbage collection
        System.gc();
        
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = finalMemory - initialMemory;
        
        // Then - should not use excessive memory
        assertThat(memoryIncrease).isLessThan(5 * 1024 * 1024); // Less than 5MB
    }
    
    @Test
    public void testResourceCleanup() {
        // Given
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isDevMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isSafeMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isLicenseDisabled).thenReturn(false);
        
        // When
        SafeLicenseChecker.checkLicenseSafe();
        SafeLicenseChecker.clearCache();
        
        // Then - should be able to create new instances without issues
        SafeLicenseChecker.LicenseResult result = SafeLicenseChecker.checkLicenseSafe();
        assertThat(result).isNotNull();
    }
    
    // ==================== Edge Case Tests ====================
    
    @Test
    public void testCheckLicenseSafe_WithNullCheckLicenseResult() {
        // Given
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isDevMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isSafeMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isLicenseDisabled).thenReturn(false);
        mockedCheckLicense.when(CheckLicense::isLicensed).thenReturn(null);
        
        // When
        SafeLicenseChecker.LicenseResult result = SafeLicenseChecker.checkLicenseSafe();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isCertain).isFalse();
        assertThat(result.status).isEqualTo("Checking...");
    }
    
    @Test
    public void testIsPremiumAvailable_WithVariousStates() {
        // Test isPremiumAvailable with different license states
        
        // Given - licensed
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isDevMode).thenReturn(true);
        boolean premium1 = SafeLicenseChecker.isPremiumAvailable();
        assertThat(premium1).isTrue();
        
        // Reset
        mockedFailsafeConfig.reset();
        SafeLicenseChecker.clearCache();
        
        // Given - fallback licensed
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isDevMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isSafeMode).thenReturn(true);
        boolean premium2 = SafeLicenseChecker.isPremiumAvailable();
        assertThat(premium2).isTrue(); // Fallback should still provide premium
        
        // Reset
        mockedFailsafeConfig.reset();
        SafeLicenseChecker.clearCache();
        
        // Given - not licensed
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isDevMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isSafeMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isLicenseDisabled).thenReturn(false);
        mockedCheckLicense.when(CheckLicense::isLicensed).thenReturn(false);
        boolean premium3 = SafeLicenseChecker.isPremiumAvailable();
        assertThat(premium3).isFalse();
    }
}
