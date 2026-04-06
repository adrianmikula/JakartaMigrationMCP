package adrianmikula.jakartamigration.platforms.service;

import adrianmikula.jakartamigration.platforms.config.PlatformConfigLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simplified platform detection service with YAML configuration
 * Uses YAML for patterns but simplified logic and results
 */
public class SimplifiedPlatformDetectionService {
    
    private static final Logger log = LoggerFactory.getLogger(SimplifiedPlatformDetectionService.class);
    private final PlatformConfigLoader configLoader;
    
    public SimplifiedPlatformDetectionService() {
        this.configLoader = new PlatformConfigLoader();
    }
    
    /**
     * Simple scan for application servers using YAML patterns and common artifacts
     */
    public List<String> scanProject(Path projectPath) {
        log.debug("Starting platform detection for project: {}", projectPath);
        List<String> detectedServers = new ArrayList<>();
        
        try {
            // Check if project path exists and is valid
            if (!Files.exists(projectPath)) {
                log.debug("Project path does not exist: {}", projectPath);
                return detectedServers;
            }
            
            log.debug("Project path exists, checking for build files...");
            
            // Simple file-based detection - check for key files
            if (Files.exists(projectPath.resolve("pom.xml"))) {
                log.debug("Found pom.xml, scanning Maven project...");
                List<String> mavenServers = scanMavenProject(projectPath);
                log.debug("Maven scan found {} servers: {}", mavenServers.size(), mavenServers);
                detectedServers.addAll(mavenServers);
            } else {
                log.debug("No pom.xml found at: {}", projectPath.resolve("pom.xml"));
            }
            
            if (Files.exists(projectPath.resolve("build.gradle"))) {
                log.debug("Found build.gradle, scanning Gradle project...");
                List<String> gradleServers = scanGradleProject(projectPath);
                log.debug("Gradle scan found {} servers: {}", gradleServers.size(), gradleServers);
                detectedServers.addAll(gradleServers);
            } else {
                log.debug("No build.gradle found at: {}", projectPath.resolve("build.gradle"));
            }
            
            // Simple server installation detection
            log.debug("Scanning for installed servers...");
            List<String> installedServers = scanForInstalledServers(projectPath);
            log.debug("Installed servers scan found {} servers: {}", installedServers.size(), installedServers);
            detectedServers.addAll(installedServers);
            
        } catch (Exception e) {
            log.error("Error scanning project: {}", e.getMessage(), e);
        }
        
        log.debug("Platform detection complete. Total servers found: {}", detectedServers.size());
        
        // Remove duplicates while preserving order
        Set<String> uniqueServers = new LinkedHashSet<>(detectedServers);
        detectedServers.clear();
        detectedServers.addAll(uniqueServers);
        
        log.debug("After deduplication: {} unique servers: {}", detectedServers.size(), detectedServers);
        return detectedServers;
    }
    
