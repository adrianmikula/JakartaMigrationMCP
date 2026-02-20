package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.SecurityApiProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.SecurityApiScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.SecurityApiUsage;
import adrianmikula.jakartamigration.advancedscanning.service.SecurityApiScanner;
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
public class SecurityApiScannerImpl implements SecurityApiScanner {

    // Known javax.security packages and their Jakarta equivalents
    private static final Map<String, SecurityApiInfo> SECURITY_APIS = new HashMap<>();

    static {
        // JAAS - Java Authentication and Authorization Service
        SECURITY_APIS.put("javax.security.auth.callback.CallbackHandler", 
            new SecurityApiInfo("jakarta.security.auth.callback.CallbackHandler", "JAAS"));
        SECURITY_APIS.put("javax.security.auth.login.LoginContext", 
            new SecurityApiInfo("jakarta.security.auth.login.LoginContext", "JAAS"));
        SECURITY_APIS.put("javax.security.auth.Subject", 
            new SecurityApiInfo("jakarta.security.auth.Subject", "JAAS"));
        SECURITY_APIS.put("javax.security.auth.spi.LoginModule", 
            new SecurityApiInfo("jakarta.security.auth.spi.LoginModule", "JAAS"));
        SECURITY_APIS.put("javax.security.auth.AuthPermission", 
            new SecurityApiInfo("jakarta.security.auth.AuthPermission", "JAAS"));
        
        // JACC - Java Authorization Contract for Containers
        SECURITY_APIS.put("javax.security.jacc.PolicyContext", 
            new SecurityApiInfo("jakarta.security.jacc.PolicyContext", "JACC"));
        SECURITY_APIS.put("javax.security.jacc.PolicyContextException", 
            new SecurityApiInfo("jakarta.security.jacc.PolicyContextException", "JACC"));
        SECURITY_APIS.put("javax.security.jacc.PolicyConfiguration", 
            new SecurityApiInfo("jakarta.security.jacc.PolicyConfiguration", "JACC"));
        SECURITY_APIS.put("javax.security.jacc.PolicyConfigurationFactory", 
            new SecurityApiInfo("jakarta.security.jacc.PolicyConfigurationFactory", "JACC"));
        SECURITY_APIS.put("javax.security.jacc.WebResourcePermission", 
            new SecurityApiInfo("jakarta.security.jacc.WebResourcePermission", "JACC"));
        SECURITY_APIS.put("javax.security.jacc.WebRoleRefPermission", 
            new SecurityApiInfo("jakarta.security.jacc.WebRoleRefPermission", "JACC"));
        SECURITY_APIS.put("javax.security.jacc.EJBRoleRefPermission", 
            new SecurityApiInfo("jakarta.security.jacc.EJBRoleRefPermission", "JACC"));
        
        // Security API (JSR 375) - but this was added in Java EE 8
        SECURITY_APIS.put("javax.security.enterprise.SecurityContext", 
            new SecurityApiInfo("jakarta.security.enterprise.SecurityContext", "Security API"));
        SECURITY_APIS.put("javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism", 
            new SecurityApiInfo("jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism", "Security API"));
        SECURITY_APIS.put("javax.security.enterprise.identitystore.IdentityStore", 
            new SecurityApiInfo("jakarta.security.enterprise.identitystore.IdentityStore", "Security API"));
        SECURITY_APIS.put("javax.security.enterprise.identitystore.IdentityStoreHandler", 
            new SecurityApiInfo("jakarta.security.enterprise.identitystore.IdentityStoreHandler", "Security API"));
        SECURITY_APIS.put("javax.security.enterprise.identitystore.LdapIdentityStoreDefinition", 
            new SecurityApiInfo("jakarta.security.enterprise.identitystore.LdapIdentityStoreDefinition", "Security API"));
        SECURITY_APIS.put("javax.security.enterprise.identitystore.DatabaseIdentityStoreDefinition", 
            new SecurityApiInfo("jakarta.security.enterprise.identitystore.DatabaseIdentityStoreDefinition", "Security API"));
        SECURITY_APIS.put("javax.security.enterprise.identitystore.Credential", 
            new SecurityApiInfo("jakarta.security.enterprise.identitystore.Credential", "Security API"));
        SECURITY_APIS.put("javax.security.enterprise.identitystore.IdentityStore.ValidationType", 
            new SecurityApiInfo("jakarta.security.enterprise.identitystore.IdentityStore.ValidationType", "Security API"));
        
        // Additional security-related classes
        SECURITY_APIS.put("javax.security.cert.X509Certificate", 
            new SecurityApiInfo("java.security.cert.X509Certificate (moved to standard Java)", "Certificate"));
    }

