package adrianmikula.jakartamigration.intellij.license;

import adrianmikula.jakartamigration.intellij.config.LicenseFailsafeConfig;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import static org.junit.Assert.*;

/**
 * Tests for SafeLicenseChecker to ensure it never blocks IDE startup.
 * 
 * These tests verify that the licensing system is safe and won't lock up the IDE
 * under any circumstances.
 */
public class SafeLicenseCheckerTest extends BasePlatformTestCase {
    
    private String originalDevMode;
    private String originalSafeMode;
    private String originalLicenseDisabled;
    private String originalPremium;
    private String originalTrialEnd;
    
    @Before
    public void setUp() throws Exception {
        super.setUp();
        
        // Save original system properties
        originalDevMode = System.getProperty("jakarta.migration.dev");
        originalSafeMode = System.getProperty("jakarta.migration.safe");
        originalLicenseDisabled = System.getProperty("jakarta.migration.license.disable");
        originalPremium = System.getProperty("jakarta.migration.premium");
        originalTrialEnd = System.getProperty("jakarta.migration.trial.end");
        
        // Clear any existing license cache
        SafeLicenseChecker.clearCache();
    }
    
    @After
    public void tearDown() throws Exception {
        // Restore original system properties
        if (originalDevMode != null) {
            System.setProperty("jakarta.migration.dev", originalDevMode);
        } else {
            System.clearProperty("jakarta.migration.dev");
        }
        
        if (originalSafeMode != null) {
            System.setProperty("jakarta.migration.safe", originalSafeMode);
        } else {
            System.clearProperty("jakarta.migration.safe");
        }
        
        if (originalLicenseDisabled != null) {
            System.setProperty("jakarta.migration.license.disable", originalLicenseDisabled);
        } else {
            System.clearProperty("jakarta.migration.license.disable");
        }
        
        if (originalPremium != null) {
            System.setProperty("jakarta.migration.premium", originalPremium);
        } else {
            System.clearProperty("jakarta.migration.premium");
        }
        
        if (originalTrialEnd != null) {
            System.setProperty("jakarta.migration.trial.end", originalTrialEnd);
        } else {
            System.clearProperty("jakarta.migration.trial.end");
        }
        
        // Clear license cache
        SafeLicenseChecker.clearCache();
        
        super.tearDown();
    }
    
    @Test
    public void testCheckLicenseSafe_NeverBlocks() {
        // This test verifies that checkLicenseSafe never blocks
        // and always returns a result quickly
        
        long startTime = System.currentTimeMillis();
        SafeLicenseChecker.LicenseResult result = SafeLicenseChecker.checkLicenseSafe();
        long endTime = System.currentTimeMillis();
        
        // Should complete very quickly (under 1 second)
        assertTrue("License check should complete quickly", (endTime - startTime) < 1000);
        
        // Should always return a non-null result
        assertNotNull("License result should never be null", result);
        
        // Should have a valid status
        assertNotNull("License status should never be null", result.status);
        assertFalse("License status should not be empty", result.status.trim().isEmpty());
    }
    
    @Test
    public void testCheckLicenseSafe_DevModeBypass() {
        // Enable dev mode
        System.setProperty("jakarta.migration.dev", "true");
        
        SafeLicenseChecker.LicenseResult result = SafeLicenseChecker.checkLicenseSafe();
        
        assertTrue("Should be licensed in dev mode", result.isLicensed);
        assertEquals("Should show dev mode status", "Development Mode", result.status);
        assertTrue("Should be certain in dev mode", result.isCertain);
        assertFalse("Should not be fallback in dev mode", result.isFallback);
    }
    
    @Test
    public void testCheckLicenseSafe_SafeModeFallback() {
        // Enable safe mode
        System.setProperty("jakarta.migration.safe", "true");
        
        SafeLicenseChecker.LicenseResult result = SafeLicenseChecker.checkLicenseSafe();
        
        // Safe mode uses fallback behavior
        assertFalse("Should not be licensed in safe mode by default", result.isLicensed);
        assertTrue("Should be fallback in safe mode", result.isFallback);
        assertFalse("Should not be certain in safe mode", result.isCertain);
    }
    
    @Test
    public void testCheckLicenseSafe_LicenseDisabled() {
        // Disable license checks
        System.setProperty("jakarta.migration.license.disable", "true");
        
        SafeLicenseChecker.LicenseResult result = SafeLicenseChecker.checkLicenseSafe();
        
        // Disabled license uses fallback behavior
        assertFalse("Should not be licensed when disabled", result.isLicensed);
        assertTrue("Should be fallback when disabled", result.isFallback);
        assertFalse("Should not be certain when disabled", result.isCertain);
    }
    
    @Test
    public void testCheckLicenseAsync_NeverBlocks() {
        // This test verifies that async license check never blocks
        
        long startTime = System.currentTimeMillis();
        SafeLicenseChecker.LicenseResult result = SafeLicenseChecker.checkLicenseAsync().join();
        long endTime = System.currentTimeMillis();
        
        // Should complete quickly
        assertTrue("Async license check should complete quickly", (endTime - startTime) < 2000);
        
        // Should return a valid result
        assertNotNull("Async license result should never be null", result);
        assertNotNull("Async license status should never be null", result.status);
    }
    
