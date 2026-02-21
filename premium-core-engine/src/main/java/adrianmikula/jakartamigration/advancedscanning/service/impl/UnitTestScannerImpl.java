package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.UnitTestProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.UnitTestUsage;
import adrianmikula.jakartamigration.advancedscanning.service.UnitTestScanner;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class UnitTestScannerImpl implements UnitTestScanner {

    private static final Pattern JAVAX_PATTERN = Pattern.compile("import\\s+javax\\.([^;]+);");
    private static final Set<String> TEST_DIRS = Set.of("test", "src/test", "tests", "src/tests");
    private static final Set<String> TEST_EXTENSIONS = Set.of(".java");

    @Override
    public UnitTestProjectScanResult scanProject(Path projectPath) {
        log.info("Starting unit test scan for project: {}", projectPath);
        List<UnitTestUsage> usages = new ArrayList<>();
        
        try (Stream<Path> paths = Files.walk(projectPath)) {
            List<Path> testFiles = paths
                .filter(Files::isRegularFile)
                .filter(p -> isTestFile(p, projectPath))
                .collect(Collectors.toList());
            
            log.info("Found {} test files to scan", testFiles.size());
            
            for (Path filePath : testFiles) {
                try {
                    String content = Files.readString(filePath);
                    String[] lines = content.split("\\r?\\n");
                    
                    for (int i = 0; i < lines.length; i++) {
                        Matcher matcher = JAVAX_PATTERN.matcher(lines[i]);
                        if (matcher.find()) {
                            String packageName = matcher.group(1);
                            usages.add(new UnitTestUsage(
                                filePath.toString(),
                                i + 1,
                                "javax." + packageName,
                                detectTestFramework(content)
                            ));
                        }
                    }
                } catch (IOException e) {
                    log.warn("Error reading test file: {}", e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("Error scanning test files: {}", e.getMessage());
        }
        
        return new UnitTestProjectScanResult(projectPath.toString(), usages, 0);
    }

    private boolean isTestFile(Path path, Path projectPath) {
        String relativePath = projectPath.relativize(path).toString();
        return TEST_DIRS.stream().anyMatch(d -> relativePath.startsWith(d))
            && TEST_EXTENSIONS.stream().anyMatch(path.toString()::endsWith);
    }

    private String detectTestFramework(String content) {
        if (content.contains("import org.junit.")) return "JUnit";
        if (content.contains("import org.testng.")) return "TestNG";
        if (content.contains("import io.quarkus.")) return "Quarkus Test";
        if (content.contains("import org.springframework.boot.test.")) return "Spring Boot Test";
        return "Unknown";
    }
}
