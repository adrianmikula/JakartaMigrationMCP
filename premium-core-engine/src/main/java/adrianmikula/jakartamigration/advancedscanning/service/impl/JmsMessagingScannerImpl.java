package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.JmsMessagingProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.JmsMessagingScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.JmsMessagingUsage;
import adrianmikula.jakartamigration.advancedscanning.service.JmsMessagingScanner;
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
public class JmsMessagingScannerImpl implements JmsMessagingScanner {

    // Known javax.jms packages and their Jakarta equivalents
    private static final Map<String, JmsApiInfo> JMS_APIS = new HashMap<>();

    static {
        // Core JMS API
        JMS_APIS.put("javax.jms.Connection", 
            new JmsApiInfo("jakarta.jms.Connection", "Connection"));
        JMS_APIS.put("javax.jms.ConnectionFactory", 
            new JmsApiInfo("jakarta.jms.ConnectionFactory", "Connection"));
        JMS_APIS.put("javax.jms.ConnectionConsumer", 
            new JmsApiInfo("Use Jakarta EE 9+ server", "Connection"));
        JMS_APIS.put("javax.jms.ConnectionMetaData", 
            new JmsApiInfo("jakarta.jms.ConnectionMetaData", "Connection"));
        
        // Session
        JMS_APIS.put("javax.jms.Session", 
            new JmsApiInfo("jakarta.jms.Session", "Session"));
        JMS_APIS.put("javax.jms.QueueSession", 
            new JmsApiInfo("jakarta.jms.QueueSession", "Session"));
        JMS_APIS.put("javax.jms.TopicSession", 
            new JmsApiInfo("jakarta.jms.TopicSession", "Session"));
        
        // Message Producers
        JMS_APIS.put("javax.jms.MessageProducer", 
            new JmsApiInfo("jakarta.jms.MessageProducer", "Producer"));
        JMS_APIS.put("javax.jms.QueueSender", 
            new JmsApiInfo("jakarta.jms.QueueSender", "Producer"));
        JMS_APIS.put("javax.jms.TopicPublisher", 
            new JmsApiInfo("jakarta.jms.TopicPublisher", "Producer"));
        
        // Message Consumers
        JMS_APIS.put("javax.jms.MessageConsumer", 
            new JmsApiInfo("jakarta.jms.MessageConsumer", "Consumer"));
        JMS_APIS.put("javax.jms.QueueReceiver", 
            new JmsApiInfo("jakarta.jms.QueueReceiver", "Consumer"));
        JMS_APIS.put("javax.jms.TopicSubscriber", 
            new JmsApiInfo("jakarta.jms.TopicSubscriber", "Consumer"));
        JMS_APIS.put("javax.jms.MessageListener", 
            new JmsApiInfo("jakarta.jms.MessageListener", "Consumer"));
        
        // Destinations
        JMS_APIS.put("javax.jms.Destination", 
            new JmsApiInfo("jakarta.jms.Destination", "Destination"));
        JMS_APIS.put("javax.jms.Queue", 
            new JmsApiInfo("jakarta.jms.Queue", "Destination"));
        JMS_APIS.put("javax.jms.Topic", 
            new JmsApiInfo("jakarta.jms.Topic", "Destination"));
        JMS_APIS.put("javax.jms.TemporaryQueue", 
            new JmsApiInfo("jakarta.jms.TemporaryQueue", "Destination"));
        JMS_APIS.put("javax.jms.TemporaryTopic", 
            new JmsApiInfo("jakarta.jms.TemporaryTopic", "Destination"));
        
        // Messages
        JMS_APIS.put("javax.jms.Message", 
            new JmsApiInfo("jakarta.jms.Message", "Message"));
        JMS_APIS.put("javax.jms.TextMessage", 
            new JmsApiInfo("jakarta.jms.TextMessage", "Message"));
        JMS_APIS.put("javax.jms.BytesMessage", 
            new JmsApiInfo("jakarta.jms.BytesMessage", "Message"));
        JMS_APIS.put("javax.jms.MapMessage", 
            new JmsApiInfo("jakarta.jms.MapMessage", "Message"));
        JMS_APIS.put("javax.jms.ObjectMessage", 
            new JmsApiInfo("jakarta.jms.ObjectMessage", "Message"));
        JMS_APIS.put("javax.jms.StreamMessage", 
            new JmsApiInfo("jakarta.jms.StreamMessage", "Message"));
        
        // Other JMS classes
        JMS_APIS.put("javax.jms.JMSException", 
            new JmsApiInfo("jakarta.jms.JMSException", "Exception"));
        JMS_APIS.put("javax.jms.JMSSecurityException", 
            new JmsApiInfo("jakarta.jms.JMSSecurityException", "Exception"));
        JMS_APIS.put("javax.jms.MessageNotReadableException", 
            new JmsApiInfo("jakarta.jms.MessageNotReadableException", "Exception"));
        JMS_APIS.put("javax.jms.MessageNotWriteableException", 
            new JmsApiInfo("jakarta.jms.MessageNotWriteableException", "Exception"));
        
        JMS_APIS.put("javax.jms.QueueConnectionFactory", 
            new JmsApiInfo("jakarta.jms.QueueConnectionFactory", "Queue"));
        JMS_APIS.put("javax.jms.TopicConnectionFactory", 
            new JmsApiInfo("jakarta.jms.TopicConnectionFactory", "Topic"));
        JMS_APIS.put("javax.jms.QueueConnection", 
            new JmsApiInfo("jakarta.jms.QueueConnection", "Queue"));
        JMS_APIS.put("javax.jms.TopicConnection", 
            new JmsApiInfo("jakarta.jms.TopicConnection", "Topic"));
        
        JMS_APIS.put("javax.jms.XAConnection", 
            new JmsApiInfo("jakarta.jms.XAConnection", "XA"));
        JMS_APIS.put("javax.jms.XAConnectionFactory", 
            new JmsApiInfo("jakarta.jms.XAConnectionFactory", "XA"));
        JMS_APIS.put("javax.jms.XASession", 
            new JmsApiInfo("jakarta.jms.XASession", "XA"));
        
        // JMS 2.0 API
        JMS_APIS.put("javax.jms.JMSContext", 
            new JmsApiInfo("jakarta.jms.JMSContext", "JMS2.0"));
        JMS_APIS.put("javax.jms.JMSProducer", 
            new JmsApiInfo("jakarta.jms.JMSProducer", "JMS2.0"));
        JMS_APIS.put("javax.jms.JMSConsumer", 
            new JmsApiInfo("jakarta.jms.JMSConsumer", "JMS2.0"));
    }

