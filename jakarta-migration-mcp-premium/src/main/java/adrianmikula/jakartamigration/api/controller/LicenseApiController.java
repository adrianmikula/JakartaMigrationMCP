package adrianmikula.jakartamigration.api.controller;

import adrianmikula.jakartamigration.api.dto.*;
import adrianmikula.jakartamigration.api.service.CreditService;
import adrianmikula.jakartamigration.api.service.StripePaymentLinkService;
import adrianmikula.jakartamigration.config.FeatureFlagsProperties;
import adrianmikula.jakartamigration.config.LicenseService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * REST API controller for license and credit management.
 * 
 * This controller provides endpoints for:
 * - License validation
 * - Credit balance queries
 * - Credit consumption
 * - Credit synchronization
 * 
 * All endpoints require API authentication via Bearer token (SERVER_API_KEY).
 * 
 * Endpoints:
 * - GET /api/v1/licenses/{licenseKey}/validate - Validate license key
 * - GET /api/v1/credits/{licenseKey}/balance - Get credit balance
 * - POST /api/v1/credits/{licenseKey}/consume - Consume credits
 * - POST /api/v1/credits/{licenseKey}/sync - Sync credits from Stripe
 */
@RestController
@RequestMapping("/api/v1")
@Slf4j
public class LicenseApiController {
    
    private final LicenseService licenseService;
    private final CreditService creditService;
    @org.springframework.lang.Nullable
    private final StripePaymentLinkService paymentLinkService;
    
    public LicenseApiController(
            LicenseService licenseService,
            CreditService creditService,
            @org.springframework.beans.factory.annotation.Autowired(required = false) 
            @org.springframework.lang.Nullable StripePaymentLinkService paymentLinkService) {
        this.licenseService = licenseService;
        this.creditService = creditService;
        this.paymentLinkService = paymentLinkService;
    }
    
    @Value("${jakarta.migration.license-api.server-api-key:}")
    private String serverApiKey;

