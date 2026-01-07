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
 * Service for validating license keys via Apify API.
 * 
 * Apify is a platform for hosting and monetizing MCP servers.
 * This service validates license keys (Apify API tokens) to determine
 * the user's license tier (COMMUNITY, PREMIUM, ENTERPRISE).
 * 
 * NOTE: Apify support is deprecated in favor of Stripe payment processing.
 * This service is only loaded if APIFY_VALIDATION_ENABLED=true.
 * 
 * The validation works by:
 * 1. Checking if the license key is a valid Apify API token
 * 2. Verifying the token has access to the premium actor
 * 3. Determining tier based on token permissions or actor access
 * 
 * Results are cached to reduce API calls.
 */
@Slf4j
@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = "jakarta.migration.apify.enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class ApifyLicenseService {

    private final ApifyLicenseProperties properties;
    private final WebClient apifyWebClient;

    public ApifyLicenseService(
        ApifyLicenseProperties properties,
        @Qualifier("apifyWebClient") WebClient apifyWebClient
    ) {
        this.properties = properties;
        this.apifyWebClient = apifyWebClient;
    }

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

    /**
     * Validate a license key via Apify API.
     * 
     * @param licenseKey The license key (Apify API token) to validate
     * @return License tier if valid, null if invalid or validation fails
     */
    public FeatureFlagsProperties.LicenseTier validateLicense(String licenseKey) {
        if (licenseKey == null || licenseKey.isBlank()) {
            return null;
        }

        // Check cache first
        CacheEntry cached = cache.get(licenseKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("License validation cache hit for key: {}", maskKey(licenseKey));
            return cached.tier;
        }

        // If Apify validation is disabled, use simple validation
        if (!properties.getEnabled()) {
            return validateLicenseSimple(licenseKey);
        }

        try {
            // Validate via Apify API
            FeatureFlagsProperties.LicenseTier tier = validateLicenseViaApify(licenseKey)
                .block(Duration.ofSeconds(properties.getTimeoutSeconds()));

            // Cache the result
            if (tier != null) {
                LocalDateTime expiresAt = LocalDateTime.now()
                    .plusSeconds(properties.getCacheTtlSeconds());
                cache.put(licenseKey, new CacheEntry(tier, expiresAt));
                log.debug("License validated and cached: {} -> {}", maskKey(licenseKey), tier);
            }

            return tier;

        } catch (Exception e) {
            log.warn("Apify license validation failed: {}", e.getMessage());

            // If offline validation is allowed, try simple validation
            if (properties.getAllowOfflineValidation()) {
                log.debug("Falling back to simple validation");
                return validateLicenseSimple(licenseKey);
            }

            return null;
        }
    }

    /**
     * Validate license key via Apify API.
     * 
     * @param licenseKey The Apify API token
     * @return Mono with license tier, or empty if invalid
     */
    private Mono<FeatureFlagsProperties.LicenseTier> validateLicenseViaApify(String licenseKey) {
        // Apify API: Get user info to validate token
        // Endpoint: GET /users/me
        return apifyWebClient
            .get()
            .uri("/users/me")
            .header("Authorization", "Bearer " + licenseKey)
            .retrieve()
            .bodyToMono(ApifyUserResponse.class)
            .map(user -> determineTierFromUser(user, licenseKey))
            .retryWhen(Retry.backoff(2, Duration.ofMillis(500))
                .filter(throwable -> {
                    // Retry on network errors, not on 401/403
                    if (throwable instanceof WebClientResponseException ex) {
                        return ex.getStatusCode().value() >= 500;
                    }
                    return true;
                })
                .doBeforeRetry(retrySignal -> 
                    log.debug("Retrying Apify API call: {}", retrySignal.totalRetries() + 1))
            )
            .onErrorResume(WebClientResponseException.class, ex -> {
                if (ex.getStatusCode().value() == 401 || ex.getStatusCode().value() == 403) {
                    log.debug("Invalid Apify API token: {}", maskKey(licenseKey));
                    return Mono.empty();
                }
                log.warn("Apify API error: {} {}", ex.getStatusCode(), ex.getMessage());
                return Mono.empty();
            })
            .onErrorResume(Exception.class, ex -> {
                log.warn("Unexpected error validating Apify license: {}", ex.getMessage());
                return Mono.empty();
            })
            .switchIfEmpty(Mono.empty());
    }

    /**
     * Determine license tier from Apify user information.
     * 
     * This is a simplified implementation. In production, you might:
     * - Check user's subscription plan
     * - Verify actor access permissions
     * - Check custom metadata or tags
     * 
     * @param user Apify user information
     * @param licenseKey The license key
     * @return License tier
     */
    private FeatureFlagsProperties.LicenseTier determineTierFromUser(
        ApifyUserResponse user,
        String licenseKey
    ) {
        // Simple implementation: Check if user has access to premium actor
        // In production, you might check:
        // - User's subscription plan (FREE, PERSONAL, TEAM, ENTERPRISE)
        // - Custom metadata on the user account
        // - Actor access permissions
        
        if (user == null) {
            return null;
        }

        // If actor ID is configured, check if user has access
        if (properties.getActorId() != null && !properties.getActorId().isBlank()) {
            // TODO: Check actor access via Apify API
            // For now, assume valid token = PREMIUM tier
            log.debug("User {} validated, granting PREMIUM tier", user.getUsername());
            return FeatureFlagsProperties.LicenseTier.PREMIUM;
        }

        // Default: Valid Apify token = PREMIUM tier
        // You can enhance this by checking user's plan
        String plan = user.getPlan() != null ? user.getPlan() : "FREE";
        
        return switch (plan.toUpperCase()) {
            case "ENTERPRISE", "TEAM" -> FeatureFlagsProperties.LicenseTier.ENTERPRISE;
            case "PERSONAL", "PRO" -> FeatureFlagsProperties.LicenseTier.PREMIUM;
            default -> {
                // Even free users might have premium if they have actor access
                // For now, grant PREMIUM for any valid token
                log.debug("User {} has {} plan, granting PREMIUM tier", user.getUsername(), plan);
                yield FeatureFlagsProperties.LicenseTier.PREMIUM;
            }
        };
    }

    /**
     * Simple license validation (fallback when Apify is unavailable).
     * 
     * @param licenseKey The license key
     * @return License tier if valid, null otherwise
     */
    private FeatureFlagsProperties.LicenseTier validateLicenseSimple(String licenseKey) {
        // Simple pattern matching (same as original implementation)
        if (licenseKey.startsWith("PREMIUM-")) {
            return FeatureFlagsProperties.LicenseTier.PREMIUM;
        }
        if (licenseKey.startsWith("ENTERPRISE-")) {
            return FeatureFlagsProperties.LicenseTier.ENTERPRISE;
        }
        // Apify tokens typically start with "apify_api_" or are UUIDs
        if (licenseKey.startsWith("apify_api_") || licenseKey.length() > 20) {
            // Assume it might be an Apify token, but can't validate offline
            log.debug("License key looks like Apify token, but offline validation not possible");
            return null;
        }
        return null;
    }

    /**
     * Clear the validation cache.
     * Useful for testing or when license status changes.
     */
    public void clearCache() {
        cache.clear();
        log.debug("License validation cache cleared");
    }

    /**
     * Clear cache entry for a specific license key.
     */
    public void clearCache(String licenseKey) {
        cache.remove(licenseKey);
        log.debug("Cache cleared for license key: {}", maskKey(licenseKey));
    }

    /**
     * Mask license key for logging (show only first/last few characters).
     */
    private String maskKey(String key) {
        if (key == null || key.length() <= 8) {
            return "***";
        }
        return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
    }

    /**
     * Apify API user response DTO.
     * Based on Apify API v2 structure: https://docs.apify.com/api/v2#/reference/users
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ApifyUserResponse {
        private String username;
        
        @JsonProperty("plan")
        private String plan;
        
        // Additional fields that might be useful
        private String email;
        private String id;
    }
}

