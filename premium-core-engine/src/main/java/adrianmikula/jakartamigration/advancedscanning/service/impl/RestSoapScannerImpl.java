package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.RestSoapProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.RestSoapScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.RestSoapUsage;
import adrianmikula.jakartamigration.advancedscanning.service.RestSoapScanner;
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
public class RestSoapScannerImpl implements RestSoapScanner {

    private static final Map<String, String> REST_SOAP_MAPPINGS = new HashMap<>();

    static {
        // JAX-RS
        REST_SOAP_MAPPINGS.put("javax.ws.rs.Path", "jakarta.ws.rs.Path");
        REST_SOAP_MAPPINGS.put("javax.ws.rs.GET", "jakarta.ws.rs.GET");
        REST_SOAP_MAPPINGS.put("javax.ws.rs.POST", "jakarta.ws.rs.POST");
        REST_SOAP_MAPPINGS.put("javax.ws.rs.PUT", "jakarta.ws.rs.PUT");
        REST_SOAP_MAPPINGS.put("javax.ws.rs.DELETE", "jakarta.ws.rs.DELETE");
        REST_SOAP_MAPPINGS.put("javax.ws.rs.PATCH", "jakarta.ws.rs.PATCH");
        REST_SOAP_MAPPINGS.put("javax.ws.rs.HEAD", "jakarta.ws.rs.HEAD");
        REST_SOAP_MAPPINGS.put("javax.ws.rs.OPTIONS", "jakarta.ws.rs.OPTIONS");
        REST_SOAP_MAPPINGS.put("javax.ws.rs.PathParam", "jakarta.ws.rs.PathParam");
        REST_SOAP_MAPPINGS.put("javax.ws.rs.QueryParam", "jakarta.ws.rs.QueryParam");
        REST_SOAP_MAPPINGS.put("javax.ws.rs.HeaderParam", "jakarta.ws.rs.HeaderParam");
        REST_SOAP_MAPPINGS.put("javax.ws.rs.CookieParam", "jakarta.ws.rs.CookieParam");
        REST_SOAP_MAPPINGS.put("javax.ws.rs.FormParam", "jakarta.ws.rs.FormParam");
        REST_SOAP_MAPPINGS.put("javax.ws.rs.MatrixParam", "jakarta.ws.rs.MatrixParam");
        REST_SOAP_MAPPINGS.put("javax.ws.rs.BeanParam", "jakarta.ws.rs.BeanParam");
        REST_SOAP_MAPPINGS.put("javax.ws.rs.Consumes", "jakarta.ws.rs.Consumes");
        REST_SOAP_MAPPINGS.put("javax.ws.rs.Produces", "jakarta.ws.rs.Produces");
        REST_SOAP_MAPPINGS.put("javax.ws.rs.core.Response", "jakarta.ws.rs.core.Response");
        REST_SOAP_MAPPINGS.put("javax.ws.rs.core.MediaType", "jakarta.ws.rs.core.MediaType");
        REST_SOAP_MAPPINGS.put("javax.ws.rs.ext.Provider", "jakarta.ws.rs.ext.Provider");
        REST_SOAP_MAPPINGS.put("javax.ws.rs.Application", "jakarta.ws.rs.Application");

        // JAX-WS (SOAP)
        REST_SOAP_MAPPINGS.put("javax.xml.ws.WebService", "jakarta.xml.ws.WebService");
        REST_SOAP_MAPPINGS.put("javax.xml.ws.WebMethod", "jakarta.xml.ws.WebMethod");
        REST_SOAP_MAPPINGS.put("javax.xml.ws.WebParam", "jakarta.xml.ws.WebParam");
        REST_SOAP_MAPPINGS.put("javax.xml.ws.WebFault", "jakarta.xml.ws.WebFault");
        REST_SOAP_MAPPINGS.put("javax.xml.ws.Endpoint", "jakarta.xml.ws.Endpoint");
        REST_SOAP_MAPPINGS.put("javax.xml.ws.soap.SOAPBinding", "jakarta.xml.ws.soap.SOAPBinding");
        REST_SOAP_MAPPINGS.put("javax.xml.ws.soap.SOAPFaultException", "jakarta.xml.ws.soap.SOAPFaultException");
        REST_SOAP_MAPPINGS.put("javax.xml.ws.http.HTTPBinding", "jakarta.xml.ws.http.HTTPBinding");
        REST_SOAP_MAPPINGS.put("javax.xml.ws.http.HTTPException", "jakarta.xml.ws.http.HTTPException");
        REST_SOAP_MAPPINGS.put("javax.xml.ws.RespectBinding", "jakarta.xml.ws.RespectBinding");
        REST_SOAP_MAPPINGS.put("javax.xml.ws.RespectBindingFeature", "jakarta.xml.ws.RespectBindingFeature");
        REST_SOAP_MAPPINGS.put("javax.xml.ws.WebServiceProvider", "jakarta.xml.ws.WebServiceProvider");
        REST_SOAP_MAPPINGS.put("javax.xml.ws.Dispatch", "jakarta.xml.ws.Dispatch");
        REST_SOAP_MAPPINGS.put("javax.xml.ws.Service", "jakarta.xml.ws.Service");
        REST_SOAP_MAPPINGS.put("javax.xml.ws.EndpointReference", "jakarta.xml.ws.EndpointReference");
        REST_SOAP_MAPPINGS.put("javax.xml.ws.BindingType", "jakarta.xml.ws.BindingType");
        REST_SOAP_MAPPINGS.put("javax.xml.ws.BindingTypeFeature", "jakarta.xml.ws.BindingTypeFeature");
        REST_SOAP_MAPPINGS.put("javax.xml.ws.WebServiceRefs", "jakarta.xml.ws.WebServiceRefs");
        REST_SOAP_MAPPINGS.put("javax.xml.ws.WebServiceRef", "jakarta.xml.ws.WebServiceRef");
        REST_SOAP_MAPPINGS.put("javax.xml.ws.Action", "jakarta.xml.ws.Action");
        REST_SOAP_MAPPINGS.put("javax.xml.ws.FaultAction", "jakarta.xml.ws.FaultAction");

        // JSR-181 (Web Services Metadata)
        REST_SOAP_MAPPINGS.put("javax.jws.WebService", "jakarta.jws.WebService");
        REST_SOAP_MAPPINGS.put("javax.jws.WebMethod", "jakarta.jws.WebMethod");
        REST_SOAP_MAPPINGS.put("javax.jws.WebParam", "jakarta.jws.WebParam");
        REST_SOAP_MAPPINGS.put("javax.jws.WebResult", "jakarta.jws.WebResult");
        REST_SOAP_MAPPINGS.put("javax.jws.OneWay", "jakarta.jws.OneWay");
        REST_SOAP_MAPPINGS.put("javax.jws.SOAPBinding", "jakarta.jws.SOAPBinding");

        // SOAP with Attachments API for Java (SAAJ)
        REST_SOAP_MAPPINGS.put("javax.xml.soap.SOAPMessage", "jakarta.xml.soap.SOAPMessage");
        REST_SOAP_MAPPINGS.put("javax.xml.soap.SOAPEnvelope", "jakarta.xml.soap.SOAPEnvelope");
        REST_SOAP_MAPPINGS.put("javax.xml.soap.SOAPBody", "jakarta.xml.soap.SOAPBody");
        REST_SOAP_MAPPINGS.put("javax.xml.soap.SOAPHeader", "jakarta.xml.soap.SOAPHeader");
        REST_SOAP_MAPPINGS.put("javax.xml.soap.SOAPPart", "jakarta.xml.soap.SOAPPart");
        REST_SOAP_MAPPINGS.put("javax.xml.soap.SOAPElement", "jakarta.xml.soap.SOAPElement");
        REST_SOAP_MAPPINGS.put("javax.xml.soap.SOAPFactory", "jakarta.xml.soap.SOAPFactory");
        REST_SOAP_MAPPINGS.put("javax.xml.soap.MessageFactory", "jakarta.xml.soap.MessageFactory");
        REST_SOAP_MAPPINGS.put("javax.xml.soap.SOAPConnection", "jakarta.xml.soap.SOAPConnection");
        REST_SOAP_MAPPINGS.put("javax.xml.soap.SOAPConnectionFactory", "jakarta.xml.soap.SOAPConnectionFactory");
    }

