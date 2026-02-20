package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.DeprecatedApiProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.DeprecatedApiScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.DeprecatedApiUsage;
import adrianmikula.jakartamigration.advancedscanning.service.DeprecatedApiScanner;
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
public class DeprecatedApiScannerImpl implements DeprecatedApiScanner {

    // Known deprecated/removed APIs in Jakarta EE
    private static final Map<String, DeprecatedApiInfo> DEPRECATED_APIS = new HashMap<>();

    static {
        // Removed in Jakarta EE 9+
        DEPRECATED_APIS.put("javax.activation.ActivationDataFlavor", 
            new DeprecatedApiInfo("jakarta.activation.ActivationDataFlavor", "removed"));
        DEPRECATED_APIS.put("javax.activation.DataContentHandler", 
            new DeprecatedApiInfo("Use Jakarta Mail", "removed"));
        DEPRECATED_APIS.put("javax.activation.DataHandler", 
            new DeprecatedApiInfo("Use Jakarta Mail", "removed"));
        DEPRECATED_APIS.put("javax.activation.DataSource", 
            new DeprecatedApiInfo("jakarta.activation.DataSource", "removed"));
        
        // JAXB - moved to separate namespace
        DEPRECATED_APIS.put("javax.xml.bind.Binder", 
            new DeprecatedApiInfo("jakarta.xml.bind.Binder", "changed"));
        DEPRECATED_APIS.put("javax.xml.bind.JAXBContext", 
            new DeprecatedApiInfo("jakarta.xml.bind.JAXBContext", "changed"));
        DEPRECATED_APIS.put("javax.xml.bind.Marshaller", 
            new DeprecatedApiInfo("jakarta.xml.bind.Marshaller", "changed"));
        DEPRECATED_APIS.put("javax.xml.bind.Unmarshaller", 
            new DeprecatedApiInfo("jakarta.xml.bind.Unmarshaller", "changed"));
        DEPRECATED_APIS.put("javax.xml.bind.annotation.XmlAccessType", 
            new DeprecatedApiInfo("jakarta.xml.bind.annotation.XmlAccessType", "changed"));
        
        // CORBA - removed entirely
        DEPRECATED_APIS.put("javax.rmi.CORBA.Stub", 
            new DeprecatedApiInfo("Use Jakarta Naming or direct instantiation", "removed"));
        DEPRECATED_APIS.put("javax.rmi.PortableRemoteObject", 
            new DeprecatedApiInfo("Use Jakarta RMI", "removed"));
        
        // JAXP - moved to Jakarta
        DEPRECATED_APIS.put("javax.xml.parsers.DocumentBuilder", 
            new DeprecatedApiInfo("jakarta.xml.parsers.DocumentBuilder", "changed"));
        DEPRECATED_APIS.put("javax.xml.parsers.SAXParser", 
            new DeprecatedApiInfo("jakarta.xml.parsers.SAXParser", "changed"));
        
        // XML Stream API
        DEPRECATED_APIS.put("javax.xml.stream.XMLInputFactory", 
            new DeprecatedApiInfo("jakarta.xml.stream.XMLInputFactory", "changed"));
        DEPRECATED_APIS.put("javax.xml.stream.XMLOutputFactory", 
            new DeprecatedApiInfo("jakarta.xml.stream.XMLOutputFactory", "changed"));
        
        // SAAJ - SOAP
        DEPRECATED_APIS.put("javax.xml.soap.SOAPMessage", 
            new DeprecatedApiInfo("jakarta.xml.soap.SOAPMessage", "changed"));
        
        // JAX-RS specific deprecated methods
        DEPRECATED_APIS.put("javax.ws.rs.core.Response.StatusType", 
            new DeprecatedApiInfo("Use jakarta.ws.rs.core.Response.StatusType", "deprecated"));
        
        // EJB removed in Jakarta EE 9
        DEPRECATED_APIS.put("javax.ejb.Stateful", 
            new DeprecatedApiInfo("Use Jakarta EE CDI @SessionScoped", "removed"));
        DEPRECATED_APIS.put("javax.ejb.Stateless", 
            new DeprecatedApiInfo("Use Jakarta EE CDI managed beans", "removed"));
        DEPRECATED_APIS.put("javax.ejb.Singleton", 
            new DeprecatedApiInfo("Use Jakarta EE CDI", "removed"));
        DEPRECATED_APIS.put("javax.ejb.MessageDriven", 
            new DeprecatedApiInfo("Use Jakarta Messaging", "removed"));
    }

