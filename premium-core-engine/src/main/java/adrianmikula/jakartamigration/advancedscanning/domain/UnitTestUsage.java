package adrianmikula.jakartamigration.advancedscanning.domain;

/**
 * Represents a usage of javax.* APIs in test files.
 * This is used by the UnitTestScanner to track test migration issues.
 */
public class UnitTestUsage {
    private final String filePath;
    private final int lineNumber;
    private final String usageType;
    private final String testFramework;

    public UnitTestUsage(String filePath, int lineNumber, String usageType, String testFramework) {
        this.filePath = filePath;
        this.lineNumber = lineNumber;
        this.usageType = usageType;
        this.testFramework = testFramework;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getUsageType() {
        return usageType;
    }

    public String getTestFramework() {
        return testFramework;
    }

    public String getReplacement() {
        return switch (usageType) {
            case "javax.servlet" -> "jakarta.servlet";
            case "javax.ejb" -> "jakarta.ejb or CDI";
            case "javax.persistence" -> "jakarta.persistence";
            case "javax.inject" -> "jakarta.inject";
            case "javax.validation" -> "jakarta.validation";
            default -> "jakarta." + usageType.substring("javax.".length());
        };
    }
}
