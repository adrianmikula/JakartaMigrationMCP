package adrianmikula.jakartamigration.dependencyanalysis.service.impl;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Namespace;
import adrianmikula.jakartamigration.dependencyanalysis.service.NamespaceClassifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SimpleNamespaceClassifier Expansion Tests")
class SimpleNamespaceClassifierExpansionTest {

    private NamespaceClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new SimpleNamespaceClassifier();
    }

    @Test
    @DisplayName("Should classify Spring Boot 3.x project dependencies as JAKARTA")
    void shouldClassifySpringBoot3Project() {
        Artifact springBoot = new Artifact(
            "org.springframework.boot", "spring-boot", "3.2.0", "compile", false);
        Artifact springBootStarter = new Artifact(
            "org.springframework.boot", "spring-boot-starter-web", "3.1.0", "compile", false);

        assertThat(classifier.classify(springBoot)).isEqualTo(Namespace.JAKARTA);
        assertThat(classifier.classify(springBootStarter)).isEqualTo(Namespace.JAKARTA);
    }

    @Test
    @DisplayName("Should classify Spring Boot 2.x project dependencies as JAVAX")
    void shouldClassifySpringBoot2Project() {
        Artifact springBoot = new Artifact(
            "org.springframework.boot", "spring-boot", "2.7.0", "compile", false);
        Artifact springBootStarter = new Artifact(
            "org.springframework.boot", "spring-boot-starter-web", "2.6.0", "compile", false);

        assertThat(classifier.classify(springBoot)).isEqualTo(Namespace.JAVAX);
        assertThat(classifier.classify(springBootStarter)).isEqualTo(Namespace.JAVAX);
    }

    @Test
    @DisplayName("Should classify Hibernate 6.x as JAKARTA")
    void shouldClassifyHibernate6Project() {
        Artifact hibernate6 = new Artifact(
            "org.hibernate.orm", "hibernate-core", "6.0.0.Final", "compile", false);
        Artifact hibernate62 = new Artifact(
            "org.hibernate.orm", "hibernate-core", "6.2.0.Final", "compile", false);

        assertThat(classifier.classify(hibernate6)).isEqualTo(Namespace.JAKARTA);
        assertThat(classifier.classify(hibernate62)).isEqualTo(Namespace.JAKARTA);
    }

    @Test
    @DisplayName("Should classify Hibernate 5.x as JAVAX")
    void shouldClassifyHibernate5Project() {
        Artifact hibernate5 = new Artifact(
            "org.hibernate", "hibernate-core", "5.6.0.Final", "compile", false);
        Artifact hibernate54 = new Artifact(
            "org.hibernate", "hibernate-core", "5.4.0.Final", "compile", false);

        assertThat(classifier.classify(hibernate5)).isEqualTo(Namespace.JAVAX);
        assertThat(classifier.classify(hibernate54)).isEqualTo(Namespace.JAVAX);
    }

    @Test
    @DisplayName("Should classify Jersey 3.x as JAKARTA")
    void shouldClassifyJersey3Project() {
        Artifact jersey3 = new Artifact(
            "org.glassfish.jersey.core", "jersey-server", "3.0.0", "compile", false);
        Artifact jersey31 = new Artifact(
            "org.glassfish.jersey.core", "jersey-server", "3.1.0", "compile", false);
        Artifact jerseyCommon3 = new Artifact(
            "org.glassfish.jersey.core", "jersey-common", "3.1.0", "compile", false);

        assertThat(classifier.classify(jersey3)).isEqualTo(Namespace.JAKARTA);
        assertThat(classifier.classify(jersey31)).isEqualTo(Namespace.JAKARTA);
        assertThat(classifier.classify(jerseyCommon3)).isEqualTo(Namespace.JAKARTA);
    }

    @Test
    @DisplayName("Should classify Jersey 2.x as JAVAX")
    void shouldClassifyJersey2Project() {
        Artifact jersey2 = new Artifact(
            "org.glassfish.jersey.core", "jersey-server", "2.39.1", "compile", false);

        assertThat(classifier.classify(jersey2)).isEqualTo(Namespace.JAVAX);
    }

    @Test
    @DisplayName("Should classify RESTEasy 6.x as JAKARTA")
    void shouldClassifyResteasy6Project() {
        Artifact resteasy6 = new Artifact(
            "org.jboss.resteasy", "resteasy-core", "6.0.0.Final", "compile", false);
        Artifact resteasy62 = new Artifact(
            "org.jboss.resteasy", "resteasy-core", "6.2.0.Final", "compile", false);

        assertThat(classifier.classify(resteasy6)).isEqualTo(Namespace.JAKARTA);
        assertThat(classifier.classify(resteasy62)).isEqualTo(Namespace.JAKARTA);
    }

    @Test
    @DisplayName("Should classify RESTEasy 5.x as JAVAX")
    void shouldClassifyResteasy5Project() {
        Artifact resteasy5 = new Artifact(
            "org.jboss.resteasy", "resteasy-core", "5.0.0.Final", "compile", false);

        assertThat(classifier.classify(resteasy5)).isEqualTo(Namespace.JAVAX);
    }

    @Test
    @DisplayName("Should classify Hibernate Validator 8.x as JAKARTA")
    void shouldClassifyHibernateValidator8Project() {
        Artifact validator8 = new Artifact(
            "org.hibernate.validator", "hibernate-validator", "8.0.0.Final", "compile", false);

        assertThat(classifier.classify(validator8)).isEqualTo(Namespace.JAKARTA);
    }

    @Test
    @DisplayName("Should classify Hibernate Validator 7.x as JAVAX")
    void shouldClassifyHibernateValidator7Project() {
        Artifact validator7 = new Artifact(
            "org.hibernate.validator", "hibernate-validator", "7.0.0.Final", "compile", false);

        assertThat(classifier.classify(validator7)).isEqualTo(Namespace.JAVAX);
    }

    @Test
    @DisplayName("Should classify Jakarta EE 10 platform as JAKARTA")
    void shouldClassifyJakartaEE10Platform() {
        Artifact platform = new Artifact(
            "jakarta.platform", "jakarta.jakartaee-api", "10.0.0", "compile", false);

        assertThat(classifier.classify(platform)).isEqualTo(Namespace.JAKARTA);
    }

    @Test
    @DisplayName("Should classify javax servlet API as JAVAX")
    void shouldClassifyJavaxServletAsJavax() {
        Artifact servlet = new Artifact(
            "javax.servlet", "javax.servlet-api", "4.0.1", "compile", false);

        assertThat(classifier.classify(servlet)).isEqualTo(Namespace.JAVAX);
    }

    @Test
    @DisplayName("Should classify jakarta servlet API as JAKARTA")
    void shouldClassifyJakartaServletAsJakarta() {
        Artifact servlet = new Artifact(
            "jakarta.servlet", "jakarta.servlet-api", "6.0.0", "compile", false);

        assertThat(classifier.classify(servlet)).isEqualTo(Namespace.JAKARTA);
    }

    @Test
    @DisplayName("Should classify JDK javax packages as UNKNOWN (no migration needed)")
    void shouldClassifyJdkProvidedPackagesAsUnknown() {
        Artifact javaxManagement = new Artifact(
            "javax.management", "javax.management-api", "1.1.1", "compile", false);
        Artifact javaxNaming = new Artifact(
            "javax.naming", "javax.naming-api", "1.3.1", "compile", false);
        Artifact javaxCrypto = new Artifact(
            "javax.crypto", "javax.crypto-api", "1.0.0", "compile", false);

        // JDK-provided javax packages should NOT be classified as JAVAX
        // They don't need migration
        assertThat(classifier.classify(javaxManagement)).isEqualTo(Namespace.UNKNOWN);
        assertThat(classifier.classify(javaxNaming)).isEqualTo(Namespace.UNKNOWN);
        assertThat(classifier.classify(javaxCrypto)).isEqualTo(Namespace.UNKNOWN);
    }

    @Test
    @DisplayName("Should classify unknown third-party as UNKNOWN")
    void shouldClassifyUnknownThirdPartyAsUnknown() {
        Artifact guava = new Artifact(
            "com.google.guava", "guava", "32.1.3-jre", "compile", false);
        Artifact jackson = new Artifact(
            "com.fasterxml.jackson.core", "jackson-databind", "2.15.2", "compile", false);

        assertThat(classifier.classify(guava)).isEqualTo(Namespace.UNKNOWN);
        assertThat(classifier.classify(jackson)).isEqualTo(Namespace.UNKNOWN);
    }

    @Test
    @DisplayName("Should classify CXF 4.x as JAKARTA")
    void shouldClassifyCxf4Project() {
        Artifact cxf4 = new Artifact(
            "org.apache.cxf", "cxf-rt-frontend-jaxrs", "4.0.0", "compile", false);

        assertThat(classifier.classify(cxf4)).isEqualTo(Namespace.JAKARTA);
    }

    @Test
    @DisplayName("Should classify CXF 3.x as JAVAX")
    void shouldClassifyCxf3Project() {
        Artifact cxf3 = new Artifact(
            "org.apache.cxf", "cxf-rt-frontend-jaxrs", "3.6.0", "compile", false);

        assertThat(classifier.classify(cxf3)).isEqualTo(Namespace.JAVAX);
    }

    @Test
    @DisplayName("Should classify batch operations correctly")
    void shouldClassifyBatchOperations() {
        var artifacts = java.util.List.of(
            new Artifact("jakarta.servlet", "jakarta.servlet-api", "6.0.0", "compile", false),
            new Artifact("javax.servlet", "javax.servlet-api", "4.0.1", "compile", false),
            new Artifact("org.springframework.boot", "spring-boot", "3.2.0", "compile", false),
            new Artifact("com.google.guava", "guava", "32.1.3-jre", "compile", false)
        );

        Map<Artifact, Namespace> results = classifier.classifyAll(artifacts);

        assertThat(results).hasSize(4);
        assertThat(results.get(artifacts.get(0))).isEqualTo(Namespace.JAKARTA);
        assertThat(results.get(artifacts.get(1))).isEqualTo(Namespace.JAVAX);
        assertThat(results.get(artifacts.get(2))).isEqualTo(Namespace.JAKARTA);
        assertThat(results.get(artifacts.get(3))).isEqualTo(Namespace.UNKNOWN);
    }
}
