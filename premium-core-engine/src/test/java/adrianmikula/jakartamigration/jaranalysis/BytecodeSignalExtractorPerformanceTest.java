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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BytecodeSignalExtractor Performance Tests")
@Tag("slow")
class BytecodeSignalExtractorPerformanceTest {

    private DefaultJarCompatibilityScanner scanner;

    @BeforeEach
    void setUp() {
        scanner = new DefaultJarCompatibilityScanner();
    }

    @Test
    @DisplayName("Should scan JAR within 5s time budget for 10MB JAR")
    void shouldScanJARWithinTimeBudget(@TempDir Path tempDir) throws IOException {
        Path jarPath = tempDir.resolve("medium.jar");
        TestJarBuilder builder = TestJarBuilder.create();
        for (int i = 0; i < 500; i++) {
            builder.withClass(TestJarBuilder.ClassSpec.builder("com/example/class" + i)
                .withSuper("java/lang/Object")
                .withInterface("java/io/Serializable")
                .withField(TestJarBuilder.FieldSpec.of(
                    org.objectweb.asm.Opcodes.ACC_PRIVATE,
                    "field" + i, "Ljava/lang/String;")));
        }
        builder.build(jarPath);

        long start = System.currentTimeMillis();
        JarCompatibilityReport report = scanner.analyzeJar(jarPath);
        long duration = System.currentTimeMillis() - start;

        assertThat(report).isNotNull();
        assertThat(duration).isLessThan(5000); // 5s budget
    }

    @Test
    @DisplayName("Should handle concurrent scanning of 4 JARs within 30s")
    void shouldHandleConcurrentScanning(@TempDir Path tempDir) throws IOException, InterruptedException {
        List<Path> jarPaths = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Path jarPath = tempDir.resolve("concurrent-" + i + ".jar");
            TestJarBuilder builder = TestJarBuilder.create();
            for (int j = 0; j < 100; j++) {
                builder.withClass(TestJarBuilder.ClassSpec.builder("com/example/class" + j)
                    .withSuper("java/lang/Object"));
            }
            builder.build(jarPath);
            jarPaths.add(jarPath);
        }

        CountDownLatch latch = new CountDownLatch(4);
        long start = System.currentTimeMillis();

        for (Path jarPath : jarPaths) {
            new Thread(() -> {
                try {
                    JarCompatibilityReport report = scanner.analyzeJar(jarPath);
                    assertThat(report).isNotNull();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await(30, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - start;

        assertThat(duration).isLessThan(30000); // 30s budget for 4 concurrent JARs
    }

    @Test
    @DisplayName("Should classify mixed namespace JAR within performance budget")
    void shouldClassifyMixedNamespaceJARWithinBudget(@TempDir Path tempDir) throws IOException {
        Path jarPath = tempDir.resolve("mixed-perf.jar");
        TestJarBuilder builder = TestJarBuilder.create();

        // Add 50 javax classes
        for (int i = 0; i < 50; i++) {
            builder.withClass(TestJarBuilder.ClassSpec.builder("javax/test/Class" + i)
                .withSuper("java/lang/Object"));
        }
        // Add 50 jakarta classes
        for (int i = 0; i < 50; i++) {
            builder.withClass(TestJarBuilder.ClassSpec.builder("jakarta/test/Class" + i)
                .withSuper("java/lang/Object"));
        }
        builder.build(jarPath);

        long start = System.currentTimeMillis();
        JarCompatibilityReport report = scanner.analyzeJar(jarPath);
        long duration = System.currentTimeMillis() - start;

        assertThat(report).isNotNull();
        assertThat(duration).isLessThan(5000);
    }
}
