package adrianmikula.jakartamigration.intellij.service;

import adrianmikula.jakartamigration.advancedscanning.domain.*;
import adrianmikula.jakartamigration.advancedscanning.service.AdvancedScanningModule;
import adrianmikula.jakartamigration.advancedscanning.service.*;
import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;
import adrianmikula.jakartamigration.coderefactoring.service.RecipeService;
import adrianmikula.jakartamigration.intellij.ui.ScanProgressListener;
import com.intellij.openapi.diagnostic.Logger;

import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service for performing advanced scanning using premium core engine.
 * This service provides access to the premium scanning features.
 */
public class AdvancedScanningService {
    private static final Logger LOG = Logger.getInstance(AdvancedScanningService.class);

    private final AdvancedScanningModule scanningModule;
    private final ThirdPartyLibScanner thirdPartyLibScanner;

    // Cache for the last scan results using SoftReference to prevent OOM
    private java.lang.ref.SoftReference<AdvancedScanSummary> cachedSummaryRef = new java.lang.ref.SoftReference<>(null);
    private Path cachedProjectPath;
    private long lastScanTime;

    private static final long CACHE_VALIDITY_MS = 5 * 60 * 1000; // 5 minutes
    
    // Memory optimization: Limit parallel scanning to prevent OOM
    private static final int MAX_PARALLEL_SCANS = 2; // Further reduced from 4 to 2 for large projects
    
    // Use a bounded thread pool to control memory usage
    private final java.util.concurrent.ExecutorService scanExecutor = java.util.concurrent.Executors
            .newFixedThreadPool(MAX_PARALLEL_SCANS);

    public AdvancedScanningService(RecipeService recipeService) {
        // Initialize scanning module
        this.scanningModule = new AdvancedScanningModule(recipeService);
        this.thirdPartyLibScanner = scanningModule.getThirdPartyLibScanner();

        LOG.info("AdvancedScanningService initialized with parallel scanning and memory optimizations");
    }

    /**
     * Scans a project for all advanced scanning types in parallel.
     * Results are cached for 5 minutes using SoftReferences.
     *
     * @param projectPath Path to the project root directory
     * @return AdvancedScanSummary containing combined results
     */
    public AdvancedScanSummary scanAll(Path projectPath) {
        return scanAll(projectPath, null);
    }

