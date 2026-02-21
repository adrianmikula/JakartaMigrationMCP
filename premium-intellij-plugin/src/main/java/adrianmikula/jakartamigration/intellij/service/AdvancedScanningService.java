package adrianmikula.jakartamigration.intellij.service;

import adrianmikula.jakartamigration.advancedscanning.domain.*;
import adrianmikula.jakartamigration.advancedscanning.service.AdvancedScanningModule;
import adrianmikula.jakartamigration.advancedscanning.service.*;
import com.intellij.openapi.diagnostic.Logger;

import java.nio.file.Path;

/**
 * Service for performing advanced scanning using premium core engine.
 * This service provides access to the premium scanning features.
 */
public class AdvancedScanningService {
    private static final Logger LOG = Logger.getInstance(AdvancedScanningService.class);

    private final AdvancedScanningModule scanningModule;
    private final JpaAnnotationScanner jpaScanner;
    private final BeanValidationScanner beanValidationScanner;
    private final ServletJspScanner servletJspScanner;
    private final CdiInjectionScanner cdiInjectionScanner;
    private final BuildConfigScanner buildConfigScanner;
    private final RestSoapScanner restSoapScanner;
    private final DeprecatedApiScanner deprecatedApiScanner;
    private final SecurityApiScanner securityApiScanner;
    private final JmsMessagingScanner jmsMessagingScanner;
    private final TransitiveDependencyScanner transitiveDependencyScanner;
    private final ConfigFileScanner configFileScanner;
    private final ClassloaderModuleScanner classloaderModuleScanner;
    private final LoggingMetricsScanner loggingMetricsScanner;
    private final SerializationCacheScanner serializationCacheScanner;
    private final ThirdPartyLibScanner thirdPartyLibScanner;

    // Cache for the last scan results
    private AdvancedScanSummary cachedSummary;
    private Path cachedProjectPath;
    private long lastScanTime;

    private static final long CACHE_VALIDITY_MS = 5 * 60 * 1000; // 5 minutes

    public AdvancedScanningService() {
        // Initialize the scanning module
        this.scanningModule = new AdvancedScanningModule();
        this.jpaScanner = scanningModule.getJpaAnnotationScanner();
        this.beanValidationScanner = scanningModule.getBeanValidationScanner();
        this.servletJspScanner = scanningModule.getServletJspScanner();
        this.cdiInjectionScanner = scanningModule.getCdiInjectionScanner();
        this.buildConfigScanner = scanningModule.getBuildConfigScanner();
        this.restSoapScanner = scanningModule.getRestSoapScanner();
        this.deprecatedApiScanner = scanningModule.getDeprecatedApiScanner();
        this.securityApiScanner = scanningModule.getSecurityApiScanner();
        this.jmsMessagingScanner = scanningModule.getJmsMessagingScanner();
        this.transitiveDependencyScanner = scanningModule.getTransitiveDependencyScanner();
        this.configFileScanner = scanningModule.getConfigFileScanner();
        this.classloaderModuleScanner = scanningModule.getClassloaderModuleScanner();
        this.loggingMetricsScanner = scanningModule.getLoggingMetricsScanner();
        this.serializationCacheScanner = scanningModule.getSerializationCacheScanner();
        this.thirdPartyLibScanner = scanningModule.getThirdPartyLibScanner();

        LOG.info("AdvancedScanningService initialized with premium scanning features");
    }

    /**
     * Scans a project for JPA/Hibernate annotations.
     *
     * @param projectPath Path to the project root directory
     * @return JpaProjectScanResult containing the analysis results
     */
    public JpaProjectScanResult scanForJpaAnnotations(Path projectPath) {
        LOG.info("Scanning for JPA annotations in: " + projectPath);
        return jpaScanner.scanProject(projectPath);
    }

    /**
     * Scans a project for Bean Validation constraints.
     *
     * @param projectPath Path to the project root directory
     * @return BeanValidationProjectScanResult containing the analysis results
     */
    public BeanValidationProjectScanResult scanForBeanValidation(Path projectPath) {
        LOG.info("Scanning for Bean Validation in: " + projectPath);
        return beanValidationScanner.scanProject(projectPath);
    }

    /**
     * Scans a project for Servlet/JSP usage.
     *
     * @param projectPath Path to the project root directory
     * @return ServletJspProjectScanResult containing the analysis results
     */
    public ServletJspProjectScanResult scanForServletJsp(Path projectPath) {
        LOG.info("Scanning for Servlet/JSP in: " + projectPath);
        return servletJspScanner.scanProject(projectPath);
    }

