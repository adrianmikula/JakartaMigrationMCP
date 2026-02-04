package adrianmikula.jakartamigration.sourcecodescanning.service.impl;

import adrianmikula.jakartamigration.sourcecodescanning.domain.FileUsage;
import adrianmikula.jakartamigration.sourcecodescanning.domain.ImportStatement;
import adrianmikula.jakartamigration.sourcecodescanning.domain.ReflectionUsage;
import adrianmikula.jakartamigration.sourcecodescanning.domain.SourceCodeAnalysisResult;
import adrianmikula.jakartamigration.sourcecodescanning.service.SourceCodeScanner;
import lombok.extern.slf4j.Slf4j;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.CompilationUnit;
import org.openrewrite.SourceFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of SourceCodeScanner using OpenRewrite JavaParser.
 * Provides fast, AST-based scanning for javax.* usage in Java source files.
 */
@Slf4j
public class SourceCodeScannerImpl implements SourceCodeScanner {
    
    // Use ThreadLocal to avoid JavaParser reset() issues when parsing files
    // with the same fully qualified names in parallel
    private final ThreadLocal<JavaParser> javaParserThreadLocal = ThreadLocal.withInitial(() ->
        JavaParser.fromJavaVersion().build()
    );
    
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
                        log.debug("Found javax usage in: {}", file);
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
                totalJavaxImports
            );
            
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
            
            // Use ThreadLocal parser to avoid reset() issues when parsing files
            // with the same fully qualified names in parallel
            // Each thread gets its own parser instance, avoiding conflicts
            JavaParser parser = javaParserThreadLocal.get();
            
            // Reset parser before parsing to avoid IllegalStateException when
            // parsing files with duplicate fully qualified names
            parser.reset();
            
            // Parse with OpenRewrite
            List<SourceFile> sourceFiles = parser.parse(content).collect(java.util.stream.Collectors.toList());
            
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
     * Discovers all Java files in the project, excluding build directories.
     */
    private List<Path> discoverJavaFiles(Path projectPath) {
        List<Path> javaFiles = new ArrayList<>();
        
        try (Stream<Path> paths = Files.walk(projectPath)) {
            paths
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java"))
                .filter(this::shouldScanFile)
                .forEach(javaFiles::add);
        } catch (IOException e) {
            log.error("Error discovering Java files in: {}", projectPath, e);
        }
        
        return javaFiles;
    }
    
    /**
     * Determines if a file should be scanned (excludes build directories only).
     * Note: Test files are included in scanning.
     */
    private boolean shouldScanFile(Path file) {
        String path = file.toString().replace('\\', '/');
        return !path.contains("/target/") &&
               !path.contains("/build/") &&
               !path.contains("/.git/") &&
               !path.contains("/node_modules/") &&
               !path.contains("/.gradle/") &&
               !path.contains("/.mvn/") &&
               !path.contains("/.idea/") &&
               !path.contains("/.vscode/") &&
               !path.contains("/out/") &&
               !path.contains("/bin/");
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
                
                // Extract package name (e.g., "javax.servlet" from "javax.servlet.ServletException")
                String javaxPackage = extractPackageName(importName);
                
                imports.add(new ImportStatement(
                    importName,
                    javaxPackage,
                    jakartaEquivalent,
                    lineNumber
                ));
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
                        adrianmikula.jakartamigration.sourcecodescanning.domain.XmlFileUsage usage = scanXmlFile(file);
                        if (usage.hasJavaxUsage()) {
                            synchronized(xmlUsages) {
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
        List<Path> xmlFiles = new ArrayList<>();
        
        try (Stream<Path> paths = Files.walk(projectPath)) {
            paths
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".xml"))
                .filter(this::shouldScanFile)
                .forEach(xmlFiles::add);
        } catch (IOException e) {
            log.error("Error discovering XML files in: {}", projectPath, e);
        }
        
        return xmlFiles;
    }
    
    private adrianmikula.jakartamigration.sourcecodescanning.domain.XmlFileUsage scanXmlFile(Path xmlFile) {
        try {
            String content = Files.readString(xmlFile);
            String[] lines = content.split("\n");
            
            List<adrianmikula.jakartamigration.sourcecodescanning.domain.XmlFileUsage.XmlNamespaceUsage> namespaceUsages = 
                extractXmlNamespaceUsages(content, lines);
            List<adrianmikula.jakartamigration.sourcecodescanning.domain.XmlFileUsage.XmlClassReference> classReferences = 
                extractXmlClassReferences(content, lines);
            
            return new adrianmikula.jakartamigration.sourcecodescanning.domain.XmlFileUsage(
                xmlFile,
                namespaceUsages,
                classReferences
            );
        } catch (Exception e) {
            log.warn("Error scanning XML file: {}", xmlFile, e);
            return new adrianmikula.jakartamigration.sourcecodescanning.domain.XmlFileUsage(
                xmlFile,
                List.of(),
                List.of()
            );
        }
    }
    
    private List<adrianmikula.jakartamigration.sourcecodescanning.domain.XmlFileUsage.XmlNamespaceUsage> extractXmlNamespaceUsages(
            String content, String[] lines) {
        List<adrianmikula.jakartamigration.sourcecodescanning.domain.XmlFileUsage.XmlNamespaceUsage> usages = new ArrayList<>();
        
        // Match javax namespace URIs
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "xmlns(?::\\w+)?\\s*=\\s*[\"'](http://java\\.sun\\.com/xml/ns/[^\"']+)[\"']"
        );
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
                lineNumber
            ));
        }
        
        return usages;
    }
    
    private List<adrianmikula.jakartamigration.sourcecodescanning.domain.XmlFileUsage.XmlClassReference> extractXmlClassReferences(
            String content, String[] lines) {
        List<adrianmikula.jakartamigration.sourcecodescanning.domain.XmlFileUsage.XmlClassReference> references = new ArrayList<>();
        
        // Match javax class names in XML (e.g., <servlet-class>javax.servlet.Servlet</servlet-class>)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "<([^>]+)>(javax\\.[\\w.]+)</([^>]+)>"
        );
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
                lineNumber
            ));
        }
        
        return references;
    }
    
    // ========== Reflection Detection Methods ==========
    
    private static final Pattern CLASS_FOR_NAME_PATTERN = Pattern.compile(
        "Class\\.forName\\s*\\(\\s*\\"([^\"]*javax\\.[^\"]*)\""
    );
    
    private static final Pattern CLASS_LOADER_PATTERN = Pattern.compile(
        "(?:loadClass|loadClass\\s*\\()\\s*\\"([^\"]*javax\\.[^\"]*)\""
    );
    
    private static final Pattern JNDI_PATTERN = Pattern.compile(
        "(?:lookup|listBindings)\\s*\\(\\s*\\"([^\"]*javax\\.[^\"]*)\""
    );
    
    private static final Pattern SPRING_BEAN_PATTERN = Pattern.compile(
        "@Bean\\s*(?:\\{[^}]*)?\\s*(?:name\\s*=\\s*)?\\"([^\"]*javax\\.[^\"]*)\""
    );
    
    private static final Pattern REFLECTION_UTILS_PATTERN = Pattern.compile(
        "(?:ReflectionUtils\\.|ClassUtils\\.).*\\(\\s*\\"([^\"]*javax\\.[^\"]*)\""
    );
    
    private static final Pattern ANNOTATION_VALUE_PATTERN = Pattern.compile(
        "@[A-Za-z0-9_]+\\([^)]*)\"([^\"]*javax\\.[^\"]*)\""
    );
    
    /**
     * Detects reflection-based javax references in the content.
     */
    public List<ReflectionUsage> detectReflectionUsages(String content, String[] lines) {
        List<ReflectionUsage> usages = new ArrayList<>();
        
        // Detect Class.forName() usages
        Matcher classForNameMatcher = CLASS_FOR_NAME_PATTERN.matcher(content);
        while (classForNameMatcher.find()) {
            String className = classForNameMatcher.group(1);
            int lineNumber = findLineNumberInContent(lines, classForNameMatcher.group(0));
            
            usages.add(new ReflectionUsage(
                className,
                className.replace("javax.", "jakarta."),
                ReflectionUsage.ReflectionType.CLASS_FOR_NAME,
                "Class.forName(\"" + className + "\")",
                lineNumber
            ));
        }
        
        // Detect class loader usages
        Matcher classLoaderMatcher = CLASS_LOADER_PATTERN.matcher(content);
        while (classLoaderMatcher.find()) {
            String className = classLoaderMatcher.group(1);
            int lineNumber = findLineNumberInContent(lines, classLoaderMatcher.group(0));
            
            usages.add(new ReflectionUsage(
                className,
                className.replace("javax.", "jakarta."),
                ReflectionUsage.ReflectionType.CLASS_LOADER,
                "loadClass(\"" + className + "\")",
                lineNumber
            ));
        }
        
        // Detect JNDI lookups
        Matcher jndiMatcher = JNDI_PATTERN.matcher(content);
        while (jndiMatcher.find()) {
            String lookupName = jndiMatcher.group(1);
            int lineNumber = findLineNumberInContent(lines, jndiMatcher.group(0));
            
            usages.add(new ReflectionUsage(
                lookupName,
                lookupName.replace("javax.", "jakarta."),
                ReflectionUsage.ReflectionType.JNDI_LOOKUP,
                "lookup(\"" + lookupName + "\")",
                lineNumber
            ));
        }
        
        // Detect Spring @Bean annotations
        Matcher springBeanMatcher = SPRING_BEAN_PATTERN.matcher(content);
        while (springBeanMatcher.find()) {
            String beanName = springBeanMatcher.group(1);
            int lineNumber = findLineNumberInContent(lines, springBeanMatcher.group(0));
            
            usages.add(new ReflectionUsage(
                beanName,
                beanName.replace("javax.", "jakarta."),
                ReflectionUsage.ReflectionType.SPRING_BEAN_NAME,
                "@Bean(name = \"" + beanName + "\")",
                lineNumber
            ));
        }
        
        // Detect annotation values with javax classes
        Matcher annotationValueMatcher = ANNOTATION_VALUE_PATTERN.matcher(content);
        while (annotationValueMatcher.find()) {
            String className = annotationValueMatcher.group(2);
            int lineNumber = findLineNumberInContent(lines, annotationValueMatcher.group(0));
            
            usages.add(new ReflectionUsage(
                className,
                className.replace("javax.", "jakarta."),
                ReflectionUsage.ReflectionType.ANNOTATION_VALUE,
                annotationValueMatcher.group(1).trim() + "\"" + className + "\"",
                lineNumber
            ));
        }
        
        return usages;
    }
    
    /**
     * Scans a file and returns both import statements and reflection usages.
     */
    public FileUsage scanFileWithReflection(Path filePath) {
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
            String[] lines = content.split("\n");
            
            // Use ThreadLocal parser
            JavaParser parser = javaParserThreadLocal.get();
            parser.reset();
            
            // Parse with OpenRewrite
            List<SourceFile> sourceFiles = parser.parse(content).collect(java.util.stream.Collectors.toList());
            
            if (sourceFiles.isEmpty()) {
                return new FileUsage(filePath, List.of(), lineCount);
            }
            
            // Extract javax imports
            List<ImportStatement> imports = new ArrayList<>();
            for (SourceFile sourceFile : sourceFiles) {
                if (sourceFile instanceof CompilationUnit) {
                    CompilationUnit cu = (CompilationUnit) sourceFile;
                    imports.addAll(extractJavaxImports(cu, content));
                }
            }
            
            // Detect reflection usages
            List<ReflectionUsage> reflectionUsages = detectReflectionUsages(content, lines);
            
            return new FileUsage(filePath, imports, lineCount, reflectionUsages);
            
        } catch (Exception e) {
            log.warn("Error scanning file: {}", filePath, e);
            return new FileUsage(filePath, List.of(), 0);
        }
    }
    
    /**
     * Returns summary of reflection usages found in the project.
     */
    public ReflectionSummary scanForReflectionUsages(Path projectPath) {
        if (projectPath == null || !Files.exists(projectPath) || !Files.isDirectory(projectPath)) {
            return new ReflectionSummary(0, 0, List.of(), List.of());
        }
        
        try {
            List<Path> javaFiles = discoverJavaFiles(projectPath);
            
            int totalFiles = javaFiles.size();
            AtomicInteger filesWithReflection = new AtomicInteger(0);
            List<ReflectionUsage> allUsages = new ArrayList<>();
            Set<String> criticalTypes = new HashSet<>();
            
            javaFiles.parallelStream().forEach(file -> {
                try {
                    String content = Files.readString(file);
                    String[] lines = content.split("\n");
                    List<ReflectionUsage> usages = detectReflectionUsages(content, lines);
                    
                    if (!usages.isEmpty()) {
                        filesWithReflection.incrementAndGet();
                        synchronized(allUsages) {
                            allUsages.addAll(usages);
                        }
                        
                        // Track critical reflection types
                        for (ReflectionUsage usage : usages) {
                            if (usage.isCritical()) {
                                synchronized(criticalTypes) {
                                    criticalTypes.add(usage.type().name());
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    log.debug("Error reading file for reflection scan: {}", file);
                }
            });
            
            return new ReflectionSummary(
                totalFiles,
                filesWithReflection.get(),
                allUsages,
                List.copyOf(criticalTypes)
            );
            
        } catch (Exception e) {
            log.error("Error scanning for reflection usages: {}", projectPath, e);
            return new ReflectionSummary(0, 0, List.of(), List.of());
        }
    }
    
    /**
     * Summary of reflection usages found in a project.
     */
    public record ReflectionSummary(
        int totalFiles,
        int filesWithReflection,
        List<ReflectionUsage> usages,
        List<String> criticalTypes
    ) {
        public int getTotalReflectionCount() {
            return usages.size();
        }
        
        public boolean hasCriticalUsages() {
            return !criticalTypes.isEmpty();
        }
    }
    
}

