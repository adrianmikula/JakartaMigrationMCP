package adrianmikula.jakartamigration.dependencyanalysis.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * Maven artifact classification configuration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MavenArtifacts {
    
    @JsonProperty("jdk")
    @Builder.Default
    private List<String> jdk = Collections.emptyList();
    
    @JsonProperty("safe")
    @Builder.Default
    private List<String> safe = Collections.emptyList();
    
    @JsonProperty("upgrade")
    @Builder.Default
    private List<String> upgrade = Collections.emptyList();
    
    @JsonProperty("review")
    @Builder.Default
    private List<String> review = Collections.emptyList();
}
