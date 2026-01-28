package unit.jakartamigration.dependencyanalysis.domain;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.TransitiveConflict;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TransitiveConflict Tests")
class TransitiveConflictTest {

    @Test
    void shouldCreateAndExposeFields() {
        Artifact root = new Artifact("com.app", "app", "1.0", "compile", false);
        Artifact conflict = new Artifact("javax.servlet", "javax.servlet-api", "4.0", "compile", true);
        TransitiveConflict tc = new TransitiveConflict(root, conflict, "MIXED_NAMESPACE", "javax vs jakarta");
        assertThat(tc.rootArtifact()).isEqualTo(root);
        assertThat(tc.conflictingArtifact()).isEqualTo(conflict);
        assertThat(tc.conflictType()).isEqualTo("MIXED_NAMESPACE");
        assertThat(tc.description()).isEqualTo("javax vs jakarta");
    }
}
