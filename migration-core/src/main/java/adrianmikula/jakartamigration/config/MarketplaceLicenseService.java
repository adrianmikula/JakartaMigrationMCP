package adrianmikula.jakartamigration.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Service for validating JetBrains Marketplace licenses.
 * 
 * This service integrates with the JetBrains Plugin Repository API
 * to verify subscription status for premium features.
 * 
 * Pricing:
 * - Monthly: $49 USD
 * - Yearly: $399 USD (17% savings)
 * 
 * @see <a href="https://plugins.jetbrains.com/docs/marketplace/license-validation.html">JetBrains License Validation</a>
 */
@Slf4j
public class MarketplaceLicenseService {

    private static final String MARKETPLACE_API_URL = "https://plugins.jetbrains.com/api/license/";
    private static final String PLUGIN_ID = "25558"; // Jakarta Migration MCP plugin ID
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    
    private final HttpClient httpClient;

    public MarketplaceLicenseService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
    }

    /**
     * Validates a license key against the JetBrains Marketplace API.
     * 
     * @param licenseKey The license key to validate
     * @return LicenseValidationResult containing validation status and details
     */
    public LicenseValidationResult validateLicense(String licenseKey) {
        if (licenseKey == null || licenseKey.isBlank()) {
            return LicenseValidationResult.invalid("License key is empty");
        }

        try {
            // In production, this would call the actual JetBrains API
            // For now, we simulate the validation
            return validateLicenseKey(licenseKey);
        } catch (Exception e) {
            log.error("Failed to validate license key: {}", e.getMessage(), e);
            return LicenseValidationResult.error("Failed to validate license: " + e.getMessage());
        }
    }

    /**
     * Validates a license key (simulated for development).
     * In production, this would call the actual JetBrains Marketplace API.
     */
    private LicenseValidationResult validateLicenseKey(String licenseKey) {
        // Simulate license key validation
        // In production, replace with actual API call:
        // HttpRequest request = HttpRequest.newBuilder()
        //     .uri(URI.create(MARKETPLACE_API_URL + PLUGIN_ID + "/validate"))
        //     .header("Authorization", "Bearer " + licenseKey)
        //     .GET()
        //     .build();
        
        // For development/testing, accept specific test keys
        if (licenseKey.startsWith("TEST-") || licenseKey.startsWith("DEV-")) {
            return LicenseValidationResult.valid(
                    licenseKey,
                    LicenseType.DEVELOPMENT,
                    Instant.now().plusSeconds(86400 * 7), // 7 days
                    "Test license"
            );
        }

        // Accept "PREMIUM" for testing
        if ("PREMIUM".equals(licenseKey)) {
            return LicenseValidationResult.valid(
                    licenseKey,
                    LicenseType.PREMIUM,
                    Instant.now().plusSeconds(86400 * 365), // 1 year
                    "Premium test license"
            );
        }

        // Accept "EXPIRED" for testing expired licenses
        if ("EXPIRED".equals(licenseKey)) {
            return LicenseValidationResult.expired(
                    licenseKey,
                    LicenseType.PREMIUM,
                    Instant.now().minusSeconds(86400), // Yesterday
                    "Expired test license"
            );
        }

        // Default: valid license for demo purposes
        return LicenseValidationResult.valid(
                licenseKey,
                LicenseType.PREMIUM,
                Instant.now().plusSeconds(86400 * 30), // 30 days
                "Demo license"
        );
    }

    /**
     * Checks if a license is valid and active.
     */
    public boolean isLicenseValid(String licenseKey) {
        LicenseValidationResult result = validateLicense(licenseKey);
        return result.isValid();
    }

    /**
     * Gets the expiration date of a license.
     */
    public Optional<Instant> getExpirationDate(String licenseKey) {
        LicenseValidationResult result = validateLicense(licenseKey);
        return result.getExpirationDate();
    }

    /**
     * Calculates remaining days for a license.
     */
    public int getRemainingDays(String licenseKey) {
        LicenseValidationResult result = validateLicense(licenseKey);
        if (result.getExpirationDate().isEmpty()) {
            return 0;
        }
        
        long remainingSeconds = result.getExpirationDate().get().getEpochSecond() - Instant.now().getEpochSecond();
        return (int) Math.max(0, remainingSeconds / (86400));
    }

    // === Result Classes ===

    /**
     * Result of a license validation attempt.
     */
    @Data
    @AllArgsConstructor
    public static class LicenseValidationResult {
        private boolean valid;
        private boolean active;
        private String status;
        private String licenseKey;
        private LicenseType licenseType;
        private Instant expirationDate;
        private String message;

        public static LicenseValidationResult valid(String licenseKey, LicenseType type, 
                Instant expiration, String message) {
            return new LicenseValidationResult(
                    true, 
                    true, 
                    "VALID",
                    licenseKey, 
                    type, 
                    expiration, 
                    message
            );
        }

        public static LicenseValidationResult expired(String licenseKey, LicenseType type,
                Instant expiration, String message) {
            return new LicenseValidationResult(
                    false,
                    false,
                    "EXPIRED",
                    licenseKey,
                    type,
                    expiration,
                    message
            );
        }

        public static LicenseValidationResult invalid(String message) {
            return new LicenseValidationResult(
                    false,
                    false,
                    "INVALID",
                    null,
                    null,
                    null,
                    message
            );
        }

        public static LicenseValidationResult error(String message) {
            return new LicenseValidationResult(
                    false,
                    false,
                    "ERROR",
                    null,
                    null,
                    null,
                    message
            );
        }

        public Optional<Instant> getExpirationDate() {
            return Optional.ofNullable(expirationDate);
        }

        public boolean isValid() {
            return valid && active && expirationDate != null && Instant.now().isBefore(expirationDate);
        }
    }

    /**
     * Types of licenses supported.
     */
    public enum LicenseType {
        /** Development/testing license */
        DEVELOPMENT,
        
        /** Paid premium subscription */
        PREMIUM,
        
        /** Enterprise license */
        ENTERPRISE,
        
        /** Free community license */
        COMMUNITY
    }
}
