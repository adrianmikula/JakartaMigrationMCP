package adrianmikula.jakartamigration.dependencyanalysis.util;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("fast")
class BuildFileDiscoveryTest {

    @TempDir
    Path tempDir;

    @Test
    void discoverBuildFiles_shouldFindProjectBuildFiles() throws IOException {
        Files.writeString(tempDir.resolve("build.gradle.kts"), "plugins { java }");

        List<Path> buildFiles = BuildFileDiscovery.discoverBuildFiles(tempDir);

        assertEquals(1, buildFiles.size());
        assertEquals("build.gradle.kts", buildFiles.get(0).getFileName().toString());
    }

    @Test
    void discoverBuildFiles_shouldExcludeBuildOutputDirectory() throws IOException {
        Path buildDir = tempDir.resolve("build");
        Files.createDirectories(buildDir);
        Files.writeString(buildDir.resolve("build.gradle"), "dependencies {}");

        Files.writeString(tempDir.resolve("build.gradle.kts"), "plugins { java }");

        List<Path> buildFiles = BuildFileDiscovery.discoverBuildFiles(tempDir);

        assertEquals(1, buildFiles.size());
        assertEquals("build.gradle.kts", buildFiles.get(0).getFileName().toString());
    }

    @Test
    void discoverBuildFiles_shouldExcludeGradleCacheDirectory() throws IOException {
        Path gradleCache = tempDir.resolve(".gradle");
        Files.createDirectories(gradleCache);
        Files.writeString(gradleCache.resolve("pom.xml"), "<project></project>");

        Files.writeString(tempDir.resolve("build.gradle.kts"), "plugins { java }");

        List<Path> buildFiles = BuildFileDiscovery.discoverBuildFiles(tempDir);

        assertEquals(1, buildFiles.size());
        assertEquals("build.gradle.kts", buildFiles.get(0).getFileName().toString());
    }

    @Test
    void discoverBuildFiles_shouldExcludeMavenTargetDirectory() throws IOException {
        Path targetDir = tempDir.resolve("target");
        Files.createDirectories(targetDir);
        Files.writeString(targetDir.resolve("pom.xml"), "<project></project>");

        Files.writeString(tempDir.resolve("pom.xml"), "<project></project>");

        List<Path> buildFiles = BuildFileDiscovery.discoverBuildFiles(tempDir);

        assertEquals(1, buildFiles.size());
        assertEquals("pom.xml", buildFiles.get(0).getFileName().toString());
    }

    @Test
    void discoverBuildFiles_shouldExcludeTestResources() throws IOException {
        Path testResources = tempDir.resolve("src/test/resources");
        Files.createDirectories(testResources);
        Files.writeString(testResources.resolve("test-build.gradle"), "dependencies {}");

        Files.writeString(tempDir.resolve("build.gradle.kts"), "plugins { java }");

        List<Path> buildFiles = BuildFileDiscovery.discoverBuildFiles(tempDir);

        assertEquals(1, buildFiles.size());
        assertEquals("build.gradle.kts", buildFiles.get(0).getFileName().toString());
    }

    @Test
    void discoverBuildFiles_shouldExcludeJacocoCachePomFiles() throws IOException {
        Path jacocoDir = tempDir.resolve("build/tmp/.cache/expanded/zip_123/META-INF/maven/org.jacoco/org.jacoco.agent");
        Files.createDirectories(jacocoDir);
        Files.writeString(jacocoDir.resolve("pom.xml"), "<project></project>");

        Files.writeString(tempDir.resolve("build.gradle.kts"), "plugins { java }");

        List<Path> buildFiles = BuildFileDiscovery.discoverBuildFiles(tempDir);

        assertEquals(1, buildFiles.size());
        assertEquals("build.gradle.kts", buildFiles.get(0).getFileName().toString());
    }

    @Test
    void discoverBuildFiles_shouldOnlyMatchBuildGradleNotSettingsGradle() throws IOException {
        Files.writeString(tempDir.resolve("build.gradle.kts"), "plugins { java }");
        Files.writeString(tempDir.resolve("settings.gradle.kts"), "rootProject.name = \"test\"");
        Files.writeString(tempDir.resolve("jacoco-report.gradle"), "apply plugin: 'jacoco'");

        List<Path> buildFiles = BuildFileDiscovery.discoverBuildFiles(tempDir);

        assertEquals(1, buildFiles.size());
        assertEquals("build.gradle.kts", buildFiles.get(0).getFileName().toString());
    }

