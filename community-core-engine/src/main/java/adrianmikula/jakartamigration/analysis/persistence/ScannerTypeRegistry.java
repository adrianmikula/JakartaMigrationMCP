package adrianmikula.jakartamigration.analysis.persistence;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for scanner type metadata, providing centralized lookup of scanner information.
 * Replaces multiple switch-based mapping methods with a data-driven approach.
 */
@Slf4j
public class ScannerTypeRegistry {
    
    private final Map<String, ScannerTypeMetadata> registry = new HashMap<>();
    
    public ScannerTypeRegistry() {
        initializeRegistry();
    }
    
    /**
     * Initializes the registry with all known scanner types and their metadata.
     */
    private void initializeRegistry() {
        // JPA Scanner
        addScanner("JPA_ANNOTATION_SCANNER", 
            "Advanced Scans",
            "javax.persistence",
            "jakarta.persistence",
            "JavaxPersistenceToJakartaPersistence",
            "Detects JPA annotations that need migration",
            "ClassNotFoundException: javax.persistence.Entity",
            "Use JPA migration recipe to update annotations",
            true);
        
        // Bean Validation Scanner
        addScanner("BEAN_VALIDATION_SCANNER",
            "Advanced Scans",
            "javax.validation",
            "jakarta.validation",
            "JavaxValidationToJakartaValidation",
            "Detects Bean Validation annotations that need migration",
            "ClassNotFoundException: javax.validation.Constraint",
            "Use Bean Validation migration recipe",
            true);
        
        // Servlet/JSP Scanner
        addScanner("SERVLET_JSP_SCANNER",
            "Advanced Scans",
            "javax.servlet",
            "jakarta.servlet",
            "JavaxServletToJakartaServlet",
            "Detects Servlet/JSP APIs that need migration",
            "ClassNotFoundException: javax.servlet.HttpServlet",
            "Use Servlet migration recipe",
            true);
        
        // CDI Injection Scanner
        addScanner("CDI_INJECTION_SCANNER",
            "Advanced Scans",
            "javax.enterprise",
            "jakarta.enterprise",
            "JavaxEnterpriseToJakartaEnterprise",
            "Detects CDI annotations that need migration",
            "ClassNotFoundException: javax.enterprise.inject.Inject",
            "Use CDI migration recipe",
            true);
        
        // Build Config Scanner
        addScanner("BUILD_CONFIG_SCANNER",
            "Advanced Scans",
            "javax.tools",
            "jakarta.tools",
            "JavaxToolsToJakartaTools",
            "Detects build configuration issues",
            "Build configuration issues",
            "Update build configuration for Jakarta EE",
            true);
        
        // REST/SOAP Scanner
        addScanner("REST_SOAP_SCANNER",
            "Advanced Scans",
            "javax.ws.rs",
            "jakarta.ws.rs",
            "JavaxWsRsToJakartaWsRs",
            "Detects REST/SOAP APIs that need migration",
            "ClassNotFoundException: javax.ws.rs.Path",
            "Use JAX-RS migration recipe",
            true);
        
        // Deprecated API Scanner
        addScanner("DEPRECATED_API_SCANNER",
            "Advanced Scans",
            "javax",
            "jakarta",
            "JavaxToJakarta",
            "Detects deprecated javax APIs",
            "Deprecated API usage warnings",
            "Replace deprecated APIs with Jakarta equivalents",
            true);
        
        // Security API Scanner
        addScanner("SECURITY_API_SCANNER",
            "Advanced Scans",
            "javax.security",
            "jakarta.security",
            "JavaxSecurityToJakartaSecurity",
            "Detects security APIs that need migration",
            "ClassNotFoundException: javax.security.auth",
            "Use security API migration recipe",
            true);
        
        // JMS Messaging Scanner
        addScanner("JMS_MESSAGING_SCANNER",
            "Advanced Scans",
            "javax.jms",
            "jakarta.jms",
            "JavaxJmsToJakartaJms",
            "Detects JMS APIs that need migration",
            "ClassNotFoundException: javax.jms.Message",
            "Use JMS migration recipe",
            true);
        
        // Transitive Dependency Scanner
        addScanner("TRANSITIVE_DEPENDENCY_SCANNER",
            "Advanced Scans",
            "javax",
            "jakarta",
            "JavaxToJakarta",
            "Detects transitive dependencies that need migration",
            "Dependency resolution issues",
            "Update dependencies to Jakarta versions",
            true);
        
        // Config File Scanner
        addScanner("CONFIG_FILE_SCANNER",
            "Advanced Scans",
            "javax",
            "jakarta",
            "JavaxToJakarta",
            "Detects configuration files that need migration",
            "Configuration file parsing errors",
            "Update configuration files for Jakarta EE",
            true);
        
        // Classloader Module Scanner
        addScanner("CLASSLOADER_MODULE_SCANNER",
            "Advanced Scans",
            "javax",
            "jakarta",
            "JavaxToJakarta",
            "Detects classloader/module issues",
            "Class loading issues",
            "Update classloader/module configuration",
            true);
        
        // Logging Metrics Scanner
        addScanner("LOGGING_METRICS_SCANNER",
            "Advanced Scans",
            "javax",
            "jakarta",
            "JavaxToJakarta",
            "Detects logging/metrics APIs that need migration",
            "Logging framework issues",
            "Update logging/metrics configuration",
            true);
        
        // Serialization Cache Scanner
        addScanner("SERIALIZATION_CACHE_SCANNER",
            "Advanced Scans",
            "javax",
            "jakarta",
            "JavaxToJakarta",
            "Detects serialization/cache APIs that need migration",
            "Serialization compatibility issues",
            "Update serialization/cache configuration",
            true);
        
        // Reflection Usage Scanner
        addScanner("REFLECTION_USAGE_SCANNER",
            "Advanced Scans",
            "javax",
            "jakarta",
            "JavaxToJakarta",
            "Detects reflection usage that may be affected",
            "Reflection access issues",
            "Update reflection calls for Jakarta compatibility",
            true);
        
        // Third Party Lib Scanner
        addScanner("THIRD_PARTY_LIB_SCANNER",
            "Advanced Scans",
            "javax",
            "jakarta",
            "JavaxToJakarta",
            "Detects third-party libraries that need Jakarta compatibility",
            "Third-party library compatibility issues",
            "Update third-party libraries to Jakarta-compatible versions",
            true);
        
        log.info("Initialized ScannerTypeRegistry with {} scanner types", registry.size());
    }
    
