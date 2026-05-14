package adrianmikula.jakartamigration.jaranalysis.service;

import adrianmikula.jakartamigration.jaranalysis.domain.JarCompatibilityReport;
import adrianmikula.jakartamigration.jaranalysis.domain.JarScanOptions;

import java.util.List;
import java.util.Map;

/**
 * JAR compatibility scanning service.
 * Analyzes JAR files to determine Jakarta migration compatibility level.
 * Corresponds to TypeSpec: JarCompatibilityScanner interface
 */
public interface JarCompatibilityScanner {

    /**
     * Analyze a single JAR file for Jakarta compatibility.
     *
     * @param jarPath Path to JAR file on the filesystem
     * @param options Optional scanning options (uses defaults if null)
     * @return Complete compatibility report
     */
    JarCompatibilityReport analyzeJar(java.nio.file.Path jarPath, JarScanOptions options);

    /**
     * Analyze a single JAR file using default options.
     *
     * @param jarPath Path to JAR file
     * @return Complete compatibility report
     */
    JarCompatibilityReport analyzeJar(java.nio.file.Path jarPath);

    /**
     * Batch analyze multiple JAR files.
     *
     * @param jarPaths List of JAR file paths
     * @param options Optional scanning options
     * @return List of compatibility reports in the same order as input
     */
    List<JarCompatibilityReport> analyzeJars(List<java.nio.file.Path> jarPaths, JarScanOptions options);

    /**
     * Get cached result if available.
     *
     * @param artifactCoordinate Artifact coordinate (groupId:artifactId:version)
     * @return Cached report or null if not cached
     */
    JarCompatibilityReport getCachedResult(String artifactCoordinate);

    /**
     * Clear the analysis cache.
     *
     * @return true if cache was cleared successfully
     */
    boolean clearCache();

    /**
     * Get cache statistics.
     *
     * @return Map containing cache statistics (size, hit rate, etc.)
     */
    Map<String, Object> getCacheStats();
}