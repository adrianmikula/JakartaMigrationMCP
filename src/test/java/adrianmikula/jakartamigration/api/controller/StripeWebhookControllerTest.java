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
        controller = new StripeWebhookController(stripeProperties, localStorageService, objectMapper);
        
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
        String signature = Base64.getEncoder().encodeToString(hash);

        return "t=" + timestamp + ",v1=" + signature;
    }
}

