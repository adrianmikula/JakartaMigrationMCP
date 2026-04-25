package adrianmikula.jakartamigration.pdfreporting.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Test project loader that reads examples.yaml and provides test project data.
 * Used for memory integration testing with real-world project examples.
 */
public class ExamplesTestProjectLoader {
    
    private final ObjectMapper yamlMapper;
    private final Map<String, Object> examplesData;
    
    public ExamplesTestProjectLoader() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        try (InputStream is = getClass().getResourceAsStream("/examples.yaml")) {
            if (is == null) {
                // Fallback to classpath examples
                is = getClass().getResourceAsStream("/examples.yaml");
            }
            this.examplesData = yamlMapper.readValue(is, Map.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load examples.yaml", e);
        }
    }
    
    /**
     * Load a test project by name from examples.yaml
     */
    public TestProject loadProject(String projectName) {
        if (!examplesData.containsKey("application_servers")) {
            throw new IllegalArgumentException("No application_servers found in examples.yaml");
        }
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> appServers = (List<Map<String, Object>>) examplesData.get("application_servers");
        
        for (Map<String, Object> server : appServers) {
            String name = (String) server.get("name");
            if (name != null && name.toLowerCase().contains(projectName.toLowerCase())) {
                return new TestProject(
                    name,
                    (String) server.get("description"),
                    (String) server.get("url"),
                    determineProjectSize(name)
                );
            }
        }
        
        throw new IllegalArgumentException("Project not found: " + projectName);
    }
    
    /**
     * Get all available test projects
     */
    public List<TestProject> getAllProjects() {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> appServers = (List<Map<String, Object>>) examplesData.get("application_servers");
        
        return appServers.stream()
            .map(server -> new TestProject(
                (String) server.get("name"),
                (String) server.get("description"),
                (String) server.get("url"),
                determineProjectSize((String) server.get("name"))
            ))
            .toList();
    }
    
    private TestProjectSize determineProjectSize(String projectName) {
        if (projectName == null) return TestProjectSize.MEDIUM;
        
        String lowerName = projectName.toLowerCase();
        if (lowerName.contains("netbeans") || lowerName.contains("javaee") || lowerName.contains("spring")) {
            return TestProjectSize.LARGE;
        } else if (lowerName.contains("tomcat") || lowerName.contains("wildfly")) {
            return TestProjectSize.MEDIUM;
        } else {
            return TestProjectSize.SMALL;
        }
    }
    
    /**
     * Test project data holder
     */
    public record TestProject(
        String name,
        String description,
        String url,
        TestProjectSize size
    ) {}
    
    /**
     * Project size enumeration for memory testing
     */
    public enum TestProjectSize {
        SMALL, MEDIUM, LARGE
    }
}
