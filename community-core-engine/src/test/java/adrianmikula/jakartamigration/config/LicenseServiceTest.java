package adrianmikula.jakartamigration.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for LicenseService.
 * Tests license tier determination, trial activation flow, and subscription
 * status.
 */
@ExtendWith(MockitoExtension.class)
class LicenseServiceTest {

    @Mock
    private FeatureFlagsProperties properties;

    private LicenseService licenseService;

    @BeforeEach
    void setUp() {
        licenseService = new LicenseService(properties);
    }

    // === License Tier Tests ===

    @Test
    @DisplayName("Should return configured default tier")
    void shouldReturnDefaultTierFromProperties() {
        lenient().when(properties.getDefaultTier()).thenReturn(FeatureFlagsProperties.LicenseTier.PREMIUM);
        assertThat(licenseService.getDefaultTier()).isEqualTo(FeatureFlagsProperties.LicenseTier.PREMIUM);
    }

    @Test
    @DisplayName("Should return COMMUNITY when configured")
    void shouldReturnCommunityWhenConfigured() {
        lenient().when(properties.getDefaultTier()).thenReturn(FeatureFlagsProperties.LicenseTier.COMMUNITY);
        assertThat(licenseService.getDefaultTier()).isEqualTo(FeatureFlagsProperties.LicenseTier.COMMUNITY);
    }

    @Test
    @DisplayName("Should return PREMIUM when configured")
    void shouldReturnPremiumWhenConfigured() {
        lenient().when(properties.getDefaultTier()).thenReturn(FeatureFlagsProperties.LicenseTier.PREMIUM);
        assertThat(licenseService.getDefaultTier()).isEqualTo(FeatureFlagsProperties.LicenseTier.PREMIUM);
    }

    // === Subscription Status Tests ===

    @Test
    @DisplayName("Should return false for community tier subscription")
    void shouldReturnFalseForCommunitySubscription() {
        lenient().when(properties.getDefaultTier()).thenReturn(FeatureFlagsProperties.LicenseTier.COMMUNITY);
        assertThat(licenseService.hasActiveSubscription()).isFalse();
    }

    @Test
    @DisplayName("Should return true for premium tier subscription")
    void shouldReturnTrueForPremiumSubscription() {
        lenient().when(properties.getDefaultTier()).thenReturn(FeatureFlagsProperties.LicenseTier.PREMIUM);
        assertThat(licenseService.hasActiveSubscription()).isTrue();
    }

    // === Trial Activation Tests ===

