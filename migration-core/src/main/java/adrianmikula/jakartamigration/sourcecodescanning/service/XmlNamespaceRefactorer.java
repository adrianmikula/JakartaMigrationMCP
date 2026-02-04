package adrianmikula.jakartamigration.sourcecodescanning.service;

import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for refactoring XML files to update javax namespaces to jakarta namespaces.
 * Handles persistence.xml, web.xml, faces-config.xml, and other Jakarta EE configuration files.
 */
@Slf4j
public class XmlNamespaceRefactorer {

    /**
     * Namespace URI mappings from javax to jakarta.
     */
    private static final Map<String, String> NAMESPACE_MAPPINGS = new HashMap<>();
    
    static {
        // Java EE to Jakarta EE namespace mappings
        NAMESPACE_MAPPINGS.put("http://xmlns.jcp.org/xml/ns/javaee", "https://jakarta.ee/xml/ns/jakartaee");
        NAMESPACE_MAPPINGS.put("http://java.sun.com/xml/ns/javaee", "https://jakarta.ee/xml/ns/jakartaee");
        NAMESPACE_MAPPINGS.put("http://xmlns.jcp.org/xml/ns/persistence", "https://jakarta.ee/xml/ns/persistence");
        NAMESPACE_MAPPINGS.put("http://java.sun.com/xml/ns/persistence", "https://jakarta.ee/xml/ns/persistence");
        NAMESPACE_MAPPINGS.put("http://xmlns.jcp.org/xml/ns/jee", "https://jakarta.ee/xml/ns/jakartaee");
        NAMESPACE_MAPPINGS.put("http://java.sun.com/xml/ns/jee", "https://jakarta.ee/xml/ns/jakartaee");
        
        // JMS
        NAMESPACE_MAPPINGS.put("http://java.sun.com/xml/ns/jms", "https://jakarta.ee/xml/ns/jakartaee");
        NAMESPACE_MAPPINGS.put("http://xmlns.jcp.org/xml/ns/jms", "https://jakarta.ee/xml/ns/jakartaee");
        
        // JTA
        NAMESPACE_MAPPINGS.put("http://java.sun.com/xml/ns/jta", "https://jakarta.ee/xml/ns/jakartaee");
    }

    /**
     * Files that need namespace updates.
     */
    private static final List<String> NAMESPACE_FILES = List.of(
            "persistence.xml",
            "web.xml",
            "faces-config.xml",
            "beans.xml",
            "ejb-jar.xml",
            "application.xml",
            "validation.xml"
    );

    /**
     * Scans XML files and returns refactoring changes needed.
     */
    public List<XmlRefactorChange> scanAndPlanRefactoring(Path projectPath) {
        List<XmlRefactorChange> changes = new ArrayList<>();
        
        try {
            List<Path> xmlFiles = discoverXmlFiles(projectPath);
            
            for (Path xmlFile : xmlFiles) {
                try {
                    String content = Files.readString(xmlFile);
                    List<XmlRefactorChange> fileChanges = analyzeXmlFile(xmlFile, content);
                    changes.addAll(fileChanges);
                } catch (Exception e) {
                    log.warn("Error analyzing XML file: {}", xmlFile, e);
                }
            }
        } catch (Exception e) {
            log.error("Error scanning for XML refactoring: {}", projectPath, e);
        }
        
        return changes;
    }

    /**
     * Applies refactoring to XML files.
     */
    public RefactorResult applyRefactoring(Path projectPath, List<XmlRefactorChange> changes, boolean dryRun) {
        int successCount = 0;
        int failCount = 0;
        List<String> errors = new ArrayList<>();
        List<Path> modifiedFiles = new ArrayList<>();
        
        // Group changes by file
        Map<Path, List<XmlRefactorChange>> changesByFile = new HashMap<>();
        for (XmlRefactorChange change : changes) {
            changesByFile
                    .computeIfAbsent(change.filePath(), k -> new ArrayList<>())
                    .add(change);
        }
        
        for (Map.Entry<Path, List<XmlRefactorChange>> entry : changesByFile.entrySet()) {
            Path xmlFile = entry.getKey();
            List<XmlRefactorChange> fileChanges = entry.getValue();
            
            try {
                if (dryRun) {
                    log.info("[DRY RUN] Would refactor {} with {} changes", xmlFile, fileChanges.size());
                    successCount++;
                    modifiedFiles.add(xmlFile);
                } else {
                    RefactorResult result = refactorXmlFile(xmlFile, fileChanges);
                    if (result.success()) {
                        successCount++;
                        modifiedFiles.add(xmlFile);
                    } else {
                        failCount++;
                        errors.addAll(result.errors());
                    }
                }
            } catch (Exception e) {
                failCount++;
                errors.add("Failed to refactor " + xmlFile + ": " + e.getMessage());
            }
        }
        
        return new RefactorResult(
                successCount,
                failCount,
                modifiedFiles,
                errors
        );
    }

