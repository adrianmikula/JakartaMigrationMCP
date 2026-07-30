package adrianmikula.jakartamigration.dependencyanalysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.net.URLEncoder;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simplified Maven Central lookup service with fuzzy matching capabilities.
 * This is a lightweight version of the MavenCentralService for the community module.
 */
@Slf4j
public class ImprovedMavenCentralLookupService {
    
    private static final String MAVEN_CENTRAL_API = "https://search.maven.org/solrsearch/select";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    // HTTP client is instance field to allow mocking in tests
    private HttpClient httpClient;
    
    // TTL for cached Maven Central lookup results; configurable via env var or system property (minutes)
    private static final Duration LOOKUP_CACHE_TTL = Duration.ofMinutes(
            getConfigLong("JAKARTA_MAVEN_CACHE_TTL_MINUTES", "jakarta.maven.cache.ttl.minutes", 30L));

    // Per-request timeout for Maven Central API calls; configurable via env var or system property (seconds)
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(
            getConfigLong("JAKARTA_MAVEN_LOOKUP_TIMEOUT_SECONDS", "jakarta.maven.lookup.timeout.seconds", 30L));

    // HTTP connect timeout for Maven Central API calls; configurable via env var or system property (seconds)
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(
            getConfigLong("JAKARTA_MAVEN_CONNECT_TIMEOUT_SECONDS", "jakarta.maven.connect.timeout.seconds", 10L));

    private record CacheEntry(List<JakartaArtifactMatch> results, Instant cachedAt) {}

    // Session-scoped cache: groupId:artifactId → results (includes misses)
    private final ConcurrentHashMap<String, CacheEntry> lookupCache = new ConcurrentHashMap<>();

    // Dedicated worker pool for blocking Maven Central HTTP calls so we don't exhaust the ForkJoinPool common pool
    private static final AtomicInteger LOOKUP_THREAD_COUNTER = new AtomicInteger(0);
    private static final ExecutorService LOOKUP_WORKER = Executors.newFixedThreadPool(
            getConfigInt("JAKARTA_MAVEN_LOOKUP_THREADS", "jakarta.maven.lookup.threads", 4),
            r -> {
                Thread t = new Thread(r, "maven-lookup-worker-" + LOOKUP_THREAD_COUNTER.incrementAndGet());
                t.setDaemon(true);
                return t;
            });

    // OpenRewrite package renames used when coordinate searches fail for unknown libraries.
    private final Map<String, String> packageRenameMap;

    public ImprovedMavenCentralLookupService() {
        this(HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build(), Map.of());
    }

    public ImprovedMavenCentralLookupService(Map<String, String> packageRenameMap) {
        this(HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build(), packageRenameMap);
    }

    // Package-private constructor for testing with mocked HTTP client
    ImprovedMavenCentralLookupService(HttpClient httpClient) {
        this(httpClient, Map.of());
    }

    ImprovedMavenCentralLookupService(HttpClient httpClient, Map<String, String> packageRenameMap) {
        this.httpClient = httpClient;
        this.packageRenameMap = packageRenameMap != null ? packageRenameMap : Map.of();
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
        ARTIFACT_MAPPINGS.put("jaxb-api", "jakarta.xml.bind-api");
        ARTIFACT_MAPPINGS.put("jaxws-api", "jakarta.xml.ws-api");
        ARTIFACT_MAPPINGS.put("javax.json.bind-api", "jakarta.json.bind-api");

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
        GROUP_MAPPINGS.put("javax.json.bind", "jakarta.json.bind");
        GROUP_MAPPINGS.put("javax.xml.soap", "jakarta.xml.soap");

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
            boolean found,
            double confidence
    ) {
        public static JakartaArtifactMatch notFound() {
            return new JakartaArtifactMatch(null, null, null, false, 0.0);
        }
        
        public static JakartaArtifactMatch of(String groupId, String artifactId, String version) {
            return new JakartaArtifactMatch(groupId, artifactId, version, true, 1.0);
        }

        public static JakartaArtifactMatch of(String groupId, String artifactId, String version, double confidence) {
            double clamped = Math.max(0.0, Math.min(1.0, confidence));
            return new JakartaArtifactMatch(groupId, artifactId, version, true, clamped);
        }
    }
    
