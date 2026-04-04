package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.config.FeatureFlags;
import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.swing.JPanel;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test for SupportComponent initialization to prevent NullPointerException
 */
@ExtendWith(MockitoExtension.class)
public class SupportComponentInitializationTest {

    private MockedStatic<FeatureFlags> mockFeatureFlags;
    private Project project;

    @BeforeEach
    void setUp() {
        mockFeatureFlags = Mockito.mockStatic(FeatureFlags.class);
        project = Mockito.mock(Project.class);
    }

    @Test
    @DisplayName("SupportComponent should initialize without NullPointerException")
    void testSupportComponentInitialization() {
        // Mock feature flags
        FeatureFlags mockInstance = Mockito.mock(FeatureFlags.class);
        mockFeatureFlags.when(FeatureFlags::getInstance).thenReturn(mockInstance);
        when(mockInstance.isExperimentalFeaturesEnabled()).thenReturn(false);

        // This should not throw NullPointerException
        assertDoesNotThrow(() -> {
            SupportComponent supportComponent = new SupportComponent(project, null);
            JPanel panel = supportComponent.getPanel();
            
            // Verify the panel is not null and has components
            assertNotNull(panel);
            assertTrue(panel.getComponentCount() > 0);
            
            // At least some panels should be present
            assertTrue(panel.getComponentCount() >= 1, "SupportComponent should have at least one panel");
        });
    }

    @Test
    @DisplayName("SupportComponent should handle experimental features checkbox initialization")
    void testExperimentalFeaturesCheckboxInitialization() {
        // Mock feature flags to return true
        FeatureFlags mockInstance = Mockito.mock(FeatureFlags.class);
        mockFeatureFlags.when(FeatureFlags::getInstance).thenReturn(mockInstance);
        when(mockInstance.isExperimentalFeaturesEnabled()).thenReturn(true);

        // This should not throw NullPointerException
        assertDoesNotThrow(() -> {
            SupportComponent supportComponent = new SupportComponent(project, null);
            JPanel panel = supportComponent.getPanel();
            
            assertNotNull(panel);
            // The component should be created without errors
            assertTrue(panel.getComponentCount() > 0);
        });
    }
}
