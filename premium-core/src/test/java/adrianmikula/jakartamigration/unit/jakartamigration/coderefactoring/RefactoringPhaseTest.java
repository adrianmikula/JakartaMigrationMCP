package unit.jakartamigration.coderefactoring;

import adrianmikula.jakartamigration.coderefactoring.domain.RefactoringPhase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RefactoringPhase Tests")
class RefactoringPhaseTest {
    
    @Test
    @DisplayName("Should create refactoring phase with all fields")
    void shouldCreateRefactoringPhase() {
        // Given
        int phaseNumber = 1;
        String description = "Update build files";
        List<String> files = List.of("pom.xml", "build.gradle");
        List<String> recipes = List.of("UpdateMavenCoordinates");
        List<String> dependencies = List.of();
        Duration duration = Duration.ofMinutes(10);
        
        // When
        RefactoringPhase phase = new RefactoringPhase(
            phaseNumber,
            description,
            files,
            List.of(), // actions
            recipes,
            dependencies,
            duration
        );
        
        // Then
        assertThat(phase.phaseNumber()).isEqualTo(phaseNumber);
        assertThat(phase.description()).isEqualTo(description);
        assertThat(phase.files()).isEqualTo(files);
        assertThat(phase.recipes()).isEqualTo(recipes);
        assertThat(phase.dependencies()).isEqualTo(dependencies);
        assertThat(phase.estimatedDuration()).isEqualTo(duration);
    }
    
    @Test
    @DisplayName("Should allow empty files list")
    void shouldAllowEmptyFilesList() {
        // Given
        RefactoringPhase phase = new RefactoringPhase(
            1,
            "Test phase",
            List.of(),
            List.of(), // actions
            List.of(),
            List.of(),
            Duration.ZERO
        );
        
        // Then
        assertThat(phase.files()).isEmpty();
    }
    
    @Test
    @DisplayName("Should allow phase with dependencies")
    void shouldAllowPhaseWithDependencies() {
        // Given
        List<String> dependencies = List.of("Phase 1", "Phase 2");
        
        // When
        RefactoringPhase phase = new RefactoringPhase(
            3,
            "Phase 3",
            List.of("file.java"),
            List.of(), // actions
            List.of("Recipe1"),
            dependencies,
            Duration.ofMinutes(5)
        );
        
        // Then
        assertThat(phase.dependencies()).hasSize(2);
        assertThat(phase.dependencies()).containsExactly("Phase 1", "Phase 2");
    }
}

