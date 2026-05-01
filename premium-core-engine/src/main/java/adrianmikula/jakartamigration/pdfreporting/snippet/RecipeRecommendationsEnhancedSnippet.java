package adrianmikula.jakartamigration.pdfreporting.snippet;

import adrianmikula.jakartamigration.advancedscanning.service.ScanRecipeRecommendationService.RecipeRecommendation;

import java.util.List;

/**
 * Displays real recipe recommendations with confidence scores from ScanRecipeRecommendationService.
 * All data sourced from actual recipe recommendations - no generic descriptions.
 */
public class RecipeRecommendationsEnhancedSnippet extends BaseHtmlSnippet {

    private final List<RecipeRecommendation> recipeRecommendations;

    public RecipeRecommendationsEnhancedSnippet(List<RecipeRecommendation> recipeRecommendations) {
        this.recipeRecommendations = recipeRecommendations;
    }

    @Override
    public String generate() throws SnippetGenerationException {
        if (recipeRecommendations == null || recipeRecommendations.isEmpty()) {
            return generateNoRecipesMessage();
        }

        return safelyFormat("""
            <div class="section recipe-recommendations-enhanced">
                <h2>Recipe Recommendations with Confidence Analysis</h2>
                <p>Real recipe recommendations based on actual scan findings with confidence scoring.</p>

                <div class="recommendations-summary">
                    <span class="summary-stat">Total Recommendations: %d</span>
                    <span class="summary-stat">High Confidence (>70%%): %d</span>
                    <span class="summary-stat">Medium Confidence (30-70%%): %d</span>
                </div>

                <div class="recommendations-table-container">
                    <table class="recommendations-table">
                        <thead>
                            <tr>
                                <th>Recipe</th>
                                <th>Category</th>
                                <th>Confidence</th>
                                <th>Status</th>
                                <th>Affected Files</th>
                                <th>Reason</th>
                            </tr>
                        </thead>
                        <tbody>
                            %s
                        </tbody>
                    </table>
                </div>
            </div>
            """,
            recipeRecommendations.size(),
            countByConfidenceRange(0.7, 1.0),
            countByConfidenceRange(0.3, 0.7),
            generateRecipeRows()
        );
    }

    private String generateRecipeRows() {
        StringBuilder rows = new StringBuilder();

        // Sort by confidence score descending
        List<RecipeRecommendation> sorted = recipeRecommendations.stream()
            .sorted((a, b) -> Double.compare(b.confidenceScore(), a.confidenceScore()))
            .toList();

        for (RecipeRecommendation rec : sorted) {
            Object recipe = rec.recipe(); // Use Object to avoid dependency on RecipeDefinition
            double confidence = rec.confidenceScore();
            String confidenceClass = getConfidenceClass(confidence);
            String confidencePercent = String.format("%.0f%%", confidence * 100);

            // Safely extract recipe properties using reflection or toString
            String recipeName = extractRecipeName(recipe);
            String categoryName = extractCategoryName(recipe);
            String statusLabel = extractStatusLabel(recipe);
            String statusClass = statusLabel.toLowerCase().replace(" ", "-").replace("_", "-");

            String affectedFilesList = generateAffectedFilesList(rec.affectedFiles());

            rows.append(String.format("""
                <tr class="confidence-%s">
                    <td class="recipe-name"><strong>%s</strong></td>
                    <td class="category"><span class="category-badge category-%s">%s</span></td>
                    <td class="confidence"><span class="confidence-badge %s">%s</span></td>
                    <td class="status"><span class="status-badge %s">%s</span></td>
                    <td class="affected-files">%s</td>
                    <td class="reason">%s</td>
                </tr>
                """,
                confidenceClass,
                escapeHtml(recipeName),
                categoryName.toLowerCase(),
                escapeHtml(categoryName),
                confidenceClass,
                confidencePercent,
                statusClass,
                statusLabel,
                affectedFilesList,
                escapeHtml(rec.reason())
            ));
        }

        return rows.toString();
    }

    private String extractRecipeName(Object recipe) {
        if (recipe == null) return "Unknown Recipe";
        try {
            // Try to call getName() method via reflection
            java.lang.reflect.Method getNameMethod = recipe.getClass().getMethod("getName");
            Object result = getNameMethod.invoke(recipe);
            return result != null ? result.toString() : "Unknown Recipe";
        } catch (Exception e) {
            // Fallback to toString or class name
            return recipe.toString();
        }
    }

    private String extractCategoryName(Object recipe) {
        if (recipe == null) return "General";
        try {
            java.lang.reflect.Method getCategoryMethod = recipe.getClass().getMethod("getCategory");
            Object category = getCategoryMethod.invoke(recipe);
            if (category != null) {
                java.lang.reflect.Method nameMethod = category.getClass().getMethod("name");
                Object name = nameMethod.invoke(category);
                return name != null ? name.toString() : "General";
            }
        } catch (Exception e) {
            // Ignore and return default
        }
        return "General";
    }

    private String extractStatusLabel(Object recipe) {
        if (recipe == null) return "Unknown";
        try {
            java.lang.reflect.Method getStatusMethod = recipe.getClass().getMethod("getStatus");
            Object status = getStatusMethod.invoke(recipe);
            if (status != null) {
                return formatStatusValue(status.toString());
            }
        } catch (Exception e) {
            // Ignore and return default
        }
        return "Unknown";
    }

    private String formatStatusValue(String status) {
        return switch (status) {
            case "NEVER_RUN" -> "Never Run";
            case "RUN_SUCCESS" -> "Success";
            case "RUN_FAILED" -> "Failed";
            case "RUN_UNDONE" -> "Undone";
            default -> status;
        };
    }

    private String generateAffectedFilesList(List<String> affectedFiles) {
        if (affectedFiles == null || affectedFiles.isEmpty()) {
            return "<em>No files specified</em>";
        }

        if (affectedFiles.size() == 1) {
            return escapeHtml(affectedFiles.get(0));
        }

        StringBuilder list = new StringBuilder("<ul class='affected-files-list'>");
        for (String file : affectedFiles) {
            list.append("<li>").append(escapeHtml(file)).append("</li>");
        }
        list.append("</ul>");
        return list.toString();
    }

    private String getConfidenceClass(double confidence) {
        if (confidence >= 0.7) return "high";
        if (confidence >= 0.3) return "medium";
        return "low";
    }

    private long countByConfidenceRange(double min, double max) {
        return recipeRecommendations.stream()
            .filter(r -> r.confidenceScore() >= min && r.confidenceScore() < max)
            .count();
    }

    private String generateNoRecipesMessage() {
        return """
            <div class="section recipe-recommendations-enhanced">
                <h2>Recipe Recommendations with Confidence Analysis</h2>
                <div class="no-data-message">
                    <p>No recipe recommendations available. Run advanced scans to generate recipe suggestions based on findings.</p>
                </div>
            </div>
            """;
    }

    @Override
    public boolean isApplicable() {
        return recipeRecommendations != null && !recipeRecommendations.isEmpty();
    }

    @Override
    public int getOrder() {
        return 70; // After library findings
    }
}
