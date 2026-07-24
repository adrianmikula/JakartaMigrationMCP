package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.DependencyTreeResult;
import adrianmikula.jakartamigration.advancedscanning.domain.ScanReason;
import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyUsage;
import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyEdge;
import adrianmikula.jakartamigration.advancedscanning.service.DependencyDeduplicationService;
import adrianmikula.jakartamigration.advancedscanning.service.DependencyTreeCommandExecutor;
import adrianmikula.jakartamigration.advancedscanning.service.ScanProgressCallback;
import adrianmikula.jakartamigration.advancedscanning.service.TransitiveDependencyScanner;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Namespace;
import adrianmikula.jakartamigration.dependencyanalysis.service.NamespaceClassifier;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.service.ImprovedMavenCentralLookupService;
import adrianmikula.jakartamigration.dependencyanalysis.service.JarResolver;
import adrianmikula.jakartamigration.dependencyanalysis.util.MavenPomParser;
import adrianmikula.jakartamigration.dependencyanalysis.util.GradleBuildParser;
import adrianmikula.jakartamigration.dependencyanalysis.util.BuildFileDiscovery;
import adrianmikula.jakartamigration.dependencyanalysis.util.ScopeConstants;
import adrianmikula.jakartamigration.scanning.RecipeBasedClassifier;
import adrianmikula.jakartamigration.scanning.BalloonNotificationService;
import adrianmikula.jakartamigration.jaranalysis.domain.JarCompatibilityLevel;
import adrianmikula.jakartamigration.jaranalysis.domain.JarCompatibilityReport;
import adrianmikula.jakartamigration.jaranalysis.service.JarCompatibilityScanner;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import adrianmikula.jakartamigration.util.ProjectFileSystemScanner;

@Slf4j
public class TransitiveDependencyScannerImpl implements TransitiveDependencyScanner {

    private final ProjectFileSystemScanner fileScanner = new ProjectFileSystemScanner();
    private final DependencyTreeCommandExecutor commandExecutor;
    private final DependencyDeduplicationService deduplicationService;
    private final NamespaceClassifier namespaceClassifier;
    private final JarCompatibilityScanner jarCompatibilityScanner;
    private final JarResolver jarResolver;
    private final ImprovedMavenCentralLookupService mavenCentralLookupService;
    private final BalloonNotificationService balloonNotificationService;

    private final Map<String, Namespace> classificationCache = new HashMap<>(1000);

    // Scopes to include in transitive dependency scanning
    private static final Set<String> MAVEN_SCOPES = ScopeConstants.DEFAULT_MAVEN_SCOPES;
    private static final Set<String> GRADLE_SCOPES = ScopeConstants.GRADLE_COMPILE_CONFIGS;

    public TransitiveDependencyScannerImpl() {
        this(new DependencyTreeCommandExecutorImpl(), new DependencyDeduplicationServiceImpl(),
             new RecipeBasedClassifier(), null, null, null, null);
    }

    public TransitiveDependencyScannerImpl(DependencyTreeCommandExecutor commandExecutor,
                                          DependencyDeduplicationService deduplicationService) {
        this(commandExecutor, deduplicationService, new RecipeBasedClassifier(), null, null, null, null);
    }

    public TransitiveDependencyScannerImpl(DependencyTreeCommandExecutor commandExecutor,
                                          DependencyDeduplicationService deduplicationService,
                                          NamespaceClassifier namespaceClassifier) {
        this(commandExecutor, deduplicationService, namespaceClassifier, null, null, null, null);
    }

    public TransitiveDependencyScannerImpl(DependencyTreeCommandExecutor commandExecutor,
                                          DependencyDeduplicationService deduplicationService,
                                          NamespaceClassifier namespaceClassifier,
                                          JarCompatibilityScanner jarCompatibilityScanner,
                                          JarResolver jarResolver) {
        this(commandExecutor, deduplicationService, namespaceClassifier, jarCompatibilityScanner, jarResolver, null, null);
    }

    public TransitiveDependencyScannerImpl(DependencyTreeCommandExecutor commandExecutor,
                                          DependencyDeduplicationService deduplicationService,
                                          NamespaceClassifier namespaceClassifier,
                                          JarCompatibilityScanner jarCompatibilityScanner,
                                          JarResolver jarResolver,
                                          ImprovedMavenCentralLookupService mavenCentralLookupService) {
        this(commandExecutor, deduplicationService, namespaceClassifier, jarCompatibilityScanner, jarResolver, mavenCentralLookupService, null);
    }

