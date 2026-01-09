package adrianmikula.jakartamigration.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration that automatically enables the appropriate licensing system
 * based on the deployment platform.
 * 
 * Platform Detection:
 * - Apify platform: ACTOR_ID environment variable is set → Apify licensing enabled
 * - Local/npm: ACTOR_ID not set → Stripe licensing enabled
 * 
 * This allows the same codebase to work correctly in both environments
 * without manual configuration.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class PlatformBasedLicensingConfig {
    
    private final PlatformDetectionService platformDetectionService;
    private final ApifyLicenseProperties apifyProperties;
    private final StripeLicenseProperties stripeProperties;
    
    /**
     * Auto-configure licensing based on platform detection.
     * 
     * This runs after all properties are loaded but before beans are created,
     * allowing us to modify the enabled flags before conditional beans are evaluated.
     */
    @PostConstruct
    public void configureLicensing() {
        boolean isApify = platformDetectionService.isApifyPlatform();
        
        // Auto-enable Apify licensing on Apify platform
        if (isApify) {
            // Only enable if not explicitly disabled via environment variable
            String apifyEnabledEnv = System.getenv("APIFY_VALIDATION_ENABLED");
            if (apifyEnabledEnv == null || apifyEnabledEnv.isBlank()) {
                apifyProperties.setEnabled(true);
                log.info("Auto-enabled Apify licensing (detected Apify platform)");
            } else {
                log.info("Apify licensing configured via APIFY_VALIDATION_ENABLED={}", apifyEnabledEnv);
            }
            
            // Disable Stripe on Apify platform (unless explicitly enabled)
            String stripeEnabledEnv = System.getenv("STRIPE_VALIDATION_ENABLED");
            if (stripeEnabledEnv == null || stripeEnabledEnv.isBlank()) {
                stripeProperties.setEnabled(false);
                log.info("Auto-disabled Stripe licensing (Apify platform detected)");
            } else {
                log.info("Stripe licensing configured via STRIPE_VALIDATION_ENABLED={}", stripeEnabledEnv);
            }
        } else {
            // Auto-enable Stripe licensing for local/npm deployment
            String stripeEnabledEnv = System.getenv("STRIPE_VALIDATION_ENABLED");
            if (stripeEnabledEnv == null || stripeEnabledEnv.isBlank()) {
                stripeProperties.setEnabled(true);
                log.info("Auto-enabled Stripe licensing (local/npm deployment detected)");
            } else {
                log.info("Stripe licensing configured via STRIPE_VALIDATION_ENABLED={}", stripeEnabledEnv);
            }
            
            // Disable Apify on local deployment (unless explicitly enabled)
            String apifyEnabledEnv = System.getenv("APIFY_VALIDATION_ENABLED");
            if (apifyEnabledEnv == null || apifyEnabledEnv.isBlank()) {
                apifyProperties.setEnabled(false);
                log.info("Auto-disabled Apify licensing (local deployment detected)");
            } else {
                log.info("Apify licensing configured via APIFY_VALIDATION_ENABLED={}", apifyEnabledEnv);
            }
        }
        
        log.info("Platform-based licensing configuration complete:");
        log.info("  Platform: {}", platformDetectionService.getPlatformName());
        log.info("  Apify enabled: {}", apifyProperties.getEnabled());
        log.info("  Stripe enabled: {}", stripeProperties.getEnabled());
    }
}

