package adrianmikula.jakartamigration.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for Stripe license validation.
 * 
 * Stripe is used for subscription-based license validation.
 * License keys can be Stripe customer IDs, subscription IDs, or custom license keys
 * that are validated against Stripe subscriptions.
 * 
 * Configuration example in application.yml:
 * <pre>
 * jakarta:
 *   migration:
 *     stripe:
 *       enabled: true
 *       api-url: https://api.stripe.com/v1
 *       secret-key: ${STRIPE_SECRET_KEY:}
 *       cache-ttl-seconds: 3600
 *       timeout-seconds: 5
 *       product-id-premium: prod_premium
 *       product-id-enterprise: prod_enterprise
 * </pre>
 */
@Data
@Validated
@ConfigurationProperties(prefix = "jakarta.migration.stripe")
public class StripeLicenseProperties {

    /**
     * Whether Stripe license validation is enabled.
     * When disabled, Stripe validation is skipped.
     */
    private Boolean enabled = true;

    /**
     * Stripe API base URL.
     * Default: https://api.stripe.com/v1
     */
    @NotBlank
    private String apiUrl = "https://api.stripe.com/v1";

    /**
     * Stripe secret key for API authentication.
     * Can be set via environment variable STRIPE_SECRET_KEY.
     * Use your Stripe secret key (starts with sk_live_ or sk_test_).
     */
    private String secretKey = "";

    /**
     * Cache TTL for license validation results (in seconds).
     * Prevents excessive API calls for the same license key.
     * Default: 3600 (1 hour).
     */
    private Long cacheTtlSeconds = 3600L;

    /**
     * Request timeout in seconds.
     * Default: 5 seconds.
     */
    private Integer timeoutSeconds = 5;

    /**
     * Stripe Product ID for Premium tier subscriptions.
     * Used to identify Premium tier subscriptions.
     */
    private String productIdPremium = "";

    /**
     * Stripe Product ID for Enterprise tier subscriptions.
     * Used to identify Enterprise tier subscriptions.
     */
    private String productIdEnterprise = "";

    /**
     * Price ID mappings for license tiers.
     * Maps Stripe Price IDs to license tiers.
     * Format: price_id -> tier (PREMIUM or ENTERPRISE)
     */
    private Map<String, String> priceIdToTier = new HashMap<>();

    /**
     * Whether to allow offline validation (fallback when API is unavailable).
     * When true, uses cached results or simple validation if API fails.
     */
    private Boolean allowOfflineValidation = true;

    /**
     * License key prefix for Stripe-based keys.
     * Keys starting with this prefix are treated as Stripe license keys.
     * Default: "stripe_"
     */
    private String licenseKeyPrefix = "stripe_";

    /**
     * Webhook secret for validating Stripe webhooks (future use).
     * Used to verify webhook signatures.
     */
    private String webhookSecret = "";

    /**
     * Payment Link URLs for one-time purchases.
     * Maps product/tier names to Stripe Payment Link URLs.
     * Format: product_name -> payment_link_url
     * Example:
     *   premium: https://buy.stripe.com/premium-link
     *   enterprise: https://buy.stripe.com/enterprise-link
     */
    private Map<String, String> paymentLinks = new HashMap<>();

    /**
     * Whether to enable email-based license validation.
     * When enabled, users can validate licenses using their email address
     * instead of license keys. The system checks if the email exists in Stripe customers.
     * Default: true
     */
    private Boolean enableEmailValidation = true;
}

