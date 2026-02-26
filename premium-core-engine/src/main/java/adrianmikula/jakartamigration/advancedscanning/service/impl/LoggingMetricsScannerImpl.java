package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.LoggingMetricsProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.LoggingMetricsScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.LoggingMetricsUsage;
import adrianmikula.jakartamigration.advancedscanning.service.LoggingMetricsScanner;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import adrianmikula.jakartamigration.util.ProjectFileSystemScanner;

/**
 * Scanner implementation for detecting javax.logging and JMX API usage.
 * Helps identify observability/monitoring code that needs migration to Jakarta
 * EE.
 */
@Slf4j
public class LoggingMetricsScannerImpl implements LoggingMetricsScanner {

    private final ProjectFileSystemScanner fileScanner = new ProjectFileSystemScanner();

    // Patterns to detect javax.logging usage
    private static final Pattern LOGGER_PATTERN = Pattern.compile(
            "import\\s+javax\\.logging\\.([^;]+);|" +
                    "Logger\\s+\\w+\\s*=\\s*Logger\\.getLogger\\(|" +
                    "\\.getLogger\\(\\s*\"[^\"]+\"\\s*\\)|" +
                    "import\\s+java\\.util\\.logging\\.Logger;");

    // Patterns to detect JMX-related usage
    private static final Pattern JMX_PATTERN = Pattern.compile(
            "import\\s+javax\\.management\\.([^;]+);|" +
                    "import\\s+javax\\.management\\.monitoring\\.([^;]+);|" +
                    "import\\s+javax\\.management\\.modelmbean\\.([^;]+);|" +
                    "import\\s+javax\\.management\\.remote\\.([^;]+);|" +
                    "MBeanServer|ObjectName|JMXConnectorServer|JMXConnectorClient");

    // Patterns for javax.annotation processing (for logging via annotations)
    private static final Pattern ANNOTATION_PATTERN = Pattern.compile(
            "import\\s+javax\\.annotation\\.([^;]+);|" +
                    "@PostConstruct|@PreDestroy");

    // File extensions to scan
    private static final Set<String> SCAN_EXTENSIONS = Set.of(".java", ".xml", ".properties");

    @Override
    public LoggingMetricsProjectScanResult scanProject(Path projectPath) {
        log.info("Starting logging/metrics scan for project: {}", projectPath);

        List<LoggingMetricsScanResult> fileResults = new ArrayList<>();

        try {
            List<Path> filesToScan = fileScanner.findFiles(projectPath, List.of(".java", ".xml", ".properties"));

            log.info("Found {} files to scan for logging/metrics", filesToScan.size());

            for (Path filePath : filesToScan) {
                try {
                    LoggingMetricsScanResult result = scanFile(filePath);
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

        log.info("Logging/metrics scan complete. Found {} files with issues", fileResults.size());

        return new LoggingMetricsProjectScanResult(
                projectPath.toString(),
                fileResults);
    }

    private LoggingMetricsScanResult scanFile(Path filePath) {
        List<LoggingMetricsUsage> usages = new ArrayList<>();

        try {
            String content = Files.readString(filePath);
            String[] lines = content.split("\\r?\\n");

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                int lineNumber = i + 1;

                // Check for javax.logging usage
                Matcher loggerMatcher = LOGGER_PATTERN.matcher(line);
                if (loggerMatcher.find()) {
                    String usageType = detectUsageType(line, "javax.logging", "java.util.logging");
                    usages.add(new LoggingMetricsUsage(
                            filePath.toString(),
                            lineNumber,
                            usageType,
                            extractClassName(line),
                            extractMethodName(line)));
                }

                // Check for JMX usage
                Matcher jmxMatcher = JMX_PATTERN.matcher(line);
                if (jmxMatcher.find()) {
                    String usageType = detectUsageType(line, "javax.management", "jakarta.management");
                    usages.add(new LoggingMetricsUsage(
                            filePath.toString(),
                            lineNumber,
                            usageType,
                            extractClassName(line),
                            extractMethodName(line)));
                }

                // Check for javax.annotation usage
                Matcher annotationMatcher = ANNOTATION_PATTERN.matcher(line);
                if (annotationMatcher.find()) {
                    String usageType = detectUsageType(line, "javax.annotation", "jakarta.annotation");
                    usages.add(new LoggingMetricsUsage(
                            filePath.toString(),
                            lineNumber,
                            usageType,
                            extractClassName(line),
                            extractMethodName(line)));
                }
            }
        } catch (IOException e) {
            log.warn("Error reading file {}: {}", filePath, e.getMessage());
        }

        return new LoggingMetricsScanResult(filePath.toString(), usages);
    }

    private String detectUsageType(String line, String oldPackage, String newPackage) {
        if (line.contains(oldPackage)) {
            return oldPackage + ".*";
        }
        if (line.contains(newPackage)) {
            return newPackage + ".* (already migrated)";
        }
        return "Logging/Metrics API";
    }

    private String extractClassName(String line) {
        // Try to extract class name from the line
        Pattern classPattern = Pattern.compile("(class|interface|enum)\\s+(\\w+)");
        Matcher matcher = classPattern.matcher(line);
        if (matcher.find()) {
            return matcher.group(2);
        }
        return "Unknown";
    }

    private String extractMethodName(String line) {
        // Try to extract method name from the line
        Pattern methodPattern = Pattern.compile("(\\w+)\\s*\\(");
        Matcher matcher = methodPattern.matcher(line);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "Unknown";
    }
}
