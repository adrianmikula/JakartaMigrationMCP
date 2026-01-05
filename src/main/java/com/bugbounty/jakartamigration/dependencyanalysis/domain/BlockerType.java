package com.bugbounty.jakartamigration.dependencyanalysis.domain;

/**
 * Types of blockers that can prevent Jakarta migration.
 */
public enum BlockerType {
    /**
     * No Jakarta equivalent exists for this artifact.
     */
    NO_JAKARTA_EQUIVALENT,
    
    /**
     * Transitive dependency conflict (mixed javax/jakarta).
     */
    TRANSITIVE_CONFLICT,
    
    /**
     * Binary incompatibility detected.
     */
    BINARY_INCOMPATIBLE,
    
    /**
     * Version incompatibility.
     */
    VERSION_INCOMPATIBLE
}

