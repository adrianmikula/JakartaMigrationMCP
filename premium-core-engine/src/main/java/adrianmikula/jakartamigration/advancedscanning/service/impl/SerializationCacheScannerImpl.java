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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    
    // Deduplication tracking
    private final Set<String> processedLines = new HashSet<>();

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
        String fileKey = filePath.toString();

        try {
            String content = Files.readString(filePath);
            String[] lines = content.split("\\r?\\n");

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                int lineNumber = i + 1;
                String lineKey = fileKey + ":" + lineNumber;
                
                // Skip if already processed this line (deduplication)
                if (processedLines.contains(lineKey)) {
                    continue;
                }
                processedLines.add(lineKey);

                // Check for any serialization-related pattern
                Matcher serializationMatcher = SERIALIZATION_PATTERN.matcher(line);
                if (serializationMatcher.find()) {
                    String usageType = determineUsageType(line);
                    if (usageType != null) {
                        usages.add(new SerializationCacheUsage(
                                filePath.toString(),
                                lineNumber,
                                usageType,
                                extractClassName(line),
                                extractMethodName(line)));
                    }
                }
            }

            // Check file content for cache library imports
            if (CACHE_LIB_PATTERN.matcher(content).find()) {
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
