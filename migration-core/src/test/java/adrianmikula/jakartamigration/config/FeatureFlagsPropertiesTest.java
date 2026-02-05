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
    @DisplayName("Monthly price should be $49 USD")
    void monthlyPriceIs49() {
        assertThat(FeatureFlagsProperties.MONTHLY_PRICE_USD).isEqualTo(49.0);
    }

    @Test
    @DisplayName("Yearly price should be $399 USD")
    void yearlyPriceIs399() {
        assertThat(FeatureFlagsProperties.YEARLY_PRICE_USD).isEqualTo(399.0);
    }

    @Test
    @DisplayName("Free trial should be 7 days")
    void freeTrialIs7Days() {
        assertThat(FeatureFlagsProperties.FREE_TRIAL_DAYS).isEqualTo(7);
    }

    // === Pricing Formatted Tests ===

    @Test
    @DisplayName("Monthly price formatted should show $49/month")
    void monthlyPriceFormatted() {
        assertThat(FeatureFlagsProperties.getMonthlyPriceFormatted()).isEqualTo("$49/month");
    }

    @Test
    @DisplayName("Yearly price formatted should show $399/year")
    void yearlyPriceFormatted() {
        assertThat(FeatureFlagsProperties.getYearlyPriceFormatted()).isEqualTo("$399/year");
    }

    // === Yearly Savings Tests ===

    @Test
    @DisplayName("Yearly savings should be calculated correctly")
    void yearlySavingsCalculated() {
        int savings = FeatureFlagsProperties.getYearlySavingsPercent();
        // 12 * $49 = $588, yearly is $399, savings = ($588 - $399) / $588 = 32%
        assertThat(savings).isGreaterThan(0);
        assertThat(savings).isLessThanOrEqualTo(40); // Should be around 32%
    }

    // === Marketplace URL Tests ===

    @Test
    @DisplayName("Marketplace URL should be non-empty")
    void marketplaceUrlIsNotEmpty() {
        String url = FeatureFlagsProperties.getMarketplaceUrl();
        assertThat(url).isNotNull().isNotEmpty();
        assertThat(url).contains("plugins.jetbrains.com");
    }

    // === Subscription Status Tests ===

    @Test
    @DisplayName("Should return false when trial not started and tier is COMMUNITY")
    void noSubscriptionWhenCommunityAndNoTrial() {
        FeatureFlagsProperties props = new FeatureFlagsProperties();
        props.setDefaultTier(FeatureFlagsProperties.LicenseTier.COMMUNITY);
        props.setTrialEndTimestamp(null);

        assertThat(props.hasActiveSubscription()).isFalse();
    }

    @Test
    @DisplayName("Should return true when tier is PREMIUM")
    void hasSubscriptionWhenPremium() {
        FeatureFlagsProperties props = new FeatureFlagsProperties();
        props.setDefaultTier(FeatureFlagsProperties.LicenseTier.PREMIUM);
        props.setTrialEndTimestamp(null);

        assertThat(props.hasActiveSubscription()).isTrue();
    }

    @Test
    @DisplayName("Should return true during active trial")
    void hasSubscriptionDuringTrial() {
        FeatureFlagsProperties props = new FeatureFlagsProperties();
        props.setDefaultTier(FeatureFlagsProperties.LicenseTier.COMMUNITY);
        // Set trial to expire 5 days from now
        props.setTrialEndTimestamp(System.currentTimeMillis() + (5L * 24 * 60 * 60 * 1000));

        assertThat(props.hasActiveSubscription()).isTrue();
        assertThat(props.getRemainingTrialDays()).isGreaterThan(0);
        assertThat(props.getRemainingTrialDays()).isLessThanOrEqualTo(5);
    }

    @Test
    @DisplayName("Should return 0 days remaining after trial expired")
    void zeroDaysAfterTrialExpired() {
        FeatureFlagsProperties props = new FeatureFlagsProperties();
        props.setDefaultTier(FeatureFlagsProperties.LicenseTier.COMMUNITY);
        // Set trial to expired yesterday
        props.setTrialEndTimestamp(System.currentTimeMillis() - (24L * 60 * 60 * 1000));

        assertThat(props.hasActiveSubscription()).isFalse();
        assertThat(props.getRemainingTrialDays()).isEqualTo(0);
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

    @Test
    @DisplayName("Trial end timestamp should be null by default")
    void trialEndTimestampNullByDefault() {
        FeatureFlagsProperties props = new FeatureFlagsProperties();
        assertThat(props.getTrialEndTimestamp()).isNull();
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
}
