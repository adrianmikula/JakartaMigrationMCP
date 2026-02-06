package adrianmikula.jakartamigration.sourcecodescanning.domain;

import java.util.List;
import java.util.Objects;

/**
 * Result of scanning a project for javax.* usage in source code.
 */
public record SourceCodeAnalysisResult(
    List<FileUsage> filesWithJavaxUsage,
    int totalFilesScanned,
    int totalFilesWithJavaxUsage,
    int totalJavaxImports
) {
    public SourceCodeAnalysisResult {
        Objects.requireNonNull(filesWithJavaxUsage, "filesWithJavaxUsage cannot be null");
        if (totalFilesScanned < 0) {
            throw new IllegalArgumentException("totalFilesScanned cannot be negative");
        }
        if (totalFilesWithJavaxUsage < 0) {
            throw new IllegalArgumentException("totalFilesWithJavaxUsage cannot be negative");
        }
        if (totalJavaxImports < 0) {
            throw new IllegalArgumentException("totalJavaxImports cannot be negative");
        }
    }
    
    /**
     * Returns true if any javax.* usage was found.
     */
    public boolean hasJavaxUsage() {
        return !filesWithJavaxUsage.isEmpty();
    }
    
    /**
     * Creates an empty result.
     */
    public static SourceCodeAnalysisResult empty() {
        return new SourceCodeAnalysisResult(List.of(), 0, 0, 0);
    }
}

