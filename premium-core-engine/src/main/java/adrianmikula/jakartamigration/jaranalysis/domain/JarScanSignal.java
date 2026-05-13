package adrianmikula.jakartamigration.jaranalysis.domain;

import java.util.Map;
import java.util.Objects;

/**
 * Individual signal extracted from JAR analysis.
 * Immutable data holder with validation.
 * Corresponds to TypeSpec: JarScanSignal model
 */
public record JarScanSignal(
    String artifactCoordinate,

    int javaxClassRefs,

    int jakartaClassRefs,

    Map<String, Integer> apiUsage,

    String[] reflectionStrings,

    boolean hasPomMetadata,

    boolean pomIndicatesJavax,

    boolean pomIndicatesJakarta,

    String automaticModuleName,

    boolean hasShadedPackages,

    String[] testOnlyPatterns) {

    /**
     * Constructs a new JarScanSignal with validation.
     */
    public JarScanSignal {
        Objects.requireNonNull(artifactCoordinate, "artifactCoordinate cannot be null");
        Objects.requireNonNull(apiUsage, "apiUsage cannot be null");
        Objects.requireNonNull(reflectionStrings, "reflectionStrings cannot be null");
        Objects.requireNonNull(testOnlyPatterns, "testOnlyPatterns cannot be null");

        if (javaxClassRefs < 0) {
            throw new IllegalArgumentException("javaxClassRefs cannot be negative");
        }
        if (jakartaClassRefs < 0) {
            throw new IllegalArgumentException("jakartaClassRefs cannot be negative");
        }
    }

    /**
     * Returns true if any javax references were detected.
     */
    public boolean hasJavaxSignal() {
        return javaxClassRefs > 0 || pomIndicatesJavax || (automaticModuleName != null && automaticModuleName.contains("javax"));
    }

    /**
     * Returns true if any jakarta references were detected.
     */
    public boolean hasJakartaSignal() {
        return jakartaClassRefs > 0 || pomIndicatesJakarta || (automaticModuleName != null && automaticModuleName.contains("jakarta"));
    }

    /**
     * Returns true if both javax and jakarta signals are present.
     */
    public boolean hasMixedSignal() {
        return hasJavaxSignal() && hasJakartaSignal();
    }

    /**
     * Gets a critical API weight from the signal, if present.
     */
    public int getCriticalApiWeight(String apiCategory) {
        return apiUsage.getOrDefault(apiCategory, 0);
    }

    /**
     * Builder for constructing JarScanSignal instances.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for JarScanSignal.
     */
    public static class Builder {
        private String artifactCoordinate;
        private int javaxClassRefs;
        private int jakartaClassRefs;
        private Map<String, Integer> apiUsage = Map.of();
        private String[] reflectionStrings = new String[0];
        private boolean hasPomMetadata;
        private boolean pomIndicatesJavax;
        private boolean pomIndicatesJakarta;
        private String automaticModuleName;
        private boolean hasShadedPackages;
        private String[] testOnlyPatterns = new String[0];

        public Builder artifactCoordinate(String artifactCoordinate) {
            this.artifactCoordinate = artifactCoordinate;
            return this;
        }

        public Builder javaxClassRefs(int count) {
            this.javaxClassRefs = count;
            return this;
        }

        public Builder jakartaClassRefs(int count) {
            this.jakartaClassRefs = count;
            return this;
        }

        public Builder apiUsage(Map<String, Integer> apiUsage) {
            this.apiUsage = apiUsage != null ? Map.copyOf(apiUsage) : Map.of();
            return this;
        }

        public Builder reflectionStrings(String[] reflectionStrings) {
            this.reflectionStrings = reflectionStrings != null ? reflectionStrings : new String[0];
            return this;
        }

        public Builder hasPomMetadata(boolean hasPomMetadata) {
            this.hasPomMetadata = hasPomMetadata;
            return this;
        }

        public Builder pomIndicatesJavax(boolean pomIndicatesJavax) {
            this.pomIndicatesJavax = pomIndicatesJavax;
            return this;
        }

        public Builder pomIndicatesJakarta(boolean pomIndicatesJakarta) {
            this.pomIndicatesJakarta = pomIndicatesJakarta;
            return this;
        }

        public Builder automaticModuleName(String automaticModuleName) {
            this.automaticModuleName = automaticModuleName;
            return this;
        }

        public Builder hasShadedPackages(boolean hasShadedPackages) {
            this.hasShadedPackages = hasShadedPackages;
            return this;
        }

        public Builder testOnlyPatterns(String[] testOnlyPatterns) {
            this.testOnlyPatterns = testOnlyPatterns != null ? testOnlyPatterns : new String[0];
            return this;
        }

        public JarScanSignal build() {
            return new JarScanSignal(
                artifactCoordinate,
                javaxClassRefs,
                jakartaClassRefs,
                apiUsage,
                reflectionStrings,
                hasPomMetadata,
                pomIndicatesJavax,
                pomIndicatesJakarta,
                automaticModuleName,
                hasShadedPackages,
                testOnlyPatterns
            );
        }
    }
}