    @Test
    void isGradleFile_shouldOnlyMatchBuildGradle() {
        assertTrue(BuildFileDiscovery.isGradleFile(Path.of("build.gradle")));
        assertTrue(BuildFileDiscovery.isGradleFile(Path.of("build.gradle.kts")));
        assertTrue(BuildFileDiscovery.isGradleFile(Path.of("/some/path/build.gradle")));
        assertTrue(BuildFileDiscovery.isGradleFile(Path.of("/some/path/build.gradle.kts")));

        assertFalse(BuildFileDiscovery.isGradleFile(Path.of("settings.gradle")));
        assertFalse(BuildFileDiscovery.isGradleFile(Path.of("settings.gradle.kts")));
        assertFalse(BuildFileDiscovery.isGradleFile(Path.of("jacoco-report.gradle")));
        assertFalse(BuildFileDiscovery.isGradleFile(Path.of("test-build-gradle-deep.gradle")));
        assertFalse(BuildFileDiscovery.isGradleFile(Path.of("pom.xml")));
    }

    @Test
    void isBuildFile_shouldMatchMavenAndGradle() {
        assertTrue(BuildFileDiscovery.isBuildFile(Path.of("pom.xml")));
        assertTrue(BuildFileDiscovery.isBuildFile(Path.of("build.gradle")));
        assertTrue(BuildFileDiscovery.isBuildFile(Path.of("build.gradle.kts")));

        assertFalse(BuildFileDiscovery.isBuildFile(Path.of("settings.gradle")));
        assertFalse(BuildFileDiscovery.isBuildFile(Path.of("readme.txt")));
        assertFalse(BuildFileDiscovery.isBuildFile(Path.of("Main.java")));
    }

    @Test
    void isMavenFile_shouldOnlyMatchPomXml() {
        assertTrue(BuildFileDiscovery.isMavenFile(Path.of("pom.xml")));
        assertTrue(BuildFileDiscovery.isMavenFile(Path.of("/some/path/pom.xml")));

        assertFalse(BuildFileDiscovery.isMavenFile(Path.of("build.gradle")));
        assertFalse(BuildFileDiscovery.isMavenFile(Path.of("readme.txt")));
    }

    @Test
    void discoverBuildFiles_shouldHandleNestedModules() throws IOException {
        Files.createDirectories(tempDir.resolve("module-a/src/main/java"));
        Files.createDirectories(tempDir.resolve("module-b/src/main/java"));

        Files.writeString(tempDir.resolve("build.gradle.kts"), "plugins { java }");
        Files.writeString(tempDir.resolve("module-a/build.gradle.kts"), "plugins { java }");
        Files.writeString(tempDir.resolve("module-b/build.gradle.kts"), "plugins { java }");

        Files.createDirectories(tempDir.resolve("module-a/build/tmp/.cache"));
        Files.writeString(tempDir.resolve("module-a/build/tmp/.cache/pom.xml"), "<project></project>");

        List<Path> buildFiles = BuildFileDiscovery.discoverBuildFiles(tempDir);

        assertEquals(3, buildFiles.size());
        assertTrue(buildFiles.stream().noneMatch(p -> p.toString().contains("build/tmp")));
    }

    @Test
    void discoverBuildFiles_shouldFindPomXmlInSubmodules() throws IOException {
        Files.createDirectories(tempDir.resolve("module-a"));
        Files.createDirectories(tempDir.resolve("module-b"));

        Files.writeString(tempDir.resolve("pom.xml"), "<project><modules><module>module-a</module></modules></project>");
        Files.writeString(tempDir.resolve("module-a/pom.xml"), "<project></project>");
        Files.writeString(tempDir.resolve("module-b/pom.xml"), "<project></project>");

        List<Path> buildFiles = BuildFileDiscovery.discoverBuildFiles(tempDir);

        assertEquals(3, buildFiles.size());
        assertTrue(buildFiles.stream().allMatch(p -> p.getFileName().toString().equals("pom.xml")));
    }
}
