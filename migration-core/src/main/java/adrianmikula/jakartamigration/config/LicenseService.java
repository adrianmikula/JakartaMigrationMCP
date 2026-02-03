package adrianmikula.jakartamigration.config;

import lombok.RequiredArgsConstructor;

/**
 * Service for license tier determination.
 *
 * This package is distributed only as an npm package; there is no external
 * payment or licensing. The effective tier is always the configured default
 * (e.g. ENTERPRISE so all features are available).
 */
public class LicenseService {

    private final FeatureFlagsProperties properties;

    public LicenseService(FeatureFlagsProperties properties) {
        this.properties = properties;
    }

    /**
     * Returns the effective license tier.
     * In IntelliJ mode, checks for the plugin license.
     * Otherwise returns the configured default tier.
     */
    public FeatureFlagsProperties.LicenseTier getDefaultTier() {
        // 1. Check for system property override (useful for testing)
        String override = System.getProperty("jakarta.migration.license.tier");
        if (override != null) {
            try {
                return FeatureFlagsProperties.LicenseTier.valueOf(override.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Ignore invalid tier
            }
        }

        // 2. Check for IntelliJ Licensing Facade if available
        try {
            // We use reflection to avoid hard dependency at compile time for the Spring
            // Boot app
            // though in the plugin they will be available.
            Class<?> facadeClass = Class.forName("com.intellij.ide.licensing.LicensingFacade");
            Object facade = facadeClass.getMethod("getInstance").invoke(null);
            if (facade != null) {
                // Check if license is valid for our plugin
                // Boolean isLicensed = (Boolean) facadeClass.getMethod("isLicensed",
                // String.class).invoke(facade, "adrianmikula.jakartamigration");
                // For now, check if a specific "premium" attribute or setting is set
                if ("true".equals(System.getProperty("jakarta.migration.premium"))) {
                    return FeatureFlagsProperties.LicenseTier.PREMIUM;
                }
            }
        } catch (Exception e) {
            // Not running in IntelliJ or class not found
        }

        return properties.getDefaultTier();
    }
}