    /**
     * Scans a project for all advanced scanning types in parallel with progress reporting.
     * Results are cached for 5 minutes using SoftReferences.
     *
     * @param projectPath Path to the project root directory
     * @param progressListener Optional listener for progress updates
     * @return AdvancedScanSummary containing combined results
     */
    public AdvancedScanSummary scanAll(Path projectPath, ScanProgressListener progressListener) {
        LOG.info("=== Starting Advanced Scan ===");
        LOG.info("Project path: " + projectPath);
        
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long availableMemory = maxMemory - usedMemory;
        
        LOG.info("=== Memory Status ===");
        LOG.info("Max Memory: " + (maxMemory / 1024 / 1024) + "MB");
        LOG.info("Used Memory: " + (usedMemory / 1024 / 1024) + "MB");
        LOG.info("Available Memory: " + (availableMemory / 1024 / 1024) + "MB");
        
        AdvancedScanSummary existing = cachedSummaryRef.get();
        // Check if cached result is still valid
        if (existing != null && cachedProjectPath != null
                && cachedProjectPath.equals(projectPath)
                && (System.currentTimeMillis() - lastScanTime) < CACHE_VALIDITY_MS) {
            LOG.info("Returning cached scan results");
            return existing;
        }

        try {
            // Memory optimization: Limit parallel scans based on available memory
            int scansToRun = Math.min(MAX_PARALLEL_SCANS, (int) (availableMemory / (25 * 1024 * 1024))); // 25MB per scan (reduced from 50MB)
            if (scansToRun < MAX_PARALLEL_SCANS) {
                LOG.info("Reducing parallel scans from " + MAX_PARALLEL_SCANS + " to " + scansToRun + " due to memory constraints");
            }
            
            // If very low memory, run sequentially
            if (availableMemory < 100 * 1024 * 1024) { // Less than 100MB available
                LOG.info("Low memory detected, running scans sequentially");
                return runScansSequentially(projectPath, progressListener);
            }
            
            // Execute scans in batches to control memory usage
            java.util.List<CompletableFuture<?>> futures = new java.util.ArrayList<>();
            
            // Batch 1: Core scans
            LOG.info("=== Starting Batch 1: Core Scans ===");
            if (progressListener != null) {
                progressListener.onScanPhase("Advanced Scans (Batch 1/3)", 0, 3);
            }
            
            CompletableFuture<ProjectScanResult<FileScanResult<JpaAnnotationUsage>>> jpaFuture = CompletableFuture
                    .supplyAsync(() -> {
                        LOG.info("Starting JPA scan...");
                        ProjectScanResult<FileScanResult<JpaAnnotationUsage>> result = scanForJpaAnnotations(projectPath);
                        if (progressListener != null && result != null && !result.fileResults().isEmpty()) {
                            int totalFindings = result.fileResults().stream()
                                .mapToInt(fileScan -> fileScan.usages().size())
                                .sum();
                            progressListener.onSubScanComplete("JPA", totalFindings);
                        }
                        return result;
                    }, scanExecutor);
            CompletableFuture<ProjectScanResult<FileScanResult<JavaxUsage>>> bvFuture = CompletableFuture
                    .supplyAsync(() -> {
                        LOG.info("Starting Bean Validation scan...");
                        ProjectScanResult<FileScanResult<JavaxUsage>> result = scanForBeanValidation(projectPath);
                        if (progressListener != null && result != null && !result.fileResults().isEmpty()) {
                            int totalFindings = result.fileResults().stream()
                                .mapToInt(fileScan -> fileScan.usages().size())
                                .sum();
                            progressListener.onSubScanComplete("Bean Validation", totalFindings);
                        }
                        return result;
                    }, scanExecutor);
            CompletableFuture<ProjectScanResult<FileScanResult<ServletJspUsage>>> sjFuture = CompletableFuture
                    .supplyAsync(() -> {
                        LOG.info("Starting Servlet/JSP scan...");
                        ProjectScanResult<FileScanResult<ServletJspUsage>> result = scanForServletJsp(projectPath);
                        if (progressListener != null && result != null && !result.fileResults().isEmpty()) {
                            int totalFindings = result.fileResults().stream()
                                .mapToInt(fileScan -> fileScan.usages().size())
                                .sum();
                            progressListener.onSubScanComplete("Servlet/JSP", totalFindings);
                        }
                        return result;
                    }, scanExecutor);
            CompletableFuture<ProjectScanResult<FileScanResult<JavaxUsage>>> cdiFuture = CompletableFuture
                    .supplyAsync(() -> {
                        LOG.info("Starting CDI scan...");
                        ProjectScanResult<FileScanResult<JavaxUsage>> result = scanForCdiInjection(projectPath);
                        if (progressListener != null && result != null && !result.fileResults().isEmpty()) {
                            int totalFindings = result.fileResults().stream()
                                .mapToInt(fileScan -> fileScan.usages().size())
                                .sum();
                            progressListener.onSubScanComplete("CDI Injection", totalFindings);
                        }
                        return result;
                    }, scanExecutor);
            
            // Wait for first batch
            LOG.info("Waiting for Batch 1 to complete...");
            CompletableFuture.allOf(jpaFuture, bvFuture, sjFuture, cdiFuture).join();
            LOG.info("Batch 1 completed");
            
            if (progressListener != null) {
                progressListener.onScanPhase("Advanced Scans (Batch 2/3)", 1, 3);
            }
            
            // Batch 2: Additional scans
            CompletableFuture<ProjectScanResult<FileScanResult<BuildConfigUsage>>> bcFuture = CompletableFuture
                    .supplyAsync(() -> {
                        ProjectScanResult<FileScanResult<BuildConfigUsage>> result = scanForBuildConfig(projectPath);
                        if (progressListener != null && result != null && !result.fileResults().isEmpty()) {
                            int totalFindings = result.fileResults().stream()
                                .mapToInt(fileScan -> fileScan.usages().size())
                                .sum();
                            progressListener.onSubScanComplete("Build Config", totalFindings);
                        }
                        return result;
                    }, scanExecutor);
            CompletableFuture<ProjectScanResult<FileScanResult<JavaxUsage>>> rsFuture = CompletableFuture
                    .supplyAsync(() -> {
                        ProjectScanResult<FileScanResult<JavaxUsage>> result = scanForRestSoap(projectPath);
                        if (progressListener != null && result != null && !result.fileResults().isEmpty()) {
                            int totalFindings = result.fileResults().stream()
                                .mapToInt(fileScan -> fileScan.usages().size())
                                .sum();
                            progressListener.onSubScanComplete("REST/SOAP", totalFindings);
                        }
                        return result;
                    }, scanExecutor);
            CompletableFuture<DeprecatedApiProjectScanResult> daFuture = CompletableFuture
                    .supplyAsync(() -> scanForDeprecatedApi(projectPath), scanExecutor);
            CompletableFuture<SecurityApiProjectScanResult> saFuture = CompletableFuture
                    .supplyAsync(() -> scanForSecurityApi(projectPath), scanExecutor);
            
            // Wait for second batch
            CompletableFuture.allOf(bcFuture, rsFuture, daFuture, saFuture).join();
            LOG.info("Batch 2 completed");
            
            if (progressListener != null) {
                progressListener.onScanPhase("Advanced Scans (Batch 3/3)", 2, 3);
            }
            
            // Batch 3: Final scans
            CompletableFuture<JmsMessagingProjectScanResult> jmFuture = CompletableFuture
                    .supplyAsync(() -> scanForJmsMessaging(projectPath), scanExecutor);
            CompletableFuture<TransitiveDependencyProjectScanResult> tdFuture = CompletableFuture
                    .supplyAsync(() -> scanForTransitiveDependencies(projectPath), scanExecutor);
            CompletableFuture<ConfigFileProjectScanResult> cfFuture = CompletableFuture
                    .supplyAsync(() -> scanForConfigFiles(projectPath), scanExecutor);
            CompletableFuture<ClassloaderModuleProjectScanResult> clFuture = CompletableFuture
                    .supplyAsync(() -> scanForClassloaderModule(projectPath), scanExecutor);
            CompletableFuture<LoggingMetricsProjectScanResult> lmFuture = CompletableFuture
                    .supplyAsync(() -> scanForLoggingMetrics(projectPath), scanExecutor);
            CompletableFuture<SerializationCacheProjectScanResult> scFuture = CompletableFuture
                    .supplyAsync(() -> scanForSerializationCache(projectPath), scanExecutor);
            CompletableFuture<ReflectionUsageProjectScanResult> ruFuture = CompletableFuture
                    .supplyAsync(() -> scanForReflectionUsage(projectPath), scanExecutor);
            CompletableFuture<ThirdPartyLibProjectScanResult> tpFuture = CompletableFuture
                    .supplyAsync(() -> scanForThirdPartyLib(projectPath), scanExecutor);

            // Wait for final batch
            CompletableFuture.allOf(jmFuture, tdFuture, cfFuture, clFuture, lmFuture, scFuture, ruFuture, tpFuture).join();
            LOG.info("Batch 3 completed");

            // Get individual scan results
            ProjectScanResult<FileScanResult<JpaAnnotationUsage>> jpaResult = jpaFuture.join();
            ProjectScanResult<FileScanResult<JavaxUsage>> beanValidationResult = bvFuture.join();
            ProjectScanResult<FileScanResult<ServletJspUsage>> servletJspResult = sjFuture.join();
            ProjectScanResult<FileScanResult<JavaxUsage>> cdiInjectionResult = cdiFuture.join();
            ProjectScanResult<FileScanResult<BuildConfigUsage>> buildConfigResult = bcFuture.join();
            ProjectScanResult<FileScanResult<JavaxUsage>> restSoapResult = rsFuture.join();
            DeprecatedApiProjectScanResult deprecatedApiResult = daFuture.join();
            SecurityApiProjectScanResult securityApiResult = saFuture.join();
            JmsMessagingProjectScanResult jmsMessagingResult = jmFuture.join();
            TransitiveDependencyProjectScanResult transitiveDependencyResult = tdFuture.join();
            ConfigFileProjectScanResult configFileResult = cfFuture.join();
            ClassloaderModuleProjectScanResult classloaderModuleResult = clFuture.join();
            LoggingMetricsProjectScanResult loggingMetricsResult = lmFuture.join();
            SerializationCacheProjectScanResult serializationCacheResult = scFuture.join();
            ReflectionUsageProjectScanResult reflectionUsageResult = ruFuture.join();
            ThirdPartyLibProjectScanResult thirdPartyLibResult = tpFuture.join();

            AdvancedScanSummary summary = new AdvancedScanSummary(
                    jpaResult,
                    beanValidationResult,
                    servletJspResult,
                    cdiInjectionResult,
                    buildConfigResult,
                    restSoapResult,
                    deprecatedApiResult,
                    securityApiResult,
                    jmsMessagingResult,
                    transitiveDependencyResult,
                    configFileResult,
                    classloaderModuleResult,
                    loggingMetricsResult,
                    serializationCacheResult,
                    thirdPartyLibResult);

            cachedSummaryRef = new java.lang.ref.SoftReference<>(summary);
            cachedProjectPath = projectPath;
            lastScanTime = System.currentTimeMillis();

            return summary;
        } catch (Exception e) {
            LOG.error("Parallel scan failed", e);
            throw new RuntimeException("Advanced scan failed", e);
        }
    }

