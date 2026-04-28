package adrianmikula.jakartamigration.pdfreporting.snippet;

import adrianmikula.jakartamigration.advancedscanning.service.ScanRecipeRecommendationService;

import java.util.List;

/**
 * HTML snippet for the Migration Checklist section.
 * Displays a phased action plan for developers to follow.
 * 
 * References: docs/spec/html-refactoring-report-requirements.md Section 3
 */
public class MigrationChecklistSnippet extends BaseHtmlSnippet {

    private final List<ScanRecipeRecommendationService.RecipeRecommendation> recipeRecommendations;

    public MigrationChecklistSnippet(List<ScanRecipeRecommendationService.RecipeRecommendation> recipeRecommendations) {
        this.recipeRecommendations = recipeRecommendations;
    }

    @Override
    public String getSnippetName() {
        return "Migration Checklist";
    }

    @Override
    public String generate() throws SnippetGenerationException {
        StringBuilder html = new StringBuilder();

        html.append("""
            <div class="section">
                <h2>Migration Checklist</h2>
                <p>Follow this phased action plan to complete your Jakarta EE migration. Each phase includes specific tasks with clear completion criteria.</p>
                
                <div class="checklist-container">
            """);

        // Phase 1: Preparation
        html.append("""
                    <div class="checklist-phase">
                        <div class="phase-header">
                            <span class="phase-number">1</span>
                            <span class="phase-title">Preparation</span>
                        </div>
                        <ul class="checklist-items">
                            <li><span class="checkbox">&#9744;</span> Backup your project before making changes</li>
                            <li><span class="checkbox">&#9744;</span> Review breaking changes documentation for Jakarta EE 9+</li>
                            <li><span class="checkbox">&#9744;</span> Ensure you have a Jakarta EE 9+ compatible runtime (Tomcat 10+, Jetty 11+, etc.)</li>
                            <li><span class="checkbox">&#9744;</span> Verify your build tool (Maven 3.6+ or Gradle 7+)</li>
                        </ul>
                    </div>
            """);

        // Phase 2: Recipe Execution
        html.append("                    <div class=\"checklist-phase\">\n");
        html.append("                        <div class=\"phase-header\">\n");
        html.append("                            <span class=\"phase-number\">2</span>\n");
        html.append("                            <span class=\"phase-title\">Recipe Execution</span>\n");
        html.append("                        </div>\n");
        html.append("                        <ul class=\"checklist-items\">\n");

        if (recipeRecommendations != null && !recipeRecommendations.isEmpty()) {
            for (ScanRecipeRecommendationService.RecipeRecommendation recommendation : recipeRecommendations) {
                if (recommendation == null || recommendation.recipe() == null) continue;
                String recipeName = recommendation.recipe().getName() != null
                    ? escapeHtml(recommendation.recipe().getName())
                    : "Recipe";
                html.append(String.format(
                    "                            <li><span class=\"checkbox\">&#9744;</span> Run recipe: %s</li>%n",
                    recipeName
                ));
            }
        } else {
            html.append("                            <li><span class=\"checkbox\">&#9744;</span> Run Jakarta EE 9 namespace migration recipe</li>\n");
            html.append("                            <li><span class=\"checkbox\">&#9744;</span> Run JPA entity annotation recipe (if applicable)</li>\n");
            html.append("                            <li><span class=\"checkbox\">&#9744;</span> Run Servlet API migration recipe (if applicable)</li>\n");
        }

        html.append("                        </ul>\n");
        html.append("                    </div>\n");

        // Phase 3: Manual Changes
        html.append("""
                    <div class="checklist-phase">
                        <div class="phase-header">
                            <span class="phase-number">3</span>
                            <span class="phase-title">Manual Changes</span>
                        </div>
                        <ul class="checklist-items">
                            <li><span class="checkbox">&#9744;</span> Update remaining javax imports not covered by recipes</li>
                            <li><span class="checkbox">&#9744;</span> Update configuration files (web.xml, application.properties)</li>
                            <li><span class="checkbox">&#9744;</span> Update dependency versions in pom.xml or build.gradle</li>
                            <li><span class="checkbox">&#9744;</span> Review and update any reflection-based code</li>
                        </ul>
                    </div>
            """);

        // Phase 4: Validation
        html.append("""
                    <div class="checklist-phase">
                        <div class="phase-header">
                            <span class="phase-number">4</span>
                            <span class="phase-title">Validation</span>
                        </div>
                        <ul class="checklist-items">
                            <li><span class="checkbox">&#9744;</span> Compile project successfully</li>
                            <li><span class="checkbox">&#9744;</span> Run all unit tests</li>
                            <li><span class="checkbox">&#9744;</span> Run integration tests</li>
                            <li><span class="checkbox">&#9744;</span> Deploy to test environment and verify startup</li>
                            <li><span class="checkbox">&#9744;</span> Test all critical application paths</li>
                        </ul>
                    </div>
                </div>
            </div>
            """);

        return html.toString();
    }

    @Override
    public boolean isApplicable() {
        return true; // Always applicable as this is a guidance section
    }
}
