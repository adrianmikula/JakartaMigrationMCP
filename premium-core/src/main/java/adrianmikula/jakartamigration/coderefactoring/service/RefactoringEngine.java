package adrianmikula.jakartamigration.coderefactoring.service;

import adrianmikula.jakartamigration.coderefactoring.domain.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Core refactoring engine that applies recipes to files.
 * 
 * IMPORTANT DISTINCTION FOR AI AGENTS:
 * 
 * - SIMPLE_STRING_REPLACEMENT & OPENREWRITE: Work on Java SOURCE CODE files (.java)
 *   → Modify source code directly
 *   → Use for: Refactoring source code from javax.* to jakarta.*
 * 
 * - APACHE_TOMCAT_MIGRATION: Works on COMPILED JAR/WAR files (bytecode), NOT source code
 *   → Creates new migrated JAR file (doesn't modify source)
 *   → Use for: Compatibility testing, bytecode validation, library assessment
 *   → Primary use cases:
 *     1. Test if compiled app works after Jakarta migration
 *     2. Cross-validate source migration completeness via bytecode diff
 *     3. Assess third-party library Jakarta compatibility
 * 
 * Supports multiple migration tools:
 * - Simple string replacement (default, fallback) - Source code
 * - Apache Tomcat Jakarta EE Migration Tool - Compiled JAR/WAR files (bytecode)
 * - OpenRewrite (when dependency is added) - Source code
 */
@Slf4j
public class RefactoringEngine {
    
    private final MigrationTool preferredTool;
    private final ApacheTomcatMigrationTool apacheTool;
    
    /**
     * Creates a refactoring engine with the default tool (simple string replacement).
     */
    public RefactoringEngine() {
        this(MigrationTool.SIMPLE_STRING_REPLACEMENT);
    }
    
    /**
     * Creates a refactoring engine with a preferred migration tool.
     *
     * @param preferredTool The preferred migration tool to use
     */
    public RefactoringEngine(MigrationTool preferredTool) {
        this.preferredTool = preferredTool;
        this.apacheTool = new ApacheTomcatMigrationTool();
    }
    
    /**
     * Creates a refactoring engine with a specific Apache Tomcat tool path.
     *
     * @param preferredTool The preferred migration tool to use
     * @param apacheToolJarPath Path to the Apache Tomcat migration tool JAR
     */
    public RefactoringEngine(MigrationTool preferredTool, Path apacheToolJarPath) {
        this.preferredTool = preferredTool;
        this.apacheTool = new ApacheTomcatMigrationTool(apacheToolJarPath);
    }
    
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
        
        // Determine which tool to use
        MigrationTool toolToUse = determineToolToUse();
        
        return switch (toolToUse) {
            case APACHE_TOMCAT_MIGRATION -> refactorWithApacheTool(filePath, recipes);
            case OPENREWRITE -> refactorWithOpenRewrite(filePath, recipes);
            case SIMPLE_STRING_REPLACEMENT -> refactorWithSimpleReplacement(filePath, recipes);
        };
    }
    
    /**
     * Determines which migration tool to use based on availability and preference.
     */
    private MigrationTool determineToolToUse() {
        // If Apache tool is preferred and available, use it
        if (preferredTool == MigrationTool.APACHE_TOMCAT_MIGRATION && apacheTool.isAvailable()) {
            return MigrationTool.APACHE_TOMCAT_MIGRATION;
        }
        
        // If OpenRewrite is preferred (and available in future), use it
        if (preferredTool == MigrationTool.OPENREWRITE) {
            // TODO: Check if OpenRewrite is available
            // For now, fall back to simple replacement
            return MigrationTool.SIMPLE_STRING_REPLACEMENT;
        }
        
        // Default to simple string replacement
        return MigrationTool.SIMPLE_STRING_REPLACEMENT;
    }
    
    /**
     * Refactors using the Apache Tomcat migration tool.
     */
    private RefactoringChanges refactorWithApacheTool(Path filePath, List<Recipe> recipes) throws IOException {
        // The Apache tool works on entire files/directories, not individual recipes
        // We'll create a temporary destination and then read the result
        
        Path tempDir = Files.createTempDirectory("jakarta-migration-");
        Path destinationPath = tempDir.resolve(filePath.getFileName());
        
        try {
            // Run the Apache migration tool
            ApacheTomcatMigrationTool.MigrationResult result = apacheTool.migrate(filePath, destinationPath);
            
            if (!result.success()) {
                throw new IOException("Apache migration tool failed: " + result.getSummary());
            }
            
            // Read the migrated content
            String originalContent = Files.readString(filePath);
            String refactoredContent = Files.readString(destinationPath);
            
            // Generate change details
            List<ChangeDetail> changes = generateChangeDetails(originalContent, refactoredContent);
            
            return new RefactoringChanges(
                filePath.toString(),
                originalContent,
                refactoredContent,
                changes,
                recipes
            );
            
        } finally {
            // Clean up temporary directory
            try {
                if (Files.exists(destinationPath)) {
                    Files.delete(destinationPath);
                }
                Files.delete(tempDir);
            } catch (IOException e) {
                // Log but don't fail - use logger instead of System.err to avoid corrupting MCP JSON stream
                log.warn("Failed to clean up temp directory: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Refactors using OpenRewrite (placeholder for future implementation).
     */
    private RefactoringChanges refactorWithOpenRewrite(Path filePath, List<Recipe> recipes) throws IOException {
        // TODO: Implement OpenRewrite integration when dependency is added
        // For now, fall back to simple replacement
        return refactorWithSimpleReplacement(filePath, recipes);
    }
    
    /**
     * Refactors using simple string replacement (fallback implementation).
     */
    private RefactoringChanges refactorWithSimpleReplacement(Path filePath, List<Recipe> recipes) throws IOException {
        String originalContent = Files.readString(filePath);
        String fileName = filePath.getFileName().toString();
        
        // Apply simple string-based refactoring
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
