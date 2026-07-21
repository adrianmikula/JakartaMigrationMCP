package adrianmikula.jakartamigration.intellij.ui.refactor;

import adrianmikula.jakartamigration.coderefactoring.service.RecipeService;
import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for AdvancedRefactorComponent.
 */
class AdvancedRefactorComponentTest {
    
    private Project mockProject;
    private RecipeService mockRecipeService;
    
    @BeforeEach
    void setUp() {
        mockProject = mock(Project.class);
        mockRecipeService = mock(RecipeService.class);
    }
    
    @Test
    void testComponentCreation() {
        AdvancedRefactorComponent component = new AdvancedRefactorComponent(mockProject, mockRecipeService);
        
        assertThat(component).isNotNull();
        assertThat(component.getPanel()).isNotNull();
    }
    
    @Test
    void testRefresh() {
        AdvancedRefactorComponent component = new AdvancedRefactorComponent(mockProject, mockRecipeService);
        
        // Refresh should not throw exceptions
        component.refresh();
        
        assertThat(component).isNotNull();
    }
}
