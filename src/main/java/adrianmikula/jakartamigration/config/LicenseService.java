package adrianmikula.jakartamigration.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service for license tier determination.
 *
 * This package is distributed only as an npm package; there is no external
 * payment or licensing. The effective tier is always the configured default
 * (e.g. ENTERPRISE so all features are available).
 */
@Service
@RequiredArgsConstructor
public class LicenseService {

    private final FeatureFlagsProperties properties;

    /**
     * Returns the effective license tier.
     * Always returns the configured default tier (no external validation).
     */
    public FeatureFlagsProperties.LicenseTier getDefaultTier() {
        return properties.getDefaultTier();
    }
}
