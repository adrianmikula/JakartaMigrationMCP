package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyUsage;
import adrianmikula.jakartamigration.advancedscanning.service.DependencyDeduplicationService;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DependencyDeduplicationServiceImplTest {

    private final DependencyDeduplicationService service = new DependencyDeduplicationServiceImpl();

    @Test
    void deduplicate_shouldReturnEmptyListForNullInput() {
        List<TransitiveDependencyUsage> result = service.deduplicate(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void deduplicate_shouldReturnEmptyListForEmptyInput() {
        List<TransitiveDependencyUsage> result = service.deduplicate(Collections.emptyList());
        assertTrue(result.isEmpty());
    }

    @Test
    void deduplicate_shouldKeepSingleDependency() {
        TransitiveDependencyUsage dep = new TransitiveDependencyUsage(
                "jaxb-api", "javax.xml.bind", "2.3.1",
                "javax.xml.bind:jaxb-api", "high", "Replace with Jakarta",
                "compile", false, 0
        );

        List<TransitiveDependencyUsage> result = service.deduplicate(Collections.singletonList(dep));

        assertEquals(1, result.size());
        assertEquals("jaxb-api", result.get(0).getArtifactId());
        assertEquals("2.3.1", result.get(0).getVersion());
        assertTrue(result.get(0).getAlternativeVersions().isEmpty());
    }

    @Test
    void deduplicate_shouldMergeDuplicateArtifactsWithDifferentVersions() {
        TransitiveDependencyUsage dep1 = new TransitiveDependencyUsage(
                "jaxb-api", "javax.xml.bind", "2.3.1",
                "javax.xml.bind:jaxb-api", "high", "Replace with Jakarta",
                "compile", false, 0
        );
        TransitiveDependencyUsage dep2 = new TransitiveDependencyUsage(
                "jaxb-api", "javax.xml.bind", "2.4.0",
                "javax.xml.bind:jaxb-api", "high", "Replace with Jakarta",
                "test", true, 1
        );

        List<TransitiveDependencyUsage> result = service.deduplicate(Arrays.asList(dep1, dep2));

        assertEquals(1, result.size());
        TransitiveDependencyUsage merged = result.get(0);
        assertEquals("jaxb-api", merged.getArtifactId());
        assertEquals(1, merged.getAlternativeVersions().size());
        assertTrue(merged.getAlternativeVersions().contains("2.4.0"));
    }

    @Test
    void deduplicate_shouldPreserveMinDepth() {
        TransitiveDependencyUsage dep1 = new TransitiveDependencyUsage(
                "jaxb-api", "javax.xml.bind", "2.3.1",
                "javax.xml.bind:jaxb-api", "high", "Replace with Jakarta",
                "compile", true, 2
        );
        TransitiveDependencyUsage dep2 = new TransitiveDependencyUsage(
                "jaxb-api", "javax.xml.bind", "2.4.0",
                "javax.xml.bind:jaxb-api", "high", "Replace with Jakarta",
                "test", false, 0
        );

        List<TransitiveDependencyUsage> result = service.deduplicate(Arrays.asList(dep1, dep2));

        assertEquals(0, result.get(0).getDepth());
        assertFalse(result.get(0).isTransitive());
    }

    @Test
    void deduplicate_shouldMergeMultipleVersions() {
        TransitiveDependencyUsage dep1 = new TransitiveDependencyUsage(
                "jaxb-api", "javax.xml.bind", "2.3.1",
                "javax.xml.bind:jaxb-api", "high", "Replace with Jakarta",
                "compile", false, 0
        );
        TransitiveDependencyUsage dep2 = new TransitiveDependencyUsage(
                "jaxb-api", "javax.xml.bind", "2.4.0",
                "javax.xml.bind:jaxb-api", "high", "Replace with Jakarta",
                "test", true, 1
        );
        TransitiveDependencyUsage dep3 = new TransitiveDependencyUsage(
                "jaxb-api", "javax.xml.bind", "2.5.0",
                "javax.xml.bind:jaxb-api", "high", "Replace with Jakarta",
                "runtime", true, 2
        );

        List<TransitiveDependencyUsage> result = service.deduplicate(Arrays.asList(dep1, dep2, dep3));

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getAlternativeVersions().size());
    }

    @Test
    void deduplicate_shouldKeepDifferentArtifactsSeparate() {
        TransitiveDependencyUsage dep1 = new TransitiveDependencyUsage(
                "jaxb-api", "javax.xml.bind", "2.3.1",
                "javax.xml.bind:jaxb-api", "high", "Replace with Jakarta",
                "compile", false, 0
        );
        TransitiveDependencyUsage dep2 = new TransitiveDependencyUsage(
                "jms-api", "javax.jms", "2.0.1",
                "javax.jms:jms-api", "high", "Replace with Jakarta",
                "compile", false, 0
        );

        List<TransitiveDependencyUsage> result = service.deduplicate(Arrays.asList(dep1, dep2));

        assertEquals(2, result.size());
    }

    @Test
    void deduplicate_shouldMergeWithCorrectMinDepth() {
        // Same artifact at depths 0, 1, 2 - result should have depth 0
        TransitiveDependencyUsage dep1 = new TransitiveDependencyUsage(
                "jaxb-api", "javax.xml.bind", "2.3.1",
                "javax.xml.bind:jaxb-api", "high", "Replace with Jakarta",
                "compile", true, 2  // Depth 2
        );
        TransitiveDependencyUsage dep2 = new TransitiveDependencyUsage(
                "jaxb-api", "javax.xml.bind", "2.4.0",
                "javax.xml.bind:jaxb-api", "high", "Replace with Jakarta",
                "test", true, 1  // Depth 1
        );
        TransitiveDependencyUsage dep3 = new TransitiveDependencyUsage(
                "jaxb-api", "javax.xml.bind", "2.5.0",
                "javax.xml.bind:jaxb-api", "high", "Replace with Jakarta",
                "runtime", false, 0  // Depth 0 (direct)
        );

        List<TransitiveDependencyUsage> result = service.deduplicate(Arrays.asList(dep1, dep2, dep3));

        assertEquals(1, result.size());
        // Should preserve minimum depth (0 for direct dependency)
        assertEquals(0, result.get(0).getDepth());
        assertFalse(result.get(0).isTransitive());
    }

    @Test
    void deduplicate_shouldTrackMultipleScopes() {
        TransitiveDependencyUsage dep1 = new TransitiveDependencyUsage(
                "jaxb-api", "javax.xml.bind", "2.3.1",
                "javax.xml.bind:jaxb-api", "high", "Replace with Jakarta",
                "compile", false, 0
        );
        TransitiveDependencyUsage dep2 = new TransitiveDependencyUsage(
                "jaxb-api", "javax.xml.bind", "2.4.0",
                "javax.xml.bind:jaxb-api", "high", "Replace with Jakarta",
                "test", true, 1
        );
        TransitiveDependencyUsage dep3 = new TransitiveDependencyUsage(
                "jaxb-api", "javax.xml.bind", "2.5.0",
                "javax.xml.bind:jaxb-api", "high", "Replace with Jakarta",
                "runtime", true, 2
        );

        List<TransitiveDependencyUsage> result = service.deduplicate(Arrays.asList(dep1, dep2, dep3));

        assertEquals(1, result.size());
        TransitiveDependencyUsage merged = result.get(0);
        // Should preserve minimum depth
        assertEquals(0, merged.getDepth());
        // Should have 2 alternative versions
        assertEquals(2, merged.getAlternativeVersions().size());
        assertTrue(merged.getAlternativeVersions().contains("2.4.0"));
        assertTrue(merged.getAlternativeVersions().contains("2.5.0"));
    }

    @Test
    void deduplicate_shouldHandleTransitiveAndDirectMixed() {
        // Mix of transitive and direct dependencies
        TransitiveDependencyUsage direct1 = new TransitiveDependencyUsage(
                "servlet-api", "javax.servlet", "4.0.1",
                "javax.servlet:servlet-api", "high", "Replace with Jakarta",
                "provided", false, 0
        );
        TransitiveDependencyUsage transitive1 = new TransitiveDependencyUsage(
                "jaxb-api", "javax.xml.bind", "2.3.1",
                "javax.xml.bind:jaxb-api", "high", "Replace with Jakarta",
                "compile", true, 1
        );
        TransitiveDependencyUsage direct2 = new TransitiveDependencyUsage(
                "jms-api", "javax.jms", "2.0.1",
                "javax.jms:jms-api", "high", "Replace with Jakarta",
                "compile", false, 0
        );
        TransitiveDependencyUsage transitive2 = new TransitiveDependencyUsage(
                "jms-api", "javax.jms", "2.0.2",
                "javax.jms:jms-api", "high", "Replace with Jakarta",
                "runtime", true, 2
        );

        List<TransitiveDependencyUsage> result = service.deduplicate(Arrays.asList(direct1, transitive1, direct2, transitive2));

        // 3 unique artifacts: servlet-api, jaxb-api, and jms-api (merged from 2 entries)
        assertEquals(3, result.size());

        // Find servlet-api result
        TransitiveDependencyUsage servletResult = result.stream()
                .filter(u -> u.getArtifactId().equals("servlet-api"))
                .findFirst()
                .orElseThrow();
        assertEquals(0, servletResult.getDepth());
        assertFalse(servletResult.isTransitive());

        // Find jaxb-api result
        TransitiveDependencyUsage jaxbResult = result.stream()
                .filter(u -> u.getArtifactId().equals("jaxb-api"))
                .findFirst()
                .orElseThrow();
        assertEquals(1, jaxbResult.getDepth());
        assertTrue(jaxbResult.isTransitive());

        // Find jms-api result (merged - should be direct since depth 0 exists)
        TransitiveDependencyUsage jmsResult = result.stream()
                .filter(u -> u.getArtifactId().equals("jms-api"))
                .findFirst()
                .orElseThrow();
        assertEquals(0, jmsResult.getDepth());
        assertFalse(jmsResult.isTransitive());
        assertEquals(1, jmsResult.getAlternativeVersions().size());
    }
}
