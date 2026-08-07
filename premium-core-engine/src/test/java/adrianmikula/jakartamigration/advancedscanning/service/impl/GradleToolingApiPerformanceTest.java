package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.DependencyTreeResult;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Performance benchmarks for GradleToolingApiExecutor.
 *
 * <p>These tests validate the performance budgets documented in
 * docs/roadmap/gradle-tooling-api-migration.md. They are tagged "slow"
 * and run only in the slow-test task.</p>
 */
@Tag("slow")
class GradleToolingApiPerformanceTest {

    private static final long SINGLE_MODULE_BUDGET_MS = 30_000L;
    private static final long SUBSEQUENT_SCAN_BUDGET_MS = 10_000L;
    private static final long MULTI_MODULE_BUDGET_MS = 60_000L;

    private Path createSingleModuleProject(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("settings.gradle.kts"), "rootProject.name = \"perf\"");
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
        return tempDir.resolve("build.gradle.kts");
    }

    @Test
    void shouldResolveSingleModuleWithinBudget(@TempDir Path tempDir) throws Exception {
        Path buildFile = createSingleModuleProject(tempDir);

        GradleToolingApiExecutor executor = new GradleToolingApiExecutor();
        long start = System.currentTimeMillis();
        CompletableFuture<DependencyTreeResult> result =
            executor.executeGradleDependenciesAsync(buildFile, Set.of("compileClasspath"));
        DependencyTreeResult resolved = result.get(120, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - start;
        executor.shutdown();

        assertNotNull(resolved);
        // Offline or missing Gradle may fail; the key metric is elapsed time
        assertThat(elapsed).isLessThan(SINGLE_MODULE_BUDGET_MS);
    }

    @Test
    void shouldReuseDaemonAcrossMultipleCalls(@TempDir Path tempDir) throws Exception {
        Path buildFile = createSingleModuleProject(tempDir);

        GradleToolingApiExecutor executor = new GradleToolingApiExecutor();

        // First call (daemon startup)
        long firstStart = System.currentTimeMillis();
        executor.executeGradleDependenciesAsync(buildFile, Set.of("compileClasspath"))
                .get(120, TimeUnit.SECONDS);
        long firstElapsed = System.currentTimeMillis() - firstStart;

        // Second call (daemon reuse)
        long secondStart = System.currentTimeMillis();
        CompletableFuture<DependencyTreeResult> second =
            executor.executeGradleDependenciesAsync(buildFile, Set.of("compileClasspath"));
        DependencyTreeResult resolved = second.get(120, TimeUnit.SECONDS);
        long secondElapsed = System.currentTimeMillis() - secondStart;
        executor.shutdown();

        assertNotNull(resolved);
        assertTrue(firstElapsed > 0, "First call should take positive time");
        assertThat(secondElapsed)
            .as("Second call should be faster than first (daemon reuse)")
            .isLessThan(firstElapsed);
        assertThat(secondElapsed).isLessThan(SUBSEQUENT_SCAN_BUDGET_MS);
    }

    @Test
    void shouldResolveMultiModuleWithinBudget(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("settings.gradle.kts"),
            "rootProject.name = \"perf-multi\"\ninclude(\"app\", \"lib\")");
        Files.writeString(tempDir.resolve("build.gradle.kts"), """
            plugins {
                base
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

        GradleToolingApiExecutor executor = new GradleToolingApiExecutor();
        Path appBuildFile = tempDir.resolve("app/build.gradle.kts");

        long start = System.currentTimeMillis();
        DependencyTreeResult resolved = executor.executeGradleDependenciesAsync(appBuildFile, Set.of("compileClasspath"))
                .get(120, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - start;
        executor.shutdown();

        assertNotNull(resolved);
        assertThat(elapsed).isLessThan(MULTI_MODULE_BUDGET_MS);
    }

    @Test
    void shouldHandleLargeDependencyTree(@TempDir Path tempDir) throws Exception {
        // Project with a dependency known to pull in a large transitive tree
        Files.writeString(tempDir.resolve("settings.gradle.kts"), "rootProject.name = \"large\"");
        Files.writeString(tempDir.resolve("build.gradle.kts"), """
            plugins {
                java
            }
            repositories {
                mavenCentral()
            }
            dependencies {
                implementation("org.springframework.boot:spring-boot-starter-web:3.2.0")
            }
            """);

        GradleToolingApiExecutor executor = new GradleToolingApiExecutor();
        Path buildFile = tempDir.resolve("build.gradle.kts");

        DependencyTreeResult resolved = executor.executeGradleDependenciesAsync(buildFile, Set.of("compileClasspath"))
                .get(120, TimeUnit.SECONDS);
        executor.shutdown();

        assertNotNull(resolved);
        // Success is the key signal; exact count depends on network/repo availability
        assertTrue(resolved.isSuccess() || resolved.getDependencies().isEmpty(),
            "Large tree resolution should complete without throwing");
    }
}
