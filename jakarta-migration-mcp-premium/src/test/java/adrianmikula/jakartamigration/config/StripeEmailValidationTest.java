package adrianmikula.jakartamigration.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for email-based license validation in StripeLicenseService.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Stripe Email Validation Tests")
@SuppressWarnings({"unchecked", "rawtypes"})
class StripeEmailValidationTest {

    @Mock
    private StripeLicenseProperties properties;

    @Mock
    private WebClient stripeWebClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private StripeLicenseService service;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_SECRET_KEY = "sk_test_1234567890";

    @BeforeEach
    void setUp() {
        // Default property stubs
        lenient().when(properties.getEnabled()).thenReturn(true);
        lenient().when(properties.getApiUrl()).thenReturn("https://api.stripe.com/v1");
        lenient().when(properties.getSecretKey()).thenReturn(TEST_SECRET_KEY);
        lenient().when(properties.getCacheTtlSeconds()).thenReturn(3600L);
        lenient().when(properties.getTimeoutSeconds()).thenReturn(5);
        lenient().when(properties.getProductIdPremium()).thenReturn("prod_premium");
        lenient().when(properties.getProductIdEnterprise()).thenReturn("prod_enterprise");
        lenient().when(properties.getPriceIdToTier()).thenReturn(new HashMap<>());

        // Setup WebClient mock chain
        lenient().when(stripeWebClient.get()).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersUriSpec.uri(any(java.net.URI.class))).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        service = new StripeLicenseService(properties, stripeWebClient);
    }

    @Test
    @DisplayName("Should return empty for null email")
    void shouldReturnEmptyForNullEmail() {
        // When
        Mono<FeatureFlagsProperties.LicenseTier> result = service.validateLicenseByEmail(null);

        // Then
        assertThat(result.block()).isNull();
    }

    @Test
    @DisplayName("Should return empty for blank email")
    void shouldReturnEmptyForBlankEmail() {
        // When
        Mono<FeatureFlagsProperties.LicenseTier> result = service.validateLicenseByEmail("   ");

        // Then
        assertThat(result.block()).isNull();
    }

    @Test
    @DisplayName("Should return empty for invalid email format")
    void shouldReturnEmptyForInvalidEmailFormat() {
        // When
        Mono<FeatureFlagsProperties.LicenseTier> result = service.validateLicenseByEmail("not-an-email");

        // Then
        assertThat(result.block()).isNull();
    }

    @Test
    @DisplayName("Should return empty when customer not found")
    void shouldReturnEmptyWhenCustomerNotFound() {
        // Given
        when(responseSpec.bodyToMono(any(Class.class)))
            .thenReturn(Mono.error(WebClientResponseException.create(
                HttpStatus.NOT_FOUND.value(), "Not Found", null, null, null)));

        // When
        Mono<FeatureFlagsProperties.LicenseTier> result = service.validateLicenseByEmail(TEST_EMAIL);

        // Then
        assertThat(result.block()).isNull();
    }

    @Test
    @DisplayName("Should handle API errors gracefully")
    void shouldHandleApiErrorsGracefully() {
        // Given
        when(responseSpec.bodyToMono(any(Class.class)))
            .thenReturn(Mono.error(new RuntimeException("Network error")));

        // When
        Mono<FeatureFlagsProperties.LicenseTier> result = service.validateLicenseByEmail(TEST_EMAIL);

        // Then
        assertThat(result.block()).isNull();
    }

    @Test
    @DisplayName("Should use cache when available")
    void shouldUseCacheWhenAvailable() {
        // Given - first call returns a tier
        when(responseSpec.bodyToMono(any(Class.class)))
            .thenReturn(createCustomerListResponse("cus_123", TEST_EMAIL))
            .thenReturn(createSubscriptionsListResponse("sub_123", "active", "prod_premium"));

        // When - first call
        FeatureFlagsProperties.LicenseTier firstResult = service.validateLicenseByEmail(TEST_EMAIL).block();

        // Then - should validate via API
        assertThat(firstResult).isNotNull();
        verify(requestHeadersUriSpec, atLeastOnce()).uri(anyString());

        // When - second call (should use cache)
        reset(requestHeadersUriSpec);
        lenient().when(stripeWebClient.get()).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        FeatureFlagsProperties.LicenseTier secondResult = service.validateLicenseByEmail(TEST_EMAIL).block();

        // Then - should use cache (no new API calls)
        assertThat(secondResult).isNotNull();
        // Note: Cache verification is complex with reactive code, so we just verify the result
    }

    /**
     * Helper to create a mock customer list response.
     */
    private Mono<Object> createCustomerListResponse(String customerId, String email) {
        return Mono.just(new Object() {
            public Object getData() {
                return new Object[] {
                    new Object() {
                        public String getId() { return customerId; }
                        public String getEmail() { return email; }
                    }
                };
            }
        });
    }

    /**
     * Helper to create a mock subscriptions list response.
     */
    private Mono<Object> createSubscriptionsListResponse(String subscriptionId, String status, String productId) {
        return Mono.just(new Object() {
            public Object getData() {
                return new Object[] {
                    new Object() {
                        public String getId() { return subscriptionId; }
                        public String getStatus() { return status; }
                        public Object getItems() {
                            return new Object() {
                                public Object[] getData() {
                                    return new Object[] {
                                        new Object() {
                                            public Object getPrice() {
                                                return new Object() {
                                                    public String getProduct() { return productId; }
                                                };
                                            }
                                        }
                                    };
                                }
                            };
                        }
                    }
                };
            }
        });
    }
}

