package unit.jakartamigration.dependencyanalysis.service.impl;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.service.JakartaMappingService;
import adrianmikula.jakartamigration.dependencyanalysis.service.impl.JakartaMappingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JakartaMappingServiceTest {
    
    private JakartaMappingService mappingService;
    
    @BeforeEach
    void setUp() {
        mappingService = new JakartaMappingServiceImpl();
    }
    
    @Test
    void shouldFindMappingForJavaxServlet() {
        // Given
        Artifact javaxServlet = new Artifact(
            "javax.servlet",
            "javax.servlet-api",
            "4.0.1",
            "compile",
            false
        );
        
        // When
        Optional<JakartaMappingService.JakartaEquivalent> mapping = mappingService.findMapping(javaxServlet);
        
        // Then
        assertThat(mapping).isPresent();
        assertThat(mapping.get().jakartaGroupId()).isEqualTo("jakarta.servlet");
        assertThat(mapping.get().jakartaArtifactId()).isEqualTo("jakarta.servlet-api");
        assertThat(mapping.get().jakartaVersion()).isEqualTo("6.0.0");
    }
    
    @Test
    void shouldFindMappingForJavaxMail() {
        // Given
        Artifact javaxMail = new Artifact(
            "javax.mail",
            "javax.mail",
            "1.5.5",
            "compile",
            false
        );
        
        // When
        Optional<JakartaMappingService.JakartaEquivalent> mapping = mappingService.findMapping(javaxMail);
        
        // Then
        assertThat(mapping).isPresent();
        assertThat(mapping.get().jakartaGroupId()).isEqualTo("com.sun.mail");
        assertThat(mapping.get().jakartaArtifactId()).isEqualTo("jakarta.mail");
        assertThat(mapping.get().jakartaVersion()).isEqualTo("2.0.1");
    }
    
    @Test
    void shouldFindMappingForJavaxValidation() {
        // Given
        Artifact javaxValidation = new Artifact(
            "javax.validation",
            "validation-api",
            "2.0.1.Final",
            "compile",
            false
        );
        
        // When
        Optional<JakartaMappingService.JakartaEquivalent> mapping = mappingService.findMapping(javaxValidation);
        
        // Then
        assertThat(mapping).isPresent();
        assertThat(mapping.get().jakartaGroupId()).isEqualTo("jakarta.validation");
        assertThat(mapping.get().jakartaArtifactId()).isEqualTo("jakarta.validation-api");
        assertThat(mapping.get().jakartaVersion()).isEqualTo("3.1.0");
    }
    
    @Test
    void shouldReturnEmptyForUnknownArtifact() {
        // Given
        Artifact unknown = new Artifact(
            "com.unknown",
            "unknown-lib",
            "1.0.0",
            "compile",
            false
        );
        
        // When
        Optional<JakartaMappingService.JakartaEquivalent> mapping = mappingService.findMapping(unknown);
        
        // Then
        assertThat(mapping).isEmpty();
    }
    
    @Test
    void shouldDetectSpringBoot3AsJakartaCompatible() {
        // When/Then
        assertThat(mappingService.isJakartaCompatible("org.springframework.boot", "spring-boot-starter-web", "3.2.0"))
            .isTrue();
        assertThat(mappingService.isJakartaCompatible("org.springframework.boot", "spring-boot-starter-web", "3.0.0"))
            .isTrue();
        assertThat(mappingService.isJakartaCompatible("org.springframework.boot", "spring-boot-starter-web", "2.7.7"))
            .isFalse();
    }
    
    @Test
    void shouldDetectQuarkusAsJakartaCompatible() {
        // When/Then
        assertThat(mappingService.isJakartaCompatible("io.quarkus", "quarkus-core", "3.0.0"))
            .isTrue();
    }
    
    @Test
    void shouldDetectJakartaArtifactsAsCompatible() {
        // When/Then
        assertThat(mappingService.isJakartaCompatible("jakarta.servlet", "jakarta.servlet-api", "6.0.0"))
            .isTrue();
    }
    
    @Test
    void shouldCheckHasMapping() {
        // When/Then
        assertThat(mappingService.hasMapping("javax.servlet", "javax.servlet-api")).isTrue();
        assertThat(mappingService.hasMapping("javax.mail", "javax.mail")).isTrue();
        assertThat(mappingService.hasMapping("com.unknown", "unknown")).isFalse();
    }
    
    @Test
    void shouldGetJakartaVersion() {
        // When/Then
        Optional<String> version = mappingService.getJakartaVersion("javax.servlet", "javax.servlet-api", "4.0.1");
        assertThat(version).isPresent();
        assertThat(version.get()).isEqualTo("6.0.0");
        
        version = mappingService.getJakartaVersion("javax.mail", "javax.mail", "1.5.5");
        assertThat(version).isPresent();
        assertThat(version.get()).isEqualTo("2.0.1");
    }
}

