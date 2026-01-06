package adrianmikula.jakartamigration.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for license key validation and tier determination.
 * 
 * This service delegates to multiple validation providers:
 * 1. StripeLicenseService for Stripe subscription validation
 * 2. ApifyLicenseService for Apify-based validation
 * 3. Simple pattern matching for test keys
 * 
 * The service tries each provider in order until one returns a valid tier.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LicenseService {

    private final ApifyLicenseService apifyLicenseService;
    private final StripeLicenseService stripeLicenseService;

    /**
     * Validate a license key and return the associated tier.
     * 
     * This method tries validation providers in order:
     * 1. Stripe validation (if license key looks like Stripe key)
     * 2. Apify validation (if license key looks like Apify key)
     * 3. Simple pattern matching for test keys
     * 
     * @param licenseKey The license key to validate
     * @return The license tier if valid, null if invalid
     */
    public FeatureFlagsProperties.LicenseTier validateLicense(String licenseKey) {
        if (licenseKey == null || licenseKey.isBlank()) {
            return null;
        }

        // Try Stripe validation first (if it looks like a Stripe key)
        if (isStripeKey(licenseKey)) {
            FeatureFlagsProperties.LicenseTier tier = stripeLicenseService.validateLicense(licenseKey);
            if (tier != null) {
                log.debug("License validated via Stripe: {}", maskKey(licenseKey));
                return tier;
            }
        }

        // Try Apify validation (if it looks like an Apify key or not a known format)
        FeatureFlagsProperties.LicenseTier tier = apifyLicenseService.validateLicense(licenseKey);
        if (tier != null) {
            log.debug("License validated via Apify: {}", maskKey(licenseKey));
            return tier;
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
     * Check if a license key looks like a Stripe key.
     */
    private boolean isStripeKey(String licenseKey) {
        return licenseKey.startsWith("stripe_") ||
               licenseKey.startsWith("cus_") ||
               licenseKey.startsWith("sub_") ||
               licenseKey.startsWith("price_");
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

