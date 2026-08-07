package adrianmikula.jakartamigration.jaranalysis;

import adrianmikula.jakartamigration.jaranalysis.domain.JarCompatibilityReport;
import adrianmikula.jakartamigration.jaranalysis.service.DefaultJarCompatibilityScanner;
import adrianmikula.jakartamigration.testutil.TestJarBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DefaultJarCompatibilityScanner Performance Tests")
@Tag("slow")
class DefaultJarCompatibilityScannerPerformanceTest {

    private DefaultJarCompatibilityScanner scanner;

    @BeforeEach
    void setUp() {
        scanner = new DefaultJarCompatibilityScanner();
    }

    @Test
    @DisplayName("Should cache results and improve performance")
    void shouldCacheResultsAndImprovePerformance(@TempDir Path tempDir) throws IOException {
        Path jarPath = tempDir.resolve("cache-perf.jar");
        TestJarBuilder.createServletJar().build(jarPath);

        // First scan (cold)
        long start1 = System.currentTimeMillis();
        JarCompatibilityReport report1 = scanner.analyzeJar(jarPath);
        long duration1 = System.currentTimeMillis() - start1;

        // Second scan (warm, should be cached)
        long start2 = System.currentTimeMillis();
        JarCompatibilityReport report2 = scanner.analyzeJar(jarPath);
        long duration2 = System.currentTimeMillis() - start2;

        assertThat(report1.level()).isEqualTo(report2.level());
        // Cached scan should be much faster (or instant)
        assertThat(duration2).isLessThanOrEqualTo(duration1);
    }

    @Test
    @DisplayName("Should handle parallel scanning of 10 JARs within 60s")
    void shouldHandleParallelScanning(@TempDir Path tempDir) throws IOException {
        List<Path> jarPaths = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Path jarPath = tempDir.resolve("parallel-" + i + ".jar");
            TestJarBuilder builder = TestJarBuilder.create();
            for (int j = 0; j < 200; j++) {
                builder.withClass(TestJarBuilder.ClassSpec.builder("com/example/class" + j)
                    .withSuper("java/lang/Object"));
            }
            builder.build(jarPath);
            jarPaths.add(jarPath);
        }

        long start = System.currentTimeMillis();
        List<JarCompatibilityReport> reports = scanner.analyzeJars(jarPaths, null);
        long duration = System.currentTimeMillis() - start;

        assertThat(reports).hasSize(10);
        for (JarCompatibilityReport report : reports) {
            assertThat(report).isNotNull();
        }
        assertThat(duration).isLessThan(60000); // 60s budget for 10 JARs
    }

    @Test
    @DisplayName("Should not exceed 150MB heap for 10 JARs")
    void shouldNotExceedMemoryBudget(@TempDir Path tempDir) throws IOException {
        Runtime runtime = Runtime.getRuntime();
        List<Path> jarPaths = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Path jarPath = tempDir.resolve("mem-" + i + ".jar");
            TestJarBuilder builder = TestJarBuilder.create();
            for (int j = 0; j < 100; j++) {
                builder.withClass(TestJarBuilder.ClassSpec.builder("com/example/class" + j)
                    .withSuper("java/lang/Object")
                    .withInterface("java/io/Serializable"));
            }
            builder.build(jarPath);
            jarPaths.add(jarPath);
        }

        runtime.gc();
        long heapBefore = runtime.totalMemory() - runtime.freeMemory();

        scanner.analyzeJars(jarPaths, null);

        runtime.gc();
        long heapAfter = runtime.totalMemory() - runtime.freeMemory();
        long heapUsed = heapAfter - heapBefore;

        // Budget: 150MB for 10 JARs
        assertThat(heapUsed).isLessThan(150 * 1024 * 1024);
    }
}
