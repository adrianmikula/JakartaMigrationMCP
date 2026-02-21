package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.ThirdPartyLibProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.ThirdPartyLibUsage;
import adrianmikula.jakartamigration.advancedscanning.service.ThirdPartyLibScanner;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scanner implementation for detecting third-party libraries that haven't been migrated to Jakarta EE.
 * Analyzes Maven pom.xml and Gradle build.gradle files.
 */
@Slf4j
public class ThirdPartyLibScannerImpl implements ThirdPartyLibScanner {

    // Known libraries that still use javax.* namespace
    private static final Map<String, LibraryInfo> KNOWN_LIBRARIES = new HashMap<>();
    
    static {
        // Libraries that are javax-only (no Jakarta version available)
        KNOWN_LIBRARIES.put("javax.xml.bind:jaxb-api", 
            new LibraryInfo("JAXB", "jakarta.xml.bind:jakarta.xml.bind-api", "javax-only"));
        KNOWN_LIBRARIES.put("javax.activation:activation", 
            new LibraryInfo("Java Activation", "jakarta.activation:jakarta.activation-api", "javax-only"));
        KNOWN_LIBRARIES.put("javax.xml.ws:jaxws-api", 
            new LibraryInfo("JAX-WS", "jakarta.xml.ws:jakarta.xml.ws-api", "javax-only"));
        KNOWN_LIBRARIES.put("javax.xml.soap:soap-api", 
            new LibraryInfo("SOAP", "jakarta.xml.soap:jakarta.xml.soap-api", "javax-only"));
        KNOWN_LIBRARIES.put("javax.jws:jsr181-api", 
            new LibraryInfo("JSR-181", "jakarta.jws:jakarta.jws-api", "javax-only"));
        
        // Libraries with partial migration (some versions support Jakarta)
        KNOWN_LIBRARIES.put("org.hibernate:hibernate-core", 
            new LibraryInfo("Hibernate ORM", "org.hibernate.orm:hibernate-core (Jakarta)", "partial-migration"));
        KNOWN_LIBRARIES.put("org.hibernate.validator:hibernate-validator", 
            new LibraryInfo("Hibernate Validator", "Use Jakarta version", "partial-migration"));
        KNOWN_LIBRARIES.put("org.springframework:spring-core", 
            new LibraryInfo("Spring Framework", "Spring 6.x (Jakarta)", "partial-migration"));
        KNOWN_LIBRARIES.put("org.springframework:spring-web", 
            new LibraryInfo("Spring Web", "Spring 6.x (Jakarta)", "partial-migration"));
        
        // Outdated libraries that need updating
        KNOWN_LIBRARIES.put("javax.servlet:javax.servlet-api", 
            new LibraryInfo("Servlet API", "jakarta.servlet:jakarta.servlet-api", "outdated"));
        KNOWN_LIBRARIES.put("javax.validation:validation-api", 
            new LibraryInfo("Bean Validation", "jakarta.validation:jakarta.validation-api", "outdated"));
    }

    // Patterns for parsing Maven pom.xml
    private static final Pattern MAVEN_DEPENDENCY_PATTERN = Pattern.compile(
        "<dependency>\\s*<groupId>([^<]+)</groupId>\\s*<artifactId>([^<]+)</artifactId>\\s*<version>([^<]*)</version>"
    );

    // Patterns for parsing Gradle build.gradle
    private static final Pattern GRADLE_DEPENDENCY_PATTERN = Pattern.compile(
        "(implementation|compile|api|compileOnly|runtimeOnly|testImplementation)\\s*['\"]([^:]+):([^:]+):([^'\"]+)['\"]"
    );

    @Override
    public ThirdPartyLibProjectScanResult scanProject(Path projectPath) {
        log.info("Starting third-party library scan for project: {}", projectPath);
        
        List<ThirdPartyLibUsage> problematicLibs = new ArrayList<>();
        String buildFileFound = "";
        
        try {
            // Look for pom.xml
            Path pomPath = projectPath.resolve("pom.xml");
            if (Files.exists(pomPath)) {
                log.info("Found Maven pom.xml, analyzing dependencies");
                buildFileFound = "pom.xml";
                problematicLibs.addAll(scanMavenPom(pomPath));
            }
            
            // Look for build.gradle
            Path gradlePath = projectPath.resolve("build.gradle");
            if (Files.exists(gradlePath)) {
                log.info("Found Gradle build.gradle, analyzing dependencies");
                buildFileFound = "build.gradle";
                problematicLibs.addAll(scanGradleBuild(gradlePath));
            }
            
            // Also check build.gradle.kts
            Path gradleKtsPath = projectPath.resolve("build.gradle.kts");
            if (Files.exists(gradleKtsPath)) {
                log.info("Found Gradle build.gradle.kts, analyzing dependencies");
                buildFileFound = "build.gradle.kts";
                problematicLibs.addAll(scanGradleBuild(gradleKtsPath));
            }
            
        } catch (Exception e) {
            log.error("Error scanning build files: {}", e.getMessage());
        }
        
        log.info("Third-party library scan complete. Found {} problematic dependencies", problematicLibs.size());
        
        return new ThirdPartyLibProjectScanResult(
            projectPath.toString(),
            problematicLibs,
            buildFileFound
        );
    }

    private List<ThirdPartyLibUsage> scanMavenPom(Path pomPath) {
        List<ThirdPartyLibUsage> usages = new ArrayList<>();
        
        try {
            String content = Files.readString(pomPath);
            Matcher matcher = MAVEN_DEPENDENCY_PATTERN.matcher(content);
            
            while (matcher.find()) {
                String groupId = matcher.group(1).trim();
                String artifactId = matcher.group(2).trim();
                String version = matcher.group(3).trim();
                
                String key = groupId + ":" + artifactId;
                LibraryInfo info = KNOWN_LIBRARIES.get(key);
                
                if (info != null) {
                    usages.add(new ThirdPartyLibUsage(
                        info.libraryName,
                        groupId,
                        artifactId,
                        version,
                        info.issueType,
                        info.suggestedReplacement
                    ));
                }
            }
        } catch (IOException e) {
            log.warn("Error reading pom.xml: {}", e.getMessage());
        }
        
        return usages;
    }

    private List<ThirdPartyLibUsage> scanGradleBuild(Path buildPath) {
        List<ThirdPartyLibUsage> usages = new ArrayList<>();
        
        try {
            String content = Files.readString(buildPath);
            Matcher matcher = GRADLE_DEPENDENCY_PATTERN.matcher(content);
            
            while (matcher.find()) {
                String groupId = matcher.group(2).trim();
                String artifactId = matcher.group(3).trim();
                String version = matcher.group(4).trim();
                
                String key = groupId + ":" + artifactId;
                LibraryInfo info = KNOWN_LIBRARIES.get(key);
                
                if (info != null) {
                    usages.add(new ThirdPartyLibUsage(
                        info.libraryName,
                        groupId,
                        artifactId,
                        version,
                        info.issueType,
                        info.suggestedReplacement
                    ));
                }
            }
        } catch (IOException e) {
            log.warn("Error reading build.gradle: {}", e.getMessage());
        }
        
        return usages;
    }

    private static class LibraryInfo {
        final String libraryName;
        final String suggestedReplacement;
        final String issueType;

        LibraryInfo(String libraryName, String suggestedReplacement, String issueType) {
            this.libraryName = libraryName;
            this.suggestedReplacement = suggestedReplacement;
            this.issueType = issueType;
        }
    }
}
