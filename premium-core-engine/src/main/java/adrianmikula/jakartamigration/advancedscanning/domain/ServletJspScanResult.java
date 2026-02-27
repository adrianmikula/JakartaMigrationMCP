package adrianmikula.jakartamigration.advancedscanning.domain;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Result of scanning a file for javax.servlet.*, javax.servlet.jsp.*, or EL usage.
 */
public record ServletJspScanResult(
    Path filePath,
    List<ServletJspUsage> usages,
    int lineCount
) {
    public ServletJspScanResult {
        Objects.requireNonNull(filePath, "filePath cannot be null");
        Objects.requireNonNull(usages, "usages cannot be null");
    }

    /**
     * Returns true if any javax.servlet.* usage was found.
     */
    public boolean hasJavaxUsage() {
        return !usages.isEmpty();
    }

    /**
     * Creates an empty result.
     */
    public static ServletJspScanResult empty(Path filePath) {
        return new ServletJspScanResult(filePath, List.of(), 0);
    }
}
