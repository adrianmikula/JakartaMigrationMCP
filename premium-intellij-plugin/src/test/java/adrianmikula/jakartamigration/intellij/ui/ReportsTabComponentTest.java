package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.service.AdvancedScanningService;
import adrianmikula.jakartamigration.intellij.service.MigrationAnalysisService;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.swing.JPanel;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ReportsTabComponent.
 *
 * NOTE: These tests require full IntelliJ Platform environment.
 */
@org.junit.jupiter.api.Disabled("Requires full IntelliJ Platform environment - run in IDE")
public class ReportsTabComponentTest extends BasePlatformTestCase {
    
    @Mock
    private AdvancedScanningService mockAdvancedScanningService;
    
    @Mock
    private MigrationAnalysisService mockMigrationAnalysisService;
    
    private ReportsTabComponent reportsTabComponent;
    
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.openMocks(this);
        reportsTabComponent = new ReportsTabComponent(getProject(), mockMigrationAnalysisService, mockAdvancedScanningService);
    }
    
    @Test
    public void testGetPanel() {
        // Act
        JPanel panel = reportsTabComponent.getPanel();
        
        // Assert
        assertNotNull(panel);
        assertTrue(panel.isVisible());
    }
    
    @Test
    public void testRefresh() {
        // Act & Assert - Should not throw any exceptions
        assertDoesNotThrow(() -> reportsTabComponent.refresh());
    }
    
    @Test
    public void testComponentInitialization() {
        // Assert
        assertNotNull(reportsTabComponent);
        assertNotNull(reportsTabComponent.getPanel());
    }
}
