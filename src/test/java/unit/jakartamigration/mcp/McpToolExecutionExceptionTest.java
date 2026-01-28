package unit.jakartamigration.mcp;

import adrianmikula.jakartamigration.mcp.McpToolExecutionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("McpToolExecutionException Tests")
class McpToolExecutionExceptionTest {

    @Test
    void shouldCreateWithMessage() {
        McpToolExecutionException e = new McpToolExecutionException("Tool failed");
        assertThat(e.getMessage()).isEqualTo("Tool failed");
        assertThat(e.getCause()).isNull();
    }

    @Test
    void shouldCreateWithMessageAndCause() {
        Throwable cause = new RuntimeException("underlying");
        McpToolExecutionException e = new McpToolExecutionException("Tool failed", cause);
        assertThat(e.getMessage()).isEqualTo("Tool failed");
        assertThat(e.getCause()).isSameAs(cause);
    }
}
