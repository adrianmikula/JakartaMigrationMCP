package adrianmikula.jakartamigration.sourcecodescanning.domain;

import java.util.Objects;

/**
 * Represents a javax.* import statement found in source code.
 */
public record ImportStatement(
    String fullImport,
    String javaxPackage,
    String jakartaEquivalent,
    int lineNumber
) {
    public ImportStatement {
        Objects.requireNonNull(fullImport, "fullImport cannot be null");
        Objects.requireNonNull(javaxPackage, "javaxPackage cannot be null");
        Objects.requireNonNull(jakartaEquivalent, "jakartaEquivalent cannot be null");
        if (lineNumber < 1) {
            throw new IllegalArgumentException("lineNumber must be >= 1");
        }
    }
    
    /**
     * Returns true if this import is a javax.* import.
     */
    public boolean isJavaxImport() {
        return fullImport.startsWith("javax.");
    }
}

