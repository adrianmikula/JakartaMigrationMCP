package adrianmikula.jakartamigration.intellij.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AnalyzeMigrationImpactRequest
 */
public class AnalyzeMigrationImpactRequestTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Request should store project path")
    public void testRequestProjectPath() {
        String projectPath = "/home/user/project";
        AnalyzeMigrationImpactRequest request = new AnalyzeMigrationImpactRequest(projectPath);
        assertThat(request.getProjectPath()).isEqualTo(projectPath);
    }

    @Test
    @DisplayName("Request should have default includeSourceAnalysis = true")
    public void testDefaultIncludeSourceAnalysis() {
        AnalyzeMigrationImpactRequest request = new AnalyzeMigrationImpactRequest("/test/path");
        assertThat(request.isIncludeSourceAnalysis()).isTrue();
    }

    @Test
    @DisplayName("Request should have default analysisDepth = STANDARD")
    public void testDefaultAnalysisDepth() {
        AnalyzeMigrationImpactRequest request = new AnalyzeMigrationImpactRequest("/test/path");
        assertThat(request.getAnalysisDepth()).isEqualTo("STANDARD");
    }

    @Test
    @DisplayName("Request should serialize projectPath to JSON correctly")
    public void testRequestJsonSerialization() throws Exception {
        AnalyzeMigrationImpactRequest request = new AnalyzeMigrationImpactRequest("/test/path");
        
        String json = objectMapper.writeValueAsString(request);
        
        assertThat(json).contains("\"projectPath\":\"/test/path\"");
    }

    @Test
    @DisplayName("Request should deserialize from JSON correctly")
    public void testRequestJsonDeserialization() throws Exception {
        String json = "{\"projectPath\":\"/home/user/project\",\"includeSourceAnalysis\":false,\"analysisDepth\":\"DEEP\"}";
        
        AnalyzeMigrationImpactRequest request = objectMapper.readValue(json, AnalyzeMigrationImpactRequest.class);
        
        assertThat(request.getProjectPath()).isEqualTo("/home/user/project");
        assertThat(request.isIncludeSourceAnalysis()).isFalse();
        assertThat(request.getAnalysisDepth()).isEqualTo("DEEP");
    }

    @Test
    @DisplayName("Request should handle empty project path")
    public void testRequestEmptyProjectPath() {
        AnalyzeMigrationImpactRequest request = new AnalyzeMigrationImpactRequest("");
        assertThat(request.getProjectPath()).isEmpty();
    }

    @Test
    @DisplayName("Request should handle special characters in path")
    public void testRequestSpecialCharsInPath() {
        String projectPath = "/home/user/My Project/dir with spaces";
        AnalyzeMigrationImpactRequest request = new AnalyzeMigrationImpactRequest(projectPath);
        assertThat(request.getProjectPath()).isEqualTo(projectPath);
    }

    @Test
    @DisplayName("Request setters should work correctly")
    public void testRequestSetters() {
        AnalyzeMigrationImpactRequest request = new AnalyzeMigrationImpactRequest("/test/path");
        
        request.setIncludeSourceAnalysis(false);
        request.setAnalysisDepth("DEEP");
        
        assertThat(request.isIncludeSourceAnalysis()).isFalse();
        assertThat(request.getAnalysisDepth()).isEqualTo("DEEP");
    }
}
