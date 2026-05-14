package adrianmikula.jakartamigration.jaranalysis.classifier;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Namespace;
import adrianmikula.jakartamigration.dependencyanalysis.service.impl.SimpleNamespaceClassifier;
import adrianmikula.jakartamigration.jaranalysis.config.JarScanningConfig;
import adrianmikula.jakartamigration.jaranalysis.domain.JarCompatibilityLevel;
import adrianmikula.jakartamigration.jaranalysis.domain.JarCompatibilityReport;
import adrianmikula.jakartamigration.jaranalysis.domain.JarScanSignal;
import adrianmikula.jakartamigration.jaranalysis.service.DefaultJarCompatibilityScanner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class BytecodeNamespaceClassifierTest {

    @TempDir
    Path tempDir;

    @Test
    void fastPathClearJavaxFromCoordinates() {
        var simpleClassifier = mock(SimpleNamespaceClassifier.class);
        when(simpleClassifier.classify(any())).thenReturn(Namespace.JAVAX);

        var jarScanner = mock(DefaultJarCompatibilityScanner.class);
        var config = mock(JarScanningConfig.class);
        when(config.isDeepScanningEnabled()).thenReturn(true);
        when(config.isCachingEnabled()).thenReturn(false);

        var classifier = new BytecodeNamespaceClassifier(simpleClassifier, jarScanner, config);

        var artifact = new Artifact("javax.servlet", "javax.servlet-api", "4.0.1", "compile", false);
        var result = classifier.classifyWithScanning(artifact, false);

        verify(jarScanner, never()).analyzeJar(any());
        assertThat(result.namespace()).isEqualTo(Namespace.JAVAX);
        assertThat(result.deepScanUsed()).isFalse();
    }

    @Test
    void fastPathClearJakartaFromCoordinates() {
        var simpleClassifier = mock(SimpleNamespaceClassifier.class);
        when(simpleClassifier.classify(any())).thenReturn(Namespace.JAKARTA);

        var jarScanner = mock(DefaultJarCompatibilityScanner.class);
        var config = mock(JarScanningConfig.class);
        when(config.isDeepScanningEnabled()).thenReturn(true);
        when(config.isCachingEnabled()).thenReturn(false);

        var classifier = new BytecodeNamespaceClassifier(simpleClassifier, jarScanner, config);

        var artifact = new Artifact("jakarta.servlet", "jakarta.servlet-api", "5.0.0", "compile", false);
        var result = classifier.classifyWithScanning(artifact, false);

        verify(jarScanner, never()).analyzeJar(any());
        assertThat(result.namespace()).isEqualTo(Namespace.JAKARTA);
        assertThat(result.deepScanUsed()).isFalse();
    }

    @Test
    void deepScanForUnknownFromCoordinates() throws Exception {
        var simpleClassifier = mock(SimpleNamespaceClassifier.class);
        when(simpleClassifier.classify(any())).thenReturn(Namespace.UNKNOWN);

        var jarScanner = mock(DefaultJarCompatibilityScanner.class);
        var config = mock(JarScanningConfig.class);
        when(config.isDeepScanningEnabled()).thenReturn(true);
        when(config.isCachingEnabled()).thenReturn(false);

        Path mockJar = tempDir.resolve("mock.jar");
        var report = new JarCompatibilityReport.Builder()
            .artifactCoordinate("org.springframework:spring-core:5.3.0")
            .level(JarCompatibilityLevel.UNKNOWN)
            .confidence(0.5)
            .reasons(List.of("Unknown"))
            .signal(new JarScanSignal.Builder().artifactCoordinate("org.springframework:spring-core:5.3.0").build())
            .analysisTimeMs(10)
            .cached(false)
            .build();
        when(jarScanner.resolveJar(any())).thenReturn(Optional.of(mockJar));
        when(jarScanner.analyzeJar(any(), any())).thenReturn(report);

        var classifier = new BytecodeNamespaceClassifier(simpleClassifier, jarScanner, config);

        var artifact = new Artifact("org.springframework", "spring-core", "5.3.0", "compile", false);
        var result = classifier.classifyWithScanning(artifact, false);

        verify(jarScanner).analyzeJar(any(), any());
        assertThat(result.deepScanUsed()).isTrue();
    }

    @Test
    void cacheClearedViaClearCache() throws Exception {
        var simpleClassifier = mock(SimpleNamespaceClassifier.class);
        when(simpleClassifier.classify(any())).thenReturn(Namespace.MIXED);

        var jarScanner = mock(DefaultJarCompatibilityScanner.class);
        var config = mock(JarScanningConfig.class);
        when(config.isDeepScanningEnabled()).thenReturn(true);
        when(config.isCachingEnabled()).thenReturn(true);

        var report = new JarCompatibilityReport.Builder()
            .artifactCoordinate("org.test:test:1.0")
            .level(JarCompatibilityLevel.MIXED)
            .confidence(0.6)
            .reasons(List.of("Mixed"))
            .signal(new JarScanSignal.Builder().artifactCoordinate("org.test:test:1.0").build())
            .analysisTimeMs(10)
            .cached(false)
            .build();

        Path jarPath = tempDir.resolve("test.jar");
        when(jarScanner.resolveJar(any())).thenReturn(Optional.of(jarPath));
        when(jarScanner.analyzeJar(any(), any())).thenReturn(report);

        var classifier = new BytecodeNamespaceClassifier(simpleClassifier, jarScanner, config);

        var artifact = new Artifact("org.test", "test", "1.0", "compile", false);
        classifier.classifyWithScanning(artifact, false);
        classifier.clearCache();
        classifier.classifyWithScanning(artifact, false);

        verify(jarScanner, times(2)).analyzeJar(any(), any());
    }

    @Test
    void fallbackWhenDeepScanningDisabled() {
        var simpleClassifier = mock(SimpleNamespaceClassifier.class);
        when(simpleClassifier.classify(any())).thenReturn(Namespace.MIXED);

        var jarScanner = mock(DefaultJarCompatibilityScanner.class);
        var config = mock(JarScanningConfig.class);
        when(config.isDeepScanningEnabled()).thenReturn(false);

        var classifier = new BytecodeNamespaceClassifier(simpleClassifier, jarScanner, config);

        var artifact = new Artifact("org.test", "test", "1.0", "compile", false);
        var result = classifier.classifyWithScanning(artifact, false);

        verify(jarScanner, never()).analyzeJar(any());
        assertThat(result.namespace()).isEqualTo(Namespace.MIXED);
    }

    @Test
    void classifyAllBatchProcessing() {
        var simpleClassifier = mock(SimpleNamespaceClassifier.class);
        when(simpleClassifier.classify(any()))
            .thenReturn(Namespace.JAVAX)
            .thenReturn(Namespace.JAKARTA);

        var jarScanner = mock(DefaultJarCompatibilityScanner.class);
        var config = mock(JarScanningConfig.class);
        when(config.isDeepScanningEnabled()).thenReturn(true);
        when(config.isCachingEnabled()).thenReturn(false);

        var classifier = new BytecodeNamespaceClassifier(simpleClassifier, jarScanner, config);

        var artifacts = List.of(
            new Artifact("g1", "a1", "v1", "compile", false),
            new Artifact("g2", "a2", "v2", "compile", false)
        );
        var map = classifier.classifyAll(artifacts);

        assertThat(map).hasSize(2);
        assertThat(map.get(artifacts.get(0))).isEqualTo(Namespace.JAVAX);
        assertThat(map.get(artifacts.get(1))).isEqualTo(Namespace.JAKARTA);
    }

    @Test
    void classifyAllWithScanningBatch() throws Exception {
        var simpleClassifier = mock(SimpleNamespaceClassifier.class);
        when(simpleClassifier.classify(any())).thenReturn(Namespace.UNKNOWN);

        var jarScanner = mock(DefaultJarCompatibilityScanner.class);
        var config = mock(JarScanningConfig.class);
        when(config.isDeepScanningEnabled()).thenReturn(true);
        when(config.isCachingEnabled()).thenReturn(false);

        var reports = List.of(
            new JarCompatibilityReport.Builder()
                .artifactCoordinate("a1")
                .level(JarCompatibilityLevel.JAVAX)
                .confidence(0.7)
                .reasons(List.of("JAVAX"))
                .signal(new JarScanSignal.Builder().artifactCoordinate("a1").build())
                .analysisTimeMs(10)
                .cached(false)
                .build(),
            new JarCompatibilityReport.Builder()
                .artifactCoordinate("a2")
                .level(JarCompatibilityLevel.JAKARTA)
                .confidence(0.7)
                .reasons(List.of("JAKARTA"))
                .signal(new JarScanSignal.Builder().artifactCoordinate("a2").build())
                .analysisTimeMs(10)
                .cached(false)
                .build()
        );

        Path jarPath = tempDir.resolve("dummy.jar");
        when(jarScanner.resolveJar(any())).thenReturn(Optional.of(jarPath));
        when(jarScanner.analyzeJar(any(), any()))
            .thenReturn(reports.get(0))
            .thenReturn(reports.get(1));

        var classifier = new BytecodeNamespaceClassifier(simpleClassifier, jarScanner, config);

        var artifacts = List.of(
            new Artifact("g1", "a1", "v1", "compile", false),
            new Artifact("g2", "a2", "v2", "compile", false)
        );
        var results = classifier.classifyAllWithScanning(artifacts, false);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).namespace()).isEqualTo(Namespace.JAVAX);
        assertThat(results.get(1).namespace()).isEqualTo(Namespace.JAKARTA);
    }

    @Test
    void noDeepScanningAllowedWhenSystemPropertyFalse() {
        var simpleClassifier = mock(SimpleNamespaceClassifier.class);
        when(simpleClassifier.classify(any())).thenReturn(Namespace.UNKNOWN);
        var jarScanner = mock(DefaultJarCompatibilityScanner.class);
        var config = mock(JarScanningConfig.class);
        when(config.isDeepScanningEnabled()).thenReturn(true);

        System.setProperty("jakarta.migration.deepscan.force", "false");
        try {
            var classifier = new BytecodeNamespaceClassifier(simpleClassifier, jarScanner, config);
            var artifact = new Artifact("g", "a", "v", "compile", false);
            var result = classifier.classifyWithScanning(artifact, false);
            assertThat(result.deepScanUsed()).isFalse();
        } finally {
            System.clearProperty("jakarta.migration.deepscan.force");
        }
    }

    @Test
    void noDeepScanningAllowedWhenSystemPropertyTrue() throws Exception {
        var simpleClassifier = mock(SimpleNamespaceClassifier.class);
        when(simpleClassifier.classify(any())).thenReturn(Namespace.UNKNOWN);

        var jarScanner = mock(DefaultJarCompatibilityScanner.class);
        var config = mock(JarScanningConfig.class);
        when(config.isDeepScanningEnabled()).thenReturn(false);
        when(config.isCachingEnabled()).thenReturn(false);

        var report = new JarCompatibilityReport.Builder()
            .artifactCoordinate("g:a:v")
            .level(JarCompatibilityLevel.MIXED)
            .confidence(0.5)
            .reasons(List.of("mixed"))
            .signal(new JarScanSignal.Builder().artifactCoordinate("g:a:v").build())
            .analysisTimeMs(10)
            .cached(false)
            .build();

        Path jarPath = tempDir.resolve("dummy.jar");
        when(jarScanner.resolveJar(any())).thenReturn(Optional.of(jarPath));
        when(jarScanner.analyzeJar(any(), any())).thenReturn(report);

        System.setProperty("jakarta.migration.deepscan.force", "true");
        try {
            var classifier = new BytecodeNamespaceClassifier(simpleClassifier, jarScanner, config);
            var artifact = new Artifact("g", "a", "v", "compile", false);
            var result = classifier.classifyWithScanning(artifact, false);
            assertThat(result.deepScanUsed()).isTrue();
        } finally {
            System.clearProperty("jakarta.migration.deepscan.force");
        }
    }
}
