package adrianmikula.jakartamigration.advancedscanning.domain;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Result of scanning a project for javax.persistence.* usage.
 */
public record JpaScanResult(
    Path filePath,
    List<JpaAnnotationUsage> annotations,
    int lineCount
) {
    public JpaScanResult {
        Objects.requireNonNull(filePath, "filePath cannot be null");
        Objects.requireNonNull(annotations, "annotations cannot be null");
    }

    /**
     * Returns true if any javax.persistence.* usage was found.
     */
    public boolean hasJavaxUsage() {
        return !annotations.isEmpty();
    }

    /**
     * Creates an empty result.
     */
    public static JpaScanResult empty(Path filePath) {
        return new JpaScanResult(filePath, List.of(), 0);
    }
}
