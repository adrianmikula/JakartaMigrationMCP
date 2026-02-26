package adrianmikula.jakartamigration.advancedscanning.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Represents a usage of javax.logging or JMX-related APIs in the codebase.
 * This is used by the LoggingMetricsScanner to track logging API migration
 * issues.
 */
@Getter
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoggingMetricsUsage {
    private final String filePath;
    private final int lineNumber;
    private final String usageType;
    private final String className;
    private final String methodName;

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
    @JsonIgnore
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
