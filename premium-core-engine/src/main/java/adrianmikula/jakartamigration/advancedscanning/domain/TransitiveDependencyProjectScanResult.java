package adrianmikula.jakartamigration.advancedscanning.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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

    /**
     * Returns all edges from all file results, aggregated into a single list.
     * Edges represent parent-child relationships in the dependency tree.
     */
    public List<TransitiveDependencyEdge> getAllEdges() {
        return fileResults.stream()
                .flatMap(fr -> fr.getEdges().stream())
                .collect(Collectors.toList());
    }

    public boolean hasJavaxDependencies() {
        return filesWithJavaxDependencies > 0;
    }
}
