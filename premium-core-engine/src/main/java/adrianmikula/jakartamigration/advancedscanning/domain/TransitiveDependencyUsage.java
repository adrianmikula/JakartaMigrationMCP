package adrianmikula.jakartamigration.advancedscanning.domain;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Getter
public class TransitiveDependencyUsage {
    private final String artifactId;
    private final String groupId;
    private final String version;
    private final String javaxPackage;
    private final String severity;
    private final String recommendation;
    private final String scope;
    private final boolean transitive;
    private final int depth;
    private final List<String> alternativeVersions;
    private final ScanReason scanReason;
    private final String detailMessage;
    private final double confidence;
    private final boolean incompatibilityFromTransitive;

    public TransitiveDependencyUsage(String artifactId, String groupId, String version,
                                       String javaxPackage, String severity, String recommendation) {
        this(artifactId, groupId, version, javaxPackage, severity, recommendation, null, false, 0, null);
    }

    public TransitiveDependencyUsage(String artifactId, String groupId, String version,
                                       String javaxPackage, String severity, String recommendation,
                                       String scope, boolean transitive, int depth) {
        this(artifactId, groupId, version, javaxPackage, severity, recommendation, scope, transitive, depth, null);
    }

    public TransitiveDependencyUsage(String artifactId, String groupId, String version,
                                       String javaxPackage, String severity, String recommendation,
                                       String scope, boolean transitive, int depth, List<String> alternativeVersions) {
        this(artifactId, groupId, version, javaxPackage, severity, recommendation, scope, transitive, depth, 
             alternativeVersions, null, null, 0.0, false);
    }

    public TransitiveDependencyUsage(String artifactId, String groupId, String version,
                                       String javaxPackage, String severity, String recommendation,
                                       String scope, boolean transitive, int depth, List<String> alternativeVersions,
                                       ScanReason scanReason, String detailMessage, double confidence,
                                       boolean incompatibilityFromTransitive) {
        this.artifactId = artifactId;
        this.groupId = groupId;
        this.version = version;
        this.javaxPackage = javaxPackage;
        this.severity = severity;
        this.recommendation = recommendation;
        this.scope = scope;
        this.transitive = transitive;
        this.depth = depth;
        this.alternativeVersions = alternativeVersions != null ? alternativeVersions : Collections.emptyList();
        this.scanReason = scanReason;
        this.detailMessage = detailMessage;
        this.confidence = confidence;
        this.incompatibilityFromTransitive = incompatibilityFromTransitive;
    }

    /**
     * Returns the artifact key (groupId:artifactId) for deduplication.
     */
    public String getArtifactKey() {
        return groupId + ":" + artifactId;
    }
}
