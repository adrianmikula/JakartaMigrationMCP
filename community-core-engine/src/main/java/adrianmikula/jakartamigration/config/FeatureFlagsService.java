package adrianmikula.jakartamigration.config;

import lombok.extern.slf4j.Slf4j;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for checking feature flag availability.
 * 
 * Integrates with JetBrains Marketplace licensing system.
 * Supports tier-based access (COMMUNITY, PREMIUM) with 7-day free trial.
 */
@Slf4j
public class FeatureFlagsService {

    private final FeatureFlagsProperties properties;
    private final LicenseService licenseService;

    public FeatureFlagsService(FeatureFlagsProperties properties, LicenseService licenseService) {
        this.properties = properties;
        this.licenseService = licenseService;
    }

    public boolean isEnabled(FeatureFlag flag) {
        if (!properties.getEnabled()) {
            log.debug("Feature flags disabled, allowing feature: {}", flag.getKey());
            return true;
        }

        Boolean override = properties.getFeatures().get(flag.getKey());
        if (override != null) {
            log.debug("Feature {} override: {}", flag.getKey(), override);
            return override;
        }

        FeatureFlagsProperties.LicenseTier currentTier = getCurrentTier();
        return flag.isAvailableFor(currentTier);
    }

    public void requireEnabled(FeatureFlag flag) {
        if (!isEnabled(flag)) {
            throw new FeatureNotAvailableException(
                    flag,
                    getCurrentTier(),
                    flag.getRequiredTier());
        }
    }

    public Set<FeatureFlag> getEnabledFeatures() {
        if (!properties.getEnabled()) {
            return EnumSet.allOf(FeatureFlag.class);
        }

        FeatureFlagsProperties.LicenseTier currentTier = getCurrentTier();
        return EnumSet.allOf(FeatureFlag.class).stream()
                .filter(flag -> {
                    Boolean override = properties.getFeatures().get(flag.getKey());
                    if (override != null) {
                        return override;
                    }
                    return flag.isAvailableFor(currentTier);
                })
                .collect(Collectors.toSet());
    }

    public FeatureFlagsProperties.LicenseTier getCurrentTier() {
        return licenseService.getDefaultTier();
    }

    public boolean hasTier(FeatureFlagsProperties.LicenseTier tier) {
        return getCurrentTier().ordinal() >= tier.ordinal();
    }

    /**
     * Check if user has an active subscription (paid or trial).
     */
    public boolean hasActiveSubscription() {
        return licenseService.hasActiveSubscription();
    }

    /**
     * Get remaining trial days.
     */
    public int getRemainingTrialDays() {
        return licenseService.getRemainingTrialDays();
    }

    /**
     * Check if trial is active.
     */
    public boolean isTrialActive() {
        return licenseService.isTrialActive();
    }

    /**
     * Start a free trial.
     */
    public void startTrial() {
        licenseService.startTrial();
    }

    /**
     * Get subscription status as a human-readable string.
     */
    public String getSubscriptionStatus() {
        return licenseService.getSubscriptionStatus();
    }

    public String getUpgradeMessage(FeatureFlag flag) {
        return String.format(
                "The '%s' feature requires a Premium subscription. %s",
                flag.getName(),
                getPricingInfo());
    }

    /**
     * Get pricing information for upgrade prompts.
     */
    public String getPricingInfo() {
        return String.format(
                "Upgrade: %s or %s. Start your free 7-day trial today!",
                FeatureFlagsProperties.getMonthlyPriceFormatted(),
                FeatureFlagsProperties.getYearlyPriceFormatted());
    }

    /**
     * Get upgrade prompt with full pricing details.
     */
    public String getFullUpgradePrompt() {
        int savings = FeatureFlagsProperties.getYearlySavingsPercent();
        return String.format(
                "Upgrade to Premium for:\n" +
                "• %s (billed monthly)\n" +
                "• %s (billed yearly - save %d%%)\n" +
                "• 7-day free trial available\n\n" +
                "Visit JetBrains Marketplace to subscribe.",
                FeatureFlagsProperties.getMonthlyPriceFormatted(),
                FeatureFlagsProperties.getYearlyPriceFormatted(),
                savings);
    }

