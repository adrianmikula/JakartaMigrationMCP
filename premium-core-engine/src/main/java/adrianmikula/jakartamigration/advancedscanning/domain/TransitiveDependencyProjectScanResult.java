package adrianmikula.jakartamigration.advancedscanning.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Getter
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
public class TransitiveDependencyProjectScanResult {
    private final List<TransitiveDependencyScanResult> fileResults;
    private final int totalBuildFilesScanned;
    private final int filesWithJavaxDependencies;
    private final int totalJavaxDependencies;

    public TransitiveDependencyProjectScanResult(List<TransitiveDependencyScanResult> fileResults,
            int totalBuildFilesScanned, int filesWithJavaxDependencies, int totalJavaxDependencies) {
        this.fileResults = fileResults != null ? fileResults : Collections.emptyList();
        this.totalBuildFilesScanned = totalBuildFilesScanned;
        this.filesWithJavaxDependencies = filesWithJavaxDependencies;
        this.totalJavaxDependencies = totalJavaxDependencies;
    }

    public static TransitiveDependencyProjectScanResult empty() {
        return new TransitiveDependencyProjectScanResult(Collections.emptyList(), 0, 0, 0);
    }

    public boolean hasJavaxDependencies() {
        return filesWithJavaxDependencies > 0;
    }
}
