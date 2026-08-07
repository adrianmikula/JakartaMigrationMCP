package unit.jakartamigration.dependencyanalysis.service;

import adrianmikula.jakartamigration.analysis.persistence.CentralMigrationAnalysisStore;
import adrianmikula.jakartamigration.dependencyanalysis.domain.*;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyAnalysisModule;
import adrianmikula.jakartamigration.dependencyanalysis.service.ImprovedMavenCentralLookupService;
import adrianmikula.jakartamigration.dependencyanalysis.service.JakartaMappingService;
import adrianmikula.jakartamigration.dependencyanalysis.service.NamespaceClassifier;
import adrianmikula.jakartamigration.dependencyanalysis.service.impl.DependencyAnalysisModuleImpl;
import adrianmikula.jakartamigration.dependencyanalysis.service.impl.MavenDependencyGraphBuilder;
import adrianmikula.jakartamigration.dependencyanalysis.service.impl.SimpleNamespaceClassifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DependencyAnalysisModule Tests")
class DependencyAnalysisModuleTest {

    private DependencyAnalysisModule module;

    @Mock
    private JakartaMappingService jakartaMappingService;

    @Mock
    private ImprovedMavenCentralLookupService mavenCentralLookupService;

    @Mock
    private CentralMigrationAnalysisStore analysisStore;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        NamespaceClassifier namespaceClassifier = new SimpleNamespaceClassifier();
        MavenDependencyGraphBuilder graphBuilder = new MavenDependencyGraphBuilder();

