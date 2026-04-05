package adrianmikula.jakartamigration.intellij.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * Test suite for all UI preventive tests.
 * Runs comprehensive tests to prevent runtime errors before they reach users.
 */
@Suite
@SelectClasses({
    UIPreventiveTests.class,
    UIIntegrationTests.class
})
@DisplayName("UI Preventive Tests Suite")
public class UIPreventiveTestSuite {
    // This class serves as a test suite organizer
    // All actual tests are in the selected classes
}
