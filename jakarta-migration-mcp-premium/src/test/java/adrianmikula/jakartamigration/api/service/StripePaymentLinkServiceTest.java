package adrianmikula.jakartamigration.api.service;

import adrianmikula.jakartamigration.config.StripeLicenseProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for StripePaymentLinkService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StripePaymentLinkService Unit Tests")
class StripePaymentLinkServiceTest {

    @Mock
    private StripeLicenseProperties stripeProperties;

    private StripePaymentLinkService service;

    @BeforeEach
    void setUp() {
        service = new StripePaymentLinkService(stripeProperties);
    }

    @Test
    @DisplayName("Should return payment link for valid product name")
    void shouldReturnPaymentLinkForValidProduct() {
        // Given
        Map<String, String> paymentLinks = new HashMap<>();
        paymentLinks.put("starter", "https://buy.stripe.com/starter-link");
        when(stripeProperties.getPaymentLinks()).thenReturn(paymentLinks);

        // When
        String link = service.getPaymentLink("starter");

        // Then
        assertThat(link).isEqualTo("https://buy.stripe.com/starter-link");
    }

    @Test
    @DisplayName("Should return null for unknown product name")
    void shouldReturnNullForUnknownProduct() {
        // Given
        Map<String, String> paymentLinks = new HashMap<>();
        paymentLinks.put("starter", "https://buy.stripe.com/starter-link");
        when(stripeProperties.getPaymentLinks()).thenReturn(paymentLinks);

        // When
        String link = service.getPaymentLink("unknown");

        // Then
        assertThat(link).isNull();
    }

    @Test
    @DisplayName("Should handle case-insensitive product names")
    void shouldHandleCaseInsensitiveProductNames() {
        // Given
        Map<String, String> paymentLinks = new HashMap<>();
        paymentLinks.put("starter", "https://buy.stripe.com/starter-link");
        when(stripeProperties.getPaymentLinks()).thenReturn(paymentLinks);

        // When
        String link = service.getPaymentLink("STARTER");

        // Then
        assertThat(link).isEqualTo("https://buy.stripe.com/starter-link");
    }

    @Test
    @DisplayName("Should return null when payment links map is null")
    void shouldReturnNullWhenPaymentLinksMapIsNull() {
        // Given
        when(stripeProperties.getPaymentLinks()).thenReturn(null);

        // When
        String link = service.getPaymentLink("starter");

        // Then
        assertThat(link).isNull();
    }

    @Test
    @DisplayName("Should return null when payment links map is empty")
    void shouldReturnNullWhenPaymentLinksMapIsEmpty() {
        // Given
        when(stripeProperties.getPaymentLinks()).thenReturn(new HashMap<>());

        // When
        String link = service.getPaymentLink("starter");

        // Then
        assertThat(link).isNull();
    }

    @Test
    @DisplayName("Should return all payment links")
    void shouldReturnAllPaymentLinks() {
        // Given
        Map<String, String> paymentLinks = new HashMap<>();
        paymentLinks.put("starter", "https://buy.stripe.com/starter-link");
        paymentLinks.put("professional", "https://buy.stripe.com/professional-link");
        when(stripeProperties.getPaymentLinks()).thenReturn(paymentLinks);

        // When
        Map<String, String> allLinks = service.getAllPaymentLinks();

        // Then
        assertThat(allLinks).hasSize(2);
        assertThat(allLinks).containsEntry("starter", "https://buy.stripe.com/starter-link");
        assertThat(allLinks).containsEntry("professional", "https://buy.stripe.com/professional-link");
    }

    @Test
    @DisplayName("Should return empty map when payment links are null")
    void shouldReturnEmptyMapWhenPaymentLinksAreNull() {
        // Given
        when(stripeProperties.getPaymentLinks()).thenReturn(null);

        // When
        Map<String, String> allLinks = service.getAllPaymentLinks();

        // Then
        assertThat(allLinks).isEmpty();
    }

    @Test
    @DisplayName("Should check if payment link exists")
    void shouldCheckIfPaymentLinkExists() {
        // Given
        Map<String, String> paymentLinks = new HashMap<>();
        paymentLinks.put("starter", "https://buy.stripe.com/starter-link");
        when(stripeProperties.getPaymentLinks()).thenReturn(paymentLinks);

        // When/Then
        assertThat(service.hasPaymentLink("starter")).isTrue();
        assertThat(service.hasPaymentLink("unknown")).isFalse();
        assertThat(service.hasPaymentLink(null)).isFalse();
        assertThat(service.hasPaymentLink("")).isFalse();
    }

    @Test
    @DisplayName("Should handle null product name")
    void shouldHandleNullProductName() {
        // Given
        Map<String, String> paymentLinks = new HashMap<>();
        paymentLinks.put("starter", "https://buy.stripe.com/starter-link");
        when(stripeProperties.getPaymentLinks()).thenReturn(paymentLinks);

        // When
        String link = service.getPaymentLink(null);

        // Then
        assertThat(link).isNull();
    }

    @Test
    @DisplayName("Should handle blank product name")
    void shouldHandleBlankProductName() {
        // Given
        Map<String, String> paymentLinks = new HashMap<>();
        paymentLinks.put("starter", "https://buy.stripe.com/starter-link");
        when(stripeProperties.getPaymentLinks()).thenReturn(paymentLinks);

        // When
        String link = service.getPaymentLink("   ");

        // Then
        assertThat(link).isNull();
    }
}

