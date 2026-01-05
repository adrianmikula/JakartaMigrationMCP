package com.bugbounty.jakartamigration.coderefactoring.domain;

/**
 * Status of a rollback operation.
 */
public enum RollbackStatus {
    /**
     * Rollback completed successfully
     */
    SUCCESS,
    
    /**
     * Rollback failed
     */
    FAILED,
    
    /**
     * Checkpoint not found
     */
    CHECKPOINT_NOT_FOUND,
    
    /**
     * File not found
     */
    FILE_NOT_FOUND,
    
    /**
     * Rollback in progress
     */
    IN_PROGRESS
}

