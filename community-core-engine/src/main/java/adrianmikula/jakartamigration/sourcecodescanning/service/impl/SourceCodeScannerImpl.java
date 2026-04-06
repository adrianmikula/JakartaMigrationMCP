package adrianmikula.jakartamigration.sourcecodescanning.service.impl;

import adrianmikula.jakartamigration.sourcecodescanning.domain.FileUsage;
import adrianmikula.jakartamigration.sourcecodescanning.domain.ImportStatement;
import adrianmikula.jakartamigration.sourcecodescanning.domain.SourceCodeAnalysisResult;
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
import java.util.stream.Collectors;

import adrianmikula.jakartamigration.util.ProjectFileSystemScanner;

/**
 * Implementation of SourceCodeScanner using OpenRewrite JavaParser.
 * Provides fast, AST-based scanning for javax.* usage in Java source files.
 */
@Slf4j
public class SourceCodeScannerImpl implements SourceCodeScanner {

    // Use ThreadLocal to avoid JavaParser reset() issues when parsing files
    // with the same fully qualified names in parallel
    private final ThreadLocal<JavaParser> javaParserThreadLocal = ThreadLocal
            .withInitial(() -> JavaParser.fromJavaVersion().build());

    private final ProjectFileSystemScanner fileScanner = new ProjectFileSystemScanner();

    @Deprecated
    private final JavaParser javaParser; // Kept for backward compatibility, not used

    public SourceCodeScannerImpl() {
        // Initialize the deprecated field (not actually used)
        this.javaParser = JavaParser.fromJavaVersion().build();
    }

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

