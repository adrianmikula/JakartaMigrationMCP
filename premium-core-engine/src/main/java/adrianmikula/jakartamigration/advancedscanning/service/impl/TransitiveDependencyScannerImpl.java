package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.DependencyTreeResult;
import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyUsage;
import adrianmikula.jakartamigration.advancedscanning.service.DependencyDeduplicationService;
import adrianmikula.jakartamigration.advancedscanning.service.DependencyTreeCommandExecutor;
import adrianmikula.jakartamigration.advancedscanning.service.TransitiveDependencyScanner;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import adrianmikula.jakartamigration.util.ProjectFileSystemScanner;

@Slf4j
public class TransitiveDependencyScannerImpl implements TransitiveDependencyScanner {

    private final ProjectFileSystemScanner fileScanner = new ProjectFileSystemScanner();
    private final DependencyTreeCommandExecutor commandExecutor;
    private final DependencyDeduplicationService deduplicationService;

    // Scopes to include in transitive dependency scanning
    private static final Set<String> MAVEN_SCOPES = Set.of("compile", "provided", "runtime", "test");
    private static final Set<String> GRADLE_SCOPES = Set.of("compileClasspath", "runtimeClasspath", "testCompileClasspath");

    public TransitiveDependencyScannerImpl() {
        this(new DependencyTreeCommandExecutorImpl(), new DependencyDeduplicationServiceImpl());
    }

    public TransitiveDependencyScannerImpl(DependencyTreeCommandExecutor commandExecutor,
                                          DependencyDeduplicationService deduplicationService) {
        this.commandExecutor = commandExecutor;
        this.deduplicationService = deduplicationService;
    }

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

    private static final int MAX_PARALLELISM = Integer.parseInt(
            System.getProperty("advanced.scan.parallelism", "4"));

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

            AtomicInteger totalScanned = new AtomicInteger(0);
            int parallelism = Math.min(MAX_PARALLELISM, buildFiles.size());

            List<TransitiveDependencyScanResult> results = buildFiles.parallelStream()
                    .map(file -> scanFileWithTracking(file, totalScanned))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            return new TransitiveDependencyProjectScanResult(results, totalScanned.get(), results.size(),
                    results.stream().mapToInt(r -> r.getUsages().size()).sum());
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

        String fileName = filePath.getFileName().toString().toLowerCase();
        boolean isMaven = fileName.equals("pom.xml");
        boolean isGradle = fileName.endsWith(".gradle") || fileName.endsWith(".gradle.kts");

        if (!isMaven && !isGradle) return TransitiveDependencyScanResult.empty(filePath);

        try {
            var future = isMaven
                ? commandExecutor.executeMavenDependencyTreeAsync(filePath, MAVEN_SCOPES)
                : commandExecutor.executeGradleDependenciesAsync(filePath, GRADLE_SCOPES);

            var result = future.get(DependencyTreeCommandExecutor.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!result.isSuccess() || result.getDependencies().isEmpty()) {
                throw new RuntimeException("Command failed or returned no dependencies");
            }
            return convertTreeResult(filePath, isMaven ? "Maven" : "Gradle", result);
        } catch (Exception e) {
            log.warn("Async scanning failed for {}, falling back to regex: {}", filePath, e.getMessage());
            return scanFileFallback(filePath);
        }
    }

    /**
     * Converts dependency tree result to scan result with deduplication and categorization.
     */
    private TransitiveDependencyScanResult convertTreeResult(Path filePath, String buildFileType,
                                                               DependencyTreeResult treeResult) {
        List<TransitiveDependencyUsage> usages = new ArrayList<>();

        for (DependencyTreeResult.DependencyNode node : treeResult.getDependencies()) {
            String artifactKey = node.getArtifactKey();

            // Check against known javax dependencies
            TransitiveDependencyInfo info = KNOWN_JAVAX_DEPENDENCIES.get(artifactKey);
            String severity = info != null ? info.severity() : null;
            String recommendation = info != null ? info.recommendation() : null;
            String javaxPackage = info != null ? artifactKey : null;

            // Also flag any javax.* artifacts
            if (node.getGroupId().contains("javax") || node.getArtifactId().contains("javax")) {
                severity = "high";
                recommendation = recommendation != null ? recommendation : "Replace javax with jakarta";
                javaxPackage = javaxPackage != null ? javaxPackage : artifactKey;
            }

            if (severity != null) {
                usages.add(new TransitiveDependencyUsage(
                        node.getArtifactId(),
                        node.getGroupId(),
                        node.getVersion(),
                        javaxPackage,
                        severity,
                        recommendation,
                        node.getScope(),
                        node.isTransitive(),
                        node.getDepth()
                ));
            }
        }

        // Deduplicate results
        List<TransitiveDependencyUsage> deduplicated = deduplicationService.deduplicate(usages);

        return new TransitiveDependencyScanResult(filePath, deduplicated, buildFileType, treeResult.getScopes());
    }

    private TransitiveDependencyScanResult scanFileFallback(Path filePath) {
        try {
            String content = Files.readString(filePath);
            String fileName = filePath.getFileName().toString().toLowerCase();

            if (fileName.equals("pom.xml")) {
                return new TransitiveDependencyScanResult(filePath,
                    parseDependencies(content, MAVEN_DEPENDENCY_PATTERN, 0), "Maven");
            }
            if (fileName.endsWith(".gradle") || fileName.endsWith(".gradle.kts")) {
                return new TransitiveDependencyScanResult(filePath,
                    parseDependencies(content, GRADLE_DEPENDENCY_PATTERN, 3), "Gradle");
            }
            return TransitiveDependencyScanResult.empty(filePath);
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

    private List<TransitiveDependencyUsage> parseDependencies(String content, Pattern pattern, int versionGroup) {
        List<TransitiveDependencyUsage> usages = new ArrayList<>();
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            String groupId = matcher.group(1).trim();
            String artifactId = matcher.group(2).trim();
            String version = versionGroup > 0 ? matcher.group(versionGroup).trim() : null;
            String key = groupId + ":" + artifactId;

            TransitiveDependencyInfo info = KNOWN_JAVAX_DEPENDENCIES.get(key);
            if (info != null || groupId.contains("javax") || artifactId.contains("javax")) {
                String severity = info != null ? info.severity() : "high";
                String recommendation = info != null ? info.recommendation() : "Replace javax with jakarta";
                usages.add(new TransitiveDependencyUsage(artifactId, groupId, version, key, severity, recommendation));
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
