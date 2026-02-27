package adrianmikula.jakartamigration.advancedscanning.domain;

/**
 * Represents a usage of serialization or cache-related APIs that may reference javax.* classes.
 * This is used by the SerializationCacheScanner to track serialization compatibility issues.
 */
public class SerializationCacheUsage {
    private final String filePath;
    private final int lineNumber;
    private final String usageType;
    private final String className;
    private final String methodName;

    public SerializationCacheUsage(String filePath, int lineNumber, String usageType, 
                                   String className, String methodName) {
        this.filePath = filePath;
        this.lineNumber = lineNumber;
        this.usageType = usageType;
        this.className = className;
        this.methodName = methodName;
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

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    /**
     * Returns the risk assessment for this serialization usage.
     */
    public String getRiskAssessment() {
        return switch (usageType) {
            case "ObjectInputStream" -> "HIGH: Can deserialize javax.* objects - verify data compatibility";
            case "ObjectOutputStream" -> "MEDIUM: Serializes objects - may include javax.* classes";
            case "Serializable" -> "MEDIUM: Check for javax.* field types";
            case "SessionSerialization" -> "HIGH: HttpSession may contain javax.* objects";
            case "CacheEntry" -> "MEDIUM: Cached objects may need migration";
            default -> "Review for javax.* compatibility";
        };
    }
}
