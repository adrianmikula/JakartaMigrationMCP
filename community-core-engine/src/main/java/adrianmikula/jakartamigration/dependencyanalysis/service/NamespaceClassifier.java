package adrianmikula.jakartamigration.dependencyanalysis.service;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Namespace;

/**
 * Classifies artifacts by their namespace (javax, jakarta, mixed, or unknown).
 */
public interface NamespaceClassifier {
    
    /**
     * Classifies an artifact's namespace based on its coordinates and metadata.
     *
     * @param artifact The artifact to classify
     * @return The namespace type (JAVAX, JAKARTA, MIXED, or UNKNOWN)
     */
    Namespace classify(Artifact artifact);
    
    /**
     * Classifies multiple artifacts and returns a map of results.
     *
     * @param artifacts The artifacts to classify
     * @return A map from artifact to namespace
     */
    java.util.Map<Artifact, Namespace> classifyAll(java.util.Collection<Artifact> artifacts);
}

