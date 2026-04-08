package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.domain.FileScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.ProjectScanResult;
import adrianmikula.jakartamigration.util.ProjectFileSystemScanner;
import lombok.extern.slf4j.Slf4j;
import org.openrewrite.java.JavaParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Abstract base scanner that consolidates all the duplicate scanner implementation code.
 * Provides common functionality for:
 * - Path validation
 * - Java file discovery
 * - Parallel project scanning
 * - Error handling
 * - Utility methods (findLineNumber, countLines)
 *
 * @param <T> The type of usage found (e.g., JavaxUsage)
 */
@Slf4j
public abstract class BaseScanner<T> {

    protected final ThreadLocal<JavaParser> javaParserThreadLocal = ThreadLocal
            .withInitial(() -> JavaParser.fromJavaVersion().build());

    protected final ProjectFileSystemScanner fileScanner = new ProjectFileSystemScanner();

    // Parallelism configuration - can be overridden via system property
    private static final int MAX_PARALLELISM = Integer.parseInt(
            System.getProperty("advanced.scan.parallelism", "4"));

    // Memory threshold for sequential fallback (100MB)
    private static final long MEMORY_THRESHOLD_BYTES = 100 * 1024 * 1024;

    /**
     * Scans a project for javax.* usages and returns a generic ProjectScanResult.
     *
     * @param projectPath The path to the project directory
     * @param scanTypeName The name of the scan type for logging (e.g., "Bean Validation")
     * @return ProjectScanResult containing all findings
     */
    protected ProjectScanResult<FileScanResult<T>> scanProjectGeneric(Path projectPath, String scanTypeName) {
        if (projectPath == null || !Files.exists(projectPath) || !Files.isDirectory(projectPath)) {
            log.warn("Invalid project path: {}", projectPath);
            return ProjectScanResult.empty();
        }

        try {
            List<Path> javaFiles = discoverJavaFiles(projectPath);

            if (javaFiles.isEmpty()) {
                log.info("No Java files found in project: {}", projectPath);
                return ProjectScanResult.empty();
            }

            log.info("Scanning {} Java files for {} in project: {}", javaFiles.size(), scanTypeName, projectPath);

            // Check available memory and determine parallelism
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long availableMemory = maxMemory - usedMemory;

            AtomicInteger totalScanned = new AtomicInteger(0);
            List<FileScanResult<T>> results;

            // Use sequential processing if low on memory, otherwise use bounded parallelism
            if (availableMemory < MEMORY_THRESHOLD_BYTES) {
                log.info("Low memory detected ({} MB available), using sequential processing for {}",
                        availableMemory / (1024 * 1024), scanTypeName);
                results = javaFiles.stream()
                        .map(file -> scanFileWithTracking(file, totalScanned, scanTypeName))
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList());
            } else {
                // Use bounded parallelism with custom ForkJoinPool
                int parallelism = Math.min(MAX_PARALLELISM, javaFiles.size());
                log.debug("Using parallel processing with parallelism={} for {}", parallelism, scanTypeName);

                ForkJoinPool customPool = new ForkJoinPool(parallelism);
                try {
                    results = customPool.submit(() ->
                            javaFiles.parallelStream()
                                    .map(file -> scanFileWithTracking(file, totalScanned, scanTypeName))
                                    .filter(java.util.Objects::nonNull)
                                    .collect(Collectors.toList())
                    ).get();
                } catch (Exception e) {
                    log.warn("Parallel scan failed for {}, falling back to sequential: {}", scanTypeName, e.getMessage());
                    results = javaFiles.stream()
                            .map(file -> scanFileWithTracking(file, totalScanned, scanTypeName))
                            .filter(java.util.Objects::nonNull)
                            .collect(Collectors.toList());
                } finally {
                    customPool.shutdown();
                }
            }

            // Cleanup ThreadLocal to prevent memory leaks
            cleanupThreadLocal();

            int totalUsages = results.stream()
                    .mapToInt(r -> r.usages().size())
                    .sum();

            log.info("{} scan complete: {} files scanned, {} files with usage, {} total usages",
                    scanTypeName, totalScanned.get(), results.size(), totalUsages);

            return new ProjectScanResult<>(results, totalScanned.get(), results.size(), totalUsages);

        } catch (Exception e) {
            log.error("Error scanning project for {}: {}", scanTypeName, projectPath, e);
            return ProjectScanResult.empty();
        }
    }

    /**
     * Scans a single file for usages. Must be implemented by subclasses.
     *
     * @param filePath The path to the file to scan
     * @return FileScanResult containing all usages found
     */
    public abstract FileScanResult<T> scanFile(Path filePath);

    /**
     * Discovers all Java files in the project.
     */
    protected List<Path> discoverJavaFiles(Path projectPath) {
        return fileScanner.findFiles(projectPath, List.of(".java"));
    }

    /**
     * Finds the line number of a search text in the content lines.
     */
    protected int findLineNumber(String[] lines, String searchText) {
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains(searchText)) {
                return i + 1;
            }
        }
        return 1;
    }

    /**
     * Counts lines in content.
     */
    protected int countLines(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        return content.split("\n").length;
    }

    /**
     * Validates file path and returns null if invalid.
     */
    protected Path validateFilePath(Path filePath) {
        if (filePath == null) {
            throw new IllegalArgumentException("filePath cannot be null");
        }
        if (!Files.exists(filePath)) {
            log.warn("File does not exist: {}", filePath);
            return null;
        }
        return filePath;
    }

    /**
     * Scans a single file with tracking for parallel processing.
     */
    private FileScanResult<T> scanFileWithTracking(Path filePath, AtomicInteger totalScanned, String scanTypeName) {
        totalScanned.incrementAndGet();
        FileScanResult<T> result = scanFile(filePath);
        if (result.hasIssues()) {
            log.debug("Found {} usage in: {}", scanTypeName, filePath);
            return result;
        }
        return null;
    }

    /**
     * Cleans up ThreadLocal JavaParser instances to prevent memory leaks.
     * Should be called after project scan completes.
     */
    protected void cleanupThreadLocal() {
        try {
            javaParserThreadLocal.remove();
        } catch (Exception e) {
            log.debug("Error cleaning up ThreadLocal: {}", e.getMessage());
        }
    }
}