    /**
     * Scans a project for all advanced scanning types.
     * Results are cached for 5 minutes.
     *
     * @param projectPath Path to the project root directory
     * @return AdvancedScanSummary containing combined results
     */
    public AdvancedScanSummary scanAll(Path projectPath) {
        LOG.info("Running all advanced scans in: " + projectPath);
        
        // Check if cached result is still valid
        if (cachedSummary != null && cachedProjectPath != null 
            && cachedProjectPath.equals(projectPath)
            && (System.currentTimeMillis() - lastScanTime) < CACHE_VALIDITY_MS) {
            LOG.info("Returning cached scan results");
            return cachedSummary;
        }
        
        JpaProjectScanResult jpaResult = scanForJpaAnnotations(projectPath);
        BeanValidationProjectScanResult beanValidationResult = scanForBeanValidation(projectPath);
        ServletJspProjectScanResult servletJspResult = scanForServletJsp(projectPath);
        CdiInjectionProjectScanResult cdiInjectionResult = scanForCdiInjection(projectPath);
        BuildConfigProjectScanResult buildConfigResult = scanForBuildConfig(projectPath);
        RestSoapProjectScanResult restSoapResult = scanForRestSoap(projectPath);
        DeprecatedApiProjectScanResult deprecatedApiResult = scanForDeprecatedApi(projectPath);
        SecurityApiProjectScanResult securityApiResult = scanForSecurityApi(projectPath);
        JmsMessagingProjectScanResult jmsMessagingResult = scanForJmsMessaging(projectPath);
        TransitiveDependencyProjectScanResult transitiveDependencyResult = scanForTransitiveDependencies(projectPath);
        ConfigFileProjectScanResult configFileResult = scanForConfigFiles(projectPath);
        ClassloaderModuleProjectScanResult classloaderModuleResult = scanForClassloaderModule(projectPath);
        LoggingMetricsProjectScanResult loggingMetricsResult = scanForLoggingMetrics(projectPath);
        SerializationCacheProjectScanResult serializationCacheResult = scanForSerializationCache(projectPath);
        ThirdPartyLibProjectScanResult thirdPartyLibResult = scanForThirdPartyLib(projectPath);
        
        cachedSummary = new AdvancedScanSummary(
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
        cachedProjectPath = projectPath;
        lastScanTime = System.currentTimeMillis();
        
        return cachedSummary;
    }

    /**
     * Gets the cached scan summary if available.
     * 
     * @return Cached AdvancedScanSummary or null if no scan has been run
     */
    public AdvancedScanSummary getCachedSummary() {
        return cachedSummary;
    }

    /**
     * Returns whether a scan has been performed and cached.
     * 
     * @return true if cached results exist
     */
    public boolean hasCachedResults() {
        return cachedSummary != null;
    }

    // Individual scan methods for each scanner type
    public CdiInjectionProjectScanResult scanForCdiInjection(Path projectPath) {
        LOG.info("Scanning for CDI Injection in: " + projectPath);
        return cdiInjectionScanner.scanProject(projectPath);
    }

    public BuildConfigProjectScanResult scanForBuildConfig(Path projectPath) {
        LOG.info("Scanning for Build Config in: " + projectPath);
        return buildConfigScanner.scanProject(projectPath);
    }

    public RestSoapProjectScanResult scanForRestSoap(Path projectPath) {
        LOG.info("Scanning for REST/SOAP in: " + projectPath);
        return restSoapScanner.scanProject(projectPath);
    }

    public DeprecatedApiProjectScanResult scanForDeprecatedApi(Path projectPath) {
        LOG.info("Scanning for Deprecated API in: " + projectPath);
        return deprecatedApiScanner.scanProject(projectPath);
    }

    public SecurityApiProjectScanResult scanForSecurityApi(Path projectPath) {
        LOG.info("Scanning for Security API in: " + projectPath);
        return securityApiScanner.scanProject(projectPath);
    }

    public JmsMessagingProjectScanResult scanForJmsMessaging(Path projectPath) {
        LOG.info("Scanning for JMS Messaging in: " + projectPath);
        return jmsMessagingScanner.scanProject(projectPath);
    }

    public TransitiveDependencyProjectScanResult scanForTransitiveDependencies(Path projectPath) {
        LOG.info("Scanning for Transitive Dependencies in: " + projectPath);
        return transitiveDependencyScanner.scanProject(projectPath);
    }

    public ConfigFileProjectScanResult scanForConfigFiles(Path projectPath) {
        LOG.info("Scanning for Config Files in: " + projectPath);
        return configFileScanner.scanProject(projectPath);
    }

    public ClassloaderModuleProjectScanResult scanForClassloaderModule(Path projectPath) {
        LOG.info("Scanning for Classloader/Module in: " + projectPath);
        return classloaderModuleScanner.scanProject(projectPath);
    }

    public LoggingMetricsProjectScanResult scanForLoggingMetrics(Path projectPath) {
        LOG.info("Scanning for Logging/Metrics in: " + projectPath);
        return loggingMetricsScanner.scanProject(projectPath);
    }

    public SerializationCacheProjectScanResult scanForSerializationCache(Path projectPath) {
        LOG.info("Scanning for Serialization/Cache in: " + projectPath);
        return serializationCacheScanner.scanProject(projectPath);
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
        ThirdPartyLibProjectScanResult thirdPartyLibResult
    ) {
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
