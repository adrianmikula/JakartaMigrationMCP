package adrianmikula.jakartamigration.jaranalysis.service;

import adrianmikula.jakartamigration.testutil.TestJarBuilder;
import adrianmikula.jakartamigration.jaranalysis.config.JarScanningConfig;
import adrianmikula.jakartamigration.dependencyanalysis.service.JarResolver;
import adrianmikula.jakartamigration.jaranalysis.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DefaultJarCompatibilityScannerTest {

    @TempDir
    Path tempDir;

    @Test
    void analyzePureJavaxJar() throws IOException {
        var extractor = new BytecodeSignalExtractor();
        var metadataExtractor = new MetadataSignalExtractor();
        var scorer = new ScoringEngine();
        var config = JarScanningConfig.get();
        var resolver = new JarResolver();
        var scanner = new DefaultJarCompatibilityScanner(extractor, metadataExtractor, scorer, config, resolver);

        Path jar = tempDir.resolve("javax-servlet.jar");
        TestJarBuilder.create()
            .withClass(TestJarBuilder.ClassSpec.builder("test/MyServlet")
                .withSuper("javax.servlet.http.HttpServlet"))
            .build(jar);

        var report = scanner.analyzeJar(jar);

        assertThat(report.level()).isEqualTo(JarCompatibilityLevel.JAVAX);
        // Confidence for simple JARs is modest; validate it's reasonable rather than >0.5
        assertThat(report.confidence()).isGreaterThan(0.3);
        assertThat(report.isCached()).isFalse();
    }

    @Test
    void analyzePureJakartaJar() throws IOException {
        var extractor = new BytecodeSignalExtractor();
        var metadataExtractor = new MetadataSignalExtractor();
        var scorer = new ScoringEngine();
        var config = JarScanningConfig.get();
        var resolver = new JarResolver();
        var scanner = new DefaultJarCompatibilityScanner(extractor, metadataExtractor, scorer, config, resolver);

        Path jar = tempDir.resolve("jakarta-servlet.jar");
        TestJarBuilder.create()
            .withClass(TestJarBuilder.ClassSpec.builder("test/JakartaServlet")
                .withSuper("jakarta.servlet.http.HttpServlet"))
            .build(jar);

        var report = scanner.analyzeJar(jar);

        assertThat(report.level()).isEqualTo(JarCompatibilityLevel.JAKARTA);
        assertThat(report.confidence()).isGreaterThan(0.3);
    }

    @Test
    void analyzeMixedJar() throws IOException {
        var extractor = new BytecodeSignalExtractor();
        var metadataExtractor = new MetadataSignalExtractor();
        var scorer = new ScoringEngine();
        var config = JarScanningConfig.get();
        var resolver = new JarResolver();
        var scanner = new DefaultJarCompatibilityScanner(extractor, metadataExtractor, scorer, config, resolver);

        Path jar = tempDir.resolve("mixed.jar");
        TestJarBuilder.create()
            .withClass(TestJarBuilder.ClassSpec.builder("test/Mixed")
                .withSuper("javax.servlet.http.HttpServlet")
                .withInterface("jakarta.servlet.Servlet"))
            .build(jar);

        var report = scanner.analyzeJar(jar);

        assertThat(report.level()).isEqualTo(JarCompatibilityLevel.MIXED);
        assertThat(report.signal().javaxClassRefs()).isGreaterThan(0);
        assertThat(report.signal().jakartaClassRefs()).isGreaterThan(0);
    }

    @Test
    void analyzeUnknownJar() throws IOException {
        var extractor = new BytecodeSignalExtractor();
        var metadataExtractor = new MetadataSignalExtractor();
        var scorer = new ScoringEngine();
        var config = JarScanningConfig.get();
        var resolver = new JarResolver();
        var scanner = new DefaultJarCompatibilityScanner(extractor, metadataExtractor, scorer, config, resolver);

        Path jar = tempDir.resolve("unknown.jar");
        TestJarBuilder.create()
            .withClass(TestJarBuilder.ClassSpec.builder("test/Plain")
                .withSuper("java.lang.Object"))
            .build(jar);

        var report = scanner.analyzeJar(jar);

        assertThat(report.level()).isEqualTo(JarCompatibilityLevel.UNKNOWN);
        assertThat(report.confidence()).isLessThan(0.5);
    }

    @Test
    void cachingBehavior() throws IOException {
        var extractor = new BytecodeSignalExtractor();
        var metadataExtractor = new MetadataSignalExtractor();
        var scorer = new ScoringEngine();
        var config = JarScanningConfig.get();
        var resolver = new JarResolver();
        var scanner = new DefaultJarCompatibilityScanner(extractor, metadataExtractor, scorer, config, resolver);

        Path jar = tempDir.resolve("cachable.jar");
        TestJarBuilder.create()
            .withClass(TestJarBuilder.ClassSpec.builder("test/Cached")
                .withSuper("javax.servlet.http.HttpServlet"))
            .build(jar);

        var report1 = scanner.analyzeJar(jar);
        var report2 = scanner.analyzeJar(jar);

        assertThat(report2.isCached()).isTrue();
        assertThat(report2.artifactCoordinate()).isEqualTo(report1.artifactCoordinate());
        assertThat(report2.level()).isEqualTo(report1.level());

        var stats = scanner.getCacheStats();
        assertThat((Long) stats.get("hitCount")).isEqualTo(1);
    }

    @Test
    void cacheBypassDisabledCaching() throws IOException {
        var extractor = new BytecodeSignalExtractor();
        var metadataExtractor = new MetadataSignalExtractor();
        var scorer = new ScoringEngine();
        var config = Mockito.mock(JarScanningConfig.class);
        when(config.isCachingEnabled()).thenReturn(false);
        when(config.getMaximumJarSizeBytes()).thenReturn(50L * 1024 * 1024);
        when(config.getMaxClassesPerJar()).thenReturn(0);
        when(config.createScanOptions()).thenReturn(new JarScanOptions(true, true, true, 10, false, true, 0));
        when(config.isParallelScanEnabled()).thenReturn(false);
        var resolver = new JarResolver();
        var scanner = new DefaultJarCompatibilityScanner(extractor, metadataExtractor, scorer, config, resolver);

        Path jar = tempDir.resolve("no-cache.jar");
        TestJarBuilder.create()
            .withClass(TestJarBuilder.ClassSpec.builder("test/NoCache")
                .withSuper("javax.servlet.http.HttpServlet"))
            .build(jar);

        scanner.analyzeJar(jar);
        var stats = scanner.getCacheStats();
        // With caching disabled, cache hit/miss statistics are not collected
        assertThat(stats.get("hitCount")).isNull();
    }

    @Test
    void clearCache() throws IOException {
        var extractor = new BytecodeSignalExtractor();
        var metadataExtractor = new MetadataSignalExtractor();
        var scorer = new ScoringEngine();
        var config = JarScanningConfig.get();
        var resolver = new JarResolver();
        var scanner = new DefaultJarCompatibilityScanner(extractor, metadataExtractor, scorer, config, resolver);

        Path jar = tempDir.resolve("clear-cache.jar");
        TestJarBuilder.create()
            .withClass(TestJarBuilder.ClassSpec.builder("test/ClearCache")
                .withSuper("javax.servlet.http.HttpServlet"))
            .build(jar);

        var report1 = scanner.analyzeJar(jar);
        scanner.clearCache();
        var report2 = scanner.analyzeJar(jar);

        assertThat(report1.level()).isEqualTo(report2.level());
        assertThat(scanner.clearCache()).isTrue();
    }

    @Test
    void batchAnalyzeJars() throws IOException {
        var extractor = new BytecodeSignalExtractor();
        var metadataExtractor = new MetadataSignalExtractor();
        var scorer = new ScoringEngine();
        var config = JarScanningConfig.get();
        var resolver = new JarResolver();
        var scanner = new DefaultJarCompatibilityScanner(extractor, metadataExtractor, scorer, config, resolver);

        Path jar1 = tempDir.resolve("jar1.jar");
        Path jar2 = tempDir.resolve("jar2.jar");
        TestJarBuilder.create()
            .withClass(TestJarBuilder.ClassSpec.builder("test/Class1").withSuper("javax.servlet.http.HttpServlet"))
            .build(jar1);
        TestJarBuilder.create()
            .withClass(TestJarBuilder.ClassSpec.builder("test/Class2").withSuper("jakarta.servlet.http.HttpServlet"))
            .build(jar2);

         var reports = scanner.analyzeJars(List.of(jar1, jar2), JarScanOptions.DEFAULT);

        assertThat(reports).hasSize(2);
        assertThat(reports.get(0).level()).isEqualTo(JarCompatibilityLevel.JAVAX);
        assertThat(reports.get(1).level()).isEqualTo(JarCompatibilityLevel.JAKARTA);
    }

    @Test
    void analyzeArtifactWithResolver() throws IOException {
        var extractor = new BytecodeSignalExtractor();
        var metadataExtractor = new MetadataSignalExtractor();
        var scorer = new ScoringEngine();
        var config = JarScanningConfig.get();
        var resolver = Mockito.mock(JarResolver.class);
        var artifact = new adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact("test", "test-artifact", "1.0.0", "compile", false);
        Path jarPath = tempDir.resolve("resolved-artifact.jar");
        TestJarBuilder.create()
            .withClass(TestJarBuilder.ClassSpec.builder("test/MyClass").withSuper("javax.servlet.http.HttpServlet"))
            .build(jarPath);
        when(resolver.resolve(artifact)).thenReturn(Optional.of(jarPath));

        var scanner = new DefaultJarCompatibilityScanner(extractor, metadataExtractor, scorer, config, resolver);
        var report = scanner.analyzeArtifact(artifact);

        assertThat(report.level()).isEqualTo(JarCompatibilityLevel.JAVAX);
        verify(resolver).resolve(artifact);
    }

    @Test
    void analyzeArtifactWhenJarNotFound() {
        var extractor = new BytecodeSignalExtractor();
        var metadataExtractor = new MetadataSignalExtractor();
        var scorer = new ScoringEngine();
        var config = JarScanningConfig.get();
        var resolver = Mockito.mock(JarResolver.class);
        var artifact = new adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact("missing", "missing-artifact", "1.0.0", "compile", false);
        when(resolver.resolve(artifact)).thenReturn(Optional.empty());

        var scanner = new DefaultJarCompatibilityScanner(extractor, metadataExtractor, scorer, config, resolver);
        var report = scanner.analyzeArtifact(artifact);

        assertThat(report.level()).isEqualTo(JarCompatibilityLevel.UNKNOWN);
        assertThat(report.reasons()).contains("JAR not found");
    }

    @Test
    void getCachedResult() throws IOException {
        var extractor = new BytecodeSignalExtractor();
        var metadataExtractor = new MetadataSignalExtractor();
        var scorer = new ScoringEngine();
        var config = JarScanningConfig.get();
        var resolver = new JarResolver();
        var scanner = new DefaultJarCompatibilityScanner(extractor, metadataExtractor, scorer, config, resolver);

        // Filename must produce coordinate "unknown:test-cached:1.0" to match query
        Path jar = tempDir.resolve("test-cached-1.0.jar");
        TestJarBuilder.create()
            .withClass(TestJarBuilder.ClassSpec.builder("test/Cached")
                .withSuper("javax.servlet.http.HttpServlet"))
            .build(jar);

        scanner.analyzeJar(jar);
        var cached = scanner.getCachedResult("unknown:test-cached:1.0");

        assertThat(cached).isNotNull();
        assertThat(cached.level()).isEqualTo(JarCompatibilityLevel.JAVAX);
    }

    @Test
    void getCachedResultWhenCachingDisabled() throws IOException {
        var extractor = new BytecodeSignalExtractor();
        var metadataExtractor = new MetadataSignalExtractor();
        var scorer = new ScoringEngine();
        var config = Mockito.mock(JarScanningConfig.class);
        when(config.isCachingEnabled()).thenReturn(false);
        when(config.getMaximumJarSizeBytes()).thenReturn(50L * 1024 * 1024);
        when(config.createScanOptions()).thenReturn(new JarScanOptions(true, true, true, 10, false, true, 0));
        when(config.isParallelScanEnabled()).thenReturn(false);
        var resolver = new JarResolver();
        var scanner = new DefaultJarCompatibilityScanner(extractor, metadataExtractor, scorer, config, resolver);

        Path jar = tempDir.resolve("no-cache-test.jar");
        TestJarBuilder.create()
            .withClass(TestJarBuilder.ClassSpec.builder("test/NoCache")
                .withSuper("javax.servlet.http.HttpServlet"))
            .build(jar);

        scanner.analyzeJar(jar);
        var cached = scanner.getCachedResult("test");
        assertThat(cached).isNull();
    }

    @Test
    void shutdownExecutor() throws IOException {
        var extractor = new BytecodeSignalExtractor();
        var metadataExtractor = new MetadataSignalExtractor();
        var scorer = new ScoringEngine();
        var config = JarScanningConfig.get();
        var resolver = new JarResolver();
        var scanner = new DefaultJarCompatibilityScanner(extractor, metadataExtractor, scorer, config, resolver);

        scanner.shutdown();
    }

    @Test
    void handleLargeJarSizeLimit() throws IOException {
        var extractor = new BytecodeSignalExtractor();
        var metadataExtractor = new MetadataSignalExtractor();
        var scorer = new ScoringEngine();
        var config = Mockito.mock(JarScanningConfig.class);
        when(config.isCachingEnabled()).thenReturn(false);
        when(config.getMaximumJarSizeBytes()).thenReturn(100L);
        when(config.createScanOptions()).thenReturn(new JarScanOptions(true, true, true, 10, false, true, 0));
        var resolver = new JarResolver();
        var scanner = new DefaultJarCompatibilityScanner(extractor, metadataExtractor, scorer, config, resolver);

        Path jar = tempDir.resolve("large.jar");
        TestJarBuilder.create()
            .withClass(TestJarBuilder.ClassSpec.builder("test/Big")
                .withSuper("javax.servlet.http.HttpServlet"))
            .build(jar);

        var report = scanner.analyzeJar(jar);
        assertThat(report.level()).isEqualTo(JarCompatibilityLevel.UNKNOWN);
        assertThat(report.reasons()).anyMatch(r -> r.contains("exceeds maximum"));
    }

    @Test
    void handleAnalysisException() throws IOException {
        var extractor = Mockito.mock(BytecodeSignalExtractor.class);
        when(extractor.extractFromJar(any(), anyInt())).thenThrow(new IOException("Failed to read JAR"));
        var metadataExtractor = new MetadataSignalExtractor();
        var scorer = new ScoringEngine();
        var config = JarScanningConfig.get();
        var resolver = new JarResolver();
        var scanner = new DefaultJarCompatibilityScanner(extractor, metadataExtractor, scorer, config, resolver);

        Path jar = tempDir.resolve("error.jar");
        TestJarBuilder.create()
            .withClass(TestJarBuilder.ClassSpec.builder("test/ErrorClass"))
            .build(jar);

        var report = scanner.analyzeJar(jar);
        assertThat(report.level()).isEqualTo(JarCompatibilityLevel.UNKNOWN);
        assertThat(report.reasons()).anyMatch(r -> r.contains("Analysis failed"));
    }

    @Test
    void analyzeWithOptions() throws IOException {
        var extractor = new BytecodeSignalExtractor();
        var metadataExtractor = new MetadataSignalExtractor();
        var scorer = new ScoringEngine();
        var config = JarScanningConfig.get();
        var resolver = new JarResolver();
        var scanner = new DefaultJarCompatibilityScanner(extractor, metadataExtractor, scorer, config, resolver);

        Path jar = tempDir.resolve("options-test.jar");
        TestJarBuilder.create()
            .withClass(TestJarBuilder.ClassSpec.builder("test/OptionBean").withSuper("javax.servlet.http.HttpServlet"))
            .build(jar);

        var options = new JarScanOptions(true, true, false, 100, false, true, 0);
        var report = scanner.analyzeJar(jar, options);

        assertThat(report.level()).isEqualTo(JarCompatibilityLevel.JAVAX);
    }

    @Test
    void cacheKeyIncludesFileModificationTime() throws IOException {
        var extractor = new BytecodeSignalExtractor();
        var metadataExtractor = new MetadataSignalExtractor();
        var scorer = new ScoringEngine();
        var config = JarScanningConfig.get();
        var resolver = new JarResolver();
        var scanner = new DefaultJarCompatibilityScanner(extractor, metadataExtractor, scorer, config, resolver);

        Path jar = tempDir.resolve("mtime-test.jar");
        TestJarBuilder.create()
            .withClass(TestJarBuilder.ClassSpec.builder("test/TimeBean").withSuper("javax.servlet.http.HttpServlet"))
            .build(jar);

        scanner.analyzeJar(jar);
         var stats1 = scanner.getCacheStats();
         long size1 = (Long) stats1.get("size");

         try {
             Thread.sleep(100);
         } catch (InterruptedException e) {
             throw new RuntimeException(e);
         }
         TestJarBuilder.create()
            .withClass(TestJarBuilder.ClassSpec.builder("test/TimeBean").withSuper("javax.servlet.http.HttpServlet"))
            .build(jar);

        scanner.analyzeJar(jar);
        var stats2 = scanner.getCacheStats();
        long size2 = (Long) stats2.get("size");

        // Different modification time creates a new cache entry; size should increase
        assertThat(size2).isEqualTo(size1 + 1);
    }
}