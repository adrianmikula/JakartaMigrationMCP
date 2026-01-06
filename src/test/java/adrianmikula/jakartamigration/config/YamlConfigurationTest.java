package adrianmikula.jakartamigration.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests to verify that YAML configuration files load correctly.
 * 
 * This test suite ensures:
 * - All @ConfigurationProperties classes are correctly bound
 * - Default values are loaded correctly
 * - Profile-specific configurations work
 * - Environment variable substitution works
 */
@SpringBootTest
class YamlConfigurationTest {

    @Autowired(required = false)
    private FeatureFlagsProperties featureFlagsProperties;

    @Autowired(required = false)
    private ApifyLicenseProperties apifyLicenseProperties;

    @Autowired(required = false)
    private StripeLicenseProperties stripeLicenseProperties;

    /**
     * Test that default application.yml loads correctly.
     */
    @Test
    void shouldLoadDefaultConfiguration() {
        assertThat(featureFlagsProperties).isNotNull();
        assertThat(apifyLicenseProperties).isNotNull();
        assertThat(stripeLicenseProperties).isNotNull();

        // Verify FeatureFlagsProperties defaults
        assertThat(featureFlagsProperties.getEnabled()).isTrue();
        assertThat(featureFlagsProperties.getDefaultTier())
            .isEqualTo(FeatureFlagsProperties.LicenseTier.COMMUNITY);
        assertThat(featureFlagsProperties.getLicenseKey()).isNotNull();
        assertThat(featureFlagsProperties.getFeatures()).isNotNull();

        // Verify ApifyLicenseProperties defaults
        assertThat(apifyLicenseProperties.getEnabled()).isTrue();
        assertThat(apifyLicenseProperties.getApiUrl())
            .isEqualTo("https://api.apify.com/v2");
        assertThat(apifyLicenseProperties.getCacheTtlSeconds()).isEqualTo(3600L);
        assertThat(apifyLicenseProperties.getTimeoutSeconds()).isEqualTo(5);
        assertThat(apifyLicenseProperties.getAllowOfflineValidation()).isTrue();

        // Verify StripeLicenseProperties defaults
        assertThat(stripeLicenseProperties.getEnabled()).isTrue();
        assertThat(stripeLicenseProperties.getApiUrl())
            .isEqualTo("https://api.stripe.com/v1");
        assertThat(stripeLicenseProperties.getCacheTtlSeconds()).isEqualTo(3600L);
        assertThat(stripeLicenseProperties.getTimeoutSeconds()).isEqualTo(5);
        assertThat(stripeLicenseProperties.getAllowOfflineValidation()).isTrue();
        assertThat(stripeLicenseProperties.getLicenseKeyPrefix()).isEqualTo("stripe_");
    }

    /**
     * Test that mcp-stdio profile loads correctly.
     */
    @Test
    @ActiveProfiles("mcp-stdio")
    void shouldLoadStdioProfileConfiguration() {
        assertThat(featureFlagsProperties).isNotNull();
        assertThat(apifyLicenseProperties).isNotNull();
        assertThat(stripeLicenseProperties).isNotNull();

        // Verify that stdio profile doesn't break configuration loading
        assertThat(featureFlagsProperties.getEnabled()).isTrue();
        assertThat(apifyLicenseProperties.getApiUrl())
            .isEqualTo("https://api.apify.com/v2");
        assertThat(stripeLicenseProperties.getApiUrl())
            .isEqualTo("https://api.stripe.com/v1");
    }

    /**
     * Test that mcp-sse profile loads correctly.
     */
    @Test
    @ActiveProfiles("mcp-sse")
    void shouldLoadSseProfileConfiguration() {
        assertThat(featureFlagsProperties).isNotNull();
        assertThat(apifyLicenseProperties).isNotNull();
        assertThat(stripeLicenseProperties).isNotNull();

        // Verify that SSE profile doesn't break configuration loading
        assertThat(featureFlagsProperties.getEnabled()).isTrue();
        assertThat(apifyLicenseProperties.getApiUrl())
            .isEqualTo("https://api.apify.com/v2");
        assertThat(stripeLicenseProperties.getApiUrl())
            .isEqualTo("https://api.stripe.com/v1");
    }

