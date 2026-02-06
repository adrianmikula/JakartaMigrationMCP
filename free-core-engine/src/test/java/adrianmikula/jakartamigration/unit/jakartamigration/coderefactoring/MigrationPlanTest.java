package unit.jakartamigration.coderefactoring;

import adrianmikula.jakartamigration.coderefactoring.domain.MigrationPlan;
import adrianmikula.jakartamigration.coderefactoring.domain.RefactoringPhase;
import adrianmikula.jakartamigration.dependencyanalysis.domain.RiskAssessment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MigrationPlan Tests")
class MigrationPlanTest {
    
    @Test
    @DisplayName("Should create migration plan with phases")
    void shouldCreateMigrationPlan() {
        // Given
        RefactoringPhase phase1 = new RefactoringPhase(
            1,
            "Phase 1",
            List.of("pom.xml"),
            List.of(), // actions
            List.of("Recipe1"),
            List.of(),
            Duration.ofMinutes(5)
        );
        RefactoringPhase phase2 = new RefactoringPhase(
            2,
            "Phase 2",
            List.of("File.java"),
            List.of(), // actions
            List.of("Recipe2"),
            List.of("Phase 1"),
            Duration.ofMinutes(10)
        );
        List<RefactoringPhase> phases = List.of(phase1, phase2);
        List<String> fileSequence = List.of("pom.xml", "File.java");
        Duration estimatedDuration = Duration.ofMinutes(15);
        RiskAssessment riskAssessment = new RiskAssessment(0.3, List.of("Low risk"), List.of());
        
        // When
        MigrationPlan plan = new MigrationPlan(
            phases,
            fileSequence,
            estimatedDuration,
            riskAssessment,
            List.of("Prerequisite 1")
        );
        
        // Then
        assertThat(plan.phases()).hasSize(2);
        assertThat(plan.fileSequence()).hasSize(2);
        assertThat(plan.estimatedDuration()).isEqualTo(estimatedDuration);
        assertThat(plan.overallRisk().riskScore()).isEqualTo(0.3);
        assertThat(plan.prerequisites()).hasSize(1);
    }
    
    @Test
    @DisplayName("Should calculate total duration from phases")
    void shouldCalculateTotalDuration() {
        // Given
        RefactoringPhase phase1 = new RefactoringPhase(1, "P1", List.of(), List.of(), List.of(), List.of(), Duration.ofMinutes(5));
        RefactoringPhase phase2 = new RefactoringPhase(2, "P2", List.of(), List.of(), List.of(), List.of(), Duration.ofMinutes(10));
        RefactoringPhase phase3 = new RefactoringPhase(3, "P3", List.of(), List.of(), List.of(), List.of(), Duration.ofMinutes(15));
        
        // When
        Duration total = phase1.estimatedDuration()
            .plus(phase2.estimatedDuration())
            .plus(phase3.estimatedDuration());
        
        // Then
        assertThat(total).isEqualTo(Duration.ofMinutes(30));
    }
}

