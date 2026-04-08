package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.IntegrationPointUsage;
import adrianmikula.jakartamigration.advancedscanning.domain.IntegrationPointsProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.service.IntegrationPointsScanner;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import adrianmikula.jakartamigration.util.ProjectFileSystemScanner;

@Slf4j
public class IntegrationPointsScannerImpl implements IntegrationPointsScanner {

    private final ProjectFileSystemScanner fileScanner = new ProjectFileSystemScanner();

    private static final Map<String, String> INTEGRATION_PATTERNS = new HashMap<>();
    static {
        INTEGRATION_PATTERNS.put("javax.rmi", "RMI");
        INTEGRATION_PATTERNS.put("javax.jms", "JMS");
        INTEGRATION_PATTERNS.put("javax.jws", "JWS");
        INTEGRATION_PATTERNS.put("javax.xml.ws", "JAX-WS");
        INTEGRATION_PATTERNS.put("javax.xml.soap", "SOAP");
        INTEGRATION_PATTERNS.put("javax.corba", "CORBA");
        INTEGRATION_PATTERNS.put("javax.naming", "JNDI");
    }

    private static final Pattern IMPORT_PATTERN = Pattern.compile("import\\s+javax\\.([^;]+);");
    private static final Set<String> SCAN_EXTENSIONS = Set.of(".java");

    // Maximum file size to scan (5MB) - larger files are skipped
    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;

    @Override
    public IntegrationPointsProjectScanResult scanProject(Path projectPath) {
        log.info("Starting integration points scan for project: {}", projectPath);
        List<IntegrationPointUsage> usages = new ArrayList<>();

        try {
            List<Path> javaFiles = fileScanner.findFiles(projectPath, List.of(".java"));

            for (Path filePath : javaFiles) {
                try {
                    // Skip large files to prevent memory issues
                    long fileSize = Files.size(filePath);
                    if (fileSize > MAX_FILE_SIZE_BYTES) {
                        log.warn("Skipping large file ({} bytes): {}", fileSize, filePath);
                        continue;
                    }

                    // Use streaming with try-with-resources for memory efficiency
                    try (Stream<String> lines = Files.lines(filePath)) {
                        final AtomicInteger lineNumber = new AtomicInteger(0);
                        lines.forEach(line -> {
                            int currentLineNumber = lineNumber.incrementAndGet();
                            Matcher matcher = IMPORT_PATTERN.matcher(line);
                            if (matcher.find()) {
                                String pkg = "javax." + matcher.group(1);
                                for (Map.Entry<String, String> entry : INTEGRATION_PATTERNS.entrySet()) {
                                    if (pkg.startsWith(entry.getKey())) {
                                        usages.add(new IntegrationPointUsage(
                                                filePath.toString(),
                                                currentLineNumber,
                                                entry.getValue(),
                                                extractClassName(line)));
                                        break;
                                    }
                                }
                            }
                        });
                    }
                } catch (IOException e) {
                    log.warn("Error reading file: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error scanning integration points: {}", e.getMessage());
        }

        return new IntegrationPointsProjectScanResult(projectPath.toString(), usages);
    }

    private String extractClassName(String line) {
        Pattern p = Pattern.compile("(class|interface|enum)\\s+(\\w+)");
        Matcher m = p.matcher(line);
        if (m.find())
            return m.group(2);
        return "Unknown";
    }
}
