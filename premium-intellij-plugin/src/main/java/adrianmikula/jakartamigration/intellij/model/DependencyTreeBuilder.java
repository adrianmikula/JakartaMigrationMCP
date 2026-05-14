package adrianmikula.jakartamigration.intellij.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds a hierarchical tree structure from a flat list of dependencies.
 * Uses the depth field to establish parent-child relationships.
 */
public class DependencyTreeBuilder {

    /**
     * Converts a flat list of dependencies to a hierarchical tree structure.
     * Assumes dependencies are ordered by their appearance in the dependency tree.
     *
     * @param dependencies Flat list of dependencies with depth information
     * @param mergeDuplicates If true, hides re-occurrences of the same dependency coordinates
     * @return List of root dependencies (depth 0) with their children populated
     */
    public List<DependencyInfo> buildTree(List<DependencyInfo> dependencies, boolean mergeDuplicates) {
        if (dependencies == null || dependencies.isEmpty()) {
            return new ArrayList<>();
        }

        // Track seen dependencies for merge duplicates
        Set<String> seenDependencies = new HashSet<>();

        // Stack to track current path in the tree
        List<DependencyInfo> path = new ArrayList<>();

        // List of root dependencies
        List<DependencyInfo> roots = new ArrayList<>();

        for (DependencyInfo dep : dependencies) {
            // Initialize children list
            dep.setChildren(new ArrayList<>());

            int depth = dep.getDepth();

            // Adjust path to current depth
            while (path.size() > depth) {
                path.remove(path.size() - 1);
            }

            String depId = dep.getDependencyId();

            if (depth == 0) {
                // Root dependency
                if (!mergeDuplicates || !seenDependencies.contains(depId)) {
                    roots.add(dep);
                    path.add(dep);
                    seenDependencies.add(depId);
                } else {
                    // Even if we skip adding this root, we need to update the path
                    // to handle children of subsequent roots correctly
                    path.clear();
                }
            } else {
                // Find parent at depth - 1
                if (!path.isEmpty() && path.size() > depth - 1) {
                    DependencyInfo parent = path.get(depth - 1);
                    if (!mergeDuplicates || !seenDependencies.contains(depId)) {
                        parent.getChildren().add(dep);
                        dep.setParentDependencyId(parent.getDependencyId());
                        path.add(dep);
                        seenDependencies.add(depId);
                    }
                }
            }
        }

        return roots;
    }
}
