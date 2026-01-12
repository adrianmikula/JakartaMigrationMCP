package adrianmikula.jakartamigration.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for detecting the deployment platform.
 * 
 * Determines whether the application is running on:
 * - Apify platform (ACTOR_ID environment variable is set)
 * - Local/npm deployment (ACTOR_ID is not set)
 * 
 * This is used to automatically configure the appropriate licensing system:
 * - Apify platform: Apify billing/licensing
 * - Local/npm: Stripe licensing
 */
@Slf4j
@Service
public class PlatformDetectionService {
    
    /**
     * Check if running on Apify platform.
     * 
     * Apify platform sets the ACTOR_ID environment variable.
     * 
     * @return true if ACTOR_ID is set and not blank, false otherwise
     */
    public boolean isApifyPlatform() {
        String actorId = System.getenv("ACTOR_ID");
        boolean isApify = actorId != null && !actorId.isBlank();
        
        if (isApify) {
            log.info("Detected Apify platform deployment (ACTOR_ID={})", actorId);
        } else {
            log.debug("Not running on Apify platform (ACTOR_ID not set)");
        }
        
        return isApify;
    }
    
    /**
     * Check if running locally (not on Apify platform).
     * 
     * This includes:
     * - npm/npx installations
     * - Local development
     * - Any deployment without ACTOR_ID
     * 
     * @return true if not on Apify platform, false otherwise
     */
    public boolean isLocalDeployment() {
        return !isApifyPlatform();
    }
    
    /**
     * Get the detected platform name.
     * 
     * @return "apify" if on Apify platform, "local" otherwise
     */
    public String getPlatformName() {
        return isApifyPlatform() ? "apify" : "local";
    }
}

