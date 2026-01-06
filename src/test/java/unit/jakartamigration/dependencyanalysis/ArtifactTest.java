package unit.jakartamigration.dependencyanalysis;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Artifact Tests")
class ArtifactTest {
    
    @Test
    @DisplayName("Should create artifact with all fields")
    void shouldCreateArtifact() {
        // Given
        String groupId = "com.example";
        String artifactId = "library";
        String version = "1.0.0";
        String scope = "compile";
        
        // When
        Artifact artifact = new Artifact(groupId, artifactId, version, scope, false);
        
        // Then
        assertEquals(groupId, artifact.groupId());
        assertEquals(artifactId, artifact.artifactId());
        assertEquals(version, artifact.version());
        assertEquals(scope, artifact.scope());
        assertFalse(artifact.transitive());
    }
    
    @Test
    @DisplayName("Should generate correct coordinate string")
    void shouldGenerateCoordinate() {
        // Given
        Artifact artifact = new Artifact("com.example", "library", "1.0.0", "compile", false);
        
        // When
        String coordinate = artifact.toCoordinate();
        
        // Then
        assertEquals("com.example:library:1.0.0", coordinate);
    }
    
    @Test
    @DisplayName("Should generate correct identifier string")
    void shouldGenerateIdentifier() {
        // Given
        Artifact artifact = new Artifact("com.example", "library", "1.0.0", "compile", false);
        
        // When
        String identifier = artifact.toIdentifier();
        
        // Then
        assertEquals("com.example:library", identifier);
    }
    
    @Test
    @DisplayName("Should throw exception when groupId is null")
    void shouldThrowWhenGroupIdIsNull() {
        // When & Then
        assertThrows(NullPointerException.class, () -> 
            new Artifact(null, "library", "1.0.0", "compile", false)
        );
    }
    
    @Test
    @DisplayName("Should throw exception when artifactId is null")
    void shouldThrowWhenArtifactIdIsNull() {
        // When & Then
        assertThrows(NullPointerException.class, () -> 
            new Artifact("com.example", null, "1.0.0", "compile", false)
        );
    }
    
    @Test
    @DisplayName("Should throw exception when version is null")
    void shouldThrowWhenVersionIsNull() {
        // When & Then
        assertThrows(NullPointerException.class, () -> 
            new Artifact("com.example", "library", null, "compile", false)
        );
    }
    
    @Test
    @DisplayName("Should throw exception when scope is null")
    void shouldThrowWhenScopeIsNull() {
        // When & Then
        assertThrows(NullPointerException.class, () -> 
            new Artifact("com.example", "library", "1.0.0", null, false)
        );
    }
}

