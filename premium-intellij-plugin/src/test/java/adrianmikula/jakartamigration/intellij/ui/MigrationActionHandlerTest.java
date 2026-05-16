package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import static org.mockito.Mockito.*;

import java.util.function.BiConsumer;

/**
 * Unit tests for MigrationActionHandler to ensure it handles null values gracefully.
 * Tests the fix for NullPointerException when currentVersion is null.
 */
public class MigrationActionHandlerTest extends BasePlatformTestCase {

    private MigrationActionHandler actionHandler;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        actionHandler = new MigrationActionHandler(getProject());
    }

    /**
     * Test that handleBinaryScanAction handles dependencies with null currentVersion gracefully.
     * This is a regression test for the NullPointerException that occurred when
     * Artifact constructor received a null version parameter.
     */
    public void testHandleBinaryScanWithNullCurrentVersion() throws Exception {
        // Create a dependency with null currentVersion
        DependencyInfo depWithNullVersion = new DependencyInfo(
            "org.example", "test-lib", null,
            null, null, null,
            "Unknown", null,
            DependencyMigrationStatus.UNKNOWN, false, false);

        SubtaskTableComponent.SubtaskItem subtask = 
            new SubtaskTableComponent.SubtaskItem("Test Task", "Test description", depWithNullVersion, "binary-scan");

        // Mock callback
        @SuppressWarnings("unchecked")
        BiConsumer<SubtaskTableComponent.SubtaskItem, String> callback = mock(BiConsumer.class);

        // This should not throw NullPointerException
        actionHandler.handleBinaryScanAction(subtask, callback);

        // Wait a bit for the async operation to complete
        Thread.sleep(100);

        // Verify the callback was called (no exception was thrown)
        verify(callback, atLeastOnce()).accept(any(), any());
    }

    /**
     * Test that handleBinaryScanAction handles dependencies with valid currentVersion.
     */
    public void testHandleBinaryScanWithValidCurrentVersion() throws Exception {
        // Create a dependency with valid version
        DependencyInfo depWithValidVersion = new DependencyInfo(
            "org.example", "test-lib", "1.0.0",
            null, null, null,
            "Compatible", null,
            DependencyMigrationStatus.COMPATIBLE, false, false);

        SubtaskTableComponent.SubtaskItem subtask = 
            new SubtaskTableComponent.SubtaskItem("Test Task", "Test description", depWithValidVersion, "binary-scan");

        // Mock callback
        @SuppressWarnings("unchecked")
        BiConsumer<SubtaskTableComponent.SubtaskItem, String> callback = mock(BiConsumer.class);

        // This should not throw NullPointerException
        actionHandler.handleBinaryScanAction(subtask, callback);

        // Wait a bit for the async operation to complete
        Thread.sleep(100);

        // Verify the callback was called
        verify(callback, atLeastOnce()).accept(any(), any());
    }

    /**
     * Test that handleBinaryScanAction handles null dependency gracefully.
     */
    public void testHandleBinaryScanWithNullDependency() {
        SubtaskTableComponent.SubtaskItem subtask = 
            new SubtaskTableComponent.SubtaskItem("Test Task", "Test description", null, "binary-scan");

        // Mock callback
        @SuppressWarnings("unchecked")
        BiConsumer<SubtaskTableComponent.SubtaskItem, String> callback = mock(BiConsumer.class);

        // This should handle null dependency gracefully
        actionHandler.handleBinaryScanAction(subtask, callback);

        // Verify the callback was called with error message
        verify(callback).accept(eq(subtask), contains("No dependency specified"));
    }
}
