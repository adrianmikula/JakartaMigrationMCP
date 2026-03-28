package adrianmikula.jakartamigration.platforms.model;

import java.util.Map;

/**
 * Represents a detected platform in a project
 */
public record PlatformDetection(
    String platformType,
    String platformName,
    String detectedVersion,
    boolean isJakartaCompatible,
    String minJakartaVersion,
    Map<String, String> requirements
) {}
