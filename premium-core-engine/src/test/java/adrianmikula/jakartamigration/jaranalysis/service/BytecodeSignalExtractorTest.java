package adrianmikula.jakartamigration.jaranalysis.service;

import adrianmikula.jakartamigration.testutil.TestJarBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.*;

class BytecodeSignalExtractorTest {
    private final BytecodeSignalExtractor extractor = new BytecodeSignalExtractor();
    @TempDir
    Path tempDir;

    @Test
    void extractFromEmptyJar() throws IOException {
        Path jar = tempDir.resolve("empty.jar");
        try (java.util.jar.JarOutputStream jos = new java.util.jar.JarOutputStream(java.nio.file.Files.newOutputStream(jar))) {}
        var signal = extractor.extractFromJar(jar, 0);
        assertThat(signal.javaxClassRefs()).isZero();
        assertThat(signal.jakartaClassRefs()).isZero();
        assertThat(signal.apiUsage()).isEmpty();
        assertThat(signal.reflectionStrings()).isEmpty();
        assertThat(signal.hasPomMetadata()).isFalse();
        assertThat(signal.pomIndicatesJavax()).isFalse();
        assertThat(signal.pomIndicatesJakarta()).isFalse();
        assertThat(signal.automaticModuleName()).isNull();
        assertThat(signal.hasShadedPackages()).isFalse();
        assertThat(signal.testOnlyPatterns()).isEmpty();
    }

    @Test
    void detectJavaxClassReferencesInSuperclass() throws IOException {
        Path jar = tempDir.resolve("javax-servlet.jar");
        TestJarBuilder.create().withClass(TestJarBuilder.ClassSpec.builder("test/MyServlet").withSuper("javax/servlet/http/HttpServlet")).build(jar);
        var signal = extractor.extractFromJar(jar, 0);
        assertThat(signal.javaxClassRefs()).isGreaterThan(0);
        assertThat(signal.jakartaClassRefs()).isZero();
        assertThat(signal.hasJavaxSignal()).isTrue();
        assertThat(signal.hasJakartaSignal()).isFalse();
    }

    @Test
    void detectJavaxClassReferencesInInterfaces() throws IOException {
        Path jar = tempDir.resolve("javax-listener.jar");
        TestJarBuilder.create().withClass(TestJarBuilder.ClassSpec.builder("test/MyListener").withInterface("javax/servlet/ServletContextListener")).build(jar);
        var signal = extractor.extractFromJar(jar, 0);
        assertThat(signal.javaxClassRefs()).isGreaterThan(0);
        assertThat(signal.jakartaClassRefs()).isZero();
    }

    @Test
    void detectJavaxInFieldDescriptors() throws IOException {
        Path jar = tempDir.resolve("javax-field.jar");
        TestJarBuilder.create().withClass(TestJarBuilder.ClassSpec.builder("test/MyBean").withField(TestJarBuilder.FieldSpec.of(Opcodes.ACC_PRIVATE, "request", "Ljavax/servlet/http/HttpServletRequest;"))).build(jar);
        var signal = extractor.extractFromJar(jar, 0);
        assertThat(signal.javaxClassRefs()).isGreaterThan(0);
    }

    @Test
    void detectJavaxInMethodDescriptors() throws IOException {
        Path jar = tempDir.resolve("javax-method.jar");
        TestJarBuilder.create().withClass(TestJarBuilder.ClassSpec.builder("test/MyConsumer").withMethod(TestJarBuilder.MethodSpec.of(Opcodes.ACC_PUBLIC, "consume", "(Ljavax/servlet/http/HttpServletRequest;)V"))).build(jar);
        var signal = extractor.extractFromJar(jar, 0);
        assertThat(signal.javaxClassRefs()).isGreaterThan(0);
    }

    @Test
    void detectJavaxInAnnotations() throws IOException {
        Path jar = tempDir.resolve("javax-annotations.jar");
        TestJarBuilder.create().withClass(TestJarBuilder.ClassSpec.builder("test/InjectBean").withAnnotation(TestJarBuilder.AnnotationSpec.of("Ljavax/inject/Inject;"))).build(jar);
        var signal = extractor.extractFromJar(jar, 0);
        assertThat(signal.javaxClassRefs()).isGreaterThan(0);
    }

