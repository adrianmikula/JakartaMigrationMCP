package adrianmikula.jakartamigration.api.service;

import adrianmikula.jakartamigration.config.StripeLicenseProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for managing Stripe Payment Links.
 * 
 * Payment Links are hosted by Stripe and don't require building a checkout page.
 * This service provides access to pre-configured payment links for different products.
 * 
 * Based on the freemium + one-time purchase model:
 * - Starter: 50 credits for $5
 * - Professional: 200 credits for $15
 * - Enterprise: 1000 credits for $50
 */
@Slf4j
@Service
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = "jakarta.migration.stripe.enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class StripePaymentLinkService {

    private final StripeLicenseProperties stripeProperties;

    /**
     * Get payment link for a product/tier.
     * 
     * @param productName Product name (e.g., "starter", "professional", "enterprise", "premium")
     * @return Payment link URL, or null if not configured
     */
    public String getPaymentLink(String productName) {
        if (productName == null || productName.isBlank()) {
            return null;
        }
        
        String normalizedName = productName.toLowerCase().trim();
        Map<String, String> paymentLinks = stripeProperties.getPaymentLinks();
        
        if (paymentLinks == null || paymentLinks.isEmpty()) {
            log.warn("No payment links configured");
            return null;
        }
        
        String paymentLink = paymentLinks.get(normalizedName);
        if (paymentLink == null) {
            log.debug("No payment link found for product: {}", productName);
            return null;
        }
        
        log.debug("Retrieved payment link for product: {}", productName);
        return paymentLink;
    }

    /**
     * Get all available payment links.
     * 
     * @return Map of product names to payment link URLs
     */
    public Map<String, String> getAllPaymentLinks() {
        Map<String, String> paymentLinks = stripeProperties.getPaymentLinks();
        if (paymentLinks == null) {
            return new HashMap<>();
        }
        return new HashMap<>(paymentLinks);
    }

    /**
     * Check if a payment link exists for a product.
     * 
     * @param productName Product name
     * @return true if payment link is configured
     */
    public boolean hasPaymentLink(String productName) {
        if (productName == null || productName.isBlank()) {
            return false;
        }
        
        String normalizedName = productName.toLowerCase().trim();
        Map<String, String> paymentLinks = stripeProperties.getPaymentLinks();
        
        return paymentLinks != null && paymentLinks.containsKey(normalizedName) 
            && paymentLinks.get(normalizedName) != null 
            && !paymentLinks.get(normalizedName).isBlank();
    }
}

