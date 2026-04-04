package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.ReflectionUsage;
import adrianmikula.jakartamigration.advancedscanning.domain.ReflectionUsageProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.ReflectionUsageScanResult;
import adrianmikula.jakartamigration.advancedscanning.service.ReflectionUsageScanner;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import adrianmikula.jakartamigration.util.ProjectFileSystemScanner;

/**
 * Scanner implementation for detecting reflection usage of javax packages.
 * Helps identify code that uses reflection to access javax.* classes which may cause
 * compatibility issues during Jakarta EE migration.
 */
@Slf4j
public class ReflectionUsageScannerImpl implements ReflectionUsageScanner {

    private final ProjectFileSystemScanner fileScanner = new ProjectFileSystemScanner();

    // Patterns for reflection API usage
    private static final Pattern CLASS_FOR_NAME_PATTERN = Pattern.compile(
            "Class\\.forName\\s*\\(\\s*\"([^\"]+)\"\\s*\\)");

    private static final Pattern CLASS_LOADER_PATTERN = Pattern.compile(
            "(?:ClassLoader|URLClassLoader|Thread\\.currentThread\\(\\)\\.getContextClassLoader\\(\\))\\.loadClass\\s*\\(\\s*\"([^\"]+)\"\\s*\\)");

    private static final Pattern METHOD_INVOKE_PATTERN = Pattern.compile(
            "Method\\.invoke\\s*\\(");

    private static final Pattern FIELD_ACCESS_PATTERN = Pattern.compile(
            "Field\\.(?:get|set)\\s*\\(");

    private static final Pattern CONSTRUCTOR_PATTERN = Pattern.compile(
            "Constructor\\.newInstance\\s*\\(");

    private static final Pattern PROXY_PATTERN = Pattern.compile(
            "Proxy\\.(?:newProxyInstance|getInvocationHandler)\\s*\\(");

    private static final Pattern ANNOTATION_PATTERN = Pattern.compile(
            "(?:getAnnotation|getAnnotations|isAnnotationPresent)\\s*\\(");

    // Pattern to detect javax class references in reflection calls
    private static final Pattern JAVAX_CLASS_PATTERN = Pattern.compile(
            "javax\\.[a-zA-Z0-9.]*");

    // File extensions to scan
    private static final Set<String> SCAN_EXTENSIONS = Set.of(".java", ".kt", ".scala");

    // Deduplication tracking
    private final Set<String> processedLines = new HashSet<>();

    @Override
    public ReflectionUsageProjectScanResult scanProject(Path projectPath) {
        log.info("Starting reflection usage scan for project: {}", projectPath);

        List<ReflectionUsageScanResult> fileResults = new ArrayList<>();

        try {
            List<Path> filesToScan = fileScanner.findFiles(projectPath, List.of(".java", ".kt", ".scala"));

            log.info("Found {} files to scan for reflection usage", filesToScan.size());

            for (Path filePath : filesToScan) {
                try {
                    ReflectionUsageScanResult result = scanFile(filePath);
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

        log.info("Reflection usage scan complete. Found {} files with issues", fileResults.size());

        return new ReflectionUsageProjectScanResult(
                projectPath.toString(),
                fileResults);
    }

    private ReflectionUsageScanResult scanFile(Path filePath) {
        List<ReflectionUsage> usages = new ArrayList<>();
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

                // Check for Class.forName usage
                Matcher classForNameMatcher = CLASS_FOR_NAME_PATTERN.matcher(line);
                if (classForNameMatcher.find()) {
                    String targetClass = classForNameMatcher.group(1);
                    if (JAVAX_CLASS_PATTERN.matcher(targetClass).find()) {
                        usages.add(new ReflectionUsage(
                                filePath.toString(),
                                lineNumber,
                                "Class.forName",
                                extractClassName(line),
                                extractMethodName(line),
                                targetClass));
                    }
                }

                // Check for ClassLoader usage
                Matcher classLoaderMatcher = CLASS_LOADER_PATTERN.matcher(line);
                if (classLoaderMatcher.find()) {
                    String targetClass = classLoaderMatcher.group(1);
                    if (JAVAX_CLASS_PATTERN.matcher(targetClass).find()) {
                        usages.add(new ReflectionUsage(
                                filePath.toString(),
                                lineNumber,
                                "ClassLoader",
                                extractClassName(line),
                                extractMethodName(line),
                                targetClass));
                    }
                }

                // Check for Method.invoke usage
                Matcher methodInvokeMatcher = METHOD_INVOKE_PATTERN.matcher(line);
                if (methodInvokeMatcher.find() && containsJavaxReference(line)) {
                    usages.add(new ReflectionUsage(
                            filePath.toString(),
                            lineNumber,
                            "Method.invoke",
                            extractClassName(line),
                            extractMethodName(line),
                            extractJavaxReference(line)));
                }

                // Check for Field access usage
                Matcher fieldAccessMatcher = FIELD_ACCESS_PATTERN.matcher(line);
                if (fieldAccessMatcher.find() && containsJavaxReference(line)) {
                    usages.add(new ReflectionUsage(
                            filePath.toString(),
                            lineNumber,
                            "Field.set",
                            extractClassName(line),
                            extractMethodName(line),
                            extractJavaxReference(line)));
                }

                // Check for Constructor.newInstance usage
                Matcher constructorMatcher = CONSTRUCTOR_PATTERN.matcher(line);
                if (constructorMatcher.find() && containsJavaxReference(line)) {
                    usages.add(new ReflectionUsage(
                            filePath.toString(),
                            lineNumber,
                            "Constructor.newInstance",
                            extractClassName(line),
                            extractMethodName(line),
                            extractJavaxReference(line)));
                }

                // Check for Proxy usage
                Matcher proxyMatcher = PROXY_PATTERN.matcher(line);
                if (proxyMatcher.find() && containsJavaxReference(line)) {
                    usages.add(new ReflectionUsage(
                            filePath.toString(),
                            lineNumber,
                            "Proxy",
                            extractClassName(line),
                            extractMethodName(line),
                            extractJavaxReference(line)));
                }

                // Check for Annotation reflection
                Matcher annotationMatcher = ANNOTATION_PATTERN.matcher(line);
                if (annotationMatcher.find() && containsJavaxReference(line)) {
                    usages.add(new ReflectionUsage(
                            filePath.toString(),
                            lineNumber,
                            "Annotation",
                            extractClassName(line),
                            extractMethodName(line),
                            extractJavaxReference(line)));
                }
            }

        } catch (IOException e) {
            log.warn("Error reading file {}: {}", filePath, e.getMessage());
        }

        return new ReflectionUsageScanResult(filePath.toString(), usages);
    }

    private boolean containsJavaxReference(String line) {
        return JAVAX_CLASS_PATTERN.matcher(line).find();
    }

    private String extractJavaxReference(String line) {
        Matcher matcher = JAVAX_CLASS_PATTERN.matcher(line);
        if (matcher.find()) {
            return matcher.group();
        }
        return "Unknown";
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
