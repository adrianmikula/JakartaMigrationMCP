package adrianmikula.jakartamigration.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for LicenseService.
 */
@ExtendWith(MockitoExtension.class)
class LicenseServiceTest {

    @Mock
    private ApifyLicenseService apifyLicenseService;

    @Mock
    private StripeLicenseService stripeLicenseService;

    private LicenseService licenseService;

    @BeforeEach
    void setUp() {
        // ApifyLicenseService and LocalLicenseStorageService are optional (nullable)
        // Pass null to simulate disabled state by default
        licenseService = new LicenseService(stripeLicenseService, null, null);
    }

    @Test
    void shouldValidatePremiumLicenseKeyViaStripe() {
        // Stripe is tried first for all keys (returns null for non-Stripe keys)
        when(stripeLicenseService.validateLicense(anyString())).thenReturn(null);
        
        // Falls back to simple validation for test keys (PREMIUM- prefix)
        FeatureFlagsProperties.LicenseTier tier = licenseService.validateLicense("PREMIUM-test-key-123");
        
        assertThat(tier).isEqualTo(FeatureFlagsProperties.LicenseTier.PREMIUM);
    }

    @Test
    void shouldValidateEnterpriseLicenseKeyViaStripe() {
        // Stripe is tried first for all keys (returns null for non-Stripe keys)
        when(stripeLicenseService.validateLicense(anyString())).thenReturn(null);
        
        // Falls back to simple validation for test keys (ENTERPRISE- prefix)
        FeatureFlagsProperties.LicenseTier tier = licenseService.validateLicense("ENTERPRISE-test-key-456");
        
        assertThat(tier).isEqualTo(FeatureFlagsProperties.LicenseTier.ENTERPRISE);
    }

    @Test
    void shouldUseStripeValidationForStripeKeys() {
        when(stripeLicenseService.validateLicense(anyString())).thenReturn(FeatureFlagsProperties.LicenseTier.PREMIUM);
        
        FeatureFlagsProperties.LicenseTier tier = licenseService.validateLicense("sub_1234567890");
        
        assertThat(tier).isEqualTo(FeatureFlagsProperties.LicenseTier.PREMIUM);
    }

    @Test
    void shouldUseApifyValidationWhenAvailable() {
        // Create service with Apify enabled for this test
        licenseService = new LicenseService(stripeLicenseService, apifyLicenseService, null);
        
        // Stripe is tried first (returns null for non-Stripe keys)
        when(stripeLicenseService.validateLicense(anyString())).thenReturn(null);
        // Then Apify is tried
        when(apifyLicenseService.validateLicense(anyString())).thenReturn(FeatureFlagsProperties.LicenseTier.PREMIUM);
        
        FeatureFlagsProperties.LicenseTier tier = licenseService.validateLicense("apify_api_token_123");
        
        assertThat(tier).isEqualTo(FeatureFlagsProperties.LicenseTier.PREMIUM);
    }

    @Test
    void shouldTryStripeFirstForStripeKeys() {
        when(stripeLicenseService.validateLicense(anyString())).thenReturn(FeatureFlagsProperties.LicenseTier.ENTERPRISE);
        
        FeatureFlagsProperties.LicenseTier tier = licenseService.validateLicense("stripe_sub_1234567890");
        
        assertThat(tier).isEqualTo(FeatureFlagsProperties.LicenseTier.ENTERPRISE);
        // Apify should not be called for Stripe keys
    }

    @Test
    void shouldRejectInvalidLicenseKey() {
        // Stripe is tried first (returns null for invalid keys)
        when(stripeLicenseService.validateLicense(anyString())).thenReturn(null);
        
        FeatureFlagsProperties.LicenseTier tier = licenseService.validateLicense("INVALID-key");
        
        assertThat(tier).isNull();
    }

    @Test
    void shouldRejectNullLicenseKey() {
        FeatureFlagsProperties.LicenseTier tier = licenseService.validateLicense(null);
        
        assertThat(tier).isNull();
    }

    @Test
    void shouldRejectBlankLicenseKey() {
        FeatureFlagsProperties.LicenseTier tier = licenseService.validateLicense("");
        
        assertThat(tier).isNull();
    }

    @Test
    void shouldCheckLicenseValidity() {
        assertThat(licenseService.isValidLicense("PREMIUM-test")).isTrue();
        assertThat(licenseService.isValidLicense("ENTERPRISE-test")).isTrue();
        assertThat(licenseService.isValidLicense("INVALID-test")).isFalse();
        assertThat(licenseService.isValidLicense(null)).isFalse();
        assertThat(licenseService.isValidLicense("")).isFalse();
    }
}

