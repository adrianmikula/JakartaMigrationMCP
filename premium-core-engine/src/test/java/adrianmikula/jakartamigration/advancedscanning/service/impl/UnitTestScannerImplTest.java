package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.service.UnitTestScanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for UnitTestScannerImpl
 */
@Tag("slow")
class UnitTestScannerImplTest {

    private UnitTestScanner scanner;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        scanner = new UnitTestScannerImpl();
    }

    @Test
    void shouldDetectJUnit4Usage() throws Exception {
        // Given
        Path packageDir = tempDir.resolve("src/test/java/com/example");
        Files.createDirectories(packageDir);

        Path javaFile = packageDir.resolve("JUnit4Test.java");
        String content = """
                package com.example;

                import org.junit.Test;
                import org.junit.Before;
                import org.junit.After;
                import javax.servlet.http.HttpServletRequest;
                import static org.junit.Assert.*;

                public class JUnit4Test {
                    @Before
                    public void setUp() {}

                    @Test
                    public void testSomething() {
                        assertEquals(1, 1);
                    }
                }
                """;
        Files.writeString(javaFile, content);

        // When
        var result = scanner.scanProject(tempDir);

        // Then
        assertThat(result.hasFindings()).isTrue();
        assertThat(result.getTotalFindings()).isGreaterThan(0);
    }

    @Test
    void shouldDetectMockitoUsage() throws Exception {
        // Given
        Path packageDir = tempDir.resolve("src/test/java/com/example");
        Files.createDirectories(packageDir);

        Path javaFile = packageDir.resolve("MockitoTest.java");
        String content = """
                package com.example;

                import org.mockito.Mockito;
                import org.mockito.Mock;
                import org.mockito.junit.MockitoJUnitRunner;
                import org.junit.runner.RunWith;
                import javax.inject.Inject;

                @RunWith(MockitoJUnitRunner.class)
                public class MockitoTest {
                    @Mock
                    private SomeService service;

                    @Test
                    public void testMock() {
                        Mockito.when(service.getValue()).thenReturn("test");
                    }
                }
                """;
        Files.writeString(javaFile, content);

        // When
        var result = scanner.scanProject(tempDir);

        // Then
        assertThat(result.hasFindings()).isTrue();
        assertThat(result.getTotalFindings()).isGreaterThan(0);
    }

    @Test
    void shouldReturnEmptyForProductionCode() throws Exception {
        // Given
        Path packageDir = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(packageDir);

        Path javaFile = packageDir.resolve("ProductionClass.java");
        String content = """
                package com.example;

                import javax.servlet.http.HttpServlet;

                public class ProductionClass extends HttpServlet {
                    public String getValue() {
                        return "value";
                    }
                }
                """;
        Files.writeString(javaFile, content);

        // When
        var result = scanner.scanProject(tempDir);

        // Then - Production code without test libraries should have no findings
        assertThat(result.getProjectPath()).isNotNull();
        assertThat(result.hasFindings()).isFalse();
    }

    @Test
    void shouldHandleEmptyDirectory() {
        // Given - Empty temp directory

        // When
        var result = scanner.scanProject(tempDir);

        // Then
        assertThat(result.hasFindings()).isFalse();
    }
}
