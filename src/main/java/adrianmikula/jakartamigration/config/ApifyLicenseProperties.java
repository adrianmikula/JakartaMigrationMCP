package adrianmikula.jakartamigration.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for Apify license validation.
 * 
 * Apify is a platform for hosting and monetizing MCP servers.
 * License keys are validated against Apify's API to determine tier.
 * 
 * Configuration example in application.yml:
 * <pre>
 * jakarta:
 *   migration:
 *     apify:
 *       enabled: true
 *       api-url: https://api.apify.com/v2
 *       api-token: ${APIFY_API_TOKEN:}
 *       cache-ttl-seconds: 3600
 *       timeout-seconds: 5
 * </pre>
 */
@Data
@Validated
@ConfigurationProperties(prefix = "jakarta.migration.apify")
public class ApifyLicenseProperties {

    /**
     * Whether Apify license validation is enabled.
     * When disabled, falls back to simple local validation.
     */
    private Boolean enabled = true;

    /**
     * Apify API base URL.
     * Default: https://api.apify.com/v2
     */
    @NotBlank
    private String apiUrl = "https://api.apify.com/v2";

    /**
     * Apify API token for making validation requests.
     * Can be set via environment variable APIFY_API_TOKEN.
     * This is your Apify account token, not the user's license key.
     */
    private String apiToken = "";

    /**
     * Cache TTL for license validation results (in seconds).
     * Prevents excessive API calls for the same license key.
     * Default: 3600 (1 hour).
     */
    private Long cacheTtlSeconds = 3600L;

    /**
     * Request timeout in seconds.
     * Default: 5 seconds.
     */
    private Integer timeoutSeconds = 5;

    /**
     * Actor ID or name for license validation.
     * If your MCP server is deployed as an Apify Actor, specify it here.
     * This is used to validate that the license key is associated with your actor.
     */
    private String actorId = "";

    /**
     * Whether to allow offline validation (fallback when API is unavailable).
     * When true, uses cached results or simple validation if API fails.
     */
    private Boolean allowOfflineValidation = true;
}