    @Test
    void detectJavaxInGenericSignatures() throws IOException {
        String sig = "Ljava/util/List<Ljavax/servlet/http/HttpServlet;>;";
        TestJarBuilder.ClassSpec spec = TestJarBuilder.ClassSpec.builder("test/GenericBean").withField(TestJarBuilder.FieldSpec.of(Opcodes.ACC_PRIVATE, "list", "Ljava/util/List;").withSignature(sig));
        Path jar = tempDir.resolve("jgen.jar");
        TestJarBuilder.create().withClass(spec).build(jar);
        var signal = extractor.extractFromJar(jar, 0);
        assertThat(signal.reflectionStrings()).contains("javax.");
    }

    @Test
    void detectJakartaClassReferences() throws IOException {
        Path jar = tempDir.resolve("jakarta-servlet.jar");
        TestJarBuilder.create().withClass(TestJarBuilder.ClassSpec.builder("test/JakartaServlet").withSuper("jakarta/servlet/http/HttpServlet")).build(jar);
        var signal = extractor.extractFromJar(jar, 0);
        assertThat(signal.jakartaClassRefs()).isGreaterThan(0);
        assertThat(signal.javaxClassRefs()).isZero();
        assertThat(signal.hasJakartaSignal()).isTrue();
    }

    @Test
    void detectMixedNamespace() throws IOException {
        Path jar = tempDir.resolve("mixed.jar");
        TestJarBuilder.create().withClass(TestJarBuilder.ClassSpec.builder("test/Mixed").withSuper("javax/servlet/http/HttpServlet").withInterface("jakarta/servlet/Servlet")).build(jar);
        var signal = extractor.extractFromJar(jar, 0);
        assertThat(signal.javaxClassRefs()).isGreaterThan(0);
        assertThat(signal.jakartaClassRefs()).isGreaterThan(0);
        assertThat(signal.hasMixedSignal()).isTrue();
    }

    @Test
    void trackApiUsageByCategory() throws IOException {
        Path jar = tempDir.resolve("api.jar");
        TestJarBuilder.create().withClass(TestJarBuilder.ClassSpec.builder("test/ServletBean").withSuper("javax/servlet/http/HttpServlet")).build(jar);
        var signal = extractor.extractFromJar(jar, 0);
        assertThat(signal.apiUsage()).containsKey("servlet");
        assertThat(signal.apiUsage().get("servlet")).isGreaterThan(0);
    }

    @Test
    void detectReflectionStringsViaSignature() throws IOException {
        String sig = "Ljava/util/Map<Ljava/lang/String;Ljavax/servlet/http/HttpServlet;>;";
        TestJarBuilder.ClassSpec spec = TestJarBuilder.ClassSpec.builder("test/ReflectBean").withField(TestJarBuilder.FieldSpec.of(Opcodes.ACC_PRIVATE, "map", "Ljava/util/Map;").withSignature(sig));
        Path jar = tempDir.resolve("refl.jar");
        TestJarBuilder.create().withClass(spec).build(jar);
        var signal = extractor.extractFromJar(jar, 0);
        assertThat(signal.reflectionStrings()).contains("javax.");
    }

    @Test
    void skipInnerClasses() throws IOException {
        Path jar = tempDir.resolve("inner.jar");
        TestJarBuilder.create().withClass(TestJarBuilder.ClassSpec.builder("test/Outer")).withClass(TestJarBuilder.ClassSpec.builder("test/Outer$Inner")).build(jar);
        var signal = extractor.extractFromJar(jar, 0);
        assertThat(signal.javaxClassRefs()).isZero();
        assertThat(signal.jakartaClassRefs()).isZero();
    }

    @Test
    void respectMaxClassesLimit() throws IOException {
        Path jar = tempDir.resolve("many.jar");
        var b = TestJarBuilder.create();
        for (int i = 0; i < 20; i++) b.withClass(TestJarBuilder.ClassSpec.builder("test/Cls" + i).withSuper("javax/servlet/http/HttpServlet"));
        b.build(jar);
        var signal = extractor.extractFromJar(jar, 5);
        assertThat(signal.javaxClassRefs()).isEqualTo(5);
    }

