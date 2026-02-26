package adrianmikula.jakartamigration.advancedscanning.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
public class SecurityApiUsage {
    private final String javaxClass;
    private final String method;
    private final String jakartaEquivalent;
    private final int lineNumber;
    private final String context;
}
