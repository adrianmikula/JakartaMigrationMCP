package adrianmikula.jakartamigration.runtimeverification.domain;

/**
 * Strategy for runtime verification approach.
 */
public enum VerificationStrategy {
    /**
     * Use bytecode analysis only (fast, lightweight).
     * Recommended for MCP tool usage.
     */
    BYTECODE_ONLY,
    
    /**
     * Use process execution only (comprehensive, slower).
     * Use when full runtime verification is needed.
     */
    PROCESS_ONLY,
    
    /**
     * Use bytecode analysis first, then process execution if issues found.
     * Best balance of speed and thoroughness.
     */
    BYTECODE_THEN_PROCESS,
    
    /**
     * Use both approaches in parallel and combine results.
     * Most comprehensive but slowest.
     */
    BOTH_PARALLEL
}

