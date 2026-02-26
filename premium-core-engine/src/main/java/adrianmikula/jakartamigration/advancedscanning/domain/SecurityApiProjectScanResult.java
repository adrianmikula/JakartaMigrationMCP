package adrianmikula.jakartamigration.advancedscanning.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Getter
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecurityApiProjectScanResult {
    private final List<SecurityApiScanResult> fileResults;
    private final int totalFilesScanned;
    private final int filesWithJavaxUsage;
    private final int totalJavaxUsages;

    public static SecurityApiProjectScanResult empty() {
        return new SecurityApiProjectScanResult(Collections.emptyList(), 0, 0, 0);
    }

    @JsonIgnore
    public boolean hasJavaxUsage() {
        return filesWithJavaxUsage > 0;
    }
}
