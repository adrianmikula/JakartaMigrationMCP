package adrianmikula.jakartamigration.jaranalysis.config;

import adrianmikula.jakartamigration.jaranalysis.domain.JarScanOptions;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class JarScanningConfigTest {

    @Test
    void defaultWeightsLoaded() {
        var config = JarScanningConfig.get();

        assertThat(config.getJakartaClassRefWeight()).isEqualTo(5);
        assertThat(config.getJavaxClassRefWeight()).isEqualTo(-5);
        assertThat(config.getJakartaCriticalApiWeight()).isEqualTo(3);
        assertThat(config.getJavaxCriticalApiWeight()).isEqualTo(-3);
        assertThat(config.getJakartaMetadataWeight()).isEqualTo(2);
        assertThat(config.getJavaxMetadataWeight()).isEqualTo(-2);
        assertThat(config.getJakartaReflectionWeight()).isEqualTo(1);
        assertThat(config.getJavaxReflectionWeight()).isEqualTo(-1);
        assertThat(config.getJakartaModuleNameWeight()).isEqualTo(1);
    }

    @Test
    void defaultThresholds() {
        var config = JarScanningConfig.get();

        assertThat(config.getJakartaThreshold()).isEqualTo(5);
        assertThat(config.getJavaxThreshold()).isEqualTo(-5);
        assertThat(config.getMixedMaxThreshold()).isEqualTo(4);
    }

    @Test
    void defaultApiCriticalityMap() {
        var config = JarScanningConfig.get();
        Map<String, Double> weights = config.getApiCriticalityWeights();

        // Config may have empty defaults or specific ones; at least should exist
        assertThat(weights).isNotNull();
    }

    @Test
    void defaultFeatureFlags() {
        var config = JarScanningConfig.get();

        assertThat(config.isDeepScanningEnabled()).isTrue();
        assertThat(config.isCachingEnabled()).isTrue();
        assertThat(config.isParallelScanEnabled()).isTrue();
        assertThat(config.isDetectShaded()).isFalse();
        assertThat(config.isDetectTestScope()).isTrue();
        assertThat(config.isEarlyExitEnabled()).isTrue();
        assertThat(config.getEarlyExitThreshold()).isEqualTo(10);
    }

    @Test
    void defaultCacheSettings() {
        var config = JarScanningConfig.get();

        assertThat(config.getCacheMaxSize()).isEqualTo(1000);
        assertThat(config.getCacheExpireAfterMillis()).isEqualTo(24L * 60 * 60 * 1000);
        assertThat(config.getMaximumJarSizeBytes()).isEqualTo(50L * 1024 * 1024);
    }

    @Test
    void defaultPerformanceSettings() {
        var config = JarScanningConfig.get();

        assertThat(config.getMaxParallelism()).isEqualTo(4);
        assertThat(config.getMaxClassesPerJar()).isZero();
    }

    @Test
    void createScanOptionsDefaults() {
        var config = JarScanningConfig.get();
        var options = config.createScanOptions();

        assertThat(options.analyzeMetadata()).isTrue();
        assertThat(options.analyzeReflection()).isTrue();
        assertThat(options.earlyExitEnabled()).isTrue();
        assertThat(options.earlyExitThreshold()).isEqualTo(10);
        assertThat(options.detectShaded()).isFalse();
        assertThat(options.detectTestScope()).isTrue();
        assertThat(options.maxClassesPerJar()).isZero();
    }

    @Test
    void singletonInstanceLoading() {
        var first = JarScanningConfig.get();
        var second = JarScanningConfig.get();

        assertThat(first).isSameAs(second);
    }

    @Test
    void jarScanOptionsDefaults() {
        var opts = JarScanOptions.DEFAULT;

        assertThat(opts.analyzeMetadata()).isTrue();
        assertThat(opts.analyzeReflection()).isTrue();
        assertThat(opts.earlyExitEnabled()).isTrue();
        assertThat(opts.earlyExitThreshold()).isEqualTo(10);
        assertThat(opts.detectShaded()).isFalse();
        assertThat(opts.detectTestScope()).isTrue();
        assertThat(opts.maxClassesPerJar()).isZero();
    }
}
