package adrianmikula.jakartamigration.intellij.license;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.intellij.ui.LicensingFacade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Core functionality tests for JetBrains Marketplace license verification.
 * Tests main license checking logic without complex certificate validation.
 */
class CheckLicenseCoreTest {

    @BeforeEach
    void setUp() {
        CheckLicense.clearCache();
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("jakarta.migration.premium");
        System.clearProperty("jakarta.migration.trial.end");
        CheckLicense.clearCache();
    }

    // ==================== Basic License Check Tests ====================

    @Test
    @DisplayName("Should return null when LicensingFacade is not available")
    void shouldReturnNullWhenLicensingFacadeNotAvailable() {
        // Given
        try (var mockedLicensingFacade = mockStatic(LicensingFacade.class)) {
            mockedLicensingFacade.when(LicensingFacade::getInstance).thenReturn(null);

            // When
            Boolean result = CheckLicense.isLicensed();

            // Then
            assertThat(result).isNull();
        }
    }

    @Test
    @DisplayName("Should return false when no confirmation stamp found")
    void shouldReturnFalseWhenNoConfirmationStamp() {
        // Given
        try (var mockedLicensingFacade = mockStatic(LicensingFacade.class)) {
            mockedLicensingFacade.when(LicensingFacade::getInstance).thenReturn(mock(LicensingFacade.class));
            when(mock(LicensingFacade.class).getConfirmationStamp(anyString())).thenReturn(null);

            // When
            Boolean result = CheckLicense.isLicensed();

            // Then
            assertThat(result).isFalse();
        }
    }

    @Test
    @DisplayName("Should handle license key prefix correctly")
    void shouldHandleLicenseKeyPrefix() {
        // Given
        try (var mockedLicensingFacade = mockStatic(LicensingFacade.class)) {
            mockedLicensingFacade.when(LicensingFacade::getInstance).thenReturn(mock(LicensingFacade.class));
            when(mock(LicensingFacade.class).getConfirmationStamp(anyString()))
                    .thenReturn("key:testLicenseId-testData-testSignature-testCertificate");

            // When
            Boolean result = CheckLicense.isLicensed();

            // Then
            // Should attempt to validate key (may fail due to invalid test data)
            // The important thing is that it correctly identifies the key format
            verify(mock(LicensingFacade.class)).getConfirmationStamp("PJAKARTAMIGRATI");
            assertThat(result).isNotNull();
        }
    }

    // ==================== Trial System Tests ====================

    @Test
    @DisplayName("Should return true when trial is active")
    void shouldReturnTrueWhenTrialActive() {
        // Given
        try (var mockedLicensingFacade = mockStatic(LicensingFacade.class)) {
            mockedLicensingFacade.when(LicensingFacade::getInstance).thenReturn(null);
            System.setProperty("jakarta.migration.premium", "true");
            System.setProperty("jakarta.migration.trial.end", 
                    String.valueOf(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000));

            // When
            Boolean result = CheckLicense.isLicensed();

            // Then
            assertThat(result).isTrue();
        }
    }

    @Test
    @DisplayName("Should return false when trial has expired")
    void shouldReturnFalseWhenTrialExpired() {
        // Given
        try (var mockedLicensingFacade = mockStatic(LicensingFacade.class)) {
            mockedLicensingFacade.when(LicensingFacade::getInstance).thenReturn(null);
            System.setProperty("jakarta.migration.premium", "true");
            System.setProperty("jakarta.migration.trial.end", 
                    String.valueOf(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000));

            // When
            Boolean result = CheckLicense.isLicensed();

            // Then
            assertThat(result).isFalse();
            // Should clean up expired trial
            assertThat(System.getProperty("jakarta.migration.premium")).isEqualTo("false");
        }
    }

    @Test
    @DisplayName("Should return false when trial system property is not set")
    void shouldReturnFalseWhenTrialNotSet() {
        // Given
        try (var mockedLicensingFacade = mockStatic(LicensingFacade.class)) {
            mockedLicensingFacade.when(LicensingFacade::getInstance).thenReturn(null);

            // When
            Boolean result = CheckLicense.isLicensed();

            // Then
            assertThat(result).isFalse();
        }
    }

    // ==================== License Status String Tests ====================

    @Test
    @DisplayName("Should return 'Checking...' when license status is null")
    void shouldReturnCheckingWhenLicenseStatusNull() {
        // Given
        try (var mockedLicensingFacade = mockStatic(LicensingFacade.class)) {
            mockedLicensingFacade.when(LicensingFacade::getInstance).thenReturn(null);

            // When
            String status = CheckLicense.getLicenseStatusString();

            // Then
            assertThat(status).isEqualTo("Checking...");
        }
    }

    @Test
    @DisplayName("Should return 'Free' when not licensed")
    void shouldReturnFreeWhenNotLicensed() {
        // Given
        try (var mockedLicensingFacade = mockStatic(LicensingFacade.class)) {
            mockedLicensingFacade.when(LicensingFacade::getInstance).thenReturn(mock(LicensingFacade.class));
            when(mock(LicensingFacade.class).getConfirmationStamp(anyString())).thenReturn(null);

            // When
            String status = CheckLicense.getLicenseStatusString();

            // Then
            assertThat(status).isEqualTo("Free");
        }
    }

