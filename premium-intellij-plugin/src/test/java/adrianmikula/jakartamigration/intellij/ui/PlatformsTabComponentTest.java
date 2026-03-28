package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.config.FeatureFlags;
import adrianmikula.jakartamigration.platforms.model.PlatformDetection;
import adrianmikula.jakartamigration.platforms.model.PlatformScanResult;

import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.swing.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PlatformsTabComponent
 */
public class PlatformsTabComponentTest extends BasePlatformTestCase {
    
    private PlatformsTabComponent platformsTab;
    private Project mockProject;
    
    @BeforeEach
    void setUp() throws Exception {
        mockProject = createMockProject();
        platformsTab = new PlatformsTabComponent(mockProject);
    }
    
    @AfterEach
    void tearDown() {
        if (platformsTab != null) {
            // Clean up any UI resources
        }
    }
    
    @Test
    void testGetPanel_ReturnsValidPanel() {
        // Given
        platformsTab = new PlatformsTabComponent(mockProject);
        
        // When
        JPanel panel = platformsTab.getPanel();
        
        // Then
        assertThat(panel).isNotNull();
        assertThat(panel.getComponentCount()).isGreaterThan(0);
    }
    
    @Test
    void testUpdatePremiumControls_NonPremiumUser_ShowsLockIcon() {
        // Given
        try (MockedStatic<FeatureFlags> mockedFlags = Mockito.mockStatic(FeatureFlags.class)) {
            mockedFlags.when(FeatureFlags::isPlatformsEnabled).thenReturn(false);
            
            platformsTab = new PlatformsTabComponent(mockProject);
            
            // When
            platformsTab.updatePremiumControls();
            
            // Then
            JPanel panel = platformsTab.getPanel();
            boolean hasLockIcon = containsComponentWithText(panel, "🔒 Premium Feature");
            boolean hasUpgradeButton = containsComponentWithText(panel, "Upgrade to Premium");
            
            assertThat(hasLockIcon).isTrue();
            assertThat(hasUpgradeButton).isTrue();
        }
    }
    
    @Test
    void testUpdatePremiumControls_PremiumUser_HidesLockIcon() {
        // Given
        try (MockedStatic<FeatureFlags> mockedFlags = Mockito.mockStatic(FeatureFlags.class)) {
            mockedFlags.when(FeatureFlags::isPlatformsEnabled).thenReturn(true);
            
            platformsTab = new PlatformsTabComponent(mockProject);
            
            // When
            platformsTab.updatePremiumControls();
            
            // Then
            JPanel panel = platformsTab.getPanel();
            boolean hasLockIcon = containsComponentWithText(panel, "🔒 Premium Feature");
            boolean hasUpgradeButton = containsComponentWithText(panel, "Upgrade to Premium");
            
            assertThat(hasLockIcon).isFalse();
            assertThat(hasUpgradeButton).isFalse();
        }
    }
    
    @Test
    void testDisplayResults_EmptyList_ShowsNoResultsMessage() {
        // Given
        platformsTab = new PlatformsTabComponent(mockProject);
        
        // When
        platformsTab.displayResults(List.of());
        
        // Then
        JPanel panel = platformsTab.getPanel();
        boolean hasNoResultsMessage = containsComponentWithText(panel, "No application servers detected");
        
        assertThat(hasNoResultsMessage).isTrue();
    }
    
    @Test
    void testDisplayResults_NonEmptyList_ShowsDetections() {
        // Given
        adrianmikula.jakartamigration.platforms.model.PlatformDetection detection = new adrianmikula.jakartamigration.platforms.model.PlatformDetection(
            "tomcat",
            "Apache Tomcat",
            "10.1.5",
            true,
            "10.0",
            java.util.Map.of("java", "11+", "jakarta", "9+")
        );
        
        platformsTab = new PlatformsTabComponent(mockProject);
        
        // When
        platformsTab.displayResults(List.of(detection));
        
        // Then
        JPanel panel = platformsTab.getPanel();
        boolean hasPlatformName = containsComponentWithText(panel, "Apache Tomcat");
        boolean hasVersion = containsComponentWithText(panel, "Version: 10.1.5");
        boolean hasCompatibleStatus = containsComponentWithText(panel, "✅ Jakarta EE Compatible");
        
        assertThat(hasPlatformName).isTrue();
        assertThat(hasVersion).isTrue();
        assertThat(hasCompatibleStatus).isTrue();
    }
    