    /**
     * Finds Jakarta equivalent artifacts with fuzzy matching strategies.
     * Uses session-scoped cache and static-mapping fast path to minimize network calls.
     */
    public CompletableFuture<List<JakartaArtifactMatch>> findJakartaEquivalents(
            String javaxGroupId, 
            String javaxArtifactId) {
        
        log.debug("Searching for Jakarta equivalents for {}:{}", javaxGroupId, javaxArtifactId);

        // Input validation
        if (javaxGroupId == null || javaxGroupId.trim().isEmpty() || 
            javaxArtifactId == null || javaxArtifactId.trim().isEmpty()) {
            log.warn("Invalid coordinates provided: groupId='{}', artifactId='{}'", javaxGroupId, javaxArtifactId);
            return CompletableFuture.completedFuture(List.of());
        }
        
        // Normalize to lowercase for case-insensitive matching
        String normalizedGroupId = javaxGroupId.toLowerCase();
        String normalizedArtifactId = javaxArtifactId.toLowerCase();
        String cacheKey = normalizedGroupId + ":" + normalizedArtifactId;
        
        // T2: Session-scoped cache check
        List<JakartaArtifactMatch> cached = getCached(cacheKey);
        if (cached != null) {
            log.debug("Cache hit for {}:{}", normalizedGroupId, normalizedArtifactId);
            return CompletableFuture.completedFuture(cached);
        }
        
        // T3: Static-mapping fast path — check ARTIFACT_MAPPINGS and GROUP_MAPPINGS before network calls
        List<JakartaArtifactMatch> fastPathResult = tryStaticMappingFastPath(normalizedGroupId, normalizedArtifactId);
        if (fastPathResult != null) {
            putCache(cacheKey, fastPathResult);
            log.info("Static mapping fast path found {} results for {}:{}", fastPathResult.size(), normalizedGroupId, normalizedArtifactId);
            return CompletableFuture.completedFuture(fastPathResult);
        }
        
        // Skip network lookup for coordinates that are not plausible Jakarta candidates
        if (!isJakartaCandidate(normalizedGroupId, normalizedArtifactId)) {
            return CompletableFuture.completedFuture(List.of());
        }
        
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
            // Prefer Jakarta-namespace matches and drop the original javax coordinate
            String originalKey = normalizedGroupId + ":" + normalizedArtifactId;
            List<JakartaArtifactMatch> uniqueResults = allResults.stream()
                    .distinct()
                    .filter(match -> match.groupId() != null && match.artifactId() != null)
                    .filter(match -> {
                        String key = match.groupId() + ":" + match.artifactId();
                        return !(key.equals(originalKey) && normalizedGroupId.startsWith("javax."));
                    })
                    .sorted((a, b) -> Integer.compare(jakartaScore(b), jakartaScore(a)))
                    .limit(5) // Limit to top 5 results
                    .toList();
            
            // T2: Cache both hits and misses
            putCache(cacheKey, uniqueResults);
            
            log.info("Found {} unique Jakarta artifacts for {}:{}", uniqueResults.size(), javaxGroupId, javaxArtifactId);
            return uniqueResults;
        }, LOOKUP_WORKER);
    }
    
    /**
     * Finds Jakarta equivalents, optionally using detected javax package names to drive
     * the search via OpenRewrite package rename mappings.
     */
    public CompletableFuture<List<JakartaArtifactMatch>> findJakartaEquivalents(
            String javaxGroupId,
            String javaxArtifactId,
            java.util.Collection<String> javaxPackages) {

        return CompletableFuture.supplyAsync(() -> {
            List<JakartaArtifactMatch> coordinateMatches = findJakartaEquivalents(javaxGroupId, javaxArtifactId).join();
            if (!coordinateMatches.isEmpty()) {
                return coordinateMatches;
            }
            if (javaxPackages == null || javaxPackages.isEmpty() || packageRenameMap.isEmpty()) {
                return List.of();
            }
            List<JakartaArtifactMatch> packageMatches = searchByPackageRename(javaxPackages);
            if (!packageMatches.isEmpty()) {
                log.info("Package-rename search found {} candidate(s) for {}:{}",
                    packageMatches.size(), javaxGroupId, javaxArtifactId);
            }
            return packageMatches;
        }, LOOKUP_WORKER);
    }

    private List<JakartaArtifactMatch> searchByPackageRename(java.util.Collection<String> javaxPackages) {
        List<JakartaArtifactMatch> all = new ArrayList<>();
        for (String javaxPackage : javaxPackages) {
            String jakartaPackage = packageRenameMap.get(javaxPackage);
            if (jakartaPackage != null && !jakartaPackage.isEmpty()) {
                List<JakartaArtifactMatch> candidates = performSearchByGroup(jakartaPackage);
                for (JakartaArtifactMatch candidate : candidates) {
                    double confidence = confidenceForPackageRename(jakartaPackage, candidate);
                    all.add(JakartaArtifactMatch.of(
                            candidate.groupId(), candidate.artifactId(), candidate.version(), confidence));
                }
            }
        }
        return all.stream()
                .distinct()
                .filter(match -> match.groupId() != null && match.artifactId() != null)
                .sorted((a, b) -> Integer.compare(jakartaScore(b), jakartaScore(a)))
                .limit(5)
                .toList();
    }

    /**
     * Scores how confident we are that a Maven Central candidate is the Jakarta equivalent
     * of a package detected in the bytecode. Higher when the candidate's groupId exactly matches
     * the expected jakarta package prefix and when the artifactId contains the package's tail segment.
     */
    private double confidenceForPackageRename(String expectedJakartaPackage, JakartaArtifactMatch match) {
        double confidence = 0.5;
        String groupId = match.groupId();
        String artifactId = match.artifactId();
        if (expectedJakartaPackage.equals(groupId)) {
            confidence += 0.4;
        } else if (groupId != null && groupId.startsWith(expectedJakartaPackage + ".")) {
            confidence += 0.2;
        }
        if (artifactId != null) {
            int lastDot = expectedJakartaPackage.lastIndexOf('.');
            String tail = lastDot >= 0 ? expectedJakartaPackage.substring(lastDot + 1) : expectedJakartaPackage;
            if (artifactId.contains(tail)) {
                confidence += 0.1;
            }
        }
        return Math.min(1.0, confidence);
    }

    private List<JakartaArtifactMatch> performSearchByGroup(String groupId) {
        return performSearchWithEndpoint(MAVEN_CENTRAL_API, groupId, null);
    }

    /**
     * Returns a cached result only if it has not exceeded the configured TTL; stale entries are removed.
     */
    private List<JakartaArtifactMatch> getCached(String key) {
        CacheEntry entry = lookupCache.get(key);
        if (entry == null) {
            return null;
        }
        if (Duration.between(entry.cachedAt(), Instant.now()).compareTo(LOOKUP_CACHE_TTL) > 0) {
            lookupCache.remove(key, entry);
            return null;
        }
        return entry.results();
    }

    /**
     * Stores a lookup result with the current timestamp.
     */
    private void putCache(String key, List<JakartaArtifactMatch> results) {
        lookupCache.put(key, new CacheEntry(results, Instant.now()));
    }

    /**
     * Scores a match by how strongly it looks like a Jakarta artifact.
     */
    private static boolean isJakartaCandidate(String groupId, String artifactId) {
        if (groupId.startsWith("javax.") || groupId.startsWith("jakarta.")
                || groupId.contains("jakarta") || groupId.contains("javax") || groupId.contains("ee4j")) {
            return true;
        }
        if (ARTIFACT_MAPPINGS.containsKey(artifactId) || GROUP_MAPPINGS.containsKey(groupId)) {
            return true;
        }
        for (String prefix : GROUP_MAPPINGS.keySet()) {
            if (groupId.equals(prefix) || groupId.startsWith(prefix + ".")) {
                return true;
            }
        }
        String lowerArtifact = artifactId.toLowerCase();
        return lowerArtifact.startsWith("javax.") || lowerArtifact.startsWith("jakarta.")
                || lowerArtifact.contains("jakarta") || lowerArtifact.contains("javax");
    }

    private static int jakartaScore(JakartaArtifactMatch match) {
        int score = 0;
        if (match.groupId() != null) {
            if (match.groupId().startsWith("jakarta.")) score += 20;
            if (match.groupId().contains("jakarta")) score += 5;
        }
        if (match.artifactId() != null) {
            if (match.artifactId().startsWith("jakarta.")) score += 10;
            if (match.artifactId().contains("jakarta")) score += 3;
        }
        return score;
    }

    /**
     * T3: Static-mapping fast path — returns known Jakarta equivalents without network calls,
     * or null if no static mapping exists.
     */
    private List<JakartaArtifactMatch> tryStaticMappingFastPath(String groupId, String artifactId) {
        String mappedGroupId = groupId;
        String mappedArtifactId = artifactId;

        // Check artifact name mapping (e.g., javax.servlet-api → jakarta.servlet-api)
        String artifactMapping = ARTIFACT_MAPPINGS.get(artifactId);
        if (artifactMapping != null) {
            mappedArtifactId = artifactMapping;
            if (groupId.startsWith("javax.")) {
                mappedGroupId = "jakarta." + groupId.substring("javax.".length());
            }
            // A group mapping may override the derived jakarta group (e.g. com.sun.jersey)
            String groupMapping = GROUP_MAPPINGS.get(groupId);
            if (groupMapping != null) {
                mappedGroupId = groupMapping;
            }
        } else {
            // Check group name mapping (e.g., javax.servlet → jakarta.servlet)
            String groupMapping = GROUP_MAPPINGS.get(groupId);
            if (groupMapping == null) {
                return null;
            }
            mappedGroupId = groupMapping;
        }

        // Trivial self-mapping (e.g. org.apache.cxf → org.apache.cxf): don't duplicate
        // the exact-match network call; return the mapped coordinate straight away.
        if (mappedGroupId.equals(groupId) && mappedArtifactId.equals(artifactId)) {
            return List.of(JakartaArtifactMatch.of(mappedGroupId, mappedArtifactId, null));
        }

        // Hydrate the static mapping with a live Maven Central lookup so the result
        // includes the real latestVersion. Fall back to the mapped coordinate with a
        // null version if Maven Central is unreachable or has no match.
        List<JakartaArtifactMatch> liveResults = performMavenCentralSearch(mappedGroupId, mappedArtifactId);
        if (!liveResults.isEmpty()) {
            return liveResults;
        }
        return List.of(JakartaArtifactMatch.of(mappedGroupId, mappedArtifactId, null));
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
     * Performs the actual Maven Central search
     */
    private List<JakartaArtifactMatch> performMavenCentralSearch(String groupId, String artifactId) {
        return performSearchWithEndpoint(MAVEN_CENTRAL_API, groupId, artifactId);
    }
    
    /**
     * Performs search with a specific endpoint
     */
    private List<JakartaArtifactMatch> performSearchWithEndpoint(String endpoint, String groupId, String artifactId) {
        try {
            String searchQuery = (artifactId != null && !artifactId.isEmpty())
                    ? "g:" + groupId + " AND a:" + artifactId
                    : "g:" + groupId;
            int rows = (artifactId != null && !artifactId.isEmpty()) ? 5 : 20;
            String url = endpoint + "?q=" + URLEncoder.encode(searchQuery, "UTF-8") + "&rows=" + rows + "&wt=json";
            
            log.debug("[MavenLookup] Querying: {}", url);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .GET()
                    .header("User-Agent", "Jakarta-Migration-MCP/1.0")
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
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
        } catch (HttpTimeoutException e) {
            log.warn("Request timed out querying Maven Central endpoint {} for {}:{}", endpoint, groupId, artifactId);
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
                    if (version.isEmpty()) {
                        version = docNode.path("v").asText();
                    }
                    
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

    private static long getConfigLong(String envKey, String sysKey, long defaultValue) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isEmpty()) {
            try {
                return Long.parseLong(envValue);
            } catch (NumberFormatException ignored) {
                // fall through to system property or default
            }
        }
        return Long.getLong(sysKey, defaultValue);
    }

    private static int getConfigInt(String envKey, String sysKey, int defaultValue) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isEmpty()) {
            try {
                return Integer.parseInt(envValue);
            } catch (NumberFormatException ignored) {
                // fall through to system property or default
            }
        }
        return Integer.getInteger(sysKey, defaultValue);
    }
}