    private record SecurityApiInfo(String jakartaEquivalent, String category) {}

    private final ThreadLocal<JavaParser> javaParserThreadLocal = ThreadLocal.withInitial(() -> JavaParser.fromJavaVersion().build());

    // Pattern for javax.security imports
    private static final Pattern SECURITY_IMPORT_PATTERN = Pattern.compile(
        "import\\s+javax\\.security[\\.\\w]*;",
        Pattern.MULTILINE
    );

    @Override
    public SecurityApiProjectScanResult scanProject(Path projectPath) {
        if (projectPath == null || !Files.exists(projectPath) || !Files.isDirectory(projectPath)) {
            return SecurityApiProjectScanResult.empty();
        }

        try {
            List<Path> javaFiles = discoverJavaFiles(projectPath);
            if (javaFiles.isEmpty()) return SecurityApiProjectScanResult.empty();

            AtomicInteger totalScanned = new AtomicInteger(0);
            List<SecurityApiScanResult> results = javaFiles.parallelStream()
                .map(file -> {
                    totalScanned.incrementAndGet();
                    SecurityApiScanResult result = scanFile(file);
                    return result.hasJavaxUsage() ? result : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            int totalUsages = results.stream().mapToInt(r -> r.getUsages().size()).sum();

            return new SecurityApiProjectScanResult(results, totalScanned.get(), results.size(), totalUsages);
        } catch (Exception e) {
            log.error("Error scanning project for security APIs", e);
            return SecurityApiProjectScanResult.empty();
        }
    }

    @Override
    public SecurityApiScanResult scanFile(Path filePath) {
        if (filePath == null || !Files.exists(filePath)) {
            return SecurityApiScanResult.empty(filePath);
        }

        try {
            String content = Files.readString(filePath);
            int lineCount = content.split("\n").length;

            // Quick check using regex first for performance
            Matcher matcher = SECURITY_IMPORT_PATTERN.matcher(content);
            if (!matcher.find()) {
                return SecurityApiScanResult.empty(filePath);
            }

            // Reset matcher and find all
            matcher.reset();
            List<String> foundImports = new ArrayList<>();
            while (matcher.find()) {
                String importMatch = matcher.group();
                // Extract the full class name
                String className = importMatch.replace("import ", "").replace(";", "").trim();
                foundImports.add(className);
            }

            if (foundImports.isEmpty()) {
                return SecurityApiScanResult.empty(filePath);
            }

            // Use OpenRewrite for detailed analysis
            JavaParser parser = javaParserThreadLocal.get();
            parser.reset();

            List<SourceFile> sourceFiles = parser.parse(content).collect(Collectors.toList());
            if (sourceFiles.isEmpty()) return SecurityApiScanResult.empty(filePath);

            List<SecurityApiUsage> usages = new ArrayList<>();
            for (SourceFile sourceFile : sourceFiles) {
                if (sourceFile instanceof CompilationUnit) {
                    CompilationUnit cu = (CompilationUnit) sourceFile;
                    usages.addAll(extractSecurityApis(cu, content, foundImports));
                }
            }

            return new SecurityApiScanResult(filePath, usages, lineCount);
        } catch (Exception e) {
            return SecurityApiScanResult.empty(filePath);
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

    private List<SecurityApiUsage> extractSecurityApis(CompilationUnit cu, String content, List<String> foundImports) {
        List<SecurityApiUsage> usages = new ArrayList<>();
        String[] lines = content.split("\n");

        for (String importName : foundImports) {
            SecurityApiInfo info = SECURITY_APIS.get(importName);
            if (info != null) {
                int lineNumber = findLineNumber(lines, importName);
                usages.add(new SecurityApiUsage(importName, null, info.jakartaEquivalent(), lineNumber, info.category()));
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
