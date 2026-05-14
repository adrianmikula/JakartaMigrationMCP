package adrianmikula.jakartamigration.jaranalysis.service;

import adrianmikula.jakartamigration.jaranalysis.domain.JarScanSignal;
import adrianmikula.jakartamigration.testutil.TestJarBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.*;

class MetadataSignalExtractorTest {

    private final MetadataSignalExtractor extractor = new MetadataSignalExtractor();

    @TempDir
    Path tempDir;

    @Test
    void detectPomXmlInMetaInfMaven() throws IOException {
        Path jar = tempDir.resolve("with-pom.jar");
        String pom = """
                     <?xml version="1.0" encoding="UTF-8"?>
                     <project>
                       <modelVersion>4.0.0</modelVersion>
                       <groupId>test</groupId>
                       <artifactId>test</artifactId>
                     </project>
                     """;
        TestJarBuilder.create()
            .withPomXml(pom)
            .build(jar);

        var signal = extractor.enhanceSignal(jar, newBaseSignal());

        assertThat(signal.hasPomMetadata()).isTrue();
    }

    @Test
    void detectPomPropertiesInMetaInfMaven() throws IOException {
        Path jar = tempDir.resolve("with-pom-props.jar");
        TestJarBuilder.create()
            .withPomProperties("groupId=test\nartifactId=test")
            .build(jar);

        var signal = extractor.enhanceSignal(jar, newBaseSignal());

        assertThat(signal.hasPomMetadata()).isTrue();
    }

    @Test
    void parsePomXmlIndicatesJavax() throws IOException {
        Path jar = tempDir.resolve("pom-javax.jar");
        String pom = """
                     <?xml version="1.0"?>
                     <project>
                       <dependencies>
                         <dependency>
                           <groupId>javax.servlet</groupId>
                           <artifactId>javax.servlet-api</artifactId>
                           <version>4.0.1</version>
                         </dependency>
                       </dependencies>
                     </project>
                     """;
        TestJarBuilder.create()
            .withPomXml(pom)
            .build(jar);

        var signal = extractor.enhanceSignal(jar, newBaseSignal());

        assertThat(signal.pomIndicatesJavax()).isTrue();
        assertThat(signal.pomIndicatesJakarta()).isFalse();
    }

    @Test
    void parsePomXmlIndicatesJakarta() throws IOException {
        Path jar = tempDir.resolve("pom-jakarta.jar");
        String pom = """
                     <?xml version="1.0"?>
                     <project>
                       <dependencies>
                         <dependency>
                           <groupId>jakarta.servlet</groupId>
                           <artifactId>jakarta.servlet-api</artifactId>
                           <version>5.0.0</version>
                         </dependency>
                       </dependencies>
                     </project>
                     """;
        TestJarBuilder.create()
            .withPomXml(pom)
            .build(jar);

        var signal = extractor.enhanceSignal(jar, newBaseSignal());

        assertThat(signal.pomIndicatesJakarta()).isTrue();
        assertThat(signal.pomIndicatesJavax()).isFalse();
    }

    @Test
    void parsePomXmlWithBothNamespaces() throws IOException {
        Path jar = tempDir.resolve("pom-both.jar");
        String pom = """
                     <?xml version="1.0"?>
                     <project>
                       <dependencies>
                         <dependency>
                           <groupId>javax.servlet</groupId>
                           <artifactId>javax.servlet-api</artifactId>
                           <version>4.0.1</version>
                         </dependency>
                         <dependency>
                           <groupId>jakarta.servlet</groupId>
                           <artifactId>jakarta.servlet-api</artifactId>
                           <version>5.0.0</version>
                         </dependency>
                       </dependencies>
                     </project>
                     """;
        TestJarBuilder.create()
            .withPomXml(pom)
            .build(jar);

        var signal = extractor.enhanceSignal(jar, newBaseSignal());

        assertThat(signal.pomIndicatesJavax()).isTrue();
        assertThat(signal.pomIndicatesJakarta()).isTrue();
    }

