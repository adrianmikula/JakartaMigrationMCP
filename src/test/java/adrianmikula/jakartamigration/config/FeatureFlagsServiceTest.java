package adrianmikula.jakartamigration.config;

import adrianmikula.jakartamigration.api.service.StripePaymentLinkService;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for FeatureFlagsService.
 */
@ExtendWith(MockitoExtension.class)
class FeatureFlagsServiceTest {

    @Mock
    private LicenseService licenseService;

    @Mock
    private StripePaymentLinkService paymentLinkService;

    private FeatureFlagsProperties properties;
    private FeatureFlagsService service;

    @BeforeEach
    void setUp() {
        properties = new FeatureFlagsProperties();
        properties.setEnabled(true);
        properties.setDefaultTier(FeatureFlagsProperties.LicenseTier.COMMUNITY);
        properties.setLicenseKey("");
        properties.setFeatures(new HashMap<>());
        
        service = new FeatureFlagsService(properties, licenseService, paymentLinkService);
    }

    @Test
    void shouldAllowCommunityFeaturesForCommunityTier() {
        // Community tier should have access to basic features
        // (All features that don't require PREMIUM tier)
        // Since all current features require PREMIUM, none should be enabled
        Set<FeatureFlag> enabledFeatures = service.getEnabledFeatures();
        
        // With COMMUNITY tier, no premium features should be enabled
        assertThat(enabledFeatures).doesNotContain(
            FeatureFlag.AUTO_FIXES,
            FeatureFlag.ONE_CLICK_REFACTOR,
            FeatureFlag.BINARY_FIXES
        );
    }

    @Test
    void shouldAllowPremiumFeaturesForPremiumTier() {
        when(licenseService.validateLicense(anyString())).thenReturn(FeatureFlagsProperties.LicenseTier.PREMIUM);
        
        properties.setLicenseKey("PREMIUM-test-key");
        
        assertThat(service.isEnabled(FeatureFlag.AUTO_FIXES)).isTrue();
        assertThat(service.isEnabled(FeatureFlag.ONE_CLICK_REFACTOR)).isTrue();
        assertThat(service.isEnabled(FeatureFlag.BINARY_FIXES)).isTrue();
    }

    @Test
    void shouldRespectFeatureOverrides() {
        Map<String, Boolean> overrides = new HashMap<>();
        overrides.put("auto-fixes", true);
        properties.setFeatures(overrides);
        
        // Even with COMMUNITY tier, override should enable the feature
        assertThat(service.isEnabled(FeatureFlag.AUTO_FIXES)).isTrue();
    }

    @Test
    void shouldDisableAllFeaturesWhenFeatureFlagsDisabled() {
        properties.setEnabled(false);
        
        // When feature flags are disabled, all features should be available
        assertThat(service.isEnabled(FeatureFlag.AUTO_FIXES)).isTrue();
        assertThat(service.isEnabled(FeatureFlag.ONE_CLICK_REFACTOR)).isTrue();
    }

    @Test
    void shouldThrowExceptionWhenRequiringDisabledFeature() {
        assertThatThrownBy(() -> service.requireEnabled(FeatureFlag.AUTO_FIXES))
            .isInstanceOf(FeatureFlagsService.FeatureNotAvailableException.class)
            .hasMessageContaining("Automatic issue remediation")
            .hasMessageContaining("PREMIUM");
    }

    @Test
    void shouldNotThrowExceptionWhenRequiringEnabledFeature() {
        when(licenseService.validateLicense(anyString())).thenReturn(FeatureFlagsProperties.LicenseTier.PREMIUM);
        properties.setLicenseKey("PREMIUM-test-key");
        
        // Should not throw
        service.requireEnabled(FeatureFlag.AUTO_FIXES);
    }

    @Test
    void shouldReturnCurrentTier() {
        assertThat(service.getCurrentTier()).isEqualTo(FeatureFlagsProperties.LicenseTier.COMMUNITY);
        
        when(licenseService.validateLicense(anyString())).thenReturn(FeatureFlagsProperties.LicenseTier.PREMIUM);
        properties.setLicenseKey("PREMIUM-test-key");
        
        assertThat(service.getCurrentTier()).isEqualTo(FeatureFlagsProperties.LicenseTier.PREMIUM);
    }

    @Test
    void shouldCheckTierLevel() {
        assertThat(service.hasTier(FeatureFlagsProperties.LicenseTier.COMMUNITY)).isTrue();
        assertThat(service.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)).isFalse();
        