    private final ThreadLocal<JavaParser> javaParserThreadLocal = ThreadLocal.withInitial(() -> JavaParser.fromJavaVersion().build());

    @Override
    public RestSoapProjectScanResult scanProject(Path projectPath) {
        if (projectPath == null || !Files.exists(projectPath) || !Files.isDirectory(projectPath)) {
            return RestSoapProjectScanResult.empty();
        }

        try {
            List<Path> javaFiles = discoverJavaFiles(projectPath);
            if (javaFiles.isEmpty()) return RestSoapProjectScanResult.empty();

            AtomicInteger totalScanned = new AtomicInteger(0);
            List<RestSoapScanResult> results = javaFiles.parallelStream()
                .map(file -> {
                    totalScanned.incrementAndGet();
                    RestSoapScanResult result = scanFile(file);
                    return result.hasJavaxUsage() ? result : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            int totalUsages = results.stream().mapToInt(r -> r.usages().size()).sum();

            return new RestSoapProjectScanResult(results, totalScanned.get(), results.size(), totalUsages);
        } catch (Exception e) {
            log.error("Error scanning project for REST/SOAP", e);
            return RestSoapProjectScanResult.empty();
        }
    }

    @Override
    public RestSoapScanResult scanFile(Path filePath) {
        if (filePath == null || !Files.exists(filePath)) {
            return RestSoapScanResult.empty(filePath);
        }

        try {
            String content = Files.readString(filePath);
            int lineCount = content.split("\n").length;

            JavaParser parser = javaParserThreadLocal.get();
            parser.reset();

            List<SourceFile> sourceFiles = parser.parse(content).collect(Collectors.toList());
            if (sourceFiles.isEmpty()) return RestSoapScanResult.empty(filePath);

            List<RestSoapUsage> usages = new ArrayList<>();
            for (SourceFile sourceFile : sourceFiles) {
                if (sourceFile instanceof CompilationUnit) {
                    CompilationUnit cu = (CompilationUnit) sourceFile;
                    usages.addAll(extractRestSoapUsages(cu, content));
                }
            }

            return new RestSoapScanResult(filePath, usages, lineCount);
        } catch (Exception e) {
            return RestSoapScanResult.empty(filePath);
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

    private List<RestSoapUsage> extractRestSoapUsages(CompilationUnit cu, String content) {
        List<RestSoapUsage> usages = new ArrayList<>();
        String[] lines = content.split("\n");

        // Check imports
        for (J.Import imp : cu.getImports()) {
            String importName = imp.getQualid().toString();
            if (importName.startsWith("javax.ws.rs.") || 
                importName.startsWith("javax.xml.ws.") ||
                importName.startsWith("javax.jws.") ||
                importName.startsWith("javax.xml.soap.")) {
                
                String jakartaEquivalent = REST_SOAP_MAPPINGS.get(importName);
                int lineNumber = findLineNumber(lines, importName);
                String type = getUsageType(importName);

                usages.add(new RestSoapUsage(
                    importName,
                    jakartaEquivalent != null ? jakartaEquivalent : importName.replace("javax.", "jakarta."),
                    lineNumber, "import", type
                ));
            }
        }

        // Search for annotation usages
        Pattern annotationPattern = Pattern.compile("@((?:javax\\\\.ws\\\\.rs[\\\\w.]*|javax\\\\.xml\\\\.ws[\\\\w.]*|javax\\\\.jws[\\\\w.]*|javax\\\\.xml\\\\.soap[\\\\w.]*))");
        Matcher matcher = annotationPattern.matcher(content);

        while (matcher.find()) {
            String annotationName = matcher.group(1);
            String jakartaEquivalent = REST_SOAP_MAPPINGS.get(annotationName);
            int lineNumber = findLineNumber(lines, matcher.group(0));
            String type = getUsageType(annotationName);

            usages.add(new RestSoapUsage(
                annotationName,
                jakartaEquivalent != null ? jakartaEquivalent : annotationName.replace("javax.", "jakarta."),
                lineNumber, "annotation", type
            ));
        }

        return usages;
    }

    private String getUsageType(String className) {
        if (className.startsWith("javax.ws.rs.")) return "rest";
        if (className.startsWith("javax.xml.ws.")) return "soap";
        if (className.startsWith("javax.jws.")) return "soap";
        if (className.startsWith("javax.xml.soap.")) return "soap";
        return "unknown";
    }

    private int findLineNumber(String[] lines, String searchText) {
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains(searchText)) return i + 1;
        }
        return 1;
    }
}
