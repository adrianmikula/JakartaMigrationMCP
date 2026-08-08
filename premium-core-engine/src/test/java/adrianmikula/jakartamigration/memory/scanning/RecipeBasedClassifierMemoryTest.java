package adrianmikula.jakartamigration.memory.scanning;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Namespace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RecipeBasedClassifier Memory Tests")
@Tag("slow")
class RecipeBasedClassifierMemoryTest {

    private RecipeBasedClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new RecipeBasedClassifier();
    }

    @Test
    @DisplayName("Should classify 1000 dependencies within 50MB heap budget")
    void shouldClassify1000DependenciesWithinBudget() {
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();

        long heapBefore = runtime.totalMemory() - runtime.freeMemory();

        List<Artifact> artifacts = generateArtifacts(1000);
        for (Artifact artifact : artifacts) {
            Namespace result = classifier.classify(artifact);
            assertThat(result).isNotNull();
        }

        runtime.gc();
        long heapAfter = runtime.totalMemory() - runtime.freeMemory();
        long heapUsed = heapAfter - heapBefore;

        // Budget: 50MB for 1000 dependencies
        assertThat(heapUsed).isLessThan(50 * 1024 * 1024);
    }

    @Test
    @DisplayName("Should not leak memory on repeated classification")
    void shouldNotLeakMemoryOnRepeatedClassification() {
        Runtime runtime = Runtime.getRuntime();

        Artifact testArtifact = new Artifact(
            "javax.servlet", "javax.servlet-api", "4.0.1", "compile", false);

        // Warm up
        for (int i = 0; i < 100; i++) {
            classifier.classify(testArtifact);
        }

        runtime.gc();
        long baseline = runtime.totalMemory() - runtime.freeMemory();

        // Run 1000 iterations
        for (int i = 0; i < 1000; i++) {
            classifier.classify(testArtifact);
        }

        runtime.gc();
        long after = runtime.totalMemory() - runtime.freeMemory();
        long delta = after - baseline;

        // Should not leak more than 5MB over baseline
        assertThat(delta).isLessThan(5 * 1024 * 1024);
    }

    @Test
    @DisplayName("Should handle large coordinate map within 10MB heap")
    void shouldHandleLargeCoordinateMap() {
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();

        long heapBefore = runtime.totalMemory() - runtime.freeMemory();

        // Create a classifier with many entries in its coordinate map
        RecipeBasedClassifier largeClassifier = new RecipeBasedClassifier();
        // The classifier already loads ~40 mappings from recipes
        // Classify many artifacts to exercise the maps
        for (int i = 0; i < 10000; i++) {
            Artifact artifact = new Artifact(
                "com.example.group" + (i % 100),
                "artifact" + (i % 50),
                "1.0." + (i % 10),
                "compile", false);
            largeClassifier.classify(artifact);
        }

        runtime.gc();
        long heapAfter = runtime.totalMemory() - runtime.freeMemory();
        long heapUsed = heapAfter - heapBefore;

        // Budget: 10MB for coordinate map operations
        assertThat(heapUsed).isLessThan(10 * 1024 * 1024);
    }

    private List<Artifact> generateArtifacts(int count) {
        List<Artifact> artifacts = new ArrayList<>(count);
        String[] groupIds = {
            "javax.servlet", "javax.persistence", "javax.validation",
            "jakarta.servlet", "jakarta.persistence", "jakarta.validation",
            "org.hibernate.orm", "org.glassfish.jersey.core",
            "com.example", "org.springframework.boot"
        };
        String[] artifactIds = {
            "javax.servlet-api", "javax.persistence-api", "validation-api",
            "jakarta.servlet-api", "jakarta.persistence-api", "jakarta.validation-api",
            "hibernate-core", "jersey-server",
            "my-library", "spring-boot"
        };

        for (int i = 0; i < count; i++) {
            int idx = i % groupIds.length;
            artifacts.add(new Artifact(
                groupIds[idx], artifactIds[idx],
                "1.0." + (i % 10), "compile", false));
        }
        return artifacts;
    }
}
