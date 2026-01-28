package unit.jakartamigration.sourcecodescanning.domain;

import adrianmikula.jakartamigration.sourcecodescanning.domain.FileUsage;
import adrianmikula.jakartamigration.sourcecodescanning.domain.ImportStatement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FileUsage Tests")
class FileUsageTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldCreateAndExposeFields() {
        Path p = tempDir.resolve("Test.java");
        ImportStatement imp = new ImportStatement("javax.servlet.Servlet", "javax.servlet", "jakarta.servlet.Servlet", 1);
        FileUsage u = new FileUsage(p, List.of(imp), 10);
        assertThat(u.filePath()).isEqualTo(p);
        assertThat(u.javaxImports()).hasSize(1);
        assertThat(u.lineCount()).isEqualTo(10);
        assertThat(u.hasJavaxUsage()).isTrue();
        assertThat(u.getJavaxImportCount()).isOne();
    }

    @Test
    void shouldReportNoJavaxUsageWhenEmptyImports() {
        FileUsage u = new FileUsage(tempDir.resolve("Empty.java"), List.of(), 0);
        assertThat(u.hasJavaxUsage()).isFalse();
        assertThat(u.getJavaxImportCount()).isZero();
    }

    @Test
    void shouldRejectNullPath() {
        assertThatThrownBy(() -> new FileUsage(null, List.of(), 0))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectNegativeLineCount() {
        assertThatThrownBy(() -> new FileUsage(tempDir.resolve("x.java"), List.of(), -1))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
