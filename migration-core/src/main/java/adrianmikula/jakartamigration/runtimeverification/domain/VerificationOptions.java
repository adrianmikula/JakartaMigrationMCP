/*
 * Copyright 2024 Adrian Kozak
 * Copyright 2024 Prairie Trail Software
 *
 * This software is proprietary and may not be used, copied, modified,
 * or distributed except under the terms of a separate commercial license agreement.
 */
package adrianmikula.jakartamigration.runtimeverification.domain;

/**
 * Options for runtime verification.
 * 
 * NOTE: This is a stub. Full implementation with bytecode analysis
 * is available in the premium edition.
 */
public record VerificationOptions(
    boolean scanDependencies,
    boolean checkTransitive,
    boolean generateReport,
    String outputFormat
) {
    public VerificationOptions {
        if (outputFormat == null) {
            outputFormat = "text";
        }
    }
    
    public static VerificationOptions defaults() {
        return new VerificationOptions(false, false, false, "text");
    }
    
    public static VerificationOptions defaultOptions() {
        return new VerificationOptions(false, false, false, "text");
    }
}
