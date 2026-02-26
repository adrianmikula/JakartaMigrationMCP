package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.coderefactoring.domain.Recipe;
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
        assertThat(refactorComponent.getRecipeListModel().isEmpty()).isTrue();
    }

    public void testSetRecipes() {
        List<Recipe> recipes = new ArrayList<>();
        recipes.add(Recipe.jakartaNamespaceRecipe());

        refactorComponent.setRecipes(recipes);

        assertThat(refactorComponent.getRecipeListModel().getSize()).isEqualTo(1);
        assertThat(refactorComponent.getRecipeListModel().getElementAt(0).name()).isEqualTo("AddJakartaNamespace");
    }

    public void testSetAllRestoredRecipes() {
        List<Recipe> recipes = List.of(
                Recipe.jakartaNamespaceRecipe(),
                Recipe.persistenceXmlRecipe(),
                Recipe.webXmlRecipe(),
                Recipe.jpaRecipe(),
                Recipe.beanValidationRecipe(),
                Recipe.servletRecipe(),
                Recipe.cdiRecipe(),
                Recipe.restRecipe(),
                Recipe.soapRecipe());

        refactorComponent.setRecipes(recipes);

        assertThat(refactorComponent.getRecipeListModel().getSize()).isEqualTo(9);
        assertThat(refactorComponent.getRecipeListModel().getElementAt(3).name()).isEqualTo("MigrateJPA");
        assertThat(refactorComponent.getRecipeListModel().getElementAt(8).name()).isEqualTo("MigrateSOAP");
    }

    public void testClearAll() {
        List<Recipe> recipes = new ArrayList<>();
        recipes.add(Recipe.jakartaNamespaceRecipe());
        refactorComponent.setRecipes(recipes);

        refactorComponent.clearAll();

        assertThat(refactorComponent.getRecipeListModel().isEmpty()).isTrue();
        assertThat(refactorComponent.getPreviewArea().getText()).isEmpty();
    }
}
