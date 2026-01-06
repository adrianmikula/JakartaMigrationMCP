package com.bugbounty.jakartamigration.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enumeration of all feature flags in the Jakarta Migration MCP Server.
 * 
 * Each feature flag represents a capability that can be enabled/disabled
 * based on the user's license tier.
 */
@Getter
@RequiredArgsConstructor
public enum FeatureFlag {

    /**
     * Auto-fixes / Auto-remediation.
     * Premium feature: Automatically fix detected issues without manual intervention.
     * Community: Can only identify problems.
     */
    AUTO_FIXES(
        "auto-fixes",
        "Automatic issue remediation",
        "Automatically fix detected Jakarta migration issues",
        FeatureFlagsProperties.LicenseTier.PREMIUM
    ),

    /**
     * One-click refactor.
     * Premium feature: Execute complete refactoring with a single command.
     * Community: Can only create migration plans.
     */
    ONE_CLICK_REFACTOR(
        "one-click-refactor",
        "One-click refactoring",
        "Execute complete Jakarta migration refactoring with a single command",
        FeatureFlagsProperties.LicenseTier.PREMIUM
    ),

    /**
     * Binary fixes.
     * Premium feature: Fix issues in compiled binaries/JARs.
     * Community: Can only analyze source code.
     */
    BINARY_FIXES(
        "binary-fixes",
        "Binary file fixes",
        "Fix Jakarta migration issues in compiled binaries and JAR files",
        FeatureFlagsProperties.LicenseTier.PREMIUM
    ),

    /**
     * Advanced analysis.
     * Premium feature: Deep dependency analysis, transitive conflict resolution.
     * Community: Basic dependency scanning.
     */
    ADVANCED_ANALYSIS(
        "advanced-analysis",
        "Advanced dependency analysis",
        "Deep dependency analysis with transitive conflict detection and resolution",
        FeatureFlagsProperties.LicenseTier.PREMIUM
    ),

    /**
     * Priority support.
     * Premium feature: Priority support with SLA guarantees.
     * Community: Community support only.
     */
    PRIORITY_SUPPORT(
        "priority-support",
        "Priority support",
        "Priority support with SLA guarantees and faster response times",
        FeatureFlagsProperties.LicenseTier.PREMIUM
    ),

    /**
     * Cloud hosting / Managed hosting.
     * Premium feature: Hosted version on cloud infrastructure.
     * Community: Local execution only.
     */
    CLOUD_HOSTING(
        "cloud-hosting",
        "Cloud hosting",
        "Managed cloud hosting with automatic scaling and monitoring",
        FeatureFlagsProperties.LicenseTier.PREMIUM
    ),

    /**
     * Batch operations.
     * Premium feature: Process multiple projects in batch.
     * Community: Single project analysis only.
     */
    BATCH_OPERATIONS(
        "batch-operations",
        "Batch processing",
        "Process multiple projects in batch operations",
        FeatureFlagsProperties.LicenseTier.PREMIUM
    ),

    /**
     * Custom recipes.
     * Premium feature: Create and use custom migration recipes.
     * Community: Standard recipes only.
     */
    CUSTOM_RECIPES(
        "custom-recipes",
        "Custom migration recipes",
        "Create and use custom Jakarta migration recipes",
        FeatureFlagsProperties.LicenseTier.PREMIUM
    ),

    /**
     * API access.
     * Premium feature: Programmatic API access for integrations.
     * Community: MCP interface only.
     */
    API_ACCESS(
        "api-access",
        "API access",
        "Programmatic API access for CI/CD and tool integrations",
        FeatureFlagsProperties.LicenseTier.PREMIUM
    ),

    /**
     * Export reports.
     * Premium feature: Export detailed reports in multiple formats.
     * Community: Basic JSON output only.
     */
    EXPORT_REPORTS(
        "export-reports",
        "Report export",
        "Export detailed migration reports in PDF, HTML, and other formats",
        FeatureFlagsProperties.LicenseTier.PREMIUM
    );

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
     * Check if this feature is available for the given tier.
     */
    public boolean isAvailableFor(FeatureFlagsProperties.LicenseTier tier) {
        return tier.ordinal() >= requiredTier.ordinal();
    }
}

