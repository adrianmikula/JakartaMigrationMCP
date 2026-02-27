package adrianmikula.jakartamigration.advancedscanning.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public class TransitiveDependencyUsage {
    public TransitiveDependencyUsage() {
        this.artifactId = null;
        this.groupId = null;
        this.version = null;
        this.javaxPackage = null;
        this.severity = null;
        this.recommendation = null;
    }

    public TransitiveDependencyUsage(String artifactId, String groupId, String version,
                                      String javaxPackage, String severity, String recommendation) {
        this.artifactId = artifactId;
        this.groupId = groupId;
        this.version = version;
        this.javaxPackage = javaxPackage;
        this.severity = severity;
        this.recommendation = recommendation;
    }

    private final String artifactId;
    private final String groupId;
    private final String version;
    private final String javaxPackage;
    private final String severity;
    private final String recommendation;
}
