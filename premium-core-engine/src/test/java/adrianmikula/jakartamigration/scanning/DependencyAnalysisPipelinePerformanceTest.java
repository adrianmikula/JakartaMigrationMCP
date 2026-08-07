package adrianmikula.jakartamigration.scanning;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Namespace;
import adrianmikula.jakartamigration.dependencyanalysis.service.NamespaceClassifier;
import adrianmikula.jakartamigration.dependencyanalysis.service.impl.SimpleNamespaceClassifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Dependency Analysis Pipeline Performance Tests")
@Tag("slow")
class DependencyAnalysisPipelinePerformanceTest {

    private NamespaceClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new SimpleNamespaceClassifier();
    }

    @Test
    @DisplayName("Should analyze small project (50 deps) within 10s budget")
    void shouldAnalyzeSmallProjectWithinBudget() {
        List<Artifact> artifacts = generateProjectDeps(50);

        long start = System.currentTimeMillis();
        Map<Artifact, Namespace> results = classifier.classifyAll(artifacts);
        long duration = System.currentTimeMillis() - start;

        assertThat(results).hasSize(50);
        assertThat(duration).isLessThan(10000); // 10s budget
    }

    @Test
    @DisplayName("Should analyze medium project (200 deps) within 60s budget")
    void shouldAnalyzeMediumProjectWithinBudget() {
        List<Artifact> artifacts = generateProjectDeps(200);

        long start = System.currentTimeMillis();
        Map<Artifact, Namespace> results = classifier.classifyAll(artifacts);
        long duration = System.currentTimeMillis() - start;

        assertThat(results).hasSize(200);
        assertThat(duration).isLessThan(60000); // 60s budget
    }

    @Test
    @DisplayName("Should analyze large project (500 deps) within 300s budget")
    void shouldAnalyzeLargeProjectWithinBudget() {
        List<Artifact> artifacts = generateProjectDeps(500);

        long start = System.currentTimeMillis();
        Map<Artifact, Namespace> results = classifier.classifyAll(artifacts);
        long duration = System.currentTimeMillis() - start;

        assertThat(results).hasSize(500);
        assertThat(duration).isLessThan(300000); // 300s budget
    }

    private List<Artifact> generateProjectDeps(int count) {
        List<Artifact> artifacts = new ArrayList<>(count);
        String[] groupIds = {
            "javax.servlet", "javax.persistence", "javax.validation",
            "javax.ws.rs", "javax.ejb", "javax.annotation",
            "jakarta.servlet", "jakarta.persistence", "jakarta.validation",
            "org.springframework.boot", "org.hibernate.orm",
            "com.google.guava", "com.fasterxml.jackson.core",
            "org.slf4j", "org.apache.commons"
        };
        String[] artifactIds = {
            "javax.servlet-api", "javax.persistence-api", "validation-api",
            "javax.ws.rs-api", "javax.ejb-api", "javax.annotation-api",
            "jakarta.servlet-api", "jakarta.persistence-api", "jakarta.validation-api",
            "spring-boot", "hibernate-core",
            "guava", "jackson-databind",
            "slf4j-api", "commons-lang3"
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