    @Test
    void handleCorruptClassFile() throws IOException {
        Path jar = tempDir.resolve("corrupt.jar");
        try (java.util.jar.JarOutputStream jos = new java.util.jar.JarOutputStream(java.nio.file.Files.newOutputStream(jar))) {
            JarEntry entry = new JarEntry("corrupt.class");
            jos.putNextEntry(entry);
            jos.write(new byte[]{(byte)0xCA, (byte)0xFE, (byte)0xBA});
            jos.closeEntry();
        }
        var signal = extractor.extractFromJar(jar, 0);
        assertThat(signal.javaxClassRefs()).isZero();
        assertThat(signal.jakartaClassRefs()).isZero();
        assertThat(signal.apiUsage()).isEmpty();
    }

    @Test
    void detectShadedPackages() throws IOException {
        Path jar = tempDir.resolve("shaded.jar");
        TestJarBuilder.create()
            .withClass(TestJarBuilder.ClassSpec.builder("com/example/shaded/javax/servlet/HttpServlet"))
            .withClass(TestJarBuilder.ClassSpec.builder("com/example/repackaged/jakarta/persistence/Entity"))
            .build(jar);
        var signal = extractor.extractFromJar(jar, 0);
        assertThat(signal.hasShadedPackages()).isTrue();
    }

    @Test
    void detectTestOnlyPatterns() throws IOException {
        Path jar = tempDir.resolve("test-only.jar");
        TestJarBuilder.create()
            .withClass(TestJarBuilder.ClassSpec.builder("test/ProductionClass"))
            .withClass(TestJarBuilder.ClassSpec.builder("test/TestHelper"))
            .withClass(TestJarBuilder.ClassSpec.builder("javax/servlet/MyTestServlet"))
            .build(jar);
        var signal = extractor.extractFromJar(jar, 0);
        assertThat(signal.testOnlyPatterns()).contains("test-directory-structure");
    }

    @Test
    void detectPomMetadata() throws IOException {
        Path jar = tempDir.resolve("with-pom.jar");
        String pomXml = "<project><modelVersion>4.0.0</modelVersion><groupId>test</groupId><artifactId>test-artifact</artifactId><version>1.0</version></project>";
        TestJarBuilder.create()
            .withClass(TestJarBuilder.ClassSpec.builder("test/MyClass"))
            .withPomXml(pomXml)
            .build(jar);
        var signal = extractor.extractFromJar(jar, 0);
        // BytecodeSignalExtractor detects POM presence (any file under META-INF/maven/ ending in pom.xml)
        assertThat(signal.hasPomMetadata()).isTrue();
    }

    // Removed POM content detection tests - those belong in MetadataSignalExtractorTest

    // POM content interpretation (javax/jakarta detection from POM dependencies) occurs in MetadataSignalExtractor, not BytecodeSignalExtractor
    // The following tests are for metadata extraction and belong in MetadataSignalExtractorTest:
    // - pomIndicatesJavaxNamespace (moved)
    // - pomIndicatesJakartaNamespace (moved)
    // Removed those tests from this file.

    @Test
    void pomIndicatesJakartaNamespace() throws IOException {
        Path jar = tempDir.resolve("pom-jakarta.jar");
        String pomXml = "<project><modelVersion>4.0.0</modelVersion><groupId>jakarta</groupId><artifactId>jakarta-servlet</artifactId><version>6.0.0</version></project>";
        TestJarBuilder.create()
            .withClass(TestJarBuilder.ClassSpec.builder("test/MyClass"))
            .withPomXml(pomXml)
            .build(jar);
        var signal = extractor.extractFromJar(jar, 0);
        assertThat(signal.hasPomMetadata()).isTrue();
    }

    @Test
    void extractAutomaticModuleName() throws IOException {
        Path jar = tempDir.resolve("module-name.jar");
        TestJarBuilder.create()
            .withManifest("com.example.myModule")
            .withClass(TestJarBuilder.ClassSpec.builder("test/MyClass"))
            .build(jar);
        var signal = extractor.extractFromJar(jar, 0);
        assertThat(signal.automaticModuleName()).isEqualTo("com.example.myModule");
    }

