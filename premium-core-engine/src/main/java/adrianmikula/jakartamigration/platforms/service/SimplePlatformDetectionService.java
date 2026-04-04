package adrianmikula.jakartamigration.platforms.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Simplified platform detection service
 * Directly scans for application servers without complex abstractions
 */
public class SimplePlatformDetectionService {
    
    private static final Pattern WILDFLY_EJB = Pattern.compile("<packaging>ejb</packaging>", Pattern.CASE_INSENSITIVE);
    private static final Pattern WILDFLY_JAVAX_EJB = Pattern.compile("<scope>provided</scope>[\\s\\S]*javax\\.ejb", Pattern.CASE_INSENSITIVE);
    private static final Pattern TOMCAT_WAR = Pattern.compile("<packaging>war</packaging>", Pattern.CASE_INSENSITIVE);
    private static final Pattern TOMCAT_JAVAX_SERVLET = Pattern.compile("<scope>provided</scope>[\\s\\S]*javax\\.servlet", Pattern.CASE_INSENSITIVE);
    private static final Pattern JETTY_DEPENDENCY = Pattern.compile("org\\.eclipse\\.jetty", Pattern.CASE_INSENSITIVE);
    private static final Pattern PAYARA_DEPENDENCY = Pattern.compile("fish\\.payara", Pattern.CASE_INSENSITIVE);
    
    /**
     * Simple scan for application servers
     */
    public List<String> scanProject(Path projectPath) {
        List<String> detectedServers = new ArrayList<>();
        
        try {
            // Scan pom.xml if exists
            Path pomFile = projectPath.resolve("pom.xml");
            if (Files.exists(pomFile)) {
                String content = Files.readString(pomFile);
                
                // WildFly detection
                if (WILDFLY_EJB.matcher(content).find() || WILDFLY_JAVAX_EJB.matcher(content).find()) {
                    detectedServers.add("WildFly");
                }
                
                // Tomcat detection
                if (TOMCAT_WAR.matcher(content).find() || TOMCAT_JAVAX_SERVLET.matcher(content).find()) {
                    detectedServers.add("Tomcat");
                }
                
                // Jetty detection
                if (JETTY_DEPENDENCY.matcher(content).find()) {
                    detectedServers.add("Jetty");
                }
                
                // Payara detection
                if (PAYARA_DEPENDENCY.matcher(content).find()) {
                    detectedServers.add("Payara");
                }
            }
            
            // Scan build.gradle if exists
            Path gradleFile = projectPath.resolve("build.gradle");
            if (Files.exists(gradleFile)) {
                String content = Files.readString(gradleFile);
                
                if (JETTY_DEPENDENCY.matcher(content).find()) {
                    detectedServers.add("Jetty");
                }
                
                if (PAYARA_DEPENDENCY.matcher(content).find()) {
                    detectedServers.add("Payara");
                }
            }
            
        } catch (IOException e) {
            // Log error but don't fail the scan
            System.err.println("Error scanning project: " + e.getMessage());
        }
        
        return detectedServers;
    }
}
