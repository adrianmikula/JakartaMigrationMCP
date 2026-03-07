package adrianmikula.jakartamigration.coderefactoring.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
public class RecipeConfigLoader {
    
    private static RecipeConfigLoader instance;
    private final Map<String, RecipeConfig> recipeConfigs;
    
    private RecipeConfigLoader() {
        this.recipeConfigs = loadRecipeConfigs();
    }
    
    public static synchronized RecipeConfigLoader getInstance() {
        if (instance == null) {
            instance = new RecipeConfigLoader();
        }
        return instance;
    }
    
    private Map<String, RecipeConfig> loadRecipeConfigs() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("recipes.yaml")) {
            if (is == null) {
                log.warn("RecipeConfigLoader: recipes.yaml not found in classpath, using empty config");
                return Collections.emptyMap();
            }
            
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            RecipeConfigRoot root = mapper.readValue(is, RecipeConfigRoot.class);
            
            if (root == null || root.getRecipes() == null) {
                log.warn("RecipeConfigLoader: No recipes found in YAML");
                return Collections.emptyMap();
            }
            
            Map<String, RecipeConfig> configs = new java.util.HashMap<>();
            for (RecipeConfig config : root.getRecipes()) {
                if (config.getName() != null) {
                    configs.put(config.getName(), config);
                    log.debug("RecipeConfigLoader: Loaded config for recipe: {}", config.getName());
                }
            }
            
            log.info("RecipeConfigLoader: Loaded {} recipe configurations", configs.size());
            return configs;
            
        } catch (IOException e) {
            log.error("RecipeConfigLoader: Failed to load recipes.yaml", e);
            return Collections.emptyMap();
        }
    }
    
    public RecipeConfig getRecipeConfig(String recipeName) {
        return recipeConfigs.get(recipeName);
    }
    
    public Map<String, RecipeConfig> getAllRecipeConfigs() {
        return Collections.unmodifiableMap(recipeConfigs);
    }
    
    public boolean hasRecipeConfig(String recipeName) {
        return recipeConfigs.containsKey(recipeName);
    }
    
    public static class RecipeConfigRoot {
        private List<RecipeConfig> recipes;
        
        public List<RecipeConfig> getRecipes() {
            return recipes;
        }
        
        public void setRecipes(List<RecipeConfig> recipes) {
            this.recipes = recipes;
        }
    }
    
    public static class RecipeConfig {
        private String name;
        private String description;
        private String pattern;
        private String safety;
        private Boolean reversible;
        private String fileFilter;
        private List<Replacement> replacements;
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public String getPattern() {
            return pattern;
        }
        
        public void setPattern(String pattern) {
            this.pattern = pattern;
        }
        
        public String getSafety() {
            return safety;
        }
        
        public void setSafety(String safety) {
            this.safety = safety;
        }
        
        public Boolean getReversible() {
            return reversible;
        }
        
        public void setReversible(Boolean reversible) {
            this.reversible = reversible;
        }
        
        public String getFileFilter() {
            return fileFilter;
        }
        
        public void setFileFilter(String fileFilter) {
            this.fileFilter = fileFilter;
        }
        
        public List<Replacement> getReplacements() {
            return replacements;
        }
        
        public void setReplacements(List<Replacement> replacements) {
            this.replacements = replacements;
        }
    }
    
    public static class Replacement {
        private String from;
        private String to;
        
        public String getFrom() {
            return from;
        }
        
        public void setFrom(String from) {
            this.from = from;
        }
        
        public String getTo() {
            return to;
        }
        
        public void setTo(String to) {
            this.to = to;
        }
    }
}