    @Test
    void detectReflectionInAutomaticModuleName() throws IOException {
        Path jar = tempDir.resolve("module-javax.jar");
        TestJarBuilder.create()
            .withManifest("javax.servlet")
            .withClass(TestJarBuilder.ClassSpec.builder("test/MyClass"))
            .build(jar);
        var signal = extractor.extractFromJar(jar, 0);
        assertThat(signal.reflectionStrings()).contains("javax.");
    }

    @Test
    void inferArtifactCoordinateFromFilename() throws IOException {
        Path jar = tempDir.resolve("myartifact-1.2.3.jar");
        TestJarBuilder.create()
            .withClass(TestJarBuilder.ClassSpec.builder("test/MyClass"))
            .build(jar);
        var signal = extractor.extractFromJar(jar, 0);
        assertThat(signal.artifactCoordinate()).contains("myartifact");
        assertThat(signal.artifactCoordinate()).contains("1.2.3");
    }

    @Test
    void trackMultipleApiCategories() throws IOException {
        Path jar = tempDir.resolve("multi-api.jar");
        TestJarBuilder.create()
            .withClass(TestJarBuilder.ClassSpec.builder("test/ServletBean").withSuper("javax/servlet/http/HttpServlet"))
            .withClass(TestJarBuilder.ClassSpec.builder("test/PersistenceBean").withSuper("javax/persistence/Entity"))
            .build(jar);
        var signal = extractor.extractFromJar(jar, 0);
        assertThat(signal.apiUsage()).containsKeys("servlet", "persistence");
    }

    @Test
    void detectJavaxInMethodParameters() throws IOException {
        Path jar = tempDir.resolve("params.jar");
        String descriptor = "(Ljavax/servlet/http/HttpServletRequest;Ljava/lang/String;)V";
        TestJarBuilder.ClassSpec spec = TestJarBuilder.ClassSpec.builder("test/ParamBean")
            .withMethod(TestJarBuilder.MethodSpec.of(Opcodes.ACC_PUBLIC, "process", descriptor));
        TestJarBuilder.create().withClass(spec).build(jar);
        var signal = extractor.extractFromJar(jar, 0);
        assertThat(signal.javaxClassRefs()).isGreaterThan(0);
    }

    @Test
    void detectJavaxInExceptionTypes() throws IOException {
        Path jar = tempDir.resolve("exceptions.jar");
        String[] exceptions = {"javax/servlet/ServletException"};
        TestJarBuilder.ClassSpec spec = TestJarBuilder.ClassSpec.builder("test/ExceptionBean")
            .withMethod(TestJarBuilder.MethodSpec.of(Opcodes.ACC_PUBLIC, "doGet", "()V").withExceptions(exceptions));
        TestJarBuilder.create().withClass(spec).build(jar);
        var signal = extractor.extractFromJar(jar, 0);
        assertThat(signal.javaxClassRefs()).isGreaterThan(0);
    }

    @Test
    void detectMultipleClassesWithMaxLimit() throws IOException {
        Path jar = tempDir.resolve("multi-limit.jar");
        var builder = TestJarBuilder.create();
        for (int i = 0; i < 100; i++) {
            builder.withClass(TestJarBuilder.ClassSpec.builder("test/Cls" + i).withSuper("javax/servlet/http/HttpServlet"));
        }
        builder.build(jar);
        var signal = extractor.extractFromJar(jar, 10);
        assertThat(signal.javaxClassRefs()).isEqualTo(10);
    }

    @Test
    void handleEmptyJarFile() throws IOException {
        Path jar = tempDir.resolve("totally-empty.jar");
        try (java.util.jar.JarOutputStream jos = new java.util.jar.JarOutputStream(java.nio.file.Files.newOutputStream(jar))) {}
        var signal = extractor.extractFromJar(jar, 0);
        assertThat(signal.javaxClassRefs()).isZero();
        assertThat(signal.jakartaClassRefs()).isZero();
    }

