package unit.jakartamigration.coderefactoring.service;

import adrianmikula.jakartamigration.coderefactoring.domain.MigrationPlan;
import adrianmikula.jakartamigration.coderefactoring.domain.RefactoringPhase;
import adrianmikula.jakartamigration.coderefactoring.service.MigrationPlanner;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyAnalysisReport;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.dependencyanalysis.domain.MigrationReadinessScore;
import adrianmikula.jakartamigration.dependencyanalysis.domain.NamespaceCompatibilityMap;
import adrianmikula.jakartamigration.dependencyanalysis.domain.RiskAssessment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MigrationPlanner Tests")
class MigrationPlannerTest {
    
    private final MigrationPlanner planner = new MigrationPlanner();
    
    @TempDir
    Path tempDir;
    
    @Test
    @DisplayName("Should create migration plan with phases")
    void shouldCreateMigrationPlan() throws Exception {
        // Given - create a real temporary project directory
        Path projectPath = tempDir.resolve("test-project");
        Files.createDirectories(projectPath);
        
        // Create a minimal pom.xml for the planner to discover
        Path pomXml = projectPath.resolve("pom.xml");
        Files.writeString(pomXml, """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.test</groupId>
                <artifactId>test-project</artifactId>
                <version>1.0.0</version>
            </project>
            """);
        
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
    void shouldOrderPhasesByDependencies() throws Exception {
        // Given - create a real temporary project directory
        Path projectPath = tempDir.resolve("test-project");
        Files.createDirectories(projectPath);
        
        // Create a minimal pom.xml
        Path pomXml = projectPath.resolve("pom.xml");
        Files.writeString(pomXml, """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.test</groupId>
                <artifactId>test-project</artifactId>
                <version>1.0.0</version>
            </project>
            """);
        
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
            new DependencyGraph(new HashSet<>(), new HashSet<>()),
            new NamespaceCompatibilityMap(Map.of()),
            List.of(),
            List.of(),
            new RiskAssessment(0.3, List.of("Low risk"), List.of()),
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

