package adrianmikula.jakartamigration.intellij.service;

import adrianmikula.jakartamigration.coderefactoring.domain.RecipeCategory;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition;
import adrianmikula.jakartamigration.coderefactoring.service.RecipeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;

/**
 * Tests for ExperimentService.
 */
class ExperimentServiceTest {
    
    private Path mockProjectRoot;
    private RecipeService mockRecipeService;
    
    @BeforeEach
    void setUp() {
        mockProjectRoot = mock(Path.class);
        mockRecipeService = mock(RecipeService.class);
    }
    
    @Test
    void testServiceCreation() {
        ExperimentService service = new ExperimentService(mockProjectRoot, mockRecipeService);
        
        assertThat(service).isNotNull();
    }
    
    @Test
    void testGetAvailableRecipes() {
        when(mockRecipeService.getRecipes(any())).thenReturn(List.of());
        
        ExperimentService service = new ExperimentService(mockProjectRoot, mockRecipeService);
        List<RecipeDefinition> recipes = service.getAvailableRecipes();
        
        assertThat(recipes).isNotNull();
        verify(mockRecipeService).getRecipes(mockProjectRoot);
    }
    
    @Test
    void testGetRecipesByCategory() {
        when(mockRecipeService.getRecipesByCategory(any(), any())).thenReturn(List.of());
        
        ExperimentService service = new ExperimentService(mockProjectRoot, mockRecipeService);
        List<RecipeDefinition> recipes = service.getRecipesByCategory(RecipeCategory.ANNOTATIONS);
        
        assertThat(recipes).isNotNull();
        verify(mockRecipeService).getRecipesByCategory(RecipeCategory.ANNOTATIONS, mockProjectRoot);
    }
}
