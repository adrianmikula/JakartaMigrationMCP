package adrianmikula.jakartamigration.config;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

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
 *       license-key: ${JAKARTA_MCP_LICENSE_KEY:}
 *       features:
 *         AUTO_FIXES: false
 *         ONE_CLICK_REFACTOR: false
 *         BINARY_FIXES: false
 *         ADVANCED_ANALYSIS: false
 * </pre>
 */
@Data
public class FeatureFlagsProperties {

    // Pricing constants for JetBrains Marketplace
    /** Monthly subscription price in USD */
    public static final double MONTHLY_PRICE_USD = 49.0;
    
    /** Yearly subscription price in USD */
    public static final double YEARLY_PRICE_USD = 399.0;
    
    /** Free trial duration in days */
    public static final int FREE_TRIAL_DAYS = 7;

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
     * Trial end timestamp (milliseconds) for 7-day free trial.
     * Set when user activates trial via JetBrains Marketplace.
     */
    private Long trialEndTimestamp = null;

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
         * Basic features: scanning, identification, analysis.
         * Available to all users without license.
         */
        COMMUNITY,

        /**
         * Premium/Paid tier.
         * All features including auto-fixes, one-click refactor, binary fixes.
         * Requires active JetBrains Marketplace subscription ($49/month or $399/year).
         * Includes 7-day free trial for new users.
         */
        PREMIUM
    }

    /**
     * Check if the user has an active premium subscription.
     * Considers both paid subscription and active trial period.
     */
    public boolean hasActiveSubscription() {
        // Check if premium tier
        if (defaultTier == LicenseTier.PREMIUM) {
            return true;
        }
        
        // Check if trial is still active
        if (trialEndTimestamp != null) {
            return System.currentTimeMillis() < trialEndTimestamp;
        }
        
        return false;
    }

    /**
     * Get remaining trial days.
     * Returns 0 if no active trial.
     */
    public int getRemainingTrialDays() {
        if (trialEndTimestamp == null) {
            return 0;
        }
        long remainingMs = trialEndTimestamp - System.currentTimeMillis();
        if (remainingMs <= 0) {
            return 0;
        }
        return (int) Math.ceil(remainingMs / (1000.0 * 60 * 60 * 24));
    }

    /**
     * Get JetBrains Marketplace purchase URL.
     */
    public static String getMarketplaceUrl() {
        return "https://plugins.jetbrains.com/plugin/";
    }

    /**
     * Get monthly subscription price formatted.
     */
    public static String getMonthlyPriceFormatted() {
        return String.format("$%.0f/month", MONTHLY_PRICE_USD);
    }

    /**
     * Get yearly subscription price formatted.
     */
    public static String getYearlyPriceFormatted() {
        return String.format("$%.0f/year", YEARLY_PRICE_USD);
    }

    /**
     * Get yearly savings compared to monthly.
     */
    public static int getYearlySavingsPercent() {
        int monthlyTotal = (int) (MONTHLY_PRICE_USD * 12);
        return (int) Math.round((1 - (YEARLY_PRICE_USD / monthlyTotal)) * 100);
    }
}
