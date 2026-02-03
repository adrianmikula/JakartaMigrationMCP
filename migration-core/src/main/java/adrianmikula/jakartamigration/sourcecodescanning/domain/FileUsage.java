package adrianmikula.jakartamigration.sourcecodescanning.domain;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Represents javax.* usage in a single file.
 */
public record FileUsage(
    Path filePath,
    List<ImportStatement> javaxImports,
    int lineCount
) {
    public FileUsage {
        Objects.requireNonNull(filePath, "filePath cannot be null");
        Objects.requireNonNull(javaxImports, "javaxImports cannot be null");
        if (lineCount < 0) {
            throw new IllegalArgumentException("lineCount cannot be negative");
        }
    }
    
    /**
     * Returns true if this file has any javax.* usage.
     */
    public boolean hasJavaxUsage() {
        return !javaxImports.isEmpty();
    }
    
    /**
     * Returns the number of javax imports found.
     */
    public int getJavaxImportCount() {
        return javaxImports.size();
    }
}

