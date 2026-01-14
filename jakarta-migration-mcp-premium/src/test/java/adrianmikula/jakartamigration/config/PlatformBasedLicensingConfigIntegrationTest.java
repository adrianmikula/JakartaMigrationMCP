package adrianmikula.jakartamigration.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for PlatformBasedLicensingConfig.
 * 
 * Verifies that Spring Boot context loads correctly with platform-based licensing configuration.
 * Uses a minimal test configuration to avoid loading all components.
 */
@SpringJUnitConfig(PlatformBasedLicensingConfigIntegrationTest.TestConfig.class)
@DisplayName("PlatformBasedLicensingConfig Integration Tests")
class PlatformBasedLicensingConfigIntegrationTest {
    
    @Configuration
    @EnableConfigurationProperties({ApifyLicenseProperties.class, StripeLicenseProperties.class})
    static class TestConfig {
        @Bean
        public PlatformDetectionService platformDetectionService() {
            return new PlatformDetectionService();
        }
        
        @Bean
        public PlatformBasedLicensingConfig platformBasedLicensingConfig(
                PlatformDetectionService platformDetectionService,
                ApifyLicenseProperties apifyProperties,
                StripeLicenseProperties stripeProperties) {
            return new PlatformBasedLicensingConfig(platformDetectionService, apifyProperties, stripeProperties);
        }
    }
    
    @Autowired
    private PlatformDetectionService platformDetectionService;
    
    @Autowired
    private ApifyLicenseProperties apifyProperties;
    
    @Autowired
    private StripeLicenseProperties stripeProperties;
    
    @Test
    @DisplayName("Should create all required beans")
    void shouldCreateAllRequiredBeans() {
        // Then
        assertThat(platformDetectionService).isNotNull();
        assertThat(apifyProperties).isNotNull();
        assertThat(stripeProperties).isNotNull();
    }
    
    @Test
    @DisplayName("Should detect current platform correctly")
    void shouldDetectCurrentPlatform() {
        // When
        boolean isApify = platformDetectionService.isApifyPlatform();
        String platformName = platformDetectionService.getPlatformName();
        
        // Then
        assertThat(platformName).isIn("apify", "local");
        if (isApify) {
            assertThat(platformName).isEqualTo("apify");
        } else {
            assertThat(platformName).isEqualTo("local");
        }
    }
    
    @Test
    @DisplayName("Properties should be initialized")
    void propertiesShouldBeInitialized() {
        // Then
        assertThat(apifyProperties.getEnabled()).isNotNull();
        assertThat(stripeProperties.getEnabled()).isNotNull();
    }
}