    /**
     * Scan Maven project for server dependencies with variable resolution
     */
    private List<String> scanMavenProject(Path projectPath) {
        log.debug("Starting Maven project scan...");
        List<String> servers = new ArrayList<>();
        Map<String, adrianmikula.jakartamigration.platforms.model.PlatformConfig> configs = configLoader.getAllPlatformConfigs();
        log.debug("Loaded {} platform configurations from YAML", configs.size());
        
        try {
            Path pomPath = projectPath.resolve("pom.xml");
            String pomContent = Files.readString(pomPath);
            log.debug("Read pom.xml content ({} characters)", pomContent.length());
            
            // First, extract variables from gradle.properties or libs.versions.toml
            Map<String, String> variables = extractGradleVariables(projectPath);
            log.debug("Extracted {} variables from properties files", variables.size());
            
            // Check each platform configuration using common artifacts
            for (Map.Entry<String, adrianmikula.jakartamigration.platforms.model.PlatformConfig> entry : configs.entrySet()) {
                String platformName = entry.getKey();
                adrianmikula.jakartamigration.platforms.model.PlatformConfig config = entry.getValue();
                
                log.debug("Checking platform: {}", platformName);
                
                // Skip java platform - we only want application servers
                if ("java".equals(platformName)) {
                    log.debug("Skipping java platform - only looking for application servers");
                    continue;
                }
                
                // Check common artifacts first (faster than regex)
                // Supports both Gradle format (group:artifact) and Maven XML format
                if (config.commonArtifacts() != null) {
                    log.debug("Platform {} has {} common artifacts to check", platformName, config.commonArtifacts().size());
                    for (String artifact : config.commonArtifacts()) {
                        if (matchesArtifact(pomContent, artifact)) {
                            servers.add(platformName);
                            log.debug("✓ Detected {} via common artifact: {}", platformName, artifact);
                            break; // Found this platform, move to next
                        }
                    }
                } else {
                    log.debug("Platform {} has no common artifacts defined", platformName);
                }
                
                // Fallback to regex patterns if no common artifacts found
                if (!servers.contains(platformName) && config.patterns() != null) {
                    log.debug("Checking {} regex patterns for platform {}", config.patterns().size(), platformName);
                    for (adrianmikula.jakartamigration.platforms.model.DetectionPattern pattern : config.patterns()) {
                        try {
                            Pattern regex = Pattern.compile(pattern.regex(), Pattern.CASE_INSENSITIVE);
                            if (regex.matcher(pomContent).find()) {
                                servers.add(platformName);
                                log.debug("✓ Detected {} via regex pattern: {}", platformName, pattern.regex());
                                break; // Found this platform, move to next
                            }
                        } catch (Exception e) {
                            log.debug("Invalid regex pattern for {}: {}", platformName, pattern.regex());
                        }
                    }
                } else if (servers.contains(platformName)) {
                    log.debug("Platform {} already detected via common artifacts, skipping regex patterns", platformName);
                } else {
                    log.debug("Platform {} has no regex patterns defined", platformName);
                }
            }
            
        } catch (Exception e) {
            log.error("Error scanning Maven project: {}", e.getMessage(), e);
        }
        
        log.debug("Maven scan complete. Found {} servers: {}", servers.size(), servers);
        return servers;
    }
    
    /**
     * Scan Gradle project for server dependencies with variable resolution
     */
    private List<String> scanGradleProject(Path projectPath) {
        log.debug("Starting Gradle project scan...");
        List<String> servers = new ArrayList<>();
        Map<String, adrianmikula.jakartamigration.platforms.model.PlatformConfig> configs = configLoader.getAllPlatformConfigs();
        log.debug("Loaded {} platform configurations from YAML", configs.size());
        
        try {
            Path gradlePath = projectPath.resolve("build.gradle");
            String gradleContent = Files.readString(gradlePath);
            log.debug("Read build.gradle content ({} characters)", gradleContent.length());
            
            // First, extract variables from gradle.properties or libs.versions.toml
            Map<String, String> variables = extractGradleVariables(projectPath);
            log.debug("Extracted {} variables from properties files", variables.size());
            
            // Check each platform configuration using common artifacts
            for (Map.Entry<String, adrianmikula.jakartamigration.platforms.model.PlatformConfig> entry : configs.entrySet()) {
                String platformName = entry.getKey();
                adrianmikula.jakartamigration.platforms.model.PlatformConfig config = entry.getValue();
                
                log.debug("Checking platform: {}", platformName);
                
                // Skip java platform - we only want application servers
                if ("java".equals(platformName)) {
                    log.debug("Skipping java platform - only looking for application servers");
                    continue;
                }
                
                // Check common artifacts first (faster than regex)
                // Supports both Gradle format (group:artifact) and Maven XML format
                if (config.commonArtifacts() != null) {
                    log.debug("Platform {} has {} common artifacts to check", platformName, config.commonArtifacts().size());
                    for (String artifact : config.commonArtifacts()) {
                        if (matchesArtifact(gradleContent, artifact)) {
                            servers.add(platformName);
                            log.debug("✓ Detected {} via common artifact: {}", platformName, artifact);
                            break; // Found this platform, move to next
                        }
                    }
                } else {
                    log.debug("Platform {} has no common artifacts defined", platformName);
                }
                
                // Fallback to regex patterns if no common artifacts found
                if (!servers.contains(platformName) && config.patterns() != null) {
                    log.debug("Checking {} regex patterns for platform {}", config.patterns().size(), platformName);
                    for (adrianmikula.jakartamigration.platforms.model.DetectionPattern pattern : config.patterns()) {
                        try {
                            Pattern regex = Pattern.compile(pattern.regex(), Pattern.CASE_INSENSITIVE);
                            if (regex.matcher(gradleContent).find()) {
                                servers.add(platformName);
                                log.debug("✓ Detected {} via regex pattern: {}", platformName, pattern.regex());
                                break; // Found this platform, move to next
                            }
                        } catch (Exception e) {
                            log.debug("Invalid regex pattern for {}: {}", platformName, pattern.regex());
                        }
                    }
                } else if (servers.contains(platformName)) {
                    log.debug("Platform {} already detected via common artifacts, skipping regex patterns", platformName);
                } else {
                    log.debug("Platform {} has no regex patterns defined", platformName);
                }
            }
            
        } catch (Exception e) {
            log.error("Error scanning Gradle project: {}", e.getMessage(), e);
        }
        
        log.debug("Gradle scan complete. Found {} servers: {}", servers.size(), servers);
        return servers;
    }
    
