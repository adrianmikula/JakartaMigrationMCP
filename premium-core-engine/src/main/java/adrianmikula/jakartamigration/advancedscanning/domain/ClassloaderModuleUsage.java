package adrianmikula.jakartamigration.advancedscanning.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ClassloaderModuleUsage {
    private final String javaxClass;
    private final String method;
    private final int lineNumber;
    private final String context;
    private final String replacement;
}
