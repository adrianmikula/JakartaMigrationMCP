package com.bugbounty.jakartamigration.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for LicenseService.
 */
class LicenseServiceTest {

    private LicenseService licenseService;

    @BeforeEach
    void setUp() {
        licenseService = new LicenseService();
    }

    @Test
    void shouldValidatePremiumLicenseKey() {
        FeatureFlagsProperties.LicenseTier tier = licenseService.validateLicense("PREMIUM-test-key-123");
        
        assertThat(tier).isEqualTo(FeatureFlagsProperties.LicenseTier.PREMIUM);
    }

    @Test
    void shouldValidateEnterpriseLicenseKey() {
        FeatureFlagsProperties.LicenseTier tier = licenseService.validateLicense("ENTERPRISE-test-key-456");
        
        assertThat(tier).isEqualTo(FeatureFlagsProperties.LicenseTier.ENTERPRISE);
    }

    @Test
    void shouldRejectInvalidLicenseKey() {
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