    @Test
    void nullJarPathThrowsException() {
        assertThatThrownBy(() -> extractor.extractFromJar(null, 0))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("jarPath cannot be null");
    }

    @Test
    void handleMultipleShadedPatterns() throws IOException {
        Path jar = tempDir.resolve("multi-shaded.jar");
        TestJarBuilder.create()
            .withClass(TestJarBuilder.ClassSpec.builder("org/shaded/javax/servlet/HttpServlet"))
            .withClass(TestJarBuilder.ClassSpec.builder("com/repackaged/jakarta/persistence/Entity"))
            .withClass(TestJarBuilder.ClassSpec.builder("net/relocated/SomeClass"))
            .build(jar);
        var signal = extractor.extractFromJar(jar, 0);
        assertThat(signal.hasShadedPackages()).isTrue();
    }

    @Test
    void detectJavaxInLdcStrings() throws IOException {
        Path jar = tempDir.resolve("ldc-strings.jar");
        TestJarBuilder.ClassSpec spec = TestJarBuilder.ClassSpec.builder("test/LdcBean")
            .withMethod(TestJarBuilder.MethodSpec.of(Opcodes.ACC_PUBLIC, "getClass", "()Ljava/lang/Class;"));
        TestJarBuilder.create().withClass(spec).build(jar);
        var signal = extractor.extractFromJar(jar, 0);
        assertThat(signal.reflectionStrings()).doesNotContain("javax.");
    }

    @Test
    void analyzeJarWithZeroMaxClasses() throws IOException {
        Path jar = tempDir.resolve("unlimited.jar");
        var builder = TestJarBuilder.create();
        for (int i = 0; i < 50; i++) {
            builder.withClass(TestJarBuilder.ClassSpec.builder("test/Cls" + i).withSuper("javax/servlet/http/HttpServlet"));
        }
        builder.build(jar);
        var signal = extractor.extractFromJar(jar, 0);
        assertThat(signal.javaxClassRefs()).isEqualTo(50);
    }

    @Test
    void artifactCoordinateFallbackForUnparsableName() throws IOException {
        Path jar = tempDir.resolve("complex-name-suffix.jar");
        TestJarBuilder.create()
            .withClass(TestJarBuilder.ClassSpec.builder("test/WeirdName"))
            .build(jar);
        var signal = extractor.extractFromJar(jar, 0);
        assertThat(signal.artifactCoordinate()).startsWith("unknown:");
    }

    @Test
    void trackDeepApiCategory() throws IOException {
        Path jar = tempDir.resolve("deep-api.jar");
        TestJarBuilder.create()
            .withClass(TestJarBuilder.ClassSpec.builder("test/ActivationBean").withSuper("javax/activation/DataSource"))
            .build(jar);
        var signal = extractor.extractFromJar(jar, 0);
        assertThat(signal.apiUsage()).containsKey("activation");
    }

    @Test
    void skipAbstractClasses() throws IOException {
        Path jar = tempDir.resolve("abstract.jar");
        TestJarBuilder.create()
            .withClass(TestJarBuilder.ClassSpec.builder("test/AbstractBase").withAccess(Opcodes.ACC_ABSTRACT))
            .withClass(TestJarBuilder.ClassSpec.builder("test/ConcreteChild").withSuper("test/AbstractBase").withSuper("javax/servlet/http/HttpServlet"))
            .build(jar);
        var signal = extractor.extractFromJar(jar, 0);
        assertThat(signal.javaxClassRefs()).isGreaterThan(0);
    }

    @Test
    void processJarWithMultipleEntryPoints() throws IOException {
        Path jar = tempDir.resolve("multi-entry.jar");
        var builder = TestJarBuilder.create();
        for (int i = 0; i < 5; i++) {
            builder.withClass(TestJarBuilder.ClassSpec.builder("test/Servlet" + i).withSuper("javax/servlet/http/HttpServlet"));
        }
        builder.build(jar);
        var signal = extractor.extractFromJar(jar, 0);
        assertThat(signal.javaxClassRefs()).isEqualTo(5);
        assertThat(signal.apiUsage().get("servlet")).isEqualTo(5);
    }
}