package com.bugbounty.jakartamigration.dependencyanalysis.domain;

/**
 * Represents a transitive dependency conflict (mixed javax/jakarta).
 */
public record TransitiveConflict(
    Artifact rootArtifact,
    Artifact conflictingArtifact,
    String conflictType,
    String description
) {}