    /**
     * Gets the cached scan summary if available.
     * 
     * @return Cached AdvancedScanSummary or null if no scan has been run or memory
     *         was reclaimed
     */
    public AdvancedScanSummary getCachedSummary() {
        return cachedSummaryRef.get();
    }

    /**
     * Sets the cached summary manually (primarily for testing).
     * 
     * @param summary The summary to cache
     */
    public void setCachedSummary(AdvancedScanSummary summary) {
        this.cachedSummaryRef = new java.lang.ref.SoftReference<>(summary);
    }

    /**
     * Gets the last comprehensive scan results.
     * 
     * @return ComprehensiveScanResults or null if no scan has been run
     */
    public ComprehensiveScanResults getLastScanResults() {
        AdvancedScanSummary summary = getCachedSummary();
        if (summary == null) {
            return null;
        }

        // Convert AdvancedScanSummary to ComprehensiveScanResults
        // Create placeholder maps for different scan types
        Map<String, Object> jpaResults = Map.of("issues", summary.getJpaCount());
        Map<String, Object> beanValidationResults = Map.of("issues", summary.getBeanValidationCount());
        Map<String, Object> cdiResults = Map.of("issues", summary.getCdiInjectionCount());
        Map<String, Object> servletJspResults = Map.of("issues", summary.getServletJspCount());
        Map<String, Object> thirdPartyLibResults = Map.of("issues", summary.getThirdPartyLibCount());
        Map<String, Object> transitiveDependencyResults = Map.of("issues", summary.getTransitiveDependencyCount());
        Map<String, Object> buildConfigResults = Map.of("issues", summary.getBuildConfigCount());
        
        List<String> recommendations = List.of(
            "Update javax dependencies to jakarta equivalents",
            "Review deprecated API usage",
            "Update configuration files"
        );

        // Create ScanSummary
        ComprehensiveScanResults.ScanSummary scanSummary = new ComprehensiveScanResults.ScanSummary(
            1000, // totalFilesScanned - placeholder
            summary.getTotalIssuesFound(), // filesWithIssues
            summary.getDeprecatedApiCount(), // criticalIssues
            summary.getSecurityApiCount(), // warningIssues
            0, // infoIssues
            75.0 // readinessScore - placeholder
        );

        return new ComprehensiveScanResults(
            cachedProjectPath != null ? cachedProjectPath.toString() : "unknown",
            java.time.LocalDateTime.now(), // scanTime
            jpaResults,
            beanValidationResults,
            cdiResults,
            servletJspResults,
            thirdPartyLibResults,
            transitiveDependencyResults,
            buildConfigResults,
            recommendations,
            summary.getTotalIssuesFound(), // totalIssuesFound
            scanSummary
        );
    }

