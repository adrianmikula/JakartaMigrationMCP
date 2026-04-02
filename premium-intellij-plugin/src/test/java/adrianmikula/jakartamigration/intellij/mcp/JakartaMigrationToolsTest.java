package adrianmikula.jakartamigration.intellij.mcp;

import adrianmikula.jakartamigration.mcp.JakartaMigrationTools;
import adrianmikula.jakartamigration.config.FeatureFlagsProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for Jakarta Migration MCP tools.
 * Tests all 9 MCP tools for proper functionality and error handling.
 */
@ExtendWith(MockitoExtension.class)
@Slf4j
public class JakartaMigrationToolsTest {
    
    @Mock
    private adrianmikula.jakartamigration.dependencyanalysis.service.DependencyAnalysisModule dependencyAnalysisModule;
    
    @Mock
    private adrianmikula.jakartamigration.sourcecodescanning.service.SourceCodeScanner sourceCodeScanner;
    
    @Mock
    private adrianmikula.jakartamigration.config.FeatureFlagsService featureFlags;
    
    @Mock
    private adrianmikula.jakartamigration.mcp.CommunityMigrationTools communityTools;
    
    private JakartaMigrationTools jakartaMigrationTools;
    private ObjectMapper objectMapper;
    private Path testProjectPath;
    
    @BeforeEach
    void setUp() {
        jakartaMigrationTools = new JakartaMigrationTools(
            communityTools,
            dependencyAnalysisModule,
            featureFlags,
            sourceCodeScanner
        );
        objectMapper = new ObjectMapper();
        testProjectPath = Paths.get("/test/project");
    }
    
    // ============================================
    // COMMUNITY TOOLS TESTS
    // ============================================
    
    @Test
    @DisplayName("analyzeJakartaReadiness should delegate to community tools")
    void testAnalyzeJakartaReadinessDelegatesToCommunity() {
        // Arrange
        String expectedResponse = "{\"status\": \"success\", \"readinessScore\": 85}";
        when(communityTools.analyzeJakartaReadiness("/test/project")).thenReturn(expectedResponse);
        
        // Act
        String result = jakartaMigrationTools.analyzeJakartaReadiness("/test/project");
        
        // Assert
        assertEquals(expectedResponse, result);
        verify(communityTools).analyzeJakartaReadiness("/test/project");
    }
    
    @Test
    @DisplayName("recommendVersions should delegate to community tools")
    void testRecommendVersionsDelegatesToCommunity() {
        // Arrange
        String expectedResponse = "{\"status\": \"success\", \"recommendations\": []}";
        when(communityTools.recommendVersions("/test/project")).thenReturn(expectedResponse);
        
        // Act
        String result = jakartaMigrationTools.recommendVersions("/test/project");
        
        // Assert
        assertEquals(expectedResponse, result);
        verify(communityTools).recommendVersions("/test/project");
    }
    
    // ============================================
    // PREMIUM TOOLS TESTS
    // ============================================
    
    @Test
    @DisplayName("listDependenciesCompatibility should require premium license")
    void testListDependenciesCompatibilityRequiresPremium() {
        // Arrange
        when(featureFlags.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)).thenReturn(false);
        when(featureFlags.getUpgradeInfo(any())).thenReturn(createMockUpgradeInfo());
        
        // Act
        String result = jakartaMigrationTools.listDependenciesCompatibility("/test/project");
        
