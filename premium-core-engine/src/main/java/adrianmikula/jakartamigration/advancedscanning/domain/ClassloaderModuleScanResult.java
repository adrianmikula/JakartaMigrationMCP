package adrianmikula.jakartamigration.advancedscanning.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

@Getter
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
public class ClassloaderModuleScanResult {
    private final Path filePath;
    private final List<ClassloaderModuleUsage> usages;
    private final int lineCount;

    public ClassloaderModuleScanResult(Path filePath, List<ClassloaderModuleUsage> usages, int lineCount) {
        this.filePath = filePath;
        this.usages = usages != null ? usages : Collections.emptyList();
        this.lineCount = lineCount;
    }

    public static ClassloaderModuleScanResult empty(Path filePath) {
        return new ClassloaderModuleScanResult(filePath, Collections.emptyList(), 0);
    }

    public boolean hasJavaxUsage() {
        return !usages.isEmpty();
    }
}
