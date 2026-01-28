package unit.jakartamigration.dependencyanalysis.domain;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Blocker;
import adrianmikula.jakartamigration.dependencyanalysis.domain.BlockerType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Blocker Tests")
class BlockerTest {

    @Test
    void shouldCreateBlocker() {
        Artifact a = new Artifact("com.legacy", "legacy-lib", "1.0", "compile", false);
        Blocker b = new Blocker(a, BlockerType.NO_JAKARTA_EQUIVALENT, "No equivalent", List.of("Find alternative"), 0.9);
        assertThat(b.artifact()).isEqualTo(a);
        assertThat(b.type()).isEqualTo(BlockerType.NO_JAKARTA_EQUIVALENT);
        assertThat(b.reason()).isEqualTo("No equivalent");
        assertThat(b.mitigationStrategies()).containsExactly("Find alternative");
        assertThat(b.confidence()).isEqualTo(0.9);
    }

    @Test
    void shouldRejectConfidenceBelowZero() {
        Artifact a = new Artifact("g", "a", "1.0", "compile", false);
        assertThatThrownBy(() -> new Blocker(a, BlockerType.NO_JAKARTA_EQUIVALENT, "r", List.of(), -0.1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Confidence");
    }

    @Test
    void shouldRejectConfidenceAboveOne() {
        Artifact a = new Artifact("g", "a", "1.0", "compile", false);
        assertThatThrownBy(() -> new Blocker(a, BlockerType.NO_JAKARTA_EQUIVALENT, "r", List.of(), 1.1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Confidence");
    }
}
