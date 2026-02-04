package adrianmikula.jakartamigration.analysis.persistence;

import adrianmikula.jakartamigration.coderefactoring.domain.*;
import adrianmikula.jakartamigration.dependencyanalysis.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CentralMigrationAnalysisStore.
 * Tests the centralized SQLite storage with user profile folder location
 * and org namespace pattern matching for internal dependencies.
 */
class CentralMigrationAnalysisStoreTest {

    @TempDir
    Path tempDir;

    private CentralMigrationAnalysisStore store;

    @BeforeEach
    void setUp() {
        // Use temp directory for testing
        store = new CentralMigrationAnalysisStore();
    }

    @Test
    @DisplayName("Should create database file")
    void shouldCreateDatabaseFile() {
        Path dbPath = store.getDbPath();
        assertThat(dbPath).isNotNull();
        assertThat(dbPath.toString()).contains("JakartaMigration");
    }

    @Test
    @DisplayName("Should save analysis report without throwing exception")
    void shouldSaveAnalysisReport() {
        // Given
        DependencyAnalysisReport report = createSampleReport();
        Path repositoryPath = tempDir.resolve("test-repo");

        // When
        store.saveAnalysisReport(repositoryPath, report, false);

        // Then - verify data was saved by getting repositories
        var repos = store.getRepositories();
        assertThat(repos).isNotEmpty();
    }

    @Test
    @DisplayName("Should set and match org namespace patterns")
    void shouldSetAndMatchOrgNamespacePatterns() {
        // Given
        Set<String> patterns = new HashSet<>();
        patterns.add("com.myorg.*");
        patterns.add("io.company.*");

        // When
        store.setOrgNamespacePatterns(patterns);

        // Then
        assertThat(store.isOrgDependency("com.myorg.library")).isTrue();
        assertThat(store.isOrgDependency("io.company.utils")).isTrue();
        assertThat(store.isOrgDependency("org.springframework")).isFalse();
        assertThat(store.isOrgDependency("com.other")).isFalse();
    }

    @Test
    @DisplayName("Should match exact namespace patterns")
    void shouldMatchExactNamespacePatterns() {
        // Given
        Set<String> patterns = new HashSet<>();
        patterns.add("com.myorg");

        // When
        store.setOrgNamespacePatterns(patterns);

        // Then
        assertThat(store.isOrgDependency("com.myorg")).isTrue();
        assertThat(store.isOrgDependency("com.myorg.sub")).isFalse();
    }

    @Test
    @DisplayName("Should register multiple repositories")
    void shouldRegisterMultipleRepositories() {
        // Given
        Path repo1 = tempDir.resolve("repo1");
        Path repo2 = tempDir.resolve("repo2");

        // When
        store.registerRepository(repo1, false);
        store.registerRepository(repo2, false);

        // Then
        var repos = store.getRepositories();
        assertThat(repos).hasSize(2);
    }

    @Test
    @DisplayName("Should save org dependency")
    void shouldSaveOrgDependency() {
        // Given
        store.setOrgNamespacePatterns(Set.of("com.myorg.*"));
        Path repo = tempDir.resolve("test-repo");

        DependencyAnalysisReport report = createSampleReportWithOrgDep("com.myorg", "internal-lib", "1.0.0");

        // When
        store.saveAnalysisReport(repo, report, false);

        // Then
        var orgDeps = store.getOrgDependencies();
        assertThat(orgDeps).isNotEmpty();
    }

    @Test
    @DisplayName("Should get unanalyzed org dependencies")
    void shouldGetUnanalyzedOrgDependencies() {
        // Given
        store.setOrgNamespacePatterns(Set.of("com.myorg.*"));
        Path repo = tempDir.resolve("test-repo");

        DependencyAnalysisReport report = createSampleReportWithOrgDep("com.myorg", "internal-lib", "1.0.0");

        // When
        store.saveAnalysisReport(repo, report, false);

        // Then
        var unanalyzed = store.getUnanalyzedOrgDependencies();
        assertThat(unanalyzed).isNotEmpty();
    }

    @Test
    @DisplayName("Should mark org dependency as analyzed")
    void shouldMarkOrgDependencyAsAnalyzed() {
        // Given
        store.setOrgNamespacePatterns(Set.of("com.myorg.*"));
        Path repo = tempDir.resolve("test-repo");

        DependencyAnalysisReport report = createSampleReportWithOrgDep("com.myorg", "internal-lib", "1.0.0");

        // When
        store.saveAnalysisReport(repo, report, false);
        store.markOrgDependencyAnalyzed("com.myorg", "internal-lib", "1.0.0", true, "READY");

        // Then
        var orgDeps = store.getOrgDependencies();
        assertThat(orgDeps.stream().filter(d -> d.groupId().equals("com.myorg") && d.artifactId().equals("internal-lib")))
            .anyMatch(dep -> dep.isAnalyzed());
    }

    // Helper methods

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

    private DependencyAnalysisReport createSampleReportWithOrgDep(String groupId, String artifactId, String version) {
        Set<Artifact> artifacts = new HashSet<>();
        artifacts.add(new Artifact(groupId, artifactId, version, "compile", false));

        Set<Dependency> dependencies = new HashSet<>();

        DependencyGraph graph = new DependencyGraph(artifacts, dependencies);

        Map<Artifact, Namespace> namespaceMap = new HashMap<>();
        // Internal deps are tracked via setOrgNamespacePatterns, not via Namespace enum
        // Use UNKNOWN since it's an internal artifact
        namespaceMap.put(new Artifact(groupId, artifactId, version, "compile", false), Namespace.UNKNOWN);
        NamespaceCompatibilityMap compatMap = new NamespaceCompatibilityMap(namespaceMap);

        List<Blocker> blockers = List.of();
        List<VersionRecommendation> recommendations = List.of();

        RiskAssessment risk = new RiskAssessment(
            0.5,
            List.of("Internal dependency"),
            List.of("Analyze for migration needs")
        );

        MigrationReadinessScore score = new MigrationReadinessScore(0.5, "Internal dependency analysis needed");

        return new DependencyAnalysisReport(graph, compatMap, blockers, recommendations, risk, score);
    }
}
