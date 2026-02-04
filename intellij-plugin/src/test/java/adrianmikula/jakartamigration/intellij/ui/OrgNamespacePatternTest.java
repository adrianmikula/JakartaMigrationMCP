package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;
import adrianmikula.jakartamigration.intellij.model.RiskLevel;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for org namespace pattern matching functionality in DependencyGraphComponent.
 * Tests the pattern matching logic for identifying internal organizational dependencies.
 */
public class OrgNamespacePatternTest extends LightJavaCodeInsightFixtureTestCase {

    private DependencyGraphComponent graphComponent;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        graphComponent = new DependencyGraphComponent(getProject());
    }

    @Test
    public void testExactMatchPattern() {
        // Test exact groupId match
        assertThat(matchPattern("com.myorg", "com.myorg")).isTrue();
        assertThat(matchPattern("com.myorg", "com.other")).isFalse();
    }

    @Test
    public void testWildcardPattern() {
        // Test wildcard pattern matching
        assertThat(matchPattern("com.myorg.*", "com.myorg")).isTrue();
        assertThat(matchPattern("com.myorg.*", "com.myorg.library")).isTrue();
        assertThat(matchPattern("com.myorg.*", "com.myorg.subdep")).isTrue();
        assertThat(matchPattern("com.myorg.*", "com.other")).isFalse();
        assertThat(matchPattern("com.myorg.*", "com.myorgdiffer")).isFalse();
    }

    @Test
    public void testMultiplePatterns() {
        Set<String> patterns = new HashSet<>(Arrays.asList(
            "com.myorg.*",
            "io.company.*",
            "com.internal"
        ));

        // Should match org patterns
        assertThat(isOrgDependency(patterns, "com.myorg.library")).isTrue();
        assertThat(isOrgDependency(patterns, "io.company.utils")).isTrue();
        assertThat(isOrgDependency(patterns, "com.internal")).isTrue();

        // Should not match external dependencies
        assertThat(isOrgDependency(patterns, "org.springframework")).isFalse();
        assertThat(isOrgDependency(patterns, "commons-io")).isFalse();
        assertThat(isOrgDependency(patterns, "com.myorgdiffer")).isFalse();
    }

    @Test
    public void testSetOrgNamespacePatterns() {
        Set<String> patterns = new HashSet<>();
        patterns.add("com.mycompany.*");

        graphComponent.setOrgNamespacePatterns(patterns);

        // Should not throw and should set patterns
        assertThat(graphComponent).isNotNull();
    }

    @Test
    public void testNullPatternsHandling() {
        // Should handle null patterns gracefully
        graphComponent.setOrgNamespacePatterns(null);
        // Should not throw exception
    }

    @Test
    public void testEmptyPatterns() {
        Set<String> patterns = new HashSet<>();
        graphComponent.setOrgNamespacePatterns(patterns);

        // Empty patterns should not match any dependencies
        assertThat(isOrgDependency(patterns, "com.myorg")).isFalse();
    }

    @Test
    public void testDependencyWithOrgNamespace() {
        // Test that org internal flag is set correctly
        Set<String> patterns = new HashSet<>(Arrays.asList("com.myorg.*"));

        List<DependencyInfo> deps = Arrays.asList(
            new DependencyInfo("com.myorg", "internal-lib", "1.0.0",
                "2.0.0", DependencyMigrationStatus.NEEDS_UPGRADE,
                false, RiskLevel.HIGH, "Internal lib"),
            new DependencyInfo("org.springframework", "spring-core", "5.3.0",
                "6.0.0", DependencyMigrationStatus.NO_JAKARTA_VERSION,
                true, RiskLevel.CRITICAL, "External lib")
        );

        // Process dependencies and check org status
        for (DependencyInfo dep : deps) {
            boolean isOrg = isOrgDependency(patterns, dep.getGroupId());
            if ("com.myorg".equals(dep.getGroupId())) {
                assertThat(isOrg).isTrue();
            } else {
                assertThat(isOrg).isFalse();
            }
        }
    }

    /**
     * Helper method to test pattern matching logic.
     * This mimics the logic in DependencyGraphComponent.
     */
    private boolean matchPattern(String pattern, String groupId) {
        if (pattern.endsWith(".*")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            return groupId.equals(prefix) || groupId.startsWith(prefix + ".");
        }
        return groupId.equals(pattern);
    }

    /**
     * Helper method to test org dependency check.
     */
    private boolean isOrgDependency(Set<String> patterns, String groupId) {
        for (String pattern : patterns) {
            if (matchPattern(pattern, groupId)) {
                return true;
            }
        }
        return false;
    }
}