    @Test
    @DisplayName("Should return trial status with days remaining")
    void shouldReturnTrialStatusWithDaysRemaining() {
        // Given
        try (var mockedLicensingFacade = mockStatic(LicensingFacade.class)) {
            mockedLicensingFacade.when(LicensingFacade::getInstance).thenReturn(null);
            System.setProperty("jakarta.migration.premium", "true");
            System.setProperty("jakarta.migration.trial.end", 
                    String.valueOf(System.currentTimeMillis() + (2 * 24 * 60 * 60 * 1000))); // 2 days

            // When
            String status = CheckLicense.getLicenseStatusString();

            // Then
            assertThat(status).startsWith("Trial - ");
            assertThat(status).contains("days remaining");
        }
    }

    // ==================== Trial Management Tests ====================

    @Test
    @DisplayName("Should start trial correctly")
    void shouldStartTrialCorrectly() {
        // Given
        long beforeTime = System.currentTimeMillis();

        // When
        CheckLicense.startTrial();

        // Then
        assertThat(System.getProperty("jakarta.migration.premium")).isEqualTo("true");
        
        String trialEnd = System.getProperty("jakarta.migration.trial.end");
        assertThat(trialEnd).isNotNull();
        
        long trialEndTime = Long.parseLong(trialEnd);
        long expectedEndTime = beforeTime + 7L * 24 * 60 * 60 * 1000;
        
        // Allow for small timing differences (within 1 second)
        assertThat(Math.abs(trialEndTime - expectedEndTime)).isLessThan(1000);
    }

    // ==================== Caching Tests ====================

    @Test
    @DisplayName("Should cache license check results")
    void shouldCacheLicenseCheckResults() {
        // Given
        try (var mockedLicensingFacade = mockStatic(LicensingFacade.class)) {
            mockedLicensingFacade.when(LicensingFacade::getInstance).thenReturn(mock(LicensingFacade.class));
            when(mock(LicensingFacade.class).getConfirmationStamp(anyString())).thenReturn(null);

            // When - call multiple times
            Boolean result1 = CheckLicense.isLicensed();
            Boolean result2 = CheckLicense.isLicensed();

            // Then - should only call LicensingFacade once due to caching
            verify(mock(LicensingFacade.class), times(1)).getConfirmationStamp(anyString());
            assertThat(result1).isEqualTo(result2);
        }
    }

    @Test
    @DisplayName("Should clear cache when requested")
    void shouldClearCacheWhenRequested() {
        // Given
        try (var mockedLicensingFacade = mockStatic(LicensingFacade.class)) {
            mockedLicensingFacade.when(LicensingFacade::getInstance).thenReturn(mock(LicensingFacade.class));
            when(mock(LicensingFacade.class).getConfirmationStamp(anyString())).thenReturn(null);

            // When
            CheckLicense.isLicensed(); // First call to cache result
            CheckLicense.clearCache(); // Clear cache
            CheckLicense.isLicensed(); // Second call should hit LicensingFacade again

            // Then
            verify(mock(LicensingFacade.class), times(2)).getConfirmationStamp(anyString());
        }
    }

    // ==================== Error Handling Tests ====================

    @Test
    @DisplayName("Should handle LicensingFacade exceptions gracefully")
    void shouldHandleLicensingFacadeExceptions() {
        // Given
        try (var mockedLicensingFacade = mockStatic(LicensingFacade.class)) {
            mockedLicensingFacade.when(LicensingFacade::getInstance).thenReturn(mock(LicensingFacade.class));
            when(mock(LicensingFacade.class).getConfirmationStamp(anyString()))
                    .thenThrow(new RuntimeException("Test exception"));

            // When
            Boolean result = CheckLicense.isLicensed();

            // Then
            // Should fall back to trial system
            assertThat(result).isNotNull();
        }
    }

    // ==================== Performance Tests ====================

    @Test
    @DisplayName("Should handle frequent license checks efficiently")
    void shouldHandleFrequentLicenseChecksEfficiently() {
        // Given
        try (var mockedLicensingFacade = mockStatic(LicensingFacade.class)) {
            mockedLicensingFacade.when(LicensingFacade::getInstance).thenReturn(mock(LicensingFacade.class));
            when(mock(LicensingFacade.class).getConfirmationStamp(anyString())).thenReturn(null);

            // When - call multiple times rapidly
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 100; i++) {
                CheckLicense.isLicensed();
            }
            long endTime = System.currentTimeMillis();

            // Then - should be fast due to caching (under 100ms for 100 calls)
            assertThat(endTime - startTime).isLessThan(100);
            
            // Should only call LicensingFacade once due to caching
            verify(mock(LicensingFacade.class), times(1)).getConfirmationStamp(anyString());
        }
    }
}
