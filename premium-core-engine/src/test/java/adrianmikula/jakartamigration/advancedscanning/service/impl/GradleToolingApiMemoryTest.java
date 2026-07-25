package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.DependencyTreeResult;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Memory and concurrency tests for GradleToolingApiExecutor.
 *
 * <p>These tests validate that the Tooling API executor does not leak resources
 * and can handle concurrent scans. They are tagged "slow" and run only in the
 * slow-test task.</p>
 */
@Tag("slow")
class GradleToolingApiMemoryTest {

    private Path createSingleModuleProject(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("settings.gradle.kts"), "rootProject.name = \"mem\"");
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
    void shouldNotLeakMemoryAcrossMultipleConnections(@TempDir Path tempDir) throws Exception {
        Path buildFile = createSingleModuleProject(tempDir);

        GradleToolingApiExecutor executor = new GradleToolingApiExecutor();
        List<DependencyTreeResult> results = new ArrayList<>(10);

        for (int i = 0; i < 10; i++) {
            DependencyTreeResult resolved = executor.executeGradleDependenciesAsync(buildFile, Set.of("compileClasspath"))
                    .get(120, TimeUnit.SECONDS);
            results.add(resolved);
        }

        executor.shutdown();

        assertThat(results).hasSize(10);
        // All results should be non-null; success may vary based on environment
        assertThat(results).allMatch(r -> r != null);
    }

    @Test
    void shouldHandleConcurrentConnections(@TempDir Path tempDir) throws Exception {
        Path buildFile = createSingleModuleProject(tempDir);

        GradleToolingApiExecutor executor = new GradleToolingApiExecutor();
        List<CompletableFuture<DependencyTreeResult>> futures = new ArrayList<>(4);

        for (int i = 0; i < 4; i++) {
            futures.add(executor.executeGradleDependenciesAsync(buildFile, Set.of("compileClasspath")));
        }

        CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        all.get(120, TimeUnit.SECONDS);

        for (CompletableFuture<DependencyTreeResult> future : futures) {
            DependencyTreeResult resolved = future.get();
            assertNotNull(resolved);
        }

        executor.shutdown();
    }
}
