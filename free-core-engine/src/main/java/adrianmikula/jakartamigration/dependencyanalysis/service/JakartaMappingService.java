package adrianmikula.jakartamigration.dependencyanalysis.service;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;

import java.util.Optional;

/**
 * Service for finding Jakarta equivalents of javax artifacts using a knowledge base.
 */
public interface JakartaMappingService {
    
    /**
     * Checks if a mapping exists for the given javax artifact.
     */
    boolean hasMapping(String javaxGroupId, String javaxArtifactId);
    
    /**
     * Finds the Jakarta equivalent for a javax artifact.
     */
    Optional<JakartaEquivalent> findMapping(Artifact javaxArtifact);
    
    /**
     * Gets the Jakarta version for a specific javax version.
     */
    Optional<String> getJakartaVersion(String javaxGroupId, String javaxArtifactId, String javaxVersion);
    
    /**
     * Checks if a framework/library is Jakarta-compatible.
     */
    boolean isJakartaCompatible(String groupId, String artifactId, String version);
    
    /**
     * Represents a Jakarta equivalent mapping.
     */
    record JakartaEquivalent(
        String jakartaGroupId,
        String jakartaArtifactId,
        String jakartaVersion,
        CompatibilityLevel compatibility
    ) {}
    
    /**
     * Compatibility level of the migration.
     */
    enum CompatibilityLevel {
        DROP_IN_REPLACEMENT,
        MINOR_CHANGES,
        MAJOR_REFACTOR,
        NO_EQUIVALENT
    }
}

