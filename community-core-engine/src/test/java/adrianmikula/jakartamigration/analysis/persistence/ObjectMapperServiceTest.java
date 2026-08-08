package adrianmikula.jakartamigration.analysis.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression tests for ObjectMapperService.
 * Verifies that java.nio.file.Path and generic record types round-trip correctly,
 * which is required for restoring saved source scan results in the IDE plugin.
 */
class ObjectMapperServiceTest {

    private ObjectMapperService objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapperService();
    }

    @Test
    @DisplayName("Should round-trip a Path as a string")
    void shouldRoundTripPath() {
        Path original = Path.of("src/main/java/com/example/Test.java");

        String json = objectMapper.toJson(original);
        Path result = objectMapper.fromJson(json, Path.class);

        assertThat(result).isEqualTo(original);
    }

    @Test
    @DisplayName("Should round-trip generic types using TypeReference")
    void shouldRoundTripGenericTypes() {
        TestUsage usage = new TestUsage("javax.persistence.Entity", "jakarta.persistence.Entity", 42);
        TestFileResult<TestUsage> fileResult = new TestFileResult<>(
                Path.of("Test.java"), List.of(usage), 100);
        TestProjectResult<TestFileResult<TestUsage>> projectResult = new TestProjectResult<>(
                List.of(fileResult), 1, 1, 1);

        String json = objectMapper.toJson(projectResult);
        TestProjectResult<TestFileResult<TestUsage>> result =
                objectMapper.fromJson(json, new TypeReference<>() {});

        assertThat(result.fileResults()).hasSize(1);
        assertThat(result.fileResults().get(0).usages().get(0).lineNumber()).isEqualTo(42);
        assertThat(result.fileResults().get(0).filePath()).isEqualTo(Path.of("Test.java"));
    }

    private record TestUsage(String className, String jakartaEquivalent, int lineNumber) {}

    private record TestFileResult<T>(Path filePath, List<T> usages, int lineCount) {}

    private record TestProjectResult<T>(
            List<T> fileResults,
            int totalFilesScanned,
            int filesWithIssues,
            int totalIssuesFound
    ) {}
}
