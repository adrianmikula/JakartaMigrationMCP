package adrianmikula.jakartamigration.runtimeverification.domain;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Options for runtime verification.
 */
public record VerificationOptions(
    Duration timeout,
    long maxMemoryBytes,
    boolean captureStdout,
    boolean captureStderr,
    List<String> jvmArgs
) {
    public VerificationOptions {
        Objects.requireNonNull(timeout, "timeout cannot be null");
        Objects.requireNonNull(jvmArgs, "jvmArgs cannot be null");
        
        if (maxMemoryBytes < 0) {
            throw new IllegalArgumentException("maxMemoryBytes cannot be negative");
        }
    }
    
    /**
     * Creates default verification options.
     */
    public static VerificationOptions defaults() {
        return new VerificationOptions(
            Duration.ofMinutes(5),
            2L * 1024 * 1024 * 1024, // 2GB
            true,
            true,
            java.util.Collections.emptyList()
        );
    }
}

