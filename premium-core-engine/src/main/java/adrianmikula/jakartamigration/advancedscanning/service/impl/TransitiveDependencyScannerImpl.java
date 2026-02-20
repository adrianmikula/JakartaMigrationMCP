package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyUsage;
import adrianmikula.jakartamigration.advancedscanning.service.TransitiveDependencyScanner;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class TransitiveDependencyScannerImpl implements TransitiveDependencyScanner {

    // Known artifacts that contain javax packages and need Jakarta equivalents
    private static final Map<String, TransitiveDependencyInfo> KNOWN_JAVAX_DEPENDENCIES = new HashMap<>();

    static {
        // JAXB
        KNOWN_JAVAX_DEPENDENCIES.put("javax.xml.bind:jaxb-api", 
            new TransitiveDependencyInfo("jakarta.xml.bind:jakarta-xml-bind-api", "high", "Replace with Jakarta XML Binding"));
        KNOWN_JAVAX_DEPENDENCIES.put("org.glassfish.jaxb:jaxb-runtime", 
            new TransitiveDependencyInfo("org.glassfish.jaxb:jaxb-runtime", "high", "Use Jakarta XML Binding"));
        
        // Activation
        KNOWN_JAVAX_DEPENDENCIES.put("javax.activation:activation", 
            new TransitiveDependencyInfo("jakarta.activation:jakarta-activation-api", "high", "Replace with Jakarta Activation"));
        
        // SOAP
        KNOWN_JAVAX_DEPENDENCIES.put("javax.xml.soap:saaj-api", 
            new TransitiveDependencyInfo("jakarta.xml.soap:jakarta-soap-impl", "medium", "Replace with Jakarta SOAP"));
        
        // JMS
        KNOWN_JAVAX_DEPENDENCIES.put("javax.jms:javax.jms-api", 
            new TransitiveDependencyInfo("jakarta.jms:jakarta.jms-api", "high", "Replace with Jakarta Messaging"));
        
        // JAXB Runtime
        KNOWN_JAVAX_DEPENDENCIES.put("com.sun.xml.bind:jaxb-impl", 
            new TransitiveDependencyInfo("org.glassfish.jaxb:jaxb-runtime", "high", "Use Jakarta JAXB Runtime"));
        KNOWN_JAVAX_DEPENDENCIES.put("com.sun.xml.bind:jaxb-core", 
            new TransitiveDependencyInfo("org.glassfish.jaxb:jaxb-runtime", "high", "Use Jakarta JAXB Runtime"));
        
        // SAAJ
        KNOWN_JAVAX_DEPENDENCIES.put("com.sun.xml.messaging.saaj:saaj-impl", 
            new TransitiveDependencyInfo("com.sun.xml.messaging.saaj:saaj-impl", "medium", "Use Jakarta SOAP with Implementation"));
        
        // Spring specific
        KNOWN_JAVAX_DEPENDENCIES.put("org.springframework:spring-core", 
            new TransitiveDependencyInfo("Check for javax imports in usage", "low", "Review usage for javax packages"));
        
        // Jackson
        KNOWN_JAVAX_DEPENDENCIES.put("com.fasterxml.jackson.core:jackson-databind", 
            new TransitiveDependencyInfo("Check for javax.xml imports", "low", "Review usage for javax.xml packages"));
    }

    private record TransitiveDependencyInfo(String jakartaEquivalent, String severity, String recommendation) {}

    // Patterns for Maven pom.xml
    private static final Pattern MAVEN_DEPENDENCY_PATTERN = Pattern.compile(
        "<dependency>\\s*<groupId>([^<]+)</groupId>\\s*<artifactId>([^<]+)</artifactId>\\s*<version>([^<]*)</version>",
        Pattern.MULTILINE | Pattern.DOTALL
    );

    // Patterns for Gradle
    private static final Pattern GRADLE_DEPENDENCY_PATTERN = Pattern.compile(
        "['\"]([^':]+):([^':]+):([^'\"]+)['\"]",
        Pattern.MULTILINE
    );

    @Override
    public TransitiveDependencyProjectScanResult scanProject(Path projectPath) {
        if (projectPath == null || !Files.exists(projectPath) || !Files.isDirectory(projectPath)) {
            return TransitiveDependencyProjectScanResult.empty();
        }

        try {
            List<Path> buildFiles = discoverBuildFiles(projectPath);
            if (buildFiles.isEmpty()) return TransitiveDependencyProjectScanResult.empty();

            AtomicInteger totalScanned = new AtomicInteger(0);
            List<TransitiveDependencyScanResult> results = buildFiles.parallelStream()
                .map(file -> {
                    totalScanned.incrementAndGet();
                    TransitiveDependencyScanResult result = scanFile(file);
                    return result.hasJavaxUsage() ? result : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            int totalUsages = results.stream().mapToInt(r -> r.usages().size()).sum();

            return new TransitiveDependencyProjectScanResult(results, totalScanned.get(), results.size(), totalUsages);
        } catch (Exception e) {
            log.error("Error scanning project for transitive dependencies", e);
            return TransitiveDependencyProjectScanResult.empty();
        }
    }

    @Override
    public TransitiveDependencyScanResult scanFile(Path filePath) {
        if (filePath == null || !Files.exists(filePath)) {
            return TransitiveDependencyScanResult.empty(filePath);
        }

        try {
            String content = Files.readString(filePath);
            String fileName = filePath.getFileName().toString().toLowerCase();
            
            String buildFileType = null;
            List<TransitiveDependencyUsage> usages = new ArrayList<>();
            
            if (fileName.equals("pom.xml")) {
                buildFileType = "Maven";
                usages = parseMavenDependencies(content);
            } else if (fileName.endsWith(".gradle") || fileName.endsWith(".gradle.kts")) {
                buildFileType = "Gradle";
                usages = parseGradleDependencies(content);
            }

            return new TransitiveDependencyScanResult(filePath, usages, buildFileType);
        } catch (Exception e) {
            return TransitiveDependencyScanResult.empty(filePath);
        }
    }

    private List<Path> discoverBuildFiles(Path projectPath) {
        try (Stream<Path> paths = Files.walk(projectPath)) {
            return paths.filter(Files::isRegularFile)
                .filter(p -> {
                    String name = p.getFileName().toString().toLowerCase();
                    return name.equals("pom.xml") || name.endsWith(".gradle") || name.endsWith(".gradle.kts");
                })
                .filter(this::shouldScanFile)
                .collect(Collectors.toList());
        } catch (IOException e) {
            return List.of();
        }
    }

    private boolean shouldScanFile(Path file) {
        String path = file.toString().replace('\\', '/');
        return !path.contains("/target/") && !path.contains("/build/") && !path.contains("/.git/");
    }

    private List<TransitiveDependencyUsage> parseMavenDependencies(String content) {
        List<TransitiveDependencyUsage> usages = new ArrayList<>();
        
        Matcher matcher = MAVEN_DEPENDENCY_PATTERN.matcher(content);
        while (matcher.find()) {
            String groupId = matcher.group(1).trim();
            String artifactId = matcher.group(2).trim();
            
            String key = groupId + ":" + artifactId;
            TransitiveDependencyInfo info = KNOWN_JAVAX_DEPENDENCIES.get(key);
            
            if (info != null) {
                usages.add(new TransitiveDependencyUsage(artifactId, groupId, null, key, info.severity(), info.recommendation()));
            }
            
            // Also check for any javax. in groupId or artifactId
            if (groupId.contains("javax") || artifactId.contains("javax")) {
                usages.add(new TransitiveDependencyUsage(artifactId, groupId, null, key, "high", "Replace javax with jakarta"));
            }
        }
        
        return usages;
    }

    private List<TransitiveDependencyUsage> parseGradleDependencies(String content) {
        List<TransitiveDependencyUsage> usages = new ArrayList<>();
        
        Matcher matcher = GRADLE_DEPENDENCY_PATTERN.matcher(content);
        while (matcher.find()) {
            String groupId = matcher.group(1).trim();
            String artifactId = matcher.group(2).trim();
            String version = matcher.group(3).trim();
            
            String key = groupId + ":" + artifactId;
            TransitiveDependencyInfo info = KNOWN_JAVAX_DEPENDENCIES.get(key);
            
            if (info != null) {
                usages.add(new TransitiveDependencyUsage(artifactId, groupId, version, key, info.severity(), info.recommendation()));
            }
            
            // Also check for any javax. in groupId or artifactId
            if (groupId.contains("javax") || artifactId.contains("javax")) {
                usages.add(new TransitiveDependencyUsage(artifactId, groupId, version, key, "high", "Replace javax with jakarta"));
            }
        }
        
        return usages;
    }
}
