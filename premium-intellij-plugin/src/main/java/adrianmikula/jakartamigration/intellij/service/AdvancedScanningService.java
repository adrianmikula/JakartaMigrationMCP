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
     *
     * @param projectPath Path to the project root directory
     * @return AdvancedScanSummary containing combined results
     */
    public AdvancedScanSummary scanAll(Path projectPath) {
        LOG.info("Running all advanced scans in: " + projectPath);
        
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
        
        return new AdvancedScanSummary(
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
            classloaderModuleResult
        );
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
        ClassloaderModuleProjectScanResult classloaderModuleResult
    ) {
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
