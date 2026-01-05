package com.bugbounty.jakartamigration.coderefactoring.service;

import com.bugbounty.jakartamigration.coderefactoring.domain.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Core refactoring engine that applies recipes to files.
 * This is a simplified implementation that can be extended with full OpenRewrite integration.
 */
public class RefactoringEngine {
    
    private static final Pattern JAVAX_IMPORT_PATTERN = Pattern.compile(
        "import\\s+javax\\.(servlet|persistence|ejb|validation|ws|xml|annotation|transaction|enterprise|decorator|interceptor|security|batch|connector|jms|json|mail|management|messaging|resource|sql|ws\\.rs)\\."
    );
    
    private static final Pattern JAVAX_PACKAGE_PATTERN = Pattern.compile(
        "package\\s+javax\\.(servlet|persistence|ejb|validation|ws|xml|annotation|transaction|enterprise|decorator|interceptor|security|batch|connector|jms|json|mail|management|messaging|resource|sql|ws\\.rs)\\."
    );
    
    /**
     * Refactors a single file by applying the given recipes.
     *
     * @param filePath Path to the file to refactor
     * @param recipes Recipes to apply
     * @return Refactoring changes
     */
    public RefactoringChanges refactorFile(Path filePath, List<Recipe> recipes) throws IOException {
        if (filePath == null) {
            throw new IllegalArgumentException("FilePath cannot be null");
        }
        if (recipes == null || recipes.isEmpty()) {
            throw new IllegalArgumentException("Recipes cannot be null or empty");
        }
        if (!Files.exists(filePath)) {
            throw new IOException("File does not exist: " + filePath);
        }
        
        String originalContent = Files.readString(filePath);
        String refactoredContent = originalContent;
        List<ChangeDetail> changes = new ArrayList<>();
        
        // Apply each recipe
        for (Recipe recipe : recipes) {
            RefactoringChanges recipeChanges = applyRecipe(refactoredContent, recipe, filePath.toString());
            refactoredContent = recipeChanges.refactoredContent();
            changes.addAll(recipeChanges.changes());
        }
        
        return new RefactoringChanges(
            filePath.toString(),
            originalContent,
            refactoredContent,
            changes,
            recipes
        );
    }
    
    /**
     * Applies a single recipe to content.
     */
    private RefactoringChanges applyRecipe(String content, Recipe recipe, String filePath) {
        String refactoredContent = content;
        List<ChangeDetail> changes = new ArrayList<>();
        
        // Apply Jakarta namespace recipe
        if ("AddJakartaNamespace".equals(recipe.name())) {
            String[] lines = content.split("\n");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                String originalLine = line;
                
                // Replace imports
                if (JAVAX_IMPORT_PATTERN.matcher(line).find()) {
                    line = line.replace("import javax.", "import jakarta.");
                    if (!line.equals(originalLine)) {
                        changes.add(new ChangeDetail(
                            i + 1,
                            originalLine,
                            line,
                            "Updated import from javax to jakarta",
                            ChangeType.IMPORT_CHANGE
                        ));
                    }
                }
                
                // Replace package declarations
                if (JAVAX_PACKAGE_PATTERN.matcher(line).find()) {
                    line = line.replace("package javax.", "package jakarta.");
                    if (!line.equals(originalLine)) {
                        changes.add(new ChangeDetail(
                            i + 1,
                            originalLine,
                            line,
                            "Updated package declaration from javax to jakarta",
                            ChangeType.PACKAGE_CHANGE
                        ));
                    }
                }
                
                // Replace type references (simple pattern matching)
                if (line.contains("javax.servlet") && !line.contains("import") && !line.contains("package")) {
                    line = line.replace("javax.servlet", "jakarta.servlet");
                    if (!line.equals(originalLine)) {
                        changes.add(new ChangeDetail(
                            i + 1,
                            originalLine,
                            line,
                            "Updated type reference from javax to jakarta",
                            ChangeType.TYPE_REFERENCE_CHANGE
                        ));
                    }
                }
                
                lines[i] = line;
            }
            refactoredContent = String.join("\n", lines);
        }
        
        // Apply persistence.xml recipe
        else if ("UpdatePersistenceXml".equals(recipe.name()) && filePath.contains("persistence.xml")) {
            refactoredContent = content.replace(
                "http://java.sun.com/xml/ns/persistence",
                "https://jakarta.ee/xml/ns/persistence"
            );
            if (!refactoredContent.equals(content)) {
                changes.add(new ChangeDetail(
                    1,
                    content.split("\n")[0],
                    refactoredContent.split("\n")[0],
                    "Updated persistence.xml namespace",
                    ChangeType.XML_NAMESPACE_CHANGE
                ));
            }
        }
        
        // Apply web.xml recipe
        else if ("UpdateWebXml".equals(recipe.name()) && filePath.contains("web.xml")) {
            refactoredContent = content.replace(
                "http://java.sun.com/xml/ns/javaee",
                "https://jakarta.ee/xml/ns/jakartaee"
            );
            if (!refactoredContent.equals(content)) {
                changes.add(new ChangeDetail(
                    1,
                    content.split("\n")[0],
                    refactoredContent.split("\n")[0],
                    "Updated web.xml namespace",
                    ChangeType.XML_NAMESPACE_CHANGE
                ));
            }
        }
        
        return new RefactoringChanges(
            filePath,
            content,
            refactoredContent,
            changes,
            List.of(recipe)
        );
    }
}

