package adrianmikula.jakartamigration.dependencyanalysis.service.impl;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Dependency;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphBuilder;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds dependency graphs from Maven pom.xml files.
 */
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
        if (buildFilePath == null || !java.nio.file.Files.exists(buildFilePath)) {
            throw new DependencyGraphException("Gradle build file does not exist: " + buildFilePath);
        }
        throw new UnsupportedOperationException("Gradle support not yet implemented");
    }
    
    @Override
    public DependencyGraph buildFromProject(Path projectRoot) {
        Path pomXml = projectRoot.resolve("pom.xml");
        if (Files.exists(pomXml)) {
            return buildFromMaven(pomXml);
        }
        
        Path buildGradle = projectRoot.resolve("build.gradle");
        Path buildGradleKts = projectRoot.resolve("build.gradle.kts");
        
        if (Files.exists(buildGradle) || Files.exists(buildGradleKts)) {
            throw new DependencyGraphException("Gradle support not yet implemented");
        }
        
        throw new DependencyGraphException("No build file found in project root: " + projectRoot);
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

