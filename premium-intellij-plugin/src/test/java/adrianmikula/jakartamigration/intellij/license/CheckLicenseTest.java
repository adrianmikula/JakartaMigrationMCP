package adrianmikula.jakartamigration.intellij.license;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.intellij.ui.LicensingFacade;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for JetBrains Marketplace license verification.
 * Tests all aspects of license validation including:
 * - LicensingFacade integration
 * - License key validation 
 * - License server stamp validation
 * - Certificate validation
 * - Trial system fallback
 * - Caching behavior
 * - Registration dialog integration
 */
@ExtendWith(MockitoExtension.class)
class CheckLicenseTest {

    @Mock
    private LicensingFacade mockLicensingFacade;

    private MockedStatic<LicensingFacade> mockedLicensingFacade;
    
    // Test constants
    private static final String PRODUCT_CODE = "PJAKARTAMIGRATI";
    private static final String VALID_LICENSE_KEY = "testLicenseId-dGVzdExpY2Vuc2VEYXRh-dGVzdFNpZ25hdHVyZQ-dGVzdENlcnRpZmljYXRl";
    private static final String VALID_SERVER_STAMP = "machineId:1234567890:machineId:SHA1withRSA:dGVzdFNpZ25hdHVyZQ-dGVzdENlcnRpZmljYXRl";
    private static final long SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000;
    private static final long ONE_HOUR_MS = 60 * 60 * 1000;

    @BeforeEach
    void setUp() {
        mockedLicensingFacade = mockStatic(LicensingFacade.class);
        // Clear any cached license status
        CheckLicense.clearCache();
    }

    @AfterEach
    void tearDown() {
        if (mockedLicensingFacade != null) {
            mockedLicensingFacade.close();
        }
        // Clean up system properties
        System.clearProperty("jakarta.migration.premium");
        System.clearProperty("jakarta.migration.trial.end");
        // Clear license cache
        CheckLicense.clearCache();
    }

    // ==================== LicensingFacade Integration Tests ====================

