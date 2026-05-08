package adrianmikula.jakartamigration.intellij.service;

import adrianmikula.jakartamigration.advancedscanning.domain.*;
import adrianmikula.jakartamigration.advancedscanning.service.*;
import adrianmikula.jakartamigration.advancedscanning.service.impl.DependencyTreeCommandExecutorImpl;
import adrianmikula.jakartamigration.coderefactoring.service.RecipeService;
import adrianmikula.jakartamigration.intellij.ui.ScanProgressListener;
import adrianmikula.jakartamigration.util.ProjectFileSystemScanner;
import adrianmikula.jakartamigration.advancedscanning.domain.DockerCicdUsage;
import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyUsage;
import adrianmikula.jakartamigration.intellij.ui.DashboardComponent;
import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.advancedscanning.service.ScanRecipeRecommendationService;
import com.intellij.openapi.diagnostic.Logger;

import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Dependency;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;

import java.nio.file.Path;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Service for performing advanced scanning using premium core engine.
 * This service provides access to the premium scanning features.
 */
public class AdvancedScanningService {
    private static final Logger LOG = Logger.getInstance(AdvancedScanningService.class);

    private final AdvancedScanningModule scanningModule;
    private final ThirdPartyLibScanner thirdPartyLibScanner;
    private final ProjectFileSystemScanner fsScanner = new ProjectFileSystemScanner();

    // Cache for the last scan results using SoftReference to prevent OOM
    private java.lang.ref.SoftReference<AdvancedScanSummary> cachedSummaryRef = new java.lang.ref.SoftReference<>(null);
    private Path cachedProjectPath;
    private long lastScanTime;

    private static final long CACHE_VALIDITY_MS = 5 * 60 * 1000; // 5 minutes
    
    // Memory optimization: Limit parallel scanning to prevent OOM
    private static final int MAX_PARALLEL_SCANS = 2;
    
    // Use a bounded thread pool to control memory usage
    private final java.util.concurrent.ExecutorService scanExecutor = java.util.concurrent.Executors
            .newFixedThreadPool(MAX_PARALLEL_SCANS);

