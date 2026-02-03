package adrianmikula.jakartamigration.analysis.persistence;

import adrianmikula.jakartamigration.coderefactoring.domain.*;
import adrianmikula.jakartamigration.dependencyanalysis.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SqliteMigrationAnalysisStore.
 */
class SqliteMigrationAnalysisStoreTest {

    @TempDir
    Path tempDir;

    private SqliteMigrationAnalysisStore store;

    @BeforeEach
    void setUp() {
        store = new SqliteMigrationAnalysisStore(tempDir);
    }

    @Test
    @DisplayName("Should create database file in project directory")
    void shouldCreateDatabaseFile() {
        Path dbPath = tempDir.resolve(".jakarta-migration").resolve("jakarta-migration.db");
        assertThat(dbPath).exists();
    }

    @Test
    @DisplayName("Should save and load analysis report")
    void shouldSaveAndLoadAnalysisReport() {
        // Given
        DependencyAnalysisReport report = createSampleReport();

        // When
        store.saveAnalysisReport(tempDir, report);
        Optional<DependencyAnalysisReport> loaded = store.loadLatestAnalysisReport(tempDir);

        // Then
        assertThat(loaded).isPresent();
        assertThat(loaded.get().dependencyGraph().getNodes()).hasSize(report.dependencyGraph().getNodes().size());
        assertThat(loaded.get().blockers()).hasSize(report.blockers().size());
    }

    @Test
    @DisplayName("Should return empty when no report exists")
    void shouldReturnEmptyWhenNoReport() {
        // When
        Optional<DependencyAnalysisReport> loaded = store.loadLatestAnalysisReport(tempDir);

        // Then
        assertThat(loaded).isEmpty();
    }

    @Test
    @DisplayName("Should save and load migration plan")
    void shouldSaveAndLoadMigrationPlan() {
        // Given
        MigrationPlan plan = createSamplePlan();

        // When
        store.saveMigrationPlan(tempDir, plan);
        Optional<MigrationPlan> loaded = store.loadLatestMigrationPlan(tempDir);

        // Then
        assertThat(loaded).isPresent();
        assertThat(loaded.get().phases()).hasSize(plan.phases().size());
        assertThat(loaded.get().totalFileCount()).isEqualTo(plan.totalFileCount());
    }

    @Test
    @DisplayName("Should save dependencies with namespace information")
    void shouldSaveDependenciesWithNamespace() {
        // Given
        DependencyAnalysisReport report = createSampleReport();
        store.saveAnalysisReport(tempDir, report);

        // When
        List<SqliteMigrationAnalysisStore.DependencyInfo> dependencies = store.getDependencies(tempDir);

        // Then
        assertThat(dependencies).isNotEmpty();
        assertThat(dependencies).anyMatch(d -> "jakarta.servlet".equals(d.groupId()) && "jakarta.servlet-api".equals(d.artifactId()));
    }

    @Test
    @DisplayName("Should filter dependencies needing migration")
    void shouldFilterDependenciesNeedingMigration() {
        // Given
        DependencyAnalysisReport report = createSampleReport();
        store.saveAnalysisReport(tempDir, report);

        // When
        List<SqliteMigrationAnalysisStore.DependencyInfo> needsMigration = store.getDependenciesNeedingMigration(tempDir);

        // Then
        assertThat(needsMigration).isNotEmpty();
        assertThat(needsMigration).allMatch(d -> "NEEDS_MIGRATION".equals(d.migrationStatus()));
    }

    @Test
    @DisplayName("Should save and retrieve analysis summary")
    void shouldSaveAndRetrieveAnalysisSummary() {
        // Given
        DependencyAnalysisReport report = createSampleReport();
        store.saveAnalysisReport(tempDir, report);

        // When
        Optional<SqliteMigrationAnalysisStore.AnalysisSummary> summary = store.getAnalysisSummary(tempDir);

        // Then
        assertThat(summary).isPresent();
        assertThat(summary.get().totalDependencies()).isGreaterThan(0);
        assertThat(summary.get().readinessScore()).isBetween(0.0, 1.0);
    }

    @Test
    @DisplayName("Should check if analysis data exists")
    void shouldCheckIfAnalysisDataExists() {
        // Initially
        assertThat(store.hasAnalysisData(tempDir)).isFalse();

        // After saving
        DependencyAnalysisReport report = createSampleReport();
        store.saveAnalysisReport(tempDir, report);

        assertThat(store.hasAnalysisData(tempDir)).isTrue();
    }

