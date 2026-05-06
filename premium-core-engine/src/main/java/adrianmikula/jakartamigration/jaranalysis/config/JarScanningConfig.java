package adrianmikula.jakartamigration.jaranalysis.config;

import adrianmikula.jakartamigration.jaranalysis.domain.JarScanOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.Map;

/**
 * Configuration loader for JAR scanning weights and options.
 * Loads from classpath resource: jar-scanning-weights.yaml
 * Corresponds to TypeSpec: JarScanOptions defaults and scoring config
 */
@Slf4j
public class JarScanningConfig {

    private static final String CONFIG_PATH = "/jar-scanning-weights.yaml";

    private final ScoringConfig scoring;
    private final CacheConfig cache;
    private final PerformanceConfig performance;
    private final FeatureFlags features;

    /**
     * Singleton instance loaded on initialization.
     */
    private static final JarScanningConfig INSTANCE = new JarScanningConfig();

    /**
     * Gets the singleton configuration instance.
     */
    public static JarScanningConfig get() {
        return INSTANCE;
    }

    /**
     * Private constructor loads configuration from YAML.
     */
    private JarScanningConfig() {
        this.scoring = new ScoringConfig();
        this.cache = new CacheConfig();
        this.performance = new PerformanceConfig();
        this.features = new FeatureFlags();
        loadFromYaml();
    }

    /**
     * Load configuration from YAML file, merging with defaults.
     */
    private void loadFromYaml() {
        try {
            InputStream inputStream = getClass().getResourceAsStream(CONFIG_PATH);
            if (inputStream == null) {
                log.warn("jar-scanning-weights.yaml not found at {}, using defaults only", CONFIG_PATH);
                return;
            }

            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            Map<String, Object> root = mapper.readValue(inputStream, Map.class);

            // Load scoring section
            if (root.containsKey("scoring")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> scoringMap = (Map<String, Object>) root.get("scoring");
                scoring.loadFromMap(scoringMap);
                // Also try to load features from scoring section (backward compat)
                if (scoringMap.containsKey("features")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> featuresMap = (Map<String, Object>) scoringMap.get("features");
                    features.loadFromMap(featuresMap);
                }
            }

            // Load cache section
            if (root.containsKey("cache")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> cacheMap = (Map<String, Object>) root.get("cache");
                cache.loadFromMap(cacheMap);
            }

            // Load performance section
            if (root.containsKey("performance")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> perfMap = (Map<String, Object>) root.get("performance");
                performance.loadFromMap(perfMap);
            }

            // Load features from root (preferred location)
            if (root.containsKey("features")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> featuresMap = (Map<String, Object>) root.get("features");
                features.loadFromMap(featuresMap);
            }

            log.info("JAR scanning configuration loaded successfully");

        } catch (Exception e) {
            log.error("Failed to load jar-scanning-weights.yaml, using defaults: {}", e.getMessage(), e);
        }
    }

    // ========== Scoring Configuration ==========

    public int getJakartaClassRefWeight() {
        return scoring.jakartaClassRef;
    }

    public int getJavaxClassRefWeight() {
        return scoring.javaxClassRef;
    }

    public int getJakartaCriticalApiWeight() {
        return scoring.jakartaCriticalApi;
    }

    public int getJavaxCriticalApiWeight() {
        return scoring.javaxCriticalApi;
    }

    public int getJakartaMetadataWeight() {
        return scoring.jakartaMetadata;
    }

    public int getJavaxMetadataWeight() {
        return scoring.javaxMetadata;
    }

    public int getJakartaReflectionWeight() {
        return scoring.jakartaReflection;
    }

    public int getJavaxReflectionWeight() {
        return scoring.javaxReflection;
    }

    public int getJakartaModuleNameWeight() {
        return scoring.jakartaModuleName;
    }

    public Map<String, Double> getApiCriticalityWeights() {
        return scoring.apiCriticality;
    }

    public int getJakartaThreshold() {
        return scoring.thresholds.jakarta;
    }

    public int getJavaxThreshold() {
        return scoring.thresholds.javax;
    }

    public int getMixedMaxThreshold() {
        return scoring.thresholds.mixedMax;
    }

    // ========== Feature Flags ==========

    public boolean isDeepScanningEnabled() {
        return features.enableDeepScanning;
    }

    public boolean isCachingEnabled() {
        return features.enableCaching;
    }

    public boolean isParallelScanEnabled() {
        return features.enableParallelScan;
    }

    public boolean isDetectShaded() {
        return features.detectShaded;
    }

    public boolean isDetectTestScope() {
        return features.detectTestScope;
    }

    public boolean isEarlyExitEnabled() {
        return features.earlyExit;
    }

    public int getEarlyExitThreshold() {
        return features.earlyExitThreshold;
    }

    // ========== Cache Configuration ==========

    public int getCacheMaxSize() {
        return cache.maxSize;
    }

    public long getCacheExpireAfterMillis() {
        return cache.expireAfterHours * 60L * 60L * 1000L;
    }

    public long getMaximumJarSizeBytes() {
        return cache.maximumJarSizeMB * 1024L * 1024L;
    }

    // ========== Performance Configuration ==========

    public int getMaxParallelism() {
        return performance.maxParallelism;
    }

    public int getMaxClassesPerJar() {
        return performance.maxClassesPerJar;
    }