            // Scan files in parallel
            AtomicInteger totalScanned = new AtomicInteger(0);
            List<FileUsage> usages = javaFiles.parallelStream()
                    .map(file -> {
                        totalScanned.incrementAndGet();
                        FileUsage usage = scanFile(file);
                        if (usage.hasJavaxUsage()) {
                            log.info("Found javax usage in: {}", file);
                            return usage;
                        }
                        return null;
                    })
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());

            int totalJavaxImports = usages.stream()
                    .mapToInt(FileUsage::getJavaxImportCount)
                    .sum();

            log.info("Scan complete: {} files scanned, {} files with javax usage, {} total javax imports",
                    totalScanned.get(), usages.size(), totalJavaxImports);

            return new SourceCodeAnalysisResult(
                    usages,
                    totalScanned.get(),
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
            // Use streaming for memory efficiency with large files
            long fileSize = Files.size(filePath);
            String content;
            
            // For files larger than 10MB, use streaming approach
            if (fileSize > 10 * 1024 * 1024) {
                log.debug("Large file detected ({} bytes), using streaming for: {}", fileSize, filePath);
                content = Files.lines(filePath).collect(java.util.stream.Collectors.joining("\n"));
            } else {
                // For smaller files, regular readString is more efficient
                content = Files.readString(filePath);
            }
            
            int lineCount = countLines(content);

            // Use ThreadLocal parser to avoid reset() issues when parsing files
            // with the same fully qualified names in parallel
            // Each thread gets its own parser instance, avoiding conflicts
            JavaParser parser = javaParserThreadLocal.get();

            // Reset parser before parsing to avoid IllegalStateException when
            // parsing files with duplicate fully qualified names
            parser.reset();

            // Parse with OpenRewrite - using try-with-resources pattern
            List<SourceFile> sourceFiles;
            try (var stream = parser.parse(content)) {
                sourceFiles = stream.collect(java.util.stream.Collectors.toList());
            }

            if (sourceFiles.isEmpty()) {
                log.debug("No source files found in file: {}", filePath);
                return new FileUsage(filePath, List.of(), lineCount);
            }

            // Extract javax imports from all compilation units
            List<ImportStatement> imports = new ArrayList<>();
            for (SourceFile sourceFile : sourceFiles) {
                if (sourceFile instanceof CompilationUnit) {
                    CompilationUnit cu = (CompilationUnit) sourceFile;
                    imports.addAll(extractJavaxImports(cu, content));
                }
            }

            return new FileUsage(filePath, imports, lineCount);

        } catch (Exception e) {
            log.warn("Error scanning file: {}", filePath, e);
            return new FileUsage(filePath, List.of(), 0);
        }
    }

    /**
     * Discovers all Java files in the project efficiently.
     */
    private List<Path> discoverJavaFiles(Path projectPath) {
        return fileScanner.findFiles(projectPath, List.of(".java"));
    }

    /**
     * Extracts javax.* imports from a compilation unit.
     */
    private List<ImportStatement> extractJavaxImports(CompilationUnit cu, String content) {
        List<ImportStatement> imports = new ArrayList<>();

        List<J.Import> importDeclarations = cu.getImports();
        String[] lines = content.split("\n");

        for (J.Import imp : importDeclarations) {
            String importName = imp.getQualid().toString();

            if (importName.startsWith("javax.")) {
                String jakartaEquivalent = importName.replace("javax.", "jakarta.");

                // Find line number by searching for the import in content
                int lineNumber = findLineNumberInContent(lines, importName);

                // Extract package name (e.g., "javax.servlet" from
                // "javax.servlet.ServletException")
                String javaxPackage = extractPackageName(importName);

                imports.add(new ImportStatement(
                        importName,
                        javaxPackage,
                        jakartaEquivalent,
                        lineNumber));
            }
        }

        return imports;
    }

    /**
     * Finds the line number of a string in the content.
     */
    private int findLineNumberInContent(String[] lines, String searchText) {
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains(searchText)) {
                return i + 1; // Line numbers are 1-based
            }
        }
        return 1; // Default to line 1 if not found
    }

    /**
     * Extracts package name from fully qualified class name.
     * Example: "javax.servlet.ServletException" -> "javax.servlet"
     */
    private String extractPackageName(String fullyQualifiedName) {
        int lastDot = fullyQualifiedName.lastIndexOf('.');
        if (lastDot > 0) {
            return fullyQualifiedName.substring(0, lastDot);
        }
        return fullyQualifiedName;
    }

    /**
     * Counts lines in content.
     */
    private int countLines(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        return content.split("\n").length;
    }

    @Override
    public List<adrianmikula.jakartamigration.sourcecodescanning.domain.XmlFileUsage> scanXmlFiles(Path projectPath) {
        if (projectPath == null || !Files.exists(projectPath) || !Files.isDirectory(projectPath)) {
            log.warn("Invalid project path: {}", projectPath);
            return List.of();
        }

        List<adrianmikula.jakartamigration.sourcecodescanning.domain.XmlFileUsage> xmlUsages = new ArrayList<>();

        try {
            // Discover XML files
            List<Path> xmlFiles = discoverXmlFiles(projectPath);

            log.info("Scanning {} XML files in project: {}", xmlFiles.size(), projectPath);

            // Scan XML files
            xmlFiles.parallelStream()
                    .forEach(file -> {
                        try {
                            adrianmikula.jakartamigration.sourcecodescanning.domain.XmlFileUsage usage = scanXmlFile(
                                    file);
                            if (usage.hasJavaxUsage()) {
                                synchronized (xmlUsages) {
                                    xmlUsages.add(usage);
                                }
                            }
                        } catch (Exception e) {
                            log.warn("Error scanning XML file: {}", file, e);
                        }
                    });

        } catch (Exception e) {
            log.error("Error scanning XML files in project: {}", projectPath, e);
        }

        return xmlUsages;
    }

    private List<Path> discoverXmlFiles(Path projectPath) {
        return fileScanner.findFiles(projectPath, List.of(".xml"));
    }

    private adrianmikula.jakartamigration.sourcecodescanning.domain.XmlFileUsage scanXmlFile(Path xmlFile) {
        try {
            String content = Files.readString(xmlFile);
            String[] lines = content.split("\n");

            List<adrianmikula.jakartamigration.sourcecodescanning.domain.XmlFileUsage.XmlNamespaceUsage> namespaceUsages = extractXmlNamespaceUsages(
                    content, lines);
            List<adrianmikula.jakartamigration.sourcecodescanning.domain.XmlFileUsage.XmlClassReference> classReferences = extractXmlClassReferences(
                    content, lines);

            return new adrianmikula.jakartamigration.sourcecodescanning.domain.XmlFileUsage(
                    xmlFile,
                    namespaceUsages,
                    classReferences);
        } catch (Exception e) {
            log.warn("Error scanning XML file: {}", xmlFile, e);
            return new adrianmikula.jakartamigration.sourcecodescanning.domain.XmlFileUsage(
                    xmlFile,
                    List.of(),
                    List.of());
        }
    }

    private List<adrianmikula.jakartamigration.sourcecodescanning.domain.XmlFileUsage.XmlNamespaceUsage> extractXmlNamespaceUsages(
            String content, String[] lines) {
        List<adrianmikula.jakartamigration.sourcecodescanning.domain.XmlFileUsage.XmlNamespaceUsage> usages = new ArrayList<>();

        // Match javax namespace URIs
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "xmlns(?::\\w+)?\\s*=\\s*[\"'](http://java\\.sun\\.com/xml/ns/[^\"']+)[\"']");
        java.util.regex.Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String namespaceUri = matcher.group(1);
            String jakartaEquivalent = namespaceUri
                    .replace("http://java.sun.com/xml/ns/javaee", "https://jakarta.ee/xml/ns/jakartaee")
                    .replace("http://java.sun.com/xml/ns/persistence", "https://jakarta.ee/xml/ns/persistence");

            int lineNumber = findLineNumberInContent(lines, matcher.group(0));

            usages.add(new adrianmikula.jakartamigration.sourcecodescanning.domain.XmlFileUsage.XmlNamespaceUsage(
                    namespaceUri,
                    jakartaEquivalent,
                    lineNumber));
        }

        return usages;
    }

    private List<adrianmikula.jakartamigration.sourcecodescanning.domain.XmlFileUsage.XmlClassReference> extractXmlClassReferences(
            String content, String[] lines) {
        List<adrianmikula.jakartamigration.sourcecodescanning.domain.XmlFileUsage.XmlClassReference> references = new ArrayList<>();

        // Match javax class names in XML (e.g.,
        // <servlet-class>javax.servlet.Servlet</servlet-class>)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "<([^>]+)>(javax\\.[\\w.]+)</([^>]+)>");
        java.util.regex.Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String elementName = matcher.group(1);
            String className = matcher.group(2);
            String jakartaEquivalent = className.replace("javax.", "jakarta.");

            int lineNumber = findLineNumberInContent(lines, matcher.group(0));

            references.add(new adrianmikula.jakartamigration.sourcecodescanning.domain.XmlFileUsage.XmlClassReference(
                    className,
                    jakartaEquivalent,
                    elementName,
                    lineNumber));
        }

        return references;
    }

}
