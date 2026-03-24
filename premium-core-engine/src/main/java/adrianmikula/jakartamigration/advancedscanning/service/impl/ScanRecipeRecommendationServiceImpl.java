package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.service.ScanRecipeRecommendationService;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeCategory;
import adrianmikula.jakartamigration.coderefactoring.service.RecipeService;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of ScanRecipeRecommendationService.
 * Maps advanced scan findings to applicable refactor recipes.
 */
@Slf4j
public class ScanRecipeRecommendationServiceImpl implements ScanRecipeRecommendationService {

    private final RecipeService recipeService;
    
    // Mapping of scan types to recipe categories
    private static final Map<String, List<RecipeCategory>> SCAN_TO_RECIPE_MAPPING = new HashMap<>();
    
    static {
        SCAN_TO_RECIPE_MAPPING.put("JPA_ANNOTATION_SCANNER", List.of(RecipeCategory.DATABASE));
        SCAN_TO_RECIPE_MAPPING.put("BEAN_VALIDATION_SCANNER", List.of(RecipeCategory.DATABASE, RecipeCategory.CDI));
        SCAN_TO_RECIPE_MAPPING.put("SERVLET_JSP_SCANNER", List.of(RecipeCategory.WEB, RecipeCategory.CONFIGURATION));
        SCAN_TO_RECIPE_MAPPING.put("CDI_INJECTION_SCANNER", List.of(RecipeCategory.CDI));
        SCAN_TO_RECIPE_MAPPING.put("REST_SOAP_SCANNER", List.of(RecipeCategory.APIS));
        SCAN_TO_RECIPE_MAPPING.put("SECURITY_API_SCANNER", List.of(RecipeCategory.SECURITY));
        SCAN_TO_RECIPE_MAPPING.put("JMS_MESSAGING_SCANNER", List.of(RecipeCategory.SECURITY));
        SCAN_TO_RECIPE_MAPPING.put("BUILD_CONFIG_SCANNER", List.of(RecipeCategory.CONFIGURATION));
        SCAN_TO_RECIPE_MAPPING.put("CONFIG_FILE_SCANNER", List.of(RecipeCategory.CONFIGURATION));
        SCAN_TO_RECIPE_MAPPING.put("DEPRECATED_API_SCANNER", List.of(RecipeCategory.APIS));
        SCAN_TO_RECIPE_MAPPING.put("TRANSITIVE_DEPENDENCY_SCANNER", List.of(RecipeCategory.DATABASE));
        SCAN_TO_RECIPE_MAPPING.put("THIRD_PARTY_LIB_SCANNER", List.of(RecipeCategory.CONFIGURATION));
        SCAN_TO_RECIPE_MAPPING.put("UNIT_TEST_SCANNER", List.of(RecipeCategory.CONFIGURATION));
    }

