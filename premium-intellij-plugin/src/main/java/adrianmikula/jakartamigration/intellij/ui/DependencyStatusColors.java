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
            case NO_JAKARTA_VERSION -> STATUS_NO_JAKARTA;
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
}
