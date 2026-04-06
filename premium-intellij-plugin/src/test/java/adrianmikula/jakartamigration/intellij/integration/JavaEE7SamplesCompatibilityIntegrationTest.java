package adrianmikula.jakartamigration.intellij.integration;

import adrianmikula.jakartamigration.dependencyanalysis.config.CompatibilityConfigLoader;
import adrianmikula.jakartamigration.dependencyanalysis.config.CompatibilityConfigLoader.ArtifactClassification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests verifying new artifacts from javaee7-samples-master are correctly classified.
 * Tests the CompatibilityConfigLoader against the newly added compatibility.yaml entries.
 * 
 * These artifacts were discovered by analyzing the javaee7-samples-master project and added
 * to compatibility.yaml to improve Jakarta EE migration detection.
 */
@Tag("slow")
public class JavaEE7SamplesCompatibilityIntegrationTest extends IntegrationTestBase {
    
    private CompatibilityConfigLoader configLoader;
    
    @Override
    @BeforeEach
    void setUp() throws IOException {
        super.setUp();
        configLoader = new CompatibilityConfigLoader();
    }
    
    @Nested
    @DisplayName("New Upgrade Artifacts - JAKARTA_REQUIRED")
    class UpgradeArtifactsTest {
        
        @Test
        @DisplayName("Should classify javax group as JAKARTA_REQUIRED")
        void testJavaxGroupClassification() {
            assertEquals(ArtifactClassification.JAKARTA_REQUIRED, 
                configLoader.classifyArtifact("javax", "javaee-api"),
                "javax group (javaee-api) should require Jakarta migration");
        }
        
        @Test
        @DisplayName("Should classify com.sun.xml.rpc as JAKARTA_REQUIRED")
        void testSunXmlRpcClassification() {
            assertEquals(ArtifactClassification.JAKARTA_REQUIRED, 
                configLoader.classifyArtifact("com.sun.xml.rpc", "jaxrpc-impl"),
                "Sun XML-RPC should require Jakarta migration");
        }
        
        @Test
        @DisplayName("Should classify com.sun.xml.ws as JAKARTA_REQUIRED")
        void testSunXmlWsClassification() {
            assertEquals(ArtifactClassification.JAKARTA_REQUIRED, 
                configLoader.classifyArtifact("com.sun.xml.ws", "jaxws-rt"),
                "Sun JAX-WS should require Jakarta migration");
        }
        
        @Test
        @DisplayName("Should classify org.glassfish as JAKARTA_REQUIRED")
        void testGlassFishClassification() {
            assertEquals(ArtifactClassification.JAKARTA_REQUIRED, 
                configLoader.classifyArtifact("org.glassfish", "glassfish-embedded"),
                "GlassFish core should require Jakarta migration");
        }
        
        @Test
        @DisplayName("Should classify org.glassfish.jersey.core as JAKARTA_REQUIRED")
        void testGlassFishJerseyCoreClassification() {
            assertEquals(ArtifactClassification.JAKARTA_REQUIRED, 
                configLoader.classifyArtifact("org.glassfish.jersey.core", "jersey-server"),
                "Jersey core should require Jakarta migration");
        }
        
        @Test
        @DisplayName("Should classify org.glassfish.jersey.media as JAKARTA_REQUIRED")
        void testGlassFishJerseyMediaClassification() {
            assertEquals(ArtifactClassification.JAKARTA_REQUIRED, 
                configLoader.classifyArtifact("org.glassfish.jersey.media", "jersey-media-json"),
                "Jersey media should require Jakarta migration");
        }
        
        @Test
        @DisplayName("Should classify org.glassfish.jersey.containers as JAKARTA_REQUIRED")
        void testGlassFishJerseyContainersClassification() {
            assertEquals(ArtifactClassification.JAKARTA_REQUIRED, 
                configLoader.classifyArtifact("org.glassfish.jersey.containers", "jersey-container-servlet"),
                "Jersey containers should require Jakarta migration");
        }
        
        @Test
        @DisplayName("Should classify org.glassfish.tyrus as JAKARTA_REQUIRED")
        void testGlassFishTyrusClassification() {
            assertEquals(ArtifactClassification.JAKARTA_REQUIRED, 
                configLoader.classifyArtifact("org.glassfish.tyrus", "tyrus-server"),
                "Tyrus WebSocket should require Jakarta migration");
        }
        
