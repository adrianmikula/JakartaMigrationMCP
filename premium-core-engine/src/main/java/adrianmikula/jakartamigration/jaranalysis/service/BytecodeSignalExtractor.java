package adrianmikula.jakartamigration.jaranalysis.service;

import adrianmikula.jakartamigration.jaranalysis.domain.JarScanSignal;
import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Extracts compatibility signals from JAR bytecode using ASM.
 * Reuses detection patterns from AsmBytecodeAnalyzer but focused on
 * collecting detailed signal counts rather than runtime verification.
 * 
 * Thread-safe and stateless - can be used by multiple threads concurrently.
 */
@Slf4j
public class BytecodeSignalExtractor {

    /**
     * Extract compatibility signals from a JAR file.
     * 
     * @param jarPath Path to JAR file
     * @param maxClasses Maximum number of classes to scan (0 = unlimited)
     * @return JarScanSignal with extracted signals
     * @throws IOException if JAR cannot be read
     */
    public JarScanSignal extractFromJar(java.nio.file.Path jarPath, int maxClasses) throws IOException {
        Objects.requireNonNull(jarPath, "jarPath cannot be null");
        
        String artifactCoordinate = inferArtifactCoordinate(jarPath);
        
        // Use sets to track unique class references
        Set<String> javaxClasses = new HashSet<>();
        Set<String> jakartaClasses = new HashSet<>();
        Map<String, Integer> apiUsage = new HashMap<>();
        Set<String> reflectionStrings = new HashSet<>();
        boolean hasPomMetadata = false;
        boolean pomIndicatesJavax = false;
        boolean pomIndicatesJakarta = false;
        String automaticModuleName = null;
        boolean hasShadedPackages = false;
        Set<String> testOnlyPatterns = new HashSet<>();
        
        int classesScanned = 0;
        
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            // Check for pom.xml in META-INF
            hasPomMetadata = checkForPomMetadata(jarFile);
            
            // Check manifest for module name
            automaticModuleName = getAutomaticModuleName(jarFile);
            
            // Scan class files
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements() && (maxClasses == 0 || classesScanned < maxClasses)) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                
                if (entryName.endsWith(".class") && !entryName.contains("$")) {
                    try (InputStream is = jarFile.getInputStream(entry)) {
                        ClassReader reader = new ClassReader(is);
                        SignalCollectingVisitor visitor = new SignalCollectingVisitor(
                            javaxClasses, jakartaClasses, apiUsage, reflectionStrings);
                        reader.accept(visitor, 
                            ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                        classesScanned++;
                    } catch (Exception e) {
                        log.trace("Failed to analyze class {} in {}: {}", 
                            entryName, jarPath.getFileName(), e.getMessage());
                    }
                }
                
                // Check for shaded/relocated packages
                if (entryName.endsWith(".class") && entryName.contains("/shaded/") || entryName.contains("/repackaged/")) {
                    hasShadedPackages = true;
                }
                
                // Check for test-specific patterns
                if (entryName.contains("/test/") || entryName.contains("/Test")) {
                    testOnlyPatterns.add("test-directory-structure");
                }
            }
        } catch (IOException e) {
            log.warn("Failed to read JAR {}: {}", jarPath, e.getMessage());
            throw e;
        }
        
        // Check for reflection patterns in class names/constants (simplified)
        if (automaticModuleName != null) {
            checkForReflectionStrings(automaticModuleName, reflectionStrings);
        }
        
        // Build and return signal
        return new JarScanSignal.Builder()
            .artifactCoordinate(artifactCoordinate)
            .javaxClassRefs(javaxClasses.size())
            .jakartaClassRefs(jakartaClasses.size())
            .apiUsage(Map.copyOf(apiUsage))
            .reflectionStrings(reflectionStrings.toArray(new String[0]))
            .hasPomMetadata(hasPomMetadata)
            .pomIndicatesJavax(pomIndicatesJavax)
            .pomIndicatesJakarta(pomIndicatesJakarta)
            .automaticModuleName(automaticModuleName)
            .hasShadedPackages(hasShadedPackages)
            .testOnlyPatterns(testOnlyPatterns.toArray(new String[0]))
            .build();
    }
    
    /**
     * Checks JAR for Maven pom.xml metadata.
     * Detects any pom.xml file under META-INF/maven/ directory.
     */
    private boolean checkForPomMetadata(JarFile jarFile) {
        // Check for pom.xml in META-INF/maven/ (standard Maven metadata location)
        // Note: JARs typically don't have explicit directory entries, so we iterate entries
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            String name = entries.nextElement().getName();
            if (name.startsWith("META-INF/maven/") && name.endsWith("pom.xml")) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Extracts Automatic-Module-Name from JAR manifest.
     */
    private String getAutomaticModuleName(JarFile jarFile) {
        try {
            java.util.jar.Manifest manifest = jarFile.getManifest();
            if (manifest != null) {
                return manifest.getMainAttributes().getValue("Automatic-Module-Name");
            }
        } catch (Exception e) {
            log.trace("No manifest or error reading manifest from {}: {}", 
                jarFile.getName(), e.getMessage());
        }
        return null;
    }
    
    /**
     * Infers artifact coordinate from JAR file name.
     */
    private String inferArtifactCoordinate(java.nio.file.Path jarPath) {
        String fileName = jarPath.getFileName().toString();
        // Remove .jar extension and hash/suffix
        // Format: artifactId-version.jar
        if (fileName.endsWith(".jar")) {
            fileName = fileName.substring(0, fileName.length() - 4);
            int lastDash = fileName.lastIndexOf('-');
            if (lastDash > 0) {
                String artifactId = fileName.substring(0, lastDash);
                String version = fileName.substring(lastDash + 1);
                return "unknown:" + artifactId + ":" + version;
            }
        }
        return "unknown:" + fileName + ":unknown";
    }
    
    private void checkForReflectionStrings(String text, Set<String> reflectionStrings) {
        if (text.contains("javax.")) {
            reflectionStrings.add("javax.");
        }
        if (text.contains("jakarta.")) {
            reflectionStrings.add("jakarta.");
        }
    }
    
    /**
     * ASM visitor that collects compatibility signals from class files.
     */
    private static class SignalCollectingVisitor extends ClassVisitor {
        private final Set<String> javaxClasses;
        private final Set<String> jakartaClasses;
        private final Map<String, Integer> apiUsage;
        private final Set<String> reflectionStrings;
        
        private String className;
        private boolean hasJavax = false;
        private boolean hasJakarta = false;
        
        public SignalCollectingVisitor(Set<String> javaxClasses, Set<String> jakartaClasses,
                Map<String, Integer> apiUsage, Set<String> reflectionStrings) {
            super(Opcodes.ASM9);
            this.javaxClasses = javaxClasses;
            this.jakartaClasses = jakartaClasses;
            this.apiUsage = apiUsage;
            this.reflectionStrings = reflectionStrings;
        }
        
        @Override
        public void visit(int version, int access, String name, 
                String signature, String superName, String[] interfaces) {
            this.className = name.replace('/', '.');
            
            checkNamespace(className);
            
            // Check superclass
            if (superName != null) {
                checkNamespace(superName.replace('/', '.'));
            }
            
            // Check interfaces
            if (interfaces != null) {
                for (String iface : interfaces) {
                    checkNamespace(iface.replace('/', '.'));
                }
            }
            
            super.visit(version, access, name, signature, superName, interfaces);
        }
        
        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            checkDescriptor(descriptor);
            return super.visitAnnotation(descriptor, visible);
        }
        
        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, 
                String signature, Object value) {
            checkDescriptor(descriptor);
            if (signature != null) {
                checkSignature(signature);
            }
            return super.visitField(access, name, descriptor, signature, value);
        }
        
        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, 
                String signature, String[] exceptions) {
            checkDescriptor(descriptor);
            if (signature != null) {
                checkSignature(signature);
            }
            if (exceptions != null) {
                for (String exception : exceptions) {
                    String exName = exception.replace('/', '.');
                    checkNamespace(exName);
                }
            }
            return new MethodSignalVisitor();
        }
        
        private void checkNamespace(String className) {
            if (className == null) return;
            
            if (className.startsWith("javax.")) {
                hasJavax = true;
                // Track critical APIs
                trackApiUsage(className);
            } else if (className.startsWith("jakarta.")) {
                hasJakarta = true;
                trackApiUsage(className);
            }
        }
        
        private void trackApiUsage(String className) {
            // Extract top-level package after javax/jakarta
            // e.g., javax.servlet.http.HttpServlet → servlet
            String[] parts = className.split("\\.");
            if (parts.length >= 2) {
                String apiCategory = parts[1].toLowerCase(); // servlet, persistence, etc.
                apiUsage.merge(apiCategory, 1, Integer::sum);
            }
        }
        
        private void checkDescriptor(String descriptor) {
            if (descriptor == null) return;
            
            // Parse Lpackage/Class; patterns
            int start = 0;
            while ((start = descriptor.indexOf('L', start)) >= 0) {
                int end = descriptor.indexOf(';', start);
                if (end > start) {
                    String className = descriptor.substring(start + 1, end).replace('/', '.');
                    checkNamespace(className);
                    start = end + 1;
                } else {
                    break;
                }
            }
        }
        
        private void checkSignature(String signature) {
            if (signature == null) return;
            
            if (signature.contains("javax/")) {
                hasJavax = true;
                reflectionStrings.add("javax.");
            }
            if (signature.contains("jakarta/")) {
                hasJakarta = true;
                reflectionStrings.add("jakarta.");
            }
        }
        
        /**
         * MethodVisitor that tracks namespace usage in method bodies.
         */
        private class MethodSignalVisitor extends MethodVisitor {
            public MethodSignalVisitor() {
                super(Opcodes.ASM9);
            }
            
            @Override
            public void visitTypeInsn(int opcode, String type) {
                checkNamespace(type.replace('/', '.'));
                super.visitTypeInsn(opcode, type);
            }
            
            @Override
            public void visitMethodInsn(int opcode, String owner, String name, 
                    String descriptor, boolean isInterface) {
                checkNamespace(owner.replace('/', '.'));
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            }
            
            @Override
            public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                checkNamespace(owner.replace('/', '.'));
                super.visitFieldInsn(opcode, owner, name, descriptor);
            }
            
            @Override
            public void visitLdcInsn(Object value) {
                if (value instanceof String) {
                    String str = (String) value;
                    if (str.contains("javax.")) {
                        hasJavax = true;
                        reflectionStrings.add("javax.");
                    }
                    if (str.contains("jakarta.")) {
                        hasJakarta = true;
                        reflectionStrings.add("jakarta.");
                    }
                }
                super.visitLdcInsn(value);
            }
        }
        
        @Override
        public void visitEnd() {
            // Finalize class-level results
            if (hasJavax) {
                javaxClasses.add(className);
            }
            if (hasJakarta) {
                jakartaClasses.add(className);
            }
        }
    }
}
