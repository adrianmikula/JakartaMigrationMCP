package adrianmikula.jakartamigration.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for validating license keys via Stripe API.
 * 
 * Stripe is used for subscription-based license validation.
 * This service validates license keys (Stripe customer IDs, subscription IDs, or custom keys)
 * to determine the user's license tier (COMMUNITY, PREMIUM, ENTERPRISE).
 * 
 * The validation works by:
 * 1. Checking if the license key is a Stripe customer/subscription ID
 * 2. Validating subscription status via Stripe API
 * 3. Determining tier based on subscription product/price
 * 4. Caching results to reduce API calls
 * 
 * Results are cached to reduce API calls.
 */
@Slf4j
@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = "jakarta.migration.stripe.enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class StripeLicenseService {

    private final StripeLicenseProperties properties;
    private final WebClient stripeWebClient;

    /**
     * Cache entry for license validation results.
     */
    private static class CacheEntry {
        final FeatureFlagsProperties.LicenseTier tier;
        final LocalDateTime expiresAt;

        CacheEntry(FeatureFlagsProperties.LicenseTier tier, LocalDateTime expiresAt) {
            this.tier = tier;
            this.expiresAt = expiresAt;
        }

        boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
    }

    /**
     * In-memory cache for license validation results.
     * Key: license key, Value: cached validation result
     */
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public StripeLicenseService(
        StripeLicenseProperties properties,
        @Qualifier("stripeWebClient") WebClient stripeWebClient
    ) {
        this.properties = properties;
        this.stripeWebClient = stripeWebClient;
    }

    /**
     * Validate a license key via Stripe API.
     * 
     * @param licenseKey The license key to validate
     * @return License tier if valid, null if invalid or validation fails
     */
    public FeatureFlagsProperties.LicenseTier validateLicense(String licenseKey) {
        if (licenseKey == null || licenseKey.isBlank()) {
            return null;
        }

        // Check if this looks like a Stripe license key
        if (!isStripeLicenseKey(licenseKey)) {
            return null;
        }

        // Check cache first
        CacheEntry cached = cache.get(licenseKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("Stripe license validation cache hit for key: {}", maskKey(licenseKey));
            return cached.tier;
        }

        // If Stripe validation is disabled, use simple validation
        if (!properties.getEnabled()) {
            return validateLicenseSimple(licenseKey);
        }

        try {
            // Validate via Stripe API
            FeatureFlagsProperties.LicenseTier tier = validateLicenseViaStripe(licenseKey)
                .block(Duration.ofSeconds(properties.getTimeoutSeconds()));

            // Cache the result
            if (tier != null) {
                LocalDateTime expiresAt = LocalDateTime.now()
                    .plusSeconds(properties.getCacheTtlSeconds());
                cache.put(licenseKey, new CacheEntry(tier, expiresAt));
                log.debug("Stripe license validated and cached: {} -> {}", maskKey(licenseKey), tier);
            }

            return tier;

        } catch (Exception e) {
            log.warn("Stripe license validation failed: {}", e.getMessage());

            // If offline validation is allowed, try simple validation
            if (properties.getAllowOfflineValidation()) {
                log.debug("Falling back to simple validation");
                return validateLicenseSimple(licenseKey);
            }

            return null;
        }
    }

    /**
     * Check if a license key looks like a Stripe key.
     */
    private boolean isStripeLicenseKey(String licenseKey) {
        // Check for Stripe prefix
        if (licenseKey.startsWith(properties.getLicenseKeyPrefix())) {
            return true;
        }
        
        // Check for Stripe customer ID format (cus_...)
        if (licenseKey.startsWith("cus_")) {
            return true;
        }
        
        // Check for Stripe subscription ID format (sub_...)
        if (licenseKey.startsWith("sub_")) {
            return true;
        }
        
        // Check for Stripe price ID format (price_...)
        if (licenseKey.startsWith("price_")) {
            return true;
        }
        
        return false;
    }

    /**
     * Validate license by email address.
     * Checks if the email exists in Stripe customers and has active subscriptions.
     * 
     * @param email The customer email address
     * @return Mono with license tier, or empty if invalid
     */
    public Mono<FeatureFlagsProperties.LicenseTier> validateLicenseByEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            log.debug("Invalid email format: {}", email);
            return Mono.empty();
        }

        // Check cache first
        String cacheKey = "email:" + email.toLowerCase();
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("Stripe email validation cache hit for: {}", maskEmail(email));
            return Mono.just(cached.tier);
        }

        if (!properties.getEnabled()) {
            return Mono.empty();
        }

        return validateEmailViaStripe(email)
            .doOnNext(tier -> {
                if (tier != null) {
                    LocalDateTime expiresAt = LocalDateTime.now()
                        .plusSeconds(properties.getCacheTtlSeconds());
                    cache.put(cacheKey, new CacheEntry(tier, expiresAt));
                    log.debug("Stripe email validated and cached: {} -> {}", maskEmail(email), tier);
                }
            })
            .onErrorResume(Exception.class, ex -> {
                log.warn("Stripe email validation failed: {}", ex.getMessage());
                return Mono.empty();
            });
    }

    /**
     * Validate email via Stripe API by checking if customer exists.
     */
    private Mono<FeatureFlagsProperties.LicenseTier> validateEmailViaStripe(String email) {
        return stripeWebClient
            .get()
            .uri(uriBuilder -> uriBuilder
                .path("/customers")
                .queryParam("email", email)
                .queryParam("limit", "1")
                .build())
            .header("Authorization", "Bearer " + properties.getSecretKey())
            .header("Content-Type", "application/x-www-form-urlencoded")
            .retrieve()
            .bodyToMono(StripeCustomersListResponse.class)
            .flatMap(customers -> {
                if (customers.getData() == null || customers.getData().isEmpty()) {
                    log.debug("No Stripe customer found for email: {}", maskEmail(email));
                    return Mono.empty();
                }
                
                // Found customer, check for active subscriptions
                String customerId = customers.getData().get(0).getId();
                return validateCustomer(customerId);
            })
            .retryWhen(Retry.backoff(2, Duration.ofMillis(500))
                .filter(throwable -> {
                    if (throwable instanceof WebClientResponseException ex) {
                        return ex.getStatusCode().value() >= 500;
                    }
                    return true;
                })
            )
            .onErrorResume(WebClientResponseException.class, ex -> {
                if (ex.getStatusCode().value() == 404) {
                    log.debug("Stripe customer not found for email: {}", maskEmail(email));
                    return Mono.empty();
                }
                if (ex.getStatusCode().value() == 401 || ex.getStatusCode().value() == 403) {
                    log.debug("Invalid Stripe API key");
                    return Mono.empty();
                }
                log.warn("Stripe API error: {} {}", ex.getStatusCode(), ex.getMessage());
                return Mono.empty();
            })
            .onErrorResume(Exception.class, ex -> {
                log.warn("Unexpected error validating Stripe email: {}", ex.getMessage());
                return Mono.empty();
            })
            .switchIfEmpty(Mono.empty());
    }

    /**
     * Validate license key via Stripe API.
     * 
     * @param licenseKey The license key (customer ID, subscription ID, or custom key)
     * @return Mono with license tier, or empty if invalid
     */
    private Mono<FeatureFlagsProperties.LicenseTier> validateLicenseViaStripe(String licenseKey) {
        // Try different validation strategies based on key format
        
        // Strategy 1: If it's a subscription ID, check subscription directly
        if (licenseKey.startsWith("sub_")) {
            return validateSubscription(licenseKey);
        }
        
        // Strategy 2: If it's a customer ID, find active subscriptions
        if (licenseKey.startsWith("cus_")) {
            return validateCustomer(licenseKey);
        }
        
        // Strategy 3: If it's a custom key with prefix, extract and validate
        if (licenseKey.startsWith(properties.getLicenseKeyPrefix())) {
            String extractedKey = licenseKey.substring(properties.getLicenseKeyPrefix().length());
            // Try as subscription ID first
            if (extractedKey.startsWith("sub_")) {
                return validateSubscription(extractedKey);
            }
            // Try as customer ID
            if (extractedKey.startsWith("cus_")) {
                return validateCustomer(extractedKey);
            }
        }
        
        // Unknown format
        log.debug("Unknown Stripe license key format: {}", maskKey(licenseKey));
        return Mono.empty();
    }

    /**
     * Validate a Stripe subscription ID.
     */
    private Mono<FeatureFlagsProperties.LicenseTier> validateSubscription(String subscriptionId) {
        return stripeWebClient
            .get()
            .uri("/subscriptions/" + subscriptionId)
            .header("Authorization", "Bearer " + properties.getSecretKey())
            .header("Content-Type", "application/x-www-form-urlencoded")
            .retrieve()
            .bodyToMono(StripeSubscriptionResponse.class)
            .map(sub -> determineTierFromSubscription(sub))
            .retryWhen(Retry.backoff(2, Duration.ofMillis(500))
                .filter(throwable -> {
                    if (throwable instanceof WebClientResponseException ex) {
                        return ex.getStatusCode().value() >= 500;
                    }
                    return true;
                })
                .doBeforeRetry(retrySignal -> 
                    log.debug("Retrying Stripe API call: {}", retrySignal.totalRetries() + 1))
            )
            .onErrorResume(WebClientResponseException.class, ex -> {
                if (ex.getStatusCode().value() == 404) {
                    log.debug("Stripe subscription not found: {}", maskKey(subscriptionId));
                    return Mono.empty();
                }
                if (ex.getStatusCode().value() == 401 || ex.getStatusCode().value() == 403) {
                    log.debug("Invalid Stripe API key");
                    return Mono.empty();
                }
                log.warn("Stripe API error: {} {}", ex.getStatusCode(), ex.getMessage());
                return Mono.empty();
            })
            .onErrorResume(Exception.class, ex -> {
                log.warn("Unexpected error validating Stripe subscription: {}", ex.getMessage());
                return Mono.empty();
            })
            .switchIfEmpty(Mono.empty());
    }

    /**
     * Validate a Stripe customer ID by finding active subscriptions.
     */
    private Mono<FeatureFlagsProperties.LicenseTier> validateCustomer(String customerId) {
        return stripeWebClient
            .get()
            .uri(uriBuilder -> uriBuilder
                .path("/subscriptions")
                .queryParam("customer", customerId)
                .queryParam("status", "active")
                .queryParam("limit", "1")
                .build())
            .header("Authorization", "Bearer " + properties.getSecretKey())
            .header("Content-Type", "application/x-www-form-urlencoded")
            .retrieve()
            .bodyToMono(StripeSubscriptionsListResponse.class)
            .map(list -> {
                if (list.getData() != null && !list.getData().isEmpty()) {
                    return determineTierFromSubscription(list.getData().get(0));
                }
                return null;
            })
            .retryWhen(Retry.backoff(2, Duration.ofMillis(500))
                .filter(throwable -> {
                    if (throwable instanceof WebClientResponseException ex) {
                        return ex.getStatusCode().value() >= 500;
                    }
                    return true;
                })
            )
            .onErrorResume(WebClientResponseException.class, ex -> {
                if (ex.getStatusCode().value() == 404) {
                    log.debug("Stripe customer not found: {}", maskKey(customerId));
                    return Mono.empty();
                }
                log.warn("Stripe API error: {} {}", ex.getStatusCode(), ex.getMessage());
                return Mono.empty();
            })
            .onErrorResume(Exception.class, ex -> {
                log.warn("Unexpected error validating Stripe customer: {}", ex.getMessage());
                return Mono.empty();
            })
            .switchIfEmpty(Mono.empty());
    }

    /**
     * Determine license tier from Stripe subscription.
     */
    private FeatureFlagsProperties.LicenseTier determineTierFromSubscription(StripeSubscriptionResponse subscription) {
        if (subscription == null || !"active".equals(subscription.getStatus()) && 
            !"trialing".equals(subscription.getStatus())) {
            return null;
        }

        // Check product ID mapping
        String productId = subscription.getItems() != null && 
            !subscription.getItems().getData().isEmpty() ?
            subscription.getItems().getData().get(0).getPrice().getProduct() : null;

        if (productId != null) {
            if (productId.equals(properties.getProductIdEnterprise())) {
                return FeatureFlagsProperties.LicenseTier.ENTERPRISE;
            }
            if (productId.equals(properties.getProductIdPremium())) {
                return FeatureFlagsProperties.LicenseTier.PREMIUM;
            }
        }

        // Check price ID mapping
        String priceId = subscription.getItems() != null && 
            !subscription.getItems().getData().isEmpty() ?
            subscription.getItems().getData().get(0).getPrice().getId() : null;

        if (priceId != null && properties.getPriceIdToTier().containsKey(priceId)) {
            String tierStr = properties.getPriceIdToTier().get(priceId);
            return FeatureFlagsProperties.LicenseTier.valueOf(tierStr);
        }

        // Default: active subscription = PREMIUM
        log.debug("Active Stripe subscription found, granting PREMIUM tier");
        return FeatureFlagsProperties.LicenseTier.PREMIUM;
    }

    /**
     * Simple license validation (fallback when Stripe is unavailable).
     */
    private FeatureFlagsProperties.LicenseTier validateLicenseSimple(String licenseKey) {
        // Simple pattern matching for test keys
        if (licenseKey.startsWith("stripe_PREMIUM-")) {
            return FeatureFlagsProperties.LicenseTier.PREMIUM;
        }
        if (licenseKey.startsWith("stripe_ENTERPRISE-")) {
            return FeatureFlagsProperties.LicenseTier.ENTERPRISE;
        }
        return null;
    }

    /**
     * Clear the validation cache.
     */
    public void clearCache() {
        cache.clear();
        log.debug("Stripe license validation cache cleared");
    }

    /**
     * Clear cache entry for a specific license key.
     */
    public void clearCache(String licenseKey) {
        cache.remove(licenseKey);
        log.debug("Cache cleared for license key: {}", maskKey(licenseKey));
    }

    /**
     * Mask license key for logging.
     */
    private String maskKey(String key) {
        if (key == null || key.length() <= 8) {
            return "***";
        }
        return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
    }

    /**
     * Mask email for logging (shows only first part and domain).
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
     * Stripe API subscription response DTO.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class StripeSubscriptionResponse {
        private String id;
        private String status; // active, canceled, past_due, etc.
        private String customer;
        
        @JsonProperty("items")
        private SubscriptionItems items;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class SubscriptionItems {
        @JsonProperty("data")
        private java.util.List<SubscriptionItem> data;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class SubscriptionItem {
        @JsonProperty("price")
        private Price price;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Price {
        private String id;
        private String product;
    }

    /**
     * Stripe API subscriptions list response DTO.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class StripeSubscriptionsListResponse {
        @JsonProperty("data")
        private java.util.List<StripeSubscriptionResponse> data;
    }

    /**
     * Stripe API customers list response DTO.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class StripeCustomersListResponse {
        @JsonProperty("data")
        private java.util.List<StripeCustomerResponse> data;
    }

    /**
     * Stripe API customer response DTO.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class StripeCustomerResponse {
        private String id;
        private String email;
    }
}

