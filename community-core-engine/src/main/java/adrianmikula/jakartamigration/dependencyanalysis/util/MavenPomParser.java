package adrianmikula.jakartamigration.dependencyanalysis.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared Maven POM parsing utility.
 * Consolidates DOM-based and regex-based POM parsing logic.
 */
public final class MavenPomParser {

    private static final DocumentBuilderFactory DOCUMENT_FACTORY = DocumentBuilderFactory.newInstance();
    private static final Pattern DEPENDENCY_PATTERN = Pattern.compile(
            "<dependency>\\s*<groupId>([^<]+)</groupId>\\s*<artifactId>([^<]+)</artifactId>\\s*<version>([^<]*)</version>(?:\\s*<scope>([^<]*)</scope>)?",
            Pattern.MULTILINE | Pattern.DOTALL);
    private static final Pattern PROPERTY_PATTERN = Pattern.compile(
            "<properties>\\s*(.*?)\\s*</properties>",
            Pattern.DOTALL);
    private static final Pattern PROPERTY_ENTRY_PATTERN = Pattern.compile(
            "<([^>]+)>([^<]*)</\\1>");

    private MavenPomParser() {
        // Utility class
    }

    /**
     * Parses a Maven POM file and returns dependencies as a list of maps.
     * Each map contains: groupId, artifactId, version, scope.
     */
    public static List<Map<String, String>> parseDependencies(Path pomXmlPath) throws IOException {
        String content = Files.readString(pomXmlPath);
        return parseDependenciesFromContent(content);
    }

    /**
     * Parses Maven POM content and returns dependencies as a list of maps.
     */
    public static List<Map<String, String>> parseDependenciesFromContent(String content) {
        Map<String, String> properties = extractProperties(content);
        return parseDependenciesWithProperties(content, properties);
    }

    /**
     * Extracts Maven properties from POM content.
     */
    public static Map<String, String> extractProperties(String content) {
        Map<String, String> properties = new HashMap<>();

        Matcher propertyBlockMatcher = PROPERTY_PATTERN.matcher(content);
        if (propertyBlockMatcher.find()) {
            String propertyBlock = propertyBlockMatcher.group(1);
            Matcher entryMatcher = PROPERTY_ENTRY_PATTERN.matcher(propertyBlock);
            while (entryMatcher.find()) {
                properties.put(entryMatcher.group(1), entryMatcher.group(2));
            }
        }

        return properties;
    }

    /**
     * Parses dependencies from POM content with property resolution.
     */
    public static List<Map<String, String>> parseDependenciesWithProperties(
            String content, Map<String, String> properties) {
        List<Map<String, String>> dependencies = new ArrayList<>();

        Matcher matcher = DEPENDENCY_PATTERN.matcher(content);
        while (matcher.find()) {
            String groupId = matcher.group(1).trim();
            String artifactId = matcher.group(2).trim();
            String version = matcher.group(3).trim();
            String scope = matcher.group(4) != null ? matcher.group(4).trim() : "compile";

            // Resolve property references in version
            if (version.startsWith("${") && version.endsWith("}")) {
                String propertyName = version.substring(2, version.length() - 1);
                version = properties.getOrDefault(propertyName, "unknown");
            }

            Map<String, String> dependency = new LinkedHashMap<>();
            dependency.put("groupId", groupId);
            dependency.put("artifactId", artifactId);
            dependency.put("version", version);
            dependency.put("scope", scope);
            dependencies.add(dependency);
        }

        return dependencies;
    }

    /**
     * Resolves a property reference from a properties map.
     * Returns the resolved value, or the original string if not a property reference.
     */
    public static String resolveProperty(String value, Map<String, String> properties) {
        if (value == null || !value.startsWith("${") || !value.endsWith("}")) {
            return value;
        }
        String propertyName = value.substring(2, value.length() - 1);
        return properties.getOrDefault(propertyName, value);
    }

    /**
     * Parses a POM file using DOM parser for more accurate parsing.
     * Returns dependencies as a list of maps.
     */
    public static List<Map<String, String>> parseWithDom(Path pomXmlPath) throws Exception {
        DocumentBuilder builder = DOCUMENT_FACTORY.newDocumentBuilder();
        Document document = builder.parse(pomXmlPath.toFile());

        Map<String, String> properties = buildPropertiesMap(document);
        Map<String, String> depMgmtVersions = buildDependencyManagementVersionMap(document);

        List<Map<String, String>> dependencies = new ArrayList<>();
        NodeList dependencyNodes = document.getElementsByTagName("dependency");

        for (int i = 0; i < dependencyNodes.getLength(); i++) {
            Element dependencyElement = (Element) dependencyNodes.item(i);

            String groupId = getTextContent(dependencyElement, "groupId");
            String artifactId = getTextContent(dependencyElement, "artifactId");
            String version = getTextContent(dependencyElement, "version");
            String scope = getTextContent(dependencyElement, "scope");

            if (groupId == null || artifactId == null) {
                continue;
            }

            // Resolve version
            if (version == null) {
                String key = groupId + ":" + artifactId;
                version = depMgmtVersions.getOrDefault(key, "unknown");
            } else if (version.startsWith("${") && version.endsWith("}")) {
                version = resolveProperty(version, properties);
            }

            if (scope == null) {
                scope = "compile";
            }

            Map<String, String> dependency = new LinkedHashMap<>();
            dependency.put("groupId", groupId);
            dependency.put("artifactId", artifactId);
            dependency.put("version", version);
            dependency.put("scope", scope);
            dependencies.add(dependency);
        }

        return dependencies;
    }

    /**
     * Builds a properties map from a DOM document.
     */
    public static Map<String, String> buildPropertiesMap(Document document) {
        Map<String, String> properties = new HashMap<>();
        NodeList propertyNodes = document.getElementsByTagName("properties");

        if (propertyNodes.getLength() > 0) {
            Element propertiesElement = (Element) propertyNodes.item(0);
            NodeList children = propertiesElement.getChildNodes();

            for (int i = 0; i < children.getLength(); i++) {
                if (children.item(i) instanceof Element) {
                    Element child = (Element) children.item(i);
                    properties.put(child.getTagName(), child.getTextContent());
                }
            }
        }

        return properties;
    }

    /**
     * Builds a dependency management version map from a DOM document.
     */
    public static Map<String, String> buildDependencyManagementVersionMap(Document document) {
        Map<String, String> depMgmtVersions = new HashMap<>();
        NodeList depMgmtNodes = document.getElementsByTagName("dependencyManagement");

        if (depMgmtNodes.getLength() > 0) {
            Element depMgmt = (Element) depMgmtNodes.item(0);
            NodeList dependencyNodes = depMgmt.getElementsByTagName("dependency");

            for (int i = 0; i < dependencyNodes.getLength(); i++) {
                Element dep = (Element) dependencyNodes.item(i);
                String groupId = getTextContent(dep, "groupId");
                String artifactId = getTextContent(dep, "artifactId");
                String version = getTextContent(dep, "version");

                if (groupId != null && artifactId != null && version != null) {
                    String key = groupId + ":" + artifactId;
                    depMgmtVersions.put(key, version);
                }
            }
        }

        return depMgmtVersions;
    }

    private static String getTextContent(Element element, String tagName) {
        NodeList nodeList = element.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent().trim();
        }
        return null;
    }
}
