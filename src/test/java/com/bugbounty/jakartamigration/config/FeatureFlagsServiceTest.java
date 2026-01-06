package com.bugbounty.jakartamigration.config;

import org.junit.jupiter.api.BeforeEach;
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
            .hasMessageContaining("AUTO_FIXES")
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
    void shouldReturnUpgradeMessage() {
        String message = service.getUpgradeMessage(FeatureFlag.AUTO_FIXES);
        
        assertThat(message)
            .contains("AUTO_FIXES")
            .contains("PREMIUM")
            .contains("upgrade");
    }
}

