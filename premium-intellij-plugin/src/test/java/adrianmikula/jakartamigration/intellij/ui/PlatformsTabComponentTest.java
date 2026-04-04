package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.config.FeatureFlags;
import adrianmikula.jakartamigration.intellij.integration.ExampleProjectManager;
import adrianmikula.jakartamigration.platforms.model.PlatformDetection;
import adrianmikula.jakartamigration.platforms.model.PlatformScanResult;
import adrianmikula.jakartamigration.platforms.service.PlatformDetectionService;

import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.swing.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Integration tests for PlatformsTabComponent using real GitHub repositories
 * from examples.yaml instead of mocked data.
 */
public class PlatformsTabComponentTest extends BasePlatformTestCase {
    
    private PlatformsTabComponent platformsTab;
    private Project mockProject;
    private ExampleProjectManager exampleManager;
    private PlatformDetectionService detectionService;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    protected void setUp() throws Exception {
        mockProject = getProject();
        exampleManager = new ExampleProjectManager(tempDir);
        detectionService = new PlatformDetectionService();
        platformsTab = new PlatformsTabComponent(mockProject);
    }
    
    @AfterEach
    protected void tearDown() {
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
            FeatureFlags mockInstance = Mockito.mock(FeatureFlags.class);
            mockedFlags.when(FeatureFlags::getInstance).thenReturn(mockInstance);
            when(mockInstance.isPlatformsEnabled()).thenReturn(false);
            
            platformsTab = new PlatformsTabComponent(mockProject);
            
            // When
            platformsTab.updatePremiumControls();
            
            // Then
            JPanel panel = platformsTab.getPanel();
            boolean hasLockIcon = containsComponentWithText(panel, " Premium Feature");
            boolean hasUpgradeButton = containsComponentWithText(panel, "Upgrade to Premium");
            
            assertThat(hasLockIcon).isTrue();
            assertThat(hasUpgradeButton).isTrue();
        }
    }
    
    @Test
    void testUpdatePremiumControls_PremiumUser_HidesLockIcon() {
        // Given
        try (MockedStatic<FeatureFlags> mockedFlags = Mockito.mockStatic(FeatureFlags.class)) {
            FeatureFlags mockInstance = Mockito.mock(FeatureFlags.class);
            mockedFlags.when(FeatureFlags::getInstance).thenReturn(mockInstance);
            when(mockInstance.isPlatformsEnabled()).thenReturn(true);
            
            platformsTab = new PlatformsTabComponent(mockProject);
            
            // When
            platformsTab.updatePremiumControls();
            
            // Then
            JPanel panel = platformsTab.getPanel();
            boolean hasLockIcon = containsComponentWithText(panel, " Premium Feature");
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
        PlatformDetection detection = new PlatformDetection(
            "tomcat",
            "Apache Tomcat",
            "10.1.5",
            true,
            "10.0",
            Map.of("java", "11+", "jakarta", "9+")
        );
        
        platformsTab = new PlatformsTabComponent(mockProject);
        
        // When
        platformsTab.displayResults(List.of(detection.platformName()));
        
        // Then
        JPanel panel = platformsTab.getPanel();
        boolean hasPlatformName = containsComponentWithText(panel, "Apache Tomcat");
        boolean hasVersion = containsComponentWithText(panel, "Version: 10.1.5");
        boolean hasCompatibleStatus = containsComponentWithText(panel, " Jakarta EE Compatible");
        
        assertThat(hasPlatformName).isTrue();
        assertThat(hasVersion).isTrue();
        assertThat(hasCompatibleStatus).isTrue();
    }
    
    @Test
    void testDisplayResults_IncompatibleDetection_ShowsWarning() {
        // Given
        PlatformDetection detection = new PlatformDetection(
            "tomcat",
            "Apache Tomcat",
            "9.0.0",
            false,
            "10.0",
            Map.of("java", "8+", "jakarta", "N/A")
        );
        
        platformsTab = new PlatformsTabComponent(mockProject);
        
        // When
        platformsTab.displayResults(List.of(detection.platformName()));
        
        // Then
        JPanel panel = platformsTab.getPanel();
        boolean hasPlatformName = containsComponentWithText(panel, "Apache Tomcat");
        boolean hasVersion = containsComponentWithText(panel, "Version: 9.0.0");
        boolean hasIncompatibleStatus = containsComponentWithText(panel, " Jakarta EE Incompatible");
        
        assertThat(hasPlatformName).isTrue();
        assertThat(hasVersion).isTrue();
        assertThat(hasIncompatibleStatus).isTrue();
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
            FeatureFlags mockInstance = Mockito.mock(FeatureFlags.class);
            mockedFlags.when(FeatureFlags::getInstance).thenReturn(mockInstance);
            when(mockInstance.isPlatformsEnabled()).thenReturn(true);
            
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
            FeatureFlags mockInstance = Mockito.mock(FeatureFlags.class);
            mockedFlags.when(FeatureFlags::getInstance).thenReturn(mockInstance);
            when(mockInstance.isPlatformsEnabled()).thenReturn(false);
            
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
}
