package adrianmikula.jakartamigration.advancedscanning.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Represents a usage of javax.* APIs in test files.
 * This is used by the UnitTestScanner to track test migration issues.
 */
@Getter
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class UnitTestUsage {
    private final String filePath;
    private final int lineNumber;
    private final String usageType;
    private final String testFramework;

    public String getReplacement() {
        return switch (usageType) {
            case "javax.servlet" -> "jakarta.servlet";
            case "javax.ejb" -> "jakarta.ejb or CDI";
            case "javax.persistence" -> "jakarta.persistence";
            case "javax.inject" -> "jakarta.inject";
            case "javax.validation" -> "jakarta.validation";
            default -> usageType.startsWith("javax.") ? "jakarta." + usageType.substring("javax.".length()) : usageType;
        };
    }
}
