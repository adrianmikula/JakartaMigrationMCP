package adrianmikula.jakartamigration.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * BeanFactoryPostProcessor that auto-configures licensing based on platform detection.
 * 
 * This runs BEFORE bean definitions are evaluated, allowing us to modify environment
 * properties before @ConditionalOnProperty annotations are processed.
 * 
 * Platform Detection:
 * - Apify platform: ACTOR_ID environment variable is set → Apify licensing enabled
 * - Local/npm: ACTOR_ID not set → Stripe licensing enabled
 * 
 * This allows the same codebase to work correctly in both environments
 * without manual configuration.
 * 
 * NOTE: This processor must run early, so it's annotated with @Order(HIGHEST_PRECEDENCE).
 * In Spring Boot, the environment is typically available through the ApplicationContext.
 * We use a fallback to System properties if the environment is not accessible.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PlatformBasedLicensingPostProcessor implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        ConfigurableEnvironment configurableEnvironment = null;
        
        // Try to get environment from ApplicationContext
        // In Spring Boot, the beanFactory is typically part of an ApplicationContext
        try {
            // Check if beanFactory is actually a ConfigurableApplicationContext
            if (beanFactory instanceof ConfigurableApplicationContext) {
                configurableEnvironment = ((ConfigurableApplicationContext) beanFactory).getEnvironment();
            } else {
                // Try to get environment bean (should be available as a singleton)
                try {
                    if (beanFactory.containsBean("environment")) {
                        configurableEnvironment = beanFactory.getBean("environment", ConfigurableEnvironment.class);
                    }
                } catch (BeansException e) {
                    // Environment bean not available yet
                    log.debug("Environment bean not available: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.debug("Could not access environment: {}", e.getMessage());
        }
        
        if (configurableEnvironment == null) {
            // Fallback: Use System properties (works but less ideal)
            log.warn("Could not access ConfigurableEnvironment, using System properties fallback");
            configureViaSystemProperties();
            return;
        }
        
        // Detect platform
        boolean isApify = isApifyPlatform();
        String platformName = isApify ? "apify" : "local";
        
        log.info("Platform-based licensing auto-configuration (running before bean definition phase):");
        log.info("  Detected platform: {}", platformName);
        
        // Check if properties are explicitly set via environment variables
        String apifyEnabledEnv = System.getenv("APIFY_VALIDATION_ENABLED");
        String stripeEnabledEnv = System.getenv("STRIPE_VALIDATION_ENABLED");
        
        // Build property overrides
        Map<String, Object> propertyOverrides = new HashMap<>();
        
        if (isApify) {
            // Apify platform: Enable Apify, disable Stripe (unless explicitly set)
            if (apifyEnabledEnv == null || apifyEnabledEnv.isBlank()) {
                propertyOverrides.put("jakarta.migration.apify.enabled", "true");
                log.info("  Auto-enabled Apify licensing (Apify platform detected)");
            } else {
                log.info("  Apify licensing configured via APIFY_VALIDATION_ENABLED={}", apifyEnabledEnv);
            }
            
            if (stripeEnabledEnv == null || stripeEnabledEnv.isBlank()) {
                propertyOverrides.put("jakarta.migration.stripe.enabled", "false");
                log.info("  Auto-disabled Stripe licensing (Apify platform detected)");
            } else {
                log.info("  Stripe licensing configured via STRIPE_VALIDATION_ENABLED={}", stripeEnabledEnv);
            }
        } else {
            // Local/npm deployment: Enable Stripe, disable Apify (unless explicitly set)
            if (stripeEnabledEnv == null || stripeEnabledEnv.isBlank()) {
                propertyOverrides.put("jakarta.migration.stripe.enabled", "true");
                log.info("  Auto-enabled Stripe licensing (local/npm deployment detected)");
            } else {
                log.info("  Stripe licensing configured via STRIPE_VALIDATION_ENABLED={}", stripeEnabledEnv);
            }
            
            if (apifyEnabledEnv == null || apifyEnabledEnv.isBlank()) {
                propertyOverrides.put("jakarta.migration.apify.enabled", "false");
                log.info("  Auto-disabled Apify licensing (local deployment detected)");
            } else {
                log.info("  Apify licensing configured via APIFY_VALIDATION_ENABLED={}", apifyEnabledEnv);
            }
        }
        
        // Apply property overrides to environment
        if (!propertyOverrides.isEmpty()) {
            MapPropertySource propertySource = new MapPropertySource(
                "platformBasedLicensingOverrides", 
                propertyOverrides
            );
            // Add with highest priority (first in the list) so it overrides existing properties
            configurableEnvironment.getPropertySources().addFirst(propertySource);
            
            log.info("Platform-based licensing configuration complete:");
            log.info("  Platform: {}", platformName);
            log.info("  Apify enabled: {}", configurableEnvironment.getProperty("jakarta.migration.apify.enabled", Boolean.class, false));
            log.info("  Stripe enabled: {}", configurableEnvironment.getProperty("jakarta.migration.stripe.enabled", Boolean.class, false));
        }
    }
    
    /**
     * Fallback method that sets system properties directly.
     * Used when ConfigurableEnvironment is not available.
     */
    private void configureViaSystemProperties() {
        boolean isApify = isApifyPlatform();
        String platformName = isApify ? "apify" : "local";
        
        log.info("Platform-based licensing auto-configuration (using System properties fallback):");
        log.info("  Detected platform: {}", platformName);
        
        String apifyEnabledEnv = System.getenv("APIFY_VALIDATION_ENABLED");
        String stripeEnabledEnv = System.getenv("STRIPE_VALIDATION_ENABLED");
        
        if (isApify) {
            if (apifyEnabledEnv == null || apifyEnabledEnv.isBlank()) {
                System.setProperty("jakarta.migration.apify.enabled", "true");
                log.info("  Auto-enabled Apify licensing (Apify platform detected)");
            }
            if (stripeEnabledEnv == null || stripeEnabledEnv.isBlank()) {
                System.setProperty("jakarta.migration.stripe.enabled", "false");
                log.info("  Auto-disabled Stripe licensing (Apify platform detected)");
            }
        } else {
            if (stripeEnabledEnv == null || stripeEnabledEnv.isBlank()) {
                System.setProperty("jakarta.migration.stripe.enabled", "true");
                log.info("  Auto-enabled Stripe licensing (local/npm deployment detected)");
            }
            if (apifyEnabledEnv == null || apifyEnabledEnv.isBlank()) {
                System.setProperty("jakarta.migration.apify.enabled", "false");
                log.info("  Auto-disabled Apify licensing (local deployment detected)");
            }
        }
    }
    
    /**
     * Check if running on Apify platform.
     * 
     * Apify platform sets the ACTOR_ID environment variable.
     * 
     * @return true if ACTOR_ID is set and not blank, false otherwise
     */
    private boolean isApifyPlatform() {
        String actorId = System.getenv("ACTOR_ID");
        return actorId != null && !actorId.isBlank();
    }
}

