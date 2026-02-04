package adrianmikula.jakartamigration.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for FeatureFlagsService.
 * No payment or external licensing; tier comes from configured default.
 */
@ExtendWith(MockitoExtension.class)
class FeatureFlagsServiceTest {

    @Mock
    private LicenseService licenseService;

    private FeatureFlagsProperties properties;
    private FeatureFlagsService service;

    @BeforeEach
    void setUp() {
        properties = new FeatureFlagsProperties();
        properties.setEnabled(true);
        properties.setDefaultTier(FeatureFlagsProperties.LicenseTier.COMMUNITY);
        properties.setLicenseKey("");
        properties.setFeatures(new HashMap<>());

        service = new FeatureFlagsService(properties, licenseService);
    }

    @Test
    void shouldAllowCommunityFeaturesForCommunityTier() {
        when(licenseService.getDefaultTier()).thenReturn(FeatureFlagsProperties.LicenseTier.COMMUNITY);

        Set<FeatureFlag> enabledFeatures = service.getEnabledFeatures();

        assertThat(enabledFeatures).doesNotContain(
            FeatureFlag.AUTO_FIXES,
            FeatureFlag.ONE_CLICK_REFACTOR,
            FeatureFlag.BINARY_FIXES
        );
    }

    @Test
    void shouldAllowPremiumFeaturesForPremiumTier() {
        when(licenseService.getDefaultTier()).thenReturn(FeatureFlagsProperties.LicenseTier.PREMIUM);

        assertThat(service.isEnabled(FeatureFlag.AUTO_FIXES)).isTrue();
        assertThat(service.isEnabled(FeatureFlag.ONE_CLICK_REFACTOR)).isTrue();
        assertThat(service.isEnabled(FeatureFlag.BINARY_FIXES)).isTrue();
    }

    @Test
    void shouldRespectFeatureOverrides() {
        when(licenseService.getDefaultTier()).thenReturn(FeatureFlagsProperties.LicenseTier.COMMUNITY);
        Map<String, Boolean> overrides = new HashMap<>();
        overrides.put("auto-fixes", true);
        properties.setFeatures(overrides);

        assertThat(service.isEnabled(FeatureFlag.AUTO_FIXES)).isTrue();
    }

    @Test
    void shouldDisableAllFeaturesWhenFeatureFlagsDisabled() {
        properties.setEnabled(false);

        assertThat(service.isEnabled(FeatureFlag.AUTO_FIXES)).isTrue();
        assertThat(service.isEnabled(FeatureFlag.ONE_CLICK_REFACTOR)).isTrue();
    }

    @Test
    void shouldThrowExceptionWhenRequiringDisabledFeature() {
        when(licenseService.getDefaultTier()).thenReturn(FeatureFlagsProperties.LicenseTier.COMMUNITY);

        assertThatThrownBy(() -> service.requireEnabled(FeatureFlag.AUTO_FIXES))
            .isInstanceOf(FeatureFlagsService.FeatureNotAvailableException.class)
            .hasMessageContaining("Automatic issue remediation")
            .hasMessageContaining("PREMIUM");
    }

    @Test
    void shouldNotThrowExceptionWhenRequiringEnabledFeature() {
        when(licenseService.getDefaultTier()).thenReturn(FeatureFlagsProperties.LicenseTier.PREMIUM);

        service.requireEnabled(FeatureFlag.AUTO_FIXES);
    }

    @Test
    void shouldReturnCurrentTier() {
        when(licenseService.getDefaultTier()).thenReturn(FeatureFlagsProperties.LicenseTier.COMMUNITY);
        assertThat(service.getCurrentTier()).isEqualTo(FeatureFlagsProperties.LicenseTier.COMMUNITY);

        when(licenseService.getDefaultTier()).thenReturn(FeatureFlagsProperties.LicenseTier.PREMIUM);
        assertThat(service.getCurrentTier()).isEqualTo(FeatureFlagsProperties.LicenseTier.PREMIUM);
    }

    @Test
    void shouldCheckTierLevel() {
        when(licenseService.getDefaultTier()).thenReturn(FeatureFlagsProperties.LicenseTier.COMMUNITY);
        assertThat(service.hasTier(FeatureFlagsProperties.LicenseTier.COMMUNITY)).isTrue();
        assertThat(service.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)).isFalse();

        when(licenseService.getDefaultTier()).thenReturn(FeatureFlagsProperties.LicenseTier.PREMIUM);
        assertThat(service.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)).isTrue();
        assertThat(service.hasTier(FeatureFlagsProperties.LicenseTier.COMMUNITY)).isTrue(); // PREMIUM includes COMMUNITY
    }

    @Test
    @DisplayName("Should return upgrade message")
    void shouldReturnUpgradeMessage() {
        when(licenseService.getDefaultTier()).thenReturn(FeatureFlagsProperties.LicenseTier.COMMUNITY);
        String message = service.getUpgradeMessage(FeatureFlag.AUTO_FIXES);

        assertThat(message)
            .contains("Automatic issue remediation")
            .contains("PREMIUM");
    }

    @Test
    @DisplayName("Should return upgrade info with null payment link")
    void shouldReturnUpgradeInfo() {
        when(licenseService.getDefaultTier()).thenReturn(FeatureFlagsProperties.LicenseTier.COMMUNITY);

        FeatureFlagsService.UpgradeInfo info = service.getUpgradeInfo(FeatureFlag.AUTO_FIXES);

        assertThat(info.getFeatureName()).isEqualTo("Automatic issue remediation");
        assertThat(info.getFeatureDescription()).isEqualTo("Automatically fix detected Jakarta migration issues");
        assertThat(info.getCurrentTier()).isEqualTo(FeatureFlagsProperties.LicenseTier.COMMUNITY);
        assertThat(info.getRequiredTier()).isEqualTo(FeatureFlagsProperties.LicenseTier.PREMIUM);
        assertThat(info.getPaymentLink()).isNull();
        assertThat(info.getMessage()).isNotEmpty();
    }
}
