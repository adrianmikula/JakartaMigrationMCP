package adrianmikula.jakartamigration.sourcecodescanning.service.impl;

import adrianmikula.jakartamigration.sourcecodescanning.domain.FileUsage;
import adrianmikula.jakartamigration.sourcecodescanning.domain.ImportStatement;
import adrianmikula.jakartamigration.sourcecodescanning.domain.SourceCodeAnalysisResult;
import adrianmikula.jakartamigration.sourcecodescanning.domain.XmlFileUsage;
import adrianmikula.jakartamigration.sourcecodescanning.service.SourceCodeScanner;
import lombok.extern.slf4j.Slf4j;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.CompilationUnit;
import org.openrewrite.SourceFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import adrianmikula.jakartamigration.util.ProjectFileSystemScanner;

/**
 * Simplified source code scanner implementation
 * Removes complex ThreadLocal management and over-engineering
 * Focuses on direct, reliable scanning
 */
@Slf4j
public class SimplifiedSourceCodeScannerImpl implements SourceCodeScanner {

    private final ProjectFileSystemScanner fileScanner = new ProjectFileSystemScanner();
    
    // Simple regex patterns for javax imports - no complex AST parsing needed
    private static final Pattern JAVAX_IMPORT_PATTERN = Pattern.compile("import\\s+javax\\.[a-zA-Z.]+");
    private static final Pattern JAKARTA_IMPORT_PATTERN = Pattern.compile("import\\s+jakarta\\.[a-zA-Z.]+");

    @Override
    public SourceCodeAnalysisResult scanProject(Path projectPath) {
        if (projectPath == null || !Files.exists(projectPath) || !Files.isDirectory(projectPath)) {
            log.warn("Invalid project path: {}", projectPath);
            return SourceCodeAnalysisResult.empty();
        }

        try {
            // Discover all Java files
            List<Path> javaFiles = discoverJavaFiles(projectPath);

            if (javaFiles.isEmpty()) {
                log.info("No Java files found in project: {}", projectPath);
                return SourceCodeAnalysisResult.empty();
            }

            log.info("Scanning {} Java files in project: {}", javaFiles.size(), projectPath);

            // Simple sequential scan - no complex parallel processing
            List<FileUsage> usages = new ArrayList<>();
            int totalJavaxImports = 0;

            for (Path file : javaFiles) {
                FileUsage usage = scanFile(file);
                if (usage != null && usage.hasJavaxUsage()) {
                    usages.add(usage);
                    totalJavaxImports += usage.getJavaxImportCount();
                    log.debug("Found javax usage in: {}", file);
                }
            }

            log.info("Scan complete: {} files scanned, {} files with javax usage, {} total javax imports",
                    javaFiles.size(), usages.size(), totalJavaxImports);

            return new SourceCodeAnalysisResult(
                    usages,
                    javaFiles.size(),
                    usages.size(),
                    totalJavaxImports);

        } catch (Exception e) {
            log.error("Error scanning project: {}", projectPath, e);
            return SourceCodeAnalysisResult.empty();
        }
    }

    @Override
    public FileUsage scanFile(Path filePath) {
        if (filePath == null) {
            throw new IllegalArgumentException("filePath cannot be null");
        }

        if (!Files.exists(filePath)) {
            log.warn("File does not exist: {}", filePath);
            return new FileUsage(filePath, List.of(), 0);
        }

        try {
            String content = Files.readString(filePath);
            int lineCount = countLines(content);

            // Simple regex-based import detection - no complex AST parsing
            List<ImportStatement> imports = new ArrayList<>();
            imports.addAll(extractJavaxImports(content));

            return new FileUsage(filePath, imports, lineCount);

        } catch (Exception e) {
            log.error("Error scanning file: {}", filePath, e);
            return new FileUsage(filePath, List.of(), 0);
        }
    }

    /**
     * Extracts javax.* imports from a compilation unit.
     */
    private List<ImportStatement> extractJavaxImports(String content) {
        return extractJavaxImports(null, content, 0);
    }
    
    /**
     * Extracts javax.* imports from a compilation unit with line tracking.
     */
    private List<ImportStatement> extractJavaxImports(CompilationUnit cu, String content, int startLine) {
        List<ImportStatement> imports = new ArrayList<>();
        
        Matcher javaxMatcher = JAVAX_IMPORT_PATTERN.matcher(content);
        while (javaxMatcher.find()) {
            imports.add(new ImportStatement(javaxMatcher.group(), "javax", "jakarta", startLine));
            startLine++;
        }
        
        Matcher jakartaMatcher = JAKARTA_IMPORT_PATTERN.matcher(content);
        while (jakartaMatcher.find()) {
            imports.add(new ImportStatement(jakartaMatcher.group(), "jakarta", "javax", startLine));
            startLine++;
        }
        
        return imports;
    }

    /**
     * Scans XML files for javax.* usage in namespaces and class references.
     * Simplified version - not implemented to keep things simple
     */
    public List<XmlFileUsage> scanXmlFiles(Path projectPath) {
        // Simplified implementation - return empty list for now
        return List.of();
    }
    
    /**
     * Simple line count
     */
    private int countLines(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        return content.split("\n").length;
    }

    /**
     * Discover Java files - reuse existing utility
     */
    private List<Path> discoverJavaFiles(Path projectPath) {
        return fileScanner.findFiles(projectPath, List.of(".java"));
    }
}