    public ScanRecipeRecommendationServiceImpl(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @Override
    public List<RecipeRecommendation> getRecipeRecommendations(Path projectPath, 
            Map<String, Object> scanResults) {
        
        log.info("Generating recipe recommendations for project: {}", projectPath);
        
        List<RecipeRecommendation> recommendations = new ArrayList<>();
        
        for (Map.Entry<String, Object> entry : scanResults.entrySet()) {
            String scanType = entry.getKey();
            Object scanResult = entry.getValue();
            
            if (scanResult != null && hasIssues(scanResult)) {
                List<RecipeRecommendation> scanRecommendations = 
                    getRecommendationsForScanType(projectPath, scanType, scanResult);
                recommendations.addAll(scanRecommendations);
            }
        }
        
        // Sort by confidence score (highest first)
        recommendations.sort((a, b) -> Double.compare(b.confidenceScore(), a.confidenceScore()));
        
        log.info("Generated {} recipe recommendations", recommendations.size());
        return recommendations;
    }

    @Override
    public List<RecipeRecommendation> getRecipeRecommendationsForFile(Path filePath, 
            String scanType) {
        
        log.debug("Getting recipe recommendations for file: {} with scan type: {}", filePath, scanType);
        
        List<RecipeRecommendation> recommendations = new ArrayList<>();
        List<RecipeCategory> categories = SCAN_TO_RECIPE_MAPPING.get(scanType);
        
        if (categories != null) {
            for (RecipeCategory category : categories) {
                List<RecipeDefinition> recipes = recipeService.getRecipesByCategory(category, filePath.getParent());
                
                for (RecipeDefinition recipe : recipes) {
                    double confidence = calculateConfidence(scanType, recipe, filePath);
                    if (confidence > 0.3) { // Only include recommendations with reasonable confidence
                        recommendations.add(new RecipeRecommendation(
                            recipe,
                            confidence,
                            getRecommendationReason(scanType, recipe),
                            List.of(filePath.toString())
                        ));
                    }
                }
            }
        }
        
        return recommendations;
    }

    @Override
    public Map<String, List<RecipeDefinition>> getRecipesByScanType() {
        Map<String, List<RecipeDefinition>> result = new HashMap<>();
        
        // Get all recipes once to avoid multiple calls
        List<RecipeDefinition> allRecipes = recipeService.getRecipes(Path.of("."));
        
        // Group recipes by category
        Map<RecipeCategory, List<RecipeDefinition>> recipesByCategory = allRecipes.stream()
            .collect(Collectors.groupingBy(RecipeDefinition::getCategory));
        
        // Map scan types to recipe categories
        for (Map.Entry<String, List<RecipeCategory>> entry : SCAN_TO_RECIPE_MAPPING.entrySet()) {
            String scanType = entry.getKey();
            List<RecipeCategory> categories = entry.getValue();
            
            List<RecipeDefinition> applicableRecipes = new ArrayList<>();
            for (RecipeCategory category : categories) {
                List<RecipeDefinition> categoryRecipes = recipesByCategory.get(category);
                if (categoryRecipes != null) {
                    applicableRecipes.addAll(categoryRecipes);
                }
            }
            
            result.put(scanType, applicableRecipes);
        }
        
        return result;
    }

    private List<RecipeRecommendation> getRecommendationsForScanType(Path projectPath, 
            String scanType, Object scanResult) {
        
        List<RecipeRecommendation> recommendations = new ArrayList<>();
        List<RecipeCategory> categories = SCAN_TO_RECIPE_MAPPING.get(scanType);
        
        if (categories != null) {
            for (RecipeCategory category : categories) {
                List<RecipeDefinition> recipes = recipeService.getRecipesByCategory(category, projectPath);
                
                for (RecipeDefinition recipe : recipes) {
                    double confidence = calculateConfidence(scanType, recipe, projectPath);
                    List<String> affectedFiles = extractAffectedFiles(scanResult);
                    
                    if (confidence > 0.3 && !affectedFiles.isEmpty()) {
                        recommendations.add(new RecipeRecommendation(
                            recipe,
                            confidence,
                            getRecommendationReason(scanType, recipe),
                            affectedFiles
                        ));
                    }
                }
            }
        }
        
        return recommendations;
    }

    private boolean hasIssues(Object scanResult) {
        // Check if scan result contains any issues
        // This is a simplified implementation - in reality, we'd check specific result types
        try {
            // Use reflection to check if the result has a method indicating issues found
            return scanResult.toString().contains("javax.") || 
                   scanResult.toString().contains("deprecated") ||
                   scanResult.toString().length() > 100; // Heuristic for non-empty results
        } catch (Exception e) {
            log.debug("Could not determine if scan result has issues: {}", e.getMessage());
            return false;
        }
    }

    private double calculateConfidence(String scanType, RecipeDefinition recipe, Path context) {
        // Base confidence based on scan type to recipe mapping
        double baseConfidence = 0.5;
        
        // Increase confidence for exact namespace matches
        if (recipe.getName().contains("Javax") && scanType.contains("ANNOTATION")) {
            baseConfidence += 0.3;
        }
        
        // Increase confidence for specific mappings
        if (isExactMatch(scanType, recipe)) {
            baseConfidence += 0.2;
        }
        
        // Cap at 1.0
        return Math.min(baseConfidence, 1.0);
    }

    private boolean isExactMatch(String scanType, RecipeDefinition recipe) {
        // Define exact mappings between scan types and recipes
        return switch (scanType) {
            case "JPA_ANNOTATION_SCANNER" -> recipe.getName().equals("MigrateJPA") || 
                                             recipe.getName().equals("UpdatePersistenceXml");
            case "BEAN_VALIDATION_SCANNER" -> recipe.getName().contains("Validation");
            case "SERVLET_JSP_SCANNER" -> recipe.getName().contains("Servlet") || 
                                         recipe.getName().equals("UpdateWebXml");
            case "CDI_INJECTION_SCANNER" -> recipe.getName().contains("CDI");
            case "REST_SOAP_SCANNER" -> recipe.getName().contains("REST") || 
                                         recipe.getName().contains("SOAP");
            case "SECURITY_API_SCANNER" -> recipe.getName().contains("JMS");
            default -> false;
        };
    }

    private String getRecommendationReason(String scanType, RecipeDefinition recipe) {
        return switch (scanType) {
            case "JPA_ANNOTATION_SCANNER" -> "Found JPA annotations that can be migrated using " + recipe.getName();
            case "BEAN_VALIDATION_SCANNER" -> "Found Bean Validation annotations that can be migrated using " + recipe.getName();
            case "SERVLET_JSP_SCANNER" -> "Found Servlet/JSP usage that can be migrated using " + recipe.getName();
            case "CDI_INJECTION_SCANNER" -> "Found CDI injection usage that can be migrated using " + recipe.getName();
            case "REST_SOAP_SCANNER" -> "Found REST/SOAP API usage that can be migrated using " + recipe.getName();
            case "SECURITY_API_SCANNER" -> "Found Security API usage that can be migrated using " + recipe.getName();
            default -> "Found issues that can be addressed by " + recipe.getName();
        };
    }

    @SuppressWarnings("unchecked")
    private List<String> extractAffectedFiles(Object scanResult) {
        try {
            // Try to extract file paths from scan result
            // This is a simplified implementation
            if (scanResult.toString().contains("fileResults")) {
                // Parse file results from toString representation
                String resultStr = scanResult.toString();
                List<String> files = new ArrayList<>();
                
                // Simple regex to extract file paths
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("([a-zA-Z]:\\\\[^,\\]]+)");
                java.util.regex.Matcher matcher = pattern.matcher(resultStr);
                
                while (matcher.find()) {
                    files.add(matcher.group(1));
                }
                
                return files;
            }
        } catch (Exception e) {
            log.debug("Could not extract affected files from scan result: {}", e.getMessage());
        }
        
        return List.of(); // Return empty list if we can't extract files
    }
}
