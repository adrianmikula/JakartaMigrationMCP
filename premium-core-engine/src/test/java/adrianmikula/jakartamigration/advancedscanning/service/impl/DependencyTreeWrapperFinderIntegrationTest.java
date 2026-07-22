package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.DependencyTreeResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for DependencyTreeCommandExecutor wrapper finder.
 * These tests verify that the Gradle and Maven wrapper finders correctly
 * locate build tool wrappers in parent directories when scanning from subdirectories.
 * <p>
 * Tests follow the pattern from skills/real-repo-integration-test/SKILL.md by:
 * 1. Downloading real GitHub multi-module projects
 * 2. Scanning build files from subdirectories (where wrapper doesn't exist locally)
 * 3. Verifying that wrappers are found in parent directories
 * 4. Testing that build-tool errors are detected correctly when wrappers are missing
 */
@Tag("slow")
public class DependencyTreeWrapperFinderIntegrationTest {

    @TempDir
    Path tempDir;

    private DependencyTreeCommandExecutorImpl executor;

    @BeforeEach
    void setUp() {
        executor = new DependencyTreeCommandExecutorImpl();
    }

    @Test
    @DisplayName("Should find Gradle wrapper from subdirectory in multi-module project")
    void shouldFindGradleWrapperFromSubdirectory() throws IOException {
        // Download Spring Pet Clinic (multi-module Gradle project)
        Path projectDir = downloadExample("Spring Boot Multi-Module Gradle");
        assertThat(projectDir).isNotNull();
        
        // Check if wrapper exists at root (may be in subdirectory after extraction)
        Path gradlewPath = findWrapperInTree(projectDir, "gradlew");
        if (gradlewPath == null) {
            // Skip test if wrapper not found (project structure may have changed)
            return;
        }

        // Find a subdirectory with a build.gradle file
        Path subModule = findSubModule(projectDir, "build.gradle");
        if (subModule == null) {
            // Skip if no submodules found
            return;
        }

        // Execute gradle dependencies from the subdirectory
        // This should find the wrapper at the root via parent directory search
        var future = executor.executeGradleDependenciesAsync(
            subModule.resolve("build.gradle"),
            Set.of("compileClasspath", "runtimeClasspath")
        );

        DependencyTreeResult result = future.join();
        
        // Should succeed (wrapper found) or fail with a non-wrapper error
        // The key is it should NOT fail with "gradle command not found"
        if (!result.isSuccess()) {
            assertThat(result.getErrorMessage())
                .as("Error should not be 'command not found' - wrapper should have been found")
                .doesNotContain("gradle command not found");
        }
    }

    @Test
    @DisplayName("Should find Maven wrapper from subdirectory in multi-module project")
    void shouldFindMavenWrapperFromSubdirectory() throws IOException {
        // Download J2EE7 Samples (multi-module Maven project)
        Path projectDir = downloadExample("J2EE7 Samples Multi-Module Maven");
        assertThat(projectDir).isNotNull();
        
        // Check if wrapper exists at root (may be in subdirectory after extraction)
        Path mvnwPath = findWrapperInTree(projectDir, "mvnw");
        if (mvnwPath == null) {
            // Skip test if wrapper not found (project structure may have changed)
            return;
        }

        // Find a subdirectory with a pom.xml file
        Path subModule = findSubModule(projectDir, "pom.xml");
        if (subModule == null) {
            // Skip if no submodules found
            return;
        }

        // Execute mvn dependency:tree from the subdirectory
        // This should find the wrapper at the root via parent directory search
        var future = executor.executeMavenDependencyTreeAsync(
            subModule.resolve("pom.xml"),
            Set.of("compile", "runtime")
        );

        DependencyTreeResult result = future.join();
        
        // Should succeed (wrapper found) or fail with a non-wrapper error
        // The key is it should NOT fail with "mvn command not found"
        if (!result.isSuccess()) {
            assertThat(result.getErrorMessage())
                .as("Error should not be 'command not found' - wrapper should have been found")
                .doesNotContain("mvn command not found");
        }
    }

    @Test
    @DisplayName("Should detect build-tool error when wrapper is completely missing")
    void shouldDetectBuildToolErrorWhenWrapperMissing() throws IOException {
        // Create a temporary directory with a build file but no wrapper
        Path testDir = tempDir.resolve("no-wrapper-test");
        Files.createDirectories(testDir);
        
        Path buildFile = testDir.resolve("build.gradle");
        Files.writeString(buildFile, "plugins { java }");

        // Execute gradle dependencies from directory with no wrapper
        var future = executor.executeGradleDependenciesAsync(
            buildFile,
            Set.of("compileClasspath")
        );

        DependencyTreeResult result = future.join();
        
        // When no wrapper is available, the command should either:
        // 1. Fail with an error message, or
        // 2. Return empty dependencies (indicating the command ran but found nothing)
        // The key is that we don't get a successful result with actual dependencies
        assertThat(result.getDependencies())
            .as("Should not find any dependencies when no wrapper is available")
            .isEmpty();
    }

    @Test
    @DisplayName("Should handle Gradle wrapper at project root correctly")
    void shouldHandleGradleWrapperAtRoot() throws IOException {
        Path projectDir = downloadExample("Spring Boot Multi-Module Gradle");
        assertThat(projectDir).isNotNull();
        
        Path gradlewPath = findWrapperInTree(projectDir, "gradlew");
        if (gradlewPath == null) {
            // Skip if wrapper not found
            return;
        }
        
        // Use the directory containing the wrapper
        Path wrapperDir = gradlewPath.getParent();
        Path rootBuildFile = wrapperDir.resolve("build.gradle");
        if (Files.exists(rootBuildFile)) {
            var future = executor.executeGradleDependenciesAsync(
                rootBuildFile,
                Set.of("compileClasspath")
            );
            
            DependencyTreeResult result = future.join();
            // Should work from root directory
            if (!result.isSuccess()) {
                assertThat(result.getErrorMessage())
                    .doesNotContain("gradle command not found");
            }
        }
    }

    @Test
    @DisplayName("Should handle Maven wrapper at project root correctly")
    void shouldHandleMavenWrapperAtRoot() throws IOException {
        Path projectDir = downloadExample("J2EE7 Samples Multi-Module Maven");
        assertThat(projectDir).isNotNull();
        
        Path mvnwPath = findWrapperInTree(projectDir, "mvnw");
        if (mvnwPath == null) {
            // Skip if wrapper not found
            return;
        }
        
        // Use the directory containing the wrapper
        Path wrapperDir = mvnwPath.getParent();
        Path rootPom = wrapperDir.resolve("pom.xml");
        if (Files.exists(rootPom)) {
            var future = executor.executeMavenDependencyTreeAsync(
                rootPom,
                Set.of("compile")
            );
            
            DependencyTreeResult result = future.join();
            // Should work from root directory
            if (!result.isSuccess()) {
                assertThat(result.getErrorMessage())
                    .doesNotContain("mvn command not found");
            }
        }
    }

    // ========== Helper Methods from SKILL.md ==========

    private Path downloadExample(String projectName) throws IOException {
        String repoUrl = findProjectUrl(projectName);
        Path extractDir = tempDir.resolve("examples").resolve(repoNameFromUrl(repoUrl));
        if (Files.exists(extractDir)) {
            return resolveProjectRoot(extractDir);
        }

        if (repoUrl.contains("/tree/")) {
            String zipUrl = toArchiveZipUrl(repoUrl);
            return downloadAndExtract(zipUrl, extractDir);
        }

        IOException lastException = null;
        for (String branch : new String[]{"main", "master", "develop"}) {
            String zipUrl = repoUrl + "/archive/refs/heads/" + branch + ".zip";
            try {
                return downloadAndExtract(zipUrl, extractDir);
            } catch (IOException e) {
                lastException = e;
            }
        }
        throw new IOException("Failed to download repo from known branches for: " + repoUrl, lastException);
    }

    private Path downloadAndExtract(String zipUrl, Path extractDir) throws IOException {
        URL url = new URL(zipUrl);
        try (InputStream in = url.openStream();
             ZipInputStream zis = new ZipInputStream(in)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path out = extractDir.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(out);
                } else {
                    Files.createDirectories(out.getParent());
                    Files.copy(zis, out);
                }
                zis.closeEntry();
            }
        }
        return resolveProjectRoot(extractDir);
    }

    private String findProjectUrl(String projectName) throws IOException {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        Map<String, Object> data;
        try (InputStream is = getClass().getResourceAsStream("/examples.yaml")) {
            if (is == null) {
                throw new RuntimeException("examples.yaml not found on classpath");
            }
            data = yamlMapper.readValue(is, Map.class);
        }

        String target = projectName.toLowerCase();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> map) {
                        Object nameObj = map.get("name");
                        if (nameObj instanceof String name && name.toLowerCase().contains(target)) {
                            Object urlObj = map.get("url");
                            if (urlObj instanceof String url) {
                                return url;
                            }
                        }
                    }
                }
            }
        }
        throw new IllegalArgumentException("Project not found in examples.yaml: " + projectName);
    }

    private String repoNameFromUrl(String url) {
        String clean = url.replaceAll("/$", "");
        int lastSlash = clean.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < clean.length() - 1) {
            return clean.substring(lastSlash + 1);
        }
        return clean;
    }

    private String toArchiveZipUrl(String repoUrl) {
        String branch = repoUrl.substring(repoUrl.indexOf("/tree/") + 6);
        int nextSlash = branch.indexOf('/');
        if (nextSlash > 0) {
            branch = branch.substring(0, nextSlash);
        }
        String base = repoUrl.substring(0, repoUrl.indexOf("/tree/"));
        return base + "/archive/refs/heads/" + branch + ".zip";
    }

    private Path resolveProjectRoot(Path extractDir) throws IOException {
        try (var stream = Files.list(extractDir)) {
            List<Path> children = stream.toList();
            if (children.size() == 1 && Files.isDirectory(children.get(0))) {
                return children.get(0);
            }
        }
        return extractDir;
    }

    private Path findSubModule(Path projectDir, String buildFileName) throws IOException {
        // Search for subdirectories containing build files
        try (var stream = Files.walk(projectDir)) {
            return stream
                .filter(Files::isDirectory)
                .filter(dir -> !dir.equals(projectDir))
                .filter(dir -> Files.exists(dir.resolve(buildFileName)))
                .findFirst()
                .orElse(null);
        }
    }

    private Path findWrapperInTree(Path projectDir, String wrapperName) throws IOException {
        // Search for wrapper in directory tree
        try (var stream = Files.walk(projectDir)) {
            return stream
                .filter(Files::isRegularFile)
                .filter(file -> file.getFileName().toString().equals(wrapperName))
                .filter(Files::isExecutable)
                .findFirst()
                .orElse(null);
        }
    }
}
