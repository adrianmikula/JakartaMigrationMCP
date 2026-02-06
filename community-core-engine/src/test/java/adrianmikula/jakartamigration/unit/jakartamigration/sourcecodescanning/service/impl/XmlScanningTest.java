package unit.jakartamigration.sourcecodescanning.service.impl;

import adrianmikula.jakartamigration.sourcecodescanning.domain.XmlFileUsage;
import adrianmikula.jakartamigration.sourcecodescanning.service.impl.SourceCodeScannerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class XmlScanningTest {
    
    private SourceCodeScannerImpl scanner;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        scanner = new SourceCodeScannerImpl();
    }
    
    @Test
    void shouldScanWebXmlWithJavaxNamespace() throws Exception {
        // Given
        Path webXml = tempDir.resolve("web.xml");
        String content = """
            <?xml version="1.0" encoding="UTF-8"?>
            <web-app xmlns="http://java.sun.com/xml/ns/javaee"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://java.sun.com/xml/ns/javaee
                     http://java.sun.com/xml/ns/javaee/web-app_3_1.xsd"
                     version="3.1">
                <servlet>
                    <servlet-name>MyServlet</servlet-name>
                    <servlet-class>javax.servlet.http.HttpServlet</servlet-class>
                </servlet>
            </web-app>
            """;
        Files.writeString(webXml, content);
        
        // When
        List<XmlFileUsage> usages = scanner.scanXmlFiles(tempDir);
        
        // Then
        assertThat(usages).hasSize(1);
        XmlFileUsage usage = usages.get(0);
        assertThat(usage.hasJavaxUsage()).isTrue();
        assertThat(usage.namespaceUsages()).isNotEmpty();
        assertThat(usage.classReferences()).isNotEmpty();
    }
    
    @Test
    void shouldScanPersistenceXmlWithJavaxNamespace() throws Exception {
        // Given
        Path persistenceXml = tempDir.resolve("persistence.xml");
        String content = """
            <?xml version="1.0" encoding="UTF-8"?>
            <persistence xmlns="http://java.sun.com/xml/ns/persistence"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         version="2.2">
                <persistence-unit name="myPU">
                    <provider>javax.persistence.EntityManager</provider>
                </persistence-unit>
            </persistence>
            """;
        Files.writeString(persistenceXml, content);
        
        // When
        List<XmlFileUsage> usages = scanner.scanXmlFiles(tempDir);
        
        // Then
        assertThat(usages).hasSize(1);
        XmlFileUsage usage = usages.get(0);
        assertThat(usage.hasJavaxUsage()).isTrue();
        assertThat(usage.namespaceUsages()).isNotEmpty();
    }
    
    @Test
    void shouldReturnEmptyForXmlWithoutJavaxUsage() throws Exception {
        // Given
        Path xmlFile = tempDir.resolve("config.xml");
        String content = """
            <?xml version="1.0" encoding="UTF-8"?>
            <config>
                <property name="key">value</property>
            </config>
            """;
        Files.writeString(xmlFile, content);
        
        // When
        List<XmlFileUsage> usages = scanner.scanXmlFiles(tempDir);
        
        // Then
        assertThat(usages).isEmpty();
    }
    
    @Test
    void shouldExcludeBuildDirectories() throws Exception {
        // Given
        Path srcDir = tempDir.resolve("src/main/resources");
        Files.createDirectories(srcDir);
        Path webXml = srcDir.resolve("web.xml");
        Files.writeString(webXml, """
            <web-app xmlns="http://java.sun.com/xml/ns/javaee">
            </web-app>
            """);
        
        // Create build directory with XML
        Path targetDir = tempDir.resolve("target/classes");
        Files.createDirectories(targetDir);
        Path buildXml = targetDir.resolve("web.xml");
        Files.writeString(buildXml, """
            <web-app xmlns="http://java.sun.com/xml/ns/javaee">
            </web-app>
            """);
        
        // When
        List<XmlFileUsage> usages = scanner.scanXmlFiles(tempDir);
        
        // Then
        // Should only find the source file, not the build file
        assertThat(usages).hasSize(1);
        assertThat(usages.get(0).filePath()).isEqualTo(webXml);
    }
}

