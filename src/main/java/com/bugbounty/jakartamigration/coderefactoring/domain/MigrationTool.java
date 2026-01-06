package com.bugbounty.jakartamigration.coderefactoring.domain;

/**
 * Enumeration of available migration tools for Jakarta EE migration.
 * 
 * Each tool has specific use cases optimized for different migration scenarios.
 * Choose the tool based on whether you're working with source code or compiled artifacts.
 */
public enum MigrationTool {
    /**
     * Simple string-based refactoring for Java source code files.
     * 
     * USE CASE: Quick refactoring of individual Java source files (.java) from javax.* to jakarta.*
     * - Works on: Java source code files (.java)
     * - Best for: Simple migrations, quick tests, fallback when other tools unavailable
     * - Limitations: Basic string replacement, may miss edge cases, doesn't handle bytecode
     * 
     * When to use: When you need to refactor source code files directly and other tools aren't available.
     */
    SIMPLE_STRING_REPLACEMENT("Simple String Replacement - Source Code Refactoring"),
    
    /**
     * Apache Tomcat Jakarta EE Migration Tool - Works on COMPILED JAR files, not source code.
     * 
     * USE CASE: Migrating compiled Java applications (JAR/WAR files) from Java EE 8 to Jakarta EE 9.
     * This tool operates on bytecode, not source code.
     * 
     * Primary Use Cases:
     * 1. Compatibility Testing: Test if a compiled application will work after Jakarta migration
     *    - Migrate a JAR/WAR file to Jakarta namespace
     *    - Run the migrated application to identify compatibility issues
     *    - Discover libraries that aren't Jakarta-compatible and need replacement
     * 
     * 2. Bytecode Analysis & Cross-Validation: Compare bytecode changes with our analysis
     *    - Perform bytecode diff between original and migrated JAR
     *    - Cross-check against our own bytecode analysis to ensure nothing was missed
     *    - Validate that all javax.* references were properly migrated
     * 
     * 3. Third-Party Library Assessment: Check if dependencies are Jakarta-compatible
     *    - Migrate JARs containing third-party libraries
     *    - Identify which libraries have compatibility issues
     *    - Determine which libraries need to be replaced with Jakarta alternatives
     * 
     * Works on: Compiled JAR files, WAR files, EAR files (bytecode level)
     * Does NOT work on: Java source code files (.java)
     * 
     * What it migrates in bytecode:
     * - Package references (javax.* → jakarta.*)
     * - String constants containing javax.* references
     * - Configuration files embedded in JARs (web.xml, persistence.xml, etc.)
     * - JSP files and TLD files
     * - All Java EE 8 packages to Jakarta EE 9 equivalents
     * 
     * Important: This tool removes cryptographic signatures from JAR files as the bytecode changes
     * invalidate the signatures. This is expected behavior.
     * 
     * @see <a href="https://github.com/apache/tomcat-jakartaee-migration">Apache Tomcat Migration Tool</a>
     */
    APACHE_TOMCAT_MIGRATION("Apache Tomcat Migration Tool - JAR/WAR Bytecode Migration"),
    
    /**
     * OpenRewrite migration recipes for Java source code.
     * 
     * USE CASE: AST-based refactoring of Java source code from javax.* to jakarta.*
     * - Works on: Java source code files (.java), XML configuration files
     * - Best for: Production source code migrations, comprehensive refactoring
     * - Advantages: AST-based (more accurate), handles complex cases, extensible
     * 
     * When to use: When you need to refactor source code with high accuracy and handle complex patterns.
     * 
     * @see <a href="https://docs.openrewrite.org">OpenRewrite</a>
     */
    OPENREWRITE("OpenRewrite - AST-Based Source Code Refactoring");
    
    private final String displayName;
    
    MigrationTool(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