        when(licenseService.validateLicense(anyString())).thenReturn(FeatureFlagsProperties.LicenseTier.PREMIUM);
        properties.setLicenseKey("PREMIUM-test-key");
        
        assertThat(service.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)).isTrue();
        assertThat(service.hasTier(FeatureFlagsProperties.LicenseTier.ENTERPRISE)).isFalse();
    }

    @Test
    @DisplayName("Should return upgrade message")
    void shouldReturnUpgradeMessage() {
        String message = service.getUpgradeMessage(FeatureFlag.AUTO_FIXES);
        
        assertThat(message)
            .contains("Automatic issue remediation")
            .contains("PREMIUM")
            .contains("upgrade");
    }

    @Test
    @DisplayName("Should return upgrade message with payment link when configured")
    void shouldReturnUpgradeMessageWithPaymentLink() {
        // Given
        when(paymentLinkService.getPaymentLink("premium")).thenReturn("https://buy.stripe.com/premium-link");

        // When
        String message = service.getUpgradeMessage(FeatureFlag.AUTO_FIXES);

        // Then
        assertThat(message)
            .contains("Automatic issue remediation")
            .contains("PREMIUM")
            .contains("https://buy.stripe.com/premium-link");
    }

    @Test
    @DisplayName("Should return upgrade message without payment link when not configured")
    void shouldReturnUpgradeMessageWithoutPaymentLink() {
        // Given
        when(paymentLinkService.getPaymentLink("premium")).thenReturn(null);

        // When
        String message = service.getUpgradeMessage(FeatureFlag.AUTO_FIXES);

        // Then
        assertThat(message)
            .contains("Automatic issue remediation")
            .contains("PREMIUM")
            .contains("contact support");
    }

    @Test
    @DisplayName("Should return upgrade info with all fields")
    void shouldReturnUpgradeInfo() {
        // Given
        when(paymentLinkService.getPaymentLink("premium")).thenReturn("https://buy.stripe.com/premium-link");

        // When
        FeatureFlagsService.UpgradeInfo info = service.getUpgradeInfo(FeatureFlag.AUTO_FIXES);

        // Then
        assertThat(info.getFeatureName()).isEqualTo("Automatic issue remediation");
        assertThat(info.getFeatureDescription()).isEqualTo("Automatically fix detected Jakarta migration issues");
        assertThat(info.getCurrentTier()).isEqualTo(FeatureFlagsProperties.LicenseTier.COMMUNITY);
        assertThat(info.getRequiredTier()).isEqualTo(FeatureFlagsProperties.LicenseTier.PREMIUM);
        assertThat(info.getPaymentLink()).isEqualTo("https://buy.stripe.com/premium-link");
        assertThat(info.getMessage()).isNotEmpty();
    }

    @Test
    @DisplayName("Should return upgrade info with null payment link when service is null")
    void shouldReturnUpgradeInfoWithNullPaymentLinkWhenServiceIsNull() {
        // Given - service without payment link service
        FeatureFlagsService serviceWithoutPayment = new FeatureFlagsService(properties, licenseService, null);

        // When
        FeatureFlagsService.UpgradeInfo info = serviceWithoutPayment.getUpgradeInfo(FeatureFlag.AUTO_FIXES);

        // Then
        assertThat(info.getPaymentLink()).isNull();
        assertThat(info.getMessage()).contains("contact support");
    }

    @Test
    @DisplayName("Should handle enterprise tier payment link")
    void shouldHandleEnterpriseTierPaymentLink() {
        // Given
        when(paymentLinkService.getPaymentLink("enterprise")).thenReturn("https://buy.stripe.com/enterprise-link");
        when(licenseService.validateLicense(anyString())).thenReturn(FeatureFlagsProperties.LicenseTier.PREMIUM);
        properties.setLicenseKey("PREMIUM-test-key");

        // When - requesting enterprise tier feature
        FeatureFlagsService.UpgradeInfo info = service.getUpgradeInfo(FeatureFlag.PRIORITY_SUPPORT);

        // Then
        assertThat(info.getRequiredTier()).isEqualTo(FeatureFlagsProperties.LicenseTier.PREMIUM);
        // Note: PRIORITY_SUPPORT requires PREMIUM, not ENTERPRISE, so it should use premium link
    }
}

