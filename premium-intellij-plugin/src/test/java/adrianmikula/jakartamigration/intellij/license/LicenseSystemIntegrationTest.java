package adrianmikula.jakartamigration.intellij.license;

import adrianmikula.jakartamigration.intellij.config.LicenseFailsafeConfig;
import adrianmikula.jakartamigration.intellij.ui.SupportComponent;
import adrianmikula.jakartamigration.intellij.LicenseCheckStartupActivity;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.mockito.MockedStatic;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Integration tests for the complete licensing system.
 * 
 * These tests verify that all licensing components work together correctly:
 * - SafeLicenseChecker
 * - LicenseFailsafeConfig  
 * - CheckLicense (original implementation)
 * - LicenseCheckStartupActivity
 * - SupportComponent
 * - UI integration
 */
public class LicenseSystemIntegrationTest extends BasePlatformTestCase {

    private MockedStatic<LicenseFailsafeConfig> mockedFailsafeConfig;
    private MockedStatic<CheckLicense> mockedCheckLicense;
    
    @Before
    public void setUp() throws Exception {
        super.setUp();
        mockedFailsafeConfig = mockStatic(LicenseFailsafeConfig.class);
        mockedCheckLicense = mockStatic(CheckLicense.class);
        
        // Clear all caches
        SafeLicenseChecker.clearCache();
        CheckLicense.clearCache();
    }
    
    @After
    public void tearDown() throws Exception {
        if (mockedFailsafeConfig != null) {
            mockedFailsafeConfig.close();
        }
        if (mockedCheckLicense != null) {
            mockedCheckLicense.close();
        }
        
        // Clear caches and restore UI state
        SafeLicenseChecker.clearCache();
        CheckLicense.clearCache();
        SupportComponent.setPremiumActive(false);
        SupportComponent.setLicenseStatus("Free");
        
        super.tearDown();
    }
    
    // ==================== Complete Flow Integration Tests ====================
    
    @Test
    public void testCompleteFlow_WithValidLicense() {
        // Given - valid JetBrains license
        setupNormalMode();
        mockedCheckLicense.when(CheckLicense::isLicensed).thenReturn(true);
        mockedCheckLicense.when(CheckLicense::getLicenseStatusString).thenReturn("Premium Active");
        
        Project project = getProject();
        
        // When - simulate complete startup flow
        // 1. Startup activity runs
        LicenseCheckStartupActivity startupActivity = new LicenseCheckStartupActivity();
        startupActivity.runActivity(project);
        
        // 2. License check performed
        SafeLicenseChecker.LicenseResult result = SafeLicenseChecker.checkLicenseSafe();
        
        // 3. UI components check status
        boolean isPremium = SupportComponent.isPremiumActive();
        String status = SafeLicenseChecker.getLicenseStatusString();
        
        // Then - everything should work correctly
        assertThat(result.isLicensed).isTrue();
        assertThat(result.status).isEqualTo("Premium Active");
        assertThat(isPremium).isTrue();
        assertThat(status).isEqualTo("Premium Active");
    }
    
    @Test
    public void testCompleteFlow_WithTrialLicense() {
        // Given - trial license
        setupNormalMode();
        mockedCheckLicense.when(CheckLicense::isLicensed).thenReturn(false); // No JetBrains license
        
        // Set up trial
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", 
            String.valueOf(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000));
        
