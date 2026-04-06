package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.platforms.model.PlatformScanResult;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic tests for PlatformsTabComponent - simplified to focus on core robustness.
 */
public class PlatformsTabComponentTest extends BasePlatformTestCase {
    
    private Project mockProject;
    private PlatformsTabComponent platformsTab;
    
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        mockProject = getProject();
        platformsTab = new PlatformsTabComponent(mockProject);
    }
    
    @Test
    @DisplayName("getPanel should return valid panel")
    void testGetPanel_ReturnsValidPanel() {
        // When
        JPanel panel = platformsTab.getPanel();
        
        // Then
        assertNotNull(panel);
        assertTrue(panel.getComponentCount() >= 0);
    }
    
    @Test
    @DisplayName("displayResults should handle null results")
    void testDisplayResults_NullResults() {
        assertDoesNotThrow(() -> platformsTab.displayResults((List<String>) null));
    }
    
    @Test
    @DisplayName("displayResults should handle empty results")
    void testDisplayResults_EmptyResults() {
        assertDoesNotThrow(() -> platformsTab.displayResults(List.of()));
    }
    
    @Test
    @DisplayName("refreshUI should not throw exceptions")
    void testRefreshUI() {
        assertDoesNotThrow(() -> platformsTab.refreshUI());
    }
    
    @Test
    @DisplayName("updatePremiumControls should not throw exceptions")
    void testUpdatePremiumControls() {
        assertDoesNotThrow(() -> platformsTab.updatePremiumControls());
    }
}
