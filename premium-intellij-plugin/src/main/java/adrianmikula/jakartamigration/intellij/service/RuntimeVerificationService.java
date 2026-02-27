package adrianmikula.jakartamigration.intellij.service;

import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.runtimeverification.domain.*;
import adrianmikula.jakartamigration.runtimeverification.service.RuntimeVerificationModule;
import adrianmikula.jakartamigration.runtimeverification.service.impl.RuntimeVerificationModuleImpl;
import com.intellij.openapi.diagnostic.Logger;

import java.nio.file.Path;
import java.util.List;

/**
 * Service for performing runtime verification using the core runtime verification library.
 * This service provides direct access to the runtime verification module
 * for detecting Jakarta migration issues at runtime.
 */
public class RuntimeVerificationService {
    private static final Logger LOG = Logger.getInstance(RuntimeVerificationService.class);

    private final RuntimeVerificationModule runtimeVerificationModule;

    public RuntimeVerificationService() {
        // Create instance of the core library component directly
        this.runtimeVerificationModule = new RuntimeVerificationModuleImpl();

        LOG.info("RuntimeVerificationService initialized with core library");
    }

    /**
     * Verifies a JAR file for Jakarta migration issues using bytecode analysis.
     *
     * @param jarPath Path to the JAR file to verify
     * @return BytecodeAnalysisResult containing the analysis results
     */
    public BytecodeAnalysisResult analyzeBytecode(Path jarPath) {
        LOG.info("Analyzing bytecode of: " + jarPath);
        return runtimeVerificationModule.analyzeBytecode(jarPath);
    }

    /**
     * Verifies runtime by executing the JAR and monitoring for issues.
     *
     * @param jarPath Path to the JAR file to verify
     * @param options Verification options
     * @return VerificationResult containing detected errors and analysis
     */
    public VerificationResult verifyRuntime(Path jarPath, VerificationOptions options) {
        LOG.info("Verifying runtime of: " + jarPath);
        return runtimeVerificationModule.verifyRuntime(jarPath, options, VerificationStrategy.PROCESS_ONLY);
    }

    /**
     * Performs static analysis on the project for migration issues.
     *
     * @param projectPath Path to the project root
     * @param dependencyGraph Dependency graph of the project
     * @return StaticAnalysisResult containing the analysis
     */
    public StaticAnalysisResult performStaticAnalysis(Path projectPath, DependencyGraph dependencyGraph) {
        LOG.info("Performing static analysis on: " + projectPath);
        return runtimeVerificationModule.performStaticAnalysis(projectPath, dependencyGraph);
    }

    /**
     * Performs health check on a running application.
     *
     * @param applicationUrl URL of the application to check
     * @param options Health check options
     * @return HealthCheckResult containing the health status
     */
    public HealthCheckResult performHealthCheck(String applicationUrl, HealthCheckOptions options) {
        LOG.info("Performing health check on: " + applicationUrl);
        return runtimeVerificationModule.performHealthCheck(applicationUrl, options);
    }

    /**
     * Analyzes runtime errors for Jakarta migration issues.
     *
     * @param errors List of runtime errors to analyze
     * @param context Migration context information
     * @return ErrorAnalysis with root cause and remediation suggestions
     */
    public ErrorAnalysis analyzeErrors(List<RuntimeError> errors, MigrationContext context) {
        LOG.info("Analyzing " + errors.size() + " runtime errors");
        return runtimeVerificationModule.analyzeErrors(errors, context);
    }

    /**
     * Gets the default verification options.
     *
     * @return Default VerificationOptions
     */
    public VerificationOptions getDefaultVerificationOptions() {
        return VerificationOptions.defaults();
    }

    /**
     * Gets the default health check options.
     *
     * @return Default HealthCheckOptions
     */
    public HealthCheckOptions getDefaultHealthCheckOptions() {
        return HealthCheckOptions.defaults();
    }
}
