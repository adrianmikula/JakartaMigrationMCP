package adrianmikula.jakartamigration.runtimeverification.domain;

import java.util.Objects;

/**
 * Options for class loader instrumentation.
 */
public record InstrumentationOptions(
    boolean trackNamespaceUsage,
    boolean logAllLoads,
    boolean detectConflicts,
    String agentPath
) {
    public InstrumentationOptions {
        Objects.requireNonNull(agentPath, "agentPath cannot be null");
    }
    
    /**
     * Creates default instrumentation options.
     */
    public static InstrumentationOptions defaults() {
        return new InstrumentationOptions(
            true,
            false,
            true,
            ""
        );
    }
}

