package adrianmikula.jakartamigration.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import adrianmikula.jakartamigration.storage.service.LocalLicenseStorageService;

/**
 * Service for license key validation and tier determination.
 * 
 * This service delegates to validation providers:
 * 1. StripeLicenseService for Stripe subscription validation (primary)
 * 2. ApifyLicenseService for Apify-based validation (optional, disabled by default)
 * 3. Simple pattern matching for test keys
 * 
 * Stripe is the primary payment processor. Apify support is deprecated.
 * The service tries each provider in order until one returns a valid tier.
 */
@Slf4j
@Service
public class LicenseService {

    @Nullable
    private final StripeLicenseService stripeLicenseService; // Optional, may be null if disabled
    @Nullable
    private final ApifyLicenseService apifyLicenseService; // Optional, may be null if disabled
    @Nullable
    private final LocalLicenseStorageService localStorageService; // Optional, may be null if disabled

    @Autowired
    public LicenseService(
            @Autowired(required = false) @Nullable StripeLicenseService stripeLicenseService,
            @Autowired(required = false) @Nullable ApifyLicenseService apifyLicenseService,
            @Autowired(required = false) @Nullable LocalLicenseStorageService localStorageService) {
        this.stripeLicenseService = stripeLicenseService;
        this.apifyLicenseService = apifyLicenseService;
        this.localStorageService = localStorageService;
    }

    /**
     * Validate a license key and return the associated tier.
     * 
     * This method tries validation providers in order:
     * 1. Stripe validation (primary - tries all keys)
     * 2. Apify validation (optional, only if enabled and Apify service available)
     * 3. Simple pattern matching for test keys
     * 
     * @param licenseKey The license key to validate
     * @return The license tier if valid, null if invalid
     */
    public FeatureFlagsProperties.LicenseTier validateLicense(String licenseKey) {
        if (licenseKey == null || licenseKey.isBlank()) {
            return null;
        }

        // Try Stripe validation first (primary payment processor)
        // Stripe can handle various key formats, so we try it for all keys
        FeatureFlagsProperties.LicenseTier tier = null;
        if (stripeLicenseService != null) {
            tier = stripeLicenseService.validateLicense(licenseKey);
            if (tier != null) {
                log.debug("License validated via Stripe: {}", maskKey(licenseKey));
                return tier;
            }
        }

        // Try Apify validation only if service is available and enabled
        // Apify support is deprecated in favor of Stripe
        if (apifyLicenseService != null) {
            tier = apifyLicenseService.validateLicense(licenseKey);
            if (tier != null) {
                log.debug("License validated via Apify: {}", maskKey(licenseKey));
                return tier;
            }
        }

        // Fallback to simple validation for test keys
        // Keys starting with "PREMIUM-" are premium tier
        // Keys starting with "ENTERPRISE-" are enterprise tier
        if (licenseKey.startsWith("PREMIUM-")) {
            log.debug("Valid premium license key detected (test key)");
            return FeatureFlagsProperties.LicenseTier.PREMIUM;
        }

        if (licenseKey.startsWith("ENTERPRISE-")) {
            log.debug("Valid enterprise license key detected (test key)");
            return FeatureFlagsProperties.LicenseTier.ENTERPRISE;
        }

        log.debug("Invalid license key format: {}", maskKey(licenseKey));
        return null;
    }

    /**
     * Validate license by email address.
     * Checks if the email exists in Stripe customers and has active subscriptions.
     * 
     * First checks local storage (if enabled), then validates via Stripe API.
     * 
     * @param email The customer email address
     * @return License tier if valid, null if invalid
     */
    public FeatureFlagsProperties.LicenseTier validateLicenseByEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            log.debug("Invalid email format for license validation");
            return null;
        }

        // Check local storage first (if enabled)
        if (localStorageService != null) {
            FeatureFlagsProperties.LicenseTier cachedTier = localStorageService.getTierByEmail(email);
            if (cachedTier != null) {
                log.debug("License validated via local storage: {}", maskEmail(email));
                return cachedTier;
            }
        }

        // Validate via Stripe API
        if (stripeLicenseService != null) {
            try {
                FeatureFlagsProperties.LicenseTier tier = stripeLicenseService.validateLicenseByEmail(email)
                    .block(java.time.Duration.ofSeconds(5));
                if (tier != null) {
                    log.debug("License validated via Stripe email: {}", maskEmail(email));
                    
                    // Store in local storage (if enabled)
                    if (localStorageService != null) {
                        localStorageService.storeSession(email, null, tier, 24L);
                    }
                    
                    return tier;
                }
            } catch (Exception e) {
                log.warn("Error validating license by email: {}", e.getMessage());
            }
        }

        log.debug("No valid license found for email: {}", maskEmail(email));
        return null;
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
     * Check if a license key is valid (not expired, properly signed, etc.).
     * 
     * @param licenseKey The license key to check
     * @return true if the license is valid, false otherwise
     */
    public boolean isValidLicense(String licenseKey) {
        return validateLicense(licenseKey) != null;
    }

    /**
     * Get license expiration date (future implementation).
     * 
     * @param licenseKey The license key
     * @return Expiration date, or null if perpetual or invalid
     */
    public java.time.LocalDate getExpirationDate(String licenseKey) {
        // TODO: Extract expiration date from license key
        return null;
    }

    /**
     * Check if a license is expired.
     * 
     * @param licenseKey The license key
     * @return true if expired, false otherwise
     */
    public boolean isExpired(String licenseKey) {
        java.time.LocalDate expiration = getExpirationDate(licenseKey);
        if (expiration == null) {
            return false; // Perpetual license or no expiration
        }
        return expiration.isBefore(java.time.LocalDate.now());
    }
}

