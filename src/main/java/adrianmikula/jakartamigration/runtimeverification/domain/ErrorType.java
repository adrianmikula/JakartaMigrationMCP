package adrianmikula.jakartamigration.runtimeverification.domain;

/**
 * Types of runtime errors that can occur.
 */
public enum ErrorType {
    /**
     * Class not found at runtime.
     */
    CLASS_NOT_FOUND,
    
    /**
     * No class definition found error.
     */
    NO_CLASS_DEF_FOUND,
    
    /**
     * Linkage error (binary incompatibility).
     */
    LINKAGE_ERROR,
    
    /**
     * No such method error.
     */
    NO_SUCH_METHOD,
    
    /**
     * No such field error.
     */
    NO_SUCH_FIELD,
    
    /**
     * Illegal access error.
     */
    ILLEGAL_ACCESS,
    
    /**
     * Class cast exception.
     */
    CLASS_CAST,
    
    /**
     * Other or unknown error type.
     */
    OTHER
}

