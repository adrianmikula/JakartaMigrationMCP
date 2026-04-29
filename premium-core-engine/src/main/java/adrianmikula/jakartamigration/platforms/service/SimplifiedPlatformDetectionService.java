package adrianmikula.jakartamigration.platforms.service;

import adrianmikula.jakartamigration.platforms.config.PlatformConfigLoader;
import adrianmikula.jakartamigration.platforms.model.EnhancedPlatformScanResult;
import adrianmikula.jakartamigration.platforms.model.PlatformDetection;
import adrianmikula.jakartamigration.platforms.model.PlatformConfig;
import adrianmikula.jakartamigration.platforms.model.JakartaCompatibility;

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
     * Enhanced scan for application servers with deployment artifact counting.
     * This method provides additional artifact information beyond the simple scan.
     *
     * @param projectPath Path to the project directory
     * @return EnhancedPlatformScanResult with detected platforms and artifact counts
     */
    public EnhancedPlatformScanResult scanProjectWithArtifacts(Path projectPath) {
        log.debug("Starting enhanced platform detection for project: {}", projectPath);

        List<String> detectedServers = new ArrayList<>();
        Map<String, Integer> deploymentArtifacts = new HashMap<>();
        Map<String, Integer> platformSpecificArtifacts = new HashMap<>();

        try {
            if (!Files.exists(projectPath)) {
                log.debug("Project path does not exist: {}", projectPath);
                return new EnhancedPlatformScanResult(detectedServers, deploymentArtifacts, platformSpecificArtifacts);
            }

            // Detect platforms using existing logic
            if (Files.exists(projectPath.resolve("pom.xml"))) {
                log.debug("Found pom.xml, scanning Maven project...");
                List<String> mavenServers = scanMavenProject(projectPath);
                detectedServers.addAll(mavenServers);

                // Count deployment artifacts in Maven project
                countArtifacts(projectPath, "pom.xml", deploymentArtifacts, platformSpecificArtifacts);
            }

            if (Files.exists(projectPath.resolve("build.gradle")) || Files.exists(projectPath.resolve("build.gradle.kts"))) {
                log.debug("Found Gradle build file, scanning Gradle project...");
                List<String> gradleServers = scanGradleProject(projectPath);
                detectedServers.addAll(gradleServers);

                // Count deployment artifacts in Gradle project
                String gradleFile = Files.exists(projectPath.resolve("build.gradle.kts")) ? "build.gradle.kts" : "build.gradle";
                countArtifacts(projectPath, gradleFile, deploymentArtifacts, platformSpecificArtifacts);
            }

            // Scan for installed servers
            List<String> installedServers = scanForInstalledServers(projectPath);
            detectedServers.addAll(installedServers);

            // Count deployment artifacts in project structure
            countProjectArtifacts(projectPath, deploymentArtifacts, platformSpecificArtifacts);

        } catch (Exception e) {
            log.error("Error scanning project: {}", e.getMessage(), e);
        }

        log.debug("Enhanced platform detection complete. Servers: {}, WARs: {}, EARs: {}, JARs: {}",
                 detectedServers.size(),
                 deploymentArtifacts.getOrDefault("war", 0),
                 deploymentArtifacts.getOrDefault("ear", 0),
                 deploymentArtifacts.getOrDefault("jar", 0));

        // Remove duplicates while preserving order
        Set<String> uniqueServers = new LinkedHashSet<>(detectedServers);
        detectedServers.clear();
        detectedServers.addAll(uniqueServers);

        log.debug("After deduplication: {} unique servers: {}", detectedServers.size(), detectedServers);

        // If no platforms detected via dependencies but artifacts exist, infer from artifacts
        List<String> inferredServers = List.of();
        if (detectedServers.isEmpty() && !deploymentArtifacts.isEmpty()) {
            inferredServers = inferPlatformsFromArtifacts(deploymentArtifacts, platformSpecificArtifacts);
            log.debug("Inferred {} platforms from artifacts: {}", inferredServers.size(), inferredServers);
        }

        // Create PlatformDetection objects from config (data collection only, no risk calculation)
        List<PlatformDetection> platformDetails = createPlatformDetectionObjects(detectedServers);

        return new EnhancedPlatformScanResult(detectedServers, inferredServers, platformDetails, deploymentArtifacts, platformSpecificArtifacts);
    }

    /**
     * Unified artifact counting for Maven and Gradle projects.
     */
    private void countArtifacts(Path projectPath, String buildFile, Map<String, Integer> deploymentArtifacts,
                               Map<String, Integer> platformSpecificArtifacts) {
        try {
            Path filePath = projectPath.resolve(buildFile);
            String content = Files.readString(filePath);

            // Count deployment artifacts based on build file type
            if (buildFile.equals("pom.xml")) {
                countMavenPackaging(content, deploymentArtifacts);
            } else {
                countGradlePlugins(content, deploymentArtifacts);
            }

            // Count platform-specific dependencies
            countPlatformDependencies(content, platformSpecificArtifacts);

        } catch (IOException e) {
            log.debug("Error reading {}: {}", buildFile, e.getMessage());
        }
    }

    private void countMavenPackaging(String content, Map<String, Integer> deploymentArtifacts) {
        int warCount = countOccurrences(content, "<packaging>war</packaging>");
        int earCount = countOccurrences(content, "<packaging>ear</packaging>");
        int jarCount = countOccurrences(content, "<packaging>jar</packaging>");

        deploymentArtifacts.merge("war", warCount, Integer::sum);
        deploymentArtifacts.merge("ear", earCount, Integer::sum);
        deploymentArtifacts.merge("jar", jarCount, Integer::sum);
    }

    private void countGradlePlugins(String content, Map<String, Integer> deploymentArtifacts) {
        int warCount = countOccurrences(content, "apply.*plugin.*['\"]war['\"]") +
                      countOccurrences(content, "id\\s*['\"]war['\"]");
        int earCount = countOccurrences(content, "apply.*plugin.*['\"]ear['\"]") +
                      countOccurrences(content, "id\\s*['\"]ear['\"]");

        deploymentArtifacts.merge("war", warCount, Integer::sum);
        deploymentArtifacts.merge("ear", earCount, Integer::sum);
    }

    /**
     * Count deployment artifacts in project structure.
     */
    private void countProjectArtifacts(Path projectPath, Map<String, Integer> deploymentArtifacts,
                                    Map<String, Integer> platformSpecificArtifacts) {
        try {
            Files.walk(projectPath)
                .filter(path -> !Files.isDirectory(path))
                .filter(path -> {
                    String fileName = path.getFileName().toString().toLowerCase();
                    return fileName.endsWith(".war") || fileName.endsWith(".ear") ||
                           fileName.endsWith(".jar") || fileName.equals("web.xml") ||
                           fileName.equals("application.xml") || fileName.equals("ejb-jar.xml");
                })
                .forEach(path -> {
                    String fileName = path.getFileName().toString().toLowerCase();
                    if (fileName.endsWith(".war")) {
                        deploymentArtifacts.put("war", deploymentArtifacts.getOrDefault("war", 0) + 1);
                    } else if (fileName.endsWith(".ear")) {
                        deploymentArtifacts.put("ear", deploymentArtifacts.getOrDefault("ear", 0) + 1);
                    } else if (fileName.endsWith(".jar")) {
                        deploymentArtifacts.put("jar", deploymentArtifacts.getOrDefault("jar", 0) + 1);
                    } else if (fileName.equals("web.xml")) {
                        platformSpecificArtifacts.put("web.xml", platformSpecificArtifacts.getOrDefault("web.xml", 0) + 1);
                    } else if (fileName.equals("application.xml")) {
                        platformSpecificArtifacts.put("application.xml", platformSpecificArtifacts.getOrDefault("application.xml", 0) + 1);
                    } else if (fileName.equals("ejb-jar.xml")) {
                        platformSpecificArtifacts.put("ejb-jar.xml", platformSpecificArtifacts.getOrDefault("ejb-jar.xml", 0) + 1);
                    }
                });

        } catch (IOException e) {
            log.debug("Error walking project structure: {}", e.getMessage());
        }
    }

    /**
     * Count platform-specific dependencies.
     */
    private void countPlatformDependencies(String content, Map<String, Integer> platformSpecificArtifacts) {
        // Common platform dependencies
        String[] platformDeps = {
            "spring-boot-starter-web", "spring-boot-starter-tomcat",
            "javax.servlet", "jakarta.servlet", "javax.ejb", "jakarta.ejb",
            "javax.persistence", "jakarta.persistence", "javax.jms", "jakarta.jms",
            "org.springframework.boot", "org.jboss", "org.apache.tomcat",
            "org.eclipse.jetty", "org.glassfish", "com.ibm.websphere",
            "com.ibm.ws", "net.wasdev.maven.tools.targets", "com.ibm.websphere.appserver"
        };

        for (String dep : platformDeps) {
            int count = countOccurrences(content.toLowerCase(), dep.toLowerCase());
            if (count > 0) {
                platformSpecificArtifacts.put(dep, platformSpecificArtifacts.getOrDefault(dep, 0) + count);
            }
        }
    }

    /**
     * Count occurrences of a pattern in text.
     */
    private int countOccurrences(String text, String pattern) {
        Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        int count = 0;
        while (m.find()) {
            count++;
        }
        return count;
    }

    /**
     * Infers likely application server platforms based on detected deployment artifacts.
     * This is a fallback when no platforms are detected via dependency analysis.
     * Uses conservative inference with platform priority to avoid over-detection.
     */
    private List<String> inferPlatformsFromArtifacts(Map<String, Integer> deploymentArtifacts,
                                                     Map<String, Integer> platformSpecificArtifacts) {
        List<String> inferredServers = new ArrayList<>();

        int warCount = deploymentArtifacts.getOrDefault("war", 0);
        int earCount = deploymentArtifacts.getOrDefault("ear", 0);
        int jarCount = deploymentArtifacts.getOrDefault("jar", 0);

        // First, check for platform-specific indicators that should prevent broad inference
        String specificPlatform = detectSpecificPlatformFromArtifacts(platformSpecificArtifacts);
        if (specificPlatform != null) {
            inferredServers.add(specificPlatform);
            log.debug("Inferred specific platform from artifacts: {}", specificPlatform);
            return inferredServers;
        }

        // Only infer multiple platforms if no specific indicators found and truly ambiguous
        if (isTrulyAmbiguousCase(deploymentArtifacts, platformSpecificArtifacts)) {
            // EAR files indicate full Java EE/Jakarta EE servers - but be conservative
            if (earCount > 0) {
                // Only add the most common EE server instead of all possible ones
                inferredServers.add("wildfly");
                log.debug("Inferred primary EE server from EAR artifacts: wildfly");
            }

            // WAR files indicate servlet containers - but be conservative
            if (warCount > 0 || (earCount == 0 && jarCount > 0)) {
                // Only add the most common servlet container instead of all possible ones
                inferredServers.add("tomcat");
                log.debug("Inferred primary servlet container from WAR artifacts: tomcat");
            }
        }

        return inferredServers;
    }

    /**
     * Detects WebSphere-specific groupIds in pom.xml content.
     */
    private boolean hasWebSphereGroupId(String pomContent) {
        return pomContent.contains("<groupId>com.ibm.websphere") ||
               pomContent.contains("<groupId>com.ibm.ws") ||
               pomContent.contains("com.ibm.websphere.pbw");
    }

    /**
     * Detects specific platform indicators from platform-specific artifacts.
     * Returns the first matching platform or null if no specific indicators found.
     */
    private String detectSpecificPlatformFromArtifacts(Map<String, Integer> platformSpecificArtifacts) {
        // Check for WebSphere-specific indicators
        if (hasWebSphereIndicators(platformSpecificArtifacts)) {
            return "websphere";
        }

        // Check for WebLogic-specific indicators
        if (hasWebLogicIndicators(platformSpecificArtifacts)) {
            return "weblogic";
        }

        // Check for WildFly-specific indicators
        if (hasWildFlyIndicators(platformSpecificArtifacts)) {
            return "wildfly";
        }

        // Check for GlassFish-specific indicators
        if (hasGlassFishIndicators(platformSpecificArtifacts)) {
            return "glassfish";
        }

        // Check for Liberty-specific indicators
        if (hasLibertyIndicators(platformSpecificArtifacts)) {
            return "liberty";
        }

        return null;
    }

    /**
     * Determines if this is a truly ambiguous case that warrants broad inference.
     */
    private boolean isTrulyAmbiguousCase(Map<String, Integer> deploymentArtifacts,
                                       Map<String, Integer> platformSpecificArtifacts) {
        int warCount = deploymentArtifacts.getOrDefault("war", 0);
        int earCount = deploymentArtifacts.getOrDefault("ear", 0);
        int jarCount = deploymentArtifacts.getOrDefault("jar", 0);
        int webXmlCount = platformSpecificArtifacts.getOrDefault("web.xml", 0);

        // Only ambiguous if we have artifacts but no specific platform indicators
        boolean hasArtifacts = warCount > 0 || earCount > 0 || jarCount > 0 || webXmlCount > 0;
        boolean hasSpecificIndicators = detectSpecificPlatformFromArtifacts(platformSpecificArtifacts) != null;

        return hasArtifacts && !hasSpecificIndicators;
    }

    // Platform-specific indicator detection methods
    private boolean hasWebSphereIndicators(Map<String, Integer> platformSpecificArtifacts) {
        return platformSpecificArtifacts.containsKey("com.ibm.websphere") ||
               platformSpecificArtifacts.containsKey("com.ibm.ws") ||
               platformSpecificArtifacts.containsKey("net.wasdev.maven.tools.targets") ||
               platformSpecificArtifacts.containsKey("com.ibm.websphere.appserver");
    }

    private boolean hasWebLogicIndicators(Map<String, Integer> platformSpecificArtifacts) {
        return platformSpecificArtifacts.containsKey("com.oracle.weblogic") ||
               platformSpecificArtifacts.containsKey("weblogic");
    }

    private boolean hasWildFlyIndicators(Map<String, Integer> platformSpecificArtifacts) {
        return platformSpecificArtifacts.containsKey("org.wildfly") ||
               platformSpecificArtifacts.containsKey("org.jboss.as");
    }

    private boolean hasGlassFishIndicators(Map<String, Integer> platformSpecificArtifacts) {
        return platformSpecificArtifacts.containsKey("org.glassfish") ||
               platformSpecificArtifacts.containsKey("org.glassfish.main") ||
               platformSpecificArtifacts.containsKey("org.glassfish.jersey");
    }

    private boolean hasLibertyIndicators(Map<String, Integer> platformSpecificArtifacts) {
        return platformSpecificArtifacts.containsKey("io.openliberty") ||
               platformSpecificArtifacts.containsKey("com.ibm.websphere.appserver");
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
            
            // Check for WebSphere-specific groupIds first (high priority detection)
            if (hasWebSphereGroupId(pomContent)) {
                servers.add("websphere");
                log.debug("✓ Detected WebSphere via groupId pattern");
                // Don't continue scanning for other platforms if WebSphere is detected
                return servers;
            }
            
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
     * Creates PlatformDetection objects from detected platform names using config.
     * This is data collection only - no risk calculation is performed here.
     * Risk scores must be calculated by RiskScoringService using risk-score.yaml.
     *
     * @param detectedPlatforms List of detected platform names
     * @return List of PlatformDetection objects with details from config
     */
    private List<PlatformDetection> createPlatformDetectionObjects(List<String> detectedPlatforms) {
        List<PlatformDetection> platformDetails = new ArrayList<>();
        
        for (String platformName : detectedPlatforms) {
            PlatformConfig config = configLoader.getPlatformConfig(platformName);
            if (config != null) {
                // Extract details from config
                String platformType = platformName;
                String detectedVersion = "unknown"; // Version detection would require additional scanning
                boolean isJakartaCompatible = false;
                String minJakartaVersion = null;
                Map<String, String> requirements = config.requirements() != null 
                    ? new HashMap<>(config.requirements()) 
                    : new HashMap<>();
                
                // Check Jakarta compatibility from config
                if (config.jakartaCompatibility() != null) {
                    isJakartaCompatible = true;
                    minJakartaVersion = config.jakartaCompatibility().minVersion();
                }
                
                PlatformDetection detection = new PlatformDetection(
                    platformType,
                    config.name(),
                    detectedVersion,
                    isJakartaCompatible,
                    minJakartaVersion,
                    requirements
                );
                platformDetails.add(detection);
                log.debug("Created PlatformDetection for {}: compatible={}, minJakarta={}", 
                    platformName, isJakartaCompatible, minJakartaVersion);
            } else {
                log.debug("No config found for platform: {}, creating minimal detection", platformName);
                // Create minimal detection for platforms without config
                PlatformDetection detection = new PlatformDetection(
                    platformName,
                    platformName,
                    "unknown",
                    false,
                    null,
                    new HashMap<>()
                );
                platformDetails.add(detection);
            }
        }
        
        return platformDetails;
    }
    
    /**
     * Checks if content contains the specified artifact.
     * Supports both Gradle format (group:artifact) and Maven XML format (separate groupId/artifactId elements).
     * Also handles parent POM references for Spring Boot detection.
     * 
     * @param content The file content to search (pom.xml or build.gradle)
     * @param artifact The artifact in group:name format
     * @return true if artifact is found in content
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
                
                // Special handling for Spring Boot parent POM detection
                if ("org.springframework.boot".equals(group) && "spring-boot-starter-parent".equals(name)) {
                    // Check for parent POM reference
                    boolean hasParentGroup = lowerContent.contains("<parent>") && 
                                        lowerContent.contains("<groupid>org.springframework.boot</groupid>");
                    boolean hasParentArtifact = lowerContent.contains("<artifactid>spring-boot-starter-parent</artifactid>");
                    if (hasParentGroup && hasParentArtifact) {
                        return true;
                    }
                }
                
                // Also check for simple artifact name match (backward compatibility)
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