    public UpgradeInfo getUpgradeInfo(FeatureFlag flag) {
        return new UpgradeInfo(
                flag.getName(),
                flag.getDescription(),
                getCurrentTier(),
                flag.getRequiredTier(),
                FeatureFlagsProperties.getMarketplaceUrl(),
                getUpgradeMessage(flag),
                getSubscriptionStatus(),
                getRemainingTrialDays());
    }

    /**
     * Get upgrade info with full pricing details.
     */
    public UpgradeInfo getFullUpgradeInfo() {
        return new UpgradeInfo(
                "Premium Subscription",
                "All premium features including auto-fixes, one-click refactor, and advanced analysis",
                getCurrentTier(),
                FeatureFlagsProperties.LicenseTier.PREMIUM,
                FeatureFlagsProperties.getMarketplaceUrl(),
                getFullUpgradePrompt(),
                getSubscriptionStatus(),
                getRemainingTrialDays());
    }

    public static class UpgradeInfo {
        private final String featureName;
        private final String featureDescription;
        private final FeatureFlagsProperties.LicenseTier currentTier;
        private final FeatureFlagsProperties.LicenseTier requiredTier;
        private final String paymentLink;
        private final String message;
        private final String subscriptionStatus;
        private final int remainingTrialDays;

        public UpgradeInfo(
                String featureName,
                String featureDescription,
                FeatureFlagsProperties.LicenseTier currentTier,
                FeatureFlagsProperties.LicenseTier requiredTier,
                String paymentLink,
                String message,
                String subscriptionStatus,
                int remainingTrialDays) {
            this.featureName = featureName;
            this.featureDescription = featureDescription;
            this.currentTier = currentTier;
            this.requiredTier = requiredTier;
            this.paymentLink = paymentLink;
            this.message = message;
            this.subscriptionStatus = subscriptionStatus;
            this.remainingTrialDays = remainingTrialDays;
        }

        public String getFeatureName() {
            return featureName;
        }

        public String getFeatureDescription() {
            return featureDescription;
        }

        public FeatureFlagsProperties.LicenseTier getCurrentTier() {
            return currentTier;
        }

        public FeatureFlagsProperties.LicenseTier getRequiredTier() {
            return requiredTier;
        }

        public String getPaymentLink() {
            return paymentLink;
        }

        public String getMessage() {
            return message;
        }

        public String getSubscriptionStatus() {
            return subscriptionStatus;
        }

        public int getRemainingTrialDays() {
            return remainingTrialDays;
        }

        public boolean isTrialActive() {
            return remainingTrialDays > 0;
        }

        public boolean canUpgrade() {
            return currentTier == FeatureFlagsProperties.LicenseTier.COMMUNITY;
        }
    }

    public static class FeatureNotAvailableException extends RuntimeException {
        private final FeatureFlag flag;
        private final FeatureFlagsProperties.LicenseTier currentTier;
        private final FeatureFlagsProperties.LicenseTier requiredTier;

        public FeatureNotAvailableException(
                FeatureFlag flag,
                FeatureFlagsProperties.LicenseTier currentTier,
                FeatureFlagsProperties.LicenseTier requiredTier) {
            super(String.format(
                    "Feature '%s' requires %s license, but current tier is %s",
                    flag.getName(),
                    requiredTier,
                    currentTier));
            this.flag = flag;
            this.currentTier = currentTier;
            this.requiredTier = requiredTier;
        }

        public FeatureFlag getFlag() {
            return flag;
        }

        public FeatureFlagsProperties.LicenseTier getCurrentTier() {
            return currentTier;
        }

        public FeatureFlagsProperties.LicenseTier getRequiredTier() {
            return requiredTier;
        }
    }
}
