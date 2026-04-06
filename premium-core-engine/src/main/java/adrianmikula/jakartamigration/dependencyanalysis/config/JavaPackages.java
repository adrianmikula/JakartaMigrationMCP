package adrianmikula.jakartamigration.dependencyanalysis.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * Java package classification configuration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JavaPackages {
    
    @JsonProperty("whitelist")
    @Builder.Default
    private List<String> whitelist = Collections.emptyList();
    
    @JsonProperty("blacklist")
    @Builder.Default
    private List<String> blacklist = Collections.emptyList();
}
