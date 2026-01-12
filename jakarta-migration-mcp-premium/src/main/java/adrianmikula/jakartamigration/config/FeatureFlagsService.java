package adrianmikula.jakartamigration.config;

import adrianmikula.jakartamigration.api.service.StripePaymentLinkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for checking feature flag availability.
 * 
 * This service determines which features are available based on:
 * - License tier (COMMUNITY, PREMIUM, ENTERPRISE)
 * - Feature flag configuration
 * - License key validation (future)
 * 
 * Usage:
 * <pre>
 * if (featureFlagsService.isEnabled(FeatureFlag.AUTO_FIXES)) {
 *     // Execute auto-fix logic
 * } else {
 *     // Return upgrade message
 * }
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureFlagsService {

    private final FeatureFlagsProperties properties;
    private final LicenseService licenseService;
    @Nullable
    private final StripePaymentLinkService paymentLinkService;

    /**
     * Check if a feature flag is enabled.
     * 
     * @param flag The feature flag to check
     * @return true if the feature is enabled, false otherwise
     */
    public boolean isEnabled(FeatureFlag flag) {
        // If feature flags are disabled, all features are available (for development)
        if (!properties.getEnabled()) {
            log.debug("Feature flags disabled, allowing feature: {}", flag.getKey());
            return true;
        }

        // Check per-feature override first
        Boolean override = properties.getFeatures().get(flag.getKey());
        if (override != null) {
            log.debug("Feature {} override: {}", flag.getKey(), override);
            return override;
        }

        // Determine current license tier
        FeatureFlagsProperties.LicenseTier currentTier = getCurrentTier();

        // Check if feature is available for current tier
        boolean available = flag.isAvailableFor(currentTier);
        
        if (!available) {
            log.debug("Feature {} requires tier {}, current tier: {}", 
                flag.getKey(), flag.getRequiredTier(), currentTier);
        }

        return available;
    }

    /**
     * Check if a feature is enabled, throwing an exception if not.
     * Useful for enforcing premium features.
     * 
     * @param flag The feature flag to check
     * @throws FeatureNotAvailableException if the feature is not enabled
     */
    public void requireEnabled(FeatureFlag flag) {
        if (!isEnabled(flag)) {
            throw new FeatureNotAvailableException(
                flag,
                getCurrentTier(),
                flag.getRequiredTier()
            );
        }
    }

    /**
     * Get all enabled features for the current license tier.
     * 
     * @return Set of enabled feature flags
     */
    public Set<FeatureFlag> getEnabledFeatures() {
        if (!properties.getEnabled()) {
            return EnumSet.allOf(FeatureFlag.class);
        }

        FeatureFlagsProperties.LicenseTier currentTier = getCurrentTier();
        
        return EnumSet.allOf(FeatureFlag.class).stream()
            .filter(flag -> {
                // Check override first
                Boolean override = properties.getFeatures().get(flag.getKey());
                if (override != null) {
                    return override;
                }
                // Check tier availability
                return flag.isAvailableFor(currentTier);
            })
            .collect(Collectors.toSet());
    }

    /**
     * Get the current license tier.
     * 
     * @return Current license tier
     */
    public FeatureFlagsProperties.LicenseTier getCurrentTier() {
        // Validate license key if provided
        if (properties.getLicenseKey() != null && !properties.getLicenseKey().isBlank()) {
            FeatureFlagsProperties.LicenseTier validatedTier = licenseService.validateLicense(properties.getLicenseKey());
            if (validatedTier != null) {
                return validatedTier;
            }
            log.warn("Invalid license key provided, falling back to default tier");
        }

        return properties.getDefaultTier();
    }

    /**
     * Check if the current tier is at least the specified tier.
     * 
     * @param tier Minimum required tier
     * @return true if current tier meets or exceeds the requirement
     */
    public boolean hasTier(FeatureFlagsProperties.LicenseTier tier) {
        FeatureFlagsProperties.LicenseTier currentTier = getCurrentTier();
        return currentTier.ordinal() >= tier.ordinal();
    }

    /**
     * Get upgrade message for a feature that requires a higher tier.
     * Includes payment link if available.
     * 
     * @param flag The feature flag
     * @return Upgrade message with purchase link
     */
    public String getUpgradeMessage(FeatureFlag flag) {
        FeatureFlagsProperties.LicenseTier currentTier = getCurrentTier();
        String paymentLink = getPaymentLinkForTier(flag.getRequiredTier());
        
        StringBuilder message = new StringBuilder();
        message.append(String.format(
            "The '%s' feature requires a %s license, but your current tier is %s.",
            flag.getName(),
            flag.getRequiredTier(),
            currentTier
        ));
        
        if (paymentLink != null && !paymentLink.isBlank()) {
            message.append(String.format(
                " To unlock this feature, please upgrade at: %s",
                paymentLink
            ));
        } else {
            message.append(" Please configure a payment link or contact support to upgrade.");
        }
        
        return message.toString();
    }

    /**
     * Get upgrade information as a structured object for JSON responses.
     * 
     * @param flag The feature flag
     * @return Upgrade information with payment link
     */
    public UpgradeInfo getUpgradeInfo(FeatureFlag flag) {
        FeatureFlagsProperties.LicenseTier currentTier = getCurrentTier();
        String paymentLink = getPaymentLinkForTier(flag.getRequiredTier());
        
        return new UpgradeInfo(
            flag.getName(),
            flag.getDescription(),
            currentTier,
            flag.getRequiredTier(),
            paymentLink,
            getUpgradeMessage(flag)
        );
    }

    /**
     * Get payment link for a specific tier.
     * 
     * @param tier The license tier
     * @return Payment link URL, or null if not configured
     */
    private String getPaymentLinkForTier(FeatureFlagsProperties.LicenseTier tier) {
        if (paymentLinkService == null) {
            return null;
        }
        
        // Map tier to product name
        String productName = switch (tier) {
            case PREMIUM -> "premium";
            case ENTERPRISE -> "enterprise";
            case COMMUNITY -> null; // No payment link for community
        };
        
        if (productName == null) {
            return null;
        }
        
        return paymentLinkService.getPaymentLink(productName);
    }

    /**
     * Upgrade information data class.
     */
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
            String message
        ) {
            this.featureName = featureName;
            this.featureDescription = featureDescription;
            this.currentTier = currentTier;
            this.requiredTier = requiredTier;
            this.paymentLink = paymentLink;
            this.message = message;
        }

        public String getFeatureName() { return featureName; }
        public String getFeatureDescription() { return featureDescription; }
        public FeatureFlagsProperties.LicenseTier getCurrentTier() { return currentTier; }
        public FeatureFlagsProperties.LicenseTier getRequiredTier() { return requiredTier; }
        public String getPaymentLink() { return paymentLink; }
        public String getMessage() { return message; }
    }

    /**
     * Exception thrown when a premium feature is accessed without proper license.
     */
    public static class FeatureNotAvailableException extends RuntimeException {
        private final FeatureFlag flag;
        private final FeatureFlagsProperties.LicenseTier currentTier;
        private final FeatureFlagsProperties.LicenseTier requiredTier;

        public FeatureNotAvailableException(
            FeatureFlag flag,
            FeatureFlagsProperties.LicenseTier currentTier,
            FeatureFlagsProperties.LicenseTier requiredTier
        ) {
            super(String.format(
                "Feature '%s' requires %s license, but current tier is %s",
                flag.getName(),
                requiredTier,
                currentTier
            ));
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