    private record DeprecatedApiInfo(String jakartaEquivalent, String deprecationType) {}

    private final ThreadLocal<JavaParser> javaParserThreadLocal = ThreadLocal.withInitial(() -> JavaParser.fromJavaVersion().build());

    @Override
    public DeprecatedApiProjectScanResult scanProject(Path projectPath) {
        if (projectPath == null || !Files.exists(projectPath) || !Files.isDirectory(projectPath)) {
            return DeprecatedApiProjectScanResult.empty();
        }

        try {
            List<Path> javaFiles = discoverJavaFiles(projectPath);
            if (javaFiles.isEmpty()) return DeprecatedApiProjectScanResult.empty();

            AtomicInteger totalScanned = new AtomicInteger(0);
            List<DeprecatedApiScanResult> results = javaFiles.parallelStream()
                .map(file -> {
                    totalScanned.incrementAndGet();
                    DeprecatedApiScanResult result = scanFile(file);
                    return result.hasJavaxUsage() ? result : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            int totalUsages = results.stream().mapToInt(r -> r.usages().size()).sum();

            return new DeprecatedApiProjectScanResult(results, totalScanned.get(), results.size(), totalUsages);
        } catch (Exception e) {
            log.error("Error scanning project for deprecated APIs", e);
            return DeprecatedApiProjectScanResult.empty();
        }
    }

    @Override
    public DeprecatedApiScanResult scanFile(Path filePath) {
        if (filePath == null || !Files.exists(filePath)) {
            return DeprecatedApiScanResult.empty(filePath);
        }

        try {
            String content = Files.readString(filePath);
            int lineCount = content.split("\n").length;

            JavaParser parser = javaParserThreadLocal.get();
            parser.reset();

            List<SourceFile> sourceFiles = parser.parse(content).collect(Collectors.toList());
            if (sourceFiles.isEmpty()) return DeprecatedApiScanResult.empty(filePath);

            List<DeprecatedApiUsage> usages = new ArrayList<>();
            for (SourceFile sourceFile : sourceFiles) {
                if (sourceFile instanceof CompilationUnit) {
                    CompilationUnit cu = (CompilationUnit) sourceFile;
                    usages.addAll(extractDeprecatedApis(cu, content));
                }
            }

            return new DeprecatedApiScanResult(filePath, usages, lineCount);
        } catch (Exception e) {
            return DeprecatedApiScanResult.empty(filePath);
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

    private List<DeprecatedApiUsage> extractDeprecatedApis(CompilationUnit cu, String content) {
        List<DeprecatedApiUsage> usages = new ArrayList<>();
        String[] lines = content.split("\n");

        // Check imports for deprecated APIs
        for (J.Import imp : cu.getImports()) {
            String importName = imp.getQualid().toString();
            DeprecatedApiInfo info = DEPRECATED_APIS.get(importName);

            if (info != null) {
                int lineNumber = findLineNumber(lines, importName);
                usages.add(new DeprecatedApiUsage(importName, null, info.jakartaEquivalent(), lineNumber, "import", info.deprecationType()));
            }
        }

        // Also scan for @Deprecated annotations and deprecated method usages
        Pattern deprecatedPattern = Pattern.compile("@Deprecated|@Deprecated\\s");
        Matcher matcher = deprecatedPattern.matcher(content);
        
        while (matcher.find()) {
            int lineNumber = findLineNumber(lines, matcher.group(0));
            // Look for method calls or class references near this line
            usages.add(new DeprecatedApiUsage("javax.deprecated.usage", null, "Review usage", lineNumber, "usage", "deprecated"));
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
