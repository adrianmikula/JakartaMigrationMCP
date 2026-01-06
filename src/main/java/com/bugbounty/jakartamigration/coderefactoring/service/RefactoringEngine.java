package com.bugbounty.jakartamigration.coderefactoring.service;

import com.bugbounty.jakartamigration.coderefactoring.domain.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Core refactoring engine that applies recipes to files.
 * TODO: Integrate with OpenRewrite when Jakarta migration dependency is added.
 */
public class RefactoringEngine {
    
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
        String fileName = filePath.getFileName().toString();
        
        // Apply simple string-based refactoring (stub implementation)
        String refactoredContent = applyRecipes(originalContent, fileName, recipes);
        
        // Generate change details from the diff
        List<ChangeDetail> changes = generateChangeDetails(originalContent, refactoredContent);
        
        return new RefactoringChanges(
            filePath.toString(),
            originalContent,
            refactoredContent,
            changes,
            recipes
        );
    }
    
    /**
     * Applies recipes using simple string replacement (stub implementation).
     * TODO: Replace with OpenRewrite when dependency is added.
     */
    private String applyRecipes(String content, String fileName, List<Recipe> recipes) {
        String result = content;
        
        for (Recipe recipe : recipes) {
            result = applyRecipe(result, fileName, recipe);
        }
        
        return result;
    }
    
    /**
     * Applies a single recipe using string replacement.
     */
    private String applyRecipe(String content, String fileName, Recipe recipe) {
        String result = content;
        
        switch (recipe.name()) {
            case "AddJakartaNamespace":
                // Simple string replacement for javax -> jakarta
                result = result.replace("javax.servlet", "jakarta.servlet");
                result = result.replace("javax.persistence", "jakarta.persistence");
                result = result.replace("javax.validation", "jakarta.validation");
                result = result.replace("javax.annotation", "jakarta.annotation");
                result = result.replace("javax.inject", "jakarta.inject");
                result = result.replace("javax.ejb", "jakarta.ejb");
                result = result.replace("javax.transaction", "jakarta.transaction");
                result = result.replace("javax.enterprise", "jakarta.enterprise");
                result = result.replace("javax.ws.rs", "jakarta.ws.rs");
                result = result.replace("javax.json", "jakarta.json");
                result = result.replace("javax.xml.bind", "jakarta.xml.bind");
                result = result.replace("javax.xml.ws", "jakarta.xml.ws");
                break;
            case "UpdatePersistenceXml":
                if (fileName.contains("persistence.xml")) {
                    result = result.replace(
                        "http://java.sun.com/xml/ns/persistence",
                        "https://jakarta.ee/xml/ns/persistence"
                    );
                }
                break;
            case "UpdateWebXml":
                if (fileName.contains("web.xml")) {
                    result = result.replace(
                        "http://java.sun.com/xml/ns/javaee",
                        "https://jakarta.ee/xml/ns/jakartaee"
                    );
                }
                break;
        }
        
        return result;
    }
    
    /**
     * Generates change details by comparing original and refactored content.
     */
    private List<ChangeDetail> generateChangeDetails(String originalContent, String refactoredContent) {
        List<ChangeDetail> changes = new ArrayList<>();
        
        if (originalContent.equals(refactoredContent)) {
            return changes;
        }
        
        String[] originalLines = originalContent.split("\n");
        String[] refactoredLines = refactoredContent.split("\n");
        
        int maxLines = Math.max(originalLines.length, refactoredLines.length);
        
        for (int i = 0; i < maxLines; i++) {
            String originalLine = i < originalLines.length ? originalLines[i] : "";
            String refactoredLine = i < refactoredLines.length ? refactoredLines[i] : "";
            
            if (!originalLine.equals(refactoredLine)) {
                ChangeType changeType = determineChangeType(originalLine, refactoredLine);
                String description = generateChangeDescription(originalLine, refactoredLine, changeType);
                
                changes.add(new ChangeDetail(
                    i + 1,
                    originalLine,
                    refactoredLine,
                    description,
                    changeType
                ));
            }
        }
        
        return changes;
    }
    
    /**
     * Determines the type of change based on the line content.
     */
    private ChangeType determineChangeType(String originalLine, String refactoredLine) {
        if (originalLine.contains("import ") && refactoredLine.contains("import ")) {
            if (originalLine.contains("javax.") && refactoredLine.contains("jakarta.")) {
                return ChangeType.IMPORT_CHANGE;
            }
        }
        
        if (originalLine.contains("package ") && refactoredLine.contains("package ")) {
            if (originalLine.contains("javax.") && refactoredLine.contains("jakarta.")) {
                return ChangeType.PACKAGE_CHANGE;
            }
        }
        
        if (originalLine.contains("xmlns") || refactoredLine.contains("xmlns")) {
            return ChangeType.XML_NAMESPACE_CHANGE;
        }
        
        if (originalLine.contains("javax.") && refactoredLine.contains("jakarta.")) {
            return ChangeType.TYPE_REFERENCE_CHANGE;
        }
        
        return ChangeType.OTHER;
    }
    
    /**
     * Generates a human-readable description of the change.
     */
    private String generateChangeDescription(String originalLine, String refactoredLine, ChangeType changeType) {
        return switch (changeType) {
            case IMPORT_CHANGE -> "Updated import from javax to jakarta";
            case PACKAGE_CHANGE -> "Updated package declaration from javax to jakarta";
            case XML_NAMESPACE_CHANGE -> "Updated XML namespace to Jakarta";
            case TYPE_REFERENCE_CHANGE -> "Updated type reference from javax to jakarta";
            default -> "Applied refactoring change";
        };
    }
}
