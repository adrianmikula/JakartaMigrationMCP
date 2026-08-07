package adrianmikula.jakartamigration.dependencyanalysis.service.impl;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Dependency;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphBuilder;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphException;
import adrianmikula.jakartamigration.dependencyanalysis.util.MavenPomParser;
import adrianmikula.jakartamigration.dependencyanalysis.util.BuildFileDiscovery;
import adrianmikula.jakartamigration.dependencyanalysis.util.ScopeConstants;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds dependency graphs from Maven pom.xml files.
 */
@Slf4j
public class MavenDependencyGraphBuilder implements DependencyGraphBuilder {
    
    private final DocumentBuilderFactory documentBuilderFactory;
    private final DocumentBuilder documentBuilder;
    
    public MavenDependencyGraphBuilder() {
        try {
            this.documentBuilderFactory = DocumentBuilderFactory.newInstance();
            this.documentBuilderFactory.setNamespaceAware(false);
            this.documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (Exception e) {
            throw new DependencyGraphException("Failed to initialize DOM parser", e);
        }
    }
    
    @Override
    public DependencyGraph buildFromMaven(Path pomXmlPath) {
        if (!Files.exists(pomXmlPath)) {
            throw new DependencyGraphException("pom.xml not found at: " + pomXmlPath);
        }
        
        try {
            Document document = documentBuilder.parse(pomXmlPath.toFile());
            
            // Build lookup maps for efficient version resolution
            Map<String, String> depMgmtVersions = buildDependencyManagementVersionMap(document);
            Map<String, String> propertiesMap = buildPropertiesMap(document);
            
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
                    // Try to resolve from dependencyManagement or properties using lookup maps
                    depVersion = resolveVersionFromMaps(depGroupId, depArtifactId, depMgmtVersions, propertiesMap, document);
                } else if (depVersion.startsWith("${") && depVersion.endsWith("}")) {
                    // Try to resolve property references
                    String resolvedVersion = MavenPomParser.resolveProperty(depVersion, propertiesMap);
                    if (resolvedVersion != null && !resolvedVersion.startsWith("${")) {
                        depVersion = resolvedVersion;
                    } else {
                        // Property not found, set to "unknown"
                        depVersion = "unknown";
                    }
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
                     false
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
        
        // Check for Maven project first
        Path pomXml = projectRoot.resolve("pom.xml");
        if (Files.exists(pomXml)) {
            log.info("Found pom.xml in root: {}", pomXml);
            return buildFromMaven(pomXml);
        }
        
        // Check for Gradle project — prefer multi-module parser when settings file exists
        Path settingsGradle = projectRoot.resolve("settings.gradle");
        Path settingsGradleKts = projectRoot.resolve("settings.gradle.kts");
        boolean hasSettings = Files.exists(settingsGradle) || Files.exists(settingsGradleKts);
        
        Path buildGradle = projectRoot.resolve("build.gradle");
        Path buildGradleKts = projectRoot.resolve("build.gradle.kts");
        boolean hasRootBuild = Files.exists(buildGradle) || Files.exists(buildGradleKts);
        
        if (hasRootBuild) {
            if (hasSettings) {
                // Multi-module project — parse all modules via settings file
                log.info("Found Gradle settings file, parsing as multi-module project");
                GradleMultiModuleParser multiModuleParser = new GradleMultiModuleParser();
                DependencyGraph graph = multiModuleParser.parseProject(projectRoot);
                if (graph.nodeCount() > 1) {
                    return graph;
                }
                log.info("Multi-module parser returned few nodes, falling back to single-file parse");
            }
            
            // Single-module or fallback: parse root build file only
            Path buildFile = Files.exists(buildGradleKts) ? buildGradleKts : buildGradle;
            log.info("Found build file in root: {}", buildFile);
            return buildFromGradle(buildFile);
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
        
        log.warn("No Maven or Gradle build file found in project: {}. Falling back to directory crawler.", projectRoot);
        
        // Fallback for Eclipse / legacy projects without Maven/Gradle
        try {
            DirectoryCrawlerDependencyGraphBuilder fallbackBuilder = new DirectoryCrawlerDependencyGraphBuilder();
            DependencyGraph fallbackGraph = fallbackBuilder.buildFromProject(projectRoot);
            if (fallbackGraph != null && !fallbackGraph.getNodes().isEmpty()) {
                log.info("Directory crawler found {} dependencies for project: {}", 
                        fallbackGraph.getNodes().size(), projectRoot);
                return fallbackGraph;
            }
        } catch (Exception e) {
            log.warn("Directory crawler fallback failed for project: {}", projectRoot, e);
        }
        
        throw new DependencyGraphException("No build file found in project: " + projectRoot);
    }
    
    /**
     * Recursively searches for build files (pom.xml, build.gradle, build.gradle.kts)
     * in the project directory and its subdirectories.
     * Limited to max depth to prevent excessive directory traversal.
     *
     * @param directory The directory to search in
     * @return The path to the first build file found, or null if none found
     */
    private Path searchForBuildFile(Path directory) {
        log.debug("Walking directory tree to find build files starting from: {}", directory);
        
        List<Path> buildFiles = BuildFileDiscovery.discoverBuildFiles(directory, 4);
        if (buildFiles.isEmpty()) {
            return null;
        }
        
        log.debug("Found build file: {}", buildFiles.get(0));
        return buildFiles.get(0);
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
            
            // Determine scope from the dependency type using shared constants
            String scope = ScopeConstants.mapConfigurationToScope(dependencyType);
            
            artifacts.add(new Artifact(
                groupId,
                artifactId,
                version,
                scope,
                false
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
        
        // Try direct property resolution for dependencies
        NodeList dependencies = document.getElementsByTagName("dependency");
        for (int i = 0; i < dependencies.getLength(); i++) {
            Element dep = (Element) dependencies.item(i);
            String gId = getTextContent(dep, "groupId");
            String aId = getTextContent(dep, "artifactId");
            if (groupId.equals(gId) && artifactId.equals(aId)) {
                String version = getTextContent(dep, "version");
                if (version != null) {
                    return resolveProperty(document, version);
                }
            }
        }
        
        return null;
    }
    
    private String resolveProperty(Document document, String propertyValue) {
        if (propertyValue != null && propertyValue.startsWith("${") && propertyValue.endsWith("}")) {
            String propertyName = propertyValue.substring(2, propertyValue.length() - 1);
            Map<String, String> propertiesMap = MavenPomParser.buildPropertiesMap(document);
            return propertiesMap.getOrDefault(propertyName, null);
        }
        return propertyValue;
    }
    
    /**
     * Builds a lookup map for dependencyManagement versions keyed by "groupId:artifactId".
     * Delegates to MavenPomParser to avoid duplication.
     */
    private Map<String, String> buildDependencyManagementVersionMap(Document document) {
        return MavenPomParser.buildDependencyManagementVersionMap(document);
    }

    /**
     * Builds a lookup map for properties keyed by property name.
     * Delegates to MavenPomParser to avoid duplication.
     */
    private Map<String, String> buildPropertiesMap(Document document) {
        return MavenPomParser.buildPropertiesMap(document);
    }
    
    /**
     * Resolves version using pre-built lookup maps for O(1) performance.
     */
    private String resolveVersionFromMaps(String groupId, String artifactId, 
                                          Map<String, String> depMgmtVersions,
                                          Map<String, String> propertiesMap,
                                          Document document) {
        String key = groupId + ":" + artifactId;
        String version = depMgmtVersions.get(key);
        if (version != null) {
            // Resolve property if version is a property reference
            return resolvePropertyFromMap(version, propertiesMap);
        }
        return null;
    }
    
    /**
     * Resolves property using pre-built lookup map for O(1) performance.
     * Delegates to MavenPomParser.resolveProperty to avoid duplication.
     */
    private String resolvePropertyFromMap(String propertyValue, Map<String, String> propertiesMap) {
        return MavenPomParser.resolveProperty(propertyValue, propertiesMap);
    }
}

