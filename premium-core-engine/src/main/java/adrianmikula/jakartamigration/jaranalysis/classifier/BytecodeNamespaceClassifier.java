package adrianmikula.jakartamigration.jaranalysis.classifier;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Namespace;
import adrianmikula.jakartamigration.dependencyanalysis.service.NamespaceClassifier;
import adrianmikula.jakartamigration.dependencyanalysis.service.impl.SimpleNamespaceClassifier;
import adrianmikula.jakartamigration.jaranalysis.config.JarScanningConfig;
import adrianmikula.jakartamigration.jaranalysis.domain.*;
import adrianmikula.jakartamigration.jaranalysis.service.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class BytecodeNamespaceClassifier implements NamespaceClassifier {
    private final SimpleNamespaceClassifier simpleClassifier;
    private final DefaultJarCompatibilityScanner jarScanner;
    private final JarScanningConfig config;
    private final Map<String, ClassificationResult> classificationCache;

    public BytecodeNamespaceClassifier() {
        this(new SimpleNamespaceClassifier(), new DefaultJarCompatibilityScanner(), JarScanningConfig.get());
    }

    public BytecodeNamespaceClassifier(SimpleNamespaceClassifier simpleClassifier,
            DefaultJarCompatibilityScanner jarScanner, JarScanningConfig config) {
        this.simpleClassifier = Objects.requireNonNull(simpleClassifier);
        this.jarScanner = Objects.requireNonNull(jarScanner);
        this.config = Objects.requireNonNull(config);
        this.classificationCache = new HashMap<>();
    }

    @Override
    public Namespace classify(Artifact artifact) {
        return classifyWithScanning(artifact, false).namespace();
    }

    public ClassificationResult classifyWithScanning(Artifact artifact, boolean forceDeepScan) {
        Objects.requireNonNull(artifact);
        if (!isDeepScanningAllowed()) {
            log.debug("Deep scanning not enabled, using simple classifier for {}", artifact.toCoordinate());
            return fallbackToSimple(artifact);
        }
        String cacheKey = artifact.toCoordinate();
        if (!forceDeepScan && config.isCachingEnabled()) {
            ClassificationResult cached = classificationCache.get(cacheKey);
            if (cached != null) {
                log.debug("Classification cache hit for {}", artifact.toCoordinate());
                return cached;
            }
        }
        Namespace fastResult = simpleClassifier.classify(artifact);
        log.debug("Fast classification for {}: {}", artifact.toCoordinate(), fastResult);
        if (!forceDeepScan && (fastResult == Namespace.JAVAX || fastResult == Namespace.JAKARTA)) {
            ClassificationResult result = new ClassificationResult(
                artifact, fastResult, 0.9,
                List.of("Coordinate-based classification: " + fastResult), false, null);
            if (config.isCachingEnabled()) classificationCache.put(cacheKey, result);
            return result;
        }
        return performDeepScanning(artifact, fastResult, forceDeepScan);
    }

    @Override
    public Map<Artifact, Namespace> classifyAll(Collection<Artifact> artifacts) {
        Objects.requireNonNull(artifacts);
        Map<Artifact, Namespace> results = new HashMap<>();
        for (Artifact a : artifacts) results.put(a, classifyWithScanning(a, false).namespace());
        return results;
    }

    public List<ClassificationResult> classifyAllWithScanning(Collection<Artifact> artifacts, boolean forceDeepScan) {
        Objects.requireNonNull(artifacts);
        return artifacts.stream().map(a -> classifyWithScanning(a, forceDeepScan)).collect(Collectors.toList());
    }

    private ClassificationResult performDeepScanning(Artifact artifact, Namespace fastResult, boolean forced) {
        long startTime = System.currentTimeMillis();
        String reasoningBase = "Fast classification: " + fastResult + ". ";
        try {
            Optional<java.nio.file.Path> jarPath = jarScanner.resolveJar(artifact);
            if (jarPath.isEmpty()) return handleMissingJar(artifact, fastResult, reasoningBase, startTime);
            JarCompatibilityReport scanReport = jarScanner.analyzeJar(jarPath.get(), null);
            Namespace namespace = scanReport.level().toNamespace();
            List<String> reasons = new ArrayList<>();
            reasons.add(reasoningBase);
            reasons.add(String.format("Deep scan: %s (confidence: %.2f)", scanReport.level(), scanReport.confidence()));
            reasons.addAll(scanReport.reasons());
            double confidence = Math.max(0.5, scanReport.confidence());
            if (fastResult != namespace && fastResult != Namespace.UNKNOWN) {
                reasons.add(String.format("Deep scan (%s) differs from fast (%s)", namespace, fastResult));
                confidence *= 0.8;
            }
            ClassificationResult result = new ClassificationResult(
                artifact, namespace, confidence, reasons, true, scanReport);
            if (config.isCachingEnabled()) classificationCache.put(artifact.toCoordinate(), result);
            log.info("Deep classified {}: {} ({} ms, confidence: {})",
                artifact.toCoordinate(), namespace, System.currentTimeMillis() - startTime, confidence);
            return result;
        } catch (Exception e) {
            long duration = Math.max(1, System.currentTimeMillis() - startTime);
            log.error("Deep scanning failed for {}: {}", artifact.toCoordinate(), e.getMessage(), e);
            Namespace fallback = (fastResult != Namespace.UNKNOWN) ? fastResult : Namespace.MIXED;
            ClassificationResult result = new ClassificationResult(
                artifact, fallback, 0.5,
                List.of(reasoningBase, "Deep scanning failed: " + e.getMessage()), true, null);
            if (config.isCachingEnabled()) classificationCache.put(artifact.toCoordinate(), result);
            return result;
        }
    }

    private ClassificationResult handleMissingJar(Artifact artifact, Namespace fastResult, String reasoningBase, long startTime) {
        log.warn("JAR not found for {}, using fast: {}", artifact.toCoordinate(), fastResult);
        Namespace fn = (fastResult == Namespace.UNKNOWN) ? Namespace.MIXED : fastResult;
        ClassificationResult result = new ClassificationResult(
            artifact, fn, 0.6,
            List.of(reasoningBase, "JAR not available", "No deep analysis"), false, null);
        if (config.isCachingEnabled()) classificationCache.put(artifact.toCoordinate(), result);
        return result;
    }

    private ClassificationResult fallbackToSimple(Artifact artifact) {
        Namespace ns = simpleClassifier.classify(artifact);
        return new ClassificationResult(artifact, ns, 0.8,
            List.of("Deep scanning disabled, using fast: " + ns), false, null);
    }

    private boolean isDeepScanningAllowed() {
        String fp = System.getProperty("jakarta.migration.deepscan.force");
        if (fp != null) return Boolean.parseBoolean(fp);
        return config.isDeepScanningEnabled();
    }

    public void clearCache() { classificationCache.clear(); }
}
