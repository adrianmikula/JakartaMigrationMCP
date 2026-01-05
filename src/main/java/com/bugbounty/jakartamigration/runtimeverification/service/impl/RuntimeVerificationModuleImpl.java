package com.bugbounty.jakartamigration.runtimeverification.service.impl;

import com.bugbounty.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import com.bugbounty.jakartamigration.runtimeverification.domain.*;
import com.bugbounty.jakartamigration.runtimeverification.service.ErrorAnalyzer;
import com.bugbounty.jakartamigration.runtimeverification.service.ProcessExecutor;
import com.bugbounty.jakartamigration.runtimeverification.service.RuntimeVerificationModule;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of RuntimeVerificationModule.
 */
public class RuntimeVerificationModuleImpl implements RuntimeVerificationModule {
    
    private final ProcessExecutor processExecutor;
    private final ErrorAnalyzer errorAnalyzer;
    private final HttpClient httpClient;
    
    public RuntimeVerificationModuleImpl() {
        this.processExecutor = new ProcessExecutor();
        this.errorAnalyzer = new ErrorAnalyzer();
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }
    
    public RuntimeVerificationModuleImpl(
        ProcessExecutor processExecutor,
        ErrorAnalyzer errorAnalyzer,
        HttpClient httpClient
    ) {
        this.processExecutor = processExecutor;
        this.errorAnalyzer = errorAnalyzer;
        this.httpClient = httpClient;
    }
    
    @Override
    public VerificationResult verifyRuntime(Path jarPath, VerificationOptions options) {
        if (!Files.exists(jarPath)) {
            return createErrorResult(
                VerificationStatus.FAILED,
                "JAR file does not exist: " + jarPath,
                options
            );
        }
        
        if (!Files.isRegularFile(jarPath)) {
            return createErrorResult(
                VerificationStatus.FAILED,
                "Path is not a file: " + jarPath,
                options
            );
        }
        
        // Execute the JAR file
        ProcessExecutor.ProcessExecutionResult executionResult = processExecutor.execute(jarPath, options);
        
        // Parse errors from output
        List<RuntimeError> errors = errorAnalyzer.parseErrorsFromOutput(
            executionResult.stderr(),
            executionResult.stdout()
        );
        
        // Determine status
        VerificationStatus status = determineStatus(executionResult, errors);
        
        // Generate warnings from stdout/stderr
        List<Warning> warnings = generateWarnings(executionResult);
        
        // Analyze errors
        MigrationContext context = new MigrationContext(
            new DependencyGraph(),
            "RUNTIME_VERIFICATION",
            false
        );
        
        ErrorAnalysis analysis = errorAnalyzer.analyzeErrors(errors, context);
        
        // Generate remediation steps
        List<RemediationStep> remediationSteps = analysis.suggestedFixes();
        
        return new VerificationResult(
            status,
            errors,
            warnings,
            executionResult.metrics(),
            analysis,
            remediationSteps
        );
    }
    
    @Override
    public ErrorAnalysis analyzeErrors(List<RuntimeError> errors, MigrationContext context) {
        return errorAnalyzer.analyzeErrors(errors, context);
    }
    
    @Override
    public StaticAnalysisResult performStaticAnalysis(Path projectPath, DependencyGraph dependencyGraph) {
        if (!Files.exists(projectPath)) {
            return new StaticAnalysisResult(
                true,
                Collections.emptyList(),
                List.of(new Warning(
                    "Project path does not exist: " + projectPath,
                    "PATH_ERROR",
                    java.time.LocalDateTime.now(),
                    1.0
                )),
                dependencyGraph,
                "Static analysis failed: project path does not exist"
            );
        }
        
        // Perform basic static analysis
        List<RuntimeError> potentialErrors = new ArrayList<>();
        List<Warning> warnings = new ArrayList<>();
        
        // Check for common Jakarta migration issues in source files
        try {
            Files.walk(projectPath)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(javaFile -> {
                    analyzeJavaFile(javaFile, potentialErrors, warnings);
                });
        } catch (Exception e) {
            warnings.add(new Warning(
                "Error during static analysis: " + e.getMessage(),
                "ANALYSIS_ERROR",
                java.time.LocalDateTime.now(),
                0.5
            ));
        }
        
        boolean hasIssues = !potentialErrors.isEmpty() || !warnings.isEmpty();
        String summary = String.format(
            "Static analysis completed. Found %d potential errors and %d warnings.",
            potentialErrors.size(),
            warnings.size()
        );
        
        return new StaticAnalysisResult(
            hasIssues,
            potentialErrors,
            warnings,
            dependencyGraph,
            summary
        );
    }
    
    @Override
    public ClassLoaderAnalysisResult instrumentClassLoading(Path jarPath, InstrumentationOptions options) {
        // This is a placeholder implementation
        // In a real implementation, this would use a Java agent to instrument class loading
        return new ClassLoaderAnalysisResult(
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            "Class loader instrumentation not yet implemented. Use verifyRuntime() for runtime verification."
        );
    }
    