    public TransitiveDependencyScannerImpl(DependencyTreeCommandExecutor commandExecutor,
                                          DependencyDeduplicationService deduplicationService,
                                          NamespaceClassifier namespaceClassifier,
                                          JarCompatibilityScanner jarCompatibilityScanner,
                                          JarResolver jarResolver,
                                          ImprovedMavenCentralLookupService mavenCentralLookupService,
                                          BalloonNotificationService balloonNotificationService) {
        this.commandExecutor = commandExecutor;
        this.deduplicationService = deduplicationService;
        this.namespaceClassifier = namespaceClassifier;
        this.jarCompatibilityScanner = jarCompatibilityScanner;
        this.jarResolver = jarResolver;
        this.mavenCentralLookupService = mavenCentralLookupService;
        this.balloonNotificationService = balloonNotificationService;
    }


    private static final int MAX_PARALLELISM = Integer.parseInt(
            System.getProperty("advanced.scan.parallelism", "4"));

    @Override
    public TransitiveDependencyProjectScanResult scanProject(Path projectPath) {
        return scanProject(projectPath, null);
    }

    @Override
    public TransitiveDependencyProjectScanResult scanProject(List<Path> filesToScan) {
        return scanProject(filesToScan, null);
    }

    @Override
    public TransitiveDependencyProjectScanResult scanProject(List<Path> filesToScan, ScanProgressCallback progressListener) {
        if (filesToScan == null || filesToScan.isEmpty()) {
            return TransitiveDependencyProjectScanResult.empty();
        }
        log.info("[DEBUG] scanProject with progress listener (list) called with {} files", filesToScan.size());

        // Check if this is a multi-module project and handle it accordingly
        if (filesToScan.size() > 1) {
            // Find a common project root from the build files
            Optional<Path> projectRoot = BuildFileDiscovery.findCommonProjectRoot(filesToScan);
            if (projectRoot.isPresent() && BuildFileDiscovery.detectMultiModuleProject(projectRoot.get())) {
                log.info("Multi-module project detected from file list, scanning from root: {}", projectRoot.get());
                TransitiveDependencyProjectScanResult result = scanProject(projectRoot.get(), progressListener);
                // Filter results to only include files that were in the original list
                if (!result.getFileResults().isEmpty()) {
                    List<TransitiveDependencyScanResult> filteredResults = result.getFileResults().stream()
                        .filter(r -> filesToScan.contains(r.getFilePath()))
                        .collect(Collectors.toList());
                    return new TransitiveDependencyProjectScanResult(
                        filteredResults,
                        result.getTotalBuildFilesScanned(),
                        filteredResults.size(),
                        filteredResults.stream().mapToInt(r -> r.getUsages().size()).sum(),
                        (int) filteredResults.stream().filter(r -> r.hasError()).count(),
                        result.isHadCommandNotFoundError(),
                        result.getErrorMessage()
                    );
                }
                return result;
            }
        }

        // Fallback to per-file scanning for single-module or if multi-module detection failed
        List<TransitiveDependencyScanResult> results = new ArrayList<>();
        AtomicInteger totalScanned = new AtomicInteger(0);

        for (Path file : filesToScan) {
            log.info("[DEBUG] Scanning file (sequential): {}", file);
            String moduleName = "Scanning module: " + file.getFileName();
            ScanProgressCallback fileListener = (phase, completed, total) -> {
                if (progressListener != null) {
                    progressListener.onPhaseProgress(moduleName + " — " + phase, completed, total);
                }
            };
            if (progressListener != null) {
                progressListener.onPhaseProgress(moduleName, 0, 0);
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
        int filesWithErrors = (int) results.stream().filter(r -> r.hasError()).count();
        boolean hadCommandNotFound = results.stream()
            .filter(r -> r.hasError())
            .anyMatch(r -> r.getErrorMessage() != null &&
                (r.getErrorMessage().contains("mvn command not found") ||
                 r.getErrorMessage().contains("gradle command not found") ||
                 r.getErrorMessage().contains("not found")));
        String errorSummary = hadCommandNotFound ?
            "Build tool (Maven/Gradle) not found. Transitive dependency scanning fell back to regex parsing." : null;
        log.info("[DEBUG] Scan complete (sequential): {} files, {} total usages, {} files with errors", results.size(), totalUsages, filesWithErrors);
        return new TransitiveDependencyProjectScanResult(results, totalScanned.get(), results.size(), totalUsages,
            filesWithErrors, hadCommandNotFound, errorSummary);
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
             List<Path> buildFiles = BuildFileDiscovery.discoverBuildFiles(projectPath);
             log.info("[DEBUG] Discovered {} build files: {}", buildFiles.size(), buildFiles);

             if (buildFiles.isEmpty()) {
                 log.warn("[DEBUG] No build files found in {}", projectPath);
                 return TransitiveDependencyProjectScanResult.empty();
             }

             // For multi-module projects, run command once from root instead of per-file
             boolean isMultiModule = buildFiles.size() > 1 && BuildFileDiscovery.detectMultiModuleProject(projectPath);
            List<TransitiveDependencyScanResult> results = null;
            AtomicInteger totalScanned = new AtomicInteger(0);

            if (isMultiModule) {
                log.info("Multi-module project detected ({} build files), scanning from root", buildFiles.size());
                results = scanMultiModuleProject(projectPath, buildFiles, progressListener);
                if (results != null) {
                    totalScanned.set(1);
                }
            }

            // Fallback to per-file scanning for single-module or if multi-module scan failed
            if (results == null) {
                results = new ArrayList<>();

                // Process build files sequentially to provide ordered progress updates
                for (Path file : buildFiles) {
                    log.info("[DEBUG] Scanning file (sequential): {}", file);
                    String moduleName = "Scanning module: " + file.getFileName();

                    // Adapter to prefix phase with module name
                    ScanProgressCallback fileListener = (phase, completed, total) -> {
                        if (progressListener != null) {
                            progressListener.onPhaseProgress(moduleName + " — " + phase, completed, total);
                        }
                    };

                    // Report start of module processing
                    if (progressListener != null) {
                        progressListener.onPhaseProgress(moduleName, 0, 0);
                    }

                    TransitiveDependencyScanResult result = scanFile(file, fileListener);
                    if (result != null) {
                        results.add(result);
                    } else {
                        log.warn("[DEBUG] File {} returned null result", file);
                    }
                    totalScanned.incrementAndGet();
                }
            }

            int totalUsages = results.stream().mapToInt(r -> r.getUsages().size()).sum();
            int filesWithErrors = (int) results.stream().filter(r -> r.hasError()).count();
            boolean hadCommandNotFound = results.stream()
                .filter(r -> r.hasError())
                .anyMatch(r -> r.getErrorMessage() != null &&
                    (r.getErrorMessage().contains("mvn command not found") ||
                     r.getErrorMessage().contains("gradle command not found") ||
                     r.getErrorMessage().contains("not found")));
            String errorSummary = hadCommandNotFound ?
                "Build tool (Maven/Gradle) not found. Transitive dependency scanning fell back to regex parsing." : null;
            log.info("[DEBUG] Scan complete (sequential): {} files, {} total usages, {} files with errors", results.size(), totalUsages, filesWithErrors);

            return new TransitiveDependencyProjectScanResult(results, totalScanned.get(), results.size(), totalUsages,
                filesWithErrors, hadCommandNotFound, errorSummary);
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
             
             if (listener != null) {
                 listener.onPhaseProgress(isMaven ? "Executing Maven dependency:tree" : "Executing Gradle dependencies", 0, 1);
             }
             
             var future = isMaven
                 ? commandExecutor.executeMavenDependencyTreeAsync(filePath, MAVEN_SCOPES)
                 : commandExecutor.executeGradleDependenciesAsync(filePath, GRADLE_SCOPES);

             var treeResult = future.get(DependencyTreeCommandExecutor.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
             
             if (listener != null) {
                 listener.onPhaseProgress(isMaven ? "Executing Maven dependency:tree" : "Executing Gradle dependencies", 1, 1);
             }
             if (!treeResult.isSuccess()) {
                 log.debug("Command execution failed for {}: {}", filePath, treeResult.getErrorMessage());
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
               log.warn("Async scanning failed for {}, falling back to regex: {}", filePath, e.getClass().getSimpleName() + ": " + e.getMessage());
               log.debug("Exception details:", e);
               // Notify user via IDE balloon (deduplicated per file path)
               if (balloonNotificationService != null) {
                   String notifKey = "build-tool-failure:" + filePath;
                   balloonNotificationService.showOnce(notifKey,
                       "Build Tool Error",
                       "Build command failed for " + filePath.getFileName()
                           + ". Results shown are based on regex fallback (partial).\n"
                           + e.getMessage());
               }
                // Fall back to regex scanning - retain the fallback's own classification
                // instead of marking everything as BUILD_TOOL_ERROR
                return scanFileFallback(filePath, listener, e.getMessage());
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

         // Use dependencies directly without sorting - the tree structure is already ordered
         // Sorting by depth was removed as it adds O(n log n) overhead with minimal benefit
         List<DependencyTreeResult.DependencyNode> nodes = treeResult.getDependencies();

         int totalNodes = nodes.size();
         List<TransitiveDependencyUsage> usages = new ArrayList<>(totalNodes);
         int processed = 0;

         // First pass: create base usages and collect those needing enrichment
         List<TransitiveDependencyUsage> usagesNeedingJarScan = new ArrayList<>();
         List<TransitiveDependencyUsage> usagesNeedingMavenLookup = new ArrayList<>();
         Map<String, Integer> usageIndexMap = new HashMap<>(totalNodes);

          for (DependencyTreeResult.DependencyNode node : nodes) {
             String artifactKey = node.getGroupId() + ":" + node.getArtifactId();
             Namespace ns = classificationCache.computeIfAbsent(artifactKey,
                 k -> namespaceClassifier.classify(new Artifact(node.getGroupId(), node.getArtifactId(), node.getVersion(), node.getScope(), node.isTransitive())));

             TransitiveDependencyUsage usage = createBaseUsage(node, ns);
             usages.add(usage);
             
             // Track index for later merging
             usageIndexMap.put(usage.getArtifactKey(), usages.size() - 1);

             // Collect usages needing JAR scanning
             if (jarCompatibilityScanner != null && jarResolver != null) {
                 if (usage.getScanReason() == ScanReason.UNKNOWN) {
                     usagesNeedingJarScan.add(usage);
                 }
             }

             // Collect usages needing Maven Central lookup
             if (mavenCentralLookupService != null) {
                 if (usage.getScanReason() == ScanReason.UNKNOWN || usage.getScanReason() == ScanReason.BYTECODE_SCAN_UNKNOWN) {
                     usagesNeedingMavenLookup.add(usage);
                 }
             }

             processed++;

             if (listener != null) {
                 listener.onPhaseProgress("Classifying dependencies", processed, totalNodes);
             }
         }
         
         // Batch JAR scanning in parallel
         if (!usagesNeedingJarScan.isEmpty()) {
             if (listener != null) {
                 listener.onPhaseProgress("Scanning JARs for javax/jakarta usage", 0, usagesNeedingJarScan.size());
             }
             Map<String, TransitiveDependencyUsage> jarScanResults = enrichWithJarScansBatch(usagesNeedingJarScan);
             // Merge results back into usages list
             for (TransitiveDependencyUsage original : usagesNeedingJarScan) {
                 TransitiveDependencyUsage enriched = jarScanResults.get(original.getArtifactKey());
                 if (enriched != null) {
                     int index = usageIndexMap.get(original.getArtifactKey());
                     usages.set(index, enriched);
                 }
             }
             if (listener != null) {
                 listener.onPhaseProgress("Scanning JARs for javax/jakarta usage", usagesNeedingJarScan.size(), usagesNeedingJarScan.size());
             }
         }
         
         // Batch Maven Central lookups in parallel
         if (!usagesNeedingMavenLookup.isEmpty()) {
             if (listener != null) {
                 listener.onPhaseProgress("Looking up Maven Central for Jakarta equivalents", 0, usagesNeedingMavenLookup.size());
             }
             Map<String, TransitiveDependencyUsage> mavenLookupResults = enrichWithMavenLookupsBatch(usagesNeedingMavenLookup);
             // Merge results back into usages list
             for (TransitiveDependencyUsage original : usagesNeedingMavenLookup) {
                 TransitiveDependencyUsage enriched = mavenLookupResults.get(original.getArtifactKey());
                 if (enriched != null) {
                     int index = usageIndexMap.get(original.getArtifactKey());
                     usages.set(index, enriched);
                 }
             }
             if (listener != null) {
                 listener.onPhaseProgress("Looking up Maven Central for Jakarta equivalents", usagesNeedingMavenLookup.size(), usagesNeedingMavenLookup.size());
             }
         }

         // Propagate incompatibility upward through the dependency tree
         usages = propagateIncompatibility(usages, parentMap);

         // Deduplicate results
         List<TransitiveDependencyUsage> deduplicated = deduplicationService.deduplicate(usages);

         // Build edges from parent map BEFORE deduplication to preserve tree structure
         List<TransitiveDependencyEdge> edges = parentMap.entrySet().stream()
                 .map(entry -> new TransitiveDependencyEdge(entry.getValue(), entry.getKey()))
                 .collect(Collectors.toList());

         return new TransitiveDependencyScanResult(filePath, deduplicated, buildFileType, treeResult.getScopes(), edges);
     }

    /**
     * Creates a base TransitiveDependencyUsage from a dependency node and its classification.
     */
    private TransitiveDependencyUsage createBaseUsage(DependencyTreeResult.DependencyNode node,
                                                        Namespace ns) {
        ScanReason scanReason = mapNamespaceToScanReason(ns);
        String severity = mapNamespaceToSeverity(ns);
        String recommendation = mapNamespaceToRecommendation(ns, node.getGroupId(), node.getArtifactId());
        String detailMessage = detailMessage(ns, node.getGroupId(), node.getArtifactId());
        String artifactKey = node.getArtifactKey();
        String javaxPackage = (ns == Namespace.JAVAX || ns == Namespace.MIXED) ? artifactKey : null;

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
        // Only scan UNKNOWN dependencies
        if (usage.getScanReason() != ScanReason.UNKNOWN) {
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
            log.warn("JAR scan failed for {}: {}", usage.getArtifactKey(), e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return Optional.empty();
    }
    
    /**
     * Batch enriches multiple usages with JAR bytecode scanning in parallel.
     * Returns a map of artifactKey to enriched usage (only for successful scans).
     */
    private Map<String, TransitiveDependencyUsage> enrichWithJarScansBatch(List<TransitiveDependencyUsage> usages) {
        Map<String, TransitiveDependencyUsage> results = new ConcurrentHashMap<>();
        
        usages.parallelStream().forEach(usage -> {
            Optional<TransitiveDependencyUsage> enriched = enrichWithJarScan(usage);
            enriched.ifPresent(u -> results.put(u.getArtifactKey(), u));
        });
        
        return results;
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
            log.warn("Maven Central lookup failed for {}: {}", usage.getArtifactKey(), e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return Optional.empty();
    }
    
    /**
     * Batch enriches multiple usages with Maven Central lookups in parallel.
     * Returns a map of artifactKey to enriched usage (only for successful lookups).
     */
    private Map<String, TransitiveDependencyUsage> enrichWithMavenLookupsBatch(List<TransitiveDependencyUsage> usages) {
        Map<String, TransitiveDependencyUsage> results = new ConcurrentHashMap<>();
        
        usages.parallelStream().forEach(usage -> {
            Optional<TransitiveDependencyUsage> enriched = enrichWithMavenLookup(usage);
            enriched.ifPresent(u -> results.put(u.getArtifactKey(), u));
        });
        
        return results;
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
     * Optimized to use single pass through parentMap and track already-marked nodes.
     */
    private List<TransitiveDependencyUsage> propagateIncompatibility(List<TransitiveDependencyUsage> usages,
                                                                       Map<String, String> parentMap) {
        // Build a map of artifact key to usage for quick lookup
        Map<String, TransitiveDependencyUsage> usageMap = usages.stream()
                .collect(Collectors.toMap(TransitiveDependencyUsage::getArtifactKey, u -> u, (a, b) -> a));

        // Track already-marked nodes to avoid duplicate processing
        Set<String> alreadyMarked = new HashSet<>();

        // Propagate incompatibility upward in single pass through parentMap
        for (Map.Entry<String, String> entry : parentMap.entrySet()) {
            String childKey = entry.getKey();
            String parentKey = entry.getValue();
            
            TransitiveDependencyUsage childUsage = usageMap.get(childKey);
            if (childUsage == null || !isIncompatibleReason(childUsage.getScanReason())) {
                continue; // Only propagate from incompatible nodes
            }
            
            // Walk up the tree marking ancestors
            String currentKey = parentKey;
            while (currentKey != null) {
                if (alreadyMarked.contains(currentKey)) {
                    break; // Already marked this node, skip
                }
                
                TransitiveDependencyUsage parentUsage = usageMap.get(currentKey);
                if (parentUsage == null) {
                    currentKey = parentMap.get(currentKey);
                    continue;
                }
                
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
                            "Incompatible due to transitive dependency: " + childKey,
                            parentUsage.getConfidence(),
                            true
                    );
                    
                    usageMap.put(currentKey, updatedUsage);
                    alreadyMarked.add(currentKey);
                }
                
                currentKey = parentMap.get(currentKey);
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

    /**
     * Error code to human-friendly message mapping for dependency annotations.
     */
    private static final Map<String, String> ERROR_CODE_MESSAGES = Map.of(
        "command_not_found", "Build tool not found — install Maven/Gradle or add wrapper to project",
        "command_failed", "Build command failed — results based on regex fallback (partial)",
        "no_dependencies", "Build command returned no dependencies — check build configuration",
        "timeout", "Build command timed out — project may be too large or build too slow",
        "parse_error", "Could not parse dependency tree — using regex fallback",
        "classification_error", "Could not classify dependency — review manually",
        "jar_scan_error", "Could not scan JAR file — JAR may be corrupted or inaccessible",
        "maven_lookup_error", "Maven Central lookup failed — network or timeout issue"
    );

    private static String friendlyErrorFor(String rawMessage) {
        if (rawMessage == null) return null;
        String lower = rawMessage.toLowerCase();
        if (lower.contains("not found")) return ERROR_CODE_MESSAGES.get("command_not_found");
        if (lower.contains("timeout")) return ERROR_CODE_MESSAGES.get("timeout");
        if (lower.contains("no dependencies")) return ERROR_CODE_MESSAGES.get("no_dependencies");
        if (lower.contains("exited with code") || lower.contains("command failed")) return ERROR_CODE_MESSAGES.get("command_failed");
        return "Build tool error — " + rawMessage;
    }

    private TransitiveDependencyScanResult scanFileFallback(Path filePath, ScanProgressCallback listener, String errorMessage) {
        try {
            if (listener != null) {
                listener.onPhaseProgress("Fallback regex scan", 0, 1);
            }
            String content = Files.readString(filePath);
            String fileName = filePath.getFileName().toString().toLowerCase();
            String fallbackDetail = friendlyErrorFor(errorMessage);

            List<TransitiveDependencyUsage> usages;
            String buildFileType;

            if (fileName.equals("pom.xml")) {
                usages = toUsages(content, dependencies -> MavenPomParser.parseDependenciesFromContent(content), fallbackDetail);
                buildFileType = "Maven";
            } else if (fileName.endsWith(".gradle") || fileName.endsWith(".gradle.kts")) {
                usages = toUsagesGradle(GradleBuildParser.parseDependencies(content), fallbackDetail);
                buildFileType = "Gradle";
            } else {
                if (listener != null) {
                    listener.onPhaseProgress("Fallback regex scan", 1, 1);
                }
                return TransitiveDependencyScanResult.empty(filePath);
            }

            // Report per-dependency progress through the fallback scan
            if (listener != null) {
                for (int i = 0; i < usages.size(); i++) {
                    listener.onPhaseProgress("Fallback regex scan", i + 1, usages.size());
                }
            }

            return new TransitiveDependencyScanResult(filePath, usages, buildFileType);
        } catch (Exception e) {
            log.warn("Fallback regex scan failed for {}: {}", filePath, e.getClass().getSimpleName() + ": " + e.getMessage());
            if (listener != null) {
                listener.onPhaseProgress("Fallback regex scan", 1, 1);
            }
            return TransitiveDependencyScanResult.empty(filePath);
        }
    }

    private List<TransitiveDependencyUsage> toUsages(String pomContent, java.util.function.Function<String, List<Map<String, String>>> parser, String fallbackDetail) {
        List<Map<String, String>> dependencies = parser.apply(pomContent);
        List<TransitiveDependencyUsage> usages = new ArrayList<>();
        for (Map<String, String> dep : dependencies) {
            String groupId = dep.get("groupId");
            String artifactId = dep.get("artifactId");
            String version = dep.getOrDefault("version", "unknown");
            String scope = dep.getOrDefault("scope", "compile");
            String key = groupId + ":" + artifactId;

            Namespace ns = classify(groupId, artifactId);
            ScanReason scanReason = mapNamespaceToScanReason(ns);
            String severity = mapNamespaceToSeverity(ns);
            String recommendation = mapNamespaceToRecommendation(ns, groupId, artifactId);
            String javaxPackage = (ns == Namespace.JAVAX || ns == Namespace.MIXED) ? key : null;
            String msg = fallbackDetail != null ? fallbackDetail : detailMessage(ns, groupId, artifactId);

            usages.add(new TransitiveDependencyUsage(
                    artifactId, groupId, version, javaxPackage, severity, recommendation,
                    scope, false, 0, null, scanReason, msg, 0.0, false));
        }
        return usages;
    }

    private List<TransitiveDependencyUsage> toUsagesGradle(List<Map<String, String>> dependencies, String fallbackDetail) {
        List<TransitiveDependencyUsage> usages = new ArrayList<>();
        for (Map<String, String> dep : dependencies) {
            String groupId = dep.get("groupId");
            String artifactId = dep.get("artifactId");
            String version = dep.getOrDefault("version", "unknown");
            String scope = dep.getOrDefault("scope", "compile");
            String key = groupId + ":" + artifactId;

            Namespace ns = classify(groupId, artifactId);
            ScanReason scanReason = mapNamespaceToScanReason(ns);
            String severity = mapNamespaceToSeverity(ns);
            String recommendation = mapNamespaceToRecommendation(ns, groupId, artifactId);
            String javaxPackage = (ns == Namespace.JAVAX || ns == Namespace.MIXED) ? key : null;
            String msg = fallbackDetail != null ? fallbackDetail : detailMessage(ns, groupId, artifactId);

            usages.add(new TransitiveDependencyUsage(
                    artifactId, groupId, version, javaxPackage, severity, recommendation,
                    scope, false, 0, null, scanReason, msg, 0.0, false));
        }
        return usages;
    }

    private Namespace classify(String groupId, String artifactId) {
        if (namespaceClassifier == null) {
            return Namespace.UNKNOWN;
        }
        try {
            return namespaceClassifier.classify(new Artifact(groupId, artifactId, "unknown", "compile", false));
        } catch (Exception e) {
            log.warn("Namespace classification failed for {}:{}: {}", groupId, artifactId, e.getClass().getSimpleName() + ": " + e.getMessage());
            return Namespace.UNKNOWN;
        }
    }

    private ScanReason mapNamespaceToScanReason(Namespace ns) {
        return switch (ns) {
            case JAKARTA, JAVAX -> ns == Namespace.JAKARTA ? ScanReason.WHITELISTED : ScanReason.BLACKLISTED;
            case MIXED -> ScanReason.UNKNOWN;
            default -> ScanReason.UNKNOWN;
        };
    }

    private String mapNamespaceToSeverity(Namespace ns) {
        return switch (ns) {
            case JAKARTA -> "low";
            case JAVAX -> "high";
            case MIXED -> "medium";
            default -> "low";
        };
    }

    private String mapNamespaceToRecommendation(Namespace ns, String groupId, String artifactId) {
        return switch (ns) {
            case JAKARTA -> "Known Jakarta artifact: " + groupId + ":" + artifactId;
            case JAVAX -> "Jakarta migration required";
            case MIXED -> "Mixed namespace, review needed";
            default -> null;
        };
    }

    private String detailMessage(Namespace ns, String groupId, String artifactId) {
        return switch (ns) {
            case JAKARTA -> "Known Jakarta artifact: " + groupId + ":" + artifactId;
            case JAVAX -> "Uses javax namespace: " + groupId + ":" + artifactId;
            case MIXED -> "Mixed namespace usage: " + groupId + ":" + artifactId;
            default -> "Unclassified artifact: " + groupId + ":" + artifactId;
        };
    }

    /**
     * Scans a multi-module project by running the build tool command once from the root.
     * This avoids running the command N times (once per submodule) which can fail
     * for submodules that rely on the root project configuration.
     *
     * For Maven: runs mvn dependency:tree from root, distributes results per submodule
     * by matching groupId prefixes.
     * For Gradle: runs gradle dependencies from root, distributes results per submodule.
     */
    private List<TransitiveDependencyScanResult> scanMultiModuleProject(
            Path projectPath, List<Path> buildFiles, ScanProgressCallback listener) {

        // Determine build tool type from first build file
        Path firstFile = buildFiles.get(0);
        String firstName = firstFile.getFileName().toString().toLowerCase();
        boolean isMavenRoot = firstName.equals("pom.xml");

        log.info("Multi-module {} project detected, running from root: {}",
                 isMavenRoot ? "Maven" : "Gradle", projectPath);

        if (isMavenRoot) {
            return scanMultiModuleMaven(projectPath, buildFiles, listener);
        } else {
            return scanMultiModuleGradle(projectPath, buildFiles, listener);
        }
    }

    /**
     * Scans a multi-module Maven project by running mvn dependency:tree from the root.
     * The root command outputs all modules in a single JSON tree. All dependencies are
     * returned as a single result for the root build file — this ensures no dependencies
     * are missed due to imperfect module attribution.
     */
    private List<TransitiveDependencyScanResult> scanMultiModuleMaven(
            Path projectPath, List<Path> buildFiles, ScanProgressCallback listener) {

        Path rootPom = projectPath.resolve("pom.xml");
        List<TransitiveDependencyScanResult> results = new ArrayList<>();

        try {
            var future = commandExecutor.executeMavenDependencyTreeAsync(rootPom, MAVEN_SCOPES);
            var treeResult = future.get(DependencyTreeCommandExecutor.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!treeResult.isSuccess() || treeResult.getDependencies().isEmpty()) {
                log.warn("Root Maven command failed or returned empty, falling back to per-file scanning");
                return null; // signal fallback
            }

            // Run full enrichment pipeline on root result — all deps are attributed here
            ScanProgressCallback rootListener = listener != null ?
                (phase, completed, total) -> listener.onPhaseProgress("Root project — " + phase, completed, total) :
                null;
            results.add(convertTreeResult(rootPom, "Maven", treeResult, rootListener));

        } catch (Exception e) {
            log.warn("Multi-module Maven scan failed: {}, falling back to per-file scanning", e.getMessage());
            return null; // signal fallback
        }

        return results;
    }

    /**
     * Scans a multi-module Gradle project by running gradle dependencies from the root.
     * Falls back to per-file scanning if the root command fails.
     */
    private List<TransitiveDependencyScanResult> scanMultiModuleGradle(
            Path projectPath, List<Path> buildFiles, ScanProgressCallback listener) {

        Path rootBuildFile = buildFiles.get(0);

        List<TransitiveDependencyScanResult> results = new ArrayList<>();

        try {
            var future = commandExecutor.executeGradleDependenciesAsync(rootBuildFile, GRADLE_SCOPES);
            var treeResult = future.get(DependencyTreeCommandExecutor.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!treeResult.isSuccess() || treeResult.getDependencies().isEmpty()) {
                log.warn("Root Gradle command failed or returned empty, falling back to per-file scanning");
                return null;
            }

            List<DependencyTreeResult.DependencyNode> allDeps = treeResult.getDependencies();

            // For Gradle, the root command output is a flat list grouped by configuration.
            // We distribute deps by matching depth-0 artifacts against module names.
            Set<String> moduleNames = new HashSet<>();
            for (Path buildFile : buildFiles) {
                Path parent = buildFile.getParent();
                if (parent != null) {
                    moduleNames.add(parent.getFileName().toString());
                }
            }

            // Put all deps on the root build file result
            Path rootResultFile = rootBuildFile;
            DependencyTreeResult rootTreeResult = new DependencyTreeResult(allDeps, treeResult.getScopes());
            String rootModuleName = "Root project";
            ScanProgressCallback rootListener = listener != null ?
                (phase, completed, total) -> listener.onPhaseProgress(rootModuleName + " — " + phase, completed, total) :
                null;
            results.add(convertTreeResult(rootResultFile, "Gradle", rootTreeResult, rootListener));

        } catch (Exception e) {
            log.warn("Multi-module Gradle root scan failed: {}, falling back to per-file", e.getMessage());
            return null;
        }

        return results;
    }

    /**
     * Finds the root Gradle build file from a list of discovered build files.
     * Looks for the build file whose parent directory contains settings.gradle(.kts).
     */
    /**
     * Scans a single file with tracking for parallel processing.
     * Returns all dependencies, not just those with javax usage.
     */
    private TransitiveDependencyScanResult scanFileWithTracking(Path filePath, AtomicInteger totalScanned) {
        totalScanned.incrementAndGet();
        return scanFile(filePath);
    }
}
