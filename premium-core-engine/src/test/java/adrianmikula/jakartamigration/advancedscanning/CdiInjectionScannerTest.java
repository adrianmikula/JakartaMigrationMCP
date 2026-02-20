package adrianmikula.jakartamigration.advancedscanning;

import adrianmikula.jakartamigration.advancedscanning.domain.CdiInjectionProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.CdiInjectionScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.CdiInjectionUsage;
import adrianmikula.jakartamigration.advancedscanning.service.CdiInjectionScanner;
import adrianmikula.jakartamigration.advancedscanning.service.impl.CdiInjectionScannerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD Tests for CDI/Injection Scanner
 * Following Test-Driven Development: Write tests first, then implementation
 */
class CdiInjectionScannerTest {

    private CdiInjectionScanner scanner;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        scanner = new CdiInjectionScannerImpl();
    }

    @Test
    void shouldReturnEmptyResultForEmptyProject() {
        CdiInjectionProjectScanResult result = scanner.scanProject(tempDir);
        
        assertThat(result.totalFilesScanned()).isEqualTo(0);
        assertThat(result.totalAnnotationsFound()).isEqualTo(0);
        assertThat(result.hasJavaxUsage()).isFalse @Test
   ();
    }

    void shouldDetectCdiAnnotations() throws Exception {
        // Create test file with CDI annotations
        Path testFile = tempDir.resolve("TestBean.java");
        String content = """
            package com.example;
            
            import javax.inject.Inject;
            import javax.inject.Named;
            import javax.inject.Qualifier;
            import javax.enterprise.context.ApplicationScoped;
            import javax.enterprise.inject.Default;
            
            @ApplicationScoped
            @Named("testBean")
            @Default
            public class TestBean {
                @Inject
                private DependencyService dependency;
                
                @Inject
                public TestBean(@Named("service") Service svc) {
                }
            }
            """;
        Files.writeString(testFile, content);

        CdiInjectionProjectScanResult result = scanner.scanProject(tempDir);

        assertThat(result.hasJavaxUsage()).isTrue();
        assertThat(result.totalAnnotationsFound()).isGreaterThan(0);
    }

    @Test
    void shouldMapToJakartaEquivalents() throws Exception {
        Path testFile = tempDir.resolve("Service.java");
        String content = """
            package com.example;
            
            import javax.inject.Singleton;
            import javax.enterprise.context.RequestScoped;
            
            @Singleton
            @RequestScoped
            public class Service {
            }
            """;
        Files.writeString(testFile, content);

        CdiInjectionProjectScanResult result = scanner.scanProject(tempDir);
        
        assertThat(result.hasJavaxUsage()).isTrue();
        
        // Check that Jakarta equivalents are provided
        List<CdiInjectionScanResult> fileResults = result.fileResults();
        assertThat(fileResults).isNotEmpty();
        
        for (CdiInjectionScanResult fileResult : fileResults) {
            for (CdiInjectionUsage usage : fileResult.usages()) {
                assertThat(usage.jakartaEquivalent()).isNotBlank();
                assertThat(usage.jakartaEquivalent()).startsWith("jakarta.");
            }
        }
    }

    @Test
    void shouldReturnCorrectLineNumbers() throws Exception {
        Path testFile = tempDir.resolve("LineNumberTest.java");
        String content = """
            package com.example;
            
            import javax.inject.Inject;
            
            public class LineNumberTest {
                @Inject
                private Service service;
            }
            """;
        Files.writeString(testFile, content);

        CdiInjectionProjectScanResult result = scanner.scanProject(tempDir);

        assertThat(result.hasJavaxUsage()).isTrue();
        
        CdiInjectionScanResult fileResult = result.fileResults().get(0);
        assertThat(fileResult.usages()).isNotEmpty();
        
        // Line 4 is where @Inject appears
        CdiInjectionUsage usage = fileResult.usages().get(0);
        assertThat(usage.lineNumber()).isGreaterThan(0);
    }

    @Test
    void shouldHandleMultipleFiles() throws Exception {
        // Create multiple test files
        Files.writeString(tempDir.resolve("Bean1.java"), """
            import javax.inject.Inject;
            public class Bean1 { @Inject Service s; }
            """);
        Files.writeString(tempDir.resolve("Bean2.java"), """
            import javax.enterprise.context.ApplicationScoped;
            @ApplicationScoped
            public class Bean2 { }
            """);

        CdiInjectionProjectScanResult result = scanner.scanProject(tempDir);

        assertThat(result.totalFilesWithJavaxUsage()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void shouldExcludeBuildDirectories() throws Exception {
        // Create file in target directory (should be ignored)
        Path targetDir = tempDir.resolve("target");
        Files.createDirectories(targetDir);
        Path buildFile = targetDir.resolve("BuiltClass.java");
        Files.writeString(buildFile, "import javax.inject.Inject;");

        CdiInjectionProjectScanResult result = scanner.scanProject(tempDir);

        // Should not find any usages since build files are excluded
        assertThat(result.hasJavaxUsage()).isFalse();
    }
}
