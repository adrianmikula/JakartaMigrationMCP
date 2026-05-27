package adrianmikula.jakartamigration.intellij.ui;

/**
 * Enum representing different scanner types with their metadata.
 * Used to eliminate duplication in panel creation logic.
 */
public enum ScannerType {
    JPA("JPA", "File", "Line", "Annotation", "Jakarta Equivalent", "Path"),
    BEAN_VALIDATION("Bean Validation", "File", "Line", "Constraint", "Jakarta Equivalent", "Path"),
    SERVLET_JSP("Servlet/JSP", "File", "Line", "Class/Usage", "Type", "Jakarta Equivalent", "Path"),
    BUILD_CONFIG("Build Config", "File", "Line", "Issue Type", "Description", "Path"),
    CONFIG_FILE("Config File", "File", "Issue", "Severity", "Description", "Path"),
    DEPRECATED_API("Deprecated API", "File", "Line", "Deprecated API", "Replacement", "Path"),
    CDI_INJECTION("CDI Injection", "File", "Line", "Injection Point", "Type", "Jakarta Equivalent", "Path"),
    REST_SOAP("REST/SOAP", "File", "Line", "Annotation/Class", "Type", "Jakarta Equivalent", "Path"),
    SECURITY_API("Security API", "File", "Line", "Security API", "Type", "Jakarta Equivalent", "Path"),
    JMS_MESSAGING("JMS Messaging", "File", "Line", "JMS API", "Type", "Jakarta Equivalent", "Path"),
    TRANSITIVE_DEPENDENCY("Transitive Dependency", "Dependency", "Version", "Scope", "Status", "Path"),
    CLASSLOADER_MODULE("Classloader/Module", "File", "Line", "Issue Type", "Description", "Path"),
    LOGGING_METRICS("Logging/Metrics", "File", "Line", "API", "Type", "Jakarta Equivalent", "Path"),
    SERIALIZATION_CACHE("Serialization/Cache", "File", "Line", "API", "Type", "Jakarta Equivalent", "Path"),
    THIRD_PARTY_LIB("Third-Party Lib", "Dependency", "Version", "Scope", "Jakarta Compatible", "Path");

    private final String displayName;
    private final String[] columns;

    ScannerType(String displayName, String... columns) {
        this.displayName = displayName;
        this.columns = columns;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String[] getColumns() {
        return columns;
    }
}
