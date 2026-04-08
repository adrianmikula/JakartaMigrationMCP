package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.SerializationCacheProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.SerializationCacheScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.SerializationCacheUsage;
import adrianmikula.jakartamigration.advancedscanning.service.SerializationCacheScanner;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import adrianmikula.jakartamigration.util.ProjectFileSystemScanner;

/**
 * Scanner implementation for detecting serialization and cache compatibility
 * issues.
 * Helps identify code that serializes or caches javax.* objects which may cause
 * compatibility issues during Jakarta EE migration.
 */
@Slf4j
public class SerializationCacheScannerImpl implements SerializationCacheScanner {

    private final ProjectFileSystemScanner fileScanner = new ProjectFileSystemScanner();

    // Patterns for Java serialization
    private static final Pattern SERIALIZATION_PATTERN = Pattern.compile(
            "(implements\\s+Serializable|new\\s+(?:Object(?:Input|Output)Stream|HttpSession)|@Cache)");
    
    // Third-party cache libraries that may serialize javax objects
    private static final Pattern CACHE_LIB_PATTERN = Pattern.compile(
            "ehcache|hazelcast|infinispan|redis|caffeine|memcached|jboss\\.cache");

    // File extensions to scan
    private static final Set<String> SCAN_EXTENSIONS = Set.of(".java", ".xml", ".properties");

    // Maximum file size to scan (5MB) - larger files are skipped
    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;

    @Override
    public SerializationCacheProjectScanResult scanProject(Path projectPath) {
        log.info("Starting serialization/cache scan for project: {}", projectPath);

        List<SerializationCacheScanResult> fileResults = new ArrayList<>();

        try {
            List<Path> filesToScan = fileScanner.findFiles(projectPath, List.of(".java", ".xml", ".properties"));

            log.info("Found {} files to scan for serialization/cache", filesToScan.size());

            for (Path filePath : filesToScan) {
                try {
                    SerializationCacheScanResult result = scanFile(filePath);
                    if (result.hasFindings()) {
                        fileResults.add(result);
                    }
                } catch (Exception e) {
                    log.warn("Error scanning file {}: {}", filePath, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error walking project directory: {}", e.getMessage());
        }

        log.info("Serialization/cache scan complete. Found {} files with issues", fileResults.size());

        return new SerializationCacheProjectScanResult(
                projectPath.toString(),
                fileResults);
    }

    private SerializationCacheScanResult scanFile(Path filePath) {
        List<SerializationCacheUsage> usages = new ArrayList<>();
        StringBuilder contentBuilder = new StringBuilder();

        try {
            // Skip large files to prevent memory issues
            long fileSize = Files.size(filePath);
            if (fileSize > MAX_FILE_SIZE_BYTES) {
                log.warn("Skipping large file ({} bytes): {}", fileSize, filePath);
                return new SerializationCacheScanResult(filePath.toString(), usages);
            }

            // Deduplication tracking - local to this file only to prevent memory leak
            Set<String> processedLines = new HashSet<>();
            String fileKey = filePath.toString();

            // Use streaming with try-with-resources for memory efficiency
            try (Stream<String> lines = Files.lines(filePath)) {
                final AtomicInteger lineNumber = new AtomicInteger(0);
                lines.forEach(line -> {
                    int currentLineNumber = lineNumber.incrementAndGet();
                    String lineKey = fileKey + ":" + currentLineNumber;

                    // Collect content for cache library pattern check (limited)
                    if (contentBuilder.length() < 10000) {
                        contentBuilder.append(line).append("\n");
                    }

                    // Skip if already processed this line (deduplication within file)
                    if (processedLines.contains(lineKey)) {
                        return;
                    }
                    processedLines.add(lineKey);

                    // Check for any serialization-related pattern
                    Matcher serializationMatcher = SERIALIZATION_PATTERN.matcher(line);
                    if (serializationMatcher.find()) {
                        String usageType = determineUsageType(line);
                        if (usageType != null) {
                            usages.add(new SerializationCacheUsage(
                                    filePath.toString(),
                                    currentLineNumber,
                                    usageType,
                                    extractClassName(line),
                                    extractMethodName(line)));
                        }
                    }
                });
            }

            // Check file content for cache library imports (on limited content)
            if (CACHE_LIB_PATTERN.matcher(contentBuilder.toString()).find()) {
                // Add a single finding for the file if it uses cache libs
                if (!usages.isEmpty()) {
                    usages.add(new SerializationCacheUsage(
                            filePath.toString(),
                            1,
                            "CacheLibrary",
                            "Multiple classes",
                            "Cache library detected"));
                }
            }

        } catch (IOException e) {
            log.warn("Error reading file {}: {}", filePath, e.getMessage());
        }

        return new SerializationCacheScanResult(filePath.toString(), usages);
    }
    
    private String determineUsageType(String line) {
        // Determine the specific type of serialization usage
        if (line.contains("implements Serializable")) {
            return "Serializable";
        } else if (line.contains("ObjectInputStream")) {
            return "ObjectInputStream";
        } else if (line.contains("ObjectOutputStream")) {
            return "ObjectOutputStream";
        } else if (line.contains("HttpSession") && line.contains("javax.")) {
            return "SessionSerialization";
        } else if (line.contains("@Cache")) {
            return "CacheEntry";
        }
        return null;
    }

    private String extractClassName(String line) {
        Pattern classPattern = Pattern.compile("(class|interface|enum)\\s+(\\w+)");
        Matcher matcher = classPattern.matcher(line);
        if (matcher.find()) {
            return matcher.group(2);
        }
        return "Unknown";
    }

    private String extractMethodName(String line) {
        Pattern methodPattern = Pattern.compile("(\\w+)\\s*\\(");
        Matcher matcher = methodPattern.matcher(line);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "Unknown";
    }
}
