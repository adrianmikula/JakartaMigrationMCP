package adrianmikula.jakartamigration.intellij;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/**
 * Test suite for all Jakarta Migration IntelliJ Plugin tests
 * Validates TypeSpec compliance, UI functionality, and MCP integration
 * 
 * Integration tests that require MCP server can be skipped by setting:
 * SKIP_MCP_INTEGRATION_TESTS=true
 * 
 * To run integration tests with a running MCP server:
 * 1. Start MCP server: ./gradlew :mcp-server:run
 * 2. Run tests: ./gradlew :intellij-plugin:test
 */
@Suite
@SelectPackages({
    "adrianmikula.jakartamigration.intellij.model",
    "adrianmikula.jakartamigration.intellij.ui",
    "adrianmikula.jakartamigration.intellij.mcp"
})
public class JakartaMigrationPluginTestSuite {
    // Test suite configuration
}