    /**
     * Adds a scanner type to the registry.
     */
    private void addScanner(String scannerType, String uiTab, String legacyNamespace, 
                           String targetNamespace, String refactorRecipe, String description,
                           String anticipatedErrorMessages, String solutionHint, boolean isPremium) {
        ScannerTypeMetadata metadata = new ScannerTypeMetadata(
            scannerType, uiTab, legacyNamespace, targetNamespace, refactorRecipe,
            description, anticipatedErrorMessages, solutionHint, isPremium
        );
        registry.put(scannerType, metadata);
    }
    
    /**
     * Gets metadata for a scanner type.
     */
    public ScannerTypeMetadata getMetadata(String scannerType) {
        return registry.get(scannerType);
    }
    
    /**
     * Gets UI tab for a scanner type.
     */
    public String getUITab(String scannerType) {
        ScannerTypeMetadata metadata = registry.get(scannerType);
        return metadata != null ? metadata.uiTab() : "Unknown";
    }
    
    /**
     * Gets legacy namespace for a scanner type.
     */
    public String getLegacyNamespace(String scannerType) {
        ScannerTypeMetadata metadata = registry.get(scannerType);
        return metadata != null ? metadata.legacyNamespace() : "javax";
    }
    
    /**
     * Gets target namespace for a scanner type.
     */
    public String getTargetNamespace(String scannerType) {
        ScannerTypeMetadata metadata = registry.get(scannerType);
        return metadata != null ? metadata.targetNamespace() : "jakarta";
    }
    
    /**
     * Gets refactor recipe for a scanner type.
     */
    public String getRefactorRecipe(String scannerType) {
        ScannerTypeMetadata metadata = registry.get(scannerType);
        return metadata != null ? metadata.refactorRecipe() : "JavaxToJakarta";
    }
    
    /**
     * Gets description for a scanner type.
     */
    public String getDescription(String scannerType) {
        ScannerTypeMetadata metadata = registry.get(scannerType);
        return metadata != null ? metadata.description() : "Unknown scanner type";
    }
    
    /**
     * Gets anticipated error messages for a scanner type.
     */
    public String getAnticipatedErrorMessages(String scannerType) {
        ScannerTypeMetadata metadata = registry.get(scannerType);
        return metadata != null ? metadata.anticipatedErrorMessages() : "Unknown error patterns";
    }
    
    /**
     * Gets solution hint for a scanner type.
     */
    public String getSolutionHint(String scannerType) {
        ScannerTypeMetadata metadata = registry.get(scannerType);
        return metadata != null ? metadata.solutionHint() : "Contact support for migration assistance";
    }
    
    /**
     * Checks if a scanner type is a premium feature.
     */
    public boolean isPremium(String scannerType) {
        ScannerTypeMetadata metadata = registry.get(scannerType);
        return metadata != null && metadata.isPremium();
    }
    
    /**
     * Gets all registered scanner types.
     */
    public Map<String, ScannerTypeMetadata> getAllScanners() {
        return new HashMap<>(registry);
    }
}