    /**
     * Validate a license key.
     * 
     * @param licenseKey The license key to validate
     * @param authHeader Authorization header (Bearer token)
     * @return License validation response
     */
    @GetMapping("/licenses/{licenseKey}/validate")
    public ResponseEntity<LicenseValidationResponse> validateLicense(
            @PathVariable String licenseKey,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        // Validate API authentication
        if (!isValidApiKey(authHeader)) {
            log.warn("Unauthorized license validation request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(LicenseValidationResponse.builder()
                    .valid(false)
                    .message("Unauthorized: Invalid API key")
                    .build());
        }
        
        log.info("Validating license key: {}", maskKey(licenseKey));
        
        try {
            FeatureFlagsProperties.LicenseTier tier = licenseService.validateLicense(licenseKey);
            
            if (tier == null) {
                return ResponseEntity.ok(LicenseValidationResponse.builder()
                    .valid(false)
                    .message("Invalid license key")
                    .build());
            }
            
            // Initialize credits if this is the first validation
            // Premium tier gets 50 credits, Enterprise gets 100 credits
            if (creditService.getBalance(licenseKey) == 0) {
                int initialCredits = tier == FeatureFlagsProperties.LicenseTier.PREMIUM ? 50 : 
                                    tier == FeatureFlagsProperties.LicenseTier.ENTERPRISE ? 100 : 0;
                if (initialCredits > 0) {
                    creditService.initializeCredits(licenseKey, initialCredits);
                }
            }
            
            return ResponseEntity.ok(LicenseValidationResponse.builder()
                .valid(true)
                .tier(tier.name())
                .expiresAt(null) // TODO: Extract expiration from license if available
                .build());
                
        } catch (Exception e) {
            log.error("Error validating license key: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(LicenseValidationResponse.builder()
                    .valid(false)
                    .message("Error validating license: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Get credit balance for a license key.
     * 
     * @param licenseKey The license key
     * @param authHeader Authorization header (Bearer token)
     * @return Credit balance response
     */
    @GetMapping("/credits/{licenseKey}/balance")
    public ResponseEntity<CreditBalanceResponse> getBalance(
            @PathVariable String licenseKey,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        // Validate API authentication
        if (!isValidApiKey(authHeader)) {
            log.warn("Unauthorized credit balance request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(CreditBalanceResponse.builder()
                    .balance(0)
                    .message("Unauthorized: Invalid API key")
                    .build());
        }
        
        log.debug("Getting credit balance for license key: {}", maskKey(licenseKey));
        
        try {
            int balance = creditService.getBalance(licenseKey);
            Instant lastSync = creditService.getLastSync(licenseKey);
            
            return ResponseEntity.ok(CreditBalanceResponse.builder()
                .balance(balance)
                .lastSync(lastSync)
                .build());
                
        } catch (Exception e) {
            log.error("Error getting credit balance: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CreditBalanceResponse.builder()
                    .balance(0)
                    .message("Error getting balance: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Consume credits for a license key.
     * 
     * @param licenseKey The license key
     * @param request Credit consumption request
     * @param authHeader Authorization header (Bearer token)
     * @return Credit consumption response
     */
    @PostMapping("/credits/{licenseKey}/consume")
    public ResponseEntity<ConsumeCreditsResponse> consumeCredits(
            @PathVariable String licenseKey,
            @Valid @RequestBody ConsumeCreditsRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        // Validate API authentication
        if (!isValidApiKey(authHeader)) {
            log.warn("Unauthorized credit consumption request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ConsumeCreditsResponse.builder()
                    .success(false)
                    .message("Unauthorized: Invalid API key")
                    .build());
        }
        
        log.info("Consuming {} credits for license key: {} (tool: {})",
            request.getAmount(), maskKey(licenseKey), request.getTool());
        
        try {
            String transactionId = creditService.consumeCredits(
                licenseKey,
                request.getAmount(),
                request.getTool()
            );
            
            if (transactionId == null) {
                int currentBalance = creditService.getBalance(licenseKey);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ConsumeCreditsResponse.builder()
                        .success(false)
                        .newBalance(currentBalance)
                        .message("Insufficient credits")
                        .build());
            }
            
            int newBalance = creditService.getBalance(licenseKey);
            
            return ResponseEntity.ok(ConsumeCreditsResponse.builder()
                .success(true)
                .newBalance(newBalance)
                .transactionId(transactionId)
                .build());
                
        } catch (Exception e) {
            log.error("Error consuming credits: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ConsumeCreditsResponse.builder()
                    .success(false)
                    .message("Error consuming credits: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Sync credits from Stripe (or other source).
     * 
     * This endpoint is typically called when:
     * - User purchases credits
     * - Subscription renews
     * - Credits are added via Stripe webhook
     * 
     * @param licenseKey The license key
     * @param authHeader Authorization header (Bearer token)
     * @return Credit sync response
     */
    @PostMapping("/credits/{licenseKey}/sync")
    public ResponseEntity<SyncCreditsResponse> syncCredits(
            @PathVariable String licenseKey,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        // Validate API authentication
        if (!isValidApiKey(authHeader)) {
            log.warn("Unauthorized credit sync request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(SyncCreditsResponse.builder()
                    .synced(false)
                    .message("Unauthorized: Invalid API key")
                    .build());
        }
        
        log.info("Syncing credits for license key: {}", maskKey(licenseKey));
        
        try {
            // TODO: Integrate with Stripe API to get actual credit balance
            // For now, we'll use a placeholder implementation
            // In production, this would:
            // 1. Query Stripe for subscription/customer info
            // 2. Calculate credits based on subscription tier
            // 3. Update local credit balance
            
            // Placeholder: Get current balance (would be replaced with Stripe query)
            int currentBalance = creditService.getBalance(licenseKey);
            
            // TODO: Replace with actual Stripe integration
            // For MVP, we'll just update the last sync time
            boolean synced = creditService.syncCredits(licenseKey, currentBalance);
            
            Instant lastSync = creditService.getLastSync(licenseKey);
            
            return ResponseEntity.ok(SyncCreditsResponse.builder()
                .balance(currentBalance)
                .synced(synced)
                .lastSync(lastSync)
                .message("Credits synced successfully")
                .build());
                
        } catch (Exception e) {
            log.error("Error syncing credits: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(SyncCreditsResponse.builder()
                    .synced(false)
                    .message("Error syncing credits: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Validate API key from Authorization header.
     * 
     * @param authHeader Authorization header (should be "Bearer {api-key}")
     * @return true if valid, false otherwise
     */
    private boolean isValidApiKey(String authHeader) {
        if (serverApiKey == null || serverApiKey.isBlank()) {
            log.warn("SERVER_API_KEY not configured - allowing all requests (not recommended for production)");
            return true; // Allow if not configured (for development)
        }
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }
        
        String token = authHeader.substring(7);
        return serverApiKey.equals(token);
    }

    /**
     * Validate a license by email address.
     * 
     * @param email The customer email address
     * @param authHeader Authorization header (Bearer token)
     * @return License validation response
     */
    @GetMapping("/licenses/email/{email}/validate")
    public ResponseEntity<LicenseValidationResponse> validateLicenseByEmail(
            @PathVariable String email,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        if (!isValidApiKey(authHeader)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(LicenseValidationResponse.builder()
                    .valid(false)
                    .message("Invalid or missing API key")
                    .build());
        }
        
        try {
            FeatureFlagsProperties.LicenseTier tier = licenseService.validateLicenseByEmail(email);
            
            return ResponseEntity.ok(LicenseValidationResponse.builder()
                .valid(tier != null)
                .tier(tier != null ? tier.name() : null)
                .message(tier != null ? "License validated successfully" : "No valid license found for email")
                .build());
                
        } catch (Exception e) {
            log.error("Error validating license by email: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(LicenseValidationResponse.builder()
                    .valid(false)
                    .message("Error validating license: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Get payment link for a product/tier.
     * 
     * @param productName Product name (e.g., "starter", "professional", "enterprise")
     * @param authHeader Authorization header (Bearer token)
     * @return Payment link response
     */
    @GetMapping("/payment-links/{productName}")
    public ResponseEntity<PaymentLinkResponse> getPaymentLink(
            @PathVariable String productName,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        if (!isValidApiKey(authHeader)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(PaymentLinkResponse.builder()
                    .success(false)
                    .message("Invalid or missing API key")
                    .build());
        }
        
        if (paymentLinkService == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(PaymentLinkResponse.builder()
                    .success(false)
                    .message("Stripe payment links are not enabled")
                    .build());
        }
        
        try {
            String paymentLink = paymentLinkService.getPaymentLink(productName);
            
            if (paymentLink == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(PaymentLinkResponse.builder()
                        .success(false)
                        .message("Payment link not found for product: " + productName)
                        .build());
            }
            
            return ResponseEntity.ok(PaymentLinkResponse.builder()
                .success(true)
                .productName(productName)
                .paymentLink(paymentLink)
                .message("Payment link retrieved successfully")
                .build());
                
        } catch (Exception e) {
            log.error("Error retrieving payment link: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(PaymentLinkResponse.builder()
                    .success(false)
                    .message("Error retrieving payment link: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Get all available payment links.
     * 
     * @param authHeader Authorization header (Bearer token)
     * @return Map of product names to payment links
     */
    @GetMapping("/payment-links")
    public ResponseEntity<java.util.Map<String, String>> getAllPaymentLinks(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        if (!isValidApiKey(authHeader)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        if (paymentLinkService == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        
        try {
            java.util.Map<String, String> paymentLinks = paymentLinkService.getAllPaymentLinks();
            return ResponseEntity.ok(paymentLinks);
        } catch (Exception e) {
            log.error("Error retrieving payment links: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Mask license key for logging.
     */
    private String maskKey(String key) {
        if (key == null || key.length() <= 8) {
            return "***";
        }
        return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
    }
}

