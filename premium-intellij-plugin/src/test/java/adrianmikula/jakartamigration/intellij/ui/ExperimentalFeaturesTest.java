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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for experimental features tab toggling functionality.
 * Tests the integration between SupportComponent, MigrationToolWindow, and tab visibility.
 */
@ExtendWith(MockitoExtension.class)
public class ExperimentalFeaturesTest {

    private MockedStatic<FeatureFlags> mockFeatureFlags;
    private FeatureFlags mockFeatureFlagsInstance;
    private SupportComponent supportComponent;
    private Project project;

    @BeforeEach
    void setUp() {
        mockFeatureFlagsInstance = Mockito.mock(FeatureFlags.class);
        mockFeatureFlags = Mockito.mockStatic(FeatureFlags.class);
        mockFeatureFlags.when(FeatureFlags::getInstance).thenReturn(mockFeatureFlagsInstance);
        
        project = Mockito.mock(Project.class);
        supportComponent = new SupportComponent(project, null);
    }

    @Test
    @DisplayName("Experimental features checkbox should initialize with current flag state")
    void testCheckboxInitialization() {
        // Mock feature flags to return false
        when(mockFeatureFlagsInstance.isExperimentalFeaturesEnabled()).thenReturn(false);
        
        supportComponent.initializeUI();
        
        // Verify checkbox state matches flag
        verify(supportComponent.experimentalFeaturesCheckbox).setSelected(false);
    }

    @Test
    @DisplayName("Enabling experimental features should update flag")
    void testEnablingFeatures() {
        when(mockFeatureFlagsInstance.isExperimentalFeaturesEnabled()).thenReturn(false);
        
        // Simulate checkbox selection
        supportComponent.experimentalFeaturesCheckbox.setSelected(true);
        supportComponent.experimentalFeaturesCheckbox.getActionListeners()[0].actionPerformed(null);
        
        // Verify flag was updated
        verify(mockFeatureFlagsInstance).setExperimentalFeaturesEnabled(true);
    }

    @Test
    @DisplayName("Disabling experimental features should update flag")
    void testDisablingFeatures() {
        when(mockFeatureFlagsInstance.isExperimentalFeaturesEnabled()).thenReturn(true);
        
        // Simulate checkbox deselection
        supportComponent.experimentalFeaturesCheckbox.setSelected(false);
        supportComponent.experimentalFeaturesCheckbox.getActionListeners()[0].actionPerformed(null);
        
        // Verify flag was updated
        verify(mockFeatureFlagsInstance).setExperimentalFeaturesEnabled(false);
    }

    @Test
    @DisplayName("SupportComponent should track premium status correctly")
    void testPremiumStatusTracking() {
        // Test initial state
        assertFalse(SupportComponent.isPremiumActive());
        
        // Test setting premium active
        SupportComponent.setPremiumActive(true);
        assertTrue(SupportComponent.isPremiumActive());
        
        // Test setting premium inactive
        SupportComponent.setPremiumActive(false);
        assertFalse(SupportComponent.isPremiumActive());
    }

    @Test
    @DisplayName("License status should be tracked correctly")
    void testLicenseStatusTracking() {
        // Test setting license status
        SupportComponent.setLicenseStatus("Active");
        // Note: We can't easily test the getter since it's private, but we can verify the setter works
        
        SupportComponent.setLicenseStatus("Expired");
        // Verify no exceptions are thrown
        assertDoesNotThrow(() -> SupportComponent.setLicenseStatus("Test Status"));
    }
}