    /**
     * Extract variables from gradle.properties and libs.versions.toml
     */
    private Map<String, String> extractGradleVariables(Path projectPath) {
        Map<String, String> variables = new HashMap<>();
        
        try {
            // Check for gradle.properties
            Path gradleProps = projectPath.resolve("gradle.properties");
            if (Files.exists(gradleProps)) {
                String content = Files.readString(gradleProps);
                content.lines()
                        .filter(line -> line.contains("="))
                        .forEach(line -> {
                            String[] parts = line.split("=", 2);
                            if (parts.length == 2) {
                                variables.put(parts[0].trim(), parts[1].trim());
                                log.debug("Found gradle variable: {} = {}", parts[0].trim(), parts[1].trim());
                            }
                        });
            }
            
            // Check for libs.versions.toml (common in Gradle projects)
            Path libsVersions = projectPath.resolve("libs.versions.toml");
            if (Files.exists(libsVersions)) {
                String content = Files.readString(libsVersions);
                content.lines()
                        .filter(line -> line.contains("="))
                        .forEach(line -> {
                            // Simple TOML parsing for key=value pairs
                            String[] parts = line.split("=", 2);
                            if (parts.length == 2) {
                                variables.put(parts[0].trim(), parts[1].trim());
                                log.debug("Found libs.versions.toml variable: {} = {}", parts[0].trim(), parts[1].trim());
                            }
                        });
            }
            
        } catch (IOException e) {
            log.debug("Error extracting variables: {}", e.getMessage(), e);
        }
        
        return variables;
    }
    
    /**
     * Scan for variable-based artifact coordinates (e.g., "${tomcatVersion}")
     */
    private List<String> scanForVariableArtifacts(String gradleContent, Map<String, String> variables, 
            String platformName, adrianmikula.jakartamigration.platforms.model.PlatformConfig config) {
        List<String> servers = new ArrayList<>();
        
        // Common variable patterns for different platforms
        Map<String, String[]> platformVariables = Map.of(
                "tomcat", new String[]{"tomcatVersion", "tomcatVersion", "catalinaVersion"},
                "wildfly", new String[]{"wildflyVersion", "wildflyVersion"},
                "jetty", new String[]{"jettyVersion", "jettyVersion"},
                "liberty", new String[]{"libertyVersion", "libertyVersion"},
                "tomee", new String[]{"tomeeVersion", "tomeeVersion"},
                "payara", new String[]{"payaraVersion", "payaraVersion"},
                "jbosseap", new String[]{"jbossVersion", "jbossVersion"}
        );
        
        String[] platformVarNames = platformVariables.get(platformName);
        if (platformVarNames != null) {
            for (String varName : platformVarNames) {
                String varPattern = "${" + varName + "}";
                if (gradleContent.contains(varPattern)) {
                    String actualValue = variables.get(varName);
                    if (actualValue != null) {
                        log.debug("Found variable-based {} artifact: {} = {}", platformName, varName, actualValue);
                        servers.add(platformName);
                        break; // Found variable-based detection
                    }
                }
            }
        }
        
        return servers;
    }
    
