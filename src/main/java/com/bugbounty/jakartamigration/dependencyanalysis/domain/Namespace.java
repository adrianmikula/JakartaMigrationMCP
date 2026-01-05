package com.bugbounty.jakartamigration.dependencyanalysis.domain;

/**
 * Represents the namespace type of a Java artifact.
 */
public enum Namespace {
    /**
     * Artifact uses javax.* packages only.
     */
    JAVAX,
    
    /**
     * Artifact uses jakarta.* packages only.
     */
    JAKARTA,
    
    /**
     * Artifact uses both javax.* and jakarta.* packages (transitional).
     */
    MIXED,
    
    /**
     * Namespace cannot be determined.
     */
    UNKNOWN
}