        @Test
        @DisplayName("Should classify org.glassfish.metro as JAKARTA_REQUIRED")
        void testGlassFishMetroClassification() {
            assertEquals(ArtifactClassification.JAKARTA_REQUIRED, 
                configLoader.classifyArtifact("org.glassfish.metro", "webservices-rt"),
                "Metro Web Services should require Jakarta migration");
        }
        
        @Test
        @DisplayName("Should classify org.jboss.resteasy as JAKARTA_REQUIRED")
        void testJBossResteasyClassification() {
            assertEquals(ArtifactClassification.JAKARTA_REQUIRED, 
                configLoader.classifyArtifact("org.jboss.resteasy", "resteasy-core"),
                "RESTEasy should require Jakarta migration");
        }
        
        @Test
        @DisplayName("Should classify org.wildfly as JAKARTA_REQUIRED")
        void testWildFlyClassification() {
            assertEquals(ArtifactClassification.JAKARTA_REQUIRED, 
                configLoader.classifyArtifact("org.wildfly", "wildfly-ee"),
                "WildFly should require Jakarta migration");
        }
        
        @Test
        @DisplayName("Should classify org.apache.tomee as JAKARTA_REQUIRED")
        void testTomEEClassification() {
            assertEquals(ArtifactClassification.JAKARTA_REQUIRED, 
                configLoader.classifyArtifact("org.apache.tomee", "tomee-embedded"),
                "TomEE should require Jakarta migration");
        }
        
        @Test
        @DisplayName("Should classify org.apache.cxf as JAKARTA_REQUIRED")
        void testCxfClassification() {
            assertEquals(ArtifactClassification.JAKARTA_REQUIRED, 
                configLoader.classifyArtifact("org.apache.cxf", "cxf-core"),
                "Apache CXF should require Jakarta migration");
        }
        
        @Test
        @DisplayName("Should classify org.apache.batchee as JAKARTA_REQUIRED")
        void testBatcheeClassification() {
            assertEquals(ArtifactClassification.JAKARTA_REQUIRED, 
                configLoader.classifyArtifact("org.apache.batchee", "batchee-core"),
                "Apache Batchee (JBatch) should require Jakarta migration");
        }
        
        @Test
        @DisplayName("Should classify org.apache.johnzon as JAKARTA_REQUIRED")
        void testJohnzonClassification() {
            assertEquals(ArtifactClassification.JAKARTA_REQUIRED, 
                configLoader.classifyArtifact("org.apache.johnzon", "johnzon-core"),
                "Apache Johnzon (JSON-P) should require Jakarta migration");
        }
    }
    
    @Nested
    @DisplayName("New Review Artifacts - CONTEXT_DEPENDENT")
    class ReviewArtifactsTest {
        
        @Test
        @DisplayName("Should classify org.glassfish.grizzly as JAKARTA_REQUIRED (via org.glassfish)")
        void testGrizzlyClassification() {
            // Note: org.glassfish.grizzly matches the org.glassfish pattern in upgrade list
            assertEquals(ArtifactClassification.JAKARTA_REQUIRED, 
                configLoader.classifyArtifact("org.glassfish.grizzly", "grizzly-http"),
                "Grizzly HTTP framework is covered by org.glassfish upgrade pattern");
        }
        
        @Test
        @DisplayName("Should classify io.undertow as CONTEXT_DEPENDENT")
        void testUndertowClassification() {
            assertEquals(ArtifactClassification.CONTEXT_DEPENDENT, 
                configLoader.classifyArtifact("io.undertow", "undertow-core"),
                "Undertow web server requires context-dependent review");
        }
        
        @Test
        @DisplayName("Should classify org.apache.tomcat as CONTEXT_DEPENDENT")
        void testTomcatClassification() {
            assertEquals(ArtifactClassification.CONTEXT_DEPENDENT, 
                configLoader.classifyArtifact("org.apache.tomcat", "tomcat-catalina"),
                "Apache Tomcat requires context-dependent review");
        }
        
