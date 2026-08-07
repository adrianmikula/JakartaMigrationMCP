package adrianmikula.jakartamigration.dependencyanalysis.service.impl;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Namespace;
import adrianmikula.jakartamigration.dependencyanalysis.service.NamespaceClassifier;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple rule-based namespace classifier using artifact coordinates and known patterns.
 * Expanded with comprehensive coordinate maps derived from OpenRewrite recipe analysis.
 */
public class SimpleNamespaceClassifier implements NamespaceClassifier {

    // Known Jakarta-compatible artifacts (Jakarta EE 9+)
    // Key: groupId:artifactId, Value: minimum Jakarta version
    private static final Map<String, String> JAKARTA_ARTIFACTS = Map.ofEntries(
        // Jakarta EE Core APIs
        Map.entry("jakarta.servlet:jakarta.servlet-api", "6.0.0"),
        Map.entry("jakarta.persistence:jakarta.persistence-api", "3.1.0"),
        Map.entry("jakarta.validation:jakarta.validation-api", "3.0.0"),
        Map.entry("jakarta.annotation:jakarta.annotation-api", "2.1.0"),
        Map.entry("jakarta.transaction:jakarta.transaction-api", "2.0.0"),
        Map.entry("jakarta.ws.rs:jakarta.ws.rs-api", "3.1.0"),
        Map.entry("jakarta.ejb:jakarta.ejb-api", "4.0.0"),
        Map.entry("jakarta.enterprise:jakarta.enterprise.cdi-api", "4.0.0"),
        Map.entry("jakarta.inject:jakarta.inject-api", "2.0.1"),
        Map.entry("jakarta.json:jakarta.json-api", "2.1.0"),
        Map.entry("jakarta.mail:jakarta.mail-api", "2.1.0"),
        Map.entry("jakarta.faces:jakarta.faces-api", "4.0.0"),
        Map.entry("jakarta.jms:jakarta.jms-api", "3.1.0"),
        Map.entry("jakarta.xml.bind:jakarta.xml.bind-api", "4.0.0"),
        Map.entry("jakarta.xml.ws:jakarta.xml.ws-api", "4.0.0"),
        Map.entry("jakarta.activation:jakarta.activation-api", "2.1.0"),
        Map.entry("jakarta.xml.soap:jakarta.xml.soap-api", "3.0.0"),
        Map.entry("jakarta.security.enterprise:jakarta.security.enterprise-api", "3.0.0"),
        Map.entry("jakarta.authorization:jakarta.authorization-api", "3.0.0"),
        Map.entry("jakarta.authentication:jakarta.authentication-api", "3.0.0"),
        // Jakarta EE Web Profile
        Map.entry("jakarta.platform:jakarta.jakartaee-api", "10.0.0"),
        Map.entry("jakarta.platform:jakarta.jakartaee-web-api", "10.0.0"),
        // Hibernate 6.x (Jakarta-native)
        Map.entry("org.hibernate.orm:hibernate-core", "6.0.0")
    );

    // Known javax artifacts that need migration (Jakarta EE 8 and earlier)
    // Key: groupId:artifactId
    private static final Map<String, String> JAVAX_ARTIFACTS = Map.ofEntries(
        // Servlet
        Map.entry("javax.servlet:javax.servlet-api", "4.0.1"),
        // Persistence
        Map.entry("javax.persistence:javax.persistence-api", "2.2"),
        // Validation
        Map.entry("javax.validation:validation-api", "2.0.1"),
        // Annotation
        Map.entry("javax.annotation:javax.annotation-api", "1.3.2"),
        // JAX-RS
        Map.entry("javax.ws.rs:javax.ws.rs-api", "2.1.1"),
        // EJB
        Map.entry("javax.ejb:javax.ejb-api", "3.2"),
        // CDI
        Map.entry("javax.enterprise:cdi-api", "2.0"),
        // Inject
        Map.entry("javax.inject:javax.inject", "1"),
        // JSON Processing
        Map.entry("javax.json:javax.json-api", "1.1.4"),
        // Mail
        Map.entry("javax.mail:javax.mail-api", "1.6.2"),
        // JMS
        Map.entry("javax.jms:javax.jms-api", "2.0.1"),
        // JAXB
        Map.entry("javax.xml.bind:jaxb-api", "2.3.1"),
        // JAX-WS
        Map.entry("javax.xml.ws:jaxws-api", "2.3.1"),
        // Activation
        Map.entry("javax.activation:javax.activation-api", "1.2.0"),
        // SOAP
        Map.entry("javax.xml.soap:saaj-api", "1.4.0"),
        // Java EE platform
        Map.entry("javax javaee:javaee-api", "8.0"),
        Map.entry("org.glassfish.javaee:javaee-api", "5.0"),
        // Hibernate 5.x (old groupId, always javax)
        Map.entry("org.hibernate:hibernate-core", "5.6.1")
    );

