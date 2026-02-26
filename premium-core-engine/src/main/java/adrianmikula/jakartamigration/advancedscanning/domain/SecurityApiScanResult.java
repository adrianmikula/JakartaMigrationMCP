package adrianmikula.jakartamigration.advancedscanning.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecurityApiScanResult {
    private final Path filePath;
    private final List<SecurityApiUsage> usages;
    private final int lineCount;

    public static SecurityApiScanResult empty(Path filePath) {
        return new SecurityApiScanResult(filePath, Collections.emptyList(), 0);
    }

    @JsonIgnore
    public boolean hasJavaxUsage() {
        return !usages.isEmpty();
    }
}
