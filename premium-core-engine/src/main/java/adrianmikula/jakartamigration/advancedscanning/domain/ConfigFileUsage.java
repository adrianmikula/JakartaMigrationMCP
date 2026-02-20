package adrianmikula.jakartamigration.advancedscanning.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ConfigFileUsage {
    private final String javaxReference;
    private final String context;
    private final int lineNumber;
    private final String replacement;
    private final String fileType;
}
