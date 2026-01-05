package unit.jakartamigration.coderefactoring.service;

import com.bugbounty.jakartamigration.coderefactoring.domain.MigrationPlan;
import com.bugbounty.jakartamigration.coderefactoring.domain.RefactoringPhase;
import com.bugbounty.jakartamigration.dependencyanalysis.domain.DependencyAnalysisReport;
import com.bugbounty.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import com.bugbounty.jakartamigration.dependencyanalysis.domain.MigrationReadinessScore;
import com.bugbounty.jakartamigration.dependencyanalysis.domain.NamespaceCompatibilityMap;
import com.bugbounty.jakartamigration.dependencyanalysis.domain.RiskAssessment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MigrationPlanner Tests")
class MigrationPlannerTest {
    
    private final MigrationPlanner planner = new MigrationPlanner();
    
    @Test
    @DisplayName("Should create migration plan with phases")
    void shouldCreateMigrationPlan() {
        // Given
        Path projectPath = Paths.get("test-project");
        DependencyAnalysisReport report = createMockReport();
        
        // When
        MigrationPlan plan = planner.createPlan(projectPath.toString(), report);
        
        // Then
        assertThat(plan).isNotNull();
        assertThat(plan.phases()).isNotEmpty();
        assertThat(plan.estimatedDuration()).isNotNull();
        assertThat(plan.overallRisk()).isNotNull();
    }
    
    @Test
    @DisplayName("Should order phases by dependencies")
    void shouldOrderPhasesByDependencies() {
        // Given
        Path projectPath = Paths.get("test-project");
        DependencyAnalysisReport report = createMockReport();
        
        // When
        MigrationPlan plan = planner.createPlan(projectPath.toString(), report);
        
        // Then
        List<RefactoringPhase> phases = plan.phases();
        for (int i = 0; i < phases.size(); i++) {
            RefactoringPhase phase = phases.get(i);
            // Each phase should only depend on previous phases
            for (String dependency : phase.dependencies()) {
                int depIndex = findPhaseIndex(phases, dependency);
                assertThat(depIndex).isLessThan(i);
            }
        }
    }
    
    @Test
    @DisplayName("Should determine optimal file order")
    void shouldDetermineOptimalFileOrder() {
        // Given
        List<String> files = List.of(
            "src/main/java/com/example/Service.java",
            "src/main/java/com/example/Controller.java",
            "pom.xml",
            "src/main/resources/persistence.xml"
        );
        
        // When
        List<String> ordered = planner.determineOptimalOrder(files);
        
        // Then
        assertThat(ordered).isNotEmpty();
        // Build files should come first
        assertThat(ordered.get(0)).contains("pom.xml");
    }
    
    private DependencyAnalysisReport createMockReport() {
        return new DependencyAnalysisReport(
            new DependencyGraph(List.of(), List.of()),
            new NamespaceCompatibilityMap(Map.of()),
            List.of(),
            List.of(),
            new RiskAssessment(0.3, List.of("Low risk")),
            new MigrationReadinessScore(0.8, "Ready for migration")
        );
    }
    
    private int findPhaseIndex(List<RefactoringPhase> phases, String phaseName) {
        for (int i = 0; i < phases.size(); i++) {
            if (phases.get(i).description().equals(phaseName)) {
                return i;
            }
        }
        return -1;
    }
}

