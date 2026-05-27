package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory class for creating mock DependencyInfo objects for testing.
 */
public class DependencyInfoTestFactory {

    /**
     * Create a single dependency info
     */
    public static DependencyInfo createSingleDependency() {
        return new DependencyInfo(
            "org.example",
            "dep1",
            "1.0.0",
            null,
            null,
            null,
            "Compatible",
            null,
            DependencyMigrationStatus.COMPATIBLE,
            false,
            false
        );
    }

    /**
     * Create a list with single dependency
     */
    public static List<DependencyInfo> createSingleDependencyList() {
        List<DependencyInfo> deps = new ArrayList<>();
        deps.add(createSingleDependency());
        return deps;
    }

    /**
     * Create a list with multiple dependencies
     */
    public static List<DependencyInfo> createMultipleDependenciesList() {
        List<DependencyInfo> deps = new ArrayList<>();
        
        deps.add(new DependencyInfo(
            "org.example",
            "dep1",
            "1.0.0",
            null,
            null,
            null,
            "Compatible",
            null,
            DependencyMigrationStatus.COMPATIBLE,
            false,
            false
        ));
        
        deps.add(new DependencyInfo(
            "org.example",
            "dep2",
            "2.0.0",
            null,
            null,
            null,
            "Needs Upgrade",
            null,
            DependencyMigrationStatus.NEEDS_UPGRADE,
            false,
            false
        ));
        
        deps.add(new DependencyInfo(
            "org.example",
            "dep3",
            "3.0.0",
            null,
            null,
            null,
            "No Jakarta Version",
            null,
            DependencyMigrationStatus.NO_JAKARTA_VERSION,
            true,
            false
        ));
        
        return deps;
    }

    /**
     * Create an empty list
     */
    public static List<DependencyInfo> createEmptyList() {
        return new ArrayList<>();
    }

    /**
     * Create a dependency with null version (edge case)
     */
    public static DependencyInfo createDependencyWithNullVersion() {
        return new DependencyInfo(
            "org.example",
            "dep1",
            null,
            null,
            null,
            null,
            "Unknown",
            null,
            DependencyMigrationStatus.UNKNOWN,
            false,
            false
        );
    }

    /**
     * Create a dependency with null groupId (edge case)
     */
    public static DependencyInfo createDependencyWithNullGroupId() {
        return new DependencyInfo(
            null,
            "dep1",
            "1.0.0",
            null,
            null,
            null,
            "Unknown",
            null,
            DependencyMigrationStatus.UNKNOWN,
            false,
            false
        );
    }

    /**
     * Create a dependency with null artifactId (edge case)
     */
    public static DependencyInfo createDependencyWithNullArtifactId() {
        return new DependencyInfo(
            "org.example",
            null,
            "1.0.0",
            null,
            null,
            null,
            "Unknown",
            null,
            DependencyMigrationStatus.UNKNOWN,
            false,
            false
        );
    }
}
