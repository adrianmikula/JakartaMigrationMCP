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
 *
 * NOTE: These tests require IntelliJ Platform environment.
 */
@org.junit.jupiter.api.Disabled("Requires IntelliJ Platform environment - run in IDE")
class CheckLicenseCoreTest {

    private String originalMode;
    private String originalMarketplaceTest;
    private String originalDevLicenseOverride;
    private String originalEnvironment;

    @BeforeEach
    void setUp() {
        // Save original property values
        originalMode = System.getProperty("jakarta.migration.mode");
        originalMarketplaceTest = System.getProperty("jakarta.migration.marketplace.test");
        originalDevLicenseOverride = System.getProperty("dev.license.override");
        originalEnvironment = System.getProperty("environment");

        // Clear properties that would trigger dev/test mode
        System.clearProperty("jakarta.migration.mode");
        System.clearProperty("jakarta.migration.marketplace.test");
        System.clearProperty("dev.license.override");
        System.clearProperty("environment");

        CheckLicense.clearCache();
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("jakarta.migration.premium");
        System.clearProperty("jakarta.migration.trial.end");
        CheckLicense.clearCache();

        // Restore original property values
        restoreProperty("jakarta.migration.mode", originalMode);
        restoreProperty("jakarta.migration.marketplace.test", originalMarketplaceTest);
        restoreProperty("dev.license.override", originalDevLicenseOverride);
        restoreProperty("environment", originalEnvironment);
    }

    private void restoreProperty(String key, String value) {
        if (value != null) {
            System.setProperty(key, value);
        } else {
            System.clearProperty(key);
        }
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

    // ==================== License Status String Tests ====================

    @Test
    @DisplayName("Should return 'Free' when LicensingFacade is not available")
    void shouldReturnFreeWhenLicensingFacadeNotAvailable() {
        // Given - LicensingFacade not available (null)
        try (var mockedLicensingFacade = mockStatic(LicensingFacade.class)) {
            mockedLicensingFacade.when(LicensingFacade::getInstance).thenReturn(null);

            // When
            String status = CheckLicense.getLicenseStatusString();

            // Then - when LicensingFacade is null, isLicensed() returns false, so status is "Free"
            assertThat(status).isEqualTo("Free");
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

    // ==================== Caching Tests ====================

    @Test
    @DisplayName("Should cache license check results")
    void shouldCacheLicenseCheckResults() {
        // Given
        LicensingFacade mockFacade = mock(LicensingFacade.class);
        when(mockFacade.getConfirmationStamp(anyString())).thenReturn(null);

        try (var mockedLicensingFacade = mockStatic(LicensingFacade.class)) {
            mockedLicensingFacade.when(LicensingFacade::getInstance).thenReturn(mockFacade);

            // When - call multiple times
            Boolean result1 = CheckLicense.isLicensed();
            Boolean result2 = CheckLicense.isLicensed();

            // Then - should only call LicensingFacade once due to caching
            verify(mockFacade, times(1)).getConfirmationStamp(anyString());
            assertThat(result1).isEqualTo(result2);
        }
    }

    @Test
    @DisplayName("Should clear cache when requested")
    void shouldClearCacheWhenRequested() {
        // Given
        LicensingFacade mockFacade = mock(LicensingFacade.class);
        when(mockFacade.getConfirmationStamp(anyString())).thenReturn(null);

        try (var mockedLicensingFacade = mockStatic(LicensingFacade.class)) {
            mockedLicensingFacade.when(LicensingFacade::getInstance).thenReturn(mockFacade);

            // When
            CheckLicense.isLicensed(); // First call to cache result
            CheckLicense.clearCache(); // Clear cache
            CheckLicense.isLicensed(); // Second call should hit LicensingFacade again

            // Then
            verify(mockFacade, times(2)).getConfirmationStamp(anyString());
        }
    }

    // ==================== Error Handling Tests ====================

    @Test
    @DisplayName("Should handle LicensingFacade exceptions gracefully")
    void shouldHandleLicensingFacadeExceptions() {
        // Given
        LicensingFacade mockFacade = mock(LicensingFacade.class);
        when(mockFacade.getConfirmationStamp(anyString()))
                .thenThrow(new RuntimeException("Test exception"));

        try (var mockedLicensingFacade = mockStatic(LicensingFacade.class)) {
            mockedLicensingFacade.when(LicensingFacade::getInstance).thenReturn(mockFacade);

            // When
            Boolean result = CheckLicense.isLicensed();

            // Then - should handle exception and return non-null result
            assertThat(result).isNotNull();
        }
    }

    // ==================== Performance Tests ====================

    @Test
    @DisplayName("Should handle frequent license checks efficiently")
    void shouldHandleFrequentLicenseChecksEfficiently() {
        // Given
        LicensingFacade mockFacade = mock(LicensingFacade.class);
        when(mockFacade.getConfirmationStamp(anyString())).thenReturn(null);

        try (var mockedLicensingFacade = mockStatic(LicensingFacade.class)) {
            mockedLicensingFacade.when(LicensingFacade::getInstance).thenReturn(mockFacade);

            // When - call multiple times rapidly
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 100; i++) {
                CheckLicense.isLicensed();
            }
            long endTime = System.currentTimeMillis();

            // Then - should be fast due to caching (under 100ms for 100 calls)
            assertThat(endTime - startTime).isLessThan(100);

            // Should only call LicensingFacade once due to caching
            verify(mockFacade, times(1)).getConfirmationStamp(anyString());
        }
    }
}
