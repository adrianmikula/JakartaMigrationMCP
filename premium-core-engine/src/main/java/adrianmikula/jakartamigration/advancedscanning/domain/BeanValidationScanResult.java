package adrianmikula.jakartamigration.advancedscanning.domain;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Result of scanning a file for javax.validation.* usage.
 */
public record BeanValidationScanResult(
    Path filePath,
    List<BeanValidationUsage> annotations,
    int lineCount
) {
    public BeanValidationScanResult {
        Objects.requireNonNull(filePath, "filePath cannot be null");
        Objects.requireNonNull(annotations, "annotations cannot be null");
    }

    /**
     * Returns true if any javax.validation.* usage was found.
     */
    public boolean hasJavaxUsage() {
        return !annotations.isEmpty();
    }

    /**
     * Creates an empty result.
     */
    public static BeanValidationScanResult empty(Path filePath) {
        return new BeanValidationScanResult(filePath, List.of(), 0);
    }
}
