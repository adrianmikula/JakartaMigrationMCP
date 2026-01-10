package adrianmikula.jakartamigration.config;

/**
 * Constants for Jakarta Migration MCP Server configuration.
 * 
 * This class centralizes all hardcoded configuration strings including:
 * - Environment variable names
 * - Default URLs and endpoints
 * - Default configuration values
 * - API endpoints
 */
public final class JakartaMigrationConstants {

    private JakartaMigrationConstants() {
        // Utility class - prevent instantiation
    }

    // ============================================================================
    // Environment Variable Names
    // ============================================================================

    /**
     * Environment variable for the purchase/upgrade URL.
     * Used to configure where users can purchase premium licenses.
     */
    public static final String ENV_PURCHASE_URL = "JAKARTA_MCP_PURCHASE_URL";

    /**
     * Environment variable for the license key.
     * Users set this to their Stripe subscription ID, Apify API token, or custom license key.
     */
    public static final String ENV_LICENSE_KEY = "JAKARTA_MCP_LICENSE_KEY";

    /**
     * Environment variable for Apify API token.
     * Used by the server to make API calls to Apify for license validation.
     */
    public static final String ENV_APIFY_API_TOKEN = "APIFY_API_TOKEN";

    /**
     * Environment variable for Stripe secret key.
     * Used by the server to make API calls to Stripe for license validation.
     */
    public static final String ENV_STRIPE_SECRET_KEY = "STRIPE_SECRET_KEY";

    /**
     * Environment variable for Apify Actor ID.
     * Used to identify if running in Apify environment.
     */
    public static final String ENV_ACTOR_ID = "ACTOR_ID";

    /**
     * Environment variable for Apify Actor Run ID.
     * Used for billing and tracking execution runs.
     */
    public static final String ENV_ACTOR_RUN_ID = "ACTOR_RUN_ID";

    // ============================================================================
    // Default URLs and Endpoints
    // ============================================================================

    /**
     * Default Stripe payment link for purchasing premium licenses.
     * This is the primary payment method for local npm installations.
     */
    public static final String DEFAULT_PURCHASE_URL = "https://buy.stripe.com/00w9AU4lv5sT7lddpM2kw00";

    /**
     * Default Apify API base URL.
     * Used for license validation when using Apify-hosted service.
     */
    public static final String DEFAULT_APIFY_API_URL = "https://api.apify.com/v2";

    /**
     * Default Stripe API base URL.
     * Used for license validation when using Stripe subscriptions.
     */
    public static final String DEFAULT_STRIPE_API_URL = "https://api.stripe.com/v1";

    /**
     * Apify store page URL.
     * Used for pricing information when using Apify-hosted service.
     */
    public static final String APIFY_STORE_URL = "https://apify.com/adrian_m/jakartamigrationmcp#pricing";

    // ============================================================================
    // Default Configuration Values
    // ============================================================================

    /**
     * Default license key prefix for Stripe-based keys.
     * Keys starting with this prefix are treated as Stripe license keys.
     */
    public static final String DEFAULT_STRIPE_LICENSE_KEY_PREFIX = "stripe_";

    /**
     * Default cache TTL for license validation results (in seconds).
     * Prevents excessive API calls for the same license key.
     */
    public static final long DEFAULT_CACHE_TTL_SECONDS = 3600L;

    /**
     * Default request timeout in seconds.
     * Used for API calls to Apify and Stripe.
     */
    public static final int DEFAULT_TIMEOUT_SECONDS = 5;

    // ============================================================================
    // Stripe License Key Patterns
    // ============================================================================

    /**
     * Prefix for Stripe customer IDs.
     */
    public static final String STRIPE_CUSTOMER_PREFIX = "cus_";

    /**
     * Prefix for Stripe subscription IDs.
     */
    public static final String STRIPE_SUBSCRIPTION_PREFIX = "sub_";

    /**
     * Prefix for Stripe price IDs.
     */
    public static final String STRIPE_PRICE_PREFIX = "price_";

    // ============================================================================
    // Apify License Key Patterns
    // ============================================================================

    /**
     * Prefix for Apify API tokens.
     */
    public static final String APIFY_TOKEN_PREFIX = "apify_api_token_";

    // ============================================================================
    // License Key Patterns (Simple Validation)
    // ============================================================================

    /**
     * Prefix for simple PREMIUM test keys.
     * Format: PREMIUM-{key}
     */
    public static final String SIMPLE_PREMIUM_PREFIX = "PREMIUM-";

    /**
     * Prefix for simple ENTERPRISE test keys.
     * Format: ENTERPRISE-{key}
     */
    public static final String SIMPLE_ENTERPRISE_PREFIX = "ENTERPRISE-";

    /**
     * Prefix for Stripe PREMIUM test keys.
     * Format: stripe_PREMIUM-{key}
     */
    public static final String STRIPE_PREMIUM_PREFIX = "stripe_PREMIUM-";

    /**
     * Prefix for Stripe ENTERPRISE test keys.
     * Format: stripe_ENTERPRISE-{key}
     */
    public static final String STRIPE_ENTERPRISE_PREFIX = "stripe_ENTERPRISE-";
}

