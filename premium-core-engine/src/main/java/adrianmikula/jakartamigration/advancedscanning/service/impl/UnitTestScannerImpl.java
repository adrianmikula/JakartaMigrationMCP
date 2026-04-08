package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.UnitTestProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.UnitTestUsage;
import adrianmikula.jakartamigration.advancedscanning.service.UnitTestScanner;
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

@Slf4j
public class UnitTestScannerImpl implements UnitTestScanner {

    private final ProjectFileSystemScanner fileScanner = new ProjectFileSystemScanner();

    private static final Pattern JAVAX_PATTERN = Pattern.compile("import\\s+javax\\.([^;]+);");
    private static final Set<String> TEST_DIRS = Set.of("test", "src/test", "tests", "src/tests");
    private static final Set<String> TEST_EXTENSIONS = Set.of(".java");

    // Maximum file size to scan (5MB) - larger files are skipped
    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;

    @Override
    public UnitTestProjectScanResult scanProject(Path projectPath) {
        log.info("Starting unit test scan for project: {}", projectPath);
        List<UnitTestUsage> usages = new ArrayList<>();

        try {
            List<Path> testFiles = fileScanner.findFiles(projectPath, p -> isTestFile(p, projectPath));

            log.info("Found {} test files to scan", testFiles.size());

            for (Path filePath : testFiles) {
                try {
                    // Skip large files to prevent memory issues
                    long fileSize = Files.size(filePath);
                    if (fileSize > MAX_FILE_SIZE_BYTES) {
                        log.warn("Skipping large test file ({} bytes): {}", fileSize, filePath);
                        continue;
                    }

                    // Collect content for framework detection (limited) and scan lines
                    StringBuilder contentBuilder = new StringBuilder();

                    // Use streaming with try-with-resources for memory efficiency
                    try (Stream<String> lines = Files.lines(filePath)) {
                        final AtomicInteger lineNumber = new AtomicInteger(0);
                        lines.forEach(line -> {
                            // Collect limited content for framework detection
                            if (contentBuilder.length() < 5000) {
                                contentBuilder.append(line).append("\n");
                            }

                            int currentLineNumber = lineNumber.incrementAndGet();
                            Matcher matcher = JAVAX_PATTERN.matcher(line);
                            if (matcher.find()) {
                                String packageName = matcher.group(1);
                                usages.add(new UnitTestUsage(
                                        filePath.toString(),
                                        currentLineNumber,
                                        "javax." + packageName,
                                        detectTestFramework(contentBuilder.toString())));
                            }
                        });
                    }
                } catch (IOException e) {
                    log.warn("Error reading test file: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error scanning test files: {}", e.getMessage());
        }

        return new UnitTestProjectScanResult(projectPath.toString(), usages, 0);
    }

    private boolean isTestFile(Path path, Path projectPath) {
        String relativePath = projectPath.relativize(path).toString().replace("\\", "/");
        return TEST_DIRS.stream().anyMatch(d -> relativePath.startsWith(d))
                && TEST_EXTENSIONS.stream().anyMatch(path.toString()::endsWith);
    }

    private String detectTestFramework(String content) {
        if (content.contains("import org.junit."))
            return "JUnit";
        if (content.contains("import org.testng."))
            return "TestNG";
        if (content.contains("import io.quarkus."))
            return "Quarkus Test";
        if (content.contains("import org.springframework.boot.test."))
            return "Spring Boot Test";
        return "Unknown";
    }
}
