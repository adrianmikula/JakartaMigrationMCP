package adrianmikula.jakartamigration.realrepo.advancedscanning;

import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyProjectScanResult;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeCategory;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeType;
import adrianmikula.jakartamigration.coderefactoring.service.RecipeService;
import adrianmikula.jakartamigration.coderefactoring.service.impl.RecipeServiceImpl;
import adrianmikula.jakartamigration.analysis.persistence.CentralMigrationAnalysisStore;
import adrianmikula.jakartamigration.analysis.persistence.SqliteMigrationAnalysisStore;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for AdvancedScanningModule using real GitHub repositories.
 * Downloads actual projects from examples.yaml to verify advanced scanning works against real-world code.
 */
@Tag("slow")
class AdvancedScanningModuleRealRepositoryTest {

    @TempDir
    Path tempDir;

    private AdvancedScanningModule scanningModule;
    private RecipeService recipeService;

    @BeforeEach
    void setUp() throws IOException {
        Path projectPath = tempDir.resolve("integration-project");
        Files.createDirectories(projectPath);

        CentralMigrationAnalysisStore centralStore = new CentralMigrationAnalysisStore(tempDir.resolve("central.db"));
        SqliteMigrationAnalysisStore projectStore = new SqliteMigrationAnalysisStore(projectPath);
        recipeService = new RecipeServiceImpl(centralStore, projectStore);

        scanningModule = new AdvancedScanningModule(recipeService);
    }

    @Test
    @DisplayName("Should scan transitive dependencies in J2EE7Samples")
    void shouldScanTransitiveDependenciesInJ2EESamples() throws IOException {
        Path sampleDir = downloadExample("J2EE7 Samples");

        TransitiveDependencyScanner scanner = scanningModule.getTransitiveDependencyScanner();
        TransitiveDependencyProjectScanResult result = scanner.scanProject(sampleDir);

        assertThat(result).isNotNull();
        assertThat(result.getFileResults()).isNotEmpty();

        long totalDependencies = result.getFileResults().stream()
            .flatMap(r -> r.getUsages().stream())
            .count();

        assertThat(totalDependencies)
            .as("J2EE7Samples should have dependencies")
            .isGreaterThan(0);
    }

    @Test
    @DisplayName("Should detect javax dependencies in J2EE7Samples")
    void shouldDetectJavaxDependenciesInJ2EESamples() throws IOException {
        Path sampleDir = downloadExample("J2EE7 Samples");

        TransitiveDependencyScanner scanner = scanningModule.getTransitiveDependencyScanner();
        TransitiveDependencyProjectScanResult result = scanner.scanProject(sampleDir);

        assertThat(result).isNotNull();
        assertThat(result.getFileResults()).isNotEmpty();

        long javaxDependencyCount = result.getFileResults().stream()
            .flatMap(r -> r.getUsages().stream())
            .filter(u -> u.getGroupId() != null && u.getGroupId().startsWith("javax."))
            .count();

        assertThat(javaxDependencyCount)
            .as("J2EE7Samples should have javax dependencies")
            .isGreaterThan(0);
    }

    @Test
    @DisplayName("Should scan transitive dependencies in Spring Boot project")
    void shouldScanTransitiveDependenciesInSpringBootProject() throws IOException {
        Path sampleDir = downloadExample("Spring Boot");

        TransitiveDependencyScanner scanner = scanningModule.getTransitiveDependencyScanner();
        TransitiveDependencyProjectScanResult result = scanner.scanProject(sampleDir);

        assertThat(result).isNotNull();
        assertThat(result.getFileResults()).isNotEmpty();

        long totalDependencies = result.getFileResults().stream()
            .flatMap(r -> r.getUsages().stream())
            .count();

        assertThat(totalDependencies)
            .as("Spring Boot project should have dependencies")
            .isGreaterThan(0);
    }

    @Test
    @DisplayName("Should classify dependencies with severity levels")
    void shouldClassifyDependenciesWithSeverityLevels() throws IOException {
        Path sampleDir = downloadExample("J2EE7 Samples");

        TransitiveDependencyScanner scanner = scanningModule.getTransitiveDependencyScanner();
        TransitiveDependencyProjectScanResult result = scanner.scanProject(sampleDir);

        assertThat(result).isNotNull();
        assertThat(result.getFileResults()).isNotEmpty();

        boolean hasHighSeverity = result.getFileResults().stream()
            .flatMap(r -> r.getUsages().stream())
            .anyMatch(u -> "high".equals(u.getSeverity()));

        assertThat(hasHighSeverity)
            .as("Should have at least one high severity dependency (javax requiring Jakarta migration)")
            .isTrue();
    }

    @Test
    @DisplayName("Should provide recommendations for javax dependencies")
    void shouldProvideRecommendationsForJavaxDependencies() throws IOException {
        Path sampleDir = downloadExample("J2EE7 Samples");

        TransitiveDependencyScanner scanner = scanningModule.getTransitiveDependencyScanner();
        TransitiveDependencyProjectScanResult result = scanner.scanProject(sampleDir);

        assertThat(result).isNotNull();
        assertThat(result.getFileResults()).isNotEmpty();

        boolean hasRecommendation = result.getFileResults().stream()
            .flatMap(r -> r.getUsages().stream())
            .filter(u -> u.getGroupId() != null && u.getGroupId().startsWith("javax."))
            .anyMatch(u -> u.getRecommendation() != null && !u.getRecommendation().isEmpty());

        assertThat(hasRecommendation)
            .as("javax dependencies should have migration recommendations")
            .isTrue();
    }

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
}