    /**
     * Returns whether a scan has been performed and cached.
     * 
     * @return true if valid cached results exist
     */
    public boolean hasCachedResults() {
        return cachedSummaryRef.get() != null;
    }

    /**
     * Runs scans sequentially for low memory situations using optimized resource management.
     */
    private AdvancedScanSummary runScansSequentially(Path projectPath, ScanProgressListener progressListener) {
        LOG.info("Running scans sequentially to conserve memory");
        
        try {
            // Report progress for sequential scans
            if (progressListener != null) {
                progressListener.onScanPhase("Advanced Scans (Sequential)", 0, 1);
            }
            
            // Use try-with-resources for automatic resource management
            ProjectScanResult<FileScanResult<JpaAnnotationUsage>> jpaResult = scanForJpaAnnotations(projectPath);
            if (progressListener != null && jpaResult != null && !jpaResult.fileResults().isEmpty()) {
                int totalFindings = jpaResult.fileResults().stream()
                    .mapToInt(fileScan -> fileScan.usages().size())
                    .sum();
                progressListener.onSubScanComplete("JPA", totalFindings);
            }
            
            ProjectScanResult<FileScanResult<JavaxUsage>> beanValidationResult = scanForBeanValidation(projectPath);
            if (progressListener != null && beanValidationResult != null && !beanValidationResult.fileResults().isEmpty()) {
                int totalFindings = beanValidationResult.fileResults().stream()
                    .mapToInt(fileScan -> fileScan.usages().size())
                    .sum();
                progressListener.onSubScanComplete("Bean Validation", totalFindings);
            }
            
            ProjectScanResult<FileScanResult<ServletJspUsage>> servletJspResult = scanForServletJsp(projectPath);
            if (progressListener != null && servletJspResult != null && !servletJspResult.fileResults().isEmpty()) {
                int totalFindings = servletJspResult.fileResults().stream()
                    .mapToInt(fileScan -> fileScan.usages().size())
                    .sum();
                progressListener.onSubScanComplete("Servlet/JSP", totalFindings);
            }
            
            ProjectScanResult<FileScanResult<JavaxUsage>> cdiInjectionResult = scanForCdiInjection(projectPath);
            if (progressListener != null && cdiInjectionResult != null && !cdiInjectionResult.fileResults().isEmpty()) {
                int totalFindings = cdiInjectionResult.fileResults().stream()
                    .mapToInt(fileScan -> fileScan.usages().size())
                    .sum();
                progressListener.onSubScanComplete("CDI Injection", totalFindings);
            }
            
            ProjectScanResult<FileScanResult<BuildConfigUsage>> buildConfigResult = scanForBuildConfig(projectPath);
            if (progressListener != null && buildConfigResult != null && !buildConfigResult.fileResults().isEmpty()) {
                int totalFindings = buildConfigResult.fileResults().stream()
                    .mapToInt(fileScan -> fileScan.usages().size())
                    .sum();
                progressListener.onSubScanComplete("Build Config", totalFindings);
            }
            
            ProjectScanResult<FileScanResult<JavaxUsage>> restSoapResult = scanForRestSoap(projectPath);
            if (progressListener != null && restSoapResult != null && !restSoapResult.fileResults().isEmpty()) {
                int totalFindings = restSoapResult.fileResults().stream()
                    .mapToInt(fileScan -> fileScan.usages().size())
                    .sum();
                progressListener.onSubScanComplete("REST/SOAP", totalFindings);
            }
            
            DeprecatedApiProjectScanResult deprecatedApiResult = scanForDeprecatedApi(projectPath);
            SecurityApiProjectScanResult securityApiResult = scanForSecurityApi(projectPath);
            JmsMessagingProjectScanResult jmsMessagingResult = scanForJmsMessaging(projectPath);
            TransitiveDependencyProjectScanResult transitiveDependencyResult = scanForTransitiveDependencies(projectPath);
            ConfigFileProjectScanResult configFileResult = scanForConfigFiles(projectPath);
            ClassloaderModuleProjectScanResult classloaderModuleResult = scanForClassloaderModule(projectPath);
            LoggingMetricsProjectScanResult loggingMetricsResult = scanForLoggingMetrics(projectPath);
            SerializationCacheProjectScanResult serializationCacheResult = scanForSerializationCache(projectPath);
            ThirdPartyLibProjectScanResult thirdPartyLibResult = scanForThirdPartyLib(projectPath);
            
            // Create summary with all results
            AdvancedScanSummary summary = new AdvancedScanSummary(
                jpaResult,
                beanValidationResult,
                servletJspResult,
                cdiInjectionResult,
                buildConfigResult,
                restSoapResult,
                deprecatedApiResult,
                securityApiResult,
                jmsMessagingResult,
                transitiveDependencyResult,
                configFileResult,
                classloaderModuleResult,
                loggingMetricsResult,
                serializationCacheResult,
                thirdPartyLibResult
            );
            
            // Cache the results
            cachedSummaryRef = new java.lang.ref.SoftReference<>(summary);
            cachedProjectPath = projectPath;
            lastScanTime = System.currentTimeMillis();

            return summary;
        } catch (Exception e) {
            LOG.error("Sequential scan failed", e);
            throw new RuntimeException("Advanced scan failed", e);
        }
    }

