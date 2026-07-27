package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;

import java.awt.Color;

/**
 * Shared utility class for dependency migration status colors.
 * Eliminates duplication across DependenciesTableComponent, DependencyTreeRenderer,
 * and DependencyGraphComponent.
 */
public final class DependencyStatusColors {

    private DependencyStatusColors() {
        // Utility class - prevent instantiation
    }

    // Status colors - consistent across all dependency UI components
    public static final Color STATUS_COMPATIBLE = new Color(40, 167, 69); // Green
    public static final Color STATUS_NEEDS_UPGRADE = new Color(255, 193, 7); // Yellow
    public static final Color STATUS_REQUIRES_MANUAL = new Color(255, 193, 7); // Yellow (same as needs upgrade)
    public static final Color STATUS_NO_JAKARTA = new Color(220, 53, 69); // Red
    public static final Color STATUS_MIGRATED = new Color(23, 162, 184); // Cyan
    public static final Color STATUS_UNKNOWN = new Color(108, 117, 125); // Gray

    /**
     * Get the status color based on migration status.
     * 
     * @param status The migration status
     * @return The color for the status
     */
    public static Color getStatusColor(DependencyMigrationStatus status) {
        if (status == null) {
            return STATUS_UNKNOWN;
        }
        return switch (status) {
            case COMPATIBLE -> STATUS_COMPATIBLE;
            case NEEDS_UPGRADE, REQUIRES_MANUAL_MIGRATION -> STATUS_NEEDS_UPGRADE;
            case UNKNOWN_REVIEW -> STATUS_UNKNOWN;
            case NO_JAKARTA_VERSION, MAVEN_LOOKUP_FAILED -> STATUS_NO_JAKARTA;
            case MIGRATED -> STATUS_MIGRATED;
            default -> STATUS_UNKNOWN;
        };
    }

    /**
     * Get background color for a status (lighter version for table cells).
     * 
     * @param status The migration status
     * @return The background color for the status
     */
    public static Color getStatusBackgroundColor(DependencyMigrationStatus status) {
        Color baseColor = getStatusColor(status);
        return new Color(
            Math.min(255, baseColor.getRed() + 50),
            Math.min(255, baseColor.getGreen() + 50),
            Math.min(255, baseColor.getBlue() + 50)
        );
    }

    /**
     * Get the user-facing label for a migration status, including an icon prefix.
     * 
     * @param status The migration status
     * @return The human-readable status label
     */
    public static String getStatusText(DependencyMigrationStatus status) {
        if (status == null) {
            return "? Unknown";
        }
        return switch (status) {
            case COMPATIBLE -> "✓ Compatible";
            case NEEDS_UPGRADE -> "↑ Upgrade Available";
            case NO_JAKARTA_VERSION -> "✗ No Jakarta Version";
            case REQUIRES_MANUAL_MIGRATION -> "⚠ Manual Review Required";
            case UNKNOWN_REVIEW -> "? Analysis Pending";
            case MIGRATED -> "✓ Migrated";
            case MAVEN_LOOKUP_FAILED -> "⚠ Jakarta Version Not Found";
            case BUILD_TOOL_ERROR -> "⚠ Build Tool Error";
            case UNKNOWN -> "? Unknown";
        };
    }

    /**
     * Get the internal slug for a migration status, used for storage and reports.
     * 
     * @param status The migration status
     * @return The slug string for the status
     */
    public static String getStatusSlug(DependencyMigrationStatus status) {
        if (status == null) {
            return "unknown";
        }
        return switch (status) {
            case COMPATIBLE -> "compatible";
            case NEEDS_UPGRADE -> "upgrade-available";
            case NO_JAKARTA_VERSION, MAVEN_LOOKUP_FAILED -> "no-jakarta-version";
            case REQUIRES_MANUAL_MIGRATION -> "requires-migration";
            case MIGRATED -> "migrated";
            case UNKNOWN, UNKNOWN_REVIEW, BUILD_TOOL_ERROR -> "unknown";
        };
    }
}
