package com.bugbounty.jakartamigration.runtimeverification.service;

import com.bugbounty.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import com.bugbounty.jakartamigration.runtimeverification.domain.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Analyzes runtime errors and provides root cause analysis and remediation suggestions.
 */
public class ErrorAnalyzer {
    
    private static final Pattern STACK_TRACE_PATTERN = Pattern.compile(
        "at\\s+([\\w\\.]+)\\.([\\w]+)\\(([\\w\\.]+\\.java):(\\d+)\\)"
    );
    
    /**
     * Analyzes a list of runtime errors and provides comprehensive analysis.
     */
    public ErrorAnalysis analyzeErrors(List<RuntimeError> errors, MigrationContext context) {
        if (errors == null || errors.isEmpty()) {
            return createEmptyAnalysis();
        }
        
        // Analyze the first error (most significant)
        RuntimeError primaryError = errors.get(0);
        
        ErrorCategory category = ErrorPatternMatcher.determineErrorCategory(
            primaryError.message(),
            primaryError.className()
        );
        
        String rootCause = determineRootCause(primaryError, category, context);
        List<String> contributingFactors = identifyContributingFactors(errors, context);
        List<SimilarPastFailure> similarFailures = findSimilarFailures(primaryError);
        List<RemediationStep> suggestedFixes = generateRemediationSteps(primaryError, category, context);
        
        double confidence = calculateConfidence(primaryError, category, context);
        
        return new ErrorAnalysis(
            category,
            rootCause,
            contributingFactors,
            similarFailures,
            suggestedFixes,
            confidence
        );
    }
    
    /**
     * Parses runtime errors from process output.
     */
    public List<RuntimeError> parseErrorsFromOutput(List<String> stderr, List<String> stdout) {
        List<RuntimeError> errors = new ArrayList<>();
        
        // Combine stderr and stdout for analysis
        List<String> allOutput = new ArrayList<>(stderr);
        allOutput.addAll(stdout);
        
        String currentException = null;
        String currentMessage = null;
        List<StackTrace.StackTraceElement> stackElements = new ArrayList<>();
        
        for (String line : allOutput) {
            // Check for exception class
            if (line.contains("Exception") || line.contains("Error")) {
                // Save previous error if exists
                if (currentException != null) {
                    errors.add(createRuntimeError(currentException, currentMessage, stackElements));
                    stackElements = new ArrayList<>();
                }
                
                // Parse new exception
                String[] parts = line.split(":", 2);
                if (parts.length > 0) {
                    currentException = parts[0].trim();
                    currentMessage = parts.length > 1 ? parts[1].trim() : "";
                }
            }
            
            // Parse stack trace elements
            Matcher matcher = STACK_TRACE_PATTERN.matcher(line);
            if (matcher.find()) {
                String className = matcher.group(1);
                String methodName = matcher.group(2);
                String fileName = matcher.group(3);
                int lineNumber = Integer.parseInt(matcher.group(4));
                
                stackElements.add(new StackTrace.StackTraceElement(
                    className,
                    methodName,
                    fileName,
                    lineNumber
                ));
            }
        }
        
        // Add last error if exists
        if (currentException != null) {
            errors.add(createRuntimeError(currentException, currentMessage, stackElements));
        }
        
        return errors;
    }
    
    private RuntimeError createRuntimeError(String exceptionClass, String message, List<StackTrace.StackTraceElement> elements) {
        ErrorType errorType = ErrorPatternMatcher.determineErrorType(message != null ? message : exceptionClass);
        
        StackTrace stackTrace = new StackTrace(
            exceptionClass,
            message != null ? message : "",
            elements
        );
        
        String className = extractClassName(exceptionClass, message);
        String methodName = extractMethodName(elements);
        
        double confidence = ErrorPatternMatcher.isJakartaMigrationRelated(message, className) ? 0.9 : 0.5;
        
        return new RuntimeError(
            errorType,
            message != null ? message : exceptionClass,
            stackTrace,
            className,
            methodName,
            LocalDateTime.now(),
            confidence
        );
    }
    
