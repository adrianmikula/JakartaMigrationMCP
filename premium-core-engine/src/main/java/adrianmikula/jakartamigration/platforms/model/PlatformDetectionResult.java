package adrianmikula.jakartamigration.platforms.model;

import java.util.List;
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

/**
 * Represents complete result of a platform scan
 */
public record PlatformScanResult(
    List<PlatformDetection> detectedPlatforms,
    int totalRiskScore,
    List<String> recommendations
) {}
