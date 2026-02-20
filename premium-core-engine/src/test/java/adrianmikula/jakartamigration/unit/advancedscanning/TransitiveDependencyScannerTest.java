package adrianmikula.jakartamigration.unit.advancedscanning;

import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyScanResult;
import adrianmikula.jakartamigration.advancedscanning.service.impl.TransitiveDependencyScannerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TransitiveDependencyScannerTest {

    private TransitiveDependencyScannerImpl scanner;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        scanner = new TransitiveDependencyScannerImpl();
    }

    @Test
    void shouldReturnEmptyForNullPath() {
        TransitiveDependencyProjectScanResult result = scanner.scanProject(null);
        assertNotNull(result);
        assertEquals(0, result.totalBuildFilesScanned());
    }

    @Test
    void shouldReturnEmptyForNonExistentPath() {
        TransitiveDependencyProjectScanResult result = scanner.scanProject(Path.of("/nonexistent/path"));
        assertNotNull(result);
        assertEquals(0, result.totalBuildFilesScanned());
    }

    @Test
    void shouldScanEmptyProject() throws Exception {
        Path projectDir = tempDir.resolve("emptyProject");
        Files.createDirectory(projectDir);

        TransitiveDependencyProjectScanResult result = scanner.scanProject(projectDir);
        assertNotNull(result);
        assertEquals(0, result.totalBuildFilesScanned());
    }

    @Test
    void shouldDetectJavaxDependencyInMavenPom() throws Exception {
        Path projectDir = tempDir.resolve("mavenProject");
        Files.createDirectory(projectDir);
        
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>my-project</artifactId>
                <version>1.0-SNAPSHOT</version>
                
                <dependencies>
                    <dependency>
                        <groupId>javax.xml.bind</groupId>
                        <artifactId>jaxb-api</artifactId>
                        <version>2.3.1</version>
                    </dependency>
                    <dependency>
                        <groupId>javax.activation</groupId>
                        <artifactId>activation</artifactId>
                        <version>1.1.1</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        
        Path pomFile = projectDir.resolve("pom.xml");
        Files.writeString(pomFile, pomContent);

        TransitiveDependencyProjectScanResult result = scanner.scanProject(projectDir);
        
        assertTrue(result.hasJavaxDependencies());
        assertEquals(1, result.filesWithJavaxDependencies());
        assertTrue(result.totalJavaxDependencies() >= 1);
        
        TransitiveDependencyScanResult fileResult = result.fileResults().get(0);
        assertEquals("Maven", fileResult.getBuildFileType());
    }

    @Test
    void shouldDetectJavaxDependencyInGradle() throws Exception {
        Path projectDir = tempDir.resolve("gradleProject");
        Files.createDirectory(projectDir);
        
        String gradleContent = """
            dependencies {
                implementation 'javax.xml.bind:jaxb-api:2.3.1'
                implementation 'javax.activation:activation:1.1.1'
                implementation 'org.glassfish.jaxb:jaxb-runtime:2.3.1'
            }
            """;
        
        Path gradleFile = projectDir.resolve("build.gradle");
        Files.writeString(gradleFile, gradleContent);

        TransitiveDependencyProjectScanResult result = scanner.scanProject(projectDir);
        
        assertTrue(result.hasJavaxDependencies());
        
        TransitiveDependencyScanResult fileResult = result.fileResults().get(0);
        assertEquals("Gradle", fileResult.getBuildFileType());
    }

    @Test
    void shouldProvideRecommendations() throws Exception {
        Path projectDir = tempDir.resolve("recommendationProject");
        Files.createDirectory(projectDir);
        
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <dependencies>
                    <dependency>
                        <groupId>javax.xml.bind</groupId>
                        <artifactId>jaxb-api</artifactId>
                        <version>2.3.1</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        
        Path pomFile = projectDir.resolve("pom.xml");
        Files.writeString(pomFile, pomContent);

        TransitiveDependencyProjectScanResult result = scanner.scanProject(projectDir);
        
        assertTrue(result.hasJavaxDependencies());
        
        TransitiveDependencyScanResult fileResult = result.fileResults().get(0);
        assertEquals(1, fileResult.usages().size());
        
        // Verify recommendation is provided
        assertNotNull(fileResult.usages().get(0).getRecommendation());
    }

    @Test
    void shouldHandleCleanProject() throws Exception {
        Path projectDir = tempDir.resolve("cleanProject");
        Files.createDirectory(projectDir);
        
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <dependencies>
                    <dependency>
                        <groupId>jakarta.xml.bind</groupId>
                        <artifactId>jakarta-xml-bind-api</artifactId>
                        <version>3.0.0</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        
        Path pomFile = projectDir.resolve("pom.xml");
        Files.writeString(pomFile, pomContent);

        TransitiveDependencyProjectScanResult result = scanner.scanProject(projectDir);
        
        // Should not have javax usage
        assertFalse(result.hasJavaxDependencies());
    }

    @Test
    void shouldScanMultipleBuildFiles() throws Exception {
        Path projectDir = tempDir.resolve("multiFileProject");
        Files.createDirectory(projectDir);
        
        // Maven pom.xml with javax
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <dependencies>
                    <dependency>
                        <groupId>javax.xml.bind</groupId>
                        <artifactId>jaxb-api</artifactId>
                    </dependency>
                </dependencies>
            </project>
            """;
        
        // Gradle without javax
        String gradleContent = """
            dependencies {
                implementation 'jakarta.servlet:jakarta.servlet-api:5.0.0'
            }
            """;
        
        Files.writeString(projectDir.resolve("pom.xml"), pomContent);
        Files.writeString(projectDir.resolve("build.gradle"), gradleContent);

        TransitiveDependencyProjectScanResult result = scanner.scanProject(projectDir);
        
        assertTrue(result.hasJavaxDependencies());
        assertEquals(2, result.totalBuildFilesScanned());
        assertEquals(1, result.filesWithJavaxDependencies());
    }

    @Test
    void shouldReturnEmptyForNonBuildFile() {
        TransitiveDependencyScanResult result = scanner.scanFile(Path.of("README.md"));
        assertNotNull(result);
        assertEquals(0, result.usages().size());
    }

    @Test
    void shouldDetectKnownProblematicDependencies() throws Exception {
        Path projectDir = tempDir.resolve("problematicProject");
        Files.createDirectory(projectDir);
        
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <dependencies>
                    <dependency>
                        <groupId>com.sun.xml.bind</groupId>
                        <artifactId>jaxb-impl</artifactId>
                        <version>2.3.0.1</version>
                    </dependency>
                    <dependency>
                        <groupId>com.sun.xml.messaging.saaj</groupId>
                        <artifactId>saaj-impl</artifactId>
                        <version>1.5.0</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        
        Path pomFile = projectDir.resolve("pom.xml");
        Files.writeString(pomFile, pomContent);

        TransitiveDependencyProjectScanResult result = scanner.scanProject(projectDir);
        
        assertTrue(result.hasJavaxDependencies());
        
        TransitiveDependencyScanResult fileResult = result.fileResults().get(0);
        assertTrue(fileResult.usages().size() >= 2);
    }
}
