package adrianmikula.jakartamigration.advancedscanning.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class ClassloaderModuleUsage {
    private final String javaxClass;
    private final String method;
    private final int lineNumber;
    private final String context;
    private final String replacement;
}
