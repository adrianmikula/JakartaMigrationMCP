package adrianmikula.jakartamigration.advancedscanning.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

@Getter
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class ConfigFileScanResult {
    private final Path filePath;
    private final List<ConfigFileUsage> usages;
    private final String fileType;

    public ConfigFileScanResult(Path filePath, List<ConfigFileUsage> usages, String fileType) {
        this.filePath = filePath;
        this.usages = usages != null ? usages : Collections.emptyList();
        this.fileType = fileType;
    }

    public static ConfigFileScanResult empty(Path filePath) {
        return new ConfigFileScanResult(filePath, Collections.emptyList(), null);
    }

    public boolean hasJavaxUsage() {
        return !usages.isEmpty();
    }
}
