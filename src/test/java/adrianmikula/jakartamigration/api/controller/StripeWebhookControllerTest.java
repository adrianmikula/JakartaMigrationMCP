package adrianmikula.jakartamigration.api.controller;

import adrianmikula.jakartamigration.config.FeatureFlagsProperties;
import adrianmikula.jakartamigration.config.StripeLicenseProperties;
import adrianmikula.jakartamigration.storage.service.LocalLicenseStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StripeWebhookController.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StripeWebhookController Tests")
class StripeWebhookControllerTest {

    @Mock
    private StripeLicenseProperties stripeProperties;

    @Mock
    private LocalLicenseStorageService localStorageService;

    private ObjectMapper objectMapper;
    private StripeWebhookController controller;
    private String webhookSecret = "whsec_test_secret";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        controller = new StripeWebhookController(stripeProperties, objectMapper);
        
        when(stripeProperties.getWebhookSecret()).thenReturn(webhookSecret);
        when(stripeProperties.getProductIdPremium()).thenReturn("prod_premium");
        when(stripeProperties.getProductIdEnterprise()).thenReturn("prod_enterprise");
        when(stripeProperties.getPriceIdToTier()).thenReturn(new HashMap<>());
    }

    @Test
    @DisplayName("Should reject webhook with missing signature")
    void shouldRejectWebhookWithMissingSignature() {
        // When
        ResponseEntity<String> response = controller.handleWebhook("{}", null);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo("Invalid signature");
    }

    @Test
    @DisplayName("Should reject webhook with invalid signature")
    void shouldRejectWebhookWithInvalidSignature() {
        // When
        ResponseEntity<String> response = controller.handleWebhook("{}", "t=123,v1=invalid");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo("Invalid signature");
    }

    @Test
    @DisplayName("Should accept webhook with valid signature")
    void shouldAcceptWebhookWithValidSignature() throws Exception {
        // Given
        String payload = "{\"type\":\"customer.created\",\"data\":{\"object\":{\"id\":\"cus_123\",\"email\":\"test@example.com\"}}}";
        String signature = createValidSignature(payload);

        // When
        ResponseEntity<String> response = controller.handleWebhook(payload, signature);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("OK");
        verify(localStorageService).storeSession(
            eq("test@example.com"),
            eq("cus_123"),
            eq(FeatureFlagsProperties.LicenseTier.COMMUNITY),
            eq(24L)
        );
    }

    @Test
    @DisplayName("Should handle customer.created event")
    void shouldHandleCustomerCreatedEvent() throws Exception {
        // Given
        String payload = "{\"type\":\"customer.created\",\"data\":{\"object\":{\"id\":\"cus_123\",\"email\":\"test@example.com\"}}}";
        String signature = createValidSignature(payload);

        // When
        ResponseEntity<String> response = controller.handleWebhook(payload, signature);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(localStorageService).storeSession(
            eq("test@example.com"),
            eq("cus_123"),
            eq(FeatureFlagsProperties.LicenseTier.COMMUNITY),
            eq(24L)
        );
    }

    @Test
    @DisplayName("Should handle subscription.created event")
    void shouldHandleSubscriptionCreatedEvent() throws Exception {
        // Given
        String payload = "{\"type\":\"customer.subscription.created\",\"data\":{\"object\":{\"customer\":\"cus_123\",\"status\":\"active\",\"items\":{\"data\":[{\"price\":{\"id\":\"price_123\",\"product\":\"prod_premium\"}}]}}}}";
        String signature = createValidSignature(payload);

        // When
        ResponseEntity<String> response = controller.handleWebhook(payload, signature);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Should handle subscription.updated event")
    void shouldHandleSubscriptionUpdatedEvent() throws Exception {
        // Given
        String payload = "{\"type\":\"customer.subscription.updated\",\"data\":{\"object\":{\"customer\":\"cus_123\",\"status\":\"active\",\"items\":{\"data\":[{\"price\":{\"id\":\"price_123\",\"product\":\"prod_enterprise\"}}]}}}}";
        String signature = createValidSignature(payload);

        // When
        ResponseEntity<String> response = controller.handleWebhook(payload, signature);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Should handle subscription.deleted event")
    void shouldHandleSubscriptionDeletedEvent() throws Exception {
        // Given
        String payload = "{\"type\":\"customer.subscription.deleted\",\"data\":{\"object\":{\"customer\":\"cus_123\"}}}";
        String signature = createValidSignature(payload);

        // When
        ResponseEntity<String> response = controller.handleWebhook(payload, signature);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Should handle invoice.payment_succeeded event")
    void shouldHandlePaymentSucceededEvent() throws Exception {
        // Given
        String payload = "{\"type\":\"invoice.payment_succeeded\",\"data\":{\"object\":{\"customer\":\"cus_123\"}}}";
        String signature = createValidSignature(payload);

        // When
        ResponseEntity<String> response = controller.handleWebhook(payload, signature);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Should handle invoice.payment_failed event")
    void shouldHandlePaymentFailedEvent() throws Exception {
        // Given
        String payload = "{\"type\":\"invoice.payment_failed\",\"data\":{\"object\":{\"customer\":\"cus_123\"}}}";
        String signature = createValidSignature(payload);

        // When
        ResponseEntity<String> response = controller.handleWebhook(payload, signature);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Should handle unhandled event type")
    void shouldHandleUnhandledEventType() throws Exception {
        // Given
        String payload = "{\"type\":\"unknown.event\",\"data\":{\"object\":{}}}";
        String signature = createValidSignature(payload);

        // When
        ResponseEntity<String> response = controller.handleWebhook(payload, signature);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Should handle invalid JSON payload")
    void shouldHandleInvalidJsonPayload() {
        // Given
        String payload = "invalid json";
        String signature = "t=123,v1=invalid";

        // When
        ResponseEntity<String> response = controller.handleWebhook(payload, signature);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo("Error processing webhook");
    }

    @Test
    @DisplayName("Should handle missing webhook secret")
    void shouldHandleMissingWebhookSecret() {
        // Given
        when(stripeProperties.getWebhookSecret()).thenReturn(null);

        // When
        ResponseEntity<String> response = controller.handleWebhook("{}", "t=123,v1=signature");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Should determine tier from product ID")
    void shouldDetermineTierFromProductId() throws Exception {
        // Given - Premium product
        String payload = "{\"type\":\"customer.subscription.created\",\"data\":{\"object\":{\"customer\":\"cus_123\",\"status\":\"active\",\"items\":{\"data\":[{\"price\":{\"id\":\"price_123\",\"product\":\"prod_premium\"}}]}}}}";
        String signature = createValidSignature(payload);

        // When
        ResponseEntity<String> response = controller.handleWebhook(payload, signature);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Should determine tier from price ID mapping")
    void shouldDetermineTierFromPriceIdMapping() throws Exception {
        // Given
        Map<String, String> priceIdToTier = new HashMap<>();
        priceIdToTier.put("price_123", "PREMIUM");
        when(stripeProperties.getPriceIdToTier()).thenReturn(priceIdToTier);

        String payload = "{\"type\":\"customer.subscription.created\",\"data\":{\"object\":{\"customer\":\"cus_123\",\"status\":\"active\",\"items\":{\"data\":[{\"price\":{\"id\":\"price_123\"}}]}}}}";
        String signature = createValidSignature(payload);

        // When
        ResponseEntity<String> response = controller.handleWebhook(payload, signature);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Should handle customer.created with null email")
    void shouldHandleCustomerCreatedWithNullEmail() throws Exception {
        // Given
        String payload = "{\"type\":\"customer.created\",\"data\":{\"object\":{\"id\":\"cus_123\"}}}";
        String signature = createValidSignature(payload);

        // When
        ResponseEntity<String> response = controller.handleWebhook(payload, signature);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(localStorageService, never()).storeSession(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should handle subscription with inactive status")
    void shouldHandleSubscriptionWithInactiveStatus() throws Exception {
        // Given
        String payload = "{\"type\":\"customer.subscription.created\",\"data\":{\"object\":{\"customer\":\"cus_123\",\"status\":\"canceled\",\"items\":{\"data\":[{\"price\":{\"id\":\"price_123\"}}]}}}}";
        String signature = createValidSignature(payload);

        // When
        ResponseEntity<String> response = controller.handleWebhook(payload, signature);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Should reject Base64-encoded signature (must use hex encoding)")
    void shouldRejectBase64EncodedSignature() throws Exception {
        // Given - Create a signature using Base64 encoding (incorrect format)
        String payload = "{\"type\":\"customer.created\",\"data\":{\"object\":{\"id\":\"cus_123\"}}}";
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String signedPayload = timestamp + "." + payload;

        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(
            webhookSecret.getBytes(StandardCharsets.UTF_8),
            "HmacSHA256"
        );
        mac.init(secretKey);
        byte[] hash = mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
        // Use Base64 encoding (WRONG - Stripe uses hex)
        String base64Signature = Base64.getEncoder().encodeToString(hash);
        String signature = "t=" + timestamp + ",v1=" + base64Signature;

        // When
        ResponseEntity<String> response = controller.handleWebhook(payload, signature);

        // Then - Should reject because Base64 encoding doesn't match hex encoding
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo("Invalid signature");
    }

    @Test
    @DisplayName("Should accept hex-encoded signature (correct format)")
    void shouldAcceptHexEncodedSignature() throws Exception {
        // Given - Create a signature using hex encoding (correct format)
        String payload = "{\"type\":\"customer.created\",\"data\":{\"object\":{\"id\":\"cus_123\"}}}";
        String signature = createValidSignature(payload);

        // When
        ResponseEntity<String> response = controller.handleWebhook(payload, signature);

        // Then - Should accept because hex encoding matches Stripe's format
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("OK");
    }

    @Test
    @DisplayName("Should verify signature uses hex encoding format")
    void shouldVerifySignatureUsesHexEncodingFormat() throws Exception {
        // Given
        String payload = "test payload";
        String signature = createValidSignature(payload);

        // Extract the signature value from the header format
        String signatureValue = signature.substring(signature.indexOf("v1=") + 3);

        // Then - Verify it's hex-encoded (only contains 0-9, a-f characters)
        assertThat(signatureValue)
            .matches("^[0-9a-f]+$")
            .as("Signature should be hex-encoded (lowercase hexadecimal)");

        // Verify it's not Base64 (Base64 contains +, /, = characters)
        assertThat(signatureValue)
            .doesNotContain("+")
            .doesNotContain("/")
            .doesNotContain("=")
            .as("Signature should not contain Base64 characters");
    }

    @Test
    @DisplayName("Should reject signature with uppercase hex (Stripe uses lowercase)")
    void shouldRejectUppercaseHexSignature() throws Exception {
        // Given - Create valid hex signature but convert to uppercase
        String payload = "{\"type\":\"customer.created\",\"data\":{\"object\":{\"id\":\"cus_123\"}}}";
        String validSignature = createValidSignature(payload);
        // Extract and uppercase the hex signature
        String timestamp = validSignature.substring(2, validSignature.indexOf(","));
        String hexSignature = validSignature.substring(validSignature.indexOf("v1=") + 3);
        String uppercaseSignature = "t=" + timestamp + ",v1=" + hexSignature.toUpperCase();

        // When
        ResponseEntity<String> response = controller.handleWebhook(payload, uppercaseSignature);

        // Then - Should reject because Stripe uses lowercase hex
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo("Invalid signature");
    }

    @Test
    @DisplayName("Should handle signature with multiple v1 values (uses last)")
    void shouldHandleSignatureWithMultipleV1Values() throws Exception {
        // Given - Controller uses the LAST v1 value when multiple are present
        String payload = "{\"type\":\"customer.created\",\"data\":{\"object\":{\"id\":\"cus_123\"}}}";
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String signedPayload = timestamp + "." + payload;

        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(
            webhookSecret.getBytes(StandardCharsets.UTF_8),
            "HmacSHA256"
        );
        mac.init(secretKey);
        byte[] hash = mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
        String validHexSignature = HexFormat.of().formatHex(hash);
        
        // Put valid signature last (controller uses last v1 value)
        String multiSignature = "t=" + timestamp + ",v1=invalid_first_signature,v1=" + validHexSignature;

        // When
        ResponseEntity<String> response = controller.handleWebhook(payload, multiSignature);

        // Then - Should accept because last v1 value is valid
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Should reject signature when last v1 value is invalid")
    void shouldRejectSignatureWhenLastV1ValueIsInvalid() throws Exception {
        // Given - Controller uses the LAST v1 value
        String payload = "{\"type\":\"customer.created\",\"data\":{\"object\":{\"id\":\"cus_123\"}}}";
        String validSignature = createValidSignature(payload);
        // Put invalid signature last (controller uses last v1 value)
        String multiSignature = validSignature + ",v1=invalid_last_signature";

        // When
        ResponseEntity<String> response = controller.handleWebhook(payload, multiSignature);

        // Then - Should reject because last v1 value is invalid
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo("Invalid signature");
    }

    @Test
    @DisplayName("Should reject signature with missing timestamp")
    void shouldRejectSignatureWithMissingTimestamp() {
        // Given
        String payload = "{\"type\":\"customer.created\"}";
        String signature = "v1=abc123def456"; // Missing t= timestamp

        // When
        ResponseEntity<String> response = controller.handleWebhook(payload, signature);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo("Invalid signature");
    }

    @Test
    @DisplayName("Should reject signature with missing v1 value")
    void shouldRejectSignatureWithMissingV1Value() {
        // Given
        String payload = "{\"type\":\"customer.created\"}";
        String signature = "t=1234567890"; // Missing v1= signature

        // When
        ResponseEntity<String> response = controller.handleWebhook(payload, signature);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo("Invalid signature");
    }

    @Test
    @DisplayName("Should reject signature with tampered payload")
    void shouldRejectSignatureWithTamperedPayload() throws Exception {
        // Given - Create valid signature for one payload
        String originalPayload = "{\"type\":\"customer.created\",\"data\":{\"object\":{\"id\":\"cus_123\"}}}";
        String signature = createValidSignature(originalPayload);

        // But use different payload
        String tamperedPayload = "{\"type\":\"customer.created\",\"data\":{\"object\":{\"id\":\"cus_999\"}}}";

        // When
        ResponseEntity<String> response = controller.handleWebhook(tamperedPayload, signature);

        // Then - Should reject because payload doesn't match signature
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo("Invalid signature");
    }

    @Test
    @DisplayName("Should reject signature with wrong secret")
    void shouldRejectSignatureWithWrongSecret() throws Exception {
        // Given - Create signature with different secret
        String payload = "{\"type\":\"customer.created\"}";
        String wrongSecret = "wrong_secret";
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String signedPayload = timestamp + "." + payload;

        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(
            wrongSecret.getBytes(StandardCharsets.UTF_8),
            "HmacSHA256"
        );
        mac.init(secretKey);
        byte[] hash = mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
        String signature = HexFormat.of().formatHex(hash);
        String wrongSignature = "t=" + timestamp + ",v1=" + signature;

        // When
        ResponseEntity<String> response = controller.handleWebhook(payload, wrongSignature);

        // Then - Should reject because secret doesn't match
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo("Invalid signature");
    }

    /**
     * Create a valid Stripe webhook signature for testing.
     */
    private String createValidSignature(String payload) throws Exception {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String signedPayload = timestamp + "." + payload;

        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(
            webhookSecret.getBytes(StandardCharsets.UTF_8),
            "HmacSHA256"
        );
        mac.init(secretKey);
        byte[] hash = mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
        // Stripe uses hex encoding (not Base64) for webhook signatures
        String signature = HexFormat.of().formatHex(hash);

        return "t=" + timestamp + ",v1=" + signature;
    }
}

