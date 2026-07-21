package adrianmikula.jakartamigration.dependencyanalysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Simplified Maven Central lookup service with fuzzy matching capabilities.
 * This is a lightweight version of the MavenCentralService for the community module.
 */
@Slf4j
public class ImprovedMavenCentralLookupService {
    
    private static final String MAVEN_CENTRAL_API = "https://search.maven.org/solrsearch/select";
    private static final String MAVEN_CENTRAL_FALLBACK = "https://search.maven.org/solrsearch/select";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    // HTTP client is instance field to allow mocking in tests
    private HttpClient httpClient;
    
    public ImprovedMavenCentralLookupService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }
    
    // Package-private constructor for testing with mocked HTTP client
    ImprovedMavenCentralLookupService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }
    
    // Common artifact name mappings for fuzzy matching
    private static final Map<String, String> ARTIFACT_MAPPINGS = new HashMap<>();
    static {
        // Standard Jakarta EE API mappings
        ARTIFACT_MAPPINGS.put("javax.servlet-api", "jakarta.servlet-api");
        ARTIFACT_MAPPINGS.put("javax.servlet.jsp-api", "jakarta.servlet.jsp-api");
        ARTIFACT_MAPPINGS.put("javax.servlet.jsp.jstl-api", "jakarta.servlet.jsp.jstl-api");
        ARTIFACT_MAPPINGS.put("javax.persistence-api", "jakarta.persistence-api");
        ARTIFACT_MAPPINGS.put("javax.transaction-api", "jakarta.transaction-api");
        ARTIFACT_MAPPINGS.put("javax.validation-api", "jakarta.validation-api");
        ARTIFACT_MAPPINGS.put("javax.inject", "jakarta.inject");
        ARTIFACT_MAPPINGS.put("javax.annotation-api", "jakarta.annotation-api");
        ARTIFACT_MAPPINGS.put("javax.ejb-api", "jakarta.ejb-api");
        ARTIFACT_MAPPINGS.put("javax.faces-api", "jakarta.faces-api");
        ARTIFACT_MAPPINGS.put("javax.jms-api", "jakarta.jms-api");
        ARTIFACT_MAPPINGS.put("javax.json-api", "jakarta.json-api");
        ARTIFACT_MAPPINGS.put("javax.websocket-api", "jakarta.websocket-api");
        ARTIFACT_MAPPINGS.put("javax.xml.bind-api", "jakarta.xml.bind-api");
        ARTIFACT_MAPPINGS.put("javax.xml.ws-api", "jakarta.xml.ws-api");
        ARTIFACT_MAPPINGS.put("javax.ws.rs-api", "jakarta.ws.rs-api");
        ARTIFACT_MAPPINGS.put("javax.mail-api", "jakarta.mail-api");
        ARTIFACT_MAPPINGS.put("javax.enterprise.cdi-api", "jakarta.enterprise.cdi-api");
        ARTIFACT_MAPPINGS.put("javax.security.enterprise-api", "jakarta.security.enterprise-api");

        // Spring Framework Ecosystem mappings (Boot 2.x → 3.x, Framework 5.x → 6.x)
        ARTIFACT_MAPPINGS.put("spring-boot-starter-web", "spring-boot-starter-web");
        ARTIFACT_MAPPINGS.put("spring-boot-starter-data-jpa", "spring-boot-starter-data-jpa");
        ARTIFACT_MAPPINGS.put("spring-boot-starter-validation", "spring-boot-starter-validation");
        ARTIFACT_MAPPINGS.put("spring-boot-starter-websocket", "spring-boot-starter-websocket");
        ARTIFACT_MAPPINGS.put("spring-boot-starter-mail", "spring-boot-starter-mail");
        ARTIFACT_MAPPINGS.put("spring-boot-starter-jersey", "spring-boot-starter-jersey");
        ARTIFACT_MAPPINGS.put("spring-boot-starter-webflux", "spring-boot-starter-webflux");
        ARTIFACT_MAPPINGS.put("spring-boot-starter-data-rest", "spring-boot-starter-data-rest");
        ARTIFACT_MAPPINGS.put("spring-boot-starter-security", "spring-boot-starter-security");
        ARTIFACT_MAPPINGS.put("spring-boot-starter-test", "spring-boot-starter-test");
        ARTIFACT_MAPPINGS.put("spring-data-jpa", "spring-data-jpa");
        ARTIFACT_MAPPINGS.put("spring-data-rest", "spring-data-rest");
        ARTIFACT_MAPPINGS.put("spring-security-config", "spring-security-config");
        ARTIFACT_MAPPINGS.put("spring-security-web", "spring-security-web");
        ARTIFACT_MAPPINGS.put("spring-web", "spring-web");
        ARTIFACT_MAPPINGS.put("spring-webmvc", "spring-webmvc");

        // JAX-RS Implementation mappings
        // Jersey: com.sun.jersey → org.glassfish.jersey
        ARTIFACT_MAPPINGS.put("jersey-core", "jersey-core");
        ARTIFACT_MAPPINGS.put("jersey-client", "jersey-client");
        ARTIFACT_MAPPINGS.put("jersey-server", "jersey-server");
        ARTIFACT_MAPPINGS.put("jersey-servlet", "jersey-container-servlet");
        // RESTEasy version-specific Jakarta compatibility
        ARTIFACT_MAPPINGS.put("resteasy-jaxrs", "resteasy-core");
        ARTIFACT_MAPPINGS.put("resteasy-client", "resteasy-client");
        ARTIFACT_MAPPINGS.put("resteasy-spring", "resteasy-spring");
        // Apache CXF complex artifact restructurings
        ARTIFACT_MAPPINGS.put("cxf-rt-frontend-jaxrs", "cxf-rt-frontend-jaxrs");
        ARTIFACT_MAPPINGS.put("cxf-rt-transports-http", "cxf-rt-transports-http");
        ARTIFACT_MAPPINGS.put("cxf-rt-rs-client", "cxf-rt-rs-client");

        // Enterprise Framework mappings
        ARTIFACT_MAPPINGS.put("wicket-core", "wicket-core");
        ARTIFACT_MAPPINGS.put("wicket-spring", "wicket-spring");
        ARTIFACT_MAPPINGS.put("mybatis-spring-boot-starter", "mybatis-spring-boot-starter");
        ARTIFACT_MAPPINGS.put("hibernate-validator", "hibernate-validator");
        ARTIFACT_MAPPINGS.put("hibernate-core", "hibernate-core");
        ARTIFACT_MAPPINGS.put("hibernate-entitymanager", "hibernate-core");

        // Bridge artifacts that support both javax and jakarta
        ARTIFACT_MAPPINGS.put("validation-api", "jakarta.validation-api");
        ARTIFACT_MAPPINGS.put("jsr250-api", "jakarta.annotation-api");
        ARTIFACT_MAPPINGS.put("javax.annotation-api", "jakarta.annotation-api");

        // Critical artifacts detected in real-world projects
        ARTIFACT_MAPPINGS.put("javax.json", "jakarta.json"); // GlassFish JSON implementation
        ARTIFACT_MAPPINGS.put("jaxb-runtime", "jaxb-runtime"); // Eclipse JAXB runtime
        ARTIFACT_MAPPINGS.put("arquillian-protocol-servlet", "arquillian-protocol-servlet"); // Arquillian testing
        ARTIFACT_MAPPINGS.put("tomee-embedded", "tomee-embedded"); // Apache TomEE
        ARTIFACT_MAPPINGS.put("wildfly-arquillian-container-remote", "wildfly-arquillian-container-remote"); // WildFly testing
        ARTIFACT_MAPPINGS.put("wildfly-arquillian-container-managed", "wildfly-arquillian-container-managed"); // WildFly testing
        ARTIFACT_MAPPINGS.put("shrinkwrap-resolver-impl-maven", "shrinkwrap-resolver-impl-maven"); // ShrinkWrap resolver
    }
    
    // Common group name mappings for fuzzy matching
    private static final Map<String, String> GROUP_MAPPINGS = new HashMap<>();
    static {
        // Standard Jakarta EE group mappings
        GROUP_MAPPINGS.put("javax.servlet", "jakarta.servlet");
        GROUP_MAPPINGS.put("javax.persistence", "jakarta.persistence");
        GROUP_MAPPINGS.put("javax.transaction", "jakarta.transaction");
        GROUP_MAPPINGS.put("javax.validation", "jakarta.validation");
        GROUP_MAPPINGS.put("javax.inject", "jakarta.inject");
        GROUP_MAPPINGS.put("javax.annotation", "jakarta.annotation");
        GROUP_MAPPINGS.put("javax.ejb", "jakarta.ejb");
        GROUP_MAPPINGS.put("javax.faces", "jakarta.faces");
        GROUP_MAPPINGS.put("javax.jms", "jakarta.jms");
        GROUP_MAPPINGS.put("javax.json", "jakarta.json");
        GROUP_MAPPINGS.put("javax.websocket", "jakarta.websocket");
        GROUP_MAPPINGS.put("javax.xml.bind", "jakarta.xml.bind");
        GROUP_MAPPINGS.put("javax.xml.ws", "jakarta.xml.ws");
        GROUP_MAPPINGS.put("javax.ws.rs", "jakarta.ws.rs");
        GROUP_MAPPINGS.put("javax.mail", "jakarta.mail");
        GROUP_MAPPINGS.put("javax.enterprise", "jakarta.enterprise");
        GROUP_MAPPINGS.put("javax.security", "jakarta.security");
        GROUP_MAPPINGS.put("javax.servlet.jsp", "jakarta.servlet.jsp");
        GROUP_MAPPINGS.put("javax.servlet.jsp.jstl", "jakarta.servlet.jsp.jstl");

        // Spring Framework Ecosystem group mappings
        // Spring Boot and Framework stay in org.springframework.boot|framework groups
        GROUP_MAPPINGS.put("org.springframework.boot", "org.springframework.boot");
        GROUP_MAPPINGS.put("org.springframework", "org.springframework");
        GROUP_MAPPINGS.put("org.springframework.data", "org.springframework.data");
        GROUP_MAPPINGS.put("org.springframework.security", "org.springframework.security");

        // JAX-RS Implementation group mappings
        // Jersey: com.sun.jersey → org.glassfish.jersey
        GROUP_MAPPINGS.put("com.sun.jersey", "org.glassfish.jersey");
        GROUP_MAPPINGS.put("com.sun.jersey.contribs", "org.glassfish.jersey.ext");
        // RESTEasy stays in org.jboss.resteasy
        GROUP_MAPPINGS.put("org.jboss.resteasy", "org.jboss.resteasy");
        // Apache CXF stays in org.apache.cxf
        GROUP_MAPPINGS.put("org.apache.cxf", "org.apache.cxf");

        // Enterprise Framework group mappings
        // Apache Wicket moved from org.apache.wicket to different structure
        GROUP_MAPPINGS.put("org.apache.wicket", "org.apache.wicket");
        // MyBatis Spring Boot stays in org.mybatis.spring.boot
        GROUP_MAPPINGS.put("org.mybatis.spring.boot", "org.mybatis.spring.boot");
        GROUP_MAPPINGS.put("org.mybatis", "org.mybatis");
        // Hibernate stays in org.hibernate
        GROUP_MAPPINGS.put("org.hibernate", "org.hibernate");
        GROUP_MAPPINGS.put("org.hibernate.validator", "org.hibernate.validator");

        // Critical groups detected in real-world projects
        GROUP_MAPPINGS.put("org.glassfish", "org.eclipse.ee4j"); // GlassFish JSON → Eclipse EE4J
        GROUP_MAPPINGS.put("org.glassfish.jaxb", "org.glassfish.jaxb"); // JAXB runtime stays same
        GROUP_MAPPINGS.put("org.jboss.arquillian.protocol", "org.jboss.arquillian.protocol"); // Arquillian stays same
        GROUP_MAPPINGS.put("org.apache.tomee", "org.apache.tomee"); // TomEE stays same
        GROUP_MAPPINGS.put("org.wildfly.arquillian", "org.wildfly.arquillian"); // WildFly Arquillian stays same
        GROUP_MAPPINGS.put("org.jboss.shrinkwrap.resolver", "org.jboss.shrinkwrap.resolver"); // ShrinkWrap stays same
    }
    
    /**
     * Result of an artifact lookup containing Jakarta artifact information.
     */
    public record JakartaArtifactMatch(
            String groupId,
            String artifactId,
            String version,
            boolean found
    ) {
        public static JakartaArtifactMatch notFound() {
            return new JakartaArtifactMatch(null, null, null, false);
        }
        
        public static JakartaArtifactMatch of(String groupId, String artifactId, String version) {
            return new JakartaArtifactMatch(groupId, artifactId, version, true);
        }
    }
    
    /**
     * Finds Jakarta equivalent artifacts with fuzzy matching strategies.
     */
    public CompletableFuture<List<JakartaArtifactMatch>> findJakartaEquivalents(
            String javaxGroupId, 
            String javaxArtifactId) {
        
        log.info("Searching for Jakarta equivalents for javax dependency: {}:{}", javaxGroupId, javaxArtifactId);
        
        // Input validation
        if (javaxGroupId == null || javaxGroupId.trim().isEmpty() || 
            javaxArtifactId == null || javaxArtifactId.trim().isEmpty()) {
            log.warn("Invalid coordinates provided: groupId='{}', artifactId='{}'", javaxGroupId, javaxArtifactId);
            return CompletableFuture.completedFuture(List.of());
        }
        
        // Normalize to lowercase for case-insensitive matching
        String normalizedGroupId = javaxGroupId.toLowerCase();
        String normalizedArtifactId = javaxArtifactId.toLowerCase();
        
        return CompletableFuture.supplyAsync(() -> {
            List<JakartaArtifactMatch> allResults = new ArrayList<>();
            
            // Try multiple search strategies for fuzzy matching
            allResults.addAll(searchWithExactMatch(normalizedGroupId, normalizedArtifactId));
            allResults.addAll(searchWithArtifactNameMapping(normalizedGroupId, normalizedArtifactId));
            allResults.addAll(searchWithGroupNameMapping(normalizedGroupId, normalizedArtifactId));
            allResults.addAll(searchWithNamingVariations(normalizedGroupId, normalizedArtifactId));
            allResults.addAll(searchWithCaseInsensitiveVariations(normalizedGroupId, normalizedArtifactId));
            allResults.addAll(searchWithFrameworkMappings(normalizedGroupId, normalizedArtifactId));
            allResults.addAll(searchWithJerseyMigrationPath(normalizedGroupId, normalizedArtifactId));
            allResults.addAll(searchWithSpringBootVersionStrategy(normalizedGroupId, normalizedArtifactId));
            
            // Remove duplicates and return first few results
            List<JakartaArtifactMatch> uniqueResults = allResults.stream()
                    .distinct()
                    .limit(5) // Limit to top 5 results
                    .toList();
            
            log.info("Found {} unique Jakarta artifacts for {}:{}", uniqueResults.size(), javaxGroupId, javaxArtifactId);
            return uniqueResults;
        });
    }
    
    /**
     * Search with exact match strategy
     */
    private List<JakartaArtifactMatch> searchWithExactMatch(String groupId, String artifactId) {
        return performMavenCentralSearch(groupId, artifactId);
    }
    
    /**
     * Search with common artifact name mappings (javax → jakarta)
     */
    private List<JakartaArtifactMatch> searchWithArtifactNameMapping(String groupId, String artifactId) {
        List<JakartaArtifactMatch> results = new ArrayList<>();
        
        String mappedArtifactId = ARTIFACT_MAPPINGS.get(artifactId);
        if (mappedArtifactId != null) {
            // Also map the groupId if it's a javax group
            String mappedGroupId = GROUP_MAPPINGS.get(groupId);
            if (mappedGroupId != null) {
                results.addAll(performMavenCentralSearch(mappedGroupId, mappedArtifactId));
            } else {
                results.addAll(performMavenCentralSearch(groupId, mappedArtifactId));
            }
        }
        
        return results;
    }
    
    /**
     * Search with common group name mappings (javax → jakarta)
     */
    private List<JakartaArtifactMatch> searchWithGroupNameMapping(String groupId, String artifactId) {
        List<JakartaArtifactMatch> results = new ArrayList<>();
        
        String mappedGroupId = GROUP_MAPPINGS.get(groupId);
        if (mappedGroupId != null) {
            results.addAll(performMavenCentralSearch(mappedGroupId, artifactId));
        }
        
        return results;
    }
    
    /**
     * Search with naming variations (e.g., "javax.servlet" vs "javax.servlet-api")
     */
    private List<JakartaArtifactMatch> searchWithNamingVariations(String groupId, String artifactId) {
        List<JakartaArtifactMatch> results = new ArrayList<>();
        
        // Try removing -api suffix if present
        if (artifactId.endsWith("-api")) {
            String baseArtifactId = artifactId.substring(0, artifactId.length() - 4);
            results.addAll(performMavenCentralSearch(groupId, baseArtifactId));
        }
        // Try adding -api suffix if not present
        else if (!artifactId.endsWith("-api")) {
            String apiArtifactId = artifactId + "-api";
            results.addAll(performMavenCentralSearch(groupId, apiArtifactId));
        }
        
        return results;
    }
    
    /**
     * Search with case insensitive variations
     */
    private List<JakartaArtifactMatch> searchWithCaseInsensitiveVariations(String groupId, String artifactId) {
        List<JakartaArtifactMatch> results = new ArrayList<>();
        
        // Try lowercase versions
        String lowerGroupId = groupId.toLowerCase();
        String lowerArtifactId = artifactId.toLowerCase();
        
        if (!groupId.equals(lowerGroupId) || !artifactId.equals(lowerArtifactId)) {
            results.addAll(performMavenCentralSearch(lowerGroupId, lowerArtifactId));
        }
        
        return results;
    }

    /**
     * Framework-specific mappings for popular frameworks with complex migration patterns.
     * Handles Spring, JAX-RS implementations, and enterprise frameworks.
     */
    private List<JakartaArtifactMatch> searchWithFrameworkMappings(String groupId, String artifactId) {
        List<JakartaArtifactMatch> results = new ArrayList<>();

        // Spring Framework Ecosystem - same artifact IDs, but version determines Jakarta compatibility
        if (groupId.startsWith("org.springframework")) {
            results.addAll(searchSpringFrameworkArtifacts(groupId, artifactId));
        }

        // JAX-RS Implementations
        if (groupId.contains("jersey") || artifactId.contains("jersey")) {
            results.addAll(searchJerseyArtifacts(groupId, artifactId));
        }
        if (groupId.contains("resteasy") || artifactId.contains("resteasy")) {
            results.addAll(searchResteasyArtifacts(groupId, artifactId));
        }
        if (groupId.contains("cxf") || artifactId.contains("cxf")) {
            results.addAll(searchCxfArtifacts(groupId, artifactId));
        }

        // Enterprise frameworks
        if (groupId.contains("wicket") || artifactId.contains("wicket")) {
            results.addAll(searchWicketArtifacts(groupId, artifactId));
        }
        if (groupId.contains("mybatis") || artifactId.contains("mybatis")) {
            results.addAll(searchMybatisArtifacts(groupId, artifactId));
        }
        if (groupId.contains("hibernate") || artifactId.contains("hibernate")) {
            results.addAll(searchHibernateArtifacts(groupId, artifactId));
        }

        return results;
    }

    /**
     * Spring Framework specific search - handles Boot 2.x → 3.x migrations.
     * Spring Boot 3.x and Framework 6.x are Jakarta EE 9+ compatible.
     */
    private List<JakartaArtifactMatch> searchSpringFrameworkArtifacts(String groupId, String artifactId) {
        List<JakartaArtifactMatch> results = new ArrayList<>();

        // Spring artifacts stay in the same group, but we want to find Jakarta-compatible versions
        // Spring Boot 3.x+ and Framework 6.x+ use jakarta.* packages
        if (groupId.equals("org.springframework.boot") && artifactId.startsWith("spring-boot-starter")) {
            // For Spring Boot starters, search for the same artifact (versions 3.x+ are Jakarta)
            results.addAll(performMavenCentralSearch(groupId, artifactId));
        } else if (groupId.equals("org.springframework")) {
            // Spring Framework 6.x+ artifacts
            results.addAll(performMavenCentralSearch(groupId, artifactId));
        } else if (groupId.equals("org.springframework.data")) {
            // Spring Data 2022.x+ (3.x for JPA) supports Jakarta
            results.addAll(performMavenCentralSearch(groupId, artifactId));
        } else if (groupId.equals("org.springframework.security")) {
            // Spring Security 6.x+ supports Jakarta
            results.addAll(performMavenCentralSearch(groupId, artifactId));
        }

        return results;
    }

    /**
     * Jersey JAX-RS implementation search.
     * Jersey 3.x+ uses jakarta.* packages with org.glassfish.jersey group.
     */
    private List<JakartaArtifactMatch> searchJerseyArtifacts(String groupId, String artifactId) {
        List<JakartaArtifactMatch> results = new ArrayList<>();

        // Jersey moved from com.sun.jersey to org.glassfish.jersey in 2.x
        // Jersey 3.x+ uses jakarta.* packages
        if (groupId.startsWith("com.sun.jersey")) {
            String newGroupId = groupId.replace("com.sun.jersey", "org.glassfish.jersey");
            // Map old artifact names to new ones where changed
            String newArtifactId = artifactId;
            if (artifactId.equals("jersey-servlet")) {
                newArtifactId = "jersey-container-servlet";
            }
            results.addAll(performMavenCentralSearch(newGroupId, newArtifactId));
        } else if (groupId.startsWith("org.glassfish.jersey")) {
            // Already in new group, search for jakarta-compatible versions
            results.addAll(performMavenCentralSearch(groupId, artifactId));
        }

        return results;
    }

    /**
     * RESTEasy JAX-RS implementation search.
     * RESTEasy 6.x+ uses jakarta.* packages.
     */
    private List<JakartaArtifactMatch> searchResteasyArtifacts(String groupId, String artifactId) {
        List<JakartaArtifactMatch> results = new ArrayList<>();

        // RESTEasy 6.x+ uses jakarta.* packages, same group ID
        if (groupId.equals("org.jboss.resteasy")) {
            // Map old artifact IDs to new ones where they changed
            String newArtifactId = artifactId;
            if (artifactId.equals("resteasy-jaxrs")) {
                newArtifactId = "resteasy-core";
            }
            results.addAll(performMavenCentralSearch(groupId, newArtifactId));
        }

        return results;
    }

    /**
     * Apache CXF JAX-RS implementation search.
     * CXF 4.x+ uses jakarta.* packages.
     */
    private List<JakartaArtifactMatch> searchCxfArtifacts(String groupId, String artifactId) {
        List<JakartaArtifactMatch> results = new ArrayList<>();

        // CXF 4.x+ uses jakarta.* packages, same group ID
        if (groupId.startsWith("org.apache.cxf")) {
            results.addAll(performMavenCentralSearch(groupId, artifactId));
        }

        return results;
    }

    /**
     * Apache Wicket search.
     * Wicket 10.x (Jakarta EE 9) and 9.x (Jakarta EE 8) use jakarta.* packages.
     */
    private List<JakartaArtifactMatch> searchWicketArtifacts(String groupId, String artifactId) {
        List<JakartaArtifactMatch> results = new ArrayList<>();

        // Wicket 9.x+ uses jakarta.* packages, same group ID
        if (groupId.equals("org.apache.wicket")) {
            results.addAll(performMavenCentralSearch(groupId, artifactId));
        }

        return results;
    }

    /**
     * MyBatis search.
     * MyBatis 3.5.10+ and MyBatis-Spring-Boot 3.x+ support Jakarta.
     */
    private List<JakartaArtifactMatch> searchMybatisArtifacts(String groupId, String artifactId) {
        List<JakartaArtifactMatch> results = new ArrayList<>();

        // MyBatis Spring Boot 3.x+ uses jakarta.* packages
        if (groupId.equals("org.mybatis.spring.boot") || groupId.equals("org.mybatis")) {
            results.addAll(performMavenCentralSearch(groupId, artifactId));
        }

        return results;
    }

    /**
     * Hibernate search.
     * Hibernate 6.x+ uses jakarta.* packages natively.
     * Hibernate Validator 7.x+ uses jakarta.* packages.
     */
    private List<JakartaArtifactMatch> searchHibernateArtifacts(String groupId, String artifactId) {
        List<JakartaArtifactMatch> results = new ArrayList<>();

        if (groupId.equals("org.hibernate")) {
            // hibernate-entitymanager was merged into hibernate-core in 5.2+
            // hibernate-core 6.x+ uses jakarta.* packages
            String searchArtifactId = artifactId;
            if (artifactId.equals("hibernate-entitymanager")) {
                searchArtifactId = "hibernate-core";
            }
            results.addAll(performMavenCentralSearch(groupId, searchArtifactId));
        } else if (groupId.equals("org.hibernate.validator")) {
            // Hibernate Validator 7.x+ uses jakarta.* packages
            results.addAll(performMavenCentralSearch(groupId, artifactId));
        }

        return results;
    }

    /**
     * Jersey-specific migration path handling for com.sun.jersey → org.glassfish.jersey.
     * Handles the complex artifact restructuring during Jersey 1.x → 2.x/3.x migration.
     */
    private List<JakartaArtifactMatch> searchWithJerseyMigrationPath(String groupId, String artifactId) {
        List<JakartaArtifactMatch> results = new ArrayList<>();

        // Only apply to legacy Jersey artifacts
        if (!groupId.startsWith("com.sun.jersey")) {
            return results;
        }

        log.debug("Applying Jersey migration path for {}:{}", groupId, artifactId);

        // Jersey 1.x (com.sun.jersey) → 2.x/3.x (org.glassfish.jersey)
        String newGroupId = "org.glassfish.jersey";

        // Map artifact IDs from old to new structure
        Map<String, String> jerseyArtifactMapping = new HashMap<>();
        jerseyArtifactMapping.put("jersey-server", "jersey-server");
        jerseyArtifactMapping.put("jersey-client", "jersey-client");
        jerseyArtifactMapping.put("jersey-core", "jersey-core");
        jerseyArtifactMapping.put("jersey-servlet", "jersey-container-servlet");
        jerseyArtifactMapping.put("jersey-grizzly", "jersey-container-grizzly2-http");
        jerseyArtifactMapping.put("jersey-json", "jersey-media-json-jackson");
        jerseyArtifactMapping.put("jersey-multipart", "jersey-media-multipart");

        String mappedArtifactId = jerseyArtifactMapping.get(artifactId);
        if (mappedArtifactId != null) {
            results.addAll(performMavenCentralSearch(newGroupId, mappedArtifactId));
            // Also try with ext for contrib artifacts
            if (groupId.contains("contribs")) {
                results.addAll(performMavenCentralSearch("org.glassfish.jersey.ext", mappedArtifactId));
            }
        } else {
            // Try direct mapping
            results.addAll(performMavenCentralSearch(newGroupId, artifactId));
        }

        return results;
    }

    /**
     * Spring Boot version-aware search strategy.
     * Detects Spring Boot 2.x vs 3.x artifacts and suggests Jakarta-compatible versions.
     */
    private List<JakartaArtifactMatch> searchWithSpringBootVersionStrategy(String groupId, String artifactId) {
        List<JakartaArtifactMatch> results = new ArrayList<>();

        // Only apply to Spring Boot artifacts
        if (!groupId.equals("org.springframework.boot")) {
            return results;
        }

        log.debug("Applying Spring Boot version strategy for {}:{}", groupId, artifactId);

        // Spring Boot 3.x+ uses Jakarta EE 9+ (jakarta.* packages)
        // For any Spring Boot starter, we search for the same artifact
        // The version will determine Jakarta compatibility
        results.addAll(performMavenCentralSearch(groupId, artifactId));

        // Handle specific Spring Boot Jakarta-related starters
        if (artifactId.equals("spring-boot-starter-jersey")) {
            // Jersey integration in Spring Boot 3.x uses Jersey 3.x (Jakarta)
            results.addAll(performMavenCentralSearch(groupId, artifactId));
        } else if (artifactId.equals("spring-boot-starter-validation")) {
            // Validation in Boot 3.x uses jakarta.validation
            results.addAll(performMavenCentralSearch(groupId, artifactId));
        }

        return results;
    }

    /**
     * Performs the actual Maven Central search with fallback endpoints
     */
    private List<JakartaArtifactMatch> performMavenCentralSearch(String groupId, String artifactId) {
        List<JakartaArtifactMatch> results = new ArrayList<>();
        
        // Try the primary endpoint first
        results.addAll(performSearchWithEndpoint(MAVEN_CENTRAL_API, groupId, artifactId));
        
        // If no results, try alternative endpoint
        if (results.isEmpty()) {
            log.info("No results from primary endpoint, trying alternative...");
            results.addAll(performSearchWithEndpoint(MAVEN_CENTRAL_FALLBACK, groupId, artifactId));
        }
        
        return results;
    }
    
    /**
     * Performs search with a specific endpoint
     */
    private List<JakartaArtifactMatch> performSearchWithEndpoint(String endpoint, String groupId, String artifactId) {
        try {
            String searchQuery = "g:" + groupId + " AND a:" + artifactId;
            String url = endpoint + "?q=" + URLEncoder.encode(searchQuery, "UTF-8") + "&rows=5&wt=json";
            
            log.info("Querying Maven Central: {}", url);
            
            log.debug("[MavenLookup] Querying: {}", url);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .header("User-Agent", "Jakarta-Migration-MCP/1.0")
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            log.info("Maven Central response status: {} for query: {}", response.statusCode(), searchQuery);
            
            log.debug("[MavenLookup] Response status: {}", response.statusCode());
            
            if (response.statusCode() == 200) {
                String body = response.body();
                log.debug("[MavenLookup] Response body (first 300 chars): {}", body.substring(0, Math.min(300, body.length())));
                List<JakartaArtifactMatch> matches = parseMavenCentralResponse(body);
                log.debug("[MavenLookup] Parsed {} matches", matches.size());
                return matches;
            } else {
                log.warn("Failed to query Maven Central endpoint {}: HTTP {}", endpoint, response.statusCode());
                return new ArrayList<>();
            }
        } catch (java.net.ConnectException e) {
            log.warn("Connection failed to Maven Central endpoint {}: {}", endpoint, e.getMessage());
            return new ArrayList<>();
        } catch (java.net.SocketTimeoutException e) {
            log.warn("Timeout connecting to Maven Central endpoint {}: {}", endpoint, e.getMessage());
            return new ArrayList<>();
        } catch (Exception e) {
            log.warn("Error querying Maven Central endpoint {} for {}:{}", endpoint, groupId, artifactId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Parses Maven Central response to extract Jakarta artifact information
     */
    private List<JakartaArtifactMatch> parseMavenCentralResponse(String responseBody) {
        List<JakartaArtifactMatch> results = new ArrayList<>();
        
        try {
            JsonNode rootNode = OBJECT_MAPPER.readTree(responseBody);
            
            // Navigate to the "docs" array - it's inside the "response" object
            JsonNode responseNode = rootNode.path("response");
            JsonNode docsNode = responseNode.path("docs");
            
            log.debug("[MavenLookup] Response numFound: {}", responseNode.path("numFound").asInt());
            log.debug("[MavenLookup] Docs array size: {}", docsNode.size());
            
            if (docsNode.isArray() && docsNode.size() > 0) {
                for (JsonNode docNode : docsNode) {
                    String foundGroupId = docNode.path("g").asText();
                    String foundArtifactId = docNode.path("a").asText();
                    String version = docNode.path("latestVersion").asText();
                    
                    log.debug("[MavenLookup] Found artifact: {}:{}:{}", foundGroupId, foundArtifactId, version);
                    
                    if (!foundGroupId.isEmpty() && !foundArtifactId.isEmpty() && !version.isEmpty()) {
                        results.add(JakartaArtifactMatch.of(foundGroupId, foundArtifactId, version));
                        log.debug("Found Jakarta artifact: {}:{}", foundGroupId, foundArtifactId);
                    }
                }
            }
            
        } catch (Exception e) {
            log.warn("Error parsing Maven Central response", e);
            log.warn("[MavenLookup] Parse error: {}", e.getMessage());
        }
        
        return results;
    }
}
