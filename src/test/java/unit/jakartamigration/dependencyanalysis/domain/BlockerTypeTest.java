package unit.jakartamigration.dependencyanalysis.domain;

import adrianmikula.jakartamigration.dependencyanalysis.domain.BlockerType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BlockerType Tests")
class BlockerTypeTest {

    @Test
    void shouldHaveExpectedValues() {
        assertThat(BlockerType.NO_JAKARTA_EQUIVALENT).isNotNull();
        assertThat(BlockerType.TRANSITIVE_CONFLICT).isNotNull();
        assertThat(BlockerType.BINARY_INCOMPATIBLE).isNotNull();
        assertThat(BlockerType.VERSION_INCOMPATIBLE).isNotNull();
        assertThat(BlockerType.values()).hasSize(4);
    }
}
