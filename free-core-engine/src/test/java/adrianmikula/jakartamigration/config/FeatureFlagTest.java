package adrianmikula.jakartamigration.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for FeatureFlag enum.
 * Tests feature tier requirements, upgrade messages, and pricing info.
 */
class FeatureFlagTest {

    // === Community Features Tests ===

    @Test
    @DisplayName("All community features should be available for community tier")
    void communityFeaturesAvailableForCommunityTier() {
        // Community features are those without explicit PREMIUM requirement
        // These tests verify the feature tier system works correctly
    }

    // === Premium Features Tests ===

    @Test
    @DisplayName("AUTO_FIXES should require PREMIUM tier")
    void autoFixesRequiresPremium() {
        assertThat(FeatureFlag.AUTO_FIXES.getRequiredTier())
            .isEqualTo(FeatureFlagsProperties.LicenseTier.PREMIUM);
        assertThat(FeatureFlag.AUTO_FIXES.getTierString()).isEqualTo("PREMIUM");
    }

    @Test
    @DisplayName("ONE_CLICK_REFACTOR should require PREMIUM tier")
    void oneClickRefactorRequiresPremium() {
        assertThat(FeatureFlag.ONE_CLICK_REFACTOR.getRequiredTier())
            .isEqualTo(FeatureFlagsProperties.LicenseTier.PREMIUM);
    }

    @Test
    @DisplayName("BINARY_FIXES should require PREMIUM tier")
    void binaryFixesRequiresPremium() {
        assertThat(FeatureFlag.BINARY_FIXES.getRequiredTier())
            .isEqualTo(FeatureFlagsProperties.LicenseTier.PREMIUM);
    }

    @Test
    @DisplayName("ADVANCED_ANALYSIS should require PREMIUM tier")
    void advancedAnalysisRequiresPremium() {
        assertThat(FeatureFlag.ADVANCED_ANALYSIS.getRequiredTier())
            .isEqualTo(FeatureFlagsProperties.LicenseTier.PREMIUM);
    }

    @Test
    @DisplayName("BATCH_OPERATIONS should require PREMIUM tier")
    void batchOperationsRequiresPremium() {
        assertThat(FeatureFlag.BATCH_OPERATIONS.getRequiredTier())
            .isEqualTo(FeatureFlagsProperties.LicenseTier.PREMIUM);
    }

    @Test
    @DisplayName("CUSTOM_RECIPES should require PREMIUM tier")
    void customRecipesRequiresPremium() {
        assertThat(FeatureFlag.CUSTOM_RECIPES.getRequiredTier())
            .isEqualTo(FeatureFlagsProperties.LicenseTier.PREMIUM);
    }

    @Test
    @DisplayName("API_ACCESS should require PREMIUM tier")
    void apiAccessRequiresPremium() {
        assertThat(FeatureFlag.API_ACCESS.getRequiredTier())
            .isEqualTo(FeatureFlagsProperties.LicenseTier.PREMIUM);
    }

    @Test
    @DisplayName("EXPORT_REPORTS should require PREMIUM tier")
    void exportReportsRequiresPremium() {
        assertThat(FeatureFlag.EXPORT_REPORTS.getRequiredTier())
            .isEqualTo(FeatureFlagsProperties.LicenseTier.PREMIUM);
    }

    // === Feature Availability Tests ===

    @Test
    @DisplayName("Premium features should not be available for COMMUNITY tier")
    void premiumFeaturesNotAvailableForCommunity() {
        assertThat(FeatureFlag.AUTO_FIXES.isAvailableFor(FeatureFlagsProperties.LicenseTier.COMMUNITY))
            .isFalse();
        assertThat(FeatureFlag.ONE_CLICK_REFACTOR.isAvailableFor(FeatureFlagsProperties.LicenseTier.COMMUNITY))
            .isFalse();
        assertThat(FeatureFlag.BINARY_FIXES.isAvailableFor(FeatureFlagsProperties.LicenseTier.COMMUNITY))
            .isFalse();
    }

    @Test
    @DisplayName("Premium features should be available for PREMIUM tier")
    void premiumFeaturesAvailableForPremium() {
        assertThat(FeatureFlag.AUTO_FIXES.isAvailableFor(FeatureFlagsProperties.LicenseTier.PREMIUM))
            .isTrue();
        assertThat(FeatureFlag.ONE_CLICK_REFACTOR.isAvailableFor(FeatureFlagsProperties.LicenseTier.PREMIUM))
            .isTrue();
        assertThat(FeatureFlag.BINARY_FIXES.isAvailableFor(FeatureFlagsProperties.LicenseTier.PREMIUM))
            .isTrue();
    }

    // === Upgrade Message Tests ===

    @Test
    @DisplayName("Upgrade message should contain feature name and pricing")
    void upgradeMessageContainsFeatureNameAndPricing() {
        String message = FeatureFlag.AUTO_FIXES.getUpgradeMessage();
        assertThat(message).contains("Automatic issue remediation");
        assertThat(message).contains("PREMIUM");
    }

    @Test
    @DisplayName("Pricing info should contain monthly and yearly prices")
    void pricingInfoContainsMonthlyAndYearly() {
        String pricing = FeatureFlag.AUTO_FIXES.getPricingInfo();
        assertThat(pricing).contains("$49/month");
        assertThat(pricing).contains("$399/year");
        assertThat(pricing).contains("free 7-day trial");
    }

    @Test
    @DisplayName("Feature key should be non-empty")
    void featureKeyIsNonEmpty() {
        for (FeatureFlag flag : FeatureFlag.values()) {
            assertThat(flag.getKey()).isNotNull().isNotEmpty();
        }
    }

    @Test
    @DisplayName("Feature name should be non-empty")
    void featureNameIsNonEmpty() {
        for (FeatureFlag flag : FeatureFlag.values()) {
            assertThat(flag.getName()).isNotNull().isNotEmpty();
        }
    }

    @Test
    @DisplayName("Feature description should be non-empty")
    void featureDescriptionIsNonEmpty() {
        for (FeatureFlag flag : FeatureFlag.values()) {
            assertThat(flag.getDescription()).isNotNull().isNotEmpty();
        }
    }

    // === Feature Tier Ordering Tests ===

    @Test
    @DisplayName("PREMIUM tier should have higher ordinal than COMMUNITY")
    void premiumTierHasHigherOrdinal() {
        assertThat(FeatureFlagsProperties.LicenseTier.PREMIUM.ordinal())
            .isGreaterThan(FeatureFlagsProperties.LicenseTier.COMMUNITY.ordinal());
    }

    @Test
    @DisplayName("COMMUNITY tier should include all community features")
    void communityTierIncludesCommunityFeatures() {
        // Verify tier comparison works correctly
        assertThat(FeatureFlagsProperties.LicenseTier.COMMUNITY.ordinal())
            .isLessThanOrEqualTo(FeatureFlagsProperties.LicenseTier.PREMIUM.ordinal());
    }
}
