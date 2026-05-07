package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.DependencyTreeResult;
import adrianmikula.jakartamigration.advancedscanning.domain.ScanReason;
import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyUsage;
import adrianmikula.jakartamigration.advancedscanning.service.DependencyDeduplicationService;
import adrianmikula.jakartamigration.advancedscanning.service.DependencyTreeCommandExecutor;
import adrianmikula.jakartamigration.advancedscanning.service.ScanProgressCallback;
import adrianmikula.jakartamigration.advancedscanning.service.TransitiveDependencyScanner;
import adrianmikula.jakartamigration.dependencyanalysis.config.CompatibilityConfigLoader;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.service.ImprovedMavenCentralLookupService;
import adrianmikula.jakartamigration.dependencyanalysis.service.JarResolver;
import adrianmikula.jakartamigration.jaranalysis.domain.JarCompatibilityLevel;
import adrianmikula.jakartamigration.jaranalysis.domain.JarCompatibilityReport;
import adrianmikula.jakartamigration.jaranalysis.service.JarCompatibilityScanner;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
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
    private final CompatibilityConfigLoader compatibilityConfigLoader;
    private final JarCompatibilityScanner jarCompatibilityScanner;
    private final JarResolver jarResolver;
    private final ImprovedMavenCentralLookupService mavenCentralLookupService;

    // Scopes to include in transitive dependency scanning
    private static final Set<String> MAVEN_SCOPES = Set.of("compile", "provided", "runtime", "test");
    private static final Set<String> GRADLE_SCOPES = Set.of("compileClasspath", "runtimeClasspath", "testCompileClasspath");

    public TransitiveDependencyScannerImpl() {
        this(new DependencyTreeCommandExecutorImpl(), new DependencyDeduplicationServiceImpl(), new CompatibilityConfigLoader(),
             null, null, null);
    }

    public TransitiveDependencyScannerImpl(DependencyTreeCommandExecutor commandExecutor,
                                          DependencyDeduplicationService deduplicationService) {
        this(commandExecutor, deduplicationService, new CompatibilityConfigLoader(), null, null, null);
    }

    public TransitiveDependencyScannerImpl(DependencyTreeCommandExecutor commandExecutor,
                                          DependencyDeduplicationService deduplicationService,
                                          CompatibilityConfigLoader compatibilityConfigLoader) {
        this(commandExecutor, deduplicationService, compatibilityConfigLoader, null, null, null);
    }

    public TransitiveDependencyScannerImpl(DependencyTreeCommandExecutor commandExecutor,
                                          DependencyDeduplicationService deduplicationService,
                                          CompatibilityConfigLoader compatibilityConfigLoader,
                                          JarCompatibilityScanner jarCompatibilityScanner,
                                          JarResolver jarResolver) {
        this(commandExecutor, deduplicationService, compatibilityConfigLoader, jarCompatibilityScanner, jarResolver, null);
    }

    public TransitiveDependencyScannerImpl(DependencyTreeCommandExecutor commandExecutor,
                                          DependencyDeduplicationService deduplicationService,
                                          CompatibilityConfigLoader compatibilityConfigLoader,
                                          JarCompatibilityScanner jarCompatibilityScanner,
                                          JarResolver jarResolver,
                                          ImprovedMavenCentralLookupService mavenCentralLookupService) {
        this.commandExecutor = commandExecutor;
        this.deduplicationService = deduplicationService;
        this.compatibilityConfigLoader = compatibilityConfigLoader;
        this.jarCompatibilityScanner = jarCompatibilityScanner;
        this.jarResolver = jarResolver;
        this.mavenCentralLookupService = mavenCentralLookupService;
    }


    private static final int MAX_PARALLELISM = Integer.parseInt(
            System.getProperty("advanced.scan.parallelism", "4"));

    // Patterns for Maven pom.xml - captures groupId, artifactId, version, and optional scope
    private static final Pattern MAVEN_DEPENDENCY_PATTERN = Pattern.compile(
            "<dependency>\\s*<groupId>([^<]+)</groupId>\\s*<artifactId>([^<]+)</artifactId>\\s*<version>([^<]*)</version>(?:\\s*<scope>([^<]*)</scope>)?",
            Pattern.MULTILINE | Pattern.DOTALL);

    // Patterns for Gradle
    private static final Pattern GRADLE_DEPENDENCY_PATTERN = Pattern.compile(
            "['\"]([^':]+):([^':]+):([^'\"]+)['\"]",
            Pattern.MULTILINE);

    @Override
    public TransitiveDependencyProjectScanResult scanProject(Path projectPath) {
        log.info("[DEBUG] scanProject called with path: {}", projectPath);

        if (projectPath == null || !Files.exists(projectPath) || !Files.isDirectory(projectPath)) {
            log.warn("[DEBUG] Invalid project path: {}", projectPath);
            return TransitiveDependencyProjectScanResult.empty();
        }

        try {
            List<Path> buildFiles = discoverBuildFiles(projectPath);
            log.info("[DEBUG] Discovered {} build files: {}", buildFiles.size(), buildFiles);

            if (buildFiles.isEmpty()) {
                log.warn("[DEBUG] No build files found in {}", projectPath);
                return TransitiveDependencyProjectScanResult.empty();
            }

            AtomicInteger totalScanned = new AtomicInteger(0);
            int parallelism = Math.min(MAX_PARALLELISM, buildFiles.size());
            log.info("[DEBUG] Scanning {} files with parallelism {}", buildFiles.size(), parallelism);

            List<TransitiveDependencyScanResult> results = buildFiles.parallelStream()
                    .map(file -> {
                        log.info("[DEBUG] Scanning file: {}", file);
                        TransitiveDependencyScanResult result = scanFileWithTracking(file, totalScanned);
                        if (result != null) {
                            log.info("[DEBUG] File {} scanned: {} usages", file, result.getUsages().size());
                        } else {
                            log.warn("[DEBUG] File {} returned null result", file);
                        }
                        return result;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            int totalUsages = results.stream().mapToInt(r -> r.getUsages().size()).sum();
            log.info("[DEBUG] Scan complete: {} files, {} total usages", results.size(), totalUsages);

            return new TransitiveDependencyProjectScanResult(results, totalScanned.get(), results.size(), totalUsages);
        } catch (Exception e) {
            log.error("[DEBUG] Error scanning project for transitive dependencies", e);
            return TransitiveDependencyProjectScanResult.empty();
        }
    }

    /**
     * Sequential project scanner with per-module progress reporting.
     * Processes build files one at a time and invokes the listener after each dependency is enriched.
     * This method is used when fine-grained progress updates are required.
     *
     * @param projectPath Path to the project root
     * @param progressListener Optional callback for progress updates, may be null
     * @return TransitiveDependencyProjectScanResult with all dependencies
     */
    @Override
    public TransitiveDependencyProjectScanResult scanProject(Path projectPath, ScanProgressCallback progressListener) {
        log.info("[DEBUG] scanProject with progress listener called for: {}", projectPath);

        if (projectPath == null || !Files.exists(projectPath) || !Files.isDirectory(projectPath)) {
            log.warn("[DEBUG] Invalid project path: {}", projectPath);
            return TransitiveDependencyProjectScanResult.empty();
        }

        try {
            List<Path> buildFiles = discoverBuildFiles(projectPath);
            log.info("[DEBUG] Discovered {} build files: {}", buildFiles.size(), buildFiles);

            if (buildFiles.isEmpty()) {
                log.warn("[DEBUG] No build files found in {}", projectPath);
                return TransitiveDependencyProjectScanResult.empty();
            }

            List<TransitiveDependencyScanResult> results = new ArrayList<>();
            AtomicInteger totalScanned = new AtomicInteger(0);

            // Process build files sequentially to provide ordered progress updates
            for (Path file : buildFiles) {
                log.info("[DEBUG] Scanning file (sequential): {}", file);
                String moduleName = "Scanning module: " + file.getFileName();

                // Adapter to prefix phase with module name
                ScanProgressCallback fileListener = (phase, completed, total) -> {
                    if (progressListener != null) {
                        progressListener.onPhaseProgress(moduleName, completed, total);
                    }
                };

                // Report start of module processing
                if (progressListener != null) {
                    fileListener.onPhaseProgress("", 0, 0);
                }

                TransitiveDependencyScanResult result = scanFile(file, fileListener);
                if (result != null) {
                    results.add(result);
                } else {
                    log.warn("[DEBUG] File {} returned null result", file);
                }
                totalScanned.incrementAndGet();
            }

            int totalUsages = results.stream().mapToInt(r -> r.getUsages().size()).sum();
            log.info("[DEBUG] Scan complete (sequential): {} files, {} total usages", results.size(), totalUsages);

            return new TransitiveDependencyProjectScanResult(results, totalScanned.get(), results.size(), totalUsages);
        } catch (Exception e) {
            log.error("[DEBUG] Error scanning project for transitive dependencies (sequential)", e);
            return TransitiveDependencyProjectScanResult.empty();
        }
    }

     @Override
     public TransitiveDependencyScanResult scanFile(Path filePath) {
         return scanFile(filePath, null);
     }

     /**
      * Core per-file scanning with optional progress callback.
      * Performs incremental enrichment and reports progress after each dependency.
      *
      * @param filePath Path to the build file
      * @param listener Optional progress callback, may be null
      * @return TransitiveDependencyScanResult with enriched dependencies
      */
     private TransitiveDependencyScanResult scanFile(Path filePath, ScanProgressCallback listener) {
         if (filePath == null || !Files.exists(filePath)) {
             return TransitiveDependencyScanResult.empty(filePath);
         }

         String fileName = filePath.getFileName().toString().toLowerCase();
         boolean isMaven = fileName.equals("pom.xml");
         boolean isGradle = fileName.endsWith(".gradle") || fileName.endsWith(".gradle.kts");

         if (!isMaven && !isGradle) return TransitiveDependencyScanResult.empty(filePath);

         try {
             log.debug("Starting {} dependency scanning for file: {}", isMaven ? "Maven" : "Gradle", filePath);
             
             var future = isMaven
                 ? commandExecutor.executeMavenDependencyTreeAsync(filePath, MAVEN_SCOPES)
                 : commandExecutor.executeGradleDependenciesAsync(filePath, GRADLE_SCOPES);

             var treeResult = future.get(DependencyTreeCommandExecutor.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
             if (!treeResult.isSuccess()) {
                 log.warn("Command execution failed for {}: {}", filePath, treeResult.getErrorMessage());
                 throw new RuntimeException(treeResult.getErrorMessage());
             }
             List<DependencyTreeResult.DependencyNode> dependencies = treeResult.getDependencies();
             if (dependencies.isEmpty()) {
                 log.debug("Command returned no dependencies for {}, falling back to regex", filePath);
                 throw new RuntimeException("Command returned no dependencies");
             }
             
             log.debug("Successfully parsed {} dependencies via command execution for {}", dependencies.size(), filePath);
             String buildFileType = isMaven ? "Maven" : "Gradle";
             return convertTreeResult(filePath, buildFileType, treeResult, listener);
         } catch (Exception e) {
             log.warn("Async scanning failed for {}, falling back to regex: {}", filePath, e.getMessage());
             log.debug("Exception details:", e);
             return scanFileFallback(filePath);
         }
     }

     /**
      * Converts dependency tree result to scan result with deduplication and categorization.
      * Includes ALL dependencies from the tree, not just javax-related ones.
      * Processes dependencies in breadth-first order (by depth) and calls progress listener after each dependency.
      *
      * @param filePath Path to the build file
      * @param buildFileType "Maven" or "Gradle"
      * @param treeResult The dependency tree result
      * @param listener Optional progress callback, may be null
      * @return TransitiveDependencyScanResult with all enriched dependencies
      */
     private TransitiveDependencyScanResult convertTreeResult(Path filePath, String buildFileType,
                                                                DependencyTreeResult treeResult,
                                                                ScanProgressCallback listener) {
        // Build parent map from tree structure
        Map<String, String> parentMap = buildParentMap(treeResult.getDependencies());

        // Sort dependencies by depth for breadth-first processing
        List<DependencyTreeResult.DependencyNode> sortedNodes = treeResult.getDependencies().stream()
                .sorted(Comparator.comparingInt(DependencyTreeResult.DependencyNode::getDepth))
                .collect(Collectors.toList());

        int totalNodes = sortedNodes.size();
        List<TransitiveDependencyUsage> usages = new ArrayList<>(totalNodes);
        int processed = 0;

        for (DependencyTreeResult.DependencyNode node : sortedNodes) {
            // Classify using CompatibilityConfigLoader
            CompatibilityConfigLoader.ArtifactClassification classification =
                    compatibilityConfigLoader.classifyArtifact(node.getGroupId(), node.getArtifactId());

            // Create base usage from classification
            TransitiveDependencyUsage usage = createBaseUsage(node, classification);

            // Enrich with JAR scanning if available
            if (jarCompatibilityScanner != null && jarResolver != null) {
                Optional<TransitiveDependencyUsage> jarOpt = enrichWithJarScan(usage);
                if (jarOpt.isPresent()) {
                    usage = jarOpt.get();
                }
            }

            // Enrich with Maven Central lookup if available
            if (mavenCentralLookupService != null) {
                Optional<TransitiveDependencyUsage> mavenOpt = enrichWithMavenLookup(usage);
                if (mavenOpt.isPresent()) {
                    usage = mavenOpt.get();
                }
            }

            usages.add(usage);
            processed++;

            if (listener != null) {
                listener.onPhaseProgress("", processed, totalNodes);
            }
        }

        // Propagate incompatibility upward through the dependency tree
        usages = propagateIncompatibility(usages, parentMap);

        // Deduplicate results
        List<TransitiveDependencyUsage> deduplicated = deduplicationService.deduplicate(usages);

        return new TransitiveDependencyScanResult(filePath, deduplicated, buildFileType, treeResult.getScopes());
    }

    /**
     * Creates a base TransitiveDependencyUsage from a dependency node and its classification.
     */
    private TransitiveDependencyUsage createBaseUsage(DependencyTreeResult.DependencyNode node,
                                                        CompatibilityConfigLoader.ArtifactClassification classification) {
        ScanReason scanReason = mapClassificationToScanReason(classification);
        String severity = mapClassificationToSeverity(classification);
        String recommendation = mapClassificationToRecommendation(classification, node.getGroupId(), node.getArtifactId());
        String detailMessage = createDetailMessage(classification, node.getGroupId(), node.getArtifactId());
        String artifactKey = node.getArtifactKey();
        String javaxPackage = (classification == CompatibilityConfigLoader.ArtifactClassification.JAKARTA_REQUIRED ||
                               classification == CompatibilityConfigLoader.ArtifactClassification.CONTEXT_DEPENDENT)
                               ? artifactKey : null;

        return new TransitiveDependencyUsage(
                node.getArtifactId(),
                node.getGroupId(),
                node.getVersion(),
                javaxPackage,
                severity,
                recommendation,
                node.getScope(),
                node.isTransitive(),
                node.getDepth(),
                null, // alternativeVersions
                scanReason,
                detailMessage,
                0.0, // confidence - will be set by JAR scanning
                false // incompatibilityFromTransitive - will be set by propagation
        );
    }

    /**
     * Enriches a usage with JAR bytecode scanning results if applicable.
     * Returns Optional.of(updatedUsage) if scan was performed and produced a result,
     * or Optional.empty() if no scan was performed or scan failed (keep original).
     */
    private Optional<TransitiveDependencyUsage> enrichWithJarScan(TransitiveDependencyUsage usage) {
        // Only scan UNKNOWN and REVIEW_REQUIRED dependencies
        if (usage.getScanReason() != ScanReason.UNKNOWN && usage.getScanReason() != ScanReason.REVIEW_REQUIRED) {
            return Optional.empty();
        }

        try {
            Artifact artifact = new Artifact(
                    usage.getGroupId(),
                    usage.getArtifactId(),
                    usage.getVersion(),
                    usage.getScope() != null ? usage.getScope() : "compile",
                    usage.isTransitive()
            );
            var jarPathOpt = jarResolver.resolve(artifact);
            if (jarPathOpt.isPresent()) {
                JarCompatibilityReport report = jarCompatibilityScanner.analyzeJar(jarPathOpt.get());
                if (report != null) {
                    ScanReason newReason = mapJarLevelToScanReason(report.level());
                    String newDetail = "JAR bytecode scan: " + report.level() + " (confidence: " +
                                       String.format("%.2f", report.confidence()) + ")";
                    TransitiveDependencyUsage updated = new TransitiveDependencyUsage(
                            usage.getArtifactId(),
                            usage.getGroupId(),
                            usage.getVersion(),
                            usage.getJavaxPackage(),
                            usage.getSeverity(),
                            usage.getRecommendation(),
                            usage.getScope(),
                            usage.isTransitive(),
                            usage.getDepth(),
                            usage.getAlternativeVersions(),
                            newReason,
                            newDetail,
                            report.confidence(),
                            usage.isIncompatibilityFromTransitive()
                    );
                    return Optional.of(updated);
                }
            }
        } catch (Exception e) {
            log.debug("JAR scan failed for {}: {}", usage.getArtifactKey(), e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Enriches a usage with Maven Central lookup if applicable.
     * Returns Optional.of(updatedUsage) if lookup was performed, or Optional.empty() if skipped.
     */
    private Optional<TransitiveDependencyUsage> enrichWithMavenLookup(TransitiveDependencyUsage usage) {
        ScanReason reason = usage.getScanReason();
        // Only lookup UNKNOWN and BYTECODE_SCAN_UNKNOWN dependencies
        if (reason != ScanReason.UNKNOWN && reason != ScanReason.BYTECODE_SCAN_UNKNOWN) {
            return Optional.empty();
        }

        try {
            var future = mavenCentralLookupService.findJakartaEquivalents(usage.getGroupId(), usage.getArtifactId());
            var matches = future.get();
            if (matches != null && !matches.isEmpty()) {
                ImprovedMavenCentralLookupService.JakartaArtifactMatch firstMatch = matches.get(0);
                String newRecommendation = firstMatch.groupId() + ":" + firstMatch.artifactId() +
                        (firstMatch.version() != null ? ":" + firstMatch.version() : "");
                TransitiveDependencyUsage updated = new TransitiveDependencyUsage(
                        usage.getArtifactId(),
                        usage.getGroupId(),
                        usage.getVersion(),
                        usage.getJavaxPackage(),
                        usage.getSeverity(),
                        newRecommendation,
                        usage.getScope(),
                        usage.isTransitive(),
                        usage.getDepth(),
                        usage.getAlternativeVersions(),
                        ScanReason.MAVEN_LOOKUP_FOUND,
                        "Maven Central found Jakarta equivalent: " + newRecommendation,
                        0.7, // heuristic confidence
                        usage.isIncompatibilityFromTransitive()
                );
                return Optional.of(updated);
            } else {
                // No Jakarta equivalent found
                TransitiveDependencyUsage updated = new TransitiveDependencyUsage(
                        usage.getArtifactId(),
                        usage.getGroupId(),
                        usage.getVersion(),
                        usage.getJavaxPackage(),
                        "low", // downgrade severity since nothing found
                        usage.getRecommendation(),
                        usage.getScope(),
                        usage.isTransitive(),
                        usage.getDepth(),
                        usage.getAlternativeVersions(),
                        ScanReason.MAVEN_LOOKUP_NONE,
                        "Maven Central found no Jakarta equivalent",
                        0.0,
                        usage.isIncompatibilityFromTransitive()
                );
                return Optional.of(updated);
            }
        } catch (Exception e) {
            log.debug("Maven Central lookup failed for {}: {}", usage.getArtifactKey(), e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Builds a parent map from the dependency tree structure.
     * Maps child artifact key to parent artifact key.
     */
    private Map<String, String> buildParentMap(List<DependencyTreeResult.DependencyNode> nodes) {
        Map<String, String> parentMap = new HashMap<>();
        for (DependencyTreeResult.DependencyNode node : nodes) {
            if (node.getParentArtifactKey() != null) {
                parentMap.put(node.getArtifactKey(), node.getParentArtifactKey());
            }
        }
        return parentMap;
    }

    /**
     * Propagates incompatibility upward through the dependency tree.
     * Marks ancestors of incompatible dependencies as TRANSITIVE_INCOMPATIBLE.
     */
    private List<TransitiveDependencyUsage> propagateIncompatibility(List<TransitiveDependencyUsage> usages,
                                                                       Map<String, String> parentMap) {
        // Build a map of artifact key to usage for quick lookup
        Map<String, TransitiveDependencyUsage> usageMap = usages.stream()
                .collect(Collectors.toMap(TransitiveDependencyUsage::getArtifactKey, u -> u, (a, b) -> a));

        // Identify incompatible nodes
        Set<String> incompatibleKeys = usages.stream()
                .filter(u -> isIncompatibleReason(u.getScanReason()))
                .map(TransitiveDependencyUsage::getArtifactKey)
                .collect(Collectors.toSet());

        // Propagate incompatibility upward
        for (String incompatibleKey : incompatibleKeys) {
            String currentKey = incompatibleKey;
            while (currentKey != null) {
                String parentKey = parentMap.get(currentKey);
                
                if (parentKey == null) {
                    break; // Reached root
                }

                TransitiveDependencyUsage parentUsage = usageMap.get(parentKey);
                if (parentUsage != null) {
                    // Only mark if not already more severe
                    if (parentUsage.getScanReason() != ScanReason.BLACKLISTED &&
                        parentUsage.getScanReason() != ScanReason.BYTECODE_SCAN_JAVAX &&
                        parentUsage.getScanReason() != ScanReason.BYTECODE_SCAN_MIXED) {
                        
                        // Create updated usage with TRANSITIVE_INCOMPATIBLE reason
                        TransitiveDependencyUsage updatedUsage = new TransitiveDependencyUsage(
                                parentUsage.getArtifactId(),
                                parentUsage.getGroupId(),
                                parentUsage.getVersion(),
                                parentUsage.getJavaxPackage(),
                                "high", // upgrade severity
                                parentUsage.getRecommendation(),
                                parentUsage.getScope(),
                                parentUsage.isTransitive(),
                                parentUsage.getDepth(),
                                parentUsage.getAlternativeVersions(),
                                ScanReason.TRANSITIVE_INCOMPATIBLE,
                                "Incompatible due to transitive dependency: " + incompatibleKey,
                                parentUsage.getConfidence(),
                                true
                        );
                        
                        // Update the map and the list
                        usageMap.put(parentKey, updatedUsage);
                    }
                }
                
                currentKey = parentKey;
            }
        }

        // Return updated list
        return new ArrayList<>(usageMap.values());
    }

    /**
     * Checks if a scan reason indicates incompatibility.
     */
    private boolean isIncompatibleReason(ScanReason reason) {
        return reason == ScanReason.BLACKLISTED ||
               reason == ScanReason.BYTECODE_SCAN_JAVAX ||
               reason == ScanReason.BYTECODE_SCAN_MIXED ||
               reason == ScanReason.TRANSITIVE_INCOMPATIBLE;
    }



    private ScanReason mapJarLevelToScanReason(JarCompatibilityLevel level) {
        return switch (level) {
            case JAVAX -> ScanReason.BYTECODE_SCAN_JAVAX;
            case JAKARTA -> ScanReason.BYTECODE_SCAN_JAKARTA;
            case MIXED -> ScanReason.BYTECODE_SCAN_MIXED;
            case UNKNOWN -> ScanReason.BYTECODE_SCAN_UNKNOWN;
            case DUAL_COMPATIBLE -> ScanReason.BYTECODE_SCAN_JAKARTA; // Dual-compatible is treated as Jakarta-compatible
        };
    }



    private ScanReason mapClassificationToScanReason(CompatibilityConfigLoader.ArtifactClassification classification) {
        return switch (classification) {
            case JDK_PROVIDED -> ScanReason.WHITELISTED;
            case JAKARTA_REQUIRED -> ScanReason.BLACKLISTED;
            case CONTEXT_DEPENDENT -> ScanReason.REVIEW_REQUIRED;
            case UNKNOWN -> ScanReason.UNKNOWN;
        };
    }

    private String mapClassificationToSeverity(CompatibilityConfigLoader.ArtifactClassification classification) {
        return switch (classification) {
            case JDK_PROVIDED -> "low";
            case JAKARTA_REQUIRED -> "high";
            case CONTEXT_DEPENDENT -> "medium";
            case UNKNOWN -> "low";
        };
    }

    private String mapClassificationToRecommendation(CompatibilityConfigLoader.ArtifactClassification classification,
                                                      String groupId, String artifactId) {
        return switch (classification) {
            case JDK_PROVIDED -> "JDK-provided package, no migration needed";
            case JAKARTA_REQUIRED -> "Configured upgrade required to Jakarta EE equivalent";
            case CONTEXT_DEPENDENT -> "Context-dependent, review needed";
            case UNKNOWN -> null;
        };
    }

    private String createDetailMessage(CompatibilityConfigLoader.ArtifactClassification classification,
                                       String groupId, String artifactId) {
        return switch (classification) {
            case JDK_PROVIDED -> "JDK-provided package: " + groupId + ":" + artifactId;
            case JAKARTA_REQUIRED -> "Configured as requiring Jakarta migration: " + groupId + ":" + artifactId;
            case CONTEXT_DEPENDENT -> "Context-dependent classification: " + groupId + ":" + artifactId;
            case UNKNOWN -> "Unclassified artifact: " + groupId + ":" + artifactId;
        };
    }

    private TransitiveDependencyScanResult scanFileFallback(Path filePath) {
        try {
            String content = Files.readString(filePath);
            String fileName = filePath.getFileName().toString().toLowerCase();

            if (fileName.equals("pom.xml")) {
                // For Maven: extract properties first, then parse dependencies with property resolution
                Map<String, String> properties = extractMavenProperties(content);
                return new TransitiveDependencyScanResult(filePath,
                    parseDependenciesWithProperties(content, MAVEN_DEPENDENCY_PATTERN, 3, properties), "Maven");
            }
            if (fileName.endsWith(".gradle") || fileName.endsWith(".gradle.kts")) {
                // For Gradle: group 3 contains version
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
        return parseDependenciesWithProperties(content, pattern, versionGroup, Map.of());
    }

    private List<TransitiveDependencyUsage> parseDependenciesWithProperties(String content, Pattern pattern, int versionGroup, Map<String, String> properties) {
        List<TransitiveDependencyUsage> usages = new ArrayList<>();
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            String groupId = matcher.group(1).trim();
            String artifactId = matcher.group(2).trim();
            String version = versionGroup > 0 && matcher.groupCount() >= versionGroup
                    ? matcher.group(versionGroup).trim() : null;
            
            // Resolve version if it's a property reference
            if (version != null && version.startsWith("${") && version.endsWith("}")) {
                String propertyName = version.substring(2, version.length() - 1);
                version = properties.get(propertyName);
            }
            
            // For Maven pattern, scope is in group 4 when present
            String scope = matcher.groupCount() >= 4 && matcher.group(4) != null
                    ? matcher.group(4).trim() : null;
            String key = groupId + ":" + artifactId;

            // Classify using CompatibilityConfigLoader
            CompatibilityConfigLoader.ArtifactClassification classification = 
                    compatibilityConfigLoader.classifyArtifact(groupId, artifactId);

            // Map classification to ScanReason and other fields
            ScanReason scanReason = mapClassificationToScanReason(classification);
            String severity = mapClassificationToSeverity(classification);
            String recommendation = mapClassificationToRecommendation(classification, groupId, artifactId);
            String detailMessage = createDetailMessage(classification, groupId, artifactId);
            String javaxPackage = (classification == CompatibilityConfigLoader.ArtifactClassification.JAKARTA_REQUIRED ||
                                   classification == CompatibilityConfigLoader.ArtifactClassification.CONTEXT_DEPENDENT) 
                                   ? key : null;

            // Add ALL dependencies, not just javax ones
            usages.add(new TransitiveDependencyUsage(artifactId, groupId, version, javaxPackage, severity, recommendation,
                    scope, false, 0, null, scanReason, detailMessage, 0.0, false));
        }
        return usages;
    }

    private Map<String, String> extractMavenProperties(String content) {
        Map<String, String> properties = new HashMap<>();
        
        // Extract properties section using regex
        Pattern propertiesPattern = Pattern.compile(
            "<properties>\\s*(.*?)\\s*</properties>",
            Pattern.DOTALL
        );
        Matcher propertiesMatcher = propertiesPattern.matcher(content);
        
        if (propertiesMatcher.find()) {
            String propertiesContent = propertiesMatcher.group(1);
            
            // Extract individual properties
            Pattern propertyPattern = Pattern.compile(
                "<([^>]+)>([^<]*)</\\1>",
                Pattern.DOTALL
            );
            Matcher propertyMatcher = propertyPattern.matcher(propertiesContent);
            
            while (propertyMatcher.find()) {
                String propertyName = propertyMatcher.group(1).trim();
                String propertyValue = propertyMatcher.group(2).trim();
                properties.put(propertyName, propertyValue);
            }
        }
        
        return properties;
    }

    /**
     * Scans a single file with tracking for parallel processing.
     * Returns all dependencies, not just those with javax usage.
     */
    private TransitiveDependencyScanResult scanFileWithTracking(Path filePath, AtomicInteger totalScanned) {
        totalScanned.incrementAndGet();
        return scanFile(filePath);
    }
}
