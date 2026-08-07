package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.DependencyTreeResult;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for GradleToolingApiExecutor.
 * Tests Tooling API-based dependency resolution.
 */
@Tag("slow")
class GradleToolingApiExecutorTest {

    @Test
    void shouldReturnErrorWhenMavenRequested() {
        GradleToolingApiExecutor executor = new GradleToolingApiExecutor();
        Path dummyPom = Path.of("/tmp/dummy/pom.xml");
        
        CompletableFuture<DependencyTreeResult> result = 
            executor.executeMavenDependencyTreeAsync(dummyPom, Set.of("compile"));
        
        DependencyTreeResult resolved = result.join();
        assertFalse(resolved.isSuccess());
        assertThat(resolved.getErrorMessage()).contains("Maven dependency resolution is not supported");
        
        executor.shutdown();
    }

    @Test
    void shouldReturnErrorWhenBuildFileHasNoParent() {
        GradleToolingApiExecutor executor = new GradleToolingApiExecutor();
        Path buildFile = Path.of("build.gradle"); // No parent directory
        
        CompletableFuture<DependencyTreeResult> result = 
            executor.executeGradleDependenciesAsync(buildFile, Set.of("compileClasspath"));
        
        DependencyTreeResult resolved = result.join();
        assertFalse(resolved.isSuccess());
        assertThat(resolved.getErrorMessage()).contains("Invalid path");
        
        executor.shutdown();
    }

    @Test
    void shouldReturnErrorForNonBuildGradleFile(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("settings.gradle.kts"), "rootProject.name = \"test\"");
        Files.writeString(tempDir.resolve("build.gradle.kts"), "plugins { java }");

        GradleToolingApiExecutor executor = new GradleToolingApiExecutor();
        Path settingsFile = tempDir.resolve("settings.gradle.kts");

        CompletableFuture<DependencyTreeResult> result =
            executor.executeGradleDependenciesAsync(settingsFile, Set.of("compileClasspath"));

        DependencyTreeResult resolved = result.join();
        executor.shutdown();

