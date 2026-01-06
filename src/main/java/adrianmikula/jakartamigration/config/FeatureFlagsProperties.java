package adrianmikula.jakartamigration.config;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for feature flags.
 * 
 * Feature flags control access to premium features based on license tier.
 * 
 * Configuration example in application.yml:
 * <pre>
 * jakarta:
 *   migration:
 *     feature-flags:
 *       enabled: true
 *       default-tier: COMMUNITY
 *       license-key: ${JAKARTA_MCP_LICENSE_KEY:}
 *       features:
 *         AUTO_FIXES: false
 *         ONE_CLICK_REFACTOR: false
 *         BINARY_FIXES: false
 *         ADVANCED_ANALYSIS: false
 *         PRIORITY_SUPPORT: false
 *         CLOUD_HOSTING: false
 * </pre>
 */
@Data
@Validated
@ConfigurationProperties(prefix = "jakarta.migration.feature-flags")
public class FeatureFlagsProperties {

    /**
     * Whether feature flags are enabled.
     * When disabled, all features are available (for development/testing).
     */
    @NotNull
    private Boolean enabled = true;

    /**
     * Default license tier when no license key is provided.
     * COMMUNITY = Free tier, PREMIUM = Paid tier
     */
    @NotNull
    private LicenseTier defaultTier = LicenseTier.COMMUNITY;

    /**
     * License key for premium features.
     * Can be set via environment variable JAKARTA_MCP_LICENSE_KEY
     */
    private String licenseKey = "";

    /**
     * Per-feature overrides.
     * Allows enabling/disabling specific features regardless of tier.
     * Useful for testing or gradual rollouts.
     */
    private Map<String, Boolean> features = new HashMap<>();

    /**
     * License tier enumeration.
     */
    public enum LicenseTier {
        /**
         * Community/Free tier.
         * Basic features: scanning, identification, analysis.
         */
        COMMUNITY,

        /**
         * Premium/Managed tier.
         * All features including auto-fixes, one-click refactor, binary fixes.
         */
        PREMIUM,

        /**
         * Enterprise tier (future).
         * Includes SLA, priority support, custom integrations.
         */
        ENTERPRISE
    }
}

