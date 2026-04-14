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

    // === Upgrade Prompt Tests ===

    @Test
    @DisplayName("Should return upgrade prompt with pricing")
    void shouldReturnUpgradePromptWithPricing() {
        String prompt = licenseService.getUpgradePrompt();
        assertThat(prompt).contains("$5/month");
        assertThat(prompt).contains("$50/year");
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
