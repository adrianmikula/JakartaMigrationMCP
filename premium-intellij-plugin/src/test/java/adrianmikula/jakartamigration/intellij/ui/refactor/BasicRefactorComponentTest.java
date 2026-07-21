package adrianmikula.jakartamigration.intellij.ui.refactor;

import adrianmikula.jakartamigration.coderefactoring.service.RecipeService;
import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for BasicRefactorComponent.
 */
class BasicRefactorComponentTest {
    
    private Project mockProject;
    private RecipeService mockRecipeService;
    
    @BeforeEach
    void setUp() {
        mockProject = mock(Project.class);
        mockRecipeService = mock(RecipeService.class);
    }
    
    @Test
    void testComponentCreation() {
        BasicRefactorComponent component = new BasicRefactorComponent(mockProject, mockRecipeService);
        
        assertThat(component).isNotNull();
        assertThat(component.getPanel()).isNotNull();
    }
    
    @Test
    void testCallbackRegistration() {
        BasicRefactorComponent component = new BasicRefactorComponent(mockProject, mockRecipeService);
        
        Runnable mockCallback = mock(Runnable.class);
        component.setOnRecipeExecuted(mockCallback);
        component.setOnCreditUsed(mockCallback);
        
        // Callbacks should be registered without throwing exceptions
        assertThat(component).isNotNull();
    }
}
