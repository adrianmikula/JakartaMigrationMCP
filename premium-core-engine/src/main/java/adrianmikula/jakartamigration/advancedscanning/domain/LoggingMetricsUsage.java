package adrianmikula.jakartamigration.advancedscanning.domain;

/**
 * Represents a usage of javax.logging or JMX-related APIs in the codebase.
 * This is used by the LoggingMetricsScanner to track logging API migration issues.
 */
public class LoggingMetricsUsage {
    private final String filePath;
    private final int lineNumber;
    private final String usageType;
    private final String className;
    private final String methodName;

    public LoggingMetricsUsage(String filePath, int lineNumber, String usageType, 
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
     * Returns the suggested Jakarta EE replacement for this logging/metrics usage.
     */
    public String getReplacement() {
        return switch (usageType) {
            case "javax.logging.Logger" -> "org.jboss.logging.Logger or java.util.logging.Logger";
            case "javax.management.MBeanServer" -> "jakarta.management.MBeanServer";
            case "javax.management.MBean" -> "jakarta.management.MBean";
            case "javax.management.JMX" -> "jakarta.management.JMX";
            default -> "Review Jakarta EE 9+ migration guide";
        };
    }
}
