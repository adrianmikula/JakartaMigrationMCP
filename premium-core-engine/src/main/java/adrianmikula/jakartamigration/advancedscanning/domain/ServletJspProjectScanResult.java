package adrianmikula.jakartamigration.advancedscanning.domain;

import java.util.List;
import java.util.Objects;

/**
 * Aggregated result of scanning a project for javax.servlet.*, javax.servlet.jsp.*, and EL usage.
 */
public record ServletJspProjectScanResult(
    List<ServletJspScanResult> fileResults,
    int totalFilesScanned,
    int totalFilesWithJavaxUsage,
    int totalUsagesFound
) {
    public ServletJspProjectScanResult {
        Objects.requireNonNull(fileResults, "fileResults cannot be null");
    }

    /**
     * Returns true if any javax.servlet.* usage was found.
     */
    public boolean hasJavaxUsage() {
        return totalUsagesFound > 0;
    }

    /**
     * Creates an empty result.
     */
    public static ServletJspProjectScanResult empty() {
        return new ServletJspProjectScanResult(List.of(), 0, 0, 0);
    }
}