        // Assert
        assertTrue(result.contains("upgrade_required"));
        assertTrue(result.contains("PREMIUM license"));
        verify(featureFlags).hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM);
    }
    
    @Test
    @DisplayName("listDependenciesCompatibility should work with premium license")
    void testListDependenciesCompatibilityWorksWithPremium() throws Exception {
        // Arrange
        when(featureFlags.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)).thenReturn(true);
        adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph mockGraph = createMockDependencyGraph();
        adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyAnalysisReport mockReport = 
            new adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyAnalysisReport(
                mockGraph, java.util.List.of(), java.util.List.of(), 
                new adrianmikula.jakartamigration.dependencyanalysis.domain.ReadinessScore(85, "Good"), 
                new adrianmikula.jakartamigration.dependencyanalysis.domain.RiskAssessment(25, "Medium")
            );
        when(dependencyAnalysisModule.analyzeProject(testProjectPath)).thenReturn(mockReport);
        
        // Act
        String result = jakartaMigrationTools.listDependenciesCompatibility("/test/project");
        
        // Assert
        assertTrue(result.contains("success"));
        assertTrue(result.contains("premium"));
        assertTrue(result.contains("\"totalDependencies\": 5"));
        assertTrue(result.contains("\"compatibleCount\": 4"));
        assertTrue(result.contains("\"incompatibleCount\": 1"));
        verify(dependencyAnalysisModule).analyzeProject(testProjectPath);
    }
    
    @Test
    @DisplayName("listRefactorRecipes should require premium license")
    void testListRefactorRecipesRequiresPremium() {
        // Arrange
        when(featureFlags.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)).thenReturn(false);
        when(featureFlags.getUpgradeInfo(any())).thenReturn(createMockUpgradeInfo());
        
        // Act
        String result = jakartaMigrationTools.listRefactorRecipes("/test/project", null, null, null, null);
        
        // Assert
        assertTrue(result.contains("upgrade_required"));
        assertTrue(result.contains("PREMIUM license"));
        verify(featureFlags).hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM);
    }
    
    @Test
    @DisplayName("listRefactorRecipes should return all recipes with premium license and no filters")
    void testListRefactorRecipesWorksWithPremiumAndNoFilters() throws Exception {
        // Arrange
        when(featureFlags.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)).thenReturn(true);
        
        // Mock RecipeService
        adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition recipe1 = 
            new adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition(
                "JavaxValidationToJakartaValidation", "Migrates javax.validation to jakarta.validation",
                adrianmikula.jakartamigration.coderefactoring.domain.RecipeCategory.VALIDATION,
                adrianmikula.jakartamigration.coderefactoring.domain.RecipePriority.HIGH,
                adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition.RecipeStatus.AVAILABLE,
                true, java.time.Instant.now());
        
        adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition recipe2 = 
            new adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition(
                "JavaxServletToJakartaServlet", "Migrates javax.servlet to jakarta.servlet",
                adrianmikula.jakartamigration.coderefactoring.domain.RecipeCategory.SERVLET,
                adrianmikula.jakartamigration.coderefactoring.domain.RecipePriority.MEDIUM,
                adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition.RecipeStatus.APPLICABLE,
                true, java.time.Instant.now());
        
        java.util.List<adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition> mockRecipes = java.util.List.of(recipe1, recipe2);
        
        // Mock RecipeService methods
        try (var mockedStatic = mockStatic(adrianmikula.jakartamigration.coderefactoring.service.impl.RecipeServiceImpl.class)) {
            adrianmikula.jakartamigration.coderefactoring.service.impl.RecipeServiceImpl mockRecipeService = mock(adrianmikula.jakartamigration.coderefactoring.service.impl.RecipeServiceImpl.class);
            when(mockedStatic.construct(any(), any())).thenReturn(mockRecipeService);
            when(mockRecipeService.getRecipes(any())).thenReturn(mockRecipes);
            when(mockRecipeService.getHistory(any())).thenReturn(java.util.List.of());
            
            // Act
            String result = jakartaMigrationTools.listRefactorRecipes("/test/project", null, null, null, null);
            
            // Assert
            assertTrue(result.contains("success"));
            assertTrue(result.contains("premium"));
            assertTrue(result.contains("\"totalRecipes\": 2"));
            assertTrue(result.contains("\"filteredRecipes\": 2"));
            assertTrue(result.contains("JavaxValidationToJakartaValidation"));
            assertTrue(result.contains("JavaxServletToJakartaServlet"));
            assertTrue(result.contains("\"category\": \"all\""));
            assertTrue(result.contains("\"applicableOnly\": \"null\""));
            assertTrue(result.contains("\"priority\": \"all\""));
            assertTrue(result.contains("\"status\": \"all\""));
        }
    }
    
    @Test
    @DisplayName("listRefactorRecipes should filter by category")
    void testListRefactorRecipesFiltersByCategory() throws Exception {
        // Arrange
        when(featureFlags.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)).thenReturn(true);
        
        adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition validationRecipe = 
            new adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition(
                "JavaxValidationToJakartaValidation", "Migrates javax.validation to jakarta.validation",
                adrianmikula.jakartamigration.coderefactoring.domain.RecipeCategory.VALIDATION,
                adrianmikula.jakartamigration.coderefactoring.domain.RecipePriority.HIGH,
                adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition.RecipeStatus.AVAILABLE,
                true, java.time.Instant.now());
        
        adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition servletRecipe = 
            new adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition(
                "JavaxServletToJakartaServlet", "Migrates javax.servlet to jakarta.servlet",
                adrianmikula.jakartamigration.coderefactoring.domain.RecipeCategory.SERVLET,
                adrianmikula.jakartamigration.coderefactoring.domain.RecipePriority.HIGH,
                adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition.RecipeStatus.AVAILABLE,
                true, java.time.Instant.now());
        
        java.util.List<adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition> mockRecipes = java.util.List.of(validationRecipe, servletRecipe);
        
        try (var mockedStatic = mockStatic(adrianmikula.jakartamigration.coderefactoring.service.impl.RecipeServiceImpl.class)) {
            adrianmikula.jakartamigration.coderefactoring.service.impl.RecipeServiceImpl mockRecipeService = mock(adrianmikula.jakartamigration.coderefactoring.service.impl.RecipeServiceImpl.class);
            when(mockedStatic.construct(any(), any())).thenReturn(mockRecipeService);
            when(mockRecipeService.getRecipes(any())).thenReturn(mockRecipes);
            when(mockRecipeService.getHistory(any())).thenReturn(java.util.List.of());
            
            // Act
            String result = jakartaMigrationTools.listRefactorRecipes("/test/project", "validation", null, null, null);
            
            // Assert
            assertTrue(result.contains("success"));
            assertTrue(result.contains("\"category\": \"validation\""));
            assertTrue(result.contains("\"totalRecipes\": 2"));
            assertTrue(result.contains("\"filteredRecipes\": 1"));
            assertTrue(result.contains("JavaxValidationToJakartaValidation"));
            assertFalse(result.contains("JavaxServletToJakartaServlet"));
        }
    }
    
    @Test
    @DisplayName("listRefactorRecipes should filter by applicability")
    void testListRefactorRecipesFiltersByApplicability() throws Exception {
        // Arrange
        when(featureFlags.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)).thenReturn(true);
        
        adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition applicableRecipe = 
            new adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition(
                "JavaxValidationToJakartaValidation", "Migrates javax.validation to jakarta.validation",
                adrianmikula.jakartamigration.coderefactoring.domain.RecipeCategory.VALIDATION,
                adrianmikula.jakartamigration.coderefactoring.domain.RecipePriority.HIGH,
                adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition.RecipeStatus.AVAILABLE,
                true, java.time.Instant.now());
        
        adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition notApplicableRecipe = 
            new adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition(
                "JavaxServletToJakartaServlet", "Migrates javax.servlet to jakarta.servlet",
                adrianmikula.jakartamigration.coderefactoring.domain.RecipeCategory.SERVLET,
                adrianmikula.jakartamigration.coderefactoring.domain.RecipePriority.HIGH,
                adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition.RecipeStatus.AVAILABLE,
                false, java.time.Instant.now());
        
        java.util.List<adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition> mockRecipes = java.util.List.of(applicableRecipe, notApplicableRecipe);
        
        try (var mockedStatic = mockStatic(adrianmikula.jakartamigration.coderefactoring.service.impl.RecipeServiceImpl.class)) {
            adrianmikula.jakartamigration.coderefactoring.service.impl.RecipeServiceImpl mockRecipeService = mock(adrianmikula.jakartamigration.coderefactoring.service.impl.RecipeServiceImpl.class);
            when(mockedStatic.construct(any(), any())).thenReturn(mockRecipeService);
            when(mockRecipeService.getRecipes(any())).thenReturn(mockRecipes);
            when(mockRecipeService.getHistory(any())).thenReturn(java.util.List.of());
            
            // Act
            String result = jakartaMigrationTools.listRefactorRecipes("/test/project", null, true, null, null);
            
            // Assert
            assertTrue(result.contains("success"));
            assertTrue(result.contains("\"applicableOnly\": \"true\""));
            assertTrue(result.contains("\"totalRecipes\": 2"));
            assertTrue(result.contains("\"filteredRecipes\": 1"));
            assertTrue(result.contains("JavaxValidationToJakartaValidation"));
            assertFalse(result.contains("JavaxServletToJakartaServlet"));
        }
    }
    
    @Test
    @DisplayName("listRefactorRecipes should filter by priority")
    void testListRefactorRecipesFiltersByPriority() throws Exception {
        // Arrange
        when(featureFlags.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)).thenReturn(true);
        
        adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition highPriorityRecipe = 
            new adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition(
                "JavaxValidationToJakartaValidation", "Migrates javax.validation to jakarta.validation",
                adrianmikula.jakartamigration.coderefactoring.domain.RecipeCategory.VALIDATION,
                adrianmikula.jakartamigration.coderefactoring.domain.RecipePriority.HIGH,
                adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition.RecipeStatus.AVAILABLE,
                true, java.time.Instant.now());
        
        adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition mediumPriorityRecipe = 
            new adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition(
                "JavaxServletToJakartaServlet", "Migrates javax.servlet to jakarta.servlet",
                adrianmikula.jakartamigration.coderefactoring.domain.RecipeCategory.SERVLET,
                adrianmikula.jakartamigration.coderefactoring.domain.RecipePriority.MEDIUM,
                adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition.RecipeStatus.AVAILABLE,
                true, java.time.Instant.now());
        
        java.util.List<adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition> mockRecipes = java.util.List.of(highPriorityRecipe, mediumPriorityRecipe);
        
        try (var mockedStatic = mockStatic(adrianmikula.jakartamigration.coderefactoring.service.impl.RecipeServiceImpl.class)) {
            adrianmikula.jakartamigration.coderefactoring.service.impl.RecipeServiceImpl mockRecipeService = mock(adrianmikula.jakartamigration.coderefactoring.service.impl.RecipeServiceImpl.class);
            when(mockedStatic.construct(any(), any())).thenReturn(mockRecipeService);
            when(mockRecipeService.getRecipes(any())).thenReturn(mockRecipes);
            when(mockRecipeService.getHistory(any())).thenReturn(java.util.List.of());
            
            // Act
            String result = jakartaMigrationTools.listRefactorRecipes("/test/project", null, null, "high", null);
            
            // Assert
            assertTrue(result.contains("success"));
            assertTrue(result.contains("\"priority\": \"high\""));
            assertTrue(result.contains("\"totalRecipes\": 2"));
            assertTrue(result.contains("\"filteredRecipes\": 1"));
            assertTrue(result.contains("JavaxValidationToJakartaValidation"));
            assertFalse(result.contains("JavaxServletToJakartaServlet"));
        }
    }
    
    @Test
    @DisplayName("listRefactorRecipes should filter by status")
    void testListRefactorRecipesFiltersByStatus() throws Exception {
        // Arrange
        when(featureFlags.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)).thenReturn(true);
        
        adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition successRecipe = 
            new adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition(
                "JavaxValidationToJakartaValidation", "Migrates javax.validation to jakarta.validation",
                adrianmikula.jakartamigration.coderefactoring.domain.RecipeCategory.VALIDATION,
                adrianmikula.jakartamigration.coderefactoring.domain.RecipePriority.HIGH,
                adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition.RecipeStatus.RUN_SUCCESS,
                true, java.time.Instant.now());
        
        adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition failedRecipe = 
            new adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition(
                "JavaxServletToJakartaServlet", "Migrates javax.servlet to jakarta.servlet",
                adrianmikula.jakartamigration.coderefactoring.domain.RecipeCategory.SERVLET,
                adrianmikula.jakartamigration.coderefactoring.domain.RecipePriority.HIGH,
                adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition.RecipeStatus.RUN_FAILED,
                true, java.time.Instant.now());
        
        java.util.List<adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition> mockRecipes = java.util.List.of(successRecipe, failedRecipe);
        
        try (var mockedStatic = mockStatic(adrianmikula.jakartamigration.coderefactoring.service.impl.RecipeServiceImpl.class)) {
            adrianmikula.jakartamigration.coderefactoring.service.impl.RecipeServiceImpl mockRecipeService = mock(adrianmikula.jakartamigration.coderefactoring.service.impl.RecipeServiceImpl.class);
            when(mockedStatic.construct(any(), any())).thenReturn(mockRecipeService);
            when(mockRecipeService.getRecipes(any())).thenReturn(mockRecipes);
            when(mockRecipeService.getHistory(any())).thenReturn(java.util.List.of());
            
            // Act
            String result = jakartaMigrationTools.listRefactorRecipes("/test/project", null, null, null, "run_success");
            
            // Assert
            assertTrue(result.contains("success"));
            assertTrue(result.contains("\"status\": \"run_success\""));
            assertTrue(result.contains("\"totalRecipes\": 2"));
            assertTrue(result.contains("\"filteredRecipes\": 1"));
            assertTrue(result.contains("JavaxValidationToJakartaValidation"));
            assertFalse(result.contains("JavaxServletToJakartaServlet"));
        }
    }
    
    @Test
    @DisplayName("listRefactorRecipes should apply multiple filters")
    void testListRefactorRecipesAppliesMultipleFilters() throws Exception {
        // Arrange
        when(featureFlags.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)).thenReturn(true);
        
        adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition matchingRecipe = 
            new adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition(
                "JavaxValidationToJakartaValidation", "Migrates javax.validation to jakarta.validation",
                adrianmikula.jakartamigration.coderefactoring.domain.RecipeCategory.VALIDATION,
                adrianmikula.jakartamigration.coderefactoring.domain.RecipePriority.HIGH,
                adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition.RecipeStatus.AVAILABLE,
                true, java.time.Instant.now());
        
        adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition nonMatchingRecipe = 
            new adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition(
                "JavaxServletToJakartaServlet", "Migrates javax.servlet to jakarta.servlet",
                adrianmikula.jakartamigration.coderefactoring.domain.RecipeCategory.SERVLET,
                adrianmikula.jakartamigration.coderefactoring.domain.RecipePriority.MEDIUM,
                adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition.RecipeStatus.AVAILABLE,
                true, java.time.Instant.now());
        
        java.util.List<adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition> mockRecipes = java.util.List.of(matchingRecipe, nonMatchingRecipe);
        
        try (var mockedStatic = mockStatic(adrianmikula.jakartamigration.coderefactoring.service.impl.RecipeServiceImpl.class)) {
            adrianmikula.jakartamigration.coderefactoring.service.impl.RecipeServiceImpl mockRecipeService = mock(adrianmikula.jakartamigration.coderefactoring.service.impl.RecipeServiceImpl.class);
            when(mockedStatic.construct(any(), any())).thenReturn(mockRecipeService);
            when(mockRecipeService.getRecipes(any())).thenReturn(mockRecipes);
            when(mockRecipeService.getHistory(any())).thenReturn(java.util.List.of());
            
            // Act - Apply multiple filters: validation category + applicable only + high priority
            String result = jakartaMigrationTools.listRefactorRecipes("/test/project", "validation", true, "high", "available");
            
            // Assert
            assertTrue(result.contains("success"));
            assertTrue(result.contains("\"category\": \"validation\""));
            assertTrue(result.contains("\"applicableOnly\": \"true\""));
            assertTrue(result.contains("\"priority\": \"high\""));
            assertTrue(result.contains("\"status\": \"available\""));
            assertTrue(result.contains("\"totalRecipes\": 2"));
            assertTrue(result.contains("\"filteredRecipes\": 1"));
            assertTrue(result.contains("JavaxValidationToJakartaValidation"));
            assertFalse(result.contains("JavaxServletToJakartaServlet"));
        }
    }
    
    @Test
    @DisplayName("listHistoricalActions should require premium license")
    void testListHistoricalActionsRequiresPremium() {
        // Arrange
        when(featureFlags.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)).thenReturn(false);
        when(featureFlags.getUpgradeInfo(any())).thenReturn(createMockUpgradeInfo());
        
        // Act
        String result = jakartaMigrationTools.listHistoricalActions("/test/project");
        
        // Assert
        assertTrue(result.contains("upgrade_required"));
        assertTrue(result.contains("PREMIUM license"));
        verify(featureFlags).hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM);
    }
    
    @Test
    @DisplayName("listHistoricalActions should return empty actions with premium license")
    void testListHistoricalActionsWorksWithPremium() {
        // Arrange
        when(featureFlags.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)).thenReturn(true);
        
        // Act
        String result = jakartaMigrationTools.listHistoricalActions("/test/project");
        
        // Assert
        assertTrue(result.contains("success"));
        assertTrue(result.contains("premium"));
        assertTrue(result.contains("\"actions\": []"));
        assertTrue(result.contains("Historical action listing is being implemented"));
        verify(featureFlags).hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM);
    }
    
    @Test
    @DisplayName("undoHistoricalAction should require premium license")
    void testUndoHistoricalActionRequiresPremium() {
        // Arrange
        when(featureFlags.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)).thenReturn(false);
        when(featureFlags.getUpgradeInfo(any())).thenReturn(createMockUpgradeInfo());
        
        // Act
        String result = jakartaMigrationTools.undoHistoricalAction("/test/project", "action123");
        
        // Assert
        assertTrue(result.contains("upgrade_required"));
        assertTrue(result.contains("PREMIUM license"));
        verify(featureFlags).hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM);
    }
    
    @Test
    @DisplayName("undoHistoricalAction should work with premium license")
    void testUndoHistoricalActionWorksWithPremium() {
        // Arrange
        when(featureFlags.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)).thenReturn(true);
        
        // Act
        String result = jakartaMigrationTools.undoHistoricalAction("/test/project", "action123");
        
        // Assert
        assertTrue(result.contains("success"));
        assertTrue(result.contains("premium"));
        assertTrue(result.contains("\"actionId\": \"action123\""));
        assertTrue(result.contains("\"undoStatus\": \"pending\""));
        assertTrue(result.contains("Historical action undo is being implemented"));
        verify(featureFlags).hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM);
    }
    
    @Test
    @DisplayName("createReport should require premium license")
    void testCreateReportRequiresPremium() {
        // Arrange
        when(featureFlags.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)).thenReturn(false);
        when(featureFlags.getUpgradeInfo(any())).thenReturn(createMockUpgradeInfo());
        
        // Act
        String result = jakartaMigrationTools.createReport("/test/project", null);
        
        // Assert
        assertTrue(result.contains("upgrade_required"));
        assertTrue(result.contains("PREMIUM license"));
        verify(featureFlags).hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM);
    }
    
    @Test
    @DisplayName("createReport should work with premium license and default format")
    void testCreateReportWorksWithPremiumAndDefaultFormat() throws Exception {
        // Arrange
        when(featureFlags.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)).thenReturn(true);
        adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph mockGraph = createMockDependencyGraph();
        adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyAnalysisReport mockReport = 
            new adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyAnalysisReport(
                mockGraph, java.util.List.of(), java.util.List.of(), 
                new adrianmikula.jakartamigration.dependencyanalysis.domain.ReadinessScore(90, "Excellent"), 
                new adrianmikula.jakartamigration.dependencyanalysis.domain.RiskAssessment(15, "Low")
            );
        when(dependencyAnalysisModule.analyzeProject(testProjectPath)).thenReturn(mockReport);
        
        // Act
        String result = jakartaMigrationTools.createReport("/test/project", null);
        
        // Assert
        assertTrue(result.contains("success"));
        assertTrue(result.contains("premium"));
        assertTrue(result.contains("\"format\": \"json\""));
        assertTrue(result.contains("\"readinessScore\": 90"));
        assertTrue(result.contains("\"totalDependencies\": 5"));
        assertTrue(result.contains("\"riskScore\": 15"));
        assertTrue(result.contains("\"generatedAt\""));
        verify(dependencyAnalysisModule).analyzeProject(testProjectPath);
    }
    
    @Test
    @DisplayName("createReport should work with premium license and custom format")
    void testCreateReportWorksWithPremiumAndCustomFormat() throws Exception {
        // Arrange
        when(featureFlags.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)).thenReturn(true);
        adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph mockGraph = createMockDependencyGraph();
        adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyAnalysisReport mockReport = 
            new adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyAnalysisReport(
                mockGraph, java.util.List.of(), java.util.List.of(), 
                new adrianmikula.jakartamigration.dependencyanalysis.domain.ReadinessScore(75, "Good"), 
                new adrianmikula.jakartamigration.dependencyanalysis.domain.RiskAssessment(35, "High")
            );
        when(dependencyAnalysisModule.analyzeProject(testProjectPath)).thenReturn(mockReport);
        
        // Act
        String result = jakartaMigrationTools.createReport("/test/project", "pdf");
        
        // Assert
        assertTrue(result.contains("success"));
        assertTrue(result.contains("premium"));
        assertTrue(result.contains("\"format\": \"pdf\""));
        assertTrue(result.contains("\"readinessScore\": 75"));
        assertTrue(result.contains("\"riskScore\": 35"));
        verify(dependencyAnalysisModule).analyzeProject(testProjectPath);
    }
    
    @Test
    @DisplayName("applyRefactorRecipe should require premium license")
    void testApplyRefactorRecipeRequiresPremium() {
        // Arrange
        when(featureFlags.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)).thenReturn(false);
        when(featureFlags.getUpgradeInfo(any())).thenReturn(createMockUpgradeInfo());
        
        // Act
        String result = jakartaMigrationTools.applyRefactorRecipe("/test/project", null, null, null);
        
        // Assert
        assertTrue(result.contains("upgrade_required"));
        assertTrue(result.contains("PREMIUM license"));
        verify(featureFlags).hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM);
    }
    
    @Test
    @DisplayName("applyRefactorRecipe should work with premium license")
    void testApplyRefactorRecipeWorksWithPremium() throws Exception {
        // Arrange
        when(featureFlags.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)).thenReturn(true);
        
        // Mock RecipeService and execution result
        adrianmikula.jakartamigration.coderefactoring.domain.RecipeExecutionResult mockResult = 
            new adrianmikula.jakartamigration.coderefactoring.domain.RecipeExecutionResult(
                true, 5, 3, java.util.List.of("file1.java", "file2.java", "file3.java"), 
                "Successfully applied recipe", 12345L);
        
        try (var mockedStatic = mockStatic(adrianmikula.jakartamigration.coderefactoring.service.impl.RecipeServiceImpl.class)) {
            adrianmikula.jakartamigration.coderefactoring.service.impl.RecipeServiceImpl mockRecipeService = mock(adrianmikula.jakartamigration.coderefactoring.service.impl.RecipeServiceImpl.class);
            when(mockedStatic.construct(any(), any())).thenReturn(mockRecipeService);
            when(mockRecipeService.applyRecipe(any(), any())).thenReturn(mockResult);
            
            // Act
            String result = jakartaMigrationTools.applyRefactorRecipe("/test/project", java.util.List.of("file1.java", "file2.java"), true);
            
            // Assert
            assertTrue(result.contains("success"));
            assertTrue(result.contains("premium"));
            assertTrue(result.contains("\"recipeName\": \"JavaxValidationToJakartaValidation\""));
            assertTrue(result.contains("\"dryRun\": true"));
            assertTrue(result.contains("\"executionResult\": {"));
            assertTrue(result.contains("\"success\": true"));
            assertTrue(result.contains("\"executionId\": 12345"));
            assertTrue(result.contains("\"filesProcessed\": 5"));
            assertTrue(result.contains("\"filesChanged\": 3"));
            assertTrue(result.contains("\"file1.java\""));
            assertTrue(result.contains("\"file2.java\""));
            assertTrue(result.contains("\"file3.java\""));
            assertTrue(result.contains("\"DRY RUN: Would modify 3 files\""));
        }
    }
    
    @Test
    @DisplayName("applyRefactorRecipe should handle execution failure")
    void testApplyRefactorRecipeHandlesFailure() throws Exception {
        // Arrange
        when(featureFlags.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)).thenReturn(true);
        
        // Mock failed execution result
        adrianmikula.jakartamigration.coderefactoring.domain.RecipeExecutionResult mockResult = 
            new adrianmikula.jakartamigration.coderefactoring.domain.RecipeExecutionResult(
                false, 5, 0, java.util.List.of(), 
                "Recipe execution failed: Invalid syntax", null);
        
        try (var mockedStatic = mockStatic(adrianmikula.jakartamigration.coderefactoring.service.impl.RecipeServiceImpl.class)) {
            adrianmikula.jakartamigration.coderefactoring.service.impl.RecipeServiceImpl mockRecipeService = mock(adrianmikula.jakartamigration.coderefactoring.service.impl.RecipeServiceImpl.class);
            when(mockedStatic.construct(any(), any())).thenReturn(mockRecipeService);
            when(mockRecipeService.applyRecipe(any(), any())).thenReturn(mockResult);
            
            // Act
            String result = jakartaMigrationTools.applyRefactorRecipe("/test/project", null, false, null);
            
            // Assert
            assertTrue(result.contains("success"));
            assertTrue(result.contains("premium"));
            assertTrue(result.contains("\"executionResult\": {"));
            assertTrue(result.contains("\"success\": false"));
            assertTrue(result.contains("\"filesProcessed\": 5"));
            assertTrue(result.contains("\"filesChanged\": 0"));
            assertTrue(result.contains("\"Recipe execution failed: Invalid syntax\""));
        }
    }
    
    // ============================================
    // ERROR HANDLING TESTS
    // ============================================
    
    @Test
    @DisplayName("should handle non-existent project path")
    void testHandlesNonExistentProjectPath() {
        // Arrange
        Path nonExistentPath = Paths.get("/non/existent/path");
        when(featureFlags.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)).thenReturn(true);
        
        // Act & Assert
        assertThrows(Exception.class, () -> {
            jakartaMigrationTools.analyzeMigrationImpact(nonExistentPath.toString());
        });
    }
    
    @Test
    @DisplayName("should handle null project path gracefully")
    void testHandlesNullProjectPath() {
        // Arrange
        when(featureFlags.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)).thenReturn(true);
        
        // Act
        String result = jakartaMigrationTools.analyzeMigrationImpact(null);
        
        // Assert
        assertTrue(result.contains("error"));
        assertTrue(result.contains("Project path does not exist or is not a directory"));
    }
    
    @Test
    @DisplayName("should handle empty project path gracefully")
    void testHandlesEmptyProjectPath() {
        // Arrange
        when(featureFlags.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)).thenReturn(true);
        
        // Act
        String result = jakartaMigrationTools.analyzeMigrationImpact("");
        
        // Assert
        assertTrue(result.contains("error"));
        assertTrue(result.contains("Project path does not exist or is not a directory"));
    }
    
    @Test
    @DisplayName("should handle dependency analysis exceptions")
    void testHandlesDependencyAnalysisExceptions() throws Exception {
        // Arrange
        when(featureFlags.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)).thenReturn(true);
        when(dependencyAnalysisModule.analyzeProject(any())).thenThrow(new RuntimeException("Analysis failed"));
        
        // Act
        String result = jakartaMigrationTools.analyzeMigrationImpact("/test/project");
        
        // Assert
        assertTrue(result.contains("error"));
        assertTrue(result.contains("Unexpected error: Analysis failed"));
        verify(dependencyAnalysisModule).analyzeProject(testProjectPath);
    }
    
    @Test
    @DisplayName("should handle source code scanner exceptions")
    void testHandlesSourceCodeScannerExceptions() throws Exception {
        // Arrange
        when(featureFlags.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)).thenReturn(true);
        adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph mockGraph = createMockDependencyGraph();
        adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyAnalysisReport mockReport = 
            new adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyAnalysisReport(
                mockGraph, java.util.List.of(), java.util.List.of(), 
                new adrianmikula.jakartamigration.dependencyanalysis.domain.ReadinessScore(80, "Good"), 
                new adrianmikula.jakartamigration.dependencyanalysis.domain.RiskAssessment(30, "Medium")
            );
        when(dependencyAnalysisModule.analyzeProject(testProjectPath)).thenReturn(mockReport);
        when(sourceCodeScanner.scanProject(any())).thenThrow(new RuntimeException("Scan failed"));
        
        // Act
        String result = jakartaMigrationTools.analyzeMigrationImpact("/test/project");
        
        // Assert
        assertTrue(result.contains("error"));
        assertTrue(result.contains("Unexpected error: Scan failed"));
        verify(dependencyAnalysisModule).analyzeProject(testProjectPath);
        verify(sourceCodeScanner).scanProject(testProjectPath);
    }
    
    // ============================================
    // HELPER METHODS
    // ============================================
    
    private adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph createMockDependencyGraph() {
        // Create mock dependency graph with 5 nodes
        java.util.List<adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyNode> nodes = java.util.List.of(
            new adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyNode("javax.validation", "1.0.0", true),
            new adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyNode("javax.servlet", "4.0.0", true),
            new adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyNode("javax.persistence", "2.1.0", false),
            new adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyNode("javax.ejb", "3.2.0", false),
            new adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyNode("javax.ws.rs", "2.0.0", false)
        );
        return new adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph(nodes, java.util.List.of());
    }
    
    private adrianmikula.jakartamigration.config.FeatureFlagsService.UpgradeInfo createMockUpgradeInfo() {
        return new adrianmikula.jakartamigration.config.FeatureFlagsService.UpgradeInfo(
            "Advanced Analysis",
            "Advanced analysis and migration planning",
            FeatureFlagsProperties.LicenseTier.PREMIUM,
            "This feature requires a PREMIUM subscription. Upgrade now for full access."
        );
    }
}
