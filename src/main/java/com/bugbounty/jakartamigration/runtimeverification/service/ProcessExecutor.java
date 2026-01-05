package com.bugbounty.jakartamigration.runtimeverification.service;

import com.bugbounty.jakartamigration.runtimeverification.domain.ExecutionMetrics;
import com.bugbounty.jakartamigration.runtimeverification.domain.VerificationOptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Executes JAR files in isolated processes and captures output.
 */
public class ProcessExecutor {
    
    /**
     * Executes a JAR file and captures stdout/stderr.
     *
     * @param jarPath Path to the JAR file
     * @param options Verification options
     * @return Process execution result with output and metrics
     */
    public ProcessExecutionResult execute(Path jarPath, VerificationOptions options) {
        List<String> stdout = new ArrayList<>();
        List<String> stderr = new ArrayList<>();
        int exitCode = -1;
        boolean timedOut = false;
        long memoryUsed = 0;
        
        ProcessBuilder processBuilder = new ProcessBuilder();
        
        // Build Java command
        List<String> command = new ArrayList<>();
        command.add("java");
        
        // Add JVM arguments
        if (options.maxMemoryBytes() > 0) {
            command.add("-Xmx" + (options.maxMemoryBytes() / (1024 * 1024)) + "m");
        }
        command.addAll(options.jvmArgs());
        
        // Add JAR file
        command.add("-jar");
        command.add(jarPath.toAbsolutePath().toString());
        
        processBuilder.command(command);
        processBuilder.redirectErrorStream(false);
        
        Process process = null;
        Instant startTime = Instant.now();
        
        try {
            process = processBuilder.start();
            
            // Capture stdout
            if (options.captureStdout()) {
                try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stdout.add(line);
                    }
                }
            }
            
            // Capture stderr
            if (options.captureStderr()) {
                try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stderr.add(line);
                    }
                }
            }
            
            // Wait for process with timeout
            boolean finished = process.waitFor(options.timeout().toSeconds(), TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                timedOut = true;
            } else {
                exitCode = process.exitValue();
            }
            
            // Estimate memory usage (rough approximation)
            Runtime runtime = Runtime.getRuntime();
            memoryUsed = runtime.totalMemory() - runtime.freeMemory();
            
        } catch (IOException e) {
            stderr.add("Failed to execute process: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (process != null) {
                process.destroyForcibly();
            }
            stderr.add("Process execution interrupted: " + e.getMessage());
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
        
        Duration executionTime = Duration.between(startTime, Instant.now());
        
        ExecutionMetrics metrics = new ExecutionMetrics(
            executionTime,
            memoryUsed,
            exitCode,
            timedOut
        );
        
        return new ProcessExecutionResult(stdout, stderr, metrics);
    }
    
    /**
     * Result of process execution.
     */
    public record ProcessExecutionResult(
        List<String> stdout,
        List<String> stderr,
        ExecutionMetrics metrics
    ) {}
}

