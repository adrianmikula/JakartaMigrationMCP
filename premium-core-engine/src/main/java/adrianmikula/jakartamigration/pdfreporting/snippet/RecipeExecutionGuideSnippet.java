package adrianmikula.jakartamigration.pdfreporting.snippet;

import adrianmikula.jakartamigration.advancedscanning.service.ScanRecipeRecommendationService;

import java.util.List;

/**
 * HTML snippet for the OpenRewrite Recipe Execution Guide section.
 * Displays step-by-step commands for running available recipes.
 * 
 * References: docs/spec/html-refactoring-report-requirements.md Section 5
 */
public class RecipeExecutionGuideSnippet extends BaseHtmlSnippet {

    private final List<ScanRecipeRecommendationService.RecipeRecommendation> recipeRecommendations;

    public RecipeExecutionGuideSnippet(List<ScanRecipeRecommendationService.RecipeRecommendation> recipeRecommendations) {
        this.recipeRecommendations = recipeRecommendations;
    }

    @Override
    public String getSnippetName() {
        return "Recipe Execution Guide";
    }

    @Override
    public String generate() throws SnippetGenerationException {
        StringBuilder html = new StringBuilder();

        html.append("""
            <div class="section">
                <h2>OpenRewrite Recipe Execution Guide</h2>
                <p>Run these OpenRewrite recipes to automate your migration. Each recipe card includes the exact command to execute, expected outcomes, and prerequisites.</p>
                
                <div class="recipe-container">
            """);

        if (recipeRecommendations != null && !recipeRecommendations.isEmpty()) {
            int sequence = 1;
            for (ScanRecipeRecommendationService.RecipeRecommendation recommendation : recipeRecommendations) {
                if (recommendation == null || recommendation.recipe() == null) continue;

                var recipe = recommendation.recipe();
                String recipeName = escapeHtml(recipe.getName() != null ? recipe.getName() : "Migration Recipe " + sequence);
                String description = escapeHtml(recipe.getDescription() != null ? recipe.getDescription() : "Automated migration recipe");
                String command = generateMavenCommand(recipe.getOpenRewriteRecipeName(), recipeName);
                int expectedFiles = recommendation.affectedFiles() != null ? recommendation.affectedFiles().size() : 0;
                String prerequisites = "Maven 3.6+ installed<br>Project compiles successfully before migration";

                html.append(String.format("""
                        <div class="recipe-card">
                            <div class="recipe-header">
                                <span class="recipe-sequence">%d</span>
                                <span class="recipe-name">%s</span>
                            </div>
                            <div class="recipe-description">%s</div>
                            <div class="recipe-command-section">
                                <div class="command-label">Command to Run:</div>
                                <pre class="recipe-command">%s</pre>
                            </div>
                            <div class="recipe-meta">
                                <div class="meta-item">
                                    <span class="meta-label">Expected Files Affected:</span>
                                    <span class="meta-value">%d</span>
                                </div>
                            </div>
                            <div class="recipe-prerequisites">
                                <div class="prereq-label">Prerequisites:</div>
                                <ul class="prereq-list">
                                    <li>%s</li>
                                </ul>
                            </div>
                        </div>
                    """, sequence, recipeName, description, escapeHtml(command), expectedFiles, prerequisites));

                sequence++;
            }
        } else {
            // Default recipe cards if no recommendations available
            html.append("""
                        <div class="recipe-card">
                            <div class="recipe-header">
                                <span class="recipe-sequence">1</span>
                                <span class="recipe-name">Jakarta EE 9 Namespace Migration</span>
                            </div>
                            <div class="recipe-description">Updates all javax.* imports to jakarta.* namespace</div>
                            <div class="recipe-command-section">
                                <div class="command-label">Command to Run (Maven):</div>
                                <pre class="recipe-command">mvn org.openrewrite.maven:rewrite-maven-plugin:run \
  -Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-migrate-java:LATEST \
  -Drewrite.activeRecipes=org.openrewrite.java.migrate.jakarta.JavaxMigrationToJakarta</pre>
                                <div class="command-label">Command to Run (Gradle):</div>
                                <pre class="recipe-command">gradle rewriteRun --activeRecipe=org.openrewrite.java.migrate.jakarta.JavaxMigrationToJakarta</pre>
                            </div>
                            <div class="recipe-meta">
                                <div class="meta-item">
                                    <span class="meta-label">Expected Files Affected:</span>
                                    <span class="meta-value">All files with javax imports</span>
                                </div>
                            </div>
                            <div class="recipe-prerequisites">
                                <div class="prereq-label">Prerequisites:</div>
                                <ul class="prereq-list">
                                    <li>Maven 3.6+ or Gradle 7+ installed</li>
                                    <li>Project compiles successfully before migration</li>
                                    <li>Backup of project created</li>
                                </ul>
                            </div>
                        </div>
                    """);
        }

        html.append("""
                </div>
            </div>
            """);

        return html.toString();
    }

    @Override
    public boolean isApplicable() {
        return true; // Always show recipe guidance even if empty
    }

    private String generateMavenCommand(String openRewriteRecipeName, String fallbackName) {
        String recipeId = openRewriteRecipeName != null ? openRewriteRecipeName : fallbackName.replace(" ", "");
        return String.format("mvn org.openrewrite.maven:rewrite-maven-plugin:run -Drewrite.activeRecipes=%s", recipeId);
    }

    private String generateDefaultCommand(String recipeName) {
        return String.format("mvn org.openrewrite.maven:rewrite-maven-plugin:run -Drewrite.activeRecipes=%s",
            recipeName.replace(" ", ""));
    }
}
