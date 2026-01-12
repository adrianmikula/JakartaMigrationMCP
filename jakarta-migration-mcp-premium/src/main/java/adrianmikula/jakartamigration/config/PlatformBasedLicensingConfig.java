package adrianmikula.jakartamigration.config;

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
 * 
 * NOTE: The actual auto-configuration is performed by {@link PlatformBasedLicensingPostProcessor},
 * which runs as a BeanFactoryPostProcessor BEFORE bean definitions are evaluated.
 * This ensures that @ConditionalOnProperty annotations see the correct property values.
 * 
 * This class is kept for backward compatibility and may be used for other
 * platform-based configuration in the future.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class PlatformBasedLicensingConfig {
    
    private final PlatformDetectionService platformDetectionService;
    private final ApifyLicenseProperties apifyProperties;
    private final StripeLicenseProperties stripeProperties;
    
    /**
     * Log the current licensing configuration.
     * 
     * This method can be called after application context is fully initialized
     * to verify the configuration. The actual configuration is done by
     * PlatformBasedLicensingPostProcessor before beans are created.
     */
    public void logConfiguration() {
        log.info("Platform-based licensing configuration status:");
        log.info("  Platform: {}", platformDetectionService.getPlatformName());
        log.info("  Apify enabled: {}", apifyProperties.getEnabled());
        log.info("  Stripe enabled: {}", stripeProperties.getEnabled());
    }
}

