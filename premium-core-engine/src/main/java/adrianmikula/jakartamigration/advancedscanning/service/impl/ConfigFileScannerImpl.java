package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.ConfigFileProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.ConfigFileScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.ConfigFileUsage;
import adrianmikula.jakartamigration.advancedscanning.service.ConfigFileScanner;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class ConfigFileScannerImpl implements ConfigFileScanner {

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

    private record ConfigFileInfo(String replacement, String context, String fileType) {}

    // Pattern for javax references in config files
    private static final Pattern JAVAX_PATTERN = Pattern.compile(
        "javax\\.\\w+(\\.\\w+)*",
        Pattern.MULTILINE
    );

    // Additional patterns for specific file types
    private static final Pattern XML_SCHEMA_PATTERN = Pattern.compile(
        "http://xmlns\\.javaee",
        Pattern.MULTILINE
    );

    private static final Pattern SPRING_BEAN_PATTERN = Pattern.compile(
        "<bean\\s+class=\"javax\\.[^\"]+\"",
        Pattern.MULTILINE | Pattern.CASE_INSENSITIVE
    );

    @Override
    public ConfigFileProjectScanResult scanProject(Path projectPath) {
        if (projectPath == null || !Files.exists(projectPath) || !Files.isDirectory(projectPath)) {
            return ConfigFileProjectScanResult.empty();
        }

        try {
            List<Path> configFiles = discoverConfigFiles(projectPath);
            if (configFiles.isEmpty()) return ConfigFileProjectScanResult.empty();

            AtomicInteger totalScanned = new AtomicInteger(0);
            List<ConfigFileScanResult> results = configFiles.parallelStream()
                .map(file -> {
                    totalScanned.incrementAndGet();
                    ConfigFileScanResult result = scanFile(file);
                    return result.hasJavaxUsage() ? result : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

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
                    String replacement = info != null ? info.replacement() : "jakarta." + javaxRef.substring("javax.".length());
                    
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
                        fileType
                    ));
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
                        fileType
                    ));
                }
            }

            return new ConfigFileScanResult(filePath, usages, fileType);
        } catch (Exception e) {
            return ConfigFileScanResult.empty(filePath);
        }
    }

    private List<Path> discoverConfigFiles(Path projectPath) {
        try (Stream<Path> paths = Files.walk(projectPath)) {
            return paths.filter(Files::isRegularFile)
                .filter(p -> {
                    String name = p.getFileName().toString().toLowerCase();
                    return isConfigFile(name);
                })
                .filter(this::shouldScanFile)
                .collect(Collectors.toList());
        } catch (IOException e) {
            return List.of();
        }
    }

    private boolean isConfigFile(String name) {
        return name.endsWith(".xml") && (
            name.contains("web") || name.contains("application") || name.contains("beans") ||
            name.contains("context") || name.contains("config") || name.contains("dispatcher-servlet")
        ) || name.endsWith(".properties") || name.endsWith(".yaml") || name.endsWith(".yml");
    }

    private String determineFileType(String fileName) {
        if (fileName.endsWith(".xml")) return "XML";
        if (fileName.endsWith(".properties")) return "Properties";
        if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) return "YAML";
        return "Unknown";
    }

    private boolean shouldScanFile(Path file) {
        String path = file.toString().replace('\\', '/');
        return !path.contains("/target/") && !path.contains("/build/") && !path.contains("/.git/");
    }

    private int findLineNumber(String[] lines, String searchText) {
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains(searchText)) return i + 1;
        }
        return 1;
    }
}
