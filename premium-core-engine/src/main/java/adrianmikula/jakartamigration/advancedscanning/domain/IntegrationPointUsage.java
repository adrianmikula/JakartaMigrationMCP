package adrianmikula.jakartamigration.advancedscanning.domain;

/**
 * Represents an integration point (RMI, CORBA, etc.) that may need migration.
 */
public class IntegrationPointUsage {
    private final String filePath;
    private final int lineNumber;
    private final String integrationType;
    private final String className;

    public IntegrationPointUsage(String filePath, int lineNumber, String integrationType, String className) {
        this.filePath = filePath;
        this.lineNumber = lineNumber;
        this.integrationType = integrationType;
        this.className = className;
    }

    public String getFilePath() { return filePath; }
    public int getLineNumber() { return lineNumber; }
    public String getIntegrationType() { return integrationType; }
    public String getClassName() { return className; }

    public String getMigrationGuidance() {
        return switch (integrationType) {
            case "javax.rmi" -> "Use Jakarta RMI or replace with REST/WebSocket";
            case "javax.jws" -> "Use Jakarta XML Web Services (JAX-WS)";
            case "javax.xml.ws" -> "Migrate to jakarta.xml.ws";
            case "javax.jms" -> "Migrate to jakarta.jms";
            default -> "Review for Jakarta EE compatible alternative";
        };
    }
}
