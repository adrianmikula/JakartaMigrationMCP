package adrianmikula.jakartamigration.config;

import adrianmikula.jakartamigration.config.FeatureFlag;
import lombok.extern.slf4j.Slf4j;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import static adrianmikula.jakartamigration.config.FeatureFlag.EXPERIMENTAL_FEATURES;

/**
 * Service for checking feature flag availability.
 *
 * Integrates with JetBrains Marketplace licensing system.
 * Supports tier-based access (COMMUNITY, PREMIUM) with credit-based free tier.
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

    public boolean isExperimentalFeaturesEnabled() {
        if (!properties.getEnabled()) {
            log.debug("Feature flags disabled, allowing experimental features");
            return true;
        }

        Boolean override = properties.getFeatures().get(EXPERIMENTAL_FEATURES.getKey());
        if (override != null) {
            log.debug("Experimental features override: {}", override);
            return true;
        }

        FeatureFlagsProperties.LicenseTier currentTier = getCurrentTier();
        return EXPERIMENTAL_FEATURES.isAvailableFor(currentTier);
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
     * Get subscription status as a human-readable string.
     */
    public String getSubscriptionStatus() {
        return licenseService.getSubscriptionStatus();
    }

    public String getUpgradeMessage(FeatureFlag flag) {
        return String.format(
                "The '%s' feature requires a PREMIUM subscription. %s",
                flag.getName(),
                getPricingInfo());
    }

    /**
     * Get pricing information for upgrade prompts.
     */
    public String getPricingInfo() {
        return String.format(
                "Upgrade: %s or %s. Get unlimited scans and refactors!",
                FeatureFlagsProperties.getMonthlyPriceFormatted(),
                FeatureFlagsProperties.getYearlyPriceFormatted());
    }

    /**
     * Get upgrade prompt with full pricing details.
     */
    public String getFullUpgradePrompt() {
        int savings = FeatureFlagsProperties.getYearlySavingsPercent();
        return String.format(
                "Upgrade to Premium for unlimited scans and refactors:\n" +
                        "• %s (billed monthly)\n" +
                        "• %s (billed yearly - save %d%%)\n\n" +
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
                getSubscriptionStatus());
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
                getSubscriptionStatus());
    }

    public static class UpgradeInfo {
        private final String featureName;
        private final String featureDescription;
        private final FeatureFlagsProperties.LicenseTier currentTier;
        private final FeatureFlagsProperties.LicenseTier requiredTier;
        private final String paymentLink;
        private final String message;
        private final String subscriptionStatus;

        public UpgradeInfo(
                String featureName,
                String featureDescription,
                FeatureFlagsProperties.LicenseTier currentTier,
                FeatureFlagsProperties.LicenseTier requiredTier,
                String paymentLink,
                String message,
                String subscriptionStatus) {
            this.featureName = featureName;
            this.featureDescription = featureDescription;
            this.currentTier = currentTier;
            this.requiredTier = requiredTier;
            this.paymentLink = paymentLink;
            this.message = message;
            this.subscriptionStatus = subscriptionStatus;
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
