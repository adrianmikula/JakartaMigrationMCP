package adrianmikula.jakartamigration.sourcecodescanning.domain;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Represents javax.* usage found in an XML file.
 */
public record XmlFileUsage(
    Path filePath,
    List<XmlNamespaceUsage> namespaceUsages,
    List<XmlClassReference> classReferences
) {
    public XmlFileUsage {
        Objects.requireNonNull(filePath, "filePath cannot be null");
        Objects.requireNonNull(namespaceUsages, "namespaceUsages cannot be null");
        Objects.requireNonNull(classReferences, "classReferences cannot be null");
    }
    
    /**
     * Returns true if this XML file has any javax.* usage.
     */
    public boolean hasJavaxUsage() {
        return !namespaceUsages.isEmpty() || !classReferences.isEmpty();
    }
    
    /**
     * Represents a javax namespace usage in XML.
     */
    public record XmlNamespaceUsage(
        String namespaceUri,
        String jakartaEquivalent,
        int lineNumber
    ) {}
    
    /**
     * Represents a javax class reference in XML.
     */
    public record XmlClassReference(
        String className,
        String jakartaEquivalent,
        String elementName,
        int lineNumber
    ) {}
}

