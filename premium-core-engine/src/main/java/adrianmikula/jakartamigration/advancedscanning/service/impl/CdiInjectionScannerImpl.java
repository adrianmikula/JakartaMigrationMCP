package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.FileScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.JavaxUsage;
import adrianmikula.jakartamigration.advancedscanning.domain.ProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.service.BaseScanner;
import adrianmikula.jakartamigration.advancedscanning.service.CdiInjectionScanner;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of CdiInjectionScanner using OpenRewrite JavaParser.
 * Provides AST-based scanning for javax.inject and javax.enterprise usage.
 */
@Slf4j
public class CdiInjectionScannerImpl extends BaseScanner<JavaxUsage> implements CdiInjectionScanner {

    private static final Map<String, String> CDI_MAPPINGS = new HashMap<>();

    static {
        // javax.inject
        CDI_MAPPINGS.put("javax.inject.Inject", "jakarta.inject.Inject");
        CDI_MAPPINGS.put("javax.inject.Named", "jakarta.inject.Named");
        CDI_MAPPINGS.put("javax.inject.Qualifier", "jakarta.inject.Qualifier");
        CDI_MAPPINGS.put("javax.inject.Scope", "jakarta.inject.Scope");
        CDI_MAPPINGS.put("javax.inject.Singleton", "jakarta.inject.Singleton");
        CDI_MAPPINGS.put("javax.inject.Provider", "jakarta.inject.Provider");
        CDI_MAPPINGS.put("javax.inject.Instance", "jakarta.inject.Instance");
        CDI_MAPPINGS.put("javax.inject.Default", "jakarta.inject.Default");

        // javax.enterprise.context
        CDI_MAPPINGS.put("javax.enterprise.context.ApplicationScoped", "jakarta.enterprise.context.ApplicationScoped");
        CDI_MAPPINGS.put("javax.enterprise.context.RequestScoped", "jakarta.enterprise.context.RequestScoped");
        CDI_MAPPINGS.put("javax.enterprise.context.SessionScoped", "jakarta.enterprise.context.SessionScoped");
        CDI_MAPPINGS.put("javax.enterprise.context.ConversationScoped", "jakarta.enterprise.context.ConversationScoped");
        CDI_MAPPINGS.put("javax.enterprise.context.Dependent", "jakarta.enterprise.context.Dependent");
        CDI_MAPPINGS.put("javax.enterprise.context.NormalScope", "jakarta.enterprise.context.NormalScope");

        // javax.enterprise.inject
        CDI_MAPPINGS.put("javax.enterprise.inject.Default", "jakarta.enterprise.inject.Default");
        CDI_MAPPINGS.put("javax.enterprise.inject.Any", "jakarta.enterprise.inject.Any");
        CDI_MAPPINGS.put("javax.enterprise.inject.Produces", "jakarta.enterprise.inject.Produces");
        CDI_MAPPINGS.put("javax.enterprise.inject.Observes", "jakarta.enterprise.inject.Observes");

        // javax.enterprise.event
        CDI_MAPPINGS.put("javax.enterprise.event.Observes", "jakarta.enterprise.event.Observes");
        CDI_MAPPINGS.put("javax.enterprise.event.Event", "jakarta.enterprise.event.Event");

        // Interceptors and Decorators
        CDI_MAPPINGS.put("javax.interceptor.Interceptor", "jakarta.interceptor.Interceptor");
        CDI_MAPPINGS.put("javax.interceptor.AroundInvoke", "jakarta.interceptor.AroundInvoke");
        CDI_MAPPINGS.put("javax.decorator.Decorator", "jakarta.decorator.Decorator");
        CDI_MAPPINGS.put("javax.decorator.Delegate", "jakarta.decorator.Delegate");
    }

    @Override
    public ProjectScanResult<FileScanResult<JavaxUsage>> scanProject(Path projectPath) {
        return scanProjectGeneric(projectPath, "CDI");
    }

    @Override
    public FileScanResult<JavaxUsage> scanFile(Path filePath) {
        Path validatedPath = validateFilePath(filePath);
        if (validatedPath == null) {
            return FileScanResult.empty(filePath);
        }

        try {
            String content = Files.readString(validatedPath);
            int lineCount = countLines(content);

            JavaParser parser = javaParserThreadLocal.get();
            parser.reset();

            List<SourceFile> sourceFiles = parser.parse(content).collect(java.util.stream.Collectors.toList());

            if (sourceFiles.isEmpty()) {
                return FileScanResult.empty(filePath);
            }

            List<JavaxUsage> usages = new ArrayList<>();
            for (SourceFile sourceFile : sourceFiles) {
                if (sourceFile instanceof CompilationUnit cu) {
                    usages.addAll(extractUsages(cu, content));
                }
            }

            return new FileScanResult<>(filePath, usages, lineCount);

        } catch (Exception e) {
            log.warn("Error scanning file for CDI: {}", filePath, e);
            return FileScanResult.empty(filePath);
        }
    }

    private List<JavaxUsage> extractUsages(CompilationUnit cu, String content) {
        List<JavaxUsage> usages = new ArrayList<>();
        String[] lines = content.split("\n");

        // Check imports
        for (J.Import imp : cu.getImports()) {
            String importName = imp.getQualid().toString();
            if (importName.startsWith("javax.inject.") || importName.startsWith("javax.enterprise.")) {
                String jakartaEquivalent = CDI_MAPPINGS.get(importName);
                int lineNumber = findLineNumber(lines, importName);
                usages.add(new JavaxUsage(importName, jakartaEquivalent != null ? jakartaEquivalent : importName.replace("javax.", "jakarta."), lineNumber, "import"));
            }
        }

        // Search for annotation usages
        Pattern annotationPattern = Pattern.compile("@((?:javax\\.inject[\\w.]*|javax\\.enterprise[\\w.]*|javax\\.interceptor[\\w.]*|javax\\.decorator[\\w.]*))");
        Matcher matcher = annotationPattern.matcher(content);

        while (matcher.find()) {
            String annotationName = matcher.group(1);
            if (annotationName.startsWith("javax.inject") || annotationName.startsWith("javax.enterprise") ||
                annotationName.startsWith("javax.interceptor") || annotationName.startsWith("javax.decorator")) {
                String jakartaEquivalent = CDI_MAPPINGS.get(annotationName);
                int lineNumber = findLineNumber(lines, matcher.group(0));
                usages.add(new JavaxUsage(annotationName, jakartaEquivalent != null ? jakartaEquivalent : annotationName.replace("javax.", "jakarta."), lineNumber, "annotation"));
            }
        }

        return usages;
    }
}
