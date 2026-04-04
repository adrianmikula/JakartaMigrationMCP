package adrianmikula.jakartamigration.platforms.model;

import java.util.List;
import java.util.Map;

/**
 * Represents complete result of a platform scan
 */
public record PlatformScanResult(
    List<PlatformDetection> detectedPlatforms,
    int totalRiskScore,
    List<String> recommendations
) {}