    @Test
    @DisplayName("Should return false for subscription when trial not started")
    void shouldReturnFalseWhenTrialNotStarted() {
        lenient().when(properties.getDefaultTier()).thenReturn(FeatureFlagsProperties.LicenseTier.COMMUNITY);
        lenient().when(properties.getTrialEndTimestamp()).thenReturn(null);
        assertThat(licenseService.hasActiveSubscription()).isFalse();
        assertThat(licenseService.isTrialActive()).isFalse();
        assertThat(licenseService.getRemainingTrialDays()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should return true for subscription during active trial")
    void shouldReturnTrueDuringActiveTrial() {
        lenient().when(properties.getDefaultTier()).thenReturn(FeatureFlagsProperties.LicenseTier.COMMUNITY);
        // Set trial to expire 5 days from now
        long trialEnd = System.currentTimeMillis() + (5L * 24 * 60 * 60 * 1000);
        lenient().when(properties.getTrialEndTimestamp()).thenReturn(trialEnd);
        assertThat(licenseService.hasActiveSubscription()).isTrue();
        assertThat(licenseService.isTrialActive()).isTrue();
        assertThat(licenseService.getRemainingTrialDays()).isGreaterThan(0);
        assertThat(licenseService.getRemainingTrialDays()).isLessThanOrEqualTo(5);
    }

    @Test
    @DisplayName("Should return false for subscription after trial expired")
    void shouldReturnFalseAfterTrialExpired() {
        lenient().when(properties.getDefaultTier()).thenReturn(FeatureFlagsProperties.LicenseTier.COMMUNITY);
        // Set trial to expired yesterday
        long trialEnd = System.currentTimeMillis() - (24L * 60 * 60 * 1000);
        lenient().when(properties.getTrialEndTimestamp()).thenReturn(trialEnd);
        assertThat(licenseService.hasActiveSubscription()).isFalse();
        assertThat(licenseService.isTrialActive()).isFalse();
        assertThat(licenseService.getRemainingTrialDays()).isEqualTo(0);
    }

    // === Subscription Status String Tests ===

    @Test
    @DisplayName("Should return Community status for free tier")
    void shouldReturnCommunityStatusForFreeTier() {
        lenient().when(properties.getDefaultTier()).thenReturn(FeatureFlagsProperties.LicenseTier.COMMUNITY);
        lenient().when(properties.getTrialEndTimestamp()).thenReturn(null);
        assertThat(licenseService.getSubscriptionStatus()).isEqualTo("Community (Free)");
    }

    @Test
    @DisplayName("Should return Premium status for paid subscription")
    void shouldReturnPremiumStatusForPaidSubscription() {
        lenient().when(properties.getDefaultTier()).thenReturn(FeatureFlagsProperties.LicenseTier.PREMIUM);
        assertThat(licenseService.getSubscriptionStatus()).isEqualTo("Premium Subscription");
    }

    @Test
    @DisplayName("Should return Trial status with days remaining")
    void shouldReturnTrialStatusWithDaysRemaining() {
        lenient().when(properties.getDefaultTier()).thenReturn(FeatureFlagsProperties.LicenseTier.COMMUNITY);
        long trialEnd = System.currentTimeMillis() + (5L * 24 * 60 * 60 * 1000);
        lenient().when(properties.getTrialEndTimestamp()).thenReturn(trialEnd);
        String status = licenseService.getSubscriptionStatus();
        assertThat(status).contains("Trial");
        assertThat(status).contains("days remaining");
    }

    // === Upgrade Prompt Tests ===

    @Test
    @DisplayName("Should return upgrade prompt with pricing")
    void shouldReturnUpgradePromptWithPricing() {
        String prompt = licenseService.getUpgradePrompt();
        assertThat(prompt).contains("$49/month");
        assertThat(prompt).contains("$399/year");
        assertThat(prompt).contains("Upgrade to Premium");
    }

    // === System Property Override Tests ===

    @Test
    @DisplayName("Should override tier via system property - PREMIUM")
    void shouldOverrideTierWithSystemPropertyPremium() {
        try {
            System.setProperty("jakarta.migration.license.tier", "PREMIUM");
            lenient().when(properties.getDefaultTier()).thenReturn(FeatureFlagsProperties.LicenseTier.COMMUNITY);
            assertThat(licenseService.getDefaultTier()).isEqualTo(FeatureFlagsProperties.LicenseTier.PREMIUM);
        } finally {
            System.clearProperty("jakarta.migration.license.tier");
        }
    }

    @Test
    @DisplayName("Should override tier via system property - COMMUNITY")
    void shouldOverrideTierWithSystemPropertyCommunity() {
        try {
            System.setProperty("jakarta.migration.license.tier", "COMMUNITY");
            lenient().when(properties.getDefaultTier()).thenReturn(FeatureFlagsProperties.LicenseTier.PREMIUM);
            assertThat(licenseService.getDefaultTier()).isEqualTo(FeatureFlagsProperties.LicenseTier.COMMUNITY);
        } finally {
            System.clearProperty("jakarta.migration.license.tier");
        }
    }

    @Test
    @DisplayName("Should ignore invalid system property value")
    void shouldIgnoreInvalidSystemPropertyValue() {
        try {
            System.setProperty("jakarta.migration.license.tier", "INVALID");
            lenient().when(properties.getDefaultTier()).thenReturn(FeatureFlagsProperties.LicenseTier.PREMIUM);
            assertThat(licenseService.getDefaultTier()).isEqualTo(FeatureFlagsProperties.LicenseTier.PREMIUM);
        } finally {
            System.clearProperty("jakarta.migration.license.tier");
        }
    }
}
