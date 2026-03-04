package adrianmikula.jakartamigration.analysis.persistence;

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
        Path dbPath = tempDir.resolve("test-central-analysis.db");
        store = new CentralMigrationAnalysisStore(dbPath);
    }

    @Test
    @DisplayName("Should create database file")
    void shouldCreateDatabaseFile() {
        Path dbPath = store.getDbPath();
        assertThat(dbPath).isNotNull();
        assertThat(dbPath.toString()).contains("test-central-analysis.db");
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
        assertThat(
                orgDeps.stream().filter(d -> d.groupId().equals("com.myorg") && d.artifactId().equals("internal-lib")))
                .anyMatch(dep -> dep.isAnalyzed());
    }

    @Test
    @DisplayName("Should track cross-repository dependencies")
    void shouldTrackCrossRepositoryDependencies() {
        // Given
        Path repo1 = tempDir.resolve("repo1");
        Path repo2 = tempDir.resolve("repo2");

        DependencyAnalysisReport report1 = createSampleReportWithOrgDep("com.myorg", "shared-lib", "1.0.0");
        store.setOrgNamespacePatterns(Set.of("com.myorg.*"));

        // When
        store.saveAnalysisReport(repo1, report1, true); // Source repo analyzed
        store.saveAnalysisReport(repo2, report1, false); // Consumer repo also uses it

        // Then
        var orgDeps = store.getOrgDependencies();
        assertThat(orgDeps).isNotEmpty();
        var sharedLib = orgDeps.stream()
                .filter(d -> d.artifactId().equals("shared-lib"))
                .findFirst();
        assertThat(sharedLib).isPresent();
        // The last analyzed repo should be repo2
        var repos = store.getRepositories();
        assertThat(repos).anyMatch(r -> r.repositoryPath().equals(repo2.toString()));
    }

    @Test
    @DisplayName("Should handle multiple organization patterns")
    void shouldHandleMultiplePatterns() {
        // Given
        store.setOrgNamespacePatterns(Set.of("com.myorg.*", "org.internal.*", "exact.match"));

        // Then
        assertThat(store.isOrgDependency("com.myorg.utils")).isTrue();
        assertThat(store.isOrgDependency("org.internal.api")).isTrue();
        assertThat(store.isOrgDependency("exact.match")).isTrue();
        assertThat(store.isOrgDependency("exact.match.sub")).isFalse();
        assertThat(store.isOrgDependency("org.apache")).isFalse();
    }

    @Test
    @DisplayName("Should update analyzed status and migration status")
    void shouldUpdateStatus() {
        // Given
        store.setOrgNamespacePatterns(Set.of("com.myorg.*"));
        Path repo = tempDir.resolve("test-repo");
        DependencyAnalysisReport report = createSampleReportWithOrgDep("com.myorg", "lib-to-migrate", "1.1.0");
        store.saveAnalysisReport(repo, report, false);

        // When
        store.markOrgDependencyAnalyzed("com.myorg", "lib-to-migrate", "1.1.0", true, "READY_FOR_JAKARTA");

        // Then
        var orgDeps = store.getOrgDependencies();
        var dep = orgDeps.stream()
                .filter(d -> d.artifactId().equals("lib-to-migrate"))
                .findFirst()
                .orElseThrow();

        assertThat(dep.isAnalyzed()).isTrue();
        assertThat(dep.migrationStatus()).isEqualTo("READY_FOR_JAKARTA");
    }

    @Test
    @DisplayName("Should save and retrieve recipe execution")
    void shouldSaveAndRetrieveRecipeExecution() {
        // Given
        String repoPath = tempDir.resolve("test-repo").toString();
        List<String> affectedFiles = List.of("src/main/java/Test.java", "pom.xml");

        // When
        store.saveRecipeExecution(repoPath, "AddJakartaNamespace", true, "Successfully applied", affectedFiles);

        // Then
        var executions = store.getRecipeExecutions(repoPath, "AddJakartaNamespace");
        assertThat(executions).hasSize(1);
        assertThat(executions.get(0).get("recipe_name")).isEqualTo("AddJakartaNamespace");
        assertThat(executions.get(0).get("success")).isEqualTo(true);
        assertThat(executions.get(0).get("affected_files")).contains("src/main/java/Test.java");
    }

    @Test
    @DisplayName("Should save failed recipe execution")
    void shouldSaveFailedRecipeExecution() {
        // Given
        String repoPath = tempDir.resolve("test-repo").toString();

        // When
        store.saveRecipeExecution(repoPath, "MigrateServletApi", false, "Build failed", null);

        // Then
        var executions = store.getRecipeExecutions(repoPath, "MigrateServletApi");
        assertThat(executions).hasSize(1);
        assertThat(executions.get(0).get("success")).isEqualTo(false);
        assertThat(executions.get(0).get("message")).isEqualTo("Build failed");
    }

    @Test
    @DisplayName("Should get all recipe executions for a repository")
    void shouldGetAllRecipeExecutions() {
        // Given
        String repoPath = tempDir.resolve("test-repo").toString();
        store.saveRecipeExecution(repoPath, "AddJakartaNamespace", true, "Success 1", List.of("file1.java"));
        store.saveRecipeExecution(repoPath, "MigrateJpa", true, "Success 2", List.of("file2.java"));
        store.saveRecipeExecution(repoPath, "MigrateCdi", false, "Failed", null);

        // When
        var allExecutions = store.getAllRecipeExecutions(repoPath);

        // Then
        assertThat(allExecutions).hasSize(3);
    }

    @Test
    @DisplayName("Should return empty list for non-existent recipe executions")
    void shouldReturnEmptyForNonExistent() {
        // Given
        String repoPath = tempDir.resolve("test-repo").toString();

        // When
        var executions = store.getRecipeExecutions(repoPath, "NonExistentRecipe");

        // Then
        assertThat(executions).isEmpty();
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
                false));

        DependencyGraph graph = new DependencyGraph(artifacts, dependencies);

        Map<Artifact, Namespace> namespaceMap = new HashMap<>();
        namespaceMap.put(new Artifact("jakarta.servlet", "jakarta.servlet-api", "6.0.0", "compile", false),
                Namespace.JAKARTA);
        namespaceMap.put(new Artifact("javax.servlet", "javax.servlet-api", "4.0.1", "compile", false),
                Namespace.JAVAX);
        namespaceMap.put(new Artifact("jakarta.validation", "jakarta.validation-api", "3.0.0", "compile", false),
                Namespace.JAKARTA);
        NamespaceCompatibilityMap compatMap = NamespaceCompatibilityMap.fromMap(namespaceMap);

        List<Blocker> blockers = List.of(
                new Blocker(
                        new Artifact("javax.servlet", "javax.servlet-api", "4.0.1", "compile", false),
                        BlockerType.NO_JAKARTA_EQUIVALENT,
                        "No Jakarta equivalent available",
                        List.of("Consider using a different servlet implementation"),
                        0.95));

        List<VersionRecommendation> recommendations = List.of(
                new VersionRecommendation(
                        new Artifact("javax.servlet", "javax.servlet-api", "4.0.1", "compile", false),
                        new Artifact("jakarta.servlet", "jakarta.servlet-api", "6.0.0", "compile", false),
                        "Direct upgrade",
                        List.of("API changes in servlet methods"),
                        0.9));

        RiskAssessment risk = new RiskAssessment(
                0.6,
                List.of("javax dependencies found", "Mixed namespace usage"),
                List.of("Update dependencies first", "Test thoroughly"));

        MigrationReadinessScore score = new MigrationReadinessScore(0.65,
                "Moderate readiness - some dependencies need migration");

        return new DependencyAnalysisReport(graph, compatMap.namespaceMap(), blockers, recommendations, risk, score);
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
        NamespaceCompatibilityMap compatMap = NamespaceCompatibilityMap.fromMap(namespaceMap);

        List<Blocker> blockers = List.of();
        List<VersionRecommendation> recommendations = List.of();

        RiskAssessment risk = new RiskAssessment(
                0.5,
                List.of("Internal dependency"),
                List.of("Analyze for migration needs"));

        MigrationReadinessScore score = new MigrationReadinessScore(0.5, "Internal dependency analysis needed");

        return new DependencyAnalysisReport(graph, compatMap.namespaceMap(), blockers, recommendations, risk, score);
    }
}
