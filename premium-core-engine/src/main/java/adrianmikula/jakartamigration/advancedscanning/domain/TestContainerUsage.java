package adrianmikula.jakartamigration.advancedscanning.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Represents a test container or embedded server that needs migration.
 */
@Getter
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class TestContainerUsage {
    private final String filePath;
    private final String containerType;
    private final String currentVersion;
    private final String issueType;

    public String getSuggestedReplacement() {
        return switch (containerType.toLowerCase()) {
            case "jetty" -> "org.eclipse.jetty:jetty-jakarta-servlet-api:11.x";
            case "tomcat" -> "org.apache.tomcat.embed:tomcat-embed-jasper with Jakarta";
            case "wildfly" -> "org.wildfly:wildfly-ee with Jakarta";
            case "glassfish" -> "org.glassfish:jakarta.servlet";
            default -> "Upgrade to Jakarta EE compatible version";
        };
    }
}