        try {
            Project project = getProject();
            
            // When - simulate complete flow
            LicenseCheckStartupActivity startupActivity = new LicenseCheckStartupActivity();
            startupActivity.runActivity(project);
            
            SafeLicenseChecker.LicenseResult result = SafeLicenseChecker.checkLicenseSafe();
            boolean isPremium = SupportComponent.isPremiumActive();
            String status = SafeLicenseChecker.getLicenseStatusString();
            
            // Then - trial should be detected
            assertThat(result.isLicensed).isTrue();
            assertThat(result.isFallback).isTrue();
            assertThat(isPremium).isTrue();
            assertThat(status).startsWith("Trial - ");
            
        } finally {
            System.clearProperty("jakarta.migration.premium");
            System.clearProperty("jakarta.migration.trial.end");
        }
    }
    
    @Test
    public void testCompleteFlow_WithExpiredTrial() {
        // Given - expired trial
        setupNormalMode();
        mockedCheckLicense.when(CheckLicense::isLicensed).thenReturn(false);
        
        // Set up expired trial
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", 
            String.valueOf(System.currentTimeMillis() - 24 * 60 * 60 * 1000));
        
        try {
            Project project = getProject();
            
            // When
            LicenseCheckStartupActivity startupActivity = new LicenseCheckStartupActivity();
            startupActivity.runActivity(project);
            
            SafeLicenseChecker.LicenseResult result = SafeLicenseChecker.checkLicenseSafe();
            boolean isPremium = SupportComponent.isPremiumActive();
            String status = SafeLicenseChecker.getLicenseStatusString();
            
            // Then - should show as expired
            assertThat(result.isLicensed).isFalse();
            assertThat(result.isFallback).isTrue();
            assertThat(isPremium).isFalse();
            assertThat(status).isEqualTo("Free");
            
        } finally {
            System.clearProperty("jakarta.migration.premium");
            System.clearProperty("jakarta.migration.trial.end");
        }
    }
    
    @Test
    public void testCompleteFlow_WithNoLicense() {
        // Given - no license at all
        setupNormalMode();
        mockedCheckLicense.when(CheckLicense::isLicensed).thenReturn(false);
        
        Project project = getProject();
        
        // When
        LicenseCheckStartupActivity startupActivity = new LicenseCheckStartupActivity();
        startupActivity.runActivity(project);
        
        SafeLicenseChecker.LicenseResult result = SafeLicenseChecker.checkLicenseSafe();
        boolean isPremium = SupportComponent.isPremiumActive();
        String status = SafeLicenseChecker.getLicenseStatusString();
        
        // Then - should show as free
        assertThat(result.isLicensed).isFalse();
        assertThat(isPremium).isFalse();
        assertThat(status).isEqualTo("Free");
    }
    
    // ==================== Failsafe Mode Integration Tests ====================
    
    @Test
    public void testCompleteFlow_DevMode() {
        // Given - dev mode
        setupDevMode();
        
        Project project = getProject();
        
        // When
        LicenseCheckStartupActivity startupActivity = new LicenseCheckStartupActivity();
        startupActivity.runActivity(project);
        
        SafeLicenseChecker.LicenseResult result = SafeLicenseChecker.checkLicenseSafe();
        boolean isPremium = SupportComponent.isPremiumActive();
        String status = SafeLicenseChecker.getLicenseStatusString();
        
        // Then - should be licensed regardless of actual license
        assertThat(result.isLicensed).isTrue();
        assertThat(result.status).isEqualTo("Development Mode");
        assertThat(isPremium).isTrue();
        assertThat(status).isEqualTo("Development Mode");
    }
    
    @Test
    public void testCompleteFlow_SafeMode() {
        // Given - safe mode
        setupSafeMode();
        
        Project project = getProject();
        
        // When
        LicenseCheckStartupActivity startupActivity = new LicenseCheckStartupActivity();
        startupActivity.runActivity(project);
        
        SafeLicenseChecker.LicenseResult result = SafeLicenseChecker.checkLicenseSafe();
        boolean isPremium = SupportComponent.isPremiumActive();
        String status = SafeLicenseChecker.getLicenseStatusString();
        
        // Then - should use fallback behavior
        assertThat(result.isFallback).isTrue();
        assertThat(isPremium).isFalse(); // Fallback defaults to not premium
        assertThat(status).isEqualTo("Free");
    }
    
    @Test
    public void testCompleteFlow_LicenseDisabled() {
        // Given - license completely disabled
        setupLicenseDisabled();
        
        Project project = getProject();
        
        // When
        LicenseCheckStartupActivity startupActivity = new LicenseCheckStartupActivity();
        startupActivity.runActivity(project);
        
        SafeLicenseChecker.LicenseResult result = SafeLicenseChecker.checkLicenseSafe();
        boolean isPremium = SupportComponent.isPremiumActive();
        String status = SafeLicenseChecker.getLicenseStatusString();
        
        // Then - should use fallback behavior
        assertThat(result.isFallback).isTrue();
        assertThat(isPremium).isFalse();
        assertThat(status).isEqualTo("Free");
    }
    
    // ==================== Error Recovery Integration Tests ====================
    
    @Test
    public void testCompleteFlow_WithLicenseCheckException() {
        // Given - license check throws exception
        setupNormalMode();
        mockedCheckLicense.when(CheckLicense::isLicensed).thenThrow(new RuntimeException("Network error"));
        
        Project project = getProject();
        
        // When
        LicenseCheckStartupActivity startupActivity = new LicenseCheckStartupActivity();
        startupActivity.runActivity(project);
        
        SafeLicenseChecker.LicenseResult result = SafeLicenseChecker.checkLicenseSafe();
        boolean isPremium = SupportComponent.isPremiumActive();
        String status = SafeLicenseChecker.getLicenseStatusString();
        
        // Then - should fallback gracefully
        assertThat(result).isNotNull();
        assertThat(result.isFallback).isTrue();
        assertThat(isPremium).isFalse();
        assertThat(status).isEqualTo("Free");
    }
    
    @Test
    public void testCompleteFlow_WithLicensingFacadeNull() {
        // Given - LicensingFacade not available
        setupNormalMode();
        mockedCheckLicense.when(CheckLicense::isLicensed).thenReturn(null);
        
        Project project = getProject();
        
        // When
        LicenseCheckStartupActivity startupActivity = new LicenseCheckStartupActivity();
        startupActivity.runActivity(project);
        
        SafeLicenseChecker.LicenseResult result = SafeLicenseChecker.checkLicenseSafe();
        String status = SafeLicenseChecker.getLicenseStatusString();
        
        // Then - should handle gracefully
        assertThat(result).isNotNull();
        assertThat(result.isCertain).isFalse();
        assertThat(status).isEqualTo("Checking...");
    }
    
    // ==================== Performance Integration Tests ====================
    
    @Test
    public void testCompleteFlow_PerformanceUnderLoad() {
        // Given
        setupNormalMode();
        mockedCheckLicense.when(CheckLicense::isLicensed).thenReturn(true);
        mockedCheckLicense.when(CheckLicense::getLicenseStatusString).thenReturn("Premium Active");
        
        Project project = getProject();
        
        // When - simulate many rapid startup sequences
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 100; i++) {
            LicenseCheckStartupActivity startupActivity = new LicenseCheckStartupActivity();
            startupActivity.runActivity(project);
            
            SafeLicenseChecker.checkLicenseSafe();
            SupportComponent.isPremiumActive();
            SafeLicenseChecker.getLicenseStatusString();
        }
        
        long endTime = System.currentTimeMillis();
        
        // Then - should handle load efficiently
        assertThat(endTime - startTime).isLessThan(5000); // Under 5 seconds for 100 iterations
        
        // Verify final state
        SafeLicenseChecker.LicenseResult result = SafeLicenseChecker.checkLicenseSafe();
        assertThat(result.isLicensed).isTrue();
    }
    
    @Test
    public void testCompleteFlow_ConcurrentAccess() throws InterruptedException {
        // Given
        setupNormalMode();
        mockedCheckLicense.when(CheckLicense::isLicensed).thenReturn(true);
        mockedCheckLicense.when(CheckLicense::getLicenseStatusString).thenReturn("Premium Active");
        
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        
        // When - concurrent startup sequences
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                Project project = getProject();
                LicenseCheckStartupActivity startupActivity = new LicenseCheckStartupActivity();
                startupActivity.runActivity(project);
                
                SafeLicenseChecker.checkLicenseSafe();
                SupportComponent.isPremiumActive();
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
        
        // Then - should handle concurrency efficiently
        assertThat(endTime - startTime).isLessThan(3000);
        
        // Verify final state
        SafeLicenseChecker.LicenseResult result = SafeLicenseChecker.checkLicenseSafe();
        assertThat(result.isLicensed).isTrue();
    }
    
    // ==================== State Transition Integration Tests ====================
    
    @Test
    public void testCompleteFlow_TrialToLicenseTransition() {
        // Given - start with trial
        setupNormalMode();
        mockedCheckLicense.when(CheckLicense::isLicensed).thenReturn(false);
        
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", 
            String.valueOf(System.currentTimeMillis() + 24 * 60 * 60 * 1000));
        
        Project project = getProject();
        
        try {
            // When - initial trial state
            LicenseCheckStartupActivity startupActivity = new LicenseCheckStartupActivity();
            startupActivity.runActivity(project);
            
            SafeLicenseChecker.LicenseResult trialResult = SafeLicenseChecker.checkLicenseSafe();
            assertThat(trialResult.isLicensed).isTrue();
            assertThat(trialResult.isFallback).isTrue();
            
            // Clear cache and simulate JetBrains license
            SafeLicenseChecker.clearCache();
            mockedCheckLicense.when(CheckLicense::isLicensed).thenReturn(true);
            mockedCheckLicense.when(CheckLicense::getLicenseStatusString).thenReturn("Premium Active");
            
            startupActivity.runActivity(project);
            SafeLicenseChecker.LicenseResult licenseResult = SafeLicenseChecker.checkLicenseSafe();
            
            // Then - should transition to proper license
            assertThat(licenseResult.isLicensed).isTrue();
            assertThat(licenseResult.isFallback).isFalse();
            assertThat(licenseResult.status).isEqualTo("Premium Active");
            
        } finally {
            System.clearProperty("jakarta.migration.premium");
            System.clearProperty("jakarta.migration.trial.end");
        }
    }
    
    @Test
    public void testCompleteFlow_LicenseToTrialTransition() {
        // Given - start with JetBrains license, then lose it
        setupNormalMode();
        mockedCheckLicense.when(CheckLicense::isLicensed).thenReturn(true);
        mockedCheckLicense.when(CheckLicense::getLicenseStatusString).thenReturn("Premium Active");
        
        Project project = getProject();
        
        // When - initial license state
        LicenseCheckStartupActivity startupActivity = new LicenseCheckStartupActivity();
        startupActivity.runActivity(project);
        
        SafeLicenseChecker.LicenseResult licenseResult = SafeLicenseChecker.checkLicenseSafe();
        assertThat(licenseResult.isLicensed).isTrue();
        assertThat(licenseResult.isFallback).isFalse();
        
        // Clear cache and simulate license loss with trial
        SafeLicenseChecker.clearCache();
        mockedCheckLicense.when(CheckLicense::isLicensed).thenReturn(false);
        
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", 
            String.valueOf(System.currentTimeMillis() + 24 * 60 * 60 * 1000));
        
        try {
            startupActivity.runActivity(project);
            SafeLicenseChecker.LicenseResult trialResult = SafeLicenseChecker.checkLicenseSafe();
            
            // Then - should transition to trial
            assertThat(trialResult.isLicensed).isTrue();
            assertThat(trialResult.isFallback).isTrue();
            assertThat(trialResult.status).startsWith("Trial - ");
            
        } finally {
            System.clearProperty("jakarta.migration.premium");
            System.clearProperty("jakarta.migration.trial.end");
        }
    }
    
    // ==================== UI Integration Tests ====================
    
    @Test
    public void testCompleteFlow_UIStateConsistency() {
        // Given - valid license
        setupNormalMode();
        mockedCheckLicense.when(CheckLicense::isLicensed).thenReturn(true);
        mockedCheckLicense.when(CheckLicense::getLicenseStatusString).thenReturn("Premium Active");
        
        Project project = getProject();
        
        // When - complete flow
        LicenseCheckStartupActivity startupActivity = new LicenseCheckStartupActivity();
        startupActivity.runActivity(project);
        
        SafeLicenseChecker.checkLicenseSafe();
        
        // Then - UI state should be consistent
        boolean isPremiumActive = SupportComponent.isPremiumActive();
        String licenseStatus = SafeLicenseChecker.getLicenseStatusString();
        SafeLicenseChecker.LicenseResult result = SafeLicenseChecker.checkLicenseSafe();
        
        assertThat(isPremiumActive).isEqualTo(result.isLicensed || result.isFallback);
        assertThat(licenseStatus).isEqualTo(result.status);
    }
    
    @Test
    public void testCompleteFlow_UIStateAfterModeChange() {
        // Given - start in normal mode with no license
        setupNormalMode();
        mockedCheckLicense.when(CheckLicense::isLicensed).thenReturn(false);
        
        Project project = getProject();
        
        // When - initial state
        LicenseCheckStartupActivity startupActivity = new LicenseCheckStartupActivity();
        startupActivity.runActivity(project);
        
        SafeLicenseChecker.checkLicenseSafe();
        boolean initialPremium = SupportComponent.isPremiumActive();
        assertThat(initialPremium).isFalse();
        
        // Change to dev mode
        setupDevMode();
        SafeLicenseChecker.clearCache();
        
        startupActivity.runActivity(project);
        SafeLicenseChecker.checkLicenseSafe();
        boolean devPremium = SupportComponent.isPremiumActive();
        
        // Then - UI should reflect mode change
        assertThat(devPremium).isTrue();
        assertThat(devPremium).isNotEqualTo(initialPremium);
    }
    
    // ==================== Helper Methods ====================
    
    private void setupNormalMode() {
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isDevMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isSafeMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isLicenseDisabled).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isTrialForced).thenReturn(false);
    }
    
    private void setupDevMode() {
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isDevMode).thenReturn(true);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isSafeMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isLicenseDisabled).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isTrialForced).thenReturn(false);
    }
    
    private void setupSafeMode() {
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isDevMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isSafeMode).thenReturn(true);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isLicenseDisabled).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isTrialForced).thenReturn(false);
    }
    
    private void setupLicenseDisabled() {
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isDevMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isSafeMode).thenReturn(false);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isLicenseDisabled).thenReturn(true);
        mockedFailsafeConfig.when(LicenseFailsafeConfig::isTrialForced).thenReturn(false);
    }
}
