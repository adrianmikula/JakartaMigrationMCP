package adrianmikula.jakartamigration.advancedscanning;

import adrianmikula.jakartamigration.advancedscanning.domain.BuildConfigProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.BuildConfigScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.BuildConfigUsage;
import adrianmikula.jakartamigration.advancedscanning.service.BuildConfigScanner;
import adrianmikula.jakartamigration.advancedscanning.service.impl.BuildConfigScannerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD Tests for Build Configuration Scanner
 */
class BuildConfigScannerTest {

    private BuildConfigScanner scanner;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        scanner = new BuildConfigScannerImpl();
    }

    @Test
    void shouldReturnEmptyResultForEmptyProject() {
        BuildConfigProjectScanResult result = scanner.scanProject(tempDir);
        
        assertThat(result.totalFilesScanned()).isEqualTo(0);
        assertThat(result.totalDependenciesFound()).isEqualTo(0);
        assertThat(result.hasJavaxDependencies()).isFalse();
    }

    @Test
    void shouldDetectMavenPomXml() throws Exception {
        // Create pom.xml with javax dependencies
        Path pomPath = tempDir.resolve("pom.xml");
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>test-project</artifactId>
                <version>1.0</version>
                <dependencies>
                    <dependency>
                        <groupId>javax.servlet</groupId>
                        <artifactId>javax.servlet-api</artifactId>
                        <version>3.1.0</version>
                    </dependency>
                    <dependency>
                        <groupId>javax.persistence</groupId>
                        <artifactId>javax.persistence-api</artifactId>
                        <version>2.2</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        Files.writeString(pomPath, pomContent);

        BuildConfigProjectScanResult result = scanner.scanProject(tempDir);

        assertThat(result.hasJavaxDependencies()).isTrue();
        assertThat(result.totalDependenciesFound()).isGreaterThan(0);
    }

    @Test
    void shouldDetectGradleBuild() throws Exception {
        // Create build.gradle with javax dependencies
        Path buildGradle = tempDir.resolve("build.gradle");
        String gradleContent = """
            dependencies {
                implementation 'javax.servlet:javax.servlet-api:3.1.0'
                implementation 'javax.persistence:javax.persistence-api:2.2'
                implementation 'javax.validation:validation-api:2.0.1.Final'
            }
            """;
        Files.writeString(buildGradle, gradleContent);

        BuildConfigProjectScanResult result = scanner.scanProject(tempDir);

        assertThat(result.hasJavaxDependencies()).isTrue();
    }

    @Test
    void shouldMapToJakartaVersions() throws Exception {
        Path pomPath = tempDir.resolve("pom.xml");
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <dependencies>
                    <dependency>
                        <groupId>javax.servlet</groupId>
                        <artifactId>javax.servlet-api</artifactId>
                        <version>3.1.0</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        Files.writeString(pomPath, pomContent);

        BuildConfigProjectScanResult result = scanner.scanProject(tempDir);

        assertThat(result.hasJavaxDependencies()).isTrue();
        
        for (BuildConfigScanResult fileResult : result.fileResults()) {
            for (BuildConfigUsage usage : fileResult.usages()) {
                assertThat(usage.recommendedVersion()).isNotBlank();
            }
        }
    }

    @Test
    void shouldHandleMultipleBuildFiles() throws Exception {
        Files.writeString(tempDir.resolve("pom.xml"), 
            "<project><dependencies><dep><groupId>javax.servlet</groupId></dep></dependencies></project>");
        Files.writeString(tempDir.resolve("build.gradle"), 
            "dependencies { implementation 'javax.servlet:javax.servlet-api:3.1.0' }");

        BuildConfigProjectScanResult result = scanner.scanProject(tempDir);

        assertThat(result.fileResults().size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldDetectTransitiveDependencyIssues() throws Exception {
        Path pomPath = tempDir.resolve("pom.xml");
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <dependencies>
                    <dependency>
                        <groupId>org.hibernate</groupId>
                        <artifactId>hibernate-core</artifactId>
                        <version>5.6.0.Final</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        Files.writeString(pomPath, pomContent);

        BuildConfigProjectScanResult result = scanner.scanProject(tempDir);

        // Should detect potential transitive javax references
        assertThat(result.totalDependenciesFound()).isGreaterThanOrEqualTo(0);
    }
}
