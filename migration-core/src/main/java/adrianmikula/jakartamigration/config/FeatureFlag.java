package adrianmikula.jakartamigration.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enumeration of all feature flags in the Jakarta Migration MCP Server.
 * 
 * Features are split into COMMUNITY (free) and PREMIUM (paid) tiers.
 * JetBrains Marketplace handles subscription management and license validation.
 */
@Getter
public enum FeatureFlag {

    // === PREMIUM FEATURES ===
    // All features below require a JetBrains Marketplace subscription
    // Pricing: $49/month or $399/year (with 7-day free trial)

    /**
     * Auto-fixes / Auto-remediation.
     * Automatically fix detected issues without manual intervention.
     */
    AUTO_FIXES(
        "auto-fixes",
        "Automatic issue remediation",
        "Automatically fix detected Jakarta migration issues",
        FeatureFlagsProperties.LicenseTier.PREMIUM
    ),

    /**
     * One-click refactor.
     * Execute complete refactoring with a single command.
     */
    ONE_CLICK_REFACTOR(
        "one-click-refactor",
        "One-click refactoring",
        "Execute complete Jakarta migration refactoring with a single command",
        FeatureFlagsProperties.LicenseTier.PREMIUM
    ),

    /**
     * Binary fixes.
     * Fix issues in compiled binaries/JARs.
     */
    BINARY_FIXES(
        "binary-fixes",
        "Binary file fixes",
        "Fix Jakarta migration issues in compiled binaries and JAR files",
        FeatureFlagsProperties.LicenseTier.PREMIUM
    ),

    /**
     * Advanced analysis.
     * Deep dependency analysis, transitive conflict resolution.
     */
    ADVANCED_ANALYSIS(
        "advanced-analysis",
        "Advanced dependency analysis",
        "Deep dependency analysis with transitive conflict detection and resolution",
        FeatureFlagsProperties.LicenseTier.PREMIUM
    ),

    /**
     * Batch operations.
     * Process multiple projects in batch.
     */
    BATCH_OPERATIONS(
        "batch-operations",
        "Batch processing",
        "Process multiple projects in batch operations",
        FeatureFlagsProperties.LicenseTier.PREMIUM
    ),

    /**
     * Custom recipes.
     * Create and use custom migration recipes.
     */
    CUSTOM_RECIPES(
        "custom-recipes",
        "Custom migration recipes",
        "Create and use custom Jakarta migration recipes",
        FeatureFlagsProperties.LicenseTier.PREMIUM
    ),

    /**
     * API access.
     * Programmatic API access for integrations.
     */
    API_ACCESS(
        "api-access",
        "API access",
        "Programmatic API access for CI/CD and tool integrations",
        FeatureFlagsProperties.LicenseTier.PREMIUM
    ),

    /**
     * Export reports.
     * Export detailed reports in multiple formats.
     */
    EXPORT_REPORTS(
        "export-reports",
        "Report export",
        "Export detailed migration reports in PDF, HTML, and other formats",
        FeatureFlagsProperties.LicenseTier.PREMIUM
    );

    // === COMMUNITY FEATURES ===
    // These features are always available (no premium required):
    // - Basic scanning
    // - Dependency analysis
    // - Migration planning
    // - Problem identification

    /**
     * Feature flag key (used in configuration).
     */
    private final String key;

    /**
     * Human-readable feature name.
     */
    private final String name;

    /**
     * Feature description.
     */
    private final String description;

    /**
     * Minimum license tier required to use this feature.
     */
    private final FeatureFlagsProperties.LicenseTier requiredTier;

    /**
     * Enum constructor.
     */
    FeatureFlag(String key, String name, String description, FeatureFlagsProperties.LicenseTier requiredTier) {
        this.key = key;
        this.name = name;
        this.description = description;
        this.requiredTier = requiredTier;
    }

    /**
     * Check if this feature is available for the given tier.
     */
    public boolean isAvailableFor(FeatureFlagsProperties.LicenseTier tier) {
        return tier.ordinal() >= requiredTier.ordinal();
    }

    /**
     * Get feature tier as a human-readable string.
     */
    public String getTierString() {
        return requiredTier == FeatureFlagsProperties.LicenseTier.PREMIUM ? "PREMIUM" : "COMMUNITY";
    }

    /**
     * Get upgrade message for this feature.
     */
    public String getUpgradeMessage() {
        return String.format(
            "%s requires a premium subscription. %s",
            name,
            getPricingInfo()
        );
    }

    /**
     * Get pricing information for upgrade prompt.
     */
    public String getPricingInfo() {
        return String.format(
            "Upgrade to Premium: %s or %s. Start your free 7-day trial today!",
            FeatureFlagsProperties.getMonthlyPriceFormatted(),
            FeatureFlagsProperties.getYearlyPriceFormatted()
        );
    }
}