    @Test
    public void testCheckLicenseWithTimeout_RespectsTimeout() {
        // This test verifies that license check respects timeout
        
        long startTime = System.currentTimeMillis();
        SafeLicenseChecker.LicenseResult result = SafeLicenseChecker.checkLicenseWithTimeout();
        long endTime = System.currentTimeMillis();
        
        // Should complete within timeout (configured timeout + small buffer)
        long maxTime = LicenseFailsafeConfig.getLicenseTimeoutMs() + 1000;
        assertTrue("License check should respect timeout", (endTime - startTime) < maxTime);
        
        // Should return a fallback result if timeout occurs
        assertNotNull("License result should never be null", result);
    }
    
    @Test
    public void testCheckLicenseOnStartup_NeverBlocks() {
        // This test verifies that startup license check never blocks
        
        Project project = getProject();
        
        long startTime = System.currentTimeMillis();
        
        // This should not block
        SafeLicenseChecker.checkLicenseOnStartup(project);
        
        long endTime = System.currentTimeMillis();
        
        // Should return immediately (async)
        assertTrue("Startup license check should return immediately", (endTime - startTime) < 100);
    }
    
    @Test
    public void trialStatus_FallbackBehavior() {
        // Set up trial
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", 
            String.valueOf(System.currentTimeMillis() + 24 * 60 * 60 * 1000)); // 1 day from now
        
        SafeLicenseChecker.LicenseResult result = SafeLicenseChecker.checkLicenseSafe();
        
        // Should detect trial
        assertTrue("Should detect active trial", result.isLicensed || result.isFallback);
        assertTrue("Should be fallback for trial", result.isFallback);
    }
    
    @Test
    public void trialStatus_ExpiredTrial() {
        // Set up expired trial
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", 
            String.valueOf(System.currentTimeMillis() - 24 * 60 * 60 * 1000)); // 1 day ago
        
        SafeLicenseChecker.LicenseResult result = SafeLicenseChecker.checkLicenseSafe();
        
        // Should detect expired trial
        assertFalse("Should not be licensed for expired trial", result.isLicensed);
        assertTrue("Should be fallback for expired trial", result.isFallback);
        assertEquals("Should show expired trial status", "Trial Expired", result.status);
    }
    
    @Test
    public void testCacheBehavior() {
        // First call should perform check
        SafeLicenseChecker.LicenseResult result1 = SafeLicenseChecker.checkLicenseSafe();
        long firstCallTime = System.currentTimeMillis();
        
        // Second call should use cache (within cache period)
        SafeLicenseChecker.LicenseResult result2 = SafeLicenseChecker.checkLicenseSafe();
        long secondCallTime = System.currentTimeMillis();
        
        // Should return same result quickly due to caching
        assertEquals("Cached result should be identical", result1.status, result2.status);
        assertTrue("Second call should be faster due to caching", 
                   (secondCallTime - firstCallTime) < 100);
    }
    
    @Test
    public void testClearCache() {
        // Perform initial check
        SafeLicenseChecker.checkLicenseSafe();
        
        // Clear cache
        SafeLicenseChecker.clearCache();
        
        // Next check should perform fresh check
        SafeLicenseChecker.LicenseResult result = SafeLicenseChecker.checkLicenseSafe();
        
        // Should still return valid result
        assertNotNull("Result after cache clear should not be null", result);
        assertNotNull("Status after cache clear should not be null", result.status);
    }
    
    @Test
    public void testGetLicenseStatusString_NeverBlocks() {
        long startTime = System.currentTimeMillis();
        String status = SafeLicenseChecker.getLicenseStatusString();
        long endTime = System.currentTimeMillis();
        
        // Should complete quickly
        assertTrue("getLicenseStatusString should complete quickly", (endTime - startTime) < 1000);
        
        // Should return valid status
        assertNotNull("Status should never be null", status);
        assertFalse("Status should not be empty", status.trim().isEmpty());
    }
    
    @Test
    public void testIsPremiumAvailable_NeverBlocks() {
        long startTime = System.currentTimeMillis();
        SafeLicenseChecker.isPremiumAvailable();
        long endTime = System.currentTimeMillis();
        
        // Should complete quickly
        assertTrue("isPremiumAvailable should complete quickly", (endTime - startTime) < 1000);
    }
    
    @Test
    public void testRequestLicenseSafely_DoesNotBlock() {
        Project project = getProject();
        
        long startTime = System.currentTimeMillis();
        
        // This should not block even in headless environment
        SafeLicenseChecker.requestLicenseSafely(project, "Test message");
        
        long endTime = System.currentTimeMillis();
        
        // Should return immediately
        assertTrue("requestLicenseSafely should return immediately", (endTime - startTime) < 100);
    }
}
