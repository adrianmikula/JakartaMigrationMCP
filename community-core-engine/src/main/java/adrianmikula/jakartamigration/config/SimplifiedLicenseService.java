package adrianmikula.jakartamigration.config;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * Simplified license service
 * Removes complex HTTP client logic and marketplace API calls
 * Focuses on basic license tier determination
 */
@Slf4j
public class SimplifiedLicenseService {
    
    /**
     * Simple license tier check - just uses system property
     */
    public LicenseTier getLicenseTier() {
        String tier = System.getProperty("jakarta.migration.license.tier", "COMMUNITY");
        
        try {
            return LicenseTier.valueOf(tier.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid license tier '{}', defaulting to COMMUNITY: {}", tier, e.getMessage());
            return LicenseTier.COMMUNITY;
        }
    }
    
    /**
     * Simple check if user has premium features
     */
    public boolean hasPremiumFeatures() {
        return getLicenseTier() == LicenseTier.PREMIUM;
    }
    
    /**
     * Simple subscription status
     */
    public String getSubscriptionStatus() {
        return hasPremiumFeatures() ? "Premium Subscription" : "Community (Free)";
    }
    
    /**
     * Simple trial check
     */
    public boolean isTrialActive() {
        return false; // Simplified - no trial logic
    }
    
    /**
     * Remaining trial days
     */
    public int getRemainingTrialDays() {
        return 0; // Simplified - no trial logic
    }
    
    /**
     * Upgrade prompt
     */
    public String getUpgradePrompt() {
        return hasPremiumFeatures() ? "Premium features active" : 
               "Upgrade to Premium for advanced features";
    }
    
    /**
     * Simple license tier enum
     */
    public enum LicenseTier {
        COMMUNITY,
        PREMIUM
    }
}
