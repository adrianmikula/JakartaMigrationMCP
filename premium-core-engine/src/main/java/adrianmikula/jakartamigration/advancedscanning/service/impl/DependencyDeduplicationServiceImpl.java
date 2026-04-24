package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyUsage;
import adrianmikula.jakartamigration.advancedscanning.service.DependencyDeduplicationService;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of dependency deduplication that merges duplicates with multi-version tracking.
 */
@Slf4j
public class DependencyDeduplicationServiceImpl implements DependencyDeduplicationService {

    @Override
    public List<TransitiveDependencyUsage> deduplicate(List<TransitiveDependencyUsage> dependencies) {
        if (dependencies == null || dependencies.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, MergedDependency> mergeMap = new LinkedHashMap<>();

        for (TransitiveDependencyUsage dep : dependencies) {
            if (dep == null) {
                continue;
            }

            String key = dep.getArtifactKey();
            MergedDependency merged = mergeMap.get(key);

            if (merged == null) {
                mergeMap.put(key, new MergedDependency(dep));
            } else {
                MergedDependency updated = merged.merge(dep);
                mergeMap.put(key, updated);
            }
        }

        List<TransitiveDependencyUsage> result = new ArrayList<>(mergeMap.size());
        for (MergedDependency merged : mergeMap.values()) {
            result.add(merged.toUsage());
        }

        log.debug("Deduplicated {} dependencies to {} unique artifacts",
                dependencies.size(), result.size());

        return result;
    }

    private record MergedDependency(String artifactId, String groupId, String javaxPackage,
                                       String severity, String recommendation, String primaryVersion,
                                       Set<String> alternativeVersions, Set<String> scopes, int minDepth, boolean transitive) {
        MergedDependency(TransitiveDependencyUsage d) {
            this(d.getArtifactId(), d.getGroupId(), d.getJavaxPackage(), d.getSeverity(), d.getRecommendation(),
                 d.getVersion(), new HashSet<>(), new HashSet<>(), d.getDepth(), d.isTransitive());
            if (d.getVersion() != null) alternativeVersions.add(d.getVersion());
            if (d.getScope() != null) scopes.add(d.getScope());
        }

        MergedDependency merge(TransitiveDependencyUsage d) {
            if (d.getVersion() != null && !d.getVersion().equals(primaryVersion)) alternativeVersions.add(d.getVersion());
            if (d.getScope() != null) scopes.add(d.getScope());
            return new MergedDependency(artifactId, groupId, javaxPackage, severity, recommendation, primaryVersion,
                alternativeVersions, scopes, Math.min(minDepth, d.getDepth()), transitive && d.isTransitive());
        }

        TransitiveDependencyUsage toUsage() {
            List<String> alt = new ArrayList<>(alternativeVersions);
            alt.remove(primaryVersion);
            Collections.sort(alt);
            return new TransitiveDependencyUsage(artifactId, groupId, primaryVersion, javaxPackage, severity,
                recommendation, scopes.isEmpty() ? null : scopes.iterator().next(), transitive, minDepth, alt);
        }
    }
}
