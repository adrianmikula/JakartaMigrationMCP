package adrianmikula.jakartamigration.jaranalysis.domain;

import java.util.List;
import java.util.Objects;

/**
 * Complete JAR compatibility assessment result.
 * Immutable data holder.
 * Corresponds to TypeSpec: JarCompatibilityReport model
 */
public record JarCompatibilityReport(
    String artifactCoordinate,
    JarCompatibilityLevel level,
    double confidence,
    List<String> reasons,
    JarScanSignal signal,
    long analysisTimeMs,
    boolean cached) {

    /**
     * Constructs a new JarCompatibilityReport with validation.
     */
    public JarCompatibilityReport {
        Objects.requireNonNull(artifactCoordinate, "artifactCoordinate cannot be null");
        Objects.requireNonNull(level, "level cannot be null");
        Objects.requireNonNull(reasons, "reasons cannot be null");
        Objects.requireNonNull(signal, "signal cannot be null");

        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("confidence must be between 0.0 and 1.0");
        }
        if (analysisTimeMs < 0) {
            throw new IllegalArgumentException("analysisTimeMs cannot be negative");
        }
    }

    /**
     * Returns true if this result was obtained from cache.
     */
    public boolean isCached() {
        return cached;
    }

    /**
     * Creates a builder for constructing JarCompatibilityReport instances.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for JarCompatibilityReport.
     */
    public static class Builder {
        private String artifactCoordinate;
        private JarCompatibilityLevel level;
        private double confidence = 0.5;
        private List<String> reasons = List.of();
        private JarScanSignal signal;
        private long analysisTimeMs;
        private boolean cached;

        public Builder artifactCoordinate(String artifactCoordinate) {
            this.artifactCoordinate = artifactCoordinate;
            return this;
        }

        public Builder level(JarCompatibilityLevel level) {
            this.level = level;
            return this;
        }

        public Builder confidence(double confidence) {
            this.confidence = confidence;
            return this;
        }

        public Builder reasons(List<String> reasons) {
            this.reasons = reasons;
            return this;
        }

        public Builder signal(JarScanSignal signal) {
            this.signal = signal;
            return this;
        }

        public Builder analysisTimeMs(long analysisTimeMs) {
            this.analysisTimeMs = analysisTimeMs;
            return this;
        }

        public Builder cached(boolean cached) {
            this.cached = cached;
            return this;
        }

        public JarCompatibilityReport build() {
            return new JarCompatibilityReport(
                artifactCoordinate,
                level,
                confidence,
                reasons,
                signal,
                analysisTimeMs,
                cached
            );
        }
    }
}
