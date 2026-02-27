package adrianmikula.jakartamigration.advancedscanning.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Represents a usage of a Jakarta EE/JEE integration point (RMI, JNDI, etc.)
 * that may need migration from javax.* to jakarta.*.
 */
@Getter
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class IntegrationPointUsage {
    private final String filePath;
    private final int lineNumber;
    private final String integrationType; // e.g., "RMI", "JNDI", "JMS"
    private final String className;

    public String getReplacementSuggestion() {
        return switch (integrationType) {
            case "RMI" -> "Review RMI usage for Jakarta EE compatibility";
            case "JNDI" -> "Migrate javax.naming to jakarta.naming";
            case "JMS" -> "Migrate javax.jms to jakarta.jms";
            case "JWS" -> "Migrate javax.jws to jakarta.jws";
            case "JAX-WS" -> "Migrate javax.xml.ws to jakarta.xml.ws";
            case "SOAP" -> "Migrate javax.xml.soap to jakarta.xml.soap";
            default -> "Review Jakarta EE 9+ migration guide";
        };
    }
}
