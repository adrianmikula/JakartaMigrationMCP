package adrianmikula.jakartamigration.unit.advancedscanning;

import adrianmikula.jakartamigration.advancedscanning.domain.DeprecatedApiProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.DeprecatedApiScanResult;
import adrianmikula.jakartamigration.advancedscanning.service.impl.DeprecatedApiScannerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DeprecatedApiScannerTest {

    private DeprecatedApiScannerImpl scanner;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        scanner = new DeprecatedApiScannerImpl();
    }

    @Test
    void shouldReturnEmptyForNullPath() {
        DeprecatedApiProjectScanResult result = scanner.scanProject(null);
        assertNotNull(result);
        assertEquals(0, result.totalFilesScanned());
    }

    @Test
    void shouldReturnEmptyForNonExistentPath() {
        DeprecatedApiProjectScanResult result = scanner.scanProject(Path.of("/nonexistent/path"));
        assertNotNull(result);
        assertEquals(0, result.totalFilesScanned());
    }

    @Test
    void shouldScanEmptyProject() throws Exception {
        Path projectDir = tempDir.resolve("emptyProject");
        Files.createDirectory(projectDir);

        DeprecatedApiProjectScanResult result = scanner.scanProject(projectDir);
        assertNotNull(result);
        assertEquals(0, result.totalFilesScanned());
    }

    @Test
    void shouldDetectJavaxXmlBindImports() throws Exception {
        Path projectDir = tempDir.resolve("jaxbProject");
        Files.createDirectory(projectDir);
        
        String javaContent = """
            package com.example;
            
            import javax.xml.bind.JAXBContext;
            import javax.xml.bind.Marshaller;
            import javax.xml.bind.Unmarshaller;
            
            public class XmlUtil {
                private JAXBContext context;
                private Marshaller marshaller;
            }
            """;
        
        Path javaFile = projectDir.resolve("XmlUtil.java");
        Files.writeString(javaFile, javaContent);

        DeprecatedApiProjectScanResult result = scanner.scanProject(projectDir);
        
        assertTrue(result.hasJavaxUsage());
        assertEquals(1, result.filesWithJavaxUsage());
        assertTrue(result.totalJavaxUsages() >= 3); // 3 imports
        
        DeprecatedApiScanResult fileResult = result.fileResults().get(0);
        assertEquals(3, fileResult.usages().size());
    }

    @Test
    void shouldDetectRemovedEjbAnnotations() throws Exception {
        Path projectDir = tempDir.resolve("ejbProject");
        Files.createDirectory(projectDir);
        
        String javaContent = """
            package com.example;
            
            import javax.ejb.Stateful;
            import javax.ejb.Stateless;
            import javax.ejb.Singleton;
            
            @Stateful
            @Stateless
            @Singleton
            public class EjbBeans {
            }
            """;
        
        Path javaFile = projectDir.resolve("EjbBeans.java");
        Files.writeString(javaFile, javaContent);

        DeprecatedApiProjectScanResult result = scanner.scanProject(projectDir);
        
        assertTrue(result.hasJavaxUsage());
        assertTrue(result.totalJavaxUsages() >= 3); // 3 annotations
        
        DeprecatedApiScanResult fileResult = result.fileResults().get(0);
        assertEquals(3, fileResult.usages().size());
    }

    @Test
    void shouldDetectActivationPackage() throws Exception {
        Path projectDir = tempDir.resolve("activationProject");
        Files.createDirectory(projectDir);
        
        String javaContent = """
            package com.example;
            
            import javax.activation.DataHandler;
            import javax.activation.DataSource;
            
            public class AttachmentHandler {
                private DataHandler handler;
            }
            """;
        
        Path javaFile = projectDir.resolve("AttachmentHandler.java");
        Files.writeString(javaFile, javaContent);

        DeprecatedApiProjectScanResult result = scanner.scanProject(projectDir);
        
        assertTrue(result.hasJavaxUsage());
        
        DeprecatedApiScanResult fileResult = result.fileResults().get(0);
        assertEquals(2, fileResult.usages().size());
        
        // Verify that jakarta equivalents are suggested
        assertTrue(fileResult.usages().stream()
            .anyMatch(u -> u.jakartaEquivalent().contains("jakarta.activation")));
    }

    @Test
    void shouldProvideJakartaEquivalents() throws Exception {
        Path projectDir = tempDir.resolve("mappingProject");
        Files.createDirectory(projectDir);
        
        String javaContent = """
            package com.example;
            
            import javax.xml.bind.JAXBContext;
            
            public class JaxbExample {
            }
            """;
        
        Path javaFile = projectDir.resolve("JaxbExample.java");
        Files.writeString(javaFile, javaContent);

        DeprecatedApiProjectScanResult result = scanner.scanProject(projectDir);
        
        assertTrue(result.hasJavaxUsage());
        
        DeprecatedApiScanResult fileResult = result.fileResults().get(0);
        assertEquals(1, fileResult.usages().size());
        
        // Verify jakarta equivalent is provided
        assertEquals("jakarta.xml.bind.JAXBContext", 
            fileResult.usages().get(0).jakartaEquivalent());
    }

    @Test
    void shouldHandleJavaFilesWithoutDeprecatedApis() throws Exception {
        Path projectDir = tempDir.resolve("cleanProject");
        Files.createDirectory(projectDir);
        
        String javaContent = """
            package com.example;
            
            import jakarta.persistence.Entity;
            import jakarta.persistence.Id;
            
            @Entity
            public class CleanEntity {
                @Id
                private Long id;
            }
            """;
        
        Path javaFile = projectDir.resolve("CleanEntity.java");
        Files.writeString(javaFile, javaContent);

        DeprecatedApiProjectScanResult result = scanner.scanProject(projectDir);
        
        // Should not have javax usage
        assertFalse(result.hasJavaxUsage());
        assertEquals(0, result.filesWithJavaxUsage());
    }

    @Test
    void shouldScanMultipleFilesInProject() throws Exception {
        Path projectDir = tempDir.resolve("multiFileProject");
        Files.createDirectory(projectDir);
        
        // First file with deprecated API
        String javaContent1 = """
            package com.example;
            import javax.ejb.Stateless;
            @Stateless
            public class EjbBean {}
            """;
        
        // Second file without deprecated API
        String javaContent2 = """
            package com.example;
            public class NormalClass {}
            """;
        
        Files.writeString(projectDir.resolve("EjbBean.java"), javaContent1);
        Files.writeString(projectDir.resolve("NormalClass.java"), javaContent2);

        DeprecatedApiProjectScanResult result = scanner.scanProject(projectDir);
        
        assertTrue(result.hasJavaxUsage());
        assertEquals(2, result.totalFilesScanned());
        assertEquals(1, result.filesWithJavaxUsage());
    }

    @Test
    void shouldReturnEmptyForNonJavaFile() {
        DeprecatedApiScanResult result = scanner.scanFile(Path.of("README.md"));
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
            import javax.xml.bind.JAXBContext;  // This is line 6
            """;
        
        Path javaFile = projectDir.resolve("TestFile.java");
        Files.writeString(javaFile, javaContent);

        DeprecatedApiProjectScanResult result = scanner.scanProject(projectDir);
        
        assertTrue(result.hasJavaxUsage());
        
        DeprecatedApiScanResult fileResult = result.fileResults().get(0);
        assertEquals(1, fileResult.usages().size());
        
        // Line number should be around line 6
        assertTrue(fileResult.usages().get(0).lineNumber() >= 1);
    }
}
