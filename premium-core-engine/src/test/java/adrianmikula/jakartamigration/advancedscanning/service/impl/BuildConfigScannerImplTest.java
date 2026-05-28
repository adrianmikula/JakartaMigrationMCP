package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.BuildConfigUsage;
import adrianmikula.jakartamigration.advancedscanning.domain.FileScanResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for BuildConfigScannerImpl, including Eclipse .classpath parsing.
 */
class BuildConfigScannerImplTest {

    private BuildConfigScannerImpl scanner;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        scanner = new BuildConfigScannerImpl();
    }

    @Test
    void shouldParsePomXmlWithJavaxDependencies() throws Exception {
        // Given
        Path pomFile = tempDir.resolve("pom.xml");
        String content = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <dependencies>
                    <dependency>
                        <groupId>javax.servlet</groupId>
                        <artifactId>javax.servlet-api</artifactId>
                        <version>4.0.1</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        Files.writeString(pomFile, content);

        // When
        FileScanResult<BuildConfigUsage> result = scanner.scanFile(pomFile);

        // Then
        assertThat(result.hasIssues()).isTrue();
        assertThat(result.usages()).hasSize(1);
        assertThat(result.usages().get(0).groupId()).isEqualTo("javax.servlet");
        assertThat(result.usages().get(0).artifactId()).isEqualTo("javax.servlet-api");
    }

    @Test
    void shouldParseEclipseClasspathWithJavaxJars() throws Exception {
        // Given
        Path classpathFile = tempDir.resolve(".classpath");
        String content = """
            <?xml version="1.0" encoding="UTF-8"?>
            <classpath>
                <classpathentry kind="src" path="src"/>
                <classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER"/>
                <classpathentry kind="lib" path="lib/javax.servlet-api-3.1.0.jar"/>
                <classpathentry kind="lib" path="lib/javax.persistence-api-2.2.jar"/>
                <classpathentry kind="output" path="bin"/>
            </classpath>
            """;
        Files.writeString(classpathFile, content);

        // When
        FileScanResult<BuildConfigUsage> result = scanner.scanFile(classpathFile);

        // Then
        assertThat(result.hasIssues()).isTrue();
        assertThat(result.usages()).hasSize(2);

        // Verify first JAR
        BuildConfigUsage first = result.usages().get(0);
        assertThat(first.artifactId()).contains("javax.servlet");

        // Verify second JAR
        BuildConfigUsage second = result.usages().get(1);
        assertThat(second.artifactId()).contains("javax.persistence");
    }

    @Test
    void shouldIgnoreNonJavaxEclipseClasspathEntries() throws Exception {
        // Given
        Path classpathFile = tempDir.resolve(".classpath");
        String content = """
            <?xml version="1.0" encoding="UTF-8"?>
            <classpath>
                <classpathentry kind="src" path="src"/>
                <classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER"/>
                <classpathentry kind="lib" path="lib/commons-lang3-3.12.0.jar"/>
                <classpathentry kind="output" path="bin"/>
            </classpath>
            """;
        Files.writeString(classpathFile, content);

        // When
        FileScanResult<BuildConfigUsage> result = scanner.scanFile(classpathFile);

        // Then
        assertThat(result.hasIssues()).isFalse();
        assertThat(result.usages()).isEmpty();
    }

    @Test
    void shouldReturnEmptyForUnknownFileType() throws Exception {
        // Given
        Path unknownFile = tempDir.resolve("random.txt");
        Files.writeString(unknownFile, "some content");

        // When
        FileScanResult<BuildConfigUsage> result = scanner.scanFile(unknownFile);

        // Then
        assertThat(result.hasIssues()).isFalse();
        assertThat(result.usages()).isEmpty();
    }
}
