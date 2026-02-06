/*
 * Copyright 2024 Adrian Kozak
 * Copyright 2024 Prairie Trail Software
 *
 * This software is proprietary and may not be used, copied, modified,
 * or distributed except under the terms of a separate commercial license agreement.
 */
package adrianmikula.jakartamigration.runtimeverification.domain;

/**
 * Category of verification errors.
 * 
 * NOTE: This is a stub. Full implementation with detailed error
 * categorization is available in the premium edition.
 */
public enum ErrorCategory {
    UNKNOWN,
    COMPILATION,
    RUNTIME,
    DEPENDENCY,
    CONFIGURATION,
    PERMISSION,
    TIMEOUT,
    MEMORY
}
