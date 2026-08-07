package adrianmikula.jakartamigration.intellij.service;

import adrianmikula.jakartamigration.advancedscanning.domain.*;
import adrianmikula.jakartamigration.advancedscanning.service.*;
import adrianmikula.jakartamigration.advancedscanning.service.impl.DependencyTreeCommandExecutorImpl;
import adrianmikula.jakartamigration.coderefactoring.service.RecipeService;
import adrianmikula.jakartamigration.intellij.ui.ScanProgressListener;
import adrianmikula.jakartamigration.intellij.ui.ThrottledProgressListener;
import adrianmikula.jakartamigration.intellij.ui.DependencyStatusColors;
import adrianmikula.jakartamigration.intellij.util.NotificationHelper;
import adrianmikula.jakartamigration.util.ProjectFileSystemScanner;
import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Dependency;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.advancedscanning.service.ScanRecipeRecommendationService;
import adrianmikula.jakartamigration.scanning.BalloonNotificationService;
import adrianmikula.jakartamigration.scanning.orchestration.AdvancedScanningEngine;
import adrianmikula.jakartamigration.scanning.orchestration.ScanMode;
import com.intellij.openapi.diagnostic.Logger;

import java.nio.file.Path;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.intellij.openapi.project.Project;

/**
 * Service for performing advanced scanning using premium core engine.
 * This service provides access to the premium scanning features.
 */
public class AdvancedScanningService implements AdvancedScanningEngine {
    private static final Logger LOG = Logger.getInstance(AdvancedScanningService.class);

    private final AdvancedScanningModule scanningModule;
    private final ThirdPartyLibScanner thirdPartyLibScanner;
    private final Project project;

    // Cache for the last scan results using SoftReference to prevent OOM
    private java.lang.ref.SoftReference<AdvancedScanSummary> cachedSummaryRef = new java.lang.ref.SoftReference<>(null);
    private Path cachedProjectPath;
    private long lastScanTime;

    /**
     * Bridges a ScanProgressListener (UI layer) to a ScanProgressCallback (core engine layer).
     * Both have the same (phase, completed, total) signature, so this is a simple delegation.
     */
    static ScanProgressCallback toScanProgressCallback(ScanProgressListener listener) {
        if (listener == null) return null;
        return (phase, completed, total) -> listener.onScanPhase(phase, completed, total);
    }

    private static ScanProgressListener toScanProgressListener(ScanProgressCallback callback) {
        if (callback == null) return null;
        return new ScanProgressListener() {
            @Override
            public void onScanPhase(String phase, int completed, int total) {
                callback.onPhaseProgress(phase, completed, total);
            }

            @Override
            public void onScanComplete() {
                // No-op
            }

            @Override
            public void onScanError(Exception error) {
                // No-op
            }

            @Override
            public void onScanPartial() {
                // No-op
            }

            @Override
            public void onSubScanComplete(String scanType, int resultCount) {
                // No-op
            }
        };
    }

    @Override
    public adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults runAdvancedScans(Path projectPath, ScanMode mode, ScanProgressCallback progressCallback) {
        ScanProgressListener listener = toScanProgressListener(progressCallback);
        if (mode == ScanMode.QUICK) {
            scanAllExcludingTransitive(projectPath, listener);
        } else {
            scanAll(projectPath, listener);
        }
        return getLastScanResults();
    }

    private static final long CACHE_VALIDITY_MS = 5 * 60 * 1000; // 5 minutes
    
    // Memory optimization: Limit parallel scanning to prevent OOM
    private static final int MAX_PARALLEL_SCANS = 2;
    
    // Discovery + the 16 individual advanced scans (used for progress bar updates)
    private static final int TOTAL_ADVANCED_SCAN_STEPS = 17;
    
    // Use a bounded thread pool to control memory usage
    private final java.util.concurrent.ExecutorService scanExecutor = java.util.concurrent.Executors
            .newFixedThreadPool(MAX_PARALLEL_SCANS);
    
    // Deduplication for build tool error notifications (project path -> last notification time)
    private static final Map<String, Long> buildToolNotificationTimestamps = new ConcurrentHashMap<>();
    private static final long NOTIFICATION_DEDUPLICATION_MS = 5 * 60 * 1000; // 5 minutes

    public AdvancedScanningService(RecipeService recipeService, Project project) {
        // Wire IntelliJ notification balloon into the core engine
        BalloonNotificationService balloonService = new BalloonNotificationService((title, message) -> {
            if (project != null) {
                NotificationHelper.showWarning(project, title, message);
            }
        });
        this.scanningModule = new AdvancedScanningModule(recipeService, balloonService);
        this.thirdPartyLibScanner = scanningModule.getThirdPartyLibScanner();
        this.project = project;

        LOG.info("AdvancedScanningService initialized with parallel scanning and memory optimizations");
    }
    
    /**
     * Legacy constructor for backward compatibility.
     * @deprecated Use the constructor with Project parameter for proper notification support.
     */
    @Deprecated
    public AdvancedScanningService(RecipeService recipeService) {
        this(recipeService, null);
    }

    /**
     * Checks if Maven or Gradle is available on the system.
     * @return true if at least one build tool is available
     */
    private boolean isBuildToolAvailable() {
        boolean mavenAvailable = DependencyTreeCommandExecutorImpl.isMavenAvailable();
        boolean gradleAvailable = DependencyTreeCommandExecutorImpl.isGradleAvailable();
        return mavenAvailable || gradleAvailable;
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
        
        // Wrap progress listener with throttled wrapper to prevent EDT flooding
        ThrottledProgressListener throttledListener = null;
        if (progressListener != null) {
            throttledListener = new ThrottledProgressListener(progressListener);
            LOG.info("Progress listener wrapped with ThrottledProgressListener");
        }
        
        try {
            return scanAllInternal(projectPath, throttledListener);
        } finally {
            // Clean up throttled listener
            if (throttledListener != null) {
                throttledListener.shutdown();
            }
        }
    }
    
