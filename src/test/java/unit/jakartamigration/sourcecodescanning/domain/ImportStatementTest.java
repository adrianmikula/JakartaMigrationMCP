package unit.jakartamigration.sourcecodescanning.domain;

import adrianmikula.jakartamigration.sourcecodescanning.domain.ImportStatement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ImportStatement Tests")
class ImportStatementTest {

    @Test
    void shouldCreateAndExposeFields() {
        ImportStatement imp = new ImportStatement("javax.servlet.Servlet", "javax.servlet", "jakarta.servlet.Servlet", 5);
        assertThat(imp.fullImport()).isEqualTo("javax.servlet.Servlet");
        assertThat(imp.javaxPackage()).isEqualTo("javax.servlet");
        assertThat(imp.jakartaEquivalent()).isEqualTo("jakarta.servlet.Servlet");
        assertThat(imp.lineNumber()).isEqualTo(5);
        assertThat(imp.isJavaxImport()).isTrue();
    }

    @Test
    void shouldDetectJavaxImport() {
        assertThat(new ImportStatement("javax.servlet.X", "javax.servlet", "jakarta.servlet.X", 1).isJavaxImport()).isTrue();
        assertThat(new ImportStatement("java.util.List", "java.util", "java.util.List", 1).isJavaxImport()).isFalse();
    }

    @Test
    void shouldRejectLineNumberBelowOne() {
        assertThatThrownBy(() -> new ImportStatement("x", "y", "z", 0))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
