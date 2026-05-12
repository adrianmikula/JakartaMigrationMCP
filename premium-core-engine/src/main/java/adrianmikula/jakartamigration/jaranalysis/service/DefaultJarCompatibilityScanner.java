package adrianmikula.jakartamigration.jaranalysis.service;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.service.JarResolver;
import adrianmikula.jakartamigration.jaranalysis.config.JarScanningConfig;
import adrianmikula.jakartamigration.jaranalysis.domain.*;
import lombok.extern.slf4j.Slf4j;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
public class DefaultJarCompatibilityScanner implements JarCompatibilityScanner {
    private final BytecodeSignalExtractor bytecodeExtractor;
    private final MetadataSignalExtractor metadataExtractor;
    private final ScoringEngine scoringEngine;
    private final JarScanningConfig config;
    private final JarResolver jarResolver;
    private final Cache<String, JarCompatibilityReport> resultCache;
    private final ExecutorService parallelExecutor;
    
    // Index for O(1) cache lookups by artifact coordinate
    private final Map<String, String> artifactCoordinateToCacheKeyIndex = new ConcurrentHashMap<>();

    public DefaultJarCompatibilityScanner() {
        this(new BytecodeSignalExtractor(), new MetadataSignalExtractor(),
            new ScoringEngine(), JarScanningConfig.get(), new JarResolver());
    }

    public DefaultJarCompatibilityScanner(BytecodeSignalExtractor bytecodeExtractor,
            MetadataSignalExtractor metadataExtractor, ScoringEngine scoringEngine,
            JarScanningConfig config, JarResolver jarResolver) {
        this.bytecodeExtractor = Objects.requireNonNull(bytecodeExtractor);
        this.metadataExtractor = Objects.requireNonNull(metadataExtractor);
        this.scoringEngine = Objects.requireNonNull(scoringEngine);
        this.config = Objects.requireNonNull(config);
        this.jarResolver = Objects.requireNonNull(jarResolver);
        this.resultCache = createCache();
        this.parallelExecutor = createExecutor();
    }

    private Cache<String, JarCompatibilityReport> createCache() {
        if (!config.isCachingEnabled()) return CacheBuilder.newBuilder().maximumSize(0).build();
        return CacheBuilder.newBuilder().maximumSize(config.getCacheMaxSize())
            .expireAfterWrite(Duration.ofHours(config.getCacheExpireAfterMillis() / (60 * 60 * 1000)))
            .recordStats().build();
    }

