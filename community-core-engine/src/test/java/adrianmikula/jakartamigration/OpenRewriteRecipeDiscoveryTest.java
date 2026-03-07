package adrianmikula.jakartamigration;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.Recipe;

import java.util.Collection;

public class OpenRewriteRecipeDiscoveryTest {

    @Test
    public void discoverRecipes() {
        Environment env = Environment.builder()
                .scanRuntimeClasspath()
                .build();

        System.out.println("=== LISTING JAKARTA RECIPES ===");
        Collection<Recipe> recipes = env.listRecipes();
        for (Recipe recipe : recipes) {
            if (recipe.getName().contains("jakarta")) {
                System.out.println("Recipe: " + recipe.getName());
            }
        }

        System.out.println("=== TRYING TO ACTIVATE SPECIFIC RECIPES ===");
        tryActivate(env, "org.openrewrite.java.migrate.jakarta.JavaxValidationToJakartaValidation");
        tryActivate(env, "org.openrewrite.java.migrate.jakarta.JavaxMigrationToJakarta");
        tryActivate(env, "org.openrewrite.java.migrate.jakarta.JavaxValidationMigrationToJakartaValidation");

    }

    private void tryActivate(Environment env, String name) {
        try {
            env.activateRecipes(name);
            System.out.println("SUCCESS ACTIVATING: " + name);
        } catch (Exception e) {
            System.out.println("FAILED ACTIVATING: " + name + " - " + e.getMessage());
        }
    }
}
