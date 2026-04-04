package adrianmikula.jakartamigration.advancedscanning.domain;

/**
 * Represents a usage of reflection APIs that may reference javax.* classes.
 * This is used by ReflectionUsageScanner to track reflection compatibility issues.
 */
public class ReflectionUsage {
    private final String filePath;
    private final int lineNumber;
    private final String usageType;
    private final String className;
    private final String methodName;
    private final String reflectionTarget;

    public ReflectionUsage(String filePath, int lineNumber, String usageType, 
                           String className, String methodName, String reflectionTarget) {
        this.filePath = filePath;
        this.lineNumber = lineNumber;
        this.usageType = usageType;
        this.className = className;
        this.methodName = methodName;
        this.reflectionTarget = reflectionTarget;
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

    public String getReflectionTarget() {
        return reflectionTarget;
    }

    /**
     * Returns risk assessment for this reflection usage.
     */
    public String getRiskAssessment() {
        return switch (usageType) {
            case "Class.forName" -> "HIGH: Dynamic class loading may fail with Jakarta packages";
            case "ClassLoader" -> "HIGH: Class loading may break with Jakarta migration";
            case "Method.invoke" -> "MEDIUM: Method invocation may fail on Jakarta classes";
            case "Field.set" -> "MEDIUM: Field access may fail on Jakarta classes";
            case "Annotation" -> "MEDIUM: Reflection on annotations may need Jakarta updates";
            case "Proxy" -> "HIGH: Dynamic proxies may break with Jakarta classes";
            default -> "Review for Jakarta compatibility";
        };
    }
}