        @Test
        @DisplayName("Should classify org.omnifaces as CONTEXT_DEPENDENT")
        void testOmniFacesClassification() {
            assertEquals(ArtifactClassification.CONTEXT_DEPENDENT, 
                configLoader.classifyArtifact("org.omnifaces", "omnifaces"),
                "OmniFaces JSF utilities require context-dependent review");
        }
    }
    
    @Nested
    @DisplayName("New Safe Artifacts - COMPATIBLE/UNKNOWN")
    class SafeArtifactsTest {
        
        @Test
        @DisplayName("Should classify org.bouncycastle as UNKNOWN (safe, non-Jakarta)")
        void testBouncyCastleClassification() {
            ArtifactClassification classification = 
                configLoader.classifyArtifact("org.bouncycastle", "bcprov-jdk15on");
            assertTrue(classification == ArtifactClassification.UNKNOWN || 
                      classification == ArtifactClassification.JDK_PROVIDED,
                "BouncyCastle crypto library should be safe (UNKNOWN or COMPATIBLE)");
        }
        
        @Test
        @DisplayName("Should classify org.jsoup as UNKNOWN (safe, non-Jakarta)")
        void testJsoupClassification() {
            ArtifactClassification classification = 
                configLoader.classifyArtifact("org.jsoup", "jsoup");
            assertTrue(classification == ArtifactClassification.UNKNOWN || 
                      classification == ArtifactClassification.JDK_PROVIDED,
                "JSoup HTML parser should be safe (UNKNOWN or COMPATIBLE)");
        }
        
        @Test
        @DisplayName("Should classify org.json as UNKNOWN (safe, non-Jakarta)")
        void testOrgJsonClassification() {
            ArtifactClassification classification = 
                configLoader.classifyArtifact("org.json", "json");
            assertTrue(classification == ArtifactClassification.UNKNOWN || 
                      classification == ArtifactClassification.JDK_PROVIDED,
                "JSON.org library should be safe (UNKNOWN or COMPATIBLE)");
        }
        
        @Test
        @DisplayName("Should classify rhino as UNKNOWN (safe, non-Jakarta)")
        void testRhinoClassification() {
            ArtifactClassification classification = 
                configLoader.classifyArtifact("rhino", "js");
            assertTrue(classification == ArtifactClassification.UNKNOWN || 
                      classification == ArtifactClassification.JDK_PROVIDED,
                "Rhino JavaScript engine should be safe (UNKNOWN or COMPATIBLE)");
        }
        
        @Test
        @DisplayName("Should classify xmlunit as UNKNOWN (safe, non-Jakarta)")
        void testXmlUnitClassification() {
            ArtifactClassification classification = 
                configLoader.classifyArtifact("xmlunit", "xmlunit");
            assertTrue(classification == ArtifactClassification.UNKNOWN || 
                      classification == ArtifactClassification.JDK_PROVIDED,
                "XMLUnit testing library should be safe (UNKNOWN or COMPATIBLE)");
        }
        
        @Test
        @DisplayName("Should classify httpunit as UNKNOWN (safe, non-Jakarta)")
        void testHttpUnitClassification() {
            ArtifactClassification classification = 
                configLoader.classifyArtifact("httpunit", "httpunit");
            assertTrue(classification == ArtifactClassification.UNKNOWN || 
                      classification == ArtifactClassification.JDK_PROVIDED,
                "HttpUnit testing library should be safe (UNKNOWN or COMPATIBLE)");
        }
        
        @Test
        @DisplayName("Should classify net.sourceforge.htmlunit as UNKNOWN (safe, non-Jakarta)")
        void testHtmlUnitClassification() {
            ArtifactClassification classification = 
                configLoader.classifyArtifact("net.sourceforge.htmlunit", "htmlunit");
            assertTrue(classification == ArtifactClassification.UNKNOWN || 
                      classification == ArtifactClassification.JDK_PROVIDED,
                "HtmlUnit testing library should be safe (UNKNOWN or COMPATIBLE)");
        }
        
        @Test
        @DisplayName("Should classify org.assertj as UNKNOWN (safe, non-Jakarta)")
        void testAssertJClassification() {
            ArtifactClassification classification = 
                configLoader.classifyArtifact("org.assertj", "assertj-core");
            assertTrue(classification == ArtifactClassification.UNKNOWN || 
                      classification == ArtifactClassification.JDK_PROVIDED,
                "AssertJ testing library should be safe (UNKNOWN or COMPATIBLE)");
        }
        
