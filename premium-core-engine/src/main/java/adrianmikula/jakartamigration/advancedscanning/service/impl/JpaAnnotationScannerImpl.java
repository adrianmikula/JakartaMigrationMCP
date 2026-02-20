package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.JpaAnnotationUsage;
import adrianmikula.jakartamigration.advancedscanning.domain.JpaProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.JpaScanResult;
import adrianmikula.jakartamigration.advancedscanning.service.JpaAnnotationScanner;
import lombok.extern.slf4j.Slf4j;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.CompilationUnit;
import org.openrewrite.SourceFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of JpaAnnotationScanner using OpenRewrite JavaParser.
 * Provides AST-based scanning for javax.persistence.* annotations.
 */
@Slf4j
public class JpaAnnotationScannerImpl implements JpaAnnotationScanner {

    // Map of javax.persistence annotations to their Jakarta equivalents
    private static final Map<String, String> JPA_ANNOTATION_MAPPINGS = new HashMap<>();
    
    static {
        // Entity and lifecycle annotations
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.Entity", "jakarta.persistence.Entity");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.MappedSuperclass", "jakarta.persistence.MappedSuperclass");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.Embeddable", "jakarta.persistence.Embeddable");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.Embedded", "jakarta.persistence.Embedded");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.EmbeddedId", "jakarta.persistence.EmbeddedId");
        
        // Field/Property annotations
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.Id", "jakarta.persistence.Id");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.GeneratedValue", "jakarta.persistence.GeneratedValue");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.GenerationType", "jakarta.persistence.GenerationType");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.Version", "jakarta.persistence.Version");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.Column", "jakarta.persistence.Column");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.Basic", "jakarta.persistence.Basic");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.Transient", "jakarta.persistence.Transient");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.Temporal", "jakarta.persistence.Temporal");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.Enumerated", "jakarta.persistence.Enumerated");
        
        // Relationship annotations
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.OneToOne", "jakarta.persistence.OneToOne");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.OneToMany", "jakarta.persistence.OneToMany");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.ManyToOne", "jakarta.persistence.ManyToOne");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.ManyToMany", "jakarta.persistence.ManyToMany");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.JoinColumn", "jakarta.persistence.JoinColumn");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.JoinColumns", "jakarta.persistence.JoinColumns");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.PrimaryKeyJoinColumn", "jakarta.persistence.PrimaryKeyJoinColumn");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.PrimaryKeyJoinColumns", "jakarta.persistence.PrimaryKeyJoinColumns");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.OrderBy", "jakarta.persistence.OrderBy");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.OrderColumn", "jakarta.persistence.OrderColumn");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.MapKey", "jakarta.persistence.MapKey");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.MapKeyJoinColumn", "jakarta.persistence.MapKeyJoinColumn");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.MapKeyClass", "jakarta.persistence.MapKeyClass");
        
        // Table and schema annotations
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.Table", "jakarta.persistence.Table");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.SecondaryTable", "jakarta.persistence.SecondaryTable");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.SecondaryTables", "jakarta.persistence.SecondaryTables");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.UniqueConstraint", "jakarta.persistence.UniqueConstraint");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.Index", "jakarta.persistence.Index");
        
        // Query annotations
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.NamedQuery", "jakarta.persistence.NamedQuery");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.NamedQueries", "jakarta.persistence.NamedQueries");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.NamedNativeQuery", "jakarta.persistence.NamedNativeQuery");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.NamedNativeQueries", "jakarta.persistence.NamedNativeQueries");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.QueryHint", "jakarta.persistence.QueryHint");
        
        // Lifecycle callbacks
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.PrePersist", "jakarta.persistence.PrePersist");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.PostPersist", "jakarta.persistence.PostPersist");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.PreRemove", "jakarta.persistence.PreRemove");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.PostRemove", "jakarta.persistence.PostRemove");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.PreUpdate", "jakarta.persistence.PreUpdate");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.PostUpdate", "jakarta.persistence.PostUpdate");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.PostLoad", "jakarta.persistence.PostLoad");
        
        // Converter and attributes
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.AttributeConverter", "jakarta.persistence.AttributeConverter");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.Converter", "jakarta.persistence.Converter");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.Convert", "jakarta.persistence.Convert");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.BasicMap", "jakarta.persistence.BasicMap");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.ElementCollection", "jakarta.persistence.ElementCollection");
        
        // Cache annotations
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.Cacheable", "jakarta.persistence.Cacheable");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.Cache", "jakarta.persistence.Cache");
        
        // Inheritance
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.Inheritance", "jakarta.persistence.Inheritance");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.InheritanceType", "jakarta.persistence.InheritanceType");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.DiscriminatorColumn", "jakarta.persistence.DiscriminatorColumn");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.DiscriminatorValue", "jakarta.persistence.DiscriminatorValue");
        
        // Sequence
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.SequenceGenerator", "jakarta.persistence.SequenceGenerator");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.TableGenerator", "jakarta.persistence.TableGenerator");
        
        // Locking
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.LockModeType", "jakarta.persistence.LockModeType");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.LockMode", "jakarta.persistence.LockMode");
        
        // Callbacks
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.Externalized", "jakarta.persistence.Externalized");
        // Persistence context
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.PersistenceContext", "jakarta.persistence.PersistenceContext");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.PersistenceContexts", "jakarta.persistence.PersistenceContexts");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.PersistenceUnit", "jakarta.persistence.PersistenceUnit");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.PersistenceUnits", "jakarta.persistence.PersistenceUnits");
    }

    // Use ThreadLocal to avoid JavaParser reset() issues when parsing files
    // with the same fully qualified names in parallel
    private final ThreadLocal<JavaParser> javaParserThreadLocal = ThreadLocal.withInitial(() ->
        JavaParser.fromJavaVersion().build()
    );

    @Override
    public JpaProjectScanResult scanProject(Path projectPath) {
        if (projectPath == null || !Files.exists(projectPath) || !Files.isDirectory(projectPath)) {
            log.warn("Invalid project path: {}", projectPath);
            return JpaProjectScanResult.empty();
        }

        try {
            // Discover all Java files
            List<Path> javaFiles = discoverJavaFiles(projectPath);

            if (javaFiles.isEmpty()) {
                log.info("No Java files found in project: {}", projectPath);
                return JpaProjectScanResult.empty();
            }

            log.info("Scanning {} Java files for JPA annotations in project: {}", javaFiles.size(), projectPath);

            // Scan files in parallel
            AtomicInteger totalScanned = new AtomicInteger(0);
            List<JpaScanResult> results = javaFiles.parallelStream()
                .map(file -> {
                    totalScanned.incrementAndGet();
                    JpaScanResult result = scanFile(file);
                    if (result.hasJavaxUsage()) {
                        log.debug("Found JPA annotations in: {}", file);
                        return result;
                    }
                    return null;
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

            int totalAnnotations = results.stream()
                .mapToInt(r -> r.annotations().size())
                .sum();

            log.info("JPA scan complete: {} files scanned, {} files with JPA usage, {} total annotations",
                totalScanned.get(), results.size(), totalAnnotations);

            return new JpaProjectScanResult(
                results,
                totalScanned.get(),
                results.size(),
                totalAnnotations
            );

        } catch (Exception e) {
            log.error("Error scanning project for JPA annotations: {}", projectPath, e);
            return JpaProjectScanResult.empty();
        }
    }

    @Override
    public JpaScanResult scanFile(Path filePath) {
        if (filePath == null) {
            throw new IllegalArgumentException("filePath cannot be null");
        }
        if (!Files.exists(filePath)) {
            log.warn("File does not exist: {}", filePath);
            return JpaScanResult.empty(filePath);
        }

        try {
            String content = Files.readString(filePath);
            int lineCount = countLines(content);

            JavaParser parser = javaParserThreadLocal.get();
            parser.reset();

            List<SourceFile> sourceFiles = parser.parse(content).collect(java.util.stream.Collectors.toList());

            if (sourceFiles.isEmpty()) {
                log.debug("No source files found in file: {}", filePath);
                return JpaScanResult.empty(filePath);
            }

            // Extract JPA annotations from all compilation units
            List<JpaAnnotationUsage> annotations = new ArrayList<>();
            for (SourceFile sourceFile : sourceFiles) {
                if (sourceFile instanceof CompilationUnit) {
                    CompilationUnit cu = (CompilationUnit) sourceFile;
                    annotations.addAll(extractJpaAnnotations(cu, content));
                }
            }

            return new JpaScanResult(filePath, annotations, lineCount);

        } catch (Exception e) {
            log.warn("Error scanning file for JPA annotations: {}", filePath, e);
            return JpaScanResult.empty(filePath);
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
     * Extracts javax.persistence.* annotations from a compilation unit.
     */
    private List<JpaAnnotationUsage> extractJpaAnnotations(CompilationUnit cu, String content) {
        List<JpaAnnotationUsage> annotations = new ArrayList<>();
        
        String[] lines = content.split("\n");
        
        // Find all imports first
        for (J.Import imp : cu.getImports()) {
            String importName = imp.getQualid().toString();
            
            if (importName.startsWith("javax.persistence.")) {
                String jakartaEquivalent = JPA_ANNOTATION_MAPPINGS.get(importName);
                
                // Find line number
                int lineNumber = findLineNumberInContent(lines, importName);
                
                annotations.add(new JpaAnnotationUsage(
                    importName,
                    jakartaEquivalent != null ? jakartaEquivalent : "jakarta.persistence." + importName.substring(17),
                    lineNumber,
                    "import",
                    "import"
                ));
            }
        }
        
        // Now search for annotation usages in the content using regex
        // Pattern: @javax.persistence.SomeAnnotation or @SomeAnnotation (for annotations in javax.persistence package)
        Pattern annotationPattern = Pattern.compile("@((?:javax\\.persistence\\.\\w+|\\w+(?=\\s*\\()))");
        Matcher matcher = annotationPattern.matcher(content);
        
        while (matcher.find()) {
            String annotationName = matcher.group(1);
            
            // Skip if it's not javax.persistence
            if (!annotationName.startsWith("javax.persistence.") && !annotationName.contains(".")) {
                // This might be a short annotation name - need to check imports
                // For now, skip these
                continue;
            }
            
            if (annotationName.startsWith("javax.persistence.")) {
                String jakartaEquivalent = JPA_ANNOTATION_MAPPINGS.get(annotationName);
                
                // Find line number
                int lineNumber = findLineNumberInContent(lines, matcher.group(0));
                
                annotations.add(new JpaAnnotationUsage(
                    annotationName,
                    jakartaEquivalent != null ? jakartaEquivalent : "jakarta.persistence." + annotationName.substring(17),
                    lineNumber,
                    "annotation",
                    "annotation"
                ));
            }
        }
        
        return annotations;
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
     * Counts lines in content.
     */
    private int countLines(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        return content.split("\n").length;
    }
}
