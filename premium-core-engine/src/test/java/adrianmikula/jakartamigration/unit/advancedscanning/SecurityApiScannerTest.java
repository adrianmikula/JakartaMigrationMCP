package adrianmikula.jakartamigration.unit.advancedscanning;

import adrianmikula.jakartamigration.advancedscanning.domain.SecurityApiProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.SecurityApiScanResult;
import adrianmikula.jakartamigration.advancedscanning.service.impl.SecurityApiScannerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SecurityApiScannerTest {

    private SecurityApiScannerImpl scanner;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        scanner = new SecurityApiScannerImpl();
    }

    @Test
    void shouldReturnEmptyForNullPath() {
        SecurityApiProjectScanResult result = scanner.scanProject(null);
        assertNotNull(result);
        assertEquals(0, result.totalFilesScanned());
    }

    @Test
    void shouldReturnEmptyForNonExistentPath() {
        SecurityApiProjectScanResult result = scanner.scanProject(Path.of("/nonexistent/path"));
        assertNotNull(result);
        assertEquals(0, result.totalFilesScanned());
    }

    @Test
    void shouldScanEmptyProject() throws Exception {
        Path projectDir = tempDir.resolve("emptyProject");
        Files.createDirectory(projectDir);

        SecurityApiProjectScanResult result = scanner.scanProject(projectDir);
        assertNotNull(result);
        assertEquals(0, result.totalFilesScanned());
    }

    @Test
    void shouldDetectJaasImports() throws Exception {
        Path projectDir = tempDir.resolve("jaasProject");
        Files.createDirectory(projectDir);
        
        String javaContent = """
            package com.example;
            
            import javax.security.auth.callback.CallbackHandler;
            import javax.security.auth.login.LoginContext;
            import javax.security.auth.Subject;
            
            public class AuthService {
                private CallbackHandler handler;
                private LoginContext loginContext;
            }
            """;
        
        Path javaFile = projectDir.resolve("AuthService.java");
        Files.writeString(javaFile, javaContent);

        SecurityApiProjectScanResult result = scanner.scanProject(projectDir);
        
        assertTrue(result.hasJavaxUsage());
        assertEquals(1, result.filesWithJavaxUsage());
        assertTrue(result.totalJavaxUsages() >= 3);
        
        SecurityApiScanResult fileResult = result.fileResults().get(0);
        assertEquals(3, fileResult.usages().size());
        
        // Verify category
        assertTrue(fileResult.usages().stream().allMatch(u -> u.getContext().equals("JAAS")));
    }

    @Test
    void shouldDetectJaccImports() throws Exception {
        Path projectDir = tempDir.resolve("jaccProject");
        Files.createDirectory(projectDir);
        
        String javaContent = """
            package com.example;
            
            import javax.security.jacc.PolicyContext;
            import javax.security.jacc.WebResourcePermission;
            import javax.security.jacc.WebRoleRefPermission;
            
            public class PolicyChecker {
                public void checkPermission() {
                    String context = PolicyContext.getContextID();
                }
            }
            """;
        
        Path javaFile = projectDir.resolve("PolicyChecker.java");
        Files.writeString(javaFile, javaContent);

        SecurityApiProjectScanResult result = scanner.scanProject(projectDir);
        
        assertTrue(result.hasJavaxUsage());
        
        SecurityApiScanResult fileResult = result.fileResults().get(0);
        assertEquals(3, fileResult.usages().size());
        
        // Verify category
        assertTrue(fileResult.usages().stream().allMatch(u -> u.getContext().equals("JACC")));
    }

    @Test
    void shouldDetectSecurityApiImports() throws Exception {
        Path projectDir = tempDir.resolve("securityApiProject");
        Files.createDirectory(projectDir);
        
        String javaContent = """
            package com.example;
            
            import javax.security.enterprise.SecurityContext;
            import javax.security.enterprise.identitystore.IdentityStore;
            import javax.security.enterprise.identitystore.DatabaseIdentityStoreDefinition;
            
            @DatabaseIdentityStoreDefinition(dataSourceLookup = "java:comp/DefaultDataSource")
            public class SecureBean {
                @Inject
                private SecurityContext securityContext;
                
                @Inject
                private IdentityStore identityStore;
            }
            """;
        
        Path javaFile = projectDir.resolve("SecureBean.java");
        Files.writeString(javaFile, javaContent);

        SecurityApiProjectScanResult result = scanner.scanProject(projectDir);
        
        assertTrue(result.hasJavaxUsage());
        
        SecurityApiScanResult fileResult = result.fileResults().get(0);
        assertEquals(3, fileResult.usages().size());
        
        // Verify category
        assertTrue(fileResult.usages().stream().allMatch(u -> u.getContext().equals("Security API")));
    }

    @Test
    void shouldProvideJakartaEquivalents() throws Exception {
        Path projectDir = tempDir.resolve("mappingProject");
        Files.createDirectory(projectDir);
        
        String javaContent = """
            package com.example;
            
            import javax.security.auth.login.LoginContext;
            
            public class LoginExample {
            }
            """;
        
        Path javaFile = projectDir.resolve("LoginExample.java");
        Files.writeString(javaFile, javaContent);

        SecurityApiProjectScanResult result = scanner.scanProject(projectDir);
        
        assertTrue(result.hasJavaxUsage());
        
        SecurityApiScanResult fileResult = result.fileResults().get(0);
        assertEquals(1, fileResult.usages().size());
        
        // Verify jakarta equivalent is provided
        assertEquals("jakarta.security.auth.login.LoginContext", 
            fileResult.usages().get(0).getJakartaEquivalent());
    }

    @Test
    void shouldHandleJavaFilesWithoutSecurityApis() throws Exception {
        Path projectDir = tempDir.resolve("cleanProject");
        Files.createDirectory(projectDir);
        
        String javaContent = """
            package com.example;
            
            import jakarta.security.auth.message.callback.CertificateValidationCallback;
            
            public class CleanClass {
            }
            """;
        
        Path javaFile = projectDir.resolve("CleanClass.java");
        Files.writeString(javaFile, javaContent);

        SecurityApiProjectScanResult result = scanner.scanProject(projectDir);
        
        // Should not have javax usage
        assertFalse(result.hasJavaxUsage());
        assertEquals(0, result.filesWithJavaxUsage());
    }

    @Test
    void shouldScanMultipleFilesInProject() throws Exception {
        Path projectDir = tempDir.resolve("multiFileProject");
        Files.createDirectory(projectDir);
        
        // First file with security API
        String javaContent1 = """
            package com.example;
            import javax.security.auth.callback.CallbackHandler;
            public class AuthHandler {}
            """;
        
        // Second file without security API
        String javaContent2 = """
            package com.example;
            public class NormalClass {}
            """;
        
        Files.writeString(projectDir.resolve("AuthHandler.java"), javaContent1);
        Files.writeString(projectDir.resolve("NormalClass.java"), javaContent2);

        SecurityApiProjectScanResult result = scanner.scanProject(projectDir);
        
        assertTrue(result.hasJavaxUsage());
        assertEquals(2, result.totalFilesScanned());
        assertEquals(1, result.filesWithJavaxUsage());
    }

    @Test
    void shouldReturnEmptyForNonJavaFile() {
        SecurityApiScanResult result = scanner.scanFile(Path.of("README.md"));
        assertNotNull(result);
        assertEquals(0, result.usages().size());
    }

    @Test
    void shouldTrackLineNumbers() throws Exception {
        Path projectDir = tempDir.resolve("lineTest");
        Files.createDirectory(projectDir);
        
        String javaContent = """
            package com.example;
            
            // This is line 3
            // This is line 4
            import javax.security.auth.callback.CallbackHandler;  // This is line 6
            """;
        
        Path javaFile = projectDir.resolve("TestFile.java");
        Files.writeString(javaFile, javaContent);

        SecurityApiProjectScanResult result = scanner.scanProject(projectDir);
        
        assertTrue(result.hasJavaxUsage());
        
        SecurityApiScanResult fileResult = result.fileResults().get(0);
        assertEquals(1, fileResult.usages().size());
        
        // Line number should be around line 6
        assertTrue(fileResult.usages().get(0).getLineNumber() >= 1);
    }

    @Test
    void shouldHandleMixedJakartaAndJavax() throws Exception {
        Path projectDir = tempDir.resolve("mixedProject");
        Files.createDirectory(projectDir);
        
        String javaContent = """
            package com.example;
            
            import javax.security.auth.callback.CallbackHandler;
            import jakarta.security.auth.message.callback.CallbackHandler;
            
            public class MixedHandler {
                // Both javax and jakarta imports present
            }
            """;
        
        Path javaFile = projectDir.resolve("MixedHandler.java");
        Files.writeString(javaFile, javaContent);

        SecurityApiProjectScanResult result = scanner.scanProject(projectDir);
        
        // Should detect javax.security usage
        assertTrue(result.hasJavaxUsage());
        
        SecurityApiScanResult fileResult = result.fileResults().get(0);
        // Should detect only javax import
        assertEquals(1, fileResult.usages().size());
    }
}
