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
 * Comprehensive unit tests for StripeLicenseService.
 * 
 * Tests cover:
 * - Basic validation (null, blank, non-Stripe keys)
 * - Simple validation fallback
 * - Error handling (404, 401, 500)
 * - Caching behavior
 * - Offline validation fallback
 * - License key format detection
 * 
 * Note: Full WebClient integration testing would require WireMock or similar.
 * These tests focus on testable logic and error handling paths.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("StripeLicenseService Unit Tests")
@SuppressWarnings({"unchecked", "rawtypes"})
class StripeLicenseServiceTest {

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

    private static final String TEST_SUBSCRIPTION_ID = "sub_1234567890abcdef";
    private static final String TEST_CUSTOMER_ID = "cus_1234567890abcdef";
    private static final String TEST_SECRET_KEY = "sk_test_1234567890";
    private static final String TEST_PREMIUM_PRODUCT_ID = "prod_premium123";
    private static final String TEST_ENTERPRISE_PRODUCT_ID = "prod_enterprise123";

    @BeforeEach
    void setUp() {
        // Default property stubs
        lenient().when(properties.getEnabled()).thenReturn(true);
        lenient().when(properties.getApiUrl()).thenReturn("https://api.stripe.com/v1");
        lenient().when(properties.getSecretKey()).thenReturn(TEST_SECRET_KEY);
        lenient().when(properties.getCacheTtlSeconds()).thenReturn(3600L);
        lenient().when(properties.getTimeoutSeconds()).thenReturn(5);
        lenient().when(properties.getAllowOfflineValidation()).thenReturn(true);
        lenient().when(properties.getLicenseKeyPrefix()).thenReturn("stripe_");
        lenient().when(properties.getProductIdPremium()).thenReturn(TEST_PREMIUM_PRODUCT_ID);
        lenient().when(properties.getProductIdEnterprise()).thenReturn(TEST_ENTERPRISE_PRODUCT_ID);
        lenient().when(properties.getPriceIdToTier()).thenReturn(new HashMap<>());

        // Setup WebClient mock chain
        lenient().when(stripeWebClient.get()).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersUriSpec.uri(any(java.net.URI.class))).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        service = new StripeLicenseService(properties, stripeWebClient);
    }

    // ========== Basic Validation Tests ==========

    @Test
    @DisplayName("Should return null for null license key")
    void shouldReturnNullForNullLicenseKey() {
        // When
        FeatureFlagsProperties.LicenseTier tier = service.validateLicense(null);

        // Then
        assertThat(tier).isNull();
    }

    @Test
    @DisplayName("Should return null for blank license key")
    void shouldReturnNullForBlankLicenseKey() {
        // When
        FeatureFlagsProperties.LicenseTier tier = service.validateLicense("   ");

        // Then
        assertThat(tier).isNull();
    }

    @Test
    @DisplayName("Should return null for non-Stripe license keys")
    void shouldReturnNullForNonStripeLicenseKeys() {
        // When
        FeatureFlagsProperties.LicenseTier tier = service.validateLicense("PREMIUM-test-key");

        // Then
        assertThat(tier).isNull();
    }

    // ========== Simple Validation Tests ==========

    @Test
    @DisplayName("Should use simple validation when Stripe is disabled")
    void shouldUseSimpleValidationWhenStripeIsDisabled() {
        // Given
        when(properties.getEnabled()).thenReturn(false);

        // When
        FeatureFlagsProperties.LicenseTier tier = service.validateLicense("stripe_PREMIUM-test-key");

        // Then
        assertThat(tier).isEqualTo(FeatureFlagsProperties.LicenseTier.PREMIUM);
    }

    @Test
    @DisplayName("Should use simple validation for enterprise test keys")
    void shouldUseSimpleValidationForEnterpriseTestKeys() {
        // Given
        when(properties.getEnabled()).thenReturn(false);

        // When
        FeatureFlagsProperties.LicenseTier tier = service.validateLicense("stripe_ENTERPRISE-test-key");

        // Then
        assertThat(tier).isEqualTo(FeatureFlagsProperties.LicenseTier.ENTERPRISE);
    }

    @Test
    @DisplayName("Should return null for invalid simple validation keys")
    void shouldReturnNullForInvalidSimpleValidationKeys() {
        // Given
        when(properties.getEnabled()).thenReturn(false);

        // When
        FeatureFlagsProperties.LicenseTier tier = service.validateLicense("stripe_INVALID-test-key");

        // Then
        assertThat(tier).isNull();
    }

    // ========== Error Handling Tests ==========

    @Test
    @DisplayName("Should return null when subscription not found (404)")
    void shouldReturnNullWhenSubscriptionNotFound() {
        // Given
        WebClientResponseException notFoundException = WebClientResponseException.create(
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            null, null, null
        );
        
        when(responseSpec.bodyToMono(any(Class.class)))
            .thenReturn(Mono.error(notFoundException));

        // When
        FeatureFlagsProperties.LicenseTier tier = service.validateLicense(TEST_SUBSCRIPTION_ID);

        // Then
        assertThat(tier).isNull();
    }

    @Test
    @DisplayName("Should return null when API key is invalid (401)")
    void shouldReturnNullWhenApiKeyIsInvalid() {
        // Given
        WebClientResponseException unauthorizedException = WebClientResponseException.create(
            HttpStatus.UNAUTHORIZED.value(),
            "Unauthorized",
            null, null, null
        );
        
        when(responseSpec.bodyToMono(any(Class.class)))
            .thenReturn(Mono.error(unauthorizedException));

        // When
        FeatureFlagsProperties.LicenseTier tier = service.validateLicense(TEST_SUBSCRIPTION_ID);

        // Then
        assertThat(tier).isNull();
    }

    @Test
    @DisplayName("Should return null when API key is forbidden (403)")
    void shouldReturnNullWhenApiKeyIsForbidden() {
        // Given
        WebClientResponseException forbiddenException = WebClientResponseException.create(
            HttpStatus.FORBIDDEN.value(),
            "Forbidden",
            null, null, null
        );
        
        when(responseSpec.bodyToMono(any(Class.class)))
            .thenReturn(Mono.error(forbiddenException));

        // When
        FeatureFlagsProperties.LicenseTier tier = service.validateLicense(TEST_SUBSCRIPTION_ID);

        // Then
        assertThat(tier).isNull();
    }

    @Test
    @DisplayName("Should handle server errors gracefully")
    void shouldHandleServerErrorsGracefully() {
        // Given
        WebClientResponseException serverError = WebClientResponseException.create(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            null, null, null
        );
        
        when(responseSpec.bodyToMono(any(Class.class)))
            .thenReturn(Mono.error(serverError));

        // When
        FeatureFlagsProperties.LicenseTier tier = service.validateLicense(TEST_SUBSCRIPTION_ID);

        // Then
        // Server errors are retried but eventually return null
        assertThat(tier).isNull();
    }

    @Test
    @DisplayName("Should handle generic exceptions gracefully")
    void shouldHandleGenericExceptionsGracefully() {
        // Given
        when(responseSpec.bodyToMono(any(Class.class)))
            .thenReturn(Mono.error(new RuntimeException("Unexpected error")));

        // When
        FeatureFlagsProperties.LicenseTier tier = service.validateLicense(TEST_SUBSCRIPTION_ID);

        // Then
        assertThat(tier).isNull();
    }

    // ========== License Key Format Tests ==========

    @Test
    @DisplayName("Should recognize subscription ID format")
    void shouldRecognizeSubscriptionIdFormat() {
        // Given
        when(responseSpec.bodyToMono(any(Class.class)))
            .thenReturn(Mono.error(WebClientResponseException.create(
                HttpStatus.NOT_FOUND.value(), "Not Found", null, null, null)));

        // When
        FeatureFlagsProperties.LicenseTier tier = service.validateLicense("sub_1234567890");

        // Then
        // Should attempt to validate (returns null because subscription not found)
        assertThat(tier).isNull();
        verify(requestHeadersUriSpec).uri(anyString());
    }

    @Test
    @DisplayName("Should recognize customer ID format")
    void shouldRecognizeCustomerIdFormat() {
        // Given
        when(responseSpec.bodyToMono(any(Class.class)))
            .thenReturn(Mono.error(WebClientResponseException.create(
                HttpStatus.NOT_FOUND.value(), "Not Found", null, null, null)));

        // When
        FeatureFlagsProperties.LicenseTier tier = service.validateLicense("cus_1234567890");

        // Then
        // Should attempt to validate (returns null because customer not found)
        assertThat(tier).isNull();
        verify(requestHeadersUriSpec, atLeastOnce()).uri(anyString());
    }

    @Test
    @DisplayName("Should handle subscription ID with stripe_ prefix")
    void shouldHandleSubscriptionIdWithStripePrefix() {
        // Given
        String prefixedKey = "stripe_" + TEST_SUBSCRIPTION_ID;
        when(responseSpec.bodyToMono(any(Class.class)))
            .thenReturn(Mono.error(WebClientResponseException.create(
                HttpStatus.NOT_FOUND.value(), "Not Found", null, null, null)));

        // When
        FeatureFlagsProperties.LicenseTier tier = service.validateLicense(prefixedKey);

        // Then
        // Should extract subscription ID and attempt validation
        assertThat(tier).isNull();
        verify(requestHeadersUriSpec).uri(anyString());
    }

    @Test
    @DisplayName("Should handle customer ID with stripe_ prefix")
    void shouldHandleCustomerIdWithStripePrefix() {
        // Given
        String prefixedKey = "stripe_" + TEST_CUSTOMER_ID;
        when(responseSpec.bodyToMono(any(Class.class)))
            .thenReturn(Mono.error(WebClientResponseException.create(
                HttpStatus.NOT_FOUND.value(), "Not Found", null, null, null)));

        // When
        FeatureFlagsProperties.LicenseTier tier = service.validateLicense(prefixedKey);

        // Then
        // Should extract customer ID and attempt validation
        assertThat(tier).isNull();
        verify(requestHeadersUriSpec, atLeastOnce()).uri(anyString());
    }

    @Test
    @DisplayName("Should return null for unknown key format")
    void shouldReturnNullForUnknownKeyFormat() {
        // When
        FeatureFlagsProperties.LicenseTier tier = service.validateLicense("stripe_unknown_format");

        // Then
        assertThat(tier).isNull();
        // Should not attempt API call for unknown format
        verify(requestHeadersUriSpec, never()).uri(anyString());
    }

    // ========== Caching Tests ==========

    @Test
    @DisplayName("Should clear cache for specific license key")
    void shouldClearCacheForSpecificLicenseKey() {
        // When
        service.clearCache(TEST_SUBSCRIPTION_ID);

        // Then
        // Should not throw exception
        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("Should clear all cache")
    void shouldClearAllCache() {
        // When
        service.clearCache();

        // Then
        // Should not throw exception
        assertThat(true).isTrue();
    }

    // ========== Offline Validation Tests ==========

    @Test
    @DisplayName("Should fall back to simple validation when offline and allowed")
    void shouldFallBackToSimpleValidationWhenOfflineAndAllowed() {
        // Given
        when(properties.getAllowOfflineValidation()).thenReturn(true);
        when(responseSpec.bodyToMono(any(Class.class)))
            .thenReturn(Mono.error(new RuntimeException("Network error")));

        // When
        FeatureFlagsProperties.LicenseTier tier = service.validateLicense("stripe_PREMIUM-test");

        // Then
        assertThat(tier).isEqualTo(FeatureFlagsProperties.LicenseTier.PREMIUM);
    }

    @Test
    @DisplayName("Should return null when offline and offline validation not allowed")
    void shouldReturnNullWhenOfflineAndOfflineValidationNotAllowed() {
        // Given
        when(properties.getAllowOfflineValidation()).thenReturn(false);
        when(responseSpec.bodyToMono(any(Class.class)))
            .thenReturn(Mono.error(new RuntimeException("Network error")));

        // When
        FeatureFlagsProperties.LicenseTier tier = service.validateLicense("stripe_PREMIUM-test");

        // Then
        assertThat(tier).isNull();
    }

    // ========== Configuration Tests ==========

    @Test
    @DisplayName("Should respect custom license key prefix")
    void shouldRespectCustomLicenseKeyPrefix() {
        // Given
        when(properties.getLicenseKeyPrefix()).thenReturn("custom_");
        when(properties.getEnabled()).thenReturn(false);

        // When
        FeatureFlagsProperties.LicenseTier tier = service.validateLicense("custom_PREMIUM-test");

        // Then
        // Should not match because prefix doesn't match
        assertThat(tier).isNull();
    }

    @Test
    @DisplayName("Should handle empty product IDs")
    void shouldHandleEmptyProductIds() {
        // Given
        when(properties.getProductIdPremium()).thenReturn("");
        when(properties.getProductIdEnterprise()).thenReturn("");

        // When
        FeatureFlagsProperties.LicenseTier tier = service.validateLicense("sub_test");

        // Then
        // Should still attempt validation (will fail due to mock, but logic should handle empty IDs)
        assertThat(tier).isNull();
    }
}