    @Test
    void parsePomXmlCriticalJakartaApis() throws IOException {
        Path jar = tempDir.resolve("pom-jakarta-critical.jar");
        String pom = """
                     <?xml version="1.0"?>
                     <project>
                       <dependencies>
                         <dependency>
                           <groupId>jakarta.xml.bind</groupId>
                           <artifactId>jakarta.xml.bind-api</artifactId>
                           <version>4.0.0</version>
                         </dependency>
                       </dependencies>
                     </project>
                     """;
        TestJarBuilder.create()
            .withPomXml(pom)
            .build(jar);

        var signal = extractor.enhanceSignal(jar, newBaseSignal());

        assertThat(signal.pomIndicatesJakarta()).isTrue();
    }

    @Test
    void parsePomXmlCriticalJavaxApis() throws IOException {
        Path jar = tempDir.resolve("pom-javax-critical.jar");
        String pom = """
                     <?xml version="1.0"?>
                     <project>
                       <dependencies>
                         <dependency>
                           <groupId>javax.persistence</groupId>
                           <artifactId>javax.persistence-api</artifactId>
                           <version>2.2</version>
                         </dependency>
                       </dependencies>
                     </project>
                     """;
        TestJarBuilder.create()
            .withPomXml(pom)
            .build(jar);

        var signal = extractor.enhanceSignal(jar, newBaseSignal());

        assertThat(signal.pomIndicatesJavax()).isTrue();
    }

    @Test
    void parsePomPropertiesContainingJavax() throws IOException {
        Path jar = tempDir.resolve("props-javax.jar");
        TestJarBuilder.create()
            .withPomProperties("groupId=javax.servlet\nartifactId=javax.servlet-api")
            .build(jar);

        var signal = extractor.enhanceSignal(jar, newBaseSignal());

        assertThat(signal.pomIndicatesJavax()).isTrue();
    }

    @Test
    void parsePomPropertiesContainingJakarta() throws IOException {
        Path jar = tempDir.resolve("props-jakarta.jar");
        TestJarBuilder.create()
            .withPomProperties("groupId=jakarta.servlet\nartifactId=jakarta.servlet-api")
            .build(jar);

        var signal = extractor.enhanceSignal(jar, newBaseSignal());

        assertThat(signal.pomIndicatesJakarta()).isTrue();
    }

    @Test
    void extractAutomaticModuleNameFromManifest() throws IOException {
        Path jar = tempDir.resolve("module-name.jar");
        TestJarBuilder.create()
            .withManifest("javax.servlet-api")
            .build(jar);

        var signal = extractor.enhanceSignal(jar, newBaseSignal());

        assertThat(signal.automaticModuleName()).isEqualTo("javax.servlet-api");
    }

    @Test
    void extractImplementationTitleFromManifest() throws IOException {
        Path jar = tempDir.resolve("impl-title.jar");
        TestJarBuilder.create()
            .withManifest("module", "javax.faces")
            .build(jar);

        var signal = extractor.enhanceSignal(jar, newBaseSignal());

        assertThat(signal.pomIndicatesJavax()).isTrue();
    }

    @Test
    void handleJarWithoutManifest() throws IOException {
        Path jar = tempDir.resolve("no-manifest.jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jar))) {}

        var signal = extractor.enhanceSignal(jar, newBaseSignal());

        assertThat(signal.automaticModuleName()).isNull();
    }

    @Test
    void enhanceSignalWithJavaxMetadata() throws IOException {
        Path jar = tempDir.resolve("enhance-javax.jar");
        String pom = """
                     <?xml version="1.0"?>
                     <project>
                       <dependencies>
                         <dependency>
                           <groupId>javax.servlet</groupId>
                           <artifactId>javax.servlet-api</artifactId>
                         </dependency>
                       </dependencies>
                     </project>
                     """;
        TestJarBuilder.create()
            .withPomXml(pom)
            .build(jar);

        var base = new JarScanSignal.Builder()
            .artifactCoordinate("test:enhance:1.0")
            .javaxClassRefs(0)
            .jakartaClassRefs(0)
            .build();
        var signal = extractor.enhanceSignal(jar, base);

        assertThat(signal.pomIndicatesJavax()).isTrue();
    }

    private JarScanSignal newBaseSignal() {
        return new JarScanSignal.Builder()
            .artifactCoordinate("test:base:1.0")
            .javaxClassRefs(0)
            .jakartaClassRefs(0)
            .build();
    }
}
