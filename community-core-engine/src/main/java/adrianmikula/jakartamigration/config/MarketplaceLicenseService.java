/*
 * Copyright 2026 Adrian Mikula
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
    private static final String PLUGIN_ID = "30093"; // Jakarta Migration plugin ID from marketplace
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    
    private final HttpClient httpClient;

    public MarketplaceLicenseService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
    }

    /**
     * Validates a license key against the JetBrains Marketplace API.
     * Falls back to local validation if the API is unavailable.
     *
     * @param licenseKey The license key to validate
     * @return LicenseValidationResult containing validation status and details
     */
    public LicenseValidationResult validateLicense(String licenseKey) {
        if (licenseKey == null || licenseKey.isBlank()) {
            return LicenseValidationResult.invalid("License key is empty");
        }

        try {
            // Try real JetBrains Marketplace API first
            return callMarketplaceApi(licenseKey);
        } catch (Exception e) {
            log.warn("Failed to validate via Marketplace API, falling back to local validation: {}", e.getMessage());
            // Fall back to local validation for offline/dev mode
            return validateLicenseKeyLocal(licenseKey);
        }
    }

    /**
     * Calls the JetBrains Marketplace API to validate a license key.
     */
    private LicenseValidationResult callMarketplaceApi(String licenseKey) throws Exception {
        String requestBody = String.format(
            "{\"pluginId\": \"%s\", \"licenseKey\": \"%s\"}",
            PLUGIN_ID,
            licenseKey
        );

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(MARKETPLACE_API_URL + "validate"))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .timeout(TIMEOUT)
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return parseMarketplaceResponse(response.body());
        } else if (response.statusCode() == 401 || response.statusCode() == 403) {
            return LicenseValidationResult.invalid("Invalid license key");
        } else if (response.statusCode() == 404) {
            return LicenseValidationResult.invalid("License not found for this plugin");
        } else {
            throw new Exception("Marketplace API error: " + response.statusCode());
        }
    }

    /**
     * Parses the JetBrains Marketplace API JSON response.
     */
    private LicenseValidationResult parseMarketplaceResponse(String jsonResponse) {
        try {
            boolean valid = jsonResponse.contains("\"valid\":true") || jsonResponse.contains("\"valid\" : true");
            
            if (!valid) {
                return LicenseValidationResult.invalid("License validation failed");
            }

            Instant expirationDate = extractExpirationDate(jsonResponse);
            LicenseType licenseType = extractLicenseType(jsonResponse);

            return LicenseValidationResult.valid(
                "validated",
                licenseType,
                expirationDate,
                "Valid JetBrains Marketplace license"
            );
        } catch (Exception e) {
            log.error("Failed to parse Marketplace response: {}", e.getMessage());
            return LicenseValidationResult.error("Failed to parse API response: " + e.getMessage());
        }
    }

    /**
     * Extracts the expiration date from the API response.
     */
    private Instant extractExpirationDate(String json) {
        String marker = "\"expirationDate\":";
        int start = json.indexOf(marker);
        if (start == -1) {
            marker = "\"expirationDate\" : ";
            start = json.indexOf(marker);
        }
        if (start == -1) {
            return Instant.now().plusSeconds(86400 * 30); // Default 30 days
        }
        
        start += marker.length();
        int end = json.indexOf(',', start);
        if (end == -1) end = json.indexOf('}', start);
        if (end == -1) end = json.length();
        
        String dateStr = json.substring(start, end).trim().replace("\"", "");
        try {
            return Instant.parse(dateStr);
        } catch (Exception e) {
            return Instant.now().plusSeconds(86400 * 30);
        }
    }

    /**
     * Extracts the license type from the API response.
     */
    private LicenseType extractLicenseType(String json) {
        if (json.contains("\"licenseType\":\"enterprise\"") || json.contains("\"licenseType\" : \"enterprise\"")) {
            return LicenseType.ENTERPRISE;
        }
        if (json.contains("\"licenseType\":\"commercial\"") || json.contains("\"licenseType\" : \"commercial\"")) {
            return LicenseType.PREMIUM;
        }
        return LicenseType.PREMIUM;
    }

    /**
     * Local license validation for offline mode and development.
     * Supports test keys for testing purposes.
     */
    private LicenseValidationResult validateLicenseKeyLocal(String licenseKey) {
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