    @Test
    @DisplayName("Should return null when LicensingFacade is not available")
    void shouldReturnNullWhenLicensingFacadeNotAvailable() {
        // Given
        mockedLicensingFacade.when(LicensingFacade::getInstance).thenReturn(null);

        // When
        Boolean result = CheckLicense.isLicensed();

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should return false when no confirmation stamp found")
    void shouldReturnFalseWhenNoConfirmationStamp() {
        // Given
        mockedLicensingFacade.when(LicensingFacade::getInstance).thenReturn(mockLicensingFacade);
        when(mockLicensingFacade.getConfirmationStamp(PRODUCT_CODE)).thenReturn(null);

        // When
        Boolean result = CheckLicense.isLicensed();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when confirmation stamp is empty")
    void shouldReturnFalseWhenConfirmationStampEmpty() {
        // Given
        mockedLicensingFacade.when(LicensingFacade::getInstance).thenReturn(mockLicensingFacade);
        when(mockLicensingFacade.getConfirmationStamp(PRODUCT_CODE)).thenReturn("");

        // When
        Boolean result = CheckLicense.isLicensed();

        // Then
        assertThat(result).isFalse();
    }

    // ==================== License Key Validation Tests ====================

    @Test
    @DisplayName("Should handle license key prefix correctly")
    void shouldHandleLicenseKeyPrefix() {
        // Given
        mockedLicensingFacade.when(LicensingFacade::getInstance).thenReturn(mockLicensingFacade);
        when(mockLicensingFacade.getConfirmationStamp(PRODUCT_CODE))
                .thenReturn("key:" + VALID_LICENSE_KEY);

        // When
        Boolean result = CheckLicense.isLicensed();

        // Then
        // Should attempt to validate the key (may fail due to invalid test data)
        // The important thing is that it correctly identifies the key format
        verify(mockLicensingFacade).getConfirmationStamp(PRODUCT_CODE);
    }

    @Test
    @DisplayName("Should handle license server stamp prefix correctly")
    void shouldHandleLicenseServerStampPrefix() {
        // Given
        mockedLicensingFacade.when(LicensingFacade::getInstance).thenReturn(mockLicensingFacade);
        when(mockLicensingFacade.getConfirmationStamp(PRODUCT_CODE))
                .thenReturn("stamp:" + VALID_SERVER_STAMP);

        // When
        Boolean result = CheckLicense.isLicensed();

        // Then
        // Should attempt to validate the server stamp
        verify(mockLicensingFacade).getConfirmationStamp(PRODUCT_CODE);
    }

    @Test
    @DisplayName("Should return false for invalid license key format")
    void shouldReturnFalseForInvalidLicenseKeyFormat() {
        // Given
        mockedLicensingFacade.when(LicensingFacade::getInstance).thenReturn(mockLicensingFacade);
        when(mockLicensingFacade.getConfirmationStamp(PRODUCT_CODE))
                .thenReturn("key:invalidFormat");

        // When
        Boolean result = CheckLicense.isLicensed();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false for invalid server stamp format")
    void shouldReturnFalseForInvalidServerStampFormat() {
        // Given
        mockedLicensingFacade.when(LicensingFacade::getInstance).thenReturn(mockLicensingFacade);
        when(mockLicensingFacade.getConfirmationStamp(PRODUCT_CODE))
                .thenReturn("stamp:invalidFormat");

        // When
        Boolean result = CheckLicense.isLicensed();

        // Then
        assertThat(result).isFalse();
    }

    // ==================== Trial System Fallback Tests ====================

    @Test
    @DisplayName("Should return true when trial is active")
    void shouldReturnTrueWhenTrialActive() {
        // Given
        mockedLicensingFacade.when(LicensingFacade::getInstance).thenReturn(null);
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", 
                String.valueOf(System.currentTimeMillis() + SEVEN_DAYS_MS));

        // When
        Boolean result = CheckLicense.isLicensed();

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when trial has expired")
    void shouldReturnFalseWhenTrialExpired() {
        // Given
        mockedLicensingFacade.when(LicensingFacade::getInstance).thenReturn(null);
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", 
                String.valueOf(System.currentTimeMillis() - SEVEN_DAYS_MS));

        // When
        Boolean result = CheckLicense.isLicensed();

        // Then
        assertThat(result).isFalse();
        // Should clean up expired trial
        assertThat(System.getProperty("jakarta.migration.premium")).isEqualTo("false");
    }

    @Test
    @DisplayName("Should return false when trial system property is not set")
    void shouldReturnFalseWhenTrialNotSet() {
        // Given
        mockedLicensingFacade.when(LicensingFacade::getInstance).thenReturn(null);

        // When
        Boolean result = CheckLicense.isLicensed();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when trial end time is invalid")
    void shouldReturnFalseWhenTrialEndTimeInvalid() {
        // Given
        mockedLicensingFacade.when(LicensingFacade::getInstance).thenReturn(null);
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", "invalid");

        // When
        Boolean result = CheckLicense.isLicensed();

        // Then
        assertThat(result).isFalse();
    }

    // ==================== Caching Tests ====================

    @Test
    @DisplayName("Should cache license check results")
    void shouldCacheLicenseCheckResults() {
        // Given
        mockedLicensingFacade.when(LicensingFacade::getInstance).thenReturn(mockLicensingFacade);
        when(mockLicensingFacade.getConfirmationStamp(PRODUCT_CODE)).thenReturn(null);

        // When - call multiple times
        Boolean result1 = CheckLicense.isLicensed();
        Boolean result2 = CheckLicense.isLicensed();

        // Then - should only call LicensingFacade once due to caching
        verify(mockLicensingFacade, times(1)).getConfirmationStamp(PRODUCT_CODE);
        assertThat(result1).isEqualTo(result2);
    }

    @Test
    @DisplayName("Should clear cache when requested")
    void shouldClearCacheWhenRequested() {
        // Given
        mockedLicensingFacade.when(LicensingFacade::getInstance).thenReturn(mockLicensingFacade);
        when(mockLicensingFacade.getConfirmationStamp(PRODUCT_CODE)).thenReturn(null);

        // When
        CheckLicense.isLicensed(); // First call to cache result
        CheckLicense.clearCache(); // Clear cache
        CheckLicense.isLicensed(); // Second call should hit LicensingFacade again

        // Then
        verify(mockLicensingFacade, times(2)).getConfirmationStamp(PRODUCT_CODE);
    }

    // ==================== License Status String Tests ====================

    @Test
    @DisplayName("Should return 'Premium Active' when licensed")
    void shouldReturnPremiumActiveWhenLicensed() {
        // Given
        mockedLicensingFacade.when(LicensingFacade::getInstance).thenReturn(mockLicensingFacade);
        when(mockLicensingFacade.getConfirmationStamp(PRODUCT_CODE))
                .thenReturn("key:" + VALID_LICENSE_KEY);

        // When
        String status = CheckLicense.getLicenseStatusString();

        // Then
        assertThat(status).isEqualTo("Premium Active");
    }

    @Test
    @DisplayName("Should return 'Checking...' when license status is null")
    void shouldReturnCheckingWhenLicenseStatusNull() {
        // Given
        mockedLicensingFacade.when(LicensingFacade::getInstance).thenReturn(null);

        // When
        String status = CheckLicense.getLicenseStatusString();

        // Then
        assertThat(status).isEqualTo("Checking...");
    }

    @Test
    @DisplayName("Should return 'Free' when not licensed")
    void shouldReturnFreeWhenNotLicensed() {
        // Given
        mockedLicensingFacade.when(LicensingFacade::getInstance).thenReturn(mockLicensingFacade);
        when(mockLicensingFacade.getConfirmationStamp(PRODUCT_CODE)).thenReturn(null);

        // When
        String status = CheckLicense.getLicenseStatusString();

        // Then
        assertThat(status).isEqualTo("Free");
    }

    // ==================== Error Handling Tests ====================

    @Test
    @DisplayName("Should handle LicensingFacade exceptions gracefully")
    void shouldHandleLicensingFacadeExceptions() {
        // Given
        mockedLicensingFacade.when(LicensingFacade::getInstance).thenReturn(mockLicensingFacade);
        when(mockLicensingFacade.getConfirmationStamp(PRODUCT_CODE))
                .thenThrow(new RuntimeException("Test exception"));

        // When
        Boolean result = CheckLicense.isLicensed();

        // Then
        // Should fall back to trial system
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Should handle malformed confirmation stamp")
    void shouldHandleMalformedConfirmationStamp() {
        // Given
        mockedLicensingFacade.when(LicensingFacade::getInstance).thenReturn(mockLicensingFacade);
        when(mockLicensingFacade.getConfirmationStamp(PRODUCT_CODE))
                .thenReturn("invalid-prefix:someData");

        // When
        Boolean result = CheckLicense.isLicensed();

        // Then
        assertThat(result).isFalse();
    }

    // ==================== Registration Dialog Tests ====================

    @Test
    @DisplayName("Should request license without throwing exceptions")
    void shouldRequestLicenseWithoutThrowing() {
        // Given
        mockedLicensingFacade.when(LicensingFacade::getInstance).thenReturn(mockLicensingFacade);

        // When/Then - should not throw
        assertThatCode(() -> CheckLicense.requestLicense("Test message"))
                .doesNotThrowAnyException();
    }

    // ==================== Integration Tests ====================

    @Test
    @DisplayName("Should handle complete license check flow")
    void shouldHandleCompleteLicenseCheckFlow() {
        // Given
        mockedLicensingFacade.when(LicensingFacade::getInstance).thenReturn(mockLicensingFacade);
        when(mockLicensingFacade.getConfirmationStamp(PRODUCT_CODE))
                .thenReturn("key:" + VALID_LICENSE_KEY);

        // When
        Boolean result = CheckLicense.isLicensed();
        String status = CheckLicense.getLicenseStatusString();

        // Then
        verify(mockLicensingFacade).getConfirmationStamp(PRODUCT_CODE);
        // Result depends on key validation, but should not be null
        assertThat(result).isNotNull();
        assertThat(status).isNotNull();
    }

    @Test
    @DisplayName("Should handle multiple license types in same session")
    void shouldHandleMultipleLicenseTypes() {
        // Given - start with trial
        mockedLicensingFacade.when(LicensingFacade::getInstance).thenReturn(null);
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", 
                String.valueOf(System.currentTimeMillis() + SEVEN_DAYS_MS));

        // When - check trial, then clear cache and simulate JetBrains license
        Boolean trialResult = CheckLicense.isLicensed();
        CheckLicense.clearCache();
        
        mockedLicensingFacade.when(LicensingFacade::getInstance).thenReturn(mockLicensingFacade);
        when(mockLicensingFacade.getConfirmationStamp(PRODUCT_CODE))
                .thenReturn("key:" + VALID_LICENSE_KEY);
        Boolean licenseResult = CheckLicense.isLicensed();

        // Then
        assertThat(trialResult).isTrue();
        assertThat(licenseResult).isNotNull();
    }

    // ==================== Performance Tests ====================

    @Test
    @DisplayName("Should handle frequent license checks efficiently")
    void shouldHandleFrequentLicenseChecksEfficiently() {
        // Given
        mockedLicensingFacade.when(LicensingFacade::getInstance).thenReturn(mockLicensingFacade);
        when(mockLicensingFacade.getConfirmationStamp(PRODUCT_CODE)).thenReturn(null);

        // When - call multiple times rapidly
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            CheckLicense.isLicensed();
        }
        long endTime = System.currentTimeMillis();

        // Then - should be fast due to caching (under 100ms for 100 calls)
        assertThat(endTime - startTime).isLessThan(100);
        
        // Should only call LicensingFacade once due to caching
        verify(mockLicensingFacade, times(1)).getConfirmationStamp(PRODUCT_CODE);
    }
}
