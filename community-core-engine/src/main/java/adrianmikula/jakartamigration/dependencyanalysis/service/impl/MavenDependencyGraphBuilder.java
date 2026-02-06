package adrianmikula.jakartamigration.dependencyanalysis.service.impl;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Dependency;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphBuilder;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphException;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds dependency graphs from Maven pom.xml files.
 */
@Slf4j
public class MavenDependencyGraphBuilder implements DependencyGraphBuilder {
    
    @Override
    public DependencyGraph buildFromMaven(Path pomXmlPath) {
        if (!Files.exists(pomXmlPath)) {
            throw new DependencyGraphException("pom.xml not found at: " + pomXmlPath);
        }
        
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(pomXmlPath.toFile());
            
            DependencyGraph graph = new DependencyGraph();
            
            // Parse project artifact
            Element project = document.getDocumentElement();
            String groupId = getTextContent(project, "groupId");
            String artifactId = getTextContent(project, "artifactId");
            String version = getTextContent(project, "version");
            
            if (groupId == null || artifactId == null || version == null) {
                // Try parent groupId/version
                Element parent = (Element) project.getElementsByTagName("parent").item(0);
                if (parent != null) {
                    if (groupId == null) groupId = getTextContent(parent, "groupId");
                    if (version == null) version = getTextContent(parent, "version");
                }
            }
            
            Artifact projectArtifact = new Artifact(
                groupId != null ? groupId : "unknown",
                artifactId != null ? artifactId : "unknown",
                version != null ? version : "unknown",
                "compile",
                false
            );
            graph.addNode(projectArtifact);
            
            // Parse dependencies
            NodeList dependencies = document.getElementsByTagName("dependency");
            for (int i = 0; i < dependencies.getLength(); i++) {
                Element dependencyElement = (Element) dependencies.item(i);
                
                String depGroupId = getTextContent(dependencyElement, "groupId");
                String depArtifactId = getTextContent(dependencyElement, "artifactId");
                String depVersion = getTextContent(dependencyElement, "version");
                String scope = getTextContent(dependencyElement, "scope");
                
                if (depGroupId == null || depArtifactId == null) {
                    continue; // Skip invalid dependencies
                }
                
                if (depVersion == null) {
                    // Try to resolve from dependencyManagement or properties
                    depVersion = resolveVersion(document, depGroupId, depArtifactId);
                }
                
                if (depVersion == null) {
                    depVersion = "unknown";
                }
                
                if (scope == null) {
                    scope = "compile";
                }
                
                Artifact dependencyArtifact = new Artifact(
                    depGroupId,
                    depArtifactId,
                    depVersion,
                    scope,
                    true
                );
                
                Dependency dependency = new Dependency(
                    projectArtifact,
                    dependencyArtifact,
                    scope,
                    "optional".equals(getTextContent(dependencyElement, "optional"))
                );
                
                graph.addEdge(dependency);
            }
            
            return graph;
            
        } catch (Exception e) {
            throw new DependencyGraphException("Failed to parse pom.xml: " + e.getMessage(), e);
        }
    }
    
    @Override
    public DependencyGraph buildFromGradle(Path buildFilePath) {
        if (buildFilePath == null || !Files.exists(buildFilePath)) {
            throw new DependencyGraphException("Gradle build file does not exist: " + buildFilePath);
        }
        
        try {
            String content = Files.readString(buildFilePath);
            DependencyGraph graph = new DependencyGraph();
            
            // Parse project artifact (simplified - would need proper Gradle parsing for production)
            String artifactId = extractProjectArtifactId(content);
            Artifact projectArtifact = new Artifact(
                "unknown",
                artifactId != null ? artifactId : "unknown",
                "unknown",
                "compile",
                false
            );
            graph.addNode(projectArtifact);
            
            // Parse dependencies
            List<Artifact> dependencies = parseGradleDependencies(content);
            for (Artifact dependency : dependencies) {
                graph.addNode(dependency);
                graph.addEdge(new Dependency(
                    projectArtifact,
                    dependency,
                    dependency.scope(),
                    false
                ));
            }
            
            return graph;
            
        } catch (Exception e) {
            throw new DependencyGraphException("Failed to parse Gradle build file: " + e.getMessage(), e);
        }
    }
    
    @Override
    public DependencyGraph buildFromProject(Path projectRoot) {
        log.info("Searching for build files in project root: {}", projectRoot);
        
        // First, try to find build files in the root directory
        Path pomXml = projectRoot.resolve("pom.xml");
        if (Files.exists(pomXml)) {
            log.info("Found pom.xml in root: {}", pomXml);
            return buildFromMaven(pomXml);
        }
        
        Path buildGradle = projectRoot.resolve("build.gradle");
        Path buildGradleKts = projectRoot.resolve("build.gradle.kts");
        
        if (Files.exists(buildGradle)) {
            log.info("Found build.gradle in root: {}", buildGradle);
            return buildFromGradle(buildGradle);
        }
        
        if (Files.exists(buildGradleKts)) {
            log.info("Found build.gradle.kts in root: {}", buildGradleKts);
            return buildFromGradle(buildGradleKts);
        }
        
        log.info("No build files found in root, searching subdirectories...");
        
        // If no build files found in root, search subdirectories
        Path foundBuildFile = searchForBuildFile(projectRoot);
        
        if (foundBuildFile != null) {
            log.info("Found build file in subdirectory: {}", foundBuildFile);
            String fileName = foundBuildFile.getFileName().toString();
            if (fileName.equals("pom.xml")) {
                return buildFromMaven(foundBuildFile);
            } else {
                return buildFromGradle(foundBuildFile);
            }
        }
        
        log.error("No build file found in project: {}", projectRoot);
        throw new DependencyGraphException("No build file found in project: " + projectRoot);
    }
    
    /**
     * Recursively searches for build files (pom.xml, build.gradle, build.gradle.kts)
     * in the project directory and its subdirectories.
     *
     * @param directory The directory to search in
     * @return The path to the first build file found, or null if none found
     */
    private Path searchForBuildFile(Path directory) {
        log.debug("Walking directory tree to find build files starting from: {}", directory);
        try (var stream = Files.walk(directory)) {
            Path foundFile = stream
                .filter(Files::isRegularFile)
                .filter(path -> {
                    String fileName = path.getFileName().toString();
                    return fileName.equals("pom.xml") || 
                           fileName.equals("build.gradle") || 
                           fileName.equals("build.gradle.kts");
                })
                .findFirst()
                .orElse(null);
            
            if (foundFile != null) {
                log.debug("Found build file: {}", foundFile);
            } else {
                log.debug("No build files found in directory tree: {}", directory);
            }
            return foundFile;
        } catch (IOException e) {
            log.error("Failed to search for build files in directory: {}", directory, e);
            throw new DependencyGraphException("Failed to search for build files in: " + directory, e);
        }
    }
    
    private List<Artifact> parseGradleDependencies(String content) {
        List<Artifact> artifacts = new ArrayList<>();
        
        // Match: implementation 'groupId:artifactId:version' (Groovy DSL with single quotes)
        // Match: implementation("groupId:artifactId:version") (Kotlin DSL with double quotes and parentheses)
        // Also match: api, compile, runtime, testImplementation, etc.
        // Capture the dependency type as group 1
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "(implementation|api|compile|runtime|testImplementation|testRuntime|compileOnly|runtimeOnly)\\s*[(]?\\s*['\"]([^:]+):([^:]+):([^'\"]+)['\"]\\s*[)]?"
        );
        
        java.util.regex.Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            String dependencyType = matcher.group(1);
            String groupId = matcher.group(2);
            String artifactId = matcher.group(3);
            String version = matcher.group(4);
            
            // Determine scope from the dependency type
            String scope = "compile";
            if (dependencyType.equals("testImplementation") || dependencyType.equals("testRuntime")) {
                scope = "test";
            } else if (dependencyType.equals("runtimeOnly") || dependencyType.equals("runtime")) {
                scope = "runtime";
            } else if (dependencyType.equals("compileOnly")) {
                scope = "provided";
            } else if (dependencyType.equals("api") || dependencyType.equals("implementation") || dependencyType.equals("compile")) {
                scope = "compile";
            }
            
            artifacts.add(new Artifact(
                groupId,
                artifactId,
                version,
                scope,
                true
            ));
        }
        
        return artifacts;
    }
    
    private String extractProjectArtifactId(String content) {
        // Try to find artifactId in build.gradle
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "(?:baseName|archivesBaseName|rootProject\\.name)\\s*=\\s*['\"]([^'\"]+)['\"]"
        );
        java.util.regex.Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // Try application plugin main class
        pattern = java.util.regex.Pattern.compile(
            "application\\s*\\{[^}]*mainClass\\s*=\\s*['\"]([^'\"]+)['\"]"
        );
        matcher = pattern.matcher(content);
        if (matcher.find()) {
            String mainClass = matcher.group(1);
            // Extract simple name from fully qualified class name
            if (mainClass.contains(".")) {
                return mainClass.substring(mainClass.lastIndexOf('.') + 1);
            }
            return mainClass;
        }
        
        return null;
    }
    
    private String getTextContent(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            Node node = nodes.item(0);
            return node.getTextContent().trim();
        }
        return null;
    }
    
    private String resolveVersion(Document document, String groupId, String artifactId) {
        // Try dependencyManagement
        NodeList depMgmt = document.getElementsByTagName("dependencyManagement");
        if (depMgmt.getLength() > 0) {
            Element depMgmtElement = (Element) depMgmt.item(0);
            NodeList deps = depMgmtElement.getElementsByTagName("dependency");
            for (int i = 0; i < deps.getLength(); i++) {
                Element dep = (Element) deps.item(i);
                String gId = getTextContent(dep, "groupId");
                String aId = getTextContent(dep, "artifactId");
                if (groupId.equals(gId) && artifactId.equals(aId)) {
                    String version = getTextContent(dep, "version");
                    if (version != null) {
                        return resolveProperty(document, version);
                    }
                }
            }
        }
        
        // Try properties
        NodeList properties = document.getElementsByTagName("properties");
        if (properties.getLength() > 0) {
            // Properties resolution would go here
        }
        
        return null;
    }
    
    private String resolveProperty(Document document, String propertyValue) {
        if (propertyValue != null && propertyValue.startsWith("${") && propertyValue.endsWith("}")) {
            String propertyName = propertyValue.substring(2, propertyValue.length() - 1);
            NodeList properties = document.getElementsByTagName("properties");
            if (properties.getLength() > 0) {
                Element propsElement = (Element) properties.item(0);
                return getTextContent(propsElement, propertyName);
            }
        }
        return propertyValue;
    }
}

