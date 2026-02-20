package adrianmikula.jakartamigration.advancedscanning.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TransitiveDependencyUsage {
    private final String artifactId;
    private final String groupId;
    private final String version;
    private final String javaxPackage;
    private final String severity;
    private final String recommendation;
}
