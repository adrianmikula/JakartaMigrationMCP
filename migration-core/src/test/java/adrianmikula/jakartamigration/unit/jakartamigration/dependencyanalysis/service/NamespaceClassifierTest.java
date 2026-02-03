package unit.jakartamigration.dependencyanalysis.service;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Namespace;
import adrianmikula.jakartamigration.dependencyanalysis.service.NamespaceClassifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NamespaceClassifier Tests")
class NamespaceClassifierTest {
    
    private NamespaceClassifier classifier;
    
    @BeforeEach
    void setUp() {
        classifier = new adrianmikula.jakartamigration.dependencyanalysis.service.impl.SimpleNamespaceClassifier();
    }
    
    @Test
    @DisplayName("Should classify javax.servlet as JAVAX")
    void shouldClassifyJavaxServletAsJavax() {
        // Given
        Artifact artifact = new Artifact(
            "javax.servlet",
            "javax.servlet-api",
            "4.0.1",
            "compile",
            false
        );
        
        // When
        Namespace namespace = classifier.classify(artifact);
        
        // Then
        assertEquals(Namespace.JAVAX, namespace);
    }
    
    @Test
    @DisplayName("Should classify jakarta.servlet as JAKARTA")
    void shouldClassifyJakartaServletAsJakarta() {
        // Given
        Artifact artifact = new Artifact(
            "jakarta.servlet",
            "jakarta.servlet-api",
            "6.0.0",
            "compile",
            false
        );
        
        // When
        Namespace namespace = classifier.classify(artifact);
        
        // Then
        assertEquals(Namespace.JAKARTA, namespace);
    }
    
    @Test
    @DisplayName("Should classify Spring Boot 2.x as JAVAX")
    void shouldClassifySpringBoot2AsJavax() {
        // Given
        Artifact artifact = new Artifact(
            "org.springframework.boot",
            "spring-boot-starter-web",
            "2.7.0",
            "compile",
            false
        );
        
        // When
        Namespace namespace = classifier.classify(artifact);
        
        // Then
        assertEquals(Namespace.JAVAX, namespace);
    }
    
    @Test
    @DisplayName("Should classify Spring Boot 3.x as JAKARTA")
    void shouldClassifySpringBoot3AsJakarta() {
        // Given
        Artifact artifact = new Artifact(
            "org.springframework.boot",
            "spring-boot-starter-web",
            "3.2.0",
            "compile",
            false
        );
        
        // When
        Namespace namespace = classifier.classify(artifact);
        
        // Then
        assertEquals(Namespace.JAKARTA, namespace);
    }
    
    @Test
    @DisplayName("Should classify unknown artifact as UNKNOWN")
    void shouldClassifyUnknownArtifactAsUnknown() {
        // Given
        Artifact artifact = new Artifact(
            "com.unknown",
            "unknown-library",
            "1.0.0",
            "compile",
            false
        );
        
        // When
        Namespace namespace = classifier.classify(artifact);
        
        // Then
        assertEquals(Namespace.UNKNOWN, namespace);
    }
    
    @Test
    @DisplayName("Should classify all artifacts in collection")
    void shouldClassifyAllArtifacts() {
        // Given
        List<Artifact> artifacts = List.of(
            new Artifact("javax.servlet", "javax.servlet-api", "4.0.1", "compile", false),
            new Artifact("jakarta.servlet", "jakarta.servlet-api", "6.0.0", "compile", false),
            new Artifact("org.springframework.boot", "spring-boot-starter-web", "3.2.0", "compile", false)
        );
        
        // When
        Map<Artifact, Namespace> classifications = classifier.classifyAll(artifacts);
        
        // Then
        assertEquals(3, classifications.size());
        assertEquals(Namespace.JAVAX, classifications.get(artifacts.get(0)));
        assertEquals(Namespace.JAKARTA, classifications.get(artifacts.get(1)));
        assertEquals(Namespace.JAKARTA, classifications.get(artifacts.get(2)));
    }
}

