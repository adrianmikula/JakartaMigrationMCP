package adrianmikula.jakartamigration.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for license tier determination.
 *
 * Integrates with JetBrains Marketplace licensing system.
 * Supports:
 * - JetBrains LicensingFacade (for IDE-installed plugins)
 * - Free tier with credits (limited scans/refactors)
 * - Premium tier with unlimited features
 */
@Slf4j
public class LicenseService {

    private final FeatureFlagsProperties properties;

    // JetBrains Marketplace plugin ID (numeric ID from marketplace URL)
    private static final String PLUGIN_ID = "30093";

    public LicenseService(FeatureFlagsProperties properties) {
        this.properties = properties;
    }

    /**
     * Returns the effective license tier.
     * Checks in order:
     * 1. System property override (for testing)
     * 2. IntelliJ LicensingFacade (for IDE-installed plugins)
     * 3. Configured default tier
     */
    public FeatureFlagsProperties.LicenseTier getDefaultTier() {
        // 1. Check for system property override (useful for testing)
        String override = System.getProperty("jakarta.migration.license.tier");
        if (override != null) {
            try {
                return FeatureFlagsProperties.LicenseTier.valueOf(override.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Ignore invalid tier
            }
        }

        // 2. Check for IntelliJ LicensingFacade if available
        try {
            Class<?> facadeClass = Class.forName("com.intellij.ide.licensing.LicensingFacade");
            Object facade = facadeClass.getMethod("getInstance").invoke(null);
            if (facade != null) {
                // Check if user has a paid subscription for our plugin
                Boolean hasLicense = (Boolean) facadeClass
                    .getMethod("isLicensed", String.class)
                    .invoke(facade, PLUGIN_ID);
                
                if (Boolean.TRUE.equals(hasLicense)) {
                    log.debug("User has valid JetBrains Marketplace license");
                    return FeatureFlagsProperties.LicenseTier.PREMIUM;
                }
            }
        } catch (Exception e) {
            // Not running in IntelliJ or LicensingFacade not available
            log.debug("IntelliJ LicensingFacade not available: {}", e.getMessage());
        }

        // 3. Return configured default tier (usually COMMUNITY for free users)
        return properties.getDefaultTier();
    }

    /**
     * Check if user has an active premium subscription.
     */
    public boolean hasActiveSubscription() {
        return getDefaultTier() == FeatureFlagsProperties.LicenseTier.PREMIUM;
    }

    /**
     * Get subscription status as a human-readable string.
     */
    public String getSubscriptionStatus() {
        if (hasActiveSubscription()) {
            return "Premium Subscription";
        }
        return "Free (Credits Limited)";
    }

    /**
     * Get upgrade prompt message with pricing.
     */
    public String getUpgradePrompt() {
        return String.format(
            "Upgrade to Premium for unlimited scans and refactors. " +
            "Get all premium features including auto-fixes, one-click refactoring, and advanced analysis.\n" +
            "Pricing: %s or %s",
            FeatureFlagsProperties.getMonthlyPriceFormatted(),
            FeatureFlagsProperties.getYearlyPriceFormatted()
        );
    }
}
