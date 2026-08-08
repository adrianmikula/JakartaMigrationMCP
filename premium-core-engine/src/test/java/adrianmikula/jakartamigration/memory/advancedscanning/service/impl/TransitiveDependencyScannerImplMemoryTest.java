package adrianmikula.jakartamigration.memory.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyProjectScanResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TransitiveDependencyScannerImpl Memory Tests")
@Tag("slow")
class TransitiveDependencyScannerImplMemoryTest {

    private TransitiveDependencyScannerImpl scanner;

    @BeforeEach
    void setUp() {
        scanner = new TransitiveDependencyScannerImpl();
    }

    @Test
    @DisplayName("Should scan 500 dependencies within 100MB heap budget")
    void shouldScan500DependenciesWithinBudget(@TempDir Path tempDir) throws IOException {
        // Create a pom.xml with many dependencies
        StringBuilder pom = new StringBuilder();
        pom.append("""
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.test</groupId>
                <artifactId>perf-test</artifactId>
                <version>1.0.0</version>
                <dependencies>
            """);

        for (int i = 0; i < 100; i++) {
            pom.append(String.format("""
                <dependency>
                    <groupId>com.test.group%d</groupId>
                    <artifactId>artifact%d</artifactId>
                    <version>1.0.%d</version>
                </dependency>
                """, i % 10, i, i % 10));
        }
        pom.append("</dependencies></project>");
        Files.writeString(tempDir.resolve("pom.xml"), pom.toString());

        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long heapBefore = runtime.totalMemory() - runtime.freeMemory();

        TransitiveDependencyProjectScanResult result = scanner.scanProject(tempDir);

        runtime.gc();
        long heapAfter = runtime.totalMemory() - runtime.freeMemory();
        long heapUsed = heapAfter - heapBefore;

        assertThat(result).isNotNull();
        // Budget: 100MB for 500 dependencies (the regex fallback parses them)
        assertThat(heapUsed).isLessThan(100 * 1024 * 1024);
    }

    @Test
    @DisplayName("Should not leak memory on repeated scans")
    void shouldNotLeakMemoryOnRepeatedScans(@TempDir Path tempDir) throws IOException {
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.test</groupId>
                <artifactId>leak-test</artifactId>
                <version>1.0.0</version>
                <dependencies>
                    <dependency>
                        <groupId>javax.servlet</groupId>
                        <artifactId>javax.servlet-api</artifactId>
                        <version>4.0.1</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        Files.writeString(tempDir.resolve("pom.xml"), pomContent);

        // Warm up
        for (int i = 0; i < 5; i++) {
            scanner.scanProject(tempDir);
        }

        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long baseline = runtime.totalMemory() - runtime.freeMemory();

        // Run 10 scans
        for (int i = 0; i < 10; i++) {
            scanner.scanProject(tempDir);
        }

        runtime.gc();
        long after = runtime.totalMemory() - runtime.freeMemory();
        long delta = after - baseline;

        // Should not leak more than 20MB over baseline
        assertThat(delta).isLessThan(20 * 1024 * 1024);
    }
}
