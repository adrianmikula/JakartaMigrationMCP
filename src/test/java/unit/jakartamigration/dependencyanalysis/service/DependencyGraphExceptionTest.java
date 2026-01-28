package unit.jakartamigration.dependencyanalysis.service;

import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DependencyGraphException Tests")
class DependencyGraphExceptionTest {

    @Test
    void shouldCreateWithMessage() {
        DependencyGraphException e = new DependencyGraphException("Parse failed");
        assertThat(e.getMessage()).isEqualTo("Parse failed");
        assertThat(e.getCause()).isNull();
    }

    @Test
    void shouldCreateWithMessageAndCause() {
        Throwable cause = new IllegalArgumentException("bad pom");
        DependencyGraphException e = new DependencyGraphException("Parse failed", cause);
        assertThat(e.getMessage()).isEqualTo("Parse failed");
        assertThat(e.getCause()).isSameAs(cause);
    }
}
