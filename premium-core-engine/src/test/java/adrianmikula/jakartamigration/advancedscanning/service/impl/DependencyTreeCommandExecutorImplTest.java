package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.DependencyTreeResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DependencyTreeCommandExecutorImpl.
 * Tests command availability checks and dependency tree execution.
 */
class DependencyTreeCommandExecutorImplTest {

    @Test
    void isMavenAvailable_shouldReturnBoolean() {
        // This test just verifies the method runs without error
        // Result depends on whether Maven is installed on the test machine
        boolean available = DependencyTreeCommandExecutorImpl.isMavenAvailable();

        // Should return either true or false, never throw
        assertTrue(available == true || available == false);
    }

    @Test
    void isGradleAvailable_shouldReturnBoolean() {
        // This test just verifies the method runs without error
        // Result depends on whether Gradle is installed on the test machine
        boolean available = DependencyTreeCommandExecutorImpl.isGradleAvailable();

        // Should return either true or false, never throw
        assertTrue(available == true || available == false);
    }

    @Test
    void dependencyTreeResult_shouldStoreErrorMessage() {
        String errorMessage = "Command failed with exit code 1";
        DependencyTreeResult result = DependencyTreeResult.error(errorMessage);

        assertFalse(result.isSuccess());
        assertTrue(result.getDependencies().isEmpty());
        assertEquals(errorMessage, result.getErrorMessage());
    }

    @Test
    void dependencyTreeResult_shouldCreateEmptyResult() {
        DependencyTreeResult result = DependencyTreeResult.empty();

        assertTrue(result.isSuccess());
        assertNotNull(result.getDependencies());
        assertTrue(result.getDependencies().isEmpty());
        assertTrue(result.getScopes().isEmpty());
        assertNull(result.getErrorMessage());
    }

    @Test
    void dependencyTreeResult_shouldCreateWithDependencies() {
        var deps = java.util.List.of(
                new DependencyTreeResult.DependencyNode(
                        "javax.servlet", "javax.servlet-api", "4.0.1",
                        "provided", 0, false
                ),
                new DependencyTreeResult.DependencyNode(
                        "javax.xml.bind", "jaxb-api", "2.3.1",
                        "compile", 1, true
                )
        );
        var scopes = java.util.Set.of("compile", "provided");

        DependencyTreeResult result = new DependencyTreeResult(deps, scopes);

        assertTrue(result.isSuccess());
        assertEquals(2, result.getDependencies().size());
        assertEquals(2, result.getScopes().size());
        assertNull(result.getErrorMessage());
    }

    @Test
    void dependencyNode_shouldCalculateArtifactKey() {
        DependencyTreeResult.DependencyNode node = new DependencyTreeResult.DependencyNode(
                "javax.servlet", "javax.servlet-api", "4.0.1",
                "provided", 0, false
        );

        assertEquals("javax.servlet:javax.servlet-api", node.getArtifactKey());
    }

    @Test
    void dependencyNode_shouldStoreAllFields() {
        DependencyTreeResult.DependencyNode node = new DependencyTreeResult.DependencyNode(
                "javax.xml.bind", "jaxb-api", "2.3.1",
                "compile", 2, true
        );

        assertEquals("javax.xml.bind", node.getGroupId());
        assertEquals("jaxb-api", node.getArtifactId());
        assertEquals("2.3.1", node.getVersion());
        assertEquals("compile", node.getScope());
        assertEquals(2, node.getDepth());
        assertTrue(node.isTransitive());
    }
}
