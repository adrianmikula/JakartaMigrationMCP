package adrianmikula.jakartamigration.advancedscanning.domain;

/**
 * Represents an application server configuration or dependency that may need migration.
 */
public class AppServerUsage {
    private final String filePath;
    private final String serverType;
    private final String configurationType;
    private final String currentValue;

    public AppServerUsage(String filePath, String serverType, String configurationType, String currentValue) {
        this.filePath = filePath;
        this.serverType = serverType;
        this.configurationType = configurationType;
        this.currentValue = currentValue;
    }

    public String getFilePath() { return filePath; }
    public String getServerType() { return serverType; }
    public String getConfigurationType() { return configurationType; }
    public String getCurrentValue() { return currentValue; }

    public String getMigrationGuidance() {
        return switch (serverType.toLowerCase()) {
            case "weblogic" -> "Upgrade to WebLogic 14c+ with Jakarta EE support";
            case "websphere" -> "Upgrade to WebSphere Liberty or traditional with Jakarta";
            case "jboss" -> "Use WildFly or JBoss EAP with Jakarta EE support";
            case "glassfish" -> "Use GlassFish 7+ or Payara with Jakarta";
            case "tomcat" -> "Use Tomcat 10+ with jakarta.servlet";
            default -> "Verify Jakarta EE compatibility";
        };
    }
}
