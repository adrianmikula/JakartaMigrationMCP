package com.bugbounty.jakartamigration.runtimeverification.domain;

import java.util.List;
import java.util.Objects;

/**
 * Represents a stack trace from a runtime error.
 */
public record StackTrace(
    String exceptionClass,
    String exceptionMessage,
    List<StackTraceElement> elements
) {
    public StackTrace {
        Objects.requireNonNull(exceptionClass, "exceptionClass cannot be null");
        Objects.requireNonNull(elements, "elements cannot be null");
    }
    
    /**
     * Represents a single stack trace element.
     */
    public record StackTraceElement(
        String className,
        String methodName,
        String fileName,
        int lineNumber
    ) {
        public StackTraceElement {
            Objects.requireNonNull(className, "className cannot be null");
            Objects.requireNonNull(methodName, "methodName cannot be null");
        }
    }
}

