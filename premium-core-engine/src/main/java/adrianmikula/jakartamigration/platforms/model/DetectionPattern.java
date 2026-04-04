package adrianmikula.jakartamigration.platforms.model;

/**
 * Represents a detection pattern for finding application servers
 */
public record DetectionPattern(
    String file,
    String regex,
    int versionGroup
) {}