    @Test
    void testDisplayResults_IncompatibleDetection_ShowsWarning() {
        // Given
        adrianmikula.jakartamigration.platforms.model.PlatformDetection detection = new adrianmikula.jakartamigration.platforms.model.PlatformDetection(
            "tomcat",
            "Apache Tomcat",
            "9.0.0",
            false,
            "10.0",
            java.util.Map.of("java", "11+", "jakarta", "9+")
        );
        
        platformsTab = new PlatformsTabComponent(mockProject);
        
        // When
        platformsTab.displayResults(List.of(detection));
        
        // Then
        JPanel panel = platformsTab.getPanel();
        boolean hasWarningStatus = containsComponentWithText(panel, "⚠️ Requires Jakarta EE Upgrade");
        
        assertThat(hasWarningStatus).isTrue();
    }
    
    @Test
    void testScanProject_CallsDetectionService() {
        // Given
        platformsTab = new PlatformsTabComponent(mockProject);
        
        // When
        platformsTab.scanProject();
        
        // Then
        // This test mainly verifies that the scan method doesn't throw exceptions
        // The actual scanning logic is tested in PlatformDetectionServiceTest
        assertThat(platformsTab.getPanel()).isNotNull();
    }
    
    @Test
    void testScanButton_InitialState_EnabledForPremiumUser() {
        // Given
        try (MockedStatic<FeatureFlags> mockedFlags = Mockito.mockStatic(FeatureFlags.class)) {
            mockedFlags.when(FeatureFlags::isPlatformsEnabled).thenReturn(true);
            
            platformsTab = new PlatformsTabComponent(mockProject);
            
            // When
            platformsTab.updatePremiumControls();
            
            // Then
            JPanel panel = platformsTab.getPanel();
            JButton scanButton = findScanButton(panel);
            
            assertThat(scanButton).isNotNull();
            assertThat(scanButton.isEnabled()).isTrue();
        }
    }
    
    @Test
    void testScanButton_InitialState_DisabledForNonPremiumUser() {
        // Given
        try (MockedStatic<FeatureFlags> mockedFlags = Mockito.mockStatic(FeatureFlags.class)) {
            mockedFlags.when(FeatureFlags::isPlatformsEnabled).thenReturn(false);
            
            platformsTab = new PlatformsTabComponent(mockProject);
            
            // When
            platformsTab.updatePremiumControls();
            
            // Then
            JPanel panel = platformsTab.getPanel();
            JButton scanButton = findScanButton(panel);
            
            assertThat(scanButton).isNotNull();
            assertThat(scanButton.isEnabled()).isFalse();
        }
    }
    
    // Helper methods
    private boolean containsComponentWithText(JPanel panel, String text) {
        for (java.awt.Component component : panel.getComponents()) {
            if (component instanceof JLabel) {
                JLabel label = (JLabel) component;
                if (label.getText() != null && label.getText().contains(text)) {
                    return true;
                }
            } else if (component instanceof JButton) {
                JButton button = (JButton) component;
                if (button.getText() != null && button.getText().contains(text)) {
                    return true;
                }
            } else if (component instanceof JPanel) {
                JPanel subPanel = (JPanel) component;
                if (containsComponentWithText(subPanel, text)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private JButton findScanButton(JPanel panel) {
        return findButtonByText(panel, "Analyse Project");
    }
    
    private JButton findButtonByText(JPanel panel, String text) {
        for (java.awt.Component component : panel.getComponents()) {
            if (component instanceof JButton) {
                JButton button = (JButton) component;
                if (text.equals(button.getText())) {
                    return button;
                }
            } else if (component instanceof JPanel) {
                JPanel subPanel = (JPanel) component;
                JButton foundButton = findButtonByText(subPanel, text);
                if (foundButton != null) {
                    return foundButton;
                }
            }
        }
        return null;
    }
    
    private Project createMockProject() {
        return new Project() {
            @Override
            public String getName() {
                return "Test Project";
            }
            
            @Override
            public java.nio.file.Path getBasePath() {
                return java.nio.file.Path.of("/test/project");
            }
            
            // Add minimal required method implementations
            @Override
            public String getLocationHash() { return "test-hash"; }
            
            @Override
            public String getPresentableUrl() { return "test-url"; }
            
            @Override
            public void save() { }
            
            @Override
            public boolean isDefault() { return false; }
        };
    }
}
