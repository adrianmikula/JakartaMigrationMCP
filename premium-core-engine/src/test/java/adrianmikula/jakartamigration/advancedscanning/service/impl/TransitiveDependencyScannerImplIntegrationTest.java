package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyProjectScanResult;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Namespace;
import adrianmikula.jakartamigration.dependencyanalysis.service.NamespaceClassifier;
import adrianmikula.jakartamigration.dependencyanalysis.service.ImprovedMavenCentralLookupService;
import adrianmikula.jakartamigration.scanning.RecipeBasedClassifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TransitiveDependencyScannerImpl Integration Tests")
@Tag("slow")
class TransitiveDependencyScannerImplIntegrationTest {

    private TransitiveDependencyScannerImpl scanner;

    @BeforeEach
    void setUp() {
        NamespaceClassifier namespaceClassifier = new RecipeBasedClassifier();
        this.scanner = new TransitiveDependencyScannerImpl(
                new DependencyTreeCommandExecutorImpl(),
                new DependencyDeduplicationServiceImpl(),
                namespaceClassifier,
                null, null,
                new ImprovedMavenCentralLookupService()
        );
    }

    @Test
    @DisplayName("Should fallback to regex on Maven failure")
    void shouldFallbackToRegexOnMavenFailure(@TempDir Path tempDir) throws IOException {
        Path pomFile = tempDir.resolve("pom.xml");
        String pomContent = """
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                                      http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>test</groupId>
                <artifactId>maven-failure-test</artifactId>
                <version>1.0-SNAPSHOT</version>
                <dependencies>
                    <dependency>
                        <groupId>javax.servlet</groupId>
                        <artifactId>javax.servlet-api</artifactId>
                        <version>4.0.1</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        Files.writeString(pomFile, pomContent);

        TransitiveDependencyProjectScanResult result = scanner.scanProject(tempDir);

        assertThat(result).isNotNull();
        assertThat(result.getFileResults()).isNotEmpty();
        var usages = result.getFileResults().stream()
            .flatMap(r -> r.getUsages().stream())
            .toList();
        assertThat(usages).isNotEmpty();
    }

    @Test
    @DisplayName("Should fallback to regex on Gradle failure")
    void shouldFallbackToRegexOnGradleFailure(@TempDir Path tempDir) throws IOException {
        Path buildFile = tempDir.resolve("build.gradle");
        String buildContent = """
            plugins {
                id 'java'
            }
            repositories {
                mavenCentral()
            }
            dependencies {
                implementation 'javax.servlet:javax.servlet-api:4.0.1'
            }
            """;
        Files.writeString(buildFile, buildContent);

        TransitiveDependencyProjectScanResult result = scanner.scanProject(tempDir);

        assertThat(result).isNotNull();
        assertThat(result.getFileResults()).isNotEmpty();
    }

    @Test
    @DisplayName("Should classify dependencies after fallback")
    void shouldClassifyDependenciesAfterFallback(@TempDir Path tempDir) throws IOException {
        Path pomFile = tempDir.resolve("pom.xml");
        String pomContent = """
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                                      http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>test</groupId>
                <artifactId>classification-test</artifactId>
                <version>1.0-SNAPSHOT</version>
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
        Files.writeString(pomFile, pomContent);

        TransitiveDependencyProjectScanResult result = scanner.scanProject(tempDir);

        assertThat(result).isNotNull();
        assertThat(result.getFileResults()).isNotEmpty();

        var usages = result.getFileResults().stream()
            .flatMap(r -> r.getUsages().stream())
            .toList();

        assertThat(usages).isNotEmpty();

        boolean hasJavaxDep = usages.stream()
            .anyMatch(u -> "javax.servlet".equals(u.getGroupId()) &&
                           "javax.servlet-api".equals(u.getArtifactId()));
        boolean hasJakartaDep = usages.stream()
            .anyMatch(u -> "jakarta.servlet".equals(u.getGroupId()) &&
                           "jakarta.servlet-api".equals(u.getArtifactId()));

        assertThat(hasJavaxDep || hasJakartaDep)
            .as("Should find at least one servlet dependency")
            .isTrue();
    }