    // Individual scan methods for each scanner type
    public ProjectScanResult<FileScanResult<JpaAnnotationUsage>> scanForJpaAnnotations(Path projectPath) {
        LOG.info("Scanning for JPA annotations in: " + projectPath);
        return scanningModule.getJpaAnnotationScanner().scanProject(projectPath);
    }

    public ProjectScanResult<FileScanResult<JavaxUsage>> scanForBeanValidation(Path projectPath) {
        LOG.info("Scanning for Bean Validation in: " + projectPath);
        return scanningModule.getBeanValidationScanner().scanProject(projectPath);
    }

    public ProjectScanResult<FileScanResult<ServletJspUsage>> scanForServletJsp(Path projectPath) {
        LOG.info("Scanning for Servlet/JSP in: " + projectPath);
        return scanningModule.getServletJspScanner().scanProject(projectPath);
    }

    public ProjectScanResult<FileScanResult<JavaxUsage>> scanForCdiInjection(Path projectPath) {
        LOG.info("Scanning for CDI Injection in: " + projectPath);
        return scanningModule.getCdiInjectionScanner().scanProject(projectPath);
    }

    public ProjectScanResult<FileScanResult<BuildConfigUsage>> scanForBuildConfig(Path projectPath) {
        LOG.info("Scanning for Build Config in: " + projectPath);
        return scanningModule.getBuildConfigScanner().scanProject(projectPath);
    }

