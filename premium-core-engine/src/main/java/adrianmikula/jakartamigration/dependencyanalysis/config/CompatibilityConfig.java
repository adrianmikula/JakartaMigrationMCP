package adrianmikula.jakartamigration.dependencyanalysis.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Root configuration for compatibility.yaml
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompatibilityConfig {
    
    @JsonProperty("maven_artifacts")
    private MavenArtifacts mavenArtifacts;
    
    @JsonProperty("java_packages")
    private JavaPackages javaPackages;
}
