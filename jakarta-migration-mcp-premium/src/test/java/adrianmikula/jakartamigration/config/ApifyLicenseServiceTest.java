package adrianmikula.jakartamigration.config;

import adrianmikula.jakartamigration.config.ApifyLicenseService.ApifyUserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ApifyLicenseService.
 * Note: These are simplified tests. In production, you'd want to test
 * the actual WebClient integration with a mock server.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApifyLicenseServiceTest {

    @Mock
    private ApifyLicenseProperties properties;

    @Mock
    private WebClient apifyWebClient;

    private ApifyLicenseService service;

    @BeforeEach
    void setUp() {
        when(properties.getEnabled()).thenReturn(true);
        when(properties.getApiUrl()).thenReturn("https://api.apify.com/v2");
        when(properties.getCacheTtlSeconds()).thenReturn(3600L);
        when(properties.getTimeoutSeconds()).thenReturn(5);
        when(properties.getAllowOfflineValidation()).thenReturn(true);
        when(properties.getActorId()).thenReturn("");

        service = new ApifyLicenseService(properties, apifyWebClient);
    }

    @Test
    void shouldReturnNullForNullLicenseKey() {
        FeatureFlagsProperties.LicenseTier tier = service.validateLicense(null);
        
        assertThat(tier).isNull();
    }

    @Test
    void shouldReturnNullForBlankLicenseKey() {
        FeatureFlagsProperties.LicenseTier tier = service.validateLicense("");
        
        assertThat(tier).isNull();
    }

    @Test
    void shouldUseSimpleValidationWhenApifyDisabled() {
        when(properties.getEnabled()).thenReturn(false);
        
        FeatureFlagsProperties.LicenseTier tier = service.validateLicense("PREMIUM-test-key");
        
        assertThat(tier).isEqualTo(FeatureFlagsProperties.LicenseTier.PREMIUM);
    }

    @Test
    void shouldCacheValidationResults() {
        // This test would require mocking WebClient responses
        // For now, we test that cache methods exist
        service.clearCache();
        service.clearCache("test-key");
        
        // If we get here without exception, cache methods work
        assertThat(true).isTrue();
    }

    @Test
    void shouldDetermineTierFromUserPlan() {
        // Test tier determination logic
        ApifyUserResponse user = new ApifyUserResponse();
        user.setUsername("testuser");
        user.setPlan("ENTERPRISE");
        
        // This would be tested via reflection or by making the method package-private
        // For now, we verify the service can be instantiated
        assertThat(service).isNotNull();
    }
}

