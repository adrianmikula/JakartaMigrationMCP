package adrianmikula.jakartamigration.advancedscanning.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Getter
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
public class TransitiveDependencyScanResult {
    private final Path filePath;
    private final List<TransitiveDependencyUsage> usages;
    private final String buildFileType;
    private final Set<String> scopes;
    private final List<TransitiveDependencyEdge> edges;
    private final String errorMessage;

    public TransitiveDependencyScanResult(Path filePath, List<TransitiveDependencyUsage> usages, String buildFileType) {
        this(filePath, usages, buildFileType, Collections.emptySet(), Collections.emptyList(), null);
    }

    public TransitiveDependencyScanResult(Path filePath, List<TransitiveDependencyUsage> usages, String buildFileType, Set<String> scopes) {
        this(filePath, usages, buildFileType, scopes, Collections.emptyList(), null);
    }

    public TransitiveDependencyScanResult(Path filePath, List<TransitiveDependencyUsage> usages, String buildFileType,
                                           Set<String> scopes, List<TransitiveDependencyEdge> edges) {
        this(filePath, usages, buildFileType, scopes, edges, null);
    }

    public TransitiveDependencyScanResult(Path filePath, List<TransitiveDependencyUsage> usages, String buildFileType,
                                           Set<String> scopes, List<TransitiveDependencyEdge> edges, String errorMessage) {
        this.filePath = filePath;
        this.usages = usages != null ? usages : Collections.emptyList();
        this.buildFileType = buildFileType;
        this.scopes = scopes != null ? scopes : Collections.emptySet();
        this.edges = edges != null ? edges : Collections.emptyList();
        this.errorMessage = errorMessage;
    }

    public static TransitiveDependencyScanResult empty(Path filePath) {
        return new TransitiveDependencyScanResult(filePath, Collections.emptyList(), null, Collections.emptySet(), Collections.emptyList(), null);
    }

    public boolean hasError() {
        return errorMessage != null && !errorMessage.isEmpty();
    }

    public boolean hasJavaxUsage() {
        return !usages.isEmpty();
    }
}
