package adrianmikula.jakartamigration.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for license tier determination.
 * 
 * Integrates with JetBrains Marketplace licensing system.
 * Supports:
 * - JetBrains LicensingFacade (for IDE-installed plugins)
 * - License key validation
 * - 7-day free trial tracking
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
     * 3. Trial period (7-day free trial)
     * 4. Configured default tier
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

        // 3. Check if trial is still active
        if (properties.getTrialEndTimestamp() != null) {
            if (System.currentTimeMillis() < properties.getTrialEndTimestamp()) {
                log.debug("User has active trial period");
                return FeatureFlagsProperties.LicenseTier.PREMIUM;
            } else {
                log.debug("Trial period has expired");
            }
        }

        // 4. Return configured default tier
        return properties.getDefaultTier();
    }

    /**
     * Check if user has an active subscription (paid or trial).
     */
    public boolean hasActiveSubscription() {
        return getDefaultTier() == FeatureFlagsProperties.LicenseTier.PREMIUM;
    }

    /**
     * Get remaining trial days.
     */
    public int getRemainingTrialDays() {
        if (properties.getTrialEndTimestamp() == null) {
            return 0;
        }
        long remainingMs = properties.getTrialEndTimestamp() - System.currentTimeMillis();
        if (remainingMs <= 0) {
            return 0;
        }
        return (int) Math.ceil(remainingMs / (1000.0 * 60 * 60 * 24));
    }

    /**
     * Check if trial has been started but not yet expired.
     */
    public boolean isTrialActive() {
        return properties.getTrialEndTimestamp() != null 
            && System.currentTimeMillis() < properties.getTrialEndTimestamp();
    }

    /**
     * Start a 7-day free trial.
     */
    public void startTrial() {
        long trialEnd = System.currentTimeMillis() + 
            (FeatureFlagsProperties.FREE_TRIAL_DAYS * 24L * 60 * 60 * 1000);
        properties.setTrialEndTimestamp(trialEnd);
        log.info("Started 7-day free trial. Expires at: {}", new java.util.Date(trialEnd));
    }

    /**
     * Get subscription status as a human-readable string.
     */
    public String getSubscriptionStatus() {
        if (hasActiveSubscription()) {
            if (isTrialActive()) {
                int days = getRemainingTrialDays();
                return String.format("Trial - %d days remaining", days);
            }
            return "Premium Subscription";
        }
        return "Community (Free)";
    }

    /**
     * Get upgrade prompt message with pricing.
     */
    public String getUpgradePrompt() {
        return String.format(
            "Upgrade to Premium for $49/month or $399/year. " +
            "Get all premium features including auto-fixes, one-click refactor, and advanced analysis.",
            FeatureFlagsProperties.getMonthlyPriceFormatted(),
            FeatureFlagsProperties.getYearlyPriceFormatted()
        );
    }
}