    @Test
    @DisplayName("Should scan Maven project with multiple dependencies")
    void shouldScanMavenProjectWithMultipleDependencies(@TempDir Path tempDir) throws IOException {
        Path pomFile = tempDir.resolve("pom.xml");
        String pomContent = """
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                                      http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>test</groupId>
                <artifactId>multi-dep-test</artifactId>
                <version>1.0-SNAPSHOT</version>
                <dependencies>
                    <dependency>
                        <groupId>javax.servlet</groupId>
                        <artifactId>javax.servlet-api</artifactId>
                        <version>4.0.1</version>
                    </dependency>
                    <dependency>
                        <groupId>javax.persistence</groupId>
                        <artifactId>javax.persistence-api</artifactId>
                        <version>2.2</version>
                    </dependency>
                    <dependency>
                        <groupId>javax.validation</groupId>
                        <artifactId>validation-api</artifactId>
                        <version>2.0.1</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        Files.writeString(pomFile, pomContent);

        TransitiveDependencyProjectScanResult result = scanner.scanProject(tempDir);

        assertThat(result).isNotNull();
        assertThat(result.getFileResults()).isNotEmpty();

        var usages = result.getFileResults().stream()
            .flatMap(r -> r.getUsages().stream())
            .toList();

        assertThat(usages).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("Should handle project with no build files gracefully")
    void shouldHandleProjectWithNoBuildFiles(@TempDir Path tempDir) throws IOException {
        Path readme = tempDir.resolve("README.md");
        Files.writeString(readme, "# Empty Project");

        TransitiveDependencyProjectScanResult result = scanner.scanProject(tempDir);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Should handle empty project directory")
    void shouldHandleEmptyProjectDirectory(@TempDir Path tempDir) throws IOException {
        TransitiveDependencyProjectScanResult result = scanner.scanProject(tempDir);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Should scan Maven project with jakarta dependencies")
    void shouldScanMavenProjectWithJakartaDependencies(@TempDir Path tempDir) throws IOException {
        Path pomFile = tempDir.resolve("pom.xml");
        String pomContent = """
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                                      http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>test</groupId>
                <artifactId>jakarta-test</artifactId>
                <version>1.0-SNAPSHOT</version>
                <dependencies>
                    <dependency>
                        <groupId>jakarta.servlet</groupId>
                        <artifactId>jakarta.servlet-api</artifactId>
                        <version>6.0.0</version>
                    </dependency>
                    <dependency>
                        <groupId>jakarta.persistence</groupId>
                        <artifactId>jakarta.persistence-api</artifactId>
                        <version>3.1.0</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        Files.writeString(pomFile, pomContent);

        TransitiveDependencyProjectScanResult result = scanner.scanProject(tempDir);

        assertThat(result).isNotNull();
        assertThat(result.getFileResults()).isNotEmpty();

        var usages = result.getFileResults().stream()
            .flatMap(r -> r.getUsages().stream())
            .toList();

        boolean hasJakartaDeps = usages.stream()
            .anyMatch(u -> u.getGroupId() != null && u.getGroupId().startsWith("jakarta."));

        assertThat(hasJakartaDeps)
            .as("Should find Jakarta dependencies")
            .isTrue();
    }

    @Test
    @DisplayName("Should scan Gradle project with dependencies")
    void shouldScanGradleProject(@TempDir Path tempDir) throws IOException {
        Path buildFile = tempDir.resolve("build.gradle");
        String buildContent = """
            plugins {
                id 'java-library'
            }
            repositories {
                mavenCentral()
            }
            dependencies {
                implementation 'javax.servlet:javax.servlet-api:4.0.1'
                implementation 'org.hibernate:hibernate-core:5.6.1.Final'
            }
            """;
        Files.writeString(buildFile, buildContent);

        TransitiveDependencyProjectScanResult result = scanner.scanProject(tempDir);

        assertThat(result).isNotNull();
        assertThat(result.getFileResults()).isNotEmpty();
    }
}
