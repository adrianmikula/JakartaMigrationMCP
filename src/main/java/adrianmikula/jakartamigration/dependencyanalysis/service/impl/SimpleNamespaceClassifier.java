package adrianmikula.jakartamigration.dependencyanalysis.service.impl;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Namespace;
import adrianmikula.jakartamigration.dependencyanalysis.service.NamespaceClassifier;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple rule-based namespace classifier using artifact coordinates and known patterns.
 */
public class SimpleNamespaceClassifier implements NamespaceClassifier {
    
    // Known Jakarta-compatible artifact patterns
    private static final Map<String, String> JAKARTA_ARTIFACTS = Map.of(
        "jakarta.servlet:jakarta.servlet-api", "6.0.0",
        "jakarta.persistence:jakarta.persistence-api", "3.1.0",
        "jakarta.validation:jakarta.validation-api", "3.0.0",
        "jakarta.annotation:jakarta.annotation-api", "2.1.0",
        "jakarta.transaction:jakarta.transaction-api", "2.0.0"
    );
    
    // Known javax artifact patterns
    private static final Map<String, String> JAVAX_ARTIFACTS = Map.of(
        "javax.servlet:javax.servlet-api", "4.0.1",
        "javax.persistence:javax.persistence-api", "2.2",
        "javax.validation:validation-api", "2.0.1",
        "javax.annotation:javax.annotation-api", "1.3.2"
    );
    
    // Spring Boot version thresholds
    private static final String SPRING_BOOT_3_MIN_VERSION = "3.0.0";
    
    @Override
    public Namespace classify(Artifact artifact) {
        String identifier = artifact.toIdentifier();
        
        // Check known Jakarta artifacts
        if (JAKARTA_ARTIFACTS.containsKey(identifier)) {
            String minVersion = JAKARTA_ARTIFACTS.get(identifier);
            if (isVersionGreaterOrEqual(artifact.version(), minVersion)) {
                return Namespace.JAKARTA;
            }
        }
        
        // Check known javax artifacts
        if (JAVAX_ARTIFACTS.containsKey(identifier)) {
            return Namespace.JAVAX;
        }
        
        // Check Spring Boot version
        if (identifier.startsWith("org.springframework.boot:")) {
            if (isVersionGreaterOrEqual(artifact.version(), SPRING_BOOT_3_MIN_VERSION)) {
                return Namespace.JAKARTA; // Spring Boot 3+ uses Jakarta
            } else {
                return Namespace.JAVAX; // Spring Boot 2.x uses javax
            }
        }
        
        // Check Spring Framework version (Spring 6+ uses Jakarta)
        if (identifier.startsWith("org.springframework:spring-")) {
            if (isVersionGreaterOrEqual(artifact.version(), "6.0.0")) {
                return Namespace.JAKARTA;
            } else {
                return Namespace.JAVAX;
            }
        }
        
        // Check groupId patterns
        if (artifact.groupId().startsWith("javax.")) {
            return Namespace.JAVAX;
        }
        
        if (artifact.groupId().startsWith("jakarta.")) {
            return Namespace.JAKARTA;
        }
        
        // Default to unknown
        return Namespace.UNKNOWN;
    }
    
    @Override
    public Map<Artifact, Namespace> classifyAll(Collection<Artifact> artifacts) {
        Map<Artifact, Namespace> result = new HashMap<>();
        for (Artifact artifact : artifacts) {
            result.put(artifact, classify(artifact));
        }
        return result;
    }
    
    /**
     * Simple version comparison. For production, use a proper version comparator.
     * This is a simplified implementation for MVP.
     */
    private boolean isVersionGreaterOrEqual(String version1, String version2) {
        try {
            // Simple semantic version comparison
            String[] v1Parts = version1.split("\\.");
            String[] v2Parts = version2.split("\\.");
            
            int maxLength = Math.max(v1Parts.length, v2Parts.length);
            
            for (int i = 0; i < maxLength; i++) {
                int v1Part = i < v1Parts.length ? parseVersionPart(v1Parts[i]) : 0;
                int v2Part = i < v2Parts.length ? parseVersionPart(v2Parts[i]) : 0;
                
                if (v1Part > v2Part) {
                    return true;
                } else if (v1Part < v2Part) {
                    return false;
                }
            }
            
            return true; // Equal
        } catch (Exception e) {
            // If version parsing fails, do string comparison
            return version1.compareTo(version2) >= 0;
        }
    }
    
    private int parseVersionPart(String part) {
        // Remove non-numeric suffix (e.g., "1.0-SNAPSHOT" -> "1.0")
        String numericPart = part.split("-")[0];
        try {
            return Integer.parseInt(numericPart);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}

