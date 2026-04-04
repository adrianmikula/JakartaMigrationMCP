package adrianmikula.jakartamigration.platforms.model;

import java.util.List;

/**
 * Represents Jakarta EE compatibility information
 */
public record JakartaCompatibility(
    String minVersion,
    List<String> supportedVersions
) {}
