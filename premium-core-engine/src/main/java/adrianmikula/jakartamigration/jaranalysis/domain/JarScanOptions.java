package adrianmikula.jakartamigration.jaranalysis.domain;

/**
 * Options controlling JAR scanning behavior.
 * Immutable configuration holder with sensible defaults.
 * Corresponds to TypeSpec: JarScanOptions model
 */
public record JarScanOptions(
    boolean analyzeMetadata,
    boolean analyzeReflection,
    boolean earlyExitEnabled,
    int earlyExitThreshold,
    boolean detectShaded,
    boolean detectTestScope,
    int maxClassesPerJar) {

    /**
     * Default options matching the spec defaults.
     */
    public static final JarScanOptions DEFAULT = new JarScanOptions(
        true,   // analyzeMetadata
        true,   // analyzeReflection
        true,   // earlyExitEnabled
        10,     // earlyExitThreshold
        false,  // detectShaded
        true,   // detectTestScope
        0       // maxClassesPerJar (0 = unlimited)
    );

    /**
     * Validates options on construction.
     */
    public JarScanOptions {
        if (earlyExitThreshold < 0) {
            throw new IllegalArgumentException("earlyExitThreshold cannot be negative");
        }
        if (maxClassesPerJar < 0) {
            throw new IllegalArgumentException("maxClassesPerJar cannot be negative");
        }
    }

    /**
     * Creates options with all defaults.
     */
    public static JarScanOptions defaults() {
        return DEFAULT;
    }

    /**
     * Creates a customized copy of these options.
     */
    public JarScanOptions withAnalyzeMetadata(boolean analyzeMetadata) {
        return new JarScanOptions(
            analyzeMetadata,
            this.analyzeReflection,
            this.earlyExitEnabled,
            this.earlyExitThreshold,
            this.detectShaded,
            this.detectTestScope,
            this.maxClassesPerJar
        );
    }

    /**
     * Creates a customized copy of these options.
     */
    public JarScanOptions withAnalyzeReflection(boolean analyzeReflection) {
        return new JarScanOptions(
            this.analyzeMetadata,
            analyzeReflection,
            this.earlyExitEnabled,
            this.earlyExitThreshold,
            this.detectShaded,
            this.detectTestScope,
            this.maxClassesPerJar
        );
    }

    /**
     * Creates a customized copy of these options.
     */
    public JarScanOptions withEarlyExit(boolean enabled, int threshold) {
        return new JarScanOptions(
            this.analyzeMetadata,
            this.analyzeReflection,
            enabled,
            threshold,
            this.detectShaded,
            this.detectTestScope,
            this.maxClassesPerJar
        );
    }

    /**
     * Creates a customized copy of these options.
     */
    public JarScanOptions withDetectShaded(boolean detectShaded) {
        return new JarScanOptions(
            this.analyzeMetadata,
            this.analyzeReflection,
            this.earlyExitEnabled,
            this.earlyExitThreshold,
            detectShaded,
            this.detectTestScope,
            this.maxClassesPerJar
        );
    }

    /**
     * Creates a customized copy of these options.
     */
    public JarScanOptions withDetectTestScope(boolean detectTestScope) {
        return new JarScanOptions(
            this.analyzeMetadata,
            this.analyzeReflection,
            this.earlyExitEnabled,
            this.earlyExitThreshold,
            this.detectShaded,
            detectTestScope,
            this.maxClassesPerJar
        );
    }

    /**
     * Creates a customized copy of these options.
     */
    public JarScanOptions withMaxClassesPerJar(int maxClassesPerJar) {
        return new JarScanOptions(
            this.analyzeMetadata,
            this.analyzeReflection,
            this.earlyExitEnabled,
            this.earlyExitThreshold,
            this.detectShaded,
            this.detectTestScope,
            maxClassesPerJar
        );
    }
}