    // ========== Build JarScanOptions from config ==========

    /**
     * Creates JarScanOptions with values from configuration.
     */
    public JarScanOptions createScanOptions() {
        return new JarScanOptions(
            true,  // analyzeMetadata always on
            true,  // analyzeReflection on by default
            isEarlyExitEnabled(),
            getEarlyExitThreshold(),
            isDetectShaded(),
            isDetectTestScope(),
            getMaxClassesPerJar()
        );
    }

    // ========== Nested Config Classes ==========

    private static class ScoringConfig {
        int jakartaClassRef = 5;
        int javaxClassRef = -5;
        int jakartaCriticalApi = 3;
        int javaxCriticalApi = -3;
        int jakartaMetadata = 2;
        int javaxMetadata = -2;
        int jakartaReflection = 1;
        int javaxReflection = -1;
        int jakartaModuleName = 1;
        Map<String, Double> apiCriticality = Map.of();
        Thresholds thresholds = new Thresholds();

        void loadFromMap(Map<String, Object> map) {
            // Load base weights
            if (map.containsKey("jakartaClassRef")) jakartaClassRef = ((Number) map.get("jakartaClassRef")).intValue();
            if (map.containsKey("javaxClassRef")) javaxClassRef = ((Number) map.get("javaxClassRef")).intValue();
            if (map.containsKey("jakartaCriticalApi")) jakartaCriticalApi = ((Number) map.get("jakartaCriticalApi")).intValue();
            if (map.containsKey("javaxCriticalApi")) javaxCriticalApi = ((Number) map.get("javaxCriticalApi")).intValue();
            if (map.containsKey("jakartaMetadata")) jakartaMetadata = ((Number) map.get("jakartaMetadata")).intValue();
            if (map.containsKey("javaxMetadata")) javaxMetadata = ((Number) map.get("javaxMetadata")).intValue();
            if (map.containsKey("jakartaReflection")) jakartaReflection = ((Number) map.get("jakartaReflection")).intValue();
            if (map.containsKey("javaxReflection")) javaxReflection = ((Number) map.get("javaxReflection")).intValue();
            if (map.containsKey("jakartaModuleName")) jakartaModuleName = ((Number) map.get("jakartaModuleName")).intValue();

            // Load API criticality
            if (map.containsKey("apiCriticality")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> apiMap = (Map<String, Object>) map.get("apiCriticality");
                this.apiCriticality = Map.copyOf(apiMap.entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        e -> ((Number) e.getValue()).doubleValue()
                    )));
            }

            // Load thresholds
            if (map.containsKey("thresholds")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> threshMap = (Map<String, Object>) map.get("thresholds");
                thresholds.loadFromMap(threshMap);
            }
        }
    }

    private static class Thresholds {
        int jakarta = 5;
        int javax = -5;
        int mixedMax = 4;

        void loadFromMap(Map<String, Object> map) {
            if (map.containsKey("jakarta")) jakarta = ((Number) map.get("jakarta")).intValue();
            if (map.containsKey("javax")) javax = ((Number) map.get("javax")).intValue();
            if (map.containsKey("mixedMax")) mixedMax = ((Number) map.get("mixedMax")).intValue();
        }
    }

    private static class CacheConfig {
        int maxSize = 1000;
        int expireAfterHours = 24;
        int maximumJarSizeMB = 50;

        void loadFromMap(Map<String, Object> map) {
            if (map.containsKey("maxSize")) maxSize = ((Number) map.get("maxSize")).intValue();
            if (map.containsKey("expireAfterHours")) expireAfterHours = ((Number) map.get("expireAfterHours")).intValue();
            if (map.containsKey("maximumJarSizeMB")) maximumJarSizeMB = ((Number) map.get("maximumJarSizeMB")).intValue();
        }
    }

    private static class PerformanceConfig {
        int maxParallelism = 4;
        int maxClassesPerJar = 0;

        void loadFromMap(Map<String, Object> map) {
            if (map.containsKey("maxParallelism")) maxParallelism = ((Number) map.get("maxParallelism")).intValue();
            if (map.containsKey("maxClassesPerJar")) maxClassesPerJar = ((Number) map.get("maxClassesPerJar")).intValue();
        }
    }

    private static class FeatureFlags {
        boolean enableDeepScanning = true;
        boolean enableCaching = true;
        boolean enableParallelScan = true;
        boolean detectShaded = false;
        boolean detectTestScope = true;
        boolean earlyExit = true;
        int earlyExitThreshold = 10;

        void loadFromMap(Map<String, Object> map) {
            if (map.containsKey("enableDeepScanning")) enableDeepScanning = (Boolean) map.get("enableDeepScanning");
            if (map.containsKey("enableCaching")) enableCaching = (Boolean) map.get("enableCaching");
            if (map.containsKey("enableParallelScan")) enableParallelScan = (Boolean) map.get("enableParallelScan");
            if (map.containsKey("detectShaded")) detectShaded = (Boolean) map.get("detectShaded");
            if (map.containsKey("detectTestScope")) detectTestScope = (Boolean) map.get("detectTestScope");
            if (map.containsKey("earlyExit")) earlyExit = (Boolean) map.get("earlyExit");
            if (map.containsKey("earlyExitThreshold")) earlyExitThreshold = ((Number) map.get("earlyExitThreshold")).intValue();
        }
    }
}