    private ExecutorService createExecutor() {
        if (!config.isParallelScanEnabled()) {
            return Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r); t.setName("jar-scanner-sequential"); t.setDaemon(true);
                return t;
            });
        }
        if (config.isUseVirtualThreads()) {
            ThreadFactory factory = r -> {
                Thread t = Thread.ofVirtual().unstarted(r);
                t.setName("jar-scanner-virtual-" + t.getId());
                return t;
            };
            return Executors.newThreadPerTaskExecutor(factory);
        }
        int parallelism = config.getMaxParallelism();
        ThreadFactory factory = r -> { Thread t = new Thread(r);
            t.setName("jar-scanner-" + t.getId()); t.setDaemon(true); return t; };
        return Executors.newFixedThreadPool(parallelism, factory);
    }

    @Override
    public JarCompatibilityReport analyzeJar(Path jarPath, JarScanOptions options) {
        Objects.requireNonNull(jarPath, "jarPath cannot be null");
        JarScanOptions opts = (options != null) ? options : config.createScanOptions();
        String cacheKey = computeCacheKeyForPath(jarPath);
        if (config.isCachingEnabled()) {
            JarCompatibilityReport cached = resultCache.getIfPresent(cacheKey);
            if (cached != null) return copyWithCachedFlag(cached, true);
        }
        try {
            long jarSizeBytes = Files.size(jarPath);
            if (config.getMaximumJarSizeBytes() > 0 && jarSizeBytes > config.getMaximumJarSizeBytes()) {
                return createUnknownReport(jarPath.toString(), "JAR exceeds maximum configured size", 0, true);
            }
        } catch (IOException e) { log.warn("Cannot read JAR size {}: {}", jarPath, e.getMessage()); }
        long startTime = System.currentTimeMillis();
        try {
            JarScanSignal signal = extractSignal(jarPath, opts);
            ScoringEngine.ScoringResult scoreResult = scoringEngine.score(signal, signal.artifactCoordinate());
            JarCompatibilityReport report = new JarCompatibilityReport(signal.artifactCoordinate(),
                scoreResult.level(), scoreResult.confidence(), scoreResult.reasons(), signal,
                Math.max(1, System.currentTimeMillis() - startTime), false);
            if (config.isCachingEnabled()) {
                resultCache.put(cacheKey, report);
                // Update index for O(1) lookups
                artifactCoordinateToCacheKeyIndex.put(signal.artifactCoordinate(), cacheKey);
            }
            log.info("Analyzed JAR {}: {} (confidence: {}, {} ms)", jarPath, report.level(), report.confidence(), report.analysisTimeMs());
            return report;
        } catch (Exception e) {
            long duration = Math.max(1, System.currentTimeMillis() - startTime);
            log.error("Failed to analyze JAR {}: {}", jarPath, e.getMessage(), e);
            return createUnknownReport(jarPath.toString(), "Analysis failed: " + e.getMessage(), duration, false);
        }
    }

    @Override
    public JarCompatibilityReport analyzeJar(Path jarPath) {
        return analyzeJar(jarPath, null);
    }

    @Override
    public List<JarCompatibilityReport> analyzeJars(List<Path> jarPaths, JarScanOptions options) {
        Objects.requireNonNull(jarPaths, "jarPaths cannot be null");
        JarScanOptions opts = (options != null) ? options : config.createScanOptions();
        return jarPaths.stream()
            .map(jarPath -> {
                try {
                    return analyzeJar(jarPath, opts);
                } catch (Exception e) {
                    return createUnknownReport(jarPath.toString(), e.getMessage(), 0, false);
                }
            })
            .collect(Collectors.toList());
    }

    @Override
    public JarCompatibilityReport getCachedResult(String artifactCoordinate) {
        if (!config.isCachingEnabled()) return null;
        String cacheKey = artifactCoordinateToCacheKeyIndex.get(artifactCoordinate);
        if (cacheKey != null) {
            JarCompatibilityReport report = resultCache.getIfPresent(cacheKey);
            if (report != null) {
                return copyWithCachedFlag(report, true);
            }
        }
        return null;
    }

    @Override
    public boolean clearCache() { 
        resultCache.invalidateAll(); 
        artifactCoordinateToCacheKeyIndex.clear();
        return true; 
    }

    @Override
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        if (config.isCachingEnabled()) {
            var cs = resultCache.stats();
            stats.put("size", resultCache.size());
            stats.put("hitCount", cs.hitCount());
            stats.put("missCount", cs.missCount());
            stats.put("hitRate", cs.hitRate());
            stats.put("evictionCount", cs.evictionCount());
        }
        stats.put("maxSize", config.getCacheMaxSize());
        return stats;
    }

    public JarCompatibilityReport analyzeArtifact(Artifact artifact) {
        return jarResolver.resolve(artifact)
            .map(jp -> analyzeJar(jp, config.createScanOptions()))
            .orElseGet(() -> createUnknownReport(artifact.toCoordinate(), "JAR not found", 0, false));
    }

    public List<JarCompatibilityReport> analyzeArtifacts(List<Artifact> artifacts) {
        return artifacts.stream().map(this::analyzeArtifact).collect(Collectors.toList());
    }

    public Optional<Path> resolveJar(Artifact artifact) {
        return jarResolver.resolve(artifact);
    }

    private JarScanSignal extractSignal(Path jarPath, JarScanOptions options) throws IOException {
        Objects.requireNonNull(jarPath, "jarPath cannot be null");
        JarScanSignal signal = bytecodeExtractor.extractFromJar(jarPath, options.maxClassesPerJar());
        if (options.analyzeMetadata()) {
            signal = metadataExtractor.enhanceSignal(jarPath, signal);
        }
        return signal;
    }

    private JarCompatibilityReport createUnknownReport(String c, String r, long d, boolean cached) {
        JarScanSignal s = new JarScanSignal.Builder().artifactCoordinate(c).build();
        return new JarCompatibilityReport.Builder().artifactCoordinate(c).level(JarCompatibilityLevel.UNKNOWN)
            .confidence(0.0).reasons(List.of(r)).signal(s).analysisTimeMs(Math.max(1, d)).cached(cached).build();
    }

    private JarCompatibilityReport copyWithCachedFlag(JarCompatibilityReport r, boolean cached) {
        return new JarCompatibilityReport(r.artifactCoordinate(), r.level(), r.confidence(),
            r.reasons(), r.signal(), r.analysisTimeMs(), cached);
    }

    private String computeCacheKeyForPath(Path jarPath) {
        try {
            return jarPath.toAbsolutePath() + ":" + Files.getLastModifiedTime(jarPath).toMillis();
        } catch (IOException e) { return jarPath.toAbsolutePath().toString(); }
    }

    public void shutdown() {
        parallelExecutor.shutdown();
        try {
            if (!parallelExecutor.awaitTermination(30, TimeUnit.SECONDS)) parallelExecutor.shutdownNow();
        } catch (InterruptedException e) {
            parallelExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}