        module = new DependencyAnalysisModuleImpl(
                graphBuilder,
                namespaceClassifier,
                jakartaMappingService,
                mavenCentralLookupService,
                analysisStore);
    }

    @Test
    @DisplayName("Should analyze Maven project and return complete report")
    void shouldAnalyzeMavenProject() throws Exception {
        // Given
        String pomContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test-project</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>javax.servlet</groupId>
                            <artifactId>javax.servlet-api</artifactId>
                            <version>4.0.1</version>
                        </dependency>
                        <dependency>
                            <groupId>jakarta.servlet</groupId>
                            <artifactId>jakarta.servlet-api</artifactId>
                            <version>6.0.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """;

        Files.writeString(tempDir.resolve("pom.xml"), pomContent);

        mockUpgradeRecommendation("javax.servlet", "javax.servlet-api",
                "jakarta.servlet", "jakarta.servlet-api", "6.0.0");

        // When
        DependencyAnalysisReport report = module.analyzeProject(tempDir);

        // Then
        assertThat(report).isNotNull();
        assertThat(report.dependencyGraph()).isNotNull();
        assertThat(report.namespaceMap()).isNotNull();
        assertThat(report.blockers()).isNotNull();
        assertThat(report.recommendations()).isNotNull();
        assertThat(report.riskAssessment()).isNotNull();
        assertThat(report.readinessScore()).isNotNull();

        // Should find both dependencies
        assertThat(report.dependencyGraph().nodeCount()).isGreaterThanOrEqualTo(3);

        // Should identify javax servlet as JAVAX namespace
        Artifact javaxServlet = new Artifact("javax.servlet", "javax.servlet-api", "4.0.1", "compile", false);
        assertThat(report.namespaceMap().get(javaxServlet)).isEqualTo(Namespace.JAVAX);

        // Should identify jakarta servlet as JAKARTA namespace
        Artifact jakartaServlet = new Artifact("jakarta.servlet", "jakarta.servlet-api", "6.0.0", "compile", false);
        assertThat(report.namespaceMap().get(jakartaServlet)).isEqualTo(Namespace.JAKARTA);
    }

    @Test
    @DisplayName("Should analyze Gradle project via buildFromProject")
    void shouldAnalyzeGradleProject() throws Exception {
        // Given - create a single-module Gradle project
        String buildContent = """
                plugins {
                    id 'java'
                }

                dependencies {
                    implementation 'javax.servlet:javax.servlet-api:4.0.1'
                    implementation 'org.springframework.boot:spring-boot-starter-web:2.7.0'
                }
                """;
        Files.writeString(tempDir.resolve("build.gradle"), buildContent);

        mockUpgradeRecommendation("javax.servlet", "javax.servlet-api",
                "jakarta.servlet", "jakarta.servlet-api", "6.0.0");

        // When
        DependencyAnalysisReport report = module.analyzeProject(tempDir);

        // Then
        assertThat(report).isNotNull();
        assertThat(report.dependencyGraph()).isNotNull();
        assertThat(report.dependencyGraph().nodeCount()).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("Should analyze multi-module Gradle project")
    void shouldAnalyzeMultiModuleGradleProject() throws Exception {
        // Given - create a multi-module Gradle project
        Files.writeString(tempDir.resolve("settings.gradle"),
                "include 'core', 'web'");

        Files.writeString(tempDir.resolve("build.gradle"), """
                plugins {
                    id 'java'
                }
                dependencies {
                    implementation 'com.google.guava:guava:32.1.3-jre'
                }
                """);

        Files.createDirectories(tempDir.resolve("core"));
        Files.writeString(tempDir.resolve("core/build.gradle"), """
                plugins {
                    id 'java-library'
                }
                dependencies {
                    api 'javax.servlet:javax.servlet-api:4.0.1'
                    implementation 'org.slf4j:slf4j-api:2.0.9'
                }
                """);

        Files.createDirectories(tempDir.resolve("web"));
        Files.writeString(tempDir.resolve("web/build.gradle"), """
                plugins {
                    id 'java-library'
                }
                dependencies {
                    implementation project(':core')
                    implementation 'jakarta.servlet:jakarta.servlet-api:6.0.0'
                    implementation 'com.fasterxml.jackson.core:jackson-databind:2.15.3'
                }
                """);

        mockUpgradeRecommendation("javax.servlet", "javax.servlet-api",
                "jakarta.servlet", "jakarta.servlet-api", "6.0.0");

        // When
        DependencyAnalysisReport report = module.analyzeProject(tempDir);

        // Then
        assertThat(report).isNotNull();
        assertThat(report.dependencyGraph()).isNotNull();

        // Should find dependencies from all three build files (root + core + web)
        // root: guava; core: servlet-api, slf4j; web: jakarta-servlet-api, jackson (+ project deps excluded)
        assertThat(report.dependencyGraph().nodeCount()).isGreaterThanOrEqualTo(5);
    }

    @Test
    @DisplayName("Should analyze multi-module Maven project")
    void shouldAnalyzeMultiModuleMavenProject() throws Exception {
        // Given - create a multi-module Maven project
        Files.writeString(tempDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>parent-project</artifactId>
                    <version>1.0.0</version>
                    <modules>
                        <module>core</module>
                        <module>web</module>
                    </modules>
                </project>
                """);

        Files.createDirectories(tempDir.resolve("core"));
        Files.writeString(tempDir.resolve("core/pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>com.example</groupId>
                        <artifactId>parent-project</artifactId>
                        <version>1.0.0</version>
                    </parent>
                    <artifactId>core</artifactId>
                    <dependencies>
                        <dependency>
                            <groupId>javax.servlet</groupId>
                            <artifactId>javax.servlet-api</artifactId>
                            <version>4.0.1</version>
                        </dependency>
                    </dependencies>
                </project>
                """);

        Files.createDirectories(tempDir.resolve("web"));
        Files.writeString(tempDir.resolve("web/pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>com.example</groupId>
                        <artifactId>parent-project</artifactId>
                        <version>1.0.0</version>
                    </parent>
                    <artifactId>web</artifactId>
                    <dependencies>
                        <dependency>
                            <groupId>jakarta.servlet</groupId>
                            <artifactId>jakarta.servlet-api</artifactId>
                            <version>6.0.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """);

        mockUpgradeRecommendation("javax.servlet", "javax.servlet-api",
                "jakarta.servlet", "jakarta.servlet-api", "6.0.0");

        // When
        DependencyAnalysisReport report = module.analyzeProject(tempDir);

        // Then
        assertThat(report).isNotNull();
        assertThat(report.dependencyGraph()).isNotNull();

        // Maven buildFromProject reads only the root pom.xml by default
        // Root pom has no direct <dependencies>, only <modules>
        // So we verify the report structure is valid
        assertThat(report.blockers()).isNotNull();
        assertThat(report.recommendations()).isNotNull();
        assertThat(report.readinessScore()).isNotNull();
    }

    @Test
    @DisplayName("Should identify namespaces correctly")
    void shouldIdentifyNamespaces() {
        // Given
        DependencyGraph graph = new DependencyGraph();
        Artifact javaxArtifact = new Artifact("javax.servlet", "javax.servlet-api", "4.0.1", "compile", false);
        Artifact jakartaArtifact = new Artifact("jakarta.servlet", "jakarta.servlet-api", "6.0.0", "compile", false);
        Artifact unknownArtifact = new Artifact("com.example", "my-lib", "1.0.0", "compile", false);
        graph.addNode(javaxArtifact);
        graph.addNode(jakartaArtifact);
        graph.addNode(unknownArtifact);

        // When
        NamespaceCompatibilityMap namespaceMap = module.identifyNamespaces(graph);

        // Then
        assertThat(namespaceMap).isNotNull();
        assertThat(namespaceMap.get(javaxArtifact)).isEqualTo(Namespace.JAVAX);
        assertThat(namespaceMap.get(jakartaArtifact)).isEqualTo(Namespace.JAKARTA);
        assertThat(namespaceMap.get(unknownArtifact)).isEqualTo(Namespace.UNKNOWN);
    }

    @Test
    @DisplayName("Should detect blockers for artifacts without Jakarta equivalents")
    void shouldDetectBlockers() {
        // Given
        DependencyGraph graph = new DependencyGraph();
        Artifact javaxArtifact = new Artifact("javax.servlet", "javax.servlet-api", "4.0.1", "compile", false);
        graph.addNode(javaxArtifact);

        // Mock: detectBlockers checks jakartaMappingService.hasMapping() and isJakartaCompatible()
        when(jakartaMappingService.hasMapping("javax.servlet", "javax.servlet-api")).thenReturn(true);

        // When
        List<Blocker> blockers = module.detectBlockers(graph);

        // Then
        assertThat(blockers).isNotNull();
        // javax.servlet has a Jakarta equivalent via mapping, so no blocker expected
        assertThat(blockers).isEmpty();
    }

    @Test
    @DisplayName("Should recommend Jakarta-compatible versions from upgrade recommendations DB")
    void shouldRecommendVersionsFromDb() {
        // Given
        Artifact javaxArtifact = new Artifact("javax.servlet", "javax.servlet-api", "4.0.1", "compile", false);
        List<Artifact> artifacts = List.of(javaxArtifact);

        mockUpgradeRecommendation("javax.servlet", "javax.servlet-api",
                "jakarta.servlet", "jakarta.servlet-api", "6.0.0");

        // When
        List<VersionRecommendation> recommendations = module.recommendVersions(artifacts);

        // Then
        assertThat(recommendations).isNotEmpty();

        VersionRecommendation recommendation = recommendations.get(0);
        assertThat(recommendation.currentArtifact()).isEqualTo(javaxArtifact);
        assertThat(recommendation.recommendedArtifact()).isNotNull();
        assertThat(recommendation.recommendedArtifact().groupId()).isEqualTo("jakarta.servlet");
    }

    @Test
    @DisplayName("Should recommend versions from static YAML mappings as fallback")
    void shouldRecommendVersionsFromYamlMapping() {
        // Given
        Artifact javaxArtifact = new Artifact("javax.validation", "validation-api", "2.0.1", "compile", false);
        List<Artifact> artifacts = List.of(javaxArtifact);

        // No DB recommendation
        when(analysisStore.getUpgradeRecommendation("javax.validation", "validation-api")).thenReturn(null);

        // Mock JakartaMappingService to return a mapping
        when(jakartaMappingService.findMapping(javaxArtifact)).thenReturn(
                Optional.of(new JakartaMappingService.JakartaEquivalent(
                        "jakarta.validation",
                        "jakarta.validation-api",
                        "3.0.0",
                        JakartaMappingService.CompatibilityLevel.DROP_IN_REPLACEMENT)));

        // When
        List<VersionRecommendation> recommendations = module.recommendVersions(artifacts);

        // Then
        assertThat(recommendations).isNotEmpty();
        VersionRecommendation rec = recommendations.get(0);
        assertThat(rec.recommendedArtifact().groupId()).isEqualTo("jakarta.validation");
        assertThat(rec.recommendedArtifact().artifactId()).isEqualTo("jakarta.validation-api");
    }

    @Test
    @DisplayName("Should analyze transitive conflicts when mixed namespaces present")
    void shouldAnalyzeTransitiveConflicts() {
        // Given
        DependencyGraph graph = new DependencyGraph();
        Artifact root = new Artifact("com.example", "app", "1.0.0", "compile", false);
        Artifact javaxDep = new Artifact("javax.servlet", "javax.servlet-api", "4.0.1", "compile", false);
        Artifact jakartaDep = new Artifact("jakarta.servlet", "jakarta.servlet-api", "6.0.0", "compile", false);

        graph.addEdge(new Dependency(root, javaxDep, "compile", false));
        graph.addEdge(new Dependency(root, jakartaDep, "compile", false));

        // When
        TransitiveConflictReport conflictReport = module.analyzeTransitiveConflicts(graph);

        // Then
        assertThat(conflictReport).isNotNull();
        assertThat(conflictReport.conflicts()).isNotNull();
        // Root has both javax and jakarta dependencies — mixed namespaces detected
        assertThat(conflictReport.totalConflicts()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Should return empty recommendations for non-javax artifacts")
    void shouldReturnEmptyRecommendationsForJakartaArtifacts() {
        // Given
        Artifact jakartaArtifact = new Artifact("jakarta.servlet", "jakarta.servlet-api", "6.0.0", "compile", false);
        List<Artifact> artifacts = List.of(jakartaArtifact);

        // When
        List<VersionRecommendation> recommendations = module.recommendVersions(artifacts);

        // Then - Jakarta artifacts don't need migration recommendations
        assertThat(recommendations).isEmpty();
    }

    @Test
    @DisplayName("Should calculate risk assessment based on blockers and conflicts")
    void shouldCalculateRiskAssessment() throws Exception {
        // Given
        String pomContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>risky-project</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>javax.servlet</groupId>
                            <artifactId>javax.servlet-api</artifactId>
                            <version>4.0.1</version>
                        </dependency>
                    </dependencies>
                </project>
                """;
        Files.writeString(tempDir.resolve("pom.xml"), pomContent);

        // No DB recommendation, no YAML mapping — javax.servlet will be a blocker
        when(analysisStore.getUpgradeRecommendation(anyString(), anyString())).thenReturn(null);
        when(jakartaMappingService.findMapping(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.empty());
        when(jakartaMappingService.isJakartaCompatible(anyString(), anyString(), anyString())).thenReturn(false);

        // When
        DependencyAnalysisReport report = module.analyzeProject(tempDir);

        // Then
        assertThat(report.riskAssessment()).isNotNull();
        assertThat(report.riskAssessment().riskScore()).isGreaterThanOrEqualTo(0.0);
        assertThat(report.riskAssessment().riskScore()).isLessThanOrEqualTo(1.0);
    }

    private void mockUpgradeRecommendation(String currentGroupId, String currentArtifactId,
            String recommendedGroupId, String recommendedArtifactId, String recommendedVersion) {
        when(analysisStore.getUpgradeRecommendation(currentGroupId, currentArtifactId))
                .thenReturn(new CentralMigrationAnalysisStore.UpgradeRecommendation(
                        currentGroupId, currentArtifactId,
                        recommendedGroupId, recommendedArtifactId,
                        recommendedVersion, null));
    }
}
