package adrianmikula.jakartamigration.intellij.service;

import adrianmikula.jakartamigration.advancedscanning.domain.*;
import adrianmikula.jakartamigration.advancedscanning.service.AdvancedScanningModule;
import adrianmikula.jakartamigration.advancedscanning.service.*;
import com.intellij.openapi.diagnostic.Logger;

import java.nio.file.Path;
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
    private final java.util.concurrent.ExecutorService scanExecutor = java.util.concurrent.Executors
            .newWorkStealingPool();

    public AdvancedScanningService() {
        // Initialize the scanning module
        this.scanningModule = new AdvancedScanningModule();
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
        LOG.info("Running parallel advanced scans in: " + projectPath);

        AdvancedScanSummary existing = cachedSummaryRef.get();
        // Check if cached result is still valid
        if (existing != null && cachedProjectPath != null
                && cachedProjectPath.equals(projectPath)
                && (System.currentTimeMillis() - lastScanTime) < CACHE_VALIDITY_MS) {
            LOG.info("Returning cached scan results");
            return existing;
        }

        try {
            // Execute all scans in parallel
            CompletableFuture<JpaProjectScanResult> jpaFuture = CompletableFuture
                    .supplyAsync(() -> scanForJpaAnnotations(projectPath), scanExecutor);
            CompletableFuture<BeanValidationProjectScanResult> bvFuture = CompletableFuture
                    .supplyAsync(() -> scanForBeanValidation(projectPath), scanExecutor);
            CompletableFuture<ServletJspProjectScanResult> sjFuture = CompletableFuture
                    .supplyAsync(() -> scanForServletJsp(projectPath), scanExecutor);
            CompletableFuture<CdiInjectionProjectScanResult> cdiFuture = CompletableFuture
                    .supplyAsync(() -> scanForCdiInjection(projectPath), scanExecutor);
            CompletableFuture<BuildConfigProjectScanResult> bcFuture = CompletableFuture
                    .supplyAsync(() -> scanForBuildConfig(projectPath), scanExecutor);
            CompletableFuture<RestSoapProjectScanResult> rsFuture = CompletableFuture
                    .supplyAsync(() -> scanForRestSoap(projectPath), scanExecutor);
            CompletableFuture<DeprecatedApiProjectScanResult> daFuture = CompletableFuture
                    .supplyAsync(() -> scanForDeprecatedApi(projectPath), scanExecutor);
            CompletableFuture<SecurityApiProjectScanResult> saFuture = CompletableFuture
                    .supplyAsync(() -> scanForSecurityApi(projectPath), scanExecutor);
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
            CompletableFuture<ThirdPartyLibProjectScanResult> tpFuture = CompletableFuture
                    .supplyAsync(() -> scanForThirdPartyLib(projectPath), scanExecutor);

            // Wait for all to complete
            CompletableFuture.allOf(jpaFuture, bvFuture, sjFuture, cdiFuture, bcFuture, rsFuture, daFuture, saFuture,
                    jmFuture, tdFuture, cfFuture, clFuture, lmFuture, scFuture, tpFuture).join();

            AdvancedScanSummary summary = new AdvancedScanSummary(
                    jpaFuture.join(),
                    bvFuture.join(),
                    sjFuture.join(),
                    cdiFuture.join(),
                    bcFuture.join(),
                    rsFuture.join(),
                    daFuture.join(),
                    saFuture.join(),
                    jmFuture.join(),
                    tdFuture.join(),
                    cfFuture.join(),
                    clFuture.join(),
                    lmFuture.join(),
                    scFuture.join(),
                    tpFuture.join());

            cachedSummaryRef = new java.lang.ref.SoftReference<>(summary);
            cachedProjectPath = projectPath;
            lastScanTime = System.currentTimeMillis();

            // Suggest GC after heavy scan to reclaim memory from ASTs
            System.gc();

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
     * Returns whether a scan has been performed and cached.
     * 
     * @return true if valid cached results exist
     */
    public boolean hasCachedResults() {
        return cachedSummaryRef.get() != null;
    }

    // Individual scan methods for each scanner type
    public JpaProjectScanResult scanForJpaAnnotations(Path projectPath) {
        LOG.info("Scanning for JPA annotations in: " + projectPath);
        return scanningModule.getJpaAnnotationScanner().scanProject(projectPath);
    }

    public BeanValidationProjectScanResult scanForBeanValidation(Path projectPath) {
        LOG.info("Scanning for Bean Validation in: " + projectPath);
        return scanningModule.getBeanValidationScanner().scanProject(projectPath);
    }

    public ServletJspProjectScanResult scanForServletJsp(Path projectPath) {
        LOG.info("Scanning for Servlet/JSP in: " + projectPath);
        return scanningModule.getServletJspScanner().scanProject(projectPath);
    }

    public CdiInjectionProjectScanResult scanForCdiInjection(Path projectPath) {
        LOG.info("Scanning for CDI Injection in: " + projectPath);
        return scanningModule.getCdiInjectionScanner().scanProject(projectPath);
    }

    public BuildConfigProjectScanResult scanForBuildConfig(Path projectPath) {
        LOG.info("Scanning for Build Config in: " + projectPath);
        return scanningModule.getBuildConfigScanner().scanProject(projectPath);
    }

    public RestSoapProjectScanResult scanForRestSoap(Path projectPath) {
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
        return scanningModule.getTransitiveDependencyScanner().scanProject(projectPath);
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

    public ThirdPartyLibProjectScanResult scanForThirdPartyLib(Path projectPath) {
        LOG.info("Scanning for Third-Party Libs in: " + projectPath);
        return thirdPartyLibScanner.scanProject(projectPath);
    }

    /**
     * Summary of all advanced scanning results.
     */
    public record AdvancedScanSummary(
            JpaProjectScanResult jpaResult,
            BeanValidationProjectScanResult beanValidationResult,
            ServletJspProjectScanResult servletJspResult,
            CdiInjectionProjectScanResult cdiInjectionResult,
            BuildConfigProjectScanResult buildConfigResult,
            RestSoapProjectScanResult restSoapResult,
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
            return jpaResult != null ? jpaResult.totalAnnotationsFound() : 0;
        }

        /**
         * Returns individual count for Bean Validation.
         */
        public int getBeanValidationCount() {
            return beanValidationResult != null ? beanValidationResult.totalAnnotationsFound() : 0;
        }

        /**
         * Returns individual count for Servlet/JSP.
         */
        public int getServletJspCount() {
            return servletJspResult != null ? servletJspResult.totalUsagesFound() : 0;
        }

        /**
         * Returns individual count for CDI Injection.
         */
        public int getCdiInjectionCount() {
            return cdiInjectionResult != null ? cdiInjectionResult.totalAnnotationsFound() : 0;
        }

        /**
         * Returns individual count for Build Config.
         */
        public int getBuildConfigCount() {
            return buildConfigResult != null ? buildConfigResult.totalDependenciesFound() : 0;
        }

        /**
         * Returns individual count for REST/SOAP.
         */
        public int getRestSoapCount() {
            return restSoapResult != null ? restSoapResult.totalUsagesFound() : 0;
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
                total += jpaResult.totalAnnotationsFound();
            }
            if (beanValidationResult != null) {
                total += beanValidationResult.totalAnnotationsFound();
            }
            if (servletJspResult != null) {
                total += servletJspResult.totalUsagesFound();
            }
            if (cdiInjectionResult != null) {
                total += cdiInjectionResult.totalAnnotationsFound();
            }
            if (buildConfigResult != null) {
                total += buildConfigResult.totalDependenciesFound();
            }
            if (restSoapResult != null) {
                total += restSoapResult.totalUsagesFound();
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
