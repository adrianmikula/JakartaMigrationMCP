package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.advancedscanning.domain.*;
import adrianmikula.jakartamigration.intellij.service.AdvancedScanningService;
import adrianmikula.jakartamigration.coderefactoring.service.RecipeService;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.file.Path;
import java.util.ArrayList;

import static org.mockito.Mockito.*;

/**
 * Test for AdvancedScansComponent refresh functionality.
 * Verifies that refreshFromCachedResults() properly displays scan results.
 */
public class AdvancedScansComponentRefreshTest extends BasePlatformTestCase {

    @Mock
    private AdvancedScanningService mockScanningService;
    
    @Mock
    private RecipeService mockRecipeService;
    
    private AdvancedScansComponent component;
    
    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.openMocks(this);
        
        Project project = getProject();
        component = new AdvancedScansComponent(project, mockScanningService);
    }
    
    @Test
    public void testRefreshFromCachedResults_DisplaysResults() {
        // Arrange: Create mock scan summary with JPA results
        ProjectScanResult<FileScanResult<JpaAnnotationUsage>> jpaResult = 
                createMockJpaResult("javax.persistence.Entity", "jakarta.persistence.Entity", 5);
        
        AdvancedScanningService.AdvancedScanSummary mockSummary = 
                new AdvancedScanningService.AdvancedScanSummary(
                        jpaResult,
                        null, // beanValidationResult
                        null, // servletJspResult
                        null, // cdiInjectionResult
                        null, // buildConfigResult
                        null, // restSoapResult
                        null, // deprecatedApiResult
                        null, // securityApiResult
                        null, // jmsMessagingResult
                        null, // transitiveDependencyResult
                        null, // configFileResult
                        null, // classloaderModuleResult
                        null, // loggingMetricsResult
                        null, // serializationCacheResult
                        null  // thirdPartyLibResult
                );
        
        // Mock the scanning service to return cached results
        when(mockScanningService.hasCachedResults()).thenReturn(true);
        when(mockScanningService.getCachedSummary()).thenReturn(mockSummary);
        
        // Act: Call refresh method
        component.refreshFromCachedResults();
        
        // Assert: Verify that JPA table shows results
        assertEquals("JPA table should have 5 rows", 5, component.getJpaTable().getRowCount());
        assertTrue("JPA status should indicate results found", 
                component.getJpaStatusLabel().getText().contains("Found"));
        
        // Verify service methods were called
        verify(mockScanningService).hasCachedResults();
        verify(mockScanningService).getCachedSummary();
    }
    
    @Test
    public void testRefreshFromCachedResults_NoCachedResults() {
        // Arrange: No cached results available
        when(mockScanningService.hasCachedResults()).thenReturn(false);
        
        // Act: Call refresh method
        component.refreshFromCachedResults();
        
        // Assert: Table should remain empty
        assertEquals("JPA table should remain empty", 0, component.getJpaTable().getRowCount());
        assertEquals("JPA status should show not scanned", "Not scanned yet", 
                component.getJpaStatusLabel().getText());
        
        // Verify service methods were called
        verify(mockScanningService).hasCachedResults();
        verify(mockScanningService, never()).getCachedSummary();
    }
    
    private ProjectScanResult<FileScanResult<JpaAnnotationUsage>> createMockJpaResult(
            String annotationName, String jakartaEquivalent, int count) {
        
        // Create mock file results
        ArrayList<FileScanResult<JpaAnnotationUsage>> fileResults = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            JpaAnnotationUsage usage = new JpaAnnotationUsage(
                    annotationName,
                    jakartaEquivalent,
                    i + 1, // lineNumber
                    "TestElement" + i, // elementName
                    "test context" // context
            );
            
            FileScanResult<JpaAnnotationUsage> fileResult = new FileScanResult<>(
                    Path.of("TestFile" + i + ".java"),
                    new ArrayList<>() {{ add(usage); }},
                    100 // lineCount
            );
            fileResults.add(fileResult);
        }
        
        return new ProjectScanResult<>(
                fileResults,
                count * 100, // totalFilesScanned
                count,       // filesWithIssues
                count         // totalIssuesFound
        );
    }
}
