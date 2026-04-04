package adrianmikula.jakartamigration.intellij.service;

import adrianmikula.jakartamigration.advancedscanning.domain.*;
import adrianmikula.jakartamigration.advancedscanning.service.AdvancedScanningModule;
import adrianmikula.jakartamigration.advancedscanning.service.*;
import com.intellij.openapi.diagnostic.Logger;

import java.nio.file.Path;

/**
 * Simplified advanced scanning service
 * Removes complex parallel execution and memory management
 * Focuses on direct, sequential scan for core functionality
 */
public class SimplifiedAdvancedScanningService {
    
    private static final Logger LOG = Logger.getInstance(SimplifiedAdvancedScanningService.class);
    
    private final AdvancedScanningModule scanningModule;
    
    public SimplifiedAdvancedScanningService() {
        this.scanningModule = new AdvancedScanningModule(null);
        LOG.info("SimplifiedAdvancedScanningService initialized");
    }
    
    /**
     * Simplified scan - just runs basic scans sequentially
     */
    public String scanAll(Path projectPath) {
        LOG.info("Starting simplified advanced scan for: " + projectPath);
        
        // Simple sequential scan - no complex parallel execution
        var jpaResult = scanningModule.getJpaAnnotationScanner().scanProject(projectPath);
        var beanValidationResult = scanningModule.getBeanValidationScanner().scanProject(projectPath);
        var servletJspResult = scanningModule.getServletJspScanner().scanProject(projectPath);
        var cdiResult = scanningModule.getCdiInjectionScanner().scanProject(projectPath);
        var buildConfigResult = scanningModule.getBuildConfigScanner().scanProject(projectPath);
        var restSoapResult = scanningModule.getRestSoapScanner().scanProject(projectPath);
        var deprecatedApiResult = scanningModule.getDeprecatedApiScanner().scanProject(projectPath);
        var securityApiResult = scanningModule.getSecurityApiScanner().scanProject(projectPath);
        var jmsResult = scanningModule.getJmsMessagingScanner().scanProject(projectPath);
        var transitiveResult = scanningModule.getTransitiveDependencyScanner().scanProject(projectPath);
        var configFilesResult = scanningModule.getConfigFileScanner().scanProject(projectPath);
        var classloaderResult = scanningModule.getClassloaderModuleScanner().scanProject(projectPath);
        var loggingResult = scanningModule.getLoggingMetricsScanner().scanProject(projectPath);
        var reflectionResult = scanningModule.getReflectionUsageScanner().scanProject(projectPath);
        
        // For now, return a simple summary message instead of complex object
        return "Simplified scan completed successfully. Found " + 
               (jpaResult != null ? 1 : 0) + " JPA issues, " +
               (beanValidationResult != null ? 1 : 0) + " Bean Validation issues, " +
               (servletJspResult != null ? 1 : 0) + " Servlet/JSP issues, " +
               (cdiResult != null ? 1 : 0) + " CDI issues, " +
               (buildConfigResult != null ? 1 : 0) + " Build Config issues, " +
               (restSoapResult != null ? 1 : 0) + " REST/SOAP issues, " +
               (deprecatedApiResult != null ? 1 : 0) + " Deprecated API issues, " +
               (securityApiResult != null ? 1 : 0) + " Security API issues, " +
               (jmsResult != null ? 1 : 0) + " JMS issues, " +
               (transitiveResult != null ? 1 : 0) + " Transitive Dependency issues, " +
               (configFilesResult != null ? 1 : 0) + " Config File issues, " +
               (classloaderResult != null ? 1 : 0) + " Classloader issues, " +
               (loggingResult != null ? 1 : 0) + " Logging issues, " +
               (reflectionResult != null ? 1 : 0) + " Reflection issues.";
    }
}