    public ProjectScanResult<FileScanResult<JavaxUsage>> scanForRestSoap(Path projectPath) {
        LOG.info("Scanning for REST/SOAP in: " + projectPath);
        return scanningModule.getRestSoapScanner().scanProject(projectPath);
    }

    public DeprecatedApiProjectScanResult scanForDeprecatedApi(Path projectPath) {
        LOG.info("Scanning for Deprecated API in: " + projectPath);
        return scanningModule.getDeprecatedApiScanner().scanProject(projectPath);
    }

    public SecurityApiProjectScanResult scanForSecurityApi(Path projectPath) {
        LOG.info("Scanning for Security API in: " + projectPath);
        return scanningModule.getSecurityApiScanner().scanProject(projectPath);
    }

    public JmsMessagingProjectScanResult scanForJmsMessaging(Path projectPath) {
        LOG.info("Scanning for JMS Messaging in: " + projectPath);
        return scanningModule.getJmsMessagingScanner().scanProject(projectPath);
    }

    public TransitiveDependencyProjectScanResult scanForTransitiveDependencies(Path projectPath) {
        LOG.info("Scanning for Transitive Dependencies in: " + projectPath);
        TransitiveDependencyProjectScanResult result = scanningModule.getTransitiveDependencyScanner().scanProject(projectPath);
        int fileCount = result.getFileResults().size();
        int totalUsages = result.getFileResults().stream().mapToInt(r -> r.getUsages().size()).sum();
        LOG.info("[DEBUG] Scan result: " + fileCount + " files, " + totalUsages + " total usages");
        for (TransitiveDependencyScanResult fileResult : result.getFileResults()) {
            LOG.info("[DEBUG] File result: " + fileResult.toString());
        }
        return result;
    }

    /**
     * Checks if Maven is available on the system for deep dependency scanning.
     * @return true if 'mvn --version' executes successfully
     */
    public boolean isMavenAvailable() {
        return adrianmikula.jakartamigration.advancedscanning.service.impl.DependencyTreeCommandExecutorImpl.isMavenAvailable();
    }

    /**
     * Checks if Gradle is available on the system for deep dependency scanning.
     * @return true if 'gradle --version' executes successfully
     */
    public boolean isGradleAvailable() {
        return adrianmikula.jakartamigration.advancedscanning.service.impl.DependencyTreeCommandExecutorImpl.isGradleAvailable();
    }

    /**
     * Runs deep dependency scanning for a project using Maven/Gradle commands.
     * This provides multi-level transitive dependency resolution.
     * Falls back to basic analysis if Maven/Gradle not available.
     *
     * @param projectPath Path to the project root
     * @return Deep dependency scan result with multi-level transitive dependencies
     */
    public TransitiveDependencyProjectScanResult scanDependenciesDeep(Path projectPath) {
        LOG.info("Running deep dependency scan for: " + projectPath);

        // Check if Maven or Gradle is available
        boolean mavenAvailable = isMavenAvailable();
        boolean gradleAvailable = isGradleAvailable();

        LOG.info("Maven available: " + mavenAvailable + ", Gradle available: " + gradleAvailable);

        if (!mavenAvailable && !gradleAvailable) {
            LOG.warn("Neither Maven nor Gradle available for deep scanning");
            return null; // Signal to use basic analysis fallback
        }

        // Run the deep transitive dependency scan
        return scanForTransitiveDependencies(projectPath);
    }