    @Override
    public HealthCheckResult performHealthCheck(String applicationUrl, HealthCheckOptions options) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(java.net.URI.create(applicationUrl + options.healthEndpoint()))
                .timeout(options.timeout())
                .GET()
                .build();
            
            Instant startTime = Instant.now();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            Duration responseTime = Duration.between(startTime, Instant.now());
            
            boolean healthy = response.statusCode() == options.expectedStatusCode();
            List<String> issues = new ArrayList<>();
            
            if (!healthy) {
                issues.add(String.format(
                    "Expected status code %d but got %d",
                    options.expectedStatusCode(),
                    response.statusCode()
                ));
            }
            
            // Check response body for health indicators
            String body = response.body();
            if (body != null && body.toLowerCase().contains("\"status\":\"down\"")) {
                healthy = false;
                issues.add("Health check endpoint reports status: down");
            }
            
            return new HealthCheckResult(
                healthy,
                response.statusCode(),
                responseTime,
                body,
                issues
            );
            
        } catch (Exception e) {
            return new HealthCheckResult(
                false,
                0,
                Duration.ZERO,
                "",
                List.of("Health check failed: " + e.getMessage())
            );
        }
    }
    
    private VerificationStatus determineStatus(
        ProcessExecutor.ProcessExecutionResult executionResult,
        List<RuntimeError> errors
    ) {
        if (executionResult.metrics().timedOut()) {
            return VerificationStatus.TIMEOUT;
        }
        
        if (executionResult.metrics().exitCode() != 0) {
            if (!errors.isEmpty()) {
                return VerificationStatus.FAILED;
            }
            return VerificationStatus.PARTIAL;
        }
        
        if (!errors.isEmpty()) {
            return VerificationStatus.PARTIAL;
        }
        
        return VerificationStatus.SUCCESS;
    }
    
    private List<Warning> generateWarnings(ProcessExecutor.ProcessExecutionResult executionResult) {
        List<Warning> warnings = new ArrayList<>();
        
        // Check stderr for warnings
        for (String line : executionResult.stderr()) {
            String lowerLine = line.toLowerCase();
            if (lowerLine.contains("warning") || lowerLine.contains("deprecated")) {
                warnings.add(new Warning(
                    line,
                    "RUNTIME_WARNING",
                    java.time.LocalDateTime.now(),
                    0.5
                ));
            }
        }
        
        return warnings;
    }
    
    private VerificationResult createErrorResult(
        VerificationStatus status,
        String errorMessage,
        VerificationOptions options
    ) {
        RuntimeError error = new RuntimeError(
            ErrorType.OTHER,
            errorMessage,
            new StackTrace("RuntimeVerificationModule", errorMessage, Collections.emptyList()),
            "RuntimeVerificationModule",
            "verifyRuntime",
            java.time.LocalDateTime.now(),
            1.0
        );
        
        ExecutionMetrics metrics = new ExecutionMetrics(
            Duration.ZERO,
            0,
            -1,
            false
        );
        
        ErrorAnalysis analysis = new ErrorAnalysis(
            ErrorCategory.UNKNOWN,
            errorMessage,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            1.0
        );
        
        return new VerificationResult(
            status,
            List.of(error),
            Collections.emptyList(),
            metrics,
            analysis,
            Collections.emptyList()
        );
    }
    
    private void analyzeJavaFile(Path javaFile, List<RuntimeError> potentialErrors, List<Warning> warnings) {
        try {
            String content = Files.readString(javaFile);
            
            // Check for javax imports that should be jakarta
            if (content.contains("import javax.servlet") ||
                content.contains("import javax.persistence") ||
                content.contains("import javax.ejb") ||
                content.contains("import javax.validation")) {
                
                warnings.add(new Warning(
                    String.format("File %s contains javax imports that may need migration", javaFile.getFileName()),
                    "JAVAX_IMPORT",
                    java.time.LocalDateTime.now(),
                    0.7
                ));
            }
            
            // Check for mixed namespaces
            boolean hasJavax = content.contains("javax.servlet") || content.contains("javax.persistence");
            boolean hasJakarta = content.contains("jakarta.servlet") || content.contains("jakarta.persistence");
            
            if (hasJavax && hasJakarta) {
                potentialErrors.add(new RuntimeError(
                    ErrorType.LINKAGE_ERROR,
                    String.format("Mixed javax and jakarta namespaces in %s", javaFile.getFileName()),
                    new StackTrace("StaticAnalysis", "Mixed namespaces", Collections.emptyList()),
                    javaFile.getFileName().toString(),
                    "static_analysis",
                    java.time.LocalDateTime.now(),
                    0.8
                ));
            }
            
        } catch (Exception e) {
            warnings.add(new Warning(
                "Error analyzing file " + javaFile + ": " + e.getMessage(),
                "ANALYSIS_ERROR",
                java.time.LocalDateTime.now(),
                0.3
            ));
        }
    }
}

