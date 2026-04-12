package adrianmikula.jakartamigration.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for FeatureFlagsProperties.
 * Tests pricing constants, trial duration, and utility methods.
 */
class FeatureFlagsPropertiesTest {

    // === Pricing Constants Tests ===

    @Test
    @DisplayName("Monthly price should be $5 USD")
    void monthlyPriceIs5() {
        assertThat(FeatureFlagsProperties.getMonthlyPriceUsd()).isEqualTo(5.0);
    }

    @Test
    @DisplayName("Yearly price should be $50 USD")
    void yearlyPriceIs50() {
        assertThat(FeatureFlagsProperties.getYearlyPriceUsd()).isEqualTo(50.0);
    }

    // === Pricing Formatted Tests ===

    @Test
    @DisplayName("Monthly price formatted should show $5/month")
    void monthlyPriceFormatted() {
        assertThat(FeatureFlagsProperties.getMonthlyPriceFormatted()).isEqualTo("$5/month");
    }

    @Test
    @DisplayName("Yearly price formatted should show $50/year")
    void yearlyPriceFormatted() {
        assertThat(FeatureFlagsProperties.getYearlyPriceFormatted()).isEqualTo("$50/year");
    }

    // === Yearly Savings Tests ===

    @Test
    @DisplayName("Yearly savings should be calculated correctly")
    void yearlySavingsCalculated() {
        int savings = FeatureFlagsProperties.getYearlySavingsPercent();
        // 12 * $5 = $60, yearly is $50, savings = ($60 - $50) / $60 = 17%
        assertThat(savings).isGreaterThan(0);
        assertThat(savings).isLessThanOrEqualTo(20); // Should be around 17%
    }

    // === Marketplace URL Tests ===

    @Test
    @DisplayName("Marketplace URL should be non-empty")
    void marketplaceUrlIsNotEmpty() {
        String url = FeatureFlagsProperties.getMarketplaceUrl();
        assertThat(url).isNotNull().isNotEmpty();
        assertThat(url).contains("plugins.jetbrains.com");
    }

    // === Default Values Tests ===

    @Test
    @DisplayName("Feature flags should be enabled by default")
    void featureFlagsEnabledByDefault() {
        FeatureFlagsProperties props = new FeatureFlagsProperties();
        assertThat(props.getEnabled()).isTrue();
    }

    @Test
    @DisplayName("Default tier should be COMMUNITY")
    void defaultTierIsCommunity() {
        FeatureFlagsProperties props = new FeatureFlagsProperties();
        assertThat(props.getDefaultTier()).isEqualTo(FeatureFlagsProperties.LicenseTier.COMMUNITY);
    }

    @Test
    @DisplayName("License key should be empty by default")
    void licenseKeyEmptyByDefault() {
        FeatureFlagsProperties props = new FeatureFlagsProperties();
        assertThat(props.getLicenseKey()).isEmpty();
    }

    // === License Tier Enum Tests ===

    @Test
    @DisplayName("COMMUNITY and PREMIUM tiers should exist")
    void licenseTiersExist() {
        assertThat(FeatureFlagsProperties.LicenseTier.COMMUNITY).isNotNull();
        assertThat(FeatureFlagsProperties.LicenseTier.PREMIUM).isNotNull();
    }

    @Test
    @DisplayName("COMMUNITY should have lower ordinal than PREMIUM")
    void communityOrdinalLowerThanPremium() {
        assertThat(FeatureFlagsProperties.LicenseTier.COMMUNITY.ordinal())
            .isLessThan(FeatureFlagsProperties.LicenseTier.PREMIUM.ordinal());
    }

    @Test
    @DisplayName("Should load pricing properties from resources")
    void shouldLoadPricingPropertiesFromResources() {
        // Test that default values are loaded correctly
        assertThat(FeatureFlagsProperties.getMonthlyPriceUsd()).isEqualTo(5.0);
        assertThat(FeatureFlagsProperties.getYearlyPriceUsd()).isEqualTo(50.0);
    }
}