    /**
     * Converts deep transitive dependency scan results to DependencyInfo objects.
     * This merges results from all scanned build files and flattens them into a single list.
     *
     * @param result The deep transitive dependency scan result
     * @return List of DependencyInfo with depth and scope information
     */
    public List<DependencyInfo> convertToDependencyInfo(TransitiveDependencyProjectScanResult result) {
        if (result == null || result.getFileResults() == null) {
            return new ArrayList<>();
        }

        List<DependencyInfo> dependencyInfos = new ArrayList<>();

        // Flatten all usages from all scanned files
        for (TransitiveDependencyScanResult fileResult : result.getFileResults()) {
            if (fileResult == null || fileResult.getUsages() == null) {
                continue;
            }

            for (TransitiveDependencyUsage usage : fileResult.getUsages()) {
                DependencyInfo info = new DependencyInfo(
                    usage.getGroupId(),
                    usage.getArtifactId(),
                    usage.getVersion(),
                    null, // recommendedGroupId - will be set by recommendation logic
                    null, // recommendedArtifactId
                    null, // recommendedVersion
                    usage.getJavaxPackage(), // jakartaCompatibilityStatus - store the javax package info
                    usage.getRecommendation(), // associatedRecipeName - store the recommendation
                    mapSeverityToStatus(usage.getSeverity()),
                    usage.isTransitive(),
                    false, // isOrganizational - determined elsewhere
                    usage.getDepth(),
                    usage.getScope()
                );
                dependencyInfos.add(info);
            }
        }

        LOG.info("Converted " + dependencyInfos.size() + " transitive dependencies to DependencyInfo");
        return dependencyInfos;
    }

    /**
     * Maps severity string to DependencyMigrationStatus.
     */
    private DependencyMigrationStatus mapSeverityToStatus(String severity) {
        if (severity == null) {
            return DependencyMigrationStatus.UNKNOWN_REVIEW;
        }
        return switch (severity.toLowerCase()) {
            case "high" -> DependencyMigrationStatus.NEEDS_UPGRADE;
            case "medium" -> DependencyMigrationStatus.NEEDS_UPGRADE;
            case "low" -> DependencyMigrationStatus.COMPATIBLE;
            default -> DependencyMigrationStatus.UNKNOWN_REVIEW;
        };
    }

    public ConfigFileProjectScanResult scanForConfigFiles(Path projectPath) {
        LOG.info("Scanning for Config Files in: " + projectPath);
        return scanningModule.getConfigFileScanner().scanProject(projectPath);
    }

    public ClassloaderModuleProjectScanResult scanForClassloaderModule(Path projectPath) {
        LOG.info("Scanning for Classloader/Module in: " + projectPath);
        return scanningModule.getClassloaderModuleScanner().scanProject(projectPath);
    }

    public LoggingMetricsProjectScanResult scanForLoggingMetrics(Path projectPath) {
        LOG.info("Scanning for Logging/Metrics in: " + projectPath);
        return scanningModule.getLoggingMetricsScanner().scanProject(projectPath);
    }

    public SerializationCacheProjectScanResult scanForSerializationCache(Path projectPath) {
        LOG.info("Scanning for Serialization/Cache in: " + projectPath);
        return scanningModule.getSerializationCacheScanner().scanProject(projectPath);
    }

    public ReflectionUsageProjectScanResult scanForReflectionUsage(Path projectPath) {
        LOG.info("Scanning for Reflection Usage in: " + projectPath);
        return scanningModule.getReflectionUsageScanner().scanProject(projectPath);
    }

    public ThirdPartyLibProjectScanResult scanForThirdPartyLib(Path projectPath) {
        LOG.info("Scanning for Third-Party Libs in: " + projectPath);
        return thirdPartyLibScanner.scanProject(projectPath);
    }