    private record JmsApiInfo(String jakartaEquivalent, String category) {}

    private final ThreadLocal<JavaParser> javaParserThreadLocal = ThreadLocal.withInitial(() -> JavaParser.fromJavaVersion().build());

    // Pattern for javax.jms imports
    private static final Pattern JMS_IMPORT_PATTERN = Pattern.compile(
        "import\\s+javax\\.jms[\\.\\w]*;",
        Pattern.MULTILINE
    );

    @Override
    public JmsMessagingProjectScanResult scanProject(Path projectPath) {
        if (projectPath == null || !Files.exists(projectPath) || !Files.isDirectory(projectPath)) {
            return JmsMessagingProjectScanResult.empty();
        }

        try {
            List<Path> javaFiles = discoverJavaFiles(projectPath);
            if (javaFiles.isEmpty()) return JmsMessagingProjectScanResult.empty();

            AtomicInteger totalScanned = new AtomicInteger(0);
            List<JmsMessagingScanResult> results = javaFiles.parallelStream()
                .map(file -> {
                    totalScanned.incrementAndGet();
                    JmsMessagingScanResult result = scanFile(file);
                    return result.hasJavaxUsage() ? result : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            int totalUsages = results.stream().mapToInt(r -> r.usages().size()).sum();

            return new JmsMessagingProjectScanResult(results, totalScanned.get(), results.size(), totalUsages);
        } catch (Exception e) {
            log.error("Error scanning project for JMS APIs", e);
            return JmsMessagingProjectScanResult.empty();
        }
    }

    @Override
    public JmsMessagingScanResult scanFile(Path filePath) {
        if (filePath == null || !Files.exists(filePath)) {
            return JmsMessagingScanResult.empty(filePath);
        }

        try {
            String content = Files.readString(filePath);
            int lineCount = content.split("\n").length;

            // Quick check using regex first for performance
            Matcher matcher = JMS_IMPORT_PATTERN.matcher(content);
            if (!matcher.find()) {
                return JmsMessagingScanResult.empty(filePath);
            }

            // Reset matcher and find all
            matcher.reset();
            List<String> foundImports = new ArrayList<>();
            while (matcher.find()) {
                String importMatch = matcher.group();
                String className = importMatch.replace("import ", "").replace(";", "").trim();
                foundImports.add(className);
            }

            if (foundImports.isEmpty()) {
                return JmsMessagingScanResult.empty(filePath);
            }

            // Use OpenRewrite for detailed analysis
            JavaParser parser = javaParserThreadLocal.get();
            parser.reset();

            List<SourceFile> sourceFiles = parser.parse(content).collect(Collectors.toList());
            if (sourceFiles.isEmpty()) return JmsMessagingScanResult.empty(filePath);

            List<JmsMessagingUsage> usages = new ArrayList<>();
            for (SourceFile sourceFile : sourceFiles) {
                if (sourceFile instanceof CompilationUnit) {
                    CompilationUnit cu = (CompilationUnit) sourceFile;
                    usages.addAll(extractJmsApis(cu, content, foundImports));
                }
            }

            return new JmsMessagingScanResult(filePath, usages, lineCount);
        } catch (Exception e) {
            return JmsMessagingScanResult.empty(filePath);
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

    private List<JmsMessagingUsage> extractJmsApis(CompilationUnit cu, String content, List<String> foundImports) {
        List<JmsMessagingUsage> usages = new ArrayList<>();
        String[] lines = content.split("\n");

        for (String importName : foundImports) {
            JmsApiInfo info = JMS_APIS.get(importName);
            if (info != null) {
                int lineNumber = findLineNumber(lines, importName);
                usages.add(new JmsMessagingUsage(importName, null, info.jakartaEquivalent(), lineNumber, info.category()));
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
