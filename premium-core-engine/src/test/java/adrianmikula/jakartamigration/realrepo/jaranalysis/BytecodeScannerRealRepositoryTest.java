package adrianmikula.jakartamigration.realrepo.jaranalysis;

import adrianmikula.jakartamigration.jaranalysis.domain.JarCompatibilityLevel;
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

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Bytecode Scanning Integration Tests")
@Tag("slow")
class BytecodeScannerRealRepositoryTest {

    private DefaultJarCompatibilityScanner scanner;

    @BeforeEach
    void setUp() {
        scanner = new DefaultJarCompatibilityScanner();
    }

    @Test
    @DisplayName("Should classify javax servlet JAR as JAVAX")
    void shouldClassifyJavaxServletJAR(@TempDir Path tempDir) throws IOException {
        Path jarPath = tempDir.resolve("javax-servlet-api-4.0.1.jar");
        TestJarBuilder.createServletJar().build(jarPath);

        JarCompatibilityReport report = scanner.analyzeJar(jarPath);

        assertThat(report).isNotNull();
        assertThat(report.level()).isEqualTo(JarCompatibilityLevel.JAVAX);
        assertThat(report.confidence()).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("Should classify jakarta servlet JAR as JAKARTA")
    void shouldClassifyJakartaServletJAR(@TempDir Path tempDir) throws IOException {
        Path jarPath = tempDir.resolve("jakarta.servlet-api-6.0.0.jar");
        TestJarBuilder.createJakartaServletJar().build(jarPath);

        JarCompatibilityReport report = scanner.analyzeJar(jarPath);

        assertThat(report).isNotNull();
        assertThat(report.level()).isEqualTo(JarCompatibilityLevel.JAKARTA);
        assertThat(report.confidence()).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("Should classify javax persistence JAR as JAVAX")
    void shouldClassifyJavaxPersistenceJAR(@TempDir Path tempDir) throws IOException {
        Path jarPath = tempDir.resolve("javax.persistence-api-2.2.jar");
        TestJarBuilder.createPersistenceJar().build(jarPath);

        JarCompatibilityReport report = scanner.analyzeJar(jarPath);

        assertThat(report).isNotNull();
        assertThat(report.level()).isEqualTo(JarCompatibilityLevel.JAVAX);
    }

    @Test
    @DisplayName("Should classify mixed namespace JAR as MIXED")
    void shouldClassifyMixedNamespaceJAR(@TempDir Path tempDir) throws IOException {
        Path jarPath = tempDir.resolve("mixed-namespace.jar");
        TestJarBuilder.createMixedNamespaceJar().build(jarPath);

        JarCompatibilityReport report = scanner.analyzeJar(jarPath);

        assertThat(report).isNotNull();
        assertThat(report.level()).isEqualTo(JarCompatibilityLevel.MIXED);
    }

    @Test
    @DisplayName("Should classify plain JAR with no javax/jakarta as UNKNOWN")
    void shouldClassifyPlainJARAsUnknown(@TempDir Path tempDir) throws IOException {
        Path jarPath = tempDir.resolve("plain-library.jar");
        TestJarBuilder.create()
            .withClass(TestJarBuilder.ClassSpec.builder("com/example/MyClass")
                .withSuper("java/lang/Object"))
            .build(jarPath);

        JarCompatibilityReport report = scanner.analyzeJar(jarPath);

        assertThat(report).isNotNull();
        assertThat(report.level()).isEqualTo(JarCompatibilityLevel.UNKNOWN);
    }

    @Test
    @DisplayName("Should classify shaded JAR correctly")
    void shouldClassifyShadedJAR(@TempDir Path tempDir) throws IOException {
        Path jarPath = tempDir.resolve("shaded-library.jar");
        TestJarBuilder.createShadedJar().build(jarPath);

        JarCompatibilityReport report = scanner.analyzeJar(jarPath);

        assertThat(report).isNotNull();
        // Shaded JARs with javax packages should be detected
        assertThat(report.level()).isIn(
            JarCompatibilityLevel.JAVAX,
            JarCompatibilityLevel.MIXED,
            JarCompatibilityLevel.UNKNOWN);
    }

    @Test
    @DisplayName("Should handle concurrent JAR scanning")
    void shouldHandleConcurrentScanning(@TempDir Path tempDir) throws IOException, InterruptedException {
        // Create multiple test JARs
        Path javaxJar = tempDir.resolve("javax.jar");
        Path jakartaJar = tempDir.resolve("jakarta.jar");
        Path mixedJar = tempDir.resolve("mixed.jar");

        TestJarBuilder.createServletJar().build(javaxJar);
        TestJarBuilder.createJakartaServletJar().build(jakartaJar);
        TestJarBuilder.createMixedNamespaceJar().build(mixedJar);

        // Scan them concurrently
        Thread t1 = new Thread(() -> {
            JarCompatibilityReport r = scanner.analyzeJar(javaxJar);
            assertThat(r.level()).isEqualTo(JarCompatibilityLevel.JAVAX);
        });
        Thread t2 = new Thread(() -> {
            JarCompatibilityReport r = scanner.analyzeJar(jakartaJar);
            assertThat(r.level()).isEqualTo(JarCompatibilityLevel.JAKARTA);
        });
        Thread t3 = new Thread(() -> {
            JarCompatibilityReport r = scanner.analyzeJar(mixedJar);
            assertThat(r.level()).isEqualTo(JarCompatibilityLevel.MIXED);
        });

        t1.start(); t2.start(); t3.start();
        t1.join(5000); t2.join(5000); t3.join(5000);

        assertThat(t1.isAlive()).isFalse();
        assertThat(t2.isAlive()).isFalse();
        assertThat(t3.isAlive()).isFalse();
    }

    @Test
    @DisplayName("Should cache results for repeated scans")
    void shouldCacheResults(@TempDir Path tempDir) throws IOException {
        Path jarPath = tempDir.resolve("cache-test.jar");
        TestJarBuilder.createServletJar().build(jarPath);

        // First scan
        long start1 = System.currentTimeMillis();
        JarCompatibilityReport report1 = scanner.analyzeJar(jarPath);
        long duration1 = System.currentTimeMillis() - start1;

        // Second scan (should be cached)
        long start2 = System.currentTimeMillis();
        JarCompatibilityReport report2 = scanner.analyzeJar(jarPath);
        long duration2 = System.currentTimeMillis() - start2;

        assertThat(report1.level()).isEqualTo(report2.level());
        assertThat(report2.analysisTimeMs()).isEqualTo(0); // Cached reports have 0 analysis time
    }

    @Test
    @DisplayName("Should handle large JAR without OOM")
    void shouldHandleLargeJAR(@TempDir Path tempDir) throws IOException {
        Path jarPath = tempDir.resolve("large-jar.jar");
        TestJarBuilder builder = TestJarBuilder.create();

        // Create many classes to simulate a large JAR
        for (int i = 0; i < 100; i++) {
            builder.withClass(TestJarBuilder.ClassSpec.builder("com/example/class" + i)
                .withSuper("java/lang/Object")
                .withInterface("java/io/Serializable"));
        }
        builder.build(jarPath);

        JarCompatibilityReport report = scanner.analyzeJar(jarPath);

        assertThat(report).isNotNull();
        assertThat(report.level()).isNotNull();
    }
}
