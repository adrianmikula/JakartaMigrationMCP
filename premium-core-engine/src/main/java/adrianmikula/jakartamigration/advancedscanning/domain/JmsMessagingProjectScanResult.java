package adrianmikula.jakartamigration.advancedscanning.domain;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Getter
public class JmsMessagingProjectScanResult {
    private final List<JmsMessagingScanResult> fileResults;
    private final int totalFilesScanned;
    private final int filesWithJavaxUsage;
    private final int totalJavaxUsages;

    public JmsMessagingProjectScanResult(List<JmsMessagingScanResult> fileResults, int totalFilesScanned, int filesWithJavaxUsage, int totalJavaxUsages) {
        this.fileResults = fileResults != null ? fileResults : Collections.emptyList();
        this.totalFilesScanned = totalFilesScanned;
        this.filesWithJavaxUsage = filesWithJavaxUsage;
        this.totalJavaxUsages = totalJavaxUsages;
    }

    public static JmsMessagingProjectScanResult empty() {
        return new JmsMessagingProjectScanResult(Collections.emptyList(), 0, 0, 0);
    }

    public boolean hasJavaxUsage() {
        return filesWithJavaxUsage > 0;
    }
}
