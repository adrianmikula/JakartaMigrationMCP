package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.IntegrationPointUsage;
import adrianmikula.jakartamigration.advancedscanning.domain.IntegrationPointsProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.service.IntegrationPointsScanner;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class IntegrationPointsScannerImpl implements IntegrationPointsScanner {

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

    @Override
    public IntegrationPointsProjectScanResult scanProject(Path projectPath) {
        log.info("Starting integration points scan for project: {}", projectPath);
        List<IntegrationPointUsage> usages = new ArrayList<>();
        
        try (Stream<Path> paths = Files.walk(projectPath)) {
            List<Path> javaFiles = paths
                .filter(Files::isRegularFile)
                .filter(p -> SCAN_EXTENSIONS.stream().anyMatch(p.toString()::endsWith))
                .collect(Collectors.toList());
            
            for (Path filePath : javaFiles) {
                try {
                    String content = Files.readString(filePath);
                    String[] lines = content.split("\\r?\\n");
                    
                    for (int i = 0; i < lines.length; i++) {
                        Matcher matcher = IMPORT_PATTERN.matcher(lines[i]);
                        if (matcher.find()) {
                            String pkg = "javax." + matcher.group(1);
                            for (Map.Entry<String, String> entry : INTEGRATION_PATTERNS.entrySet()) {
                                if (pkg.startsWith(entry.getKey())) {
                                    usages.add(new IntegrationPointUsage(
                                        filePath.toString(),
                                        i + 1,
                                        entry.getValue(),
                                        extractClassName(lines[i])
                                    ));
                                    break;
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    log.warn("Error reading file: {}", e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("Error scanning integration points: {}", e.getMessage());
        }
        
        return new IntegrationPointsProjectScanResult(projectPath.toString(), usages);
    }

    private String extractClassName(String line) {
        Pattern p = Pattern.compile("(class|interface|enum)\\s+(\\w+)");
        Matcher m = p.matcher(line);
        if (m.find()) return m.group(2);
        return "Unknown";
    }
}
