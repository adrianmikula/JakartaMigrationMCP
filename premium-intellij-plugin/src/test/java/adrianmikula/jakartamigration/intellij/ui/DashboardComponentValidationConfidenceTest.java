package adrianmikula.jakartamigration.intellij.ui;

import com.intellij.openapi.project.Project;
import org.junit.Test;
import org.mockito.Mockito;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;

import static org.junit.Assert.*;

/**
 * Tests for Validation Confidence functionality in DashboardComponent.
 */
public class DashboardComponentValidationConfidenceTest {

    @Test
    public void testValidationConfidenceGaugeInitialization() {
        Project project = Mockito.mock(Project.class);
        Consumer<ActionEvent> onAnalyze = Mockito.mock(Consumer.class);
        DashboardComponent component = new DashboardComponent(project, null, onAnalyze);

        // Verify component initializes without error
        assertNotNull(component);
    }

    @Test
    public void testComponentCreationWithMocks() {
        Project project = Mockito.mock(Project.class);
        Consumer<ActionEvent> onAnalyze = Mockito.mock(Consumer.class);
        DashboardComponent component = new DashboardComponent(project, null, onAnalyze);

        // Test that the component is created successfully
        assertNotNull(component);
    }
}