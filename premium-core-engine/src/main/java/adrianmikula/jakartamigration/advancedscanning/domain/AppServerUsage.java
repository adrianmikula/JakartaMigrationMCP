package adrianmikula.jakartamigration.advancedscanning.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Represents an application server configuration or dependency that may need
 * migration.
 */
@Getter
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class AppServerUsage {
    private final String filePath;
    private final String serverType;
    private final String configurationType;
    private final String currentValue;

    public String getFilePath() {
        return filePath;
    }

    public String getServerType() {
        return serverType;
    }

    public String getConfigurationType() {
        return configurationType;
    }

    public String getCurrentValue() {
        return currentValue;
    }

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