    /**
     * Simple scan for installed application servers - search anywhere in project
     */
    private List<String> scanForInstalledServers(Path projectPath) {
        log.debug("Starting installed servers scan...");
        List<String> servers = new ArrayList<>();
        Map<String, adrianmikula.jakartamigration.platforms.model.PlatformConfig> configs = configLoader.getAllPlatformConfigs();
        log.debug("Loaded {} platform configurations from YAML", configs.size());
        
        try {
            // Search for key server files anywhere in project (not just specific folders)
            String[] serverFiles = {
                "catalina.bat", "standalone.sh", "start.ini", "catalina.jar",
                "tomee-catalina.jar", "asadmin", "payara.jar"
            };
            log.debug("Searching for {} server file patterns", serverFiles.length);
            
            // Use Files.walk() to search entire project tree efficiently
            try (var paths = Files.walk(projectPath)) {
                paths.filter(Files::isRegularFile)
                     .forEach(filePath -> {
                         String fileName = filePath.getFileName().toString();
                         
                         // Check if this is a server file we're looking for
                         for (String serverFile : serverFiles) {
                             if (fileName.contains(serverFile)) {
                                 // Found a server file, now determine which platform
                                 for (Map.Entry<String, adrianmikula.jakartamigration.platforms.model.PlatformConfig> entry : configs.entrySet()) {
                                     String platformName = entry.getKey();
                                     adrianmikula.jakartamigration.platforms.model.PlatformConfig config = entry.getValue();
                                     
                                     // Skip java platform
                                     if ("java".equals(platformName)) {
                                         continue;
                                     }
                                     
                                     // Check if this platform uses this file and extract version
                                     if (config.patterns() != null) {
                                         for (adrianmikula.jakartamigration.platforms.model.DetectionPattern pattern : config.patterns()) {
                                             if (fileName.equals(pattern.file())) {
                                                 try {
                                                     String content = Files.readString(filePath);
                                                     Pattern regex = Pattern.compile(pattern.regex(), Pattern.CASE_INSENSITIVE);
                                                     Matcher matcher = regex.matcher(content);
                                                     
                                                     if (matcher.find()) {
                                                         String version = matcher.group(pattern.versionGroup());
                                                         if (!servers.contains(platformName)) {
                                                             servers.add(platformName);
                                                             log.debug("Detected {} via file {} with version: {}", 
                                                                     platformName, fileName, version);
                                                         }
                                                         break; // Found version for this platform
                                                     }
                                                 } catch (IOException e) {
                                                     log.debug("Error reading file {}: {}", filePath, e.getMessage());
                                                 } catch (Exception e) {
                                                     log.debug("Invalid pattern: {}", pattern.regex(), e);
                                                 }
                                             }
                                         }
                                     }
                                 }
                             }
                         }
                     });
            }
        } catch (IOException e) {
            log.debug("Error walking project tree: {}", e.getMessage(), e);
        }
        
        log.debug("Installed servers scan complete. Found {} servers: {}", servers.size(), servers);
        return servers;
    }
    
    /**
     * Checks if the content contains the specified artifact.
     * Supports both Gradle format (group:artifact) and Maven XML format (separate groupId/artifactId elements).
     * 
     * @param content The file content to search (pom.xml or build.gradle)
     * @param artifact The artifact in group:name format
     * @return true if the artifact is found in the content
     */
    private boolean matchesArtifact(String content, String artifact) {
        String lowerContent = content.toLowerCase();
        String lowerArtifact = artifact.toLowerCase();
        
        // First check for Gradle format: group:artifact
        if (lowerContent.contains(lowerArtifact)) {
            return true;
        }
        
        // For Maven XML format, check if both group and artifact are present
        if (artifact.contains(":")) {
            String[] parts = artifact.split(":");
            if (parts.length == 2) {
                String group = parts[0].toLowerCase();
                String name = parts[1].toLowerCase();
                
                // Check for Maven XML format with case-insensitive tag matching
                // Pattern: <groupId>group</groupId> anywhere in content
                boolean hasGroup = lowerContent.contains("<groupid>" + group + "</groupid>");
                boolean hasArtifact = lowerContent.contains("<artifactid>" + name + "</artifactid>");
                
                if (hasGroup && hasArtifact) {
                    return true;
                }
                
                // Also check for simple artifact name match (backward compatibility)
                // This handles cases where only artifactId is specified without group
                if (lowerContent.contains(name)) {
                    return true;
                }
            }
        } else {
            // No colon - simple artifact name check (backward compatibility)
            if (lowerContent.contains(lowerArtifact)) {
                return true;
            }
        }
        
        return false;
    }
}