    private String extractClassName(String exceptionClass, String message) {
        // Try to extract class name from message
        if (message != null) {
            Pattern pattern = Pattern.compile("(javax|jakarta)\\.([\\w\\.]+)");
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                return matcher.group(0);
            }
        }
        return exceptionClass;
    }
    
    private String extractMethodName(List<StackTrace.StackTraceElement> elements) {
        if (!elements.isEmpty()) {
            return elements.get(0).methodName();
        }
        return "unknown";
    }
    
    private String determineRootCause(RuntimeError error, ErrorCategory category, MigrationContext context) {
        switch (category) {
            case NAMESPACE_MIGRATION:
                return String.format(
                    "javax namespace class '%s' not migrated to jakarta. This is likely due to incomplete migration or missing Jakarta dependencies.",
                    error.className()
                );
            case CLASSPATH_ISSUE:
                return String.format(
                    "Jakarta class '%s' not found in classpath. Ensure Jakarta-compatible dependencies are included.",
                    error.className()
                );
            case BINARY_INCOMPATIBILITY:
                return "Mixed javax and jakarta namespaces detected in classpath, causing binary incompatibility.";
            case CONFIGURATION_ERROR:
                return "Configuration file contains javax namespace references that need to be updated to jakarta.";
            default:
                return String.format("Unknown error: %s", error.message());
        }
    }
    
    private List<String> identifyContributingFactors(List<RuntimeError> errors, MigrationContext context) {
        List<String> factors = new ArrayList<>();
        
        if (context.isPostMigration()) {
            factors.add("Migration completed but runtime errors detected");
        }
        
        if (errors.size() > 1) {
            factors.add(String.format("Multiple errors detected (%d total)", errors.size()));
        }
        
        // Check for common patterns
        boolean hasJavax = errors.stream().anyMatch(e -> 
            e.className() != null && e.className().startsWith("javax."));
        boolean hasJakarta = errors.stream().anyMatch(e -> 
            e.className() != null && e.className().startsWith("jakarta."));
        
        if (hasJavax && hasJakarta) {
            factors.add("Mixed javax and jakarta namespaces in codebase");
        }
        
        return factors;
    }
    
    private List<SimilarPastFailure> findSimilarFailures(RuntimeError error) {
        // In a real implementation, this would query a database of past failures
        // For now, return empty list
        return Collections.emptyList();
    }
    
    private List<RemediationStep> generateRemediationSteps(
        RuntimeError error,
        ErrorCategory category,
        MigrationContext context
    ) {
        List<RemediationStep> steps = new ArrayList<>();
        
        switch (category) {
            case NAMESPACE_MIGRATION:
                steps.add(new RemediationStep(
                    "Update import statements",
                    "Replace javax imports with jakarta equivalents",
                    List.of(
                        "Find all occurrences of: " + error.className(),
                        "Replace with jakarta namespace equivalent",
                        "Rebuild and test"
                    ),
                    1
                ));
                steps.add(new RemediationStep(
                    "Add Jakarta dependency",
                    "Ensure Jakarta-compatible dependency is in classpath",
                    List.of(
                        "Check pom.xml or build.gradle",
                        "Add Jakarta dependency if missing",
                        "Remove old javax dependency"
                    ),
                    2
                ));
                break;
            case CLASSPATH_ISSUE:
                steps.add(new RemediationStep(
                    "Add missing Jakarta dependency",
                    "Add the required Jakarta dependency to build file",
                    List.of(
                        "Identify missing dependency: " + error.className(),
                        "Add to pom.xml or build.gradle",
                        "Rebuild project"
                    ),
                    1
                ));
                break;
            case BINARY_INCOMPATIBILITY:
                steps.add(new RemediationStep(
                    "Remove javax dependencies",
                    "Remove all javax.* dependencies from classpath",
                    List.of(
                        "Identify all javax dependencies",
                        "Remove or replace with jakarta equivalents",
                        "Clean and rebuild"
                    ),
                    1
                ));
                break;
            case CONFIGURATION_ERROR:
                steps.add(new RemediationStep(
                    "Update configuration files",
                    "Update XML/properties files to use jakarta namespaces",
                    List.of(
                        "Search for javax references in config files",
                        "Update to jakarta equivalents",
                        "Restart application"
                    ),
                    1
                ));
                break;
        }
        
        return steps;
    }
    
    private double calculateConfidence(RuntimeError error, ErrorCategory category, MigrationContext context) {
        double confidence = 0.5; // Base confidence
        
        // Increase confidence if error is clearly Jakarta-related
        if (ErrorPatternMatcher.isJakartaMigrationRelated(error.message(), error.className())) {
            confidence += 0.3;
        }
        
        // Increase confidence if in post-migration phase
        if (context.isPostMigration()) {
            confidence += 0.1;
        }
        
        // Increase confidence if error type matches category
        if (category != ErrorCategory.UNKNOWN) {
            confidence += 0.1;
        }
        
        return Math.min(1.0, confidence);
    }
    
    private ErrorAnalysis createEmptyAnalysis() {
        return new ErrorAnalysis(
            ErrorCategory.UNKNOWN,
            "No errors to analyze",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            0.0
        );
    }
}

