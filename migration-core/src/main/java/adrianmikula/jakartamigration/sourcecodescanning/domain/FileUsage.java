package adrianmikula.jakartamigration.sourcecodescanning.domain;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Represents javax.* usage in a single file.
 * Includes both import statements and reflection-based usages.
 */
public record FileUsage(
    Path filePath,
    List<ImportStatement> javaxImports,
    int lineCount,
    List<ReflectionUsage> reflectionUsages
) {
    public FileUsage {
        Objects.requireNonNull(filePath, "filePath cannot be null");
        Objects.requireNonNull(javaxImports, "javaxImports cannot be null");
        Objects.requireNonNull(reflectionUsages, "reflectionUsages cannot be null");
        if (lineCount < 0) {
            throw new IllegalArgumentException("lineCount cannot be negative");
        }
    }
    
    /**
     * Convenience constructor for files without reflection usages.
     */
    public FileUsage(Path filePath, List<ImportStatement> javaxImports, int lineCount) {
        this(filePath, javaxImports, lineCount, List.of());
    }
    
    /**
     * Returns true if this file has any javax.* usage.
     */
    public boolean hasJavaxUsage() {
        return !javaxImports.isEmpty() || !reflectionUsages.isEmpty();
    }
    
    /**
     * Returns the number of javax imports found.
     */
    public int getJavaxImportCount() {
        return javaxImports.size();
    }
    
    /**
     * Returns the number of reflection-based usages found.
     */
    public int getReflectionUsageCount() {
        return reflectionUsages.size();
    }
    
    /**
     * Returns the total number of javax-related usages (imports + reflection).
     */
    public int getTotalJavaxCount() {
        return getJavaxImportCount() + getReflectionUsageCount();
    }
    
    /**
     * Returns true if this file has critical reflection usages that might break migration.
     */
    public boolean hasCriticalReflectionUsages() {
        return reflectionUsages.stream().anyMatch(ReflectionUsage::isCritical);
    }
    
    /**
     * Returns list of critical reflection usages.
     */
    public List<ReflectionUsage> getCriticalReflectionUsages() {
        return reflectionUsages.stream()
                .filter(ReflectionUsage::isCritical)
                .toList();
    }
    
    /**
     * Returns all reflection usages of a specific type.
     */
    public List<ReflectionUsage> getReflectionUsagesByType(ReflectionUsage.ReflectionType type) {
        return reflectionUsages.stream()
                .filter(u -> u.type() == type)
                .toList();
    }
    
    /**
     * Returns a summary of all reflection usages for this file.
     */
    public ReflectionSummary getReflectionSummary() {
        long classForNameCount = reflectionUsages.stream()
                .filter(u -> u.type() == ReflectionUsage.ReflectionType.CLASS_FOR_NAME)
                .count();
        long classLoaderCount = reflectionUsages.stream()
                .filter(u -> u.type() == ReflectionUsage.ReflectionType.CLASS_LOADER)
                .count();
        long jndiCount = reflectionUsages.stream()
                .filter(u -> u.type() == ReflectionUsage.ReflectionType.JNDI_LOOKUP)
                .count();
        long annotationCount = reflectionUsages.stream()
                .filter(u -> u.type() == ReflectionUsage.ReflectionType.ANNOTATION_VALUE)
                .count();
        long otherCount = reflectionUsages.stream()
                .filter(u -> u.type() == ReflectionUsage.ReflectionType.OTHER)
                .count();
        
        return new ReflectionSummary(
                reflectionUsages.size(),
                classForNameCount,
                classLoaderCount,
                jndiCount,
                annotationCount,
                otherCount,
                hasCriticalReflectionUsages()
        );
    }
    
    /**
     * Summary of reflection usages in this file.
     */
    public record ReflectionSummary(
            int total,
            long classForName,
            long classLoader,
            long jndi,
            long annotation,
            long other,
            boolean hasCritical
    ) {}
}
