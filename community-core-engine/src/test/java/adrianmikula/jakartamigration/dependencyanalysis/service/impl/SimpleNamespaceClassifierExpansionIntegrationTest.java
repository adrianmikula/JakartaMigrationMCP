package adrianmikula.jakartamigration.dependencyanalysis.service.impl;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Namespace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SimpleNamespaceClassifier Expansion Integration Tests")
@Tag("slow")
class SimpleNamespaceClassifierExpansionIntegrationTest {

    private SimpleNamespaceClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new SimpleNamespaceClassifier();
    }

    @Test
    @DisplayName("Should classify Spring Boot 3.x dependencies as JAKARTA")
    void shouldClassifySpringBoot3Project() {
        List<Artifact> springBoot3Deps = List.of(
            new Artifact("org.springframework.boot", "spring-boot-starter-web", "3.2.0", "compile", false),
            new Artifact("org.springframework.boot", "spring-boot-starter-data-jpa", "3.1.0", "compile", false),
            new Artifact("org.springframework.boot", "spring-boot-starter-test", "3.0.0", "test", false)
        );

        for (Artifact artifact : springBoot3Deps) {
            Namespace result = classifier.classify(artifact);
            assertThat(result).isEqualTo(Namespace.JAKARTA)
                .as("Spring Boot 3.x dependency %s should be JAKARTA", artifact.toCoordinate());
        }
    }

    @Test
    @DisplayName("Should classify Spring Boot 2.x dependencies as JAVAX")
    void shouldClassifySpringBoot2Project() {
        List<Artifact> springBoot2Deps = List.of(
            new Artifact("org.springframework.boot", "spring-boot-starter-web", "2.7.18", "compile", false),
            new Artifact("org.springframework.boot", "spring-boot-starter-data-jpa", "2.6.0", "compile", false),
            new Artifact("org.springframework.boot", "spring-boot-starter-test", "2.5.0", "test", false)
        );

        for (Artifact artifact : springBoot2Deps) {
            Namespace result = classifier.classify(artifact);
            assertThat(result).isEqualTo(Namespace.JAVAX)
                .as("Spring Boot 2.x dependency %s should be JAVAX", artifact.toCoordinate());
        }
    }

    @Test
    @DisplayName("Should classify Spring Framework 6.x dependencies as JAKARTA")
    void shouldClassifySpringFramework6Project() {
        List<Artifact> spring6Deps = List.of(
            new Artifact("org.springframework", "spring-core", "6.1.0", "compile", false),
            new Artifact("org.springframework", "spring-web", "6.0.0", "compile", false),
            new Artifact("org.springframework", "spring-context", "6.2.0", "compile", false)
        );

        for (Artifact artifact : spring6Deps) {
            Namespace result = classifier.classify(artifact);
            assertThat(result).isEqualTo(Namespace.JAKARTA)
                .as("Spring Framework 6.x dependency %s should be JAKARTA", artifact.toCoordinate());
        }
    }

    @Test
    @DisplayName("Should classify Spring Framework 5.x dependencies as JAVAX")
    void shouldClassifySpringFramework5Project() {
        List<Artifact> spring5Deps = List.of(
            new Artifact("org.springframework", "spring-core", "5.3.31", "compile", false),
            new Artifact("org.springframework", "spring-web", "5.2.25.RELEASE", "compile", false),
            new Artifact("org.springframework", "spring-context", "5.0.0", "compile", false)
        );

        for (Artifact artifact : spring5Deps) {
            Namespace result = classifier.classify(artifact);
            assertThat(result).isEqualTo(Namespace.JAVAX)
                .as("Spring Framework 5.x dependency %s should be JAVAX", artifact.toCoordinate());
        }
    }

    @Test
    @DisplayName("Should classify Hibernate 6.x dependencies as JAKARTA")
    void shouldClassifyHibernate6Project() {
        List<Artifact> hibernate6Deps = List.of(
            new Artifact("org.hibernate.orm", "hibernate-core", "6.0.0.Final", "compile", false),
            new Artifact("org.hibernate.orm", "hibernate-core", "6.4.0.Final", "compile", false),
            new Artifact("org.hibernate.orm", "hibernate-core", "6.5.0", "compile", false)
        );

        for (Artifact artifact : hibernate6Deps) {
            Namespace result = classifier.classify(artifact);
            assertThat(result).isEqualTo(Namespace.JAKARTA)
                .as("Hibernate 6.x dependency %s should be JAKARTA", artifact.toCoordinate());
        }
    }

    @Test
    @DisplayName("Should classify Hibernate 5.x dependencies as JAVAX")
    void shouldClassifyHibernate5Project() {
        List<Artifact> hibernate5Deps = List.of(
            new Artifact("org.hibernate", "hibernate-core", "5.6.1.Final", "compile", false),
            new Artifact("org.hibernate", "hibernate-core", "5.4.0.Final", "compile", false)
        );

        for (Artifact artifact : hibernate5Deps) {
            Namespace result = classifier.classify(artifact);
            assertThat(result).isEqualTo(Namespace.JAVAX)
                .as("Hibernate 5.x dependency %s should be JAVAX", artifact.toCoordinate());
        }
    }

    @Test
    @DisplayName("Should classify Jersey 3.x dependencies as JAKARTA")
    void shouldClassifyJersey3Project() {
        List<Artifact> jersey3Deps = List.of(
            new Artifact("org.glassfish.jersey.core", "jersey-server", "3.1.0", "compile", false),
            new Artifact("org.glassfish.jersey.core", "jersey-common", "3.0.0", "compile", false),
            new Artifact("org.glassfish.jersey.core", "jersey-client", "3.2.0", "compile", false),
            new Artifact("org.glassfish.jersey.containers", "jersey-container-servlet", "3.1.0", "compile", false),
            new Artifact("org.glassfish.jersey.inject", "jersey-hk2", "3.0.0", "compile", false)
        );

        for (Artifact artifact : jersey3Deps) {
            Namespace result = classifier.classify(artifact);
            assertThat(result).isEqualTo(Namespace.JAKARTA)
                .as("Jersey 3.x dependency %s should be JAKARTA", artifact.toCoordinate());
        }
    }

    @Test
    @DisplayName("Should classify Jersey 2.x dependencies as JAVAX")
    void shouldClassifyJersey2Project() {
        List<Artifact> jersey2Deps = List.of(
            new Artifact("org.glassfish.jersey.core", "jersey-server", "2.39.1", "compile", false),
            new Artifact("org.glassfish.jersey.core", "jersey-common", "2.35", "compile", false),
            new Artifact("org.glassfish.jersey.containers", "jersey-container-servlet", "2.25", "compile", false)
        );

        for (Artifact artifact : jersey2Deps) {
            Namespace result = classifier.classify(artifact);
            assertThat(result).isEqualTo(Namespace.JAVAX)
                .as("Jersey 2.x dependency %s should be JAVAX", artifact.toCoordinate());
        }
    }

    @Test
    @DisplayName("Should classify RESTEasy 6.x dependencies as JAKARTA")
    void shouldClassifyRESTEasy6Project() {
        List<Artifact> resteasy6Deps = List.of(
            new Artifact("org.jboss.resteasy", "resteasy-core", "6.0.0.Final", "compile", false),
            new Artifact("org.jboss.resteasy", "resteasy-core", "6.2.0", "compile", false)
        );

        for (Artifact artifact : resteasy6Deps) {
            Namespace result = classifier.classify(artifact);
            assertThat(result).isEqualTo(Namespace.JAKARTA)
                .as("RESTEasy 6.x dependency %s should be JAKARTA", artifact.toCoordinate());
        }
    }

    @Test
    @DisplayName("Should classify RESTEasy 4.x dependencies as JAVAX")
    void shouldClassifyRESTEasy4Project() {
        List<Artifact> resteasy4Deps = List.of(
            new Artifact("org.jboss.resteasy", "resteasy-core", "4.7.9.Final", "compile", false),
            new Artifact("org.jboss.resteasy", "resteasy-core", "3.15.1.Final", "compile", false)
        );

        for (Artifact artifact : resteasy4Deps) {
            Namespace result = classifier.classify(artifact);
            assertThat(result).isEqualTo(Namespace.JAVAX)
                .as("RESTEasy 4.x dependency %s should be JAVAX", artifact.toCoordinate());
        }
    }

    @Test
    @DisplayName("Should classify known Jakarta EE artifacts with correct version check")
    void shouldClassifyJakartaEEArtifactsWithVersionCheck() {
        assertThat(classifier.classify(new Artifact(
            "jakarta.servlet", "jakarta.servlet-api", "6.0.0", "compile", false)))
            .isEqualTo(Namespace.JAKARTA);

        assertThat(classifier.classify(new Artifact(
            "jakarta.persistence", "jakarta.persistence-api", "3.1.0", "compile", false)))
            .isEqualTo(Namespace.JAKARTA);

        assertThat(classifier.classify(new Artifact(
            "jakarta.validation", "jakarta.validation-api", "3.0.0", "compile", false)))
            .isEqualTo(Namespace.JAKARTA);

        assertThat(classifier.classify(new Artifact(
            "jakarta.annotation", "jakarta.annotation-api", "2.1.0", "compile", false)))
            .isEqualTo(Namespace.JAKARTA);

        assertThat(classifier.classify(new Artifact(
            "jakarta.transaction", "jakarta.transaction-api", "2.0.0", "compile", false)))
            .isEqualTo(Namespace.JAKARTA);

        assertThat(classifier.classify(new Artifact(
            "jakarta.ws.rs", "jakarta.ws.rs-api", "3.1.0", "compile", false)))
            .isEqualTo(Namespace.JAKARTA);

        assertThat(classifier.classify(new Artifact(
            "jakarta.ejb", "jakarta.ejb-api", "4.0.0", "compile", false)))
            .isEqualTo(Namespace.JAKARTA);

        assertThat(classifier.classify(new Artifact(
            "jakarta.enterprise", "jakarta.enterprise.cdi-api", "4.0.0", "compile", false)))
            .isEqualTo(Namespace.JAKARTA);

        assertThat(classifier.classify(new Artifact(
            "jakarta.inject", "jakarta.inject-api", "2.0.1", "compile", false)))
            .isEqualTo(Namespace.JAKARTA);
    }

    @Test
    @DisplayName("Should classify known javax artifacts as needing migration")
    void shouldClassifyKnownJavaxArtifactsAsNeedingMigration() {
        assertThat(classifier.classify(new Artifact(
            "javax.servlet", "javax.servlet-api", "4.0.1", "compile", false)))
            .isEqualTo(Namespace.JAVAX);

        assertThat(classifier.classify(new Artifact(
            "javax.persistence", "javax.persistence-api", "2.2", "compile", false)))
            .isEqualTo(Namespace.JAVAX);

        assertThat(classifier.classify(new Artifact(
            "javax.validation", "validation-api", "2.0.1", "compile", false)))
            .isEqualTo(Namespace.JAVAX);

        assertThat(classifier.classify(new Artifact(
            "javax.annotation", "javax.annotation-api", "1.3.2", "compile", false)))
            .isEqualTo(Namespace.JAVAX);

        assertThat(classifier.classify(new Artifact(
            "javax.ws.rs", "javax.ws.rs-api", "2.1.1", "compile", false)))
            .isEqualTo(Namespace.JAVAX);

        assertThat(classifier.classify(new Artifact(
            "javax.ejb", "javax.ejb-api", "3.2", "compile", false)))
            .isEqualTo(Namespace.JAVAX);

        assertThat(classifier.classify(new Artifact(
            "javax.inject", "javax.inject", "1", "compile", false)))
            .isEqualTo(Namespace.JAVAX);
    }

    @Test
    @DisplayName("Should classify Hibernate Validator 8.x as JAKARTA")
    void shouldClassifyHibernateValidator8AsJakarta() {
        Artifact artifact = new Artifact(
            "org.hibernate.validator", "hibernate-validator", "8.0.0.Final", "compile", false);

        Namespace result = classifier.classify(artifact);

        assertThat(result).isEqualTo(Namespace.JAKARTA);
    }

    @Test
    @DisplayName("Should classify Hibernate Validator 6.x as JAVAX")
    void shouldClassifyHibernateValidator6AsJavax() {
        Artifact artifact = new Artifact(
            "org.hibernate.validator", "hibernate-validator", "6.2.5.Final", "compile", false);

        Namespace result = classifier.classify(artifact);

        assertThat(result).isEqualTo(Namespace.JAVAX);
    }

    @Test
    @DisplayName("Should classify CXF 4.x as JAKARTA")
    void shouldClassifyCxf4AsJakarta() {
        Artifact artifact = new Artifact(
            "org.apache.cxf", "cxf-rt-frontend-jaxrs", "4.0.0", "compile", false);

        Namespace result = classifier.classify(artifact);

        assertThat(result).isEqualTo(Namespace.JAKARTA);
    }

    @Test
    @DisplayName("Should classify CXF 3.x as JAVAX")
    void shouldClassifyCxf3AsJavax() {
        Artifact artifact = new Artifact(
            "org.apache.cxf", "cxf-rt-frontend-jaxrs", "3.6.0", "compile", false);

        Namespace result = classifier.classify(artifact);

        assertThat(result).isEqualTo(Namespace.JAVAX);
    }

    @Test
    @DisplayName("Should classify Jakarta groupId prefix artifacts as JAKARTA")
    void shouldClassifyJakartaGroupIdPrefix() {
        Artifact artifact = new Artifact(
            "jakarta.json", "jakarta.json-api", "2.0.0", "compile", false);

        Namespace result = classifier.classify(artifact);

        assertThat(result).isEqualTo(Namespace.JAKARTA);
    }

    @Test
    @DisplayName("Should classify javax groupId prefix artifacts as JAVAX (non-JDK)")
    void shouldClassifyJavaxGroupIdPrefix() {
        Artifact artifact = new Artifact(
            "javax.mail", "javax.mail-api", "1.6.2", "compile", false);

        Namespace result = classifier.classify(artifact);

        assertThat(result).isEqualTo(Namespace.JAVAX);
    }

    @Test
    @DisplayName("Should classify JDK javax packages as UNKNOWN")
    void shouldClassifyJdkJavaxPackagesAsUnknown() {
        List<Artifact> jdkJavax = List.of(
            new Artifact("javax.management", "jmx-remote-api", "1.0", "compile", false),
            new Artifact("javax.naming", "ldap", "1.0", "compile", false),
            new Artifact("javax.crypto", "crypto", "1.0", "compile", false),
            new Artifact("javax.net", "ssl", "1.0", "compile", false),
            new Artifact("javax.script", "script-engine", "1.0", "compile", false),
            new Artifact("javax.sql", "rowset", "1.0", "compile", false)
        );

        for (Artifact artifact : jdkJavax) {
            Namespace result = classifier.classify(artifact);
            assertThat(result).isEqualTo(Namespace.UNKNOWN)
                .as("JDK javax package %s should be UNKNOWN", artifact.toCoordinate());
        }
    }

    @Test
    @DisplayName("Should classify batch of mixed artifacts correctly")
    void shouldClassifyBatchOfMixedArtifacts() {
        List<Artifact> mixedArtifacts = List.of(
            new Artifact("jakarta.servlet", "jakarta.servlet-api", "6.0.0", "compile", false),
            new Artifact("javax.servlet", "javax.servlet-api", "4.0.1", "compile", false),
            new Artifact("org.hibernate.orm", "hibernate-core", "6.4.0.Final", "compile", false),
            new Artifact("org.hibernate", "hibernate-core", "5.6.1.Final", "compile", false),
            new Artifact("org.springframework.boot", "spring-boot-starter-web", "3.2.0", "compile", false),
            new Artifact("org.springframework.boot", "spring-boot-starter-web", "2.7.18", "compile", false),
            new Artifact("com.google.guava", "guava", "31.1-jre", "compile", false)
        );

        Map<Artifact, Namespace> results = classifier.classifyAll(mixedArtifacts);

        assertThat(results).hasSize(7);
        assertThat(results.values()).containsExactlyInAnyOrder(
            Namespace.JAKARTA, Namespace.JAVAX, Namespace.JAKARTA, Namespace.JAVAX,
            Namespace.JAKARTA, Namespace.JAVAX, Namespace.UNKNOWN
        );
    }
}
