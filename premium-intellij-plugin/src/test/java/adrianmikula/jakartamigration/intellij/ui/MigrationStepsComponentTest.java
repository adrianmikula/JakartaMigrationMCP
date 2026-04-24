package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class MigrationStepsComponentTest extends BasePlatformTestCase {

    private MigrationStepsComponent migrationStepsComponent;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        migrationStepsComponent = new MigrationStepsComponent(getProject());
    }

    @Test
    public void testUpdateStepsWithNullMigrationStatus_ShouldNotThrowNPE() {
        // Given: A list of dependencies with null migration status
        List<DependencyInfo> dependencies = new ArrayList<>();
        
        // Add dependency with null migration status (this was causing NPE)
        DependencyInfo nullStatusDep = new DependencyInfo(
            "javax.servlet", "javax.servlet-api", "4.0.1",
            "jakarta.servlet", "jakarta.servlet-api", "6.0.0",
            "INCOMPATIBLE", null, null, false, false
        );
        dependencies.add(nullStatusDep);
        
        // Add dependency with valid migration status
        DependencyInfo validStatusDep = new DependencyInfo(
            "javax.persistence", "javax.persistence-api", "2.2",
            "jakarta.persistence", "jakarta.persistence-api", "3.1.0",
            "INCOMPATIBLE", "jakarta-persistence-recipe", DependencyMigrationStatus.NEEDS_UPGRADE, false, false
        );
        dependencies.add(validStatusDep);

        // When: Update steps with dependencies containing null migration status
        // Then: Should not throw NPE
        try {
            migrationStepsComponent.setDependencies(dependencies);
            migrationStepsComponent.updateSteps();
            
            // Verify the component handled null status gracefully
            assertThat(migrationStepsComponent).isNotNull();
            
        } catch (NullPointerException e) {
            fail("MigrationStepsComponent should handle null migration status without throwing NPE: " + e.getMessage());
        }
    }

    @Test
    public void testUpdateStepsWithMixedNullAndValidStatuses_ShouldHandleGracefully() {
        // Given: A mix of dependencies with null and valid migration statuses
        List<DependencyInfo> dependencies = new ArrayList<>();
        
        // Add multiple dependencies with null status
        for (int i = 0; i < 3; i++) {
            DependencyInfo nullDep = new DependencyInfo(
                "javax.test" + i, "test-api" + i, "1.0." + i,
                "jakarta.test" + i, "test-api" + i, "2.0." + i,
                "INCOMPATIBLE", null, null, false, false
            );
            dependencies.add(nullDep);
        }
        
        // Add dependencies with valid statuses
        DependencyInfo needsUpgradeDep = new DependencyInfo(
            "javax.annotation", "javax.annotation-api", "1.3.2",
            "jakarta.annotation", "jakarta.annotation-api", "2.1.1",
            "INCOMPATIBLE", "jakarta-annotation-recipe", DependencyMigrationStatus.NEEDS_UPGRADE, false, false
        );
        dependencies.add(needsUpgradeDep);
        
        DependencyInfo compatibleDep = new DependencyInfo(
            "org.springframework", "spring-core", "6.0.0",
            null, null, null,
            "COMPATIBLE", null, DependencyMigrationStatus.COMPATIBLE, false, false
        );
        dependencies.add(compatibleDep);

        // When: Update steps with mixed null and valid statuses
        // Then: Should handle gracefully without NPE
        try {
            migrationStepsComponent.setDependencies(dependencies);
            migrationStepsComponent.updateSteps();
            
            assertThat(migrationStepsComponent).isNotNull();
            
        } catch (NullPointerException e) {
            fail("MigrationStepsComponent should handle mixed null/valid migration statuses without throwing NPE: " + e.getMessage());
        }
    }

    @Test
    public void testUpdateStepsWithEmptyDependenciesList_ShouldHandleGracefully() {
        // Given: Empty dependencies list
        List<DependencyInfo> dependencies = new ArrayList<>();

        // When: Update steps with empty list
        // Then: Should handle gracefully
        try {
            migrationStepsComponent.setDependencies(dependencies);
            migrationStepsComponent.updateSteps();
            
            assertThat(migrationStepsComponent).isNotNull();
            
        } catch (NullPointerException e) {
            fail("MigrationStepsComponent should handle empty dependencies list without throwing NPE: " + e.getMessage());
        }
    }

    @Test
    public void testUpdateStepsWithNullDependenciesList_ShouldHandleGracefully() {
        // When: Update steps with null dependencies list
        // Then: Should handle gracefully
        try {
            migrationStepsComponent.setDependencies(null);
            migrationStepsComponent.updateSteps();
            
            assertThat(migrationStepsComponent).isNotNull();
            
        } catch (NullPointerException e) {
            fail("MigrationStepsComponent should handle null dependencies list without throwing NPE: " + e.getMessage());
        }
    }
}
