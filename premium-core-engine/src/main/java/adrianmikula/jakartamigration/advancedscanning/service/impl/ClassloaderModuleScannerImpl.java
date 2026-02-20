package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.ClassloaderModuleProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.ClassloaderModuleScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.ClassloaderModuleUsage;
import adrianmikula.jakartamigration.advancedscanning.service.ClassloaderModuleScanner;
import lombok.extern.slf4j.Slf4j;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.CompilationUnit;
import org.openrewrite.SourceFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class ClassloaderModuleScannerImpl implements ClassloaderModuleScanner {

    // Known javax classloader and module APIs
    private static final Map<String, ClassloaderModuleInfo> CLASSLOADER_MODULE_APIS = new HashMap<>();

    static {
        // Thread Context ClassLoader
        CLASSLOADER_MODULE_APIS.put("javax.management.loading.MLet", 
            new ClassloaderModuleInfo("Use standard classloading or CDI", "MLet"));
        
        // RMI ClassLoader
        CLASSLOADER_MODULE_APIS.put("java.rmi.server.ObjID", 
            new ClassloaderModuleInfo("Review RMI usage", "RMI"));
        
        // JNDI ClassLoader
        CLASSLOADER_MODULE_APIS.put("javax.naming.Context", 
            new ClassloaderModuleInfo("jakarta.naming.Context", "JNDI"));
        
        // Module system (Java 9+) related
        CLASSLOADER_MODULE_APIS.put("javax.annotation.processing.Processor", 
            new ClassloaderModuleInfo("jakarta.annotation.processing.Processor", "Annotation Processing"));
    }

    private record ClassloaderModuleInfo(String replacement, String context) {}

    private final ThreadLocal<JavaParser> javaParserThreadLocal = ThreadLocal.withInitial(() -> JavaParser.fromJavaVersion().build());

    // Pattern for javax classloader/module imports
    private static final Pattern CLASSLOADER_IMPORT_PATTERN = Pattern.compile(
        "import\\s+javax\\.(management\\.loading|annotation\\.processing|\\w*\\.classloader|\\w*\\.module)[\\.\\w]*;",
        Pattern.MULTILINE
    );

    // Pattern for Thread.currentThread().getContextClassLoader() usage
    private static final Pattern CONTEXT_CLASSLOADER_PATTERN = Pattern.compile(
        "getContextClassLoader\\(\\)|setContextClassLoader",
        Pattern.MULTILINE
    );

    // Pattern for module-related calls
    private static final Pattern MODULE_PATTERN = Pattern.compile(
        "getModule\\(\\)|getClassLoader\\(\\)",
        Pattern.MULTILINE
    );

    @Override
    public ClassloaderModuleProjectScanResult scanProject(Path projectPath) {
        if (projectPath == null || !Files.exists(projectPath) || !Files.isDirectory(projectPath)) {
            return ClassloaderModuleProjectScanResult.empty();
        }

        try {
            List<Path> javaFiles = discoverJavaFiles(projectPath);
            if (javaFiles.isEmpty()) return ClassloaderModuleProjectScanResult.empty();

            AtomicInteger totalScanned = new AtomicInteger(0);
            List<ClassloaderModuleScanResult> results = javaFiles.parallelStream()
                .map(file -> {
                    totalScanned.incrementAndGet();
                    ClassloaderModuleScanResult result = scanFile(file);
                    return result.hasJavaxUsage() ? result : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            int totalUsages = results.stream().mapToInt(r -> r.usages().size()).sum();

            return new ClassloaderModuleProjectScanResult(results, totalScanned.get(), results.size(), totalUsages);
        } catch (Exception e) {
            log.error("Error scanning project for classloader/module APIs", e);
            return ClassloaderModuleProjectScanResult.empty();
        }
    }

    @Override
    public ClassloaderModuleScanResult scanFile(Path filePath) {
        if (filePath == null || !Files.exists(filePath)) {
            return ClassloaderModuleScanResult.empty(filePath);
        }

        try {
            String content = Files.readString(filePath);
            int lineCount = content.split("\n").length;

            // Quick check using regex first for performance
            boolean hasPotentialUsage = CLASSLOADER_IMPORT_PATTERN.matcher(content).find() ||
                CONTEXT_CLASSLOADER_PATTERN.matcher(content).find() ||
                MODULE_PATTERN.matcher(content).find();
            
            if (!hasPotentialUsage) {
                return ClassloaderModuleScanResult.empty(filePath);
            }

            // Use OpenRewrite for detailed analysis
            JavaParser parser = javaParserThreadLocal.get();
            parser.reset();

            List<SourceFile> sourceFiles = parser.parse(content).collect(Collectors.toList());
            if (sourceFiles.isEmpty()) return ClassloaderModuleScanResult.empty(filePath);

            List<ClassloaderModuleUsage> usages = new ArrayList<>();
            for (SourceFile sourceFile : sourceFiles) {
                if (sourceFile instanceof CompilationUnit) {
                    CompilationUnit cu = (CompilationUnit) sourceFile;
                    usages.addAll(extractClassloaderModuleApis(cu, content));
                }
            }

            return new ClassloaderModuleScanResult(filePath, usages, lineCount);
        } catch (Exception e) {
            return ClassloaderModuleScanResult.empty(filePath);
        }
    }

    private List<Path> discoverJavaFiles(Path projectPath) {
        try (Stream<Path> paths = Files.walk(projectPath)) {
            return paths.filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java"))
                .filter(this::shouldScanFile)
                .collect(Collectors.toList());
        } catch (IOException e) {
            return List.of();
        }
    }

    private boolean shouldScanFile(Path file) {
        String path = file.toString().replace('\\', '/');
        return !path.contains("/target/") && !path.contains("/build/") && !path.contains("/.git/");
    }

    private List<ClassloaderModuleUsage> extractClassloaderModuleApis(CompilationUnit cu, String content) {
        List<ClassloaderModuleUsage> usages = new ArrayList<>();
        String[] lines = content.split("\n");

        // Check imports for javax classloader/module classes
        for (J.Import imp : cu.getImports()) {
            String importName = imp.getQualid().toString();
            ClassloaderModuleInfo info = CLASSLOADER_MODULE_APIS.get(importName);

            if (info != null) {
                int lineNumber = findLineNumber(lines, importName);
                usages.add(new ClassloaderModuleUsage(importName, null, lineNumber, info.context(), info.replacement()));
            }
        }

        // Check for Thread.getContextClassLoader() usage
        Matcher matcher = CONTEXT_CLASSLOADER_PATTERN.matcher(content);
        while (matcher.find()) {
            int lineNumber = findLineNumber(lines, "getContextClassLoader");
            usages.add(new ClassloaderModuleUsage(
                "Thread.getContextClassLoader()", 
                "getContextClassLoader", 
                lineNumber, 
                "ClassLoader", 
                "Review for Jakarta EE compatibility"
            ));
        }

        // Check for module-related patterns
        matcher = MODULE_PATTERN.matcher(content);
        while (matcher.find()) {
            int lineNumber = findLineNumber(lines, "getModule");
            if (lineNumber > 0) {
                usages.add(new ClassloaderModuleUsage(
                    "Class.getModule()", 
                    "getModule", 
                    lineNumber, 
                    "Module", 
                    "Use standard Java module API"
                ));
            }
        }

        return usages;
    }

    private int findLineNumber(String[] lines, String searchText) {
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains(searchText)) return i + 1;
        }
        return 1;
    }
}