    /**
     * Internal implementation of scanAll that uses a potentially throttled listener.
     */
    private AdvancedScanSummary scanAllInternal(Path projectPath, ScanProgressListener progressListener) {
        
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long availableMemory = maxMemory - usedMemory;
        
        LOG.info("=== Memory Status ===");
        LOG.info("Max Memory: " + (maxMemory / 1024 / 1024) + "MB");
        LOG.info("Used Memory: " + (usedMemory / 1024 / 1024) + "MB");
        LOG.info("Available Memory: " + (availableMemory / 1024 / 1024) + "MB");
        
        AdvancedScanSummary existing = cachedSummaryRef.get();
        if (existing != null && cachedProjectPath != null
                && cachedProjectPath.equals(projectPath)
                && (System.currentTimeMillis() - lastScanTime) < CACHE_VALIDITY_MS) {
            LOG.info("Returning cached scan results");
            return existing;
        }

        try {
            // Memory optimization: Limit parallel scans
            int scansToRun = Math.min(MAX_PARALLEL_SCANS, (int) (availableMemory / (25 * 1024 * 1024)));
            if (scansToRun < MAX_PARALLEL_SCANS) {
                LOG.info("Reducing parallel scans from " + MAX_PARALLEL_SCANS + " to " + scansToRun + " due to memory constraints");
            }
            
            if (availableMemory < 100 * 1024 * 1024) {
                LOG.info("Low memory detected, running scans sequentially");
                return runScansSequentially(projectPath, progressListener);
            }
            
            // Discover all files once per category
            java.util.concurrent.atomic.AtomicInteger completedSteps = new java.util.concurrent.atomic.AtomicInteger(0);
            if (progressListener != null) {
                progressListener.onScanPhase("Advanced Scans", completedSteps.get(), TOTAL_ADVANCED_SCAN_STEPS);
            }
            
            Map<FileCategory, List<Path>> allFiles = discoverAllFilesOnce(projectPath);
            if (progressListener != null) {
                progressListener.onScanPhase("Advanced Scans", completedSteps.incrementAndGet(), TOTAL_ADVANCED_SCAN_STEPS);
            }
            
            java.util.List<CompletableFuture<?>> futures = new java.util.ArrayList<>();
            
            LOG.info("=== Starting Batch 1: Core Scans ===");
            // Progress updated after batch 1 completes
            
            CompletableFuture<ProjectScanResult<FileScanResult<JpaAnnotationUsage>>> jpaFuture = CompletableFuture
                    .supplyAsync(() -> {
                        LOG.info("Starting JPA scan...");
                        ProjectScanResult<FileScanResult<JpaAnnotationUsage>> result = scanForJpaAnnotations(allFiles.get(FileCategory.JAVA));
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
                        ProjectScanResult<FileScanResult<JavaxUsage>> result = scanForBeanValidation(allFiles.get(FileCategory.JAVA));
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
                        ProjectScanResult<FileScanResult<ServletJspUsage>> result = scanForServletJsp(allFiles.get(FileCategory.JAVA));
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
                        ProjectScanResult<FileScanResult<JavaxUsage>> result = scanForCdiInjection(allFiles.get(FileCategory.JAVA));
                        if (progressListener != null && result != null && !result.fileResults().isEmpty()) {
                            int totalFindings = result.fileResults().stream()
                                .mapToInt(fileScan -> fileScan.usages().size())
                                .sum();
                            progressListener.onSubScanComplete("CDI Injection", totalFindings);
                        }
                        return result;
                    }, scanExecutor);
             
            CompletableFuture.allOf(jpaFuture, bvFuture, sjFuture, cdiFuture)
                    .whenComplete((v, t) -> {
                        if (progressListener != null) {
                            progressListener.onScanPhase("Advanced Scans", completedSteps.addAndGet(4), TOTAL_ADVANCED_SCAN_STEPS);
                        }
                    })
                    .join();
            LOG.info("Batch 1 completed");
             
            // Progress updated after batch 2 completes
             
            CompletableFuture<ProjectScanResult<FileScanResult<BuildConfigUsage>>> bcFuture = CompletableFuture
                    .supplyAsync(() -> {
                        ProjectScanResult<FileScanResult<BuildConfigUsage>> result = scanForBuildConfig(allFiles.get(FileCategory.BUILD));
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
                        ProjectScanResult<FileScanResult<JavaxUsage>> result = scanForRestSoap(allFiles.get(FileCategory.JAVA));
                        if (progressListener != null && result != null && !result.fileResults().isEmpty()) {
                            int totalFindings = result.fileResults().stream()
                                .mapToInt(fileScan -> fileScan.usages().size())
                                .sum();
                            progressListener.onSubScanComplete("REST/SOAP", totalFindings);
                        }
                        return result;
                    }, scanExecutor);
            CompletableFuture<DeprecatedApiProjectScanResult> daFuture = CompletableFuture
                    .supplyAsync(() -> scanForDeprecatedApi(allFiles.get(FileCategory.JAVA)), scanExecutor);
            CompletableFuture<SecurityApiProjectScanResult> saFuture = CompletableFuture
                    .supplyAsync(() -> scanForSecurityApi(allFiles.get(FileCategory.JAVA)), scanExecutor);
             
            CompletableFuture.allOf(bcFuture, rsFuture, daFuture, saFuture)
                    .whenComplete((v, t) -> {
                        if (progressListener != null) {
                            progressListener.onScanPhase("Advanced Scans", completedSteps.addAndGet(4), TOTAL_ADVANCED_SCAN_STEPS);
                        }
                    })
                    .join();
            LOG.info("Batch 2 completed");
             
            // Progress updated after batch 3 completes
             
            CompletableFuture<JmsMessagingProjectScanResult> jmFuture = CompletableFuture
                    .supplyAsync(() -> scanForJmsMessaging(allFiles.get(FileCategory.JAVA)), scanExecutor);
            CompletableFuture<TransitiveDependencyProjectScanResult> tdFuture = CompletableFuture
                    .supplyAsync(() -> scanForTransitiveDependencies(allFiles.get(FileCategory.BUILD), progressListener), scanExecutor);
            CompletableFuture<ConfigFileProjectScanResult> cfFuture = CompletableFuture
                    .supplyAsync(() -> scanForConfigFiles(allFiles.get(FileCategory.CONFIG)), scanExecutor);
            CompletableFuture<ClassloaderModuleProjectScanResult> clFuture = CompletableFuture
                    .supplyAsync(() -> scanForClassloaderModule(allFiles.get(FileCategory.JAVA)), scanExecutor);
            CompletableFuture<LoggingMetricsProjectScanResult> lmFuture = CompletableFuture
                    .supplyAsync(() -> scanForLoggingMetrics(allFiles.get(FileCategory.JAVA)), scanExecutor);
            CompletableFuture<SerializationCacheProjectScanResult> scFuture = CompletableFuture
                    .supplyAsync(() -> scanForSerializationCache(allFiles.get(FileCategory.JAVA)), scanExecutor);
            CompletableFuture<ReflectionUsageProjectScanResult> ruFuture = CompletableFuture
                    .supplyAsync(() -> scanForReflectionUsage(allFiles.get(FileCategory.JAVA)), scanExecutor);
            CompletableFuture<ThirdPartyLibProjectScanResult> tpFuture = CompletableFuture
                    .supplyAsync(() -> scanForThirdPartyLib(allFiles.get(FileCategory.BUILD)), scanExecutor);

            CompletableFuture.allOf(jmFuture, tdFuture, cfFuture, clFuture, lmFuture, scFuture, ruFuture, tpFuture)
                    .whenComplete((v, t) -> {
                        if (progressListener != null) {
                            progressListener.onScanPhase("Advanced Scans", completedSteps.addAndGet(8), TOTAL_ADVANCED_SCAN_STEPS);
                        }
                    })
                    .join();
            LOG.info("Batch 3 completed");

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
     * Determines if a file is a test file based on common test directory patterns.
     */
    private boolean isTestFile(Path file, Path projectRoot) {
        String relative = projectRoot.relativize(file).toString().replace("\\", "/");
        return relative.startsWith("src/test/") ||
               relative.startsWith("src/tests/") ||
               relative.startsWith("test/") ||
               relative.startsWith("tests/") ||
               (relative.contains("/test/") && relative.endsWith(".java")) ||
               (relative.contains("\\test\\") && relative.endsWith(".java"));
    }

    /**
     * Discovers all files once per category to avoid redundant I/O.
     * This method walks the file tree 5-6 times total instead of 12-15 times.
     */
    private Map<FileCategory, List<Path>> discoverAllFilesOnce(Path projectPath) {
        Map<FileCategory, List<Path>> files = new HashMap<>();
        
        // Use gitignore-enabled scanner for better folder exclusion
        ProjectFileSystemScanner scanner = ProjectFileSystemScanner.withGitIgnore(projectPath);
        
        // Java source files - scan once, reuse for both JAVA and TEST categories
        List<Path> javaFiles = scanner.findFiles(projectPath, List.of(".java"));
        files.put(FileCategory.JAVA, javaFiles);
        
        // Test files (filtered from the same .java scan)
        List<Path> testFiles = javaFiles.stream()
                .filter(f -> isTestFile(f, projectPath))
                .collect(Collectors.toList());
        files.put(FileCategory.TEST, testFiles);
        
        // Config files
        files.put(FileCategory.CONFIG, scanner.findFiles(projectPath, List.of(".xml", ".properties", ".yaml", ".yml")));
        
        // Build files (pom.xml, build.gradle, Eclipse .project/.classpath, etc.)
        files.put(FileCategory.BUILD, scanner.findFiles(projectPath, path -> {
            String name = path.getFileName().toString();
            return name.equals("pom.xml") || name.startsWith("build.gradle") || name.equals(".classpath");
        }));
        
        // Dockerfiles
        files.put(FileCategory.DOCKER, scanner.findFiles(projectPath, path -> {
            String name = path.getFileName().toString();
            return name.equals("Dockerfile") || name.equals("dockerfile");
        }));
        
        // JSP files
        files.put(FileCategory.JSP, scanner.findFiles(projectPath, List.of(".jsp")));
        
        LOG.info("Discovered files: JAVA=" + files.get(FileCategory.JAVA).size() +
                ", TEST=" + files.get(FileCategory.TEST).size() +
                ", CONFIG=" + files.get(FileCategory.CONFIG).size() +
                ", BUILD=" + files.get(FileCategory.BUILD).size() +
                ", DOCKER=" + files.get(FileCategory.DOCKER).size() +
                ", JSP=" + files.get(FileCategory.JSP).size());
        
        return files;
    }

    private enum FileCategory {
        JAVA, CONFIG, BUILD, DOCKER, JSP, TEST
    }

    // ... rest of the existing methods (getCachedSummary, setCachedSummary, getLastScanResults, 
    // conversion methods, runScansSequentially, scanAllExcludingTransitive, runQuickScansSequentially,
    // individual scan methods needing updates, etc.)

    // Individual scan methods - updated to accept pre-discovered file lists
    public ProjectScanResult<FileScanResult<JpaAnnotationUsage>> scanForJpaAnnotations(List<Path> javaFiles) {
        LOG.info("Scanning " + javaFiles.size() + " Java files for JPA annotations");
        return scanningModule.getJpaAnnotationScanner().scanProject(javaFiles);
    }
    
    public ProjectScanResult<FileScanResult<JavaxUsage>> scanForBeanValidation(List<Path> javaFiles) {
        LOG.info("Scanning " + javaFiles.size() + " Java files for Bean Validation");
        return scanningModule.getBeanValidationScanner().scanProject(javaFiles);
    }
    
    public ProjectScanResult<FileScanResult<ServletJspUsage>> scanForServletJsp(List<Path> javaAndJspFiles) {
        LOG.info("Scanning " + javaAndJspFiles.size() + " files for Servlet/JSP");
        return scanningModule.getServletJspScanner().scanProject(javaAndJspFiles);
    }
    
    public ProjectScanResult<FileScanResult<JavaxUsage>> scanForCdiInjection(List<Path> javaFiles) {
        LOG.info("Scanning " + javaFiles.size() + " Java files for CDI Injection");
        return scanningModule.getCdiInjectionScanner().scanProject(javaFiles);
    }
    
    public ProjectScanResult<FileScanResult<BuildConfigUsage>> scanForBuildConfig(List<Path> buildFiles) {
        LOG.info("Scanning " + buildFiles.size() + " build files for Build Config");
        return scanningModule.getBuildConfigScanner().scanProject(buildFiles);
    }
    
    public ProjectScanResult<FileScanResult<JavaxUsage>> scanForRestSoap(List<Path> javaFiles) {
        LOG.info("Scanning " + javaFiles.size() + " Java files for REST/SOAP");
        return scanningModule.getRestSoapScanner().scanProject(javaFiles);
    }
    
    public DeprecatedApiProjectScanResult scanForDeprecatedApi(List<Path> javaFiles) {
        LOG.info("Scanning " + javaFiles.size() + " Java files for Deprecated API");
        return scanningModule.getDeprecatedApiScanner().scanProject(javaFiles);
    }
    
    public SecurityApiProjectScanResult scanForSecurityApi(List<Path> javaFiles) {
        LOG.info("Scanning " + javaFiles.size() + " Java files for Security API");
        return scanningModule.getSecurityApiScanner().scanProject(javaFiles);
    }
    
    public JmsMessagingProjectScanResult scanForJmsMessaging(List<Path> javaFiles) {
        LOG.info("Scanning " + javaFiles.size() + " Java files for JMS Messaging");
        return scanningModule.getJmsMessagingScanner().scanProject(javaFiles);
    }
    
    public TransitiveDependencyProjectScanResult scanForTransitiveDependencies(List<Path> buildFiles, ScanProgressListener progressListener) {
        LOG.info("Scanning " + buildFiles.size() + " build files for Transitive Dependencies");
        
        // Check if build tools are available before scanning
        if (!isBuildToolAvailable()) {
            LOG.warn("Maven and Gradle are not available. Skipping transitive dependency scan.");
            // Show a single notification balloon with deduplication
            showBuildToolNotification();
            return TransitiveDependencyProjectScanResult.empty();
        }
        
        ScanProgressCallback callback = toScanProgressCallback(progressListener);
        TransitiveDependencyProjectScanResult result = scanningModule.getTransitiveDependencyScanner().scanProject(buildFiles, callback);
        
        // Check if there were command errors during scanning
        if (result.isHadCommandNotFoundError()) {
            LOG.warn("Command errors detected during transitive dependency scanning. Files with errors: " + result.getFilesWithCommandErrors());
            showBuildToolNotification();
        }
        
        return result;
    }

    /**
     * Backward-compatible overload without progress listener.
     */
    public TransitiveDependencyProjectScanResult scanForTransitiveDependencies(List<Path> buildFiles) {
        return scanForTransitiveDependencies(buildFiles, null);
    }
    
    /**
     * Shows a notification about missing build tools with deduplication.
     * Notifications are throttled to once per 5 minutes per project.
     */
    private void showBuildToolNotification() {
        if (project == null) {
            LOG.debug("Cannot show notification: project is null");
            return;
        }
        
        String projectKey = project.getBasePath();
        if (projectKey == null) {
            projectKey = project.getName();
        }
        
        long now = System.currentTimeMillis();
        Long lastNotification = buildToolNotificationTimestamps.get(projectKey);
        
        if (lastNotification != null && (now - lastNotification) < NOTIFICATION_DEDUPLICATION_MS) {
            LOG.debug("Skipping build tool notification - already shown within last 5 minutes for project: " + projectKey);
            return;
        }
        
        // Update timestamp and show notification
        buildToolNotificationTimestamps.put(projectKey, now);
        
        NotificationHelper.showError(
            project,
            "Build Tools Not Found",
            "Maven and/or Gradle commands failed during transitive dependency scanning. " +
            "Deep dependency analysis fell back to regex parsing. " +
            "Please ensure Maven (https://maven.apache.org/download.cgi) or Gradle (https://gradle.org/install/) is properly installed."
        );
        LOG.info("Build tool notification shown for project: " + projectKey);
    }
    
    public ConfigFileProjectScanResult scanForConfigFiles(List<Path> configFiles) {
        LOG.info("Scanning " + configFiles.size() + " config files");
        return scanningModule.getConfigFileScanner().scanProject(configFiles);
    }
    
    public ClassloaderModuleProjectScanResult scanForClassloaderModule(List<Path> javaFiles) {
        LOG.info("Scanning " + javaFiles.size() + " Java files for Classloader/Module");
        return scanningModule.getClassloaderModuleScanner().scanProject(javaFiles);
    }
    
    public LoggingMetricsProjectScanResult scanForLoggingMetrics(List<Path> javaFiles) {
        LOG.info("Scanning " + javaFiles.size() + " Java files for Logging/Metrics");
        return scanningModule.getLoggingMetricsScanner().scanProject(javaFiles);
    }
    
    public SerializationCacheProjectScanResult scanForSerializationCache(List<Path> javaFiles) {
        LOG.info("Scanning " + javaFiles.size() + " Java files for Serialization/Cache");
        return scanningModule.getSerializationCacheScanner().scanProject(javaFiles);
    }
    
    public ReflectionUsageProjectScanResult scanForReflectionUsage(List<Path> sourceFiles) {
        LOG.info("Scanning " + sourceFiles.size() + " source files for Reflection Usage");
        return scanningModule.getReflectionUsageScanner().scanProject(sourceFiles);
    }
    
    public ThirdPartyLibProjectScanResult scanForThirdPartyLib(List<Path> buildAndDockerFiles) {
        LOG.info("Scanning " + buildAndDockerFiles.size() + " build/Docker files for Third-Party Libs");
        return scanningModule.getThirdPartyLibScanner().scanProject(buildAndDockerFiles);
    }
    
    // Update sequential scan to use pre-discovered files
    private AdvancedScanSummary runScansSequentially(Path projectPath, ScanProgressListener progressListener) {
        LOG.info("Running scans sequentially to conserve memory");
        
        try {
            int completed = 0;
            if (progressListener != null) {
                progressListener.onScanPhase("Advanced Scans", completed, TOTAL_ADVANCED_SCAN_STEPS);
            }
            
            Map<FileCategory, List<Path>> allFiles = discoverAllFilesOnce(projectPath);
            if (progressListener != null) {
                completed++;
                progressListener.onScanPhase("Advanced Scans", completed, TOTAL_ADVANCED_SCAN_STEPS);
            }
            
            ProjectScanResult<FileScanResult<JpaAnnotationUsage>> jpaResult = scanForJpaAnnotations(allFiles.get(FileCategory.JAVA));
            if (progressListener != null) {
                completed++;
                progressListener.onScanPhase("Advanced Scans", completed, TOTAL_ADVANCED_SCAN_STEPS);
            }
            if (progressListener != null && jpaResult != null && !jpaResult.fileResults().isEmpty()) {
                int totalFindings = jpaResult.fileResults().stream().mapToInt(fr -> fr.usages().size()).sum();
                progressListener.onSubScanComplete("JPA", totalFindings);
            }
            
            ProjectScanResult<FileScanResult<JavaxUsage>> beanValidationResult = scanForBeanValidation(allFiles.get(FileCategory.JAVA));
            if (progressListener != null) {
                completed++;
                progressListener.onScanPhase("Advanced Scans", completed, TOTAL_ADVANCED_SCAN_STEPS);
            }
            if (progressListener != null && beanValidationResult != null && !beanValidationResult.fileResults().isEmpty()) {
                int totalFindings = beanValidationResult.fileResults().stream().mapToInt(fr -> fr.usages().size()).sum();
                progressListener.onSubScanComplete("Bean Validation", totalFindings);
            }
            
            ProjectScanResult<FileScanResult<ServletJspUsage>> servletJspResult = scanForServletJsp(allFiles.get(FileCategory.JAVA));
            if (progressListener != null) {
                completed++;
                progressListener.onScanPhase("Advanced Scans", completed, TOTAL_ADVANCED_SCAN_STEPS);
            }
            if (progressListener != null && servletJspResult != null && !servletJspResult.fileResults().isEmpty()) {
                int totalFindings = servletJspResult.fileResults().stream().mapToInt(fr -> fr.usages().size()).sum();
                progressListener.onSubScanComplete("Servlet/JSP", totalFindings);
            }
            
            ProjectScanResult<FileScanResult<JavaxUsage>> cdiInjectionResult = scanForCdiInjection(allFiles.get(FileCategory.JAVA));
            if (progressListener != null) {
                completed++;
                progressListener.onScanPhase("Advanced Scans", completed, TOTAL_ADVANCED_SCAN_STEPS);
            }
            if (progressListener != null && cdiInjectionResult != null && !cdiInjectionResult.fileResults().isEmpty()) {
                int totalFindings = cdiInjectionResult.fileResults().stream().mapToInt(fr -> fr.usages().size()).sum();
                progressListener.onSubScanComplete("CDI Injection", totalFindings);
            }
            
            ProjectScanResult<FileScanResult<BuildConfigUsage>> buildConfigResult = scanForBuildConfig(allFiles.get(FileCategory.BUILD));
            if (progressListener != null) {
                completed++;
                progressListener.onScanPhase("Advanced Scans", completed, TOTAL_ADVANCED_SCAN_STEPS);
            }
            if (progressListener != null && buildConfigResult != null && !buildConfigResult.fileResults().isEmpty()) {
                int totalFindings = buildConfigResult.fileResults().stream().mapToInt(fr -> fr.usages().size()).sum();
                progressListener.onSubScanComplete("Build Config", totalFindings);
            }
            
            ProjectScanResult<FileScanResult<JavaxUsage>> restSoapResult = scanForRestSoap(allFiles.get(FileCategory.JAVA));
            if (progressListener != null) {
                completed++;
                progressListener.onScanPhase("Advanced Scans", completed, TOTAL_ADVANCED_SCAN_STEPS);
            }
            if (progressListener != null && restSoapResult != null && !restSoapResult.fileResults().isEmpty()) {
                int totalFindings = restSoapResult.fileResults().stream().mapToInt(fr -> fr.usages().size()).sum();
                progressListener.onSubScanComplete("REST/SOAP", totalFindings);
            }
            
            DeprecatedApiProjectScanResult deprecatedApiResult = scanForDeprecatedApi(allFiles.get(FileCategory.JAVA));
            if (progressListener != null) {
                completed++;
                progressListener.onScanPhase("Advanced Scans", completed, TOTAL_ADVANCED_SCAN_STEPS);
            }
            SecurityApiProjectScanResult securityApiResult = scanForSecurityApi(allFiles.get(FileCategory.JAVA));
            if (progressListener != null) {
                completed++;
                progressListener.onScanPhase("Advanced Scans", completed, TOTAL_ADVANCED_SCAN_STEPS);
            }
            JmsMessagingProjectScanResult jmsMessagingResult = scanForJmsMessaging(allFiles.get(FileCategory.JAVA));
            if (progressListener != null) {
                completed++;
                progressListener.onScanPhase("Advanced Scans", completed, TOTAL_ADVANCED_SCAN_STEPS);
            }
            ConfigFileProjectScanResult configFileResult = scanForConfigFiles(allFiles.get(FileCategory.CONFIG));
            if (progressListener != null) {
                completed++;
                progressListener.onScanPhase("Advanced Scans", completed, TOTAL_ADVANCED_SCAN_STEPS);
            }
            ClassloaderModuleProjectScanResult classloaderModuleResult = scanForClassloaderModule(allFiles.get(FileCategory.JAVA));
            if (progressListener != null) {
                completed++;
                progressListener.onScanPhase("Advanced Scans", completed, TOTAL_ADVANCED_SCAN_STEPS);
            }
            LoggingMetricsProjectScanResult loggingMetricsResult = scanForLoggingMetrics(allFiles.get(FileCategory.JAVA));
            if (progressListener != null) {
                completed++;
                progressListener.onScanPhase("Advanced Scans", completed, TOTAL_ADVANCED_SCAN_STEPS);
            }
            SerializationCacheProjectScanResult serializationCacheResult = scanForSerializationCache(allFiles.get(FileCategory.JAVA));
            if (progressListener != null) {
                completed++;
                progressListener.onScanPhase("Advanced Scans", completed, TOTAL_ADVANCED_SCAN_STEPS);
            }
            ReflectionUsageProjectScanResult reflectionUsageResult = scanForReflectionUsage(allFiles.get(FileCategory.JAVA));
            if (progressListener != null) {
                completed++;
                progressListener.onScanPhase("Advanced Scans", completed, TOTAL_ADVANCED_SCAN_STEPS);
            }
            ThirdPartyLibProjectScanResult thirdPartyLibResult = scanForThirdPartyLib(allFiles.get(FileCategory.BUILD));
            if (progressListener != null) {
                completed++;
                progressListener.onScanPhase("Advanced Scans", completed, TOTAL_ADVANCED_SCAN_STEPS);
            }
            TransitiveDependencyProjectScanResult transitiveDependencyResult = scanForTransitiveDependencies(allFiles.get(FileCategory.BUILD), progressListener);
            if (progressListener != null) {
                completed++;
                progressListener.onScanPhase("Advanced Scans", completed, TOTAL_ADVANCED_SCAN_STEPS);
            }
            if (progressListener != null && transitiveDependencyResult != null && !transitiveDependencyResult.getFileResults().isEmpty()) {
                int totalFindings = transitiveDependencyResult.getTotalJavaxDependencies();
                progressListener.onSubScanComplete("Transitive Dependencies", totalFindings);
            }
            
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
            LOG.error("Sequential scan failed", e);
            throw new RuntimeException("Advanced scan failed", e);
        }
    }
    
    // Existing methods that need to stay (conversion methods, etc.)
    // ... keeping those unchanged
    
    // Individual scan methods for each scanner type (deprecated - keeping for compatibility but delegating to file-list versions)
    @Deprecated
    public ProjectScanResult<FileScanResult<JpaAnnotationUsage>> scanForJpaAnnotations(Path projectPath) {
        LOG.info("Scanning for JPA annotations in: " + projectPath);
        ProjectFileSystemScanner scanner = ProjectFileSystemScanner.withGitIgnore(projectPath);
        List<Path> javaFiles = scanner.findFiles(projectPath, List.of(".java"));
        return scanForJpaAnnotations(javaFiles);
    }
    
    @Deprecated
    public ProjectScanResult<FileScanResult<JavaxUsage>> scanForBeanValidation(Path projectPath) {
        LOG.info("Scanning for Bean Validation in: " + projectPath);
        ProjectFileSystemScanner scanner = ProjectFileSystemScanner.withGitIgnore(projectPath);
        List<Path> javaFiles = scanner.findFiles(projectPath, List.of(".java"));
        return scanForBeanValidation(javaFiles);
    }
    
    @Deprecated
    public ProjectScanResult<FileScanResult<ServletJspUsage>> scanForServletJsp(Path projectPath) {
        LOG.info("Scanning for Servlet/JSP in: " + projectPath);
        ProjectFileSystemScanner scanner = ProjectFileSystemScanner.withGitIgnore(projectPath);
        // ServletJspScanner needs both .java and .jsp files
        List<Path> allFiles = new ArrayList<>(scanner.findFiles(projectPath, List.of(".java")));
        allFiles.addAll(scanner.findFiles(projectPath, List.of(".jsp")));
        return scanForServletJsp(allFiles);
    }
    
    @Deprecated
    public ProjectScanResult<FileScanResult<JavaxUsage>> scanForCdiInjection(Path projectPath) {
        LOG.info("Scanning for CDI Injection in: " + projectPath);
        ProjectFileSystemScanner scanner = ProjectFileSystemScanner.withGitIgnore(projectPath);
        List<Path> javaFiles = scanner.findFiles(projectPath, List.of(".java"));
        return scanForCdiInjection(javaFiles);
    }
    
    @Deprecated
    public ProjectScanResult<FileScanResult<BuildConfigUsage>> scanForBuildConfig(Path projectPath) {
        LOG.info("Scanning for Build Config in: " + projectPath);
        ProjectFileSystemScanner scanner = ProjectFileSystemScanner.withGitIgnore(projectPath);
        List<Path> buildFiles = scanner.findFiles(projectPath, path -> {
            String name = path.getFileName().toString();
            return name.equals("pom.xml") || name.startsWith("build.gradle");
        });
        return scanForBuildConfig(buildFiles);
    }
    
    @Deprecated
    public ProjectScanResult<FileScanResult<JavaxUsage>> scanForRestSoap(Path projectPath) {
        LOG.info("Scanning for REST/SOAP in: " + projectPath);
        ProjectFileSystemScanner scanner = ProjectFileSystemScanner.withGitIgnore(projectPath);
        List<Path> javaFiles = scanner.findFiles(projectPath, List.of(".java"));
        return scanForRestSoap(javaFiles);
    }
    
    @Deprecated
    public DeprecatedApiProjectScanResult scanForDeprecatedApi(Path projectPath) {
        LOG.info("Scanning for Deprecated API in: " + projectPath);
        ProjectFileSystemScanner scanner = ProjectFileSystemScanner.withGitIgnore(projectPath);
        List<Path> javaFiles = scanner.findFiles(projectPath, List.of(".java"));
        return scanForDeprecatedApi(javaFiles);
    }
    
    @Deprecated
    public SecurityApiProjectScanResult scanForSecurityApi(Path projectPath) {
        LOG.info("Scanning for Security API in: " + projectPath);
        ProjectFileSystemScanner scanner = ProjectFileSystemScanner.withGitIgnore(projectPath);
        List<Path> javaFiles = scanner.findFiles(projectPath, List.of(".java"));
        return scanForSecurityApi(javaFiles);
    }
    
    @Deprecated
    public JmsMessagingProjectScanResult scanForJmsMessaging(Path projectPath) {
        LOG.info("Scanning for JMS Messaging in: " + projectPath);
        ProjectFileSystemScanner scanner = ProjectFileSystemScanner.withGitIgnore(projectPath);
        List<Path> javaFiles = scanner.findFiles(projectPath, List.of(".java"));
        return scanForJmsMessaging(javaFiles);
    }
    
    @Deprecated
    public TransitiveDependencyProjectScanResult scanForTransitiveDependencies(Path projectPath, ScanProgressListener progressListener) {
        LOG.info("Scanning for Transitive Dependencies in: " + projectPath);
        ProjectFileSystemScanner scanner = ProjectFileSystemScanner.withGitIgnore(projectPath);
        List<Path> buildFiles = scanner.findFiles(projectPath, path -> {
            String name = path.getFileName().toString();
            return name.equals("pom.xml") || name.startsWith("build.gradle");
        });
        return scanForTransitiveDependencies(buildFiles, progressListener);
    }

    public TransitiveDependencyProjectScanResult scanForTransitiveDependencies(Path projectPath) {
        return scanForTransitiveDependencies(projectPath, null);
    }
    
    @Deprecated
    public ConfigFileProjectScanResult scanForConfigFiles(Path projectPath) {
        LOG.info("Scanning for Config Files in: " + projectPath);
        ProjectFileSystemScanner scanner = ProjectFileSystemScanner.withGitIgnore(projectPath);
        List<Path> configFiles = scanner.findFiles(projectPath, List.of(".xml", ".properties", ".yaml", ".yml"));
        return scanForConfigFiles(configFiles);
    }
    
    @Deprecated
    public ClassloaderModuleProjectScanResult scanForClassloaderModule(Path projectPath) {
        LOG.info("Scanning for Classloader/Module in: " + projectPath);
        ProjectFileSystemScanner scanner = ProjectFileSystemScanner.withGitIgnore(projectPath);
        List<Path> javaFiles = scanner.findFiles(projectPath, List.of(".java"));
        return scanForClassloaderModule(javaFiles);
    }
    
    @Deprecated
    public LoggingMetricsProjectScanResult scanForLoggingMetrics(Path projectPath) {
        LOG.info("Scanning for Logging/Metrics in: " + projectPath);
        ProjectFileSystemScanner scanner = ProjectFileSystemScanner.withGitIgnore(projectPath);
        List<Path> javaFiles = scanner.findFiles(projectPath, List.of(".java"));
        return scanForLoggingMetrics(javaFiles);
    }
    
    @Deprecated
    public SerializationCacheProjectScanResult scanForSerializationCache(Path projectPath) {
        LOG.info("Scanning for Serialization/Cache in: " + projectPath);
        ProjectFileSystemScanner scanner = ProjectFileSystemScanner.withGitIgnore(projectPath);
        List<Path> javaFiles = scanner.findFiles(projectPath, List.of(".java"));
        return scanForSerializationCache(javaFiles);
    }
    
    @Deprecated
    public ReflectionUsageProjectScanResult scanForReflectionUsage(Path projectPath) {
        LOG.info("Scanning for Reflection Usage in: " + projectPath);
        ProjectFileSystemScanner scanner = ProjectFileSystemScanner.withGitIgnore(projectPath);
        // Reflection scanner needs .java, .kt, .scala
        List<Path> allSource = new ArrayList<>(scanner.findFiles(projectPath, List.of(".java")));
        allSource.addAll(scanner.findFiles(projectPath, List.of(".kt")));
        allSource.addAll(scanner.findFiles(projectPath, List.of(".scala")));
        return scanForReflectionUsage(allSource);
    }
    
    @Deprecated
    public ThirdPartyLibProjectScanResult scanForThirdPartyLib(Path projectPath) {
        LOG.info("Scanning for Third-Party Libraries in: " + projectPath);
        ProjectFileSystemScanner scanner = ProjectFileSystemScanner.withGitIgnore(projectPath);
        List<Path> buildFiles = scanner.findFiles(projectPath, path -> {
            String name = path.getFileName().toString();
            return name.equals("pom.xml") || name.startsWith("build.gradle") || 
                   name.equals("Dockerfile") || name.equals("dockerfile");
        });
        return scanForThirdPartyLib(buildFiles);
    }
    
    @Deprecated
    public UnitTestProjectScanResult scanForUnitTests(Path projectPath) {
        LOG.info("Scanning for Unit Tests in: " + projectPath);
        ProjectFileSystemScanner scanner = ProjectFileSystemScanner.withGitIgnore(projectPath);
        List<Path> javaFiles = scanner.findFiles(projectPath, List.of(".java"));
        return scanningModule.getUnitTestScanner().scanProject(javaFiles);
    }
    
    @Deprecated
    public IntegrationPointsProjectScanResult scanForIntegrationPoints(Path projectPath) {
        LOG.info("Scanning for Integration Points in: " + projectPath);
        ProjectFileSystemScanner scanner = ProjectFileSystemScanner.withGitIgnore(projectPath);
        List<Path> javaFiles = scanner.findFiles(projectPath, List.of(".java"));
        return scanningModule.getIntegrationPointsScanner().scanProject(javaFiles);
    }
    
    @Deprecated
    public AppServerProjectScanResult scanForAppServer(Path projectPath) {
        LOG.info("Scanning for App Server Config in: " + projectPath);
        ProjectFileSystemScanner scanner = ProjectFileSystemScanner.withGitIgnore(projectPath);
        List<Path> javaFiles = scanner.findFiles(projectPath, List.of(".java"));
        return scanningModule.getAppServerScanner().scanProject(javaFiles);
    }
    
    @Deprecated
    public ProjectScanResult<FileScanResult<DockerCicdUsage>> scanForDockerCicd(Path projectPath) {
        LOG.info("Scanning for Docker/CI-CD in: " + projectPath);
        ProjectFileSystemScanner scanner = ProjectFileSystemScanner.withGitIgnore(projectPath);
        List<Path> dockerFiles = scanner.findFiles(projectPath, path -> {
            String name = path.getFileName().toString();
            return name.equals("Dockerfile") || name.equals("dockerfile") ||
                   name.endsWith(".yml") || name.endsWith(".yaml");
        });
        return scanningModule.getDockerCicdScanner().scanProject(dockerFiles);
    }
    
    @Deprecated
    public TestContainersProjectScanResult scanForTestContainers(Path projectPath) {
        LOG.info("Scanning for TestContainers in: " + projectPath);
        ProjectFileSystemScanner scanner = ProjectFileSystemScanner.withGitIgnore(projectPath);
        List<Path> buildFiles = scanner.findFiles(projectPath, path -> {
            String name = path.getFileName().toString();
            return name.equals("pom.xml") || name.startsWith("build.gradle");
        });
        return scanningModule.getTestContainersScanner().scanProject(buildFiles);
    }
    
    // Getter methods for AdvancedScanningModule scanners (already exist)
    public JpaAnnotationScanner getJpaAnnotationScanner() {
        return scanningModule.getJpaAnnotationScanner();
    }
    
    public BeanValidationScanner getBeanValidationScanner() {
        return scanningModule.getBeanValidationScanner();
    }
    
    public ServletJspScanner getServletJspScanner() {
        return scanningModule.getServletJspScanner();
    }
    
    public CdiInjectionScanner getCdiInjectionScanner() {
        return scanningModule.getCdiInjectionScanner();
    }
    
    public BuildConfigScanner getBuildConfigScanner() {
        return scanningModule.getBuildConfigScanner();
    }
    
    public RestSoapScanner getRestSoapScanner() {
        return scanningModule.getRestSoapScanner();
    }
    
    public DeprecatedApiScanner getDeprecatedApiScanner() {
        return scanningModule.getDeprecatedApiScanner();
    }
    
    public SecurityApiScanner getSecurityApiScanner() {
        return scanningModule.getSecurityApiScanner();
    }
    
    public JmsMessagingScanner getJmsMessagingScanner() {
        return scanningModule.getJmsMessagingScanner();
    }
    
    public TransitiveDependencyScanner getTransitiveDependencyScanner() {
        return scanningModule.getTransitiveDependencyScanner();
    }
    
    public ConfigFileScanner getConfigFileScanner() {
        return scanningModule.getConfigFileScanner();
    }
    
    public ClassloaderModuleScanner getClassloaderModuleScanner() {
        return scanningModule.getClassloaderModuleScanner();
    }
    
    public LoggingMetricsScanner getLoggingMetricsScanner() {
        return scanningModule.getLoggingMetricsScanner();
    }
    
    public SerializationCacheScanner getSerializationCacheScanner() {
        return scanningModule.getSerializationCacheScanner();
    }
    
    public ReflectionUsageScanner getReflectionUsageScanner() {
        return scanningModule.getReflectionUsageScanner();
    }
    
    public ThirdPartyLibScanner getThirdPartyLibScanner() {
        return scanningModule.getThirdPartyLibScanner();
    }
    
    public UnitTestScanner getUnitTestScanner() {
        return scanningModule.getUnitTestScanner();
    }
    
    public IntegrationPointsScanner getIntegrationPointsScanner() {
        return scanningModule.getIntegrationPointsScanner();
    }
    
    public AppServerScanner getAppServerScanner() {
        return scanningModule.getAppServerScanner();
    }
    
    public DockerCicdScanner getDockerCicdScanner() {
        return scanningModule.getDockerCicdScanner();
    }
    
    public TestContainersScanner getTestContainersScanner() {
        return scanningModule.getTestContainersScanner();
    }

    public boolean hasCachedResults() {
        return cachedSummaryRef.get() != null;
    }

    public AdvancedScanSummary getCachedSummary() {
        return cachedSummaryRef.get();
    }

    public ComprehensiveScanResults getLastScanResults() {
        AdvancedScanSummary summary = getCachedSummary();
        if (summary == null) {
            return null;
        }

        // Convert generic ProjectScanResult<FileScanResult<T>> into typed domain records
        JpaProjectScanResult jpaProjectResult = convertToJpaProjectScanResult(summary.jpaResult());
        CdiInjectionProjectScanResult cdiProjectResult = convertToCdiProjectScanResult(summary.cdiInjectionResult());
        ServletJspProjectScanResult servletProjectResult = convertToServletJspProjectScanResult(summary.servletJspResult());
        BuildConfigProjectScanResult buildConfigProjectResult = convertToBuildConfigProjectScanResult(summary.buildConfigResult());

        Map<String, Object> jpaResults = jpaProjectResult != null && jpaProjectResult.hasJavaxUsage()
                ? Map.of("jpa", jpaProjectResult) : Map.of();
        Map<String, Object> beanValidationResults = summary.beanValidationResult() != null && summary.beanValidationResult().hasIssues()
                ? Map.of("beanValidation", summary.beanValidationResult()) : Map.of();
        Map<String, Object> cdiResults = cdiProjectResult != null && cdiProjectResult.hasJavaxUsage()
                ? Map.of("cdi", cdiProjectResult) : Map.of();
        Map<String, Object> servletJspResults = servletProjectResult != null && servletProjectResult.hasJavaxUsage()
                ? Map.of("servlet", servletProjectResult) : Map.of();
        Map<String, Object> buildConfigResults = buildConfigProjectResult != null && buildConfigProjectResult.hasJavaxDependencies()
                ? Map.of("buildConfig", buildConfigProjectResult) : Map.of();
        Map<String, Object> thirdPartyLibResults = summary.thirdPartyLibResult() != null && summary.thirdPartyLibResult().hasFindings()
                ? Map.of("thirdParty", summary.thirdPartyLibResult()) : Map.of();
        Map<String, Object> transitiveDependencyResults = summary.transitiveDependencyResult() != null && summary.transitiveDependencyResult().getTotalJavaxDependencies() > 0
                ? Map.of("transitive", summary.transitiveDependencyResult()) : Map.of();

        int totalIssues = summary.getTotalIssuesFound();
        int totalFilesScanned = computeTotalFilesScanned(summary);
        int filesWithIssues = computeFilesWithIssues(summary);
        double readinessScore = totalFilesScanned > 0 ? Math.max(0.0, 1.0 - (double) filesWithIssues / totalFilesScanned) : 1.0;

        ComprehensiveScanResults.ScanSummary scanSummary = new ComprehensiveScanResults.ScanSummary(
                totalFilesScanned,
                filesWithIssues,
                0, // criticalIssues - not tracked at this level
                totalIssues,
                0, // infoIssues - not tracked at this level
                readinessScore
        );

        return new ComprehensiveScanResults(
                "",
                LocalDateTime.now(),
                jpaResults,
                beanValidationResults,
                cdiResults,
                servletJspResults,
                thirdPartyLibResults,
                transitiveDependencyResults,
                buildConfigResults,
                List.of(),
                totalIssues,
                scanSummary
        );
    }

    private JpaProjectScanResult convertToJpaProjectScanResult(ProjectScanResult<FileScanResult<JpaAnnotationUsage>> result) {
        if (result == null) return JpaProjectScanResult.empty();
        List<JpaScanResult> fileResults = result.fileResults().stream()
                .map(fr -> new JpaScanResult(fr.filePath(), fr.usages(), fr.lineCount()))
                .collect(Collectors.toList());
        return new JpaProjectScanResult(fileResults, result.totalFilesScanned(), result.filesWithIssues(), result.totalIssuesFound());
    }

    private CdiInjectionProjectScanResult convertToCdiProjectScanResult(ProjectScanResult<FileScanResult<JavaxUsage>> result) {
        if (result == null) return CdiInjectionProjectScanResult.empty();
        List<CdiInjectionScanResult> fileResults = result.fileResults().stream()
                .map(fr -> new CdiInjectionScanResult(fr.filePath(), convertToCdiUsages(fr.usages()), fr.lineCount()))
                .collect(Collectors.toList());
        return new CdiInjectionProjectScanResult(fileResults, result.totalFilesScanned(), result.filesWithIssues(), result.totalIssuesFound());
    }

    private List<CdiInjectionUsage> convertToCdiUsages(List<JavaxUsage> usages) {
        return usages.stream()
                .map(u -> new CdiInjectionUsage(u.className(), u.jakartaEquivalent(), u.lineNumber(), u.context(), u.context()))
                .collect(Collectors.toList());
    }

    private ServletJspProjectScanResult convertToServletJspProjectScanResult(ProjectScanResult<FileScanResult<ServletJspUsage>> result) {
        if (result == null) return ServletJspProjectScanResult.empty();
        List<ServletJspScanResult> fileResults = result.fileResults().stream()
                .map(fr -> new ServletJspScanResult(fr.filePath(), fr.usages(), fr.lineCount()))
                .collect(Collectors.toList());
        return new ServletJspProjectScanResult(fileResults, result.totalFilesScanned(), result.filesWithIssues(), result.totalIssuesFound());
    }

    private BuildConfigProjectScanResult convertToBuildConfigProjectScanResult(ProjectScanResult<FileScanResult<BuildConfigUsage>> result) {
        if (result == null) return BuildConfigProjectScanResult.empty();
        List<BuildConfigScanResult> fileResults = result.fileResults().stream()
                .map(fr -> new BuildConfigScanResult(fr.filePath(), fr.usages(), ""))
                .collect(Collectors.toList());
        return new BuildConfigProjectScanResult(fileResults, result.totalFilesScanned(), result.filesWithIssues(), result.totalIssuesFound());
    }

    private int computeTotalFilesScanned(AdvancedScanSummary summary) {
        int max = 0;
        if (summary.jpaResult() != null) max = Math.max(max, summary.jpaResult().totalFilesScanned());
        if (summary.beanValidationResult() != null) max = Math.max(max, summary.beanValidationResult().totalFilesScanned());
        if (summary.cdiInjectionResult() != null) max = Math.max(max, summary.cdiInjectionResult().totalFilesScanned());
        if (summary.servletJspResult() != null) max = Math.max(max, summary.servletJspResult().totalFilesScanned());
        return max;
    }

    private int computeFilesWithIssues(AdvancedScanSummary summary) {
        int total = 0;
        if (summary.jpaResult() != null) total += summary.jpaResult().filesWithIssues();
        if (summary.beanValidationResult() != null) total += summary.beanValidationResult().filesWithIssues();
        if (summary.cdiInjectionResult() != null) total += summary.cdiInjectionResult().filesWithIssues();
        if (summary.servletJspResult() != null) total += summary.servletJspResult().filesWithIssues();
        if (summary.buildConfigResult() != null) total += summary.buildConfigResult().filesWithIssues();
        return total;
    }

    /**
     * Summary of advanced scanning results aggregated from all scanners.
     */
    public static record AdvancedScanSummary(
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

        public int getJpaCount() {
            return jpaResult != null ? jpaResult.totalIssuesFound() : 0;
        }

        public int getBeanValidationCount() {
            return beanValidationResult != null ? beanValidationResult.totalIssuesFound() : 0;
        }

        public int getServletJspCount() {
            return servletJspResult != null ? servletJspResult.totalIssuesFound() : 0;
        }

        public int getCdiInjectionCount() {
            return cdiInjectionResult != null ? cdiInjectionResult.totalIssuesFound() : 0;
        }

        public int getBuildConfigCount() {
            return buildConfigResult != null ? buildConfigResult.totalIssuesFound() : 0;
        }

        public int getRestSoapCount() {
            return restSoapResult != null ? restSoapResult.totalIssuesFound() : 0;
        }

        public int getDeprecatedApiCount() {
            return deprecatedApiResult != null ? deprecatedApiResult.totalUsagesFound() : 0;
        }

        public int getSecurityApiCount() {
            return securityApiResult != null ? securityApiResult.getTotalJavaxUsages() : 0;
        }

        public int getJmsMessagingCount() {
            return jmsMessagingResult != null ? jmsMessagingResult.getTotalJavaxUsages() : 0;
        }

        public int getConfigFileCount() {
            return configFileResult != null ? configFileResult.getTotalJavaxUsages() : 0;
        }

        public int getTransitiveDependencyCount() {
            return transitiveDependencyResult != null ? transitiveDependencyResult.getTotalJavaxDependencies() : 0;
        }

        public int getClassloaderModuleCount() {
            return classloaderModuleResult != null ? classloaderModuleResult.getTotalJavaxUsages() : 0;
        }

        public int getLoggingMetricsCount() {
            return loggingMetricsResult != null ? loggingMetricsResult.getTotalFindings() : 0;
        }

        public int getSerializationCacheCount() {
            return serializationCacheResult != null ? serializationCacheResult.getTotalFindings() : 0;
        }

        public int getThirdPartyLibCount() {
            return thirdPartyLibResult != null ? thirdPartyLibResult.getTotalLibraries() : 0;
        }

        public int getCount(AdvancedScanCategory category) {
            if (category == null) {
                return 0;
            }
            return category.getCount(this);
        }

        public int getTotalIssuesFound() {
            return Arrays.stream(AdvancedScanCategory.values())
                    .mapToInt(this::getCount)
                    .sum();
        }

        public int getTotalIssuesWithRecipes() {
            return Arrays.stream(AdvancedScanCategory.values())
                    .filter(AdvancedScanCategory::hasRecipe)
                    .mapToInt(this::getCount)
                    .sum();
        }

        public int getTotalSourceIssues() {
            return Arrays.stream(AdvancedScanCategory.values())
                    .filter(AdvancedScanCategory::isSourceIssue)
                    .mapToInt(this::getCount)
                    .sum();
        }

        public int getTotalConfigIssues() {
            return Arrays.stream(AdvancedScanCategory.values())
                    .filter(AdvancedScanCategory::isConfigIssue)
                    .mapToInt(this::getCount)
                    .sum();
        }
    }

    // Additional methods required by UI and external components
    public boolean isMavenAvailable() {
        // TODO: Implement proper detection (check PATH, common locations)
        return true;
    }

    public boolean isGradleAvailable() {
        // TODO: Implement proper detection
        return true;
    }

    public ScanRecipeRecommendationService getRecipeRecommendationService() {
        return scanningModule.getRecipeRecommendationService();
    }

    public List<DependencyInfo> convertToDependencyInfo(TransitiveDependencyProjectScanResult deepResult) {
        if (deepResult == null) {
            return List.of();
        }

        Map<String, DependencyInfo> dependencyMap = new HashMap<>();

        // Process all file results and usages
        for (adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyScanResult fileResult : deepResult.getFileResults()) {
            String fileErrorMessage = fileResult.hasError() ? fileResult.getErrorMessage() : null;

            for (adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyUsage usage : fileResult.getUsages()) {
                String artifactKey = usage.getArtifactKey();

                // Deduplicate by artifact key
                if (dependencyMap.containsKey(artifactKey)) {
                    continue;
                }

                DependencyInfo info = new DependencyInfo();
                info.setGroupId(usage.getGroupId());
                info.setArtifactId(usage.getArtifactId());
                info.setCurrentVersion(usage.getVersion());
                info.setTransitive(usage.isTransitive());
                info.setDepth(usage.getDepth());
                info.setScope(usage.getScope() != null ? usage.getScope() : "compile");

                // Determine migration status based on scan reason
                DependencyMigrationStatus status = determineMigrationStatus(usage);
                info.setMigrationStatus(status);

                // Set a non-null recipe name for UI display
                info.setAssociatedRecipeName(determineAssociatedRecipeName(usage, status));

                // Set Jakarta compatibility status based on scan reason
                info.setJakartaCompatibilityStatus(determineJakartaCompatibilityStatus(usage.getScanReason()));

                // Set scan reason (convert enum to string)
                if (usage.getScanReason() != null) {
                    info.setScanReason(usage.getScanReason().name());
                }

                // Set detail message — prefer file-level error if the file had a build tool error
                String detailMsg = usage.getDetailMessage();
                if (fileErrorMessage != null && usage.getScanReason() == ScanReason.BUILD_TOOL_ERROR) {
                    detailMsg = friendlyErrorMessage(fileErrorMessage);
                } else if (fileErrorMessage != null && detailMsg == null) {
                    detailMsg = "Results based on regex fallback — build tool command failed";
                }
                info.setDetailMessage(detailMsg);

                // Set confidence
                info.setConfidence(usage.getConfidence());

                // Set incompatibility from transitive flag
                info.setIncompatibilityFromTransitive(usage.isIncompatibilityFromTransitive());

                // Map severity, recommendation, and javaxPackage
                info.setSeverity(usage.getSeverity());
                info.setRecommendation(usage.getRecommendation());
                info.setJavaxPackage(usage.getJavaxPackage());

                // Set recommended version if available
                if (usage.getAlternativeVersions() != null && !usage.getAlternativeVersions().isEmpty()) {
                    info.setRecommendedVersion(usage.getAlternativeVersions().get(0));
                }

                // Parse the full recommendation (group:artifact:version) into structured fields
                // so the UI can display a Jakarta equivalent in the dependencies table.
                if (usage.getRecommendation() != null && !usage.getRecommendation().isBlank()) {
                    info.setRecommendedArtifactCoordinates(usage.getRecommendation());
                }

                dependencyMap.put(artifactKey, info);
            }
        }

        return new ArrayList<>(dependencyMap.values());
    }

    /**
     * Generates a summary banner message describing project-level scan errors.
     * Returns null when there are no errors to report.
     */
    public String buildErrorBanner(TransitiveDependencyProjectScanResult deepResult) {
        if (deepResult == null) return null;
        int filesWithErrors = deepResult.getFilesWithCommandErrors();
        if (filesWithErrors <= 0) return null;
        return "⚠ " + filesWithErrors + " build file" + (filesWithErrors > 1 ? "s" : "")
            + " failed — results for those modules are based on regex fallback (partial)";
    }

    private static String friendlyErrorMessage(String raw) {
        if (raw == null) return null;
        String lower = raw.toLowerCase();
        if (lower.contains("not found")) return "Build tool not found — install Maven/Gradle or add wrapper to project";
        if (lower.contains("timeout")) return "Build command timed out — project may be too large or build too slow";
        if (lower.contains("no dependencies")) return "Build command returned no dependencies — check build configuration";
        if (lower.contains("exited with code") || lower.contains("command failed")) return "Build command failed — results based on regex fallback (partial)";
        return "Build tool error — " + raw;
    }

    private String determineJakartaCompatibilityStatus(ScanReason scanReason) {
        return DependencyStatusColors.getStatusSlug(determineMigrationStatus(scanReason));
    }

    private String determineAssociatedRecipeName(TransitiveDependencyUsage usage, DependencyMigrationStatus status) {
        if (status == DependencyMigrationStatus.COMPATIBLE || status == DependencyMigrationStatus.MIGRATED) {
            return "Compatible with Jakarta EE";
        }
        if (usage.getGroupId() != null && usage.getGroupId().startsWith("javax.")) {
            return "Migrate " + usage.getGroupId() + " to Jakarta EE";
        }
        if (status == DependencyMigrationStatus.NEEDS_UPGRADE) {
            return "Upgrade to Jakarta EE equivalent";
        }
        return "Review dependency";
    }

    private static DependencyMigrationStatus determineMigrationStatus(ScanReason reason) {
        if (reason == null) {
            return DependencyMigrationStatus.UNKNOWN;
        }
        return switch (reason) {
            case WHITELISTED, BYTECODE_SCAN_JAKARTA -> DependencyMigrationStatus.COMPATIBLE;
            case BLACKLISTED, BYTECODE_SCAN_JAVAX, TRANSITIVE_INCOMPATIBLE, MAVEN_LOOKUP_FOUND -> DependencyMigrationStatus.NEEDS_UPGRADE;
            case MAVEN_LOOKUP_NONE -> DependencyMigrationStatus.NO_JAKARTA_VERSION;
            case BYTECODE_SCAN_MIXED -> DependencyMigrationStatus.REQUIRES_MANUAL_MIGRATION;
            case BUILD_TOOL_ERROR -> DependencyMigrationStatus.BUILD_TOOL_ERROR;
            case BYTECODE_SCAN_UNKNOWN, UNKNOWN -> DependencyMigrationStatus.UNKNOWN_REVIEW;
        };
    }

    public DependencyGraph buildDependencyGraphFromDeepResult(TransitiveDependencyProjectScanResult deepResult) {
        if (deepResult == null) {
            return new DependencyGraph();
        }

        Set<Artifact> nodes = new HashSet<>();
        Set<Dependency> edges = new HashSet<>();

        // Map from artifact key to Artifact object for deduplication
        Map<String, Artifact> artifactMap = new HashMap<>();

        // Build a lookup map from 2-part key (groupId:artifactId) → version from usages
        // This is needed because TransitiveDependencyEdge uses 2-part keys
        Map<String, String> versionLookup = new HashMap<>();
        for (adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyScanResult fileResult : deepResult.getFileResults()) {
            for (adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyUsage usage : fileResult.getUsages()) {
                String idKey = usage.getGroupId() + ":" + usage.getArtifactId();
                if (usage.getVersion() != null) {
                    versionLookup.putIfAbsent(idKey, usage.getVersion());
                }
            }
        }

        // Access edges through fileResults since getAllEdges() might not be available in all versions
        for (adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyScanResult fileResult : deepResult.getFileResults()) {
            for (adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyEdge edge : fileResult.getEdges()) {
                String parentKey = edge.parentArtifactKey();
                String childKey = edge.childArtifactKey();

                // Parse or create parent artifact
                Artifact parentArtifact = artifactMap.computeIfAbsent(parentKey, key -> {
                    String[] parts = key.split(":");
                    if (parts.length >= 3) {
                        return new Artifact(parts[0], parts[1], parts[2], "compile", true);
                    }
                    if (parts.length == 2) {
                        String version = versionLookup.getOrDefault(key, "unknown");
                        return new Artifact(parts[0], parts[1], version, "compile", true);
                    }
                    return null;
                });

                // Parse or create child artifact
                Artifact childArtifact = artifactMap.computeIfAbsent(childKey, key -> {
                    String[] parts = key.split(":");
                    if (parts.length >= 3) {
                        return new Artifact(parts[0], parts[1], parts[2], "compile", true);
                    }
                    if (parts.length == 2) {
                        String version = versionLookup.getOrDefault(key, "unknown");
                        return new Artifact(parts[0], parts[1], version, "compile", true);
                    }
                    return null;
                });

                // Create dependency edge if both artifacts were parsed successfully
                if (parentArtifact != null && childArtifact != null) {
                    nodes.add(parentArtifact);
                    nodes.add(childArtifact);
                    edges.add(new Dependency(parentArtifact, childArtifact, "compile", false));
                }
            }
        }

        // Also create nodes from usages to ensure all discovered dependencies are present
        // even when edge information is missing (e.g. regex fallback scan)
        for (adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyScanResult fileResult : deepResult.getFileResults()) {
            for (adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyUsage usage : fileResult.getUsages()) {
                String groupId = usage.getGroupId();
                String artifactId = usage.getArtifactId();
                if (groupId == null || artifactId == null) {
                    continue;
                }
                String version = usage.getVersion() != null ? usage.getVersion() : "unknown";
                String scope = usage.getScope() != null ? usage.getScope() : "compile";
                boolean transitive = usage.isTransitive();
                String key = groupId + ":" + artifactId + ":" + version;

                Artifact artifact = artifactMap.computeIfAbsent(key, k ->
                        new Artifact(groupId, artifactId, version, scope, transitive));
                nodes.add(artifact);
            }
        }

        return new DependencyGraph(nodes, edges);
    }

    public TransitiveDependencyProjectScanResult scanDependenciesDeep(Path projectPath, ScanProgressListener progressListener) {
        // Wrap progress listener with throttled wrapper to prevent EDT flooding
        ThrottledProgressListener throttledListener = null;
        if (progressListener != null) {
            throttledListener = new ThrottledProgressListener(progressListener);
        }
        
        try {
            // Full transitive dependency scan using scanner with progress callback
            ScanProgressCallback callback = toScanProgressCallback(throttledListener);
            return scanningModule.getTransitiveDependencyScanner().scanProject(projectPath, callback);
        } finally {
            // Clean up throttled listener
            if (throttledListener != null) {
                throttledListener.shutdown();
            }
        }
    }

    /**
     * Overload for backward compatibility - uses null listener.
     */
    public TransitiveDependencyProjectScanResult scanDependenciesDeep(Path projectPath) {
        return scanDependenciesDeep(projectPath, null);
    }

    /**
     * Runs advanced scans excluding transitive dependency analysis (faster quick scan).
     */
    public AdvancedScanSummary scanAllExcludingTransitive(Path projectPath, ScanProgressListener progressListener) {
        // Wrap progress listener with throttled wrapper to prevent EDT flooding
        ThrottledProgressListener throttledListener = null;
        if (progressListener != null) {
            throttledListener = new ThrottledProgressListener(progressListener);
        }
        
        try {
            // Sequential execution already excludes transitive; reuse that implementation
            return runScansSequentially(projectPath, throttledListener);
        } finally {
            // Clean up throttled listener
            if (throttledListener != null) {
                throttledListener.shutdown();
            }
        }
    }

    /**
     * Runs quick scans sequentially - legacy compatibility.
     */
    public AdvancedScanSummary runQuickScansSequentially(Path projectPath, ScanProgressListener progressListener) {
        return runScansSequentially(projectPath, progressListener);
    }

    /**
     * Sets the cached scan summary directly (primarily for testing).
     */
    public void setCachedSummary(AdvancedScanSummary summary) {
        this.cachedSummaryRef = new java.lang.ref.SoftReference<>(summary);
        this.cachedProjectPath = summary != null ? Path.of("") : null;
        this.lastScanTime = summary != null ? System.currentTimeMillis() : 0;
    }

    /**
     * Determines migration status for a transitive dependency based on its classification.
     * Maps ScanReason to a user-facing DependencyMigrationStatus.
     */
    public DependencyMigrationStatus determineMigrationStatus(TransitiveDependencyUsage usage) {
        if (usage == null) {
            return DependencyMigrationStatus.UNKNOWN;
        }
        if (usage.getScanReason() != null) {
            return determineMigrationStatus(usage.getScanReason());
        }
        return mapSeverityToMigrationStatus(usage.getSeverity());
    }

    private static DependencyMigrationStatus mapSeverityToMigrationStatus(String severity) {
        if (severity == null) {
            return DependencyMigrationStatus.UNKNOWN_REVIEW;
        }
        return switch (severity.toLowerCase()) {
            case "high", "critical", "medium" -> DependencyMigrationStatus.NEEDS_UPGRADE;
            case "low", "none" -> DependencyMigrationStatus.COMPATIBLE;
            case "unknown" -> DependencyMigrationStatus.UNKNOWN_REVIEW;
            default -> DependencyMigrationStatus.UNKNOWN_REVIEW;
        };
    }
}
