package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.CdiInjectionProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.CdiInjectionScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.CdiInjectionUsage;
import adrianmikula.jakartamigration.advancedscanning.service.CdiInjectionScanner;
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
 * Implementation of CdiInjectionScanner using OpenRewrite JavaParser.
 * Provides AST-based scanning for javax.inject and javax.enterprise usage.
 */
@Slf4j
public class CdiInjectionScannerImpl implements CdiInjectionScanner {

    // Map of javax.inject and javax.enterprise to Jakarta equivalents
    private static final Map<String, String> CDI_MAPPINGS = new HashMap<>();

    static {
        // javax.inject annotations
        CDI_MAPPINGS.put("javax.inject.Inject", "jakarta.inject.Inject");
        CDI_MAPPINGS.put("javax.inject.Named", "jakarta.inject.Named");
        CDI_MAPPINGS.put("javax.inject.Qualifier", "jakarta.inject.Qualifier");
        CDI_MAPPINGS.put("javax.inject.Scope", "jakarta.inject.Scope");
        CDI_MAPPINGS.put("javax.inject.Singleton", "jakarta.inject.Singleton");
        CDI_MAPPINGS.put("javax.inject.Provider", "jakarta.inject.Provider");
        CDI_MAPPINGS.put("javax.inject.Instance", "jakarta.inject.Instance");
        CDI_MAPPINGS.put("javax.inject.Inject", "jakarta.inject.Inject");
        CDI_MAPPINGS.put("javax.inject.Default", "jakarta.inject.Default");

        // javax.enterprise.context
        CDI_MAPPINGS.put("javax.enterprise.context.ApplicationScoped", "jakarta.enterprise.context.ApplicationScoped");
        CDI_MAPPINGS.put("javax.enterprise.context.RequestScoped", "jakarta.enterprise.context.RequestScoped");
        CDI_MAPPINGS.put("javax.enterprise.context.SessionScoped", "jakarta.enterprise.context.SessionScoped");
        CDI_MAPPINGS.put("javax.enterprise.context.ConversationScoped", "jakarta.enterprise.context.ConversationScoped");
        CDI_MAPPINGS.put("javax.enterprise.context.Dependent", "jakarta.enterprise.context.Dependent");
        CDI_MAPPINGS.put("javax.enterprise.context.ApplicationScoped", "jakarta.enterprise.context.ApplicationScoped");
        CDI_MAPPINGS.put("javax.enterprise.context.NormalScope", "jakarta.enterprise.context.NormalScope");
        CDI_MAPPINGS.put("javax.enterprise.context.Scope", "jakarta.enterprise.context.Scope");

        // javax.enterprise.inject
        CDI_MAPPINGS.put("javax.enterprise.inject.Default", "jakarta.enterprise.inject.Default");
        CDI_MAPPINGS.put("javax.enterprise.inject.Any", "jakarta.enterprise.inject.Any");
        CDI_MAPPINGS.put("javax.enterprise.inject.New", "jakarta.enterprise.inject.New");
        CDI_MAPPINGS.put("javax.enterprise.inject.Instance", "jakarta.enterprise.inject.Instance");
        CDI_MAPPINGS.put("javax.enterprise.inject.Typed", "jakarta.enterprise.inject.Typed");
        CDI_MAPPINGS.put("javax.enterprise.inject.Produces", "jakarta.enterprise.inject.Produces");
        CDI_MAPPINGS.put("javax.enterprise.inject.Disposes", "jakarta.enterprise.inject.Disposes");
        CDI_MAPPINGS.put("javax.enterprise.inject.Observer", "jakarta.enterprise.inject.Observer");
        CDI_MAPPINGS.put("javax.enterprise.inject.Observes", "jakarta.enterprise.inject.Observes");
        CDI_MAPPINGS.put("javax.enterprise.inject.Creates", "jakarta.enterprise.inject.Creates");

        // javax.enterprise.event
        CDI_MAPPINGS.put("javax.enterprise.event.Observes", "jakarta.enterprise.event.Observes");
        CDI_MAPPINGS.put("javax.enterprise.event.ObservesAsync", "jakarta.enterprise.event.ObservesAsync");
        CDI_MAPPINGS.put("javax.enterprise.event.Event", "jakarta.enterprise.event.Event");
        CDI_MAPPINGS.put("javax.enterprise.event.Fires", "jakarta.enterprise.event.Fires");

        // javax.enterprise.util
        CDI_MAPPINGS.put("javax.enterprise.util.AnnotationLiteral", "jakarta.enterprise.util.AnnotationLiteral");
        CDI_MAPPINGS.put("javax.enterprise.util.TypeLiteral", "jakarta.enterprise.util.TypeLiteral");
        CDI_MAPPINGS.put("javax.enterprise.util.Instance", "jakarta.enterprise.util.Instance");

        // javax.enterprise.v09.framework
        CDI_MAPPINGS.put("javax.enterprise.context.control.RequestContextController", 
            "jakarta.enterprise.context.control.RequestContextController");

        // Interceptors and Decorators
        CDI_MAPPINGS.put("javax.interceptor.Interceptor", "jakarta.interceptor.Interceptor");
        CDI_MAPPINGS.put("javax.interceptor.Interceptors", "jakarta.interceptor.Interceptors");
        CDI_MAPPINGS.put("javax.interceptor.AroundInvoke", "jakarta.interceptor.AroundInvoke");
        CDI_MAPPINGS.put("javax.interceptor.AroundTimeout", "jakarta.interceptor.AroundTimeout");
        CDI_MAPPINGS.put("javax.interceptor.InvocationContext", "jakarta.interceptor.InvocationContext");
        CDI_MAPPINGS.put("javax.decorator.Decorator", "jakarta.decorator.Decorator");
        CDI_MAPPINGS.put("javax.decorator.Delegate", "jakarta.decorator.Delegate");
    }

