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
public class JmsMessagingScanResult {
    private final Path filePath;
    private final List<JmsMessagingUsage> usages;
    private final int lineCount;

    public JmsMessagingScanResult(Path filePath, List<JmsMessagingUsage> usages, int lineCount) {
        this.filePath = filePath;
        this.usages = usages != null ? usages : Collections.emptyList();
        this.lineCount = lineCount;
    }

    public static JmsMessagingScanResult empty(Path filePath) {
        return new JmsMessagingScanResult(filePath, Collections.emptyList(), 0);
    }

    public boolean hasJavaxUsage() {
        return !usages.isEmpty();
    }
}
