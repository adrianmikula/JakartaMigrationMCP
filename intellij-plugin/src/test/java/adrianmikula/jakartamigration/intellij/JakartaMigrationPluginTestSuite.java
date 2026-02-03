package adrianmikula.jakartamigration.intellij;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/**
 * Test suite for all Jakarta Migration IntelliJ Plugin tests
 * Validates TypeSpec compliance and UI functionality
 */
@Suite
@SelectPackages({
    "adrianmikula.jakartamigration.intellij.model",
    "adrianmikula.jakartamigration.intellij.ui"
})
public class JakartaMigrationPluginTestSuite {
    // Test suite configuration
}