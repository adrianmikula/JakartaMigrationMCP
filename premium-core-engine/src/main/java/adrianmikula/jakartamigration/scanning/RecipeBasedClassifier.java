package adrianmikula.jakartamigration.scanning;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Namespace;
import adrianmikula.jakartamigration.dependencyanalysis.service.NamespaceClassifier;
import adrianmikula.jakartamigration.jaranalysis.domain.JarCompatibilityLevel;
import adrianmikula.jakartamigration.jaranalysis.domain.JarCompatibilityReport;
import adrianmikula.jakartamigration.jaranalysis.service.DefaultJarCompatibilityScanner;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;

/**
 * Hybrid classifier that uses recipe-derived coordinate mappings and package rename patterns
 * for fast O(1) classification, falling back to JAR regex scanning for UNKNOWN artifacts.
 *
 * Two-tier matching:
 * 1. Fast coordinate lookup (O(1), handles ~80% of cases)
 * 2. Full regex scan of JAR entries for remaining UNKNOWN artifacts
 */
@Slf4j
public class RecipeBasedClassifier implements NamespaceClassifier {

    private static final int DEFAULT_SCAN_THREADS = 4;

    private final RecipePatternExtractor patternExtractor;
    private final DefaultJarCompatibilityScanner jarScanner;
    private final ExecutorService scanExecutor;

    // Cached maps built from recipe patterns
    private volatile Map<String, String> coordinateMap = Map.of();
    private volatile Map<String, String> packageRenameMap = Map.of();
    private volatile Set<String> jakartaGroupPrefixes = Set.of();

    public RecipeBasedClassifier() {
        this(new RecipePatternExtractor(), new DefaultJarCompatibilityScanner());
    }

    public RecipeBasedClassifier(RecipePatternExtractor patternExtractor,
                                  DefaultJarCompatibilityScanner jarScanner) {
        this(patternExtractor, jarScanner, DEFAULT_SCAN_THREADS);
    }

    public RecipeBasedClassifier(RecipePatternExtractor patternExtractor,
                                  DefaultJarCompatibilityScanner jarScanner,
                                  int scanThreads) {
        this.patternExtractor = Objects.requireNonNull(patternExtractor);
        this.jarScanner = jarScanner;
        this.scanExecutor = Executors.newFixedThreadPool(scanThreads, r -> {
            Thread t = new Thread(r, "recipe-classifier-scan");
            t.setDaemon(true);
            return t;
        });
        refreshMaps();
    }

    /**
     * Refreshes the internal coordinate and package rename maps from the pattern extractor.
     */
    public void refreshMaps() {
        coordinateMap = patternExtractor.getCoordinateMap();
        packageRenameMap = patternExtractor.getPackageRenameMap();

        // Build set of known Jakarta group ID prefixes from coordinate mappings
        Set<String> prefixes = new HashSet<>();
        for (String key : coordinateMap.keySet()) {
            String groupId = key.substring(0, key.indexOf(':'));
            if (groupId.startsWith("jakarta.")) {
                prefixes.add(groupId);
            }
        }
        // Add common Jakarta prefixes
        prefixes.add("jakarta");
        jakartaGroupPrefixes = Collections.unmodifiableSet(prefixes);

        log.info("RecipeBasedClassifier maps refreshed: {} coordinate mappings, {} package renames",
            coordinateMap.size(), packageRenameMap.size());
    }

    @Override
    public Namespace classify(Artifact artifact) {
        Objects.requireNonNull(artifact);

        // Tier 1: Fast coordinate lookup (O(1))
        String identifier = artifact.toIdentifier();
        Namespace tier1Result = classifyByCoordinate(artifact, identifier);
        if (tier1Result != Namespace.UNKNOWN) {
            return tier1Result;
        }

        // Tier 2: groupId prefix heuristics
        Namespace prefixResult = classifyByGroupPrefix(artifact);
        if (prefixResult != Namespace.UNKNOWN) {
            return prefixResult;
        }

        // Tier 3: JAR regex scanning (if scanner available)
        if (jarScanner != null) {
            Namespace jarResult = classifyByJarScan(artifact);
            if (jarResult != Namespace.UNKNOWN) {
                return jarResult;
            }
        }

        return Namespace.UNKNOWN;
    }

