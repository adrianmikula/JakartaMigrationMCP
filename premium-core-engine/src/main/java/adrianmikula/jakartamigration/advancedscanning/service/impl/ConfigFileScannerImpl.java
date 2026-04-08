package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.ConfigFileProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.ConfigFileScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.ConfigFileUsage;
import adrianmikula.jakartamigration.advancedscanning.service.ConfigFileScanner;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import adrianmikula.jakartamigration.util.ProjectFileSystemScanner;

@Slf4j
public class ConfigFileScannerImpl implements ConfigFileScanner {

    private final ProjectFileSystemScanner fileScanner = new ProjectFileSystemScanner();

    // Known javax references in configuration files and their Jakarta equivalents
    private static final Map<String, ConfigFileInfo> CONFIG_FILE_PATTERNS = new HashMap<>();

    static {
        // web.xml patterns
        CONFIG_FILE_PATTERNS.put("javax.servlet.http.HttpServlet",
                new ConfigFileInfo("jakarta.servlet.http.HttpServlet", "Servlet", "web.xml"));
        CONFIG_FILE_PATTERNS.put("javax.servlet.ServletContextListener",
                new ConfigFileInfo("jakarta.servlet.ServletContextListener", "Listener", "web.xml"));
        CONFIG_FILE_PATTERNS.put("javax.servlet.Filter",
                new ConfigFileInfo("jakarta.servlet.Filter", "Filter", "web.xml"));

        // Spring XML patterns
        CONFIG_FILE_PATTERNS.put("org.springframework.beans.factory.config.BeanReferenceFactoryBean",
                new ConfigFileInfo("Check for javax references in bean definitions", "Spring", "spring.xml"));
        CONFIG_FILE_PATTERNS.put("org.springframework.jndi.JndiObjectTargetSource",
                new ConfigFileInfo("Check for javax.* JNDI resources", "Spring", "spring.xml"));

        // JMS destination patterns
        CONFIG_FILE_PATTERNS.put("javax.jms.Queue",
                new ConfigFileInfo("jakarta.jms.Queue", "JMS", "config"));
        CONFIG_FILE_PATTERNS.put("javax.jms.Topic",
                new ConfigFileInfo("jakarta.jms.Topic", "JMS", "config"));
    }

    private record ConfigFileInfo(String replacement, String context, String fileType) {
    }

    // Parallelism configuration - can be overridden via system property
    private static final int MAX_PARALLELISM = Integer.parseInt(
            System.getProperty("advanced.scan.parallelism", "4"));

    // Memory threshold for sequential fallback (100MB)
    private static final long MEMORY_THRESHOLD_BYTES = 100 * 1024 * 1024;

    // Pattern for javax references in config files
    private static final Pattern JAVAX_PATTERN = Pattern.compile(
            "javax\\.\\w+(\\.\\w+)*",
            Pattern.MULTILINE);

    // Additional patterns for specific file types
    private static final Pattern XML_SCHEMA_PATTERN = Pattern.compile(
            "http://xmlns\\.javaee",
            Pattern.MULTILINE);

    private static final Pattern SPRING_BEAN_PATTERN = Pattern.compile(
            "<bean\\s+class=\"javax\\.[^\"]+\"",
            Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    @Override
    public ConfigFileProjectScanResult scanProject(Path projectPath) {
        if (projectPath == null || !Files.exists(projectPath) || !Files.isDirectory(projectPath)) {
            return ConfigFileProjectScanResult.empty();
        }

        try {
            List<Path> configFiles = discoverConfigFiles(projectPath);
            if (configFiles.isEmpty())
                return ConfigFileProjectScanResult.empty();

            // Check available memory and determine parallelism
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long availableMemory = maxMemory - usedMemory;

            AtomicInteger totalScanned = new AtomicInteger(0);
            List<ConfigFileScanResult> results;

            // Use sequential processing if low on memory, otherwise use bounded parallelism
            if (availableMemory < MEMORY_THRESHOLD_BYTES) {
                log.info("Low memory detected ({} MB available), using sequential processing for Config File scan",
                        availableMemory / (1024 * 1024));
                results = configFiles.stream()
                        .map(file -> scanFileWithTracking(file, totalScanned))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            } else {
                // Use bounded parallelism with custom ForkJoinPool
                int parallelism = Math.min(MAX_PARALLELISM, configFiles.size());
                log.debug("Using parallel processing with parallelism={} for Config File scan", parallelism);

                ForkJoinPool customPool = new ForkJoinPool(parallelism);
                try {
                    results = customPool.submit(() ->
                            configFiles.parallelStream()
                                    .map(file -> scanFileWithTracking(file, totalScanned))
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toList())
                    ).get();
                } catch (Exception e) {
                    log.warn("Parallel scan failed for Config File, falling back to sequential: {}", e.getMessage());
                    results = configFiles.stream()
                            .map(file -> scanFileWithTracking(file, totalScanned))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                } finally {
                    customPool.shutdown();
                }
            }

            int totalUsages = results.stream().mapToInt(r -> r.getUsages().size()).sum();

            return new ConfigFileProjectScanResult(results, totalScanned.get(), results.size(), totalUsages);
        } catch (Exception e) {
            log.error("Error scanning project for config files", e);
            return ConfigFileProjectScanResult.empty();
        }
    }

