package adrianmikula.jakartamigration.config;

import lombok.Data;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Configuration properties for feature flags.
 * 
 * Feature flags control access to premium features based on license tier.
 * 
 * Configuration example in application.yml:
 * 
 * <pre>
 * jakarta:
 *   migration:
 *     feature-flags:
 *       enabled: true
 *       default-tier: COMMUNITY
 *       features:
 *         JPA_MIGRATION: true
 *         BEAN_VALIDATION: true
 *         SERVLET_JSP: true
 *         CDI_INJECTION: false
 *         BUILD_CONFIG: true
 *         REST_SOAP: false
 *         DEPRECATED_API: false
 *         SECURITY_API: false
 *         JMS_MESSAGING: false
 *         CONFIG_FILES: true
 *         BINARY_FIXES: false
 *         ADVANCED_ANALYSIS: false
 * </pre>
 */
@Data
public class FeatureFlagsProperties {

    private static final Properties PRICING_PROPERTIES = loadPricingProperties();

    /**
     * Load pricing properties from resources/pricing.properties
     */
    private static Properties loadPricingProperties() {
        Properties props = new Properties();
        try (InputStream input = FeatureFlagsProperties.class.getClassLoader().getResourceAsStream("pricing.properties")) {
            if (input != null) {
                props.load(input);
            } else {
                // Fallback to default values if file not found
                props.setProperty("jakarta.migration.pricing.monthly.usd", "?");
                props.setProperty("jakarta.migration.pricing.yearly.usd", "?");
                props.setProperty("jakarta.migration.trial.days", "?");
            }
        } catch (Exception e) {
            // Fallback to default values on error
            props.setProperty("jakarta.migration.pricing.monthly.usd", "?");
            props.setProperty("jakarta.migration.pricing.yearly.usd", "?");
        }
        return props;
    }

    /**
     * Get monthly subscription price in USD from pricing.properties
     */
    public static double getMonthlyPriceUsd() {
        return Double.parseDouble(PRICING_PROPERTIES.getProperty("jakarta.migration.pricing.monthly.usd", "5.0"));
    }

    /**
     * Get yearly subscription price in USD from pricing.properties
     */
    public static double getYearlyPriceUsd() {
        return Double.parseDouble(PRICING_PROPERTIES.getProperty("jakarta.migration.pricing.yearly.usd", "50.0"));
    }

    /**
     * Whether feature flags are enabled.
     * When disabled, all features are available (for development/testing).
     */
    private Boolean enabled = true;

    /**
     * Default license tier when no license key is provided.
     * COMMUNITY = Free tier, PREMIUM = Paid tier
     */
    private LicenseTier defaultTier = LicenseTier.COMMUNITY;

    /**
     * License key for premium features.
     * Can be set via environment variable JAKARTA_MCP_LICENSE_KEY
     */
    private String licenseKey = "";

    /**
     * Per-feature overrides.
     * Allows enabling/disabling specific features regardless of tier.
     * Useful for testing or gradual rollouts.
     */
    private Map<String, Boolean> features = new HashMap<>();

    /**
     * License tier enumeration.
     * Simplified to COMMUNITY (free) and PREMIUM (paid).
     * JetBrains Marketplace handles subscription management.
     */
    public enum LicenseTier {
        /**
         * Community/Free tier.
         * Basic features with limited credits: 10 basic scans, 50 advanced scans, 5 refactors.
         * Available to all users without license.
         */
        COMMUNITY,

        /**
         * Premium/Paid tier.
         * All features including auto-fixes, one-click refactor, binary fixes.
         * Requires active JetBrains Marketplace subscription ($49/month or $399/year).
         */
        PREMIUM
    }

    /**
     * Get JetBrains Marketplace purchase URL.
     */
    public static String getMarketplaceUrl() {
        return "https://plugins.jetbrains.com/plugin/30093-jakarta-migration";
    }

    /**
     * Get monthly subscription price formatted.
     */
    public static String getMonthlyPriceFormatted() {
        return String.format("$%.0f/month", getMonthlyPriceUsd());
    }

    /**
     * Get yearly subscription price formatted.
     */
    public static String getYearlyPriceFormatted() {
        return String.format("$%.0f/year", getYearlyPriceUsd());
    }

    /**
     * Get yearly savings compared to monthly.
     */
    public static int getYearlySavingsPercent() {
        int monthlyTotal = (int) (getMonthlyPriceUsd() * 12);
        return (int) Math.round((1 - (getYearlyPriceUsd() / monthlyTotal)) * 100);
    }
}