        assertFalse(resolved.isSuccess());
        assertThat(resolved.getErrorMessage()).contains("Not a build.gradle build file");
    }

    @Test
    void findGradleProjectRoot_shouldFindSettingsFile(@TempDir Path tempDir) throws Exception {
        // Create settings.gradle.kts
        Files.writeString(tempDir.resolve("settings.gradle.kts"), "rootProject.name = \"test\"");
        
        Path result = GradleToolingApiExecutor.findGradleProjectRoot(tempDir);
        assertEquals(tempDir, result);
    }

    @Test
    void findGradleProjectRoot_shouldWalkUpToFindSettings(@TempDir Path tempDir) throws Exception {
        // Create nested directory structure
        Path subDir = tempDir.resolve("a/b/c");
        Files.createDirectories(subDir);
        
        // Create settings.gradle.kts in root
        Files.writeString(tempDir.resolve("settings.gradle.kts"), "rootProject.name = \"test\"");
        
        Path result = GradleToolingApiExecutor.findGradleProjectRoot(subDir);
        assertEquals(tempDir, result);
    }

    @Test
    void findGradleProjectRoot_shouldReturnNullWhenNoSettings(@TempDir Path tempDir) {
        Path result = GradleToolingApiExecutor.findGradleProjectRoot(tempDir);
        assertNull(result);
    }

    @Test
    void computeGradleModuleName_shouldHandleSingleLevel(@TempDir Path tempDir) {
        Path subDir = tempDir.resolve("community-core-engine");
        String moduleName = GradleToolingApiExecutor.computeGradleModuleName(tempDir, subDir);
        assertEquals(":community-core-engine", moduleName);
    }

    @Test
    void computeGradleModuleName_shouldHandleNestedPath(@TempDir Path tempDir) {
        Path subDir = tempDir.resolve("a/b/community-core-engine");
        String moduleName = GradleToolingApiExecutor.computeGradleModuleName(tempDir, subDir);
        assertEquals(":a:b:community-core-engine", moduleName);
    }

    @Test
    void shouldHandleEmptyOutput() {
        GradleToolingApiExecutor executor = new GradleToolingApiExecutor();
        
        var deps = executor.parseGradleOutput("", Set.of());
        
        assertThat(deps).isEmpty();
        
        executor.shutdown();
    }

    @Test
    void shouldResolveDependenciesForSingleModule(@TempDir Path tempDir) throws Exception {
        // Create a minimal single-module Gradle project with the real wrapper
        Files.writeString(tempDir.resolve("settings.gradle.kts"), "rootProject.name = \"single\"");
        Files.writeString(tempDir.resolve("build.gradle.kts"), """
            plugins {
                java
            }
            repositories {
                mavenCentral()
            }
            dependencies {
                implementation("org.slf4j:slf4j-api:2.0.9")
            }
            """);
        copyProjectWrapper(tempDir);

        GradleToolingApiExecutor executor = new GradleToolingApiExecutor();
        Path buildFile = tempDir.resolve("build.gradle.kts");

        CompletableFuture<DependencyTreeResult> result =
            executor.executeGradleDependenciesAsync(buildFile, Set.of("compileClasspath"));

        DependencyTreeResult resolved = result.join();
        executor.shutdown();

        assertNotNull(resolved);
        assertTrue(resolved.isSuccess(),
            "Tooling API should resolve a single-module project: " + resolved.getErrorMessage());
        assertThat(resolved.getDependencies()).isNotEmpty();
    }

    @Test
    void shouldResolveAllSubprojectsInMultiModule(@TempDir Path tempDir) throws Exception {
        // Create minimal multi-module Gradle project with the real wrapper
        Files.writeString(tempDir.resolve("settings.gradle.kts"), "rootProject.name = \"multi\"\ninclude(\"app\", \"lib\")");
        Files.writeString(tempDir.resolve("build.gradle.kts"), """
            plugins {
                base
            }
            repositories {
                mavenCentral()
            }
            """);
        Files.createDirectories(tempDir.resolve("app"));
        Files.writeString(tempDir.resolve("app/build.gradle.kts"), """
            plugins {
                java
            }
            repositories {
                mavenCentral()
            }
            dependencies {
                implementation(project(":lib"))
                implementation("com.google.guava:guava:32.1.3-jre")
            }
            """);
        Files.createDirectories(tempDir.resolve("lib"));
        Files.writeString(tempDir.resolve("lib/build.gradle.kts"), """
            plugins {
                java
                `java-library`
            }
            repositories {
                mavenCentral()
            }
            dependencies {
                api("org.apache.commons:commons-lang3:3.14.0")
            }
            """);
        copyProjectWrapper(tempDir);

        GradleToolingApiExecutor executor = new GradleToolingApiExecutor();
        Path appBuildFile = tempDir.resolve("app/build.gradle.kts");

        CompletableFuture<DependencyTreeResult> result =
            executor.executeGradleDependenciesAsync(appBuildFile, Set.of("compileClasspath"));

        DependencyTreeResult resolved = result.join();
        executor.shutdown();

        assertNotNull(resolved);
        assertTrue(resolved.isSuccess(),
            "Tooling API should resolve a multi-module project: " + resolved.getErrorMessage());
        assertThat(resolved.getDependencies()).isNotEmpty();
    }

    private static Path findProjectRoot() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null && dir.getNameCount() > 0) {
            if (Files.exists(dir.resolve("settings.gradle.kts")) && Files.exists(dir.resolve("gradlew"))) {
                return dir;
            }
            dir = dir.getParent();
        }
        return Path.of("").toAbsolutePath().getParent();
    }

    private static void copyProjectWrapper(Path targetDir) throws Exception {
        Path projectRoot = findProjectRoot();
        Path gradlew = projectRoot.resolve("gradlew");
        Path gradlewBat = projectRoot.resolve("gradlew.bat");
        Path wrapperDir = projectRoot.resolve("gradle/wrapper");

        assumeTrue(Files.exists(gradlew), "Project gradlew wrapper must exist for this test");
        assumeTrue(Files.exists(wrapperDir), "Project gradle/wrapper directory must exist for this test");

        Path targetWrapperDir = targetDir.resolve("gradle/wrapper");
        Files.createDirectories(targetWrapperDir);

        copyExecutable(gradlew, targetDir.resolve("gradlew"));
        if (Files.exists(gradlewBat)) {
            Files.copy(gradlewBat, targetDir.resolve("gradlew.bat"), StandardCopyOption.REPLACE_EXISTING);
        }
        try (var stream = Files.newDirectoryStream(wrapperDir)) {
            for (Path source : stream) {
                Files.copy(source, targetWrapperDir.resolve(source.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static void copyExecutable(Path source, Path target) throws Exception {
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        Set<PosixFilePermission> perms = Files.getPosixFilePermissions(source);
        Files.setPosixFilePermissions(target, perms);
    }
}
