package adrianmikula.jakartamigration.platforms.service;

import adrianmikula.jakartamigration.platforms.config.PlatformConfigLoader;
import adrianmikula.jakartamigration.platforms.model.DetectionPattern;
import adrianmikula.jakartamigration.platforms.model.JakartaCompatibility;
import adrianmikula.jakartamigration.platforms.model.PlatformConfig;
import adrianmikula.jakartamigration.platforms.model.PlatformDetection;
import adrianmikula.jakartamigration.platforms.model.PlatformScanResult;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for detecting application servers in a project
 */
public class PlatformDetectionService {
    private final PlatformConfigLoader configLoader;
    
    public PlatformDetectionService() {
        this.configLoader = new PlatformConfigLoader();
    }
    
    /**
     * Scans the entire project for application servers
     */
    public PlatformScanResult scanProject(Path projectPath) {
        List<PlatformDetection> detections = scanForPlatforms(projectPath);
        int totalRiskScore = calculateRiskScore(detections);
        List<String> recommendations = generateRecommendations(detections);
        
        return new PlatformScanResult(detections, totalRiskScore, recommendations);
    }
    
    /**
     * Detects a specific platform in the project
     */
    public PlatformDetection detectPlatform(Path projectPath, PlatformConfig config) {
        for (DetectionPattern pattern : config.patterns()) {
            String version = detectVersion(projectPath, pattern);
            if (version != null) {
                boolean isJakartaCompatible = isVersionCompatible(version, config.jakartaCompatibility().supportedVersions());
                return new PlatformDetection(
                    config.name(),
                    config.description(),
                    version,
                    isJakartaCompatible,
                    config.jakartaCompatibility().minVersion(),
                    config.requirements()
                );
            }
        }
        return null;
    }
    
    /**
     * Scans for all configured platforms
     */
    public List<PlatformDetection> scanForPlatforms(Path projectPath) {
        List<PlatformDetection> detections = new ArrayList<>();
        Map<String, PlatformConfig> configs = configLoader.getAllPlatformConfigs();
        
        for (Map.Entry<String, PlatformConfig> entry : configs.entrySet()) {
            PlatformDetection detection = detectPlatform(projectPath, entry.getValue());
            if (detection != null) {
                detections.add(detection);
            }
        }
        
        return detections;
    }
    
    /**
     * Calculates risk score based on detected platforms
     */
    public int calculateRiskScore(List<PlatformDetection> detections) {
        int totalScore = 0;
        
        for (PlatformDetection detection : detections) {
            String platformType = detection.platformType().toLowerCase();
            
            if (!detection.isJakartaCompatible()) {
                // Major version change (javax to jakarta) is significant risk
                totalScore += 10;
            }
            
            // Platform-specific risk scoring (5-10 points range)
            if (platformType.equals("java")) {
                // Java version changes are significant
                totalScore += 8;
            } else if (platformType.equals("spring")) {
                // Framework changes are moderate risk
                totalScore += 6;
            } else {
                // Runtime changes add moderate risk
                totalScore += 5;
            }
        }
        
        return totalScore;
    }
    
    /**
     * Detects version using a pattern
     */
    private String detectVersion(Path projectPath, DetectionPattern pattern) {
        try {
            Path targetFile = projectPath.resolve(pattern.file());
            if (!Files.exists(targetFile)) {
                return null;
            }
            
            String content = Files.readString(targetFile);
            Pattern regex = Pattern.compile(pattern.regex());
            Matcher matcher = regex.matcher(content);
            
            if (matcher.find()) {
                return matcher.group(pattern.versionGroup());
            }
            
            return null;
        } catch (IOException e) {
            return null;
        }
    }
    
    /**
     * Checks if detected version is compatible with Jakarta
     */
    private boolean isVersionCompatible(String detectedVersion, List<String> supportedVersions) {
        for (String supportedVersion : supportedVersions) {
            if (isVersionGreaterOrEqual(detectedVersion, supportedVersion)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Compares version strings (simple semantic version comparison)
     */
    private boolean isVersionGreaterOrEqual(String version1, String version2) {
        String[] v1Parts = version1.split("\\.");
        String[] v2Parts = version2.split("\\.");
        
        for (int i = 0; i < Math.min(v1Parts.length, v2Parts.length); i++) {
            try {
                int num1 = Integer.parseInt(v1Parts[i]);
                int num2 = Integer.parseInt(v2Parts[i]);
                
                if (num1 > num2) {
                    return true;
                } else if (num1 < num2) {
                    return false;
                }
            } catch (NumberFormatException e) {
                // Handle non-numeric parts gracefully
                continue;
            }
        }
        
        return v1Parts.length >= v2Parts.length;
    }
    
    /**
     * Generates recommendations based on detections
     */
    private List<String> generateRecommendations(List<PlatformDetection> detections) {
        List<String> recommendations = new ArrayList<>();
        
        for (PlatformDetection detection : detections) {
            if (!detection.isJakartaCompatible()) {
                recommendations.add(String.format(
                    "Upgrade %s to version %s+ for Jakarta EE compatibility",
                    detection.platformName(),
                    detection.minJakartaVersion()
                ));
            }
        }
        
        if (detections.isEmpty()) {
            recommendations.add("No application servers detected in the project");
        }
        
        return recommendations;
    }
}
