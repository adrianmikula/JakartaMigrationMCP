package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyUsage;
import adrianmikula.jakartamigration.advancedscanning.service.TransitiveDependencyScanner;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import adrianmikula.jakartamigration.util.ProjectFileSystemScanner;

@Slf4j
public class TransitiveDependencyScannerImpl implements TransitiveDependencyScanner {

    private final ProjectFileSystemScanner fileScanner = new ProjectFileSystemScanner();

    // Known artifacts that contain javax packages and need Jakarta equivalents
    private static final Map<String, TransitiveDependencyInfo> KNOWN_JAVAX_DEPENDENCIES = new HashMap<>();

    static {
        // JAXB
        KNOWN_JAVAX_DEPENDENCIES.put("javax.xml.bind:jaxb-api",
                new TransitiveDependencyInfo("jakarta.xml.bind:jakarta-xml-bind-api", "high",
                        "Replace with Jakarta XML Binding"));
        KNOWN_JAVAX_DEPENDENCIES.put("org.glassfish.jaxb:jaxb-runtime",
                new TransitiveDependencyInfo("org.glassfish.jaxb:jaxb-runtime", "high", "Use Jakarta XML Binding"));

        // Activation
        KNOWN_JAVAX_DEPENDENCIES.put("javax.activation:activation",
                new TransitiveDependencyInfo("jakarta.activation:jakarta-activation-api", "high",
                        "Replace with Jakarta Activation"));

        // SOAP
        KNOWN_JAVAX_DEPENDENCIES.put("javax.xml.soap:saaj-api",
                new TransitiveDependencyInfo("jakarta.xml.soap:jakarta-soap-impl", "medium",
                        "Replace with Jakarta SOAP"));

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
                new TransitiveDependencyInfo("com.sun.xml.messaging.saaj:saaj-impl", "medium",
                        "Use Jakarta SOAP with Implementation"));

        // Spring specific
        KNOWN_JAVAX_DEPENDENCIES.put("org.springframework:spring-core",
                new TransitiveDependencyInfo("Check for javax imports in usage", "low",
                        "Review usage for javax packages"));

        // Jackson
        KNOWN_JAVAX_DEPENDENCIES.put("com.fasterxml.jackson.core:jackson-databind",
                new TransitiveDependencyInfo("Check for javax.xml imports", "low",
                        "Review usage for javax.xml packages"));
    }

    private record TransitiveDependencyInfo(String jakartaEquivalent, String severity, String recommendation) {
    }

    // Parallelism configuration - can be overridden via system property
    private static final int MAX_PARALLELISM = Integer.parseInt(
            System.getProperty("advanced.scan.parallelism", "4"));

    // Memory threshold for sequential fallback (100MB)
    private static final long MEMORY_THRESHOLD_BYTES = 100 * 1024 * 1024;

    // Patterns for Maven pom.xml
    private static final Pattern MAVEN_DEPENDENCY_PATTERN = Pattern.compile(
            "<dependency>\\s*<groupId>([^<]+)</groupId>\\s*<artifactId>([^<]+)</artifactId>\\s*<version>([^<]*)</version>",
            Pattern.MULTILINE | Pattern.DOTALL);

    // Patterns for Gradle
    private static final Pattern GRADLE_DEPENDENCY_PATTERN = Pattern.compile(
            "['\"]([^':]+):([^':]+):([^'\"]+)['\"]",
            Pattern.MULTILINE);

    @Override
    public TransitiveDependencyProjectScanResult scanProject(Path projectPath) {
        if (projectPath == null || !Files.exists(projectPath) || !Files.isDirectory(projectPath)) {
            return TransitiveDependencyProjectScanResult.empty();
        }

        try {
            List<Path> buildFiles = discoverBuildFiles(projectPath);
            if (buildFiles.isEmpty())
                return TransitiveDependencyProjectScanResult.empty();

            // Check available memory and determine parallelism
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long availableMemory = maxMemory - usedMemory;

            AtomicInteger totalScanned = new AtomicInteger(0);
            List<TransitiveDependencyScanResult> results;

            // Use sequential processing if low on memory, otherwise use bounded parallelism
            if (availableMemory < MEMORY_THRESHOLD_BYTES) {
                log.info("Low memory detected ({} MB available), using sequential processing for Transitive Dependency scan",
                        availableMemory / (1024 * 1024));
                results = buildFiles.stream()
                        .map(file -> scanFileWithTracking(file, totalScanned))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            } else {
                // Use bounded parallelism with custom ForkJoinPool
                int parallelism = Math.min(MAX_PARALLELISM, buildFiles.size());
                log.debug("Using parallel processing with parallelism={} for Transitive Dependency scan", parallelism);

                ForkJoinPool customPool = new ForkJoinPool(parallelism);
                try {
                    results = customPool.submit(() ->
                            buildFiles.parallelStream()
                                    .map(file -> scanFileWithTracking(file, totalScanned))
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toList())
                    ).get();
                } catch (Exception e) {
                    log.warn("Parallel scan failed for Transitive Dependency, falling back to sequential: {}", e.getMessage());
                    results = buildFiles.stream()
                            .map(file -> scanFileWithTracking(file, totalScanned))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                } finally {
                    customPool.shutdown();
                }
            }

            int totalUsages = results.stream().mapToInt(r -> r.getUsages().size()).sum();

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
        return fileScanner.findFiles(projectPath, path -> {
            String name = path.getFileName().toString().toLowerCase();
            return name.equals("pom.xml") || name.endsWith(".gradle") || name.endsWith(".gradle.kts");
        });
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
                usages.add(new TransitiveDependencyUsage(artifactId, groupId, null, key, info.severity(),
                        info.recommendation()));
            }

            // Also check for any javax. in groupId or artifactId
            if (groupId.contains("javax") || artifactId.contains("javax")) {
                usages.add(new TransitiveDependencyUsage(artifactId, groupId, null, key, "high",
                        "Replace javax with jakarta"));
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
                usages.add(new TransitiveDependencyUsage(artifactId, groupId, version, key, info.severity(),
                        info.recommendation()));
            }

            // Also check for any javax. in groupId or artifactId
            if (groupId.contains("javax") || artifactId.contains("javax")) {
                usages.add(new TransitiveDependencyUsage(artifactId, groupId, version, key, "high",
                        "Replace javax with jakarta"));
            }
        }

        return usages;
    }

    /**
     * Scans a single file with tracking for parallel processing.
     */
    private TransitiveDependencyScanResult scanFileWithTracking(Path filePath, AtomicInteger totalScanned) {
        totalScanned.incrementAndGet();
        TransitiveDependencyScanResult result = scanFile(filePath);
        return result.hasJavaxUsage() ? result : null;
    }
}
