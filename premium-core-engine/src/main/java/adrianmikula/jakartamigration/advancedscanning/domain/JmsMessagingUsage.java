package adrianmikula.jakartamigration.advancedscanning.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class JmsMessagingUsage {
    private final String javaxClass;
    private final String method;
    private final String jakartaEquivalent;
    private final int lineNumber;
    private final String context;
}
