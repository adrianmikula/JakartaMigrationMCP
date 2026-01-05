package com.bugbounty.jakartamigration.dependencyanalysis.domain;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps artifacts to their namespace classifications.
 */
public class NamespaceCompatibilityMap {
    private final Map<Artifact, Namespace> namespaceMap;
    
    public NamespaceCompatibilityMap() {
        this.namespaceMap = new HashMap<>();
    }
    
    public NamespaceCompatibilityMap(Map<Artifact, Namespace> namespaceMap) {
        this.namespaceMap = new HashMap<>(namespaceMap);
    }
    
    public void put(Artifact artifact, Namespace namespace) {
        namespaceMap.put(artifact, namespace);
    }
    
    public Namespace get(Artifact artifact) {
        return namespaceMap.getOrDefault(artifact, Namespace.UNKNOWN);
    }
    
    public Map<Artifact, Namespace> getAll() {
        return new HashMap<>(namespaceMap);
    }
    
    public boolean containsKey(Artifact artifact) {
        return namespaceMap.containsKey(artifact);
    }
    
    public int size() {
        return namespaceMap.size();
    }
}

