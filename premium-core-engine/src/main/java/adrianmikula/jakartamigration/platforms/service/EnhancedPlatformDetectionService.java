package adrianmikula.jakartamigration.platforms.service;

import adrianmikula.jakartamigration.platforms.config.PlatformConfigLoader;
import adrianmikula.jakartamigration.platforms.model.EnhancedPlatformScanResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enhanced platform detection service that counts deployment artifacts
 * Extends SimplifiedPlatformDetectionService with WAR/EAR/JAR counting
 */
public class EnhancedPlatformDetectionService {
    
    private static final Logger log = LoggerFactory.getLogger(EnhancedPlatformDetectionService.class);
    private final PlatformConfigLoader configLoader;
    
    // Delegate to SimplifiedPlatformDetectionService for actual platform detection
    private final SimplifiedPlatformDetectionService simpleDetectionService;
    
    public EnhancedPlatformDetectionService() {
        this.configLoader = new PlatformConfigLoader();
        this.simpleDetectionService = new SimplifiedPlatformDetectionService();
    }
    
    public EnhancedPlatformDetectionService(PlatformConfigLoader configLoader) {
        this.configLoader = configLoader;
        this.simpleDetectionService = new SimplifiedPlatformDetectionService();
    }
    
    /**
     * Enhanced scan for application servers with deployment artifact counting
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
        
        return new EnhancedPlatformScanResult(detectedServers, deploymentArtifacts, platformSpecificArtifacts);
    }
    
    /**
     * Unified artifact counting for Maven and Gradle projects
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
     * Count deployment artifacts in project structure
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
     * Count platform-specific dependencies
     */
    private void countPlatformDependencies(String content, Map<String, Integer> platformSpecificArtifacts) {
        // Common platform dependencies
        String[] platformDeps = {
            "spring-boot-starter-web", "spring-boot-starter-tomcat",
            "javax.servlet", "jakarta.servlet", "javax.ejb", "jakarta.ejb",
            "javax.persistence", "jakarta.persistence", "javax.jms", "jakarta.jms",
            "org.springframework.boot", "org.jboss", "org.apache.tomcat",
            "org.eclipse.jetty", "org.glassfish", "com.ibm.websphere"
        };
        
        for (String dep : platformDeps) {
            int count = countOccurrences(content.toLowerCase(), dep.toLowerCase());
            if (count > 0) {
                platformSpecificArtifacts.put(dep, platformSpecificArtifacts.getOrDefault(dep, 0) + count);
            }
        }
    }
    
    /**
     * Count occurrences of a pattern in text
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
    
    // Reuse existing methods from SimplifiedPlatformDetectionService
    private List<String> scanMavenProject(Path projectPath) {
        return simpleDetectionService.scanProject(projectPath);
    }
    
    private List<String> scanGradleProject(Path projectPath) {
        return simpleDetectionService.scanProject(projectPath);
    }
    
    private List<String> scanForInstalledServers(Path projectPath) {
        return simpleDetectionService.scanProject(projectPath);
    }
}