    /**
     * Analyzes an XML file and returns required refactoring changes.
     */
    public List<XmlRefactorChange> analyzeXmlFile(Path xmlFile, String content) {
        List<XmlRefactorChange> changes = new ArrayList<>();
        String[] lines = content.split("\n");
        
        // Check for namespace declarations that need updating
        for (Map.Entry<String, String> mapping : NAMESPACE_MAPPINGS.entrySet()) {
            String oldNamespace = mapping.getKey();
            String newNamespace = mapping.getValue();
            
            // Find namespace declarations
            Pattern namespacePattern = Pattern.compile(
                    "xmlns(?::\\w+)?\\s*=\\s*[\"'](https?://[^\"']*" + 
                    Pattern.quote(oldNamespace.replace("https://", "").replace("http://", "")) + 
                    "[^\"']*)[\"']",
                    Pattern.CASE_INSENSITIVE
            );
            
            Matcher matcher = namespacePattern.matcher(content);
            while (matcher.find()) {
                String fullMatch = matcher.group(0);
                int lineNumber = findLineNumber(lines, fullMatch);
                
                changes.add(new XmlRefactorChange(
                        xmlFile,
                        RefactorChangeType.NAMESPACE_UPDATE,
                        lineNumber,
                        oldNamespace,
                        newNamespace,
                        "Update XML namespace declaration from " + oldNamespace + " to " + newNamespace
                ));
            }
        }
        
        // Find class references that need updating (e.g., <class>javax.persistence.Entity</class>)
        Pattern classPattern = Pattern.compile(
                "<class>\\s*(javax\\.[\\w.]+)\\s*</class>",
                Pattern.CASE_INSENSITIVE
        );
        Matcher classMatcher = classPattern.matcher(content);
        while (classMatcher.find()) {
            String oldClass = classMatcher.group(1);
            String newClass = oldClass.replace("javax.", "jakarta.");
            int lineNumber = findLineNumber(lines, classMatcher.group(0));
            
            changes.add(new XmlRefactorChange(
                    xmlFile,
                    RefactorChangeType.CLASS_REFERENCE_UPDATE,
                    lineNumber,
                    oldClass,
                    newClass,
                    "Update class reference from " + oldClass + " to " + newClass
            ));
        }
        
        // Find fully qualified class names in other elements
        Pattern fqcnPattern = Pattern.compile(
                ">(javax\\.[\\w.]+)<",
                Pattern.CASE_INSENSITIVE
        );
        Matcher fqcnMatcher = fqcnPattern.matcher(content);
        while (fqcnMatcher.find()) {
            String oldClass = fqcnMatcher.group(1);
            String newClass = oldClass.replace("javax.", "jakarta.");
            int lineNumber = findLineNumber(lines, fqcnMatcher.group(0));
            
            changes.add(new XmlRefactorChange(
                    xmlFile,
                    RefactorChangeType.FQCN_UPDATE,
                    lineNumber,
                    oldClass,
                    newClass,
                    "Update FQCN from " + oldClass + " to " + newClass
            ));
        }
        
        return changes;
    }

    /**
     * Refactors a single XML file with the given changes.
     */
    private RefactorResult refactorXmlFile(Path xmlFile, List<XmlRefactorChange> changes) {
        try {
            String content = Files.readString(xmlFile);
            String modifiedContent = content;
            
            // Apply changes in reverse order to preserve line numbers
            for (int i = changes.size() - 1; i >= 0; i--) {
                XmlRefactorChange change = changes.get(i);
                modifiedContent = applyChange(modifiedContent, change);
            }
            
            // Write back to file
            Files.writeString(xmlFile, modifiedContent);
            
            return new RefactorResult(true, List.of());
            
        } catch (IOException e) {
            return new RefactorResult(false, List.of("Failed to write file: " + e.getMessage()));
        }
    }

    /**
     * Applies a single change to the content.
     */
    private String applyChange(String content, XmlRefactorChange change) {
        switch (change.type()) {
            case NAMESPACE_UPDATE:
                // Replace namespace in xmlns declarations
                return content.replace(change.oldValue(), change.newValue());
                
            case CLASS_REFERENCE_UPDATE:
            case FQCN_UPDATE:
                // Replace exact class references
                return content.replace(">" + change.oldValue() + "<", ">" + change.newValue() + "<");
                
            default:
                return content.replace(change.oldValue(), change.newValue());
        }
    }

    /**
     * Discovers XML files in the project.
     */
    private List<Path> discoverXmlFiles(Path projectPath) {
        List<Path> xmlFiles = new ArrayList<>();
        
        try (var paths = Files.walk(projectPath)) {
            paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".xml"))
                    .filter(this::shouldScanFile)
                    .forEach(xmlFiles::add);
        } catch (IOException e) {
            log.error("Error discovering XML files: {}", projectPath, e);
        }
        
        return xmlFiles;
    }

    /**
     * Determines if an XML file should be scanned.
     */
    private boolean shouldScanFile(Path file) {
        String path = file.toString().replace('\\', '/');
        return !path.contains("/target/") &&
                !path.contains("/build/") &&
                !path.contains("/.git/") &&
                !path.contains("/.idea/") &&
                !path.contains("/.vscode/");
    }

    /**
     * Finds the line number of a string in the content.
     */
    private int findLineNumber(String[] lines, String searchText) {
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains(searchText)) {
                return i + 1;
            }
        }
        return 1;
    }

    /**
     * Result of applying refactoring changes.
     */
    public record RefactorResult(
            boolean success,
            List<String> errors
    ) {
        public RefactorResult(int success, int fail, List<Path> modifiedFiles, List<String> errors) {
            this(fail == 0, errors);
        }
    }

    /**
     * A single refactoring change.
     */
    public record XmlRefactorChange(
            Path filePath,
            RefactorChangeType type,
            int lineNumber,
            String oldValue,
            String newValue,
            String description
    ) {}

    /**
     * Types of XML refactoring changes.
     */
    public enum RefactorChangeType {
        NAMESPACE_UPDATE,
        CLASS_REFERENCE_UPDATE,
        FQCN_UPDATE,
        SCHEMA_LOCATION_UPDATE,
        DTD_UPDATE
    }
}
