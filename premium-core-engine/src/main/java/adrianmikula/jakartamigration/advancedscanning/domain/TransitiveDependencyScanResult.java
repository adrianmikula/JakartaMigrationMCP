package adrianmikula.jakartamigration.advancedscanning.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

@Getter
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
public class TransitiveDependencyScanResult {
    private final Path filePath;
    private final List<TransitiveDependencyUsage> usages;
    private final String buildFileType;

    public TransitiveDependencyScanResult(Path filePath, List<TransitiveDependencyUsage> usages, String buildFileType) {
        this.filePath = filePath;
        this.usages = usages != null ? usages : Collections.emptyList();
        this.buildFileType = buildFileType;
    }

    public static TransitiveDependencyScanResult empty(Path filePath) {
        return new TransitiveDependencyScanResult(filePath, Collections.emptyList(), null);
    }

    public boolean hasJavaxUsage() {
        return !usages.isEmpty();
    }
}
