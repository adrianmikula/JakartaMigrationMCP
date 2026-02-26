package adrianmikula.jakartamigration.dependencyanalysis.domain;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Maps artifacts to their namespace classifications.
 * Implemented as a record for robust JSON serialization.
 * Keys are artifact coordinate strings (groupId:artifactId:version).
 */
public record NamespaceCompatibilityMap(
        @JsonProperty("namespaceMap") Map<String, Namespace> namespaceMap) {

    public NamespaceCompatibilityMap() {
        this(new HashMap<>());
    }

    /**
     * Factory method to create from a Map of Artifact to Namespace.
     */
    public static NamespaceCompatibilityMap fromMap(Map<Artifact, Namespace> map) {
        Map<String, Namespace> result = new HashMap<>();
        if (map != null) {
            for (Map.Entry<Artifact, Namespace> entry : map.entrySet()) {
                result.put(entry.getKey().toCoordinate(), entry.getValue());
            }
        }
        return new NamespaceCompatibilityMap(result);
    }

    public void put(Artifact artifact, Namespace namespace) {
        namespaceMap.put(artifact.toCoordinate(), namespace);
    }

    public Namespace get(Artifact artifact) {
        return namespaceMap.getOrDefault(artifact.toCoordinate(), Namespace.UNKNOWN);
    }

    public Map<Artifact, Namespace> getAll() {
        Map<Artifact, Namespace> result = new HashMap<>();
        for (Map.Entry<String, Namespace> entry : namespaceMap.entrySet()) {
            result.put(Artifact.fromCoordinate(entry.getKey()), entry.getValue());
        }
        return result;
    }

    public boolean containsKey(Artifact artifact) {
        return namespaceMap.containsKey(artifact.toCoordinate());
    }

    public int size() {
        return namespaceMap.size();
    }
}
