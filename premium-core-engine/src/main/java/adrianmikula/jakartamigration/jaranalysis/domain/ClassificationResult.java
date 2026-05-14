package adrianmikula.jakartamigration.jaranalysis.domain;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Namespace;

import java.util.List;
import java.util.Objects;

/**
 * Classification result with detailed reasoning.
 * Corresponds to TypeSpec: ClassificationResult model
 */
public record ClassificationResult(
    Artifact artifact,
    Namespace namespace,
    double confidence,
    List<String> reasoning,
    boolean deepScanUsed,
    JarCompatibilityReport report) {

    /**
     * Constructs a new ClassificationResult with validation.
     */
    public ClassificationResult {
        Objects.requireNonNull(artifact, "artifact cannot be null");
        Objects.requireNonNull(namespace, "namespace cannot be null");
        Objects.requireNonNull(reasoning, "reasoning cannot be null");

        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("confidence must be between 0.0 and 1.0");
        }
    }

    /**
     * Creates a builder for constructing ClassificationResult instances.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for ClassificationResult.
     */
    public static class Builder {
        private Artifact artifact;
        private Namespace namespace;
        private double confidence;
        private List<String> reasoning = List.of();
        private boolean deepScanUsed;
        private JarCompatibilityReport report;

        public Builder artifact(Artifact artifact) {
            this.artifact = artifact;
            return this;
        }

        public Builder namespace(Namespace namespace) {
            this.namespace = namespace;
            return this;
        }

        public Builder confidence(double confidence) {
            this.confidence = confidence;
            return this;
        }

        public Builder reasoning(List<String> reasoning) {
            this.reasoning = reasoning;
            return this;
        }

        public Builder deepScanUsed(boolean deepScanUsed) {
            this.deepScanUsed = deepScanUsed;
            return this;
        }

        public Builder report(JarCompatibilityReport report) {
            this.report = report;
            return this;
        }

        public ClassificationResult build() {
            return new ClassificationResult(
                artifact,
                namespace,
                confidence,
                reasoning,
                deepScanUsed,
                report
            );
        }
    }
}