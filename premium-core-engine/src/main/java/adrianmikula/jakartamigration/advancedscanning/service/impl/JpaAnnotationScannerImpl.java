package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.FileScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.JpaAnnotationUsage;
import adrianmikula.jakartamigration.advancedscanning.domain.ProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.service.BaseScanner;
import adrianmikula.jakartamigration.advancedscanning.service.JpaAnnotationScanner;
import lombok.extern.slf4j.Slf4j;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.CompilationUnit;
import org.openrewrite.SourceFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Implementation of JpaAnnotationScanner using OpenRewrite JavaParser.
 * Provides AST-based scanning for javax.persistence.* annotations.
 */
@Slf4j
public class JpaAnnotationScannerImpl extends BaseScanner<JpaAnnotationUsage> implements JpaAnnotationScanner {

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
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.PrimaryKeyJoinColumn",
                "jakarta.persistence.PrimaryKeyJoinColumn");
        JPA_ANNOTATION_MAPPINGS.put("javax.persistence.PrimaryKeyJoinColumns",
                "jakarta.persistence.PrimaryKeyJoinColumns");
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

    // Parallelism configuration - can be overridden via system property
    private static final int MAX_PARALLELISM = Integer.parseInt(
            System.getProperty("advanced.scan.parallelism", "4"));

    // Memory threshold for sequential fallback (100MB)
    private static final long MEMORY_THRESHOLD_BYTES = 100 * 1024 * 1024;

    @Override
    public ProjectScanResult<FileScanResult<JpaAnnotationUsage>> scanProject(Path projectPath) {
        if (projectPath == null || !Files.exists(projectPath) || !Files.isDirectory(projectPath)) {
            log.warn("Invalid project path: {}", projectPath);
            return ProjectScanResult.empty();
        }

        try {
            List<Path> javaFiles = discoverJavaFiles(projectPath);

            if (javaFiles.isEmpty()) {
                log.info("No Java files found in project: {}", projectPath);
                return ProjectScanResult.empty();
            }

            log.info("Scanning {} Java files for JPA annotations in project: {}", javaFiles.size(), projectPath);

            // Check available memory and determine parallelism
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long availableMemory = maxMemory - usedMemory;

            AtomicInteger totalScanned = new AtomicInteger(0);
            List<FileScanResult<JpaAnnotationUsage>> results;

            // Use sequential processing if low on memory, otherwise use bounded parallelism
            if (availableMemory < MEMORY_THRESHOLD_BYTES) {
                log.info("Low memory detected ({} MB available), using sequential processing for JPA scan",
                        availableMemory / (1024 * 1024));
                results = javaFiles.stream()
                        .map(file -> scanFileWithTracking(file, totalScanned))
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList());
            } else {
                // Use bounded parallelism with custom ForkJoinPool
                int parallelism = Math.min(MAX_PARALLELISM, javaFiles.size());
                log.debug("Using parallel processing with parallelism={} for JPA scan", parallelism);

                ForkJoinPool customPool = new ForkJoinPool(parallelism);
                try {
                    results = customPool.submit(() ->
                            javaFiles.parallelStream()
                                    .map(file -> scanFileWithTracking(file, totalScanned))
                                    .filter(java.util.Objects::nonNull)
                                    .collect(Collectors.toList())
                    ).get();
                } catch (Exception e) {
                    log.warn("Parallel scan failed for JPA, falling back to sequential: {}", e.getMessage());
                    results = javaFiles.stream()
                            .map(file -> scanFileWithTracking(file, totalScanned))
                            .filter(java.util.Objects::nonNull)
                            .collect(Collectors.toList());
                } finally {
                    customPool.shutdown();
                }
            }

            // Cleanup ThreadLocal to prevent memory leaks
            cleanupThreadLocal();

            int totalAnnotations = results.stream()
                    .mapToInt(r -> r.usages().size())
                    .sum();

            log.info("JPA scan complete: {} files scanned, {} files with JPA usage, {} total annotations",
                    totalScanned.get(), results.size(), totalAnnotations);

            return new ProjectScanResult<>(results, totalScanned.get(), results.size(), totalAnnotations);

        } catch (Exception e) {
            log.error("Error scanning project for JPA annotations: {}", projectPath, e);
            return ProjectScanResult.empty();
        }
    }

    @Override
    public FileScanResult<JpaAnnotationUsage> scanFile(Path filePath) {
        Path validatedPath = validateFilePath(filePath);
        if (validatedPath == null) {
            return FileScanResult.empty(filePath);
        }

        try {
            String content = Files.readString(validatedPath);
            int lineCount = countLines(content);

            JavaParser parser = javaParserThreadLocal.get();
            parser.reset();

            List<SourceFile> sourceFiles = parser.parse(content).collect(Collectors.toList());

            if (sourceFiles.isEmpty()) {
                return FileScanResult.empty(filePath);
            }

            List<JpaAnnotationUsage> annotations = new ArrayList<>();
            for (SourceFile sourceFile : sourceFiles) {
                if (sourceFile instanceof CompilationUnit cu) {
                    annotations.addAll(extractJpaAnnotations(cu, content));
                }
            }

            return new FileScanResult<>(filePath, annotations, lineCount);

        } catch (Exception e) {
            log.warn("Error scanning file for JPA annotations: {}", filePath, e);
            return FileScanResult.empty(filePath);
        }
    }

    private List<JpaAnnotationUsage> extractJpaAnnotations(CompilationUnit cu, String content) {
        List<JpaAnnotationUsage> annotations = new ArrayList<>();
        String[] lines = content.split("\n");

        // Check imports
        for (J.Import imp : cu.getImports()) {
            String importName = imp.getQualid().toString();
            if (importName.startsWith("javax.persistence.")) {
                String jakartaEquivalent = JPA_ANNOTATION_MAPPINGS.get(importName);
                int lineNumber = findLineNumber(lines, importName);
                annotations.add(new JpaAnnotationUsage(
                        importName,
                        jakartaEquivalent != null ? jakartaEquivalent : importName.replace("javax.", "jakarta."),
                        lineNumber,
                        "import",
                        "import"));
            }
        }

        // Search for annotation usages
        Pattern annotationPattern = Pattern.compile("@((?:javax\\.persistence\\.\\w+|\\w+(?=\\s*\\()))");
        Matcher matcher = annotationPattern.matcher(content);

        while (matcher.find()) {
            String annotationName = matcher.group(1);
            if (!annotationName.startsWith("javax.persistence.")) {
                continue;
            }
            String jakartaEquivalent = JPA_ANNOTATION_MAPPINGS.get(annotationName);
            int lineNumber = findLineNumber(lines, matcher.group(0));
            annotations.add(new JpaAnnotationUsage(
                    annotationName,
                    jakartaEquivalent != null ? jakartaEquivalent : annotationName.replace("javax.", "jakarta."),
                    lineNumber,
                    "annotation",
                    "annotation"));
        }

        return annotations;
    }

    /**
     * Scans a single file with tracking for parallel processing.
     */
    private FileScanResult<JpaAnnotationUsage> scanFileWithTracking(Path filePath, AtomicInteger totalScanned) {
        totalScanned.incrementAndGet();
        FileScanResult<JpaAnnotationUsage> result = scanFile(filePath);
        return result.hasIssues() ? result : null;
    }

    /**
     * Cleans up ThreadLocal JavaParser instances to prevent memory leaks.
     * Should be called after project scan completes.
     */
    protected void cleanupThreadLocal() {
        try {
            javaParserThreadLocal.remove();
        } catch (Exception e) {
            log.debug("Error cleaning up ThreadLocal: {}", e.getMessage());
        }
    }
}