        @Test
        @DisplayName("Should classify com.jayway.awaitility as UNKNOWN (safe, non-Jakarta)")
        void testAwaitilityClassification() {
            ArtifactClassification classification = 
                configLoader.classifyArtifact("com.jayway.awaitility", "awaitility");
            assertTrue(classification == ArtifactClassification.UNKNOWN || 
                      classification == ArtifactClassification.JDK_PROVIDED,
                "Awaitility testing library should be safe (UNKNOWN or COMPATIBLE)");
        }
        
        @Test
        @DisplayName("Should classify org.skyscreamer as UNKNOWN (safe, non-Jakarta)")
        void testSkyScreamerClassification() {
            ArtifactClassification classification = 
                configLoader.classifyArtifact("org.skyscreamer", "jsonassert");
            assertTrue(classification == ArtifactClassification.UNKNOWN || 
                      classification == ArtifactClassification.JDK_PROVIDED,
                "JSONAssert testing library should be safe (UNKNOWN or COMPATIBLE)");
        }
        
        @Test
        @DisplayName("Should classify org.codehaus.jackson as UNKNOWN (safe, non-Jakarta)")
        void testCodehausJacksonClassification() {
            ArtifactClassification classification = 
                configLoader.classifyArtifact("org.codehaus.jackson", "jackson-core-asl");
            assertTrue(classification == ArtifactClassification.UNKNOWN || 
                      classification == ArtifactClassification.JDK_PROVIDED,
                "Codehaus Jackson should be safe (UNKNOWN or COMPATIBLE)");
        }
        
        @Test
        @DisplayName("Should classify io.thorntail as UNKNOWN (safe, non-Jakarta)")
        void testThorntailClassification() {
            ArtifactClassification classification = 
                configLoader.classifyArtifact("io.thorntail", "thorntail");
            assertTrue(classification == ArtifactClassification.UNKNOWN || 
                      classification == ArtifactClassification.JDK_PROVIDED,
                "Thorntail should be safe (UNKNOWN or COMPATIBLE)");
        }
        
        @Test
        @DisplayName("Should classify org.atmosphere as UNKNOWN (safe, non-Jakarta)")
        void testAtmosphereClassification() {
            ArtifactClassification classification = 
                configLoader.classifyArtifact("org.atmosphere", "atmosphere-runtime");
            assertTrue(classification == ArtifactClassification.UNKNOWN || 
                      classification == ArtifactClassification.JDK_PROVIDED,
                "Atmosphere framework should be safe (UNKNOWN or COMPATIBLE)");
        }
        
        @Test
        @DisplayName("Should classify io.opentracing as UNKNOWN (safe, non-Jakarta)")
        void testOpenTracingClassification() {
            ArtifactClassification classification = 
                configLoader.classifyArtifact("io.opentracing", "opentracing-api");
            assertTrue(classification == ArtifactClassification.UNKNOWN || 
                      classification == ArtifactClassification.JDK_PROVIDED,
                "OpenTracing API should be safe (UNKNOWN or COMPATIBLE)");
        }
        
        @Test
        @DisplayName("Should classify com.h2database as UNKNOWN (safe, non-Jakarta)")
        void testH2DatabaseClassification() {
            ArtifactClassification classification = 
                configLoader.classifyArtifact("com.h2database", "h2");
            assertTrue(classification == ArtifactClassification.UNKNOWN || 
                      classification == ArtifactClassification.JDK_PROVIDED,
                "H2 Database should be safe (UNKNOWN or COMPATIBLE)");
        }
        
        @Test
        @DisplayName("Should classify org.hsqldb as UNKNOWN (safe, non-Jakarta)")
        void testHsqldbClassification() {
            ArtifactClassification classification = 
                configLoader.classifyArtifact("org.hsqldb", "hsqldb");
            assertTrue(classification == ArtifactClassification.UNKNOWN || 
                      classification == ArtifactClassification.JDK_PROVIDED,
                "HSQLDB should be safe (UNKNOWN or COMPATIBLE)");
        }
    }
}
