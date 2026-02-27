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
    private static final Pattern SERIALIZABLE_PATTERN = Pattern.compile(
            "implements\\s+(?:public\\s+)?Serializable|" +
                    "extends\\s+\\w+\\s+implements\\s+Serializable");

    private static final Pattern OBJECT_INPUT_STREAM = Pattern.compile(
            "new\\s+ObjectInputStream\\(|" +
                    "ObjectInputStream\\s+\\w+\\s*=");

    private static final Pattern OBJECT_OUTPUT_STREAM = Pattern.compile(
            "new\\s+ObjectOutputStream\\(|" +
                    "ObjectOutputStream\\s+\\w+\\s*=");

    // Patterns for session serialization (HttpSession)
    private static final Pattern HTTP_SESSION_PATTERN = Pattern.compile(
            "HttpSession|getSession\\(|setAttribute\\(|getAttribute\\(|removeAttribute\\(");

    // Patterns for cache implementations
    private static final Pattern CACHE_PATTERN = Pattern.compile(
            "Cache\\s*\\{|" +
                    "@Cache(?:able|Evict|Put)?|" +
                    "CacheManager|");

    // Third-party cache libraries that may serialize javax objects
    private static final Pattern CACHE_LIB_PATTERN = Pattern.compile(
            "ehcache|hazelcast|infinispan|redis|caffeine|memcached|jboss\\.cache");

    // File extensions to scan
    private static final Set<String> SCAN_EXTENSIONS = Set.of(".java", ".xml", ".properties");

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

        try {
            String content = Files.readString(filePath);
            String[] lines = content.split("\\r?\\n");

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                int lineNumber = i + 1;

                // Check for Serializable implementation
                Matcher serializableMatcher = SERIALIZABLE_PATTERN.matcher(line);
                if (serializableMatcher.find()) {
                    usages.add(new SerializationCacheUsage(
                            filePath.toString(),
                            lineNumber,
                            "Serializable",
                            extractClassName(line),
                            "class definition"));
                }

                // Check for ObjectInputStream (deserialization)
                Matcher oisMatcher = OBJECT_INPUT_STREAM.matcher(line);
                if (oisMatcher.find()) {
                    usages.add(new SerializationCacheUsage(
                            filePath.toString(),
                            lineNumber,
                            "ObjectInputStream",
                            extractClassName(line),
                            extractMethodName(line)));
                }

                // Check for ObjectOutputStream (serialization)
                Matcher oosMatcher = OBJECT_OUTPUT_STREAM.matcher(line);
                if (oosMatcher.find()) {
                    usages.add(new SerializationCacheUsage(
                            filePath.toString(),
                            lineNumber,
                            "ObjectOutputStream",
                            extractClassName(line),
                            extractMethodName(line)));
                }

                // Check for HttpSession usage
                Matcher sessionMatcher = HTTP_SESSION_PATTERN.matcher(line);
                if (sessionMatcher.find() && line.contains("javax.")) {
                    usages.add(new SerializationCacheUsage(
                            filePath.toString(),
                            lineNumber,
                            "SessionSerialization",
                            extractClassName(line),
                            extractMethodName(line)));
                }

                // Check for cache annotations/usage
                Matcher cacheMatcher = CACHE_PATTERN.matcher(line);
                if (cacheMatcher.find()) {
                    usages.add(new SerializationCacheUsage(
                            filePath.toString(),
                            lineNumber,
                            "CacheEntry",
                            extractClassName(line),
                            extractMethodName(line)));
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
