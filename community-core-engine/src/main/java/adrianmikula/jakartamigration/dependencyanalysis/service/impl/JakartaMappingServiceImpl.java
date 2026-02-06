package adrianmikula.jakartamigration.dependencyanalysis.service.impl;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.service.JakartaMappingService;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.*;

/**
 * Implementation of JakartaMappingService that loads mappings from YAML file.
 */
@Slf4j
public class JakartaMappingServiceImpl implements JakartaMappingService {
    
    private final Map<String, MappingEntry> mappings = new HashMap<>();
    private final Map<String, Map<String, String>> versionMappings = new HashMap<>();
    
    public JakartaMappingServiceImpl() {
        loadMappings();
    }
    
    @SuppressWarnings("unchecked")
    private void loadMappings() {
        try (InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("jakarta-mappings.yaml")) {
            
            if (inputStream == null) {
                log.warn("jakarta-mappings.yaml not found in classpath, using empty mappings");
                return;
            }
            
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(inputStream);
            
            List<Map<String, Object>> mappingList = (List<Map<String, Object>>) data.get("mappings");
            if (mappingList == null) {
                log.warn("No mappings found in jakarta-mappings.yaml");
                return;
            }
            
            for (Map<String, Object> mapping : mappingList) {
                Map<String, Object> javax = (Map<String, Object>) mapping.get("javax");
                Map<String, Object> jakarta = (Map<String, Object>) mapping.get("jakarta");
                Map<String, String> versionMapping = (Map<String, String>) mapping.get("versionMapping");
                
                String javaxGroupId = (String) javax.get("groupId");
                String javaxArtifactId = (String) javax.get("artifactId");
                String jakartaGroupId = (String) jakarta.get("groupId");
                String jakartaArtifactId = (String) jakarta.get("artifactId");
                
                String key = javaxGroupId + ":" + javaxArtifactId;
                mappings.put(key, new MappingEntry(jakartaGroupId, jakartaArtifactId));
                
                if (versionMapping != null) {
                    versionMappings.put(key, versionMapping);
                }
            }
            
            log.info("Loaded {} Jakarta mappings from jakarta-mappings.yaml", mappings.size());
            
        } catch (Exception e) {
            log.error("Failed to load jakarta-mappings.yaml", e);
        }
    }
    
    @Override
    public boolean hasMapping(String javaxGroupId, String javaxArtifactId) {
        String key = javaxGroupId + ":" + javaxArtifactId;
        return mappings.containsKey(key);
    }
    
    @Override
    public Optional<JakartaEquivalent> findMapping(Artifact javaxArtifact) {
        String key = javaxArtifact.groupId() + ":" + javaxArtifact.artifactId();
        MappingEntry entry = mappings.get(key);
        
        if (entry == null) {
            return Optional.empty();
        }
        
        String jakartaVersion = getJakartaVersion(
            javaxArtifact.groupId(),
            javaxArtifact.artifactId(),
            javaxArtifact.version()
        ).orElse("6.0.0"); // Default Jakarta version
        
        CompatibilityLevel compatibility = determineCompatibility(
            javaxArtifact.groupId(),
            javaxArtifact.artifactId()
        );
        
        return Optional.of(new JakartaEquivalent(
            entry.jakartaGroupId,
            entry.jakartaArtifactId,
            jakartaVersion,
            compatibility
        ));
    }
    
    @Override
    public Optional<String> getJakartaVersion(String javaxGroupId, String javaxArtifactId, String javaxVersion) {
        String key = javaxGroupId + ":" + javaxArtifactId;
        Map<String, String> versionMap = versionMappings.get(key);
        
        if (versionMap == null) {
            return Optional.empty();
        }
        
        // Try exact match first
        if (versionMap.containsKey(javaxVersion)) {
            return Optional.of(versionMap.get(javaxVersion));
        }
        
        // Try to find closest match (for version ranges)
        // For now, return the first available version
        if (!versionMap.isEmpty()) {
            return Optional.of(versionMap.values().iterator().next());
        }
        
        return Optional.empty();
    }
    
    @Override
    public boolean isJakartaCompatible(String groupId, String artifactId, String version) {
        // Check if it's already Jakarta
        if (groupId.startsWith("jakarta.")) {
            return true;
        }
        
        // Check if it's a Jakarta-compatible framework
        return isJakartaFramework(groupId, artifactId, version);
    }
    
    private boolean isJakartaFramework(String groupId, String artifactId, String version) {
        // Spring Boot 3.x+ uses Jakarta
        if (groupId.equals("org.springframework.boot")) {
            return isVersion3OrHigher(version);
        }
        
        // Quarkus uses Jakarta
        if (groupId.equals("io.quarkus")) {
            return true;
        }
        
        // Jakarta EE implementations
        if (groupId.startsWith("jakarta.")) {
            return true;
        }
        
        // WildFly 26+ uses Jakarta
        if (groupId.equals("org.wildfly") || groupId.equals("org.jboss.as")) {
            return isVersion26OrHigher(version);
        }
        
        return false;
    }
    
    private boolean isVersion3OrHigher(String version) {
        if (version == null || version.isEmpty()) {
            return false;
        }
        try {
            String[] parts = version.split("\\.");
            if (parts.length > 0) {
                int major = Integer.parseInt(parts[0]);
                return major >= 3;
            }
        } catch (NumberFormatException e) {
            // Check string patterns
            return version.startsWith("3.") || version.contains("-3.");
        }
        return false;
    }
    
    private boolean isVersion26OrHigher(String version) {
        if (version == null || version.isEmpty()) {
            return false;
        }
        try {
            String[] parts = version.split("\\.");
            if (parts.length > 0) {
                int major = Integer.parseInt(parts[0]);
                return major >= 26;
            }
        } catch (NumberFormatException e) {
            return false;
        }
        return false;
    }
    
    private CompatibilityLevel determineCompatibility(String javaxGroupId, String javaxArtifactId) {
        // Most Jakarta migrations are drop-in replacements with minor changes
        if (javaxGroupId.equals("javax.mail")) {
            return CompatibilityLevel.DROP_IN_REPLACEMENT;
        }
        if (javaxGroupId.equals("javax.servlet")) {
            return CompatibilityLevel.MINOR_CHANGES;
        }
        if (javaxGroupId.equals("javax.validation")) {
            return CompatibilityLevel.MINOR_CHANGES;
        }
        return CompatibilityLevel.MINOR_CHANGES;
    }
    
    private record MappingEntry(String jakartaGroupId, String jakartaArtifactId) {}
}

