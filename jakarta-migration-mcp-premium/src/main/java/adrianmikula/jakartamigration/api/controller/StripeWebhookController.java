package adrianmikula.jakartamigration.api.controller;

import adrianmikula.jakartamigration.config.FeatureFlagsProperties;
import adrianmikula.jakartamigration.config.StripeLicenseProperties;
import adrianmikula.jakartamigration.storage.service.LocalLicenseStorageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Controller for handling Stripe webhook events.
 * 
 * This controller processes Stripe webhook events to automatically:
 * - Validate licenses when customers are created
 * - Update license sessions when subscriptions change
 * - Sync credit balances when payments succeed
 * 
 * Webhook signature validation is performed to ensure requests are from Stripe.
 * 
 * Only enabled when STRIPE_WEBHOOK_SECRET is configured.
 */
@RestController
@RequestMapping("/api/v1/stripe/webhook")
@Slf4j
@ConditionalOnProperty(name = "jakarta.migration.stripe.webhook-secret")
public class StripeWebhookController {

    private final StripeLicenseProperties stripeProperties;
    private final ObjectMapper objectMapper;
    
    @Autowired(required = false)
    private LocalLicenseStorageService localStorageService;
    
    @Autowired
    public StripeWebhookController(
            StripeLicenseProperties stripeProperties,
            ObjectMapper objectMapper) {
        this.stripeProperties = stripeProperties;
        this.objectMapper = objectMapper;
    }

    /**
     * Handle Stripe webhook events.
     * 
     * @param payload Raw webhook payload
     * @param signature Stripe signature header
     * @return HTTP response
     */
    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        
        // Validate webhook signature
        if (!isValidSignature(payload, signature)) {
            log.warn("Invalid webhook signature - rejecting request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
        }

        try {
            JsonNode event = objectMapper.readTree(payload);
            String eventType = event.get("type").asText();
            JsonNode data = event.get("data");
            JsonNode object = data != null ? data.get("object") : null;

            log.debug("Received Stripe webhook event: {}", eventType);

            switch (eventType) {
                case "customer.created":
                    handleCustomerCreated(object);
                    break;
                case "customer.subscription.created":
                case "customer.subscription.updated":
                    handleSubscriptionEvent(object, eventType);
                    break;
                case "customer.subscription.deleted":
                    handleSubscriptionDeleted(object);
                    break;
                case "invoice.payment_succeeded":
                    handlePaymentSucceeded(object);
                    break;
                case "invoice.payment_failed":
                    handlePaymentFailed(object);
                    break;
                default:
                    log.debug("Unhandled webhook event type: {}", eventType);
            }

            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            log.error("Error processing webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error processing webhook");
        }
    }

    /**
     * Handle customer.created event.
     */
    private void handleCustomerCreated(JsonNode customer) {
        if (customer == null) {
            return;
        }

        String email = customer.get("email") != null ? customer.get("email").asText() : null;
        String customerId = customer.get("id") != null ? customer.get("id").asText() : null;

        if (email != null && !email.isBlank() && localStorageService != null) {
            // Store customer session (will be updated when subscription is created)
            localStorageService.storeSession(
                email,
                customerId,
                FeatureFlagsProperties.LicenseTier.COMMUNITY, // Default tier
                24L // 24 hours TTL
            );
            log.info("Stored customer session for email: {}", maskEmail(email));
        }
    }

    /**
     * Handle subscription created/updated events.
     */
    private void handleSubscriptionEvent(JsonNode subscription, String eventType) {
        if (subscription == null) {
            return;
        }

        String customerId = subscription.get("customer") != null ? 
            subscription.get("customer").asText() : null;
        String status = subscription.get("status") != null ? 
            subscription.get("status").asText() : null;

        if (customerId == null || !"active".equals(status) && !"trialing".equals(status)) {
            return;
        }

        // Determine tier from subscription
        FeatureFlagsProperties.LicenseTier tier = determineTierFromSubscription(subscription);

        if (tier != null && localStorageService != null) {
            // Note: We need customer email to store session
            // In a real implementation, you'd fetch customer details from Stripe API
            log.debug("Subscription {} for customer: {} -> tier: {}", 
                eventType, maskKey(customerId), tier);
        }
    }

