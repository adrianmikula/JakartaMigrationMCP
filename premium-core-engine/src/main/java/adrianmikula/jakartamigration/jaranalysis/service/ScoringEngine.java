package adrianmikula.jakartamigration.jaranalysis.service;

import adrianmikula.jakartamigration.jaranalysis.config.JarScanningConfig;
import adrianmikula.jakartamigration.jaranalysis.domain.JarCompatibilityLevel;
import adrianmikula.jakartamigration.jaranalysis.domain.JarScanSignal;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Scores JAR scan signals to produce compatibility classifications.
 * 
 * Uses configurable weights from JarScanningConfig to compute a weighted
 * score, then determines the JarCompatibilityLevel based on thresholds.
 */
@Slf4j
public class ScoringEngine {

    private final JarScanningConfig config;

    /**
     * Creates a scoring engine with the default configuration.
     */
    public ScoringEngine() {
        this(JarScanningConfig.get());
    }

    /**
     * Creates a scoring engine with a specific configuration.
     */
    public ScoringEngine(JarScanningConfig config) {
        this.config = Objects.requireNonNull(config, "config cannot be null");
    }

    /**
     * Scores a JAR scan signal and produces a classification result.
     * 
     * @param signal The scanned signal data
     * @param artifactCoordinate The artifact coordinate for reporting
     * @return ScoringResult containing level, confidence, and reasoning
     */
    public ScoringResult score(JarScanSignal signal, String artifactCoordinate) {
        Objects.requireNonNull(signal, "signal cannot be null");

        long startTime = System.nanoTime();
        double score = 0.0;
        List<String> reasons = new ArrayList<>();

        // --- Base reference counts ---
        int javaxCount = signal.javaxClassRefs();
        int jakartaCount = signal.jakartaClassRefs();

        double javaxScore = javaxCount * config.getJavaxClassRefWeight();
        double jakartaScore = jakartaCount * config.getJakartaClassRefWeight();
        score += javaxScore + jakartaScore;

        if (javaxCount > 0) {
            reasons.add(String.format(
                "%d javax class references (weight: %d, contribution: %.1f)",
                javaxCount, config.getJavaxClassRefWeight(), javaxScore));
        }
        if (jakartaCount > 0) {
            reasons.add(String.format(
                "%d jakarta class references (weight: %d, contribution: %.1f)",
                jakartaCount, config.getJakartaClassRefWeight(), jakartaScore));
        }

        // --- Critical API usage (multipliers) ---
        double apiContribution = 0.0;
        for (Map.Entry<String, Integer> entry : signal.apiUsage().entrySet()) {
            String apiCategory = entry.getKey();
            int count = entry.getValue();
            double multiplier = config.getApiCriticalityWeights()
                .getOrDefault(apiCategory, 1.0);
            double apiScore = count * multiplier;
            apiContribution += apiScore;
            
            // Determine direction based on package presence
            boolean isJavax = isJavaxApi(apiCategory, signal);
            if (isJavax) {
                apiScore = -Math.abs(apiScore);
            }
            score += apiScore;
            
            if (count > 0) {
                reasons.add(String.format(
                    "API '%s' used %d times (criticality: %.1fx, contribution: %.1f)",
                    apiCategory, count, multiplier, apiScore));
            }
        }

        // --- Metadata signals ---
        if (signal.hasPomMetadata()) {
            if (signal.pomIndicatesJavax()) {
                score += config.getJavaxMetadataWeight();
                reasons.add(String.format(
                    "POM indicates javax dependencies (weight: %d)",
                    config.getJavaxMetadataWeight()));
            }
            if (signal.pomIndicatesJakarta()) {
                score += config.getJakartaMetadataWeight();
                reasons.add(String.format(
                    "POM indicates jakarta dependencies (weight: %d)",
                    config.getJakartaMetadataWeight()));
            }
        }

        String moduleName = signal.automaticModuleName();
        if (moduleName != null && !moduleName.isEmpty()) {
            if (moduleName.toLowerCase().contains("javax")) {
                score += config.getJavaxMetadataWeight();
                reasons.add(String.format(
                    "Automatic-Module-Name contains 'javax' (weight: %d)",
                    config.getJavaxMetadataWeight()));
            } else if (moduleName.toLowerCase().contains("jakarta")) {
                score += config.getJakartaMetadataWeight();
                reasons.add(String.format(
                    "Automatic-Module-Name contains 'jakarta' (weight: %d)",
                    config.getJakartaMetadataWeight()));
            }
        }

        // --- Reflection strings ---
        for (String refl : signal.reflectionStrings()) {
            if (refl.contains("javax")) {
                score += config.getJavaxReflectionWeight();
                reasons.add(String.format(
                    "Reflection string contains 'javax' (weight: %d)",
                    config.getJavaxReflectionWeight()));
                break;
            }
        }
        for (String refl : signal.reflectionStrings()) {
            if (refl.contains("jakarta")) {
                score += config.getJakartaReflectionWeight();
                reasons.add(String.format(
                    "Reflection string contains 'jakarta' (weight: %d)",
                    config.getJakartaReflectionWeight()));
                break;
            }
        }

        // --- Determine level based on thresholds ---
        JarCompatibilityLevel level = determineLevel(score, signal);

        // --- Confidence calculation ---
        double confidence = calculateConfidence(score, signal, level);

        // --- Mixed/Mixed signal warnings ---
        if (signal.hasMixedSignal()) {
            reasons.add("Both javax and jakarta signals detected (mixed namespace warning)");
        }
        if (signal.testOnlyPatterns().length > 0) {
            reasons.add("Test-only patterns detected (" + 
                String.join(", ", signal.testOnlyPatterns()) + ")");
        }
        if (signal.hasShadedPackages()) {
            reasons.add("Shaded/relocated packages detected - may affect analysis accuracy");
        }

        long durationMs = (System.nanoTime() - startTime) / 1_000_000;

        return new ScoringResult(
            level,
            confidence,
            reasons,
            Math.max(1, durationMs),
            score
        );
    }

