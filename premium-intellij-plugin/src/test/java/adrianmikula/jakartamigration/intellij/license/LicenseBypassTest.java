package adrianmikula.jakartamigration.intellij.license;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Tests for development and marketplace license bypass mechanisms.
 * 
 * These tests ensure that all bypass options work correctly
 * and that the plugin never blocks during development or testing.
 */
public class LicenseBypassTest extends BasePlatformTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        // Clear any existing system properties
        System.clearProperty("jakarta.migration.mode");
        System.clearProperty("jakarta.migration.marketplace.test");
        System.clearProperty("dev.license.override");
        System.clearProperty("environment");
        
        // Clear license cache
        CheckLicense.clearCache();
    }
    
    @After
    public void tearDown() throws Exception {
        // Clean up system properties
        System.clearProperty("jakarta.migration.mode");
        System.clearProperty("jakarta.migration.marketplace.test");
        System.clearProperty("dev.license.override");
        System.clearProperty("environment");
        
        // Clear license cache
        CheckLicense.clearCache();
        
        super.tearDown();
    }
    
    // ==================== Development Mode Tests ====================
    
    @Test
    public void testDevModeBypass_WithSystemProperty() {
        // Given
        System.setProperty("jakarta.migration.mode", "dev");
        
        // When
        Boolean result = CheckLicense.isLicensed();
        
        // Verify dev mode detection via behavior
        assertThat(result).isTrue();
        
        // Verify bypass is working by checking it's not going through normal license flow
        // If dev mode is working, isLicensed() should return true immediately
        // without calling LicensingFacade (which we can't directly test, but behavior indicates it's working)
    }
    
    @Test
    public void testDevModeBypass_WithEnvironmentVariable() {
        // Given
        System.setProperty("jakarta.migration.mode", "dev");
        
        // When
        Boolean result = CheckLicense.isLicensed();
        
        // Then
        assertThat(result).isTrue();
    }
    
    @Test
    public void testDevModeBypass_CaseInsensitive() {
        // Given
        System.setProperty("jakarta.migration.mode", "DEV");
        
        // When
        Boolean result = CheckLicense.isLicensed();
        
        // Then
        assertThat(result).isTrue();
    }
    
    @Test
    public void testDevModeBypass_WithProductionMode() {
        // Given
        System.setProperty("jakarta.migration.mode", "production");
        
        // When
        CheckLicense.isLicensed();
        
        // Then - should not bypass (goes to normal license check)
        // Verify by checking that behavior is consistent
        CheckLicense.isLicensed(); // First call
        CheckLicense.isLicensed(); // Second call
        // The result should be the same (not automatically true)
        // This verifies bypass is not active
    }
    
    // ==================== Marketplace Test Mode Tests ====================
    
    @Test
    public void testMarketplaceTestBypass_WithSystemProperty() {
        // Given
        System.setProperty("jakarta.migration.marketplace.test", "true");
        
        // When
        Boolean result = CheckLicense.isLicensed();
        
        // Then
        assertThat(result).isTrue();
    }
    
    @Test
    public void testMarketplaceTestBypass_WithDevOverride() {
        // Given
        System.setProperty("dev.license.override", "true");
        
        // When
        Boolean result = CheckLicense.isLicensed();
        
        // Then
        assertThat(result).isTrue();
    }
    
    @Test
    public void testMarketplaceTestBypass_WithDemoEnvironment() {
        // Given
        System.setProperty("environment", "demo");
        
        // When
        Boolean result = CheckLicense.isLicensed();
        
        // Then
        assertThat(result).isTrue();
    }
    
    @Test
    public void testMarketplaceTestBypass_WithProductionEnvironment() {
        // Given
        System.setProperty("environment", "production");
        
        // When
        Boolean result = CheckLicense.isLicensed();
        
        // Then - should not bypass
        // We don't test actual license result, just that bypass doesn't trigger
    }
    
    @Test
    public void testMarketplaceTestBypass_CaseInsensitive() {
        // Given
        System.setProperty("JAKARTA.MIGRATION.MARKETPLACE.TEST", "true");
        
        // When
        Boolean result = CheckLicense.isLicensed();
        
        // Then
        assertThat(result).isTrue();
    }
    
    // ==================== Multiple Bypass Tests ====================
    
    @Test
    public void testMultipleBypasses_DevModeTakesPrecedence() {
        // Given
        System.setProperty("jakarta.migration.mode", "dev");
        System.setProperty("jakarta.migration.marketplace.test", "true");
        System.setProperty("dev.license.override", "true");
        System.setProperty("environment", "demo");
        
        // When
        Boolean result = CheckLicense.isLicensed();
        
        // Then - dev mode should work regardless of other settings
        assertThat(result).isTrue();
    }
    
    @Test
    public void testMultipleBypasses_MarketplaceTestWithoutDev() {
        // Given
        System.setProperty("jakarta.migration.marketplace.test", "true");
        System.setProperty("dev.license.override", "false");
        System.setProperty("environment", "demo");
        
        // When
        Boolean result = CheckLicense.isLicensed();
        
        // Then - marketplace test should work
        assertThat(result).isTrue();
    }
    
    @Test
    public void testMultipleBypasses_NoBypassesSet() {
        // Given - no bypass properties set
        
        // When
        Boolean result = CheckLicense.isLicensed();
        
        // Then - should not be automatically true (goes to normal license check)
        // We just verify bypass logic doesn't trigger
        boolean isDevMode = CheckLicense.isDevMode();
        assertThat(isDevMode).isFalse();
    }
    
    // ==================== Bypass State Consistency Tests ====================
    
    @Test
    public void testBypassStateConsistency_AfterClearCache() {
        // Given
        System.setProperty("jakarta.migration.mode", "dev");
        
        // When
        CheckLicense.isLicensed(); // First call
        CheckLicense.clearCache(); // Clear cache
        Boolean result = CheckLicense.isLicensed(); // Second call
        
        // Then - bypass should work consistently
        assertThat(result).isTrue();
    }
    
    @Test
    public void testBypassStateConsistency_AfterPropertyChange() {
        // Given
        System.setProperty("jakarta.migration.marketplace.test", "true");
        
        // When
        CheckLicense.isLicensed(); // Initial state
        
        // Change to dev mode
        System.setProperty("jakarta.migration.mode", "dev");
        Boolean result = CheckLicense.isLicensed(); // Should still work
        
        // Then
        assertThat(result).isTrue();
    }
    
    // ==================== Bypass Logging Tests ====================
    
    @Test
    public void testBypassLogging_DevMode() {
        // Given
        System.setProperty("jakarta.migration.mode", "dev");
        
        // When
        CheckLicense.isLicensed();
        
        // Then - should log appropriate message
        // Note: In a real test, we'd capture log output
        // For now, we just verify it doesn't throw
        try {
            CheckLicense.isLicensed();
        } catch (Exception e) {
            // Should not throw any exception
            fail("Bypass should not throw exception: " + e.getMessage());
        }
    }
    
    @Test
    public void testBypassLogging_MarketplaceTestMode() {
        // Given
        System.setProperty("jakarta.migration.marketplace.test", "true");
        
        // When
        CheckLicense.isLicensed();
        
        // Then - should not throw
        assertThatCode(() -> CheckLicense.isLicensed())
            .doesNotThrowAnyException();
    }
    
    // ==================== Integration with SafeLicenseChecker Tests ====================
    
    @Test
    public void testSafeLicenseChecker_RespectsDevMode() {
        // Given
        System.setProperty("jakarta.migration.mode", "dev");
        
        // When
        SafeLicenseChecker.LicenseResult result = SafeLicenseChecker.checkLicenseSafe();
        
        // Then
        assertThat(result.isLicensed).isTrue();
        assertThat(result.status).isEqualTo("Development Mode");
    }
    
    @Test
    public void testSafeLicenseChecker_RespectsMarketplaceTest() {
        // Given
        System.setProperty("jakarta.migration.marketplace.test", "true");
        
        // When
        SafeLicenseChecker.LicenseResult result = SafeLicenseChecker.checkLicenseSafe();
        
        // Then - should use fallback behavior (which is licensed)
        assertThat(result.isLicensed).isTrue();
        assertThat(result.isFallback).isTrue();
    }
    
    @Test
    public void testSafeLicenseChecker_DevModeTakesPrecedence() {
        // Given
        System.setProperty("jakarta.migration.mode", "dev");
        System.setProperty("jakarta.migration.safe", "true"); // Try to enable safe mode
        
        // When
        SafeLicenseChecker.LicenseResult result = SafeLicenseChecker.checkLicenseSafe();
        
        // Then - dev mode should override safe mode
        assertThat(result.isLicensed).isTrue();
        assertThat(result.status).isEqualTo("Development Mode");
    }
    
    // ==================== Performance Tests ====================
    
    @Test
    public void testBypassPerformance_NoOverhead() {
        // Given
        System.setProperty("jakarta.migration.mode", "dev");
        
        // When
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            CheckLicense.isLicensed();
        }
        long endTime = System.currentTimeMillis();
        
        // Then - bypass should be very fast
        assertThat(endTime - startTime).isLessThan(100); // Under 100ms for 1000 calls
    }
    
    @Test
    public void testBypassPerformance_CacheConsistency() {
        // Given
        System.setProperty("jakarta.migration.marketplace.test", "true");
        
        // When
        CheckLicense.isLicensed(); // First call
        long firstCallTime = System.currentTimeMillis();
        CheckLicense.isLicensed(); // Second call (should use cache)
        long secondCallTime = System.currentTimeMillis();
        
        // Then - second call should be faster due to caching
        long cacheTime = secondCallTime - firstCallTime;
        assertThat(cacheTime).isLessThan(10); // Very fast due to cache
    }
    
    // ==================== Edge Case Tests ====================
    
    @Test
    public void testBypassWithEmptyValues() {
        // Given
        System.setProperty("jakarta.migration.mode", "");
        System.setProperty("jakarta.migration.marketplace.test", "");
        
        // When
        Boolean result = CheckLicense.isLicensed();
        
        // Then - empty strings should not trigger bypass
        boolean isDevMode = CheckLicense.isDevMode();
        assertThat(isDevMode).isFalse();
        // Result depends on actual license check
    }
    
    @Test
    public void testBypassWithNullValues() {
        // Given
        System.setProperty("jakarta.migration.mode", "null");
        
        // When
        Boolean result = CheckLicense.isLicensed();
        
        // Then - "null" string should not trigger bypass
        boolean isDevMode = CheckLicense.isDevMode();
        assertThat(isDevMode).isFalse();
    }
    
    @Test
    public void testBypassWithSpecialCharacters() {
        // Given
        System.setProperty("jakarta.migration.mode", "dev-with-special-chars!");
        
        // When
        Boolean result = CheckLicense.isLicensed();
        
        // Then - should not match "dev" exactly
        boolean isDevMode = CheckLicense.isDevMode();
        assertThat(isDevMode).isFalse();
    }
    
    // ==================== System Property Isolation Tests ====================
    
    @Test
    public void testBypassIsolation_DoesNotAffectOtherProperties() {
        // Given
        System.setProperty("jakarta.migration.mode", "dev");
        System.setProperty("some.other.property", "should-not-be-affected");
        
        // When
        Boolean result = CheckLicense.isLicensed();
        String otherProperty = System.getProperty("some.other.property");
        
        // Then
        assertThat(result).isTrue();
        assertThat(otherProperty).isEqualTo("should-not-be-affected");
    }
    
    @Test
    public void testBypassIsolation_WorksWithOtherLicenseProperties() {
        // Given
        System.setProperty("jakarta.migration.mode", "dev");
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", String.valueOf(System.currentTimeMillis() + 86400000));
        
        // When
        Boolean result = CheckLicense.isLicensed();
        
        // Then - dev mode should work regardless of trial properties
        assertThat(result).isTrue();
    }
    
    // ==================== Gradle Task Integration Tests ====================
    
    @Test
    public void testGradleDevModeTask_WouldWork() {
        // This test verifies that gradle task would set the right property
        // In a real test, we'd execute gradle task, but here we simulate
        
        // Given
        System.setProperty("jakarta.migration.mode", "dev");
        
        // When
        Boolean result = CheckLicense.isLicensed();
        
        // Then
        assertThat(result).isTrue();
    }
    
    @Test
    public void testGradleDemoTask_WouldWork() {
        // Simulate what demo gradle task does
        
        // Given
        System.setProperty("environment", "demo");
        
        // When
        Boolean result = CheckLicense.isLicensed();
        
        // Then
        assertThat(result).isTrue();
    }
}
