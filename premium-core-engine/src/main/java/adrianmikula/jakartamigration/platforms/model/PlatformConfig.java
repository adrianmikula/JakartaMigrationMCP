package adrianmikula.jakartamigration.platforms.model;

import java.util.List;
import java.util.Map;

/**
 * Represents a platform configuration loaded from platforms.yaml
 */
public record PlatformConfig(
    String name,
    String description,
    List<DetectionPattern> patterns,
    JakartaCompatibility jakartaCompatibility,
    List<String> javaxVersions,
    Map<String, String> requirements
) {}
