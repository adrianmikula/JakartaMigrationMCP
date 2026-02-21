package adrianmikula.jakartamigration.advancedscanning.domain;

/**
 * Represents a test container or embedded server that needs migration.
 */
public class TestContainerUsage {
    private final String filePath;
    private final String containerType;
    private final String currentVersion;
    private final String issueType;

    public TestContainerUsage(String filePath, String containerType, String currentVersion, String issueType) {
        this.filePath = filePath;
        this.containerType = containerType;
        this.currentVersion = currentVersion;
        this.issueType = issueType;
    }

    public String getFilePath() { return filePath; }
    public String getContainerType() { return containerType; }
    public String getCurrentVersion() { return currentVersion; }
    public String getIssueType() { return issueType; }

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