    public AdvancedScanningService(RecipeService recipeService) {
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
            Map<FileCategory, List<Path>> allFiles = discoverAllFilesOnce(projectPath);
            
            java.util.List<CompletableFuture<?>> futures = new java.util.ArrayList<>();
            
            LOG.info("=== Starting Batch 1: Core Scans ===");
            if (progressListener != null) {
                progressListener.onScanPhase("Advanced Scans (Batch 1/3)", 0, 3);
            }
            
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
             
            CompletableFuture.allOf(jpaFuture, bvFuture, sjFuture, cdiFuture).join();
            LOG.info("Batch 1 completed");
             
            if (progressListener != null) {
                progressListener.onScanPhase("Advanced Scans (Batch 2/3)", 1, 3);
            }
             
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
             
            CompletableFuture.allOf(bcFuture, rsFuture, daFuture, saFuture).join();
            LOG.info("Batch 2 completed");
             
            if (progressListener != null) {
                progressListener.onScanPhase("Advanced Scans (Batch 3/3)", 2, 3);
            }
             
            CompletableFuture<JmsMessagingProjectScanResult> jmFuture = CompletableFuture
                    .supplyAsync(() -> scanForJmsMessaging(allFiles.get(FileCategory.JAVA)), scanExecutor);
            CompletableFuture<TransitiveDependencyProjectScanResult> tdFuture = CompletableFuture
                    .supplyAsync(() -> scanForTransitiveDependencies(allFiles.get(FileCategory.BUILD)), scanExecutor);
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

            CompletableFuture.allOf(jmFuture, tdFuture, cfFuture, clFuture, lmFuture, scFuture, ruFuture, tpFuture).join();
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
        
        // Java source files
        files.put(FileCategory.JAVA, fsScanner.findFiles(projectPath, List.of(".java")));
        
        // Test files (filtered from .java files)
        List<Path> javaFiles = fsScanner.findFiles(projectPath, List.of(".java"));
        List<Path> testFiles = javaFiles.stream()
                .filter(f -> isTestFile(f, projectPath))
                .collect(Collectors.toList());
        files.put(FileCategory.TEST, testFiles);
        
        // Config files
        files.put(FileCategory.CONFIG, fsScanner.findFiles(projectPath, List.of(".xml", ".properties", ".yaml", ".yml")));
        
        // Build files (pom.xml, build.gradle, etc.)
        files.put(FileCategory.BUILD, fsScanner.findFiles(projectPath, path -> {
            String name = path.getFileName().toString();
            return name.equals("pom.xml") || name.startsWith("build.gradle") || name.endsWith(".gradle") || name.endsWith(".gradle.kts");
        }));
        
        // Dockerfiles
        files.put(FileCategory.DOCKER, fsScanner.findFiles(projectPath, path -> {
            String name = path.getFileName().toString();
            return name.equals("Dockerfile") || name.equals("dockerfile");
        }));
        
        // JSP files
        files.put(FileCategory.JSP, fsScanner.findFiles(projectPath, List.of(".jsp")));
        
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
    
    public TransitiveDependencyProjectScanResult scanForTransitiveDependencies(List<Path> buildFiles) {
        LOG.info("Scanning " + buildFiles.size() + " build files for Transitive Dependencies");
        return scanningModule.getTransitiveDependencyScanner().scanProject(buildFiles);
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
            if (progressListener != null) {
                progressListener.onScanPhase("Advanced Scans (Sequential)", 0, 1);
            }
            
            Map<FileCategory, List<Path>> allFiles = discoverAllFilesOnce(projectPath);
            
            ProjectScanResult<FileScanResult<JpaAnnotationUsage>> jpaResult = scanForJpaAnnotations(allFiles.get(FileCategory.JAVA));
            if (progressListener != null && jpaResult != null && !jpaResult.fileResults().isEmpty()) {
                int totalFindings = jpaResult.fileResults().stream().mapToInt(fr -> fr.usages().size()).sum();
                progressListener.onSubScanComplete("JPA", totalFindings);
            }
            
            ProjectScanResult<FileScanResult<JavaxUsage>> beanValidationResult = scanForBeanValidation(allFiles.get(FileCategory.JAVA));
            if (progressListener != null && beanValidationResult != null && !beanValidationResult.fileResults().isEmpty()) {
                int totalFindings = beanValidationResult.fileResults().stream().mapToInt(fr -> fr.usages().size()).sum();
                progressListener.onSubScanComplete("Bean Validation", totalFindings);
            }
            
            ProjectScanResult<FileScanResult<ServletJspUsage>> servletJspResult = scanForServletJsp(allFiles.get(FileCategory.JAVA));
            if (progressListener != null && servletJspResult != null && !servletJspResult.fileResults().isEmpty()) {
                int totalFindings = servletJspResult.fileResults().stream().mapToInt(fr -> fr.usages().size()).sum();
                progressListener.onSubScanComplete("Servlet/JSP", totalFindings);
            }
            
            ProjectScanResult<FileScanResult<JavaxUsage>> cdiInjectionResult = scanForCdiInjection(allFiles.get(FileCategory.JAVA));
            if (progressListener != null && cdiInjectionResult != null && !cdiInjectionResult.fileResults().isEmpty()) {
                int totalFindings = cdiInjectionResult.fileResults().stream().mapToInt(fr -> fr.usages().size()).sum();
                progressListener.onSubScanComplete("CDI Injection", totalFindings);
            }
            
            ProjectScanResult<FileScanResult<BuildConfigUsage>> buildConfigResult = scanForBuildConfig(allFiles.get(FileCategory.BUILD));
            if (progressListener != null && buildConfigResult != null && !buildConfigResult.fileResults().isEmpty()) {
                int totalFindings = buildConfigResult.fileResults().stream().mapToInt(fr -> fr.usages().size()).sum();
                progressListener.onSubScanComplete("Build Config", totalFindings);
            }
            
            ProjectScanResult<FileScanResult<JavaxUsage>> restSoapResult = scanForRestSoap(allFiles.get(FileCategory.JAVA));
            if (progressListener != null && restSoapResult != null && !restSoapResult.fileResults().isEmpty()) {
                int totalFindings = restSoapResult.fileResults().stream().mapToInt(fr -> fr.usages().size()).sum();
                progressListener.onSubScanComplete("REST/SOAP", totalFindings);
            }
            
            DeprecatedApiProjectScanResult deprecatedApiResult = scanForDeprecatedApi(allFiles.get(FileCategory.JAVA));
            SecurityApiProjectScanResult securityApiResult = scanForSecurityApi(allFiles.get(FileCategory.JAVA));
            JmsMessagingProjectScanResult jmsMessagingResult = scanForJmsMessaging(allFiles.get(FileCategory.JAVA));
            ConfigFileProjectScanResult configFileResult = scanForConfigFiles(allFiles.get(FileCategory.CONFIG));
            ClassloaderModuleProjectScanResult classloaderModuleResult = scanForClassloaderModule(allFiles.get(FileCategory.JAVA));
            LoggingMetricsProjectScanResult loggingMetricsResult = scanForLoggingMetrics(allFiles.get(FileCategory.JAVA));
            SerializationCacheProjectScanResult serializationCacheResult = scanForSerializationCache(allFiles.get(FileCategory.JAVA));
            ReflectionUsageProjectScanResult reflectionUsageResult = scanForReflectionUsage(allFiles.get(FileCategory.JAVA));
            ThirdPartyLibProjectScanResult thirdPartyLibResult = scanForThirdPartyLib(allFiles.get(FileCategory.BUILD));
            
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
                    null, // transitiveDependencyResult - EXCLUDED for quick scan
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
        List<Path> javaFiles = fsScanner.findFiles(projectPath, List.of(".java"));
        return scanForJpaAnnotations(javaFiles);
    }
    
    @Deprecated
    public ProjectScanResult<FileScanResult<JavaxUsage>> scanForBeanValidation(Path projectPath) {
        LOG.info("Scanning for Bean Validation in: " + projectPath);
        List<Path> javaFiles = fsScanner.findFiles(projectPath, List.of(".java"));
        return scanForBeanValidation(javaFiles);
    }
    
    @Deprecated
    public ProjectScanResult<FileScanResult<ServletJspUsage>> scanForServletJsp(Path projectPath) {
        LOG.info("Scanning for Servlet/JSP in: " + projectPath);
        // ServletJspScanner needs both .java and .jsp files
        List<Path> allFiles = new ArrayList<>(fsScanner.findFiles(projectPath, List.of(".java")));
        allFiles.addAll(fsScanner.findFiles(projectPath, List.of(".jsp")));
        return scanForServletJsp(allFiles);
    }
    
    @Deprecated
    public ProjectScanResult<FileScanResult<JavaxUsage>> scanForCdiInjection(Path projectPath) {
        LOG.info("Scanning for CDI Injection in: " + projectPath);
        List<Path> javaFiles = fsScanner.findFiles(projectPath, List.of(".java"));
        return scanForCdiInjection(javaFiles);
    }
    
    @Deprecated
    public ProjectScanResult<FileScanResult<BuildConfigUsage>> scanForBuildConfig(Path projectPath) {
        LOG.info("Scanning for Build Config in: " + projectPath);
        List<Path> buildFiles = fsScanner.findFiles(projectPath, path -> {
            String name = path.getFileName().toString();
            return name.equals("pom.xml") || name.startsWith("build.gradle");
        });
        return scanForBuildConfig(buildFiles);
    }
    
    @Deprecated
    public ProjectScanResult<FileScanResult<JavaxUsage>> scanForRestSoap(Path projectPath) {
        LOG.info("Scanning for REST/SOAP in: " + projectPath);
        List<Path> javaFiles = fsScanner.findFiles(projectPath, List.of(".java"));
        return scanForRestSoap(javaFiles);
    }
    
    @Deprecated
    public DeprecatedApiProjectScanResult scanForDeprecatedApi(Path projectPath) {
        LOG.info("Scanning for Deprecated API in: " + projectPath);
        List<Path> javaFiles = fsScanner.findFiles(projectPath, List.of(".java"));
        return scanForDeprecatedApi(javaFiles);
    }
    
    @Deprecated
    public SecurityApiProjectScanResult scanForSecurityApi(Path projectPath) {
        LOG.info("Scanning for Security API in: " + projectPath);
        List<Path> javaFiles = fsScanner.findFiles(projectPath, List.of(".java"));
        return scanForSecurityApi(javaFiles);
    }
    
    @Deprecated
    public JmsMessagingProjectScanResult scanForJmsMessaging(Path projectPath) {
        LOG.info("Scanning for JMS Messaging in: " + projectPath);
        List<Path> javaFiles = fsScanner.findFiles(projectPath, List.of(".java"));
        return scanForJmsMessaging(javaFiles);
    }
    
    @Deprecated
    public TransitiveDependencyProjectScanResult scanForTransitiveDependencies(Path projectPath) {
        LOG.info("Scanning for Transitive Dependencies in: " + projectPath);
        List<Path> buildFiles = fsScanner.findFiles(projectPath, path -> {
            String name = path.getFileName().toString();
            return name.equals("pom.xml") || name.startsWith("build.gradle");
        });
        return scanForTransitiveDependencies(buildFiles);
    }
    
    @Deprecated
    public ConfigFileProjectScanResult scanForConfigFiles(Path projectPath) {
        LOG.info("Scanning for Config Files in: " + projectPath);
        List<Path> configFiles = fsScanner.findFiles(projectPath, List.of(".xml", ".properties", ".yaml", ".yml"));
        return scanForConfigFiles(configFiles);
    }
    
    @Deprecated
    public ClassloaderModuleProjectScanResult scanForClassloaderModule(Path projectPath) {
        LOG.info("Scanning for Classloader/Module in: " + projectPath);
        List<Path> javaFiles = fsScanner.findFiles(projectPath, List.of(".java"));
        return scanForClassloaderModule(javaFiles);
    }
    
    @Deprecated
    public LoggingMetricsProjectScanResult scanForLoggingMetrics(Path projectPath) {
        LOG.info("Scanning for Logging/Metrics in: " + projectPath);
        List<Path> javaFiles = fsScanner.findFiles(projectPath, List.of(".java"));
        return scanForLoggingMetrics(javaFiles);
    }
    
    @Deprecated
    public SerializationCacheProjectScanResult scanForSerializationCache(Path projectPath) {
        LOG.info("Scanning for Serialization/Cache in: " + projectPath);
        List<Path> javaFiles = fsScanner.findFiles(projectPath, List.of(".java"));
        return scanForSerializationCache(javaFiles);
    }
    
    @Deprecated
    public ReflectionUsageProjectScanResult scanForReflectionUsage(Path projectPath) {
        LOG.info("Scanning for Reflection Usage in: " + projectPath);
        // Reflection scanner needs .java, .kt, .scala
        List<Path> allSource = new ArrayList<>(fsScanner.findFiles(projectPath, List.of(".java")));
        allSource.addAll(fsScanner.findFiles(projectPath, List.of(".kt")));
        allSource.addAll(fsScanner.findFiles(projectPath, List.of(".scala")));
        return scanForReflectionUsage(allSource);
    }
    
    @Deprecated
    public ThirdPartyLibProjectScanResult scanForThirdPartyLib(Path projectPath) {
        LOG.info("Scanning for Third-Party Libraries in: " + projectPath);
        List<Path> buildFiles = fsScanner.findFiles(projectPath, path -> {
            String name = path.getFileName().toString();
            return name.equals("pom.xml") || name.startsWith("build.gradle") || 
                   name.equals("Dockerfile") || name.equals("dockerfile");
        });
        return scanForThirdPartyLib(buildFiles);
    }
    
    @Deprecated
    public UnitTestProjectScanResult scanForUnitTests(Path projectPath) {
        LOG.info("Scanning for Unit Tests in: " + projectPath);
        List<Path> javaFiles = fsScanner.findFiles(projectPath, List.of(".java"));
        return scanningModule.getUnitTestScanner().scanProject(javaFiles);
    }
    
    @Deprecated
    public IntegrationPointsProjectScanResult scanForIntegrationPoints(Path projectPath) {
        LOG.info("Scanning for Integration Points in: " + projectPath);
        List<Path> javaFiles = fsScanner.findFiles(projectPath, List.of(".java"));
        return scanningModule.getIntegrationPointsScanner().scanProject(javaFiles);
    }
    
    @Deprecated
    public AppServerProjectScanResult scanForAppServer(Path projectPath) {
        LOG.info("Scanning for App Server Config in: " + projectPath);
        List<Path> javaFiles = fsScanner.findFiles(projectPath, List.of(".java"));
        return scanningModule.getAppServerScanner().scanProject(javaFiles);
    }
    
    @Deprecated
    public ProjectScanResult<FileScanResult<DockerCicdUsage>> scanForDockerCicd(Path projectPath) {
        LOG.info("Scanning for Docker/CI-CD in: " + projectPath);
        List<Path> dockerFiles = fsScanner.findFiles(projectPath, path -> {
            String name = path.getFileName().toString();
            return name.equals("Dockerfile") || name.equals("dockerfile") ||
                   name.endsWith(".yml") || name.endsWith(".yaml");
        });
        return scanningModule.getDockerCicdScanner().scanProject(dockerFiles);
    }
    
    @Deprecated
    public TestContainersProjectScanResult scanForTestContainers(Path projectPath) {
        LOG.info("Scanning for TestContainers in: " + projectPath);
        List<Path> buildFiles = fsScanner.findFiles(projectPath, path -> {
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
        return new ComprehensiveScanResults(
                "",
                LocalDateTime.now(),
                Map.of(), // jpaResults
                Map.of(), // beanValidationResults
                Map.of(), // cdiResults
                Map.of(), // servletJspResults
                Map.of(), // thirdPartyLibResults
                Map.of(), // transitiveDependencyResults
                Map.of(), // buildConfigResults
                List.of(),
                0,
                new ComprehensiveScanResults.ScanSummary(0, 0, 0, 0, 0, 0)
        );
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

        public int getTotalIssuesFound() {
            return getJpaCount() + getBeanValidationCount() + getServletJspCount() + getCdiInjectionCount() + getBuildConfigCount() + getRestSoapCount() + getDeprecatedApiCount() + getSecurityApiCount() + getJmsMessagingCount() + getConfigFileCount() + getTransitiveDependencyCount() + getClassloaderModuleCount() + getLoggingMetricsCount() + getSerializationCacheCount() + getThirdPartyLibCount();
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
        // TODO: Implement conversion from TransitiveDependencyProjectScanResult to DependencyInfo list
        return List.of();
    }

    public DependencyGraph buildDependencyGraphFromDeepResult(TransitiveDependencyProjectScanResult deepResult) {
        // TODO: Build full dependency graph including transitive edges
        return new DependencyGraph();
    }

    public TransitiveDependencyProjectScanResult scanDependenciesDeep(Path projectPath, ScanProgressListener progressListener) {
        // Full transitive dependency scan using scanner directly
        return scanningModule.getTransitiveDependencyScanner().scanProject(projectPath);
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
        // Sequential execution already excludes transitive; reuse that implementation
        return runScansSequentially(projectPath, progressListener);
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
        if (usage == null || usage.getScanReason() == null) {
            return DependencyMigrationStatus.UNKNOWN;
        }
        ScanReason reason = usage.getScanReason();
        return switch (reason) {
            case WHITELISTED, BYTECODE_SCAN_JAKARTA, MAVEN_LOOKUP_FOUND -> DependencyMigrationStatus.COMPATIBLE;
            case BLACKLISTED, BYTECODE_SCAN_JAVAX, TRANSITIVE_INCOMPATIBLE -> DependencyMigrationStatus.NEEDS_UPGRADE;
            case MAVEN_LOOKUP_NONE -> DependencyMigrationStatus.NO_JAKARTA_VERSION;
            case BYTECODE_SCAN_MIXED, REVIEW_REQUIRED -> DependencyMigrationStatus.REQUIRES_MANUAL_MIGRATION;
            case BYTECODE_SCAN_UNKNOWN, UNKNOWN -> DependencyMigrationStatus.UNKNOWN_REVIEW;
        };
    }
}