    @Override
    public ConfigFileScanResult scanFile(Path filePath) {
        if (filePath == null || !Files.exists(filePath)) {
            return ConfigFileScanResult.empty(filePath);
        }

        try {
            // Skip files in temporary or system directories
            String fullPath = filePath.toString().toLowerCase();
            if (fullPath.contains("tmp") || fullPath.contains("temp") || 
                fullPath.contains("idea-sandbox") || fullPath.contains("system/")) {
                return ConfigFileScanResult.empty(filePath);
            }

            String content = Files.readString(filePath);
            String fileName = filePath.getFileName().toString().toLowerCase();

            String fileType = determineFileType(fileName);
            List<ConfigFileUsage> usages = new ArrayList<>();

            // Check for javax references
            Matcher matcher = JAVAX_PATTERN.matcher(content);
            int lineNumber = 0;
            String[] lines = content.split("\n");

            Set<String> foundReferences = new HashSet<>();
            while (matcher.find()) {
                String javaxRef = matcher.group();
                if (!foundReferences.contains(javaxRef)) {
                    foundReferences.add(javaxRef);
                    lineNumber = findLineNumber(lines, javaxRef);

                    ConfigFileInfo info = CONFIG_FILE_PATTERNS.get(javaxRef);
                    String context = info != null ? info.context() : "Unknown";
                    String replacement = info != null ? info.replacement()
                            : "jakarta." + javaxRef.substring("javax.".length());

                    usages.add(new ConfigFileUsage(javaxRef, context, lineNumber, replacement, fileType));
                }
            }

            // Check for JavaEE namespace in XML files
            if (fileName.endsWith(".xml")) {
                Matcher nsMatcher = XML_SCHEMA_PATTERN.matcher(content);
                while (nsMatcher.find()) {
                    lineNumber = findLineNumber(lines, "javaee");
                    usages.add(new ConfigFileUsage(
                            "http://xmlns.javaee",
                            "XML Namespace",
                            lineNumber,
                            "http://xmlns.jakarta.org",
                            fileType));
                }

                // Check for Spring bean definitions with javax classes
                Matcher beanMatcher = SPRING_BEAN_PATTERN.matcher(content);
                while (beanMatcher.find()) {
                    lineNumber = findLineNumber(lines, "class=\"javax");
                    usages.add(new ConfigFileUsage(
                            "Spring bean with javax class",
                            "Spring",
                            lineNumber,
                            "Update to jakarta class",
                            fileType));
                }
            }

            return new ConfigFileScanResult(filePath, usages, fileType);
        } catch (Exception e) {
            log.debug("Skipping file due to access error: {} - {}", filePath, e.getMessage());
            return ConfigFileScanResult.empty(filePath);
        }
    }

    private List<Path> discoverConfigFiles(Path projectPath) {
        return fileScanner.findFiles(projectPath, List.of(".xml", ".properties", ".yaml", ".yml"));
    }

    private boolean isConfigFile(String name) {
        return name.endsWith(".xml")
                && (name.contains("web") || name.contains("application") || name.contains("beans") ||
                        name.contains("context") || name.contains("config") || name.contains("dispatcher-servlet"))
                || name.endsWith(".properties") || name.endsWith(".yaml") || name.endsWith(".yml");
    }

    private String determineFileType(String fileName) {
        if (fileName.endsWith(".xml"))
            return "XML";
        if (fileName.endsWith(".properties"))
            return "Properties";
        if (fileName.endsWith(".yaml") || fileName.endsWith(".yml"))
            return "YAML";
        return "Unknown";
    }

    private int findLineNumber(String[] lines, String searchText) {
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains(searchText))
                return i + 1;
        }
        return 1;
    }

    /**
     * Scans a single file with tracking for parallel processing.
     */
    private ConfigFileScanResult scanFileWithTracking(Path filePath, AtomicInteger totalScanned) {
        totalScanned.incrementAndGet();
        ConfigFileScanResult result = scanFile(filePath);
        return result.hasJavaxUsage() ? result : null;
    }
}