    /**
     * Summary of all advanced scanning results.
     */
    public record AdvancedScanSummary(
            ProjectScanResult<FileScanResult<JpaAnnotationUsage>> jpaResult,
            ProjectScanResult<FileScanResult<JavaxUsage>> beanValidationResult,
            ProjectScanResult<FileScanResult<ServletJspUsage>> servletJspResult,
            ProjectScanResult<FileScanResult<JavaxUsage>> cdiInjectionResult,
            ProjectScanResult<FileScanResult<BuildConfigUsage>> buildConfigResult,
            ProjectScanResult<FileScanResult<JavaxUsage>> restSoapResult,
            DeprecatedApiProjectScanResult deprecatedApiResult,
            SecurityApiProjectScanResult securityApiResult,
            JmsMessagingProjectScanResult jmsMessagingResult,
            TransitiveDependencyProjectScanResult transitiveDependencyResult,
            ConfigFileProjectScanResult configFileResult,
            ClassloaderModuleProjectScanResult classloaderModuleResult,
            LoggingMetricsProjectScanResult loggingMetricsResult,
            SerializationCacheProjectScanResult serializationCacheResult,
            ThirdPartyLibProjectScanResult thirdPartyLibResult) {
        /**
         * Returns individual count for JPA annotations.
         */
        public int getJpaCount() {
            return jpaResult != null ? jpaResult.totalIssuesFound() : 0;
        }

        /**
         * Returns individual count for Bean Validation.
         */
        public int getBeanValidationCount() {
            return beanValidationResult != null ? beanValidationResult.totalIssuesFound() : 0;
        }

        /**
         * Returns individual count for Servlet/JSP.
         */
        public int getServletJspCount() {
            return servletJspResult != null ? servletJspResult.totalIssuesFound() : 0;
        }

        /**
         * Returns individual count for CDI Injection.
         */
        public int getCdiInjectionCount() {
            return cdiInjectionResult != null ? cdiInjectionResult.totalIssuesFound() : 0;
        }

        /**
         * Returns individual count for Build Config.
         */
        public int getBuildConfigCount() {
            return buildConfigResult != null ? buildConfigResult.totalIssuesFound() : 0;
        }

        /**
         * Returns individual count for REST/SOAP.
         */
        public int getRestSoapCount() {
            return restSoapResult != null ? restSoapResult.totalIssuesFound() : 0;
        }

        /**
         * Returns individual count for Deprecated API.
         */
        public int getDeprecatedApiCount() {
            return deprecatedApiResult != null ? deprecatedApiResult.totalUsagesFound() : 0;
        }

        /**
         * Returns individual count for Security API.
         */
        public int getSecurityApiCount() {
            return securityApiResult != null ? securityApiResult.getTotalJavaxUsages() : 0;
        }

        /**
         * Returns individual count for JMS Messaging.
         */
        public int getJmsMessagingCount() {
            return jmsMessagingResult != null ? jmsMessagingResult.getTotalJavaxUsages() : 0;
        }

        /**
         * Returns individual count for Transitive Dependencies.
         */
        public int getTransitiveDependencyCount() {
            return transitiveDependencyResult != null ? transitiveDependencyResult.getTotalJavaxDependencies() : 0;
        }

        /**
         * Returns individual count for Config Files.
         */
        public int getConfigFileCount() {
            return configFileResult != null ? configFileResult.getTotalJavaxUsages() : 0;
        }

        /**
         * Returns individual count for Classloader/Module.
         */
        public int getClassloaderModuleCount() {
            return classloaderModuleResult != null ? classloaderModuleResult.getTotalJavaxUsages() : 0;
        }

        /**
         * Returns individual count for Logging/Metrics.
         */
        public int getLoggingMetricsCount() {
            return loggingMetricsResult != null ? loggingMetricsResult.getTotalFindings() : 0;
        }

        /**
         * Returns individual count for Serialization/Cache.
         */
        public int getSerializationCacheCount() {
            return serializationCacheResult != null ? serializationCacheResult.getTotalFindings() : 0;
        }

        /**
         * Returns individual count for Third-Party Libs.
         */
        public int getThirdPartyLibCount() {
            return thirdPartyLibResult != null ? thirdPartyLibResult.getTotalLibraries() : 0;
        }

        /**
         * Returns the total number of issues found across all scans.
         */
        public int getTotalIssuesFound() {
            int total = 0;
            if (jpaResult != null) {
                total += jpaResult.totalIssuesFound();
            }
            if (beanValidationResult != null) {
                total += beanValidationResult.totalIssuesFound();
            }
            if (servletJspResult != null) {
                total += servletJspResult.totalIssuesFound();
            }
            if (cdiInjectionResult != null) {
                total += cdiInjectionResult.totalIssuesFound();
            }
            if (buildConfigResult != null) {
                total += buildConfigResult.totalIssuesFound();
            }
            if (restSoapResult != null) {
                total += restSoapResult.totalIssuesFound();
            }
            if (deprecatedApiResult != null) {
                total += deprecatedApiResult.totalUsagesFound();
            }
            if (securityApiResult != null) {
                total += securityApiResult.getTotalJavaxUsages();
            }
            if (jmsMessagingResult != null) {
                total += jmsMessagingResult.getTotalJavaxUsages();
            }
            if (transitiveDependencyResult != null) {
                total += transitiveDependencyResult.getTotalJavaxDependencies();
            }
            if (configFileResult != null) {
                total += configFileResult.getTotalJavaxUsages();
            }
            if (classloaderModuleResult != null) {
                total += classloaderModuleResult.getTotalJavaxUsages();
            }
            if (loggingMetricsResult != null) {
                total += loggingMetricsResult.getTotalFindings();
            }
            if (serializationCacheResult != null) {
                total += serializationCacheResult.getTotalFindings();
            }
            if (thirdPartyLibResult != null) {
                total += thirdPartyLibResult.getTotalLibraries();
            }
            return total;
        }

        /**
         * Returns true if any issues were found.
         */
        public boolean hasIssues() {
            return getTotalIssuesFound() > 0;
        }
    }
}