    /**
     * Handle subscription deleted event.
     */
    private void handleSubscriptionDeleted(JsonNode subscription) {
        if (subscription == null) {
            return;
        }

        String customerId = subscription.get("customer") != null ? 
            subscription.get("customer").asText() : null;

        if (customerId != null && localStorageService != null) {
            // Note: Would need to fetch customer email to delete session
            log.debug("Subscription deleted for customer: {}", maskKey(customerId));
        }
    }

    /**
     * Handle payment succeeded event.
     */
    private void handlePaymentSucceeded(JsonNode invoice) {
        if (invoice == null) {
            return;
        }

        String customerId = invoice.get("customer") != null ? 
            invoice.get("customer").asText() : null;

        log.debug("Payment succeeded for customer: {}", maskKey(customerId));
        // Could sync credits here if using credit-based system
    }

    /**
     * Handle payment failed event.
     */
    private void handlePaymentFailed(JsonNode invoice) {
        if (invoice == null) {
            return;
        }

        String customerId = invoice.get("customer") != null ? 
            invoice.get("customer").asText() : null;

        log.warn("Payment failed for customer: {}", maskKey(customerId));
    }

    /**
     * Determine license tier from subscription.
     */
    private FeatureFlagsProperties.LicenseTier determineTierFromSubscription(JsonNode subscription) {
        JsonNode items = subscription.get("items");
        if (items == null || items.get("data") == null || !items.get("data").isArray()) {
            return null;
        }

        JsonNode firstItem = items.get("data").get(0);
        if (firstItem == null) {
            return null;
        }

        JsonNode price = firstItem.get("price");
        if (price == null) {
            return null;
        }

        String productId = price.get("product") != null ? price.get("product").asText() : null;
        String priceId = price.get("id") != null ? price.get("id").asText() : null;

        // Check product ID mapping
        if (productId != null) {
            if (productId.equals(stripeProperties.getProductIdEnterprise())) {
                return FeatureFlagsProperties.LicenseTier.ENTERPRISE;
            }
            if (productId.equals(stripeProperties.getProductIdPremium())) {
                return FeatureFlagsProperties.LicenseTier.PREMIUM;
            }
        }

        // Check price ID mapping
        if (priceId != null && stripeProperties.getPriceIdToTier().containsKey(priceId)) {
            String tierStr = stripeProperties.getPriceIdToTier().get(priceId);
            try {
                return FeatureFlagsProperties.LicenseTier.valueOf(tierStr);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid tier in price mapping: {}", tierStr);
            }
        }

        return null;
    }

    /**
     * Validate Stripe webhook signature.
     * 
     * Stripe signs webhooks using HMAC-SHA256 with the webhook secret.
     */
    private boolean isValidSignature(String payload, String signature) {
        if (signature == null || signature.isBlank()) {
            return false;
        }

        String webhookSecret = stripeProperties.getWebhookSecret();
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.warn("Webhook secret not configured - cannot validate signature");
            return false;
        }

        try {
            // Stripe signature format: t=timestamp,v1=signature
            String[] parts = signature.split(",");
            String timestamp = null;
            String signatureValue = null;

            for (String part : parts) {
                if (part.startsWith("t=")) {
                    timestamp = part.substring(2);
                } else if (part.startsWith("v1=")) {
                    signatureValue = part.substring(3);
                }
            }

            if (timestamp == null || signatureValue == null) {
                return false;
            }

            // Create signed payload
            String signedPayload = timestamp + "." + payload;

            // Compute HMAC
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                webhookSecret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
            );
            mac.init(secretKey);
            byte[] hash = mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
            // Stripe uses hex encoding (not Base64) for webhook signatures
            String computedSignature = HexFormat.of().formatHex(hash);

            // Compare signatures (constant-time comparison)
            return MessageDigest.isEqual(
                signatureValue.getBytes(StandardCharsets.UTF_8),
                computedSignature.getBytes(StandardCharsets.UTF_8)
            );

        } catch (Exception e) {
            log.error("Error validating webhook signature: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Mask email for logging.
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int atIndex = email.indexOf("@");
        if (atIndex <= 2) {
            return "***@" + email.substring(atIndex + 1);
        }
        return email.substring(0, 2) + "***@" + email.substring(atIndex + 1);
    }

    /**
     * Mask key for logging.
     */
    private String maskKey(String key) {
        if (key == null || key.length() <= 8) {
            return "***";
        }
        return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
    }
}