    /**
     * Test that environment variable substitution works.
     */
    @Test
    @TestPropertySource(properties = {
        "jakarta.migration.feature-flags.enabled=false",
        "jakarta.migration.feature-flags.default-tier=PREMIUM",
        "jakarta.migration.feature-flags.license-key=test-license-key",
        "jakarta.migration.apify.enabled=false",
        "jakarta.migration.apify.api-url=https://test.apify.com/v2",
        "jakarta.migration.apify.api-token=test-apify-token",
        "jakarta.migration.apify.cache-ttl-seconds=7200",
        "jakarta.migration.apify.timeout-seconds=10",
        "jakarta.migration.stripe.enabled=false",
        "jakarta.migration.stripe.api-url=https://test.stripe.com/v1",
        "jakarta.migration.stripe.secret-key=test-stripe-key",
        "jakarta.migration.stripe.cache-ttl-seconds=7200",
        "jakarta.migration.stripe.timeout-seconds=10",
        "jakarta.migration.stripe.license-key-prefix=test_"
    })
    void shouldLoadConfigurationFromProperties() {
        assertThat(featureFlagsProperties).isNotNull();
        assertThat(apifyLicenseProperties).isNotNull();
        assertThat(stripeLicenseProperties).isNotNull();

        // Verify FeatureFlagsProperties overrides
        assertThat(featureFlagsProperties.getEnabled()).isFalse();
        assertThat(featureFlagsProperties.getDefaultTier())
            .isEqualTo(FeatureFlagsProperties.LicenseTier.PREMIUM);
        assertThat(featureFlagsProperties.getLicenseKey()).isEqualTo("test-license-key");

        // Verify ApifyLicenseProperties overrides
        assertThat(apifyLicenseProperties.getEnabled()).isFalse();
        assertThat(apifyLicenseProperties.getApiUrl())
            .isEqualTo("https://test.apify.com/v2");
        assertThat(apifyLicenseProperties.getApiToken()).isEqualTo("test-apify-token");
        assertThat(apifyLicenseProperties.getCacheTtlSeconds()).isEqualTo(7200L);
        assertThat(apifyLicenseProperties.getTimeoutSeconds()).isEqualTo(10);

        // Verify StripeLicenseProperties overrides
        assertThat(stripeLicenseProperties.getEnabled()).isFalse();
        assertThat(stripeLicenseProperties.getApiUrl())
            .isEqualTo("https://test.stripe.com/v1");
        assertThat(stripeLicenseProperties.getSecretKey()).isEqualTo("test-stripe-key");
        assertThat(stripeLicenseProperties.getCacheTtlSeconds()).isEqualTo(7200L);
        assertThat(stripeLicenseProperties.getTimeoutSeconds()).isEqualTo(10);
        assertThat(stripeLicenseProperties.getLicenseKeyPrefix()).isEqualTo("test_");
    }

    /**
     * Test that feature overrides work correctly.
     */
    @Test
    @TestPropertySource(properties = {
        "jakarta.migration.feature-flags.features.auto-fixes=true",
        "jakarta.migration.feature-flags.features.one-click-refactor=false"
    })
    void shouldLoadFeatureOverrides() {
        assertThat(featureFlagsProperties).isNotNull();
        assertThat(featureFlagsProperties.getFeatures()).isNotNull();
        assertThat(featureFlagsProperties.getFeatures().get("auto-fixes")).isTrue();
        assertThat(featureFlagsProperties.getFeatures().get("one-click-refactor")).isFalse();
    }

    /**
     * Test that Stripe price ID mappings work correctly.
     */
    @Test
    @TestPropertySource(properties = {
        "jakarta.migration.stripe.product-id-premium=prod_premium",
        "jakarta.migration.stripe.product-id-enterprise=prod_enterprise",
        "jakarta.migration.stripe.price-id-to-tier.price_123=PREMIUM",
        "jakarta.migration.stripe.price-id-to-tier.price_456=ENTERPRISE"
    })
    void shouldLoadStripePriceIdMappings() {
        assertThat(stripeLicenseProperties).isNotNull();
        assertThat(stripeLicenseProperties.getProductIdPremium()).isEqualTo("prod_premium");
        assertThat(stripeLicenseProperties.getProductIdEnterprise()).isEqualTo("prod_enterprise");
        assertThat(stripeLicenseProperties.getPriceIdToTier()).isNotNull();
        assertThat(stripeLicenseProperties.getPriceIdToTier().get("price_123")).isEqualTo("PREMIUM");
        assertThat(stripeLicenseProperties.getPriceIdToTier().get("price_456")).isEqualTo("ENTERPRISE");
    }

    /**
     * Test that Apify actor ID is loaded correctly.
     */
    @Test
    @TestPropertySource(properties = {
        "jakarta.migration.apify.actor-id=test-actor-id"
    })
    void shouldLoadApifyActorId() {
        assertThat(apifyLicenseProperties).isNotNull();
        assertThat(apifyLicenseProperties.getActorId()).isEqualTo("test-actor-id");
    }

    /**
     * Test that Stripe webhook secret is loaded correctly.
     */
    @Test
    @TestPropertySource(properties = {
        "jakarta.migration.stripe.webhook-secret=test-webhook-secret"
    })
    void shouldLoadStripeWebhookSecret() {
        assertThat(stripeLicenseProperties).isNotNull();
        assertThat(stripeLicenseProperties.getWebhookSecret()).isEqualTo("test-webhook-secret");
    }
}

