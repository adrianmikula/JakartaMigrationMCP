package adrianmikula.jakartamigration.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests to verify that YAML configuration files load correctly.
 * Only feature-flags configuration (no Stripe/Apify).
 */
@SpringBootTest(classes = adrianmikula.jakartamigration.JakartaMigrationMcpApplication.class)
class YamlConfigurationTest {

    @Autowired(required = false)
    private FeatureFlagsProperties featureFlagsProperties;

    @Test
    void shouldLoadDefaultConfiguration() {
        assertThat(featureFlagsProperties).isNotNull();

        assertThat(featureFlagsProperties.getEnabled()).isTrue();
        assertThat(featureFlagsProperties.getDefaultTier())
            .isEqualTo(FeatureFlagsProperties.LicenseTier.PREMIUM);
        assertThat(featureFlagsProperties.getLicenseKey()).isNotNull();
        assertThat(featureFlagsProperties.getFeatures()).isNotNull();
    }

    @Nested
    @SpringBootTest(classes = adrianmikula.jakartamigration.JakartaMigrationMcpApplication.class)
    @ActiveProfiles("mcp-stdio")
    class StdioProfileTest {
        @Autowired(required = false)
        private FeatureFlagsProperties featureFlagsProperties;

        @Test
        void shouldLoadStdioProfileConfiguration() {
            assertThat(featureFlagsProperties).isNotNull();
            assertThat(featureFlagsProperties.getEnabled()).isTrue();
        }
    }

    @Nested
    @SpringBootTest(classes = adrianmikula.jakartamigration.JakartaMigrationMcpApplication.class)
    @ActiveProfiles("mcp-sse")
    class SseProfileTest {
        @Autowired(required = false)
        private FeatureFlagsProperties featureFlagsProperties;

        @Test
        void shouldLoadSseProfileConfiguration() {
            assertThat(featureFlagsProperties).isNotNull();
            assertThat(featureFlagsProperties.getEnabled()).isTrue();
        }
    }

    @Nested
    @SpringBootTest(classes = adrianmikula.jakartamigration.JakartaMigrationMcpApplication.class)
    @TestPropertySource(properties = {
        "jakarta.migration.feature-flags.enabled=false",
        "jakarta.migration.feature-flags.default-tier=PREMIUM",
        "jakarta.migration.feature-flags.license-key=test-license-key"
    })
    class PropertiesOverrideTest {
        @Autowired(required = false)
        private FeatureFlagsProperties featureFlagsProperties;

        @Test
        void shouldLoadConfigurationFromProperties() {
            assertThat(featureFlagsProperties).isNotNull();
            assertThat(featureFlagsProperties.getEnabled()).isFalse();
            assertThat(featureFlagsProperties.getDefaultTier())
                .isEqualTo(FeatureFlagsProperties.LicenseTier.PREMIUM);
            assertThat(featureFlagsProperties.getLicenseKey()).isEqualTo("test-license-key");
        }
    }

    @Nested
    @SpringBootTest(classes = adrianmikula.jakartamigration.JakartaMigrationMcpApplication.class)
    @TestPropertySource(properties = {
        "jakarta.migration.feature-flags.features.auto-fixes=true",
        "jakarta.migration.feature-flags.features.one-click-refactor=false"
    })
    class FeatureOverridesTest {
        @Autowired(required = false)
        private FeatureFlagsProperties featureFlagsProperties;

        @Test
        void shouldLoadFeatureOverrides() {
            assertThat(featureFlagsProperties).isNotNull();
            assertThat(featureFlagsProperties.getFeatures()).isNotNull();
            assertThat(featureFlagsProperties.getFeatures().get("auto-fixes")).isTrue();
            assertThat(featureFlagsProperties.getFeatures().get("one-click-refactor")).isFalse();
        }
    }
}