    private final ThreadLocal<JavaParser> javaParserThreadLocal = ThreadLocal.withInitial(() ->
        JavaParser.fromJavaVersion().build()
    );

    @Override
    public CdiInjectionProjectScanResult scanProject(Path projectPath) {
        if (projectPath == null || !Files.exists(projectPath) || !Files.isDirectory(projectPath)) {
            log.warn("Invalid project path: {}", projectPath);
            return CdiInjectionProjectScanResult.empty();
        }

        try {
            List<Path> javaFiles = discoverJavaFiles(projectPath);

            if (javaFiles.isEmpty()) {
                log.info("No Java files found in project: {}", projectPath);
                return CdiInjectionProjectScanResult.empty();
            }

            log.info("Scanning {} Java files for CDI in project: {}", javaFiles.size(), projectPath);

            AtomicInteger totalScanned = new AtomicInteger(0);
            List<CdiInjectionScanResult> results = javaFiles.parallelStream()
                .map(file -> {
                    totalScanned.incrementAndGet();
                    CdiInjectionScanResult result = scanFile(file);
                    if (result.hasJavaxUsage()) {
                        log.debug("Found CDI usage in: {}", file);
                        return result;
                    }
                    return null;
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

            int totalAnnotations = results.stream()
                .mapToInt(r -> r.usages().size())
                .sum();

            log.info("CDI scan complete: {} files scanned, {} files with usage, {} total usages",
                totalScanned.get(), results.size(), totalAnnotations);

            return new CdiInjectionProjectScanResult(
                results,
                totalScanned.get(),
                results.size(),
                totalAnnotations
            );

        } catch (Exception e) {
            log.error("Error scanning project for CDI: {}", projectPath, e);
            return CdiInjectionProjectScanResult.empty();
        }
    }

    @Override
    public CdiInjectionScanResult scanFile(Path filePath) {
        if (filePath == null) {
            throw new IllegalArgumentException("filePath cannot be null");
        }
        if (!Files.exists(filePath)) {
            log.warn("File does not exist: {}", filePath);
            return CdiInjectionScanResult.empty(filePath);
        }

        try {
            String content = Files.readString(filePath);
            int lineCount = countLines(content);

            JavaParser parser = javaParserThreadLocal.get();
            parser.reset();

            List<SourceFile> sourceFiles = parser.parse(content).collect(java.util.stream.Collectors.toList());

            if (sourceFiles.isEmpty()) {
                return CdiInjectionScanResult.empty(filePath);
            }

            List<CdiInjectionUsage> usages = new ArrayList<>();
            for (SourceFile sourceFile : sourceFiles) {
                if (sourceFile instanceof CompilationUnit) {
                    CompilationUnit cu = (CompilationUnit) sourceFile;
                    usages.addAll(extractCdiUsages(cu, content));
                }
            }

            return new CdiInjectionScanResult(filePath, usages, lineCount);

        } catch (Exception e) {
            log.warn("Error scanning file for CDI: {}", filePath, e);
            return CdiInjectionScanResult.empty(filePath);
        }
    }

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

    private List<CdiInjectionUsage> extractCdiUsages(CompilationUnit cu, String content) {
        List<CdiInjectionUsage> usages = new ArrayList<>();
        String[] lines = content.split("\n");

        // Check imports
        for (J.Import imp : cu.getImports()) {
            String importName = imp.getQualid().toString();

            if (importName.startsWith("javax.inject.") || 
                importName.startsWith("javax.enterprise.")) {

                String jakartaEquivalent = CDI_MAPPINGS.get(importName);
                int lineNumber = findLineNumber(lines, importName);

                usages.add(new CdiInjectionUsage(
                    importName,
                    jakartaEquivalent != null ? jakartaEquivalent : importName.replace("javax.", "jakarta."),
                    lineNumber,
                    "import",
                    "import"
                ));
            }
        }

        // Search for annotation usages
        Pattern annotationPattern = Pattern.compile("@((?:javax\\\\.inject[\\\\w.]*|javax\\\\.enterprise[\\\\w.]*|javax\\\\.interceptor[\\\\w.]*|javax\\\\.decorator[\\\\w.]*))");
        Matcher matcher = annotationPattern.matcher(content);

        while (matcher.find()) {
            String annotationName = matcher.group(1);

            if (annotationName.startsWith("javax.inject") || 
                annotationName.startsWith("javax.enterprise") ||
                annotationName.startsWith("javax.interceptor") ||
                annotationName.startsWith("javax.decorator")) {

                String jakartaEquivalent = CDI_MAPPINGS.get(annotationName);
                int lineNumber = findLineNumber(lines, matcher.group(0));

                usages.add(new CdiInjectionUsage(
                    annotationName,
                    jakartaEquivalent != null ? jakartaEquivalent : annotationName.replace("javax.", "jakarta."),
                    lineNumber,
                    "annotation",
                    "annotation"
                ));
            }
        }

        return usages;
    }

    private int findLineNumber(String[] lines, String searchText) {
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains(searchText)) {
                return i + 1;
            }
        }
        return 1;
    }

    private int countLines(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        return content.split("\n").length;
    }
}
