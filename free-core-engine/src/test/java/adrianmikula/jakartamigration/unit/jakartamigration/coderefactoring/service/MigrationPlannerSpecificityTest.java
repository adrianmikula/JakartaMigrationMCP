package unit.jakartamigration.coderefactoring.service;

import adrianmikula.jakartamigration.coderefactoring.domain.MigrationPlan;
import adrianmikula.jakartamigration.coderefactoring.domain.PhaseAction;
import adrianmikula.jakartamigration.coderefactoring.domain.RefactoringPhase;
import adrianmikula.jakartamigration.coderefactoring.service.MigrationPlanner;
import adrianmikula.jakartamigration.dependencyanalysis.domain.*;
import adrianmikula.jakartamigration.sourcecodescanning.domain.FileUsage;
import adrianmikula.jakartamigration.sourcecodescanning.domain.ImportStatement;
import adrianmikula.jakartamigration.sourcecodescanning.service.SourceCodeScanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MigrationPlannerSpecificityTest {
    
    private MigrationPlanner planner;
    private SourceCodeScanner mockScanner;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        mockScanner = mock(SourceCodeScanner.class);
        planner = new MigrationPlanner(mockScanner);
    }
    
    @Test
    void shouldCreatePhasesWithFileSpecificActions() throws Exception {
        // Given
        Path srcDir = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(srcDir);
        
        Path servletFile = srcDir.resolve("MyServlet.java");
        Files.writeString(servletFile, """
            package com.example;
            import javax.servlet.ServletException;
            import javax.servlet.http.HttpServlet;
            public class MyServlet extends HttpServlet {
            }
            """);
        
        // Mock scanner to return specific imports
        when(mockScanner.scanFile(any(Path.class))).thenReturn(
            new FileUsage(
                servletFile,
                List.of(
                    new ImportStatement("javax.servlet.ServletException", "javax.servlet", "jakarta.servlet.ServletException", 3),
                    new ImportStatement("javax.servlet.http.HttpServlet", "javax.servlet", "jakarta.servlet.http.HttpServlet", 4)
                ),
                10
            )
        );
        
        DependencyAnalysisReport report = createSampleReport();
        
        // When
        MigrationPlan plan = planner.createPlan(tempDir.toString(), report);
        
        // Then
        assertThat(plan.phases()).isNotEmpty();
        
        // Find Java file phase
        RefactoringPhase javaPhase = plan.phases().stream()
            .filter(p -> p.description().contains("Java files"))
            .findFirst()
            .orElseThrow();
        
        assertThat(javaPhase.actions()).isNotEmpty();
        
        // Check that actions contain specific import changes
        PhaseAction action = javaPhase.actions().get(0);
        assertThat(action.specificChanges()).isNotEmpty();
        assertThat(action.specificChanges().get(0)).contains("Line");
        assertThat(action.specificChanges().get(0)).contains("javax.servlet");
        assertThat(action.specificChanges().get(0)).contains("jakarta.servlet");
    }
    
    @Test
    void shouldCreateBuildFileActions() throws Exception {
        // Given
        Path pomXml = tempDir.resolve("pom.xml");
        Files.writeString(pomXml, "<project></project>");
        
        DependencyAnalysisReport report = createSampleReport();
        
        // When
        MigrationPlan plan = planner.createPlan(tempDir.toString(), report);
        
        // Then
        RefactoringPhase buildPhase = plan.phases().stream()
            .filter(p -> p.description().contains("build files"))
            .findFirst()
            .orElseThrow();
        
        assertThat(buildPhase.actions()).isNotEmpty();
        PhaseAction action = buildPhase.actions().get(0);
        assertThat(action.actionType()).isEqualTo("UPDATE_DEPENDENCY");
        assertThat(action.specificChanges()).isNotEmpty();
    }
    
    @Test
    void shouldHandleNullScannerGracefully() throws Exception {
        // Given - Create at least one file so phases are generated
        Path pomXml = tempDir.resolve("pom.xml");
        Files.writeString(pomXml, "<project></project>");
        
        MigrationPlanner plannerWithoutScanner = new MigrationPlanner();
        DependencyAnalysisReport report = createSampleReport();
        
        // When
        MigrationPlan plan = plannerWithoutScanner.createPlan(tempDir.toString(), report);
        
        // Then
        assertThat(plan.phases()).isNotEmpty();
        // Should still create phases, just with generic actions
        RefactoringPhase firstPhase = plan.phases().get(0);
        assertThat(firstPhase.actions()).isNotEmpty(); // Should have actions even without scanner
    }
    
    private DependencyAnalysisReport createSampleReport() {
        DependencyGraph graph = new DependencyGraph();
        NamespaceCompatibilityMap namespaceMap = new NamespaceCompatibilityMap();
        List<Blocker> blockers = List.of();
        List<VersionRecommendation> recommendations = List.of();
        RiskAssessment risk = new RiskAssessment(0.3, List.of(), List.of());
        MigrationReadinessScore readiness = new MigrationReadinessScore(0.7, "Ready");
        return new DependencyAnalysisReport(graph, namespaceMap, blockers, recommendations, risk, readiness);
    }
}

