package com.bugbounty.jakartamigration.coderefactoring.domain;

/**
 * Type of change made during refactoring.
 */
public enum ChangeType {
    /**
     * Import statement changed (e.g., javax.* to jakarta.*)
     */
    IMPORT_CHANGE,
    
    /**
     * Package declaration changed
     */
    PACKAGE_CHANGE,
    
    /**
     * Type reference changed
     */
    TYPE_REFERENCE_CHANGE,
    
    /**
     * XML namespace changed
     */
    XML_NAMESPACE_CHANGE,
    
    /**
     * XML attribute changed
     */
    XML_ATTRIBUTE_CHANGE,
    
    /**
     * Other type of change
     */
    OTHER
}