    @Override
    public Map<Artifact, Namespace> classifyAll(Collection<Artifact> artifacts) {
        Objects.requireNonNull(artifacts);
        Map<Artifact, Namespace> results = new HashMap<>();
        for (Artifact artifact : artifacts) {
            results.put(artifact, classify(artifact));
        }
        return results;
    }

    /**
     * Fast coordinate-based classification using recipe-derived mappings.
     */
    private Namespace classifyByCoordinate(Artifact artifact, String identifier) {
        // Check if it's a known Jakarta artifact
        if (coordinateMap.containsValue(artifact.groupId() + ":" + artifact.artifactId())) {
            return Namespace.JAKARTA;
        }

        // Check if it maps FROM javax TO jakarta (meaning it's currently javax/incompatible)
        String jakartaEquiv = coordinateMap.get(identifier);
        if (jakartaEquiv != null) {
            // This artifact has a known Jakarta equivalent → it's currently using javax
            return Namespace.JAVAX;
        }

        // Check if the artifact itself IS the Jakarta target of a mapping
        for (Map.Entry<String, String> entry : coordinateMap.entrySet()) {
            if (entry.getValue().equals(identifier)) {
                return Namespace.JAKARTA;
            }
        }

        return Namespace.UNKNOWN;
    }

    /**
     * Classifies based on groupId prefix patterns.
     */
    private Namespace classifyByGroupPrefix(Artifact artifact) {
        String groupId = artifact.groupId();

        if (groupId.startsWith("jakarta.")) {
            return Namespace.JAKARTA;
        }

        if (groupId.startsWith("javax.") && !isJdkProvidedPackage(groupId)) {
            return Namespace.JAVAX;
        }

        return Namespace.UNKNOWN;
    }

    // Exact JDK javax.xml subpackages (SAX/StAX/DOM/XPath etc. are JDK; bind/ws/soap are not)
    private static final Set<String> JDK_JAVAX_XML_SUBPACKAGES = Set.of(
            "parsers", "stream", "xpath", "transform", "validation",
            "namespace", "datatype", "crypto");

    /**
     * JDK-provided javax packages that don't need migration.
     */
    private boolean isJdkProvidedPackage(String groupId) {
        if (groupId.startsWith("javax.management") ||
            groupId.startsWith("javax.naming") ||
            groupId.startsWith("javax.crypto") ||
            groupId.startsWith("javax.net") ||
            groupId.startsWith("javax.script") ||
            groupId.startsWith("javax.sql") ||
            groupId.startsWith("javax.annotation.processing") ||
            groupId.startsWith("javax.lang.model") ||
            groupId.startsWith("javax.tools")) {
            return true;
        }
        if (groupId.startsWith("javax.xml.")) {
            String suffix = groupId.substring("javax.xml.".length());
            int nextDot = suffix.indexOf('.');
            if (nextDot > 0) {
                suffix = suffix.substring(0, nextDot);
            }
            return JDK_JAVAX_XML_SUBPACKAGES.contains(suffix);
        }
        return false;
    }

    /**
     * Scans JAR entries using package rename patterns from recipes.
     */
    private Namespace classifyByJarScan(Artifact artifact) {
        try {
            Optional<java.nio.file.Path> jarPath = jarScanner.resolveJar(artifact);
            if (jarPath.isEmpty()) {
                return Namespace.UNKNOWN;
            }
            JarCompatibilityReport report = jarScanner.analyzeJar(jarPath.get(), null);
            return mapJarLevelToNamespace(report.level());
        } catch (Exception e) {
            log.debug("JAR scan failed for {}: {}", artifact.toCoordinate(), e.getClass().getSimpleName() + ": " + e.getMessage());
            return Namespace.UNKNOWN;
        }
    }

    private Namespace mapJarLevelToNamespace(JarCompatibilityLevel level) {
        return switch (level) {
            case JAVAX -> Namespace.JAVAX;
            case JAKARTA -> Namespace.JAKARTA;
            case MIXED -> Namespace.MIXED;
            case DUAL_COMPATIBLE -> Namespace.JAKARTA;
            case UNKNOWN -> Namespace.UNKNOWN;
        };
    }

    /**
     * Shuts down the scan thread pool.
     */
    public void shutdown() {
        scanExecutor.shutdown();
        try {
            if (!scanExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                scanExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            scanExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
