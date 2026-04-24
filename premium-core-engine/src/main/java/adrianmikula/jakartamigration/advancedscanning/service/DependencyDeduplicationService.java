package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyUsage;

import java.util.List;

/**
 * Deduplicates transitive dependencies by grouping artifacts with the same groupId:artifactId.
 * When multiple versions of the same artifact are found, they are merged into a single result
 * with alternative versions tracked.
 */
public interface DependencyDeduplicationService {

    /**
     * Deduplicates a list of dependencies by artifact key (groupId:artifactId).
     * When the same artifact appears with different versions, they are merged into
     * a single record with alternativeVersions populated.
     *
     * @param dependencies The list of dependencies to deduplicate
     * @return A deduplicated list where each artifact key appears only once
     */
    List<TransitiveDependencyUsage> deduplicate(List<TransitiveDependencyUsage> dependencies);
}