    // Libraries that switched to Jakarta in specific versions
    // Key: groupId:artifactId, Value: "minJakartaVersion:javaxVersionCeiling"
    private static final Map<String, String> LIBRARY_VERSION_THRESHOLDS = Map.ofEntries(
        // Jersey 3.x = Jakarta EE 9+
        Map.entry("org.glassfish.jersey.core:jersey-server", "3.0.0"),
        Map.entry("org.glassfish.jersey.core:jersey-common", "3.0.0"),
        Map.entry("org.glassfish.jersey.core:jersey-client", "3.0.0"),
        Map.entry("org.glassfish.jersey.containers:jersey-container-servlet", "3.0.0"),
        Map.entry("org.glassfish.jersey.containers:jersey-container-servlet-core", "3.0.0"),
        Map.entry("org.glassfish.jersey.media:jersey-media-json-jackson", "3.0.0"),
        Map.entry("org.glassfish.jersey.inject:jersey-hk2", "3.0.0"),
        // RESTEasy 6.x = Jakarta EE 9+
        Map.entry("org.jboss.resteasy:resteasy-core", "6.0.0"),
        Map.entry("org.jboss.resteasy:resteasy-servlet-initializer", "6.0.0"),
        Map.entry("org.jboss.resteasy:resteasy-jackson2-provider", "6.0.0"),
        // Hibernate Validator 8.x = Jakarta
        Map.entry("org.hibernate.validator:hibernate-validator", "8.0.0"),
        // Hibernate 5.x = javax (old groupId, never reaches 6.x)
        // Hibernate 6.x uses org.hibernate.orm groupId (in JAKARTA_ARTIFACTS above)
        // Apache CXF 4.x = Jakarta
        Map.entry("org.apache.cxf:cxf-rt-frontend-jaxrs", "4.0.0"),
        Map.entry("org.apache.cxf:cxf-core", "4.0.0"),
        // TomEE 10.x = Jakarta
        Map.entry("org.apache.tomee:openejb-core", "10.0.0"),
        // MicroProfile
        Map.entry("org.eclipse.microprofile:microprofile", "6.0"),
        // GlassFish
        Map.entry("org.glassfish.main.web:web-core", "7.0.0")
    );

    // Spring Boot version thresholds
    private static final String SPRING_BOOT_3_MIN_VERSION = "3.0.0";

    // JDK-provided javax packages (no migration needed)
    private static final String[] JDK_JAVAX_PREFIXES = {
        "javax.management", "javax.naming", "javax.crypto", "javax.net",
        "javax.script", "javax.sql"
    };

    @Override
    public Namespace classify(Artifact artifact) {
        String identifier = artifact.toIdentifier();

        // Check known Jakarta artifacts
        if (JAKARTA_ARTIFACTS.containsKey(identifier)) {
            String minVersion = JAKARTA_ARTIFACTS.get(identifier);
            if (isVersionGreaterOrEqual(artifact.version(), minVersion)) {
                return Namespace.JAKARTA;
            }
        }

        // Check known javax artifacts
        if (JAVAX_ARTIFACTS.containsKey(identifier)) {
            return Namespace.JAVAX;
        }

        // Check library version thresholds (e.g., Jersey 3.x, RESTEasy 6.x)
        if (LIBRARY_VERSION_THRESHOLDS.containsKey(identifier)) {
            String jakartaMinVersion = LIBRARY_VERSION_THRESHOLDS.get(identifier);
            if (isVersionGreaterOrEqual(artifact.version(), jakartaMinVersion)) {
                return Namespace.JAKARTA;
            } else {
                return Namespace.JAVAX;
            }
        }

        // Check Spring Boot version
        if (identifier.startsWith("org.springframework.boot:")) {
            if (isVersionGreaterOrEqual(artifact.version(), SPRING_BOOT_3_MIN_VERSION)) {
                return Namespace.JAKARTA;
            } else {
                return Namespace.JAVAX;
            }
        }

        // Check Spring Framework version (Spring 6+ uses Jakarta)
        if (identifier.startsWith("org.springframework:spring-")) {
            if (isVersionGreaterOrEqual(artifact.version(), "6.0.0")) {
                return Namespace.JAKARTA;
            } else {
                return Namespace.JAVAX;
            }
        }

        // Check groupId patterns
        if (artifact.groupId().startsWith("jakarta.")) {
            return Namespace.JAKARTA;
        }

        if (artifact.groupId().startsWith("javax.") && !isJdkProvidedPackage(artifact.groupId())) {
            return Namespace.JAVAX;
        }

        // Default to unknown
        return Namespace.UNKNOWN;
    }

    @Override
    public Map<Artifact, Namespace> classifyAll(Collection<Artifact> artifacts) {
        Map<Artifact, Namespace> result = new HashMap<>();
        for (Artifact artifact : artifacts) {
            result.put(artifact, classify(artifact));
        }
        return result;
    }

    private boolean isJdkProvidedPackage(String groupId) {
        for (String prefix : JDK_JAVAX_PREFIXES) {
            if (groupId.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Simple version comparison. For production, use a proper version comparator.
     * This is a simplified implementation for MVP.
     */
    private boolean isVersionGreaterOrEqual(String version1, String version2) {
        try {
            String[] v1Parts = version1.split("\\.");
            String[] v2Parts = version2.split("\\.");

            int maxLength = Math.max(v1Parts.length, v2Parts.length);

            for (int i = 0; i < maxLength; i++) {
                int v1Part = i < v1Parts.length ? parseVersionPart(v1Parts[i]) : 0;
                int v2Part = i < v2Parts.length ? parseVersionPart(v2Parts[i]) : 0;

                if (v1Part > v2Part) {
                    return true;
                } else if (v1Part < v2Part) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            return version1.compareTo(version2) >= 0;
        }
    }

    private int parseVersionPart(String part) {
        String numericPart = part.split("-")[0];
        try {
            return Integer.parseInt(numericPart);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
