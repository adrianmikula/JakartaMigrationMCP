package adrianmikula.jakartamigration.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for LicenseService.
 * LicenseService now only returns the configured default tier (no external validation).
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

    @Test
    void shouldReturnDefaultTierFromProperties() {
        when(properties.getDefaultTier()).thenReturn(FeatureFlagsProperties.LicenseTier.ENTERPRISE);

        assertThat(licenseService.getDefaultTier()).isEqualTo(FeatureFlagsProperties.LicenseTier.ENTERPRISE);
    }

    @Test
    void shouldReturnCommunityWhenConfigured() {
        when(properties.getDefaultTier()).thenReturn(FeatureFlagsProperties.LicenseTier.COMMUNITY);

        assertThat(licenseService.getDefaultTier()).isEqualTo(FeatureFlagsProperties.LicenseTier.COMMUNITY);
    }

    @Test
    void shouldReturnPremiumWhenConfigured() {
        when(properties.getDefaultTier()).thenReturn(FeatureFlagsProperties.LicenseTier.PREMIUM);

        assertThat(licenseService.getDefaultTier()).isEqualTo(FeatureFlagsProperties.LicenseTier.PREMIUM);
    }
}
