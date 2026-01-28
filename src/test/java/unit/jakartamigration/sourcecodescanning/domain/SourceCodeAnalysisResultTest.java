package unit.jakartamigration.sourcecodescanning.domain;

import adrianmikula.jakartamigration.sourcecodescanning.domain.FileUsage;
import adrianmikula.jakartamigration.sourcecodescanning.domain.ImportStatement;
import adrianmikula.jakartamigration.sourcecodescanning.domain.SourceCodeAnalysisResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SourceCodeAnalysisResult Tests")
class SourceCodeAnalysisResultTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldCreateResult() {
        SourceCodeAnalysisResult r = new SourceCodeAnalysisResult(List.of(), 10, 2, 5);
        assertThat(r.filesWithJavaxUsage()).isEmpty();
        assertThat(r.totalFilesScanned()).isEqualTo(10);
        assertThat(r.totalFilesWithJavaxUsage()).isEqualTo(2);
        assertThat(r.totalJavaxImports()).isEqualTo(5);
        assertThat(r.hasJavaxUsage()).isFalse();
    }

    @Test
    void shouldReportHasJavaxUsageWhenNonEmpty() {
        ImportStatement imp = new ImportStatement("javax.servlet.Servlet", "javax.servlet", "jakarta.servlet.Servlet", 1);
        FileUsage u = new FileUsage(tempDir.resolve("p.java"), List.of(imp), 1);
        SourceCodeAnalysisResult r = new SourceCodeAnalysisResult(List.of(u), 1, 1, 1);
        assertThat(r.hasJavaxUsage()).isTrue();
    }

    @Test
    void emptyFactory() {
        SourceCodeAnalysisResult r = SourceCodeAnalysisResult.empty();
        assertThat(r.filesWithJavaxUsage()).isEmpty();
        assertThat(r.totalFilesScanned()).isZero();
        assertThat(r.totalFilesWithJavaxUsage()).isZero();
        assertThat(r.totalJavaxImports()).isZero();
    }

    @Test
    void shouldRejectNullFiles() {
        assertThatThrownBy(() -> new SourceCodeAnalysisResult(null, 0, 0, 0))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectNegativeTotals() {
        assertThatThrownBy(() -> new SourceCodeAnalysisResult(List.of(), -1, 0, 0))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SourceCodeAnalysisResult(List.of(), 0, -1, 0))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SourceCodeAnalysisResult(List.of(), 0, 0, -1))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
