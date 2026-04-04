package adrianmikula.jakartamigration.intellij.license;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.intellij.ui.LicensingFacade;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

/**
 * Tests for license key and certificate validation methods.
 * These tests use reflection to test private methods in CheckLicense.
 */
class CheckLicenseValidationTest {

    private static final String VALID_LICENSE_ID = "TEST123456789";
    private static final String VALID_LICENSE_DATA = "{\"licenseId\":\"TEST123456789\",\"product\":\"PJAKARTAMIGRATI\"}";
    private static final String VALID_SIGNATURE = "dGVzdFNpZ25hdHVyZQ=="; // "testSignature" in base64
    private static final String VALID_CERTIFICATE = "dGVzdENlcnRpZmljYXRl"; // "testCertificate" in base64
    private static final String INVALID_FORMAT_KEY = "invalidFormat";
    private static final String MALFORMED_KEY = "part1-part2"; // missing parts

    @BeforeEach
    void setUp() {
        CheckLicense.clearCache();
    }

    @AfterEach
    void tearDown() {
        CheckLicense.clearCache();
    }

    // ==================== License Key Validation Tests ====================

    @Test
    @DisplayName("Should reject license key with invalid format")
    void shouldRejectInvalidLicenseKeyFormat() throws Exception {
        // Given
        Method isKeyValidMethod = CheckLicense.class.getDeclaredMethod("isKeyValid", String.class);
        isKeyValidMethod.setAccessible(true);

        // When
        Boolean result = (Boolean) isKeyValidMethod.invoke(null, INVALID_FORMAT_KEY);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should reject license key with wrong number of parts")
    void shouldRejectLicenseKeyWithWrongParts() throws Exception {
        // Given
        Method isKeyValidMethod = CheckLicense.class.getDeclaredMethod("isKeyValid", String.class);
        isKeyValidMethod.setAccessible(true);

        // When
        Boolean result = (Boolean) isKeyValidMethod.invoke(null, MALFORMED_KEY);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should handle license key validation exceptions gracefully")
    void shouldHandleLicenseKeyValidationExceptions() throws Exception {
        // Given
        Method isKeyValidMethod = CheckLicense.class.getDeclaredMethod("isKeyValid", String.class);
        isKeyValidMethod.setAccessible(true);
        
        // Create a key that will cause validation to fail
        String problematicKey = VALID_LICENSE_ID + "-" + VALID_LICENSE_DATA + "-" + VALID_SIGNATURE + "-" + VALID_CERTIFICATE;

        // When
        Boolean result = (Boolean) isKeyValidMethod.invoke(null, problematicKey);

        // Then
        // Should return false rather than throwing exception
        assertThat(result).isFalse();
    }

    // ==================== License Server Stamp Validation Tests ====================

    @Test
    @DisplayName("Should reject server stamp with invalid format")
    void shouldRejectInvalidServerStampFormat() throws Exception {
        // Given
        Method isLicenseServerStampValidMethod = CheckLicense.class.getDeclaredMethod("isLicenseServerStampValid", String.class);
        isLicenseServerStampValidMethod.setAccessible(true);

        // When
        Boolean result = (Boolean) isLicenseServerStampValidMethod.invoke(null, "invalidFormat");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should reject server stamp with insufficient parts")
    void shouldRejectServerStampWithInsufficientParts() throws Exception {
        // Given
        Method isLicenseServerStampValidMethod = CheckLicense.class.getDeclaredMethod("isLicenseServerStampValid", String.class);
        isLicenseServerStampValidMethod.setAccessible(true);

        // When
        Boolean result = (Boolean) isLicenseServerStampValidMethod.invoke(null, "part1:part2:part3");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should reject server stamp with invalid timestamp")
    void shouldRejectServerStampWithInvalidTimestamp() throws Exception {
        // Given
        Method isLicenseServerStampValidMethod = CheckLicense.class.getDeclaredMethod("isLicenseServerStampValid", String.class);
        isLicenseServerStampValidMethod.setAccessible(true);
        
        String stampWithInvalidTimestamp = "machineId:invalidTimestamp:machineId:SHA1withRSA:signature:certificate";

        // When
        Boolean result = (Boolean) isLicenseServerStampValidMethod.invoke(null, stampWithInvalidTimestamp);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should reject server stamp with expired timestamp")
    void shouldRejectServerStampWithExpiredTimestamp() throws Exception {
        // Given
        Method isLicenseServerStampValidMethod = CheckLicense.class.getDeclaredMethod("isLicenseServerStampValid", String.class);
        isLicenseServerStampValidMethod.setAccessible(true);
        
        // Create timestamp from 2 hours ago (beyond 1-hour validity window)
        long expiredTimestamp = System.currentTimeMillis() - (2 * 60 * 60 * 1000);
        String expiredStamp = "machineId:" + expiredTimestamp + ":machineId:SHA1withRSA:signature:certificate";

        // When
        Boolean result = (Boolean) isLicenseServerStampValidMethod.invoke(null, expiredStamp);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should handle server stamp validation exceptions gracefully")
    void shouldHandleServerStampValidationExceptions() throws Exception {
        // Given
        Method isLicenseServerStampValidMethod = CheckLicense.class.getDeclaredMethod("isLicenseServerStampValid", String.class);
        isLicenseServerStampValidMethod.setAccessible(true);
        
        // Create a stamp that will cause validation to fail
        String problematicStamp = "machineId:1234567890:machineId:InvalidAlgorithm:signature:certificate";

        // When
        Boolean result = (Boolean) isLicenseServerStampValidMethod.invoke(null, problematicStamp);

        // Then
        // Should return false rather than throwing exception
        assertThat(result).isFalse();
    }

    // ==================== Certificate Validation Tests ====================

    @Test
    @DisplayName("Should handle certificate creation exceptions")
    void shouldHandleCertificateCreationExceptions() throws Exception {
        // Given
        Method createCertificateMethod = CheckLicense.class.getDeclaredMethod("createCertificate", byte[].class, java.util.Collection.class, boolean.class);
        createCertificateMethod.setAccessible(true);
        
        byte[] invalidCertBytes = "invalid certificate data".getBytes(StandardCharsets.UTF_8);

        // When/Then
        assertThatCode(() -> createCertificateMethod.invoke(null, invalidCertBytes, java.util.Collections.emptySet(), false))
                .hasCauseInstanceOf(Exception.class);
    }

    // ==================== Integration Validation Tests ====================

    @Test
    @DisplayName("Should validate license ID in license data")
    void shouldValidateLicenseIdInLicenseData() throws Exception {
        // Given
        Method isKeyValidMethod = CheckLicense.class.getDeclaredMethod("isKeyValid", String.class);
        isKeyValidMethod.setAccessible(true);
        
        // Create a key with mismatched license ID
        String mismatchedLicenseData = "{\"licenseId\":\"DIFFERENT_ID\",\"product\":\"PJAKARTAMIGRATI\"}";
        String keyWithMismatchedId = VALID_LICENSE_ID + "-" + 
                Base64.getMimeEncoder().encodeToString(mismatchedLicenseData.getBytes()) + 
                "-" + VALID_SIGNATURE + "-" + VALID_CERTIFICATE;

        // When
        Boolean result = (Boolean) isKeyValidMethod.invoke(null, keyWithMismatchedId);

        // Then
        // Should reject due to license ID mismatch
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should handle machine ID mismatch in server stamp")
    void shouldHandleMachineIdMismatch() throws Exception {
        // Given
        Method isLicenseServerStampValidMethod = CheckLicense.class.getDeclaredMethod("isLicenseServerStampValid", String.class);
        isLicenseServerStampValidMethod.setAccessible(true);
        
        long currentTimestamp = System.currentTimeMillis();
        // Create stamp with mismatched machine IDs
        String stampWithMismatchedIds = "expectedMachineId:" + currentTimestamp + ":differentMachineId:SHA1withRSA:signature:certificate";

        // When
        Boolean result = (Boolean) isLicenseServerStampValidMethod.invoke(null, stampWithMismatchedIds);

        // Then
        assertThat(result).isFalse();
    }

    // ==================== Edge Case Tests ====================

    @Test
    @DisplayName("Should handle empty license key")
    void shouldHandleEmptyLicenseKey() throws Exception {
        // Given
        Method isKeyValidMethod = CheckLicense.class.getDeclaredMethod("isKeyValid", String.class);
        isKeyValidMethod.setAccessible(true);

        // When
        Boolean result = (Boolean) isKeyValidMethod.invoke(null, "");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should handle null license key")
    void shouldHandleNullLicenseKey() throws Exception {
        // Given
        Method isKeyValidMethod = CheckLicense.class.getDeclaredMethod("isKeyValid", String.class);
        isKeyValidMethod.setAccessible(true);

        // When
        Boolean result = (Boolean) isKeyValidMethod.invoke(null, (String) null);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should handle empty server stamp")
    void shouldHandleEmptyServerStamp() throws Exception {
        // Given
        Method isLicenseServerStampValidMethod = CheckLicense.class.getDeclaredMethod("isLicenseServerStampValid", String.class);
        isLicenseServerStampValidMethod.setAccessible(true);

        // When
        Boolean result = (Boolean) isLicenseServerStampValidMethod.invoke(null, "");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should handle null server stamp")
    void shouldHandleNullServerStamp() throws Exception {
        // Given
        Method isLicenseServerStampValidMethod = CheckLicense.class.getDeclaredMethod("isLicenseServerStampValid", String.class);
        isLicenseServerStampValidMethod.setAccessible(true);

        // When
        Boolean result = (Boolean) isLicenseServerStampValidMethod.invoke(null, (String) null);

        // Then
        assertThat(result).isFalse();
    }

    // ==================== Base64 Encoding Tests ====================

    @Test
    @DisplayName("Should handle invalid base64 in license key")
    void shouldHandleInvalidBase64InLicenseKey() throws Exception {
        // Given
        Method isKeyValidMethod = CheckLicense.class.getDeclaredMethod("isKeyValid", String.class);
        isKeyValidMethod.setAccessible(true);
        
        String keyWithInvalidBase64 = VALID_LICENSE_ID + "-invalidBase64-" + VALID_SIGNATURE + "-" + VALID_CERTIFICATE;

        // When
        Boolean result = (Boolean) isKeyValidMethod.invoke(null, keyWithInvalidBase64);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should handle invalid base64 in server stamp")
    void shouldHandleInvalidBase64InServerStamp() throws Exception {
        // Given
        Method isLicenseServerStampValidMethod = CheckLicense.class.getDeclaredMethod("isLicenseServerStampValid", String.class);
        isLicenseServerStampValidMethod.setAccessible(true);
        
        long currentTimestamp = System.currentTimeMillis();
        String stampWithInvalidBase64 = "machineId:" + currentTimestamp + ":machineId:SHA1withRSA:invalidBase64:certificate";

        // When
        Boolean result = (Boolean) isLicenseServerStampValidMethod.invoke(null, stampWithInvalidBase64);

        // Then
        assertThat(result).isFalse();
    }
}
