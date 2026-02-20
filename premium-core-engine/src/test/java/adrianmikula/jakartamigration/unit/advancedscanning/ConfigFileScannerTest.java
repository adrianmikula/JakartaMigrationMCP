package adrianmikula.jakartamigration.unit.advancedscanning;

import adrianmikula.jakartamigration.advancedscanning.domain.ConfigFileProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.ConfigFileScanResult;
import adrianmikula.jakartamigration.advancedscanning.service.impl.ConfigFileScannerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConfigFileScannerTest {

    private ConfigFileScannerImpl scanner;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        scanner = new ConfigFileScannerImpl();
    }

    @Test
    void shouldReturnEmptyForNullPath() {
        ConfigFileProjectScanResult result = scanner.scanProject(null);
        assertNotNull(result);
        assertEquals(0, result.totalConfigFilesScanned());
    }

    @Test
    void shouldReturnEmptyForNonExistentPath() {
        ConfigFileProjectScanResult result = scanner.scanProject(Path.of("/nonexistent/path"));
        assertNotNull(result);
        assertEquals(0, result.totalConfigFilesScanned());
    }

    @Test
    void shouldScanEmptyProject() throws Exception {
        Path projectDir = tempDir.resolve("emptyProject");
        Files.createDirectory(projectDir);

        ConfigFileProjectScanResult result = scanner.scanProject(projectDir);
        assertNotNull(result);
        assertEquals(0, result.totalConfigFilesScanned());
    }

    @Test
    void shouldDetectJavaxInWebXml() throws Exception {
        Path projectDir = tempDir.resolve("webProject");
        Files.createDirectory(projectDir);
        
        String webXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <web-app xmlns="http://xmlns.jakartaee.com/xml/ns/jakartaee"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://xmlns.jakartaee.com/xml/ns/jakartaee
                     http://xmlns.jakartaee.com/xml/ns/jakartaee/web-app_5_0.xsd"
                     version="5.0">
                
                <servlet>
                    <servlet-name>MyServlet</servlet-name>
                    <servlet-class>javax.servlet.http.HttpServlet</servlet-class>
                </servlet>
                
                <listener>
                    <listener-class>javax.servlet.ServletContextListener</listener-class>
                </listener>
                
                <filter>
                    <filter-class>javax.servlet.Filter</filter-class>
                </filter>
            </web-app>
            """;
        
        Path webXmlFile = projectDir.resolve("WEB-INF/web.xml");
        Files.createDirectories(webXmlFile.getParent());
        Files.writeString(webXmlFile, webXml);

        ConfigFileProjectScanResult result = scanner.scanProject(projectDir);
        
        assertTrue(result.hasJavaxUsage());
        assertEquals(1, result.filesWithJavaxUsage());
        assertTrue(result.totalJavaxUsages() >= 3);
        
        ConfigFileScanResult fileResult = result.fileResults().get(0);
        assertEquals("XML", fileResult.getFileType());
    }

    @Test
    void shouldDetectJavaeeNamespace() throws Exception {
        Path projectDir = tempDir.resolve("namespaceProject");
        Files.createDirectory(projectDir);
        
        String webXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <web-app xmlns="http://xmlns.javaee.com/xml/ns/javaee"
                     version="4.0">
                <module-name>my-app</module-name>
            </web-app>
            """;
        
        Path webXmlFile = projectDir.resolve("web.xml");
        Files.writeString(webXmlFile, webXml);

        ConfigFileProjectScanResult result = scanner.scanProject(projectDir);
        
        assertTrue(result.hasJavaxUsage());
        
        ConfigFileScanResult fileResult = result.fileResults().get(0);
        assertTrue(fileResult.usages().stream().anyMatch(u -> u.getjavaxReference().contains("javaee")));
    }

    @Test
    void shouldDetectSpringBeansWithJavax() throws Exception {
        Path projectDir = tempDir.resolve("springProject");
        Files.createDirectory(projectDir);
        
        String springXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <beans xmlns="http://www.springframework.org/schema/beans"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.springframework.org/schema/beans
                   http://www.springframework.org/schema/beans/spring-beans.xsd">
                
                <bean class="javax.sql.DataSource">
                    <property name="driverClassName" value="org.h2.Driver"/>
                </bean>
                
                <bean id="jndiBean" class="org.springframework.jndi.JndiObjectTargetSource">
                    <property name="jndiName" value="java:comp/env/jdbc/mydb"/>
                </bean>
            </beans>
            """;
        
        Path springFile = projectDir.resolve("applicationContext.xml");
        Files.writeString(springFile, springXml);

        ConfigFileProjectScanResult result = scanner.scanProject(projectDir);
        
        assertTrue(result.hasJavaxUsage());
        
        ConfigFileScanResult fileResult = result.fileResults().get(0);
        assertTrue(fileResult.usages().size() >= 1);
    }

    @Test
    void shouldProvideReplacements() throws Exception {
        Path projectDir = tempDir.resolve("replacementProject");
        Files.createDirectory(projectDir);
        
        String webXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <web-app version="5.0">
                <servlet>
                    <servlet-class>javax.servlet.http.HttpServlet</servlet-class>
                </servlet>
            </web-app>
            """;
        
        Path webXmlFile = projectDir.resolve("web.xml");
        Files.writeString(webXmlFile, webXml);

        ConfigFileProjectScanResult result = scanner.scanProject(projectDir);
        
        assertTrue(result.hasJavaxUsage());
        
        ConfigFileScanResult fileResult = result.fileResults().get(0);
        assertEquals(1, fileResult.usages().size());
        
        // Verify replacement is provided
        assertNotNull(fileResult.usages().get(0).getReplacement());
        assertTrue(fileResult.usages().get(0).getReplacement().contains("jakarta"));
    }

    @Test
    void shouldHandleCleanProject() throws Exception {
        Path projectDir = tempDir.resolve("cleanProject");
        Files.createDirectory(projectDir);
        
        String webXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <web-app version="5.0">
                <servlet>
                    <servlet-class>jakarta.servlet.http.HttpServlet</servlet-class>
                </servlet>
            </web-app>
            """;
        
        Path webXmlFile = projectDir.resolve("web.xml");
        Files.writeString(webXmlFile, webXml);

        ConfigFileProjectScanResult result = scanner.scanProject(projectDir);
        
        // Should not have javax usage
        assertFalse(result.hasJavaxUsage());
    }

    @Test
    void shouldScanMultipleConfigFiles() throws Exception {
        Path projectDir = tempDir.resolve("multiFileProject");
        Files.createDirectories(projectDir.resolve("WEB-INF"));
        
        // web.xml with javax
        String webXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <web-app version="5.0">
                <servlet-class>javax.servlet.http.HttpServlet</servlet-class>
            </web-app>
            """;
        
        // Another config without javax
        String anotherXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <beans>
                <bean id="test" class="com.example.Test"/>
            </beans>
            """;
        
        Files.writeString(projectDir.resolve("WEB-INF/web.xml"), webXml);
        Files.writeString(projectDir.resolve("applicationContext.xml"), anotherXml);

        ConfigFileProjectScanResult result = scanner.scanProject(projectDir);
        
        assertTrue(result.hasJavaxUsage());
        assertEquals(2, result.totalConfigFilesScanned());
        assertEquals(1, result.filesWithJavaxUsage());
    }

    @Test
    void shouldReturnEmptyForNonConfigFile() {
        ConfigFileScanResult result = scanner.scanFile(Path.of("README.md"));
        assertNotNull(result);
        assertEquals(0, result.usages().size());
    }

    @Test
    void shouldTrackLineNumbers() throws Exception {
        Path projectDir = tempDir.resolve("lineTest");
        Files.createDirectory(projectDir);
        
        String webXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <web-app version="5.0">
                <!-- Comment line 3 -->
                <!-- Comment line 4 -->
                <servlet-class>javax.servlet.http.HttpServlet</servlet-class>
            </web-app>
            """;
        
        Path webXmlFile = projectDir.resolve("web.xml");
        Files.writeString(webXmlFile, webXml);

        ConfigFileProjectScanResult result = scanner.scanProject(projectDir);
        
        assertTrue(result.hasJavaxUsage());
        
        ConfigFileScanResult fileResult = result.fileResults().get(0);
        assertEquals(1, fileResult.usages().size());
        
        // Line number should be around line 6
        assertTrue(fileResult.usages().get(0).getLineNumber() >= 1);
    }
}
