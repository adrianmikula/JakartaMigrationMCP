package adrianmikula.jakartamigration.dependencyanalysis.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Loads and provides access to compatibility.yaml configuration
 * for classifying javax dependencies based on whitelist/blacklist/mixed categories.
 */
@Slf4j
public class CompatibilityConfigLoader {
    
    private static final String CONFIG_FILE = "/compatibility.yaml";
    private final CompatibilityConfig config;
    
    public CompatibilityConfigLoader() {
        this.config = loadConfig();
    }
    
    private CompatibilityConfig loadConfig() {
        try (InputStream is = getClass().getResourceAsStream(CONFIG_FILE)) {
            if (is == null) {
                log.warn("Could not find {} on classpath, using default config", CONFIG_FILE);
                return createDefaultConfig();
            }
            
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            return mapper.readValue(is, CompatibilityConfig.class);
        } catch (Exception e) {
            log.error("Failed to load compatibility config from {}: {}", CONFIG_FILE, e.getMessage());
            return createDefaultConfig();
        }
    }
    
    private CompatibilityConfig createDefaultConfig() {
        // Default configuration if file is not found
        return CompatibilityConfig.builder()
            .mavenArtifacts(MavenArtifacts.builder()
                .jdk(List.of(
                    "javax.management",
                    "javax.naming",
                    "javax.crypto",
                    "javax.net",
                    "javax.script",
                    "javax.sql"
                ))
                .safe(List.of(
                    "org.apache.commons",
                    "commons-lang",
                    "commons-io",
                    "commons-collections",
                    "commons-logging",
                    "commons-dbcp",
                    "commons-pool",
                    "commons-fileupload",
                    "commons-codec",
                    "commons-cli"
                ))
                .upgrade(List.of(
                    "javax.servlet",
                    "javax.persistence",
                    "javax.validation",
                    "javax.ws.rs",
                    "javax.ejb",
                    "javax.jms",
                    "javax.faces",
                    "javax.enterprise"
                ))
                .review(List.of(
                    "javax.xml.bind",
                    "javax.xml.ws",
                    "javax.mail",
                    "javax.activation",
                    "javax.cache",
                    "javax.measure",
                    "javax.transaction"
                ))
                .build())
            .javaPackages(JavaPackages.builder()
                .whitelist(Collections.emptyList())
                .blacklist(Collections.emptyList())
                .build())
            .build();
    }
    
    /**
     * Classifies a Maven artifact based on its groupId.
     * 
     * @param groupId The Maven groupId
     * @param artifactId The Maven artifactId (optional, for additional context)
     * @return Classification result
     */
    public ArtifactClassification classifyArtifact(String groupId, String artifactId) {
        if (groupId == null || groupId.isEmpty()) {
            return ArtifactClassification.UNKNOWN;
        }
        
        String normalizedGroup = groupId.toLowerCase();
        
        // Check JDK-provided packages first (no migration needed)
        for (String jdkPattern : sortByLengthDesc(config.getMavenArtifacts().getJdk())) {
            if (matchesPattern(normalizedGroup, jdkPattern.toLowerCase())) {
                log.debug("Artifact {}:{} matches JDK pattern {}, classified as JDK_PROVIDED", 
                    groupId, artifactId, jdkPattern);
                return ArtifactClassification.JDK_PROVIDED;
            }
        }
        
        // Check safe packages (no migration needed)
        for (String safePattern : sortByLengthDesc(config.getMavenArtifacts().getSafe())) {
            if (matchesPattern(normalizedGroup, safePattern.toLowerCase())) {
                log.debug("Artifact {}:{} matches safe pattern {}, classified as JDK_PROVIDED", 
                    groupId, artifactId, safePattern);
                return ArtifactClassification.JDK_PROVIDED;
            }
        }
        
        // Check review list (context-dependent) BEFORE upgrade list
        // This ensures more specific patterns like javax.xml.bind are matched
        // before generic patterns like javax
        for (String reviewPattern : sortByLengthDesc(config.getMavenArtifacts().getReview())) {
            if (matchesPattern(normalizedGroup, reviewPattern.toLowerCase())) {
                log.debug("Artifact {}:{} matches review pattern {}, classified as CONTEXT_DEPENDENT", 
                    groupId, artifactId, reviewPattern);
                return ArtifactClassification.CONTEXT_DEPENDENT;
            }
        }
        
        // Check upgrade list (requires Jakarta migration)
        for (String upgradePattern : sortByLengthDesc(config.getMavenArtifacts().getUpgrade())) {
            if (matchesPattern(normalizedGroup, upgradePattern.toLowerCase())) {
                log.debug("Artifact {}:{} matches upgrade pattern {}, classified as JAKARTA_REQUIRED", 
                    groupId, artifactId, upgradePattern);
                return ArtifactClassification.JAKARTA_REQUIRED;
            }
        }
        
        // Not in any list - needs further investigation
        log.debug("Artifact {}:{} not found in any compatibility list, classified as UNKNOWN", 
            groupId, artifactId);
        return ArtifactClassification.UNKNOWN;
    }
    
    /**
     * Checks if a groupId matches a pattern (exact match or starts with)
     */
    private boolean matchesPattern(String groupId, String pattern) {
        return groupId.equals(pattern) || groupId.startsWith(pattern + ".");
    }
    
    /**
     * Sorts a list of patterns by length in descending order.
     * This ensures more specific (longer) patterns are checked before generic (shorter) ones.
     */
    private List<String> sortByLengthDesc(List<String> patterns) {
        if (patterns == null) {
            return Collections.emptyList();
        }
        return patterns.stream()
            .sorted((a, b) -> Integer.compare(b.length(), a.length()))
            .collect(Collectors.toList());
    }
    
    /**
     * Gets all JDK-provided patterns
     */
    public List<String> getJdkPatterns() {
        return config.getMavenArtifacts().getJdk();
    }
    
    /**
     * Gets all safe/no-migration-needed patterns
     */
    public List<String> getSafePatterns() {
        return config.getMavenArtifacts().getSafe();
    }
    
    /**
     * Gets all upgrade-required patterns
     */
    public List<String> getUpgradePatterns() {
        return config.getMavenArtifacts().getUpgrade();
    }
    
    /**
     * Gets all review-required patterns
     */
    public List<String> getReviewPatterns() {
        return config.getMavenArtifacts().getReview();
    }
    
    /**
     * Artifact classification categories
     */
    public enum ArtifactClassification {
        /** JDK-provided, no Jakarta migration needed */
        JDK_PROVIDED,
        /** Must migrate to Jakarta EE equivalent */
        JAKARTA_REQUIRED,
        /** Context-dependent, requires manual review */
        CONTEXT_DEPENDENT,
        /** Not classified, needs lookup or manual review */
        UNKNOWN
    }
}