    @Test
    @DisplayName("Should get last analysis time")
    void shouldGetLastAnalysisTime() {
        // Given
        DependencyAnalysisReport report = createSampleReport();
        store.saveAnalysisReport(tempDir, report);

        // When
        Optional<Instant> lastTime = store.getLastAnalysisTime(tempDir);

        // Then
        assertThat(lastTime).isPresent();
        assertThat(lastTime.get()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    @DisplayName("Should clear analysis data")
    void shouldClearAnalysisData() {
        // Given
        DependencyAnalysisReport report = createSampleReport();
        store.saveAnalysisReport(tempDir, report);
        assertThat(store.hasAnalysisData(tempDir)).isTrue();

        // When
        boolean cleared = store.clearAnalysisData(tempDir);

        // Then
        assertThat(cleared).isTrue();
        assertThat(store.hasAnalysisData(tempDir)).isFalse();
    }

    @Test
    @DisplayName("Should save and load blockers")
    void shouldSaveAndLoadBlockers() {
        // Given
        DependencyAnalysisReport report = createSampleReport();
        store.saveAnalysisReport(tempDir, report);

        // When
        List<SqliteMigrationAnalysisStore.StoredBlocker> blockers = store.getBlockers(tempDir);

        // Then
        assertThat(blockers).isNotEmpty();
        assertThat(blockers.get(0).type()).isNotNull();
    }

    @Test
    @DisplayName("Should handle multiple projects in same database")
    void shouldHandleMultipleProjects() throws Exception {
        // Given
        Path project1 = tempDir.resolve("project1");
        Path project2 = tempDir.resolve("project2");
        Files.createDirectories(project1);
        Files.createDirectories(project2);

        DependencyAnalysisReport report1 = createSampleReport();
        DependencyAnalysisReport report2 = createSampleReport();

        // When
        store.saveAnalysisReport(project1, report1);
        store.saveAnalysisReport(project2, report2);

        // Then
        assertThat(store.hasAnalysisData(project1)).isTrue();
        assertThat(store.hasAnalysisData(project2)).isTrue();
        assertThat(store.loadLatestAnalysisReport(project1)).isPresent();
        assertThat(store.loadLatestAnalysisReport(project2)).isPresent();
    }

    @Test
    @DisplayName("Should get database path")
    void shouldGetDatabasePath() {
        // When
        Path dbPath = store.getDbPath();

        // Then
        assertThat(dbPath).isEqualTo(tempDir.resolve(".jakarta-migration").resolve("jakarta-migration.db"));
    }

    // Helper methods to create test data

    private DependencyAnalysisReport createSampleReport() {
        Set<Artifact> artifacts = new HashSet<>();
        artifacts.add(new Artifact("jakarta.servlet", "jakarta.servlet-api", "6.0.0", "compile", false));
        artifacts.add(new Artifact("javax.servlet", "javax.servlet-api", "4.0.1", "compile", false));
        artifacts.add(new Artifact("jakarta.validation", "jakarta.validation-api", "3.0.0", "compile", false));

        Set<Dependency> dependencies = new HashSet<>();
        dependencies.add(new Dependency(
            new Artifact("com.example", "myapp", "1.0.0", "compile", false),
            new Artifact("javax.servlet", "javax.servlet-api", "4.0.1", "compile", false),
            "compile",
            false
        ));

        DependencyGraph graph = new DependencyGraph(artifacts, dependencies);

        Map<Artifact, Namespace> namespaceMap = new HashMap<>();
        namespaceMap.put(new Artifact("jakarta.servlet", "jakarta.servlet-api", "6.0.0", "compile", false), Namespace.JAKARTA);
        namespaceMap.put(new Artifact("javax.servlet", "javax.servlet-api", "4.0.1", "compile", false), Namespace.JAVAX);
        namespaceMap.put(new Artifact("jakarta.validation", "jakarta.validation-api", "3.0.0", "compile", false), Namespace.JAKARTA);
        NamespaceCompatibilityMap compatMap = new NamespaceCompatibilityMap(namespaceMap);

        List<Blocker> blockers = List.of(
            new Blocker(
                new Artifact("javax.servlet", "javax.servlet-api", "4.0.1", "compile", false),
                BlockerType.NO_JAKARTA_EQUIVALENT,
                "No Jakarta equivalent available",
                List.of("Consider using a different servlet implementation"),
                0.95
            )
        );

        List<VersionRecommendation> recommendations = List.of(
            new VersionRecommendation(
                new Artifact("javax.servlet", "javax.servlet-api", "4.0.1", "compile", false),
                new Artifact("jakarta.servlet", "jakarta.servlet-api", "6.0.0", "compile", false),
                "Direct upgrade",
                List.of("API changes in servlet methods"),
                0.9
            )
        );

        RiskAssessment risk = new RiskAssessment(
            0.6,
            List.of("javax dependencies found", "Mixed namespace usage"),
            List.of("Update dependencies first", "Test thoroughly")
        );

        MigrationReadinessScore score = new MigrationReadinessScore(0.65, "Moderate readiness - some dependencies need migration");

        return new DependencyAnalysisReport(graph, compatMap, blockers, recommendations, risk, score);
    }

    private MigrationPlan createSamplePlan() {
        RefactoringPhase phase1 = new RefactoringPhase(
            1,
            "Update servlet dependencies",
            List.of("src/main/java/com/example/MyServlet.java"),
            List.of(new PhaseAction("src/main/java/com/example/MyServlet.java", "UPDATE_IMPORTS", List.of("javax.servlet.http.HttpServlet -> jakarta.servlet.http.HttpServlet"))),
            List.of("jakarta-servlet-recipe"),
            List.of("jakarta.servlet:jakarta.servlet-api:6.0.0"),
            Duration.ofHours(1)
        );

        RefactoringPhase phase2 = new RefactoringPhase(
            2,
            "Update validation dependencies",
            List.of("src/main/java/com/example/MyValidator.java"),
            List.of(new PhaseAction("src/main/java/com/example/MyValidator.java", "UPDATE_IMPORTS", List.of("javax.validation -> jakarta.validation"))),
            List.of("jakarta-validation-recipe"),
            List.of("jakarta.validation:jakarta.validation-api:3.0.0"),
            Duration.ofMinutes(30)
        );

        RiskAssessment risk = new RiskAssessment(
            0.4,
            List.of("Small number of files"),
            List.of("Test after each phase")
        );

        return new MigrationPlan(
            List.of(phase1, phase2),
            List.of("src/main/java/com/example/MyServlet.java", "src/main/java/com/example/MyValidator.java"),
            Duration.ofHours(2),
            risk,
            List.of("Ensure all tests pass before starting")
        );
    }
}