    /**
     * Determines the compatibility level from the raw score.
     */
    private JarCompatibilityLevel determineLevel(double score, JarScanSignal signal) {
        // If both javax and jakarta signals are present, prefer MIXED
        // unless one direction strongly dominates
        if (signal.hasMixedSignal()) {
            double absScore = Math.abs(score);
            if (absScore >= config.getJakartaThreshold()) {
                return score > 0 ? JarCompatibilityLevel.JAKARTA : JarCompatibilityLevel.JAVAX;
            } else if (absScore <= config.getMixedMaxThreshold()) {
                return JarCompatibilityLevel.MIXED;
            }
        }

        // Pure signal cases
        if (score >= config.getJakartaThreshold()) {
            return JarCompatibilityLevel.JAKARTA;
        } else if (score <= config.getJavaxThreshold()) {
            return JarCompatibilityLevel.JAVAX;
        } else if (signal.hasJavaxSignal() && signal.hasJakartaSignal()) {
            return JarCompatibilityLevel.MIXED;
        } else if (!signal.hasJavaxSignal() && !signal.hasJakartaSignal()) {
            return JarCompatibilityLevel.UNKNOWN;
        }

        // Weak signal in one direction
        return JarCompatibilityLevel.UNKNOWN;
    }

    /**
     * Calculates confidence score (0.0 to 1.0).
     * Higher absolute scores and stronger signal presence increase confidence.
     */
    private double calculateConfidence(double score, JarScanSignal signal, JarCompatibilityLevel level) {
        double absScore = Math.abs(score);
        
        // Base confidence from score magnitude (sigmoid-like, capped at ~0.7)
        double scoreConfidence = Math.min(absScore / 20.0, 0.7);

        // Signal strength factor
        int totalRelevantRefs = signal.javaxClassRefs() + signal.jakartaClassRefs();
        double signalFactor = totalRelevantRefs > 0 ? 
            Math.min(totalRelevantRefs / 20.0, 0.3) : 0.0;

        // API criticality factor
        double apiFactor = Math.min(
            signal.apiUsage().values().stream().mapToInt(Integer::intValue).sum() / 20.0,
            0.2
        );

        // Metadata bonus
        double metaBonus = 0.0;
        if (signal.hasPomMetadata()) metaBonus += 0.05;
        if (signal.automaticModuleName() != null && !signal.automaticModuleName().isEmpty()) {
            metaBonus += 0.03;
        }

        double confidence = scoreConfidence + signalFactor + apiFactor + metaBonus;

        // Mixed signals reduce confidence slightly
        if (signal.hasMixedSignal()) {
            confidence *= 0.85;
        }

        return Math.min(Math.max(confidence, 0.0), 1.0);
    }

    /**
     * Checks if an API category corresponds to javax namespace.
     */
    private boolean isJavaxApi(String apiCategory, JarScanSignal signal) {
        return signal.javaxClassRefs() > signal.jakartaClassRefs();
    }

    /**
     * Result of scoring a JAR signal.
     */
    public record ScoringResult(
        JarCompatibilityLevel level,
        double confidence,
        List<String> reasons,
        long analysisTimeMs,
        double rawScore
    ) {}
}
