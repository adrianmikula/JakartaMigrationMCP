package adrianmikula.jakartamigration.scanning;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Namespace;
import adrianmikula.jakartamigration.jaranalysis.service.DefaultJarCompatibilityScanner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import adrianmikula.jakartamigration.testutil.TestJarBuilder;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RecipeBasedClassifier Integration Tests")
@Tag("slow")
class RecipeBasedClassifierIntegrationTest {

    private RecipeBasedClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new RecipeBasedClassifier();
    }

    @AfterEach
    void tearDown() {
        classifier.shutdown();
    }

    @Test
    @DisplayName("Should classify known Jakarta artifact as JAKARTA")
    void shouldClassifyKnownJakartaArtifact() {
        Artifact artifact = new Artifact(
            "jakarta.servlet", "jakarta.servlet-api", "6.0.0", "compile", false);

        Namespace result = classifier.classify(artifact);

        assertThat(result).isEqualTo(Namespace.JAKARTA);
    }

    @Test
    @DisplayName("Should classify known javax artifact as JAVAX")
    void shouldClassifyKnownJavaxArtifact() {
        Artifact artifact = new Artifact(
            "javax.servlet", "javax.servlet-api", "4.0.1", "compile", false);

        Namespace result = classifier.classify(artifact);

        assertThat(result).isEqualTo(Namespace.JAVAX);
    }

    @Test
    @DisplayName("Should classify jakarta.persistence as JAKARTA")
    void shouldClassifyJakartaPersistence() {
        Artifact artifact = new Artifact(
            "jakarta.persistence", "jakarta.persistence-api", "3.1.0", "compile", false);

        Namespace result = classifier.classify(artifact);

        assertThat(result).isEqualTo(Namespace.JAKARTA);
    }

    @Test
    @DisplayName("Should classify javax.persistence as JAVAX")
    void shouldClassifyJavaxPersistence() {
        Artifact artifact = new Artifact(
            "javax.persistence", "javax.persistence-api", "2.2", "compile", false);

        Namespace result = classifier.classify(artifact);

        assertThat(result).isEqualTo(Namespace.JAVAX);
    }

    @Test
    @DisplayName("Should classify jakarta.validation as JAKARTA")
    void shouldClassifyJakartaValidation() {
        Artifact artifact = new Artifact(
            "jakarta.validation", "jakarta.validation-api", "3.0.0", "compile", false);

        Namespace result = classifier.classify(artifact);

        assertThat(result).isEqualTo(Namespace.JAKARTA);
    }

    @Test
    @DisplayName("Should classify javax.ws.rs as JAVAX via groupId prefix")
    void shouldClassifyJavaxWsRs() {
        Artifact artifact = new Artifact(
            "javax.ws.rs", "javax.ws.rs-api", "2.1.1", "compile", false);

        Namespace result = classifier.classify(artifact);

        assertThat(result).isEqualTo(Namespace.JAVAX);
    }

    @Test
    @DisplayName("Should classify jakarta.ws.rs as JAKARTA via groupId prefix")
    void shouldClassifyJakartaWsRs() {
        Artifact artifact = new Artifact(
            "jakarta.ws.rs", "jakarta.ws.rs-api", "3.1.0", "compile", false);

        Namespace result = classifier.classify(artifact);

        assertThat(result).isEqualTo(Namespace.JAKARTA);
    }

    @Test
    @DisplayName("Should classify Hibernate 6 as JAKARTA via coordinate map")
    void shouldClassifyHibernate6AsCompatible() {
        Artifact artifact = new Artifact(
            "org.hibernate.orm", "hibernate-core", "6.0.0.Final", "compile", false);

        Namespace result = classifier.classify(artifact);

        assertThat(result).isEqualTo(Namespace.JAKARTA);
    }

    @Test
    @DisplayName("Should classify Hibernate 5 as JAVAX via coordinate map")
    void shouldClassifyHibernate5AsJavax() {
        Artifact artifact = new Artifact(
            "org.hibernate", "hibernate-core", "5.6.1.Final", "compile", false);

        Namespace result = classifier.classify(artifact);

        assertThat(result).isEqualTo(Namespace.JAVAX);
    }

    @Test
    @DisplayName("Should classify JDK javax packages as UNKNOWN (no migration needed)")
    void shouldClassifyJdkJavaxAsUnknown() {
        Artifact artifact = new Artifact(
            "javax.management", "jmx-remote-api", "1.0", "compile", false);

        Namespace result = classifier.classify(artifact);

        assertThat(result).isEqualTo(Namespace.UNKNOWN);
    }

    @Test
    @DisplayName("Should classify javax.xml.bind as JAVAX (not JDK-provided)")
    void shouldClassifyJavaxXmlBindAsJavax() {
        Artifact artifact = new Artifact(
            "javax.xml.bind", "jaxb-api", "2.3.1", "compile", false);

        Namespace result = classifier.classify(artifact);

        assertThat(result).isEqualTo(Namespace.JAVAX);
    }

    @Test
    @DisplayName("Should classify javax.inject as JAVAX")
    void shouldClassifyJavaxInjectAsJavax() {
        Artifact artifact = new Artifact(
            "javax.inject", "javax.inject", "1", "compile", false);

        Namespace result = classifier.classify(artifact);

        assertThat(result).isEqualTo(Namespace.JAVAX);
    }

    @Test
    @DisplayName("Should classify jakarta.inject as JAKARTA")
    void shouldClassifyJakartaInjectAsJakarta() {
        Artifact artifact = new Artifact(
            "jakarta.inject", "jakarta.inject-api", "2.0.1", "compile", false);

        Namespace result = classifier.classify(artifact);

        assertThat(result).isEqualTo(Namespace.JAKARTA);
    }

    @Test
    @DisplayName("Should classify unknown artifact as UNKNOWN")
    void shouldClassifyUnknownArtifact() {
        Artifact artifact = new Artifact(
            "com.google.guava", "guava", "31.1-jre", "compile", false);

        Namespace result = classifier.classify(artifact);

        assertThat(result).isEqualTo(Namespace.UNKNOWN);
    }

    @Test
    @DisplayName("Should classify batch of artifacts correctly")
    void shouldClassifyBatchOfArtifacts() {
        List<Artifact> artifacts = List.of(
            new Artifact("jakarta.servlet", "jakarta.servlet-api", "6.0.0", "compile", false),
            new Artifact("javax.servlet", "javax.servlet-api", "4.0.1", "compile", false),
            new Artifact("com.google.guava", "guava", "31.1-jre", "compile", false)
        );

        Map<Artifact, Namespace> results = classifier.classifyAll(artifacts);

        assertThat(results).hasSize(3);
        assertThat(results.get(artifacts.get(0))).isEqualTo(Namespace.JAKARTA);
        assertThat(results.get(artifacts.get(1))).isEqualTo(Namespace.JAVAX);
        assertThat(results.get(artifacts.get(2))).isEqualTo(Namespace.UNKNOWN);
    }

    @Test
    @DisplayName("Should classify javax.annotation as JAVAX")
    void shouldClassifyJavaxAnnotationAsJavax() {
        Artifact artifact = new Artifact(
            "javax.annotation", "javax.annotation-api", "1.3.2", "compile", false);

        Namespace result = classifier.classify(artifact);

        assertThat(result).isEqualTo(Namespace.JAVAX);
    }

    @Test
    @DisplayName("Should classify jakarta.annotation as JAKARTA")
    void shouldClassifyJakartaAnnotationAsJakarta() {
        Artifact artifact = new Artifact(
            "jakarta.annotation", "jakarta.annotation-api", "2.1.0", "compile", false);

        Namespace result = classifier.classify(artifact);

        assertThat(result).isEqualTo(Namespace.JAKARTA);
    }

    @Test
    @DisplayName("Should classify javax.transaction as JAVAX")
    void shouldClassifyJavaxTransactionAsJavax() {
        Artifact artifact = new Artifact(
            "javax.transaction", "javax.transaction-api", "1.3", "compile", false);

        Namespace result = classifier.classify(artifact);

        assertThat(result).isEqualTo(Namespace.JAVAX);
    }

    @Test
    @DisplayName("Should classify jakarta.transaction as JAKARTA")
    void shouldClassifyJakartaTransactionAsJakarta() {
        Artifact artifact = new Artifact(
            "jakarta.transaction", "jakarta.transaction-api", "2.0.0", "compile", false);

        Namespace result = classifier.classify(artifact);

        assertThat(result).isEqualTo(Namespace.JAKARTA);
    }

    @Test
    @DisplayName("Should classify with JAR scan fallback when scanner is available")
    void shouldClassifyWithJarScanFallback(@TempDir Path tempDir) throws IOException {
        Path jakartaJar = tempDir.resolve("jakarta.servlet-api-6.0.0.jar");
        TestJarBuilder.createJakartaServletJar().build(jakartaJar);

        DefaultJarCompatibilityScanner jarScanner = new DefaultJarCompatibilityScanner();
        RecipeBasedClassifier classifierWithJar = new RecipeBasedClassifier(
            new RecipePatternExtractor(), jarScanner);

        try {
            Artifact artifact = new Artifact(
                "com.example", "unknown-lib", "1.0", "compile", false);

            Namespace result = classifierWithJar.classify(artifact);

            assertThat(result).isNotNull();
        } finally {
            classifierWithJar.shutdown();
        }
    }
}
