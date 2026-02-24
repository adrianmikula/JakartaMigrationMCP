package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.coderefactoring.domain.Recipe;
import adrianmikula.jakartamigration.coderefactoring.domain.RefactoringPhase;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.ArrayList;
import java.util.List;

public class RefactorComponentTest extends BasePlatformTestCase {

    private RefactorComponent refactorComponent;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        refactorComponent = new RefactorComponent(getProject());
    }

    public void testInitialization() {
        assertThat(refactorComponent.getPanel()).isNotNull();
        assertThat(refactorComponent.getPhaseListModel().isEmpty()).isTrue();
        assertThat(refactorComponent.getRecipeListModel().isEmpty()).isTrue();
    }

    public void testSetPhases() {
        List<RefactoringPhase> phases = new ArrayList<>();
        phases.add(new RefactoringPhase(1, "Phase 1", new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), java.time.Duration.ZERO));

        refactorComponent.setPhases(phases);

        assertThat(refactorComponent.getPhaseListModel().getSize()).isEqualTo(1);
        assertThat(refactorComponent.getPhaseListModel().getElementAt(0)).isEqualTo("Phase 1");
    }

    public void testSetRecipes() {
        List<Recipe> recipes = new ArrayList<>();
        recipes.add(Recipe.jakartaNamespaceRecipe());

        refactorComponent.setRecipes(recipes);

        assertThat(refactorComponent.getRecipeListModel().getSize()).isEqualTo(1);
        assertThat(refactorComponent.getRecipeListModel().getElementAt(0).name()).isEqualTo("AddJakartaNamespace");
    }

    public void testClearAll() {
        List<RefactoringPhase> phases = new ArrayList<>();
        phases.add(new RefactoringPhase(1, "Phase 1", new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), java.time.Duration.ZERO));
        refactorComponent.setPhases(phases);

        List<Recipe> recipes = new ArrayList<>();
        recipes.add(Recipe.jakartaNamespaceRecipe());
        refactorComponent.setRecipes(recipes);

        refactorComponent.clearAll();

        assertThat(refactorComponent.getPhaseListModel().isEmpty()).isTrue();
        assertThat(refactorComponent.getRecipeListModel().isEmpty()).isTrue();
        assertThat(refactorComponent.getPreviewArea().getText()).isEmpty();
    }
}
