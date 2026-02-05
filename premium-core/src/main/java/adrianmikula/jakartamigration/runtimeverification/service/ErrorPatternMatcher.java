package adrianmikula.jakartamigration.runtimeverification.service;

import adrianmikula.jakartamigration.runtimeverification.domain.ErrorCategory;
import adrianmikula.jakartamigration.runtimeverification.domain.ErrorType;

import java.util.regex.Pattern;

/**
 * Symbolic layer for matching error patterns and categorizing them.
 */
public class ErrorPatternMatcher {
    
    // Patterns for namespace migration errors
    private static final Pattern JAVAX_CLASS_NOT_FOUND = Pattern.compile(
        ".*ClassNotFoundException.*javax\\.(servlet|persistence|ejb|validation|ws|xml|jms|mail|security|transaction|annotation|inject|decorator|interceptor|batch|connector|json|jsonb|jta|jpa|faces|cdi).*",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern JAVAX_NO_CLASS_DEF = Pattern.compile(
        ".*NoClassDefFoundError.*javax\\.(servlet|persistence|ejb|validation|ws|xml|jms|mail|security|transaction|annotation|inject|decorator|interceptor|batch|connector|json|jsonb|jta|jpa|faces|cdi).*",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern JAKARTA_CLASS_NOT_FOUND = Pattern.compile(
        ".*ClassNotFoundException.*jakarta\\.(servlet|persistence|ejb|validation|ws|xml|jms|mail|security|transaction|annotation|inject|decorator|interceptor|batch|connector|json|jsonb|jta|jpa|faces|cdi).*",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern LINKAGE_ERROR_PATTERN = Pattern.compile(
        ".*LinkageError.*(javax|jakarta).*",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern MIXED_NAMESPACE_PATTERN = Pattern.compile(
        ".*(javax|jakarta).*\\s+(javax|jakarta).*",
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * Determines the error type from an error message.
     */
    public static ErrorType determineErrorType(String errorMessage) {
        if (errorMessage == null) {
            return ErrorType.OTHER;
        }
        
        String lowerMessage = errorMessage.toLowerCase();
        
        if (lowerMessage.contains("classnotfoundexception")) {
            return ErrorType.CLASS_NOT_FOUND;
        } else if (lowerMessage.contains("noclassdeffounderror")) {
            return ErrorType.NO_CLASS_DEF_FOUND;
        } else if (lowerMessage.contains("linkageerror")) {
            return ErrorType.LINKAGE_ERROR;
        } else if (lowerMessage.contains("nosuchmethoderror")) {
            return ErrorType.NO_SUCH_METHOD;
        } else if (lowerMessage.contains("nosuchfielderror")) {
            return ErrorType.NO_SUCH_FIELD;
        } else if (lowerMessage.contains("illegalaccesserror")) {
            return ErrorType.ILLEGAL_ACCESS;
        } else if (lowerMessage.contains("classcastexception")) {
            return ErrorType.CLASS_CAST;
        }
        
        return ErrorType.OTHER;
    }
    
    /**
     * Determines the error category from an error message.
     */
    public static ErrorCategory determineErrorCategory(String errorMessage, String className) {
        if (errorMessage == null && className == null) {
            return ErrorCategory.UNKNOWN;
        }
        
        String combined = (errorMessage != null ? errorMessage : "") + " " + (className != null ? className : "");
        String lowerCombined = combined.toLowerCase();
        String lowerMessage = errorMessage != null ? errorMessage.toLowerCase() : "";
        String lowerClassName = className != null ? className.toLowerCase() : "";
        
        // Check if className or message contains javax/jakarta patterns
        boolean hasJavaxInClassName = lowerClassName.startsWith("javax.");
        boolean hasJakartaInClassName = lowerClassName.startsWith("jakarta.");
        boolean hasJavaxInMessage = lowerMessage.contains("javax.");
        boolean hasJakartaInMessage = lowerMessage.contains("jakarta.");
        
        // First, check for namespace migration issues (javax classes not found)
        // This should be checked before binary incompatibility
        if (JAVAX_CLASS_NOT_FOUND.matcher(combined).matches() ||
            JAVAX_NO_CLASS_DEF.matcher(combined).matches() ||
            (hasJavaxInClassName || hasJavaxInMessage)) {
            return ErrorCategory.NAMESPACE_MIGRATION;
        }
        
        // Check for classpath issues (Jakarta classes not found)
        // This should be checked before binary incompatibility
        if (JAKARTA_CLASS_NOT_FOUND.matcher(combined).matches() ||
            (hasJakartaInClassName || hasJakartaInMessage)) {
            return ErrorCategory.CLASSPATH_ISSUE;
        }
        
        // Check for binary incompatibility (LinkageError or explicit mixed namespace)
        // Only check this if we haven't matched the above patterns
        if (LINKAGE_ERROR_PATTERN.matcher(combined).matches()) {
            return ErrorCategory.BINARY_INCOMPATIBILITY;
        }
        
        // Check for mixed namespaces - but only if both javax and jakarta appear
        // and it's not already classified above
        if ((hasJavaxInClassName || hasJavaxInMessage) && 
            (hasJakartaInClassName || hasJakartaInMessage) && 
            MIXED_NAMESPACE_PATTERN.matcher(combined).matches()) {
            return ErrorCategory.BINARY_INCOMPATIBILITY;
        }
        
        // Check for configuration errors (XML, properties, etc.)
        if (lowerCombined.contains("xml") ||
            lowerCombined.contains("configuration") ||
            lowerCombined.contains("properties")) {
            return ErrorCategory.CONFIGURATION_ERROR;
        }
        
        return ErrorCategory.UNKNOWN;
    }
    
    /**
     * Checks if an error is related to Jakarta migration.
     */
    public static boolean isJakartaMigrationRelated(String errorMessage, String className) {
        ErrorCategory category = determineErrorCategory(errorMessage, className);
        return category == ErrorCategory.NAMESPACE_MIGRATION ||
               category == ErrorCategory.CLASSPATH_ISSUE ||
               category == ErrorCategory.BINARY_INCOMPATIBILITY;
    }
}

