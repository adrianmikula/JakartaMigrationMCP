package adrianmikula.jakartamigration.runtimeverification.service;

import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.runtimeverification.domain.*;

import java.nio.file.Path;
import java.util.List;

/**
 * Main interface for the Runtime Verification Module.
 * Supports both bytecode analysis (fast) and process execution (comprehensive)
 * for detecting Jakarta migration issues.
 */
public interface RuntimeVerificationModule {
    
    /**
     * Verifies runtime using the specified strategy.
     *
     * @param jarPath Path to the JAR file to verify
     * @param options Verification options
     * @param strategy Verification strategy to use
     * @return Verification result with errors, warnings, and analysis
     */
    VerificationResult verifyRuntime(
        Path jarPath,
        VerificationOptions options,
        VerificationStrategy strategy
    );
    
    /**
     * Executes JAR in isolated process and monitors for issues.
     * Uses PROCESS_ONLY strategy (backward compatibility).
     *
     * @param jarPath Path to the JAR file to execute
     * @param options Verification options
     * @return Verification result with errors, warnings, and analysis
     */
    VerificationResult verifyRuntime(Path jarPath, VerificationOptions options);
    
    /**
     * Analyzes JAR using bytecode analysis (fast, lightweight).
     *
     * @param jarPath Path to the JAR file to analyze
     * @return Bytecode analysis result
     */
    BytecodeAnalysisResult analyzeBytecode(Path jarPath);
    
    /**
     * Analyzes runtime errors for Jakarta migration issues.
     *
     * @param errors List of runtime errors to analyze
     * @param context Migration context information
     * @return Error analysis with root cause and remediation suggestions
     */
    ErrorAnalysis analyzeErrors(
        List<RuntimeError> errors,
        MigrationContext context
    );
    
    /**
     * Performs static analysis as alternative to runtime execution.
     *
     * @param projectPath Path to the project root
     * @param dependencyGraph Dependency graph of the project
     * @return Static analysis result
     */
    StaticAnalysisResult performStaticAnalysis(
        Path projectPath,
        DependencyGraph dependencyGraph
    );
    
    /**
     * Instruments class loading to detect resolution issues.
     *
     * @param jarPath Path to the JAR file
     * @param options Instrumentation options
     * @return Class loader analysis result
     */
    ClassLoaderAnalysisResult instrumentClassLoading(
        Path jarPath,
        InstrumentationOptions options
    );
    
    /**
     * Validates application health after migration.
     *
     * @param applicationUrl URL of the application to check
     * @param options Health check options
     * @return Health check result
     */
    HealthCheckResult performHealthCheck(
        String applicationUrl,
        HealthCheckOptions options
    );
}

