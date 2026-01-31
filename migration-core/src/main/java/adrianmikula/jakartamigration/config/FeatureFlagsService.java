package adrianmikula.jakartamigration.config;

import lombok.extern.slf4j.Slf4j;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for checking feature flag availability.
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

    public String getUpgradeMessage(FeatureFlag flag) {
        return String.format(
                "The '%s' feature requires a %s license. Your current tier is %s.",
                flag.getName(),
                flag.getRequiredTier(),
                getCurrentTier());
    }

    public UpgradeInfo getUpgradeInfo(FeatureFlag flag) {
        return new UpgradeInfo(
                flag.getName(),
                flag.getDescription(),
                getCurrentTier(),
                flag.getRequiredTier(),
                null,
                getUpgradeMessage(flag));
    }

    public static class UpgradeInfo {
        private final String featureName;
        private final String featureDescription;
        private final FeatureFlagsProperties.LicenseTier currentTier;
        private final FeatureFlagsProperties.LicenseTier requiredTier;
        private final String paymentLink;
        private final String message;

        public UpgradeInfo(
                String featureName,
                String featureDescription,
                FeatureFlagsProperties.LicenseTier currentTier,
                FeatureFlagsProperties.LicenseTier requiredTier,
                String paymentLink,
                String message) {
            this.featureName = featureName;
            this.featureDescription = featureDescription;
            this.currentTier = currentTier;
            this.requiredTier = requiredTier;
            this.paymentLink = paymentLink;
            this.message = message;
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
