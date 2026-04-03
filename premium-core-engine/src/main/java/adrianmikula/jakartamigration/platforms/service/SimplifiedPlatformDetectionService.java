package adrianmikula.jakartamigration.platforms.service;

import adrianmikula.jakartamigration.platforms.config.PlatformConfigLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simplified platform detection service with YAML configuration
 * Uses YAML for patterns but simplified logic and results
 */
public class SimplifiedPlatformDetectionService {
    
    private final PlatformConfigLoader configLoader;
    
    public SimplifiedPlatformDetectionService() {
        this.configLoader = new PlatformConfigLoader();
    }
    
    /**
     * Simple scan for application servers using YAML patterns
     */
    public List<String> scanProject(Path projectPath) {
        List<String> detectedServers = new ArrayList<>();
        
        try {
            // Get platform configurations from YAML
            Map<String, adrianmikula.jakartamigration.platforms.model.PlatformConfig> configs = configLoader.getAllPlatformConfigs();
            
            // Scan pom.xml if exists
            Path pomFile = projectPath.resolve("pom.xml");
            if (Files.exists(pomFile)) {
                String content = Files.readString(pomFile);
                
                // Check each platform configuration
                for (Map.Entry<String, adrianmikula.jakartamigration.platforms.model.PlatformConfig> entry : configs.entrySet()) {
                    String platformName = entry.getKey();
                    
                    // Skip java platform - we only want application servers
                    if ("java".equals(platformName)) {
                        continue;
                    }
                    
                    adrianmikula.jakartamigration.platforms.model.PlatformConfig config = entry.getValue();
                    
                    if (detectPlatformInContent(content, config)) {
                        detectedServers.add(platformName);
                    }
                }
            }
            
            // Scan build.gradle if exists
            Path gradleFile = projectPath.resolve("build.gradle");
            if (Files.exists(gradleFile)) {
                String content = Files.readString(gradleFile);
                
                // Check each platform configuration
                for (Map.Entry<String, adrianmikula.jakartamigration.platforms.model.PlatformConfig> entry : configs.entrySet()) {
                    String platformName = entry.getKey();
                    
                    // Skip java platform - we only want application servers
                    if ("java".equals(platformName)) {
                        continue;
                    }
                    
                    adrianmikula.jakartamigration.platforms.model.PlatformConfig config = entry.getValue();
                    
                    if (detectPlatformInContent(content, config)) {
                        detectedServers.add(platformName);
                    }
                }
            }
            
        } catch (IOException e) {
            // Log error but don't fail the scan
            System.err.println("Error scanning project: " + e.getMessage());
        }
        
        return detectedServers;
    }
    
    /**
     * Detect platform in content using YAML patterns
     */
    private boolean detectPlatformInContent(String content, adrianmikula.jakartamigration.platforms.model.PlatformConfig config) {
        if (config.patterns() == null) {
            return false;
        }
        
        for (adrianmikula.jakartamigration.platforms.model.DetectionPattern pattern : config.patterns()) {
            try {
                Pattern regex = Pattern.compile(pattern.regex(), Pattern.CASE_INSENSITIVE);
                Matcher matcher = regex.matcher(content);
                
                if (matcher.find()) {
                    return true; // Found at least one matching pattern
                }
            } catch (Exception e) {
                // Skip invalid patterns but continue checking others
                System.err.println("Invalid pattern: " + pattern.regex() + " - " + e.getMessage());
            }
        }
        
        return false;
    }
}
