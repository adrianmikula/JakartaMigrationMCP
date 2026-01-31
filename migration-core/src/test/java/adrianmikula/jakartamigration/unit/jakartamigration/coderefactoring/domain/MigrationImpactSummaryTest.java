package unit.jakartamigration.coderefactoring.domain;

import adrianmikula.jakartamigration.coderefactoring.domain.MigrationImpactSummary;
import adrianmikula.jakartamigration.dependencyanalysis.domain.*;
import adrianmikula.jakartamigration.sourcecodescanning.domain.FileUsage;
import adrianmikula.jakartamigration.sourcecodescanning.domain.ImportStatement;
import adrianmikula.jakartamigration.sourcecodescanning.domain.SourceCodeAnalysisResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MigrationImpactSummaryTest {
    
    @Test
    void shouldCreateImpactSummaryFromAnalysisResults() {
        // Given
        DependencyAnalysisReport depReport = createSampleDependencyReport();
        SourceCodeAnalysisResult scanResult = createSampleSourceCodeResult();
        
        // When
        MigrationImpactSummary summary = MigrationImpactSummary.from(depReport, scanResult);
        
        // Then
        assertThat(summary.totalFilesToMigrate()).isEqualTo(2);
        assertThat(summary.totalJavaxImports()).isEqualTo(3);
        assertThat(summary.totalBlockers()).isEqualTo(1);
        assertThat(summary.totalRecommendations()).isEqualTo(2);
        assertThat(summary.estimatedEffort().toMinutes()).isGreaterThan(0);
        assertThat(summary.complexity()).isNotNull();
    }
    
    @Test
    void shouldDetermineLowComplexityForSmallProject() {
        // Given
        DependencyAnalysisReport depReport = createSmallDependencyReport();
        SourceCodeAnalysisResult scanResult = createSmallSourceCodeResult();
        
        // When
        MigrationImpactSummary summary = MigrationImpactSummary.from(depReport, scanResult);
        
        // Then
        assertThat(summary.complexity()).isEqualTo(MigrationImpactSummary.MigrationComplexity.LOW);
    }
    
    @Test
    void shouldDetermineMediumComplexityForMediumProject() {
        // Given
        DependencyAnalysisReport depReport = createMediumDependencyReport();
        SourceCodeAnalysisResult scanResult = createMediumSourceCodeResult();
        
        // When
        MigrationImpactSummary summary = MigrationImpactSummary.from(depReport, scanResult);
        
        // Then
        assertThat(summary.complexity()).isEqualTo(MigrationImpactSummary.MigrationComplexity.MEDIUM);
    }
    
    @Test
    void shouldDetermineHighComplexityForLargeProject() {
        // Given
        DependencyAnalysisReport depReport = createLargeDependencyReport();
        SourceCodeAnalysisResult scanResult = createLargeSourceCodeResult();
        
        // When
        MigrationImpactSummary summary = MigrationImpactSummary.from(depReport, scanResult);
        
        // Then
        assertThat(summary.complexity()).isEqualTo(MigrationImpactSummary.MigrationComplexity.HIGH);
    }
    
    private DependencyAnalysisReport createSampleDependencyReport() {
        DependencyGraph graph = new DependencyGraph();
        graph.addNode(new Artifact("javax.servlet", "javax.servlet-api", "4.0.1", "compile", false));
        
        NamespaceCompatibilityMap namespaceMap = new NamespaceCompatibilityMap();
        
        List<Blocker> blockers = List.of(
            new Blocker(
                new Artifact("com.example", "old-lib", "1.0.0", "compile", false),
                BlockerType.NO_JAKARTA_EQUIVALENT,
                "No Jakarta equivalent available",
                List.of("Find alternative library"),
                0.9
            )
        );
        
        List<VersionRecommendation> recommendations = List.of(
            new VersionRecommendation(
                new Artifact("javax.servlet", "javax.servlet-api", "4.0.1", "compile", false),
                new Artifact("jakarta.servlet", "jakarta.servlet-api", "6.0.0", "compile", false),
                "Direct replacement",
                List.of(),
                1.0
            ),
            new VersionRecommendation(
                new Artifact("javax.mail", "javax.mail", "1.5.5", "compile", false),
                new Artifact("com.sun.mail", "jakarta.mail", "2.0.1", "compile", false),
                "Group ID change required",
                List.of(),
                0.95
            )
        );
        
        RiskAssessment risk = new RiskAssessment(0.3, List.of("Low risk"), List.of());
        MigrationReadinessScore readiness = new MigrationReadinessScore(0.7, "Ready for migration");
        
        return new DependencyAnalysisReport(graph, namespaceMap, blockers, recommendations, risk, readiness);
    }
    
    private SourceCodeAnalysisResult createSampleSourceCodeResult() {
        FileUsage file1 = new FileUsage(
            Paths.get("src/main/java/MyServlet.java"),
            List.of(
                new ImportStatement("javax.servlet.ServletException", "javax.servlet", "jakarta.servlet.ServletException", 5),
                new ImportStatement("javax.servlet.http.HttpServlet", "javax.servlet", "jakarta.servlet.http.HttpServlet", 6)
            ),
            50
        );
        
        FileUsage file2 = new FileUsage(
            Paths.get("src/main/java/Validator.java"),
            List.of(
                new ImportStatement("javax.validation.NotNull", "javax.validation", "jakarta.validation.NotNull", 3)
            ),
            30
        );
        
        return new SourceCodeAnalysisResult(
            List.of(file1, file2),
            10,
            2,
            3
        );
    }
    
    private DependencyAnalysisReport createSmallDependencyReport() {
        DependencyGraph graph = new DependencyGraph();
        RiskAssessment risk = new RiskAssessment(0.2, List.of(), List.of());
        MigrationReadinessScore readiness = new MigrationReadinessScore(0.8, "Ready");
        return new DependencyAnalysisReport(graph, new NamespaceCompatibilityMap(), List.of(), List.of(), risk, readiness);
    }
    
    private SourceCodeAnalysisResult createSmallSourceCodeResult() {
        return new SourceCodeAnalysisResult(List.of(), 5, 0, 0);
    }
    
    private DependencyAnalysisReport createMediumDependencyReport() {
        DependencyGraph graph = new DependencyGraph();
        RiskAssessment risk = new RiskAssessment(0.6, List.of("Medium risk"), List.of());
        MigrationReadinessScore readiness = new MigrationReadinessScore(0.5, "Moderate readiness");
        List<Blocker> blockers = List.of(
            new Blocker(
                new Artifact("com.example", "lib", "1.0", "compile", false),
                BlockerType.NO_JAKARTA_EQUIVALENT,
                "No equivalent",
                List.of(),
                0.8
            )
        );
        return new DependencyAnalysisReport(graph, new NamespaceCompatibilityMap(), blockers, List.of(), risk, readiness);
    }
    
    private SourceCodeAnalysisResult createMediumSourceCodeResult() {
        // Create 60 files with javax usage
        List<FileUsage> files = new java.util.ArrayList<>();
        for (int i = 0; i < 60; i++) {
            files.add(new FileUsage(
                Paths.get("src/main/java/File" + i + ".java"),
                List.of(new ImportStatement("javax.servlet.ServletException", "javax.servlet", "jakarta.servlet.ServletException", 1)),
                50
            ));
        }
        return new SourceCodeAnalysisResult(files, 100, 60, 60);
    }
    
    private DependencyAnalysisReport createLargeDependencyReport() {
        DependencyGraph graph = new DependencyGraph();
        RiskAssessment risk = new RiskAssessment(0.9, List.of("High risk"), List.of());
        MigrationReadinessScore readiness = new MigrationReadinessScore(0.3, "Not ready");
        List<Blocker> blockers = List.of(
            new Blocker(new Artifact("com.example", "lib1", "1.0", "compile", false), BlockerType.NO_JAKARTA_EQUIVALENT, "No equivalent", List.of(), 0.9),
            new Blocker(new Artifact("com.example", "lib2", "1.0", "compile", false), BlockerType.NO_JAKARTA_EQUIVALENT, "No equivalent", List.of(), 0.9),
            new Blocker(new Artifact("com.example", "lib3", "1.0", "compile", false), BlockerType.NO_JAKARTA_EQUIVALENT, "No equivalent", List.of(), 0.9),
            new Blocker(new Artifact("com.example", "lib4", "1.0", "compile", false), BlockerType.NO_JAKARTA_EQUIVALENT, "No equivalent", List.of(), 0.9),
            new Blocker(new Artifact("com.example", "lib5", "1.0", "compile", false), BlockerType.NO_JAKARTA_EQUIVALENT, "No equivalent", List.of(), 0.9),
            new Blocker(new Artifact("com.example", "lib6", "1.0", "compile", false), BlockerType.NO_JAKARTA_EQUIVALENT, "No equivalent", List.of(), 0.9)
        );
        return new DependencyAnalysisReport(graph, new NamespaceCompatibilityMap(), blockers, List.of(), risk, readiness);
    }
    
    private SourceCodeAnalysisResult createLargeSourceCodeResult() {
        // Create 100 files with javax usage
        List<FileUsage> files = new java.util.ArrayList<>();
        for (int i = 0; i < 100; i++) {
            files.add(new FileUsage(
                Paths.get("src/main/java/File" + i + ".java"),
                List.of(new ImportStatement("javax.servlet.ServletException", "javax.servlet", "jakarta.servlet.ServletException", 1)),
                100
            ));
        }
        return new SourceCodeAnalysisResult(files, 150, 100, 100);
    }
}

