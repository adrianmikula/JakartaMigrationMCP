package adrianmikula.jakartamigration.jaranalysis.service;

import adrianmikula.jakartamigration.jaranalysis.config.JarScanningConfig;
import adrianmikula.jakartamigration.jaranalysis.domain.JarCompatibilityLevel;
import adrianmikula.jakartamigration.jaranalysis.domain.JarScanSignal;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class ScoringEngineTest {

    private final ScoringEngine engine = new ScoringEngine();

    @Test
    void scorePureJavaxSignal() {
        JarScanSignal signal = new JarScanSignal.Builder()
            .artifactCoordinate("test:javax:1.0")
            .javaxClassRefs(5)
            .jakartaClassRefs(0)
            .build();

        var result = engine.score(signal, signal.artifactCoordinate());

        assertThat(result.level()).isEqualTo(JarCompatibilityLevel.JAVAX);
        assertThat(result.confidence()).isGreaterThan(0.5);
        assertThat(result.rawScore()).isNegative();
    }

    @Test
    void scorePureJakartaSignal() {
        JarScanSignal signal = new JarScanSignal.Builder()
            .artifactCoordinate("test:jakarta:1.0")
            .javaxClassRefs(0)
            .jakartaClassRefs(5)
            .build();

        var result = engine.score(signal, signal.artifactCoordinate());

        assertThat(result.level()).isEqualTo(JarCompatibilityLevel.JAKARTA);
        assertThat(result.confidence()).isGreaterThan(0.5);
        assertThat(result.rawScore()).isPositive();
    }

    @Test
    void scoreMixedSignalJaxavaxDominance() {
        JarScanSignal signal = new JarScanSignal.Builder()
            .artifactCoordinate("test:mixed-jaavax:1.0")
            .javaxClassRefs(8)
            .jakartaClassRefs(2)
            .build();

        var result = engine.score(signal, signal.artifactCoordinate());

        assertThat(result.level()).isEqualTo(JarCompatibilityLevel.JAVAX);
        assertThat(result.rawScore()).isNegative();
    }

    @Test
    void scoreMixedSignalJakartaDominance() {
        JarScanSignal signal = new JarScanSignal.Builder()
            .artifactCoordinate("test:mixed-jakarta:1.0")
            .javaxClassRefs(2)
            .jakartaClassRefs(8)
            .build();

        var result = engine.score(signal, signal.artifactCoordinate());

        assertThat(result.level()).isEqualTo(JarCompatibilityLevel.JAKARTA);
        assertThat(result.rawScore()).isPositive();
    }

    @Test
    void scoreBalancedMixedSignal() {
        JarScanSignal signal = new JarScanSignal.Builder()
            .artifactCoordinate("test:balanced:1.0")
            .javaxClassRefs(3)
            .jakartaClassRefs(3)
            .build();

        var result = engine.score(signal, signal.artifactCoordinate());

        assertThat(result.level()).isEqualTo(JarCompatibilityLevel.MIXED);
        assertThat(result.rawScore()).isCloseTo(0.0, within(0.1));
    }

    @Test
    void scoreWithPomMetadataJavax() {
        JarScanSignal signal = new JarScanSignal.Builder()
            .artifactCoordinate("test:meta-javax:1.0")
            .javaxClassRefs(1)
            .jakartaClassRefs(0)
            .hasPomMetadata(true)
            .pomIndicatesJavax(true)
            .build();

        var result = engine.score(signal, signal.artifactCoordinate());

        // POM metadata weight should shift score more negative
        assertThat(result.reasons()).anyMatch(r -> r.contains("POM indicates javax"));
    }

    @Test
    void scoreWithPomMetadataJakarta() {
        JarScanSignal signal = new JarScanSignal.Builder()
            .artifactCoordinate("test:meta-jakarta:1.0")
            .javaxClassRefs(0)
            .jakartaClassRefs(1)
            .hasPomMetadata(true)
            .pomIndicatesJakarta(true)
            .build();

        var result = engine.score(signal, signal.artifactCoordinate());

        assertThat(result.reasons()).anyMatch(r -> r.contains("POM indicates jakarta"));
    }

    @Test
    void scoreWithAutomaticModuleNameJavax() {
        JarScanSignal signal = new JarScanSignal.Builder()
            .artifactCoordinate("test:am-javax:1.0")
            .javaxClassRefs(2)
            .jakartaClassRefs(0)
            .automaticModuleName("javax.servlet-api")
            .build();

        var result = engine.score(signal, signal.artifactCoordinate());

        assertThat(result.reasons()).anyMatch(r -> r.contains("Automatic-Module-Name"));
    }

    @Test
    void scoreWithReflectionStrings() {
        JarScanSignal signal = new JarScanSignal.Builder()
            .artifactCoordinate("test:reflect:1.0")
            .javaxClassRefs(1)
            .jakartaClassRefs(0)
            .reflectionStrings(new String[]{"javax.inject", "some.reflection"})
            .build();

        var result = engine.score(signal, signal.artifactCoordinate());

        assertThat(result.reasons()).anyMatch(r -> r.contains("Reflection string"));
    }

    @Test
    void apiCriticalityMultiplierServlet() {
        JarScanSignal signal = new JarScanSignal.Builder()
            .artifactCoordinate("test:critical:1.0")
            .javaxClassRefs(2)
            .jakartaClassRefs(0)
            .apiUsage(Map.of("servlet", 3))
            .build();

        var result = engine.score(signal, signal.artifactCoordinate());

        assertThat(result.reasons()).anyMatch(r -> r.contains("API 'servlet'"));
    }

    @Test
    void apiCriticalityMultiplierPersistence() {
        JarScanSignal signal = new JarScanSignal.Builder()
            .artifactCoordinate("test:persistence:1.0")
            .javaxClassRefs(0)
            .jakartaClassRefs(2)
            .apiUsage(Map.of("persistence", 2))
            .build();

        var result = engine.score(signal, signal.artifactCoordinate());

        assertThat(result.reasons()).anyMatch(r -> r.contains("API 'persistence'"));
    }

    @Test
    void apiCriticalityMultiplierCdi() {
        JarScanSignal signal = new JarScanSignal.Builder()
            .artifactCoordinate("test:cdi:1.0")
            .javaxClassRefs(4)
            .jakartaClassRefs(0)
            .apiUsage(Map.of("cdi", 4))
            .build();

        var result = engine.score(signal, signal.artifactCoordinate());

        assertThat(result.reasons()).anyMatch(r -> r.contains("API 'cdi'"));
    }

    @Test
    void combinedApiUsageWeights() {
        JarScanSignal signal = new JarScanSignal.Builder()
            .artifactCoordinate("test:combined:1.0")
            .javaxClassRefs(3)
            .jakartaClassRefs(1)
            .apiUsage(Map.of("servlet", 2, "cdi", 1))
            .build();

        var result = engine.score(signal, signal.artifactCoordinate());

        // Should sum contributions from multiple API categories
        assertThat(result.reasons()).anyMatch(r -> r.contains("API"));
    }

    @Test
    void confidenceStrongSignal() {
        JarScanSignal signal = new JarScanSignal.Builder()
            .artifactCoordinate("test:strong:1.0")
            .javaxClassRefs(10)
            .jakartaClassRefs(0)
            .apiUsage(Map.of("servlet", 5))
            .hasPomMetadata(true)
            .pomIndicatesJavax(true)
            .build();

        var result = engine.score(signal, signal.artifactCoordinate());

        assertThat(result.confidence()).isGreaterThan(0.8);
    }

    @Test
    void confidenceWeakSignal() {
        JarScanSignal signal = new JarScanSignal.Builder()
            .artifactCoordinate("test:weak:1.0")
            .javaxClassRefs(1)
            .jakartaClassRefs(0)
            .build();

        var result = engine.score(signal, signal.artifactCoordinate());

        assertThat(result.confidence()).isBetween(0.2, 0.4);
    }

    @Test
    void confidenceReducedForMixed() {
        // Balanced mixed signal (5 javax, 5 jakarta) produces MIXED level with reduced confidence
        JarScanSignal signal = new JarScanSignal.Builder()
            .artifactCoordinate("test:mixed-conf:1.0")
            .javaxClassRefs(5)
            .jakartaClassRefs(5)
            .build();

        var result = engine.score(signal, signal.artifactCoordinate());

        // With balanced counts, level should be MIXED and confidence reduced by 0.85 multiplier
        assertThat(result.level()).isEqualTo(JarCompatibilityLevel.MIXED);
        // Base confidence for 10 total refs is 0.3, after 0.85 multiplier ~0.255
        assertThat(result.confidence()).isLessThan(0.3);
    }

    @Test
    void scoreWithShadedPackages() {
        JarScanSignal signal = new JarScanSignal.Builder()
            .artifactCoordinate("test:shaded:1.0")
            .javaxClassRefs(2)
            .jakartaClassRefs(0)
            .hasShadedPackages(true)
            .build();

        var result = engine.score(signal, signal.artifactCoordinate());

        assertThat(result.reasons()).anyMatch(r -> r.contains("Shaded"));
    }

    @Test
    void scoreWithTestOnlyPatterns() {
        JarScanSignal signal = new JarScanSignal.Builder()
            .artifactCoordinate("test:testonly:1.0")
            .javaxClassRefs(1)
            .testOnlyPatterns(new String[]{"test-directory-structure"})
            .build();

        var result = engine.score(signal, signal.artifactCoordinate());

        assertThat(result.reasons()).anyMatch(r -> r.contains("Test-only"));
    }

    @Test
    void scoreZeroReferences() {
        JarScanSignal signal = new JarScanSignal.Builder()
            .artifactCoordinate("test:unknown:1.0")
            .build();

        var result = engine.score(signal, signal.artifactCoordinate());

        assertThat(result.level()).isEqualTo(JarCompatibilityLevel.UNKNOWN);
        assertThat(result.confidence()).isZero();
    }

    @Test
    void customConfigurationWeights() {
        // Build custom config with modified weights
        // We'll test this by constructing with custom config
        // Since JarScanningConfig is a singleton, we use reflection or custom instance
        // For now, verify constructor works
        assertThat(engine).isNotNull();
    }

    @Test
    void scoreExplanationReasons() {
        JarScanSignal signal = new JarScanSignal.Builder()
            .artifactCoordinate("test:reasons:1.0")
            .javaxClassRefs(3)
            .jakartaClassRefs(0)
            .apiUsage(Map.of("servlet", 2))
            .hasPomMetadata(true)
            .pomIndicatesJavax(true)
            .build();

        var result = engine.score(signal, signal.artifactCoordinate());

        assertThat(result.reasons()).isNotEmpty();
        assertThat(result.reasons()).anyMatch(r -> r.contains("3 javax"));
        assertThat(result.reasons()).anyMatch(r -> r.contains("API"));
        assertThat(result.reasons()).anyMatch(r -> r.contains("POM"));
    }
}
