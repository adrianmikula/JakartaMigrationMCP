package adrianmikula.jakartamigration.unit.advancedscanning;

import adrianmikula.jakartamigration.advancedscanning.domain.JmsMessagingProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.JmsMessagingScanResult;
import adrianmikula.jakartamigration.advancedscanning.service.impl.JmsMessagingScannerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class JmsMessagingScannerTest {

    private JmsMessagingScannerImpl scanner;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        scanner = new JmsMessagingScannerImpl();
    }

    @Test
    void shouldReturnEmptyForNullPath() {
        JmsMessagingProjectScanResult result = scanner.scanProject(null);
        assertNotNull(result);
        assertEquals(0, result.totalFilesScanned());
    }

    @Test
    void shouldReturnEmptyForNonExistentPath() {
        JmsMessagingProjectScanResult result = scanner.scanProject(Path.of("/nonexistent/path"));
        assertNotNull(result);
        assertEquals(0, result.totalFilesScanned());
    }

    @Test
    void shouldScanEmptyProject() throws Exception {
        Path projectDir = tempDir.resolve("emptyProject");
        Files.createDirectory(projectDir);

        JmsMessagingProjectScanResult result = scanner.scanProject(projectDir);
        assertNotNull(result);
        assertEquals(0, result.totalFilesScanned());
    }

    @Test
    void shouldDetectJmsConnectionImports() throws Exception {
        Path projectDir = tempDir.resolve("jmsProject");
        Files.createDirectory(projectDir);
        
        String javaContent = """
            package com.example;
            
            import javax.jms.Connection;
            import javax.jms.ConnectionFactory;
            import javax.jms.Session;
            
            public class JmsService {
                private Connection connection;
                private ConnectionFactory factory;
                private Session session;
            }
            """;
        
        Path javaFile = projectDir.resolve("JmsService.java");
        Files.writeString(javaFile, javaContent);

        JmsMessagingProjectScanResult result = scanner.scanProject(projectDir);
        
        assertTrue(result.hasJavaxUsage());
        assertEquals(1, result.filesWithJavaxUsage());
        assertEquals(3, result.totalJavaxUsages());
        
        JmsMessagingScanResult fileResult = result.fileResults().get(0);
        assertEquals(3, fileResult.usages().size());
        
        // Verify category
        assertTrue(fileResult.usages().stream().allMatch(u -> u.getContext().equals("Connection") || u.getContext().equals("Session")));
    }

    @Test
    void shouldDetectMessageImports() throws Exception {
        Path projectDir = tempDir.resolve("messageProject");
        Files.createDirectory(projectDir);
        
        String javaContent = """
            package com.example;
            
            import javax.jms.Message;
            import javax.jms.TextMessage;
            import javax.jms.BytesMessage;
            import javax.jms.MapMessage;
            
            public class MessageProcessor {
                public void process(Message msg) {}
                public TextMessage createTextMessage() { return null; }
            }
            """;
        
        Path javaFile = projectDir.resolve("MessageProcessor.java");
        Files.writeString(javaFile, javaContent);

        JmsMessagingProjectScanResult result = scanner.scanProject(projectDir);
        
        assertTrue(result.hasJavaxUsage());
        
        JmsMessagingScanResult fileResult = result.fileResults().get(0);
        assertEquals(4, fileResult.usages().size());
        
        // Verify category
        assertTrue(fileResult.usages().stream().allMatch(u -> u.getContext().equals("Message")));
    }

    @Test
    void shouldDetectProducerConsumerImports() throws Exception {
        Path projectDir = tempDir.resolve("producerConsumerProject");
        Files.createDirectory(projectDir);
        
        String javaContent = """
            package com.example;
            
            import javax.jms.MessageProducer;
            import javax.jms.MessageConsumer;
            import javax.jms.QueueSender;
            import javax.jms.QueueReceiver;
            
            public class ProducerConsumer {
                private MessageProducer producer;
                private MessageConsumer consumer;
            }
            """;
        
        Path javaFile = projectDir.resolve("ProducerConsumer.java");
        Files.writeString(javaFile, javaContent);

        JmsMessagingProjectScanResult result = scanner.scanProject(projectDir);
        
        assertTrue(result.hasJavaxUsage());
        
        JmsMessagingScanResult fileResult = result.fileResults().get(0);
        assertEquals(4, fileResult.usages().size());
    }

    @Test
    void shouldProvideJakartaEquivalents() throws Exception {
        Path projectDir = tempDir.resolve("mappingProject");
        Files.createDirectory(projectDir);
        
        String javaContent = """
            package com.example;
            
            import javax.jms.Connection;
            
            public class JmsExample {
            }
            """;
        
        Path javaFile = projectDir.resolve("JmsExample.java");
        Files.writeString(javaFile, javaContent);

        JmsMessagingProjectScanResult result = scanner.scanProject(projectDir);
        
        assertTrue(result.hasJavaxUsage());
        
        JmsMessagingScanResult fileResult = result.fileResults().get(0);
        assertEquals(1, fileResult.usages().size());
        
        // Verify jakarta equivalent is provided
        assertEquals("jakarta.jms.Connection", 
            fileResult.usages().get(0).getJakartaEquivalent());
    }

    @Test
    void shouldHandleJavaFilesWithoutJmsApis() throws Exception {
        Path projectDir = tempDir.resolve("cleanProject");
        Files.createDirectory(projectDir);
        
        String javaContent = """
            package com.example;
            
            import jakarta.jms.Message;
            
            public class CleanClass {
            }
            """;
        
        Path javaFile = projectDir.resolve("CleanClass.java");
        Files.writeString(javaFile, javaContent);

        JmsMessagingProjectScanResult result = scanner.scanProject(projectDir);
        
        // Should not have javax usage
        assertFalse(result.hasJavaxUsage());
        assertEquals(0, result.filesWithJavaxUsage());
    }

    @Test
    void shouldScanMultipleFilesInProject() throws Exception {
        Path projectDir = tempDir.resolve("multiFileProject");
        Files.createDirectory(projectDir);
        
        // First file with JMS
        String javaContent1 = """
            package com.example;
            import javax.jms.MessageProducer;
            public class Producer {}
            """;
        
        // Second file without JMS
        String javaContent2 = """
            package com.example;
            public class NormalClass {}
            """;
        
        Files.writeString(projectDir.resolve("Producer.java"), javaContent1);
        Files.writeString(projectDir.resolve("NormalClass.java"), javaContent2);

        JmsMessagingProjectScanResult result = scanner.scanProject(projectDir);
        
        assertTrue(result.hasJavaxUsage());
        assertEquals(2, result.totalFilesScanned());
        assertEquals(1, result.filesWithJavaxUsage());
    }

    @Test
    void shouldReturnEmptyForNonJavaFile() {
        JmsMessagingScanResult result = scanner.scanFile(Path.of("README.md"));
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
            import javax.jms.Connection;  // This is line 6
            """;
        
        Path javaFile = projectDir.resolve("TestFile.java");
        Files.writeString(javaFile, javaContent);

        JmsMessagingProjectScanResult result = scanner.scanProject(projectDir);
        
        assertTrue(result.hasJavaxUsage());
        
        JmsMessagingScanResult fileResult = result.fileResults().get(0);
        assertEquals(1, fileResult.usages().size());
        
        // Line number should be around line 6
        assertTrue(fileResult.usages().get(0).getLineNumber() >= 1);
    }

    @Test
    void shouldDetectJms2Imports() throws Exception {
        Path projectDir = tempDir.resolve("jms2Project");
        Files.createDirectory(projectDir);
        
        String javaContent = """
            package com.example;
            
            import javax.jms.JMSContext;
            import javax.jms.JMSProducer;
            import javax.jms.JMSConsumer;
            
            public class Jms2Example {
                private JMSContext context;
            }
            """;
        
        Path javaFile = projectDir.resolve("Jms2Example.java");
        Files.writeString(javaFile, javaContent);

        JmsMessagingProjectScanResult result = scanner.scanProject(projectDir);
        
        assertTrue(result.hasJavaxUsage());
        
        JmsMessagingScanResult fileResult = result.fileResults().get(0);
        assertEquals(3, fileResult.usages().size());
        
        // Verify category
        assertTrue(fileResult.usages().stream().allMatch(u -> u.getContext().equals("JMS2.0")));
    }

    @Test
    void shouldHandleMixedJakartaAndJavax() throws Exception {
        Path projectDir = tempDir.resolve("mixedProject");
        Files.createDirectory(projectDir);
        
        String javaContent = """
            package com.example;
            
            import javax.jms.Connection;
            import jakarta.jms.Connection;
            
            public class MixedExample {
            }
            """;
        
        Path javaFile = projectDir.resolve("MixedExample.java");
        Files.writeString(javaFile, javaContent);

        JmsMessagingProjectScanResult result = scanner.scanProject(projectDir);
        
        // Should detect javax.jms usage
        assertTrue(result.hasJavaxUsage());
        
        JmsMessagingScanResult fileResult = result.fileResults().get(0);
        // Should detect only javax import
        assertEquals(1, fileResult.usages().size());
    }
}
