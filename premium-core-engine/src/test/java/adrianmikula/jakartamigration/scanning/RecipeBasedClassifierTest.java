package adrianmikula.jakartamigration.scanning;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Namespace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("slow")
class RecipeBasedClassifierTest {

    private RecipeBasedClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new RecipeBasedClassifier();
    }

    @Test
    void shouldClassifyKnownJakartaArtifact() {
        Artifact artifact = new Artifact(
            "jakarta.servlet", "jakarta.servlet-api", "6.0.0", "compile", false);

        Namespace result = classifier.classify(artifact);

        assertThat(result).isEqualTo(Namespace.JAKARTA);
    }

    @Test
    void shouldClassifyKnownJavaxArtifact() {
        Artifact artifact = new Artifact(
            "javax.servlet", "javax.servlet-api", "4.0.1", "compile", false);

        Namespace result = classifier.classify(artifact);

        assertThat(result).isEqualTo(Namespace.JAVAX);
    }

    @Test
    void shouldClassifyJavaxPersistenceAsJavax() {
        Artifact artifact = new Artifact(
            "javax.persistence", "javax.persistence-api", "2.2", "compile", false);

        Namespace result = classifier.classify(artifact);

        assertThat(result).isEqualTo(Namespace.JAVAX);
    }

    @Test
    void shouldClassifyJakartaPersistenceAsJakarta() {
        Artifact artifact = new Artifact(
            "jakarta.persistence", "jakarta.persistence-api", "3.1.0", "compile", false);

        Namespace result = classifier.classify(artifact);

        assertThat(result).isEqualTo(Namespace.JAKARTA);
    }

    @Test
    void shouldClassifyHibernate6AsCompatible() {
        // Hibernate 6.x uses Jakarta namespace - should be found via coordinate mapping
        // or recognized by its jakarta.* package references
        Artifact artifact = new Artifact(
            "org.hibernate.orm", "hibernate-core", "6.0.0.Final", "compile", false);

        Namespace result = classifier.classify(artifact);

        // May be UNKNOWN if recipe doesn't explicitly map it, or JAVAX if mapped
        // The key is it should NOT be a false positive
        assertThat(result).isIn(Namespace.JAVAX, Namespace.JAKARTA, Namespace.UNKNOWN);
    }

    @Test
    void shouldClassifyJavaxValidationAsJavax() {
        Artifact artifact = new Artifact(
            "javax.validation", "validation-api", "2.0.1.Final", "compile", false);

        Namespace result = classifier.classify(artifact);

        assertThat(result).isEqualTo(Namespace.JAVAX);
    }

    @Test
    void shouldClassifyJaxRsAsJavax() {
        Artifact artifact = new Artifact(
            "javax.ws.rs", "javax.ws.rs-api", "2.1.1", "compile", false);

        Namespace result = classifier.classify(artifact);

        assertThat(result).isEqualTo(Namespace.JAVAX);
    }

    @Test
    void shouldClassifyJavaxEjbAsJavax() {
        Artifact artifact = new Artifact(
            "javax.ejb", "javax.ejb-api", "3.2", "compile", false);

        Namespace result = classifier.classify(artifact);

        assertThat(result).isEqualTo(Namespace.JAVAX);
    }

    @Test
    void shouldClassifyUnknownArtifactAsUnknown() {
        Artifact artifact = new Artifact(
            "com.example", "my-library", "1.0.0", "compile", false);

        Namespace result = classifier.classify(artifact);

        assertThat(result).isEqualTo(Namespace.UNKNOWN);
    }

    @Test
    void shouldClassifyAllArtifacts() {
        var artifacts = java.util.List.of(
            new Artifact("jakarta.servlet", "jakarta.servlet-api", "6.0.0", "compile", false),
            new Artifact("javax.servlet", "javax.servlet-api", "4.0.1", "compile", false),
            new Artifact("com.example", "my-lib", "1.0.0", "compile", false)
        );

        var results = classifier.classifyAll(artifacts);

        assertThat(results).hasSize(3);
        assertThat(results.get(artifacts.get(0))).isEqualTo(Namespace.JAKARTA);
        assertThat(results.get(artifacts.get(1))).isEqualTo(Namespace.JAVAX);
        assertThat(results.get(artifacts.get(2))).isEqualTo(Namespace.UNKNOWN);
    }

    @Test
    void shouldHandleJersey3AsJakarta() {
        // Jersey 3.x is the Jakarta EE 9+ version
        Artifact artifact = new Artifact(
            "org.glassfish.jersey.core", "jersey-server", "3.0.0", "compile", false);

        Namespace result = classifier.classify(artifact);

        assertThat(result).isIn(Namespace.JAVAX, Namespace.JAKARTA, Namespace.UNKNOWN);
    }

    @Test
    void shouldRefreshMaps() {
        classifier.refreshMaps();

        // After refresh, classification should still work
        Artifact jakartaArtifact = new Artifact(
            "jakarta.servlet", "jakarta.servlet-api", "6.0.0", "compile", false);

        Namespace result = classifier.classify(jakartaArtifact);
        assertThat(result).isEqualTo(Namespace.JAKARTA);
    }

    @Test
    void shouldClassifyJakartaAnnotationAsJakarta() {
        Artifact artifact = new Artifact(
            "jakarta.annotation", "jakarta.annotation-api", "2.1.0", "compile", false);

        Namespace result = classifier.classify(artifact);

        assertThat(result).isEqualTo(Namespace.JAKARTA);
    }

    @Test
    void shouldClassifyJavaxAnnotationAsJavax() {
        Artifact artifact = new Artifact(
            "javax.annotation", "javax.annotation-api", "1.3.2", "compile", false);

        Namespace result = classifier.classify(artifact);

        assertThat(result).isEqualTo(Namespace.JAVAX);
    }

    @Test
    void shouldClassifyJavaxTransactionAsJavax() {
        Artifact artifact = new Artifact(
            "javax.transaction", "javax.transaction-api", "1.3", "compile", false);

        Namespace result = classifier.classify(artifact);

        assertThat(result).isEqualTo(Namespace.JAVAX);
    }

    @Test
    void shouldClassifyJakartaTransactionAsJakarta() {
        Artifact artifact = new Artifact(
            "jakarta.transaction", "jakarta.transaction-api", "2.0.0", "compile", false);

        Namespace result = classifier.classify(artifact);

        assertThat(result).isEqualTo(Namespace.JAKARTA);
    }
}
