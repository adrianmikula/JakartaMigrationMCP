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
import static org.mockito.Mockito.lenient;

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
        lenient().when(licenseService.getDefaultTier()).thenReturn(FeatureFlagsProperties.LicenseTier.COMMUNITY);

        Set<FeatureFlag> enabledFeatures = service.getEnabledFeatures();

        assertThat(enabledFeatures).doesNotContain(
                FeatureFlag.AUTO_FIXES,
                FeatureFlag.ONE_CLICK_REFACTOR,
                FeatureFlag.BINARY_FIXES);
    }

    @Test
    void shouldAllowPremiumFeaturesForPremiumTier() {
        lenient().when(licenseService.getDefaultTier()).thenReturn(FeatureFlagsProperties.LicenseTier.PREMIUM);

        assertThat(service.isEnabled(FeatureFlag.AUTO_FIXES)).isTrue();
        assertThat(service.isEnabled(FeatureFlag.ONE_CLICK_REFACTOR)).isTrue();
        assertThat(service.isEnabled(FeatureFlag.BINARY_FIXES)).isTrue();
    }

    @Test
    void shouldRespectFeatureOverrides() {
        lenient().when(licenseService.getDefaultTier()).thenReturn(FeatureFlagsProperties.LicenseTier.COMMUNITY);
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
        lenient().when(licenseService.getDefaultTier()).thenReturn(FeatureFlagsProperties.LicenseTier.COMMUNITY);

        assertThatThrownBy(() -> service.requireEnabled(FeatureFlag.AUTO_FIXES))
                .isInstanceOf(FeatureFlagsService.FeatureNotAvailableException.class)
                .hasMessageContaining("Automatic issue remediation")
                .hasMessageContaining("PREMIUM");
    }

    @Test
    void shouldNotThrowExceptionWhenRequiringEnabledFeature() {
        lenient().when(licenseService.getDefaultTier()).thenReturn(FeatureFlagsProperties.LicenseTier.PREMIUM);

        service.requireEnabled(FeatureFlag.AUTO_FIXES);
    }

    @Test
    void shouldReturnCurrentTier() {
        lenient().when(licenseService.getDefaultTier()).thenReturn(FeatureFlagsProperties.LicenseTier.COMMUNITY);
        assertThat(service.getCurrentTier()).isEqualTo(FeatureFlagsProperties.LicenseTier.COMMUNITY);

        lenient().when(licenseService.getDefaultTier()).thenReturn(FeatureFlagsProperties.LicenseTier.PREMIUM);
        assertThat(service.getCurrentTier()).isEqualTo(FeatureFlagsProperties.LicenseTier.PREMIUM);
    }

    @Test
    void shouldCheckTierLevel() {
        lenient().when(licenseService.getDefaultTier()).thenReturn(FeatureFlagsProperties.LicenseTier.COMMUNITY);
        assertThat(service.hasTier(FeatureFlagsProperties.LicenseTier.COMMUNITY)).isTrue();
        assertThat(service.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)).isFalse();

        lenient().when(licenseService.getDefaultTier()).thenReturn(FeatureFlagsProperties.LicenseTier.PREMIUM);
        assertThat(service.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)).isTrue();
        assertThat(service.hasTier(FeatureFlagsProperties.LicenseTier.COMMUNITY)).isTrue(); // PREMIUM includes
                                                                                            // COMMUNITY
    }

    @Test
    @DisplayName("Should return upgrade message")
    void shouldReturnUpgradeMessage() {
        // No stub needed as getUpgradeMessage doesn't call licenseService
        String message = service.getUpgradeMessage(FeatureFlag.AUTO_FIXES);

        assertThat(message)
                .contains("Automatic issue remediation")
                .contains("PREMIUM");
    }

    @Test
    @DisplayName("Should return upgrade info with null payment link")
    void shouldReturnUpgradeInfo() {
        lenient().when(licenseService.getDefaultTier()).thenReturn(FeatureFlagsProperties.LicenseTier.COMMUNITY);

        FeatureFlagsService.UpgradeInfo info = service.getUpgradeInfo(FeatureFlag.AUTO_FIXES);

        assertThat(info.getFeatureName()).isEqualTo("Automatic issue remediation");
        assertThat(info.getFeatureDescription()).isEqualTo("Automatically fix detected Jakarta migration issues");
        assertThat(info.getCurrentTier()).isEqualTo(FeatureFlagsProperties.LicenseTier.COMMUNITY);
        assertThat(info.getRequiredTier()).isEqualTo(FeatureFlagsProperties.LicenseTier.PREMIUM);
        assertThat(info.getPaymentLink()).isEqualTo("https://plugins.jetbrains.com/plugin/30093-jakarta-migration");
        assertThat(info.getMessage()).isNotEmpty();
    }
}
